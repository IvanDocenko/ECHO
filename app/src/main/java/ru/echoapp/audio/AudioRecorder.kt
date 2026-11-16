package ru.echoapp.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import ru.echoapp.settings.AudioQuality
import java.io.File

/** Обёртка над MediaRecorder: пишет AAC в .m4a во внутреннюю память приложения. */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var startedAtMs: Long = 0
    private var file: File? = null

    val isRecording: Boolean get() = recorder != null

    fun start(quality: AudioQuality = AudioQuality.STANDARD): File {
        val target = File(context.filesDir, "recordings/echo_${System.currentTimeMillis()}.m4a")
        target.parentFile?.mkdirs()

        val (bitrate, sampleRate) = when (quality) {
            AudioQuality.ECO -> 64_000 to 22_050
            AudioQuality.STANDARD -> 128_000 to 44_100
            AudioQuality.HIGH -> 192_000 to 48_000
        }

        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(bitrate)
            setAudioSamplingRate(sampleRate)
            setOutputFile(target.absolutePath)
            prepare()
            start()
        }
        recorder = mediaRecorder
        file = target
        startedAtMs = SystemClock.elapsedRealtime()
        return target
    }

    /** Текущая амплитуда (0..32767) для waveform; 0, если не пишем. */
    fun maxAmplitude(): Int = recorder?.maxAmplitude ?: 0

    /** Сколько миллисекунд идёт запись. */
    fun elapsedMs(): Long =
        if (recorder != null) SystemClock.elapsedRealtime() - startedAtMs else 0L

    /** Останавливает запись и возвращает файл и длительность в миллисекундах. */
    fun stop(): Pair<File, Long>? {
        val current = recorder ?: return null
        val target = file ?: return null
        return runCatching { current.stop() }
            .map { target to (SystemClock.elapsedRealtime() - startedAtMs) }
            .getOrNull()
            .also { result ->
                release()
                if (result == null) target.delete()
            }
    }

    /** Прерывает запись и удаляет файл. */
    fun cancel() {
        runCatching { recorder?.stop() }
        release()
        file?.delete()
    }

    private fun release() {
        runCatching { recorder?.release() }
        recorder = null
        file = null
    }
}
