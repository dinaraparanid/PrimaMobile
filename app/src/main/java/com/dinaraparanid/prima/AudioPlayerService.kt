package com.dinaraparanid.prima

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Icon
import android.media.*
import android.media.AudioManager.*
import android.media.MediaPlayer.*
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
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
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import arrow.core.None
import arrow.core.Some
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.equalizer.EqualizerModel
import com.dinaraparanid.prima.utils.equalizer.EqualizerSettings
import com.dinaraparanid.prima.utils.extensions.unwrap
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrackListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

/**
 * [Service] to play music
 */

class AudioPlayerService : Service(), OnCompletionListener,
    OnPreparedListener, OnErrorListener, OnSeekCompleteListener, OnInfoListener,
    OnBufferingUpdateListener, OnAudioFocusChangeListener {
    private companion object {
        private const val ACTION_PLAY = "com.dinaraparanid.prima.media.ACTION_PLAY"
        private const val ACTION_PAUSE = "com.dinaraparanid.prima.media.ACTION_PAUSE"
        private const val ACTION_PREVIOUS = "com.dinaraparanid.prima.media.ACTION_PREVIOUS"
        private const val ACTION_NEXT = "com.dinaraparanid.prima.media.ACTION_NEXT"
        private const val ACTION_STOP = "com.dinaraparanid.prima.media.ACTION_STOP"
        private const val ACTION_LOOP_TRACK = "com.dinaraparanid.prima.media.ACTION_LOOP_TRACK"
        private const val ACTION_NO_LOOP = "com.dinaraparanid.prima.media.ACTION_NO_LOOP"
        private const val ACTION_LOOP_PLAYLIST =
            "com.dinaraparanid.prima.media.ACTION_LOOP_PLAYLIST"
        private const val ACTION_LIKE = "com.dinaraparanid.prima.media.ACTION_LIKE"
        private const val ACTION_NO_LIKE = "com.dinaraparanid.prima.media.ACTION_NO_LIKE"
        private const val ACTION_REMOVE = "com.dinaraparanid.prima.media.ACTION_REMOVE"
        private const val MEDIA_CHANNEL_ID = "media_playback_channel"
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
    private var audioManager: AudioManager? = null

    // Binder given to clients
    private val iBinder: IBinder = LocalBinder()

    private var resumePosition = 0
    private var isStarted = false
    private var startFromLooping = false
    private var startFromPause = false
    private var isLiked = false

    // Handle incoming phone calls
    private var ongoingCall = false
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyManager: TelephonyManager? = null
    private lateinit var notificationAlbumImage: Bitmap

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

    internal inline val isLooping1
        get() = Params.instance.loopingStatus == Params.Companion.Looping.TRACK

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
                highlightedRow = Some(curTrack.unwrap().path)
                mainActivity?.currentFragment?.takeIf { it is AbstractTrackListFragment<*> }?.let {
                    ((it as AbstractTrackListFragment<*>).adapter!!).highlight(curTrack.unwrap().path)
                }
            }

            // A PLAY_NEW_TRACK action received
            // Reset mediaPlayer to play the new Track

            stopMedia()
            mediaPlayer?.reset()
            initMediaPlayer()
            updateMetaDataAsync(true)
            mediaPlayer!!.isLooping = isLooping1
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

            updateMetaDataAsync(false)
            buildNotification(PlaybackStatus.PLAYING)
        }
    }

    private val pausePlayingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            pauseMedia()
            updateMetaDataAsync(false)
            buildNotification(PlaybackStatus.PAUSED)
        }
    }

    private val setLoopingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (mediaPlayer == null)
                initMediaPlayer()

            mediaPlayer!!.isLooping = isLooping1
            StorageUtil(applicationContext).storeLooping(Params.instance.loopingStatus)

            buildNotification(
                when {
                    mediaPlayer!!.isPlaying -> PlaybackStatus.PLAYING
                    else -> PlaybackStatus.PAUSED
                }
            )
        }
    }

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            resumePosition = mediaPlayer?.currentPosition ?: run {
                initMediaPlayer()
                StorageUtil(applicationContext).loadTrackPauseTime()
            }

            saveIfNeeded()
            removeNotification()
            stopSelf()
            (application as MainApplication).mainActivity?.customize(false)
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

        isLiked = runBlocking {
            FavouriteRepository.instance.getTrackAsync(curPath).await()
        } != null
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

            saveIfNeeded()
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
        inline val service: AudioPlayerService
            get() = this@AudioPlayerService
    }

    override fun onBufferingUpdate(mp: MediaPlayer, percent: Int): Unit = Unit

    override fun onCompletion(mp: MediaPlayer) {
        // Invoked when playback of a media source has completed.

        stopMedia()

        (application as MainApplication).mainActivity?.run {
            when (Params.instance.loopingStatus) {
                Params.Companion.Looping.TRACK -> playAudio(curPath)
                Params.Companion.Looping.PLAYLIST -> playNextAndUpdUI()
                Params.Companion.Looping.NONE -> playNextOrStop()
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

        isStarted -> playMedia().apply {
            buildNotification(PlaybackStatus.PLAYING)
        }

        else -> {
            isStarted = true

            when ((application as MainApplication).startPath) {
                None -> playMedia()

                else -> when ((application as MainApplication).startPath.unwrap()) {
                    curPath -> resumeMedia()
                    else -> playMedia()
                }
            }
        }
    }

    override fun onSeekComplete(mp: MediaPlayer): Unit = Unit

    override fun onAudioFocusChange(focusState: Int) {
        when (focusState) {
            AUDIOFOCUS_GAIN -> {
                // Resume playback

                /*if (mediaPlayer == null) initMediaPlayer(true)
                else if (!mediaPlayer!!.isPlaying) mediaPlayer!!.start()
                mediaPlayer!!.setVolume(1.0f, 1.0f)
                mediaSession!!.isActive = true
                buildNotification(PlaybackStatus.PLAYING, false)*/
            }

            AUDIOFOCUS_LOSS -> {
                // Lost focus for an unbounded amount of time

                if (mediaPlayer != null) {
                    if (mediaPlayer!!.isPlaying) {
                        mediaPlayer!!.stop()
                        resumePosition = mediaPlayer!!.currentPosition
                        saveIfNeeded()
                    }

                    mediaPlayer!!.reset()
                    mediaPlayer!!.release()
                    mediaPlayer = null
                }

                buildNotification(PlaybackStatus.PAUSED)
            }

            AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume

                try {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer!!.pause()
                        resumePosition = mediaPlayer!!.currentPosition
                        saveIfNeeded()
                    }
                    buildNotification(PlaybackStatus.PAUSED)
                } catch (e: Exception) {
                    initMediaPlayer(true)
                }
            }

            AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level

                if (mediaPlayer!!.isPlaying) mediaPlayer!!.setVolume(0.1f, 0.1f)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        removeNotification()
        stopMedia()
        stopSelf()

        try {
            (application as MainApplication).run {
                equalizer.release()
                bassBoost?.release()
                presetReverb?.release()
            }
        } catch (ignored: Exception) {
            // equalizer isn't used
        }

        super.onTaskRemoved(rootIntent)
        exitProcess(0)
    }

    private fun requestTrackFocus() =
        (getSystemService(AUDIO_SERVICE) as AudioManager).let { audioManager ->
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> audioManager.requestAudioFocus(
                    AudioFocusRequest.Builder(AUDIOFOCUS_GAIN)
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAcceptsDelayedFocusGain(true)
                        .setOnAudioFocusChangeListener(this)
                        .build()
                )

                else -> audioManager.requestAudioFocus(
                    this,
                    STREAM_MUSIC,
                    AUDIOFOCUS_GAIN
                )
            }
        }

    private val isTrackFocusGranted: Boolean
        get() = requestTrackFocus() == AUDIOFOCUS_REQUEST_GRANTED

    private fun removeTrackFocus() =
        AUDIOFOCUS_REQUEST_GRANTED == audioManager?.abandonAudioFocus(this)

    /**
     * initializes and prepares [MediaPlayer] and sets actions
     */

    @Synchronized
    internal fun initMediaPlayer(resume: Boolean = false) {
        if (mediaPlayer == null) mediaPlayer = MediaPlayer()

        mediaPlayer!!.apply {
            (application as MainApplication).audioSessionId = audioSessionId

            setOnCompletionListener(this@AudioPlayerService)
            setOnErrorListener(this@AudioPlayerService)
            setOnPreparedListener(this@AudioPlayerService)
            setOnBufferingUpdateListener(this@AudioPlayerService)
            setOnSeekCompleteListener(this@AudioPlayerService)
            setOnInfoListener(this@AudioPlayerService)

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
                saveIfNeeded()
                stopSelf()
            }

            isLooping = isLooping1

            try {
                prepare()
                if (resume) seekTo(resumePosition)

                (application as MainApplication).run {
                    musicPlayer = this@apply

                    if (Params.instance.isStartingWithEqualizer)
                        startEqualizer()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    applicationContext,
                    resources.getString(R.string.file_is_corrupted),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    @Synchronized
    private fun initEqualizer() {
        EqualizerSettings.instance.isEditing = true

        if (EqualizerSettings.instance.equalizerModel == null) {
            EqualizerSettings.instance.equalizerModel = EqualizerModel(applicationContext).apply {
                reverbPreset = PresetReverb.PRESET_NONE
                bassStrength = (1000 / 19).toShort()
            }
        }

        val audioSessionId = mediaPlayer!!.audioSessionId
        val app = application as MainApplication

        app.equalizer = Equalizer(0, audioSessionId)

        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q)
            app.bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = EqualizerSettings.instance.isEqualizerEnabled
                properties = BassBoost.Settings(properties.toString()).apply {
                    strength = StorageUtil(applicationContext).loadBassStrength()
                }
            }

        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q)
            app.presetReverb = PresetReverb(0, audioSessionId).apply {
                try {
                    preset = StorageUtil(applicationContext).loadReverbPreset()
                } catch (ignored: Exception) {
                    // not supported
                }
                enabled = EqualizerSettings.instance.isEqualizerEnabled
            }

        app.equalizer.enabled = EqualizerSettings.instance.isEqualizerEnabled

        val seekBarPoses = StorageUtil(applicationContext).loadEqualizerSeekbarsPos()
            ?: EqualizerSettings.instance.seekbarPos

        when (EqualizerSettings.instance.presetPos) {
            0 -> (0 until app.equalizer.numberOfBands).forEach {
                app.equalizer.setBandLevel(
                    it.toShort(),
                    seekBarPoses[it].toShort()
                )
            }

            else -> app.equalizer.usePreset(EqualizerSettings.instance.presetPos.toShort())
        }
    }

    @Synchronized
    private fun playMedia() {
        if (mediaPlayer == null)
            initMediaPlayer()

        requestTrackFocus()

        if (!mediaPlayer!!.isPlaying) {
            mediaPlayer!!.run {
                start()

                if (EqualizerSettings.instance.isEqualizerEnabled) {
                    initEqualizer()

                    val loader = StorageUtil(applicationContext)
                    playbackParams = PlaybackParams()
                        .setPitch(loader.loadPitch())
                        .setSpeed(loader.loadSpeed())
                }

                (application as MainApplication).mainActivity?.initAudioVisualizer()
            }

            (application as MainApplication).run {
                mainActivity?.apply {
                    buildNotification(PlaybackStatus.PLAYING)
                    reinitializePlayingCoroutine()
                    customize(true)
                }

                try {
                    ((mainActivity!!.currentFragment!! as AbstractTrackListFragment<*>).adapter!!)
                        .highlight(curTrack.unwrap().path)
                } catch (ignored: Exception) {
                }
            }
        }
    }

    @Synchronized
    internal fun stopMedia() {
        if (mediaPlayer == null)
            return

        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.stop()
            resumePosition = mediaPlayer!!.currentPosition
            saveIfNeeded()
        }
    }

    @Synchronized
    internal fun pauseMedia() {
        if (mediaPlayer == null)
            initMediaPlayer()

        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
            resumePosition = mediaPlayer!!.currentPosition
            saveIfNeeded()

            try {
                (application as MainApplication).mainActivity?.customize(false)
            } catch (ignored: Exception) {
            }
        }
    }

    @Synchronized
    internal fun resumeMedia(resumePos: Int = resumePosition) {

        if (mediaPlayer == null)
            initMediaPlayer(true)

        requestTrackFocus()

        mediaPlayer!!.run {
            val sv = isLooping
            seekTo(resumePos)

            if (EqualizerSettings.instance.isEqualizerEnabled) {
                initEqualizer()

                val loader = StorageUtil(applicationContext)
                playbackParams = PlaybackParams()
                    .setPitch(loader.loadPitch())
                    .setSpeed(loader.loadSpeed())
            }

            start()
            isLooping = sv
        }

        (application as MainApplication).mainActivity?.initAudioVisualizer()

        try {
            (application as MainApplication).mainActivity!!.run {
                buildNotification(PlaybackStatus.PLAYING)
                reinitializePlayingCoroutine()
                customize(false)

                ((currentFragment!! as AbstractTrackListFragment<*>).adapter!!)
                    .highlight(curTrack.unwrap().path)
            }
        } catch (ignored: Exception) {
        }
    }

    @Synchronized
    internal fun skipToNext() {
        (application as MainApplication).run {
            val curIndex = (curInd + 1).let { if (it == curPlaylist.size) 0 else it }
            curPath = curPlaylist[curIndex].path
        }

        // Update stored index
        StorageUtil(applicationContext).storeTrackPath(curPath)
        stopMedia()

        mediaPlayer!!.reset()
        initMediaPlayer()
        mediaPlayer!!.isLooping = isLooping1
    }

    @Synchronized
    internal fun skipToPrevious() {
        (application as MainApplication).run {
            val curIndex = (curInd - 1).let { if (it < 0) curPlaylist.size - 1 else it }
            curPath = curPlaylist[curIndex].path
        }

        // Update stored index
        StorageUtil(applicationContext).storeTrackPath(curPath)
        stopMedia()

        mediaPlayer!!.reset()
        initMediaPlayer()
        mediaPlayer!!.isLooping = isLooping1
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
                    TelephonyManager.CALL_STATE_OFFHOOK, TelephonyManager.CALL_STATE_RINGING ->
                        if (mediaPlayer != null) {
                            val wasPlaying = mediaPlayer!!.isPlaying
                            pauseMedia()
                            ongoingCall = wasPlaying
                            buildNotification(PlaybackStatus.PAUSED)
                            saveIfNeeded()
                            (application as MainApplication).mainActivity?.customize(false)
                        }

                    TelephonyManager.CALL_STATE_IDLE ->
                        // Phone idle. Start playing

                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false
                                resumeMedia()
                                buildNotification(PlaybackStatus.PLAYING)
                                (application as MainApplication).mainActivity?.customize(false)
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
     * initializes [MediaSession] and [Notification] actions
     */

    @Synchronized
    private fun initMediaSession() {
        if (mediaSessionManager != null) return

        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE)!! as MediaSessionManager
        mediaSession = MediaSession(applicationContext, "AudioPlayer")
        transportControls = mediaSession!!.controller.transportControls
        mediaSession!!.isActive = true

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            mediaSession!!.setFlags(
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
                    .or(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
            )

        updateMetaDataAsync(true)

        mediaSession!!.setCallback(object : MediaSession.Callback() {
            override fun onPlay() {
                super.onPlay()
                resumeMedia()
                buildNotification(PlaybackStatus.PLAYING)
                (application as MainApplication).mainActivity?.customize(false)
            }

            override fun onPause() {
                super.onPause()
                pauseMedia()
                updateMetaDataAsync(false)
                buildNotification(PlaybackStatus.PAUSED)
                saveIfNeeded()
                (application as MainApplication).mainActivity?.customize(false)
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                skipToNext()
                updateMetaDataAsync(true)
                buildNotification(PlaybackStatus.PLAYING)
                (application as MainApplication).mainActivity?.customize(true)
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                skipToPrevious()
                updateMetaDataAsync(true)
                buildNotification(PlaybackStatus.PLAYING)
                (application as MainApplication).mainActivity?.customize(true)
            }

            override fun onStop() {
                super.onStop()
                removeNotification()
                resumePosition = mediaPlayer?.currentPosition ?: run {
                    initMediaPlayer()
                    StorageUtil(applicationContext).loadTrackPauseTime()
                }
                saveIfNeeded()
                stopSelf()
            }
        })

        mediaSession!!.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY
                            or PlaybackState.ACTION_PLAY_PAUSE
                            or PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                            or PlaybackState.ACTION_PAUSE
                            or PlaybackState.ACTION_SKIP_TO_NEXT
                            or PlaybackState.ACTION_SKIP_TO_PREVIOUS
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

    /**
     * Updates metadata for notification asynchronously
     */

    @Synchronized
    internal fun updateMetaDataAsync(updImage: Boolean) = runBlocking {
        val activeTrack = curTrack.unwrap()
        launch(Dispatchers.Default) {
            mediaSession!!.setMetadata(
                MediaMetadata.Builder()
                    .putBitmap(
                        MediaMetadata.METADATA_KEY_ALBUM_ART,
                        when {
                            updImage -> (application as MainApplication)
                                .getAlbumPictureAsync(
                                    curPath,
                                    Params.instance.isPlaylistsImagesShown
                                )
                                .await()
                                .also { notificationAlbumImage = it }
                            else -> notificationAlbumImage
                        }
                    )
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, activeTrack.artist)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, activeTrack.playlist)
                    .putString(MediaMetadata.METADATA_KEY_TITLE, activeTrack.title)
                    .build()
            )
        }
    }

    /**
     * For Android Api 28+
     *
     * Build a new notification according to
     * the current state of the MediaPlayer
     * @param playbackStatus playing or paused
     */

    @RequiresApi(Build.VERSION_CODES.P)
    private fun buildNotificationPie(playbackStatus: PlaybackStatus) {
        val activeTrack = curTrack.unwrap()

        val notificationView = RemoteViews(
            applicationContext.packageName,
            R.layout.notification_layout
        ).apply {
            setTextViewText(R.id.notification_title, activeTrack.title)
            setTextColor(R.id.notification_title, Color.WHITE)
            setTextViewTextSize(R.id.notification_title, TypedValue.COMPLEX_UNIT_SP, 16F)

            setTextViewText(R.id.notification_artist_album, activeTrack.artistAndAlbumFormatted)
            setTextColor(R.id.notification_artist_album, Color.WHITE)
            setTextViewTextSize(R.id.notification_artist_album, TypedValue.COMPLEX_UNIT_SP, 12F)

            setImageViewBitmap(
                R.id.notification_album_image,
                runBlocking {
                    (application as MainApplication).getAlbumPictureAsync(
                        activeTrack.path,
                        true
                    ).await()
                }
            )

            setImageViewResource(
                R.id.notification_repeat_button, when (Params.instance.loopingStatus) {
                    Params.Companion.Looping.PLAYLIST -> R.drawable.repeat_white
                    Params.Companion.Looping.TRACK -> R.drawable.repeat_1_white
                    Params.Companion.Looping.NONE -> R.drawable.no_repeat_white
                }
            )

            setImageViewResource(
                R.id.notification_play_button, when (playbackStatus) {
                    PlaybackStatus.PLAYING -> R.drawable.pause_white
                    PlaybackStatus.PAUSED -> R.drawable.play_white
                }
            )

            setImageViewResource(
                R.id.notification_like_button, when {
                    runBlocking {
                        FavouriteRepository.instance.getTrackAsync(activeTrack.path).await()
                    } != null -> R.drawable.heart_like_white

                    else -> R.drawable.heart_white
                }
            )

            setOnClickPendingIntent(
                R.id.notification_repeat_button, when (Params.instance.loopingStatus) {
                    Params.Companion.Looping.PLAYLIST -> playbackAction(4)
                    Params.Companion.Looping.TRACK -> playbackAction(5)
                    Params.Companion.Looping.NONE -> playbackAction(6)
                }
            )

            setOnClickPendingIntent(R.id.notification_prev_track, playbackAction(3))

            setOnClickPendingIntent(
                R.id.notification_play_button, when (playbackStatus) {
                    PlaybackStatus.PLAYING -> playbackAction(1)
                    PlaybackStatus.PAUSED -> playbackAction(0)
                }
            )

            setOnClickPendingIntent(R.id.notification_next_track, playbackAction(2))

            setOnClickPendingIntent(
                R.id.notification_like_button, when {
                    isLiked -> playbackAction(8)
                    else -> playbackAction(7)
                }
            )
        }

        startForeground(
            NOTIFICATION_ID,
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                    Notification.Builder(applicationContext, MEDIA_CHANNEL_ID)
                        .setStyle(Notification.DecoratedCustomViewStyle())
                        .setContent(notificationView)
                        .setCustomContentView(notificationView)
                        .setSmallIcon(R.drawable.cat)
                        .setShowWhen(false)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .build()

                else -> NotificationCompat.Builder(applicationContext, MEDIA_CHANNEL_ID)
                    .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                    .setContent(notificationView)
                    .setCustomContentView(notificationView)
                    .setSmallIcon(R.drawable.cat)
                    .setShowWhen(false)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .build()
            }
        )
    }

    /**
     * For Android Api 27-
     *
     * Builds Notification when playing or paused
     * @param playbackStatus playing or paused
     * @param updImage does track image need update
     */

    private fun buildNotificationCompat(playbackStatus: PlaybackStatus, updImage: Boolean) {
        val play = R.drawable.play_white
        val pause = R.drawable.pause_white
        val prev = R.drawable.prev_track_white
        val next = R.drawable.next_track_white
        val repeatPlaylist = R.drawable.repeat_white
        val repeatTrack = R.drawable.repeat_1_white
        val noRepeat = R.drawable.no_repeat_white
        val noLike = R.drawable.heart_white
        val like = R.drawable.heart_like_white

        val playPauseAction: PendingIntent?
        val loopAction: PendingIntent?
        val likeAction: PendingIntent?

        // Build a new notification according to the current state of the MediaPlayer

        val playAction = when (playbackStatus) {
            PlaybackStatus.PLAYING -> {
                playPauseAction = playbackAction(1)
                pause
            }

            PlaybackStatus.PAUSED -> {
                playPauseAction = playbackAction(0)
                play
            }
        }

        val loopingAction = when (Params.instance.loopingStatus) {
            Params.Companion.Looping.PLAYLIST -> {
                loopAction = playbackAction(4)
                repeatPlaylist
            }

            Params.Companion.Looping.TRACK -> {
                loopAction = playbackAction(5)
                repeatTrack
            }

            Params.Companion.Looping.NONE -> {
                loopAction = playbackAction(6)
                noRepeat
            }
        }

        val likingAction = when {
            isLiked -> {
                likeAction = playbackAction(8)
                like
            }

            else -> {
                likeAction = playbackAction(7)
                noLike
            }
        }

        val activeTrack = curTrack.unwrap()

        val customize = { builder: Notification.Builder ->
            runBlocking {
                async {
                    builder.setShowWhen(false)                                  // Set the Notification style
                        .setStyle(
                            Notification.MediaStyle()                           // Attach our MediaSession token
                                .setMediaSession(mediaSession!!.sessionToken)   // Show our playback controls in the compat view
                                .setShowActionsInCompactView(1, 2, 3)
                        )                                                       // Set the Notification color
                        .setColor(Params.instance.primaryColor)                 // Set the large and small icons
                        .setLargeIcon(when {
                            updImage -> (application as MainApplication)
                                .getAlbumPictureAsync(
                                    curPath,
                                    Params.instance.isPlaylistsImagesShown
                                )
                                .await()
                                .also { notificationAlbumImage = it }
                            else -> notificationAlbumImage
                        })
                        .setSmallIcon(R.drawable.cat)                           // Set Notification content information
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
                            Notification.Action.Builder(
                                Icon.createWithResource("", loopingAction),
                                "looping",
                                loopAction
                            ).build()
                        )
                        .addAction(
                            Notification.Action.Builder(
                                Icon.createWithResource("", prev),
                                "previous",
                                playbackAction(3)
                            ).build()
                        )
                        .addAction(
                            Notification.Action.Builder(
                                Icon.createWithResource("", playAction),
                                "pause",
                                playPauseAction
                            ).build()
                        )
                        .addAction(
                            Notification.Action.Builder(
                                Icon.createWithResource("", next),
                                "next",
                                playbackAction(2)
                            ).build()
                        )
                        .addAction(
                            Notification.Action.Builder(
                                Icon.createWithResource("", likingAction),
                                "like",
                                likeAction
                            ).build()
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

    /**
     * Builds Notification when playing or paused
     * @param playbackStatus playing or paused
     * @param updImage does track image need update
     */

    @Synchronized
    internal fun buildNotification(playbackStatus: PlaybackStatus, updImage: Boolean = true) =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> when {
                Params.instance.isUsingAndroidNotification ->
                    buildNotificationCompat(playbackStatus, updImage)

                else -> buildNotificationPie(playbackStatus)
            }

            else -> buildNotificationCompat(playbackStatus, updImage)
        }

    /**
     * 0 -> Play
     *
     * 1 -> Pause
     *
     * 2 -> Next track
     *
     * 3 -> Previous track
     *
     * 4 -> Set playlist looping
     *
     * 5 -> Set track looping
     *
     * 6 -> Set no looping
     *
     * 7 -> Remove like from track
     *
     * 8 -> Set like to track
     *
     * 9 -> Remove notification
     */

    @Synchronized
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun playbackAction(actionNumber: Int): PendingIntent? {
        val playbackAction = Intent(this, AudioPlayerService::class.java)

        return when (actionNumber) {
            in 0..9 -> {
                playbackAction.action = when (actionNumber) {
                    0 -> ACTION_PLAY            // Play
                    1 -> ACTION_PAUSE           // Pause
                    2 -> ACTION_NEXT            // Next track
                    3 -> ACTION_PREVIOUS        // Previous track
                    4 -> ACTION_LOOP_PLAYLIST   // Playlist looping
                    5 -> ACTION_LOOP_TRACK      // Track looping
                    6 -> ACTION_NO_LOOP         // No looping
                    7 -> ACTION_LIKE            // Like track
                    8 -> ACTION_NO_LIKE         // Not like track
                    else -> ACTION_REMOVE       // Remove notification
                }

                PendingIntent.getService(this, actionNumber, playbackAction, 0)
            }

            else -> null
        }
    }

    @Synchronized
    internal fun removeNotification() =
        (getSystemService(NOTIFICATION_SERVICE)!! as NotificationManager)
            .cancel(NOTIFICATION_ID)

    @Synchronized
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
                        saveIfNeeded()
                    } catch (e: Exception) {
                        // on close app error
                        removeNotification()
                        stopSelf()
                    }
                }

            actionString.equals(ACTION_NEXT, ignoreCase = true) -> transportControls!!.skipToNext()

            actionString.equals(ACTION_PREVIOUS, ignoreCase = true) ->
                transportControls!!.skipToPrevious()

            actionString.equals(ACTION_LOOP_PLAYLIST, ignoreCase = true) ||
                    actionString.equals(ACTION_LOOP_TRACK, ignoreCase = true) ||
                    actionString.equals(ACTION_NO_LOOP, ignoreCase = true) -> {
                buildNotification(
                    when (mediaPlayer?.isPlaying) {
                        true -> PlaybackStatus.PLAYING
                        else -> PlaybackStatus.PAUSED
                    }
                )
                (application as MainApplication).mainActivity?.updateLooping()
            }

            actionString.equals(ACTION_LIKE, ignoreCase = true) -> runBlocking {
                isLiked = !isLiked
                FavouriteRepository.instance.addTrackAsync(curTrack.unwrap().asFavourite())
                buildNotification(
                    when (mediaPlayer?.isPlaying) {
                        true -> PlaybackStatus.PLAYING
                        else -> PlaybackStatus.PAUSED
                    }
                )

                (application as MainApplication).mainActivity?.setLikeButtonImage(isLiked)
            }

            actionString.equals(ACTION_NO_LIKE, ignoreCase = false) -> runBlocking {
                isLiked = !isLiked
                FavouriteRepository.instance.removeTrackAsync(curTrack.unwrap().asFavourite())
                buildNotification(
                    when (mediaPlayer?.isPlaying) {
                        true -> PlaybackStatus.PLAYING
                        else -> PlaybackStatus.PAUSED
                    }
                )

                (application as MainApplication).mainActivity?.setLikeButtonImage(isLiked)
            }

            actionString.equals(ACTION_REMOVE, ignoreCase = true) -> removeNotification()

            actionString.equals(ACTION_STOP, ignoreCase = true) ->
                transportControls!!.stop().apply {
                    resumePosition = mediaPlayer?.currentPosition ?: run {
                        initMediaPlayer()
                        StorageUtil(applicationContext).loadTrackPauseTime()
                    }
                    saveIfNeeded()
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
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> NotificationManager.IMPORTANCE_DEFAULT
                        else -> 0
                    }
                ).apply {
                    this.description = "Media playback controls"
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
    }

    /**
     * Saves playing progress if user wishes it and checked tracks
     */

    internal fun saveIfNeeded() = try {
        StorageUtil(applicationContext).run {
            val app = application as MainApplication

            Params.instance.run {
                if (saveCurTrackAndPlaylist) {
                    storeCurPlaylist(app.curPlaylist)
                    storeTrackPauseTime(app.musicPlayer!!.currentPosition)
                    curPath.takeIf { it != "_____ЫЫЫЫЫЫЫЫ_____" }?.let(::storeTrackPath)
                }
            }
        }
    } catch (ignored: Exception) {
        // music player isn't initialized
    }

    internal fun audioFocusHelp() {
        if (!isTrackFocusGranted) {
            resumePosition = mediaPlayer?.currentPosition ?: run {
                initMediaPlayer()
                StorageUtil(applicationContext).loadTrackPauseTime()
            }
            saveIfNeeded()
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
                    saveIfNeeded()
                } catch (ignored: Exception) {
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
                mediaPlayer!!.isLooping = isLooping1
            }
        }
    }
}