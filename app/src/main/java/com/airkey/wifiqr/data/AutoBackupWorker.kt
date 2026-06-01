package com.airkey.wifiqr.data

import android.content.Context
import android.net.Uri
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class AutoBackupWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefs = ctx.getSharedPreferences("airkey_backup_prefs", Context.MODE_PRIVATE)
            val uriStr = prefs.getString(KEY_BACKUP_URI, null)
                ?: return@withContext Result.failure()

            val folderUri = Uri.parse(uriStr)
            val db = WifiDatabase.getDatabase(ctx)
            val networks = db.wifiDao().getAllNetworksList()

            val result = BackupManager.performBackup(ctx, folderUri, networks)
            if (result.isSuccess) {
                prefs.edit()
                    .putLong(KEY_LAST_BACKUP_TIME, System.currentTimeMillis())
                    .putString(KEY_LAST_BACKUP_NAME, result.getOrNull())
                    .apply()
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val KEY_BACKUP_URI = "backup_folder_uri"
        const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        const val KEY_LAST_BACKUP_NAME = "last_backup_name"
        const val WORK_NAME = "airkey_auto_backup"

        fun schedule(context: Context, intervalDays: Int) {
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                intervalDays.toLong(), TimeUnit.DAYS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun getPrefs(context: Context) =
            context.getSharedPreferences("airkey_backup_prefs", Context.MODE_PRIVATE)
    }
}
