package com.dinaraparanid.prima.utils

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.dinaraparanid.prima.utils.workers.*
import java.util.concurrent.TimeUnit

/** Launcher for statistics refreshing tasks */

internal class RefreshWorkerLauncher private constructor() {
    internal companion object {
        private const val DAILY_WORK_NAME = "daily_refresh"
        private const val WEEKLY_WORK_NAME = "weekly_refresh"
        private const val MONTHLY_WORK_NAME = "monthly_refresh"
        private const val YEARLY_WORK_NAME = "yearly_refresh"

        @JvmStatic
        private var INSTANCE: RefreshWorkerLauncher? = null

        @JvmStatic
        internal fun launchWorkers(context: Context) {
            INSTANCE = RefreshWorkerLauncher().apply {
                WorkManager
                    .getInstance(context.applicationContext)
                    .run {
                        enqueueUniquePeriodicWork(
                            DAILY_WORK_NAME,
                            ExistingPeriodicWorkPolicy.KEEP,
                            refreshStatisticsDailyRequest
                        )

                        enqueueUniquePeriodicWork(
                            WEEKLY_WORK_NAME,
                            ExistingPeriodicWorkPolicy.KEEP,
                            refreshStatisticsWeeklyRequest
                        )

                        enqueueUniquePeriodicWork(
                            MONTHLY_WORK_NAME,
                            ExistingPeriodicWorkPolicy.KEEP,
                            refreshStatisticsMonthlyRequest
                        )

                        enqueueUniquePeriodicWork(
                            YEARLY_WORK_NAME,
                            ExistingPeriodicWorkPolicy.KEEP,
                            refreshStatisticsYearlyRequest
                        )
                    }
            }
        }
    }

    private val refreshStatisticsDailyRequest = PeriodicWorkRequest
        .Builder(
            RefreshStatisticsDailyWorker::class.java,
            1L,
            TimeUnit.DAYS
        )
        .build()

    private val refreshStatisticsWeeklyRequest = PeriodicWorkRequest
        .Builder(
            RefreshStatisticsWeeklyWorker::class.java,
            7L,
            TimeUnit.DAYS
        )
        .build()

    private val refreshStatisticsMonthlyRequest = PeriodicWorkRequest
        .Builder(
            RefreshStatisticsMonthlyWorker::class.java,
            30L,
            TimeUnit.DAYS
        )
        .build()

    private val refreshStatisticsYearlyRequest = PeriodicWorkRequest
        .Builder(
            RefreshStatisticsYearlyWorker::class.java,
            365L,
            TimeUnit.DAYS
        )
        .build()
}
