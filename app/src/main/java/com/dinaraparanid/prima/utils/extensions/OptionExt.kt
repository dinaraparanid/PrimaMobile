package com.dinaraparanid.prima.utils.extensions

import arrow.core.Option
import arrow.core.Some
import arrow.core.orElse

fun <T> Option<T>.unwrap(): T = orNull()!!
fun <T> Option<T>.unwrapOr(or: T): T = orElse { Some(or) }.unwrap()
inline fun <T> Option<T>.unwrapOrElse(gen: () -> T): T = unwrapOr(gen())