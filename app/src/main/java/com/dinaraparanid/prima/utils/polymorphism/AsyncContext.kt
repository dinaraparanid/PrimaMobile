package com.dinaraparanid.prima.utils.polymorphism

import kotlinx.coroutines.*

/**
 * Coroutine context, which can run asynchronous actions
 */

internal interface AsyncContext { val coroutineScope: CoroutineScope }

/**
 * Runs some action in a coroutine of the Worker Thread
 * @param action action to run
 */

internal inline fun AsyncContext.runOnWorkerThread(crossinline action: suspend CoroutineScope.() -> Unit) =
    runAsync(Dispatchers.Default, action)

/**
 * Runs some action in a coroutine of the IO Thread
 * @param action action to run
 */

internal inline fun AsyncContext.runOnIOThread(crossinline action: suspend CoroutineScope.() -> Unit) =
    runAsync(Dispatchers.IO, action)

/**
 * Runs some action in a coroutine of the Main Thread
 * @param action action to run
 */

internal inline fun AsyncContext.runOnUIThread(crossinline action: suspend CoroutineScope.() -> Unit) =
    runAsync(Dispatchers.Main, action)

/**
 * Runs some action in a coroutine with given [CoroutineDispatcher]
 * @param dispatcher [CoroutineDispatcher] in which coroutine should be run
 * @param action action to run
 */

internal inline fun AsyncContext.runAsync(
    dispatcher: CoroutineDispatcher,
    crossinline action: suspend CoroutineScope.() -> Unit
) = coroutineScope.launch(dispatcher) { action() }

/**
 * Runs some [action] in a coroutine of the Worker Thread
 * and gets a value from that [action].
 * @param action action to run
 * @return value from [action]
 */

internal inline fun <T> AsyncContext.getFromWorkerThreadAsync(crossinline action: suspend CoroutineScope.() -> T) =
    getAsync(Dispatchers.Default, action)

/**
 * Runs some [action] in a coroutine of the IO Thread
 * and gets a value from that [action].
 * @param action action to run
 * @return value from [action]
 */

internal inline fun <T> AsyncContext.getFromIOThreadAsync(crossinline action: suspend CoroutineScope.() -> T) =
    getAsync(Dispatchers.IO, action)

/**
 * Runs some action in a coroutine of the Main Thread
 * and gets a value from that [action].
 * @param action action to run
 * @return value from [action]
 */

internal inline fun <T> AsyncContext.getFromUIThreadAsync(crossinline action: suspend CoroutineScope.() -> T) =
    getAsync(Dispatchers.Main, action)

/**
 * Runs some action in a coroutine with given [CoroutineDispatcher]
 * and gets a value from that [action].
 * @param dispatcher [CoroutineDispatcher] in which coroutine should be run
 * @param action action to run
 * @return value from [action]
 */

internal inline fun <T> AsyncContext.getAsync(
    dispatcher: CoroutineDispatcher,
    crossinline action: suspend CoroutineScope.() -> T
) = coroutineScope.async(dispatcher) { action() }