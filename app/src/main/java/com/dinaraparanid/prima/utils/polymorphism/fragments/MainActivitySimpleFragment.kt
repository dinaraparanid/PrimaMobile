package com.dinaraparanid.prima.utils.polymorphism.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.lifecycleScope
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.utils.AsyncCondVar
import com.dinaraparanid.prima.utils.polymorphism.runOnWorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.lang.ref.WeakReference

/**
 * Fragment of [MainActivity].
 * It is needed to set main label's text
 */

abstract class MainActivitySimpleFragment<B : ViewDataBinding> :
    AbstractFragment<B, MainActivity>(),
    MainActivityFragment,
    MenuProviderFragment {
    final override var isMainLabelInitialized = false
    final override val awaitMainLabelInitCondition = AsyncCondVar()
    final override lateinit var mainLabelCurText: String
    final override val menuProvider = defaultMenuProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.Default) {
            val condition = AsyncCondVar()

            while (activity == null) withTimeout(100) {
                condition.blockAsync()
            }

            while (!fragmentActivity.isBindingInitialized)
                fragmentActivity.awaitBindingInitCondition.blockAsync()

            launch(Dispatchers.Main) {
                if (this@MainActivitySimpleFragment !is ViewPagerFragment)
                    fragmentActivity.currentFragment =
                        WeakReference(this@MainActivitySimpleFragment)
            }
        }
    }

    final override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        fragmentActivity.run {
            runOnWorkerThread {
                while (!isMainLabelInitialized)
                    awaitMainLabelInitCondition.block()

                launch(Dispatchers.Main) {
                    mainLabelCurText = this@MainActivitySimpleFragment.mainLabelCurText
                }
            }
        }
    }

    final override fun onMenuItemSelected(menuItem: MenuItem) = true

    override fun onResume() {
        super.onResume()
        requireActivity().addMenuProvider(menuProvider)
    }

    final override fun onPause() {
        super.onPause()
        requireActivity().removeMenuProvider(menuProvider)
    }

    final override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) =
        super.onCreateOptionsMenu(menu, inflater)

    override fun onDestroy() {
        super.onDestroy()
        isMainLabelInitialized = false
    }

    /**
     * !! FOR MAIN ACTIVITY FRAGMENTS ONLY !!
     * Asynchronous version of [setMainLabelInitializedSync]
     * Should be called immediately after
     * initializing fragment's main label in [onCreate]
     */

    protected suspend fun setMainLabelInitializedAsync() {
        isMainLabelInitialized = true
        awaitMainLabelInitCondition.openAsync()
    }

    /**
     * !! FOR MAIN ACTIVITY FRAGMENTS ONLY !!
     * Should be called immediately after
     * initializing fragment's main label in [onCreate]
     */

    protected fun setMainLabelInitializedSync() {
        isMainLabelInitialized = true
        awaitMainLabelInitCondition.open()
    }
}