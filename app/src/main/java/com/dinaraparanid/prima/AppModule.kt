package com.dinaraparanid.prima

import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import com.dinaraparanid.prima.mvvmp.presenters.ColorPickerPresenter
import com.dinaraparanid.prima.mvvmp.presenters.InputDialogPresenter
import com.dinaraparanid.prima.mvvmp.ui_handlers.AfterSaveRingtoneUIHandler
import com.dinaraparanid.prima.mvvmp.ui_handlers.AfterSaveTimeUIHandler
import com.dinaraparanid.prima.mvvmp.ui_handlers.CheckHiddenPasswordUIHandler
import com.dinaraparanid.prima.mvvmp.ui_handlers.ColorPickerUIHandler
import com.dinaraparanid.prima.mvvmp.view_models.AfterSaveRingtoneViewModel
import com.dinaraparanid.prima.mvvmp.view_models.InputDialogViewModel
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
}