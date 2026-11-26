package com.example.cartproject.data.settings

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cartproject.R
import com.example.cartproject.data.local.RoomLocalDeckStore
import com.example.cartproject.data.local.ZapomniDatabase
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.SaveOwnCard
import com.example.cartproject.domain.decks.SaveOwnDeck
import com.example.cartproject.domain.settings.ReminderPreference
import com.example.cartproject.domain.settings.ReminderStatus
import java.io.File
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudyReminderControllerTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    /** Проверяет реальное уведомление Android, фильтр Room и отмену будильника. */
    @Test
    fun postsOnlyForDueCardsAndCancelsWhenDisabled() = runBlocking {
        if (Build.VERSION.SDK_INT >= 33) {
            instrumentation.uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
        val notifications = context.getSystemService(NotificationManager::class.java)
        val job = SupervisorJob()
        val file = File(context.cacheDir, "reminder-${UUID.randomUUID()}.preferences_pb")
        val preferences = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + job),
        ) {
            file
        }
        val settings = DataStoreSettingsStore(preferences)
        val database = Room.inMemoryDatabaseBuilder(context, ZapomniDatabase::class.java).build()
        val controller = StudyReminderController(context, settings, database)
        val time = LocalTime.MIDNIGHT
        try {
            val now = ZonedDateTime.now()
            val occurrence = reminderToday(now, time).toInstant().toEpochMilli()
            val deckStore = RoomLocalDeckStore(database)
            val deck = (SaveOwnDeck(deckStore)(null, "Напоминание", "") as Outcome.Success).value
            val future = now.toInstant().plusSeconds(3600)
            val ready = now.toInstant().minusSeconds(60)
            check(
                SaveOwnCard(deckStore) {
                    future
                }(deck, null, "Позже", "Ответ") is Outcome.Success,
            )

            settings.saveReminder(ReminderPreference.On(time))
            assertEquals(Outcome.Success(ReminderStatus.Scheduled), controller.reconcileLatest())
            assertNotNull(pendingAlarm())
            controller.deliver(occurrence)
            assertNull(notification(notifications))

            check(
                SaveOwnCard(deckStore) {
                    ready
                }(deck, null, "Сейчас", "Ответ") is Outcome.Success,
            )
            controller.deliver(occurrence - 60_000)
            assertNull(notification(notifications))
            controller.deliver(occurrence)
            withTimeout(3_000) {
                while (notification(notifications) == null) delay(50)
            }

            assertEquals(Outcome.Success(false), settings.claimReminder(now.toLocalDate(), time))

            settings.saveReminder(ReminderPreference.Off(time))
            assertEquals(Outcome.Success(ReminderStatus.Disabled), controller.reconcileLatest())
            assertNull(pendingAlarm())
            withTimeout(3_000) {
                while (notification(notifications) != null) delay(50)
            }
        } finally {
            settings.saveReminder(ReminderPreference.Off())
            controller.reconcileLatest()
            job.cancelAndJoin()
            database.close()
            file.delete()
        }
        Unit
    }

    private fun pendingAlarm(): PendingIntent? = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, StudyReminderReceiver::class.java).setAction(REMINDER_ALARM_ACTION),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notification(notifications: NotificationManager) =
        notifications.activeNotifications.firstOrNull {
            val title = it.notification.extras.getCharSequence(Notification.EXTRA_TITLE)
            title == context.getString(R.string.reminder_notification_title)
        }
}
