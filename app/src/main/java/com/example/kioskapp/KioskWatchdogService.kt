package com.example.kioskapp

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper

/**
 * שירות שרץ בפרוססס נפרד (":watchdog", ראה AndroidManifest) כך שהוא נשאר בחיים
 * גם אם הפרוססס הראשי של האפליקציה קורס (crash / OOM kill).
 *
 * כל 5 שניות הוא בודק אם למשימה (Task) של האפליקציה יש עדיין activity חי.
 * אם לא - סימן שהאפליקציה נהרגה - הוא מפעיל מחדש את MainActivity.
 */
class KioskWatchdogService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkIntervalMs = 5_000L

    private val checkRunnable = object : Runnable {
        override fun run() {
            checkAndRecover()
            handler.postDelayed(this, checkIntervalMs)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.removeCallbacks(checkRunnable)
        handler.post(checkRunnable)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        super.onDestroy()
    }

    private fun checkAndRecover() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        // getAppTasks() מחזיר את המשימות של האפליקציה שלנו (לפי package/uid),
        // ועובד גם כשקוראים לו מפרוססס משני של אותה אפליקציה.
        val hasLiveTask = try {
            am.appTasks.isNotEmpty()
        } catch (e: Exception) {
            true // אם הבדיקה נכשלת, לא נניח שהאפליקציה מתה כדי לא ליצור לולאת הפעלות
        }

        if (!hasLiveTask) {
            relaunchKiosk()
        }
    }

    private fun relaunchKiosk() {
        val launch = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(launch)
    }

    private fun buildNotification(): Notification {
        val channelId = "kiosk_watchdog_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "שירות שמירה על הקיוסק",
                NotificationManager.IMPORTANCE_MIN
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        return Notification.Builder(this, channelId)
            .setContentTitle("מצב קיוסק פעיל")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 42

        fun start(context: Context) {
            val intent = Intent(context, KioskWatchdogService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
