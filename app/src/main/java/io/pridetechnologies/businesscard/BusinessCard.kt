package io.pridetechnologies.businesscard

import android.app.Application
import com.google.firebase.FirebaseApp

class BusinessCard: Application() {
    override fun onCreate() {
        super.onCreate()

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
