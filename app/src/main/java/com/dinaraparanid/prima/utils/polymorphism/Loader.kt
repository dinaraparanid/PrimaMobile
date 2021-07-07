package com.dinaraparanid.prima.utils.polymorphism

import kotlinx.coroutines.Deferred

internal interface Loader<T> {
    suspend fun loadAsync(): Deferred<Unit>
    val loaderContent: T
}