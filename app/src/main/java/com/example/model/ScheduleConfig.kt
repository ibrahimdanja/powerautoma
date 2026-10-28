package com.example.model

import java.util.Calendar

enum class SleepActionType(val title: String, val description: String) {
    ECO_DEEP_SLEEP(
        title = "Eco Deep Sleep & Lock",
        description = "Locks screen immediately, enables Do Not Disturb, and minimizes battery drain to ~1% overnight."
    ),
    OPEN_POWER_MENU(
        title = "Automated Power Menu",
        description = "Opens Samsung power menu (Shutdown / Restart) so you can turn off the phone with 1 tap."
    ),
    ROOT_SHUTDOWN(
        title = "Direct Root Shutdown",
        description = "Executes hardware shutdown command (requires rooted Samsung Galaxy A10s)."
    )
}

data class ScheduleConfig(
    val sleepHour: Int = 23,
    val sleepMinute: Int = 0,
    val wakeHour: Int = 7,
    val wakeMinute: Int = 0,
    val isSleepEnabled: Boolean = true,
    val isWakeEnabled: Boolean = true,
    val activeDays: Set<Int> = setOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY,
        Calendar.SUNDAY
    ),
    val sleepAction: SleepActionType = SleepActionType.ECO_DEEP_SLEEP,
    val enableDnd: Boolean = true,
    val muteAudio: Boolean = true,
    val enableCountdown: Boolean = true,
    val wakeSound: Boolean = true,
    val wakeVibration: Boolean = true,
    val lastSleepExecutedTime: Long = 0L,
    val lastWakeExecutedTime: Long = 0L
)
