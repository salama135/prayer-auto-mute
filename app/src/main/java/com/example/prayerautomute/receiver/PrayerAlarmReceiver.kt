package com.example.prayerautomute.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.example.prayerautomute.data.PrayerTime
import com.example.prayerautomute.service.MuteService
import com.example.prayerautomute.util.PrayerAlarmManager
import com.example.prayerautomute.viewmodel.PrayerAutoMuteViewModel

/**
 * BroadcastReceiver that handles AlarmManager triggers for prayer times.
 * When triggered, it starts the MuteService to mute the phone.
 * Also handles device boot to reschedule alarms.
 */
class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PrayerAlarmReceiver"
        const val EXTRA_PRAYER_NAME = "prayer_name"
        const val EXTRA_MUTE_DURATION = "mute_duration"
        const val DEFAULT_MUTE_DURATION = 20 // Default mute duration in minutes
        
        private const val PREFS_NAME = "prayer_alarm_prefs"
        private const val KEY_ALARMS_ENABLED = "alarms_enabled"
        
        // Vendor-specific boot completed actions
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
        private const val ACTION_QUICKBOOT_POWERON_HTC = "com.htc.intent.action.QUICKBOOT_POWERON"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent action: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, 
            ACTION_QUICKBOOT_POWERON,
            ACTION_QUICKBOOT_POWERON_HTC,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // Device rebooted or app updated, reschedule alarms if they were enabled
                handleBootCompleted(context)
            }
            else -> {
                // Normal alarm trigger
                handleAlarmTrigger(context, intent)
            }
        }
    }
    
    private fun handleAlarmTrigger(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
        val muteDuration = intent.getIntExtra(EXTRA_MUTE_DURATION, DEFAULT_MUTE_DURATION)

        Log.d(TAG, "Alarm triggered for $prayerName. Muting for $muteDuration minutes.")

        // Start the MuteService to mute the phone
        val serviceIntent = Intent(context, MuteService::class.java).apply {
            action = MuteService.ACTION_START_MUTE
            putExtra(MuteService.EXTRA_DURATION_MINUTES, muteDuration)
        }

        // Use startForegroundService for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
    
    private fun handleBootCompleted(context: Context) {
        Log.d(TAG, "Device booted, checking if alarms need to be rescheduled")
        
        // Check if alarms were enabled before reboot
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alarmsEnabled = prefs.getBoolean(KEY_ALARMS_ENABLED, false)
        
        if (alarmsEnabled) {
            Log.d(TAG, "Alarms were enabled, rescheduling...")
            
            // Get default prayer times (these would ideally be stored in preferences)
            val prayerTimes = listOf(
                PrayerTime("Fajr", "04:54"),
                PrayerTime("Fajr", "05:54"),
                PrayerTime("Fajr", "04:54"),
                PrayerTime("Fajr", "04:54"),
                PrayerTime("Fajr", "04:54"),
                PrayerTime("Fajr", "04:54"),
                PrayerTime("Fajr", "04:54"),
                PrayerTime("Fajr", "04:54"),
                PrayerTime("Fajr", "04:54"),
                PrayerTime("Dhuhr", "12:59"),
                PrayerTime("Asr", "16:33"),
                PrayerTime("Maghrib", "19:29"),
                PrayerTime("Isha", "20:50")
            )
            
            // Reschedule alarms
            val alarmManager = PrayerAlarmManager(context)
            alarmManager.scheduleAllPrayerAlarms(prayerTimes)
            
            Log.d(TAG, "Alarms rescheduled successfully")
        } else {
            Log.d(TAG, "Alarms were not enabled, nothing to reschedule")
        }
    }
    
    /**
     * Save alarm enabled state to preferences.
     * This should be called when alarms are enabled/disabled.
     */
    fun saveAlarmsEnabledState(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ALARMS_ENABLED, enabled).apply()
        Log.d(TAG, "Saved alarms enabled state: $enabled")
    }
}