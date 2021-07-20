package com.dinaraparanid.prima

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.media.*
import android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaPlayer.*
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.support.v4.media.session.MediaSessionCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import arrow.core.None
import arrow.core.Some
import com.dinaraparanid.prima.utils.polymorphism.TrackListFragment
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.extensions.unwrap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

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
        private const val NO_PATH = "_____ЫЫЫЫЫЫЫЫ_____"
        private const val NOTIFICATION_ID = 101
    }

    internal enum class PlaybackStatus {
        PLAYING, PAUSED
    }

    private var mediaPlayer: MediaPlayer? = null

    // MediaSession
    private var mediaSessionManager: MediaSessionManager? = null
    private var mediaSession: MediaSession? = null
    private var transportControls: MediaController.TransportControls? = null

    // TrackFocus
    private var trackManager: AudioManager? = null

    // Binder given to clients
    private val iBinder: IBinder = LocalBinder()

    private var resumePosition = 0
    private var started = false
    private var startFromLooping = false
    private var startFromPause = false

    // Handle incoming phone calls
    private var ongoingCall = false
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyManager: TelephonyManager? = null

    internal inline val curTrack
        get() = (application as MainApplication).run {
            curPath.takeIf { it != "_____ЫЫЫЫЫЫЫЫ_____" }
                ?.let {
                    Some(
                        curPlaylist.run { get(indexOfFirst { track -> track.path == it }) }
                    )
                }
                ?: None
        }

    private inline val curPath
        get() = (application as MainApplication).curPath

    private inline val curInd
        get() = (application as MainApplication)
            .curPlaylist.indexOfFirst { it.path == curPath }

    internal inline val trackList
        get() = (application as MainApplication).curPlaylist

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            // Pause track on ACTION_AUDIO_BECOMING_NOISY

            pauseMedia()
            buildNotification(PlaybackStatus.PAUSED)
        }
    }

    private val playNewTrackReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            audioFocusHelp()

            (application as MainApplication).run {
                highlightedRows.clear()
                highlightedRows.add(curTrack.unwrap().path)
                highlightedRows = highlightedRows.distinct().toMutableList()
                mainActivity?.currentFragment?.takeIf { it is TrackListFragment }?.let {
                    ((it as TrackListFragment).adapter!! as TrackListFragment.TrackAdapter).highlight(
                        curTrack.unwrap()
                    )
                }
            }

            // A PLAY_NEW_TRACK action received
            // Reset mediaPlayer to play the new Track

            val looping = mediaPlayer?.isLooping
                ?: StorageUtil(applicationContext).loadLooping()
            stopMedia()
            mediaPlayer?.reset()
            initMediaPlayer()
            updateMetaData()
            mediaPlayer!!.isLooping = looping
            buildNotification(PlaybackStatus.PLAYING)
        }
    }

    private val resumePlayingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
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
            pauseMedia()
            updateMetaData()
            buildNotification(PlaybackStatus.PAUSED)
        }
    }

    private val setLoopingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (mediaPlayer == null)
                initMediaPlayer()

            mediaPlayer!!.isLooping =
                intent?.getBooleanExtra(MainActivity.IS_LOOPING_ARG, false)
                    ?: StorageUtil(applicationContext).loadLooping()
        }
    }

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            resumePosition = mediaPlayer?.currentPosition ?: run {
                initMediaPlayer()
                StorageUtil(applicationContext).loadTrackPauseTime()
            }

            save()
            removeNotification()
            stopSelf()
            (application as MainApplication).mainActivity?.customize()
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
            val loadResume = intent!!.getIntExtra(MainActivity.RESUME_POSITION_ARG, -1)

            startFromLooping = intent.action?.let { it == MainActivity.LOOPING_PRESSED_ARG }
                ?: false
            startFromPause = intent.action?.let { it == MainActivity.PAUSED_PRESSED_ARG }
                ?: false

            resumePosition = when {
                loadResume != -1 -> loadResume
                else -> when {
                    resumePosition != 0 -> resumePosition
                    else -> storage.loadTrackPauseTime().takeIf { it != -1 } ?: 0
                }
            }
        } catch (e: Exception) {
            resumePosition = mediaPlayer?.currentPosition ?: run {
                initMediaPlayer()
                StorageUtil(applicationContext).loadTrackPauseTime()
            }

            save()
            removeNotification()
            stopSelf()
        }

        // Request track focus
        audioFocusHelp()

        // Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent)
        return super.onStartCommand(intent, flags, startId)
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

        (application as MainApplication).mainActivity!!.run {
            when {
                mp.isLooping -> playAudio(curPath)
                else -> playNext()
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

    override fun onPrepared(mp: MediaPlayer): Unit = when {
        startFromLooping -> startFromLooping = false

        startFromPause -> {
            startFromPause = false
            pauseMedia()
            buildNotification(PlaybackStatus.PAUSED)
        }

        started -> playMedia().apply {
            buildNotification(PlaybackStatus.PLAYING)
        }

        else -> {
            started = true

            when ((application as MainApplication).startPath) {
                None -> playMedia()

                is Some -> when ((application as MainApplication).startPath.unwrap()) {
                    curPath -> resumeMedia()
                    else -> playMedia()
                }
            }

            buildNotification(PlaybackStatus.PLAYING)
        }
    }

    override fun onSeekComplete(mp: MediaPlayer): Unit = Unit

    override fun onAudioFocusChange(focusState: Int) {
        when (focusState) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Resume playback

                if (mediaPlayer == null) initMediaPlayer(true)
                else if (!mediaPlayer!!.isPlaying) mediaPlayer!!.start()
                mediaPlayer!!.setVolume(1.0f, 1.0f)
                mediaSession!!.isActive = true
                buildNotification(PlaybackStatus.PLAYING)
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                // Lost focus for an unbounded amount of time

                try {
                    if (mediaPlayer!!.isPlaying) {
                        mediaPlayer!!.stop()
                        resumePosition = mediaPlayer!!.currentPosition
                        save()
                    }

                    mediaPlayer!!.release()
                    mediaPlayer = null
                } catch (e: Exception) {
                    // on reload app error
                }

                buildNotification(PlaybackStatus.PAUSED)
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume

                try {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer!!.pause()
                        resumePosition = mediaPlayer!!.currentPosition
                        save()
                    }
                    buildNotification(PlaybackStatus.PAUSED)
                } catch (e: Exception) {
                    initMediaPlayer(true)
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level

                if (mediaPlayer!!.isPlaying) mediaPlayer!!.setVolume(0.1f, 0.1f)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        removeNotification()
        stopMedia()
        stopSelf()
        super.onTaskRemoved(rootIntent)
        exitProcess(0)
    }

    /**
     * TrackFocus
     */
    private fun trackFocusGranted(): Boolean {
        trackManager = getSystemService(AUDIO_SERVICE) as AudioManager

        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> trackManager!!.requestAudioFocus(
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .build()
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

            else -> trackManager!!.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun removeTrackFocus() =
        AudioManager.AUDIOFOCUS_REQUEST_GRANTED == trackManager?.abandonAudioFocus(this)

    /**
     * MediaPlayer actions
     */
    internal fun initMediaPlayer(resume: Boolean = false) {
        if (mediaPlayer == null) mediaPlayer = MediaPlayer()

        mediaPlayer!!.apply {
            setOnCompletionListener(this@MediaPlayerService)
            setOnErrorListener(this@MediaPlayerService)
            setOnPreparedListener(this@MediaPlayerService)
            setOnBufferingUpdateListener(this@MediaPlayerService)
            setOnSeekCompleteListener(this@MediaPlayerService)
            setOnInfoListener(this@MediaPlayerService)

            reset()

            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            try {
                setDataSource(curPath)
            } catch (e: Exception) {
                resumePosition = mediaPlayer!!.currentPosition
                save()
                stopSelf()
            }

            isLooping = StorageUtil(applicationContext).loadLooping()
            try {
                prepare()
                if (resume) seekTo(resumePosition)
                (application as MainApplication).musicPlayer = mediaPlayer
            } catch (e: Exception) {
                Toast.makeText(
                    applicationContext,
                    resources.getString(R.string.file_is_corrupted),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun playMedia() {
        if (mediaPlayer == null)
            initMediaPlayer()

        if (!mediaPlayer!!.isPlaying) {
            mediaPlayer!!.start()
            (application as MainApplication).run {
                mainActivity?.apply {
                    buildNotification(PlaybackStatus.PLAYING)
                    time()
                    customize()
                }

                try {
                    ((mainActivity!!.currentFragment!! as TrackListFragment)
                        .adapter!! as TrackListFragment.TrackAdapter).highlight(curTrack.unwrap())
                } catch (e: Exception) {
                }
            }
        }
    }

    internal fun stopMedia() {
        if (mediaPlayer == null)
            return

        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.stop()
            resumePosition = mediaPlayer!!.currentPosition
            save()
            (application as MainApplication).mainActivity?.customize()
        }
    }

    internal fun pauseMedia() {
        if (mediaPlayer == null)
            initMediaPlayer()

        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
            resumePosition = mediaPlayer!!.currentPosition
            save()

            try {
                (application as MainApplication).mainActivity?.customize()
            } catch (e: Exception) {
            }
        }
    }

    internal fun resumeMedia(resumePos: Int = resumePosition) {
        if (mediaPlayer == null)
            initMediaPlayer(true)

        mediaPlayer!!.apply {
            val sv = isLooping
            seekTo(resumePos)
            start()
            isLooping = sv
        }

        try {
            (application as MainApplication).mainActivity!!.run {
                buildNotification(PlaybackStatus.PLAYING)
                time()
                customize()

                ((currentFragment!! as TrackListFragment)
                    .adapter!! as TrackListFragment.TrackAdapter).highlight(curTrack.unwrap())
            }
        } catch (e: Exception) {
        }
    }

    internal fun skipToNext() {
        val looping = mediaPlayer?.isLooping ?: run {
            initMediaPlayer()
            StorageUtil(applicationContext).loadLooping()
        }

        (application as MainApplication).run {
            val curIndex = (curInd + 1).let { if (it == curPlaylist.size) 0 else it }
            curPath = curPlaylist[curIndex].path
        }

        // Update stored index
        StorageUtil(applicationContext).storeTrackPath(curPath)
        stopMedia()

        mediaPlayer!!.reset()
        initMediaPlayer()
        mediaPlayer!!.isLooping = looping
    }

    internal fun skipToPrevious() {
        val looping = mediaPlayer?.isLooping ?: run {
            initMediaPlayer()
            StorageUtil(applicationContext).loadLooping()
        }

        (application as MainApplication).run {
            val curIndex = (curInd - 1).let { if (it < 0) curPlaylist.size - 1 else it }
            curPath = curPlaylist[curIndex].path
        }

        // Update stored index
        StorageUtil(applicationContext).storeTrackPath(curPath)
        stopMedia()

        mediaPlayer!!.reset()
        initMediaPlayer()
        mediaPlayer!!.isLooping = looping
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
                        buildNotification(PlaybackStatus.PAUSED)
                        save()
                        (application as MainApplication).mainActivity?.customize()
                    }

                    TelephonyManager.CALL_STATE_IDLE ->
                        // Phone idle. Start playing

                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false
                                resumeMedia()
                                buildNotification(PlaybackStatus.PLAYING)
                                (application as MainApplication).mainActivity?.customize()
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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
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
                (application as MainApplication).mainActivity?.customize()
            }

            override fun onPause() {
                super.onPause()
                pauseMedia()
                buildNotification(PlaybackStatus.PAUSED)
                save()
                (application as MainApplication).mainActivity?.customize()
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                skipToNext()
                updateMetaData()
                buildNotification(PlaybackStatus.PLAYING)
                (application as MainApplication).mainActivity?.customize()
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                skipToPrevious()
                updateMetaData()
                buildNotification(PlaybackStatus.PLAYING)
                (application as MainApplication).mainActivity?.customize()
            }

            override fun onStop() {
                super.onStop()
                removeNotification()
                resumePosition = mediaPlayer?.currentPosition ?: run {
                    initMediaPlayer()
                    StorageUtil(applicationContext).loadTrackPauseTime()
                }
                save()
                stopSelf()
            }
        })

        mediaSession!!.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY
                        .or(PlaybackState.ACTION_PLAY_PAUSE)
                        .or(PlaybackState.ACTION_PLAY_FROM_MEDIA_ID)
                        .or(PlaybackState.ACTION_PAUSE)
                        .or(PlaybackState.ACTION_SKIP_TO_NEXT)
                        .or(PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                )
                .setState(
                    PlaybackState.STATE_PAUSED,
                    resumePosition.toLong(),
                    1.0F,
                    SystemClock.elapsedRealtime()
                )
                .build()
        )
    }

    internal fun updateMetaData() = runBlocking {
        val activeTrack = curTrack.unwrap()
        launch(Dispatchers.Default) {
            val task = (application as MainApplication).getAlbumPictureAsync(curPath)
            mediaSession!!.setMetadata(
                MediaMetadata.Builder()
                    .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, task.await())
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, activeTrack.artist)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, activeTrack.playlist)
                    .putString(MediaMetadata.METADATA_KEY_TITLE, activeTrack.title)
                    .build()
            )
        }
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
        val playPauseAction: PendingIntent?

        // Build a new notification according to the current state of the MediaPlayer

        val notificationAction = when (playbackStatus) {
            PlaybackStatus.PLAYING -> {
                playPauseAction = playbackAction(1)
                pause
            }

            PlaybackStatus.PAUSED -> {
                playPauseAction = playbackAction(0)
                play
            }
        }

        val activeTrack = curTrack.unwrap()

        val customize = { builder: Notification.Builder ->
            runBlocking {
                val task = (application as MainApplication).getAlbumPictureAsync(curPath)
                async {
                    builder.setShowWhen(false)                                  // Set the Notification style
                        .setStyle(
                            Notification.MediaStyle()                           // Attach our MediaSession token
                                .setMediaSession(mediaSession!!.sessionToken)   // Show our playback controls in the compat view
                                .setShowActionsInCompactView(0, 1, 2)
                        )                                                       // Set the Notification color
                        .setColor(Params.instance.theme.rgb)                    // Set the large and small icons
                        .setLargeIcon(task.await())
                        .setSmallIcon(android.R.drawable.stat_sys_headset)      // Set Notification content information
                        .setSubText(activeTrack.playlist.let {
                            if (it == "<unknown>" ||
                                it == curPath.split('/').takeLast(2).first()
                            ) resources.getString(R.string.unknown_album) else it
                        })
                        .setContentText(activeTrack.artist
                            .let { if (it == "<unknown>") resources.getString(R.string.unknown_artist) else it })
                        .setContentTitle(activeTrack.title
                            .let { if (it == "<unknown>") resources.getString(R.string.unknown_track) else it })
                        .addAction(
                            Notification
                                .Action
                                .Builder(
                                    Icon.createWithResource("", prev),
                                    "previous",
                                    playbackAction(3)
                                )
                                .build()
                        )
                        .addAction(
                            Notification
                                .Action
                                .Builder(
                                    Icon.createWithResource("", notificationAction),
                                    "pause",
                                    playPauseAction
                                )
                                .build()
                        )
                        .addAction(
                            Notification
                                .Action
                                .Builder(
                                    Icon.createWithResource("", next),
                                    "next",
                                    playbackAction(2)
                                )
                                .build()
                        )
                }
            }
        }

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> customize(
                Notification.Builder(this, MEDIA_CHANNEL_ID)
            ).let { runBlocking { startForeground(NOTIFICATION_ID, it.await().build()) } }

            else -> customize(Notification.Builder(this))
                .let { runBlocking { startForeground(NOTIFICATION_ID, it.await().build()) } }
        }
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
                transportControls!!.pause().apply {
                    try {
                        resumePosition = mediaPlayer?.currentPosition ?: run {
                            initMediaPlayer()
                            StorageUtil(applicationContext).loadTrackPauseTime()
                        }
                        save()
                    } catch (e: Exception) {
                        // on close app error
                        removeNotification()
                        stopSelf()
                    }
                }

            actionString.equals(ACTION_NEXT, ignoreCase = true) ->
                transportControls!!.skipToNext()

            actionString.equals(ACTION_PREVIOUS, ignoreCase = true) ->
                transportControls!!.skipToPrevious()

            actionString.equals(ACTION_STOP, ignoreCase = true) ->
                transportControls!!.stop().apply {
                    resumePosition = mediaPlayer?.currentPosition ?: run {
                        initMediaPlayer()
                        StorageUtil(applicationContext).loadTrackPauseTime()
                    }
                    save()
                }
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

    internal fun save() = (application as MainApplication).save()

    internal fun audioFocusHelp() {
        if (!trackFocusGranted()) {
            resumePosition = mediaPlayer?.currentPosition ?: run {
                initMediaPlayer()
                StorageUtil(applicationContext).loadTrackPauseTime()
            }
            save()
            removeNotification()
            stopSelf()
        }

        if (mediaSessionManager == null) {
            try {
                initMediaSession()
                initMediaPlayer()
            } catch (e: Exception) {
                try {
                    resumePosition = mediaPlayer?.currentPosition ?: run {
                        initMediaPlayer()
                        StorageUtil(applicationContext).loadTrackPauseTime()
                    }
                    save()
                } catch (e: Exception) {
                    // on close app error
                }

                removeNotification()
                stopSelf()
            }
        }

        when (mediaPlayer) {
            null -> (getSystemService(NOTIFICATION_SERVICE)!! as NotificationManager).cancelAll()

            else -> {
                (application as MainApplication).musicPlayer = mediaPlayer!!
                mediaPlayer!!.isLooping = StorageUtil(applicationContext).loadLooping().let {
                    if (startFromLooping) !it else it
                }
            }
        }
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