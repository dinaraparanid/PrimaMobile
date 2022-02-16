package com.dinaraparanid.prima.utils.rustlibs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;

/**
 * NativeLibrary class
 * to support native Rust code
 */

public enum NativeLibrary {;

    static {
        System.loadLibrary("NativeLibrary");
    }

    @NonNull
    private static final native String artistImageBind(final @NonNull byte[] name);

    /**
     * Converts artist name to the next pattern:
     * name family ... -> NF (upper case).
     * If artist don't have second word in his name, it will return only first letter
     *
     * @param name artist's name as byte array
     * @return name patter
     */

    @NonNull
    public static final String artistImageBind(final @NonNull String name) {
        return artistImageBind(name.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Calculates time in hh:mm:ss format
     * @param millis millisecond to convert
     * @return int[hh, mm, ss]
     */

    @NonNull
    public static final native int[] calcTrackTime(final int millis);

    @NonNull
    private static final native String playlistTitle(
            final @NonNull byte[] trackPlaylist,
            final @NonNull byte[] trackPath,
            final @NonNull byte[] unknown
    );

    /**
     * Gets playlist title.
     * If it equals to path,
     * it'll return 'Unknown album' in selected locale
     *
     * @param trackPlaylist album name
     * @param trackPath path to track (DATA column from MediaStore)
     * @param unknown 'Unknown album' string in selected locale
     * @return correct album title or 'Unknown album'
     */

    @NonNull
    public static final String playlistTitle(
            final @NonNull String trackPlaylist,
            final @NonNull String trackPath,
            final @NonNull String unknown
    ) {
        return playlistTitle(
                trackPlaylist.getBytes(StandardCharsets.UTF_8),
                trackPath.getBytes(StandardCharsets.UTF_8),
                unknown.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Gets lyrics of some track by it's url from Genius
     * @param url url of Genius' web page with track
     * @return lyrics of song or null if there are some errors
     */

    @Nullable
    public static final native String getLyricsByUrl(final @NonNull String url);

    private static final native byte getTrackNumberInAlbum(
            final @NonNull String url,
            final @NonNull byte[] trackTitle
    );

    /**
     * Gets track's number (starting from zero) in album by album's URL
     * @param url url to the album's web page in Genius
     * @param trackTitle track title as byte array
     * @return number in album (starting from zero) or -1 if track isn't found in album
     */

    public static final byte getTrackNumberInAlbum(
            final @NonNull String url,
            final @NonNull String trackTitle
    ) { return getTrackNumberInAlbum(url, trackTitle.getBytes(StandardCharsets.UTF_8)); }
}
