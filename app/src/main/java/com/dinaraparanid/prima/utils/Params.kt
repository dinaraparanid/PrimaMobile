package com.dinaraparanid.prima.utils

import android.app.Application
import android.content.Context
import android.content.Intent
import com.dinaraparanid.prima.MainActivity
import com.yariksoffice.lingver.Lingver
import java.util.Locale

/**
 * Container of some params for app
 */

internal class Params private constructor() {
    companion object {
        /** Supported languages */
        internal enum class Language {
            EN, AR, BE, BG, DE, EL, ES, FR, IT, JA, KO, MN, NO, PL, PT, RU, SV, TR, UK, ZH
        }

        private var INSTANCE: Params? = null

        /**
         * Initialises class only once.
         * Sets theme, progress (if user allow it),
         * rounding of playlists and language.
         */

        @JvmStatic
        fun initialize(app: Application) {
            if (INSTANCE == null) {
                INSTANCE = Params().apply {
                    val su = StorageUtil(app)
                    theme = su.loadTheme()
                    isSavingProgress = su.loadSaveProgress()
                    isRoundingPlaylistImage = su.loadRounded()
                    font = su.loadFont()
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
        val instance: Params
            get() = INSTANCE
                ?: throw UninitializedPropertyAccessException("Params is not initialized")

        /**
         * Converts [Int] to [Colors] with themes
         */

        @JvmStatic
        fun chooseTheme(theme: Int): Colors = when (theme) {
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
    }

    /** Current theme for app */
    lateinit var theme: Colors
        private set

    /** App's font */
    lateinit var font: String

    /** User's wish to save playing progress */
    var isSavingProgress = true

    /** User's wish of rounded playlist's images */
    var isRoundingPlaylistImage = true

    /**
     * Changes language and restarts activity
     * @param number number of language
     */

    fun changeLang(context: Context, number: Int) {
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

    fun changeTheme(context: Context, theme: Int) {
        StorageUtil(context).storeTheme(theme)
        this.theme = chooseTheme(theme)
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}