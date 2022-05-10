package com.dinaraparanid.prima.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.core.DefaultTrack
import com.dinaraparanid.prima.utils.equalizer.EqualizerSettings
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.fragments.TrackListSearchFragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.ref.WeakReference

/** Manipulates with [SharedPreferences] data */

internal class StorageUtil private constructor(private val _context: WeakReference<Context>) {
    internal companion object {
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
        private const val HIDE_COVER = "hide_cover"
        private const val SHOW_AUDIO_VISUALIZER_KEY = "show_audio_visualizer"
        private const val SAVE_CUR_TRACK_PLAYLIST_KEY = "save_cur_track_playlist"
        private const val SAVE_LOOPING_KEY = "save_looping"
        private const val SAVE_EQUALIZER_SETTINGS_KEY = "save_equalizer"
        private const val TRACKS_ORDER_KEY = "tracks_order_key"
        private const val TRACKS_SEARCH_ORDER_KEY = "tracks_search_order"
        private const val CUSTOM_THEME_COLORS_KEY = "custom_theme_colors"
        private const val BACKGROUND_IMAGE_KEY = "background_image_key"
        private const val BLOOM_KEY = "bloom"
        private const val START_WITH_EQUALIZER_KEY = "start_with_equalizer"
        private const val USE_ANDROID_NOTIFICATION_KEY = "use_android_notification"
        private const val VISUALIZER_STYLE_KEY = "visualizer_style"
        private const val HOME_SCREEN_KEY = "home_screen_key"
        private const val PATH_TO_SAVE_KEY = "path_to_save"
        private const val BLUR_ON_BACKGROUND_KEY = "blur_on_background"
        private const val DISPLAY_COVERS_KEY = "display_covers"
        private const val ROTATE_COVER_KEY = "rotate_cover"
        private const val STATISTICS_KEY = "statistics"
        private const val STATISTICS_DAILY_KEY = "statistics_daily"
        private const val STATISTICS_WEEKLY_KEY = "statistics_weekly"
        private const val STATISTICS_MONTHLY_KEY = "statistics_monthly"
        private const val STATISTICS_YEARLY_KEY = "statistics_yearly"
        private const val HIDDEN_PASSWORD_KEY = "hidden_password"

        @Deprecated("Switched to Genius API")
        private const val HAPPI_API_KEY = "happi_api_key"

        @Deprecated("Now updating metadata in files (Android 11+)")
        private const val CHANGED_TRACKS_KEY = "changed_tracks"

        @JvmStatic
        private var INSTANCE: StorageUtil? = null

        @JvmStatic
        private val mutex = Mutex()

        @JvmStatic
        internal fun initialize(context: Context) {
            INSTANCE = StorageUtil(WeakReference(context))
            RefreshWorkerLauncher.launchWorkers(context.applicationContext)
            EqualizerSettings.initialize()
        }

        /** Gets instance with [Mutex]'s protection */

        @JvmStatic
        internal suspend fun getInstanceSynchronized() = mutex.withLock { instance }

        /** Gets instance without any protection */

        @JvmStatic
        internal val instance: StorageUtil
            get() = INSTANCE
                ?: throw UninitializedPropertyAccessException("StorageUtil is not initialized")

        /** Runs tasks in scope with mutex's protection */

        @JvmStatic
        internal suspend inline fun runSynchronized(actions: StorageUtil.() -> Unit) =
            mutex.withLock { actions(instance) }
    }

    private inline val context
        get() = _context.unchecked

    private inline val preferences
        get() = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!

    /**
     * Saves tracks in [SharedPreferences]
     * @param trackList track list to save
     */

    @Deprecated("Current playlist saved in MainApplication")
    internal suspend fun storeTracksLocking(trackList: List<AbstractTrack?>?) = mutex.withLock {
        preferences.edit().run {
            putString(TRACK_LIST_KEY, Gson().toJson(trackList))
            apply()
        }
    }

    /**
     * Loads tracks from [SharedPreferences]
     * @return loaded tracks
     */

    @Deprecated("Current playlist saved in MainApplication")
    internal suspend fun loadTracksLocking(): List<AbstractTrack> = mutex.withLock {
        Gson().fromJson(
            preferences.getString(TRACK_LIST_KEY, null),
            object : TypeToken<ArrayList<AbstractTrack?>?>() {}.type
        )
    }

