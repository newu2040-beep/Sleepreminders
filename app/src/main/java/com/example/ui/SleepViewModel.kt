package com.example.ui

import android.app.Application
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ui.theme.AppThemePreset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class SleepViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SleepDatabase.getDatabase(application)
    private val repository = SleepRepository(db.sleepDao())
    private val scheduler = AlarmScheduler(application)

    // UI state theme
    private val prefs = application.getSharedPreferences("sleep_reminders_prefs", Context.MODE_PRIVATE)
    private val _themePreset = MutableStateFlow(
        AppThemePreset.valueOf(prefs.getString("theme", AppThemePreset.GEOMETRIC_BALANCE.name) ?: AppThemePreset.GEOMETRIC_BALANCE.name)
    )
    val themePreset: StateFlow<AppThemePreset> = _themePreset.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Alarm sounding state
    private val _isAlarmSounding = MutableStateFlow(false)
    val isAlarmSounding: StateFlow<Boolean> = _isAlarmSounding.asStateFlow()

    private val _ringingSession = MutableStateFlow<SleepSession?>(null)
    val ringingSession: StateFlow<SleepSession?> = _ringingSession.asStateFlow()

    // Active audio parameters
    private val _selectedSoundId = MutableStateFlow(prefs.getString("sound_id", "sound_soft_alarm") ?: "sound_soft_alarm")
    val selectedSoundId: StateFlow<String> = _selectedSoundId.asStateFlow()

    private val _customTtsText = MutableStateFlow(prefs.getString("tts_text", "") ?: "")
    val customTtsText: StateFlow<String> = _customTtsText.asStateFlow()

    private val _customAudioUri = MutableStateFlow<String?>(prefs.getString("custom_audio_uri", null))
    val customAudioUri: StateFlow<String?> = _customAudioUri.asStateFlow()

    // User profile parameters
    private val _profileName = MutableStateFlow(prefs.getString("profile_name", "Active Rest Optimizer") ?: "Active Rest Optimizer")
    val profileName: StateFlow<String> = _profileName.asStateFlow()

    private val _profileAge = MutableStateFlow(prefs.getString("profile_age", "28") ?: "28")
    val profileAge: StateFlow<String> = _profileAge.asStateFlow()

    private val _profileAddress = MutableStateFlow(prefs.getString("profile_address", "128 Sleepy Hollow Rd, Dreamland") ?: "128 Sleepy Hollow Rd, Dreamland")
    val profileAddress: StateFlow<String> = _profileAddress.asStateFlow()

    private val _profilePhotoUri = MutableStateFlow<String?>(prefs.getString("profile_photo_uri", null))
    val profilePhotoUri: StateFlow<String?> = _profilePhotoUri.asStateFlow()

    private val _profileGender = MutableStateFlow(prefs.getString("profile_gender", "Unspecified") ?: "Unspecified")
    val profileGender: StateFlow<String> = _profileGender.asStateFlow()

    private val _profileChronotype = MutableStateFlow(prefs.getString("profile_chronotype", "Night Owl") ?: "Night Owl")
    val profileChronotype: StateFlow<String> = _profileChronotype.asStateFlow()

    // Custom audio trimming parameters
    private val _customAudioStartMs = MutableStateFlow(prefs.getLong("custom_audio_start_ms", 0L))
    val customAudioStartMs: StateFlow<Long> = _customAudioStartMs.asStateFlow()

    private val _customAudioEndMs = MutableStateFlow(prefs.getLong("custom_audio_end_ms", 30000L))
    val customAudioEndMs: StateFlow<Long> = _customAudioEndMs.asStateFlow()

    private val _customAudioDurationMs = MutableStateFlow(prefs.getLong("custom_audio_duration_ms", 30000L))
    val customAudioDurationMs: StateFlow<Long> = _customAudioDurationMs.asStateFlow()

    // Pre-made ambient nature sounds state
    private val _ambientDurationMinutes = MutableStateFlow(prefs.getInt("ambient_duration_minutes", 15))
    val ambientDurationMinutes: StateFlow<Int> = _ambientDurationMinutes.asStateFlow()

    val playingAmbientId: StateFlow<String?> = AudioAlarmManager.playingAmbientId.asStateFlow()
    val ambientTimeRemainingSec: StateFlow<Int> = AudioAlarmManager.ambientTimeRemainingSec.asStateFlow()

    val customWindVol: StateFlow<Float> = AudioAlarmManager.customWindVol.asStateFlow()
    val customRainVol: StateFlow<Float> = AudioAlarmManager.customRainVol.asStateFlow()
    val customStreamVol: StateFlow<Float> = AudioAlarmManager.customStreamVol.asStateFlow()
    val customCampfireVol: StateFlow<Float> = AudioAlarmManager.customCampfireVol.asStateFlow()
    val customChimesVol: StateFlow<Float> = AudioAlarmManager.customChimesVol.asStateFlow()

    private val _playingPremadePreviewId = MutableStateFlow<String?>(null)
    val playingPremadePreviewId: StateFlow<String?> = _playingPremadePreviewId.asStateFlow()

    private val audioManager = AudioAlarmManager(application)

    // Local data flows
    val allSessions: StateFlow<List<SleepSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSchedules: StateFlow<List<SleepSchedule>> = repository.allSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSession: StateFlow<SleepSession?> = repository.activeSessionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Dynamic timer countdown flow
    private val _countdownSecondsLeft = MutableStateFlow(0L)
    val countdownSecondsLeft: StateFlow<Long> = _countdownSecondsLeft.asStateFlow()

    private val _isDelayCountdown = MutableStateFlow(false)
    val isDelayCountdown: StateFlow<Boolean> = _isDelayCountdown.asStateFlow()

    private var timerJob: Job? = null

    init {
        // Collect active session to manage the system countdown ticks accurately
        viewModelScope.launch {
            activeSession.collect { session ->
                timerJob?.cancel()
                if (session != null) {
                    startVolumeTicker(session)
                } else {
                    _countdownSecondsLeft.value = 0L
                    _isDelayCountdown.value = false
                }
            }
        }
    }

    fun selectTheme(preset: AppThemePreset) {
        _themePreset.value = preset
        prefs.edit().putString("theme", preset.name).apply()
    }

    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        prefs.edit().putBoolean("dark_theme", isDark).apply()
    }

    fun selectSound(soundId: String) {
        _selectedSoundId.value = soundId
        prefs.edit().putString("sound_id", soundId).apply()
    }

    fun updateCustomTts(text: String) {
        _customTtsText.value = text
        prefs.edit().putString("tts_text", text).apply()
    }

    fun setCustomAudioUri(uriString: String?) {
        _customAudioUri.value = uriString
        prefs.edit().putString("custom_audio_uri", uriString).apply()

        if (uriString != null) {
            val duration = try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(getApplication(), Uri.parse(uriString))
                val timeStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                retriever.release()
                timeStr?.toLong() ?: 30000L
            } catch (e: Exception) {
                30000L
            }
            _customAudioDurationMs.value = duration
            _customAudioStartMs.value = 0L
            _customAudioEndMs.value = if (duration > 30000L) 30000L else duration

            prefs.edit().apply {
                putLong("custom_audio_duration_ms", duration)
                putLong("custom_audio_start_ms", 0L)
                putLong("custom_audio_end_ms", if (duration > 30000L) 30000L else duration)
            }.apply()
        } else {
            _customAudioDurationMs.value = 30000L
            _customAudioStartMs.value = 0L
            _customAudioEndMs.value = 30000L
            prefs.edit().apply {
                putLong("custom_audio_duration_ms", 30000L)
                putLong("custom_audio_start_ms", 0L)
                putLong("custom_audio_end_ms", 30000L)
            }.apply()
        }
    }

    fun updateCustomAudioTrim(startMs: Long, endMs: Long) {
        _customAudioStartMs.value = startMs
        _customAudioEndMs.value = endMs
        prefs.edit().apply {
            putLong("custom_audio_start_ms", startMs)
            putLong("custom_audio_end_ms", endMs)
        }.apply()
    }

    fun playPreview(uriString: String, startMs: Long, endMs: Long) {
        audioManager.playPreview(uriString, startMs, endMs)
    }

    fun stopPreview() {
        audioManager.stopPreview()
    }

    fun updateAmbientDuration(minutes: Int) {
        _ambientDurationMinutes.value = minutes
        prefs.edit().putInt("ambient_duration_minutes", minutes).apply()
    }

    fun playAmbientSound(soundId: String) {
        audioManager.playAmbient(soundId, _ambientDurationMinutes.value)
    }

    fun stopAmbientSound() {
        AudioAlarmManager.stopActiveAmbient()
    }

    fun playPremadePreview(soundId: String) {
        stopPremadePreview()
        _playingPremadePreviewId.value = soundId
        if (soundId == "custom_audio") {
            _customAudioUri.value?.let { uri ->
                audioManager.playPreview(uri, _customAudioStartMs.value, _customAudioEndMs.value)
            }
        } else {
            audioManager.playAlert(soundId)
        }
    }

    fun stopPremadePreview() {
        _playingPremadePreviewId.value = null
        audioManager.stopAll()
    }

    fun updateCustomWindVol(vol: Float) {
        AudioAlarmManager.customWindVol.value = vol
    }
    fun updateCustomRainVol(vol: Float) {
        AudioAlarmManager.customRainVol.value = vol
    }
    fun updateCustomStreamVol(vol: Float) {
        AudioAlarmManager.customStreamVol.value = vol
    }
    fun updateCustomCampfireVol(vol: Float) {
        AudioAlarmManager.customCampfireVol.value = vol
    }
    fun updateCustomChimesVol(vol: Float) {
        AudioAlarmManager.customChimesVol.value = vol
    }

    fun updateProfile(name: String, age: String, address: String, photoUri: String?, gender: String, chronotype: String) {
        _profileName.value = name
        _profileAge.value = age
        _profileAddress.value = address
        _profilePhotoUri.value = photoUri
        _profileGender.value = gender
        _profileChronotype.value = chronotype

        prefs.edit().apply {
            putString("profile_name", name)
            putString("profile_age", age)
            putString("profile_address", address)
            putString("profile_photo_uri", photoUri)
            putString("profile_gender", gender)
            putString("profile_chronotype", chronotype)
        }.apply()
    }

    // Timer scheduler logic
    fun startSleepSession(durationMinutes: Int, delayMinutes: Int) {
        viewModelScope.launch {
            // Cancel current if busy
            activeSession.value?.let { scheduler.cancelSessionAlarms(it) }

            val now = System.currentTimeMillis()
            val delayMillis = delayMinutes * 60 * 1000L

            val status: String
            val startTime: Long
            val endTime: Long

            if (delayMinutes > 0) {
                status = "PENDING"
                startTime = now + delayMillis
                endTime = startTime + (durationMinutes * 60 * 1000L)
            } else {
                status = "ACTIVE"
                startTime = now
                endTime = now + (durationMinutes * 60 * 1000L)
            }

            val session = SleepSession(
                startTime = startTime,
                endTime = endTime,
                durationMinutes = durationMinutes,
                delayMinutes = delayMinutes,
                status = status,
                timestamp = now
            )

            val id = repository.insertSession(session)
            val updatedConfig = session.copy(id = id)

            if (delayMinutes > 0) {
                scheduler.scheduleDelayStartAlarm(updatedConfig)
            } else {
                scheduler.scheduleCompletionAlarm(updatedConfig)
            }
        }
    }

    fun cancelActiveSession() {
        viewModelScope.launch {
            activeSession.value?.let { session ->
                scheduler.cancelSessionAlarms(session)
                val cancelled = session.copy(status = "CANCELLED")
                repository.updateSession(cancelled)
            }
        }
    }

    private var pausedSecondsCache: Long = 0L

    fun pauseActiveSession() {
        viewModelScope.launch {
            activeSession.value?.let { session ->
                if (session.status == "ACTIVE" || session.status == "PENDING") {
                    scheduler.cancelSessionAlarms(session)
                    pausedSecondsCache = _countdownSecondsLeft.value
                    val pausedSession = session.copy(status = "PAUSED")
                    repository.updateSession(pausedSession)
                }
            }
        }
    }

    fun resumeActiveSession() {
        viewModelScope.launch {
            activeSession.value?.let { session ->
                if (session.status == "PAUSED") {
                    val now = System.currentTimeMillis()
                    val newEndTime = now + (pausedSecondsCache * 1000)
                    
                    val resumedSession = session.copy(
                        status = "ACTIVE",
                        startTime = now,
                        endTime = newEndTime
                    )
                    repository.updateSession(resumedSession)
                    scheduler.scheduleCompletionAlarm(resumedSession)
                }
            }
        }
    }

    private fun startVolumeTicker(session: SleepSession) {
        timerJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                if (session.status == "PENDING") {
                    _isDelayCountdown.value = true
                    val diff = session.startTime - now
                    if (diff > 0) {
                        _countdownSecondsLeft.value = diff / 1000
                    } else {
                        _countdownSecondsLeft.value = 0L
                    }
                } else if (session.status == "ACTIVE") {
                    _isDelayCountdown.value = false
                    val diff = session.endTime - now
                    if (diff > 0) {
                        _countdownSecondsLeft.value = diff / 1000
                    } else {
                        _countdownSecondsLeft.value = 0L
                    }
                } else if (session.status == "PAUSED") {
                    _isDelayCountdown.value = false
                    _countdownSecondsLeft.value = pausedSecondsCache
                } else {
                    _countdownSecondsLeft.value = 0L
                    break
                }
                delay(1000)
            }
        }
    }

    // Alarm management triggers
    fun handleIncomingAlarm(sessionId: Long) {
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId)
            if (session != null) {
                _ringingSession.value = session
                _isAlarmSounding.value = true
                audioManager.playAlert(
                    soundId = _selectedSoundId.value,
                    customUriString = _customAudioUri.value,
                    voiceText = if (_customTtsText.value.isNotBlank()) _customTtsText.value else null,
                    customStartMs = _customAudioStartMs.value,
                    customEndMs = _customAudioEndMs.value
                )
            }
        }
    }

    fun stopAlarmSound() {
        _isAlarmSounding.value = false
        _ringingSession.value = null
        audioManager.stopAll()
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }

    // Schedules CRUD
    fun addSchedule(label: String, hour: Int, minute: Int, days: List<String>) {
        viewModelScope.launch {
            val schedule = SleepSchedule(
                label = label,
                hour = hour,
                minute = minute,
                daysOfWeek = days.joinToString(","),
                active = true
            )
            val id = repository.insertSchedule(schedule)
            scheduler.scheduleBedtimeAlarm(schedule.copy(id = id))
        }
    }

    fun toggleSchedule(schedule: SleepSchedule) {
        viewModelScope.launch {
            val updated = schedule.copy(active = !schedule.active)
            repository.updateSchedule(updated)
            if (updated.active) {
                scheduler.scheduleBedtimeAlarm(updated)
            } else {
                scheduler.cancelBedtimeAlarm(updated)
            }
        }
    }

    fun deleteSchedule(schedule: SleepSchedule) {
        viewModelScope.launch {
            scheduler.cancelBedtimeAlarm(schedule)
            repository.deleteScheduleById(schedule.id)
        }
    }

    // Historical analytics & Smart Suggestions
    fun getSleepStats(): SleepStats {
        val completed = allSessions.value.filter { it.status == "COMPLETED" }
        val totalSessions = completed.size

        val totalMinutes = completed.sumOf { it.durationMinutes }
        val averageMinutes = if (totalSessions > 0) totalMinutes / totalSessions else 0

        // Strict calculation of Sleep Streak based on consecutive active history
        var streak = 0
        if (completed.isNotEmpty()) {
            val todayDayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val todayYear = Calendar.getInstance().get(Calendar.YEAR)

            val dayKeys = completed.map { s ->
                val cal = Calendar.getInstance().apply { timeInMillis = s.timestamp }
                "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
            }.distinct()

            var checkCal = Calendar.getInstance()
            while (true) {
                val key = "${checkCal.get(Calendar.YEAR)}-${checkCal.get(Calendar.DAY_OF_YEAR)}"
                if (dayKeys.contains(key)) {
                    streak++
                    checkCal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        }

        // Sleep Debt Tracker: assumes healthy sleeping goal of 8 hours (480 minutes) per day
        val totalDaysLogged = allSessions.value.map { s ->
            val cal = Calendar.getInstance().apply { timeInMillis = s.timestamp }
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        }.distinct().size

        val expectedSleepMin = totalDaysLogged * 480
        val actualSleepMin = completed.sumOf { it.durationMinutes }
        val sleepDebtHours = if (expectedSleepMin > actualSleepMin) {
            (expectedSleepMin - actualSleepMin) / 60.0
        } else {
            0.0
        }

        // Consistency score: percentages of sleep completed versus total attempted
        val totalAttempted = allSessions.value.size
        val consistencyScore = if (totalAttempted > 0) {
            (completed.size.toDouble() / totalAttempted.toDouble() * 100).toInt()
        } else {
            100
        }

        // Prepping weekly chart segments
        val weeklyOverview = mutableMapOf<Int, Int>() // CalendarDayOfWeek (1 to 7) to Minutes slept
        val cal = Calendar.getInstance()
        for (i in 1..7) {
            weeklyOverview[i] = 0
        }
        for (s in completed) {
            val sCal = Calendar.getInstance().apply { timeInMillis = s.timestamp }
            val weekDiffSinceNow = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                add(Calendar.DAY_OF_YEAR, -7)
            }
            if (sCal.after(weekDiffSinceNow)) {
                val dayOfWeek = sCal.get(Calendar.DAY_OF_WEEK)
                val currentMins = weeklyOverview[dayOfWeek] ?: 0
                weeklyOverview[dayOfWeek] = currentMins + s.durationMinutes
            }
        }

        return SleepStats(
            totalSessions = totalSessions,
            totalHoursSlept = totalMinutes / 60.0,
            averageDurationMinutes = averageMinutes,
            streak = streak,
            sleepDebtHours = sleepDebtHours,
            consistencyScore = consistencyScore,
            weeklyMinutes = weeklyOverview.values.toList()
        )
    }

    // Local data backup & restore helpers
    fun backupData(): String {
        return repository.exportHistoryToJson(allSessions.value)
    }

    fun restoreData(json: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val worked = repository.importHistoryFromJson(json)
            onComplete(worked)
        }
    }

    fun wipeDatabase() {
        viewModelScope.launch {
            repository.clearAllSessions()
        }
    }

    fun deleteSessionById(id: Long) {
        viewModelScope.launch {
            repository.deleteSessionById(id)
        }
    }
}

data class SleepStats(
    val totalSessions: Int,
    val totalHoursSlept: Double,
    val averageDurationMinutes: Int,
    val streak: Int,
    val sleepDebtHours: Double,
    val consistencyScore: Int,
    val weeklyMinutes: List<Int>
)
