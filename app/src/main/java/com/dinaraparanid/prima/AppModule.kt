package com.dinaraparanid.prima

import com.dinaraparanid.prima.mvvmp.presenters.*
import com.dinaraparanid.prima.mvvmp.ui_handlers.*
import com.dinaraparanid.prima.mvvmp.view_models.*
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val appModule = module {
    singleOf(Params::instance)
    singleOf(StorageUtil::instance)
    factoryOf(::BasePresenter)

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
    viewModelOf(::GTMSetStartPlaybackViewModel)
    singleOf(::GTMSetStartPlaybackUIHandler)

    factoryOf(::GTMSetStartPropertiesPresenter)
    viewModelOf(::GTMSetStartPropertiesViewModel)
    singleOf(::GTMSetStartPropertiesUIHandler)

    singleOf(::NewFolderUIHandler)

    factory { RecordParamsPresenter() }
    viewModel { RecordParamsViewModel() }
    singleOf(::RecordParamsUIHandler)
}