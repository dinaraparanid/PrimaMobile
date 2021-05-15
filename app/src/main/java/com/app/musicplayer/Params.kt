package com.app.musicplayer

import com.app.musicplayer.utils.Colors

class Params private constructor() {
    companion object {
        private var INSTANCE: Params? = null

        fun initialize() {
            if (INSTANCE == null)
                INSTANCE = Params()
        }

        fun getInstance() =
            INSTANCE ?: throw IllegalStateException("MusicRepository is not initialized")
    }

    val theme: Colors = Colors.Sea()
}