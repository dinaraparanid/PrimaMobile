package com.dinaraparanid.prima.utils.polymorphism.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.utils.AsyncCondVar
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.polymorphism.AsyncListDifferAdapter
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.utils.polymorphism.runOnWorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.Serializable
import java.lang.ref.WeakReference

/** [UpdatingListFragment] for MainActivity's list fragment with [updateUIAsync] */

abstract class MainActivityUpdatingListFragment<T, A, VH, B> :
    UpdatingListFragment<MainActivity, T, A, VH, B>(),
    MainActivityFragment,
    MenuProviderFragment,
    Rising
        where T : Serializable,
              VH : RecyclerView.ViewHolder,
              A : AsyncListDifferAdapter<T, VH>,
              B : ViewDataBinding {
    final override var isMainLabelInitialized = false
    final override val awaitMainLabelInitCondition = AsyncCondVar()
    final override lateinit var mainLabelCurText: String
    final override val menuProvider = defaultMenuProvider

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(menuProvider)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        fragmentActivity.run {
            runOnWorkerThread {
                while (!isMainLabelInitialized)
                    awaitMainLabelInitCondition.block()

                launch(Dispatchers.Main) {
                    mainLabelCurText = this@MainActivityUpdatingListFragment.mainLabelCurText
                }
            }

            currentFragment = WeakReference(this@MainActivityUpdatingListFragment)
        }
    }

    final override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) =
        super.onCreateOptionsMenu(menu, inflater)

    final override fun onOptionsItemSelected(item: MenuItem) =
        super.onOptionsItemSelected(item)

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().removeMenuProvider(menuProvider)
    }

    final override fun onDestroy() {
        super.onDestroy()
        isMainLabelInitialized = false
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