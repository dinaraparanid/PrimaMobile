package com.app.musicplayer.utils

import android.graphics.Color

sealed class Colors(private val r: Int, private val g: Int, private val b: Int) : Color() {
    init {
        rgb(r, g, b)
    }

    val rgb: Int
        get() = rgb(r, g, b)

    class Purple : Colors(110, 60, 255)
    class Red : Colors(160, 25, 30)
    class Blue : Colors(30, 45, 210)
    class Green : Colors(25, 160, 40)
    class Golden : Colors(180, 200, 15)
    class Orange : Colors(225, 135, 15)
    class Lemon : Colors(190, 225, 15)
    class Turquoise : Colors(15, 225, 200)
    class GreenTurquoise : Colors(15, 225, 150)
    class Sea : Colors(15, 210, 225)
    class Pink : Colors(220, 15, 225)
}