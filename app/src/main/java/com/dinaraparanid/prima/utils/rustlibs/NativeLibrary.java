package com.dinaraparanid.prima.utils.rustlibs;

public final class NativeLibrary {
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
    public static final native String artistImageBind(final byte[] name);
}
