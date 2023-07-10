package io.pridetechnologies.businesscard

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp

class BusinessCard: Application() {
    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        FirebaseApp.initializeApp(this)
    }
    private var mInstance: BusinessCard? = null

    fun getInstance(): BusinessCard? {
        if (mInstance == null) {
            mInstance = BusinessCard()
        }
        return mInstance
    }
}
