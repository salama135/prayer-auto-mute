package com.example.prayerautomute

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

class MuteWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            // Mute the phone
            mutePhone(applicationContext)

            // Create notification for Task A
            createNotification("Phone Muted", "Phone muted as scheduled!")

            Result.success()
        } catch (exception: Exception) {
            Result.failure()
        }
    }

    private fun mutePhone(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Check if we have permission to modify Do Not Disturb settings on Android 6.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.isNotificationPolicyAccessGranted) {
                // Use Do Not Disturb mode for better control
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            } else {
                // Fallback: just set to vibrate mode (doesn't require special permission)
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            }
        } else {
            // For older Android versions
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        }
    }

    private fun createNotification(title: String, content: String) {
        val notificationId = if (title.contains("Muted")) 1 else 2

        val notification = NotificationCompat.Builder(applicationContext, "TASK_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
}

class UnmuteWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            // Unmute the phone
            unmutePhone(applicationContext)

            // Create notification for Task B
            createNotification("Phone Unmuted", "Phone unmuted as scheduled!")

            Result.success()
        } catch (exception: Exception) {
            Result.failure()
        }
    }

    private fun unmutePhone(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Restore normal ringer mode
        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
    }

    private fun createNotification(title: String, content: String) {
        val notificationId = if (title.contains("Muted")) 1 else 2

        val notification = NotificationCompat.Builder(applicationContext, "TASK_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
}

// Keep the old class names for backward compatibility if needed
typealias TaskAWorker = MuteWorker
typealias TaskBWorker = UnmuteWorker