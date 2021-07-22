package com.dinaraparanid.prima.utils

import android.app.Application
import android.content.Context
import android.content.Intent
import com.dinaraparanid.prima.MainActivity
import com.yariksoffice.lingver.Lingver
import java.util.Locale

class Params private constructor() {
    companion object {
        internal enum class Language {
            EN, AR, BE, BG, DE, EL, ES, FR, IT, JA, KO, MN, NO, PL, PT, RU, SV, TR, UK, ZH
        }

        private var INSTANCE: Params? = null

        @JvmStatic
        fun initialize(app: Application) {
            if (INSTANCE == null) {
                INSTANCE = Params()

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
            get() = INSTANCE ?: throw IllegalStateException("Params is not initialized")
    }

    val theme: Colors = Colors.RedNight()

    fun changeLang(context: Context, number: Int) {
        val lang = Language.values()[number]
        Lingver.getInstance().setLocale(context, Locale(lang.name.lowercase()))
        StorageUtil(context).storeLanguage(lang)

        context.startActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}