    /**
     * Saves current track's path in [SharedPreferences]
     * @param path path to track (DATA column from MediaStore)
     */

    internal suspend fun storeTrackPathLocking(path: String) = mutex.withLock {
        preferences.edit().run {
            putString(TRACK_PATH_KEY, path)
            apply()
        }
    }

    /**
     * Loads current track's path from [SharedPreferences]
     * @return current track's path or [Params.NO_PATH]
     */

    internal suspend fun loadTrackPathLocking() =
        mutex.withLock { preferences.getString(TRACK_PATH_KEY, Params.NO_PATH)!! }

    /**
     * Saves current track's pause time in [SharedPreferences]
     * @param pause pause time
     */

    internal suspend fun storeTrackPauseTimeLocking(pause: Int) = mutex.withLock {
        preferences.edit().run {
            putInt(PAUSE_TIME_KEY, pause)
            apply()
        }
    }

    /**
     * Loads current track's pause time from [SharedPreferences]
     * @return current track's pause time or -1 if it wasn't saved
     */

    internal suspend fun loadTrackPauseTimeLocking() =
        mutex.withLock { preferences.getInt(PAUSE_TIME_KEY, -1) }

    /**
     * Saves looping in [SharedPreferences]
     * @param loopingStatus [Params.Companion.Looping] when playing track
     */

    internal suspend fun storeLoopingLocking(loopingStatus: Params.Companion.Looping) =
        mutex.withLock {
            preferences.edit().run {
                putInt(LOOPING_STATUS_KEY, loopingStatus.ordinal)
                apply()
            }
        }

    /**
     * Loads looping from [SharedPreferences]
     * @return looping when playing track or [Params.Companion.Looping.PLAYLIST] if it wasn't saved
     */

    internal fun loadLooping() =
        Params.Companion.Looping.values()[preferences.getInt(LOOPING_STATUS_KEY, 0)]

    /**
     * Saves current playlist in [SharedPreferences]
     * @param curPlaylist current playlist to save
     */

    internal suspend fun storeCurPlaylistLocking(curPlaylist: AbstractPlaylist) = mutex.withLock {
        preferences.edit().run {
            putString(CURRENT_PLAYLIST_KEY, Gson().toJson(curPlaylist))
            apply()
        }
    }

    /**
     * Loads current playlist from [SharedPreferences]
     * @return current playlist or null if it wasn't save or even created
     */

    internal fun loadCurPlaylist() = Gson().fromJson<List<AbstractTrack>>(
        context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
            .getString(CURRENT_PLAYLIST_KEY, null),
        object : TypeToken<ArrayList<DefaultTrack?>?>() {}.type
    )?.toPlaylist()

    /**
     * Saves current playlist in [SharedPreferences] (Android 11+)
     * @param changedTracks dictionary with changed tracks
     */

    @Deprecated("Now updating metadata in files (Android 11+)")
    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun storeChangedTracksLocking(changedTracks: MutableMap<String, AbstractTrack>) =
        mutex.withLock {
            preferences.edit().run {
                putString(CHANGED_TRACKS_KEY, Gson().toJson(changedTracks))
                apply()
            }
        }

    /**
     * Loads current playlist from [SharedPreferences] (Android 11+)
     * @return dictionary with changed tracks or null if it wasn't save or even created
     */

    @Deprecated("Now updating metadata in files (Android 11+)")
    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun loadChangedTracksLocking(): MutableMap<String, AbstractTrack>? =
        mutex.withLock {
            Gson().fromJson(
                preferences.getString(CHANGED_TRACKS_KEY, null),
                object : TypeToken<HashMap<String, AbstractTrack>?>() {}.type
            )
        }

    /**
     * Saves current language in [SharedPreferences]
     * @param language [Params.Companion.Language] to save
     */

    internal fun storeLanguage(language: Params.Companion.Language) = preferences.edit().run {
        putInt(LANGUAGE_KEY, language.ordinal)
        apply()
    }

