package com.legendsayantan.adbtools

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.legendsayantan.adbtools.lib.Logger.Companion.log
import com.legendsayantan.adbtools.lib.ShizukuRunner
import com.legendsayantan.adbtools.lib.Utils.Companion.getAllInstalledApps
import com.legendsayantan.adbtools.lib.Utils.Companion.initialiseStatusBar
import com.legendsayantan.adbtools.lib.Utils.Companion.postNotification
import com.legendsayantan.adbtools.services.ThemePatcherService
import java.util.Timer
import kotlin.concurrent.schedule

/**
 * @author legendsayantan
 */
class ThemePatcherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_patcher)
        initialiseStatusBar()


        val themeStoreBtn = findViewById<MaterialButton>(R.id.launchThemeStore)

        ShizukuRunner.command("pm grant $packageName android.permission.WRITE_SETTINGS",
            object : ShizukuRunner.CommandResultListener { })
        ShizukuRunner.command("pm grant $packageName android.permission.WRITE_SECURE_SETTINGS",
            object : ShizukuRunner.CommandResultListener { })

        themeStoreBtn.setOnClickListener {
            themeStoreBtn.isEnabled = false
            startForegroundService(Intent(this, ThemePatcherService::class.java))

            Thread{
                var themeStores =
                    packageManager.getAllInstalledApps().filter { it.packageName.contains("theme")||it.loadLabel(packageManager).contains("theme") }
                if (themeStores.any { it.packageName.contains("store") }) themeStores =
                    themeStores.filter { it.packageName.contains("store") }
                runOnUiThread {
                    //start via intent
                    val intent = packageManager.getLaunchIntentForPackage(themeStores[0].packageName)
                    intent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            }.start()

        }
    }



}