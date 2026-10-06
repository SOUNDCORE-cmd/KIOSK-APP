package com.example.kioskapp

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * אחראי על אחסון מוצפן (EncryptedSharedPreferences, מגובה ב-Android Keystore)
 * של:
 *  - ה-PIN (נשמר כ-hash מלוח, לא בטקסט גלוי)
 *  - רשימת חבילות האפליקציות שמוצגות במסך הקיוסק
 */
object PinManager {

    private const val PREFS_NAME = "kiosk_secure_prefs"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_PIN_SALT = "pin_salt"
    private const val KEY_ALLOWED_PACKAGES = "allowed_packages"
    private const val DEFAULT_PIN = "1234" // PIN התחלתי - יש להחליף מיד בהפעלה הראשונה!

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // אם זו ההרצה הראשונה - קובע PIN התחלתי
        if (!prefs.contains(KEY_PIN_HASH)) {
            setPin(DEFAULT_PIN)
        }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun setPin(newPin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            .joinToString("") { "%02x".format(it) }
        val hash = sha256(newPin + salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, salt)
            .putString(KEY_PIN_HASH, hash)
            .apply()
    }

    fun verifyPin(candidate: String): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return sha256(candidate + salt) == storedHash
    }

    fun isDefaultPinStillSet(): Boolean = verifyPin(DEFAULT_PIN)

    fun getAllowedPackages(): Set<String> {
        return prefs.getStringSet(KEY_ALLOWED_PACKAGES, emptySet()) ?: emptySet()
    }

    fun setAllowedPackages(packages: Set<String>) {
        val safe = packages - PERMANENTLY_BLOCKED_PACKAGES
        prefs.edit().putStringSet(KEY_ALLOWED_PACKAGES, safe).apply()
    }
}
