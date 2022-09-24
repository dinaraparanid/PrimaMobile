package com.dinaraparanid.prima.utils.polymorphism.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.lifecycleScope
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.utils.AsyncCondVar
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Fragment of [MainActivity].
 * It is needed to set main label's text
 */

abstract class MainActivitySimpleFragment<B : ViewDataBinding> :
    AbstractFragment<B, MainActivity>(),
    MainActivityFragment by MainActivityFragmentImpl(),
    MenuProviderFragment {
    final override val menuProvider = defaultMenuProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.Default) {
            val condition = AsyncCondVar()

            while (activity == null)
                condition.blockAsync(100)

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
            runOnUIThread {
                while (!isMainLabelInitialized.get())
                    awaitMainLabelInitCondition.blockAsync()

                launch(Dispatchers.Main) {
                    mainLabelCurText = this@MainActivitySimpleFragment.mainLabelCurText.get()
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
        isMainLabelInitialized.set(false)
    }

    /**
     * !! FOR MAIN ACTIVITY FRAGMENTS ONLY !!
     * Asynchronous version of [setMainLabelInitializedSync]
     * Should be called immediately after
     * initializing fragment's main label in [onCreate]
     */

    protected suspend fun setMainLabelInitializedAsync() {
        isMainLabelInitialized.set(true)
        awaitMainLabelInitCondition.openAsync()
    }

    /**
     * !! FOR MAIN ACTIVITY FRAGMENTS ONLY !!
     * Should be called immediately after
     * initializing fragment's main label in [onCreate]
     */

    protected fun setMainLabelInitializedSync() {
        isMainLabelInitialized.set(true)
        awaitMainLabelInitCondition.open()
    }
}