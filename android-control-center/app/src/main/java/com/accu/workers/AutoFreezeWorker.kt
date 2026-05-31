package com.accu.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.accu.connection.AccuConnectionManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
class AutoFreezeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val connectionManager: AccuConnectionManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = applicationContext.getSharedPreferences("accu_freeze_prefs", Context.MODE_PRIVATE)
            val frozenPackages = prefs.getStringSet("frozen_packages", emptySet()) ?: return Result.success()
            if (frozenPackages.isEmpty()) return Result.success()

            if (!connectionManager.isPrivilegeAvailable()) {
                Timber.w("AutoFreezeWorker: ACCU privilege not available, will retry")
                return Result.retry()
            }

            for (pkg in frozenPackages) {
                try {
                    connectionManager.exec("pm suspend $pkg")
                    Timber.d("AutoFreezeWorker: suspended $pkg")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to suspend $pkg")
                }
            }
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "AutoFreezeWorker failed")
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "auto_freeze_periodic"

        fun schedule(context: Context, intervalHours: Long = 24) {
            val request = PeriodicWorkRequestBuilder<AutoFreezeWorker>(intervalHours, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
