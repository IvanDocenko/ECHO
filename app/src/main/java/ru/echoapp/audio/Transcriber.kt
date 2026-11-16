package ru.echoapp.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Офлайн-транскрипция параллельно с записью. Полностью опциональна:
 * если распознавание недоступно или падает — запись сохраняется без текста.
 */
class Transcriber(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var pendingResult: ((String?) -> Unit)? = null
    private var lastText: String? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(locale: Locale) {
        if (!isAvailable()) return
        runCatching {
            val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) { /* молча игнорируем */ }
                override fun onResults(results: Bundle?) {
                    lastText = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, locale)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            recognizer = speechRecognizer
            speechRecognizer.startListening(intent)
        }
    }

    /** Останавливает распознавание; результат придёт в callback (или null). */
    fun finish(onText: (String?) -> Unit) {
        val speechRecognizer = recognizer
        if (speechRecognizer == null) {
            onText(null)
            return
        }
        pendingResult = onText
        runCatching { speechRecognizer.stopListening() }
        // stopListening не гарантирует callback — забираем результат с небольшой задержкой.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            runCatching { speechRecognizer.destroy() }
            recognizer = null
            pendingResult?.invoke(lastText)
            pendingResult = null
        }, 1200)
    }

    fun cancel() {
        runCatching { recognizer?.destroy() }
        recognizer = null
        pendingResult = null
    }
}
