package com.dinaraparanid.prima.mvvmp.view_models

import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter

/** [ObservableViewModel] without any states */

class DefaultViewModel<P : BasePresenter>(presenter: P) : ObservableViewModel<P>(presenter)