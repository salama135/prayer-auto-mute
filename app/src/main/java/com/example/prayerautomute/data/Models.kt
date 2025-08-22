package com.example.prayerautomute.data

data class PrayerTime(
    val name: String,
    val time: String,
    val isNext: Boolean = false
)

data class Location(
    val city: String,
    val country: String
)