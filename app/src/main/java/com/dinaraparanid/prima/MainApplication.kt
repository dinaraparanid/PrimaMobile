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
import android.media.MediaMetadataRetriever
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
import com.dinaraparanid.prima.core.AbstractTrack
import com.dinaraparanid.prima.core.DefaultTrack
import com.dinaraparanid.prima.databases.entities.TrackImage
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.databases.repositories.ImageRepository
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.equalizer.EqualizerModel
import com.dinaraparanid.prima.utils.equalizer.EqualizerSettings
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.Loader
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import kotlin.concurrent.thread

class MainApplication : Application(), Loader<AbstractPlaylist> {
    private companion object {
        private const val AUDIO_SERVICE_NAME = ".AudioPlayerService"
        private const val CONVERTER_SERVICE_NAME = ".utils.ConverterService"
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
    internal var highlightedRow: Option<String> = None
    internal var curPath = MainActivity.NO_PATH
    internal var playingBarIsVisible = false
    internal val allTracks = DefaultPlaylist()

    internal inline val audioSessionId
        get() = musicPlayer?.audioSessionId

    internal var isAudioServiceBounded = false
        private set

    internal var isConverterServiceBounded = false
        private set

    @Deprecated("Now updating metadata in files (Android 11+)")
    internal val changedTracks = mutableMapOf<String, AbstractTrack>()

    internal val curPlaylist: AbstractPlaylist by lazy {
        StorageUtil(applicationContext).loadCurPlaylist() ?: DefaultPlaylist()
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

    override fun onCreate() {
        super.onCreate()
        Params.initialize(this)
        ImageRepository.initialize(this)
        EqualizerSettings.initialize(this)
        FavouriteRepository.initialize(this)
        CustomPlaylistsRepository.initialize(this)
        YoutubeDL.getInstance().init(this)
        FFmpeg.getInstance().init(this)

        thread {
            try {
                YoutubeDL.getInstance().updateYoutubeDL(this)
            } catch (ignored: Exception) {
            }
        }

        if (!Params.instance.saveCurTrackAndPlaylist)
            StorageUtil(applicationContext).clearPlayingProgress()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Glide.with(applicationContext).onTrimMemory(Glide.TRIM_MEMORY_MODERATE)
    }

    override suspend fun loadAsync(): Job = coroutineScope {
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

    internal suspend fun getAlbumPictureAsync(dataPath: String, getRealImage: Boolean) =
        coroutineScope {
            async(Dispatchers.IO) {
                val data = ImageRepository.instance
                    .getTrackWithImageAsync(dataPath)
                    .await()
                    ?.let(TrackImage::image) ?: try {
                    if (getRealImage)
                        MediaMetadataRetriever().apply { setDataSource(dataPath) }.embeddedPicture
                    else null
                } catch (e: Exception) {
                    null
                }

                when {
                    data != null -> data.toBitmap()

                    else -> BitmapFactory
                        .decodeResource(resources, R.drawable.album_default)
                        .let { ViewSetter.getPictureInScale(it, it.width, it.height) }
                }
            }
        }

    /** Saves paused time of track */

    internal fun savePauseTime() {
        if (Params.instance.saveCurTrackAndPlaylist && musicPlayer != null)
            try {
                StorageUtil(applicationContext).storeTrackPauseTime(musicPlayer!!.currentPosition)
            } catch (ignored: Exception) {
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

    /**
     * Adds tracks from database
     */

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

    /** Enables equalizer */

    internal fun startEqualizer() {
        musicPlayer!!.run {
            if (isPlaying) {
                val loader = StorageUtil(applicationContext)
                playbackParams = PlaybackParams()
                    .setPitch(loader.loadPitch())
                    .setSpeed(loader.loadSpeed())
            }
        }

        EqualizerSettings.instance.run {
            isEqualizerEnabled = true
            isEditing = true

            if (equalizerModel == null) {
                equalizerModel = EqualizerModel(applicationContext).apply {
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
                    strength = StorageUtil(applicationContext).loadBassStrength()
                }
            }

        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q)
            presetReverb = PresetReverb(0, audioSessionId!!).apply {
                try {
                    preset = StorageUtil(applicationContext).loadReverbPreset()
                } catch (ignored: Exception) {
                    // not supported
                }
                enabled = EqualizerSettings.instance.isEqualizerEnabled
            }

        equalizer.enabled = EqualizerSettings.instance.isEqualizerEnabled

        val seekBarPoses = StorageUtil(applicationContext).loadEqualizerSeekbarsPos()
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
}