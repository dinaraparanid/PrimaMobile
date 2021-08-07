package com.dinaraparanid.prima.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import com.dinaraparanid.prima.R

/**
 * View objects setter for main activity
 */

internal enum class ViewSetter {;

    companion object {
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

        /**
         * Gets return arrow button image
         * depending on current theme
         */

        @JvmStatic
        internal inline val returnButtonImage
            get() = when (Params.instance.theme) {
                is Colors.Blue -> R.drawable.arrow_blue
                is Colors.BlueNight -> R.drawable.arrow_blue
                is Colors.Green -> R.drawable.arrow_green
                is Colors.GreenNight -> R.drawable.arrow_green
                is Colors.GreenTurquoise -> R.drawable.arrow_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.arrow_green_turquoise
                is Colors.Lemon -> R.drawable.arrow_lemon
                is Colors.LemonNight -> R.drawable.arrow_lemon
                is Colors.Orange -> R.drawable.arrow_orange
                is Colors.OrangeNight -> R.drawable.arrow_orange
                is Colors.Pink -> R.drawable.arrow_pink
                is Colors.PinkNight -> R.drawable.arrow_pink
                is Colors.Purple -> R.drawable.arrow_purple
                is Colors.PurpleNight -> R.drawable.arrow_purple
                is Colors.Red -> R.drawable.arrow_red
                is Colors.RedNight -> R.drawable.arrow_red
                is Colors.Sea -> R.drawable.arrow_sea
                is Colors.SeaNight -> R.drawable.arrow_sea
                is Colors.Turquoise -> R.drawable.arrow_turquoise
                is Colors.TurquoiseNight -> R.drawable.arrow_turquoise
                else -> R.drawable.arrow
            }

        /**
         * Gets settings button image
         * depending on current theme
         */

        @JvmStatic
        internal inline val settingsButtonImage
            get() = when (Params.instance.theme) {
                is Colors.Blue -> R.drawable.three_dots_blue
                is Colors.BlueNight -> R.drawable.three_dots_blue
                is Colors.Green -> R.drawable.three_dots_green
                is Colors.GreenNight -> R.drawable.three_dots_green
                is Colors.GreenTurquoise -> R.drawable.three_dots_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.three_dots_green_turquoise
                is Colors.Lemon -> R.drawable.three_dots_lemon
                is Colors.LemonNight -> R.drawable.three_dots_lemon
                is Colors.Orange -> R.drawable.three_dots_orange
                is Colors.OrangeNight -> R.drawable.three_dots_orange
                is Colors.Pink -> R.drawable.three_dots_pink
                is Colors.PinkNight -> R.drawable.three_dots_pink
                is Colors.Purple -> R.drawable.three_dots_purple
                is Colors.PurpleNight -> R.drawable.three_dots_purple
                is Colors.Red -> R.drawable.three_dots_red
                is Colors.RedNight -> R.drawable.three_dots_red
                is Colors.Sea -> R.drawable.three_dots_sea
                is Colors.SeaNight -> R.drawable.three_dots_sea
                is Colors.Turquoise -> R.drawable.three_dots_turquoise
                is Colors.TurquoiseNight -> R.drawable.three_dots_turquoise
                else -> R.drawable.three_dots
            }

        /**
         * Gets tracks image in main menu
         * depending on current theme
         */

        @JvmStatic
        internal inline val tracksMenuImage
            get() = when (Params.instance.theme) {
                is Colors.Blue -> R.drawable.tracks_blue
                is Colors.BlueNight -> R.drawable.tracks_blue
                is Colors.Green -> R.drawable.tracks_green
                is Colors.GreenNight -> R.drawable.tracks_green
                is Colors.GreenTurquoise -> R.drawable.tracks_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.tracks_green_turquoise
                is Colors.Lemon -> R.drawable.tracks_lemon
                is Colors.LemonNight -> R.drawable.tracks_lemon
                is Colors.Orange -> R.drawable.tracks_orange
                is Colors.OrangeNight -> R.drawable.tracks_orange
                is Colors.Pink -> R.drawable.tracks_pink
                is Colors.PinkNight -> R.drawable.tracks_pink
                is Colors.Purple -> R.drawable.tracks_purple
                is Colors.PurpleNight -> R.drawable.tracks_purple
                is Colors.Red -> R.drawable.tracks_red
                is Colors.RedNight -> R.drawable.tracks_red
                is Colors.Sea -> R.drawable.tracks_sea
                is Colors.SeaNight -> R.drawable.tracks_sea
                is Colors.Turquoise -> R.drawable.tracks_turquoise
                is Colors.TurquoiseNight -> R.drawable.tracks_turquoise
                else -> R.drawable.tracks_blue
            }

        /**
         * Gets tracks collections image in main menu
         * depending on current theme
         */

        @JvmStatic
        internal inline val playlistMenuImage
            get() = when (Params.instance.theme) {
                is Colors.Blue -> R.drawable.playlist_blue
                is Colors.BlueNight -> R.drawable.playlist_blue
                is Colors.Green -> R.drawable.playlist_green
                is Colors.GreenNight -> R.drawable.playlist_green
                is Colors.GreenTurquoise -> R.drawable.playlist_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.playlist_green_turquoise
                is Colors.Lemon -> R.drawable.playlist_lemon
                is Colors.LemonNight -> R.drawable.playlist_lemon
                is Colors.Orange -> R.drawable.playlist_orange
                is Colors.OrangeNight -> R.drawable.playlist_orange
                is Colors.Pink -> R.drawable.playlist_pink
                is Colors.PinkNight -> R.drawable.playlist_pink
                is Colors.Purple -> R.drawable.playlist_purple
                is Colors.PurpleNight -> R.drawable.playlist_purple
                is Colors.Red -> R.drawable.playlist_red
                is Colors.RedNight -> R.drawable.playlist_red
                is Colors.Sea -> R.drawable.playlist_sea
                is Colors.SeaNight -> R.drawable.playlist_sea
                is Colors.Turquoise -> R.drawable.playlist_turquoise
                is Colors.TurquoiseNight -> R.drawable.playlist_turquoise
                else -> R.drawable.playlist
            }

        /**
         * Gets artist image in main menu
         * depending on current theme
         */

        @JvmStatic
        internal inline val artistMenuImage
            get() = when (Params.instance.theme) {
                is Colors.Blue -> R.drawable.human_blue
                is Colors.BlueNight -> R.drawable.human_blue
                is Colors.Green -> R.drawable.human_green
                is Colors.GreenNight -> R.drawable.human_green
                is Colors.GreenTurquoise -> R.drawable.human_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.human_green_turquoise
                is Colors.Lemon -> R.drawable.human_lemon
                is Colors.LemonNight -> R.drawable.human_lemon
                is Colors.Orange -> R.drawable.human_orange
                is Colors.OrangeNight -> R.drawable.human_orange
                is Colors.Pink -> R.drawable.human_pink
                is Colors.PinkNight -> R.drawable.human_pink
                is Colors.Purple -> R.drawable.human_purple
                is Colors.PurpleNight -> R.drawable.human_purple
                is Colors.Red -> R.drawable.human_red
                is Colors.RedNight -> R.drawable.human_red
                is Colors.Sea -> R.drawable.human_sea
                is Colors.SeaNight -> R.drawable.human_sea
                is Colors.Turquoise -> R.drawable.human_turquoise
                is Colors.TurquoiseNight -> R.drawable.human_turquoise
                else -> R.drawable.human
            }

        /**
         * Gets favourite tracks image in main menu
         * depending on current theme
         */

        @JvmStatic
        internal inline val favouriteTrackMenuImage
            get() = when (Params.instance.theme) {
                is Colors.Blue -> R.drawable.favourite_track_blue
                is Colors.BlueNight -> R.drawable.favourite_track_blue
                is Colors.Green -> R.drawable.favourite_track_green
                is Colors.GreenNight -> R.drawable.favourite_track_green
                is Colors.GreenTurquoise -> R.drawable.favourite_track_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.favourite_track_green_turquoise
                is Colors.Lemon -> R.drawable.favourite_track_lemon
                is Colors.LemonNight -> R.drawable.favourite_track_lemon
                is Colors.Orange -> R.drawable.favourite_track_orange
                is Colors.OrangeNight -> R.drawable.favourite_track_orange
                is Colors.Pink -> R.drawable.favourite_track_pink
                is Colors.PinkNight -> R.drawable.favourite_track_pink
                is Colors.Purple -> R.drawable.favourite_track_purple
                is Colors.PurpleNight -> R.drawable.favourite_track_purple
                is Colors.Red -> R.drawable.favourite_track_red
                is Colors.RedNight -> R.drawable.favourite_track_red
                is Colors.Sea -> R.drawable.favourite_track_sea
                is Colors.SeaNight -> R.drawable.favourite_track_sea
                is Colors.Turquoise -> R.drawable.favourite_track_turquoise
                is Colors.TurquoiseNight -> R.drawable.favourite_track_turquoise
                else -> R.drawable.favourite_track
            }

        /**
         * Gets favourite artist image in main menu
         * depending on current theme
         */

        @JvmStatic
        internal inline val favouriteArtistMenuImage
            get() = when (Params.instance.theme) {
                is Colors.Blue -> R.drawable.favourite_artist_blue
                is Colors.BlueNight -> R.drawable.favourite_artist_blue
                is Colors.Green -> R.drawable.favourite_artist_green
                is Colors.GreenNight -> R.drawable.favourite_artist_green
                is Colors.GreenTurquoise -> R.drawable.favourite_artist_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.favourite_artist_green_turquoise
                is Colors.Lemon -> R.drawable.favourite_artist_lemon
                is Colors.LemonNight -> R.drawable.favourite_artist_lemon
                is Colors.Orange -> R.drawable.favourite_artist_orange
                is Colors.OrangeNight -> R.drawable.favourite_artist_orange
                is Colors.Pink -> R.drawable.favourite_artist_pink
                is Colors.PinkNight -> R.drawable.favourite_artist_pink
                is Colors.Purple -> R.drawable.favourite_artist_purple
                is Colors.PurpleNight -> R.drawable.favourite_artist_purple
                is Colors.Red -> R.drawable.favourite_artist_red
                is Colors.RedNight -> R.drawable.favourite_artist_red
                is Colors.Sea -> R.drawable.favourite_artist_sea
                is Colors.SeaNight -> R.drawable.favourite_artist_sea
                is Colors.Turquoise -> R.drawable.favourite_artist_turquoise
                is Colors.TurquoiseNight -> R.drawable.favourite_artist_turquoise
                else -> R.drawable.favourite_artist
            }

        /**
         * Gets settings image in main menu
         * depending on current theme
         */

        @JvmStatic
        internal inline val settingsMenuImage
            get() = when (Params.instance.theme) {
                is Colors.Blue -> R.drawable.settings_blue
                is Colors.BlueNight -> R.drawable.settings_blue
                is Colors.Green -> R.drawable.settings_green
                is Colors.GreenNight -> R.drawable.settings_green
                is Colors.GreenTurquoise -> R.drawable.settings_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.settings_green_turquoise
                is Colors.Lemon -> R.drawable.settings_lemon
                is Colors.LemonNight -> R.drawable.settings_lemon
                is Colors.Orange -> R.drawable.settings_orange
                is Colors.OrangeNight -> R.drawable.settings_orange
                is Colors.Pink -> R.drawable.settings_pink
                is Colors.PinkNight -> R.drawable.settings_pink
                is Colors.Purple -> R.drawable.settings_purple
                is Colors.PurpleNight -> R.drawable.settings_purple
                is Colors.Red -> R.drawable.settings_red
                is Colors.RedNight -> R.drawable.settings_red
                is Colors.Sea -> R.drawable.settings_sea
                is Colors.SeaNight -> R.drawable.settings_sea
                is Colors.Turquoise -> R.drawable.settings_turquoise
                is Colors.TurquoiseNight -> R.drawable.settings_turquoise
                else -> R.drawable.settings
            }

        /**
         * Gets about app image in main menu
         * depending on current theme
         */

        @JvmStatic
        internal inline val aboutAppMenuImage
            get() = when (Params.instance.theme) {
                is Colors.Blue -> R.drawable.about_app_blue
                is Colors.BlueNight -> R.drawable.about_app_blue
                is Colors.Green -> R.drawable.about_app_green
                is Colors.GreenNight -> R.drawable.about_app_green
                is Colors.GreenTurquoise -> R.drawable.about_app_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.about_app_green_turquoise
                is Colors.Lemon -> R.drawable.about_app_lemon
                is Colors.LemonNight -> R.drawable.about_app_lemon
                is Colors.Orange -> R.drawable.about_app_orange
                is Colors.OrangeNight -> R.drawable.about_app_orange
                is Colors.Pink -> R.drawable.about_app_pink
                is Colors.PinkNight -> R.drawable.about_app_pink
                is Colors.Purple -> R.drawable.about_app_purple
                is Colors.PurpleNight -> R.drawable.about_app_purple
                is Colors.Red -> R.drawable.about_app_red
                is Colors.RedNight -> R.drawable.about_app_red
                is Colors.Sea -> R.drawable.about_app_sea
                is Colors.SeaNight -> R.drawable.about_app_sea
                is Colors.Turquoise -> R.drawable.about_app_turquoise
                is Colors.TurquoiseNight -> R.drawable.about_app_turquoise
                else -> R.drawable.about_app
            }

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
         * @param isLooping looping status
         */

        @JvmStatic
        internal fun getRepeatButtonImage(isLooping: Boolean) = when {
            isLooping -> R.drawable.repeat_1
            else -> R.drawable.repeat
        }

        /**
         * Gets shuffle image
         * depending on current theme
         */

        @JvmStatic
        internal inline val shuffleImage
            get() = when (Params.instance.theme) {
                is Colors.Blue -> R.drawable.shuffle_blue
                is Colors.BlueNight -> R.drawable.shuffle_blue
                is Colors.Green -> R.drawable.shuffle_green
                is Colors.GreenNight -> R.drawable.shuffle_green
                is Colors.GreenTurquoise -> R.drawable.shuffle_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.shuffle_green_turquoise
                is Colors.Lemon -> R.drawable.shuffle_lemon
                is Colors.LemonNight -> R.drawable.shuffle_lemon
                is Colors.Orange -> R.drawable.shuffle_orange
                is Colors.OrangeNight -> R.drawable.shuffle_orange
                is Colors.Pink -> R.drawable.shuffle_pink
                is Colors.PinkNight -> R.drawable.shuffle_pink
                is Colors.Purple -> R.drawable.shuffle_purple
                is Colors.PurpleNight -> R.drawable.shuffle_purple
                is Colors.Red -> R.drawable.shuffle_red
                is Colors.RedNight -> R.drawable.shuffle_red
                is Colors.Sea -> R.drawable.shuffle_sea
                is Colors.SeaNight -> R.drawable.shuffle_sea
                is Colors.Turquoise -> R.drawable.shuffle_turquoise
                is Colors.TurquoiseNight -> R.drawable.shuffle_turquoise
                else -> R.drawable.shuffle
            }

        /**
         * Gets play or pause image for small button
         * @param isPlaying is music playing now
         */

        @JvmStatic
        internal fun getPlayButtonSmallImage(isPlaying: Boolean) = when (isPlaying) {
            true -> android.R.drawable.ic_media_pause
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
    }
}