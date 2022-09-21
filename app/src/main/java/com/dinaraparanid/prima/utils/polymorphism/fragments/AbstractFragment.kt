package com.dinaraparanid.prima.utils.polymorphism.fragments

import android.os.Bundle
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.utils.polymorphism.AbstractActivity
import java.lang.ref.WeakReference
import kotlin.reflect.KClass

/**
 * Ancestor [Fragment] for all my fragments.
 * [MainActivity] fragments manipulates with app's main label
 */

@Suppress("UNCHECKED_CAST")
abstract class AbstractFragment<B : ViewDataBinding, A : AbstractActivity> : Fragment() {
    protected abstract var binding: B?

    internal inline val fragmentActivity
        get() = requireActivity() as A

    protected inline val application
        get() = requireActivity().application as MainApplication

    internal companion object {
        internal const val MAIN_LABEL_CUR_TEXT_KEY = "main_label_cur_text"

        /**
         * Creates instances of fragments
         * with only main label params.
         *
         * @param mainLabelCurText text to show when fragment' ll be active.
         * Can be null if fragment has constant title (like FAQ)
         * @param clazz [KClass] of fragment (::class)
         */

        @JvmStatic
        internal fun <B : ViewDataBinding, A : AbstractActivity, T : AbstractFragment<B, A>> defaultInstance(
            mainLabelCurText: String?,
            clazz: KClass<out T>
        ): T = clazz.constructors.first().call().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onResume() {
        super.onResume()

        if (this !is ViewPagerFragment)
            fragmentActivity.currentFragment = WeakReference(this)
    }
}