package com.dinaraparanid.prima.utils.polymorphism.fragments

import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.utils.AsyncCondVar
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface MainActivityFragment {
    var isMainLabelInitialized: Boolean
    val awaitMainLabelInitCondition: AsyncCondVar
    var mainLabelCurText: String
}

/**
 * Should be called immediately after
 * initializing fragment's main label in AbstractActivity.onCreate
 */

internal suspend fun MainActivityFragment.setMainLabelInitializedAsync() {
    isMainLabelInitialized = true
    awaitMainLabelInitCondition.openAsync()
}

internal fun <T> T.setMainActivityMainLabel()
        where T : MainActivityFragment,
              T : AbstractFragment<*, MainActivity> = fragmentActivity.runOnIOThread {
    while (!fragmentActivity.isBindingInitialized)
        fragmentActivity.awaitBindingInitCondition.block()

    launch(Dispatchers.Main) {
        fragmentActivity.mainLabelCurText = mainLabelCurText
    }
}