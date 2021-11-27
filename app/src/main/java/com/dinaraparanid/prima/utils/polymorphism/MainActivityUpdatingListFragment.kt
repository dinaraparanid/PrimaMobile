package com.dinaraparanid.prima.utils.polymorphism

import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.utils.Params
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.Serializable
import java.lang.ref.WeakReference
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** [UpdatingListFragment] for MainActivity's list fragment with [updateUIAsync] */

abstract class MainActivityUpdatingListFragment<T, A, VH, B> :
    UpdatingListFragment<MainActivity, T, A, VH, B>(),
    MainActivityFragment,
    Rising
        where T : Serializable,
              VH : RecyclerView.ViewHolder,
              A : AsyncListDifferAdapter<T, VH>,
              B : ViewDataBinding {
    final override var isMainLabelInitialized = false
    final override val awaitMainLabelInitLock: Lock = ReentrantLock()
    final override val awaitMainLabelInitCondition: Condition = awaitMainLabelInitLock.newCondition()

    final override lateinit var mainLabelOldText: String
    final override lateinit var mainLabelCurText: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setMainActivityMainLabel()
    }

    override fun onStop() {
        fragmentActivity.mainLabelCurText = mainLabelOldText
        super.onStop()
    }

    override fun onResume() {
        super.onResume()

        fragmentActivity.run {
            lifecycleScope.launch {
                awaitMainLabelInitLock.withLock {
                    while (!isMainLabelInitialized)
                        awaitMainLabelInitCondition.await()

                    launch(Dispatchers.Main) {
                        mainLabelCurText = this@MainActivityUpdatingListFragment.mainLabelCurText
                    }
                }
            }

            currentFragment = WeakReference(this@MainActivityUpdatingListFragment)
        }
    }

    final override fun onDestroy() {
        super.onDestroy()
        isMainLabelInitialized = false
    }

    final override fun up() {
        if (!fragmentActivity.isUpped)
            recyclerView!!.layoutParams =
                (recyclerView!!.layoutParams as ConstraintLayout.LayoutParams).apply {
                    bottomMargin = Params.PLAYING_TOOLBAR_HEIGHT
                }
    }

    protected fun setMainLabelInitialized() {
        isMainLabelInitialized = true
        awaitMainLabelInitLock.withLock(awaitMainLabelInitCondition::signal)
    }
}