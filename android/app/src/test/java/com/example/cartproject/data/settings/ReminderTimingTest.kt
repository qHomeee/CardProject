package com.example.cartproject.data.settings

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTimingTest {
    private val time = LocalTime.of(20, 0)

    @Test
    fun choosesTodayBeforeSelectedTimeAndTomorrowAfterIt() {
        val today = ZonedDateTime.parse("2026-11-26T19:59:59+03:00[Europe/Moscow]")
        assertEquals(today.toLocalDate(), nextReminder(today, time).toLocalDate())
        assertEquals(
            today.toLocalDate().plusDays(1),
            nextReminder(today.plusSeconds(1), time).toLocalDate(),
        )
        assertEquals(time, nextReminder(today.plusHours(2), time).toLocalTime())
    }

    @Test
    fun keepsLocalTimeAcrossAChangeOfOffset() {
        val berlin = ZoneId.of("Europe/Berlin")
        val saturday = LocalDate.of(2026, 3, 28)
        val now = saturday.atTime(21, 0).atZone(berlin)

        assertEquals(saturday.plusDays(1).atTime(time).atZone(berlin), nextReminder(now, time))
    }

    @Test
    fun resolvesADaylightSavingGapAndChoosesOneFallOccurrence() {
        val berlin = ZoneId.of("Europe/Berlin")
        val gap = LocalTime.of(2, 30)
        val spring = LocalDate.of(2026, 3, 29).atStartOfDay(berlin)
        val fall = LocalDate.of(2026, 10, 25).atStartOfDay(berlin)
        assertEquals(LocalTime.of(3, 30), nextReminder(spring, gap).toLocalTime())
        val first = nextReminder(fall, gap)
        assertEquals(
            fall.toLocalDate().plusDays(1),
            nextReminder(first.plusMinutes(5), gap).toLocalDate(),
        )
    }
}
