package com.dinaraparanid.prima.utils.polymorphism

import com.dinaraparanid.prima.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import kotlin.concurrent.withLock

interface MainActivityFragment {
    var isMainLabelInitialized: Boolean
    val awaitMainLabelInitLock: Lock
    val awaitMainLabelInitCondition: Condition

    var mainLabelOldText: String
    var mainLabelCurText: String
}

/**
 * Should be called immediately after
 * initializing fragment's main label in [AbstractActivity.onCreate]
 */

internal fun MainActivityFragment.setMainLabelInitialized() {
    isMainLabelInitialized = true
    awaitMainLabelInitLock.withLock(awaitMainLabelInitCondition::signal)
}

internal fun <T> T.setMainActivityMainLabel()
    where T : MainActivityFragment,
          T: AbstractFragment<*, MainActivity> = fragmentActivity.runOnWorkerThread {
    fragmentActivity.awaitBindingInitLock.withLock {
        while (!fragmentActivity.isBindingInitialized)
            fragmentActivity.awaitBindingInitCondition.await()

        launch(Dispatchers.Main) {
            fragmentActivity.mainLabelCurText = mainLabelCurText
        }
    }
}