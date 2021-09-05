package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentConvertFromYouTubeBinding
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import com.dinaraparanid.prima.viewmodels.mvvm.DownloadFromYouTubeViewModel

/**
 * [AbstractFragment] to convert and download audio from YouTube
 */

class ConvertFromYouTubeFragment : AbstractFragment<FragmentConvertFromYouTubeBinding>() {
    override var binding: FragmentConvertFromYouTubeBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = resources.getString(R.string.convert_from_youtube_to_mp3)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentConvertFromYouTubeBinding>(
            inflater,
            R.layout.fragment_convert_from_you_tube,
            container,
            false
        ).apply { viewModel = DownloadFromYouTubeViewModel(pasteUrlEdit, requireActivity()) }

        return binding!!.root
    }
}