    /**
     * Loads current language from [SharedPreferences]
     * @return [Params.Companion.Language] that was chosen before
     * or [Params.Companion.Language.EN] as a default language if it wasn't
     */

    internal fun loadLanguage() = Params.Companion.Language.values()
        .getOrNull(preferences.getInt(LANGUAGE_KEY, -1))

    /**
     * Saves current theme in [SharedPreferences]
     * @param theme number of theme to save
     * @see Params.chooseTheme
     */

    internal fun storeTheme(theme: Int) = preferences.edit().run {
        putInt(THEME_KEY, theme)
        apply()
    }

    /**
     * Loads theme from [SharedPreferences]
     * @return [Colors] of theme that was chosen before
     * or [Colors.PurpleNight] as a default theme if it wasn't
     */

    internal fun loadTheme() = Params.chooseTheme(preferences.getInt(THEME_KEY, 1))

    /**
     * Saves flag about rounding playlists' images in [SharedPreferences]
     * @param isRounded rounding playlists' images flag to save
     */

    internal fun storeRounded(isRounded: Boolean) = preferences.edit().run {
        putBoolean(ROUNDED_PLAYLIST_KEY, isRounded)
        apply()
    }

    /**
     * Loads flag about rounding playlists' images from [SharedPreferences]
     * @return saving rounding playlists' images flag or true if it's wasn't saved
     */

    internal fun loadRounded() = preferences.getBoolean(ROUNDED_PLAYLIST_KEY, true)

    /**
     * Saves font title in [SharedPreferences]
     * @param font font title to save
     */

    internal fun storeFont(font: String) = preferences.edit().run {
        putString(FONT_KEY, font)
        apply()
    }

    /**
     * Loads font title from [SharedPreferences]
     * @return font title
     */

    internal fun loadFont() = preferences.getString(FONT_KEY, "Sans Serif")!!

    /**
     * Saves Equalizer's seekbars positions in [SharedPreferences]
     * @param seekbarPos seekbars positions to save
     */

    internal suspend fun storeEqualizerSeekbarsPosLocking(seekbarPos: IntArray) = mutex.withLock {
        preferences.edit().run {
            putString(EQUALIZER_SEEKBARS_POS_KEY, Gson().toJson(seekbarPos))
            apply()
        }
    }

    /**
     * Loads Equalizer's seekbars positions from [SharedPreferences]
     * @return font seekbars positions as int array
     */

    internal suspend fun loadEqualizerSeekbarsPosLocking(): IntArray? = mutex.withLock {
        Gson().fromJson(
            preferences.getString(EQUALIZER_SEEKBARS_POS_KEY, null),
            object : TypeToken<IntArray?>() {}.type
        )
    }

    /**
     * Loads Equalizer's preset position from [SharedPreferences]
     * @return preset position or 0 if it's wasn't saved
     */

    internal suspend fun loadPresetPosLocking() =
        mutex.withLock { preferences.getInt(EQUALIZER_PRESET_POS_KEY, 0) }

    /**
     * Saves Equalizer's preset position in [SharedPreferences]
     * @param presetPos preset position to save
     */

    internal suspend fun storePresetPosLocking(presetPos: Int) = mutex.withLock {
        preferences.edit().run {
            putInt(EQUALIZER_PRESET_POS_KEY, presetPos)
            apply()
        }
    }

    /**
     * Loads Equalizer's reverb preset from [SharedPreferences]
     * @return reverb preset or -1 if it's wasn't saved
     */

    internal suspend fun loadReverbPresetLocking() = mutex.withLock {
        preferences
            .getInt(EQUALIZER_REVERB_PRESET, -1)
            .toShort()
    }

    /**
     * Saves Equalizer's reverb preset in [SharedPreferences]
     * @param reverbPreset reverb preset to save
     */

    internal suspend fun storeReverbPresetLocking(reverbPreset: Short) = mutex.withLock {
        preferences.edit().run {
            putInt(EQUALIZER_REVERB_PRESET, reverbPreset.toInt())
            apply()
        }
    }

    /**
     * Loads Equalizer's bass strength from [SharedPreferences]
     * @return bass strength or -1 if it's wasn't saved
     */

    internal suspend fun loadBassStrengthLocking() = mutex.withLock {
        preferences
            .getInt(EQUALIZER_BASS_STRENGTH, -1)
            .toShort()
    }

