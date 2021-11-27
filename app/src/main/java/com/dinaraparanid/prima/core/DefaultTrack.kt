package com.dinaraparanid.prima.core

import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack

/** Default entity for songs */
data class DefaultTrack(
    override val androidId: Long,
    override val title: String,
    override val artist: String,
    override val playlist: String,
    override val path: String,          // DATA from media columns
    override val duration: Long,
    override val relativePath: String?, // RELATIVE_PATH from media columns
    override val displayName: String?,  // DISPLAY_NAME from media columns
    override val addDate: Long
) : AbstractTrack(
    androidId,
    title,
    artist,
    playlist,
    path,
    duration,
    relativePath,
    displayName,
    addDate
)