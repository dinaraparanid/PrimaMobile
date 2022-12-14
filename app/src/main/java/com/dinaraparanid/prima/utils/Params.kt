package com.dinaraparanid.prima.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.dinaraparanid.prima.BR
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.drawables.Divider
import com.dinaraparanid.prima.utils.drawables.FontDivider
import com.dinaraparanid.prima.utils.drawables.Marker
import com.dinaraparanid.prima.utils.extensions.rootPath
import com.dinaraparanid.prima.utils.extensions.toDp
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.yariksoffice.lingver.Lingver
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/** Container of some params for app */

class Params private constructor() : BaseObservable() {
    companion object {
        @Deprecated(
            "The YouTube API key is very limited in resources, " +
                    "and it will not be enough for users from the Play Market"
        )
        const val YOUTUBE_API = "null"

        const val NO_PATH = "_____NO_PATH_____"

        val DEFAULT_PATH by lazy {
            instance.applicationContext.rootPath + Environment.DIRECTORY_MUSIC
        }

        /** Supported languages */
        @Suppress("Reformat")
        enum class Language {
            EN,
            BE,
            RU,
            ZH,
            @Deprecated("Against Russia") AR,
            @Deprecated("Against Russia") BG,
            @Deprecated("Against Russia") DE,
            @Deprecated("Against Russia") EL,
            @Deprecated("Against Russia") ES,
            @Deprecated("Against Russia") FR,
            @Deprecated("Against Russia") IT,
            @Deprecated("Against Russia") JA,
            @Deprecated("Against Russia") KO,
            @Deprecated("Against Russia") MN,
            @Deprecated("Against Russia") NO,
            @Deprecated("Against Russia") PL,
            @Deprecated("Against Russia") PT,
            @Deprecated("Against Russia") SV,
            @Deprecated("Against Russia") TR,
            @Deprecated("Against Russia") UK,
        }

        /** Tracks ordering by some param */
        enum class TracksOrder {
            TITLE, ARTIST, ALBUM, DATE, POS_IN_ALBUM
        }

        enum class Looping {
            PLAYLIST, TRACK, NONE;

            internal inline val next
                get() = values()[(ordinal + 1) % 3]

            internal operator fun inc() = next
        }

        enum class VisualizerStyle { BAR, WAVE }

        enum class HomeScreen {
            TRACKS,

            @Deprecated("Now using BottomSheetDialogFragment")
            CURRENT_PLAYLIST,
            TRACK_COLLECTION,
            ARTISTS,
            FAVOURITES,
            MP3_CONVERTER,
            GUESS_THE_MELODY,
            SETTINGS,
            ABOUT_APP
        }

        @JvmStatic
        private var INSTANCE: Params? = null

        @JvmStatic
        private val mutex = Mutex()

        /** Height of playing toolbar (to make fragments higher) */
        internal var PLAYING_TOOLBAR_HEIGHT = 0
            private set

        /**
         * Initialises class only once.
         * Sets theme, progress (if user allow it),
         * rounding of playlists and language.
         */

        @SuppressLint("SyntheticAccessor")
        @JvmStatic
        fun initialize(app: Application) {
            INSTANCE = Params().apply {
                application = WeakReference(app)
                val su = StorageUtil.instance
                theme = su.loadTheme()
                areCoversRounded = su.loadRounded()
                font = su.loadFont()
                loopingStatus = su.loadLooping()
                isCoverHidden = su.loadHideCover()
                isVisualizerShown = su.loadShowVisualizer()
                isSavingCurTrackAndPlaylist = su.loadSaveCurTrackAndPlaylist()
                isSavingLooping = su.loadSaveLooping()
                isSavingEqualizerSettings = su.loadSaveEqualizerSettings()
                tracksOrder = su.loadTrackOrder() ?: (TracksOrder.TITLE to true)
                primaryColor = getPrimaryColor(su.loadPrimaryThemeColor())
                secondaryColor = getSecondaryColor(su.loadSecondaryThemeColor())
                fontColor = getFontColor()
                backgroundImage = su.loadBackgroundImage()
                isStartingWithEqualizer = su.loadStartWithEqualizer()
                visualizerStyle = su.loadVisualizerStyle()
                homeScreen = su.loadHomeScreen()
                isBlurEnabled = su.loadBlurred()
                isCoversDisplayed = su.loadDisplayCovers()
                isCoverRotating = su.loadRotateCover()
                autoSaveTime.set(su.loadAutoSaveTime())

                // Free deprecated memory
                su.clearCustomThemeColors()

                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> isBloomEnabled = su.loadBloom()
                    else -> areDividersShown = su.loadDividersShown()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    isUsingAndroidNotification = su.loadUseAndroidNotification()
            }

            INSTANCE!!.pathToSave = StorageUtil.instance.loadPathToSave()
            PLAYING_TOOLBAR_HEIGHT = 70.toDp(app)
        }

        @JvmStatic
        suspend fun getInstanceSynchronized() = mutex.withLock { instance }

        /**
         * Gets instance without any protection
         * @throws UninitializedPropertyAccessException if it wasn't initialized
         * @return instance if it was created
         * @see initialize
         */

        @JvmStatic
        val instance: Params
            get() = INSTANCE
                ?: throw UninitializedPropertyAccessException("Params is not initialized")

        /** Converts [Int] to [Colors] with themes */

        @JvmStatic
        fun chooseTheme(theme: Int) = when (theme) {
            0 -> Colors.Purple()
            1 -> Colors.PurpleNight()
            2 -> Colors.Red()
            3 -> Colors.RedNight()
            4 -> Colors.Blue()
            5 -> Colors.BlueNight()
            6 -> Colors.Green()
            7 -> Colors.GreenNight()
            8 -> Colors.Orange()
            9 -> Colors.OrangeNight()
            10 -> Colors.Lemon()
            11 -> Colors.LemonNight()
            12 -> Colors.Turquoise()
            13 -> Colors.TurquoiseNight()
            14 -> Colors.GreenTurquoise()
            15 -> Colors.GreenTurquoiseNight()
            16 -> Colors.Sea()
            17 -> Colors.SeaNight()
            18 -> Colors.Pink()
            19 -> Colors.PinkNight()
            else -> Colors.PurpleNight()
        }

        /**
         * Creates sorted track lists by [tracksOrder] from given track list
         * @param trackList track list to sort
         * @return sorted track lists
         */

        @JvmStatic
        fun <T : AbstractTrack> sortedTrackList(trackList: List<Pair<Int, T>>) = when {
            instance.tracksOrder.second -> when (instance.tracksOrder.first) {
                TracksOrder.TITLE -> trackList.sortedBy { it.second.title }
                TracksOrder.ARTIST -> trackList.sortedBy { it.second.artist }
                TracksOrder.ALBUM -> trackList.sortedBy { it.second.album }
                TracksOrder.DATE -> trackList.sortedBy { it.second.addDate }
                TracksOrder.POS_IN_ALBUM -> trackList.sortedBy { it.second.trackNumberInAlbum }
            }

            else -> when (instance.tracksOrder.first) {
                TracksOrder.TITLE ->
                    trackList.sortedByDescending { it.second.title }
                TracksOrder.ARTIST ->
                    trackList.sortedByDescending { it.second.artist }
                TracksOrder.ALBUM ->
                    trackList.sortedByDescending { it.second.album }
                TracksOrder.DATE ->
                    trackList.sortedByDescending { it.second.addDate }
                TracksOrder.POS_IN_ALBUM ->
                    trackList.sortedByDescending { it.second.trackNumberInAlbum }
            }
        }
    }