    /**
     * Saves Equalizer's bass strength in [SharedPreferences]
     * @param bassStrength bass strength to save
     */

    internal suspend fun storeBassStrengthLocking(bassStrength: Short) = mutex.withLock {
        preferences.edit().run {
            putInt(EQUALIZER_BASS_STRENGTH, bassStrength.toInt())
            apply()
        }
    }

    /**
     * Loads audio pitch from [SharedPreferences]
     * @return audio pitch or 1 if it's wasn't saved
     */

    internal suspend fun loadPitchAsyncLocking() =
        mutex.withLock { preferences.getFloat(PITCH_KEY, 1F) }

    /**
     * Saves audio pitch in [SharedPreferences]
     * @param pitch audio pitch to save
     */

    internal suspend fun storePitchLocking(pitch: Float) = mutex.withLock {
        preferences.edit().run {
            putFloat(PITCH_KEY, pitch)
            apply()
        }
    }

    /**
     * Loads audio speed from [SharedPreferences]
     * @return audio speed or 1 if it's wasn't saved
     */

    internal suspend fun loadSpeedAsyncLocking() =
        mutex.withLock { preferences.getFloat(SPEED_KEY, 1F) }

    /**
     * Saves audio speed in [SharedPreferences]
     * @param speed audio speed to save
     */

    internal suspend fun storeSpeedLocking(speed: Float) = mutex.withLock {
        preferences.edit().run {
            putFloat(SPEED_KEY, speed)
            apply()
        }
    }

    /**
     * Loads hide track's cover on playing panel flag from [SharedPreferences]
     * @return hide track's cover on playing panel flag or false if it's wasn't saved
     */

    internal fun loadHideCover() = preferences.getBoolean(HIDE_COVER, false)

    /**
     * Saves hide track's cover on playing panel flag in [SharedPreferences]
     * @param isCoverHidden flag to save
     */

    internal fun storeHideCover(isCoverHidden: Boolean) = preferences.edit().run {
        putBoolean(HIDE_COVER, isCoverHidden)
        apply()
    }

    /**
     * Loads show audio visualizer flag from [SharedPreferences]
     * @return show audio visualizer or true if it's wasn't saved
     */

    internal fun loadShowVisualizer() = preferences.getBoolean(SHOW_AUDIO_VISUALIZER_KEY, true)

    /**
     * Saves show audio visualizer flag in [SharedPreferences]
     * @param isVisualizerShown show audio visualizer flag to save
     */

    internal fun storeShowVisualizer(isVisualizerShown: Boolean) = preferences.edit().run {
        putBoolean(SHOW_AUDIO_VISUALIZER_KEY, isVisualizerShown)
        apply()
    }

    /**
     * Loads save cur track and playlist flag from [SharedPreferences]
     * @return save cur track and playlist flag or true if it's wasn't saved
     */

    internal fun loadSaveCurTrackAndPlaylist() =
        preferences.getBoolean(SAVE_CUR_TRACK_PLAYLIST_KEY, true)

    /**
     * Saves save cur track and playlist flag in [SharedPreferences]
     * @param areCurTrackAndPlaylistSaved save cur track and playlist flag to save
     */

    internal suspend fun storeSaveCurTrackAndPlaylistLocking(areCurTrackAndPlaylistSaved: Boolean) =
        mutex.withLock {
            preferences.edit().run {
                putBoolean(SAVE_CUR_TRACK_PLAYLIST_KEY, areCurTrackAndPlaylistSaved)
                apply()
            }
        }

    /**
     * Loads save looping flag from [SharedPreferences]
     * @return save looping flag or true if it's wasn't saved
     */

    internal fun loadSaveLooping() = preferences.getBoolean(SAVE_LOOPING_KEY, true)

    /**
     * Saves save looping flag in [SharedPreferences]
     * @param isLoopingSaved save looping flag to save
     */

    internal fun storeSaveLooping(isLoopingSaved: Boolean) = preferences.edit().run {
        putBoolean(SAVE_LOOPING_KEY, isLoopingSaved)
        apply()
    }

