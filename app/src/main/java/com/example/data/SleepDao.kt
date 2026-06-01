package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {
    // Sessions
    @Query("SELECT * FROM sleep_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<SleepSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SleepSession): Long

    @Update
    suspend fun updateSession(session: SleepSession)

    @Query("SELECT * FROM sleep_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): SleepSession?

    @Query("SELECT * FROM sleep_sessions WHERE status = 'ACTIVE' OR status = 'PENDING' OR status = 'PAUSED' LIMIT 1")
    fun getActiveSessionFlow(): Flow<SleepSession?>

    @Query("SELECT * FROM sleep_sessions WHERE status = 'ACTIVE' OR status = 'PENDING' OR status = 'PAUSED' LIMIT 1")
    suspend fun getActiveSession(): SleepSession?

    @Query("DELETE FROM sleep_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("DELETE FROM sleep_sessions")
    suspend fun clearAllSessions()

    // Schedules
    @Query("SELECT * FROM sleep_schedules ORDER BY hour ASC, minute ASC")
    fun getAllSchedules(): Flow<List<SleepSchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: SleepSchedule): Long

    @Update
    suspend fun updateSchedule(schedule: SleepSchedule)

    @Query("DELETE FROM sleep_schedules WHERE id = :id")
    suspend fun deleteScheduleById(id: Long)
}
