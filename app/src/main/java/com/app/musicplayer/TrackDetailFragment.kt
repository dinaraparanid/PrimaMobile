package com.app.musicplayer

import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.app.musicplayer.core.Track
import com.app.musicplayer.database.MusicRepository
import com.app.musicplayer.utils.Colors
import java.lang.IllegalStateException
import java.util.UUID

class TrackDetailFragment private constructor() : Fragment() {
    interface Callbacks {
        fun onReturnSelected()
    }

    private lateinit var track: Track
    private lateinit var trackLayout: ConstraintLayout
    private lateinit var settingsButton: ImageButton
    private lateinit var albumImage: ImageView
    private lateinit var trackBar: SeekBar
    private lateinit var curTime: TextView
    private lateinit var trackLength: TextView
    private lateinit var trackTitle: TextView
    private lateinit var artistsAlbum: TextView
    private lateinit var playButton: ImageButton
    private lateinit var prevTrackButton: ImageButton
    private lateinit var nextTrackButton: ImageButton
    private lateinit var likeButton: ImageButton
    private lateinit var repeatButton: ImageButton
    private lateinit var playlistButton: ImageButton
    private lateinit var returnButton: ImageButton
    private val trackDetailedViewModel: TrackDetailedViewModel by lazy {
        ViewModelProvider(this)[TrackDetailedViewModel::class.java]
    }

    private var callbacks: Callbacks? = null
    private var player: MediaPlayer? = MediaPlayer()
    private var isPlaying = false
    private var like = false
    private var repeat1 = false

