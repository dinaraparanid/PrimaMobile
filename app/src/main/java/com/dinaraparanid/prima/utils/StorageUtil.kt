package com.dinaraparanid.prima.utils

import android.content.Context
import android.content.SharedPreferences
import com.dinaraparanid.prima.core.Track
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StorageUtil(private val context: Context) {
    companion object {
        private const val STORAGE = "com.dinaraparanid.prima.STORAGE"
    }

    private var preferences: SharedPreferences? = null

    fun storeTracks(arrayList: List<Track?>?) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)
        preferences!!.edit().apply {
            putString("track_list", Gson().toJson(arrayList))
            apply()
        }
    }

    fun loadTracks(): List<Track> {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)

        return Gson().fromJson(
            preferences!!.getString("track_list", null),
            (object : TypeToken<List<Track?>?>() {}).type
        )
    }

    fun storeTrackIndex(index: Int) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)
        preferences!!.edit().apply {
            putInt("track_index", index)
            apply()
        }
    }

    fun loadTrackIndex(): Int {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)
        return preferences!!.getInt("track_index", -1) // return -1 if no data found
    }

    fun clearCachedPlaylist() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)
        preferences!!.edit().apply {
            clear()
            apply()
        }
    }
}