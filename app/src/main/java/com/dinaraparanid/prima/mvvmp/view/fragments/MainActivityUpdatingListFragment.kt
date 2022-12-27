package com.dinaraparanid.prima.mvvmp.view.fragments

import android.os.Parcelable
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.polymorphism.AsyncListDifferAdapter
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** [UpdatingListFragment] for MainActivity's list fragment with [updateUIAsync] */

abstract class MainActivityUpdatingListFragment<T, A, VH, B> :
    UpdatingListFragment<MainActivity, T, A, VH, B>(),
    MainActivityFragment,
    MenuProviderFragment,
    Rising
        where T : Parcelable,
              VH : RecyclerView.ViewHolder,
              A : AsyncListDifferAdapter<T, VH>,
              B : ViewDataBinding {
    final override val menuProvider = defaultMenuProvider

    override fun onResume() {
        super.onResume()
        requireActivity().addMenuProvider(menuProvider)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) = fragmentActivity.run {
        runOnUIThread {
            while (!isMainLabelInitialized.get())
                awaitMainLabelInitCondition.blockAsync()

            launch(Dispatchers.Main) {
                mainLabelText = this@MainActivityUpdatingListFragment.mainLabelText.get()
            }
        }

        currentFragment = WeakReference(this@MainActivityUpdatingListFragment)
    }

    final override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) =
        super.onCreateOptionsMenu(menu, inflater)

    final override fun onOptionsItemSelected(item: MenuItem) =
        super.onOptionsItemSelected(item)

    final override fun onPause() {
        super.onPause()
        requireActivity().removeMenuProvider(menuProvider)
    }

    final override fun onDestroy() {
        super.onDestroy()
        isMainLabelInitialized.set(false)
        Glide.get(requireContext()).clearMemory()
    }

    final override fun up() {
        runOnUIThread {
            if (!fragmentActivity.isUpped) {
                while (recyclerView == null)
                    delay(100)

                recyclerView!!.layoutParams =
                    (recyclerView!!.layoutParams as ConstraintLayout.LayoutParams).apply {
                        bottomMargin = Params.PLAYING_TOOLBAR_HEIGHT
                    }
            }
        }
    }
}