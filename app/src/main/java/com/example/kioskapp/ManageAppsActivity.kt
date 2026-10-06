package com.example.kioskapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * מסך זה נגיש רק לאחר אימות PIN מוצלח ב-MainActivity.
 * הוא נשאר בתוך אותו Lock Task (כי הוא באותה חבילה - com.example.kioskapp).
 */
class ManageAppsActivity : Activity() {

    private lateinit var adapter: InstalledAppAdapter
    private lateinit var policy: KioskPolicy

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage)
        policy = KioskPolicy(this)

        val recyclerView: RecyclerView = findViewById(R.id.manageRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val currentAllowed = PinManager.getAllowedPackages()
        val rows = listLaunchableApps(packageManager, packageName, currentAllowed).toMutableList()
        adapter = InstalledAppAdapter(rows)
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            saveSelection()
        }

        findViewById<Button>(R.id.changePinButton).setOnClickListener {
            promptChangePin()
        }

        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.switchLauncherButton).setOnClickListener {
            confirmSwitchToOriginalLauncher()
        }
    }

    /**
     * פותח את מסך המערכת לבחירת אפליקציית "בית" (Home app). זו הדרך הרשמית
     * היחידה לעשות זאת ללא Device Owner - המשתמש (המנהל, לאחר PIN) בוחר שם
     * ידנית את הלאנצ'ר המקורי של המכשיר במקום הקיוסק.
     */
    private fun confirmSwitchToOriginalLauncher() {
        android.app.AlertDialog.Builder(this)
            .setTitle("החלפת לאנצ'ר")
            .setMessage(
                "ייפתח מסך הגדרות המערכת לבחירת אפליקציית הבית. " +
                    "בחר שם את הלאנצ'ר המקורי של המכשיר (למשל 'Pixel Launcher' או " +
                    "לאנצ'ר היצרן) כדי לצאת ממצב הקיוסק. אפשר תמיד לחזור למצב קיוסק " +
                    "ע\"י בחירה מחדש באפליקציה הזו במסך הזה."
            )
            .setPositiveButton("המשך") { _, _ ->
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            }
            .setNegativeButton("ביטול", null)
            .show()
    }

    override fun onBackPressed() {
        // מותר לחזור מכאן ללאנצ'ר הראשי (לא יוצא מהקיוסק, רק חוזר למסך האפליקציות)
        finish()
    }

    private fun saveSelection() {
        val selected = adapter.getSelectedPackages()
        PinManager.setAllowedPackages(selected)
        policy.applyAllowedPackages(selected)
        Toast.makeText(this, "רשימת האפליקציות עודכנה (${selected.size} אפליקציות)", Toast.LENGTH_SHORT).show()
    }

    private fun promptChangePin() {
        val input = EditText(this).apply {
            hint = "PIN חדש (4 ספרות ומעלה)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("שינוי קוד PIN")
            .setView(input)
            .setPositiveButton("שמור") { _, _ ->
                val newPin = input.text.toString()
                if (newPin.length < 4) {
                    Toast.makeText(this, "ה-PIN חייב להכיל לפחות 4 ספרות", Toast.LENGTH_SHORT).show()
                } else {
                    PinManager.setPin(newPin)
                    Toast.makeText(this, "PIN עודכן בהצלחה", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("ביטול", null)
            .show()
    }
}
