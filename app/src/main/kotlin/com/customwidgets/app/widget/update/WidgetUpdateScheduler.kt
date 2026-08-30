package com.customwidgets.app.widget.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WidgetUpdateScheduler {

    const val PERIODIC_WORK_TAG = "widget_periodic_updates"
    const val ONE_TIME_WORK_TAG = "widget_immediate_update"
    const val MINIMUM_INTERVAL_MINUTES = 15L

    fun buildPeriodicWorkRequest(): PeriodicWorkRequest {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        return PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            MINIMUM_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(PERIODIC_WORK_TAG)
            .build()
    }

    fun schedulePeriodicUpdates(context: Context) {
        val workRequest = buildPeriodicWorkRequest()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun triggerImmediateUpdate(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
            .addTag(ONE_TIME_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelAllUpdates(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(PERIODIC_WORK_TAG)
    }
}
