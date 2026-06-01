package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.SleepDatabase
import com.example.data.SleepSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SleepReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: StringIntent?) {
        val action = intent?.action ?: return
        val db = SleepDatabase.getDatabase(context)
        val ioScope = CoroutineScope(Dispatchers.IO)

        when (action) {
            ACTION_BEDTIME -> {
                val label = intent.getStringExtra("EXTRA_LABEL") ?: "Sleep Time"
                showNotification(
                    context,
                    NOTIFICATION_CHANNEL_BEDTIME,
                    "Bedtime Reminder",
                    "It's time to head to bed: $label. Press start to log your sleep session.",
                    1001,
                    ACTION_START_NOW
                )
            }
            ACTION_DELAY_START -> {
                val sessionId = intent.getLongExtra("EXTRA_SESSION_ID", -1L)
                if (sessionId != -1L) {
                    ioScope.launch {
                        val session = db.sleepDao().getSessionById(sessionId)
                        if (session != null && session.status == "PENDING") {
                            // Update session status to ACTIVE now that delay has passed
                            val updated = session.copy(
                                status = "ACTIVE",
                                startTime = System.currentTimeMillis(),
                                endTime = System.currentTimeMillis() + (session.durationMinutes * 60 * 1000)
                            )
                            db.sleepDao().insertSession(updated)

                            // Schedule completion alarm for the new endTime
                            val scheduler = AlarmScheduler(context)
                            scheduler.scheduleCompletionAlarm(updated)

                            // Post notification about transition
                            showNotification(
                                context,
                                NOTIFICATION_CHANNEL_TIMER,
                                "Sleep Session Started",
                                "The delay has finished. Your ${session.durationMinutes}-minute sleep session has started.",
                                1002,
                                null
                            )
                        }
                    }
                }
            }
            ACTION_WAKEUP -> {
                val sessionId = intent.getLongExtra("EXTRA_SESSION_ID", -1L)
                if (sessionId != -1L) {
                    ioScope.launch {
                        val session = db.sleepDao().getSessionById(sessionId)
                        if (session != null && (session.status == "ACTIVE" || session.status == "PENDING")) {
                            val updated = session.copy(status = "COMPLETED")
                            db.sleepDao().insertSession(updated)

                            // Trigger alarm notification
                            showWakeUpNotification(context, updated)
                        }
                    }
                }
            }
            ACTION_SNOOZE -> {
                val sessionId = intent.getLongExtra("EXTRA_SESSION_ID", -1L)
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(1003) // Close wake up notification

                if (sessionId != -1L) {
                    ioScope.launch {
                        val session = db.sleepDao().getSessionById(sessionId)
                        if (session != null) {
                            // Snooze for 5 minutes
                            val snoozedSession = SleepSession(
                                startTime = System.currentTimeMillis(),
                                endTime = System.currentTimeMillis() + (5 * 60 * 1000), // 5 mins
                                durationMinutes = 5,
                                delayMinutes = 0,
                                status = "PENDING",
                                timestamp = System.currentTimeMillis()
                            )
                            val newId = db.sleepDao().insertSession(snoozedSession)
                            val scheduler = AlarmScheduler(context)
                            val loaded = snoozedSession.copy(id = newId)
                            scheduler.scheduleCompletionAlarm(loaded)

                            showNotification(
                                context,
                                NOTIFICATION_CHANNEL_TIMER,
                                "Sleep Snoozed",
                                "Sleep timer snoozed for 5 minutes.",
                                1004,
                                null
                            )
                        }
                    }
                }
            }
            ACTION_DISMISS -> {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(1003) // Close wake up notification
            }
        }
    }

    private fun showNotification(
        context: Context,
        channelId: String,
        title: String,
        message: String,
        notificationId: Int,
        actionClick: String?
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels(context, notificationManager)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(true)

        if (actionClick != null) {
            val actionIntent = Intent(context, MainActivity::class.java).apply {
                this.action = actionClick
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val actionPendingIntent = PendingIntent.getActivity(
                context,
                notificationId + 50,
                actionIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(android.R.drawable.ic_media_play, "Start Now", actionPendingIntent)
        }

        notificationManager.notify(notificationId, builder.build())
    }

    private fun showWakeUpNotification(context: Context, session: SleepSession) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels(context, notificationManager)

        // Pass details to trigger custom voice templates or synthetic alarms in WakeUp activity screen
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_WAKEUP_OPEN
            putExtra("EXTRA_SESSION_ID", session.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            999,
            fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Snooze intent
        val snoozeIntent = Intent(context, SleepReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra("EXTRA_SESSION_ID", session.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            998,
            snoozeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Dismiss intent
        val dismissIntent = Intent(context, SleepReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra("EXTRA_SESSION_ID", session.id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            997,
            dismissIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_WAKEUP)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Rise and Shine!")
            .setContentText("Your scheduled ${session.durationMinutes}m sleep is complete. Tap for custom voice reminders and soundscapes.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(contentPendingIntent, true)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze (5m)", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)

        notificationManager.notify(1003, builder.build())
    }

    private fun createNotificationChannels(context: Context, manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val bedtimeChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_BEDTIME,
                "Bedtime Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when scheduled bedtime is reached."
            }

            val timerChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_TIMER,
                "Sleep Timing Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Status of delays or session conversions."
            }

            val wakeupChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_WAKEUP,
                "Wake-Up Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent high-vibration full-screen wakeup alarms."
                enableVibration(true)
                vibrationPattern = longArrayOf(1000, 1000, 1000, 1000, 1000)
            }

            manager.createNotificationChannel(bedtimeChannel)
            manager.createNotificationChannel(timerChannel)
            manager.createNotificationChannel(wakeupChannel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_BEDTIME = "bedtime_reminders"
        const val NOTIFICATION_CHANNEL_TIMER = "sleep_timers"
        const val NOTIFICATION_CHANNEL_WAKEUP = "wakeup_alarms"

        const val ACTION_BEDTIME = "com.sleepreminders.app.ACTION_BEDTIME"
        const val ACTION_DELAY_START = "com.sleepreminders.app.ACTION_DELAY_START"
        const val ACTION_WAKEUP = "com.sleepreminders.app.ACTION_WAKEUP"
        const val ACTION_SNOOZE = "com.sleepreminders.app.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.sleepreminders.app.ACTION_DISMISS"

        // App navigation triggers
        const val ACTION_START_NOW = "com.sleepreminders.app.ACTION_START_NOW"
        const val ACTION_WAKEUP_OPEN = "com.sleepreminders.app.ACTION_WAKEUP_OPEN"
    }
}

// Simple typealias to avoid any compilation errors due to string formatting
typealias StringIntent = Intent
