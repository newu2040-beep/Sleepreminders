package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_sessions")
data class SleepSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long, // Epoch millis
    val endTime: Long,   // Epoch millis
    val durationMinutes: Int,
    val delayMinutes: Int,
    val status: String,   // PENDING, ACTIVE, COMPLETED, CANCELLED, SNOOZED
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sleep_schedules")
data class SleepSchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: String, // e.g. "Mon,Tue,Wed,Thu,Fri,Sat,Sun"
    val active: Boolean = true
)
