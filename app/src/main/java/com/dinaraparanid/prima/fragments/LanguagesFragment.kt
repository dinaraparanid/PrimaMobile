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
import com.dinaraparanid.prima.databinding.FragmentLanguagesBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel

/**
 * Fragment for choosing languages
 */

class LanguagesFragment : AbstractFragment<FragmentLanguagesBinding>(), Rising {
    override var binding: FragmentLanguagesBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = resources.getString(R.string.language)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_languages, container, false)
        val binding = binding!!
        binding.viewModel = ViewModel()

        arrayOf(
            binding.english,
            binding.arabian,
            binding.belarusian,
            binding.bulgarian,
            binding.german,
            binding.greek,
            binding.spanish,
            binding.french,
            binding.italian,
            binding.japanese,
            binding.korean,
            binding.mongolian,
            binding.norwegian,
            binding.polish,
            binding.portuguese,
            binding.russian,
            binding.swedish,
            binding.turkish,
            binding.ukrainian,
            binding.chinese
        ).forEachIndexed { ind, b ->
            b.setOnClickListener {
                Params.instance.changeLang(requireContext(), ind)
            }
        }

        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        return binding.root
    }

    override fun up() {
        val act = requireActivity() as MainActivity

        if (!act.upped)
            binding!!.languagesLayout.layoutParams =
                (binding!!.languagesLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = act.playingToolbarHeight
                }
    }
}