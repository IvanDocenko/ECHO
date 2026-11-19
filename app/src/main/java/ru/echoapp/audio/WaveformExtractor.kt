package ru.echoapp.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Достаёт реальные амплитуды из готового .m4a (AAC): декодирует в PCM через
 * MediaExtractor + MediaCodec и сворачивает в [buckets] уровней (0..100),
 * чтобы рисовать настоящий waveform у старых записей.
 */
object WaveformExtractor {

    /** Возвращает строку уровней "n,n,n…" (0..100) или null при ошибке декодирования. */
    suspend fun extract(filePath: String, buckets: Int = 48): String? =
        withContext(Dispatchers.IO) {
            runCatching { extractSync(filePath, buckets) }.getOrNull()
        }

    fun parse(levels: String?): List<Int> =
        levels?.split(',')?.mapNotNull { it.toIntOrNull() } ?: emptyList()

    private fun extractSync(filePath: String, buckets: Int): String? {
        val file = File(filePath)
        if (!file.exists()) return null

        val extractor = MediaExtractor()
        extractor.setDataSource(filePath)
        val trackIndex = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: run { extractor.release(); return null }
        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: return null

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val info = MediaCodec.BufferInfo()
        // Пик каждого выходного буфера PCM; в конце ресемплим в [buckets] равномерно.
        val chunkPeaks = mutableListOf<Double>()
        var done = false
        val timeoutUs = 10_000L

        try {
            while (!done) {
                // Вливаем данные из контейнера.
                val inIdx = codec.dequeueInputBuffer(timeoutUs)
                if (inIdx >= 0) {
                    val buffer = codec.getInputBuffer(inIdx) ?: continue
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(
                            inIdx, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                        )
                    } else {
                        codec.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }

                // Забираем PCM.
                val outIdx = codec.dequeueOutputBuffer(info, timeoutUs)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                    outIdx >= 0 -> {
                        val out = codec.getOutputBuffer(outIdx) ?: continue
                        out.position(info.offset)
                        val shortBuffer = out.slice().asShortBuffer()
                        val shorts = ShortArray(shortBuffer.remaining())
                        shortBuffer.get(shorts)
                        codec.releaseOutputBuffer(outIdx, false)
                        var peak = 0.0
                        for (s in shorts) {
                            val amp = kotlin.math.abs(s.toInt()) / 32768.0
                            if (amp > peak) peak = amp
                        }
                        if (shorts.isNotEmpty()) chunkPeaks.add(peak)
                    }
                }
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) done = true
            }
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }

        if (chunkPeaks.isEmpty()) return null
        val peaks = DoubleArray(buckets) { b ->
            val from = (b * chunkPeaks.size / buckets)
            val to = (((b + 1) * chunkPeaks.size / buckets)).coerceAtLeast(from + 1)
            (from until to.coerceAtMost(chunkPeaks.size)).maxOf { chunkPeaks[it] }
        }
        val max = peaks.maxOrNull()?.takeIf { it > 0.0 } ?: return null
        return peaks.joinToString(",") { (it / max * 100.0).toInt().coerceIn(0, 100).toString() }
    }
}
