package com.dinaraparanid.prima.utils.polymorphism.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import carbon.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.repositories.CoversRepository
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databinding.ListItemPlaylistBinding
import com.dinaraparanid.prima.fragments.track_collections.DefaultPlaylistListFragment
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AsyncListDifferAdapter
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.viewmodels.androidx.DefaultViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** [ListFragment] for all albums and user's playlists */

abstract class AbstractPlaylistListFragment<T : ViewDataBinding> : MainActivityUpdatingListFragment<
        AbstractPlaylist,
        AbstractPlaylistListFragment<T>.PlaylistAdapter,
        AbstractPlaylistListFragment<T>.PlaylistAdapter.PlaylistHolder, T>() {
    interface Callbacks : CallbacksFragment.Callbacks {
        /**
         * Calls new [TypicalViewTrackListFragment] with playlist's (album's) tracks
         * @param id id of playlist or 0 if it's album
         * @param title title of playlist or album
         */

        fun onPlaylistSelected(
            title: String,
            type: AbstractPlaylist.PlaylistType,
            id: Long = 0,
        )
    }

    final override var _adapter: PlaylistAdapter? = null

    final override val viewModel by lazy {
        ViewModelProvider(this)[DefaultViewModel::class.java]
    }

    final override var updater: SwipeRefreshLayout? = null
    final override var binding: T? = null
    final override var emptyTextView: TextView? = null
    protected lateinit var mvvmViewModel: ViewModel

    final override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelCurText.set(requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY)!!)
        setMainLabelInitializedSync()
        super.onCreate(savedInstanceState)
    }

    override fun onDestroyView() {
        Glide.get(requireContext()).clearMemory()
        super.onDestroyView()
    }

    final override suspend fun updateUIAsyncNoLock(src: List<AbstractPlaylist>) {
        adapter.setCurrentList(src)
        recyclerView!!.adapter = adapter
        setEmptyTextViewVisibility(src)
    }

    final override fun filter(models: Collection<AbstractPlaylist>?, query: String) =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.title.lowercase() } ?: listOf()
        }

    final override fun initAdapter() {
        _adapter = PlaylistAdapter().apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
    }

    /** [RecyclerView.Adapter] for [AbstractPlaylistListFragment] */

    inner class PlaylistAdapter : AsyncListDifferAdapter<AbstractPlaylist, PlaylistAdapter.PlaylistHolder>() {
        override fun areItemsEqual(
            first: AbstractPlaylist,
            second: AbstractPlaylist
        ) = first == second

        /** [RecyclerView.ViewHolder] for tracks of [PlaylistAdapter] */

        inner class PlaylistHolder(private val playlistBinding: ListItemPlaylistBinding) :
            RecyclerView.ViewHolder(playlistBinding.root),
            View.OnClickListener {
            private lateinit var playlist: AbstractPlaylist

            internal val playlistImage: ImageView = itemView
                .findViewById<ImageView>(R.id.playlist_image)
                .apply { if (!Params.instance.isRoundingPlaylistImage) setCornerRadius(0F) }

            init {
                playlistBinding.viewModel = mvvmViewModel
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                runOnUIThread {
                    (callbacker as Callbacks).onPlaylistSelected(
                        playlist.title,
                        playlist.type,
                        when (this@AbstractPlaylistListFragment) {
                            is DefaultPlaylistListFragment -> CustomPlaylistsRepository
                                .getInstanceSynchronized()
                                .getPlaylistAsync(playlist.title)
                                .await()!!
                                .id

                            else -> 0
                        }
                    )
                }
            }

            /**
             * Makes all GUI customizations for the playlist
             * @param _playlist playlist to bind
             */

            internal fun bind(_playlist: AbstractPlaylist) {
                playlistBinding.title = _playlist.title
                playlistBinding.executePendingBindings()
                playlist = _playlist

                if (Params.instance.areCoversDisplayed) runOnIOThread {
                    playlist.takeIf(AbstractPlaylist::isNotEmpty)?.run {
                        try {
                            val taskDB = when (this@AbstractPlaylistListFragment) {
                                is PlaylistListFragment -> CoversRepository
                                    .getInstanceSynchronized()
                                    .getPlaylistWithCoverAsync(playlist.title)
                                    .await()

                                else -> CoversRepository
                                    .getInstanceSynchronized()
                                    .getAlbumWithCoverAsync(playlist.title)
                                    .await()
                            }

                            when {
                                taskDB != null -> runOnUIThread {
                                    Glide.with(this@AbstractPlaylistListFragment)
                                        .load(taskDB.image.toBitmap())
                                        .placeholder(R.drawable.album_default)
                                        .skipMemoryCache(true)
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .override(playlistImage.width, playlistImage.height)
                                        .into(playlistImage)
                                }

                                else -> {
                                    val task = application.getAlbumPictureAsync(
                                        currentTrack.path
                                    )

                                    runOnUIThread {
                                        Glide.with(this@AbstractPlaylistListFragment)
                                            .load(task.await())
                                            .placeholder(R.drawable.album_default)
                                            .skipMemoryCache(true)
                                            .transition(DrawableTransitionOptions.withCrossFade())
                                            .override(
                                                playlistImage.width,
                                                playlistImage.height
                                            )
                                            .into(playlistImage)
                                    }
                                }
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
                    } ?: run {
                        try {
                            val taskDB = when (this@AbstractPlaylistListFragment) {
                                is DefaultPlaylistListFragment -> CoversRepository
                                    .getInstanceSynchronized()
                                    .getPlaylistWithCoverAsync(playlist.title)
                                    .await()

                                else -> CoversRepository
                                    .getInstanceSynchronized()
                                    .getAlbumWithCoverAsync(playlist.title)
                                    .await()
                            }

                            when {
                                taskDB != null -> runOnUIThread {
                                    Glide.with(this@AbstractPlaylistListFragment)
                                        .load(taskDB.image.toBitmap())
                                        .placeholder(R.drawable.album_default)
                                        .skipMemoryCache(true)
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .override(playlistImage.width, playlistImage.height)
                                        .into(playlistImage)
                                }

                                else -> runOnUIThread {
                                    Glide.with(this@AbstractPlaylistListFragment)
                                        .load(R.drawable.album_default)
                                        .skipMemoryCache(true)
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .override(playlistImage.width, playlistImage.height)
                                        .into(playlistImage)
                                }
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
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PlaylistHolder(
            ListItemPlaylistBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        override fun onBindViewHolder(holder: PlaylistHolder, position: Int) =
            holder.bind(differ.currentList[position])

        override fun onViewRecycled(holder: PlaylistHolder) {
            Glide.with(this@AbstractPlaylistListFragment).clear(holder.playlistImage)
            super.onViewRecycled(holder)
        }
    }
}