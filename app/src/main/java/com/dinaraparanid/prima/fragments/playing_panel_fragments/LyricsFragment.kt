package com.dinaraparanid.prima.fragments.playing_panel_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentLyricsBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.Statistics
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.utils.polymorphism.StatisticsUpdatable
import com.dinaraparanid.prima.utils.polymorphism.fragments.MainActivitySimpleFragment
import com.dinaraparanid.prima.utils.polymorphism.fragments.setMainActivityMainLabel
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel

class LyricsFragment :
    MainActivitySimpleFragment<FragmentLyricsBinding>(),
    StatisticsUpdatable,
    Rising {
    private lateinit var lyrics: String
    override var binding: FragmentLyricsBinding? = null
    override val coroutineScope get() = lifecycleScope
    override val updateStyle = Statistics::withIncrementedNumberOfLyricsShown

    internal companion object {
        private const val LYRICS_KEY = "lyrics"

        /**
         * Creates new instance of fragment with params
         * @param mainLabelCurText main label text for current fragment
         * @param lyrics track's lyrics to show
         * @return new instance of fragment with params in bundle
         */

        @JvmStatic
        internal fun newInstance(
            mainLabelCurText: String,
            lyrics: String
        ) = LyricsFragment().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
                putString(LYRICS_KEY, lyrics)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelCurText = requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY)!!
        lyrics = requireArguments().getString(LYRICS_KEY)!!
        setMainLabelInitialized()
        super.onCreate(savedInstanceState)
        setMainActivityMainLabel()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate<FragmentLyricsBinding>(inflater, R.layout.fragment_lyrics, container, false)
            .apply {
                viewModel = ViewModel()
                lyrics = this@LyricsFragment.lyrics
            }

        runOnIOThread { updateStatisticsAsync() }
        if (application.playingBarIsVisible) up()
        return binding!!.root
    }

    /**
     * Rise fragment if playing bar is active.
     * It handles GUI error when playing bar was hiding some content
     */

    override fun up() {
        if (!fragmentActivity.isUpped)
            binding!!.lyricsLayout.layoutParams =
                (binding!!.lyricsLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = Params.PLAYING_TOOLBAR_HEIGHT
                }
    }
}