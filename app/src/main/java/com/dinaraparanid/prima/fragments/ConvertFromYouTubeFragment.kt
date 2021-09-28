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
import com.dinaraparanid.prima.databinding.FragmentConvertFromYouTubeBinding
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.viewmodels.mvvm.ConvertFromYouTubeViewModel

/**
 * [AbstractFragment] to convert and download audio from YouTube
 */

class ConvertFromYouTubeFragment : AbstractFragment<FragmentConvertFromYouTubeBinding>(), Rising {
    override var binding: FragmentConvertFromYouTubeBinding? = null

    internal companion object {
        internal const val Broadcast_ADD_TRACK_TO_QUEUE = "add_track_to_queue"
        internal const val TRACK_URL_ARG = "track_url"
    }

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
        ).apply { viewModel = ConvertFromYouTubeViewModel(pasteUrlEdit, requireActivity()) }

        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        return binding!!.root
    }

    override fun up(): Unit = (requireActivity() as MainActivity).let { act ->
        if (!act.upped)
            binding!!.convertYoutubeLayout.layoutParams =
                (binding!!.convertYoutubeLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = act.playingToolbarHeight + 250
                }
    }
}