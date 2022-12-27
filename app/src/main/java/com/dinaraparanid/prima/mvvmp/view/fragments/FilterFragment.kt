package com.dinaraparanid.prima.mvvmp.view.fragments

/** Filters items of Loader */

interface FilterFragment<T> {
    /**
     * Filters items of Loader
     * @param models items to filter
     * @param query text which must be in item's names
     * @return list with founded items
     */

    fun filter(models: Collection<T>, query: String): List<T>
}