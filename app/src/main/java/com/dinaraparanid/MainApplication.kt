package com.dinaraparanid

import android.app.Application
import android.content.ComponentName
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.IBinder
import arrow.core.None
import arrow.core.Option
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params

class MainApplication : Application() {
    internal var mainActivity: MainActivity? = null
    internal var musicPlayer: MediaPlayer? = null
    internal var startPath: Option<String> = None
    internal var serviceBound = false
        private set

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
    }

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
}