package ru.echoapp.ui.record

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.echoapp.EchoApp
import ru.echoapp.R
import ru.echoapp.audio.AudioRecorder
import ru.echoapp.audio.SoundEffects
import ru.echoapp.audio.Transcriber
import ru.echoapp.settings.AudioQuality
import java.io.File
import java.util.Locale

private const val MAX_DURATION_MS = 120_000L
private const val MIN_DURATION_MS = 1_000L
private const val PROMPT_SILENCE_MS = 5_000L
private const val AUTO_STOP_SILENCE_MS = 15_000L
private const val SILENCE_THRESHOLD = 500

/**
 * Состояние записи живёт здесь, а не в composable: запись переживает
 * навигацию между экранами и поворот. Экран лишь отображает и шлёт команды.
 */
class RecordViewModel(app: Application) : AndroidViewModel(app) {

    private val echoApp get() = getApplication<EchoApp>()
    private val recorder = AudioRecorder(app)
    private val transcriber = Transcriber(app)
    private val prompts = app.resources.getStringArray(R.array.prompts)

    var isRecording by mutableStateOf(false); private set
    var elapsedMs by mutableLongStateOf(0L); private set
    var saving by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var sealedUntil by mutableStateOf<Long?>(null); private set
    var showMoodPicker by mutableStateOf(false); private set
    var showPrompt by mutableStateOf(false); private set
    var promptIndex by mutableIntStateOf(0); private set

    private var pendingFile by mutableStateOf<File?>(null)
    private var pendingDuration by mutableLongStateOf(0L)
    private var watchJob: Job? = null

    fun maxAmplitude(): Int = recorder.maxAmplitude()

    fun toggleRecording(quality: AudioQuality, locale: Locale, autoStopSilence: Boolean) {
        error = null
        if (!isRecording) startRecording(quality, locale, autoStopSilence) else stopForMood()
    }

    fun dismissMoodPicker() {
        showMoodPicker = false
    }

    fun setSeal(until: Long?) {
        sealedUntil = until
    }

    fun save(mood: Int, rootReplyId: Long?, parentReplyId: Long?, onSaved: () -> Unit) {
        val file = pendingFile
        val duration = pendingDuration
        if (file == null || duration < MIN_DURATION_MS || !file.exists()) {
            showMoodPicker = false
            error = echoApp.getString(R.string.record_error_short)
            return
        }
        viewModelScope.launch {
            saving = true
            val result = runCatching {
                if (rootReplyId != null) {
                    null to echoApp.repository.saveReply(rootReplyId, parentReplyId, file, duration, mood).id
                } else {
                    echoApp.repository.saveRecording(file, duration, mood, sealedUntil).id to null
                }
            }
            saving = false
            val (savedRecordingId, savedReplyId) = result.getOrElse {
                file.delete()
                error = echoApp.getString(R.string.record_error_save)
                return@launch
            }
            // Транскрипция — опционально, не ломает запись.
            transcriber.finish { text ->
                if (!text.isNullOrBlank()) {
                    viewModelScope.launch {
                        savedRecordingId?.let { echoApp.repository.updateTranscript(it, text) }
                        savedReplyId?.let { echoApp.repository.updateReplyTranscript(it, text) }
                    }
                }
            }
            pendingFile = null
            SoundEffects.click()
            onSaved()
        }
    }

    private fun startRecording(quality: AudioQuality, locale: Locale, autoStopSilence: Boolean) {
        runCatching {
            recorder.start(quality = quality)
            SoundEffects.click()
            transcriber.start(locale)
        }
            .onSuccess {
                isRecording = true
                startWatching(autoStopSilence)
            }
            .onFailure { error = echoApp.getString(R.string.record_error_start) }
    }

    private fun stopForMood() {
        if (!isRecording) return
        isRecording = false
        val result = recorder.stop()
        val file = result?.first
        val duration = result?.second ?: 0L
        if (file == null || duration < MIN_DURATION_MS) {
            transcriber.cancel()
            pendingFile = File("")
            pendingDuration = 0L
        } else {
            pendingFile = file
            pendingDuration = duration
        }
        showMoodPicker = true
    }

    private fun startWatching(autoStopSilence: Boolean) {
        watchJob?.cancel()
        watchJob = viewModelScope.launch {
            // Таймер записи.
            launch {
                while (isRecording) {
                    elapsedMs = recorder.elapsedMs()
                    delay(100)
                }
                elapsedMs = 0L
            }
            // Автостоп на 120 секундах.
            launch {
                delay(MAX_DURATION_MS)
                if (isRecording) stopForMood()
            }
            // Промпты при молчании + автостоп по тишине.
            launch {
                var lastLoud = System.currentTimeMillis()
                var lastPromptRotation = 0L
                while (isRecording) {
                    val amplitude = recorder.maxAmplitude()
                    if (amplitude > SILENCE_THRESHOLD) {
                        lastLoud = System.currentTimeMillis()
                        if (showPrompt) showPrompt = false
                    } else {
                        val silentFor = System.currentTimeMillis() - lastLoud
                        if (silentFor > PROMPT_SILENCE_MS) {
                            if (!showPrompt) showPrompt = true
                            if (System.currentTimeMillis() - lastPromptRotation > 10_000) {
                                promptIndex = (promptIndex + 1) % prompts.size
                                lastPromptRotation = System.currentTimeMillis()
                            }
                        }
                        if (autoStopSilence && silentFor > AUTO_STOP_SILENCE_MS) {
                            stopForMood()
                            break
                        }
                    }
                    delay(200)
                }
                showPrompt = false
            }
        }
    }

    override fun onCleared() {
        watchJob?.cancel()
        if (isRecording) {
            recorder.cancel()
        } else {
            pendingFile?.takeIf { it.exists() }?.delete()
        }
        transcriber.cancel()
    }
}
