package com.dinaraparanid.prima.services

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.AsyncCondVar
import com.dinaraparanid.prima.utils.polymorphism.AbstractService
import com.dinaraparanid.prima.utils.polymorphism.runOnWorkerThread
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** [Service] which starts countdown for a playback sleep */

class SleepService : AbstractService() {
    private var minutesLeft: Short = 0
    private val executor = Executors.newSingleThreadExecutor()
    private var sleepingTask: Future<*>? = null
    private val sleepCondition = AsyncCondVar()

    @Volatile
    private var isPlaybackGoingToSleep = false

    internal companion object {
        private const val NOTIFICATION_ID = 103
        private const val SLEEP_CHANNEL_ID = "sleep_channel"
        private const val SLEEP_TIME = 60000L

        internal const val Broadcast_CHANGE_TIME = "com.dinaraparanid.prima.utils.CHANGE_TIME"
        internal const val NEW_TIME_ARG = "new_time"
        internal const val ACTION_PAUSE = "pause"
        internal const val ACTION_CONTINUE = "continue"
        internal const val ACTION_DISMISS = "dismiss"
    }

    private val changeTimeReceiver = object : BroadcastReceiver() {
        @SuppressLint("SyntheticAccessor")
        override fun onReceive(context: Context?, intent: Intent) {
            isPlaybackGoingToSleep = true
            minutesLeft = intent.getShortExtra(NEW_TIME_ARG, minutesLeft)
            runOnWorkerThread { buildNotification(isLocking = true) }
            startCountdown()
        }
    }

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

        runOnWorkerThread {
            buildNotification(isLocking = true)
            handleIncomingActions(intent, isLocking = true)
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(changeTimeReceiver)
    }

    private fun registerChangeTimeReceiver() =
        registerReceiver(changeTimeReceiver, IntentFilter(Broadcast_CHANGE_TIME))

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createChannel() = (getSystemService(NOTIFICATION_SERVICE)!! as NotificationManager)
        .createNotificationChannel(
            NotificationChannel(
                SLEEP_CHANNEL_ID,
                "Sleep",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
            }
        )

    override suspend fun handleIncomingActionsNoLock(action: Intent?) {
        if (action?.action == null) {
            startCountdown()
            return
        }

        val actionString = action.action

        when {
            actionString.equals(ACTION_CONTINUE, ignoreCase = true) -> startCountdown()
            actionString.equals(ACTION_PAUSE, ignoreCase = true) -> pauseTimer()
            actionString.equals(ACTION_DISMISS, ignoreCase = true) -> {
                pauseTimer()
                removeNotificationAsync(isLocking = false)
                minutesLeft = 0
            }
        }
    }

    /**
     * Starts countdown and when [minutesLeft]
     * reaches 0, stops playback
     */

    private fun startCountdown() {
        sleepingTask = executor.submit {
            while (minutesLeft > 0 && isPlaybackGoingToSleep) runOnWorkerThread {
                withTimeout(SLEEP_TIME) { sleepCondition.blockAsync() }

                if (isPlaybackGoingToSleep) {
                    minutesLeft--
                    runOnWorkerThread { buildNotification(isLocking = true) }
                }
            }

            if (minutesLeft == 0.toShort()) runOnWorkerThread {
                isPlaybackGoingToSleep = false
                removeNotificationAsync(isLocking = true)
                applicationContext.sendBroadcast(
                    Intent(MainActivity.Broadcast_PAUSE)
                        .putExtra(MainActivity.UPDATE_UI_ON_PAUSE_ARG, true)
                )
            }
        }
    }

    private fun pauseTimer() {
        sleepCondition.open()
        sleepingTask?.cancel(true)
        sleepingTask = null
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun buildNotificationNoLock() {
        val pauseAction = Intent(this@SleepService, SleepService::class.java).let {
            it.action = if (isPlaybackGoingToSleep) ACTION_PAUSE else ACTION_CONTINUE
            PendingIntent.getService(
                this@SleepService,
                if (isPlaybackGoingToSleep) 0 else 1,
                it,
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        val dismissAction = Intent(this@SleepService, SleepService::class.java).let {
            it.action = ACTION_DISMISS
            PendingIntent.getService(this@SleepService, 2, it, PendingIntent.FLAG_IMMUTABLE)
        }

        startForeground(
            NOTIFICATION_ID, NotificationCompat.Builder(applicationContext, SLEEP_CHANNEL_ID)
                .setShowWhen(false)
                .setSmallIcon(R.drawable.octopus)
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

    private suspend fun buildNotification(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { buildNotificationNoLock() }
        else -> buildNotificationNoLock()
    }
}