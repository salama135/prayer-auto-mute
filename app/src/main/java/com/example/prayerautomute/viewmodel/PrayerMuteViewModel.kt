package com.example.prayerautomute.viewmodel

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.prayerautomute.data.Location
import com.example.prayerautomute.data.PrayerTime
import com.example.prayerautomute.service.MuteService
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class PrayerAutoMuteViewModel : ViewModel() {
    private val _location = mutableStateOf(Location("Cairo", "Egypt"))
    val location: State<Location> = _location

    private val _prayerTimes = mutableStateOf(
        listOf(
            PrayerTime("Fajr", "04:54"),
            PrayerTime("Dhuhr", "12:59"),
            PrayerTime("Asr", "16:33"),
            PrayerTime("Maghrib", "19:29"),
            PrayerTime("Isha", "20:50")
        )
    )
    val prayerTimes: State<List<PrayerTime>> = _prayerTimes

    private val _currentTime = mutableStateOf("")
    val currentTime: State<String> = _currentTime

    private val _nextPrayer = mutableStateOf<PrayerTime?>(null)
    val nextPrayer: State<PrayerTime?> = _nextPrayer

    private val _timeUntilNext = mutableStateOf("")
    val timeUntilNext: State<String> = _timeUntilNext

    private val _isEnabled = mutableStateOf(false)
    val isEnabled: State<Boolean> = _isEnabled

    private val _isMuted = mutableStateOf(false)
    val isMuted: State<Boolean> = _isMuted

    private val _muteEndTime = mutableStateOf<Date?>(null)
    val muteEndTime: State<Date?> = _muteEndTime

    private val isDST = true
    private val dstOffsetMinutes = if (isDST) 60 else 0

    init {
        updateCurrentTime()
        findNextPrayer()
    }

    private fun getCalendarForPrayer(time: String): Calendar {
        val cal = Calendar.getInstance()
        val timeParts = time.split(":")
        var hour = timeParts[0].toInt()
        var minute = timeParts[1].toInt()

        if (minute >= 60) {
            hour += minute / 60
            minute %= 60
        }

        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return cal
    }

    private fun updateCurrentTime() {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        _currentTime.value = sdf.format(Date())

        _nextPrayer.value?.let { next ->
            val now = Calendar.getInstance()
            val prayerCal = getCalendarForPrayer(next.time)

            if (prayerCal.before(now)) {
                prayerCal.add(Calendar.DAY_OF_MONTH, 1)
            }

            val diff = prayerCal.timeInMillis - now.timeInMillis
            val hours = diff / (1000 * 60 * 60)
            val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)

            _timeUntilNext.value = "${hours}h ${minutes}m"
        }
    }

    private fun findNextPrayer() {
        val now = Calendar.getInstance(TimeZone.getDefault())
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        for (prayer in _prayerTimes.value) {
            val prayerCal = getCalendarForPrayer(prayer.time)
            val prayerMinutes = prayerCal.get(Calendar.HOUR_OF_DAY) * 60 +
                    prayerCal.get(Calendar.MINUTE)

            if (prayerMinutes > currentMinutes) {
                _nextPrayer.value = prayer.copy(isNext = true)
                return
            }
        }

        _nextPrayer.value = _prayerTimes.value.first().copy(isNext = true)
    }

    fun toggleEnabled() {
        _isEnabled.value = !_isEnabled.value
    }

    fun updateLocation(city: String, country: String) {
        _location.value = Location(city, country)
    }

    fun updatePrayerTime(index: Int, time: String) {
        val updatedList = _prayerTimes.value.toMutableList()
        updatedList[index] = updatedList[index].copy(time = time)
        _prayerTimes.value = updatedList
        findNextPrayer()
    }

    // Updated method to use background service
    fun testMutePhone(context: Context) {
        _isMuted.value = true
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, 1)
        _muteEndTime.value = calendar.time

        // Start the background service
        val intent = Intent(context, MuteService::class.java)
        intent.action = MuteService.ACTION_START_MUTE
        intent.putExtra(MuteService.EXTRA_DURATION_MINUTES, 5)
        context.startForegroundService(intent)
    }

    fun unmutePhone(context: Context) {
        _isMuted.value = false
        _muteEndTime.value = null

        // Stop the background service
        val intent = Intent(context, MuteService::class.java)
        intent.action = MuteService.ACTION_STOP_MUTE
        context.startService(intent)
    }

    // Method to update UI when service reports status changes
    fun updateMuteStatus(isMuted: Boolean) {
        _isMuted.value = isMuted
        if (!isMuted) {
            _muteEndTime.value = null
        }
    }

    suspend fun startTimeUpdates() {
        while (true) {
            updateCurrentTime()
            delay(1000)
        }
    }
}