    companion object {
        private const val ARG_TRACK_ID = "track_id"
        private const val ARG_IS_PLAYING = "is_playing"

        fun newInstance(trackId: UUID, isPlaying: Boolean) = TrackDetailFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_TRACK_ID, trackId)
                putBoolean(ARG_IS_PLAYING, isPlaying)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        track = Track()
        trackDetailedViewModel.loadTrack(arguments?.getSerializable(ARG_TRACK_ID) as UUID)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_track_detail, container, false)

        trackLayout = view.findViewById(R.id.track_layout)
        settingsButton = view.findViewById(R.id.track_settings_button)
        albumImage = view.findViewById(R.id.album_picture)
        trackBar = view.findViewById(R.id.track_bar)
        curTime = view.findViewById(R.id.current_time)
        trackLength = view.findViewById(R.id.track_length)
        trackTitle = view.findViewById(R.id.track_title_big)
        artistsAlbum = view.findViewById(R.id.artists_album)
        playButton = view.findViewById(R.id.play_button)
        prevTrackButton = view.findViewById(R.id.previous_track_button)
        nextTrackButton = view.findViewById(R.id.next_track_button)
        likeButton = view.findViewById(R.id.like_button)
        repeatButton = view.findViewById(R.id.repeat_button)
        playlistButton = view.findViewById(R.id.playlist_button)
        returnButton = view.findViewById(R.id.return_button)

        curTime.setTextColor(if (Params.getInstance().theme.isNight) Color.WHITE else Color.BLACK)
        trackLength.setTextColor(if (Params.getInstance().theme.isNight) Color.WHITE else Color.BLACK)
        trackTitle.setTextColor(if (Params.getInstance().theme.isNight) Color.WHITE else Color.BLACK)
        artistsAlbum.setTextColor(if (Params.getInstance().theme.isNight) Color.WHITE else Color.BLACK)

        returnButton.setImageResource(
            when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.arrow_blue
                is Colors.BlueNight -> R.drawable.arrow_blue
                is Colors.Green -> R.drawable.arrow_green
                is Colors.GreenNight -> R.drawable.arrow_green
                is Colors.GreenTurquoise -> R.drawable.arrow_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.arrow_green_turquoise
                is Colors.Lemon -> R.drawable.arrow_lemon
                is Colors.LemonNight -> R.drawable.arrow_lemon
                is Colors.Orange -> R.drawable.arrow_orange
                is Colors.OrangeNight -> R.drawable.arrow_orange
                is Colors.Pink -> R.drawable.arrow_pink
                is Colors.PinkNight -> R.drawable.arrow_pink
                is Colors.Purple -> R.drawable.arrow_purple
                is Colors.PurpleNight -> R.drawable.arrow_purple
                is Colors.Red -> R.drawable.arrow_red
                is Colors.RedNight -> R.drawable.arrow_red
                is Colors.Sea -> R.drawable.arrow_sea
                is Colors.SeaNight -> R.drawable.arrow_sea
                is Colors.Turquoise -> R.drawable.arrow_turquoise
                is Colors.TurquoiseNight -> R.drawable.arrow_turquoise
                else -> R.drawable.arrow
            }
        )

        nextTrackButton.setImageResource(
            when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.next_track_blue
                is Colors.BlueNight -> R.drawable.next_track_blue
                is Colors.Green -> R.drawable.next_track_green
                is Colors.GreenNight -> R.drawable.next_track_green
                is Colors.GreenTurquoise -> R.drawable.next_track_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.next_track_green_turquoise
                is Colors.Lemon -> R.drawable.next_track_lemon
                is Colors.LemonNight -> R.drawable.next_track_lemon
                is Colors.Orange -> R.drawable.next_track_orange
                is Colors.OrangeNight -> R.drawable.next_track_orange
                is Colors.Pink -> R.drawable.next_track_pink
                is Colors.PinkNight -> R.drawable.next_track_pink
                is Colors.Purple -> R.drawable.next_track_purple
                is Colors.PurpleNight -> R.drawable.next_track_purple
                is Colors.Red -> R.drawable.next_track_red
                is Colors.RedNight -> R.drawable.next_track_red
                is Colors.Sea -> R.drawable.next_track_sea
                is Colors.SeaNight -> R.drawable.next_track_sea
                is Colors.Turquoise -> R.drawable.next_track_turquoise
                is Colors.TurquoiseNight -> R.drawable.next_track_turquoise
                else -> R.drawable.next_track
            }
        )

        prevTrackButton.setImageResource(
            when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.prev_track_blue
                is Colors.BlueNight -> R.drawable.prev_track_blue
                is Colors.Green -> R.drawable.prev_track_green
                is Colors.GreenNight -> R.drawable.prev_track_green
                is Colors.GreenTurquoise -> R.drawable.prev_track_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.prev_track_green_turquoise
                is Colors.Lemon -> R.drawable.prev_track_lemon
                is Colors.LemonNight -> R.drawable.prev_track_lemon
                is Colors.Orange -> R.drawable.prev_track_orange
                is Colors.OrangeNight -> R.drawable.prev_track_orange
                is Colors.Pink -> R.drawable.prev_track_pink
                is Colors.PinkNight -> R.drawable.prev_track_pink
                is Colors.Purple -> R.drawable.prev_track_purple
                is Colors.PurpleNight -> R.drawable.prev_track_purple
                is Colors.Red -> R.drawable.prev_track_red
                is Colors.RedNight -> R.drawable.prev_track_red
                is Colors.Sea -> R.drawable.prev_track_sea
                is Colors.SeaNight -> R.drawable.prev_track_sea
                is Colors.Turquoise -> R.drawable.prev_track_turquoise
                is Colors.TurquoiseNight -> R.drawable.prev_track_turquoise
                else -> R.drawable.prev_track
            }
        )

        playlistButton.setImageResource(
            when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.playlist_blue
                is Colors.BlueNight -> R.drawable.playlist_blue
                is Colors.Green -> R.drawable.playlist_green
                is Colors.GreenNight -> R.drawable.playlist_green
                is Colors.GreenTurquoise -> R.drawable.playlist_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.playlist_green_turquoise
                is Colors.Lemon -> R.drawable.playlist_lemon
                is Colors.LemonNight -> R.drawable.playlist_lemon
                is Colors.Orange -> R.drawable.playlist_orange
                is Colors.OrangeNight -> R.drawable.playlist_orange
                is Colors.Pink -> R.drawable.playlist_pink
                is Colors.PinkNight -> R.drawable.playlist_pink
                is Colors.Purple -> R.drawable.playlist_purple
                is Colors.PurpleNight -> R.drawable.playlist_purple
                is Colors.Red -> R.drawable.playlist_red
                is Colors.RedNight -> R.drawable.playlist_red
                is Colors.Sea -> R.drawable.playlist_sea
                is Colors.SeaNight -> R.drawable.playlist_sea
                is Colors.Turquoise -> R.drawable.playlist_turquoise
                is Colors.TurquoiseNight -> R.drawable.playlist_turquoise
                else -> R.drawable.playlist
            }
        )

        settingsButton.setImageResource(
            when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.three_dots_blue
                is Colors.BlueNight -> R.drawable.three_dots_blue
                is Colors.Green -> R.drawable.three_dots_green
                is Colors.GreenNight -> R.drawable.three_dots_green
                is Colors.GreenTurquoise -> R.drawable.three_dots_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.three_dots_green_turquoise
                is Colors.Lemon -> R.drawable.three_dots_lemon
                is Colors.LemonNight -> R.drawable.three_dots_lemon
                is Colors.Orange -> R.drawable.three_dots_orange
                is Colors.OrangeNight -> R.drawable.three_dots_orange
                is Colors.Pink -> R.drawable.three_dots_pink
                is Colors.PinkNight -> R.drawable.three_dots_pink
                is Colors.Purple -> R.drawable.three_dots_purple
                is Colors.PurpleNight -> R.drawable.three_dots_purple
                is Colors.Red -> R.drawable.three_dots_red
                is Colors.RedNight -> R.drawable.three_dots_red
                is Colors.Sea -> R.drawable.three_dots_sea
                is Colors.SeaNight -> R.drawable.three_dots_sea
                is Colors.Turquoise -> R.drawable.three_dots_turquoise
                is Colors.TurquoiseNight -> R.drawable.three_dots_turquoise
                else -> R.drawable.three_dots
            }
        )

        setLikeButtonImage()
        setPlayButtonImage()
        setRepeatButtonImage()

        // trackLayout.setBackgroundColor(Params.getInstance().theme.rgb)

        playButton.setOnClickListener {
            isPlaying = !isPlaying
            setPlayButtonImage()
            // playTrack("") TODO: Track playing
        }

        likeButton.setOnClickListener {
            like = !like
            setLikeButtonImage()
            // TODO: favourites
        }

        repeatButton.setOnClickListener {
            repeat1 = !repeat1
            setRepeatButtonImage()
            // TODO: repeat playlist / song
        }

        returnButton.setOnClickListener {
            (activity!! as Callbacks).onReturnSelected()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        trackDetailedViewModel.trackLiveData.observe(viewLifecycleOwner) {
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
        (activity!! as AppCompatActivity).supportActionBar!!.show()
        trackDetailedViewModel.saveTrack(track)
    }

    override fun onStart() {
        super.onStart()
        (activity!! as AppCompatActivity).supportActionBar!!.hide()
    }

    private fun updateUI() {
        trackTitle.text = track.title
        artistsAlbum.text = MusicRepository
            .getInstance()
            .getArtistsByTrack(track.trackId)
            ?.fold("") { acc, (artist) -> "$acc${artist.name} " } ?: "Unknown artist" + "/ ${
            track.albumId?.let {
                MusicRepository.getInstance().getAlbumOfTrack(it)
            } ?: "Unknown album"
        }"
    }

    private fun setLikeButtonImage() = likeButton.setImageResource(
        when {
            like -> R.drawable.heart_like
            else -> when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.heart_blue
                is Colors.BlueNight -> R.drawable.heart_blue
                is Colors.Green -> R.drawable.heart_green
                is Colors.GreenNight -> R.drawable.heart_green
                is Colors.GreenTurquoise -> R.drawable.heart_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.heart_green_turquoise
                is Colors.Lemon -> R.drawable.heart_lemon
                is Colors.LemonNight -> R.drawable.heart_lemon
                is Colors.Orange -> R.drawable.heart_orange
                is Colors.OrangeNight -> R.drawable.heart_orange
                is Colors.Pink -> R.drawable.heart_pink
                is Colors.PinkNight -> R.drawable.heart_pink
                is Colors.Purple -> R.drawable.heart_purple
                is Colors.PurpleNight -> R.drawable.heart_purple
                is Colors.Red -> R.drawable.heart_red
                is Colors.RedNight -> R.drawable.heart_red
                is Colors.Sea -> R.drawable.heart_sea
                is Colors.SeaNight -> R.drawable.heart_sea
                is Colors.Turquoise -> R.drawable.heart_turquoise
                is Colors.TurquoiseNight -> R.drawable.heart_turquoise
                else -> R.drawable.heart
            }
        }
    )

    private fun setPlayButtonImage() = playButton.setImageResource(
        when {
            isPlaying -> when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.play_blue
                is Colors.BlueNight -> R.drawable.play_blue
                is Colors.Green -> R.drawable.play_green
                is Colors.GreenNight -> R.drawable.play_green
                is Colors.GreenTurquoise -> R.drawable.play_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.play_green_turquoise
                is Colors.Lemon -> R.drawable.play_lemon
                is Colors.LemonNight -> R.drawable.play_lemon
                is Colors.Orange -> R.drawable.play_orange
                is Colors.OrangeNight -> R.drawable.play_orange
                is Colors.Pink -> R.drawable.play_pink
                is Colors.PinkNight -> R.drawable.play_pink
                is Colors.Purple -> R.drawable.play_purple
                is Colors.PurpleNight -> R.drawable.play_purple
                is Colors.Red -> R.drawable.play_red
                is Colors.RedNight -> R.drawable.play_red
                is Colors.Sea -> R.drawable.play_sea
                is Colors.SeaNight -> R.drawable.play_sea
                is Colors.Turquoise -> R.drawable.play_turquoise
                is Colors.TurquoiseNight -> R.drawable.play_turquoise
                else -> R.drawable.play
            }
            else -> when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.pause_blue
                is Colors.BlueNight -> R.drawable.pause_blue
                is Colors.Green -> R.drawable.pause_green
                is Colors.GreenNight -> R.drawable.pause_green
                is Colors.GreenTurquoise -> R.drawable.pause_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.pause_green_turquoise
                is Colors.Lemon -> R.drawable.pause_lemon
                is Colors.LemonNight -> R.drawable.pause_lemon
                is Colors.Orange -> R.drawable.pause_orange
                is Colors.OrangeNight -> R.drawable.pause_orange
                is Colors.Pink -> R.drawable.pause_pink
                is Colors.PinkNight -> R.drawable.pause_pink
                is Colors.Purple -> R.drawable.pause_purple
                is Colors.PurpleNight -> R.drawable.pause_purple
                is Colors.Red -> R.drawable.pause_red
                is Colors.RedNight -> R.drawable.pause_red
                is Colors.Sea -> R.drawable.pause_sea
                is Colors.SeaNight -> R.drawable.pause_sea
                is Colors.Turquoise -> R.drawable.pause_turquoise
                is Colors.TurquoiseNight -> R.drawable.pause_turquoise
                else -> R.drawable.pause
            }
        }
    )

    private fun setRepeatButtonImage() = repeatButton.setImageResource(
        when {
            repeat1 -> when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.repeat1_blue
                is Colors.BlueNight -> R.drawable.repeat1_blue
                is Colors.Green -> R.drawable.repeat1_green
                is Colors.GreenNight -> R.drawable.repeat1_green
                is Colors.GreenTurquoise -> R.drawable.repeat1_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.repeat1_green_turquoise
                is Colors.Lemon -> R.drawable.repeat1_lemon
                is Colors.LemonNight -> R.drawable.repeat1_lemon
                is Colors.Orange -> R.drawable.repeat1_orange
                is Colors.OrangeNight -> R.drawable.repeat1_orange
                is Colors.Pink -> R.drawable.repeat1_pink
                is Colors.PinkNight -> R.drawable.repeat1_pink
                is Colors.Purple -> R.drawable.repeat1_purple
                is Colors.PurpleNight -> R.drawable.repeat1_purple
                is Colors.Red -> R.drawable.repeat1_red
                is Colors.RedNight -> R.drawable.repeat1_red
                is Colors.Sea -> R.drawable.repeat1_sea
                is Colors.SeaNight -> R.drawable.repeat1_sea
                is Colors.Turquoise -> R.drawable.repeat1_turquoise
                is Colors.TurquoiseNight -> R.drawable.repeat1_turquoise
                else -> R.drawable.repeat_1
            }

            else -> when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.repeat_blue
                is Colors.BlueNight -> R.drawable.repeat_blue
                is Colors.Green -> R.drawable.repeat_green
                is Colors.GreenNight -> R.drawable.repeat_green
                is Colors.GreenTurquoise -> R.drawable.repeat_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.repeat_green_turquoise
                is Colors.Lemon -> R.drawable.repeat_lemon
                is Colors.LemonNight -> R.drawable.repeat_lemon
                is Colors.Orange -> R.drawable.repeat_orange
                is Colors.OrangeNight -> R.drawable.repeat_orange
                is Colors.Pink -> R.drawable.repeat_pink
                is Colors.PinkNight -> R.drawable.repeat_pink
                is Colors.Purple -> R.drawable.repeat_purple
                is Colors.PurpleNight -> R.drawable.repeat_purple
                is Colors.Red -> R.drawable.repeat_red
                is Colors.RedNight -> R.drawable.repeat_red
                is Colors.Sea -> R.drawable.repeat_sea
                is Colors.SeaNight -> R.drawable.repeat_sea
                is Colors.Turquoise -> R.drawable.repeat_turquoise
                is Colors.TurquoiseNight -> R.drawable.repeat_turquoise
                else -> R.drawable.repeat
            }
        }
    )

    /*override fun onDestroy() {
        super.onDestroy()
        clearMediaPlayer()
    }*/
}