    private inline val resources
        get() = application.unchecked.resources

    /** Gets language title by given [Language] */
    fun getLangTitle(lang: Language?) = when (lang) {
        Language.EN -> resources.getString(R.string.english)
        Language.BE -> resources.getString(R.string.belarusian)
        Language.RU -> resources.getString(R.string.russian)
        Language.ZH -> resources.getString(R.string.chinese)
        null -> when (val language = Lingver.getInstance().getLanguage()) {
            "en" -> resources.getString(R.string.english)
            "be" -> resources.getString(R.string.belarusian)
            "ru" -> resources.getString(R.string.russian)
            "zh" -> resources.getString(R.string.russian)
            else -> language
        }
        else -> resources.getString(R.string.english)
    }

    /** Gets home screen title by given [HomeScreen] */
    fun getHomeScreenTitle(homeScreen: HomeScreen) = when (homeScreen) {
        HomeScreen.TRACKS -> resources.getString(R.string.tracks)
        HomeScreen.CURRENT_PLAYLIST -> resources.getString(R.string.current_playlist)
        HomeScreen.TRACK_COLLECTION -> resources.getString(R.string.track_collections)
        HomeScreen.ARTISTS -> resources.getString(R.string.artists)
        HomeScreen.FAVOURITES -> resources.getString(R.string.favourites)
        HomeScreen.MP3_CONVERTER -> resources.getString(R.string.mp3_converter)
        HomeScreen.GUESS_THE_MELODY -> resources.getString(R.string.guess_the_melody)
        HomeScreen.SETTINGS -> resources.getString(R.string.settings)
        HomeScreen.ABOUT_APP -> resources.getString(R.string.about_app)
    }

