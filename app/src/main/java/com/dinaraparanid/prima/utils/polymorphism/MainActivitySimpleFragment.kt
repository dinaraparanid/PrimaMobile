package com.dinaraparanid.prima.utils.polymorphism

import android.os.Bundle
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.lifecycleScope
import com.dinaraparanid.prima.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Fragment of [MainActivity].
 * It is needed to set main label's text
 */

abstract class MainActivitySimpleFragment<B: ViewDataBinding> :
    AbstractFragment<B, MainActivity>(),
    MainActivityFragment {
    final override var isMainLabelInitialized = false
    final override val awaitMainLabelInitLock: Lock = ReentrantLock()
    final override val awaitMainLabelInitCondition: Condition = awaitMainLabelInitLock.newCondition()

    final override lateinit var mainLabelOldText: String
    final override lateinit var mainLabelCurText: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.Default) {
            val lock = ReentrantLock()
            val condition = lock.newCondition()

            while (activity == null)
                condition.await(100, TimeUnit.MILLISECONDS)

            fragmentActivity.awaitBindingInitLock.withLock {
                while (!fragmentActivity.isBindingInitialized)
                    fragmentActivity.awaitBindingInitCondition.await()

                launch(Dispatchers.Main) {
                    fragmentActivity.mainLabelCurText = mainLabelCurText
                }
            }
        }
    }

    final override fun onStop() {
        fragmentActivity.mainLabelCurText = mainLabelOldText
        super.onStop()
    }

    final override fun onResume() {
        super.onResume()

        fragmentActivity.run {
            lifecycleScope.launch {
                awaitMainLabelInitLock.withLock {
                    while (!isMainLabelInitialized)
                        awaitMainLabelInitCondition.await()
                    launch(Dispatchers.Main) { mainLabelCurText = this@MainActivitySimpleFragment.mainLabelCurText }
                }
            }

            currentFragment = WeakReference(this@MainActivitySimpleFragment)
        }

        /*if (this is EqualizerFragment &&
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
            (resources.configuration.screenLayout.and(Configuration.SCREENLAYOUT_SIZE_MASK) !=
                    Configuration.SCREENLAYOUT_SIZE_LARGE ||
                    resources.configuration.screenLayout.and(Configuration.SCREENLAYOUT_SIZE_MASK) !=
                    Configuration.SCREENLAYOUT_SIZE_XLARGE)
        ) {
            requireActivity().supportFragmentManager.popBackStack()
            return
        }*/
    }

    override fun onDestroy() {
        super.onDestroy()
        isMainLabelInitialized = false
    }

    /**
     * !! FOR MAIN ACTIVITY FRAGMENTS ONLY !!
     * Should be called immediately after
     * initializing fragment's main label in [onCreate]
     */

    protected fun setMainLabelInitialized() {
        isMainLabelInitialized = true
        awaitMainLabelInitLock.withLock(awaitMainLabelInitCondition::signal)
    }
}