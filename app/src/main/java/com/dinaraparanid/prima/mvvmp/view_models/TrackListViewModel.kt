package com.dinaraparanid.prima.mvvmp.view_models

import com.dinaraparanid.prima.mvvmp.presenters.TrackListPresenter

/** [ObservableViewModel] for TrackListFragments */

class TrackListViewModel(presenter: TrackListPresenter) :
    ObservableViewModel<TrackListPresenter>(presenter)