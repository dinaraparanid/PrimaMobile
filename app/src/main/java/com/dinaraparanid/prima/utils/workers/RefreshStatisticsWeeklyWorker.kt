package com.dinaraparanid.prima.utils.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.polymorphism.AbstractRefreshStatisticsWorker

/**
 * Refreshes statistics counting weekly
 * for all entities in FavouritesDatabase
 */

class RefreshStatisticsWeeklyWorker(
    context: Context,
    workerParameters: WorkerParameters,
) : AbstractRefreshStatisticsWorker(
    context = context.applicationContext,
    workerParameters = workerParameters,
    refreshStatisticsAction = {
        refreshTracksCountingWeeklyAsync()
        refreshArtistsCountingWeeklyAsync()
        refreshPlaylistsCountingWeeklyAsync()
        StorageUtil.getInstanceSynchronized().clearStatisticsWeekly()
    }
)