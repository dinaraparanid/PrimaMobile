package com.dinaraparanid.prima.utils.polymorphism.fragments

import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.utils.AsyncCondVar
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Ancestor for all [MainActivity] fragments.
 * Deals with main label changes
 */

interface MainActivityFragment {
    val isMainLabelInitialized: AtomicBoolean
    val awaitMainLabelInitCondition: AsyncCondVar
    val mainLabelCurText: AtomicReference<String>
}

internal class MainActivityFragmentImpl : MainActivityFragment {
    override val isMainLabelInitialized = AtomicBoolean()
    override val awaitMainLabelInitCondition = AsyncCondVar()
    override val mainLabelCurText: AtomicReference<String> = AtomicReference("")
}

/**
 * Asynchronous version of [setMainLabelInitializedSync]
 * Should be called immediately after
 * initializing fragment's main label in AbstractActivity.onCreate
 */

internal suspend fun MainActivityFragment.setMainLabelInitializedAsync() {
    isMainLabelInitialized.set(true)
    awaitMainLabelInitCondition.openAsync()
}

/**
 * Should be called immediately after
 * initializing fragment's main label in AbstractActivity.onCreate
 */

internal fun MainActivityFragment.setMainLabelInitializedSync() {
    isMainLabelInitialized.set(true)
    awaitMainLabelInitCondition.open()
}

internal fun <T> T.setMainActivityMainLabel()
        where T : MainActivityFragment,
              T : AbstractFragment<*, MainActivity> = fragmentActivity.runOnIOThread {
    while (!fragmentActivity.isBindingInitialized)
        fragmentActivity.awaitBindingInitCondition.blockAsync()

    launch(Dispatchers.Main) {
        fragmentActivity.mainLabelCurText = mainLabelCurText.get()
    }
}