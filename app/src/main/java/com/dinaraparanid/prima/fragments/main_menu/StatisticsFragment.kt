package com.dinaraparanid.prima.fragments.main_menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.statistics.StatisticsArtist
import com.dinaraparanid.prima.databases.entities.statistics.StatisticsPlaylist
import com.dinaraparanid.prima.databases.entities.statistics.StatisticsTrack
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.ImageRepository
import com.dinaraparanid.prima.databases.repositories.StatisticsRepository
import com.dinaraparanid.prima.databinding.FragmentStatisticsBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.Statistics
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.AsyncContext
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.utils.polymorphism.UIUpdatable
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel
import com.kaopiz.kprogresshud.KProgressHUD
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class StatisticsFragment :
    MainActivitySimpleFragment<FragmentStatisticsBinding>(),
    Rising,
    UIUpdatable<Unit>,
    AsyncContext {
    private var awaitDialog: Deferred<KProgressHUD>? = null
    private lateinit var type: Type

    override var binding: FragmentStatisticsBinding? = null
    override val mutex = Mutex()
    override val coroutineScope get() = lifecycleScope

    internal companion object {
        private const val TYPE_KEY = "type"

        /** Which statistics is shown */
        internal enum class Type { ALL, DAILY, WEEKLY, MONTHLY, YEARLY }

        /**
         * Creates new instance of [StatisticsFragment]
         * @param mainLabelOldText current main label text
         * @param type [Type] of created fragment
         */

        @JvmStatic
        internal fun newInstance(mainLabelOldText: String, type: Type) = StatisticsFragment().apply {
            arguments = Bundle().apply {
                putInt(TYPE_KEY, type.ordinal)
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = resources.getString(R.string.statistics)
        type = Type.values()[requireArguments().getInt(TYPE_KEY)]
        setMainLabelInitialized()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate<FragmentStatisticsBinding>(inflater, R.layout.fragment_statistics, container, false)
            .apply {
                viewModel = ViewModel()

                statisticsSwipeRefreshLayout.apply {
                    setOnRefreshListener {
                        setColorSchemeColors(Params.instance.primaryColor)
                        runOnUIThread {
                            updateUI(isLocking = true)
                            isRefreshing = false
                        }
                    }
                }

                runOnUIThread { updateUI(isLocking = true) }
            }

        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        awaitDialog?.cancel()
        awaitDialog = null
    }

    override suspend fun updateUINoLock(src: Unit) {
        awaitDialog = getFromUIThreadAsync {
            createAndShowAwaitDialog(
                context = requireContext(),
                isCancelable = false
            )
        }

        runOnIOThread {
            val tasks = StatisticsRepository.getMultipleTasks {
                when (type) {
                    Type.ALL -> listOf(
                        getMaxCountingTrackAsync(),
                        getMaxCountingArtistAsync(),
                        getMaxCountingPlaylistAsync()
                    )

                    Type.DAILY -> listOf(
                        getMaxCountingTrackDailyAsync(),
                        getMaxCountingArtistDailyAsync(),
                        getMaxCountingPlaylistDailyAsync()
                    )

                    Type.WEEKLY -> listOf(
                        getMaxCountingTrackWeeklyAsync(),
                        getMaxCountingArtistWeeklyAsync(),
                        getMaxCountingPlaylistWeeklyAsync()
                    )

                    Type.MONTHLY -> listOf(
                        getMaxCountingTrackMonthlyAsync(),
                        getMaxCountingArtistMonthlyAsync(),
                        getMaxCountingPlaylistMonthlyAsync()
                    )

                    Type.YEARLY -> listOf(
                        getMaxCountingTrackYearlyAsync(),
                        getMaxCountingArtistYearlyAsync(),
                        getMaxCountingPlaylistYearlyAsync()
                    )
                }
            }

            launch(Dispatchers.Main) {
                val statistics = when (type) {
                    Type.ALL -> StorageUtil.getInstanceSynchronized().loadStatistics()
                    Type.DAILY -> StorageUtil.getInstanceSynchronized().loadStatisticsDaily()
                    Type.WEEKLY -> StorageUtil.getInstanceSynchronized().loadStatisticsWeekly()
                    Type.MONTHLY -> StorageUtil.getInstanceSynchronized().loadStatisticsMonthly()
                    Type.YEARLY -> StorageUtil.getInstanceSynchronized().loadStatisticsYearly()
                } ?: Statistics.empty

                binding!!.run {
                    musicInTime.text = statistics.musicInMinutes.asFormattedTime()
                    tracksListened.text = statistics.numberOfTracks.toString()
                    tracksConverted.text = statistics.numberOfConverted.toString()
                    tracksRecorded.text = statistics.numberOfRecorded.toString()
                    tracksTrimmed.text = statistics.numberOfTrimmed.toString()
                    tracksChanged.text = statistics.numberOfChanged.toString()
                    lyricsShown.text = statistics.numberOfLyricsShown.toString()
                    numberOfCreatedPlaylists.text = statistics.numberOfCreatedPlaylists.toString()
                    numberOfGuessedTracks.text = statistics.numberOfGuessedTracksInGTM.toString()
                    
                    val bestTrack = tasks[0].await() as StatisticsTrack?

                    bestTrack?.path?.let {
                        Glide.with(this@StatisticsFragment)
                            .load(application.getAlbumPictureAsync(it))
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .override(bestTrackImage.width, bestTrackImage.height)
                            .into(bestTrackImage)
                    }

                    bestTrackTitle.text = bestTrack?.title ?: ""
                    bestTrackArtist.text = bestTrack?.artist ?: ""

                    val bestArtist = tasks[1].await() as StatisticsArtist?

                    // TODO: artist image

                    bestArtistName.text = bestArtist?.name ?: ""

                    val bestPlaylist = tasks[2].await() as StatisticsPlaylist?

                    bestPlaylist?.let {
                        Glide.with(this@StatisticsFragment)
                            .load(getPlaylistCoverAsync(it.title, it.type))
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .override(bestPlaylistImage.width, bestPlaylistImage.height)
                            .into(bestPlaylistImage)
                    }

                    bestPlaylistTitle.text = bestPlaylist?.title ?: ""
                    awaitDialog?.await()?.dismiss()
                }
            }
        }
    }

    override fun up() {
        if (!fragmentActivity.isUpped)
            binding!!.statisticsLayout.layoutParams =
                (binding!!.statisticsLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = Params.PLAYING_TOOLBAR_HEIGHT
                }
    }

    private suspend fun updateUI(isLocking: Boolean) = updateUI(Unit, isLocking)

    private fun Long.asFormattedTime(): String {
        var it = this
        val days = it / 1440; it /= 1440
        val hours = it / 60; it /= 60
        return "$days d., $hours h., $it m. ($this m)"
    }

    private suspend fun getPlaylistCoverAsync(
        title: String,
        type: AbstractPlaylist.PlaylistType
    ) = getFromIOThreadAsync {
        try {
            val taskDB = when (type) {
                AbstractPlaylist.PlaylistType.CUSTOM -> ImageRepository
                    .getInstanceSynchronized()
                    .getPlaylistWithImageAsync(title)
                    .await()

                AbstractPlaylist.PlaylistType.ALBUM -> ImageRepository
                    .getInstanceSynchronized()
                    .getAlbumWithImageAsync(title)
                    .await()

                AbstractPlaylist.PlaylistType.GTM ->
                    throw IllegalArgumentException("GTM playlist in favourites")
            }

            when {
                taskDB != null -> taskDB.image.toBitmap()

                else -> when (type) {
                    AbstractPlaylist.PlaylistType.ALBUM ->
                        application.allTracks.firstOrNull { it.playlist == title }?.path

                    AbstractPlaylist.PlaylistType.CUSTOM -> CustomPlaylistsRepository
                        .getInstanceSynchronized()
                        .getFirstTrackOfPlaylistAsync(title)
                        .await()
                        ?.path

                    AbstractPlaylist.PlaylistType.GTM ->
                        throw IllegalArgumentException("GTM playlist in favourites")
                }?.let { application.getAlbumPictureAsync(it) }
            }
        } catch (e: Exception) {
            launch(Dispatchers.Main) {
                Toast.makeText(
                    requireContext(),
                    R.string.image_too_big,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}