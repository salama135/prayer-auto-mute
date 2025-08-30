package com.example.prayerautomute.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.prayerautomute.data.PrayerTime
import com.example.prayerautomute.receiver.PrayerAlarmReceiver
import java.util.*

/**
 * Utility class for managing AlarmManager alarms for prayer times.
 */
class PrayerAlarmManager(private val context: Context) {

    companion object {
        private const val TAG = "PrayerAlarmManager"
        private const val MUTE_DURATION_MINUTES = 20 // Default mute duration in minutes
        
        // Request codes for each prayer (used to create unique PendingIntents)
        const val REQUEST_CODE_FAJR = 1001
        const val REQUEST_CODE_DHUHR = 1002
        const val REQUEST_CODE_ASR = 1003
        const val REQUEST_CODE_MAGHRIB = 1004
        const val REQUEST_CODE_ISHA = 1005
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedule an alarm for a specific prayer time.
     * 
     * @param prayerTime The prayer time to schedule
     * @param requestCode The request code for the PendingIntent
     * @param muteDuration The duration to mute the phone in minutes
     */
    fun schedulePrayerAlarm(prayerTime: PrayerTime, requestCode: Int, muteDuration: Int = MUTE_DURATION_MINUTES) {
        val calendar = getCalendarForPrayerTime(prayerTime.time)
        
        // If the prayer time has already passed today, schedule for tomorrow
        val now = Calendar.getInstance()
        if (calendar.before(now)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerTime.name)
            putExtra(PrayerAlarmReceiver.EXTRA_MUTE_DURATION, muteDuration)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Schedule the alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
        
        Log.d(TAG, "Scheduled alarm for ${prayerTime.name} at ${prayerTime.time}, " +
                "calendar time: ${calendar.time}, mute duration: $muteDuration minutes")
    }
    
    /**
     * Cancel an alarm for a specific prayer time.
     * 
     * @param requestCode The request code for the PendingIntent
     */
    fun cancelPrayerAlarm(requestCode: Int) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        
        Log.d(TAG, "Cancelled alarm with request code: $requestCode")
    }
    
    /**
     * Schedule alarms for all prayer times.
     * 
     * @param prayerTimes List of prayer times
     * @param muteDuration The duration to mute the phone in minutes
     */
    fun scheduleAllPrayerAlarms(prayerTimes: List<PrayerTime>, muteDuration: Int = MUTE_DURATION_MINUTES) {
        if (prayerTimes.size < 5) {
            Log.e(TAG, "Not enough prayer times provided. Expected 5, got ${prayerTimes.size}")
            return
        }
        
        // Schedule each prayer alarm with its corresponding request code
        schedulePrayerAlarm(prayerTimes[0], REQUEST_CODE_FAJR, muteDuration)
        schedulePrayerAlarm(prayerTimes[1], REQUEST_CODE_DHUHR, muteDuration)
        schedulePrayerAlarm(prayerTimes[2], REQUEST_CODE_ASR, muteDuration)
        schedulePrayerAlarm(prayerTimes[3], REQUEST_CODE_MAGHRIB, muteDuration)
        schedulePrayerAlarm(prayerTimes[4], REQUEST_CODE_ISHA, muteDuration)
        
        Log.d(TAG, "Scheduled alarms for all prayer times")
    }
    
    /**
     * Cancel all prayer alarms.
     */
    fun cancelAllPrayerAlarms() {
        cancelPrayerAlarm(REQUEST_CODE_FAJR)
        cancelPrayerAlarm(REQUEST_CODE_DHUHR)
        cancelPrayerAlarm(REQUEST_CODE_ASR)
        cancelPrayerAlarm(REQUEST_CODE_MAGHRIB)
        cancelPrayerAlarm(REQUEST_CODE_ISHA)
        
        Log.d(TAG, "Cancelled all prayer alarms")
    }
    
    /**
     * Convert a prayer time string (HH:mm) to a Calendar object.
     * 
     * @param timeString The prayer time string in format "HH:mm"
     * @return Calendar object set to the prayer time
     */
    private fun getCalendarForPrayerTime(timeString: String): Calendar {
        val calendar = Calendar.getInstance()
        val timeParts = timeString.split(":")
        
        if (timeParts.size < 2) {
            Log.e(TAG, "Invalid time format: $timeString")
            return calendar
        }
        
        var hour = timeParts[0].toIntOrNull() ?: 0
        var minute = timeParts[1].toIntOrNull() ?: 0
        
        // Handle overflow
        if (minute >= 60) {
            hour += minute / 60
            minute %= 60
        }
        
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        return calendar
    }
}