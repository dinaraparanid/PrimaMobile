package com.dinaraparanid.prima.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.dinaraparanid.prima.utils.polymorphism.Playlist
import com.dinaraparanid.prima.utils.polymorphism.TrackListSearchFragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Manipulates [SharedPreferences] data for app
 */

internal class StorageUtil(private val context: Context) {
    private companion object {
        private const val STORAGE = "com.dinaraparanid.prima.STORAGE"
        private const val TRACK_LIST_KEY = "track_list"
        private const val TRACK_PATH_KEY = "track_path"
        private const val PAUSE_TIME_KEY = "pause_time"
        private const val LOOPING_STATUS_KEY = "looping_status"
        private const val CURRENT_PLAYLIST_KEY = "current_p"
        private const val LANGUAGE_KEY = "language"
        private const val THEME_KEY = "theme"
        private const val ROUNDED_PLAYLIST_KEY = "round"
        private const val FONT_KEY = "font"
        private const val EQUALIZER_SEEKBARS_POS_KEY = "seekbar_pos"
        private const val EQUALIZER_PRESET_POS_KEY = "preset_pos"
        private const val EQUALIZER_REVERB_PRESET = "reverb_preset"
        private const val EQUALIZER_BASS_STRENGTH = "bass_strength"
        private const val PITCH_KEY = "pitch"
        private const val SPEED_KEY = "speed"
        private const val SHOW_PLAYLISTS_IMAGES_KEY = "show_playlists_images"
        private const val SHOW_AUDIO_VISUALIZER_KEY = "show_audio_visualizer"
        private const val SAVE_CUR_TRACK_PLAYLIST_KEY = "save_cur_track_playlist"
        private const val SAVE_LOOPING_KEY = "save_looping"
        private const val SAVE_EQUALIZER_SETTINGS_KEY = "save_equalizer"
        private const val TRACKS_ORDER_KEY = "tracks_order_key"
        private const val TRACKS_SEARCH_ORDER_KEY = "tracks_search_order"
        private const val HAPPI_API_KEY = "happi_api_key"
        private const val CUSTOM_THEME_COLORS_KEY = "custom_theme_colors"
        private const val BACKGROUND_IMAGE_KEY = "background_image_key"
        private const val BLOOM_KEY = "bloom"
        private const val START_WITH_EQUALIZER_KEY = "start_with_equalizer"
        private const val USE_ANDROID_NOTIFICATION_KEY = "use_android_notification"

        @Deprecated("Now updating metadata in files (Android 11+)")
        private const val CHANGED_TRACKS_KEY = "changed_tracks"
    }

    private var preferences: SharedPreferences? = null

    /**
     * Saves tracks in [SharedPreferences]
     * @param trackList track list to save
     */

