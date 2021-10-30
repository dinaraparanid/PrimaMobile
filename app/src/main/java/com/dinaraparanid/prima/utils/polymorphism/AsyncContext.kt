package com.dinaraparanid.prima.utils.polymorphism

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

/**
 * Context with [ViewModel], which can run asynchronous actions
 */

internal interface AsyncContext { val viewModel: ViewModel }

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

internal inline fun AsyncContext.runAsync(dispatcher: CoroutineDispatcher, crossinline action: suspend CoroutineScope.() -> Unit) =
    viewModel.viewModelScope.launch(dispatcher) { action() }

/**
 * Runs some [action] in a coroutine of the Worker Thread
 * and gets a value from that [action].
 * @param action action to run
 * @return value from [action]
 */

internal inline fun <T> AsyncContext.getOnWorkerThreadAsync(crossinline action: suspend CoroutineScope.() -> T) =
    getAsync(Dispatchers.Default, action)

/**
 * Runs some [action] in a coroutine of the IO Thread
 * and gets a value from that [action].
 * @param action action to run
 * @return value from [action]
 */

internal inline fun <T> AsyncContext.getOnIOThreadAsync(crossinline action: suspend CoroutineScope.() -> T) =
    getAsync(Dispatchers.IO, action)

/**
 * Runs some action in a coroutine of the Main Thread
 * and gets a value from that [action].
 * @param action action to run
 * @return value from [action]
 */

internal inline fun <T> AsyncContext.getOnUIThreadAsync(crossinline action: suspend CoroutineScope.() -> T) =
    getAsync(Dispatchers.Main, action)

/**
 * Runs some action in a coroutine with given [CoroutineDispatcher]
 * and gets a value from that [action].
 * @param dispatcher [CoroutineDispatcher] in which coroutine should be run
 * @param action action to run
 * @return value from [action]
 */

internal inline fun <T> AsyncContext.getAsync(dispatcher: CoroutineDispatcher, crossinline action: suspend CoroutineScope.() -> T) =
    viewModel.viewModelScope.async(dispatcher) { action() }