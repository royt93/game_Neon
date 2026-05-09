package com.tranphuloi.neon

import android.app.Application
import com.tranphuloi.neon.utils.Logger

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Logger.d("App.onCreate")
    }
}
