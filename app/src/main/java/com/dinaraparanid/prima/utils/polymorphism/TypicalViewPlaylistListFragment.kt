package com.dinaraparanid.prima.utils.polymorphism

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentPlaylistsBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.decorations.HorizontalSpaceItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel
import com.kaopiz.kprogresshud.KProgressHUD

/** [AbstractPlaylistListFragment] with no floating button */

abstract class TypicalViewPlaylistListFragment : AbstractPlaylistListFragment<FragmentPlaylistsBinding>() {
    private var awaitDialog: KProgressHUD? = null

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate<FragmentPlaylistsBinding>(
                inflater,
                R.layout.fragment_playlists,
                container,
                false
            )
            .apply {
                viewModel = ViewModel()

                mvvmViewModel = viewModel!!
                emptyTextView = playlistsEmpty

                updater = playlistSwipeRefreshLayout.apply {
                    setColorSchemeColors(Params.instance.primaryColor)
                    setOnRefreshListener {
                        runOnUIThread {
                            loadAsync().join()
                            updateUI(isLocking = true)
                            isRefreshing = false
                        }
                    }
                }

                runOnUIThread {
                    awaitDialog = createAndShowAwaitDialog(requireContext(), false)

                    loadAsync().join()
                    awaitDialog?.dismiss()
                    initAdapter()

                    itemListSearch.addAll(itemList)
                    adapter.setCurrentList(itemList)
                    setEmptyTextViewVisibility(itemList)

                    recyclerView = playlistRecyclerView.apply {
                        layoutManager = when (resources.configuration.orientation) {
                            Configuration.ORIENTATION_PORTRAIT ->
                                when (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
                                    Configuration.SCREENLAYOUT_SIZE_NORMAL ->
                                        GridLayoutManager(context, 2)

                                    Configuration.SCREENLAYOUT_SIZE_LARGE ->
                                        GridLayoutManager(context, 3)

                                    else -> GridLayoutManager(context, 2)
                                }

                            else -> when (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
                                Configuration.SCREENLAYOUT_SIZE_NORMAL ->
                                    GridLayoutManager(context, 3)

                                Configuration.SCREENLAYOUT_SIZE_LARGE ->
                                    GridLayoutManager(context, 4)

                                else -> GridLayoutManager(context, 3)
                            }
                        }

                        adapter = this@TypicalViewPlaylistListFragment.adapter
                        addItemDecoration(VerticalSpaceItemDecoration(30))
                        addItemDecoration(HorizontalSpaceItemDecoration(30))
                    }

                    if (application.playingBarIsVisible) up()
                }
            }

        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        awaitDialog?.dismiss()
        awaitDialog = null
    }
}