    /**
     * Loads save equalizer's settings flag from [SharedPreferences]
     * @return save equalizer's settings flag or true if it's wasn't saved
     */

    internal fun loadSaveEqualizerSettings() =
        preferences.getBoolean(SAVE_EQUALIZER_SETTINGS_KEY, true)

    /**
     * Saves save equalizer's settings flag in [SharedPreferences]
     * @param isEqualizerSettingsSaved save equalizer's settings flag to save
     */

    internal fun storeSaveEqualizerSettings(isEqualizerSettingsSaved: Boolean) =
        preferences.edit().run {
            putBoolean(SAVE_EQUALIZER_SETTINGS_KEY, isEqualizerSettingsSaved)
            apply()
        }

    /**
     * Loads track order from [SharedPreferences]
     * @return track order or (TITLE, true) if it's wasn't saved
     */

    internal fun loadTrackOrder() = Gson().fromJson<Pair<Int, Boolean>?>(
        preferences.getString(TRACKS_ORDER_KEY, null),
        object : TypeToken<Pair<Int, Boolean>?>() {}.type
    )?.let { (ord, isAsc) -> Params.Companion.TracksOrder.values()[ord] to isAsc }

    /**
     * Saves track order in [SharedPreferences]
     * @param trackOrder track order to save
     */

    internal suspend fun storeTrackOrderLocking(trackOrder: Pair<Params.Companion.TracksOrder, Boolean>) =
        mutex.withLock {
            preferences.edit().run {
                putString(
                    TRACKS_ORDER_KEY,
                    Gson().toJson(trackOrder.let { (ord, isAsc) ->
                        ord.ordinal to isAsc
                    })
                )
                apply()
            }
        }

    /**
     * Loads tracks search order from [SharedPreferences]
     * @return tracks search order or everything if it's wasn't saved
     */

    internal fun loadTrackSearchOrder() = Gson().fromJson<IntArray?>(
        preferences.getString(TRACKS_SEARCH_ORDER_KEY, null),
        object : TypeToken<IntArray?>() {}.type
    )?.map(TrackListSearchFragment.SearchOrder.values()::get)

    /**
     * Saves tracks search order in [SharedPreferences]
     * @param trackSearchOrder tracks search order to save
     */

    internal suspend fun storeTrackSearchOrderLocking(
        trackSearchOrder: List<TrackListSearchFragment.SearchOrder>
    ) = mutex.withLock {
        preferences.edit().run {
            putString(
                TRACKS_SEARCH_ORDER_KEY,
                Gson().toJson(trackSearchOrder.map(TrackListSearchFragment.SearchOrder::ordinal))
            )
            apply()
        }
    }

    /**
     * Loads happi api key from [SharedPreferences]
     * @return happi api key or null if it's wasn't saved
     */

    @Deprecated("Switched to Genius API")
    internal suspend fun loadHappiApiKeyLocking() =
        mutex.withLock { preferences.getString(HAPPI_API_KEY, null) }

    /**
     * Saves happi api key in [SharedPreferences]
     * @param happiApiKey happi api key to save
     */

    @Deprecated("Switched to Genius API")
    internal suspend fun storeHappiApiKeyLocking(happiApiKey: String) = mutex.withLock {
        preferences.edit().run {
            putString(HAPPI_API_KEY, happiApiKey)
            apply()
        }
    }

    /**
     * Loads custom theme's colors from [SharedPreferences]
     * @return custom theme colors or null if it's wasn't saved
     */

    internal fun loadCustomThemeColors(): Pair<Int, Int>? = Gson().fromJson(
        preferences.getString(CUSTOM_THEME_COLORS_KEY, null),
        object : TypeToken<Pair<Int, Int>?>() {}.type
    )

    /**
     * Saves custom theme's colors in [SharedPreferences]
     * @param customThemeColors custom theme colors to save
     */

    internal fun storeCustomThemeColors(customThemeColors: Pair<Int, Int>) =
        preferences.edit().run {
            putString(CUSTOM_THEME_COLORS_KEY, Gson().toJson(customThemeColors))
            apply()
        }

    /**
     * Loads app's background image from [SharedPreferences]
     * @return app's background image or null if it's wasn't saved
     */

