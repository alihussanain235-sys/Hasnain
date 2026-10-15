package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiApiClient
import com.example.ai.GeminiResult
import com.example.launcher.AppLauncher
import com.example.launcher.LaunchResult
import com.example.model.ActionType
import com.example.model.AppShortcut
import com.example.model.AssistantSettings
import com.example.model.ChatMessage
import com.example.model.MessageSender
import com.example.model.NotificationItem
import com.example.service.AssistantNotificationListenerService
import com.example.voice.SpeechManager
import com.example.voice.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val speechManager = SpeechManager(application)
    private val ttsManager = TtsManager(application)
    private val appLauncher = AppLauncher(application)
    private val geminiClient = GeminiApiClient()

    // State flows from Speech & TTS
    val isListening: StateFlow<Boolean> = speechManager.isListening
    val rmsDb: StateFlow<Float> = speechManager.rmsDb
    val partialSpeech: StateFlow<String> = speechManager.partialText
    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    // Active notifications from service
    val activeNotifications: StateFlow<List<NotificationItem>> =
        AssistantNotificationListenerService.activeNotifications

    // Conversation messages
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "Hello! I am your Gemini Voice Assistant. Tap the microphone or say commands like 'Open WhatsApp', 'Launch YouTube', 'Open TikTok', 'Open Easypaisa', or 'Read my notifications'.",
                actionType = ActionType.GEMINI_RESPONSE
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // Status message for top banner
    private val _statusMessage = MutableStateFlow("Tap microphone to speak")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Processing indicator
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Settings
    private val _settings = MutableStateFlow(AssistantSettings())
    val settings: StateFlow<AssistantSettings> = _settings.asStateFlow()

    // Dialog visibility
    val showPythonDialog = MutableStateFlow(false)
    val showSettingsDialog = MutableStateFlow(false)

    init {
        // Load saved preferences
        val prefs = application.getSharedPreferences("gemini_voice_prefs", Context.MODE_PRIVATE)
        val key = prefs.getString("custom_api_key", "").orEmpty()
        val pitch = prefs.getFloat("tts_pitch", 1.0f)
        val rate = prefs.getFloat("tts_rate", 1.0f)
        val autoSpeak = prefs.getBoolean("tts_auto_speak", true)

        _settings.value = AssistantSettings(
            customApiKey = key,
            pitch = pitch,
            speechRate = rate,
            autoSpeak = autoSpeak
        )
        ttsManager.setPitch(pitch)
        ttsManager.setSpeechRate(rate)
    }

    fun onMicTapped() {
        if (isListening.value) {
            speechManager.stopListening()
            _statusMessage.value = "Microphone stopped"
        } else {
            ttsManager.stop()
            _statusMessage.value = "Listening to your voice…"
            speechManager.startListening(
                onResult = { recognizedText ->
                    handleUserQuery(recognizedText)
                },
                onError = { error ->
                    _statusMessage.value = error
                    // Add error message to chat if significant
                    if (!error.contains("No speech detected")) {
                        addMessage(
                            ChatMessage(
                                sender = MessageSender.SYSTEM,
                                text = "Speech error: $error",
                                actionType = ActionType.ERROR
                            )
                        )
                    }
                }
            )
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun handleUserQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        // Add user message
        addMessage(
            ChatMessage(
                sender = MessageSender.USER,
                text = trimmed
            )
        )

        _isProcessing.value = true
        _statusMessage.value = "Processing command…"

        val lower = trimmed.lowercase()

        // 1. Check Voice Command: WhatsApp
        if (lower.contains("open whatsapp") || lower.contains("launch whatsapp") || lower.contains("whatsapp")) {
            executeAppLaunch(AppLauncher.WHATSAPP)
            return
        }

        // 2. Check Voice Command: YouTube
        if (lower.contains("open youtube") || lower.contains("launch youtube") || lower.contains("youtube")) {
            executeAppLaunch(AppLauncher.YOUTUBE)
            return
        }

        // 3. Check Voice Command: TikTok
        if (lower.contains("open tiktok") || lower.contains("launch tiktok") || lower.contains("tiktok")) {
            executeAppLaunch(AppLauncher.TIKTOK)
            return
        }

        // 4. Check Voice Command: Easypaisa
        if (lower.contains("open easypaisa") || lower.contains("launch easypaisa") || lower.contains("easypaisa") || lower.contains("easy paisa")) {
            executeAppLaunch(AppLauncher.EASYPAISA)
            return
        }

        // 5. Check Voice Command: Read Notifications
        if (lower.contains("notification") || lower.contains("read messages") || lower.contains("read my notifications")) {
            readNotifications()
            return
        }

        // 6. Check Voice Command: Python script
        if (lower.contains("python") || lower.contains("kivy") || lower.contains("beeware") || lower.contains("script")) {
            _isProcessing.value = false
            _statusMessage.value = "Opening Python code exporter"
            showPythonDialog.value = true
            speakIfEnabled("Opening the Python assistant script for Kivy and BeeWare.")
            addMessage(
                ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = "Here is the complete Python script for Kivy and BeeWare with Termux and Android APIs.",
                    actionType = ActionType.PYTHON_SCRIPT
                )
            )
            return
        }

        // 7. General Voice Query: Process via Google Gemini API
        viewModelScope.launch {
            val result = geminiClient.generateResponse(
                prompt = trimmed,
                customKey = _settings.value.customApiKey
            )
            _isProcessing.value = false

            when (result) {
                is GeminiResult.Success -> {
                    _statusMessage.value = "Gemini replied"
                    addMessage(
                        ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = result.text,
                            actionType = ActionType.GEMINI_RESPONSE
                        )
                    )
                    speakIfEnabled(result.text)
                }

                is GeminiResult.Error -> {
                    _statusMessage.value = "Gemini request failed"
                    val msg = "Gemini encountered an issue: ${result.message}"
                    addMessage(
                        ChatMessage(
                            sender = MessageSender.SYSTEM,
                            text = msg,
                            actionType = ActionType.ERROR
                        )
                    )
                    speakIfEnabled(msg)
                }
            }
        }
    }

    fun executeAppLaunch(shortcut: AppShortcut) {
        _isProcessing.value = false
        val result = appLauncher.launchApp(shortcut)
        val replyText = when (result) {
            is LaunchResult.Success -> result.message
            is LaunchResult.FallbackOpened -> result.message
            is LaunchResult.Error -> result.message
        }

        _statusMessage.value = replyText
        addMessage(
            ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = replyText,
                actionType = ActionType.LAUNCH_APP,
                appTarget = shortcut
            )
        )
        speakIfEnabled(replyText)
    }

    fun readNotifications() {
        _isProcessing.value = false
        val context = getApplication<Application>()
        val hasAccess = AssistantNotificationListenerService.isNotificationAccessGranted(context)

        if (!hasAccess) {
            val msg = "Notification listener permission is required to read notifications. Please grant access in settings."
            _statusMessage.value = "Notification access required"
            addMessage(
                ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = msg,
                    actionType = ActionType.READ_NOTIFICATIONS
                )
            )
            speakIfEnabled(msg)
            return
        }

        val notifications = activeNotifications.value
        if (notifications.isEmpty()) {
            val msg = "You currently have no unread notifications."
            _statusMessage.value = msg
            addMessage(
                ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = msg,
                    actionType = ActionType.READ_NOTIFICATIONS
                )
            )
            speakIfEnabled(msg)
        } else {
            val count = notifications.size
            val summaryBuilder = StringBuilder()
            summaryBuilder.append("You have $count active notifications. ")
            notifications.take(3).forEachIndexed { index, item ->
                summaryBuilder.append("${item.appName}: ${item.title}. ${item.text}. ")
            }

            val fullText = summaryBuilder.toString()
            _statusMessage.value = "Read $count notifications"
            addMessage(
                ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = fullText,
                    actionType = ActionType.READ_NOTIFICATIONS,
                    notificationsRead = notifications
                )
            )
            speakIfEnabled(fullText)
        }
    }

    fun speakMessage(message: ChatMessage) {
        ttsManager.speak(message.text)
    }

    private fun speakIfEnabled(text: String) {
        if (_settings.value.autoSpeak) {
            ttsManager.speak(text)
        }
    }

    fun updateSettings(apiKey: String, pitch: Float, rate: Float, autoSpeak: Boolean) {
        _settings.value = AssistantSettings(
            customApiKey = apiKey,
            pitch = pitch,
            speechRate = rate,
            autoSpeak = autoSpeak
        )
        ttsManager.setPitch(pitch)
        ttsManager.setSpeechRate(rate)

        val prefs = getApplication<Application>().getSharedPreferences("gemini_voice_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("custom_api_key", apiKey)
            .putFloat("tts_pitch", pitch)
            .putFloat("tts_rate", rate)
            .putBoolean("tts_auto_speak", autoSpeak)
            .apply()
    }

    private fun addMessage(message: ChatMessage) {
        val current = _messages.value.toMutableList()
        current.add(message)
        _messages.value = current
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
        ttsManager.shutdown()
    }
}
