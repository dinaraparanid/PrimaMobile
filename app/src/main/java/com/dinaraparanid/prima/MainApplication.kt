package com.dinaraparanid.prima

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Typeface
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
import androidx.core.content.res.ResourcesCompat
import arrow.core.None
import arrow.core.Option
import com.bumptech.glide.Glide
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.equalizer.EqualizerSettings
import com.dinaraparanid.prima.utils.polymorphism.Loader
import com.dinaraparanid.prima.utils.polymorphism.Playlist
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class MainApplication : Application(), Loader<Playlist> {
    internal lateinit var equalizer: Equalizer
    internal lateinit var bassBoost: BassBoost
    internal lateinit var presetReverb: PresetReverb

    internal var mainActivity: MainActivity? = null
    internal var musicPlayer: MediaPlayer? = null
    internal var startPath: Option<String> = None
    internal var highlightedRows = mutableListOf<String>()
    internal var curPath = "_____ЫЫЫЫЫЫЫЫ_____"
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
        EqualizerSettings.initialize(this)
        FavouriteRepository.initialize(this)
        CustomPlaylistsRepository.initialize(this)

        if (!Params.instance.saveCurTrackAndPlaylist)
            StorageUtil(applicationContext).clearPlayingProgress()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        allTracks.clear()
        mainActivity = null
        Glide.with(applicationContext).onTrimMemory(Glide.TRIM_MEMORY_MODERATE)

        try {
            musicPlayer?.release()
            equalizer.release()
            bassBoost.release()
            presetReverb.release()
        } catch (ignored: Exception) {
            // not initialized
        }
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

    internal suspend fun getAlbumPictureAsync(dataPath: String, useDefault: Boolean) =
        coroutineScope {
            async(Dispatchers.IO) {
                val data = try {
                    if (useDefault)
                        MediaMetadataRetriever().apply { setDataSource(dataPath) }.embeddedPicture
                    else null
                } catch (e: Exception) {
                    null
                }

                when {
                    data != null -> BitmapFactory
                        .decodeByteArray(data, 0, data.size)
                        .let { ViewSetter.getPictureInScale(it, it.width, it.height) }

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
                if (saveLooping) storeLooping(musicPlayer!!.isLooping)
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

        val listPermissionsNeeded: MutableList<String> = mutableListOf()

        if (permissionReadPhoneState != PackageManager.PERMISSION_GRANTED)
            listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE)

        if (permissionReadStorage != PackageManager.PERMISSION_GRANTED)
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)

        if (permissionWriteStorage != PackageManager.PERMISSION_GRANTED)
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permissionRecord != PackageManager.PERMISSION_GRANTED)
            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO)

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
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
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

    /**
     * Gets font from font name
     * @param font font name
     * @return font with expected name or Sans-Serif if it's not found
     */

    // Generator:
    //
    // import java.io.File
    //
    // @OptIn(ExperimentalStdlibApi::class)
    // fun main() = File("F:\\PROGRAMMING\\android\\MusicPlayer\\app\\src\\main\\res\\font")
    //     .listFiles()!!
    //     .forEach { file ->
    //         println(
    //             "${Char(34)}${
    //                 file.nameWithoutExtension
    //                     .split('_')
    //                     .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
    //             }${Char(34)} -> ResourcesCompat.getFont(applicationContext, R.font.${file.nameWithoutExtension})"
    //         )
    //     }

    internal fun getFontFromName(font: String): Typeface = when (font) {
        "Abeezee" -> ResourcesCompat.getFont(applicationContext, R.font.abeezee)
        "Abel" -> ResourcesCompat.getFont(applicationContext, R.font.abel)
        "Abril Fatface" -> ResourcesCompat.getFont(applicationContext, R.font.abril_fatface)
        "Aclonica" -> ResourcesCompat.getFont(applicationContext, R.font.aclonica)
        "Adamina" -> ResourcesCompat.getFont(applicationContext, R.font.adamina)
        "Advent Pro" -> ResourcesCompat.getFont(applicationContext, R.font.advent_pro)
        "Aguafina Script" -> ResourcesCompat.getFont(applicationContext, R.font.aguafina_script)
        "Akronim" -> ResourcesCompat.getFont(applicationContext, R.font.akronim)
        "Aladin" -> ResourcesCompat.getFont(applicationContext, R.font.aladin)
        "Aldrich" -> ResourcesCompat.getFont(applicationContext, R.font.aldrich)
        "Alegreya Sc" -> ResourcesCompat.getFont(applicationContext, R.font.alegreya_sc)
        "Alex Brush" -> ResourcesCompat.getFont(applicationContext, R.font.alex_brush)
        "Alfa Slab One" -> ResourcesCompat.getFont(applicationContext, R.font.alfa_slab_one)
        "Allan" -> ResourcesCompat.getFont(applicationContext, R.font.allan)
        "Allerta" -> ResourcesCompat.getFont(applicationContext, R.font.allerta)
        "Almendra" -> ResourcesCompat.getFont(applicationContext, R.font.almendra)
        "Almendra Sc" -> ResourcesCompat.getFont(applicationContext, R.font.almendra_sc)
        "Amarante" -> ResourcesCompat.getFont(applicationContext, R.font.amarante)
        "Amiko" -> ResourcesCompat.getFont(applicationContext, R.font.amiko)
        "Amita" -> ResourcesCompat.getFont(applicationContext, R.font.amita)
        "Anarchy" -> ResourcesCompat.getFont(applicationContext, R.font.anarchy)
        "Andika" -> ResourcesCompat.getFont(applicationContext, R.font.andika)
        "Android" -> ResourcesCompat.getFont(applicationContext, R.font.android)
        "Android Hollow" -> ResourcesCompat.getFont(applicationContext, R.font.android_hollow)
        "Android Italic" -> ResourcesCompat.getFont(applicationContext, R.font.android_italic)
        "Android Scratch" -> ResourcesCompat.getFont(applicationContext, R.font.android_scratch)
        "Annie Use Your Telescope" -> ResourcesCompat.getFont(
            applicationContext,
            R.font.annie_use_your_telescope
        )
        "Anton" -> ResourcesCompat.getFont(applicationContext, R.font.anton)
        "Architects Daughter" -> ResourcesCompat.getFont(
            applicationContext,
            R.font.architects_daughter
        )
        "Archivo Black" -> ResourcesCompat.getFont(applicationContext, R.font.archivo_black)
        "Arima Madurai Medium" -> ResourcesCompat.getFont(
            applicationContext,
            R.font.arima_madurai_medium
        )
        "Arizonia" -> ResourcesCompat.getFont(applicationContext, R.font.arizonia)
        "Artifika" -> ResourcesCompat.getFont(applicationContext, R.font.artifika)
        "Atma" -> ResourcesCompat.getFont(applicationContext, R.font.atma)
        "Atomic Age" -> ResourcesCompat.getFont(applicationContext, R.font.atomic_age)
        "Audiowide" -> ResourcesCompat.getFont(applicationContext, R.font.audiowide)
        "Bad Script" -> ResourcesCompat.getFont(applicationContext, R.font.bad_script)
        "Bangers" -> ResourcesCompat.getFont(applicationContext, R.font.bangers)
        "Bastong" -> ResourcesCompat.getFont(applicationContext, R.font.bastong)
        "Berkshire Swash" -> ResourcesCompat.getFont(applicationContext, R.font.berkshire_swash)
        "Bilbo Swash Caps" -> ResourcesCompat.getFont(applicationContext, R.font.bilbo_swash_caps)
        "Black Ops One" -> ResourcesCompat.getFont(applicationContext, R.font.black_ops_one)
        "Bonbon" -> ResourcesCompat.getFont(applicationContext, R.font.bonbon)
        "Boogaloo" -> ResourcesCompat.getFont(applicationContext, R.font.boogaloo)
        "Bracknell F" -> ResourcesCompat.getFont(applicationContext, R.font.bracknell_f)
        "Bungee Inline" -> ResourcesCompat.getFont(applicationContext, R.font.bungee_inline)
        "Bungee Shade" -> ResourcesCompat.getFont(applicationContext, R.font.bungee_shade)
        "Caesar Dressing" -> ResourcesCompat.getFont(applicationContext, R.font.caesar_dressing)
        "Calligraffitti" -> ResourcesCompat.getFont(applicationContext, R.font.calligraffitti)
        "Carter One" -> ResourcesCompat.getFont(applicationContext, R.font.carter_one)
        "Caveat Bold" -> ResourcesCompat.getFont(applicationContext, R.font.caveat_bold)
        "Cedarville Cursive" -> ResourcesCompat.getFont(
            applicationContext,
            R.font.cedarville_cursive
        )
        "Changa One" -> ResourcesCompat.getFont(applicationContext, R.font.changa_one)
        "Cherry Cream Soda" -> ResourcesCompat.getFont(applicationContext, R.font.cherry_cream_soda)
        "Cherry Swash" -> ResourcesCompat.getFont(applicationContext, R.font.cherry_swash)
        "Chewy" -> ResourcesCompat.getFont(applicationContext, R.font.chewy)
        "Cinzel Decorative" -> ResourcesCompat.getFont(applicationContext, R.font.cinzel_decorative)
        "Coming Soon" -> ResourcesCompat.getFont(applicationContext, R.font.coming_soon)
        "Condiment" -> ResourcesCompat.getFont(applicationContext, R.font.condiment)
        "Dancing Script Bold" -> ResourcesCompat.getFont(
            applicationContext,
            R.font.dancing_script_bold
        )
        "Delius Unicase" -> ResourcesCompat.getFont(applicationContext, R.font.delius_unicase)
        "Droid Sans Mono" -> ResourcesCompat.getFont(applicationContext, R.font.droid_sans_mono)
        "Droid Serif" -> ResourcesCompat.getFont(applicationContext, R.font.droid_serif)
        "Extendo Italic" -> ResourcesCompat.getFont(applicationContext, R.font.extendo_italic)
        "Faster One" -> ResourcesCompat.getFont(applicationContext, R.font.faster_one)
        "Fira Sans Thin" -> ResourcesCompat.getFont(applicationContext, R.font.fira_sans_thin)
        "Gruppo" -> ResourcesCompat.getFont(applicationContext, R.font.gruppo)
        "Homemade Apple" -> ResourcesCompat.getFont(applicationContext, R.font.homemade_apple)
        "Jim Nightshade" -> ResourcesCompat.getFont(applicationContext, R.font.jim_nightshade)
        "Magretta" -> ResourcesCompat.getFont(applicationContext, R.font.magretta)
        "Mako" -> ResourcesCompat.getFont(applicationContext, R.font.mako)
        "Mclaren" -> ResourcesCompat.getFont(applicationContext, R.font.mclaren)
        "Megrim" -> ResourcesCompat.getFont(applicationContext, R.font.megrim)
        "Metal Mania" -> ResourcesCompat.getFont(applicationContext, R.font.metal_mania)
        "Modern Antiqua" -> ResourcesCompat.getFont(applicationContext, R.font.modern_antiqua)
        "Morning Vintage" -> ResourcesCompat.getFont(applicationContext, R.font.morning_vintage)
        "Mountains Of Christmas" -> ResourcesCompat.getFont(
            applicationContext,
            R.font.mountains_of_christmas
        )
        "Naylime" -> ResourcesCompat.getFont(applicationContext, R.font.naylime)
        "Nova Flat" -> ResourcesCompat.getFont(applicationContext, R.font.nova_flat)
        "Orbitron" -> ResourcesCompat.getFont(applicationContext, R.font.orbitron)
        "Oxygen" -> ResourcesCompat.getFont(applicationContext, R.font.oxygen)
        "Pacifico" -> ResourcesCompat.getFont(applicationContext, R.font.pacifico)
        "Paprika" -> ResourcesCompat.getFont(applicationContext, R.font.paprika)
        "Permanent Marker" -> ResourcesCompat.getFont(applicationContext, R.font.permanent_marker)
        "Press Start 2p" -> ResourcesCompat.getFont(applicationContext, R.font.press_start_2p)
        "Pristina" -> ResourcesCompat.getFont(applicationContext, R.font.pristina)
        "Pt Sans" -> ResourcesCompat.getFont(applicationContext, R.font.pt_sans)
        "Puritan" -> ResourcesCompat.getFont(applicationContext, R.font.puritan)
        "Rock Salt" -> ResourcesCompat.getFont(applicationContext, R.font.rock_salt)
        "Rusthack" -> ResourcesCompat.getFont(applicationContext, R.font.rusthack)
        "Shadows Into Light Two" -> ResourcesCompat.getFont(
            applicationContext,
            R.font.shadows_into_light_two
        )
        "Sniglet" -> ResourcesCompat.getFont(applicationContext, R.font.sniglet)
        "Special Elite" -> ResourcesCompat.getFont(applicationContext, R.font.special_elite)
        "Thejulayna" -> ResourcesCompat.getFont(applicationContext, R.font.thejulayna)
        "Trade Winds" -> ResourcesCompat.getFont(applicationContext, R.font.trade_winds)
        "Tropical Summer Signature" -> ResourcesCompat.getFont(
            applicationContext,
            R.font.tropical_summer_signature
        )
        "Ubuntu" -> ResourcesCompat.getFont(applicationContext, R.font.ubuntu)
        "Monospace" -> Typeface.MONOSPACE
        "Serif" -> Typeface.SERIF
        else -> Typeface.SANS_SERIF
    }!!
}