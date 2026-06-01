package com.example.ui

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.SleepSchedule
import com.example.data.SleepSession
import java.util.Calendar

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleBedtimeAlarm(schedule: SleepSchedule) {
        val intent = Intent(context, SleepReceiver::class.java).apply {
            action = SleepReceiver.ACTION_BEDTIME
            putExtra("EXTRA_LABEL", schedule.label)
            putExtra("EXTRA_SCHEDULE_ID", schedule.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.toInt() + 10000,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, schedule.hour)
            set(Calendar.MINUTE, schedule.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
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
        } catch (e: Exception) {
            // Log or fallback to set if policy restricted
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    fun cancelBedtimeAlarm(schedule: SleepSchedule) {
        val intent = Intent(context, SleepReceiver::class.java).apply {
            action = SleepReceiver.ACTION_BEDTIME
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.toInt() + 10000,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleDelayStartAlarm(session: SleepSession) {
        val intent = Intent(context, SleepReceiver::class.java).apply {
            action = SleepReceiver.ACTION_DELAY_START
            putExtra("EXTRA_SESSION_ID", session.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            session.id.toInt() + 20000,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val triggerAt = System.currentTimeMillis() + (session.delayMinutes * 60 * 1000)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleCompletionAlarm(session: SleepSession) {
        val intent = Intent(context, SleepReceiver::class.java).apply {
            action = SleepReceiver.ACTION_WAKEUP
            putExtra("EXTRA_SESSION_ID", session.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            session.id.toInt() + 30000,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val triggerAt = session.endTime

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancelSessionAlarms(session: SleepSession) {
        val delayIntent = Intent(context, SleepReceiver::class.java).apply {
            action = SleepReceiver.ACTION_DELAY_START
        }
        val delayPI = PendingIntent.getBroadcast(
            context,
            session.id.toInt() + 20000,
            delayIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (delayPI != null) {
            alarmManager.cancel(delayPI)
        }

        val wakeupIntent = Intent(context, SleepReceiver::class.java).apply {
            action = SleepReceiver.ACTION_WAKEUP
        }
        val wakeupPI = PendingIntent.getBroadcast(
            context,
            session.id.toInt() + 30000,
            wakeupIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (wakeupPI != null) {
            alarmManager.cancel(wakeupPI)
        }
    }
}
