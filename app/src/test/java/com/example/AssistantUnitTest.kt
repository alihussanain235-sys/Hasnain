package com.example

import com.example.launcher.AppLauncher
import com.example.model.AssistantSettings
import com.example.python.PythonScriptProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantUnitTest {

    @Test
    fun testPythonScriptContainsRequiredFeatures() {
        val script = PythonScriptProvider.KIVY_PYTHON_SCRIPT

        // Verify Gemini API integration
        assertTrue(script.contains("google.generativeai"))
        assertTrue(script.contains("genai.GenerativeModel"))

        // Verify Voice recognition and TTS
        assertTrue(script.contains("termux-speech-to-text"))
        assertTrue(script.contains("termux-tts-speak"))

        // Verify App launch custom voice commands
        assertTrue(script.contains("com.whatsapp"))
        assertTrue(script.contains("com.google.android.youtube"))
        assertTrue(script.contains("com.zhiliaoapp.musically"))
        assertTrue(script.contains("pk.com.telenor.phoenix"))

        // Verify Unread Notifications feature
        assertTrue(script.contains("termux-notification-list"))

        // Verify Kivy UI and Dark Theme
        assertTrue(script.contains("class VoiceAssistantApp"))
        assertTrue(script.contains("class MicButton"))
    }

    @Test
    fun testAppShortcutsConfigured() {
        assertEquals(4, AppLauncher.QUICK_APPS.size)
        assertEquals("WhatsApp", AppLauncher.WHATSAPP.name)
        assertEquals("com.whatsapp", AppLauncher.WHATSAPP.packageName)

        assertEquals("YouTube", AppLauncher.YOUTUBE.name)
        assertEquals("com.google.android.youtube", AppLauncher.YOUTUBE.packageName)

        assertEquals("TikTok", AppLauncher.TIKTOK.name)
        assertEquals("com.zhiliaoapp.musically", AppLauncher.TIKTOK.packageName)

        assertEquals("Easypaisa", AppLauncher.EASYPAISA.name)
        assertEquals("pk.com.telenor.phoenix", AppLauncher.EASYPAISA.packageName)
    }

    @Test
    fun testAssistantSettingsDefaults() {
        val settings = AssistantSettings()
        assertEquals(1.0f, settings.speechRate, 0.01f)
        assertEquals(1.0f, settings.pitch, 0.01f)
        assertTrue(settings.autoSpeak)
        assertTrue(settings.customApiKey.isEmpty())
    }
}
