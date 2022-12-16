package com.dinaraparanid.prima

import com.dinaraparanid.prima.mvvmp.presenters.*
import com.dinaraparanid.prima.mvvmp.ui_handlers.*
import com.dinaraparanid.prima.mvvmp.view.dialogs.PrimaReleaseDialogFragment
import com.dinaraparanid.prima.mvvmp.view_models.*
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.web.github.ReleaseInfo
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    singleOf(Params::instance)
    singleOf(StorageUtil::instance)

    factoryOf(::BasePresenter)
    viewModelOf(::DefaultViewModel)

    viewModelOf(::AfterSaveRingtoneViewModel)
    singleOf(::AfterSaveRingtoneUIHandler)

    factory { (textType: Int, maxLength: Int) -> InputDialogPresenter(textType, maxLength) }

    viewModel { (textType: Int, maxLength: Int) ->
        InputDialogViewModel(get { parametersOf(textType, maxLength) })
    }

    singleOf(::AfterSaveTimeUIHandler)

    singleOf(::CheckHiddenPasswordUIHandler)

    factoryOf(::ColorPickerPresenter)
    factoryOf(::ColorPickerUIHandler)

    singleOf(::CreateHiddenPasswordUIHandler)

    factory { (initialFileName: String) -> TrimmedAudioFileSavePresenter(initialFileName) }

    singleOf(::TrimmedAudioFileSaveUIHandler)

    viewModel { (initialFileName: String) ->
        TrimmedAudioFileSaveViewModel(get { parametersOf(initialFileName) })
    }

    factoryOf(::GTMSetStartPlaybackPresenter)
    singleOf(::GTMSetStartPlaybackUIHandler)

    factoryOf(::GTMSetStartPropertiesPresenter)
    singleOf(::GTMSetStartPropertiesUIHandler)

    singleOf(::NewFolderUIHandler)

    factoryOf(::RecordParamsPresenter)
    viewModelOf(::RecordParamsViewModel)
    singleOf(::RecordParamsUIHandler)

    factory(named(PrimaReleasePresenter.NEW_VERSION_STR_RES)) {
        androidContext().resources.getString(R.string.new_version)
    }

    factory(named(PrimaReleasePresenter.VERSION_STR_RES)) {
        androidContext().resources.getString(R.string.version)
    }

    factory { (releaseInfo: ReleaseInfo, target: PrimaReleaseDialogFragment.Target) ->
        PrimaReleasePresenter(releaseInfo, target)
    }

    viewModel { (releaseInfo: ReleaseInfo, target: PrimaReleaseDialogFragment.Target) ->
        PrimaReleaseViewModel(get { parametersOf(releaseInfo, target) })
    }

    singleOf(::PrimaReleaseUIHandler)
    singleOf(::RenamePlaylistUIHandler)
    singleOf(::SleepUIHandler)
}