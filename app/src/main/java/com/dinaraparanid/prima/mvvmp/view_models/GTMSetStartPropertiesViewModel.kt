package com.dinaraparanid.prima.mvvmp.view_models

import com.dinaraparanid.prima.mvvmp.presenters.GTMSetStartPropertiesPresenter
import org.koin.core.component.inject

/** [ObservableViewModel] for GTMSetStartPropertiesDialog */

class GTMSetStartPropertiesViewModel : GTMPropertiesViewModel<GTMSetStartPropertiesPresenter>() {
    override val presenter by inject<GTMSetStartPropertiesPresenter>()
}