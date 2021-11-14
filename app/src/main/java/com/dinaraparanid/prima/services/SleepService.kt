package com.dinaraparanid.prima.services

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** [Service] which starts countdown for a playback sleep */

class SleepService : Service() {
    private var minutesLeft: Short = 0
    private val iBinder = LocalBinder()
    private val executor = Executors.newSingleThreadExecutor()
    private var sleepingTask: Future<*>? = null
    private val sleepLock = ReentrantLock()
    private val sleepCondition = sleepLock.newCondition()

    @Volatile
    private var isPlaybackGoingToSleep = false

    internal companion object {
        private const val NOTIFICATION_ID = 103
        private const val SLEEP_CHANNEL_ID = "sleep_channel"
        internal const val Broadcast_CHANGE_TIME = "com.dinaraparanid.prima.utils.CHANGE_TIME"
        internal const val NEW_TIME_ARG = "new_time"
        internal const val ACTION_PAUSE = "pause"
        internal const val ACTION_CONTINUE = "continue"
        internal const val ACTION_DISMISS = "dismiss"
    }

    private inner class LocalBinder : Binder() {
        inline val service
            get() = this@SleepService
    }

    private val changeTimeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            isPlaybackGoingToSleep = true
            minutesLeft = intent.getShortExtra(NEW_TIME_ARG, minutesLeft)
            buildNotification()
            startCountdown()
        }
    }

    override fun onBind(intent: Intent?): IBinder = iBinder

    override fun onCreate() {
        super.onCreate()
        registerChangeTimeReceiver()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel()

        isPlaybackGoingToSleep = when (intent.action) {
            null -> {
                minutesLeft = intent.getShortExtra(NEW_TIME_ARG, minutesLeft)
                true
            }

            ACTION_CONTINUE -> true
            else -> false
        }

        buildNotification()
        handleIncomingActions(intent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(changeTimeReceiver)
    }

    private fun registerChangeTimeReceiver() =
        registerReceiver(changeTimeReceiver, IntentFilter(Broadcast_CHANGE_TIME))

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            (getSystemService(NOTIFICATION_SERVICE)!! as NotificationManager).createNotificationChannel(
                NotificationChannel(
                    SLEEP_CHANNEL_ID,
                    "Sleep",
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> NotificationManager.IMPORTANCE_LOW
                        else -> 0
                    }
                ).apply {
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setSound(null, null)
                }
            )
    }

    private fun startCountdown() {
        sleepingTask = executor.submit {
            sleepLock.withLock {
                while (minutesLeft > 0 && isPlaybackGoingToSleep) {
                    sleepCondition.await(1, TimeUnit.MINUTES)

                    if (isPlaybackGoingToSleep) {
                        minutesLeft--
                        buildNotification()
                    }
                }

                if (minutesLeft == 0.toShort()) {
                    isPlaybackGoingToSleep = false
                    removeNotification()
                    applicationContext.sendBroadcast(Intent(MainActivity.Broadcast_PAUSE))
                }
            }
        }
    }

    private fun pauseTimer() {
        sleepLock.withLock(sleepCondition::signal)
        sleepingTask?.cancel(true)
        sleepingTask = null
    }

    @Synchronized
    private fun handleIncomingActions(action: Intent) {
        if (action.action == null) {
            startCountdown()
            return
        }

        val actionString = action.action

        when {
            actionString.equals(ACTION_CONTINUE, ignoreCase = true) -> startCountdown()
            actionString.equals(ACTION_PAUSE, ignoreCase = true) -> pauseTimer()
            actionString.equals(ACTION_DISMISS, ignoreCase = true) -> {
                pauseTimer()
                removeNotification()
                minutesLeft = 0
            }
        }
    }

    @Synchronized
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun buildNotification() {
        val pauseAction = Intent(this, SleepService::class.java).let {
            it.action = if (isPlaybackGoingToSleep) ACTION_PAUSE else ACTION_CONTINUE
            PendingIntent.getService(this, if (isPlaybackGoingToSleep) 0 else 1, it, 0)
        }

        val dismissAction = Intent(this, SleepService::class.java).let {
            it.action = ACTION_DISMISS
            PendingIntent.getService(this, 2, it, 0)
        }

        startForeground(
            NOTIFICATION_ID, NotificationCompat.Builder(applicationContext, SLEEP_CHANNEL_ID)
                .setShowWhen(false)
                .setSmallIcon(R.drawable.cat)
                .setContentTitle(resources.getString(R.string.time_left))
                .setContentText("${resources.getString(R.string.minutes_left)}: $minutesLeft")
                .setAutoCancel(true)
                .setSilent(true)
                .addAction(
                    NotificationCompat.Action.Builder(
                        IconCompat.createWithResource(
                            applicationContext,
                            if (isPlaybackGoingToSleep) R.drawable.pause else R.drawable.play
                        ),
                        resources.getString(
                            when {
                                isPlaybackGoingToSleep -> R.string.sleep_pause
                                else -> R.string.sleep_continue
                            }
                        ),
                        pauseAction
                    ).build()
                )
                .addAction(
                    NotificationCompat.Action.Builder(
                        IconCompat.createWithResource(
                            applicationContext,
                            R.drawable.carbon_clear
                        ),
                        resources.getString(R.string.sleep_dismiss),
                        dismissAction
                    ).build()
                )
                .build()
        )
    }

    @Synchronized
    private fun removeNotification() = stopForeground(true)
}