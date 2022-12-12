package com.dinaraparanid.prima.mvvmp.view.dialogs

import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.DialogGtmSetPlaybackBinding
import com.dinaraparanid.prima.mvvmp.presenters.GTMSetStartPlaybackPresenter
import com.dinaraparanid.prima.mvvmp.ui_handlers.GTMSetStartPlaybackUIHandler
import com.dinaraparanid.prima.mvvmp.view_models.GTMSetStartPlaybackViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.inject

/**
 * Specially for custom playlists
 * Similar to [GTMSetStartPropertiesDialog] but without track's amount param
 */

class GTMSetStartPlaybackDialog : GTMSetPropertiesDialog<
        GTMSetStartPlaybackPresenter,
        GTMSetStartPlaybackViewModel,
        GTMSetStartPlaybackUIHandler,
        DialogGtmSetPlaybackBinding
>() {
    override val uiHandler by inject<GTMSetStartPlaybackUIHandler>()
    override val viewModel by viewModel<GTMSetStartPlaybackViewModel>()

    override val dialogBinding
        get() = DataBindingUtil
            .inflate<DialogGtmSetPlaybackBinding>(
                layoutInflater,
                R.layout.dialog_gtm_set_playback,
                null, false
            )
            .apply { viewModel = this@GTMSetStartPlaybackDialog.viewModel }
}