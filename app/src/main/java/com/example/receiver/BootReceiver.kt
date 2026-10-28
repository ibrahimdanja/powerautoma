package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.PowerPreferencesRepository
import com.example.util.PowerScheduler

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Device booted, restoring PowerSchedule alarms")
            val repo = PowerPreferencesRepository(context)
            val config = repo.configFlow.value
            PowerScheduler.scheduleAll(context, config)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
