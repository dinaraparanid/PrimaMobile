package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentFaqBinding
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import com.dinaraparanid.prima.utils.polymorphism.Rising

/**
 * Fragment with facts and questions
 */

class FAQFragment : AbstractFragment(), Rising {
    private lateinit var binding: FragmentFaqBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = resources.getString(R.string.faq)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_faq, container, false)
        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        return binding.root
    }

    override fun up() {
        val act = requireActivity() as MainActivity
        if (!act.upped)
            binding.faqLayout.layoutParams =
                (binding.faqLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = act.playingToolbarHeight
                }
    }
}