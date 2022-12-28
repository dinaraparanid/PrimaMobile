package com.dinaraparanid.prima.fragments.guess_the_melody

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.GuessTheMelodyActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.entities.DefaultPlaylist
import com.dinaraparanid.prima.databinding.FragmentGtmGameBinding
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.mvvmp.view.fragments.ObservableFragment
import com.dinaraparanid.prima.mvvmp.old_shit.GtmGameViewModel
import java.lang.ref.WeakReference

class GTMGameFragment : ObservableFragment<FragmentGtmGameBinding, GuessTheMelodyActivity>() {
    private var score = 0
    private var trackNumber = 0
    private var playbackStart = 0
    private var playbackLength: Byte = 0
    internal var musicPlayer: MediaPlayer? = null

    private lateinit var allTracks: AbstractPlaylist
    private lateinit var unsolvedTracks: AbstractPlaylist
    private lateinit var tracksOnButtons: AbstractPlaylist

    override var binding: FragmentGtmGameBinding? = null

    internal companion object {
        private const val SCORE_KEY = "score"
        private const val TRACK_NUMBER_KEY = "track_number"
        private const val ALL_TRACKS_KEY = "all_tracks"
        private const val UNSOLVED_TRACKS_KEY = "unsolved_tracks"
        private const val PLAYBACK_LENGTH_KEY = "playback_length"
        private const val TRACKS_ON_BUTTONS_KEY = "tracks_on_buttons"
        private const val PLAYBACK_START_KEY = "playback_start"

        /**
         * Creates new instance of [GTMGameFragment] with params
         * @param allTracks All tracks that should be guessed by user
         * @param tracksOnButtons 4 tracks to choose
         * @param playbackStart playback's start position in ms
         * @param playbackLength playback's length in secs
         * @param trackNumber position of right answer
         * @param score current user's score (number of guessed tracks)
         * @param unsolvedTracks unsolved tracks by user (unused todo)
         */

        @JvmStatic
        internal fun newInstance(
            allTracks: AbstractPlaylist,
            tracksOnButtons: AbstractPlaylist,
            playbackStart: Int,
            playbackLength: Byte,
            trackNumber: Int = 1,
            score: Int = 0,
            unsolvedTracks: AbstractPlaylist = DefaultPlaylist()
        ) = GTMGameFragment().apply {
            arguments = Bundle().apply {
                putInt(SCORE_KEY, score)
                putInt(TRACK_NUMBER_KEY, trackNumber)
                putSerializable(ALL_TRACKS_KEY, allTracks)
                putSerializable(UNSOLVED_TRACKS_KEY, unsolvedTracks)
                putByte(PLAYBACK_LENGTH_KEY, playbackLength)
                putInt(PLAYBACK_START_KEY, playbackStart)
                putSerializable(TRACKS_ON_BUTTONS_KEY, tracksOnButtons)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        score = requireArguments().getInt(SCORE_KEY)
        trackNumber = requireArguments().getInt(TRACK_NUMBER_KEY)
        allTracks = requireArguments().getSerializable(ALL_TRACKS_KEY) as AbstractPlaylist
        unsolvedTracks = requireArguments().getSerializable(UNSOLVED_TRACKS_KEY) as AbstractPlaylist
        tracksOnButtons = requireArguments().getSerializable(TRACKS_ON_BUTTONS_KEY) as AbstractPlaylist
        playbackLength = requireArguments().getByte(PLAYBACK_LENGTH_KEY)
        playbackStart = requireArguments().getInt(PLAYBACK_START_KEY)
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
                WeakReference(this@GTMGameFragment),
                trackNumber,
                tracksOnButtons,
                allTracks,
                playbackStart,
                playbackLength,
                this@GTMGameFragment.score,
                unsolvedTracks
            ).apply {
                setButtonWithCorrectTrack(
                    arrayOf(
                        gtmTrack1,
                        gtmTrack2,
                        gtmTrack3,
                        gtmTrack4
                    )[tracksOnButtons.indexOfFirst {
                        it.gtmFormat == allTracks[this@GTMGameFragment.trackNumber - 1].gtmFormat
                    }]
                )
            }
        }

        return binding!!.root
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMusicPlayer()
    }

    /**
     * Releases music player of this fragment
     * when switching to another fragment
     */

    internal fun releaseMusicPlayer() {
        if (musicPlayer != null)
            try {
                musicPlayer!!.stop()
                musicPlayer!!.release()
                musicPlayer = null
            } catch (ignored: Exception) {
                // already released
            }
    }

    /**
     * Sets play button image
     * @param isPlaying is music playing
     */

    internal fun setPlayButtonImage(isPlaying: Boolean) =
        binding?.gtmPlayButton?.setImageResource(ViewSetter.getPlayButtonImage(isPlaying))

    /**
     * Sets next button as clickable when track was clicked
     * @param isClickable is track clicked
     */

    internal fun setNextButtonClickable(isClickable: Boolean) {
        binding!!.gtmNextTrackButton.isClickable = isClickable
    }

    /**
     * Sets tracks button as clickable
     * @param isClickable is track clickable
     */

    internal fun setTracksButtonsClickable(isClickable: Boolean) = binding!!.run {
        arrayOf(gtmTrack1, gtmTrack2, gtmTrack3, gtmTrack4).forEach { it.isClickable = isClickable }
    }

    /** Current score text */
    internal inline var scoreText
        get() = binding!!.score.text.toString()
        set(value) = binding!!.score.setText(value)
}