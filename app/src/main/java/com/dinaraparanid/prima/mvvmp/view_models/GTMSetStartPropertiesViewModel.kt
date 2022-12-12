package com.dinaraparanid.prima.mvvmp.view_models

import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import com.dinaraparanid.prima.mvvmp.presenters.GTMSetStartPropertiesPresenter
import org.koin.core.component.inject

/** [ObservableViewModel] for GTMSetStartPropertiesDialog */

class GTMSetStartPropertiesViewModel : ObservableViewModel<GTMSetStartPropertiesPresenter>() {
    override val presenter by inject<GTMSetStartPropertiesPresenter>()
}