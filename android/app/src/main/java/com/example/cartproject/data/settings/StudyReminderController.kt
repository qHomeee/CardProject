package com.example.cartproject.data.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.cartproject.MainActivity
import com.example.cartproject.R
import com.example.cartproject.data.local.ZapomniDatabase
import com.example.cartproject.data.local.sqliteOutcome
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.settings.ReminderPreference
import com.example.cartproject.domain.settings.ReminderScheduler
import com.example.cartproject.domain.settings.ReminderStatus
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Планирует один неточный будильник и отправляет напоминание с учётом актуального расписания. */
internal class StudyReminderController(
    private val context: Context,
    private val settings: DataStoreSettingsStore,
    private val database: ZapomniDatabase,
) : ReminderScheduler {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val mutex = Mutex()

    override suspend fun reconcileLatest(): Outcome<ReminderStatus> = reconcile(replace = true)

    /** При возвращении в приложение сохраняет уже поставленный будильник. */
    suspend fun restoreSchedule(): Outcome<ReminderStatus> = reconcile(replace = false)

    private suspend fun reconcile(replace: Boolean): Outcome<ReminderStatus> = mutex.withLock {
        when (val saved = settings.observe().first()) {
            is Outcome.Failure -> saved
            is Outcome.Success -> plan(saved.value.reminder, replace, ZonedDateTime.now())
        }
    }

    /** Перепроверяет время и наличие карточек перед отправкой из фона. */
    suspend fun deliver(triggerMillis: Long) = mutex.withLock {
        val saved = settings.observe().first()
        if (saved !is Outcome.Success) return@withLock
        val now = ZonedDateTime.now()
        val reminder = saved.value.reminder
        val enabled = reminder as? ReminderPreference.On
        val occurrence = enabled?.let { reminderToday(now, it.time).toInstant() }
        val isCurrent = occurrence?.toEpochMilli() == triggerMillis && occurrence <= now.toInstant()
        val status = plan(reminder, replace = isCurrent, now)
        if (isCurrent && status == Outcome.Success(ReminderStatus.Scheduled)) {
            notifyIfDue(now, enabled)
        }
    }

    private fun plan(
        reminder: ReminderPreference,
        replace: Boolean,
        now: ZonedDateTime,
    ): Outcome<ReminderStatus> = try {
        val status = when {
            reminder is ReminderPreference.Off -> {
                cancel()
                ReminderStatus.Disabled
            }
            !notificationsAllowed() -> {
                cancel()
                ReminderStatus.PermissionRequired
            }
            else -> {
                reminder as ReminderPreference.On
                if (replace || existingAlarm() == null) {
                    val next = nextReminder(now, reminder.time)
                    schedule(next, next.toInstant().toEpochMilli())
                }
                ReminderStatus.Scheduled
            }
        }
        Outcome.Success(status)
    } catch (_: SecurityException) {
        Outcome.Failure(AppError.ReminderSchedule)
    } catch (_: IllegalStateException) {
        Outcome.Failure(AppError.ReminderSchedule)
    }

    private fun schedule(at: ZonedDateTime, triggerMillis: Long) {
        val intent = alarmIntent().putExtra(REMINDER_TRIGGER_EXTRA, triggerMillis)
        val pending = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            ALARM_FLAGS or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        try {
            alarms.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                at.toInstant().toEpochMilli(),
                pending,
            )
        } catch (failure: RuntimeException) {
            pending.cancel()
            throw failure
        }
    }

    private fun cancel() {
        existingAlarm()?.let {
            alarms.cancel(it)
            it.cancel()
        }
        notifications.cancel(REMINDER_NOTIFICATION_ID)
    }

    private fun existingAlarm(): PendingIntent? = PendingIntent.getBroadcast(
        context,
        0,
        alarmIntent(),
        ALARM_FLAGS or PendingIntent.FLAG_NO_CREATE,
    )

    private fun alarmIntent() =
        Intent(context, StudyReminderReceiver::class.java).setAction(REMINDER_ALARM_ACTION)

    private suspend fun notifyIfDue(now: ZonedDateTime, reminder: ReminderPreference.On) {
        val due = sqliteOutcome(AppError.LocalRead) {
            Outcome.Success(database.reviews().hasDueCards(now.toInstant().toEpochMilli()))
        }
        if (due != Outcome.Success(true) || !notificationsAllowed()) return
        if (settings.claimReminder(now.toLocalDate(), reminder.time) !=
            Outcome.Success(true)
        ) {
            return
        }
        try {
            postNotification()
        } catch (_: SecurityException) {
            // Разрешение могло измениться между проверкой и отправкой.
        }
    }

    private fun notificationsAllowed(): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < 26) return true
        notifications.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val channel = notifications.getNotificationChannel(CHANNEL_ID)
        return channel != null && channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    /* Разрешение проверено перед записью отметки и может измениться к моменту вызова. */
    @SuppressLint("MissingPermission")
    private fun postNotification() {
        val openApp = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            openApp,
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_study_reminder)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(context.getString(R.string.reminder_notification_body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        notifications.notify(REMINDER_NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "study_reminders"
        const val REMINDER_NOTIFICATION_ID = 1
        const val ALARM_FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
    }
}
