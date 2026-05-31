package com.accu.sdkdemo

import android.app.Application
import com.accu.sdkdemo.data.CrashManager
import com.accu.sdkdemo.data.LogManager

class AccuSdkTestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashManager.init(this)
        LogManager.info("App", "ACCU SDK Test App started — package: $packageName")
    }
}
