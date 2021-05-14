package com.app.musicplayer

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.app.musicplayer.core.Track
import com.app.musicplayer.database.MusicRepository
import java.util.UUID

class TrackDetailFragment private constructor() : Fragment() {
    interface Callbacks {
        fun onReturnSelected()
    }

    private lateinit var track: Track
    private lateinit var settingButton: ImageButton
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

        settingButton = view.findViewById(R.id.track_settings_button)
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

        playButton.setOnClickListener {
            playButton.setImageResource(if (isPlaying) R.drawable.pause else R.drawable.play)
            isPlaying = !isPlaying
            //playTrack("") TODO: Track playing
        }

        likeButton.setOnClickListener {
            likeButton.setImageResource(if (like) R.drawable.heart else R.drawable.heart_like )
            like = !like
            // TODO: favourites
        }

        repeatButton.setOnClickListener {
            repeatButton.setImageResource(if (repeat1) R.drawable.repeat else R.drawable.repeat_1)
            repeat1 = !repeat1
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
        Log.d("UPD UI", "UPD UI")
        trackTitle.text = track.title
        artistsAlbum.text = MusicRepository
            .getInstance()
            .getArtistsByTrack(track.trackId)
            ?.fold("") { acc, (artist) -> "$acc${artist.name} " } ?: "Unknown artist" + "/ ${
            track.albumId?.let { MusicRepository.getInstance().getAlbumOfTrack(it) } ?: "Unknown album"
        }"
    }

    /*override fun onDestroy() {
        super.onDestroy()
        clearMediaPlayer()
    }*/
}