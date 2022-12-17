package com.dinaraparanid.prima.mvvmp.view.dialogs

import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.DialogTrackSearchParamsBinding
import com.dinaraparanid.prima.dialogs.MessageDialog
import com.dinaraparanid.prima.fragments.track_lists.TrackListFoundFragment
import com.dinaraparanid.prima.mvvmp.StateChangedCallback
import com.dinaraparanid.prima.mvvmp.presenters.TrackSearchParamsPresenter
import com.dinaraparanid.prima.mvvmp.ui_handlers.TrackSearchParamsUIHandler
import com.dinaraparanid.prima.mvvmp.view_models.TrackSearchParamsViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * Dialog to input title and artist
 * for track of which lyrics should be found
 */

abstract class TrackSearchParamsDialogFragment(
    trackTitle: String,
    artistName: String,
    private val target: TrackListFoundFragment.Target,
    private val setSheetBehaviourFromExpandedToCollapsedChannel: Channel<Unit>
) : ObservableDialogFragment<
        TrackSearchParamsPresenter,
        TrackSearchParamsViewModel,
        TrackSearchParamsUIHandler,
        DialogTrackSearchParamsBinding
>() {
    final override val uiHandler by inject<TrackSearchParamsUIHandler>()

    final override val viewModel by inject<TrackSearchParamsViewModel> {
        parametersOf(trackTitle, artistName)
    }

    final override val stateChangesCallbacks =
        emptyArray<StateChangedCallback<TrackSearchParamsUIHandler>>()

    final override val dialogBinding
        get() = DataBindingUtil.inflate<DialogTrackSearchParamsBinding>(
            layoutInflater,
            R.layout.dialog_track_search_params,
            null, false
        ).apply {
            viewModel = this@TrackSearchParamsDialogFragment.viewModel
        }

    final override val dialogView
        get() = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setCancelable(true)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                lifecycleScope.launch {
                    uiHandler
                        .runCatching {
                            dialog.onOkPressedAsync(
                                fragmentManager = parentFragmentManager,
                                trackTitle = viewModel.presenter.searchedTrackTitle,
                                artistName = viewModel.presenter.searchedArtistName,
                                target = target,
                                setSheetBehaviourFromExpandedToCollapsedChannel
                            )
                        }
                        .getOrElse {
                            dialog.cancel()
                            MessageDialog(R.string.unknown_error).show(parentFragmentManager, null)
                        }
                }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create()
}