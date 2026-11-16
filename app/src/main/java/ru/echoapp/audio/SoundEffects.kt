package ru.echoapp.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Тёплый шелчок «кассеты» — синтезируется кодом, без ассетов. */
object SoundEffects {

    private const val SAMPLE_RATE = 22050
    private const val DURATION_S = 0.06

    fun click() {
        runCatching {
            val sampleCount = (SAMPLE_RATE * DURATION_S).toInt()
            val buffer = ShortArray(sampleCount)
            for (i in 0 until sampleCount) {
                val t = i.toFloat() / SAMPLE_RATE
                val envelope = exp(-t * 70.0)
                val tone = sin(2.0 * Math.PI * 2400.0 * t) * 0.45
                val noise = (Random.nextFloat() * 2f - 1f) * 0.55
                buffer[i] = ((tone + noise) * envelope * 12000.0).roundToInt()
                    .toShort()
                    .coerceIn(-32768, 32767)
            }
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(buffer, 0, buffer.size)
            track.play()
            // Освобождаем после проигрывания.
            Handler(Looper.getMainLooper()).postDelayed({
                runCatching {
                    track.stop()
                    track.release()
                }
            }, (DURATION_S * 1000).toLong() + 300)
        }
    }
}
