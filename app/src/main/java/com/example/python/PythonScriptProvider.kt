package com.example.python

object PythonScriptProvider {

    val KIVY_PYTHON_SCRIPT: String = """
# ==============================================================================
# Gemini Android AI Assistant (Kivy + Termux / Android System APIs)
# Features:
#   1. Google Gemini API integration (google-generativeai)
#   2. Speech-to-Text (STT) via Termux API or Android Pyjnius
#   3. Text-to-Speech (TTS) via termux-tts-speak or Android TTS
#   4. Voice Commands: Launch WhatsApp, YouTube, TikTok, Easypaisa
#   5. Read Unread Notifications via termux-notification-list
#   6. Clean Dark-Themed UI with visible, animated microphone button
#
# Termux Prerequisites:
#   pkg update && pkg upgrade -y
#   pkg install python termux-api git clang -y
#   pip install google-generativeai kivy requests
# ==============================================================================

import os
import sys
import json
import subprocess
import threading
import google.generativeai as genai
from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.label import Label
from kivy.uix.button import Button
from kivy.uix.scrollview import ScrollView
from kivy.uix.textinput import TextInput
from kivy.graphics import Color, RoundedRectangle, Ellipse
from kivy.clock import Clock
from kivy.core.window import Window

# Dark Cyber Theme configuration
Window.clearcolor = (0.04, 0.05, 0.08, 1)

GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY", "YOUR_GEMINI_API_KEY_HERE")
if GEMINI_API_KEY and GEMINI_API_KEY != "YOUR_GEMINI_API_KEY_HERE":
    genai.configure(api_key=GEMINI_API_KEY)
    gemini_model = genai.GenerativeModel(
        model_name="gemini-1.5-flash",
        system_instruction=(
            "You are a fast, intelligent, polite Android voice assistant. "
            "Keep answers concise (2 to 4 sentences) and natural for voice readout."
        )
    )
else:
    gemini_model = None


# ------------------------------------------------------------------------------
# System & Termux Hardware Connectors
# ------------------------------------------------------------------------------

def speak_text(text: str):
    \"\"\"Speaks text using Termux TTS or system API\"\"\"
    clean_text = text.replace('"', '').replace("'", "").strip()
    try:
        # 1. Try Termux TTS API
        subprocess.run(
            ["termux-tts-speak", "-r", "1.0", "-p", "1.0", clean_text],
            timeout=15,
            check=False
        )
    except Exception:
        try:
            # 2. Fallback to pyttsx3 or termux-toast
            subprocess.run(["termux-toast", clean_text], check=False)
        except Exception as e:
            print(f"[TTS Error] {e}")


def listen_speech() -> str:
    \"\"\"Captures speech from mic via termux-speech-to-text\"\"\"
    try:
        proc = subprocess.run(
            ["termux-speech-to-text"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=12
        )
        if proc.returncode == 0:
            return proc.stdout.strip()
    except subprocess.TimeoutExpired:
        return ""
    except Exception as e:
        print(f"[STT Error] {e}")
    return ""


def get_unread_notifications() -> list:
    \"\"\"Reads unread system notifications via termux-notification-list\"\"\"
    try:
        proc = subprocess.run(
            ["termux-notification-list"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=5
        )
        if proc.returncode == 0 and proc.stdout.strip():
            raw_items = json.loads(proc.stdout)
            notifications = []
            for item in raw_items:
                title = item.get("title", "")
                content = item.get("content", "")
                pkg = item.get("packageName", "")
                if title or content:
                    notifications.append({
                        "package": pkg,
                        "title": title,
                        "content": content
                    })
            return notifications
    except Exception as e:
        print(f"[Notification Error] {e}")
    return []


def launch_android_app(package_name: str, app_name: str) -> str:
    \"\"\"Launches target app via Android Activity Manager (am)\"\"\"
    try:
        # Check if am command is available (Termux/Root/ADB)
        subprocess.run(
            ["am", "start", "-a", "android.intent.action.MAIN", "-c", "android.intent.category.LAUNCHER", package_name],
            check=False
        )
        return f"Opening {app_name}…"
    except Exception:
        try:
            # Alternate monkey launch
            subprocess.run(["monkey", "-p", package_name, "-c", "android.intent.category.LAUNCHER", "1"], check=False)
            return f"Launching {app_name}…"
        except Exception as e:
            return f"Failed to open {app_name}: {e}"


# ------------------------------------------------------------------------------
# Voice Command Dispatcher
# ------------------------------------------------------------------------------

def process_command(user_query: str, callback_update_ui):
    query_lower = user_query.lower().strip()
    callback_update_ui("User", user_query)

    # 1. Custom Voice Command: WhatsApp
    if "open whatsapp" in query_lower or "launch whatsapp" in query_lower:
        msg = launch_android_app("com.whatsapp", "WhatsApp")
        callback_update_ui("Gemini Voice AI", msg)
        speak_text(msg)
        return

    # 2. Custom Voice Command: YouTube
    if "open youtube" in query_lower or "launch youtube" in query_lower:
        msg = launch_android_app("com.google.android.youtube", "YouTube")
        callback_update_ui("Gemini Voice AI", msg)
        speak_text(msg)
        return

    # 3. Custom Voice Command: TikTok
    if "open tiktok" in query_lower or "launch tiktok" in query_lower:
        msg = launch_android_app("com.zhiliaoapp.musically", "TikTok")
        callback_update_ui("Gemini Voice AI", msg)
        speak_text(msg)
        return

    # 4. Custom Voice Command: Easypaisa
    if "open easypaisa" in query_lower or "launch easypaisa" in query_lower or "easy paisa" in query_lower:
        msg = launch_android_app("pk.com.telenor.phoenix", "Easypaisa")
        callback_update_ui("Gemini Voice AI", msg)
        speak_text(msg)
        return

    # 5. Feature: Read Notifications
    if "notification" in query_lower or "read my messages" in query_lower:
        notifications = get_unread_notifications()
        if not notifications:
            reply = "You have no unread notifications at this time."
        else:
            count = len(notifications)
            reply = f"You have {count} active notifications. "
            for i, n in enumerate(notifications[:3]):
                reply += f"From {n.get('title', 'Unknown')}: {n.get('content', '')}. "
        callback_update_ui("Gemini Voice AI", reply)
        speak_text(reply)
        return

    # 6. Default: Process with Google Gemini API
    if gemini_model:
        try:
            response = gemini_model.generate_content(user_query)
            reply = response.text.strip()
            callback_update_ui("Gemini Voice AI", reply)
            speak_text(reply)
        except Exception as e:
            err = f"Gemini API error: {e}"
            callback_update_ui("Error", err)
            speak_text("Sorry, I encountered an error connecting to Gemini.")
    else:
        fallback = (
            "Gemini API key is not configured. Please set GEMINI_API_KEY environment "
            "variable. Voice command features (WhatsApp, YouTube, TikTok, Easypaisa, "
            "and Notifications) remain active!"
        )
        callback_update_ui("Assistant", fallback)
        speak_text(fallback)


# ------------------------------------------------------------------------------
# Kivy User Interface
# ------------------------------------------------------------------------------

class DarkCard(BoxLayout):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        with self.canvas.before:
            Color(0.08, 0.10, 0.15, 1)
            self.rect = RoundedRectangle(size=self.size, pos=self.pos, radius=[12])
        self.bind(size=self._update_rect, pos=self._update_rect)

    def _update_rect(self, *args):
        self.rect.size = self.size
        self.rect.pos = self.pos


class MicButton(Button):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.background_color = (0, 0, 0, 0)
        self.text = "🎤"
        self.font_size = "34sp"
        self.size_hint = (None, None)
        self.size = (90, 90)
        self.is_active = False
        with self.canvas.before:
            self.glow_color = Color(0, 0.89, 1.0, 0.3)
            self.glow = Ellipse(size=(110, 110), pos=(self.center_x - 55, self.center_y - 55))
            self.bg_color = Color(0, 0.89, 1.0, 1.0)
            self.circle = Ellipse(size=self.size, pos=self.pos)
        self.bind(pos=self._update, size=self._update)

    def _update(self, *args):
        self.circle.pos = self.pos
        self.circle.size = self.size
        self.glow.pos = (self.center_x - 55, self.center_y - 55)

    def set_listening(self, active: bool):
        self.is_active = active
        if active:
            self.bg_color.rgba = (1.0, 0.2, 0.4, 1.0)
            self.glow_color.rgba = (1.0, 0.2, 0.4, 0.5)
            self.text = "🛑"
        else:
            self.bg_color.rgba = (0, 0.89, 1.0, 1.0)
            self.glow_color.rgba = (0, 0.89, 1.0, 0.3)
            self.text = "🎤"


class VoiceAssistantApp(App):
    def build(self):
        self.title = "Gemini Voice AI"
        root = BoxLayout(orientation='vertical', padding=16, spacing=12)

        # Header
        header = BoxLayout(size_hint_y=None, height=50)
        header.add_widget(Label(
            text="[b]✦ Gemini Voice AI[/b]",
            markup=True,
            font_size="22sp",
            color=(0, 0.89, 1.0, 1),
            halign="left"
        ))
        root.add_widget(header)

        # Status Label
        self.status_label = Label(
            text="Ready. Tap the mic or type below.",
            size_hint_y=None,
            height=30,
            font_size="13sp",
            color=(0.6, 0.65, 0.75, 1)
        )
        root.add_widget(self.status_label)

        # Chat / Log Scroll View
        scroll = ScrollView(size_hint=(1, 1), do_scroll_x=False)
        self.chat_layout = BoxLayout(orientation='vertical', size_hint_y=None, spacing=8)
        self.chat_layout.bind(minimum_height=self.chat_layout.setter('height'))
        scroll.add_widget(self.chat_layout)
        root.add_widget(scroll)

        # Quick action chips
        chips = BoxLayout(size_hint_y=None, height=44, spacing=8)
        apps = [("WhatsApp", "open whatsapp"), ("YouTube", "open youtube"), ("TikTok", "open tiktok"), ("Easypaisa", "open easypaisa"), ("Notify", "read notifications")]
        for name, cmd in apps:
            b = Button(
                text=name,
                size_hint=(1, 1),
                background_normal='',
                background_color=(0.12, 0.15, 0.22, 1),
                color=(0.9, 0.95, 1, 1)
            )
            b.bind(on_release=lambda instance, c=cmd: self.send_text_command(c))
            chips.add_widget(b)
        root.add_widget(chips)

        # Bottom Bar: Input + Mic Button
        bottom_bar = BoxLayout(size_hint_y=None, height=90, spacing=12)

        self.input_field = TextInput(
            hint_text="Type command or question…",
            multiline=False,
            size_hint=(1, None),
            height=50,
            pos_hint={'center_y': 0.5},
            background_color=(0.1, 0.12, 0.18, 1),
            foreground_color=(1, 1, 1, 1)
        )
        self.input_field.bind(on_text_validate=lambda _: self.send_text_command(self.input_field.text))
        bottom_bar.add_widget(self.input_field)

        self.mic_btn = MicButton(pos_hint={'center_y': 0.5})
        self.mic_btn.bind(on_release=self.on_mic_tap)
        bottom_bar.add_widget(self.mic_btn)

        root.add_widget(bottom_bar)
        return root

    def append_message(self, sender: str, text: str):
        def _add(*_):
            card = DarkCard(size_hint_y=None, height=75, padding=10)
            is_user = sender == "User"
            c = "[color=00e5ff]" if not is_user else "[color=00e676]"
            label = Label(
                text=f"{c}[b]{sender}:[/b][/color] {text}",
                markup=True,
                text_size=(Window.width - 64, None),
                color=(0.95, 0.95, 0.98, 1),
                halign="left"
            )
            card.add_widget(label)
            self.chat_layout.add_widget(card)
        Clock.schedule_once(_add)

    def on_mic_tap(self, instance):
        if self.mic_btn.is_active:
            self.mic_btn.set_listening(False)
            self.status_label.text = "Microphone stopped."
            return

        self.mic_btn.set_listening(True)
        self.status_label.text = "Listening… Speak now."

        def _voice_worker():
            spoken = listen_speech()
            Clock.schedule_once(lambda _: self.mic_btn.set_listening(False))
            if spoken:
                Clock.schedule_once(lambda _: setattr(self.status_label, 'text', f"Processing: '{spoken}'"))
                process_command(spoken, self.append_message)
            else:
                Clock.schedule_once(lambda _: setattr(self.status_label, 'text', "No speech detected. Try again."))

        threading.Thread(target=_voice_worker, daemon=True).start()

    def send_text_command(self, query: str):
        if not query.strip():
            return
        self.input_field.text = ""
        self.status_label.text = "Processing command…"
        threading.Thread(target=process_command, args=(query, self.append_message), daemon=True).start()


if __name__ == "__main__":
    VoiceAssistantApp().run()
""".trimIndent()

