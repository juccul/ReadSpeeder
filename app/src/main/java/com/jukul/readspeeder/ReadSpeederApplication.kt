package com.jukul.readspeeder

import android.app.Application
import com.google.android.material.color.DynamicColors

class ReadSpeederApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
