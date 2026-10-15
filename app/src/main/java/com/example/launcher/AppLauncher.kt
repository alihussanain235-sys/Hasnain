package com.example.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import com.example.model.AppShortcut

sealed class LaunchResult {
    data class Success(val appName: String, val message: String) : LaunchResult()
    data class FallbackOpened(val appName: String, val message: String) : LaunchResult()
    data class Error(val appName: String, val message: String) : LaunchResult()
}

class AppLauncher(private val context: Context) {

    companion object {
        private const val TAG = "AppLauncher"

        val WHATSAPP = AppShortcut(
            name = "WhatsApp",
            packageName = "com.whatsapp",
            description = "Chat & messaging",
            iconKey = "whatsapp",
            fallbackUrl = "https://play.google.com/store/apps/details?id=com.whatsapp"
        )

        val YOUTUBE = AppShortcut(
            name = "YouTube",
            packageName = "com.google.android.youtube",
            description = "Videos & music",
            iconKey = "youtube",
            fallbackUrl = "https://www.youtube.com"
        )

        val TIKTOK = AppShortcut(
            name = "TikTok",
            packageName = "com.zhiliaoapp.musically",
            description = "Short-form video",
            iconKey = "tiktok",
            fallbackUrl = "https://play.google.com/store/apps/details?id=com.zhiliaoapp.musically"
        )

        val EASYPAISA = AppShortcut(
            name = "Easypaisa",
            packageName = "pk.com.telenor.phoenix",
            description = "Mobile wallet & payments",
            iconKey = "easypaisa",
            fallbackUrl = "https://play.google.com/store/apps/details?id=pk.com.telenor.phoenix"
        )

        val QUICK_APPS = listOf(WHATSAPP, YOUTUBE, TIKTOK, EASYPAISA)
    }

    fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            // Also check alternate packages (e.g. TikTok alternative or WhatsApp Business)
            if (packageName == "com.zhiliaoapp.musically") {
                try {
                    context.packageManager.getPackageInfo("com.ss.android.ugc.trill", 0)
                    true
                } catch (e2: Exception) {
                    false
                }
            } else if (packageName == "com.whatsapp") {
                try {
                    context.packageManager.getPackageInfo("com.whatsapp.w4b", 0)
                    true
                } catch (e2: Exception) {
                    false
                }
            } else {
                false
            }
        }
    }

    fun launchApp(appShortcut: AppShortcut): LaunchResult {
        val pm = context.packageManager

        // Check primary package
        var launchIntent = pm.getLaunchIntentForPackage(appShortcut.packageName)

        // Alternate checks
        if (launchIntent == null && appShortcut.packageName == "com.zhiliaoapp.musically") {
            launchIntent = pm.getLaunchIntentForPackage("com.ss.android.ugc.trill")
        } else if (launchIntent == null && appShortcut.packageName == "com.whatsapp") {
            launchIntent = pm.getLaunchIntentForPackage("com.whatsapp.w4b")
        }

        return if (launchIntent != null) {
            try {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                LaunchResult.Success(appShortcut.name, "Launching ${appShortcut.name}…")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch ${appShortcut.name}", e)
                LaunchResult.Error(appShortcut.name, "Could not open ${appShortcut.name}: ${e.localizedMessage}")
            }
        } else {
            // Fallback to store / web
            try {
                val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(appShortcut.fallbackUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(storeIntent)
                LaunchResult.FallbackOpened(
                    appShortcut.name,
                    "${appShortcut.name} is not installed on this device. Opened Play Store/browser to download."
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open fallback URL for ${appShortcut.name}", e)
                LaunchResult.Error(appShortcut.name, "${appShortcut.name} is not installed.")
            }
        }
    }

    fun launchCamera(): LaunchResult {
        return try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            LaunchResult.Success("Camera", "Opening Camera…")
        } catch (e: Exception) {
            LaunchResult.Error("Camera", "Failed to open camera: ${e.localizedMessage}")
        }
    }

    fun launchSettings(): LaunchResult {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            LaunchResult.Success("Settings", "Opening System Settings…")
        } catch (e: Exception) {
            LaunchResult.Error("Settings", "Failed to open settings: ${e.localizedMessage}")
        }
    }
}
