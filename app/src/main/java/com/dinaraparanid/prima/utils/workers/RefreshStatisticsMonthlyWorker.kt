package com.dinaraparanid.prima.utils.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.polymorphism.AbstractRefreshStatisticsWorker

/**
 * Refreshes statistics counting monthly
 * for all entities in FavouritesDatabase
 */

class RefreshStatisticsMonthlyWorker(
    context: Context,
    workerParameters: WorkerParameters,
) : AbstractRefreshStatisticsWorker(
    context = context.applicationContext,
    workerParameters = workerParameters,
    refreshStatisticsAction = {
        refreshTracksCountingMonthlyAsync()
        refreshArtistsCountingMonthlyAsync()
        refreshPlaylistsCountingMonthlyAsync()
        StorageUtil.getInstanceSynchronized().clearStatisticsMonthly()
    }
)