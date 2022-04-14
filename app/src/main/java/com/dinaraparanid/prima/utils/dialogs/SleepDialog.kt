package com.dinaraparanid.prima.utils.dialogs

import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.text.InputType
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.services.SleepService
import com.dinaraparanid.prima.utils.polymorphism.InputDialog

/** Dialog to start countdown for playback sleeping */

internal class SleepDialog(application: MainApplication) : InputDialog(
    R.string.sleep_time,
    { input, _ ->
        input.toUShortOrNull()?.takeIf { it > 0u }?.let { time ->
            when {
                !application.isSleepServiceBounded ->
                    Intent(application.applicationContext, SleepService::class.java)
                        .apply { putExtra(SleepService.NEW_TIME_ARG, time.toShort()) }
                        .let { sleepIntent ->
                            application.applicationContext.startService(sleepIntent)
                            application.applicationContext.bindService(
                                sleepIntent,
                                application.sleepServiceConnection,
                                BIND_AUTO_CREATE
                            )
                        }

                else -> application.applicationContext.sendBroadcast(
                    Intent(SleepService.Broadcast_CHANGE_TIME).apply {
                        putExtra(SleepService.NEW_TIME_ARG, time.toShort())
                    }
                )
            }
        } ?: throw Exception()
    },
    R.string.sleep_time_error,
    InputType.TYPE_NUMBER_FLAG_SIGNED,
    3
)