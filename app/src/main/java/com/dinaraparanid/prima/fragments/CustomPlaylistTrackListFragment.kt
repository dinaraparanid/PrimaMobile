package com.dinaraparanid.prima.fragments

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomTarget
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.PlaylistImage
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.ImageRepository
import com.dinaraparanid.prima.databinding.FragmentCustomPlaylistTrackListBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.dialogs.AreYouSureDialog
import com.dinaraparanid.prima.utils.dialogs.RenamePlaylistDialog
import com.dinaraparanid.prima.utils.extensions.enumerated
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.extensions.toByteArray
import com.dinaraparanid.prima.utils.extensions.tracks
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.ChangeImageFragment
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.viewmodels.mvvm.CustomPlaylistTrackListViewModel
import kotlinx.coroutines.*

/**
 * [AbstractTrackListFragment] for user's playlists
 */

class CustomPlaylistTrackListFragment :
    AbstractTrackListFragment<FragmentCustomPlaylistTrackListBinding>(),
    ChangeImageFragment {
    private var playlistId = 0L
    internal val playlistTitle: String by lazy { mainLabelCurText }

    override var binding: FragmentCustomPlaylistTrackListBinding? = null
    override var updater: SwipeRefreshLayout? = null
    override var emptyTextView: TextView? = null
    override var amountOfTracks: carbon.widget.TextView? = null
    override var trackOrderTitle: carbon.widget.TextView? = null

    internal companion object {
        private const val PLAYLIST_ID_KEY = "playlist_id"

        /**
         * Creates new instance of fragment with params
         * @param mainLabelOldText old main label text (to return)
         * @param mainLabelCurText main label text for current fragment
         * @param playlistId id of playlist
         * @return new instance of fragment with params in bundle
         */

        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            mainLabelCurText: String,
            playlistId: Long,
        ) = CustomPlaylistTrackListFragment().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
                putLong(PLAYLIST_ID_KEY, playlistId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlistId = requireArguments().getLong(PLAYLIST_ID_KEY)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate<FragmentCustomPlaylistTrackListBinding>(
                inflater,
                R.layout.fragment_custom_playlist_track_list,
                container,
                false
            )
            .apply {
                viewModel = CustomPlaylistTrackListViewModel(
                    this@CustomPlaylistTrackListFragment,
                    mainLabelCurText,
                    playlistId,
                    itemList.tracks
                )

                updater = customPlaylistTrackSwipeRefreshLayout.apply {
                    setOnRefreshListener {
                        try {
                            runOnUIThread {
                                setColorSchemeColors(Params.getInstanceSynchronized().primaryColor)
                                loadAsync().await()
                                updateUI(isLocking = true)
                                isRefreshing = false
                            }
                        } catch (ignored: Exception) {
                            // permissions not given
                        }
                    }
                }

                emptyTextView = customPlaylistTrackListEmpty
                trackOrderTitle = customPlaylistTrackOrderTitle

                try {
                    runOnUIThread {
                        val task = loadAsync()
                        val progress = createAndShowAwaitDialog(requireContext(), false)

                        task.await()
                        progress.dismiss()

                        setEmptyTextViewVisibility(itemList)
                        itemListSearch.addAll(itemList)
                        adapter.setCurrentList(itemList)

                        Glide.with(this@CustomPlaylistTrackListFragment)
                            .load(
                                application.run {
                                    try {
                                        val repImage = ImageRepository
                                            .instance
                                            .getPlaylistWithImageAsync(playlistTitle)
                                            .await()

                                        when {
                                            repImage != null ->
                                                repImage.image.toBitmap()

                                            itemList.isEmpty() ->
                                                getAlbumPictureAsync("").await()

                                            else ->
                                                getAlbumPictureAsync(itemList.first().second.path)
                                                    .await()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            requireContext(),
                                            R.string.image_too_big,
                                            Toast.LENGTH_LONG
                                        ).show()

                                        getAlbumPictureAsync("").await()
                                    }
                                }
                            )
                            .skipMemoryCache(true)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .override(
                                customPlaylistTracksImage.width,
                                customPlaylistTracksImage.height
                            )
                            .into(customPlaylistTracksImage)

                        amountOfTracks = amountOfTracksCustomPlaylist.apply {
                            isSelected = true
                            val txt = "${resources.getString(R.string.tracks)}: ${itemList.size}"
                            text = txt
                        }

                        recyclerView = customPlaylistTrackRecyclerView.apply {
                            layoutManager = LinearLayoutManager(context)
                            adapter = this@CustomPlaylistTrackListFragment.adapter
                            addItemDecoration(VerticalSpaceItemDecoration(30))
                        }

                        if (application.playingBarIsVisible) up()
                    }
                } catch (ignored: Exception) {
                    // permissions not given
                }

                amountOfTracks = amountOfTracksCustomPlaylist.apply {
                    isSelected = true
                    val txt = "${resources.getString(R.string.tracks)}: ${itemList.size}"
                    text = txt
                }

                trackOrderTitle = customPlaylistTrackOrderTitle
                updateOrderTitle()
            }

        fragmentActivity.mainLabelCurText = mainLabelCurText
        return binding!!.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_custom_playlist_menu, menu)

        (menu.findItem(R.id.custom_playlist_search).actionView as SearchView).run {
            setOnQueryTextListener(this@CustomPlaylistTrackListFragment)
        }

        menu.findItem(R.id.cp_find_by).setOnMenuItemClickListener { selectSearch() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.rename_playlist -> RenamePlaylistDialog(this)
                .show(requireActivity().supportFragmentManager, null)

            R.id.remove_playlist -> AreYouSureDialog(
                R.string.ays_remove_playlist,
            ) {
                CustomPlaylistsRepository.instance.run {
                    runOnIOThread {
                        removePlaylistAsync(mainLabelCurText)
                        removeTracksOfPlaylistAsync(mainLabelCurText)
                    }
                }
                requireActivity().supportFragmentManager.popBackStack()
            }.show(requireActivity().supportFragmentManager, null)
        }

        return super.onOptionsItemSelected(item)
    }

    override suspend fun loadAsync(): Deferred<Unit> = coroutineScope {
        async(Dispatchers.IO) {
            val task = CustomPlaylistsRepository.instance
                .getTracksOfPlaylistAsync(mainLabelCurText)

            itemList.clear()
            itemList.addAll(Params.sortedTrackList(task.await().enumerated()))
            Unit
        }
    }

    override fun setUserImage(image: Uri) {
        Glide.with(this)
            .asBitmap()
            .load(image)
            .skipMemoryCache(true)
            .into(
                object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                    ) {
                        val playlistImage = PlaylistImage(
                            playlistTitle,
                            resource.toByteArray()
                        )

                        val rep = ImageRepository.instance

                        runOnIOThread {
                            rep.removePlaylistWithImageAsync(playlistTitle).join()

                            try {
                                rep.addPlaylistWithImageAsync(playlistImage)

                                launch(Dispatchers.Main) {
                                    Glide.with(this@CustomPlaylistTrackListFragment)
                                        .load(image)
                                        .skipMemoryCache(true)
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .override(
                                            binding!!.customPlaylistTracksImage.width,
                                            binding!!.customPlaylistTracksImage.height
                                        )
                                        .into(binding!!.customPlaylistTracksImage)
                                }
                            } catch (e: Exception) {
                                rep.removePlaylistWithImageAsync(playlistTitle)

                                Toast.makeText(
                                    requireContext(),
                                    R.string.image_too_big,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) = Unit
                }
            )
    }

    /**
     * Renames main label when playlist is rename
     * @param title new playlist's title
     */

    internal fun renameTitle(title: String) {
        mainLabelCurText = title
        fragmentActivity.mainLabelCurText = mainLabelCurText
    }
}