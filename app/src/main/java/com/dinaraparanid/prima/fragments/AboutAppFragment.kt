package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentAboutAppBinding
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import com.dinaraparanid.prima.viewmodels.mvvm.AboutAppViewModel
import java.lang.ref.WeakReference

/**
 * Fragment with app info.
 * It shows current version, how to contact with developer and FAQ
 */

class AboutAppFragment : AbstractFragment<FragmentAboutAppBinding>() {
    override var binding: FragmentAboutAppBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = resources.getString(R.string.about_app)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentAboutAppBinding>(
            inflater,
            R.layout.fragment_about_app,
            container,
            false
        ).apply {
            viewModel = AboutAppViewModel(WeakReference(requireActivity()))

            FAQButton.setOnClickListener {
                requireActivity().supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.fade_in,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.fade_out
                    )
                    .replace(
                        R.id.fragment_container,
                        defaultInstance(
                            mainLabelCurText,
                            resources.getString(R.string.faq),
                            FAQFragment::class
                        )
                    )
                    .addToBackStack(null)
                    .commit()
            }
        }

        return binding!!.root
    }
}