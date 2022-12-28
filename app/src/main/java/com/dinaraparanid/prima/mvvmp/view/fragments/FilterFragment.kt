package com.dinaraparanid.prima.mvvmp.view.fragments

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Filters items of Loader */

interface FilterFragment<T> {
    /** Item list to show data */
    val itemList: MutableList<T>

    /** Item list for search operations */
    val itemListSearch: MutableList<T>

    /**
     * Filters items of Loader
     * @param query text which must be in item's names
     * @param models items to filter
     * @return list with founded items
     */

    fun filter(query: String, models: Collection<T> = itemList): List<T>

    /**
     * Filters items of Loader asynchronously
     * @param query text which must be in item's names
     * @param models items to filter
     * @return list with founded items
     */

    suspend fun filterAsync(query: String, models: Collection<T> = itemList) = coroutineScope {
        async(Dispatchers.Default) { synchronized(itemList) { filter(query) } }
    }
}