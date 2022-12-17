package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import kotlinx.coroutines.channels.Channel

/** [InputDialogUIHandler] for SleepDialog */

class SleepUIHandler : InputDialogUIHandler<SleepUIHandler.Args> {
    @JvmInline
    value class Args(val startSleepServiceChannel: Channel<Short>) : InputDialogUIHandler.Args

    override suspend fun Args.onOkAsync(input: String, dialog: DialogInterface) =
        input
            .toUShortOrNull()
            ?.takeIf { it > 0u }
            ?.let { time -> startSleepServiceChannel.send(time.toShort()) }
            ?: throw Exception()
}