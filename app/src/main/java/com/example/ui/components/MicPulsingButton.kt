package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonEmerald

@Composable
fun MicPulsingButton(
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    rmsDb: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")

    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.45f else if (isProcessing) 1.25f else 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 650 else 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale_1"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.7f else 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 800 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale_2"
    )

    // Dynamic scale according to speech volume
    val volumeScale = if (isListening) {
        1.0f + (rmsDb / 15f).coerceIn(0f, 0.35f)
    } else {
        1.0f
    }

    val primaryColor = when {
        isListening -> CrimsonError
        isSpeaking -> NeonEmerald
        isProcessing -> ElectricViolet
        else -> CyberCyan
    }

    val glowGradient = Brush.radialGradient(
        colors = listOf(
            primaryColor.copy(alpha = if (isListening) 0.6f else 0.3f),
            primaryColor.copy(alpha = 0.05f),
            Color.Transparent
        )
    )

    val buttonGradient = Brush.linearGradient(
        colors = when {
            isListening -> listOf(CrimsonError, Color(0xFFFF8A80))
            isSpeaking -> listOf(NeonEmerald, CyberCyan)
            isProcessing -> listOf(ElectricViolet, CyberCyan)
            else -> listOf(CyberCyan, ElectricViolet)
        }
    )

    Box(
        modifier = modifier
            .size(130.dp)
            .semantics { contentDescription = "Microphone control button" },
        contentAlignment = Alignment.Center
    ) {
        // Outer animated ripple ring 2
        if (isListening || isProcessing) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale2 * volumeScale)
                    .clip(CircleShape)
                    .background(glowGradient)
            )
        }

        // Animated ripple ring 1
        Box(
            modifier = Modifier
                .size(98.dp)
                .scale(pulseScale1 * volumeScale)
                .clip(CircleShape)
                .background(glowGradient)
        )

        // Main Button Core
        Box(
            modifier = Modifier
                .size(76.dp)
                .scale(volumeScale)
                .clip(CircleShape)
                .background(buttonGradient)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = DarkBackground),
                    onClick = onClick
                )
                .testTag("mic_pulse_button"),
            contentAlignment = Alignment.Center
        ) {
            val icon = when {
                isListening -> Icons.Default.Stop
                isSpeaking -> Icons.Default.GraphicEq
                isProcessing -> Icons.Default.MicOff
                else -> Icons.Default.Mic
            }

            Icon(
                imageVector = icon,
                contentDescription = if (isListening) "Stop listening" else "Start listening",
                tint = DarkBackground,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
