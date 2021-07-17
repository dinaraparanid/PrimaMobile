package com.dinaraparanid.prima.utils.rustlibs;

import androidx.annotation.NonNull;

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

    @NonNull
    public static final native int[] calcTrackTime(final int millis);

    @NonNull
    public static final native String playlistTitle(
            final @NonNull byte[] trackPlaylist,
            final @NonNull byte[] trackPath,
            final @NonNull byte[] unknown
    );

    // Not ready for usage

    private static final native long newTrack(
      final long androidId,
      final byte[] title,
      final byte[] artist,
      final byte[] playlist,
      final byte[] path,
      final long duration,
      final byte[] relativePath,
      final byte[] displayName
    );

    // -------------------------------- Track Methods --------------------------------

    private static final native long getTrackAndroidId(final long pointer);
    private static final native long getTrackDuration(final long pointer);
    private static final native String getTrackTitle(final long pointer);
    private static final native String getTrackArtist(final long pointer);
    private static final native String getTrackPlaylist(final long pointer);
    private static final native String getTrackPath(final long pointer);
    private static final native String getTrackRelativePath(final long pointer);
    private static final native String getTrackDisplayName(final long pointer);
}
