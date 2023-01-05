package com.dinaraparanid.prima.mvvmp.view.dialogs

import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.DialogGtmSetStartPropertiesBinding
import com.dinaraparanid.prima.mvvmp.presenters.GTMSetStartPropertiesPresenter
import com.dinaraparanid.prima.mvvmp.ui_handlers.GTMSetStartPropertiesUIHandler
import com.dinaraparanid.prima.mvvmp.view_models.GTMSetStartPropertiesViewModel
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AsyncContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.inject

/**
 * Dialog to set properties for game (amount of tracks and maximum playback time).
 * Tracks amount must be smaller than 9999, playback limit is 99 seconds.
 * @param playlist playlist in which tracks will be guessed
 */

class GTMSetStartPropertiesDialogFragment(val playlist: AbstractPlaylist) :
    GTMSetPropertiesDialogFragment<
            GTMSetStartPropertiesPresenter,
            GTMSetStartPropertiesViewModel,
            GTMSetStartPropertiesUIHandler,
            DialogGtmSetStartPropertiesBinding>(),
    AsyncContext {
    override val uiHandler by inject<GTMSetStartPropertiesUIHandler>()
    override val viewModel by viewModel<GTMSetStartPropertiesViewModel>()
    override val coroutineScope get() = lifecycleScope

    override val dialogBinding
        get() = DataBindingUtil
            .inflate<DialogGtmSetStartPropertiesBinding>(
                layoutInflater,
                R.layout.dialog_gtm_set_start_properties,
                null, false
            )
            .apply { presenter = viewModel.presenter }
}