package com.dinaraparanid.prima.utils

import arrow.core.Option
import arrow.core.Some
import arrow.core.orElse

fun <T> Option<T>.unwrap(): T = this.orNull()!!
fun <T> Option<T>.unwrapOr(or: T): T = this.orElse { Some(or) }.unwrap()
inline fun <T> Option<T>.unwrapOrElse(gen: () -> T): T = this.orElse { Some(gen()) }.unwrap()