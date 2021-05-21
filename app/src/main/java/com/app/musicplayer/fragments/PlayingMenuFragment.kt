package com.app.musicplayer.fragments

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.app.musicplayer.MainActivity
import com.app.musicplayer.R
import com.app.musicplayer.core.Track
import com.app.musicplayer.database.MusicRepository
import com.app.musicplayer.viewmodels.PlayingMenuViewModel
import de.hdodenhof.circleimageview.CircleImageView
import java.util.UUID

class PlayingMenuFragment private constructor() : Fragment() {
    interface Callbacks {
        fun onPlayingToolbarClicked(trackId: UUID, isPlaying: Boolean = false)
    }

    private lateinit var track: Track
    private lateinit var toolbar: Toolbar
    private lateinit var albumImage: CircleImageView
    private lateinit var trackTitle: TextView
    private lateinit var trackArtists: TextView
    private lateinit var playButton: ImageButton
    private lateinit var prevTrack: ImageButton
    private lateinit var nextTrack: ImageButton
    private val playingMenuViewModel: PlayingMenuViewModel by lazy {
        ViewModelProvider(this)[PlayingMenuViewModel::class.java]
    }

    private var callbacks: Callbacks? = null
    private var player: MediaPlayer? = MediaPlayer()
    private var isPlaying = false
    private var repeat1 = false

    companion object {
        private const val ARG_TRACK_ID = "track_id"
        private const val ARG_IS_PLAYING = "is_playing"

        fun newInstance(trackId: UUID, isPlaying: Boolean) = PlayingMenuFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_TRACK_ID, trackId)
                putBoolean(ARG_IS_PLAYING, isPlaying)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        track = Track()
        playingMenuViewModel.loadTrack(arguments?.getSerializable(ARG_TRACK_ID) as UUID)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_playing_menu, container, false)

        toolbar = view.findViewById(R.id.playing_toolbar)
        val playingLayout = toolbar.findViewById<ConstraintLayout>(R.id.playing_layout)

        albumImage = playingLayout.findViewById(R.id.playing_album_image)
        trackTitle = playingLayout.findViewById(R.id.playing_track_title)
        trackArtists = playingLayout.findViewById(R.id.playing_track_artists)
        playButton = playingLayout.findViewById(R.id.playing_play_button)
        prevTrack = playingLayout.findViewById(R.id.playing_prev_track)
        nextTrack = playingLayout.findViewById(R.id.playing_next_track)

        setPlayButtonImage()

        toolbar.setOnClickListener {
            (activity!! as MainActivity).onPlayingToolbarClicked(track.trackId, isPlaying)
        }

        playButton.setOnClickListener {
            isPlaying = !isPlaying
            setPlayButtonImage()
            // playTrack("") TODO: Track playing
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playingMenuViewModel.trackLiveData.observe(viewLifecycleOwner) {
            it?.let {
                track = it
                updateUI()
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onStop() {
        super.onStop()
        playingMenuViewModel.saveTrack(track)
    }

    private fun setPlayButtonImage() = playButton.setImageResource(
        when {
            isPlaying -> android.R.drawable.ic_media_play
            else -> android.R.drawable.ic_media_pause
        }
    )

    private fun updateUI() {
        trackTitle.text = track.title
        trackArtists.text = MusicRepository
            .getInstance()
            .getArtistsByTrack(track.trackId)
            ?.fold("") { acc, (artist) -> "$acc${artist.name}, " }
            ?.dropLast(2) ?: "Unknown artist"
    }
}