package ru.echoapp.audio

import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ru.echoapp.data.RecordingEntity
import java.io.IOException

data class PlaybackState(
    val recordingId: Long? = null,
    val isPlaying: Boolean = false,
)

/** Простой плеер: играет одну запись за раз, состояние — во Flow для Compose. */
class AudioPlayer {

    private var player: MediaPlayer? = null
    private var current: RecordingEntity? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state

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
                _state.value = PlaybackState(recordingId = itemId, isPlaying = false)
            }
            mediaPlayer.prepare()
            mediaPlayer.start()
            player = mediaPlayer
            current = null
            _state.value = PlaybackState(recordingId = itemId, isPlaying = true)
        } catch (e: IOException) {
            stopInternal()
        }
    }

    private fun pause() {
        player?.pause()
        _state.value = _state.value.copy(isPlaying = false)
    }

    fun stop() = stopInternal()

    private fun stopInternal() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        current = null
        _state.value = PlaybackState()
    }
}