    val BEEWARE_PYTHON_SNIPPET: String = """
# ==============================================================================
# BeeWare / Toga Alternative Implementation
# Install: pip install toga google-generativeai
# Run: briefcase dev
# ==============================================================================

import toga
from toga.style import Pack
from toga.style.pack import COLUMN, ROW, CENTER
import google.generativeai as genai
import subprocess
import os

genai.configure(api_key=os.environ.get("GEMINI_API_KEY", "YOUR_API_KEY"))

class GeminiVoiceAssistant(toga.App):
    def startup(self):
        main_box = toga.Box(style=Pack(direction=COLUMN, padding=16, background_color="#0A0D14"))

        self.title_label = toga.Label("Gemini Voice AI", style=Pack(padding=(0, 0, 10, 0), color="#00E5FF", font_size=20))
        self.log_area = toga.MultilineTextInput(readonly=True, style=Pack(flex=1, background_color="#131824", color="#F0F4FC"))
        
        btn_box = toga.Box(style=Pack(direction=ROW, alignment=CENTER, padding=10))
        self.mic_btn = toga.Button("🎤 Tap to Speak", on_press=self.on_mic_press, style=Pack(background_color="#00E5FF", color="#000000", padding=8))
        btn_box.add(self.mic_btn)
        
        main_box.add(self.title_label)
        main_box.add(self.log_area)
        main_box.add(btn_box)

        self.main_window = toga.MainWindow(title=self.formal_name)
        self.main_window.content = main_box
        self.main_window.show()

    def on_mic_press(self, widget):
        self.log_area.value += "\\n[System] Listening...\\n"
        proc = subprocess.run(["termux-speech-to-text"], stdout=subprocess.PIPE, text=True)
        query = proc.stdout.strip()
        if query:
            self.log_area.value += f"[User] {query}\\n"
            if "open whatsapp" in query.lower():
                subprocess.run(["am", "start", "-n", "com.whatsapp/.Main"])
                self.log_area.value += "[Assistant] Launching WhatsApp...\\n"
            else:
                model = genai.GenerativeModel("gemini-1.5-flash")
                response = model.generate_content(query)
                self.log_area.value += f"[Gemini] {response.text}\\n"
                subprocess.run(["termux-tts-speak", response.text])

def main():
    return GeminiVoiceAssistant("Gemini Voice AI", "org.gemini.voiceai")
""".trimIndent()
}
