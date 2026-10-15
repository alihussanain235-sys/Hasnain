package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.model.NotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AssistantNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListener"
        private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
        val activeNotifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

        var isConnected: Boolean = false
            private set

        fun isNotificationAccessGranted(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val myPackageName = context.packageName
            return enabledListeners.contains(myPackageName)
        }

        fun openNotificationAccessSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open notification listener settings", e)
            }
        }

        fun clearNotifications() {
            _notifications.value = emptyList()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        Log.d(TAG, "Notification Listener Connected")
        refreshActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        Log.d(TAG, "Notification Listener Disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let { extractAndAddNotification(it) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn?.let { removed ->
            _notifications.value = _notifications.value.filterNot {
                it.packageName == removed.packageName && it.title == getTitle(removed)
            }
        }
    }

    private fun refreshActiveNotifications() {
        try {
            val sbns = activeNotifications ?: return
            val items = sbns.mapNotNull { extractNotification(it) }
            _notifications.value = items.take(20)
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing notifications", e)
        }
    }

    private fun extractAndAddNotification(sbn: StatusBarNotification) {
        val item = extractNotification(sbn) ?: return
        val current = _notifications.value.toMutableList()
        // Deduplicate
        current.removeAll { it.packageName == item.packageName && it.title == item.title }
        current.add(0, item)
        _notifications.value = current.take(25)
    }

    private fun extractNotification(sbn: StatusBarNotification): NotificationItem? {
        val pkg = sbn.packageName ?: return null
        // Skip system noise
        if (pkg == "android" || pkg == "com.android.systemui") return null

        val extras = sbn.notification.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        if (title.isBlank() && text.isBlank()) return null

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        } catch (e: Exception) {
            pkg.substringAfterLast('.')
        }

        return NotificationItem(
            packageName = pkg,
            appName = appName,
            title = title.ifBlank { "Notification from $appName" },
            text = text.ifBlank { "New update" },
            timestamp = sbn.postTime
        )
    }

    private fun getTitle(sbn: StatusBarNotification): String {
        return sbn.notification.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
    }
}
