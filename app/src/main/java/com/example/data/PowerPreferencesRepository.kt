package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ScheduleConfig
import com.example.model.SleepActionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

class PowerPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("powerschedule_prefs", Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<ScheduleConfig> = _configFlow.asStateFlow()

    private fun loadConfig(): ScheduleConfig {
        val sleepHour = prefs.getInt(KEY_SLEEP_HOUR, 23)
        val sleepMinute = prefs.getInt(KEY_SLEEP_MINUTE, 0)
        val wakeHour = prefs.getInt(KEY_WAKE_HOUR, 7)
        val wakeMinute = prefs.getInt(KEY_WAKE_MINUTE, 0)
        val isSleepEnabled = prefs.getBoolean(KEY_IS_SLEEP_ENABLED, true)
        val isWakeEnabled = prefs.getBoolean(KEY_IS_WAKE_ENABLED, true)

        val daysString = prefs.getString(KEY_ACTIVE_DAYS, "1,2,3,4,5,6,7") ?: "1,2,3,4,5,6,7"
        val activeDays = daysString.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
            .ifEmpty {
                setOf(
                    Calendar.MONDAY,
                    Calendar.TUESDAY,
                    Calendar.WEDNESDAY,
                    Calendar.THURSDAY,
                    Calendar.FRIDAY,
                    Calendar.SATURDAY,
                    Calendar.SUNDAY
                )
            }

        val actionName = prefs.getString(KEY_SLEEP_ACTION, SleepActionType.ECO_DEEP_SLEEP.name)
        val sleepAction = try {
            SleepActionType.valueOf(actionName ?: SleepActionType.ECO_DEEP_SLEEP.name)
        } catch (_: Exception) {
            SleepActionType.ECO_DEEP_SLEEP
        }

        val enableDnd = prefs.getBoolean(KEY_ENABLE_DND, true)
        val muteAudio = prefs.getBoolean(KEY_MUTE_AUDIO, false)
        val enableCountdown = prefs.getBoolean(KEY_ENABLE_COUNTDOWN, true)
        val wakeSound = prefs.getBoolean(KEY_WAKE_SOUND, true)
        val wakeVibration = prefs.getBoolean(KEY_WAKE_VIBRATION, true)
        val lastSleepTime = prefs.getLong(KEY_LAST_SLEEP_TIME, 0L)
        val lastWakeTime = prefs.getLong(KEY_LAST_WAKE_TIME, 0L)

        return ScheduleConfig(
            sleepHour = sleepHour,
            sleepMinute = sleepMinute,
            wakeHour = wakeHour,
            wakeMinute = wakeMinute,
            isSleepEnabled = isSleepEnabled,
            isWakeEnabled = isWakeEnabled,
            activeDays = activeDays,
            sleepAction = sleepAction,
            enableDnd = enableDnd,
            muteAudio = muteAudio,
            enableCountdown = enableCountdown,
            wakeSound = wakeSound,
            wakeVibration = wakeVibration,
            lastSleepExecutedTime = lastSleepTime,
            lastWakeExecutedTime = lastWakeTime
        )
    }

    fun updateConfig(config: ScheduleConfig) {
        prefs.edit()
            .putInt(KEY_SLEEP_HOUR, config.sleepHour)
            .putInt(KEY_SLEEP_MINUTE, config.sleepMinute)
            .putInt(KEY_WAKE_HOUR, config.wakeHour)
            .putInt(KEY_WAKE_MINUTE, config.wakeMinute)
            .putBoolean(KEY_IS_SLEEP_ENABLED, config.isSleepEnabled)
            .putBoolean(KEY_IS_WAKE_ENABLED, config.isWakeEnabled)
            .putString(KEY_ACTIVE_DAYS, config.activeDays.joinToString(","))
            .putString(KEY_SLEEP_ACTION, config.sleepAction.name)
            .putBoolean(KEY_ENABLE_DND, config.enableDnd)
            .putBoolean(KEY_MUTE_AUDIO, config.muteAudio)
            .putBoolean(KEY_ENABLE_COUNTDOWN, config.enableCountdown)
            .putBoolean(KEY_WAKE_SOUND, config.wakeSound)
            .putBoolean(KEY_WAKE_VIBRATION, config.wakeVibration)
            .putLong(KEY_LAST_SLEEP_TIME, config.lastSleepExecutedTime)
            .putLong(KEY_LAST_WAKE_TIME, config.lastWakeExecutedTime)
            .apply()

        _configFlow.value = config
    }

    fun recordSleepExecution(timestamp: Long = System.currentTimeMillis()) {
        updateConfig(_configFlow.value.copy(lastSleepExecutedTime = timestamp))
    }

    fun recordWakeExecution(timestamp: Long = System.currentTimeMillis()) {
        updateConfig(_configFlow.value.copy(lastWakeExecutedTime = timestamp))
    }

    companion object {
        private const val KEY_SLEEP_HOUR = "key_sleep_hour"
        private const val KEY_SLEEP_MINUTE = "key_sleep_minute"
        private const val KEY_WAKE_HOUR = "key_wake_hour"
        private const val KEY_WAKE_MINUTE = "key_wake_minute"
        private const val KEY_IS_SLEEP_ENABLED = "key_is_sleep_enabled"
        private const val KEY_IS_WAKE_ENABLED = "key_is_wake_enabled"
        private const val KEY_ACTIVE_DAYS = "key_active_days"
        private const val KEY_SLEEP_ACTION = "key_sleep_action"
        private const val KEY_ENABLE_DND = "key_enable_dnd"
        private const val KEY_MUTE_AUDIO = "key_mute_audio"
        private const val KEY_ENABLE_COUNTDOWN = "key_enable_countdown"
        private const val KEY_WAKE_SOUND = "key_wake_sound"
        private const val KEY_WAKE_VIBRATION = "key_wake_vibration"
        private const val KEY_LAST_SLEEP_TIME = "key_last_sleep_time"
        private const val KEY_LAST_WAKE_TIME = "key_last_wake_time"
    }
}
