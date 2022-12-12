package com.dinaraparanid.prima.mvvmp.view_models

import com.dinaraparanid.prima.mvvmp.presenters.GTMSetStartPlaybackPresenter
import org.koin.core.component.inject

/** [ObservableViewModel] for GTMSetStartPlaybackDialog */

class GTMSetStartPlaybackViewModel : GTMPropertiesViewModel<GTMSetStartPlaybackPresenter>() {
    override val presenter by inject<GTMSetStartPlaybackPresenter>()
}