package com.dinaraparanid.prima.utils

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaPlayer
import android.media.MediaPlayer.*
import android.media.session.MediaSessionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import arrow.core.Some
import com.dinaraparanid.MainApplication
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class MediaPlayerService : Service(), OnCompletionListener,
    OnPreparedListener, OnErrorListener, OnSeekCompleteListener, OnInfoListener,
    OnBufferingUpdateListener, OnAudioFocusChangeListener {
    companion object {
        const val ACTION_PLAY: String = "com.dinaraparanid.prima.utils.ACTION_PLAY"
        const val ACTION_PAUSE: String = "com.dinaraparanid.prima.utils.ACTION_PAUSE"
        const val ACTION_PREVIOUS: String = "com.dinaraparanid.prima.utils.ACTION_PREVIOUS"
        const val ACTION_NEXT: String = "com.dinaraparanid.prima.utils.ACTION_NEXT"
        const val ACTION_STOP: String = "com.dinaraparanid.prima.utils.ACTION_STOP"
        private const val MEDIA_CHANNEL_ID = "media_playback_channel"

        // TrackPlayer notification ID
        private const val NOTIFICATION_ID = 101
    }

    enum class PlaybackStatus {
        PLAYING, PAUSED
    }

    var mediaPlayer: MediaPlayer? = null

    // MediaSession
    private var mediaSessionManager: MediaSessionManager? = null
    private var mediaSession: MediaSessionCompat? = null
    private var transportControls: MediaControllerCompat.TransportControls? = null

    // Used to pause/resume MediaPlayer
    private var resumePosition = 0
        private set

    // TrackFocus
    private var trackManager: AudioManager? = null

    // Binder given to clients
    private val iBinder: IBinder = LocalBinder()

    // List of available Track files
    internal var trackList = listOf<Track>()
        private set

    internal var activeTrack: Track? = null
        private set

    private var trackIndex = -1

    // Handle incoming phone calls
    private var ongoingCall = false
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyManager: TelephonyManager? = null

    /**
     * Service lifecycle methods
     */
    override fun onBind(intent: Intent): IBinder {
        return iBinder
    }

    override fun onCreate() {
        super.onCreate()
        // Perform one-time setup procedures

        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming call,
        // Resume on hangup.
        callStateListener()
        // ACTION_AUDIO_BECOMING_NOISY -- change in track outputs -- BroadcastReceiver
        registerBecomingNoisyReceiver()
        // Listen for new Track to play -- BroadcastReceiver
        registerPlayNewTrack()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                createChannel()

            // Load data from SharedPreferences
            val storage = StorageUtil(applicationContext)
            trackList = storage.loadTracks()
            trackIndex = storage.loadTrackIndex()

            when {
                trackIndex != -1 && trackIndex < trackList.size ->
                    activeTrack = trackList[trackIndex]
                else -> stopSelf()
            }
        } catch (e: Exception) {
            stopSelf()
        }

        // Request track focus
        if (!requestTrackFocus())
            stopSelf()

        if (mediaSessionManager == null) {
            try {
                initMediaSession()
                initMediaPlayer()
            } catch (e: Exception) {
                stopSelf()
            }

            buildNotification(PlaybackStatus.PLAYING)
        }

        // Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onUnbind(intent: Intent): Boolean {
        mediaSession!!.release()
        removeNotification()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (mediaPlayer != null) {
            stopMedia()
            mediaPlayer!!.release()
        }

        removeTrackFocus()

        // Disable the PhoneStateListener

        if (phoneStateListener != null)
            telephonyManager!!.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)

        removeNotification()

        // Unregister BroadcastReceivers
        unregisterReceiver(becomingNoisyReceiver)
        unregisterReceiver(playNewTrack)

        // Clear cached playlist
        StorageUtil(applicationContext).clearCachedPlaylist()
    }

    /**
     * Service Binder
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        val service: MediaPlayerService
            get() = this@MediaPlayerService
    }

    override fun onBufferingUpdate(mp: MediaPlayer, percent: Int): Unit = Unit

    override fun onCompletion(mp: MediaPlayer) {
        // Invoked when playback of a media source has completed.
        stopMedia()

        (application as MainApplication).run {
            when {
                !mp.isLooping -> mainActivity!!.playNext()
                else -> mainActivity!!.playAudio(trackIndex)
            }
        }
    }

    @SuppressLint("LogConditional")
    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        //Invoked when there has been an error during an asynchronous operation
        when (what) {
            MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> Log.d(
                "MediaPlayer Error",
                "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK $extra"
            )

            MEDIA_ERROR_SERVER_DIED -> Log.d(
                "MediaPlayer Error",
                "MEDIA ERROR SERVER DIED $extra"
            )

            MEDIA_ERROR_UNKNOWN -> Log.d(
                "MediaPlayer Error",
                "MEDIA ERROR UNKNOWN $extra"
            )
        }

        return false
    }

    override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean = false
    override fun onPrepared(mp: MediaPlayer): Unit = playMedia()
    override fun onSeekComplete(mp: MediaPlayer): Unit = Unit

    override fun onAudioFocusChange(focusState: Int) {

        // Invoked when the track focus of the system is updated.

        when (focusState) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Resume playback

                if (mediaPlayer == null) initMediaPlayer()
                else if (!mediaPlayer!!.isPlaying) mediaPlayer!!.start()
                mediaPlayer!!.setVolume(1.0f, 1.0f)
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                // Lost focus for an unbounded amount of time: stop playback and release media player

                if (mediaPlayer!!.isPlaying) mediaPlayer!!.stop()
                mediaPlayer!!.release()
                mediaPlayer = null
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume

                if (mediaPlayer!!.isPlaying) mediaPlayer!!.pause()

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level

                if (mediaPlayer!!.isPlaying) mediaPlayer!!.setVolume(0.1f, 0.1f)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("TEST", "TEST")
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
    }

    /**
     * TrackFocus
     */
    private fun requestTrackFocus(): Boolean {
        trackManager = getSystemService(AUDIO_SERVICE) as AudioManager

        val result = trackManager!!.requestAudioFocus(
            this,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )

        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun removeTrackFocus() =
        AudioManager.AUDIOFOCUS_REQUEST_GRANTED == trackManager!!.abandonAudioFocus(this)

    /**
     * MediaPlayer actions
     */
    internal fun initMediaPlayer() {
        if (mediaPlayer == null) mediaPlayer = MediaPlayer() // new MediaPlayer instance

        // Set up MediaPlayer event listeners

        mediaPlayer!!.apply {
            setOnCompletionListener(this@MediaPlayerService)
            setOnErrorListener(this@MediaPlayerService)
            setOnPreparedListener(this@MediaPlayerService)
            setOnBufferingUpdateListener(this@MediaPlayerService)
            setOnSeekCompleteListener(this@MediaPlayerService)
            setOnInfoListener(this@MediaPlayerService)

            reset()
            setAudioStreamType(AudioManager.STREAM_MUSIC)

            try {
                setDataSource(activeTrack!!.path)
            } catch (e: Exception) {
                stopSelf()
            }

            prepareAsync()
            (application as MainApplication).musicPlayer = this
        }
    }

    private fun playMedia() {
        if (!mediaPlayer!!.isPlaying) {
            mediaPlayer!!.start()
            (applicationContext as MainApplication).run {
                mainActivity?.playingThread = Some(thread { mainActivity!!.run() })
                curTrack = Some(activeTrack!!)
                mainActivity?.customize()
            }
        }
    }

    internal fun stopMedia() {
        if (mediaPlayer == null) return
        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.stop()
            (applicationContext as MainApplication).run {
                mainActivity?.playingThread?.orNull()?.join()
            }
        }
    }

    internal fun pauseMedia() {
        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
            resumePosition = mediaPlayer!!.currentPosition

            try {
                (application as MainApplication).run {
                    mainActivity?.playingThread?.orNull()?.join()
                }
            } catch (e: Exception) {}
        }
    }

    internal fun resumeMedia(resumePos: Int = resumePosition) {
        if (!mediaPlayer!!.isPlaying) {
            mediaPlayer!!.seekTo(resumePos)
            mediaPlayer!!.start()

            try {
                (application as MainApplication).run {
                    mainActivity!!.playingThread = Some(thread { mainActivity!!.run() })
                }
            } catch (e: Exception) {}
        }
    }

    internal fun skipToNext() {
        if (trackIndex == trackList.size - 1) {
            trackIndex = 0
            activeTrack = trackList[trackIndex]
        } else activeTrack = trackList[++trackIndex]

        // Update stored index
        StorageUtil(applicationContext).storeTrackIndex(trackIndex)
        stopMedia()

        mediaPlayer!!.reset()
        initMediaPlayer()
    }

    internal fun skipToPrevious() {
        if (trackIndex == 0) {
            trackIndex = trackList.size - 1
            activeTrack = trackList[trackIndex]
        } else activeTrack = trackList[--trackIndex]

        // Update stored index
        StorageUtil(applicationContext).storeTrackIndex(trackIndex)
        stopMedia()

        mediaPlayer!!.reset()
        initMediaPlayer()
    }

    /**
     * ACTION_AUDIO_BECOMING_NOISY -- change in track outputs
     */
    private val becomingNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Pause track on ACTION_AUDIO_BECOMING_NOISY

            pauseMedia()
            buildNotification(PlaybackStatus.PAUSED)
        }
    }

    private fun registerBecomingNoisyReceiver() {
        // Register after getting track focus

        registerReceiver(
            becomingNoisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        )
    }

    /**
     * Handle PhoneState changes
     */
    private fun callStateListener() {
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, incomingNumber: String) {
                when (state) {
                    TelephonyManager.CALL_STATE_OFFHOOK, TelephonyManager.CALL_STATE_RINGING -> if (mediaPlayer != null) {
                        pauseMedia()
                        ongoingCall = true
                    }

                    TelephonyManager.CALL_STATE_IDLE ->
                        // Phone idle. Start playing

                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false
                                resumeMedia()
                            }
                        }
                }
            }
        }

        // Register the listener with the telephony manager
        // Listen for changes to the device call state

        telephonyManager!!.listen(
            phoneStateListener,
            PhoneStateListener.LISTEN_CALL_STATE
        )
    }

    /**
     * MediaSession and Notification actions
     */

    private fun initMediaSession() {
        if (mediaSessionManager != null) return  // mediaSessionManager exists
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        mediaSession = MediaSessionCompat(applicationContext, "TrackPlayer")
        transportControls = mediaSession!!.controller.transportControls
        mediaSession!!.isActive = true
        mediaSession!!.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        updateMetaData()

        mediaSession!!.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                super.onPlay()
                resumeMedia()
                buildNotification(PlaybackStatus.PLAYING)
            }

            override fun onPause() {
                super.onPause()
                pauseMedia()
                buildNotification(PlaybackStatus.PAUSED)
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                skipToNext()
                updateMetaData()
                buildNotification(PlaybackStatus.PLAYING)
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                skipToPrevious()
                updateMetaData()
                buildNotification(PlaybackStatus.PLAYING)
            }

            override fun onStop() {
                super.onStop()
                removeNotification()
                stopSelf()
            }
        })
    }

    internal fun updateMetaData() {
        val albumArt = BitmapFactory.decodeResource(
            resources,
            R.drawable.album_default
        )

        mediaSession!!.setMetadata(
            MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, activeTrack!!.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, activeTrack!!.album)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, activeTrack!!.title)
                .build()
        )
    }

    internal fun buildNotification(playbackStatus: PlaybackStatus) {
        /**
         * Notification actions -> playbackAction()
         * 0 -> Play
         * 1 -> Pause
         * 2 -> Next track
         * 3 -> Previous track
         */

        /*val pause = when (Params.getInstance().theme) {
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

        val play = when (Params.getInstance().theme) {
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

        val prev = when (Params.getInstance().theme) {
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

        val next = when (Params.getInstance().theme) {
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
        }*/

        val play = android.R.drawable.ic_media_play
        val pause = android.R.drawable.ic_media_pause
        val prev = android.R.drawable.ic_media_previous
        val next = android.R.drawable.ic_media_next

        var notificationAction = pause
        var playPauseAction: PendingIntent? = null

        // Build a new notification according to the current state of the MediaPlayer

        when (playbackStatus) {
            PlaybackStatus.PLAYING -> {
                notificationAction = pause
                // create the pause action
                playPauseAction = playbackAction(1)
            }

            PlaybackStatus.PAUSED -> {
                notificationAction = play
                // create the play action
                playPauseAction = playbackAction(0)
            }
        }

        val largeIcon = BitmapFactory.decodeResource(
            resources,
            R.drawable.album_default
        )

        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, MEDIA_CHANNEL_ID)  // Hide the timestamp
                .setShowWhen(false)                                     // Set the Notification style
                .setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()  // Attach our MediaSession token
                        .setMediaSession(mediaSession!!.sessionToken)   // Show our playback controls in the compat view
                        .setShowActionsInCompactView(0, 1, 2)
                )                                                       // Set the Notification color
                .setColor(Params.getInstance().theme.rgb)               // Set the large and small icons
                .setLargeIcon(largeIcon)
                .setSmallIcon(android.R.drawable.stat_sys_headset)      // Set Notification content information
                .setContentInfo(activeTrack!!.album.let {
                    if (it == "<unknown>" || it == activeTrack!!
                            .path
                            .split('/')
                            .takeLast(2)
                            .first()
                    ) "Unknown album" else it
                })
                .setContentText(activeTrack!!.artist
                    .let { if (it == "<unknown>") "Unknown artist" else it })
                .setContentTitle(activeTrack!!.title
                    .let { if (it == "<unknown>") "Unknown track" else it })                   // Add playback actions
                .addAction(prev, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", playPauseAction)
                .addAction(next, "next", playbackAction(2))

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(
            NOTIFICATION_ID,
            notificationBuilder.build()
        )
    }

    private fun playbackAction(actionNumber: Int): PendingIntent? {
        val playbackAction = Intent(this, MediaPlayerService::class.java)

        return when (actionNumber) {
            0 -> {
                // Play
                playbackAction.action = ACTION_PLAY
                PendingIntent.getService(this, actionNumber, playbackAction, 0)
            }

            1 -> {
                // Pause
                playbackAction.action = ACTION_PAUSE
                PendingIntent.getService(this, actionNumber, playbackAction, 0)
            }

            2 -> {
                // Next track
                playbackAction.action = ACTION_NEXT
                PendingIntent.getService(this, actionNumber, playbackAction, 0)
            }

            3 -> {
                // Previous track
                playbackAction.action = ACTION_PREVIOUS
                PendingIntent.getService(this, actionNumber, playbackAction, 0)
            }

            else -> null
        }
    }

    internal fun removeNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun handleIncomingActions(playbackAction: Intent?) {
        if (playbackAction == null || playbackAction.action == null) return
        val actionString = playbackAction.action

        when {
            actionString.equals(ACTION_PLAY, ignoreCase = true) ->
                transportControls!!.play()

            actionString.equals(ACTION_PAUSE, ignoreCase = true) ->
                transportControls!!.pause()

            actionString.equals(ACTION_NEXT, ignoreCase = true) ->
                transportControls!!.skipToNext()

            actionString.equals(ACTION_PREVIOUS, ignoreCase = true) ->
                transportControls!!.skipToPrevious()

            actionString.equals(ACTION_STOP, ignoreCase = true) ->
                transportControls!!.stop()
        }
    }

    /**
     * Play new Track
     */
    private val playNewTrack: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            // Get the new media index form SharedPreferences
            trackIndex = StorageUtil(applicationContext).loadTrackIndex()

            when {
                trackIndex != -1 && trackIndex < trackList.size ->
                    activeTrack = trackList[trackIndex]
                else -> stopSelf()
            }

            // A PLAY_NEW_AUDIO action received
            // Reset mediaPlayer to play the new Track

            stopMedia()
            mediaPlayer!!.reset()
            initMediaPlayer()
            updateMetaData()
            buildNotification(PlaybackStatus.PLAYING)
        }
    }

    private fun registerPlayNewTrack() {
        // Register playNewMedia receiver
        val filter = IntentFilter(MainActivity.Broadcast_PLAY_NEW_TRACK)
        registerReceiver(playNewTrack, filter)
    }

    private fun createChannel() {
        val id = MEDIA_CHANNEL_ID
        val name = "Media playback"
        val description = "Media playback controls"

        val importance: Int = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> NotificationManager.IMPORTANCE_LOW
            else -> 0
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                NotificationChannel(id, name, importance).apply {
                    this.description = description
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
    }
}