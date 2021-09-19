package com.dinaraparanid.prima.utils.extensions

import java.lang.ref.WeakReference

internal inline val <T> WeakReference<T>.unchecked
    get() = this.get()!!