package com.example.kioskapp

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * מקבל האירועים של מדיניות המכשיר. חובה שיוגדר כ-Device Admin לפני
 * שאפשר להפוך אותו ל-Device Owner (dpm set-device-owner).
 */
class KioskAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i("KioskAdmin", "Device admin enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w("KioskAdmin", "Device admin disabled")
    }
}
