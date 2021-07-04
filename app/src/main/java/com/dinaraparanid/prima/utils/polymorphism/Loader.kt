package com.dinaraparanid.prima.utils.polymorphism

internal interface Loader<T> {
    fun load()
    val loaderContent: T
}