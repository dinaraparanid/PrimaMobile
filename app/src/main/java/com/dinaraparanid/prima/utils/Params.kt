package com.dinaraparanid.prima.utils

import android.content.Context

class Params private constructor() {
    companion object {
        internal enum class Language {
            ENGLISH,
            ARABIC,
            BELARUSIAN,
            BULGARIAN,
            GERMAN,
            GREEK,
            SPANISH,
            FRENCH,
            ITALIAN,
            JAPANESE,
            KOREAN,
            MONGOLIAN,
            NORWEGIAN,
            POLISH,
            PORTUGUESE,
            RUSSIAN,
            SWEDISH,
            TURKISH,
            UKRAINIAN,
            CHINESE
        }

        private var INSTANCE: Params? = null

        @JvmStatic
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = Params()
                INSTANCE!!.language = StorageUtil(context).loadLanguage()
            }
        }

        @JvmStatic
        val instance: Params
            get() = INSTANCE ?: throw IllegalStateException("Params is not initialized")
    }

    internal lateinit var language: Language
    val theme: Colors = Colors.RedNight()
}