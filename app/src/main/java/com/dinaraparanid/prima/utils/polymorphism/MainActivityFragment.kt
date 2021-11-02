package com.dinaraparanid.prima.utils.polymorphism

import android.os.Bundle
import androidx.databinding.ViewDataBinding
import com.dinaraparanid.prima.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.concurrent.withLock

/**
 * Fragment of [MainActivity].
 * It is needed to set main label's text
 */

abstract class MainActivityFragment<B: ViewDataBinding> : AbstractFragment<B, MainActivity>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragmentActivity.runOnWorkerThread {
            fragmentActivity.awaitBindingInitLock.withLock {
                while (!fragmentActivity.isBindingInitialized)
                    fragmentActivity.awaitBindingInitCondition.await()

                launch(Dispatchers.Main) {
                    fragmentActivity.mainLabelCurText = mainLabelCurText
                }
            }
        }
    }
}