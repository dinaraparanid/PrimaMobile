package com.dinaraparanid.prima.utils.extensions

internal fun <T> MutableMap<T, Unit>.add(element: T) = put(element, Unit)