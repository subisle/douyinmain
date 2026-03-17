package com.example.myapplication

import android.app.Application

class PzzApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: PzzApplication
            private set
    }
}
