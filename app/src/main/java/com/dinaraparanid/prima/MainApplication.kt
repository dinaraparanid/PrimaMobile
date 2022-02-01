package com.dinaraparanid.prima

import android.Manifest
import android.app.AlertDialog
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.bumptech.glide.Glide
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.core.DefaultTrack
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.databases.repositories.ImageRepository
import com.dinaraparanid.prima.databases.repositories.StatisticsRepository
import com.dinaraparanid.prima.services.MediaScannerService
import com.dinaraparanid.prima.utils.*
import com.dinaraparanid.prima.utils.equalizer.EqualizerModel
import com.dinaraparanid.prima.utils.equalizer.EqualizerSettings
import com.dinaraparanid.prima.utils.extensions.playbackParam
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.Loader
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import java.lang.ref.WeakReference
import kotlin.concurrent.thread

class MainApplication : Application(), Loader<AbstractPlaylist> {
    private companion object {
        private const val AUDIO_SERVICE_NAME = ".services.AudioPlayerService"
        private const val CONVERTER_SERVICE_NAME = ".services.ConverterService"
        private const val SLEEP_SERVICE_NAME = ".services.SleepService"
        private const val MIC_RECORDING_SERVICE_NAME = ".services.MicRecordService"
        private const val PLAYBACK_RECORDING_SERVICE_NAME = ".services.PlaybackRecordService"
        private const val MEDIA_SCANNER_SERVICE_NAME = ".services.MediaScannerService"
    }

    internal lateinit var equalizer: Equalizer

