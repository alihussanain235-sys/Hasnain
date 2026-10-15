package com.example.model

import java.util.UUID

enum class MessageSender {
    USER,
    ASSISTANT,
    SYSTEM
}

enum class ActionType {
    NONE,
    LAUNCH_APP,
    READ_NOTIFICATIONS,
    GEMINI_RESPONSE,
    PYTHON_SCRIPT,
    ERROR
}

data class AppShortcut(
    val name: String,
    val packageName: String,
    val description: String,
    val iconKey: String,
    val fallbackUrl: String
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: ActionType = ActionType.NONE,
    val appTarget: AppShortcut? = null,
    val isSpoken: Boolean = false,
    val notificationsRead: List<NotificationItem>? = null
)

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AssistantSettings(
    val customApiKey: String = "",
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val autoSpeak: Boolean = true
)
