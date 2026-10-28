package com.example.util

import android.util.Log
import java.io.DataOutputStream
import java.io.File

object RootUtil {
    private const val TAG = "RootUtil"

    fun isDeviceRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }

        return checkSuBinary()
    }

    private fun checkSuBinary(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (_: Exception) {
            try {
                process = Runtime.getRuntime().exec(arrayOf("which", "su"))
                process.waitFor() == 0
            } catch (_: Exception) {
                false
            }
        } finally {
            process?.destroy()
        }
    }

    fun shutdownDevice(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            // Try standard root poweroff commands
            os.writeBytes("svc power shutdown\n")
            os.writeBytes("reboot -p\n")
            os.flush()
            os.close()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Root shutdown failed", e)
            false
        }
    }

    fun rebootDevice(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("reboot\n")
            os.flush()
            os.close()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Root reboot failed", e)
            false
        }
    }
}
