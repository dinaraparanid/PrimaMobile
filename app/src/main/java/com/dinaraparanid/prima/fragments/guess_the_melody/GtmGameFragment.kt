package com.dinaraparanid.prima.fragments.guess_the_melody

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.GuessTheMelodyActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.databinding.FragmentGtmGameBinding
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.viewmodels.mvvm.GtmGameViewModel
import java.lang.ref.WeakReference

class GtmGameFragment : AbstractFragment<FragmentGtmGameBinding, GuessTheMelodyActivity>() {
    private var score = 0
    private var trackNumber = 0

    private lateinit var tracksLeft: AbstractPlaylist
    private lateinit var unsolvedTracks: AbstractPlaylist

    override var binding: FragmentGtmGameBinding? = null

    internal companion object {
        private const val SCORE_KEY = "score"
        private const val TRACK_NUMBER_KEY = "track_number"
        private const val UNSOLVED_TRACKS_KEY = "unsolved_tracks"
        private const val TRACKS_LEFT_KEY = "tracks_left"

        @JvmStatic
        internal fun newInstance(
            score: Int,
            trackNumber: Int,
            tracksLeft: AbstractPlaylist,
            unsolvedTracks: AbstractPlaylist = DefaultPlaylist()
        ) = GtmGameFragment().apply {
            arguments = Bundle().apply {
                putInt(SCORE_KEY, score)
                putInt(TRACK_NUMBER_KEY, trackNumber)
                putSerializable(TRACKS_LEFT_KEY, tracksLeft)
                putSerializable(UNSOLVED_TRACKS_KEY, unsolvedTracks)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        score = requireArguments().getInt(SCORE_KEY)
        trackNumber = requireArguments().getInt(TRACK_NUMBER_KEY)
        tracksLeft = requireArguments().getSerializable(TRACKS_LEFT_KEY) as AbstractPlaylist
        unsolvedTracks = requireArguments().getSerializable(UNSOLVED_TRACKS_KEY) as AbstractPlaylist
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentGtmGameBinding>(
            inflater,
            R.layout.fragment_gtm_game,
            container,
            false
        ).apply {
            viewModel = GtmGameViewModel(
                WeakReference(this@GtmGameFragment),
                trackNumber,
                tracksLeft - tracksLeft.first(),
                tracksLeft.first(),
                this@GtmGameFragment.score
            )
        }

        return binding!!.root
    }
}