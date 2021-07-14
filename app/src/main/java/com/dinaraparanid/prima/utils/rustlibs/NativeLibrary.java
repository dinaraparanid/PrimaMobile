package com.dinaraparanid.prima.utils.rustlibs;

public final class NativeLibrary {
    static {
        System.loadLibrary("NativeLibrary");
    }

    public static final native String artistImageBind(final byte[] name, final int bytes);
}
