package com.dinaraparanid.prima

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import arrow.core.None
import arrow.core.Option
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.utils.polymorphism.Playlist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.polymorphism.Loader
import kotlinx.coroutines.*

class MainApplication : Application(), Loader<Playlist> {
    internal var mainActivity: MainActivity? = null
    internal var musicPlayer: MediaPlayer? = null
    internal var startPath: Option<String> = None
    internal var highlightedRows = mutableListOf<String>()
    internal val albumImages = mutableMapOf<String, Bitmap>()
    internal var curPath = "_____ЫЫЫЫЫЫЫЫ_____"
    internal var playingBarIsVisible = false
    internal val allTracks = DefaultPlaylist()
    internal val changedTracks = mutableMapOf<String, Track>()
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
        Params.initialize()
        FavouriteRepository.initialize(this)
        CustomPlaylistsRepository.initialize(this)
    }

    override suspend fun loadAsync(): Deferred<Unit> = coroutineScope {
        StorageUtil(applicationContext).loadChangedTracks()?.let(changedTracks::putAll)

        async(Dispatchers.IO) {
            val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
            val order = MediaStore.Audio.Media.TITLE + " ASC"

            val projection = mutableListOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DISPLAY_NAME
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

    internal suspend fun getAlbumPictureAsync(dataPath: String) = coroutineScope {
        async(Dispatchers.IO) {
            val data = try {
                MediaMetadataRetriever().apply { setDataSource(dataPath) }.embeddedPicture
            } catch (e: Exception) {
                null
            }

            when {
                data != null -> {
                    val albumPicture = BitmapFactory.decodeByteArray(data, 0, data.size)
                    val width = albumPicture.width
                    val height = albumPicture.height

                    Bitmap.createBitmap(
                        albumPicture,
                        0,
                        0,
                        width,
                        height,
                        Matrix(),
                        false
                    )
                }

                else -> {
                    val albumPicture =
                        BitmapFactory.decodeResource(resources, R.drawable.album_default)
                    val width = albumPicture.width
                    val height = albumPicture.height

                    Bitmap.createBitmap(
                        albumPicture,
                        0,
                        0,
                        width,
                        height,
                        Matrix(),
                        false
                    )
                }
            }
        }
    }

    internal fun save() = try {
        StorageUtil(applicationContext).run {
            storeChangedTracks(changedTracks)
            storeLooping(musicPlayer!!.isLooping)
            storeCurPlaylist(curPlaylist)
            storeTrackPauseTime(musicPlayer!!.currentPosition)
            curPath.takeIf { it != "_____ЫЫЫЫЫЫЫЫ_____" }?.let(::storeTrackPath)
        }
    } catch (e: Exception) {
        // music player isn't initialized
    }

    internal fun checkAndRequestPermissions() = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
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

            val listPermissionsNeeded: MutableList<String> = mutableListOf()

            if (permissionReadPhoneState != PackageManager.PERMISSION_GRANTED)
                listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE)

            if (permissionReadStorage != PackageManager.PERMISSION_GRANTED)
                listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)

            if (permissionWriteStorage != PackageManager.PERMISSION_GRANTED)
                listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)

            when {
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

        else -> false
    }

    internal fun addTracksFromStorage(cursor: Cursor, location: MutableList<Track>) {
        while (cursor.moveToNext()) {
            val path = cursor.getString(4)

            (changedTracks[path] ?: Track(
                cursor.getLong(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                path,
                cursor.getLong(5),
                displayName = cursor.getString(6),
                relativePath = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                        cursor.getString(7)
                    else -> null
                }
            )).apply(location::add)
        }
    }
}