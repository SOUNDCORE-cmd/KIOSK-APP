package com.example.kioskapp

import android.app.Application

class KioskApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // מוודא שקובץ ההעדפות המוצפן קיים מוקדם ככל האפשר
        PinManager.init(this)
    }
}
