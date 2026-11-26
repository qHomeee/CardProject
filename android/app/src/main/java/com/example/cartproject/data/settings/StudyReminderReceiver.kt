package com.example.cartproject.data.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.context.GlobalContext

/** Восстанавливает расписание после системных изменений и принимает сигнал будильника. */
class StudyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminders = GlobalContext.get().get<StudyReminderController>()
                withTimeoutOrNull(8_000) {
                    when (action) {
                        REMINDER_ALARM_ACTION -> reminders.deliver(
                            intent.getLongExtra(REMINDER_TRIGGER_EXTRA, -1L),
                        )
                        Intent.ACTION_BOOT_COMPLETED,
                        Intent.ACTION_MY_PACKAGE_REPLACED,
                        Intent.ACTION_TIME_CHANGED,
                        Intent.ACTION_TIMEZONE_CHANGED,
                        -> reminders.reconcileLatest()
                        else -> Unit
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}

internal const val REMINDER_ALARM_ACTION = "com.example.cartproject.STUDY_REMINDER"
internal const val REMINDER_TRIGGER_EXTRA = "trigger"
