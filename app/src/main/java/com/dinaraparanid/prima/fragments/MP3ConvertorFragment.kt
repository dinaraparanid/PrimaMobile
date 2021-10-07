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
import com.dinaraparanid.prima.databinding.FragmentMp3ConvertBinding
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.viewmodels.mvvm.MP3ConvertViewModel

/**
 * [AbstractFragment] to convert and download audio
 */

class MP3ConvertorFragment : AbstractFragment<FragmentMp3ConvertBinding>(), Rising {
    override var binding: FragmentMp3ConvertBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = resources.getString(R.string.mp3_converter)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentMp3ConvertBinding>(
            inflater,
            R.layout.fragment_mp3_convert,
            container,
            false
        ).apply { viewModel = MP3ConvertViewModel(pasteUrlEdit, requireActivity()) }

        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        return binding!!.root
    }

    override fun up(): Unit = (requireActivity() as MainActivity).let { act ->
        if (!act.isUpped)
            binding!!.convertYoutubeLayout.layoutParams =
                (binding!!.convertYoutubeLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = act.playingToolbarHeight + 250
                }
    }
}