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
 * Asynchronous version of [setMainLabelInitializedSync]
 * Should be called immediately after
 * initializing fragment's main label in AbstractActivity.onCreate
 */

internal suspend fun MainActivityFragment.setMainLabelInitializedAsync() {
    isMainLabelInitialized = true
    awaitMainLabelInitCondition.openAsync()
}

/**
 * Should be called immediately after
 * initializing fragment's main label in AbstractActivity.onCreate
 */

internal fun MainActivityFragment.setMainLabelInitializedSync() {
    isMainLabelInitialized = true
    awaitMainLabelInitCondition.open()
}

internal fun <T> T.setMainActivityMainLabel()
        where T : MainActivityFragment,
              T : AbstractFragment<*, MainActivity> = fragmentActivity.runOnIOThread {
    while (!fragmentActivity.isBindingInitialized)
        fragmentActivity.awaitBindingInitCondition.blockAsync()

    launch(Dispatchers.Main) {
        fragmentActivity.mainLabelCurText = mainLabelCurText
    }
}