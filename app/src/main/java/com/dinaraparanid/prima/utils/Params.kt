package com.dinaraparanid.prima.utils

import android.app.Application
import android.content.Context
import android.content.Intent
import com.dinaraparanid.prima.MainActivity
import com.yariksoffice.lingver.Lingver
import java.util.Locale

internal class Params private constructor() {
    companion object {
        internal enum class Language {
            EN, AR, BE, BG, DE, EL, ES, FR, IT, JA, KO, MN, NO, PL, PT, RU, SV, TR, UK, ZH
        }

        private var INSTANCE: Params? = null

        @JvmStatic
        fun initialize(app: Application) {
            if (INSTANCE == null) {
                INSTANCE = Params().apply {
                    val su = StorageUtil(app)
                    theme = su.loadTheme()
                    saveProgress = su.loadSaveProgress()
                    roundPlaylist = su.loadRounded()
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

        @JvmStatic
        val instance: Params
            get() = INSTANCE
                ?: throw UninitializedPropertyAccessException("Params is not initialized")

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

    lateinit var theme: Colors
        private set

    var saveProgress = true
    var roundPlaylist = true

    fun changeLang(context: Context, number: Int) {
        val lang = Language.values()[number]
        Lingver.getInstance().setLocale(context, Locale(lang.name.lowercase()))
        StorageUtil(context).storeLanguage(lang)

        context.startActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun changeTheme(context: Context, theme: Int) {
        StorageUtil(context).storeTheme(theme)
        this.theme = chooseTheme(theme)
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}