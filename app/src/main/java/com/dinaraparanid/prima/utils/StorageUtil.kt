package com.dinaraparanid.prima.utils

import android.content.Context
import android.content.SharedPreferences
import com.dinaraparanid.prima.core.Track
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

internal class StorageUtil(private val context: Context) {
    companion object {
        private const val STORAGE = "com.dinaraparanid.prima.STORAGE"
    }

    private var preferences: SharedPreferences? = null

    fun storeTracks(arrayList: List<Track?>?) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putString("track_list", Gson().toJson(arrayList))
            apply()
        }

    fun loadTracks(): List<Track> = Gson().fromJson(
        context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
            .getString("track_list", null),
        (object : TypeToken<List<Track?>?>() {}).type
    )

    fun storeTrackIndex(index: Int) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putInt("track_index", index)
            apply()
        }

    fun loadTrackIndex() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getInt("track_index", -1) // return -1 if no data found

    fun storeTrackPauseTime(pause: Int) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putInt("pause_time", pause)
            apply()
        }

    fun loadTrackPauseTime() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getInt("pause_time", -1)

    fun storeLooping(looping: Boolean) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putBoolean("looping", looping)
            apply()
        }

    fun loadLooping() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getBoolean("looping", false)


    fun clearCachedPlaylist() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)
        preferences!!.edit().apply {
            remove("track_list")
            apply()
        }
    }
}