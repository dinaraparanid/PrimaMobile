package com.dinaraparanid.prima.utils.polymorphism

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.dinaraparanid.prima.databases.repositories.StatisticsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Refreshes statistics counting
 * for all entities in FavouritesDatabase
 */

abstract class AbstractRefreshStatisticsWorker(
    context: Context,
    workerParameters: WorkerParameters,
    private val refreshStatisticsAction: suspend StatisticsRepository.() -> Unit
) : Worker(context, workerParameters), CoroutineScope by MainScope() {
    override fun doWork(): Result {
        launch(Dispatchers.IO) {
            refreshStatisticsAction(StatisticsRepository.getInstanceSynchronized())
        }
        return Result.success()
    }
}