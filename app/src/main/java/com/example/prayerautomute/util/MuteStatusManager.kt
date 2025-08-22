package com.example.prayerautomute.util

import android.content.Context
import android.content.SharedPreferences

class MuteStatusManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "mute_status_prefs"
        private const val KEY_IS_MUTED = "is_muted"
        private const val KEY_MUTE_END_TIME = "mute_end_time"

        @Volatile
        private var INSTANCE: MuteStatusManager? = null

        fun getInstance(context: Context): MuteStatusManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MuteStatusManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isMuted: Boolean
        get() = sharedPreferences.getBoolean(KEY_IS_MUTED, false)
        set(value) {
            sharedPreferences.edit().putBoolean(KEY_IS_MUTED, value).apply()
        }

    var muteEndTime: Long
        get() = sharedPreferences.getLong(KEY_MUTE_END_TIME, 0)
        set(value) {
            sharedPreferences.edit().putLong(KEY_MUTE_END_TIME, value).apply()
        }

    fun clearMuteStatus() {
        sharedPreferences.edit()
            .remove(KEY_IS_MUTED)
            .remove(KEY_MUTE_END_TIME)
            .apply()
    }
}