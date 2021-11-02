package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentFaqBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.polymorphism.MainActivityFragment
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel

/**
 * Fragment with facts and questions
 */

class FAQFragment : MainActivityFragment<FragmentFaqBinding>(), Rising {
    override var binding: FragmentFaqBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = resources.getString(R.string.faq)
        setMainLabelInitialized()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate<FragmentFaqBinding>(inflater, R.layout.fragment_faq, container, false)
            .apply { viewModel = ViewModel() }

        if (application.playingBarIsVisible) up()
        return binding!!.root
    }

    override fun up() {
        if (!fragmentActivity.isUpped)
            binding!!.faqLayout.layoutParams =
                (binding!!.faqLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = Params.PLAYING_TOOLBAR_HEIGHT
                }
    }
}