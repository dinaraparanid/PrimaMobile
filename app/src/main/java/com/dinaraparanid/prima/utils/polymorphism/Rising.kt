package com.dinaraparanid.prima.utils.polymorphism

/** Interface to rise fragment */

internal interface Rising {
    /**
     * Rise fragment if playing bar is active.
     * It handles GUI error when playing bar was hiding some content
     */

    fun up()
}