package com.dinaraparanid.prima.utils.polymorphism

import android.content.res.Configuration
import android.os.Bundle
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.fragments.EqualizerFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.reflect.KClass

/**
 * Ancestor [Fragment] for all my fragments.
 * [MainActivity] fragments manipulates with app's main label
 */

abstract class AbstractFragment<B : ViewDataBinding, A : AbstractActivity> : Fragment() {
    private var isMainLabelInitialized = false
    private val awaitMainLabelInitLock: Lock = ReentrantLock()
    private val awaitMainLabelInitCondition = awaitMainLabelInitLock.newCondition()

    protected lateinit var mainLabelOldText: String
    protected lateinit var mainLabelCurText: String
    protected abstract var binding: B?

    protected inline val fragmentActivity
        get() = requireActivity() as A

    protected inline val application
        get() = requireActivity().application as MainApplication

    internal companion object {
        internal const val MAIN_LABEL_OLD_TEXT_KEY = "main_label_old_text"
        internal const val MAIN_LABEL_CUR_TEXT_KEY = "main_label_cur_text"

        /**
         * Creates instances of fragments
         * with only main label params.
         *
         * @param mainLabelOldText current main label text
         * @param mainLabelCurText text to show when fragment' ll be active.
         * Can be null if fragment has constant title (like FAQ)
         * @param clazz [KClass] of fragment (::class)
         */

        @JvmStatic
        internal fun <B : ViewDataBinding, A : AbstractActivity, T : AbstractFragment<B, A>> defaultInstance(
            mainLabelOldText: String,
            mainLabelCurText: String?,
            clazz: KClass<out T>
        ): T = clazz.constructors.first().call().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
            }
        }
    }

    override fun onStop() {
        if (fragmentActivity is MainActivity)
            (fragmentActivity as MainActivity).mainLabelCurText = mainLabelOldText

        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onResume() {
        super.onResume()

        fragmentActivity.run {
            if (this is MainActivity)
                lifecycleScope.launch {
                    awaitMainLabelInitLock.withLock {
                        while (!isMainLabelInitialized)
                            awaitMainLabelInitCondition.await()
                        launch(Dispatchers.Main) { mainLabelCurText = this@AbstractFragment.mainLabelCurText }
                    }
                }

            currentFragment = WeakReference(this@AbstractFragment)
        }

        if (this is EqualizerFragment &&
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
            (resources.configuration.screenLayout.and(Configuration.SCREENLAYOUT_SIZE_MASK) !=
                    Configuration.SCREENLAYOUT_SIZE_LARGE ||
                    resources.configuration.screenLayout.and(Configuration.SCREENLAYOUT_SIZE_MASK) !=
                    Configuration.SCREENLAYOUT_SIZE_XLARGE)
        ) {
            requireActivity().supportFragmentManager.popBackStack()
            return
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isMainLabelInitialized = false
    }

    /**
     * Should be called immediately after
     * initializing fragment's main label in [onCreate]
     */

    protected fun setMainLabelInitialized() {
        isMainLabelInitialized = true
        awaitMainLabelInitLock.withLock(awaitMainLabelInitCondition::signal)
    }
}