package com.dinaraparanid.prima.utils.polymorphism

import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.MainActivity

abstract class AbstractFragment : Fragment() {
    protected lateinit var mainLabelOldText: String
    protected lateinit var mainLabelCurText: String

    protected companion object {
        const val MAIN_LABEL_OLD_TEXT_KEY: String = "main_label_old_text"
        const val MAIN_LABEL_CUR_TEXT_KEY: String = "main_label_cur_text"
    }

    override fun onStop() {
        (requireActivity() as MainActivity).mainLabel.text = mainLabelOldText
        super.onStop()
    }

    override fun onResume() {
        (requireActivity() as MainActivity).run {
            mainLabel.text = mainLabelCurText
            currentFragment = this@AbstractFragment
        }

        super.onResume()
    }
}