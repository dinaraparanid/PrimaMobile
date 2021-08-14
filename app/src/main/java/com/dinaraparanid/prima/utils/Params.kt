package com.dinaraparanid.prima.utils

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.BaseObservable
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track
import com.yariksoffice.lingver.Lingver
import java.util.*

/**
 * Container of some params for app
 */

internal class Params private constructor() : BaseObservable() {
    internal companion object {
        /** Supported languages */
        internal enum class Language {
            EN, AR, BE, BG, DE, EL, ES, FR, IT, JA, KO, MN, NO, PL, PT, RU, SV, TR, UK, ZH
        }

        /** Tracks ordering by some param */
        internal enum class TracksOrder {
            TITLE, ARTIST, ALBUM, DATE
        }

        private var INSTANCE: Params? = null

        /**
         * Initialises class only once.
         * Sets theme, progress (if user allow it),
         * rounding of playlists and language.
         */

        @JvmStatic
        internal fun initialize(app: Application) {
            if (INSTANCE == null) {
                INSTANCE = Params().apply {
                    application = app
                    val su = StorageUtil(app)
                    theme = su.loadTheme()
                    isRoundingPlaylistImage = su.loadRounded()
                    font = su.loadFont()
                    showPlaylistsImages = su.loadShowPlaylistsImages()
                    showVisualizer = su.loadShowVisualizer()
                    saveCurTrackAndPlaylist = su.loadSaveCurTrackAndPlaylist()
                    saveLooping = su.loadSaveLooping()
                    saveEqualizerSettings = su.loadSaveEqualizerSettings()
                    tracksOrder = su.loadTrackOrder() ?: TracksOrder.TITLE to true
                    themeColor = su.loadCustomThemeColors() ?: -1 to -1
                }

                var noLang = false

                Lingver.init(
                    app,
                    Locale(StorageUtil(app).loadLanguage()?.name?.lowercase() ?: run {
                        noLang = true
                        Language.EN.name.lowercase()
                    })
                )

                if (noLang)
                    Lingver.getInstance().setFollowSystemLocale(app)
            }
        }

        /**
         * Gets instance
         * @throws UninitializedPropertyAccessException if it wasn't initialized
         * @return instance if it was created
         * @see initialize
         */

        @JvmStatic
        internal val instance: Params
            get() = INSTANCE
                ?: throw UninitializedPropertyAccessException("Params is not initialized")

        /**
         * Converts [Int] to [Colors] with themes
         */

        @JvmStatic
        internal fun chooseTheme(theme: Int): Colors = when (theme) {
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
        internal fun sortedTrackList(trackList: List<Track>) = when {
            instance.tracksOrder.second -> when (instance.tracksOrder.first) {
                Companion.TracksOrder.TITLE -> trackList.sortedBy(Track::title)
                Companion.TracksOrder.ARTIST -> trackList.sortedBy(Track::artist)
                Companion.TracksOrder.ALBUM -> trackList.sortedBy(Track::playlist)
                Companion.TracksOrder.DATE -> trackList.sortedBy(Track::addDate)
            }

            else -> when (instance.tracksOrder.first) {
                Companion.TracksOrder.TITLE ->
                    trackList.sortedByDescending(Track::title)
                Companion.TracksOrder.ARTIST ->
                    trackList.sortedByDescending(Track::artist)
                Companion.TracksOrder.ALBUM ->
                    trackList.sortedByDescending(Track::playlist)
                Companion.TracksOrder.DATE ->
                    trackList.sortedByDescending(Track::addDate)
            }
        }
    }

    internal lateinit var application: Application
        private set

    /** Current theme for app */
    lateinit var theme: Colors
        private set

    /** App's font */
    lateinit var font: String

    /** User's wish to show playlists' images */
    var showPlaylistsImages = true

    /** User's wish of rounded playlist's images */
    var isRoundingPlaylistImage = true

    /** User's wish to show audio visualizer */
    var showVisualizer = true

    /** User's wish to save current track and playlist */
    var saveCurTrackAndPlaylist = true

    /** User's wish to save looping */
    var saveLooping = true

    /** User's wish to save equalizer settings */
    var saveEqualizerSettings = true

    /** Tracks' order (By what and is ascending) */
    var tracksOrder = TracksOrder.TITLE to true

    /** Custom theme color */
    lateinit var themeColor: Pair<Int, Int>

    internal val primaryColor
        @JvmName("getPrimaryColor")
        get() = if (themeColor.first != -1) themeColor.first else theme.rgb

    internal val secondaryColor
        @JvmName("getSecondaryColor")
        get() = if (themeColor.second != -1) themeColor.second else
            ViewSetter.getBackgroundColor(application)

    internal val fontColor
        @JvmName("getFontColor")
        get() = when {
            themeColor.second != -1 -> when (themeColor.second) {
                0 -> Color.BLACK
                else -> Color.WHITE
            }

            else -> ViewSetter.textColor
        }

    /**
     * Changes language and restarts activity
     * @param number number of language
     */

    internal fun changeLang(context: Context, number: Int) {
        val lang = Language.values()[number]
        Lingver.getInstance().setLocale(context, Locale(lang.name.lowercase()))
        StorageUtil(context).storeLanguage(lang)

        context.startActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /**
     * Changes theme and restarts activity
     * @param theme number of theme
     * @see Colors
     */

    internal fun changeTheme(context: Context, theme: Int) {
        StorageUtil(context).storeTheme(theme)
        this.theme = chooseTheme(theme)
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private inline val applicationContext
        get() = application.applicationContext

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

    @JvmName("getFontFromName")
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