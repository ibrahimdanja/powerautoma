package com.example.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PowerPreferencesRepository
import com.example.model.ScheduleConfig
import com.example.model.SleepActionType
import com.example.receiver.PowerDeviceAdminReceiver
import com.example.service.PowerAccessibilityService
import com.example.util.BatteryInfo
import com.example.util.BatteryTelemetry
import com.example.util.NotificationHelper
import com.example.util.PowerScheduler
import com.example.util.RootUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PermissionStatus(
    val hasAccessibility: Boolean = false,
    val hasDeviceAdmin: Boolean = false,
    val hasDnd: Boolean = false,
    val hasExactAlarm: Boolean = true,
    val isRootAvailable: Boolean = false
)

data class MainUiState(
    val config: ScheduleConfig = ScheduleConfig(),
    val batteryInfo: BatteryInfo = BatteryInfo(),
    val permissions: PermissionStatus = PermissionStatus(),
    val nextSleepFormatted: String = "",
    val nextSleepUntil: String = "",
    val nextWakeFormatted: String = "",
    val nextWakeUntil: String = "",
    val isTestScheduled: Boolean = false,
    val testMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val repository = PowerPreferencesRepository(context)
    private val batteryTelemetry = BatteryTelemetry(context)

    private val _permissionsFlow = MutableStateFlow(checkPermissions())
    private val _testMessageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MainUiState> = combine(
        repository.configFlow,
        batteryTelemetry.batteryInfoFlow,
        _permissionsFlow,
        _testMessageFlow
    ) { config, battery, perms, testMsg ->
        val nextSleepMs = if (config.isSleepEnabled) {
            PowerScheduler.computeNextOccurrence(config.sleepHour, config.sleepMinute, config.activeDays)
        } else 0L

        val nextWakeMs = if (config.isWakeEnabled) {
            PowerScheduler.computeNextOccurrence(config.wakeHour, config.wakeMinute, config.activeDays)
        } else 0L

        MainUiState(
            config = config,
            batteryInfo = battery,
            permissions = perms,
            nextSleepFormatted = if (nextSleepMs > 0) PowerScheduler.formatEpoch(nextSleepMs) else "Disabled",
            nextSleepUntil = if (nextSleepMs > 0) PowerScheduler.formatTimeUntil(nextSleepMs) else "",
            nextWakeFormatted = if (nextWakeMs > 0) PowerScheduler.formatEpoch(nextWakeMs) else "Disabled",
            nextWakeUntil = if (nextWakeMs > 0) PowerScheduler.formatTimeUntil(nextWakeMs) else "",
            isTestScheduled = testMsg != null,
            testMessage = testMsg
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MainUiState()
    )

    init {
        // Ensure alarms are synced with stored preferences
        PowerScheduler.scheduleAll(context, repository.configFlow.value)
    }

    fun refreshPermissions() {
        _permissionsFlow.value = checkPermissions()
    }

    private fun checkPermissions(): PermissionStatus {
        val hasA11y = PowerAccessibilityService.isAccessibilityEnabled(context)
        val hasAdmin = PowerDeviceAdminReceiver.isAdminActive(context)
        val hasDnd = NotificationHelper.isDndPermissionGranted(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val hasAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }
        val isRoot = RootUtil.isDeviceRooted()

        return PermissionStatus(
            hasAccessibility = hasA11y,
            hasDeviceAdmin = hasAdmin,
            hasDnd = hasDnd,
            hasExactAlarm = hasAlarm,
            isRootAvailable = isRoot
        )
    }

    fun updateConfig(newConfig: ScheduleConfig) {
        repository.updateConfig(newConfig)
        PowerScheduler.scheduleAll(context, newConfig)
    }

    fun setSleepTime(hour: Int, minute: Int) {
        val updated = repository.configFlow.value.copy(sleepHour = hour, sleepMinute = minute)
        updateConfig(updated)
    }

    fun setWakeTime(hour: Int, minute: Int) {
        val updated = repository.configFlow.value.copy(wakeHour = hour, wakeMinute = minute)
        updateConfig(updated)
    }

    fun toggleSleep(enabled: Boolean) {
        val updated = repository.configFlow.value.copy(isSleepEnabled = enabled)
        updateConfig(updated)
    }

    fun toggleWake(enabled: Boolean) {
        val updated = repository.configFlow.value.copy(isWakeEnabled = enabled)
        updateConfig(updated)
    }

    fun toggleDay(dayOfWeek: Int) {
        val currentDays = repository.configFlow.value.activeDays.toMutableSet()
        if (currentDays.contains(dayOfWeek)) {
            if (currentDays.size > 1) { // keep at least 1 day
                currentDays.remove(dayOfWeek)
            }
        } else {
            currentDays.add(dayOfWeek)
        }
        val updated = repository.configFlow.value.copy(activeDays = currentDays)
        updateConfig(updated)
    }

    fun setSleepAction(action: SleepActionType) {
        val updated = repository.configFlow.value.copy(sleepAction = action)
        updateConfig(updated)
    }

    fun setDndEnabled(enabled: Boolean) {
        val updated = repository.configFlow.value.copy(enableDnd = enabled)
        updateConfig(updated)
    }

    fun setCountdownEnabled(enabled: Boolean) {
        val updated = repository.configFlow.value.copy(enableCountdown = enabled)
        updateConfig(updated)
    }

    fun setWakeSoundEnabled(enabled: Boolean) {
        val updated = repository.configFlow.value.copy(wakeSound = enabled)
        updateConfig(updated)
    }

    fun setWakeVibrationEnabled(enabled: Boolean) {
        val updated = repository.configFlow.value.copy(wakeVibration = enabled)
        updateConfig(updated)
    }

    fun testSleepIn10Seconds() {
        PowerScheduler.trigger10sTestSleep(context)
        _testMessageFlow.value = "Sleep test scheduled! In 10s countdown will trigger."
    }

    fun testWakeIn10Seconds() {
        PowerScheduler.trigger10sTestWake(context)
        _testMessageFlow.value = "Wake test scheduled! Lock your phone screen NOW to watch it turn on in 10s."
    }

    fun clearTestMessage() {
        _testMessageFlow.value = null
    }

    fun lockScreenNow(): Boolean {
        val lockedWithA11y = PowerAccessibilityService.lockScreen()
        return if (!lockedWithA11y) {
            PowerDeviceAdminReceiver.lockNow(context)
        } else {
            true
        }
    }

    fun openPowerMenuNow(): Boolean {
        return PowerAccessibilityService.openPowerMenu()
    }

    fun openAccessibilitySettings() {
        PowerAccessibilityService.openAccessibilitySettings(context)
    }

    fun requestDeviceAdmin() {
        PowerDeviceAdminReceiver.requestAdmin(context)
    }

    fun openDndSettings() {
        NotificationHelper.openDndSettings(context)
    }

    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun openSamsungDeviceCare() {
        val intentList = listOf(
            Intent().setClassName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
            Intent().setClassName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity"),
            Intent().setClassName("com.samsung.android.lool", "com.samsung.android.sm.ui.cstyle.SmartManagerActivity"),
            Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in intentList) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        batteryTelemetry.unregister()
    }
}
