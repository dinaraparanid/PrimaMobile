package com.dinaraparanid.prima.utils

class Params private constructor() {
    companion object {
        private var INSTANCE: Params? = null

        fun initialize() {
            if (INSTANCE == null)
                INSTANCE = Params()
        }

        val instance: Params
            get() = INSTANCE ?: throw IllegalStateException("Params is not initialized")
    }

    val theme: Colors = Colors.PurpleNight()
}