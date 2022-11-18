package com.dinaraparanid.prima.utils.rustlibs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * NativeLibrary class
 * to support native Rust code
 */

public enum NativeLibrary {;

    static {
        System.loadLibrary("prima_native");
    }

    /**
     * Converts artist name to the next pattern:
     * name family ... -> NF (upper case).
     * If artist don't have second word in his name, it will return only first letter
     *
     * @param name artist's name as byte array
     * @return name patter
     */

    @NonNull
    public static final native String artistImageBind(@NonNull final String name);

    /**
     * Calculates time in hh:mm:ss format
     * @param millis millisecond to convert
     * @return int[hh, mm, ss]
     */

    @NonNull
    public static final native int[] calcTrackTime(final int millis);

    /**
     * Gets playlist title.
     * If it equals to path,
     * it'll return 'Unknown album' in selected locale
     *
     * @param trackPlaylist album name
     * @param trackPath path to track (DATA column from MediaStore)
     * @param unknown 'Unknown album' string in selected locale
     * @return correct album title or 'Unknown album'
     *
     * @deprecated Path checking isn't needed now
     */

    @NonNull
    @Deprecated
    public static final native String playlistTitle(
            @NonNull final String trackPlaylist,
            @NonNull final String trackPath,
            @NonNull final String unknown
    );

    /**
     * Gets lyrics of some track by it's url from Genius
     * @param url url of Genius' web page with track
     * @return lyrics of song or null if there are some errors
     */

    @Nullable
    public static final native String getLyricsByUrl(@NonNull final String url);

    /**
     * Gets track's number (starting from zero) in album by album's URL
     * @param url url to the album's web page in Genius
     * @param trackTitle track title as byte array
     * @return number in album (starting from zero) or -1 if track isn't found in album
     */

    public static final native byte getTrackNumberInAlbum(
            @NonNull final String url,
            @NonNull final String trackTitle
    );
}
