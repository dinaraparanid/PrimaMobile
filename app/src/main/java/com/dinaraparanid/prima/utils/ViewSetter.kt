package com.dinaraparanid.prima.utils

import android.graphics.Color
import com.dinaraparanid.prima.R

/**
 * View objects setter for main activity
 */

internal enum class ViewSetter {;

    companion object {
        @JvmStatic
        internal inline val textColor
            get() = if (Params.instance.theme.isNight) Color.WHITE else Color.BLACK

        @JvmStatic
        internal inline val backgroundColor
            get() = if (Params.instance.theme.isNight) Color.BLACK else Color.WHITE

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

        @JvmStatic
        internal inline val nextTrackButtonImage
            get() = when (Params.instance.theme) {
                is Colors.Blue -> R.drawable.next_track_blue
                is Colors.BlueNight -> R.drawable.next_track_blue
                is Colors.Green -> R.drawable.next_track_green
                is Colors.GreenNight -> R.drawable.next_track_green
                is Colors.GreenTurquoise -> R.drawable.next_track_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.next_track_green_turquoise
                is Colors.Lemon -> R.drawable.next_track_lemon
                is Colors.LemonNight -> R.drawable.next_track_lemon
                is Colors.Orange -> R.drawable.next_track_orange
                is Colors.OrangeNight -> R.drawable.next_track_orange
                is Colors.Pink -> R.drawable.next_track_pink
                is Colors.PinkNight -> R.drawable.next_track_pink
                is Colors.Purple -> R.drawable.next_track_purple
                is Colors.PurpleNight -> R.drawable.next_track_purple
                is Colors.Red -> R.drawable.next_track_red
                is Colors.RedNight -> R.drawable.next_track_red
                is Colors.Sea -> R.drawable.next_track_sea
                is Colors.SeaNight -> R.drawable.next_track_sea
                is Colors.Turquoise -> R.drawable.next_track_turquoise
                is Colors.TurquoiseNight -> R.drawable.next_track_turquoise
                else -> R.drawable.next_track
            }

        @JvmStatic
        internal inline val prevTrackButtonImage
            get() = when (Params.instance.theme) {
                is Colors.Blue -> R.drawable.prev_track_blue
                is Colors.BlueNight -> R.drawable.prev_track_blue
                is Colors.Green -> R.drawable.prev_track_green
                is Colors.GreenNight -> R.drawable.prev_track_green
                is Colors.GreenTurquoise -> R.drawable.prev_track_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.prev_track_green_turquoise
                is Colors.Lemon -> R.drawable.prev_track_lemon
                is Colors.LemonNight -> R.drawable.prev_track_lemon
                is Colors.Orange -> R.drawable.prev_track_orange
                is Colors.OrangeNight -> R.drawable.prev_track_orange
                is Colors.Pink -> R.drawable.prev_track_pink
                is Colors.PinkNight -> R.drawable.prev_track_pink
                is Colors.Purple -> R.drawable.prev_track_purple
                is Colors.PurpleNight -> R.drawable.prev_track_purple
                is Colors.Red -> R.drawable.prev_track_red
                is Colors.RedNight -> R.drawable.prev_track_red
                is Colors.Sea -> R.drawable.prev_track_sea
                is Colors.SeaNight -> R.drawable.prev_track_sea
                is Colors.Turquoise -> R.drawable.prev_track_turquoise
                is Colors.TurquoiseNight -> R.drawable.prev_track_turquoise
                else -> R.drawable.prev_track
            }

        @JvmStatic
        internal inline val playlistButtonImage
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

        @JvmStatic
        internal inline val lyricsButtonImage
            get() = when (Params.instance.theme) {
                is Colors.Blue -> R.drawable.text_blue
                is Colors.BlueNight -> R.drawable.text_blue
                is Colors.Green -> R.drawable.text_green
                is Colors.GreenNight -> R.drawable.text_green
                is Colors.GreenTurquoise -> R.drawable.text_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.text_green_turquoise
                is Colors.Lemon -> R.drawable.text_lemon
                is Colors.LemonNight -> R.drawable.text_lemon
                is Colors.Orange -> R.drawable.text_orange
                is Colors.OrangeNight -> R.drawable.text_orange
                is Colors.Pink -> R.drawable.text_pink
                is Colors.PinkNight -> R.drawable.text_pink
                is Colors.Purple -> R.drawable.text_purple
                is Colors.PurpleNight -> R.drawable.text_purple
                is Colors.Red -> R.drawable.text_red
                is Colors.RedNight -> R.drawable.text_red
                is Colors.Sea -> R.drawable.text_sea
                is Colors.SeaNight -> R.drawable.text_sea
                is Colors.Turquoise -> R.drawable.text_turquoise
                is Colors.TurquoiseNight -> R.drawable.text_turquoise
                else -> R.drawable.text
            }

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

        @JvmStatic
        internal inline val recommendationsMenuImage
            get() = when (Params.instance.theme) {
                is Colors.Blue -> R.drawable.recommendation_blue
                is Colors.BlueNight -> R.drawable.recommendation_blue
                is Colors.Green -> R.drawable.recommendation_green
                is Colors.GreenNight -> R.drawable.recommendation_green
                is Colors.GreenTurquoise -> R.drawable.recommendation_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.recommendation_green_turquoise
                is Colors.Lemon -> R.drawable.recommendation_lemon
                is Colors.LemonNight -> R.drawable.recommendation_lemon
                is Colors.Orange -> R.drawable.recommendation_orange
                is Colors.OrangeNight -> R.drawable.recommendation_orange
                is Colors.Pink -> R.drawable.recommendation_pink
                is Colors.PinkNight -> R.drawable.recommendation_pink
                is Colors.Purple -> R.drawable.recommendation_purple
                is Colors.PurpleNight -> R.drawable.recommendation_purple
                is Colors.Red -> R.drawable.recommendation_red
                is Colors.RedNight -> R.drawable.recommendation_red
                is Colors.Sea -> R.drawable.recommendation_sea
                is Colors.SeaNight -> R.drawable.recommendation_sea
                is Colors.Turquoise -> R.drawable.recommendation_turquoise
                is Colors.TurquoiseNight -> R.drawable.recommendation_turquoise
                else -> R.drawable.recommendation
            }

        @JvmStatic
        internal inline val compilationMenuImage
            get() = when (Params.instance.theme) {
                is Colors.Blue -> R.drawable.compilation_blue
                is Colors.BlueNight -> R.drawable.compilation_blue
                is Colors.Green -> R.drawable.compilation_green
                is Colors.GreenNight -> R.drawable.compilation_green
                is Colors.GreenTurquoise -> R.drawable.compilation_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.compilation_green_turquoise
                is Colors.Lemon -> R.drawable.compilation_lemon
                is Colors.LemonNight -> R.drawable.compilation_lemon
                is Colors.Orange -> R.drawable.compilation_orange
                is Colors.OrangeNight -> R.drawable.compilation_orange
                is Colors.Pink -> R.drawable.compilation_pink
                is Colors.PinkNight -> R.drawable.compilation_pink
                is Colors.Purple -> R.drawable.compilation_purple
                is Colors.PurpleNight -> R.drawable.compilation_purple
                is Colors.Red -> R.drawable.compilation_red
                is Colors.RedNight -> R.drawable.compilation_red
                is Colors.Sea -> R.drawable.compilation_sea
                is Colors.SeaNight -> R.drawable.compilation_sea
                is Colors.Turquoise -> R.drawable.compilation_turquoise
                is Colors.TurquoiseNight -> R.drawable.compilation_turquoise
                else -> R.drawable.compilation
            }

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

        @JvmStatic
        internal fun getLikeButtonImage(like: Boolean) = when {
            like -> R.drawable.heart_like
            else -> when (Params.instance.theme) {
                is Colors.Blue -> R.drawable.heart_blue
                is Colors.BlueNight -> R.drawable.heart_blue
                is Colors.Green -> R.drawable.heart_green
                is Colors.GreenNight -> R.drawable.heart_green
                is Colors.GreenTurquoise -> R.drawable.heart_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.heart_green_turquoise
                is Colors.Lemon -> R.drawable.heart_lemon
                is Colors.LemonNight -> R.drawable.heart_lemon
                is Colors.Orange -> R.drawable.heart_orange
                is Colors.OrangeNight -> R.drawable.heart_orange
                is Colors.Pink -> R.drawable.heart_pink
                is Colors.PinkNight -> R.drawable.heart_pink
                is Colors.Purple -> R.drawable.heart_purple
                is Colors.PurpleNight -> R.drawable.heart_purple
                is Colors.Red -> R.drawable.heart_red
                is Colors.RedNight -> R.drawable.heart_red
                is Colors.Sea -> R.drawable.heart_sea
                is Colors.SeaNight -> R.drawable.heart_sea
                is Colors.Turquoise -> R.drawable.heart_turquoise
                is Colors.TurquoiseNight -> R.drawable.heart_turquoise
                else -> R.drawable.heart
            }
        }

        @JvmStatic
        internal fun getRepeatButtonImage(repeat1: Boolean) = when {
            repeat1 -> when (Params.instance.theme) {
                is Colors.Blue -> R.drawable.repeat1_blue
                is Colors.BlueNight -> R.drawable.repeat1_blue
                is Colors.Green -> R.drawable.repeat1_green
                is Colors.GreenNight -> R.drawable.repeat1_green
                is Colors.GreenTurquoise -> R.drawable.repeat1_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.repeat1_green_turquoise
                is Colors.Lemon -> R.drawable.repeat1_lemon
                is Colors.LemonNight -> R.drawable.repeat1_lemon
                is Colors.Orange -> R.drawable.repeat1_orange
                is Colors.OrangeNight -> R.drawable.repeat1_orange
                is Colors.Pink -> R.drawable.repeat1_pink
                is Colors.PinkNight -> R.drawable.repeat1_pink
                is Colors.Purple -> R.drawable.repeat1_purple
                is Colors.PurpleNight -> R.drawable.repeat1_purple
                is Colors.Red -> R.drawable.repeat1_red
                is Colors.RedNight -> R.drawable.repeat1_red
                is Colors.Sea -> R.drawable.repeat1_sea
                is Colors.SeaNight -> R.drawable.repeat1_sea
                is Colors.Turquoise -> R.drawable.repeat1_turquoise
                is Colors.TurquoiseNight -> R.drawable.repeat1_turquoise
                else -> R.drawable.repeat_1
            }

            else -> when (Params.instance.theme) {
                is Colors.Blue -> R.drawable.repeat_blue
                is Colors.BlueNight -> R.drawable.repeat_blue
                is Colors.Green -> R.drawable.repeat_green
                is Colors.GreenNight -> R.drawable.repeat_green
                is Colors.GreenTurquoise -> R.drawable.repeat_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.repeat_green_turquoise
                is Colors.Lemon -> R.drawable.repeat_lemon
                is Colors.LemonNight -> R.drawable.repeat_lemon
                is Colors.Orange -> R.drawable.repeat_orange
                is Colors.OrangeNight -> R.drawable.repeat_orange
                is Colors.Pink -> R.drawable.repeat_pink
                is Colors.PinkNight -> R.drawable.repeat_pink
                is Colors.Purple -> R.drawable.repeat_purple
                is Colors.PurpleNight -> R.drawable.repeat_purple
                is Colors.Red -> R.drawable.repeat_red
                is Colors.RedNight -> R.drawable.repeat_red
                is Colors.Sea -> R.drawable.repeat_sea
                is Colors.SeaNight -> R.drawable.repeat_sea
                is Colors.Turquoise -> R.drawable.repeat_turquoise
                is Colors.TurquoiseNight -> R.drawable.repeat_turquoise
                else -> R.drawable.repeat
            }
        }

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
    }
}