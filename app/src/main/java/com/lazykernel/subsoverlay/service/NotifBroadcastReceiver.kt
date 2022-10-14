package com.lazykernel.subsoverlay.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager

class NotifBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "com.lazykernel.subsoverlay.STOP_BG_SERVICE") {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            if (preferences.getBoolean("accessibilityServiceRunning", false)) {
                preferences.edit().putBoolean("accessibilityServiceRunning", false).apply()
            }
        }
    }
}
