package com.dinaraparanid.prima.fragments.main_menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentMp3ConverterBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.utils.polymorphism.fragments.AbstractFragment
import com.dinaraparanid.prima.utils.polymorphism.fragments.MainActivitySimpleFragment
import com.dinaraparanid.prima.viewmodels.mvvm.MP3ConvertViewModel
import java.lang.ref.WeakReference

/** [AbstractFragment] to convert and download audio */

class MP3ConverterFragment : MainActivitySimpleFragment<FragmentMp3ConverterBinding>(), Rising {
    override var binding: FragmentMp3ConverterBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelCurText.set(resources.getString(R.string.mp3_converter))
        setMainLabelInitializedSync()
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
        ).apply {
            viewModel = MP3ConvertViewModel(pasteUrlEdit, WeakReference(requireActivity()))
            pasteUrlText.isSelected = true
            clearInput.setOnClickListener { pasteUrlEdit.text.clear() }
        }

        if (application.playingBarIsVisible) up()
        return binding!!.root
    }

    /**
     * Rise fragment if playing bar is active.
     * It handles GUI error when playing bar was hiding some content
     */

    override fun up() {
        if (!fragmentActivity.isUpped)
            binding!!.convertYoutubeLayout.layoutParams =
                (binding!!.convertYoutubeLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = Params.PLAYING_TOOLBAR_HEIGHT + 250
                }
    }
}