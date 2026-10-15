package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistantSettings
import com.example.service.AssistantNotificationListenerService
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentSettings: AssistantSettings,
    onSave: (apiKey: String, pitch: Float, rate: Float, autoSpeak: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var apiKeyText by remember { mutableStateOf(currentSettings.customApiKey) }
    var pitch by remember { mutableFloatStateOf(currentSettings.pitch) }
    var speechRate by remember { mutableFloatStateOf(currentSettings.speechRate) }
    var autoSpeak by remember { mutableStateOf(currentSettings.autoSpeak) }

    val hasNotificationPermission = remember {
        AssistantNotificationListenerService.isNotificationAccessGranted(context)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        scrimColor = DarkBackground.copy(alpha = 0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Assistant Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_settings_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close settings",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gemini API Key Input
            Text(
                text = "Google Gemini API Key",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "Leave blank to use default configuration or enter your personal Gemini API key.",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            OutlinedTextField(
                value = apiKeyText,
                onValueChange = { apiKeyText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gemini_api_key_input"),
                placeholder = { Text("AIzaSy...", color = TextSecondary) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Key, contentDescription = null, tint = CyberCyan)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = DarkSurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkBackground,
                    unfocusedContainerColor = DarkBackground
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Voice Speech Controls
            Text(
                text = "Speech Synthesis (TTS)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            // Auto-speak toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Auto-Speak Responses", fontSize = 14.sp, color = TextPrimary)
                    Text("Automatically vocalize Gemini answers", fontSize = 11.sp, color = TextSecondary)
                }
                Switch(
                    checked = autoSpeak,
                    onCheckedChange = { autoSpeak = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBackground,
                        checkedTrackColor = CyberCyan
                    ),
                    modifier = Modifier.testTag("auto_speak_switch")
                )
            }

            // Speech Rate
            Text(
                text = "Speech Rate: ${"%.1f".format(speechRate)}x",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
            Slider(
                value = speechRate,
                onValueChange = { speechRate = it },
                valueRange = 0.5f..2.0f,
                steps = 6,
                colors = SliderDefaults.colors(
                    thumbColor = CyberCyan,
                    activeTrackColor = CyberCyan,
                    inactiveTrackColor = DarkSurfaceBorder
                ),
                modifier = Modifier.testTag("speech_rate_slider")
            )

            // Pitch
            Text(
                text = "Voice Pitch: ${"%.1f".format(pitch)}x",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
            Slider(
                value = pitch,
                onValueChange = { pitch = it },
                valueRange = 0.5f..1.8f,
                steps = 5,
                colors = SliderDefaults.colors(
                    thumbColor = ElectricViolet,
                    activeTrackColor = ElectricViolet,
                    inactiveTrackColor = DarkSurfaceBorder
                ),
                modifier = Modifier.testTag("pitch_slider")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Notification Access Shortcut
            Text(
                text = "Notification Reader Access",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = if (hasNotificationPermission) "✓ Notification access is active" else "⚠ Permission needed to read notifications aloud",
                fontSize = 11.sp,
                color = if (hasNotificationPermission) NeonEmerald else TextSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            OutlinedButton(
                onClick = {
                    AssistantNotificationListenerService.openNotificationAccessSettings(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_notification_settings_btn"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (hasNotificationPermission) NeonEmerald else CyberCyan
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (hasNotificationPermission) NeonEmerald.copy(alpha = 0.5f) else CyberCyan.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (hasNotificationPermission) "Manage Notification Access" else "Grant Notification Permission",
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    onSave(apiKeyText.trim(), pitch, speechRate, autoSpeak)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_settings_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkBackground),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    text = "Save Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
