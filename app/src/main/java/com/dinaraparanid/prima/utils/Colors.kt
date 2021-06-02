package com.dinaraparanid.prima.utils

import android.graphics.Color

sealed class Colors(
    private val r: Int,
    private val g: Int,
    private val b: Int,
    val isNight: Boolean
) : Color() {
    init { rgb }

    val rgb: Int get() = rgb(r, g, b)

    class Purple : Colors(110, 60, 255, false)
    class Red : Colors(230, 90, 125, false)
    class Blue : Colors(30, 45, 210, false)
    class Green : Colors(25, 160, 40, false)
    class Orange : Colors(225, 135, 15, false)
    class Lemon : Colors(190, 225, 15, false)
    class Turquoise : Colors(15, 225, 200, false)
    class GreenTurquoise : Colors(15, 225, 150, false)
    class Sea : Colors(15, 210, 225, false)
    class Pink : Colors(220, 15, 225, false)

    class PurpleNight : Colors(110, 60, 255, true)
    class RedNight : Colors(160, 25, 30, true)
    class BlueNight : Colors(30, 45, 210, true)
    class GreenNight : Colors(25, 160, 40, true)
    class OrangeNight : Colors(225, 135, 15, true)
    class LemonNight : Colors(190, 225, 15, true)
    class TurquoiseNight : Colors(15, 225, 200, true)
    class GreenTurquoiseNight : Colors(15, 225, 150, true)
    class SeaNight : Colors(15, 210, 225, true)
    class PinkNight : Colors(220, 15, 225, true)

    @Deprecated("Too awful")
    private class Golden : Colors(180, 200, 15, false)

    @Deprecated("Too awful")
    private class GoldenNight : Colors(180, 200, 15, true)
}