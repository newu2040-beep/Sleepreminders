package com.example.data

import android.content.Context
import com.example.data.SleepDao
import com.example.data.SleepSession
import com.example.data.SleepSchedule
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class SleepRepository(private val sleepDao: SleepDao) {

    val allSessions: Flow<List<SleepSession>> = sleepDao.getAllSessions()
    val allSchedules: Flow<List<SleepSchedule>> = sleepDao.getAllSchedules()
    val activeSessionFlow: Flow<SleepSession?> = sleepDao.getActiveSessionFlow()

    suspend fun insertSession(session: SleepSession): Long = sleepDao.insertSession(session)

    suspend fun updateSession(session: SleepSession) = sleepDao.updateSession(session)

    suspend fun getActiveSession(): SleepSession? = sleepDao.getActiveSession()

    suspend fun getSessionById(id: Long): SleepSession? = sleepDao.getSessionById(id)

    suspend fun deleteSessionById(id: Long) = sleepDao.deleteSessionById(id)

    suspend fun clearAllSessions() = sleepDao.clearAllSessions()

    // Schedules
    suspend fun insertSchedule(schedule: SleepSchedule) = sleepDao.insertSchedule(schedule)

    suspend fun updateSchedule(schedule: SleepSchedule) = sleepDao.updateSchedule(schedule)

    suspend fun deleteScheduleById(id: Long) = sleepDao.deleteScheduleById(id)

    // Export history
    fun exportHistoryToJson(sessions: List<SleepSession>): String {
        val array = JSONArray()
        for (session in sessions) {
            val obj = JSONObject().apply {
                put("id", session.id)
                put("startTime", session.startTime)
                put("endTime", session.endTime)
                put("durationMinutes", session.durationMinutes)
                put("delayMinutes", session.delayMinutes)
                put("status", session.status)
                put("timestamp", session.timestamp)
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    // Import history
    suspend fun importHistoryFromJson(jsonStr: String): Boolean {
        return try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val session = SleepSession(
                    startTime = obj.getLong("startTime"),
                    endTime = obj.getLong("endTime"),
                    durationMinutes = obj.getInt("durationMinutes"),
                    delayMinutes = obj.getInt("delayMinutes"),
                    status = obj.getString("status"),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
                sleepDao.insertSession(session)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