    lateinit var application: WeakReference<Application>
        @JvmName("getApplication") get
        private set

    /** Current theme for app */

    @get:Bindable
    var theme: Colors = Colors.PinkNight()
        @JvmName("getTheme") get
        @JvmName("setTheme")
        private set(value) {
            field = value
            notifyPropertyChanged(BR.theme)
        }

    /** App's font */

    @get:Bindable
    var font = ""
        @JvmName("getFont") get
        @JvmName("setFont")
        set(value) {
            field = value
            notifyPropertyChanged(BR.font)
        }

    /** [Looping] status of playback */

    @get:Bindable
    var loopingStatus = Looping.NONE
        @JvmName("getLoopingStatus") get
        @JvmName("setLoopingStatus")
        internal set(value) {
            field = value
            notifyPropertyChanged(BR.loopingStatus)
        }

    /** current [VisualizerStyle] */

    @get:Bindable
    var visualizerStyle = VisualizerStyle.BAR
        @JvmName("getVisualizerStyle") get
        @JvmName("setVisualizerStyle")
        internal set(value) {
            field = value
            notifyPropertyChanged(BR.visualizerStyle)
        }

    /** Start fragment when app is opened */

    @get:Bindable
    var homeScreen = HomeScreen.TRACKS
        @JvmName("getHomeScreen") get
        @JvmName("setHomeScreen")
        internal set(value) {
            field = value
            notifyPropertyChanged(BR.homeScreen)
        }


    /** User's wish to hide track's cover on playback panel */

    @get:Bindable
    var isCoverHidden = false
        @JvmName("isCoverHidden") get
        @JvmName("setCoverHidden")
        set(value) {
            field = value
            notifyPropertyChanged(BR.coverHidden)
        }

    /** User's wish to display covers (optimization boosting) */

    @get:Bindable
    var isCoversDisplayed = true
        @JvmName("isCoversDisplayed") get
        @JvmName("setCoversDisplayed")
        set(value) {
            field = value
            notifyPropertyChanged(BR.coversDisplayed)
        }

    /** User's wish to rotate cover on small playback panel */

    @get:Bindable
    var isCoverRotating = true
        @JvmName("isCoverRotating") get
        @JvmName("setCoverRotating")
        set(value) {
            field = value
            notifyPropertyChanged(BR.coverRotating)
        }

    /** User's wish of rounded playlist's images */