    // No supported for Android 10
    internal var bassBoost: BassBoost? = null
        get() = if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q) field else null

    // No supported for Android 10
    internal var presetReverb: PresetReverb? = null
        get() = if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q) field else null

    internal var mainActivity: WeakReference<MainActivity> = WeakReference(null)
    internal var musicPlayer: MediaPlayer? = null
    internal var startPath: Option<String> = None
    internal var highlightedPath: Option<String> = None
    internal var playingBarIsVisible = false
    internal val allTracks = DefaultPlaylist()
    private val mutex = Mutex()

    internal inline val audioSessionId
        get() = try { musicPlayer?.audioSessionId } catch (ignored: Exception) { 0 }

    @Volatile
    internal var isMicRecording = false

    @Volatile
    internal var isPlaybackRecording = false

    internal var isAudioServiceBounded = false
        private set

    internal var isConverterServiceBounded = false
        private set

    internal var isSleepServiceBounded = false
        private set

    internal var isMicRecordingServiceBounded = false
        private set

    internal var isPlaybackRecordingServiceBounded = false
        private set

    internal var isMediaScannerServiceBounded = false
        private set

    internal val curPlaylist: AbstractPlaylist by lazy {
        StorageUtil.instance.loadCurPlaylist() ?: DefaultPlaylist()
    }

    internal val audioServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            if (name.shortClassName == AUDIO_SERVICE_NAME)
                isAudioServiceBounded = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            if (name.shortClassName == AUDIO_SERVICE_NAME)
                isAudioServiceBounded = false
        }
    }

    internal val converterServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            if (name.shortClassName == CONVERTER_SERVICE_NAME)
                isConverterServiceBounded = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            if (name.shortClassName == CONVERTER_SERVICE_NAME)
                isConverterServiceBounded = false
        }
    }

    internal val sleepServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder?) {
            if (name.shortClassName == SLEEP_SERVICE_NAME)
                isSleepServiceBounded = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            if (name.shortClassName == SLEEP_SERVICE_NAME)
                isSleepServiceBounded = false
        }
    }

    internal val micRecordServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder?) {
            if (name.shortClassName == MIC_RECORDING_SERVICE_NAME)
                isMicRecordingServiceBounded = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            if (name.shortClassName == MIC_RECORDING_SERVICE_NAME)
                isMicRecordingServiceBounded = false
        }
    }

    internal val playbackRecordServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder?) {
            if (name.shortClassName == PLAYBACK_RECORDING_SERVICE_NAME)
                isPlaybackRecordingServiceBounded = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            if (name.shortClassName == PLAYBACK_RECORDING_SERVICE_NAME)
                isPlaybackRecordingServiceBounded = false
        }
    }

    private val mediaScannerServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder?) {
            if (name.shortClassName == MEDIA_SCANNER_SERVICE_NAME)
                isMediaScannerServiceBounded = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            if (name.shortClassName == MEDIA_SCANNER_SERVICE_NAME)
                isMediaScannerServiceBounded = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        StorageUtil.initialize(applicationContext)
        Params.initialize(this)
        ImageRepository.initialize(applicationContext)
        FavouriteRepository.initialize(applicationContext)
        CustomPlaylistsRepository.initialize(applicationContext)
        StatisticsRepository.initialize(applicationContext)
        YoutubeDL.getInstance().init(applicationContext)
        FFmpeg.getInstance().init(applicationContext)

        thread {
            try {
                YoutubeDL.getInstance().updateYoutubeDL(applicationContext)
            } catch (ignored: Exception) {}
        }

        if (!Params.instance.saveCurTrackAndPlaylist)
            StorageUtil.instance.clearPlayingProgress()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Glide.get(this).clearMemory()
        Glide.with(applicationContext).onTrimMemory(Glide.TRIM_MEMORY_MODERATE)
    }

    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
            val order = MediaStore.Audio.Media.TITLE + " ASC"

            val projection = mutableListOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATE_ADDED
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                projection.add(MediaStore.Audio.Media.RELATIVE_PATH)

            if (checkAndRequestPermissions())
                contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection.toTypedArray(),
                    selection,
                    null,
                    order
                ).use { cursor ->
                    allTracks.clear()

                    if (cursor != null)
                        addTracksFromStorage(cursor, allTracks)
                }
        }
    }

    override val loaderContent: AbstractPlaylist get() = allTracks

    /**
     * Gets album picture asynchronously
     * @param dataPath path to track (DATA column from MediaStore)
     */

    internal suspend fun getAlbumPictureAsync(dataPath: String) = coroutineScope {
        async(Dispatchers.IO) {
            try {
                AudioFileIO
                    .read(File(dataPath))
                    .tagOrCreateAndSetDefault
                    ?.firstArtwork
                    ?.binaryData
                    ?.toBitmap()
                    ?: BitmapFactory
                        .decodeResource(resources, R.drawable.album_default)
                        .let { ViewSetter.getPictureInScale(it, it.width, it.height) }
            } catch (e: Exception) {
                // File not found
                BitmapFactory
                    .decodeResource(resources, R.drawable.album_default)
                    .let { ViewSetter.getPictureInScale(it, it.width, it.height) }
            }
        }
    }

    /** Saves paused time of track */

    internal suspend fun savePauseTime() {
        if (Params.getInstanceSynchronized().saveCurTrackAndPlaylist && musicPlayer != null)
            try {
                musicPlayer?.currentPosition?.let {
                    StorageUtil.getInstanceSynchronized().storeTrackPauseTime(it)
                }
            } catch (ignored: Exception) {
                // Something wrong with MediaPlayer
            }
    }

    /**
     * Check for permissions and requests
     * if some of them weren't give
     */

    internal fun checkAndRequestPermissions(): Boolean {
        val permissionReadPhoneState =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)

        val permissionReadStorage =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        val permissionWriteStorage = when {
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.R ->
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )

            else -> PackageManager.PERMISSION_GRANTED
        }

        val permissionRecord =
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)

        val permissionReadContacts =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)

        val permissionWriteContacts =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS)

        val listPermissionsNeeded = mutableListOf<String>()

        if (permissionReadPhoneState != PackageManager.PERMISSION_GRANTED)
            listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE)

        if (permissionReadStorage != PackageManager.PERMISSION_GRANTED)
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)

        if (permissionWriteStorage != PackageManager.PERMISSION_GRANTED)
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permissionRecord != PackageManager.PERMISSION_GRANTED)
            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO)

        if (permissionReadContacts != PackageManager.PERMISSION_GRANTED)
            listPermissionsNeeded.add(Manifest.permission.READ_CONTACTS)

        if (permissionWriteContacts != PackageManager.PERMISSION_GRANTED)
            listPermissionsNeeded.add(Manifest.permission.WRITE_CONTACTS)

        return when {
            listPermissionsNeeded.isNotEmpty() -> {
                requestPermissions(
                    mainActivity.unchecked,
                    listPermissionsNeeded.toTypedArray(),
                    MainActivity.REQUEST_ID_MULTIPLE_PERMISSIONS
                )
                false
            }

            else -> true
        }
    }

    /**
     * !!! ANDROID 11+ ONLY !!!
     *
     * Checks that [Manifest.permission.MANAGE_EXTERNAL_STORAGE] permission is given.
     * If it isn't given, runs request for permission.
     *
     * @param act action to do if permission is given
     */

    @RequiresApi(Build.VERSION_CODES.R)
    internal inline fun <T> checkAndRequestManageExternalStoragePermission(act: () -> T): Option<T> =
        when {
            !Environment.isExternalStorageManager() -> {
                AlertDialog.Builder(mainActivity.unchecked)
                    .setMessage(R.string.android11_permission)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok) { d, _ ->
                        d.dismiss()

                        mainActivity.unchecked.startActivity(
                            Intent().apply {
                                action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                            }
                        )
                    }
                    .setNegativeButton(R.string.cancel) { d, _ -> d.dismiss() }
                    .show()

                None
            }

            else -> Some(act())
        }

    /** Adds tracks from database */

    internal fun addTracksFromStorage(cursor: Cursor, location: MutableList<AbstractTrack>) {
        while (cursor.moveToNext()) {
            val path = cursor.getString(4)

            location.add(
                DefaultTrack(
                    cursor.getLong(0),
                    cursor.getString(1) ?: "",
                    cursor.getString(2) ?: "",
                    cursor.getString(3) ?: "",
                    path,
                    cursor.getLong(5),
                    relativePath = when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                            cursor.getString(8)
                        else -> null
                    },
                    displayName = cursor.getString(6),
                    cursor.getLong(7)
                )
            )
        }
    }

    /** Adds tracks from database */

    internal fun addTracksFromStoragePaired(cursor: Cursor, location: MutableList<Pair<Int, AbstractTrack>>) {
        var ind = 0

        while (cursor.moveToNext()) {
            val path = cursor.getString(4)

            location.add(
                ind++ to DefaultTrack(
                    cursor.getLong(0),
                    cursor.getString(1) ?: "",
                    cursor.getString(2) ?: "",
                    cursor.getString(3) ?: "",
                    path,
                    cursor.getLong(5),
                    relativePath = when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                            cursor.getString(8)
                        else -> null
                    },
                    displayName = cursor.getString(6),
                    cursor.getLong(7)
                )
            )
        }
    }

    /** Enables equalizer */

    internal suspend fun startEqualizer() = mutex.withLock {
        musicPlayer!!.run {
            if (isPlaying) {
                val loader = StorageUtil.getInstanceSynchronized()
                playbackParams = PlaybackParams()
                    .setPitch(loader.loadPitch().playbackParam)
                    .setSpeed(loader.loadSpeed().playbackParam)
            }
        }

        EqualizerSettings.instance.run {
            isEqualizerEnabled = true
            isEditing = true

            if (equalizerModel == null) {
                equalizerModel = EqualizerModel.newInstance().apply {
                    reverbPreset = PresetReverb.PRESET_NONE
                    bassStrength = (1000 / 19).toShort()
                }
            }
        }

        equalizer = Equalizer(0, audioSessionId!!)

        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q)
            bassBoost = BassBoost(0, audioSessionId!!).apply {
                enabled = EqualizerSettings.instance.isEqualizerEnabled
                properties = BassBoost.Settings(properties.toString()).apply {
                    strength = StorageUtil.getInstanceSynchronized().loadBassStrength()
                }
            }

        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q)
            presetReverb = PresetReverb(0, audioSessionId!!).apply {
                try {
                    preset = StorageUtil.getInstanceSynchronized().loadReverbPreset()
                } catch (ignored: Exception) {
                    // not supported
                }
                enabled = EqualizerSettings.instance.isEqualizerEnabled
            }

        val seekBarPoses = StorageUtil.getInstanceSynchronized().loadEqualizerSeekbarsPos()
            ?: EqualizerSettings.instance.seekbarPos

        when (EqualizerSettings.instance.presetPos) {
            0 -> (0 until equalizer.numberOfBands).forEach {
                equalizer.setBandLevel(
                    it.toShort(),
                    seekBarPoses[it].toShort()
                )
            }

            else -> equalizer.usePreset(EqualizerSettings.instance.presetPos.toShort())
        }
    }

    /**
     * Gets all tracks specified by album's title from [MediaStore]
     * @param albumTitle album's title itself
     */

    internal suspend fun getAlbumTracksAsync(albumTitle: String) = coroutineScope {
        async(Dispatchers.IO) {
            val selection = "${MediaStore.Audio.Media.ALBUM} = ?"

            val order = "${
                when (Params.instance.tracksOrder.first) {
                    Params.Companion.TracksOrder.TITLE -> MediaStore.Audio.Media.TITLE
                    Params.Companion.TracksOrder.ARTIST -> MediaStore.Audio.Media.ARTIST
                    Params.Companion.TracksOrder.ALBUM -> MediaStore.Audio.Media.ALBUM
                    Params.Companion.TracksOrder.DATE -> MediaStore.Audio.Media.DATE_ADDED
                }
            } ${if (Params.getInstanceSynchronized().tracksOrder.second) "ASC" else "DESC"}"

            val trackList = mutableListOf<AbstractTrack>()

            val projection = mutableListOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATE_ADDED
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                projection.add(MediaStore.Audio.Media.RELATIVE_PATH)

            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection.toTypedArray(),
                selection,
                arrayOf(albumTitle),
                order
            ).use { cursor ->
                if (cursor != null)
                    addTracksFromStorage(cursor, trackList)
            }

            trackList.distinctBy(AbstractTrack::path).toPlaylist()
        }
    }

    internal fun scanAllFiles() {
        when {
            !isMediaScannerServiceBounded -> {
                val scannerIntent = Intent(applicationContext, MediaScannerService::class.java)
                    .apply { action = MediaScannerService.Broadcast_SCAN_ALL_FILES }

                startService(scannerIntent)

                bindService(
                    scannerIntent,
                    mediaScannerServiceConnection,
                    BIND_AUTO_CREATE
                )
            }

            else -> sendBroadcast(Intent(MediaScannerService.Broadcast_SCAN_ALL_FILES))
        }
    }

    internal fun scanSingleFile(trackPath: String) {
        when {
            !isMediaScannerServiceBounded -> {
                val scannerIntent = Intent(applicationContext, MediaScannerService::class.java)
                    .putExtra(MediaScannerService.TRACK_TO_SCAN_ARG, trackPath)
                    .apply { action = MediaScannerService.Broadcast_SCAN_SINGLE_FILE }

                startService(scannerIntent)

                bindService(
                    scannerIntent,
                    mediaScannerServiceConnection,
                    BIND_AUTO_CREATE
                )
            }

            else -> sendBroadcast(
                Intent(MediaScannerService.Broadcast_SCAN_SINGLE_FILE)
                    .putExtra(MediaScannerService.TRACK_TO_SCAN_ARG, trackPath)
            )
        }
    }
}