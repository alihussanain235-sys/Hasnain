package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.launcher.AppLauncher
import com.example.model.ActionType
import com.example.model.AppShortcut
import com.example.model.ChatMessage
import com.example.model.MessageSender
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EasypaisaGreen
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.YouTubeRed

@Composable
fun MessageBubble(
    message: ChatMessage,
    onSpeakClick: () -> Unit,
    onAppLaunchClick: ((AppShortcut) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == MessageSender.USER
    val isSystem = message.sender == MessageSender.SYSTEM

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Assistant avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isSystem) DarkSurfaceVariant else CyberCyan.copy(alpha = 0.15f))
                    .border(1.dp, if (isSystem) DarkSurfaceBorder else CyberCyan.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSystem) Icons.Default.Notifications else Icons.Default.AutoAwesome,
                    contentDescription = "Assistant Avatar",
                    tint = if (isSystem) TextSecondary else CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = when {
                    isUser -> CyberCyan.copy(alpha = 0.18f)
                    isSystem -> DarkSurfaceVariant
                    else -> DarkSurface
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    when {
                        isUser -> CyberCyan.copy(alpha = 0.5f)
                        isSystem -> DarkSurfaceBorder
                        else -> DarkSurfaceBorder
                    }
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        lineHeight = 21.sp
                    )

                    // If message launched an app, show badge and button
                    message.appTarget?.let { shortcut ->
                        Spacer(modifier = Modifier.height(8.dp))
                        AppBadge(
                            shortcut = shortcut,
                            onClick = { onAppLaunchClick?.invoke(shortcut) }
                        )
                    }

                    // If message contains notifications
                    message.notificationsRead?.let { list ->
                        if (list.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Notifications summary (${list.size}):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyberCyan
                            )
                            list.take(3).forEach { notif ->
                                Text(
                                    text = "• ${notif.appName}: ${notif.title}",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom action row for assistant responses (TTS replay button)
            if (!isUser && !isSystem) {
                Row(
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onSpeakClick,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("tts_replay_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Read aloud again",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Listen",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ElectricViolet.copy(alpha = 0.2f))
                    .border(1.dp, ElectricViolet.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User Avatar",
                    tint = ElectricViolet,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AppBadge(
    shortcut: AppShortcut,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brandColor = when (shortcut.packageName) {
        "com.whatsapp" -> WhatsAppGreen
        "com.google.android.youtube" -> YouTubeRed
        "com.zhiliaoapp.musically" -> TikTokCyan
        "pk.com.telenor.phoenix" -> EasypaisaGreen
        else -> CyberCyan
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = brandColor.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, brandColor.copy(alpha = 0.5f)),
        modifier = modifier.testTag("launch_badge_${shortcut.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Launch,
                contentDescription = null,
                tint = brandColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Open ${shortcut.name}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = brandColor
            )
        }
    }
}

@Composable
fun QuickActionsRow(
    onCommandSelected: (String) -> Unit,
    onAppSelected: (AppShortcut) -> Unit,
    onReadNotifications: () -> Unit,
    onOpenPython: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickChip(label = "💬 WhatsApp", color = WhatsAppGreen) {
            onAppSelected(AppLauncher.WHATSAPP)
        }
        QuickChip(label = "▶️ YouTube", color = YouTubeRed) {
            onAppSelected(AppLauncher.YOUTUBE)
        }
        QuickChip(label = "🎵 TikTok", color = TikTokCyan) {
            onAppSelected(AppLauncher.TIKTOK)
        }
        QuickChip(label = "💳 Easypaisa", color = EasypaisaGreen) {
            onAppSelected(AppLauncher.EASYPAISA)
        }
        QuickChip(label = "🔔 Notifications", color = AmberWarningColor) {
            onReadNotifications()
        }
        QuickChip(label = "🐍 Python Script", color = ElectricViolet) {
            onOpenPython()
        }
        QuickChip(label = "✨ Tell Joke", color = CyberCyan) {
            onCommandSelected("Tell me a funny joke")
        }
    }
}

private val AmberWarningColor = Color(0xFFFFB300)

@Composable
private fun QuickChip(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
