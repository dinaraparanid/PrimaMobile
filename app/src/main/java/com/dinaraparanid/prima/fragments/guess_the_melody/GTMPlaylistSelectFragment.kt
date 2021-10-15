package com.dinaraparanid.prima.fragments.guess_the_melody

import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databinding.FragmentSelectPlaylistBinding
import com.dinaraparanid.prima.databinding.ListItemGtmSelectPlaylistBinding
import com.dinaraparanid.prima.fragments.PlaylistSelectFragment
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.decorations.DividerItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.CallbacksFragment
import com.dinaraparanid.prima.utils.polymorphism.Playlist
import com.dinaraparanid.prima.utils.polymorphism.UpdatingListFragment
import com.dinaraparanid.prima.viewmodels.androidx.DefaultViewModel
import kotlinx.coroutines.*

/**
 * Fragment which chooses playlists
 * to start "Guess the Melody" game
 */

class GTMPlaylistSelectFragment : UpdatingListFragment<
        Playlist,
        GTMPlaylistSelectFragment.PlaylistAdapter,
        GTMPlaylistSelectFragment.PlaylistAdapter.PlaylistHolder,
        FragmentSelectPlaylistBinding>() {
    internal interface Callbacks : CallbacksFragment.Callbacks {
        fun onPlaylistSelected()
    }

    override var binding: FragmentSelectPlaylistBinding? = null
    override var adapter: PlaylistAdapter? = null
    override var emptyTextView: TextView? = null
    override var updater: SwipeRefreshLayout? = null

    override val viewModel: ViewModel by lazy {
        ViewModelProvider(this)[DefaultViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainLabelOldText =
            requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY) ?: titleDefault
        mainLabelCurText = resources.getString(R.string.playlists)

        viewModel.viewModelScope.launch(Dispatchers.IO) {
            val task = loadAsync()
            val progress = async(Dispatchers.Main) {
                createAndShowAwaitDialog(requireContext(), false)
            }

            task.join()
            launch(Dispatchers.Main) { progress.await().dismiss() }

            try {
                launch(Dispatchers.Main) { setEmptyTextViewVisibility(itemList) }
            } catch (ignored: Exception) {
                // not initialized
            }

            itemListSearch.addAll(itemList)
            adapter = PlaylistAdapter(itemList).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentSelectPlaylistBinding>(
            inflater,
            R.layout.fragment_select_playlist,
            container,
            false
        ).apply {
            viewModel = com.dinaraparanid.prima.viewmodels.mvvm.ViewModel()

            updater = selectPlaylistSwipeRefreshLayout.apply {
                setColorSchemeColors(Params.instance.primaryColor)
                setOnRefreshListener {
                    this@GTMPlaylistSelectFragment.viewModel.viewModelScope.launch(Dispatchers.IO) {
                        val task = loadAsync()
                        val progress = async(Dispatchers.Main) {
                            createAndShowAwaitDialog(requireContext(), false)
                        }

                        task.join()

                        launch(Dispatchers.Main) {
                            progress.await().dismiss()
                            updateUI()
                            isRefreshing = false
                        }
                    }
                }
            }

            emptyTextView = selectPlaylistEmpty
            setEmptyTextViewVisibility(itemList)

            recyclerView = selectPlaylistRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@GTMPlaylistSelectFragment.adapter?.apply {
                    stateRestorationPolicy =
                        RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                }

                addItemDecoration(VerticalSpaceItemDecoration(30))
                addItemDecoration(DividerItemDecoration(requireActivity()))
            }
        }

        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        (requireActivity() as MainActivity).mainLabelCurText = mainLabelCurText
        return binding!!.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            val task = loadAsync()
            val progress = async(Dispatchers.Main) {
                createAndShowAwaitDialog(requireContext(), false)
            }

            task.join()

            launch(Dispatchers.Main) {
                progress.await().dismiss()
                updateUI()
            }
        }
    }

    override fun filter(models: Collection<Playlist>?, query: String): List<Playlist> =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.title.lowercase() } ?: listOf()
        }

    override suspend fun loadAsync(): Job = coroutineScope {
        launch(Dispatchers.IO) {
            itemList.clear()

            // albums

            try {
                requireActivity().contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Albums.ALBUM),
                    null,
                    null,
                    MediaStore.Audio.Media.ALBUM + " ASC"
                ).use { cursor ->
                    if (cursor != null) {
                        val playlistList = mutableListOf<Playlist>()

                        while (cursor.moveToNext()) {
                            val albumTitle = cursor.getString(0)

                            (requireActivity().application as MainApplication).allTracks
                                .firstOrNull { it.playlist == albumTitle }
                                ?.let { track ->
                                    playlistList.add(
                                        DefaultPlaylist(
                                            albumTitle,
                                            tracks = mutableListOf(track) // album image
                                        )
                                    )
                                }
                        }

                        itemList.addAll(playlistList.distinctBy(Playlist::title))
                    }
                }
            } catch (ignored: Exception) {
                // Permission to storage not given
            }

            // user's playlists

            if ((requireActivity().application as MainApplication).checkAndRequestPermissions()) {
                val task = CustomPlaylistsRepository.instance.getPlaylistsAsync()
                itemList.addAll(task.await().map { DefaultPlaylist(it.title) })
                Unit
            }
        }
    }

    override fun updateUI(src: List<Playlist>) {
        adapter = PlaylistAdapter(src).apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        recyclerView!!.adapter = adapter
        setEmptyTextViewVisibility(src)
    }

    /**
     * [RecyclerView.Adapter] for [PlaylistSelectFragment]
     */

    inner class PlaylistAdapter(private val playlists: List<Playlist>) :
        RecyclerView.Adapter<PlaylistAdapter.PlaylistHolder>() {

        /**
         * [RecyclerView.ViewHolder] for playlists of [PlaylistAdapter]
         */

        inner class PlaylistHolder(private val playlistBinding: ListItemGtmSelectPlaylistBinding) :
            RecyclerView.ViewHolder(playlistBinding.root),
            View.OnClickListener {
            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?): Unit =
                (callbacker as Callbacks).onPlaylistSelected()

            /**
             * Constructs GUI for playlist item
             * @param title playlist's title
             */

            fun bind(title: String): Unit = playlistBinding.run {
                viewModel = com.dinaraparanid.prima.viewmodels.mvvm.ViewModel()
                this.title = title
                executePendingBindings()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistHolder =
            PlaylistHolder(
                ListItemGtmSelectPlaylistBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun getItemCount(): Int = playlists.size

        override fun onBindViewHolder(holder: PlaylistHolder, position: Int): Unit =
            holder.bind(playlists[position].title)
    }
}