    @get:Bindable
    var areCoversRounded = true
        @JvmName("areCoversRounded") get
        @JvmName("setCoversRounded")
        set(value) {
            field = value
            notifyPropertyChanged(BR.coversRounded)
        }

    /** User's wish to show audio visualizer */

    @get:Bindable
    var isVisualizerShown = true
        @JvmName("isVisualizerShown") get
        @JvmName("setVisualizerShown")
        set(value) {
            field = value
            notifyPropertyChanged(BR.visualizerShown)
        }

    /** User's wish to save current track and playlist */

    @get:Bindable
    var isSavingCurTrackAndPlaylist = true
        @JvmName("isSavingCurTrackAndPlaylist") get
        @JvmName("setSavingCurTrackAndPlaylist")
        set(value) {
            field = value
            notifyPropertyChanged(BR.savingCurTrackAndPlaylist)
        }

    /** User's wish to save looping */

    @get:Bindable
    var isSavingLooping = true
        @JvmName("isSavingLooping") get
        @JvmName("getSavingLooping")
        set(value) {
            field = value
            notifyPropertyChanged(BR.savingLooping)
        }

    /** User's wish to save equalizer settings */

    @get:Bindable
    var isSavingEqualizerSettings = true
        @JvmName("isSavingEqualizerSettings") get
        @JvmName("setSavingEqualizerSettings")
        set(value) {
            field = value
            notifyPropertyChanged(BR.savingEqualizerSettings)
        }

    /**
     * Enable or disable bloom effect in app.
     * Bloom effects is available from Android N+
     */

    @get:Bindable
    @RequiresApi(Build.VERSION_CODES.N)
    var isBloomEnabled = true
        @JvmName("isBloomEnabled")
        @RequiresApi(Build.VERSION_CODES.N)
        get

        @JvmName("setBloomEnabled")
        @RequiresApi(Build.VERSION_CODES.N)
        set(value) {
            field = value
            notifyPropertyChanged(BR.bloomEnabled)
        }

    /**
     * Enable or disable dividers for list fragments.
     * Works only on old APIs (before Android N)
     */

    @get:Bindable
    internal var areDividersShown = true
        @JvmName("isDividersShown") get
        @JvmName("setDividersShown")
        set(value) {
            field = value
            notifyPropertyChanged(BR.dividersShown)
        }

    /** Tracks' order (By what and is ascending) */

    @get:Bindable
    var tracksOrder = TracksOrder.TITLE to true
        @JvmName("getTracksOrder") get
        @JvmName("setTracksOrder")
        set(value) {
            field = value
            notifyPropertyChanged(BR.tracksOrder)
        }

    /** App's background image */

    @get:Bindable
    var backgroundImage: ByteArray? = null
        @JvmName("getBackgroundImage") get
        @JvmName("setBackgroundImage")
        set(value) {
            field = value
            notifyPropertyChanged(BR.backgroundImage)
        }

    /** Start first playback with equalizer */

    @get:Bindable
    var isStartingWithEqualizer = false
        @JvmName("isStartingWithEqualizer") get
        @JvmName("setStartingWithEqualizer")
        set(value) {
            field = value
            notifyPropertyChanged(BR.startingWithEqualizer)
        }

    @get:Bindable
    @RequiresApi(Build.VERSION_CODES.P)
    var isUsingAndroidNotification = false
        @JvmName("isUsingAndroidNotification")
        @RequiresApi(Build.VERSION_CODES.P)
        get

        @JvmName("setUsingAndroidNotification")
        @RequiresApi(Build.VERSION_CODES.P)
        set(value) {
            field = value
            notifyPropertyChanged(BR.usingAndroidNotification)
        }

    /** Path where converted tracks are saved */

    @get:Bindable
    var pathToSave = NO_PATH
        @JvmName("getPathToSave") get
        @JvmName("setPathToSave")
        set(value) {
            field = value
            notifyPropertyChanged(BR.pathToSave)
        }


    /** Is background set with blurred images */

