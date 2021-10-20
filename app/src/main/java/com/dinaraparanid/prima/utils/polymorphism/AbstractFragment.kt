package com.dinaraparanid.prima.utils.polymorphism

import android.content.res.Configuration
import android.os.Bundle
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.fragments.EqualizerFragment
import java.lang.ref.WeakReference
import kotlin.reflect.KClass

/**
 * Ancestor [Fragment] for all my fragments.
 * Manipulates with app's main label
 */

abstract class AbstractFragment<B : ViewDataBinding> : Fragment() {
    protected lateinit var mainLabelOldText: String
    protected lateinit var mainLabelCurText: String
    protected abstract var binding: B?

    protected inline val mainActivity
        get() = requireActivity() as MainActivity

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
        internal fun <B : ViewDataBinding, T : AbstractFragment<B>> defaultInstance(
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
        mainActivity.mainLabelCurText = mainLabelOldText
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onResume() {
        super.onResume()

        mainActivity.run {
            var label: String? = null

            while (label == null)
                label = try {
                    this@AbstractFragment.mainLabelCurText
                } catch (e: Exception) {
                    null
                }

            mainLabelCurText = label
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
}