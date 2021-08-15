package com.dinaraparanid.prima.utils.polymorphism

import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.fragments.EqualizerFragment
import kotlin.reflect.KClass

/**
 * Ancestor [Fragment] for all my fragments.
 * Manipulates with app's main label
 */

abstract class AbstractFragment : Fragment() {
    protected lateinit var mainLabelOldText: String
    protected lateinit var mainLabelCurText: String

    internal companion object {
        internal const val MAIN_LABEL_OLD_TEXT_KEY: String = "main_label_old_text"
        internal const val MAIN_LABEL_CUR_TEXT_KEY: String = "main_label_cur_text"

        /**
         * Creates instances of fragments
         * with only main label params.
         *
         * @param mainLabelOldText current main label text
         * @param mainLabelCurText text to show when fragment' ll be active
         * @param clazz [KClass] of fragment (::class)
         */

        @JvmStatic
        fun defaultInstance(
            mainLabelOldText: String,
            mainLabelCurText: String,
            clazz: KClass<out AbstractFragment>
        ): Fragment = clazz.constructors.first().call().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
            }
        }
    }

    override fun onStop() {
        (requireActivity() as MainActivity).binding.mainLabel.text = mainLabelOldText
        super.onStop()
    }

    override fun onResume() {
        super.onResume()

        (requireActivity() as MainActivity).run {
            var label: String? = null

            while (label == null) {
                label = try {
                    mainLabelCurText
                } catch (e: Exception) {
                    null
                }
            }

            binding.mainLabel.text = label
            currentFragment = this@AbstractFragment
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