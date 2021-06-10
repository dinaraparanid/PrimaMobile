package com.dinaraparanid.prima

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.MediaPlayer.*
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.dinaraparanid.MainApplication
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.fragments.TrackListFragment
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.unwrap
import kotlin.concurrent.thread

class MediaPlayerService : Service(), OnCompletionListener,
    OnPreparedListener, OnErrorListener, OnSeekCompleteListener, OnInfoListener,
    OnBufferingUpdateListener, OnAudioFocusChangeListener {
    companion object {
        private const val ACTION_PLAY: String = "com.dinaraparanid.prima.media.ACTION_PLAY"
        private const val ACTION_PAUSE: String = "com.dinaraparanid.prima.media.ACTION_PAUSE"
        private const val ACTION_PREVIOUS: String = "com.dinaraparanid.prima.media.ACTION_PREVIOUS"
        private const val ACTION_NEXT: String = "com.dinaraparanid.prima.media.ACTION_NEXT"
        private const val ACTION_STOP: String = "com.dinaraparanid.prima.media.ACTION_STOP"
        private const val MEDIA_CHANNEL_ID = "media_playback_channel"

        // TrackPlayer notification ID
        private const val NOTIFICATION_ID = 101
    }

    enum class PlaybackStatus {
        PLAYING, PAUSED
    }

    private var mediaPlayer: MediaPlayer? = null

    // MediaSession
    private var mediaSessionManager: MediaSessionManager? = null
    private var mediaSession: MediaSession? = null
    private var transportControls: MediaController.TransportControls? = null

    private var resumePosition = 0

    // TrackFocus
    private var trackManager: AudioManager? = null

    // Binder given to clients
    private val iBinder: IBinder = LocalBinder()

    private var trackList = listOf<Track>()

    private var activeTrack: Option<Track> = None

    private var trackIndex = -1

    // Handle incoming phone calls
    private var ongoingCall = false
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyManager: TelephonyManager? = null

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            // Pause track on ACTION_AUDIO_BECOMING_NOISY

            pauseMedia()
            buildNotification(PlaybackStatus.PAUSED)
        }
    }

    private val playNewTrackReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            // Get the new media index form SharedPreferences
            trackIndex = StorageUtil(applicationContext).loadTrackIndex()

            when {
                trackIndex != -1 && trackIndex < trackList.size ->
                    activeTrack = Some(trackList[trackIndex])
                else -> stopSelf()
            }

            (application as MainApplication)
                .mainActivity?.mainActivityViewModel?.curIndexLiveData?.value = trackIndex

            // A PLAY_NEW_AUDIO action received
            // Reset mediaPlayer to play the new Track

            stopMedia()
            mediaPlayer!!.reset()
            initMediaPlayer()
            updateMetaData()
            buildNotification(PlaybackStatus.PLAYING)
        }
    }

    private val resumePlayingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            Log.d("RESUME", "asdasdas")
            resumeMedia(
                intent
                    ?.getIntExtra("resume_position", resumePosition)
                    ?.takeIf { it != -1 } ?: resumePosition
            )
            updateMetaData()
            buildNotification(PlaybackStatus.PLAYING)
        }
    }

    private val pausePlayingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            Log.d("PAUSE", "asdasdas")
            pauseMedia()
            updateMetaData()
            buildNotification(PlaybackStatus.PAUSED)
        }
    }

    private val setLoopingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            Log.d("LOOPING", "asdasdas")
            mediaPlayer!!.isLooping =
                intent?.getBooleanExtra("is_looping", false) ?: false
        }
    }

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            Log.d("STOP", "asdasdas")
            stopSelf()
        }
    }

    /**
     * Service lifecycle methods
     */
    override fun onBind(intent: Intent?): IBinder = iBinder

    override fun onCreate() {
        super.onCreate()

        // Perform one-time setup procedures

        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming call,
        // Resume on hangup.

        callStateListener()

        registerBecomingNoisyReceiver()
        registerPlayNewTrack()
        registerResume()
        registerPause()
        registerSetLooping()
        registerStop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                createChannel()

            // Load data from SharedPreferences
            val storage = StorageUtil(applicationContext)
            trackList = storage.loadTracks()
            trackIndex = storage.loadTrackIndex()

            when {
                trackIndex != -1 && trackIndex < trackList.size ->
                    activeTrack = Some(trackList[trackIndex])
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

        (application as MainApplication).musicPlayer = mediaPlayer!!

        // Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        mediaSession!!.release()
        removeNotification()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d("DESTROY", "sadasdas")

        if (mediaPlayer != null) {
            stopMedia()
            mediaPlayer!!.release()
        }

        removeTrackFocus()

        // Disable the PhoneStateListener

        if (phoneStateListener != null)
            telephonyManager!!.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)

        removeNotification()

        unregisterReceiver(playNewTrackReceiver)
        unregisterReceiver(becomingNoisyReceiver)
        unregisterReceiver(resumePlayingReceiver)
        unregisterReceiver(pausePlayingReceiver)
        unregisterReceiver(setLoopingReceiver)
        unregisterReceiver(stopReceiver)

        // Clear cached playlist
        StorageUtil(applicationContext).clearCachedPlaylist()
    }

    /**
     * Service Binder
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        inline val service: MediaPlayerService
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
                mediaSession!!.isActive = true
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                // Lost focus for an unbounded amount of time

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
        (getSystemService(NOTIFICATION_SERVICE)!! as NotificationManager).cancelAll()
    }

    /**
     * TrackFocus
     */
    private fun requestTrackFocus() =
        (getSystemService(AUDIO_SERVICE)!! as AudioManager).requestAudioFocus(
            this,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

    private fun removeTrackFocus() =
        AudioManager.AUDIOFOCUS_REQUEST_GRANTED == trackManager?.abandonAudioFocus(this) ?: true

    /**
     * MediaPlayer actions
     */
    internal fun initMediaPlayer() {
        if (mediaPlayer == null) mediaPlayer = MediaPlayer()

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
                setDataSource(activeTrack.unwrap().path)
            } catch (e: Exception) {
                stopSelf()
            }

            prepareAsync()
        }
    }

    private fun playMedia() {
        if (!mediaPlayer!!.isPlaying) {
            mediaPlayer!!.start()
            (application as MainApplication).run {
                mainActivity?.playingThread = Some(thread { mainActivity!!.run() })

                (application as MainApplication)
                    .mainActivity?.mainActivityViewModel?.curIndexLiveData?.value = trackIndex

                // update UI part
                mainActivity?.customize()

                try {
                    (mainActivity!!.currentFragment!! as TrackListFragment)
                        .adapter!!.highlight(activeTrack.unwrap())
                } catch (e: Exception) {
                }
            }
        }
    }

    internal fun stopMedia() {
        if (mediaPlayer == null) return
        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.stop()
            (application as MainApplication).mainActivity?.customize()
        }
    }

    internal fun pauseMedia() {
        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
            resumePosition = mediaPlayer!!.currentPosition

            try {
                (application as MainApplication).run {
                    mainActivity?.playingThread?.orNull()?.join()
                    mainActivity?.customize()
                }
            } catch (e: Exception) {
            }
        }
    }

    internal fun resumeMedia(resumePos: Int = resumePosition) {
        val isLooping = mediaPlayer!!.isLooping
        mediaPlayer!!.seekTo(resumePos)
        mediaPlayer!!.start()
        mediaPlayer!!.isLooping = isLooping

        try {
            (application as MainApplication).run {
                mainActivity!!.playingThread = Some(thread { mainActivity!!.run() })
                mainActivity!!.customize()
            }
        } catch (e: Exception) {
        }
    }

    internal fun skipToNext() {
        activeTrack = when (trackIndex) {
            trackList.size - 1 -> {
                trackIndex = 0
                Some(trackList[trackIndex])
            }

            else -> Some(trackList[++trackIndex])
        }

        // Update stored index
        StorageUtil(applicationContext).storeTrackIndex(trackIndex)
        stopMedia()

        mediaPlayer!!.reset()
        initMediaPlayer()
    }

    internal fun skipToPrevious() {
        activeTrack = when (trackIndex) {
            0 -> {
                trackIndex = trackList.size - 1
                Some(trackList[trackIndex])
            }

            else -> Some(trackList[--trackIndex])
        }

        // Update stored index
        StorageUtil(applicationContext).storeTrackIndex(trackIndex)
        stopMedia()

        mediaPlayer!!.reset()
        initMediaPlayer()
    }

    private fun registerBecomingNoisyReceiver() = registerReceiver(
        becomingNoisyReceiver,
        IntentFilter(ACTION_AUDIO_BECOMING_NOISY)
    )

    private fun registerPlayNewTrack() =
        registerReceiver(playNewTrackReceiver, IntentFilter(MainActivity.Broadcast_PLAY_NEW_TRACK))

    private fun registerResume() =
        registerReceiver(resumePlayingReceiver, IntentFilter(MainActivity.Broadcast_RESUME))

    private fun registerPause() =
        registerReceiver(pausePlayingReceiver, IntentFilter(MainActivity.Broadcast_PAUSE))

    private fun registerSetLooping() =
        registerReceiver(setLoopingReceiver, IntentFilter(MainActivity.Broadcast_LOOPING))

    private fun registerStop() =
        registerReceiver(stopReceiver, IntentFilter(MainActivity.Broadcast_STOP))

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
        if (mediaSessionManager != null) return
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE)!! as MediaSessionManager
        mediaSession = MediaSession(applicationContext, "TrackPlayer")
        transportControls = mediaSession!!.controller.transportControls
        mediaSession!!.isActive = true
        mediaSession!!.setFlags(
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
                .or(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
        )
        updateMetaData()

        mediaSession!!.setCallback(object : MediaSession.Callback() {
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

        mediaSession!!.setPlaybackState(
            PlaybackState.Builder().setActions(
                PlaybackState.ACTION_PLAY_PAUSE
                    .or(PlaybackState.ACTION_PLAY)
                    .or(PlaybackState.ACTION_PAUSE)
                    .or(PlaybackState.ACTION_SKIP_TO_NEXT)
                    .or(PlaybackState.ACTION_SKIP_TO_PREVIOUS)
            ).setState(PlaybackState.STATE_PLAYING, 0, 1.0F).build()
        )
    }

    internal fun updateMetaData() {
        val albumArt = BitmapFactory.decodeResource(
            resources,
            R.drawable.album_default
        )

        mediaSession!!.setMetadata(
            MediaMetadata.Builder()
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, activeTrack.unwrap().artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, activeTrack.unwrap().album)
                .putString(MediaMetadata.METADATA_KEY_TITLE, activeTrack.unwrap().title)
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
                playPauseAction = playbackAction(1)
            }

            PlaybackStatus.PAUSED -> {
                notificationAction = play
                playPauseAction = playbackAction(0)
            }
        }

        val largeIcon = BitmapFactory.decodeResource(
            resources,
            R.drawable.album_default
        )

        val notificationBuilder: Notification.Builder =
            Notification.Builder(this)                          // Hide the timestamp
                .setShowWhen(false)                                     // Set the Notification style
                .setStyle(
                    Notification.MediaStyle()                           // Attach our MediaSession token
                        .setMediaSession(mediaSession!!.sessionToken)   // Show our playback controls in the compat view
                        .setShowActionsInCompactView(0, 1, 2)
                )                                                       // Set the Notification color
                .setColor(Params.getInstance().theme.rgb)               // Set the large and small icons
                .setLargeIcon(largeIcon)
                .setSmallIcon(android.R.drawable.stat_sys_headset)      // Set Notification content information
                .setContentInfo(activeTrack.unwrap().album.let {
                    if (it == "<unknown>" ||
                        it == activeTrack.unwrap().path.split('/').takeLast(2).first()
                    ) "Unknown album" else it
                })
                .setContentText(activeTrack.unwrap().artist
                    .let { if (it == "<unknown>") "Unknown artist" else it })
                .setContentTitle(activeTrack.unwrap().title
                    .let { if (it == "<unknown>") "Unknown track" else it })
                .addAction(prev, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", playPauseAction)
                .addAction(next, "next", playbackAction(2))

        (getSystemService(NOTIFICATION_SERVICE)!! as NotificationManager).notify(
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

    internal fun removeNotification() =
        (getSystemService(NOTIFICATION_SERVICE)!! as NotificationManager).cancel(NOTIFICATION_ID)

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

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            (getSystemService(NOTIFICATION_SERVICE)!! as NotificationManager).createNotificationChannel(
                NotificationChannel(
                    MEDIA_CHANNEL_ID,
                    "Media playback",
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> NotificationManager.IMPORTANCE_LOW
                        else -> 0
                    }
                ).apply {
                    this.description = "Media playback controls"
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
    }

    internal inline val isPlaying
        get() = mediaPlayer?.isPlaying == true

    internal inline val isLooping
        get() = mediaPlayer?.isLooping == true

    internal inline val curPosition
        get() = mediaPlayer?.currentPosition ?: 0

    internal inline val duration
        get() = mediaPlayer?.duration ?: 0

    internal fun setLooping(looping: Boolean) {
        mediaPlayer?.isLooping = looping
    }
}