    @Deprecated("Current playlist saved in MainApplication")
    internal fun storeTracks(trackList: List<Track?>?) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putString(TRACK_LIST_KEY, Gson().toJson(trackList))
            apply()
        }

    /**
     * Loads tracks from [SharedPreferences]
     * @return loaded tracks
     */

    @Deprecated("Current playlist saved in MainApplication")
    internal fun loadTracks(): List<Track> = Gson().fromJson(
        context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
            .getString(TRACK_LIST_KEY, null),
        object : TypeToken<List<Track?>?>() {}.type
    )

    /**
     * Saves current track's path in [SharedPreferences]
     * @param path path to track (DATA column from MediaStore)
     */

    internal fun storeTrackPath(path: String) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putString(TRACK_PATH_KEY, path)
            apply()
        }

    /**
     * Loads current track's path from [SharedPreferences]
     * @return current track's path or [com.dinaraparanid.prima.MainActivity.NO_PATH]
     */

    internal fun loadTrackPath() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getString(TRACK_PATH_KEY, "_____ЫЫЫЫЫЫЫЫ_____")!!

    /**
     * Saves current track's pause time in [SharedPreferences]
     * @param pause pause time
     */

    internal fun storeTrackPauseTime(pause: Int) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putInt(PAUSE_TIME_KEY, pause)
            apply()
        }

    /**
     * Loads current track's pause time from [SharedPreferences]
     * @return current track's pause time or -1 if it wasn't saved
     */

    internal fun loadTrackPauseTime() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getInt(PAUSE_TIME_KEY, -1)

    /**
     * Saves looping in [SharedPreferences]
     * @param loopingStatus [Params.Companion.Looping] when playing track
     */

    internal fun storeLooping(loopingStatus: Params.Companion.Looping) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putInt(LOOPING_STATUS_KEY, loopingStatus.ordinal)
            apply()
        }

    /**
     * Loads looping from [SharedPreferences]
     * @return looping when playing track or [Params.Companion.Looping.PLAYLIST] if it wasn't saved
     */

    internal fun loadLooping() = Params.Companion.Looping.values()[
            context
                .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
                .getInt(LOOPING_STATUS_KEY, 0)
    ]

    /**
     * Saves current playlist in [SharedPreferences]
     * @param curPlaylist current playlist to save
     */

    internal fun storeCurPlaylist(curPlaylist: Playlist) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putString(CURRENT_PLAYLIST_KEY, Gson().toJson(curPlaylist))
            apply()
        }

    /**
     * Loads current playlist from [SharedPreferences]
     * @return current playlist or null if it wasn't save or even created
     */

    internal fun loadCurPlaylist() = Gson().fromJson<List<Track>>(
        context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
            .getString(CURRENT_PLAYLIST_KEY, null),
        object : TypeToken<List<Track?>?>() {}.type
    )?.toPlaylist()

    /**
     * Saves current playlist in [SharedPreferences] (Android 11+)
     * @param changedTracks dictionary with changed tracks
     */

    @Deprecated("Now updating metadata in files (Android 11+)")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun storeChangedTracks(changedTracks: MutableMap<String, Track>) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putString(CHANGED_TRACKS_KEY, Gson().toJson(changedTracks))
            apply()
        }

    /**
     * Loads current playlist from [SharedPreferences] (Android 11+)
     * @return dictionary with changed tracks or null if it wasn't save or even created
     */

    @Deprecated("Now updating metadata in files (Android 11+)")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun loadChangedTracks(): MutableMap<String, Track>? = Gson().fromJson(
        context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
            .getString(CHANGED_TRACKS_KEY, null),
        object : TypeToken<MutableMap<String, Track>?>() {}.type
    )

    /**
     * Saves current language in [SharedPreferences]
     * @param language [Params.Companion.Language] to save
     */

    internal fun storeLanguage(language: Params.Companion.Language) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putInt(LANGUAGE_KEY, language.ordinal)
            apply()
        }

    /**
     * Loads current language from [SharedPreferences]
     * @return [Params.Companion.Language] that was chosen before
     * or [Params.Companion.Language.EN] as a default language if it wasn't
     */

    internal fun loadLanguage() = Params.Companion.Language.values().getOrNull(
        context
            .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
            .getInt(LANGUAGE_KEY, -1)
    )

    /**
     * Saves current theme in [SharedPreferences]
     * @param theme number of theme to save
     * @see Params.chooseTheme
     */

    internal fun storeTheme(theme: Int) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putInt(THEME_KEY, theme)
            apply()
        }

    /**
     * Loads theme from [SharedPreferences]
     * @return [Colors] of theme that was chosen before
     * or [Colors.PurpleNight] as a default theme if it wasn't
     */

    internal fun loadTheme() = Params.chooseTheme(
        context
            .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
            .getInt(THEME_KEY, 1)
    )

    /**
     * Saves flag about rounding playlists' images in [SharedPreferences]
     * @param isRounded rounding playlists' images flag to save
     */

    internal fun storeRounded(isRounded: Boolean) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putBoolean(ROUNDED_PLAYLIST_KEY, isRounded)
            apply()
        }

    /**
     * Loads flag about rounding playlists' images from [SharedPreferences]
     * @return saving rounding playlists' images flag or true if it's wasn't saved
     */

    internal fun loadRounded() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getBoolean(ROUNDED_PLAYLIST_KEY, true)

    /**
     * Saves font title in [SharedPreferences]
     * @param font font title to save
     */

    internal fun storeFont(font: String) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putString(FONT_KEY, font)
            apply()
        }

    /**
     * Loads font title from [SharedPreferences]
     * @return font title
     */

    internal fun loadFont() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getString(FONT_KEY, "Sans Serif")!!

    /**
     * Saves Equalizer's seekbars positions in [SharedPreferences]
     * @param seekbarPos seekbars positions to save
     */

    internal fun storeEqualizerSeekbarsPos(seekbarPos: IntArray) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putString(EQUALIZER_SEEKBARS_POS_KEY, Gson().toJson(seekbarPos))
            apply()
        }

    /**
     * Loads Equalizer's seekbars positions from [SharedPreferences]
     * @return font seekbars positions as int array
     */

    internal fun loadEqualizerSeekbarsPos(): IntArray? = Gson().fromJson(
        context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
            .getString(EQUALIZER_SEEKBARS_POS_KEY, null),
        object : TypeToken<IntArray?>() {}.type
    )

    /**
     * Loads Equalizer's preset position from [SharedPreferences]
     * @return preset position or 0 if it's wasn't saved
     */

    internal fun loadPresetPos() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getInt(EQUALIZER_PRESET_POS_KEY, 0)

    /**
     * Saves Equalizer's preset position in [SharedPreferences]
     * @param presetPos preset position to save
     */

    internal fun storePresetPos(presetPos: Int) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putInt(EQUALIZER_PRESET_POS_KEY, presetPos)
            apply()
        }

    /**
     * Loads Equalizer's reverb preset from [SharedPreferences]
     * @return reverb preset or -1 if it's wasn't saved
     */

    internal fun loadReverbPreset() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getInt(EQUALIZER_REVERB_PRESET, -1)
        .toShort()

    /**
     * Saves Equalizer's reverb preset in [SharedPreferences]
     * @param reverbPreset reverb preset to save
     */

    internal fun storeReverbPreset(reverbPreset: Short) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putInt(EQUALIZER_REVERB_PRESET, reverbPreset.toInt())
            apply()
        }

    /**
     * Loads Equalizer's bass strength from [SharedPreferences]
     * @return bass strength or -1 if it's wasn't saved
     */

    internal fun loadBassStrength() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getInt(EQUALIZER_BASS_STRENGTH, -1)
        .toShort()

    /**
     * Saves Equalizer's bass strength in [SharedPreferences]
     * @param bassStrength bass strength to save
     */

    internal fun storeBassStrength(bassStrength: Short) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putInt(EQUALIZER_BASS_STRENGTH, bassStrength.toInt())
            apply()
        }

    /**
     * Loads audio pitch from [SharedPreferences]
     * @return audio pitch or 1 if it's wasn't saved
     */

    internal fun loadPitch() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getFloat(PITCH_KEY, 1F)

    /**
     * Saves audio pitch in [SharedPreferences]
     * @param pitch audio pitch to save
     */

    internal fun storePitch(pitch: Float) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putFloat(PITCH_KEY, pitch)
            apply()
        }

    /**
     * Loads audio speed from [SharedPreferences]
     * @return audio speed or 1 if it's wasn't saved
     */

    internal fun loadSpeed() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getFloat(SPEED_KEY, 1F)

    /**
     * Saves audio speed in [SharedPreferences]
     * @param speed audio speed to save
     */

    internal fun storeSpeed(speed: Float) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putFloat(SPEED_KEY, speed)
            apply()
        }

    /**
     * Loads show playlists' images flag from [SharedPreferences]
     * @return show playlists' images flag or true if it's wasn't saved
     */

    internal fun loadShowPlaylistsImages() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getBoolean(SHOW_PLAYLISTS_IMAGES_KEY, true)

    /**
     * Saves show playlists' images flag in [SharedPreferences]
     * @param showPlaylistsImages show playlists' images flag to save
     */

    internal fun storeShowPlaylistsImages(showPlaylistsImages: Boolean) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putBoolean(SHOW_PLAYLISTS_IMAGES_KEY, showPlaylistsImages)
            apply()
        }

    /**
     * Loads show audio visualizer flag from [SharedPreferences]
     * @return show audio visualizer or true if it's wasn't saved
     */

    internal fun loadShowVisualizer() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getBoolean(SHOW_AUDIO_VISUALIZER_KEY, true)

    /**
     * Saves show audio visualizer flag in [SharedPreferences]
     * @param showVisualizer show audio visualizer flag to save
     */

    internal fun storeShowVisualizer(showVisualizer: Boolean) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putBoolean(SHOW_AUDIO_VISUALIZER_KEY, showVisualizer)
            apply()
        }

    /**
     * Loads save cur track and playlist flag from [SharedPreferences]
     * @return save cur track and playlist flag or true if it's wasn't saved
     */

    internal fun loadSaveCurTrackAndPlaylist() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getBoolean(SAVE_CUR_TRACK_PLAYLIST_KEY, true)

    /**
     * Saves save cur track and playlist flag in [SharedPreferences]
     * @param saveCurTrackAndPlaylist save cur track and playlist flag to save
     */

    internal fun storeSaveCurTrackAndPlaylist(saveCurTrackAndPlaylist: Boolean) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putBoolean(SAVE_CUR_TRACK_PLAYLIST_KEY, saveCurTrackAndPlaylist)
            apply()
        }

    /**
     * Loads save looping flag from [SharedPreferences]
     * @return save looping flag or true if it's wasn't saved
     */

    internal fun loadSaveLooping() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getBoolean(SAVE_LOOPING_KEY, true)

    /**
     * Saves save looping flag in [SharedPreferences]
     * @param saveLooping save looping flag to save
     */

    internal fun storeSaveLooping(saveLooping: Boolean) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putBoolean(SAVE_LOOPING_KEY, saveLooping)
            apply()
        }

    /**
     * Loads save equalizer's settings flag from [SharedPreferences]
     * @return save equalizer's settings flag or true if it's wasn't saved
     */

    internal fun loadSaveEqualizerSettings() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getBoolean(SAVE_EQUALIZER_SETTINGS_KEY, true)

    /**
     * Saves save equalizer's settings flag in [SharedPreferences]
     * @param saveEqualizerSettings save equalizer's settings flag to save
     */

    internal fun storeSaveEqualizerSettings(saveEqualizerSettings: Boolean) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putBoolean(SAVE_EQUALIZER_SETTINGS_KEY, saveEqualizerSettings)
            apply()
        }

    /**
     * Loads track order from [SharedPreferences]
     * @return track order or (TITLE, true) if it's wasn't saved
     */

    internal fun loadTrackOrder() = Gson()
        .fromJson<Pair<Int, Boolean>?>(
            context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
                .getString(TRACKS_ORDER_KEY, null),
            object : TypeToken<Pair<Int, Boolean>?>() {}.type
        )
        ?.let { (ord, isAsc) -> Params.Companion.TracksOrder.values()[ord] to isAsc }

    /**
     * Saves track order in [SharedPreferences]
     * @param trackOrder track order to save
     */

    internal fun storeTrackOrder(trackOrder: Pair<Params.Companion.TracksOrder, Boolean>) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putString(
                TRACKS_ORDER_KEY,
                Gson().toJson(trackOrder.let { (ord, isAsc) ->
                    ord.ordinal to isAsc
                })
            )
            apply()
        }

    /**
     * Loads tracks search order from [SharedPreferences]
     * @return tracks search order or everything if it's wasn't saved
     */

    internal fun loadTrackSearchOrder() = Gson().fromJson<IntArray?>(
        context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
            .getString(TRACKS_SEARCH_ORDER_KEY, null),
        object : TypeToken<IntArray?>() {}.type
    )?.map(TrackListSearchFragment.SearchOrder.values()::get)

    /**
     * Saves tracks search order in [SharedPreferences]
     * @param trackSearchOrder tracks search order to save
     */

    internal fun storeTrackSearchOrder(trackSearchOrder: List<TrackListSearchFragment.SearchOrder>) =
        context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putString(
                TRACKS_SEARCH_ORDER_KEY,
                Gson().toJson(trackSearchOrder.map(TrackListSearchFragment.SearchOrder::ordinal))
            )
            apply()
        }

    /**
     * Loads happi api key from [SharedPreferences]
     * @return happi api key or null if it's wasn't saved
     */

    internal fun loadHappiApiKey() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getString(HAPPI_API_KEY, null)

    /**
     * Saves happi api key in [SharedPreferences]
     * @param happiApiKey happi api key to save
     */

    internal fun storeHappiApiKey(happiApiKey: String) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putString(HAPPI_API_KEY, happiApiKey)
            apply()
        }

    /**
     * Loads custom theme's colors from [SharedPreferences]
     * @return custom theme colors or null if it's wasn't saved
     */

    internal fun loadCustomThemeColors(): Pair<Int, Int>? = Gson().fromJson<Pair<Int, Int>?>(
        context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
            .getString(CUSTOM_THEME_COLORS_KEY, null),
        object : TypeToken<Pair<Int, Int>?>() {}.type
    )

    /**
     * Saves custom theme's colors in [SharedPreferences]
     * @param customThemeColors custom theme colors to save
     */

    internal fun storeCustomThemeColors(customThemeColors: Pair<Int, Int>) =
        context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putString(CUSTOM_THEME_COLORS_KEY, Gson().toJson(customThemeColors))
            apply()
        }

    /**
     * Loads app's background image from [SharedPreferences]
     * @return app's background image or null if it's wasn't saved
     */

    internal fun loadBackgroundImage(): ByteArray? = Gson().fromJson(
        context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
            .getString(BACKGROUND_IMAGE_KEY, null),
        object : TypeToken<ByteArray?>() {}.type
    )

    /**
     * Saves app's background image in [SharedPreferences]
     * @param background [ByteArray] of app's background image
     */

    internal fun storeBackgroundImage(background: ByteArray) =
        context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putString(BACKGROUND_IMAGE_KEY, Gson().toJson(background))
            apply()
        }

    /**
     * Loads is bloom enabled flag from [SharedPreferences]
     * @return is bloom enabled flag or true if it's wasn't saved
     */

    internal fun loadBloom() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getBoolean(BLOOM_KEY, true)

    /**
     * Saves is blooming flag in [SharedPreferences]
     * @param isBloomEnabled flag to save
     */

    internal fun storeBloom(isBloomEnabled: Boolean) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putBoolean(BLOOM_KEY, isBloomEnabled)
            apply()
        }

    /**
     * Loads is starting with equalizer flag from [SharedPreferences]
     * @return is starting with equalizer flag or false if it's wasn't saved
     */

    internal fun loadStartWithEqualizer() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getBoolean(START_WITH_EQUALIZER_KEY, false)

    /**
     * Saves is starting with equalizer flag in [SharedPreferences]
     * @param isStartingWithEqualizer flag to save
     */

    internal fun storeStartWithEqualizer(isStartingWithEqualizer: Boolean) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putBoolean(START_WITH_EQUALIZER_KEY, isStartingWithEqualizer)
            apply()
        }

    /**
     * Loads use android notification flag from [SharedPreferences]
     * @return use android notification flag or false if it's wasn't saved
     */

    @RequiresApi(Build.VERSION_CODES.P)
    internal fun loadUseAndroidNotification() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getBoolean(USE_ANDROID_NOTIFICATION_KEY, false)

    /**
     * Saves use android notification flag in [SharedPreferences]
     * @param useAndroidNotification flag to save
     */

    @RequiresApi(Build.VERSION_CODES.P)
    internal fun storeUseAndroidNotification(useAndroidNotification: Boolean) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putBoolean(USE_ANDROID_NOTIFICATION_KEY, useAndroidNotification)
            apply()
        }

    /**
     * Clears playlist data in [SharedPreferences]
     */

    internal fun clearCachedPlaylist() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)
        preferences!!.edit().apply {
            remove(TRACK_LIST_KEY)
            apply()
        }
    }

    /**
     * Clears tracks progress (cur track, playlist) in [SharedPreferences]
     */

    internal fun clearPlayingProgress() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)
        preferences!!.edit().apply {
            remove(TRACK_PATH_KEY)
            remove(PAUSE_TIME_KEY)
            remove(CURRENT_PLAYLIST_KEY)
            apply()
        }
    }

    /**
     * Clears looping status in [SharedPreferences]
     */

    internal fun clearLooping() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)
        preferences!!.edit().apply {
            remove(LOOPING_STATUS_KEY)
            apply()
        }
    }

    /**
     * Clears equalizer progress in [SharedPreferences]
     */

    internal fun clearEqualizerProgress() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)
        preferences!!.edit().apply {
            remove(EQUALIZER_SEEKBARS_POS_KEY)
            remove(EQUALIZER_PRESET_POS_KEY)
            remove(EQUALIZER_BASS_STRENGTH)
            remove(EQUALIZER_REVERB_PRESET)
            remove(PITCH_KEY)
            remove(SPEED_KEY)
            apply()
        }
    }

    /**
     * Clears custom theme's colors in [SharedPreferences]
     */

    internal fun clearCustomThemeColors() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)
        preferences!!.edit().apply {
            remove(CUSTOM_THEME_COLORS_KEY)
            apply()
        }
    }

    /**
     * Clears app's background picture in [SharedPreferences]
     */

    internal fun clearBackgroundImage() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)
        preferences!!.edit().apply {
            remove(BACKGROUND_IMAGE_KEY)
            apply()
        }
    }
}