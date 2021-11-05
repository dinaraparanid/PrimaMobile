package com.dinaraparanid.prima.utils.extensions

import arrow.core.Option
import arrow.core.Some
import arrow.core.orElse

/**
 * Gets value from [Option]
 * @return value if it's [Some]
 * @throws NullPointerException if it's [arrow.core.None]
 */
@Throws(NullPointerException::class)
internal fun <T> Option<T>.unwrap(): T = orNull()!!

/**
 * Gets value from [Option] if it's [Some]
 * or default value if it's [arrow.core.None]
 * @param or default value
 * @return value if it's [Some]
 * or default value if it's [arrow.core.None]
 */
@Throws(NullPointerException::class)
internal fun <T> Option<T>.unwrapOr(or: T): T = orElse { Some(or) }.unwrap()

/**
 * Similar to [unwrapOr] but takes function instead of value
 * @param gen function to generate new value
 */
@Throws(NullPointerException::class)
internal inline fun <T> Option<T>.unwrapOrElse(gen: () -> T): T = unwrapOr(gen())