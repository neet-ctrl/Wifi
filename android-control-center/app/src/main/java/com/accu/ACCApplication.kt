package com.accu

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class ACCApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        val channels = listOf(
            NotificationChannel("audio_dsp", "Audio DSP Engine", NotificationManager.IMPORTANCE_LOW).apply {
                description = "RootlessJamesDSP audio processing service"
            },
            NotificationChannel("call_recording", "Call Recording", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Active call recording notification"
            },
            NotificationChannel("shizuku_service", "Shizuku Service", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shizuku elevated service status"
            },
            NotificationChannel("cleanup_worker", "Storage Cleanup", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Background storage cleanup tasks"
            },
            NotificationChannel("app_manager", "App Manager", NotificationManager.IMPORTANCE_LOW).apply {
                description = "App freeze/hide/uninstall operations"
            },
            NotificationChannel("key_mapper", "Key Mapper", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Key mapping service running"
            },
            NotificationChannel("general", "General", NotificationManager.IMPORTANCE_DEFAULT),
        )
        nm.createNotificationChannels(channels)
    }
}
