package com.dinaraparanid.prima.utils.rustlibs;

import androidx.annotation.NonNull;

/**
 * NativeLibrary class
 * to support native Rust code
 */
public enum NativeLibrary {;

    static {
        System.loadLibrary("NativeLibrary");
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
    public static final native String artistImageBind(final @NonNull byte[] name);

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
     */
    @NonNull
    public static final native String playlistTitle(
            final @NonNull byte[] trackPlaylist,
            final @NonNull byte[] trackPath,
            final @NonNull byte[] unknown
    );
}