    internal fun loadBackgroundImage(): ByteArray? = Gson().fromJson(
        preferences.getString(BACKGROUND_IMAGE_KEY, null),
        object : TypeToken<ByteArray?>() {}.type
    )

    /**
     * Saves app's background image in [SharedPreferences]
     * @param background [ByteArray] of app's background image
     */

    internal suspend fun storeBackgroundImageAsync(background: ByteArray) = mutex.withLock {
        preferences.edit().run {
            putString(BACKGROUND_IMAGE_KEY, Gson().toJson(background))
            apply()
        }
    }

    /**
     * Loads is bloom enabled flag from [SharedPreferences]
     * @return is bloom enabled flag or true if it's wasn't saved
     */

    internal fun loadBloom() = preferences.getBoolean(BLOOM_KEY, true)

    /**
     * Saves is blooming flag in [SharedPreferences]
     * @param isBloomEnabled flag to save
     */

    internal fun storeBloom(isBloomEnabled: Boolean) = preferences.edit().run {
        putBoolean(BLOOM_KEY, isBloomEnabled)
        apply()
    }

    /**
     * Loads is starting with equalizer flag from [SharedPreferences]
     * @return is starting with equalizer flag or false if it's wasn't saved
     */

    internal fun loadStartWithEqualizer() = preferences.getBoolean(START_WITH_EQUALIZER_KEY, false)

    /**
     * Saves is starting with equalizer flag in [SharedPreferences]
     * @param isStartingWithEqualizer flag to save
     */

    internal fun storeStartWithEqualizer(isStartingWithEqualizer: Boolean) =
        preferences.edit().run {
            putBoolean(START_WITH_EQUALIZER_KEY, isStartingWithEqualizer)
            apply()
        }

    /**
     * Loads use android notification flag from [SharedPreferences]
     * @return use android notification flag or false if it's wasn't saved
     */

    @RequiresApi(Build.VERSION_CODES.P)
    internal fun loadUseAndroidNotification() =
        preferences.getBoolean(USE_ANDROID_NOTIFICATION_KEY, false)

    /**
     * Saves current visualizer style in [SharedPreferences]
     * @param visualizerStyle to save
     */

    internal fun storeVisualizerStyle(visualizerStyle: Params.Companion.VisualizerStyle) =
        preferences.edit().run {
            putInt(VISUALIZER_STYLE_KEY, visualizerStyle.ordinal)
            apply()
        }

    /**
     * Loads current visualizer style from [SharedPreferences]
     * @return current visualizer style or [Params.Companion.VisualizerStyle.BAR] if it's wasn't saved
     */

    internal fun loadVisualizerStyle() =
        Params.Companion.VisualizerStyle.values()[preferences.getInt(VISUALIZER_STYLE_KEY, 0)]

    /**
     * Saves is using android notification flag in [SharedPreferences]
     * @param isUsingAndroidNotification flag to save
     */

    @RequiresApi(Build.VERSION_CODES.P)
    internal fun storeIsUsingAndroidNotification(isUsingAndroidNotification: Boolean) =
        preferences.edit().run {
            putBoolean(USE_ANDROID_NOTIFICATION_KEY, isUsingAndroidNotification)
            apply()
        }

    /**
     * Saves current home screen in [SharedPreferences]
     * @param homeScreen to save
     */

    internal fun storeHomeScreen(homeScreen: Params.Companion.HomeScreen) = preferences.edit().run {
        putInt(HOME_SCREEN_KEY, homeScreen.ordinal)
        apply()
    }

    /**
     * Loads current home screen from [SharedPreferences]
     * @return current home screen or [Params.Companion.HomeScreen.TRACKS] if it's wasn't saved
     */

    internal fun loadHomeScreen() =
        Params.Companion.HomeScreen.values()[preferences.getInt(HOME_SCREEN_KEY, 0)]

    /**
     * Saves path where converted mp3 tracks will be saved in [SharedPreferences]
     * @param pathToSave with converted mp3 tracks
     */

    internal fun storePathToSave(pathToSave: String) = preferences.edit().run {
        putString(PATH_TO_SAVE_KEY, pathToSave)
        apply()
    }

