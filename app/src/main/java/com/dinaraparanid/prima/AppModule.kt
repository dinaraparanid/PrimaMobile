package com.dinaraparanid.prima

import com.dinaraparanid.prima.entities.Track
import com.dinaraparanid.prima.mvvmp.presenters.*
import com.dinaraparanid.prima.mvvmp.ui_handlers.*
import com.dinaraparanid.prima.mvvmp.view_models.GTMSetStartPropertiesViewModel
import com.dinaraparanid.prima.mvvmp.view.dialogs.PrimaReleaseDialogFragment
import com.dinaraparanid.prima.mvvmp.view_models.*
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.web.github.ReleaseInfo
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module

private val paramsModule = module {
    singleOf(Params::instance)
    singleOf(StorageUtil::instance)
}

private val defaultMVVMPModule = module {
    factoryOf(::BasePresenter)
}

// --------------------------------- Dialogs ---------------------------------

private val afterSaveRingtoneModule = module {
    viewModelOf(::AfterSaveRingtoneViewModel)
    singleOf(::AfterSaveRingtoneUIHandler)
}

private val inputDialogModule = module {
    factory { (textType: Int, maxLength: Int) -> InputDialogPresenter(textType, maxLength) }

    viewModel { (textType: Int, maxLength: Int) ->
        InputDialogViewModel(get { parametersOf(textType, maxLength) })
    }
}

private val colorPickerModule = module {
    factoryOf(::ColorPickerPresenter)
    factoryOf(::ColorPickerUIHandler)
}

private val trimmedAudioFileSaveModule = module {
    factory { (initialFileName: String) -> TrimmedAudioFileSavePresenter(initialFileName) }

    singleOf(::TrimmedAudioFileSaveUIHandler)

    viewModel { (initialFileName: String) ->
        TrimmedAudioFileSaveViewModel(get { parametersOf(initialFileName) })
    }
}

private val gtmSetStartPropertiesModule = module {
    factoryOf(::GTMSetStartPlaybackPresenter)
    viewModelOf(::GTMSetStartPlaybackViewModel)
    singleOf(::GTMSetStartPlaybackUIHandler)

    factoryOf(::GTMSetStartPropertiesPresenter)
    viewModelOf(::GTMSetStartPropertiesViewModel)
    singleOf(::GTMSetStartPropertiesUIHandler)
}

private val recordParamsModule = module {
    factoryOf(::RecordParamsPresenter)
    viewModelOf(::RecordParamsViewModel)
    singleOf(::RecordParamsUIHandler)
}

private val primaReleaseModule = module {
    factory { (releaseInfo: ReleaseInfo, target: PrimaReleaseDialogFragment.Target) ->
        PrimaReleasePresenter(releaseInfo, target)
    }

    viewModel { (releaseInfo: ReleaseInfo, target: PrimaReleaseDialogFragment.Target) ->
        PrimaReleaseViewModel(get { parametersOf(releaseInfo, target) })
    }

    singleOf(::PrimaReleaseUIHandler)
}

private val trackSearchParamsModule = module {
    factory { (trackTitle: String, artistName: String) ->
        TrackSearchParamsPresenter(trackTitle, artistName)
    }

    viewModel { (trackTitle: String, artistName: String) ->
        TrackSearchParamsViewModel(get { parametersOf(trackTitle, artistName) })
    }

    singleOf(::TrackSearchParamsUIHandler)
}

private val dialogsModule = module {
    includes(
        afterSaveRingtoneModule,
        inputDialogModule,
        colorPickerModule,
        trimmedAudioFileSaveModule,
        gtmSetStartPropertiesModule,
        recordParamsModule,
        primaReleaseModule,
        trackSearchParamsModule
    )

    singleOf(::AfterSaveTimeUIHandler)
    singleOf(::CheckHiddenPasswordUIHandler)
    singleOf(::CreateHiddenPasswordUIHandler)
    singleOf(::NewFolderUIHandler)
    singleOf(::RenamePlaylistUIHandler)
    singleOf(::SleepUIHandler)
}

// --------------------------------- Fragments ---------------------------------

private val trackListModule = module {
    factory { (initialNumberOfTracks: Int, recyclerViewBottomMargin: Int) ->
        TrackListPresenter(initialNumberOfTracks, recyclerViewBottomMargin)
    }

    viewModel { (initialNumberOfTracks: Int, recyclerViewBottomMargin: Int) ->
        TrackListViewModel(get { parametersOf(initialNumberOfTracks, recyclerViewBottomMargin) })
    }
}

private val trackItemModule = module {
    factory { (trackList: List<Track>, curItemIndex: Int) ->
        TrackItemPresenter(
            trackList = trackList,
            curItemIndex = curItemIndex,
            currentPlayingTrackPathFlow = get(named(MainApplication.CURRENT_PLAYING_TRACK_PATH_FLOW))
        )
    }

    singleOf(::TrackItemUIHandler)
}

private val fragmentsModule = module {
    includes(trackListModule, trackItemModule)

    factory { (recyclerViewBottomMargin: Int) ->
        MainActivityListPresenter(recyclerViewBottomMargin)
    }
}

// --------------------------------- Constants and Utils ---------------------------------

private val constantsModule = module {
    factory(named(PrimaReleasePresenter.NEW_VERSION_STR_RES)) {
        androidContext().resources.getString(R.string.new_version)
    }

    factory(named(PrimaReleasePresenter.VERSION_STR_RES)) {
        androidContext().resources.getString(R.string.version)
    }

    factory(named(Track.UNKNOWN_TRACK)) {
        androidContext().resources.getString(R.string.unknown_track)
    }

    factory(named(Track.UNKNOWN_ARTIST)) {
        androidContext().resources.getString(R.string.unknown_artist)
    }

    factory(named(Track.UNKNOWN_ALBUM)) {
        androidContext().resources.getString(R.string.unknown_album)
    }
}

private inline val Scope.mainApplication
    get() = androidApplication() as MainApplication

private val utilsModule = module {
    factory(named(MainApplication.CURRENT_PLAYING_TRACK_PATH_FLOW)) {
        mainApplication.currentPlayingTrackPathFlow
    }
}

private val constantsAndUtilsModule = module {
    includes(constantsModule, utilsModule)
}

@JvmField
val appModule = module {
    includes(
        paramsModule,
        defaultMVVMPModule,
        dialogsModule,
        fragmentsModule,
        constantsAndUtilsModule
    )
}