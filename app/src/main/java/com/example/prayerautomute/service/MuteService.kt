package com.example.prayerautomute.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

import com.example.prayerautomute.MainActivity
import com.example.prayerautomute.R

class MuteService : Service() {

    companion object {
        const val ACTION_START_MUTE = "com.example.prayerautomute.START_MUTE"
        const val ACTION_STOP_MUTE = "com.example.prayerautomute.STOP_MUTE"
        const val EXTRA_DURATION_MINUTES = "duration_minutes"
        const val BROADCAST_MUTE_STATUS = "com.example.prayerautomute.MUTE_STATUS"
        const val EXTRA_IS_MUTED = "is_muted"

        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "mute_service_channel"
    }

    private lateinit var audioManager: AudioManager
    private var originalRingerMode: Int = AudioManager.RINGER_MODE_NORMAL
    private var handler: Handler? = null
    private var unmuteRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        handler = Handler(Looper.getMainLooper())

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MUTE -> {
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 1)
                startMuting(durationMinutes)
            }
            ACTION_STOP_MUTE -> {
                stopMuting()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startMuting(durationMinutes: Int) {
        // Save current ringer mode
        originalRingerMode = audioManager.ringerMode

        // Mute the phone
        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT

        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification(true, durationMinutes))

        // Broadcast mute status
        broadcastMuteStatus(true)

        // Schedule unmute
        unmuteRunnable = Runnable {
            stopMuting()
            stopSelf()
        }

        handler?.postDelayed(unmuteRunnable!!, (durationMinutes * 60 * 1000).toLong())
    }

    private fun stopMuting() {
        // Cancel scheduled unmute
        unmuteRunnable?.let { handler?.removeCallbacks(it) }

        // Restore original ringer mode
        audioManager.ringerMode = originalRingerMode

        // Broadcast unmute status
        broadcastMuteStatus(false)

        // Update notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(false, 0))

        // Stop foreground after a short delay to show unmute notification
        Handler(Looper.getMainLooper()).postDelayed({
            stopForeground(true)
        }, 2000)
    }

    private fun broadcastMuteStatus(isMuted: Boolean) {
        val intent = Intent(BROADCAST_MUTE_STATUS)
        intent.putExtra(EXTRA_IS_MUTED, isMuted)
        intent.setPackage(packageName) // Ensure it stays within our app
        sendBroadcast(intent)
    }

    private fun createNotification(isMuted: Boolean, durationMinutes: Int): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isMuted) "Phone Muted for Prayer" else "Phone Unmuted"
        val text = if (isMuted) "Will unmute in $durationMinutes minute(s)" else "Ringer restored"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification) // You'll need to add this icon
            .setContentIntent(pendingIntent)
            .setAutoCancel(!isMuted)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mute Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Controls phone muting for prayer times"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        unmuteRunnable?.let { handler?.removeCallbacks(it) }
        // Ensure phone is unmuted when service is destroyed
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) {
            audioManager.ringerMode = originalRingerMode
        }
    }
}