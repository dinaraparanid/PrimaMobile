package com.dinaraparanid.prima.utils.polymorphism

internal interface ContentUpdatable<T> : UIUpdatable<T>

internal fun <T> ContentUpdatable<T>.updateContent(upd: T) = updateUI(upd).run { false }