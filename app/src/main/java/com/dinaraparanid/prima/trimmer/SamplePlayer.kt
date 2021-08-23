/*
 * Copyright (C) 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dinaraparanid.prima.trimmer

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.dinaraparanid.prima.trimmer.soundfile.SoundFile
import com.dinaraparanid.prima.utils.extensions.unwrap
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.ShortBuffer

internal class SamplePlayer(
    private val samples: ShortBuffer,
    private val sampleRate: Int,
    /** Number of samples per channel */
    private val channels: Int,
    private val numSamples: Int
) {
    internal interface OnCompletionListener {
        fun onCompletion()
    }

    private val audioTrack: AudioTrack
    private val buffer: ShortArray
    private var playbackStart = 0
    private var playCoroutine: Option<Job>
    private var keepPlaying: Boolean
    private var listener: Option<OnCompletionListener> = None

    internal constructor(sf: SoundFile) : this(
        sf.samples.unwrap(),
        sf.sampleRate,
        sf.channels,
        sf.numSamples
    )

    internal fun setOnCompletionListener(listener: OnCompletionListener) {
        this.listener = Some(listener)
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

    internal fun start() {
        if (isPlaying)
            return

        keepPlaying = true
        audioTrack.flush()
        audioTrack.play()

        // Setting thread feeding the audio samples to the audio hardware
        // (Assumes channels = 1 or 2)

        playCoroutine = Some(
            runBlocking {
                launch {
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
        )
    }

    internal fun pause() {
        if (isPlaying)
            audioTrack.pause()
    }

    internal fun stop() {
        if (isPlaying || isPaused) {
            keepPlaying = false
            audioTrack.pause()
            audioTrack.stop() // Unblock audioTrack.write() to avoid deadlocks

            if (playCoroutine.isNotEmpty()) {
                runBlocking { playCoroutine.unwrap().join() }
                playCoroutine = None
            }

            audioTrack.flush()
        }
    }

    internal fun release() {
        stop()
        audioTrack.release()
    }

    internal fun seekTo(ms: Int) {
        val wasPlaying = isPlaying
        stop()

        playbackStart = (ms * (sampleRate / 1000.0)).toInt()

        if (playbackStart > numSamples)
            playbackStart = numSamples // Nothing to play

        audioTrack.notificationMarkerPosition = numSamples - 1 - playbackStart
        if (wasPlaying) start()
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

        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            if (channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            buffer.size * 2,
            AudioTrack.MODE_STREAM
        )

        // Check when player played all the given data and notify user if listener is set.

        audioTrack.notificationMarkerPosition = numSamples - 1 // Set the marker to the end.

        audioTrack.setPlaybackPositionUpdateListener(
            object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onPeriodicNotification(track: AudioTrack) = Unit
                override fun onMarkerReached(track: AudioTrack) {
                    stop()
                    if (listener.isNotEmpty())
                        listener.unwrap().onCompletion()
                }
            })

        playCoroutine = None
        keepPlaying = true
        listener = None
    }
}