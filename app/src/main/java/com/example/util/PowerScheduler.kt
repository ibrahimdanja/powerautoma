package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.model.ScheduleConfig
import com.example.receiver.PowerAlarmReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object PowerScheduler {
    private const val TAG = "PowerScheduler"

    const val ACTION_TRIGGER_SLEEP = "com.example.powerschedule.ACTION_TRIGGER_SLEEP"
    const val ACTION_TRIGGER_WAKE = "com.example.powerschedule.ACTION_TRIGGER_WAKE"
    const val EXTRA_IS_TEST = "extra_is_test"

    private const val REQUEST_CODE_SLEEP = 1001
    private const val REQUEST_CODE_WAKE = 1002
    private const val REQUEST_CODE_TEST_SLEEP = 2001
    private const val REQUEST_CODE_TEST_WAKE = 2002

    fun scheduleAll(context: Context, config: ScheduleConfig) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Schedule Sleep
        if (config.isSleepEnabled) {
            val nextSleepMs = computeNextOccurrence(
                hour = config.sleepHour,
                minute = config.sleepMinute,
                activeDays = config.activeDays
            )
            setAlarm(
                context = context,
                alarmManager = alarmManager,
                triggerTimeMs = nextSleepMs,
                action = ACTION_TRIGGER_SLEEP,
                requestCode = REQUEST_CODE_SLEEP
            )
            Log.d(TAG, "Scheduled next sleep at ${formatEpoch(nextSleepMs)}")
        } else {
            cancelAlarm(context, alarmManager, ACTION_TRIGGER_SLEEP, REQUEST_CODE_SLEEP)
        }

        // 2. Schedule Wake
        if (config.isWakeEnabled) {
            val nextWakeMs = computeNextOccurrence(
                hour = config.wakeHour,
                minute = config.wakeMinute,
                activeDays = config.activeDays
            )
            setAlarm(
                context = context,
                alarmManager = alarmManager,
                triggerTimeMs = nextWakeMs,
                action = ACTION_TRIGGER_WAKE,
                requestCode = REQUEST_CODE_WAKE
            )
            Log.d(TAG, "Scheduled next wake at ${formatEpoch(nextWakeMs)}")
        } else {
            cancelAlarm(context, alarmManager, ACTION_TRIGGER_WAKE, REQUEST_CODE_WAKE)
        }
    }

    fun trigger10sTestSleep(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTimeMs = System.currentTimeMillis() + 10_000L
        setAlarm(
            context = context,
            alarmManager = alarmManager,
            triggerTimeMs = triggerTimeMs,
            action = ACTION_TRIGGER_SLEEP,
            requestCode = REQUEST_CODE_TEST_SLEEP,
            isTest = true
        )
    }

    fun trigger10sTestWake(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTimeMs = System.currentTimeMillis() + 10_000L
        setAlarm(
            context = context,
            alarmManager = alarmManager,
            triggerTimeMs = triggerTimeMs,
            action = ACTION_TRIGGER_WAKE,
            requestCode = REQUEST_CODE_TEST_WAKE,
            isTest = true
        )
    }

    private fun setAlarm(
        context: Context,
        alarmManager: AlarmManager,
        triggerTimeMs: Long,
        action: String,
        requestCode: Int,
        isTest: Boolean = false
    ) {
        val intent = Intent(context, PowerAlarmReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_IS_TEST, isTest)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode + 100,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val clockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, showPendingIntent)
        try {
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission check required", e)
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } catch (fallbackEx: Exception) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            }
        }
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        cancelAlarm(context, alarmManager, ACTION_TRIGGER_SLEEP, REQUEST_CODE_SLEEP)
        cancelAlarm(context, alarmManager, ACTION_TRIGGER_WAKE, REQUEST_CODE_WAKE)
    }

    private fun cancelAlarm(
        context: Context,
        alarmManager: AlarmManager,
        action: String,
        requestCode: Int
    ) {
        val intent = Intent(context, PowerAlarmReceiver::class.java).apply {
            this.action = action
        }
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun computeNextOccurrence(hour: Int, minute: Int, activeDays: Set<Int>): Long {
        val now = Calendar.getInstance()
        val candidate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If activeDays is empty, fallback to every day
        val days = if (activeDays.isEmpty()) {
            (1..7).toSet()
        } else {
            activeDays
        }

        for (dayOffset in 0..7) {
            val checkCal = candidate.clone() as Calendar
            checkCal.add(Calendar.DAY_OF_YEAR, dayOffset)
            val dayOfWeek = checkCal.get(Calendar.DAY_OF_WEEK)

            if (days.contains(dayOfWeek)) {
                if (checkCal.timeInMillis > now.timeInMillis) {
                    return checkCal.timeInMillis
                }
            }
        }

        // Fallback to candidate + 1 day
        candidate.add(Calendar.DAY_OF_YEAR, 1)
        return candidate.timeInMillis
    }

    fun formatEpoch(epochMs: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
        return sdf.format(epochMs)
    }

    fun formatTimeUntil(epochMs: Long): String {
        val diffMs = epochMs - System.currentTimeMillis()
        if (diffMs <= 0) return "Right now"
        val totalMinutes = diffMs / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "in ${hours}h ${minutes}m"
            hours > 0 -> "in ${hours}h"
            else -> "in ${minutes}m"
        }
    }

    fun formatHourMinute(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(cal.time)
    }
}
