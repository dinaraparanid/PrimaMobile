package com.dinaraparanid.prima.utils.polymorphism

import android.os.Bundle
import android.os.ConditionVariable
import android.view.Menu
import android.view.MenuInflater
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.lifecycleScope
import com.dinaraparanid.prima.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Fragment of [MainActivity].
 * It is needed to set main label's text
 */

abstract class MainActivitySimpleFragment<B : ViewDataBinding> :
    AbstractFragment<B, MainActivity>(),
    MainActivityFragment {
    final override var isMainLabelInitialized = false
    final override val awaitMainLabelInitCondition = ConditionVariable()
    final override lateinit var mainLabelCurText: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        lifecycleScope.launch(Dispatchers.Default) {
            val condition = ConditionVariable()

            while (activity == null)
                condition.block(100)

            while (!fragmentActivity.isBindingInitialized)
                fragmentActivity.awaitBindingInitCondition.block()

            launch(Dispatchers.Main) {
                fragmentActivity.mainLabelCurText = mainLabelCurText
            }
        }
    }

    final override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        fragmentActivity.run {
            lifecycleScope.launch {
                while (!isMainLabelInitialized)
                    awaitMainLabelInitCondition.block()

                launch(Dispatchers.Main) {
                    mainLabelCurText = this@MainActivitySimpleFragment.mainLabelCurText
                }
            }

            currentFragment = WeakReference(this@MainActivitySimpleFragment)
        }
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
        awaitMainLabelInitCondition.open()
    }
}