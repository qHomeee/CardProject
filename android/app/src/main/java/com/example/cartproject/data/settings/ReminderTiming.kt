package com.example.cartproject.data.settings

import java.time.LocalTime
import java.time.ZonedDateTime

/** Возвращает ближайшее будущее срабатывание по местным часам. */
internal fun nextReminder(now: ZonedDateTime, time: LocalTime): ZonedDateTime {
    val today = reminderToday(now, time)
    return if (today.isAfter(
            now,
        )
    ) {
        today
    } else {
        now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
    }
}

/** Для пропущенного часа при переводе часов вперёд используется следующее действительное время. */
internal fun reminderToday(now: ZonedDateTime, time: LocalTime): ZonedDateTime =
    now.toLocalDate().atTime(time).atZone(now.zone)
