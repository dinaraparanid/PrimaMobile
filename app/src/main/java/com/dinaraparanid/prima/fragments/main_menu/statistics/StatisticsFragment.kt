package com.dinaraparanid.prima.fragments.main_menu.statistics

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.CoversRepository
import com.dinaraparanid.prima.databases.repositories.StatisticsRepository
import com.dinaraparanid.prima.databinding.FragmentStatisticsBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.Statistics
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.extensions.toFormattedTimeString
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.AsyncContext
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.utils.polymorphism.UIUpdatable
import com.dinaraparanid.prima.utils.polymorphism.fragments.MainActivitySimpleFragment
import com.dinaraparanid.prima.utils.web.genius.GeniusFetcher
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class StatisticsFragment :
    MainActivitySimpleFragment<FragmentStatisticsBinding>(),
    Rising,
    UIUpdatable<Unit>,
    AsyncContext {
    private lateinit var statisticsType: StatisticsType
    override var binding: FragmentStatisticsBinding? = null
    override val mutex = Mutex()
    override val coroutineScope get() = lifecycleScope

    private val geniusFetcher by lazy { GeniusFetcher() }

    internal companion object {
        private const val TYPE_KEY = "type"

        /** Which statistics is shown */
        internal enum class StatisticsType { ALL, DAILY, WEEKLY, MONTHLY, YEARLY }

        /**
         * Creates new instance of [StatisticsFragment]
         * @param statisticsType [StatisticsType] of created fragment
         */

        @JvmStatic
        internal fun newInstance(statisticsType: StatisticsType) = StatisticsFragment().apply {
            arguments = Bundle().apply { putInt(TYPE_KEY, statisticsType.ordinal) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelCurText = resources.getString(R.string.statistics)
        statisticsType = StatisticsType.values()[requireArguments().getInt(TYPE_KEY)]
        setMainLabelInitialized()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate<FragmentStatisticsBinding>(
                inflater,
                R.layout.fragment_statistics,
                container,
                false
            )
            .apply {
                viewModel = ViewModel()

                statisticsSwipeRefreshLayout.apply {
                    setColorSchemeColors(Params.instance.primaryColor)
                    setOnRefreshListener {
                        runOnUIThread {
                            updateUIAsync(isLocking = true)
                            isRefreshing = false
                        }
                    }
                }

                runOnUIThread { updateUIAsync(isLocking = true) }
            }

        if (application.playingBarIsVisible) up()
        return binding!!.root
    }

    /** Refreshes all data */
    override fun onResume() {
        super.onResume()
        runOnUIThread { updateUIAsync(isLocking = true) }
    }

    /** Frees all images */
    override fun onStop() {
        super.onStop()
        freeUIMemory()
    }

    /** Frees all images */
    override fun onDestroyView() {
        freeUIMemory()
        super.onDestroyView()
    }

    /** Frees all images */
    private fun freeUIMemory() {
        binding?.bestTrackImage?.let(Glide.with(this)::clear)
        binding?.bestArtistImage?.let(Glide.with(this)::clear)
        binding?.bestPlaylistImage?.let(Glide.with(this)::clear)

        Glide.get(requireContext()).run {
            bitmapPool.clearMemory()
            clearMemory()
            runOnIOThread { clearDiskCache() }
        }
    }

    /** Updates all data */
    override suspend fun updateUIAsyncNoLock(src: Unit) {
        runOnIOThread {
            val tasks = StatisticsRepository.getInstanceSynchronized().getFullFragmentStatistics {
                when (statisticsType) {
                    StatisticsType.ALL -> Triple(
                        getMaxCountingTrackAsync(),
                        getMaxCountingArtistAsync(),
                        getMaxCountingPlaylistAsync()
                    )

                    StatisticsType.DAILY -> Triple(
                        getMaxCountingTrackDailyAsync(),
                        getMaxCountingArtistDailyAsync(),
                        getMaxCountingPlaylistDailyAsync()
                    )

                    StatisticsType.WEEKLY -> Triple(
                        getMaxCountingTrackWeeklyAsync(),
                        getMaxCountingArtistWeeklyAsync(),
                        getMaxCountingPlaylistWeeklyAsync()
                    )

                    StatisticsType.MONTHLY -> Triple(
                        getMaxCountingTrackMonthlyAsync(),
                        getMaxCountingArtistMonthlyAsync(),
                        getMaxCountingPlaylistMonthlyAsync()
                    )

                    StatisticsType.YEARLY -> Triple(
                        getMaxCountingTrackYearlyAsync(),
                        getMaxCountingArtistYearlyAsync(),
                        getMaxCountingPlaylistYearlyAsync()
                    )
                }
            }

            launch(Dispatchers.Main) {
                val statistics = when (statisticsType) {
                    StatisticsType.ALL -> StorageUtil
                        .getInstanceSynchronized()
                        .loadStatistics()

                    StatisticsType.DAILY -> StorageUtil
                        .getInstanceSynchronized()
                        .loadStatisticsDaily()

                    StatisticsType.WEEKLY -> StorageUtil
                        .getInstanceSynchronized()
                        .loadStatisticsWeekly()

                    StatisticsType.MONTHLY -> StorageUtil
                        .getInstanceSynchronized()
                        .loadStatisticsMonthly()

                    StatisticsType.YEARLY -> StorageUtil
                        .getInstanceSynchronized()
                        .loadStatisticsYearly()
                } ?: Statistics.empty

                binding!!.run {
                    appWasOpened.text = statistics.appWasOpened.toString()
                    musicInTime.text = statistics.musicInMinutes.toFormattedTimeString()
                    tracksListened.text = statistics.numberOfTracks.toString()
                    tracksConverted.text = statistics.numberOfConverted.toString()
                    tracksRecorded.text = statistics.numberOfRecorded.toString()
                    tracksTrimmed.text = statistics.numberOfTrimmed.toString()
                    tracksChanged.text = statistics.numberOfChanged.toString()
                    lyricsShown.text = statistics.numberOfLyricsShown.toString()
                    numberOfCreatedPlaylists.text = statistics.numberOfCreatedPlaylists.toString()
                    numberOfGuessedTracks.text = statistics.numberOfGuessedTracksInGTM.toString()

                    val bestTrack = tasks.first.await()

                    bestTrack?.path?.let {
                        Glide.with(this@StatisticsFragment)
                            .load(application.getAlbumPictureAsync(it).await())
                            .error(R.drawable.album_default)
                            .fallback(R.drawable.album_default)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .override(bestTrackImage.width, bestTrackImage.height)
                            .into(bestTrackImage)
                    }

                    bestTrackTitle.text = bestTrack?.title ?: ""
                    bestTrackArtist.text = bestTrack?.artist ?: ""

                    val bestArtist = tasks.second.await()

                    bestArtist?.name?.let { name ->
                        geniusFetcher
                            .fetchTrackDataSearch(name)
                            .observe(viewLifecycleOwner) { response ->
                                response
                                    .response
                                    .hits
                                    .firstOrNull()
                                    ?.result
                                    ?.primaryArtist
                                    ?.imageUrl
                                    ?.toUri()
                                    ?.let {
                                        Glide.with(this@StatisticsFragment)
                                            .load(it)
                                            .error(R.drawable.album_default)
                                            .fallback(R.drawable.album_default)
                                            .transition(DrawableTransitionOptions.withCrossFade())
                                            .override(bestArtistImage.width, bestArtistImage.height)
                                            .into(bestArtistImage)
                                    }
                            }
                    }

                    bestArtistName.text = bestArtist?.name ?: ""

                    val bestPlaylist = tasks.third.await()

                    bestPlaylist?.let {
                        runOnIOThread {
                            setPlaylistCoverAsync(
                                it.title,
                                AbstractPlaylist.PlaylistType.values()[it.type]
                            )
                        }
                    }

                    bestPlaylistTitle.text = bestPlaylist?.title ?: ""
                }
            }
        }
    }

    /**
     * Rise fragment if playing bar is active.
     * It handles GUI error when playing bar was hiding some content
     */

    override fun up() {
        if (!fragmentActivity.isUpped)
            binding!!.statisticsLayout.layoutParams =
                (binding!!.statisticsLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = Params.PLAYING_TOOLBAR_HEIGHT
                }
    }

    /** Refreshes all data */
    private suspend fun updateUIAsync(isLocking: Boolean) =
        updateUIAsync(Unit, isLocking)

    private suspend fun setTrackCoverToAlbum(path: String) {
        val cover = getFromIOThreadAsync {
            application.getAlbumPictureAsync(path).await()
        }.await()

        runOnUIThread {
            binding?.run {
                cover?.let {
                    Glide
                        .with(this@StatisticsFragment)
                        .load(it)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .error(R.drawable.album_default)
                        .fallback(R.drawable.album_default)
                        .override(bestPlaylistImage.width, bestPlaylistImage.height)
                        .into(bestPlaylistImage)
                } ?: Glide
                    .with(this@StatisticsFragment)
                    .load(BitmapFactory.decodeResource(resources, R.drawable.album_default))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.album_default)
                    .fallback(R.drawable.album_default)
                    .override(bestPlaylistImage.width, bestPlaylistImage.height)
                    .into(bestPlaylistImage)
            }
        }
    }

    /** Gets playlist cover by its [title] and [type] asynchronously */
    private suspend fun setPlaylistCoverAsync(
        title: String,
        type: AbstractPlaylist.PlaylistType
    ) {
        try {
            val taskDB = when (type) {
                AbstractPlaylist.PlaylistType.CUSTOM -> CoversRepository
                    .getInstanceSynchronized()
                    .getPlaylistWithCoverAsync(title)
                    .await()

                AbstractPlaylist.PlaylistType.ALBUM -> CoversRepository
                    .getInstanceSynchronized()
                    .getAlbumWithCoverAsync(title)
                    .await()

                AbstractPlaylist.PlaylistType.GTM ->
                    throw IllegalArgumentException("GTM playlist in favourites")
            }

            when {
                taskDB != null -> taskDB.image.toBitmap()

                else -> when (type) {
                    AbstractPlaylist.PlaylistType.ALBUM -> application
                        .allTracksWithoutHidden
                        .firstOrNull { it.album == title }

                    AbstractPlaylist.PlaylistType.CUSTOM -> CustomPlaylistsRepository
                        .getInstanceSynchronized()
                        .getFirstTrackOfPlaylistAsync(title)
                        .await()

                    AbstractPlaylist.PlaylistType.GTM ->
                        throw IllegalArgumentException("GTM playlist in favourites")
                }?.run {
                    runOnUIThread {
                        geniusFetcher
                            .fetchTrackDataSearch("$artist $title")
                            .observe(viewLifecycleOwner) { searchResponse ->
                                when (searchResponse.meta.status) {
                                    !in 200 until 300 -> runOnIOThread {
                                        setTrackCoverToAlbum(path)
                                    }

                                    else -> searchResponse.response.hits.firstOrNull()?.result?.id?.let {
                                        geniusFetcher
                                            .fetchTrackInfoSearch(it)
                                            .observe(viewLifecycleOwner) { (meta, response) ->
                                                when (meta.status) {
                                                    !in 200 until 300 -> runOnIOThread {
                                                        setTrackCoverToAlbum(path)
                                                    }

                                                    else -> response.song.album?.coverArtUrl?.let {
                                                        binding!!.run {
                                                            Glide
                                                                .with(this@StatisticsFragment)
                                                                .load(it)
                                                                .transition(
                                                                    DrawableTransitionOptions.withCrossFade()
                                                                )
                                                                .error(R.drawable.album_default)
                                                                .fallback(R.drawable.album_default)
                                                                .override(
                                                                    bestPlaylistImage.width,
                                                                    bestPlaylistImage.height
                                                                )
                                                                .into(bestPlaylistImage)
                                                        }
                                                    } ?: runOnIOThread {
                                                        setTrackCoverToAlbum(path)
                                                    }
                                                }
                                            }
                                    }
                                }
                            }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()

            runOnUIThread {
                Toast.makeText(
                    requireContext(),
                    R.string.image_too_big,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}