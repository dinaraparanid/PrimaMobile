package com.dinaraparanid.prima.utils

import android.content.Context
import android.content.SharedPreferences
import com.dinaraparanid.prima.utils.polymorphism.Playlist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

internal class StorageUtil(private val context: Context) {
    companion object {
        private const val STORAGE = "com.dinaraparanid.prima.STORAGE"
        private const val TRACK_LIST_KEY = "track_list"
        private const val TRACK_PATH_KEY = "track_path"
        private const val PAUSE_TIME_KEY = "pause_time"
        private const val LOOPING_KEY = "looping"
        private const val CURRENT_PLAYLIST = "current_p"
        private const val CHANGED_TRACKS = "changed_tracks"
        private const val HIDDEN_TRACKS = "hidden_tracks"
    }

    private var preferences: SharedPreferences? = null

    fun storeTracks(arrayList: List<Track?>?) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putString(TRACK_LIST_KEY, Gson().toJson(arrayList))
            apply()
        }

    fun loadTracks(): List<Track> = Gson().fromJson(
        context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
            .getString(TRACK_LIST_KEY, null),
        (object : TypeToken<List<Track?>?>() {}).type
    )

    fun storeTrackPath(path: String) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putString(TRACK_PATH_KEY, path)
            apply()
        }

    fun loadTrackPath() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getString(TRACK_PATH_KEY, "_____ЫЫЫЫЫЫЫЫ_____")!!

    fun storeTrackPauseTime(pause: Int) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putInt(PAUSE_TIME_KEY, pause)
            apply()
        }

    fun loadTrackPauseTime() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getInt(PAUSE_TIME_KEY, -1)

    fun storeLooping(looping: Boolean) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putBoolean(LOOPING_KEY, looping)
            apply()
        }

    fun loadLooping() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getBoolean(LOOPING_KEY, false)

    fun storeCurPlaylist(curPlaylist: Playlist) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putString(CURRENT_PLAYLIST, Gson().toJson(curPlaylist))
            apply()
        }

    fun loadCurPlaylist() = Gson().fromJson<List<Track>>(
        context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
            .getString(CURRENT_PLAYLIST, null),
        (object : TypeToken<List<Track?>?>() {}).type
    )?.toPlaylist()

    fun storeChangedTracks(changedTracks: MutableMap<String, Track>) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putString(CHANGED_TRACKS, Gson().toJson(changedTracks))
            apply()
        }

    fun loadChangedTracks(): MutableMap<String, Track>? = Gson().fromJson(
        context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
            .getString(CHANGED_TRACKS, null),
        (object : TypeToken<MutableMap<String, Track>?>() {}).type
    )

    fun storeHiddenTracks(hiddenTracks: MutableList<String>) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putString(HIDDEN_TRACKS, Gson().toJson(hiddenTracks))
            apply()
        }

    fun loadHiddenTracks(): MutableList<String>? = Gson().fromJson(
        context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
            .getString(HIDDEN_TRACKS, null),
        (object : TypeToken<MutableList<String>?>() {}).type
    )

    fun clearCachedPlaylist() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)
        preferences!!.edit().apply {
            remove(TRACK_LIST_KEY)
            apply()
        }
    }
}