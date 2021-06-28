package com.dinaraparanid.prima.utils.polymorphism

internal interface FilterFragment<T> {
    fun filter(models: Collection<T>?, query: String): List<T>
}