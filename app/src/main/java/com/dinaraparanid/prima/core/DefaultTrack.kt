package com.dinaraparanid.prima.core

import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack

/**
 * Default entity for song
 * without any special properties
 */

data class DefaultTrack(
    /** _ID from media columns */
    override val androidId: Long,

    /** TITLE from media columns */
    override val title: String,

    /** ARTIST from media columns */
    override val artist: String,

    /** ALBUM from media columns */
    override val album: String,

    /** DATA from media columns */
    override val path: String,

    /** DURATION from media columns */
    override val duration: Long,

    /** RELATIVE_PATH from media columns */
    override val relativePath: String?,

    /** DISPLAY_NAME from media columns */
    override val displayName: String?,

    /** DATE_ADDED from media columns */
    override val addDate: Long,

    /** TRACK from media columns */
    override val trackNumberInAlbum: Byte
) : AbstractTrack(
    androidId,
    title,
    artist,
    album,
    path,
    duration,
    relativePath,
    displayName,
    addDate,
    trackNumberInAlbum
) {
    /** Compares track by it's [path] */
    override fun equals(other: Any?) = super.equals(other)

    /** Hashes [DefaultTrack] by it's [path] */
    override fun hashCode() = path.hashCode()
}