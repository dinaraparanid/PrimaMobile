package com.dinaraparanid.prima

import com.dinaraparanid.prima.mvvmp.presenters.*
import com.dinaraparanid.prima.mvvmp.ui_handlers.*
import com.dinaraparanid.prima.mvvmp.view.dialogs.CreateHiddenPasswordDialog
import com.dinaraparanid.prima.mvvmp.view_models.AfterSaveRingtoneViewModel
import com.dinaraparanid.prima.mvvmp.view_models.GTMSetStartPropertiesViewModel
import com.dinaraparanid.prima.mvvmp.view_models.InputDialogViewModel
import com.dinaraparanid.prima.mvvmp.view_models.TrimmedAudioFileSaveViewModel
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import kotlinx.coroutines.channels.Channel
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

    factory { (updateAutosaveTimeButtonChannel: Channel<Unit>) ->
        AfterSaveTimeUIHandler(updateAutosaveTimeButtonChannel)
    }

    factory { (passwordHash: Int, showHiddenFragmentChannel: Channel<Unit>) ->
        CheckHiddenPasswordUIHandler(passwordHash, showHiddenFragmentChannel)
    }

    factoryOf(::ColorPickerPresenter)
    factoryOf(::ColorPickerUIHandler)

    factory { (target: CreateHiddenPasswordDialog.Target, showHiddenFragmentChannel: Channel<Unit>) ->
        CreateHiddenPasswordUIHandler(target, showHiddenFragmentChannel)
    }

    factory { (initialFileName: String) -> TrimmedAudioFileSavePresenter(initialFileName) }

    singleOf(::TrimmedAudioFileSaveUIHandler)

    viewModel { (initialFileName: String) ->
        TrimmedAudioFileSaveViewModel(get { parametersOf(initialFileName) })
    }

    factoryOf(::GTMSetStartPropertiesPresenter)
    viewModelOf(::GTMSetStartPropertiesViewModel)
    singleOf(::GTMSetStartPropertiesUIHandler)
}