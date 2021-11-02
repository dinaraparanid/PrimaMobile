package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentMp3ConverterBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import com.dinaraparanid.prima.utils.polymorphism.MainActivityFragment
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.viewmodels.mvvm.MP3ConvertViewModel
import java.lang.ref.WeakReference

/**
 * [AbstractFragment] to convert and download audio
 */

class MP3ConverterFragment : MainActivityFragment<FragmentMp3ConverterBinding>(), Rising {
    override var binding: FragmentMp3ConverterBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = resources.getString(R.string.mp3_converter)
        setMainLabelInitialized()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentMp3ConverterBinding>(
            inflater,
            R.layout.fragment_mp3_converter,
            container,
            false
        ).apply { viewModel = MP3ConvertViewModel(pasteUrlEdit, WeakReference(requireActivity())) }

        if (application.playingBarIsVisible) up()
        return binding!!.root
    }

    override fun up() {
        if (!fragmentActivity.isUpped)
            binding!!.convertYoutubeLayout.layoutParams =
                (binding!!.convertYoutubeLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = Params.PLAYING_TOOLBAR_HEIGHT + 250
                }
    }
}