    /**
     * Loads path where converted mp3 tracks will be saved from [SharedPreferences]
     * @return path itself or [Params.NO_PATH]
     */

    internal fun loadPathToSave() = preferences.getString(PATH_TO_SAVE_KEY, Params.DEFAULT_PATH)!!

    /**
     * Saves flag about blurred images in [SharedPreferences]
     * @param isBlurred  images flag to save
     */

    internal fun storeBlurred(isBlurred: Boolean) = preferences.edit().run {
        putBoolean(BLUR_ON_BACKGROUND_KEY, isBlurred)
        apply()
    }

    /**
     * Loads flag about blurred images from [SharedPreferences]
     * @return saving blurred images flag or true if it's wasn't saved
     */

    internal fun loadBlurred() = preferences.getBoolean(BLUR_ON_BACKGROUND_KEY, true)

    /**
     * Loads display covers flag from [SharedPreferences]
     * @return display covers flag or true if it's wasn't saved
     */

    internal fun loadDisplayCovers() = preferences.getBoolean(DISPLAY_COVERS_KEY, true)

    /**
     * Saves display covers flag in [SharedPreferences]
     * @param areCoversDisplayed flag to save
     */

    internal fun storeDisplayCovers(areCoversDisplayed: Boolean) = preferences.edit().run {
        putBoolean(DISPLAY_COVERS_KEY, areCoversDisplayed)
        apply()
    }

    /**
     * Loads rotate cover on small playback panel flag from [SharedPreferences]
     * @return rotate cover on small playback panel flag or true if it's wasn't saved
     */

    internal fun loadRotateCover() = preferences.getBoolean(ROTATE_COVER_KEY, true)

    /**
     * Saves rotate cover on small playback panel flag in [SharedPreferences]
     * @param isCoverRotating flag to save
     */

    internal fun storeRotateCover(isCoverRotating: Boolean) = preferences.edit().run {
        putBoolean(ROTATE_COVER_KEY, isCoverRotating)
        apply()
    }

    /**
     * Saves all time statistics in [SharedPreferences]
     * @param statistics to save
     */

    internal fun storeStatistics(statistics: Statistics) = preferences.edit().run {
        putString(STATISTICS_KEY, Gson().toJson(statistics))
        apply()
    }

    /**
     * Loads all time statistics from [SharedPreferences]
     * @return all time statistics
     */

    internal fun loadStatistics(): Statistics? = Gson().fromJson(
        preferences.getString(STATISTICS_KEY, null),
        object : TypeToken<Statistics?>() {}.type
    )

    /**
     * Saves daily statistics in [SharedPreferences]
     * @param statisticsDaily to save
     */

    internal fun storeStatisticsDaily(statisticsDaily: Statistics) = preferences.edit().run {
        putString(STATISTICS_DAILY_KEY, Gson().toJson(statisticsDaily))
        apply()
    }

    /**
     * Loads daily statistics from [SharedPreferences]
     * @return daily statistics
     */

    internal fun loadStatisticsDaily(): Statistics? = Gson().fromJson(
        preferences.getString(STATISTICS_DAILY_KEY, null),
        object : TypeToken<Statistics?>() {}.type
    )

    /**
     * Saves weekly statistics in [SharedPreferences]
     * @param statisticsWeekly to save
     */

    internal fun storeStatisticsWeekly(statisticsWeekly: Statistics) = preferences.edit().run {
        putString(STATISTICS_WEEKLY_KEY, Gson().toJson(statisticsWeekly))
        apply()
    }

    /**
     * Loads weekly statistics from [SharedPreferences]
     * @return weekly statistics
     */

    internal fun loadStatisticsWeekly(): Statistics? = Gson().fromJson(
        preferences.getString(STATISTICS_WEEKLY_KEY, null),
        object : TypeToken<Statistics?>() {}.type
    )

    /**
     * Saves monthly statistics in [SharedPreferences]
     * @param statisticsMonthly to save
     */

    internal fun storeStatisticsMonthly(statisticsMonthly: Statistics) = preferences.edit().run {
        putString(STATISTICS_MONTHLY_KEY, Gson().toJson(statisticsMonthly))
        apply()
    }

    /**
     * Loads monthly statistics from [SharedPreferences]
     * @return monthly statistics
     */

