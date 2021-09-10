package com.dinaraparanid.prima

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import arrow.core.None
import arrow.core.Option
import com.bumptech.glide.Glide
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.databases.entities.TrackImage
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.databases.repositories.ImageRepository
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.equalizer.EqualizerSettings
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.polymorphism.Loader
import com.dinaraparanid.prima.utils.polymorphism.Playlist
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.*
import kotlin.concurrent.thread

class MainApplication : Application(), Loader<Playlist> {
    internal lateinit var equalizer: Equalizer
    internal lateinit var bassBoost: BassBoost
    internal lateinit var presetReverb: PresetReverb

    internal var mainActivity: MainActivity? = null
    internal var musicPlayer: MediaPlayer? = null
    internal var startPath: Option<String> = None
    internal var highlightedRow: Option<String> = None
    internal var curPath = MainActivity.NO_PATH
    internal var playingBarIsVisible = false
    internal val allTracks = DefaultPlaylist()
    internal val changedTracks = mutableMapOf<String, Track>()
    internal var audioSessionId = 0
    internal var serviceBound = false
        private set

    internal val curPlaylist: Playlist by lazy {
        StorageUtil(applicationContext).loadCurPlaylist() ?: DefaultPlaylist()
    }

    internal val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            serviceBound = false
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
        thread { YoutubeDL.getInstance().updateYoutubeDL(this) }

        if (!Params.instance.saveCurTrackAndPlaylist)
            StorageUtil(applicationContext).clearPlayingProgress()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Glide.with(applicationContext).onTrimMemory(Glide.TRIM_MEMORY_MODERATE)
    }

    override suspend fun loadAsync(): Job = coroutineScope {
        StorageUtil(applicationContext).loadChangedTracks()?.let(changedTracks::putAll)

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

    override val loaderContent: Playlist get() = allTracks

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

    /** Saves changed tracks and playing progress */

    internal fun save() = try {
        StorageUtil(applicationContext).run {
            storeChangedTracks(changedTracks)
            Params.instance.run {
                if (saveCurTrackAndPlaylist) {
                    storeCurPlaylist(curPlaylist)
                    storeTrackPauseTime(musicPlayer!!.currentPosition)
                    curPath.takeIf { it != "_____ЫЫЫЫЫЫЫЫ_____" }?.let(::storeTrackPath)
                }
            }
        }
    } catch (ignored: Exception) {
        // music player isn't initialized
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

        val permissionRecord = ContextCompat
            .checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)

        val permissionReadContacts = ContextCompat
            .checkSelfPermission(this, Manifest.permission.READ_CONTACTS)

        val permissionWriteContacts = ContextCompat
            .checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS)

        val listPermissionsNeeded: MutableList<String> = mutableListOf()

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
                    mainActivity!!,
                    listPermissionsNeeded.toTypedArray(),
                    MainActivity.REQUEST_ID_MULTIPLE_PERMISSIONS
                )
                false
            }

            else -> true
        }
    }

    /**
     * Adds tracks from database
     */

    internal fun addTracksFromStorage(cursor: Cursor, location: MutableList<Track>) {
        while (cursor.moveToNext()) {
            val path = cursor.getString(4)

            (changedTracks[path] ?: Track(
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
            )).apply(location::add)
        }
    }
}