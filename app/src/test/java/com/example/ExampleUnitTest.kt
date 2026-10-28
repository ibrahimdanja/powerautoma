package com.example

import com.example.model.ScheduleConfig
import com.example.model.SleepActionType
import com.example.util.PowerScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
    @Test
    fun testDefaultScheduleConfig() {
        val config = ScheduleConfig()
        assertEquals(23, config.sleepHour)
        assertEquals(0, config.sleepMinute)
        assertEquals(7, config.wakeHour)
        assertEquals(0, config.wakeMinute)
        assertTrue(config.isSleepEnabled)
        assertTrue(config.isWakeEnabled)
        assertEquals(7, config.activeDays.size)
        assertEquals(SleepActionType.ECO_DEEP_SLEEP, config.sleepAction)
    }

    @Test
    fun testPowerSchedulerNextOccurrence() {
        val nextSleepMs = PowerScheduler.computeNextOccurrence(
            hour = 23,
            minute = 0,
            activeDays = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
        )
        assertTrue(nextSleepMs > System.currentTimeMillis())

        val formatted = PowerScheduler.formatEpoch(nextSleepMs)
        assertNotNull(formatted)
        assertTrue(formatted.isNotEmpty())
    }

    @Test
    fun testHourMinuteFormatting() {
        val formattedPm = PowerScheduler.formatHourMinute(23, 15)
        assertTrue(formattedPm.contains("11:15") || formattedPm.contains("23:15"))

        val formattedAm = PowerScheduler.formatHourMinute(7, 30)
        assertTrue(formattedAm.contains("7:30") || formattedAm.contains("07:30"))
    }
}
