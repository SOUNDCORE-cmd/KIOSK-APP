package com.example.kioskapp

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.UserManager

/**
 * עוטף את כל הקריאות ל-DevicePolicyManager במקום אחד.
 * חשוב: כל הפונקציות כאן פועלות רק אם האפליקציה כבר הוגדרה כ-Device Owner
 * (ראה README.md לגבי dpm set-device-owner).
 */
class KioskPolicy(context: Context) {

    private val appContext = context.applicationContext
    private val dpm = appContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val admin = ComponentName(appContext, KioskAdminReceiver::class.java)

    val isDeviceOwner: Boolean
        get() = dpm.isDeviceOwnerApp(appContext.packageName)

    /** מגדיר אילו חבילות מותרות בזמן Lock Task Mode (כולל את עצמנו, לעולם לא Settings). */
    fun applyAllowedPackages(bankPackages: Set<String>) {
        if (!isDeviceOwner) return
        val safe = bankPackages - PERMANENTLY_BLOCKED_PACKAGES
        val full = (safe + appContext.packageName).toTypedArray()
        dpm.setLockTaskPackages(admin, full)
    }

    /** נועל את המכשיר לגמרי בתוך המשימה הנוכחית - בלי Home, בלי Recents, בלי סטטוס בר. */
    fun enterFullLockdown() {
        if (!isDeviceOwner) return
        dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
        dpm.setStatusBarDisabled(admin, true)
        dpm.setKeyguardDisabled(admin, true)
    }

    /** משחרר זמנית פיצ'רים (למשל בזמן שהמנהל בתוך מסך הניהול) - עדיין בתוך lock task. */
    fun relaxForManagement() {
        if (!isDeviceOwner) return
        dpm.setStatusBarDisabled(admin, false)
    }

    fun registerAsHomeApp(activityComponent: ComponentName) {
        if (!isDeviceOwner) return
        val filter = IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        dpm.addPersistentPreferredActivity(admin, filter, activityComponent)
    }

    fun isLockTaskPermitted(packageName: String): Boolean = dpm.isLockTaskPermitted(packageName)

    /**
     * חוסם Factory Reset מתוך הגדרות המערכת (Settings > System > Reset).
     * לא חוסם fastboot/recovery ברמת bootloader - זה נשאר תלוי בנעילת ה-bootloader עצמה.
     */
    fun disableFactoryReset() {
        if (!isDeviceOwner) return
        dpm.setFactoryResetDisabled(admin, true)
    }

    /**
     * חוסם כניסה ל-Safe Mode. זו נקודת העקיפה הכי נפוצה לקיוסקים (Safe Mode מכבה
     * את כל אפליקציות הצד השלישי, כולל את אפליקציית הקיוסק עצמה, ומחזיר גישה מלאה
     * ללאנצ'ר המקורי של המכשיר). חשוב במיוחד כשה-bootloader פתוח.
     */
    fun disableSafeBoot() {
        if (!isDeviceOwner) return
        dpm.addUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT)
    }

    /** חוסם הוספת משתמשים נוספים למכשיר - כל משתמש חדש היה עוקף את הקיוסק. */
    fun disableAddingUsers() {
        if (!isDeviceOwner) return
        dpm.addUserRestriction(admin, UserManager.DISALLOW_ADD_USER)
    }

    /** חוסם התקנת/הסרת אפליקציות מחוץ למסך הניהול שלנו. */
    fun disableAppInstallUninstall() {
        if (!isDeviceOwner) return
        dpm.addUserRestriction(admin, UserManager.DISALLOW_INSTALL_APPS)
        dpm.addUserRestriction(admin, UserManager.DISALLOW_UNINSTALL_APPS)
    }
}
