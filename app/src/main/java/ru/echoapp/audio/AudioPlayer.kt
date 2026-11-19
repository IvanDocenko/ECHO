package ru.echoapp.audio

import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ru.echoapp.data.RecordingEntity
import java.io.IOException

data class PlaybackState(
    val recordingId: Long? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
)

/** Простой плеер: играет одну запись за раз, состояние — во Flow для Compose. */
class AudioPlayer {

    private var player: MediaPlayer? = null
    private var current: RecordingEntity? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            val mp = player ?: return
            runCatching {
                _state.value = _state.value.copy(positionMs = mp.currentPosition.toLong())
            }
            if (_state.value.isPlaying) handler.postDelayed(this, 200)
        }
    }

    fun play(recording: RecordingEntity) {
        playFile(recording.id, recording.filePath)
    }

    /** Проигрывает любой аудиофайл (запись или ответ) с тем же состоянием плеера. */
    fun playFile(itemId: Long, filePath: String) {
        if (current?.id == itemId && _state.value.isPlaying) {
            pause()
            return
        }
        stopInternal()
        try {
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(filePath)
            mediaPlayer.setOnCompletionListener {
                _state.value = _state.value.copy(isPlaying = false, positionMs = 0)
                handler.removeCallbacks(tick)
            }
            mediaPlayer.prepare()
            mediaPlayer.start()
            player = mediaPlayer
            current = null
            _state.value = PlaybackState(
                recordingId = itemId,
                isPlaying = true,
                durationMs = mediaPlayer.duration.toLong(),
            )
            handler.post(tick)
        } catch (e: IOException) {
            stopInternal()
        }
    }

    private fun pause() {
        player?.pause()
        _state.value = _state.value.copy(isPlaying = false)
        handler.removeCallbacks(tick)
    }

    fun stop() = stopInternal()

    /** Перемотка текущей записи на [positionMs]. */
    fun seekTo(positionMs: Long) {
        player?.let { mp -> runCatching { mp.seekTo(positionMs.toInt()) } }
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    private fun stopInternal() {
        handler.removeCallbacks(tick)
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        current = null
        _state.value = PlaybackState()
    }
}
