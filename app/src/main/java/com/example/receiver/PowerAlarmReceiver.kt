package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.SleepCountdownActivity
import com.example.WakeActivity
import com.example.data.PowerPreferencesRepository
import com.example.model.SleepActionType
import com.example.service.PowerAccessibilityService
import com.example.util.NotificationHelper
import com.example.util.PowerScheduler
import com.example.util.RootUtil
import java.util.Calendar

class PowerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val isTest = intent.getBooleanExtra(PowerScheduler.EXTRA_IS_TEST, false)

        val repo = PowerPreferencesRepository(context)
        val config = repo.configFlow.value
        val todayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        Log.d(TAG, "PowerAlarmReceiver triggered action: $action, isTest: $isTest")

        when (action) {
            PowerScheduler.ACTION_TRIGGER_SLEEP -> {
                if (!isTest && !config.activeDays.contains(todayDow)) {
                    Log.d(TAG, "Today ($todayDow) is not in active schedule days, skipping sleep.")
                    PowerScheduler.scheduleAll(context, config)
                    return
                }

                if (config.enableCountdown) {
                    val countdownIntent = Intent(context, SleepCountdownActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    context.startActivity(countdownIntent)
                } else {
                    if (config.enableDnd) {
                        NotificationHelper.setDndMode(context, true)
                    }

                    when (config.sleepAction) {
                        SleepActionType.ECO_DEEP_SLEEP -> {
                            val locked = PowerAccessibilityService.lockScreen()
                            if (!locked) {
                                PowerDeviceAdminReceiver.lockNow(context)
                            }
                            NotificationHelper.showSleepNotification(
                                context,
                                "Device screen locked for scheduled sleep period."
                            )
                        }
                        SleepActionType.OPEN_POWER_MENU -> {
                            PowerAccessibilityService.openPowerMenu()
                        }
                        SleepActionType.ROOT_SHUTDOWN -> {
                            RootUtil.shutdownDevice()
                        }
                    }
                    repo.recordSleepExecution()
                }

                // Reschedule for next occurrence
                PowerScheduler.scheduleAll(context, config)
            }

            PowerScheduler.ACTION_TRIGGER_WAKE -> {
                if (!isTest && !config.activeDays.contains(todayDow)) {
                    Log.d(TAG, "Today ($todayDow) is not in active schedule days, skipping wake.")
                    PowerScheduler.scheduleAll(context, config)
                    return
                }

                val wakeIntent = Intent(context, WakeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(wakeIntent)

                // Reschedule for next occurrence
                PowerScheduler.scheduleAll(context, config)
            }
        }
    }

    companion object {
        private const val TAG = "PowerAlarmReceiver"
    }
}