    internal fun loadStatisticsMonthly(): Statistics? = Gson().fromJson(
        preferences.getString(STATISTICS_MONTHLY_KEY, null),
        object : TypeToken<Statistics?>() {}.type
    )

    /**
     * Saves yearly statistics in [SharedPreferences]
     * @param statisticsYearly to save
     */

    internal fun storeStatisticsYearly(statisticsYearly: Statistics) = preferences.edit().run {
        putString(STATISTICS_YEARLY_KEY, Gson().toJson(statisticsYearly))
        apply()
    }

    /**
     * Loads yearly statistics from [SharedPreferences]
     * @return yearly statistics
     */

    internal fun loadStatisticsYearly(): Statistics? = Gson().fromJson(
        preferences.getString(STATISTICS_YEARLY_KEY, null),
        object : TypeToken<Statistics?>() {}.type
    )

    /**
     * Saves password's hash in [SharedPreferences]
     * @param password hash of password
     */

    internal suspend fun storeHiddenPassword(password: Int) = mutex.withLock {
        preferences.edit().run {
            putString(HIDDEN_PASSWORD_KEY, password.toString())
            apply()
        }
    }

    /**
     * Loads password's hash from [SharedPreferences]
     * @return hash of password or null if it wasn't set
     */

    internal suspend fun loadHiddenPassword() = mutex.withLock {
        preferences.getString(HIDDEN_PASSWORD_KEY, null)?.toInt()
    }

    /** Clears playlist data in [SharedPreferences] */

    internal fun clearCachedPlaylist() = preferences.edit().apply {
        remove(TRACK_LIST_KEY)
        apply()
    }

    /** Clears tracks progress (cur track, playlist) in [SharedPreferences] */

    internal fun clearPlayingProgress() = preferences.edit().apply {
        remove(TRACK_PATH_KEY)
        remove(PAUSE_TIME_KEY)
        remove(CURRENT_PLAYLIST_KEY)
        apply()
    }

    /** Clears looping status in [SharedPreferences]*/

    internal fun clearLooping() = preferences.edit().apply {
        remove(LOOPING_STATUS_KEY)
        apply()
    }

    /** Clears equalizer progress in [SharedPreferences] */

    internal fun clearEqualizerProgress() = preferences.edit().apply {
        remove(EQUALIZER_SEEKBARS_POS_KEY)
        remove(EQUALIZER_PRESET_POS_KEY)
        remove(EQUALIZER_BASS_STRENGTH)
        remove(EQUALIZER_REVERB_PRESET)
        remove(PITCH_KEY)
        remove(SPEED_KEY)
        apply()
    }

    /** Clears custom theme's colors in [SharedPreferences] */

    internal fun clearCustomThemeColors() = preferences.edit().apply {
        remove(CUSTOM_THEME_COLORS_KEY)
        apply()
    }

    /** Clears app's background picture in [SharedPreferences] */

    internal fun clearBackgroundImage() = preferences.edit().apply {
        remove(BACKGROUND_IMAGE_KEY)
        apply()
    }

    /** Clears whole user's statistics */

    internal fun clearStatistics() = preferences.edit().apply {
        remove(STATISTICS_KEY)
        remove(STATISTICS_DAILY_KEY)
        remove(STATISTICS_WEEKLY_KEY)
        remove(STATISTICS_MONTHLY_KEY)
        remove(STATISTICS_YEARLY_KEY)
        apply()
    }

    /** Clears daily user's statistics */

    internal fun clearStatisticsDaily() = preferences.edit().apply {
        remove(STATISTICS_DAILY_KEY)
        apply()
    }

    /** Clears weekly user's statistics */

    internal fun clearStatisticsWeekly() = preferences.edit().apply {
        remove(STATISTICS_WEEKLY_KEY)
        apply()
    }

    /** Clears monthly user's statistics */

    internal fun clearStatisticsMonthly() = preferences.edit().apply {
        remove(STATISTICS_MONTHLY_KEY)
        apply()
    }

    /** Clears yearly user's statistics */

    internal fun clearStatisticsYearly() = preferences.edit().apply {
        remove(STATISTICS_YEARLY_KEY)
        apply()
    }
}