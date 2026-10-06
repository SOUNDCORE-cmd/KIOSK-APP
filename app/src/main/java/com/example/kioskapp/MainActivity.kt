package com.example.kioskapp

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var policy: KioskPolicy
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PinManager.init(applicationContext)
        policy = KioskPolicy(this)

        recyclerView = findViewById(R.id.appGrid)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        emptyStateText = findViewById(R.id.emptyStateText)

        val settingsIcon: ImageButton = findViewById(R.id.settingsIcon)
        settingsIcon.setOnClickListener {
            PinPrompt.show(this, "PIN לניהול הקיוסק") {
                openManagementScreen()
            }
        }

        // בהפעלה ראשונה: רושם את עצמנו כ-Home, מגדיר את רשימת האפליקציות המורשות,
        // וסוגר את דרכי העקיפה הידועות (Safe Mode, Factory Reset מתוך ההגדרות)
        if (policy.isDeviceOwner) {
            policy.registerAsHomeApp(ComponentName(this, MainActivity::class.java))
            policy.applyAllowedPackages(PinManager.getAllowedPackages())
            policy.disableSafeBoot()
            policy.disableFactoryReset()
            policy.disableAddingUsers()
        }

        KioskWatchdogService.start(this)

        loadApps()
    }

    override fun onResume() {
        super.onResume()
        loadApps()
        enforceLockTask()
    }

    override fun onBackPressed() {
        // מבטל לחלוטין את כפתור ה-Back - אין יציאה בלי PIN
        // (כוונה מפורשת - לא לקרוא ל-super.onBackPressed())
    }

    private fun enforceLockTask() {
        if (!policy.isDeviceOwner) {
            // האפליקציה עדיין לא הוגדרה כ-Device Owner - ראה README.md
            return
        }
        policy.enterFullLockdown()
        if (policy.isLockTaskPermitted(packageName)) {
            try {
                startLockTask()
            } catch (e: IllegalStateException) {
                // כבר בתוך lock task - אין צורך לעשות דבר
            }
        }
    }

    private fun loadApps() {
        val allowed = PinManager.getAllowedPackages()
        if (allowed.isEmpty()) {
            emptyStateText.visibility = TextView.VISIBLE
            recyclerView.adapter = null
            return
        }
        emptyStateText.visibility = TextView.GONE
        val entries = resolveBankApps(packageManager, allowed)
        recyclerView.adapter = AppGridAdapter(entries) { entry ->
            launchBankApp(entry.packageName)
        }
    }

    private fun launchBankApp(packageName: String) {
        val launchIntent: Intent? = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "לא ניתן לפתוח את האפליקציה", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openManagementScreen() {
        startActivity(Intent(this, ManageAppsActivity::class.java))
    }
}