    @get:Bindable
    var isBlurEnabled = true
        @JvmName("isBlurEnabled") get
        @JvmName("setBlurEnabled")
        set(value) {
            field = value
            notifyPropertyChanged(BR.blurEnabled)
        }

    /** Auto save time in seconds */

    @JvmField
    var autoSaveTime = AtomicInteger()

    @get:Bindable
    var primaryColor = -1
        @JvmName("getPrimaryColor") get
        @JvmName("setPrimaryColor")
        set(value) {
            field = value
            notifyPropertyChanged(BR.primaryColor)
        }

    private fun getPrimaryColor(primaryColor: Int) =
        primaryColor.takeIf { it != -1 } ?: theme.rgb

    @get:Bindable
    var secondaryColor = -1
        @JvmName("getSecondaryColor") get
        @JvmName("setSecondaryColor")
        set(value) {
            field = value
            notifyPropertyChanged(BR.secondaryColor)
        }

    private fun getSecondaryColor(secondaryColor: Int) = when {
        secondaryColor != -1 -> when (secondaryColor) {
            0 -> applicationContext.resources.getColor(R.color.white, null)
            else -> applicationContext.resources.getColor(R.color.black, null)
        }

        else -> applicationContext.resources.getColor(
            if (theme.isNight) R.color.black else R.color.white, null
        )
    }

    @get:Bindable
    var fontColor = if (theme.isNight) Color.WHITE else Color.BLACK
        @JvmName("getFontColor") get
        @JvmName("setFontColor")
        set(value) {
            field = value
            notifyPropertyChanged(BR.fontColor)
        }

    internal fun getFontColor() = StorageUtil.instance.loadFontColor()
        .takeIf { it != Int.MIN_VALUE }
        ?: when {
            secondaryColor != -1 -> when (secondaryColor) {
                0 -> Color.BLACK
                else -> Color.WHITE
            }

            else -> if (theme.isNight) Color.WHITE else Color.BLACK
        }

    /**
     * Changes language and restarts activity
     * @param lang [Language] itself
     */

    internal fun changeLang(context: Context, lang: Language) {
        Lingver.getInstance().setLocale(context.applicationContext, Locale(lang.name.lowercase()))
        StorageUtil.instance.storeLanguage(lang)
    }

    /**
     * Changes theme and restarts activity
     * @param theme number of theme
     * @see Colors
     */

    internal fun changeTheme(activity: Activity, theme: Int) {
        StorageUtil.instance.storeTheme(theme)
        this.theme = chooseTheme(theme)
        primaryColor = this.theme.rgb
        secondaryColor = applicationContext.resources.getColor(
            if (this.theme.isNight) R.color.black else R.color.white, null
        )

        Divider.update()
        FontDivider.update()
        Marker.update()

        activity.finishAndRemoveTask()
        activity.startActivity(
            Intent(applicationContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private inline val applicationContext
        get() = application.unchecked.applicationContext

    /**
     * Gets color if bloom is enabled.
     * @param color color to set in some view
     * @return [color] itself if [isBloomEnabled] else [android.R.color.transparent]
     */

    @RequiresApi(Build.VERSION_CODES.N)
    @JvmName("getBloomOrTransparent")
    internal fun getBloomOrTransparent(color: Int) =
        if (isBloomEnabled) color else android.R.color.transparent

    internal inline val isCustomTheme
        get() = secondaryColor != -1

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
    // fun main() = File("...\\Prima\\app\\src\\main\\res\\font")
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

    @JvmName("getFontFromName")
    internal fun getFontFromName(font: String) = when (font) {
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
        "Anders" -> ResourcesCompat.getFont(applicationContext, R.font.anders)
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
        "Arial" -> ResourcesCompat.getFont(applicationContext, R.font.arialn)
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
        "Calibri" -> ResourcesCompat.getFont(applicationContext, R.font.calibri)
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
        "Times New Roman" -> ResourcesCompat.getFont(applicationContext, R.font.times_new_roman)
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