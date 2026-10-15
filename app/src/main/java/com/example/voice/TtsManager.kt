package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TtsManager"
    }

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var currentRate = 1.0f
    private var currentPitch = 1.0f

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to English US
                tts?.setLanguage(Locale.US)
            }
            tts?.setSpeechRate(currentRate)
            tts?.setPitch(currentPitch)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    Log.w(TAG, "TTS Utterance error: $errorCode")
                }
            })
            isInitialized = true
            Log.d(TAG, "TTS Initialized successfully")
        } else {
            Log.e(TAG, "Failed to initialize TTS with status: $status")
        }
    }

    fun speak(text: String, onStart: (() -> Unit)? = null, onDone: (() -> Unit)? = null) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not yet initialized")
            return
        }
        val cleanText = cleanTextForSpeech(text)
        if (cleanText.isBlank()) return

        stop()
        val utteranceId = UUID.randomUUID().toString()
        _isSpeaking.value = true
        onStart?.invoke()

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        if (isInitialized && tts?.isSpeaking == true) {
            tts?.stop()
        }
        _isSpeaking.value = false
    }

    fun setSpeechRate(rate: Float) {
        currentRate = rate
        if (isInitialized) {
            tts?.setSpeechRate(rate)
        }
    }

    fun setPitch(pitch: Float) {
        currentPitch = pitch
        if (isInitialized) {
            tts?.setPitch(pitch)
        }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    private fun cleanTextForSpeech(raw: String): String {
        // Strip markdown asterisks, hashes, backticks, bullet symbols for smooth speaking
        return raw
            .replace(Regex("""[*_`#~>|]"""), "")
            .replace(Regex("""\[(.*?)\]\(.*?\)"""), "$1")
            .replace(Regex("""\n+"""), ". ")
            .trim()
    }
}
