package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import com.dinaraparanid.prima.utils.polymorphism.Rising

/**
 * Fragment with facts and questions
 */

class FAQFragment : AbstractFragment(), Rising {
    private lateinit var faqLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = resources.getString(R.string.faq)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_faq, container, false)

        faqLayout = view
            .findViewById<NestedScrollView>(R.id.faq_nested)
            .findViewById(R.id.faq_layout)

        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        return view
    }

    override fun up() {
        if (!(requireActivity() as MainActivity).upped)
            faqLayout.layoutParams = (faqLayout.layoutParams as FrameLayout.LayoutParams).apply {
                bottomMargin = 210
            }
    }
}