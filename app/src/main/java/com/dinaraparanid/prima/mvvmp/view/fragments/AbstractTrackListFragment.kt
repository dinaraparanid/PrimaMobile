package com.dinaraparanid.prima.mvvmp.view.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.ListItemTrackBinding
import com.dinaraparanid.prima.entities.Track
import com.dinaraparanid.prima.mvvmp.presenters.TrackListPresenter
import com.dinaraparanid.prima.mvvmp.ui_handlers.UIHandler
import com.dinaraparanid.prima.mvvmp.view_models.TrackListViewModel
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.polymorphism.*
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named

/** Ancestor for all tracks fragments */

abstract class AbstractTrackListFragment<H, B> : TrackListSearchFragment<
        TrackListPresenter,
        TrackListViewModel,
        H, B,
        Track,
        AbstractTrackListFragment<H, B>.TrackAdapter,
        AbstractTrackListFragment<H, B>.TrackAdapter.TrackHolder>()
        where H : UIHandler,
              B : ViewDataBinding {
    companion object {
        private const val UNINITIALIZED = -1
        private const val NOT_FOUND = -2

        const val Broadcast_ON_TRACK_SELECTED = "on_track_selected_channel"
        const val ON_TRACK_SELECTED_TRACK_ARG = "on_track_selected_track"
        const val ON_TRACK_SELECTED_TRACK_LIST_ARG = "on_track_selected_track_list"
    }

    override lateinit var adapter: TrackAdapter

    override val mainLabelText by lazy {
        requireArguments().getString(MainActivityFragment.MAIN_LABEL_CUR_TEXT_KEY)!!
    }

    final override fun onQueryTextChange(query: String?): Boolean {
        runOnUIThread {
            filterAndReplaceAsync(query).join()
            viewModel.presenter.numberOfTracks = itemListSearch.size
        }

        return true
    }

    override fun onDestroyView() {
        Glide.get(requireContext()).clearMemory()
        super.onDestroyView()
    }

    final override fun initAdapter() {
        adapter = TrackAdapter().apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
    }

    /** [RecyclerView.Adapter] for [TypicalViewTrackListFragment] */

    inner class TrackAdapter : AsyncListDifferAdapter<Track, TrackAdapter.TrackHolder>() {
        private val currentPlayingTrackPathFlow by inject<StateFlow<String>>(
            named(MainApplication.CURRENT_PLAYING_TRACK_PATH_FLOW)
        )

        override fun areItemsEqual(first: Track, second: Track) = first == second

        /** [RecyclerView.ViewHolder] for tracks of [TrackAdapter] */

        inner class TrackHolder(private val trackBinding: ListItemTrackBinding) :
            RecyclerView.ViewHolder(trackBinding.root), View.OnClickListener, KoinComponent {
            private val params by inject<Params>()

            private lateinit var track: Track

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) =
                requireActivity().sendBroadcast(
                    Intent(Broadcast_ON_TRACK_SELECTED).apply {
                        arguments = Bundle().apply {
                            putParcelable(ON_TRACK_SELECTED_TRACK_ARG, track)
                            putSerializable(
                                ON_TRACK_SELECTED_TRACK_LIST_ARG,
                                differ.currentList.toTypedArray()
                            )
                        }
                    }
                )

            /**
             * Constructs GUI for track item
             * @param track track to bind and use
             */

            fun bind(track: Track) {
                trackBinding.presenter = get {
                    parametersOf(
                        differ.currentList,
                        layoutPosition,
                        application.currentPlayingTrackPathFlow
                    )
                }

                trackBinding.executePendingBindings()
                this@TrackHolder.track = track

                if (params.isCoversDisplayed) runOnUIThread {
                    val albumImage = trackBinding.trackAlbumImage
                    val coverTask = application.getAlbumPictureAsync(track.path)

                    Glide.with(this@AbstractTrackListFragment)
                        .load(coverTask.await())
                        .placeholder(R.drawable.album_default)
                        .error(R.drawable.album_default)
                        .skipMemoryCache(true)
                        .thumbnail(
                            Glide.with(requireContext())
                                .asDrawable()
                                .sizeMultiplier(0.5F)
                        )
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .override(albumImage.width, albumImage.height)
                        .into(albumImage)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            TrackHolder(
                ListItemTrackBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: TrackHolder, position: Int) =
            holder.bind(differ.currentList[position])

        private fun getOldAndNewTrackIndexesAsync(newPath: String) = getFromWorkerThreadAsync {
            val oldPath = currentPlayingTrackPathFlow.value

            var oldInd = oldPath
                .takeIf { it != MainApplication.NO_PATH }
                ?.let { UNINITIALIZED }
                ?: NOT_FOUND

            var newInd = UNINITIALIZED

            for (ind in 0 until currentList.size) {
                if (oldInd != UNINITIALIZED && newInd != UNINITIALIZED)
                    break

                val track = currentList[ind]
                if (oldInd != NOT_FOUND && track.path == oldPath) oldInd = ind
                if (track.path == newPath) newInd = ind
            }

            oldInd to newInd
        }

        /**
         * Highlight track in [RecyclerView]
         * @param newPath path of track to highlight
         */

        suspend fun highlightAsync(newPath: String) {
            val (oldInd, newInd) = getOldAndNewTrackIndexesAsync(newPath).await()
            oldInd.takeIf { it != NOT_FOUND }?.let(this@TrackAdapter::notifyItemChanged)
            notifyItemChanged(newInd)
        }
    }
}