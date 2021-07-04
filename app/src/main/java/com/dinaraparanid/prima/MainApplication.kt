package com.dinaraparanid.prima

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
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
import com.dinaraparanid.prima.core.Playlist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.polymorphism.Loader

class MainApplication : Application(), Loader<Playlist> {
    internal var mainActivity: MainActivity? = null
    internal var musicPlayer: MediaPlayer? = null
    internal var startPath: Option<String> = None
    internal var highlightedRows = mutableListOf<String>()
    internal val albumImages = mutableMapOf<String, Bitmap>()
    internal var curPath = "_____ЫЫЫЫЫЫЫЫ_____"
    internal var playingBarIsVisible = false
    internal val allTracks = DefaultPlaylist()
    internal var serviceBound = false
        private set

    internal val curPlaylist: Playlist by lazy {
        StorageUtil(applicationContext).loadCurPlaylist() ?: DefaultPlaylist()
    }

    internal val serviceConnection: ServiceConnection = object : ServiceConnection {
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

    override fun load() {
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val order = MediaStore.Audio.Media.TITLE + " ASC"

        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION
        )

        while (!checkAndRequestPermissions()) Unit

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            order
        ).use { cursor ->
            allTracks.clear()

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    allTracks.add(
                        Track(
                            cursor.getString(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getLong(4)
                        )
                    )
                }
            }
        }
    }

    override val loaderContent: Playlist get() = allTracks

    internal fun getAlbumPicture(dataPath: String): Bitmap {
        val data = MediaMetadataRetriever().apply { setDataSource(dataPath) }.embeddedPicture

        return when {
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
                val albumPicture = BitmapFactory.decodeResource(resources, R.drawable.album_default)
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

    internal fun save() = try {
        StorageUtil(applicationContext).storeLooping(musicPlayer!!.isLooping)
        StorageUtil(applicationContext).storeCurPlaylist(curPlaylist)
        StorageUtil(applicationContext).storeTrackPauseTime(musicPlayer!!.currentPosition)
        curPath.takeIf { it != "_____ЫЫЫЫЫЫЫЫ_____" }
            ?.let(StorageUtil(applicationContext)::storeTrackPath)
    } catch (e: Exception) {
    }

    internal fun checkAndRequestPermissions() = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
            val permissionReadPhoneState =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)

            val permissionStorage =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

            val listPermissionsNeeded: MutableList<String> = mutableListOf()

            if (permissionReadPhoneState != PackageManager.PERMISSION_GRANTED)
                listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE)

            if (permissionStorage != PackageManager.PERMISSION_GRANTED)
                listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)

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
}