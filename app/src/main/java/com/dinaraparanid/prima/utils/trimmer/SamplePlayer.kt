package com.dinaraparanid.prima.utils.trimmer

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.dinaraparanid.prima.utils.trimmer.soundfile.SoundFile
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.ShortBuffer

internal class SamplePlayer(
    private val samples: ShortBuffer,
    private val sampleRate: Int,
    /** Number of samples per channel */
    private val channels: Int,
    private val numSamples: Int
) : CoroutineScope by MainScope() {
    internal interface OnCompletionListener {
        fun onCompletion()
    }

    private val audioTrack: AudioTrack
    private val buffer: ShortArray
    private var playbackStart = 0
    private var playCoroutine: Job? = null
    private var keepPlaying: Boolean
    private var listener: OnCompletionListener? = null
    private val mutex = Mutex()

    internal constructor(sf: SoundFile) : this(
        sf.decodedSamples!!,
        sf.sampleRate,
        sf.channels,
        sf.numSamples
    )

    internal fun setOnCompletionListener(listener: OnCompletionListener) {
        this.listener = listener
    }

    internal inline fun setOnCompletionListener(crossinline onCompletion: () -> Unit) =
        setOnCompletionListener(
            object : OnCompletionListener {
                override fun onCompletion() {
                    onCompletion()
                }
            }
        )

    internal val isPlaying: Boolean
        get() = audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING

    internal val isPaused: Boolean
        get() = audioTrack.playState == AudioTrack.PLAYSTATE_PAUSED

    private fun startNoLock() {
        if (isPlaying)
            return

        keepPlaying = true
        audioTrack.flush()
        audioTrack.play()

        // Setting thread feeding the audio samples to the audio hardware
        // (Assumes channels = 1 or 2)

        playCoroutine = launch(Dispatchers.Default) {
            samples.position(playbackStart * channels)
            val limit = numSamples * channels

            while (samples.position() < limit && keepPlaying) {
                val numSamplesLeft = limit - samples.position()

                when {
                    numSamplesLeft >= buffer.size -> samples[buffer]

                    else -> {
                        buffer.fill(0, numSamplesLeft)
                        samples[buffer, 0, numSamplesLeft]
                    }
                }

                audioTrack.write(buffer, 0, buffer.size)
            }
        }
    }

    internal suspend fun start(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { startNoLock() }
        else -> startNoLock()
    }

    private fun pauseNoLock() {
        if (isPlaying)
            audioTrack.pause()
    }

    internal suspend fun pause(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { pauseNoLock() }
        else -> pauseNoLock()
    }

    private fun stopNoLock() {
        if (isPlaying || isPaused) {
            keepPlaying = false
            audioTrack.pause()
            audioTrack.stop() // Unblock audioTrack.write() to avoid deadlocks

            if (playCoroutine != null)
                launch {
                    playCoroutine?.join()
                    playCoroutine = null
                }

            audioTrack.flush()
        }
    }

    internal suspend fun stop(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { stopNoLock() }
        else -> stopNoLock()
    }

    private fun releaseNoLock() {
        stopNoLock()
        audioTrack.release()
    }

    internal suspend fun release(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { releaseNoLock() }
        else -> releaseNoLock()
    }

    private fun seekToNoLock(ms: Int) {
        val wasPlaying = isPlaying
        stopNoLock()

        playbackStart = (ms * (sampleRate / 1000.0)).toInt()

        if (playbackStart > numSamples)
            playbackStart = numSamples // Nothing to play

        audioTrack.notificationMarkerPosition = numSamples - 1 - playbackStart
        if (wasPlaying) startNoLock()
    }

    internal suspend fun seekTo(isLocking: Boolean, ms: Int) = when {
        isLocking -> mutex.withLock { seekToNoLock(ms) }
        else -> seekToNoLock(ms)
    }

    internal val currentPosition: Int
        get() = ((playbackStart + audioTrack.playbackHeadPosition) *
                (1000.0 / sampleRate)).toInt()

    init {
        var bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            if (channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        // Make sure minBufferSize can contain at least 1 second of audio (16 bits sample)

        if (bufferSize < channels * sampleRate shl 1)
            bufferSize = channels * sampleRate shl 1

        buffer = ShortArray(bufferSize shr 1) // bufferSize is in Bytes

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setChannelMask(
                        when (channels) {
                            1 -> AudioFormat.CHANNEL_OUT_MONO
                            else -> AudioFormat.CHANNEL_OUT_STEREO
                        }
                    )
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .build()

        // Check when player played all the given data and notify user if listener is set.

        audioTrack.notificationMarkerPosition = numSamples - 1 // Set the marker to the end.

        audioTrack.setPlaybackPositionUpdateListener(
            object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onPeriodicNotification(track: AudioTrack) = Unit
                override fun onMarkerReached(track: AudioTrack) {
                    launch { stop(isLocking = true) }

                    if (listener != null)
                        listener!!.onCompletion()
                }
            })

        playCoroutine = null
        keepPlaying = true
        listener = null
    }
}