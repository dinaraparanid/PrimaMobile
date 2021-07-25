package com.dinaraparanid.prima.utils.polymorphism

/**
 * Updates UI
 */

internal interface UIUpdatable<T> {
    /**
     * Updates UI
     * @param src source which will be used to update
     */

    fun updateUI(src: T)
}