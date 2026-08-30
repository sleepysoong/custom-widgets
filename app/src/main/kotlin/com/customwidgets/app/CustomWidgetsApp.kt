package com.customwidgets.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CustomWidgetsApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
