package com.dinaraparanid.prima.dialogs

import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.fragments.track_collections.DefaultPlaylistListFragment
import com.dinaraparanid.prima.utils.Statistics
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.polymorphism.InputDialog
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [InputDialog] to create new playlist.
 * Asks about playlist's title and creates
 * new playlist if it still doesn't exists.
 */

internal class NewPlaylistDialog(fragment: DefaultPlaylistListFragment) : InputDialog(
    message = R.string.playlist_title,
    okAction = { input, _ ->
        fragment.runOnIOThread {
            launch(Dispatchers.IO) {
                launch(Dispatchers.IO) {
                    StorageUtil.runSynchronized {
                        storeStatistics(
                            loadStatistics()
                                ?.let(Statistics::withIncrementedNumberOfCreatedPlaylists)
                                ?: Statistics.empty.withIncrementedNumberOfCreatedPlaylists
                        )

                        storeStatisticsDaily(
                            loadStatisticsDaily()
                                ?.let(Statistics::withIncrementedNumberOfCreatedPlaylists)
                                ?: Statistics.empty.withIncrementedNumberOfCreatedPlaylists
                        )

                        storeStatisticsWeekly(
                            loadStatisticsWeekly()
                                ?.let(Statistics::withIncrementedNumberOfCreatedPlaylists)
                                ?: Statistics.empty.withIncrementedNumberOfCreatedPlaylists
                        )

                        storeStatisticsMonthly(
                            loadStatisticsMonthly()
                                ?.let(Statistics::withIncrementedNumberOfCreatedPlaylists)
                                ?: Statistics.empty.withIncrementedNumberOfCreatedPlaylists
                        )

                        storeStatisticsYearly(
                            loadStatisticsYearly()
                                ?.let(Statistics::withIncrementedNumberOfCreatedPlaylists)
                                ?: Statistics.empty.withIncrementedNumberOfCreatedPlaylists
                        )
                    }
                }

                CustomPlaylistsRepository
                    .getInstanceSynchronized()
                    .addPlaylistsAsync(CustomPlaylist.Entity(0, input))
                    .join()

                fragment.updateUIOnChangeContentAsync()
            }
        }
    },
    errorMessage = R.string.playlist_exists
)