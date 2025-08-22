package com.example.prayerautomute.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

class PrayerTimeWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (hasDoNotDisturbPermission()) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT

            val unmuteWork = OneTimeWorkRequestBuilder<UnmuteWorker>()
                .setInitialDelay(20, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(applicationContext).enqueue(unmuteWork)
            showMuteNotification()
        }

        return Result.success()
    }

    private fun hasDoNotDisturbPermission(): Boolean {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            true
        }
    }

    private fun showMuteNotification() {
        val channelId = "prayer_mute_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Prayer Mute Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Prayer Time - Phone Muted")
            .setContentText("Your phone will be unmuted automatically in 20 minutes")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(1, notification)
        }
    }
}

class UnmuteWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL

        showUnmuteNotification()
        return Result.success()
    }

    private fun showUnmuteNotification() {
        val channelId = "prayer_mute_channel"

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Prayer Time Complete")
            .setContentText("Your phone has been unmuted")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(2, notification)
        }
    }
}