package com.dinaraparanid.prima.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import com.dinaraparanid.prima.R

/**
 * View objects setter for main activity
 */

internal enum class ViewSetter {;

    internal companion object {
        /**
         * Text color for different themes
         * @return If theme is a day theme -> black else white
         */

        @JvmStatic
        internal inline val textColor
            get() = if (Params.instance.theme.isNight) Color.WHITE else Color.BLACK

        /**
         * Background color for different themes
         * @return If theme is a day theme -> white else black
         */

        @JvmStatic
        internal fun getBackgroundColor(context: Context) = context.resources.getColor(
            if (Params.instance.theme.isNight) R.color.black else R.color.white, null
        )

        /** Gets app theme */

        @JvmStatic
        internal inline val appTheme
            get() = when (Params.instance.theme) {
                is Colors.Blue -> R.style.Theme_MusicPlayerBlue
                is Colors.Green -> R.style.Theme_MusicPlayerGreen
                is Colors.GreenTurquoise -> R.style.Theme_MusicPlayerGreenTurquoise
                is Colors.Lemon -> R.style.Theme_MusicPlayerLemon
                is Colors.Orange -> R.style.Theme_MusicPlayerOrange
                is Colors.Pink -> R.style.Theme_MusicPlayerPink
                is Colors.Purple -> R.style.Theme_MusicPlayerPurple
                is Colors.Red -> R.style.Theme_MusicPlayerRed
                is Colors.Sea -> R.style.Theme_MusicPlayerSea
                is Colors.Turquoise -> R.style.Theme_MusicPlayerTurquoise
                is Colors.BlueNight -> R.style.Theme_MusicPlayerBlueNight
                is Colors.GreenNight -> R.style.Theme_MusicPlayerGreenNight
                is Colors.GreenTurquoiseNight -> R.style.Theme_MusicPlayerGreenTurquoiseNight
                is Colors.LemonNight -> R.style.Theme_MusicPlayerLemonNight
                is Colors.OrangeNight -> R.style.Theme_MusicPlayerOrangeNight
                is Colors.PinkNight -> R.style.Theme_MusicPlayerPinkNight
                is Colors.PurpleNight -> R.style.Theme_MusicPlayerPurpleNight
                is Colors.RedNight -> R.style.Theme_MusicPlayerRedNight
                is Colors.SeaNight -> R.style.Theme_MusicPlayerSeaNight
                is Colors.TurquoiseNight -> R.style.Theme_MusicPlayerTurquoiseNight
                else -> throw IllegalStateException("Wrong theme")
            }

        /**
         * Gets like button image
         * depending on current theme and like status
         * @param isLiked was track liked
         */

        @JvmStatic
        internal fun getLikeButtonImage(isLiked: Boolean) = when {
            isLiked -> R.drawable.heart_like
            else -> R.drawable.heart
        }

        /**
         * Gets looping button image
         * depending on current theme and repeat status
         */

        @JvmStatic
        internal fun getRepeatButtonImage() = when (Params.instance.loopingStatus) {
            Params.Companion.Looping.PLAYLIST -> R.drawable.repeat
            Params.Companion.Looping.TRACK -> R.drawable.repeat_1
            Params.Companion.Looping.NONE -> R.drawable.no_repeat
        }

        /**
         * Gets play or pause image for small button
         * @param isPlaying is music playing now
         */

        @JvmStatic
        internal fun getPlayButtonSmallImage(isPlaying: Boolean) = when {
            isPlaying -> android.R.drawable.ic_media_pause
            else -> android.R.drawable.ic_media_play
        }

        /**
         * Gets play or pause image for big button
         * @param isPlaying is music playing now
         */

        @JvmStatic
        internal fun getPlayButtonImage(isPlaying: Boolean) = when {
            isPlaying -> R.drawable.pause
            else -> R.drawable.play
        }

        /**
         * Converts bitmap in some scale
         * @param image image to convert
         * @param width width to convert
         * @param height height to convert
         * @return converted picture
         */

        @JvmStatic
        internal fun getPictureInScale(image: Bitmap, width: Int, height: Int): Bitmap =
            Bitmap.createBitmap(
                image,
                0, 0,
                width, height,
                Matrix(),
                false
            )

        /**
         * Gets color state list with theme's color
         */

        @JvmStatic
        internal inline val colorStateList
            get() = ColorStateList.valueOf(Params.instance.primaryColor)
    }
}