package com.dinaraparanid.prima.utils.trimmer.soundfile

import android.annotation.SuppressLint
import android.media.*
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A SoundFile object should only be created
 * using the static methods [create] and [record]
 */

internal class SoundFile private constructor() : Serializable {

    internal companion object {
        internal const val SAMPLES_PER_FRAME = 1024
        private val SUPPORTED_EXTENSIONS = arrayOf("mp3", "wav", "3gpp", "3gp", "amr", "aac", "m4a", "ogg")

        /**
         * Creates a [SoundFile] object using the given file name
         * @param fileName name of created file
         * @param progressListener [ProgressListener]
         * @return created [SoundFile] or null if file is incorrect
         */

        @JvmStatic
        internal fun create(
            fileName: String,
            progressListener: ProgressListener
        ): SoundFile? {
            val f = File(fileName)

            if (!f.exists())
                return null

            val components = f
                .name
                .lowercase(Locale.getDefault())
                .split("\\.".toRegex())
                .toTypedArray()

            if (components.size < 2)
                return null

            if (components[components.size - 1] !in SUPPORTED_EXTENSIONS)
                return null

            return SoundFile().apply {
                setProgressListener(progressListener)
                readFile(f)
            }
        }

        /**
         * Creates a [SoundFile] object by recording a mono audio stream.
         * @param progressListener [ProgressListener]
         * @return created [SoundFile] or null if file is incorrect
         */

        @JvmStatic
        internal fun record(progressListener: ProgressListener?): SoundFile? = progressListener?.let {
            SoundFile().apply {
                setProgressListener(it)
                recordAudio()
            }
        }
    }

    private var progressListener: ProgressListener? = null
    private var inputFile: File? = null

    internal lateinit var filetype: String
        private set

    private var fileSizeBytes = 0

    /** Average bit rate in kbps. */
    internal var avgBitrateKbps = 0
        private set

    internal var sampleRate = 0
        private set

    internal var channels = 0
        private set

    /**
     * Number of samples per channel.
     * Total number of samples per channel in audio file
     */

    internal var numSamples = 0
        private set

    /** Raw audio data */
    private var decodedBytes: ByteBuffer? = null

    /** shared buffer with [decodedBytes]. */
    internal var decodedSamples: ShortBuffer? = null
        private set

    internal var numFrames = 0
        private set

    internal var frameGains: IntArray? = null
        private set

    private var frameLens: IntArray? = null
    private var frameOffsets: IntArray? = null

    /** Progress listener interface. */
    internal interface ProgressListener {
        /**
         * Will be called by the SoundFile class periodically
         * with values between 0.0 and 1.0.  Return true to continue
         * loading the file or recording the audio, and false to cancel or stop recording.
         */
        fun reportProgress(fractionComplete: Double): Boolean
    }

    private fun setProgressListener(progressListener: ProgressListener) {
        this.progressListener = progressListener
    }

    private fun readFile(inputFile: File): Unit? {
        this.inputFile = inputFile

        val extractor = MediaExtractor()
        val components = inputFile.path.split("\\.".toRegex()).toTypedArray()

        filetype = components[components.size - 1]
        fileSizeBytes = this.inputFile!!.length().toInt()
        extractor.setDataSource(this.inputFile!!.path)

        val numTracks = extractor.trackCount
        var format: MediaFormat? = null

        var i = 0

        while (i < numTracks) {
            format = extractor.getTrackFormat(i)

            if (format.getString(MediaFormat.KEY_MIME)!!.startsWith("audio/")) {
                extractor.selectTrack(i)
                break
            }

            i++
        }

        if (i == numTracks)
            return null

        channels = format!!.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)

        val expectedNumSamples = (format.getLong(MediaFormat.KEY_DURATION) / 1000000F * sampleRate + 0.5F).toInt()

        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!).apply {
            configure(format, null, null, 0)
            start()
        }

        var decodedSamplesSize = 0 // size of the output buffer containing decoded samples.
        var decodedSamples: ByteArray? = null
        var sampleSize: Int
        val info = MediaCodec.BufferInfo()
        var presentationTime: Long
        var totSizeRead = 0
        var doneReading = false
        var firstSampleData = true

        decodedBytes = ByteBuffer.allocate(1 shl 20)

        loop@ while (true) {
            // Read data from file and feed it to the decoder input buffers.
            val inputBufferIndex = codec.dequeueInputBuffer(100)

            if (!doneReading && inputBufferIndex >= 0) {
                sampleSize = extractor.readSampleData(codec.getInputBuffer(inputBufferIndex)!!, 0)

                when {
                    firstSampleData &&
                            format.getString(MediaFormat.KEY_MIME) == "audio/mp4a-latm" &&
                            sampleSize == 2 -> {
                        extractor.advance()
                        totSizeRead += sampleSize
                    }

                    sampleSize < 0 -> {
                        codec.queueInputBuffer(
                            inputBufferIndex,
                            0, 0, -1,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        doneReading = true
                    }

                    else -> {
                        presentationTime = extractor.sampleTime

                        codec.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            sampleSize,
                            presentationTime,
                            0
                        )

                        extractor.advance()
                        totSizeRead += sampleSize

                        if (progressListener != null) {
                            if (!progressListener!!
                                    .reportProgress((totSizeRead.toFloat() / fileSizeBytes).toDouble())
                            ) {
                                // We are asked to stop reading the file. Returning immediately. The
                                // SoundFile object is invalid and should NOT be used afterward!

                                extractor.release()
                                codec.stop()
                                codec.release()
                                return Unit
                            }
                        }
                    }
                }

                firstSampleData = false
            }

            // Get decoded stream from the decoder output buffers.

            val outputBufferIndex = codec.dequeueOutputBuffer(info, 100)

            if (outputBufferIndex >= 0 && info.size > 0) {
                if (decodedSamplesSize < info.size) {
                    decodedSamplesSize = info.size
                    decodedSamples = ByteArray(decodedSamplesSize)
                }

                codec.getOutputBuffer(outputBufferIndex)?.run {
                    get(decodedSamples!!, 0, info.size)
                    clear()
                }

                // Check if buffer is big enough. Resize it if it's too small.

                if (decodedBytes!!.remaining() < info.size) {

                    // Getting a rough estimate of the total size, allocate 20% more, and
                    // make sure to allocate at least 5MB more than the initial size.

                    val position = decodedBytes!!.position()
                    var newSize = (position * (1.0 * fileSizeBytes / totSizeRead) * 1.2).toInt()

                    if (newSize - position < info.size + 5 * (1 shl 20))
                        newSize = position + info.size + 5 * (1 shl 20)

                    var newDecodedBytes: ByteBuffer? = null

                    // Try to allocate memory. If we are OOM, try to run the garbage collector.

                    var retry = 10

                    while (retry > 0) {
                        try {
                            newDecodedBytes = ByteBuffer.allocate(newSize)
                            break
                        } catch (e: OutOfMemoryError) {
                            System.gc()
                            retry--
                        }
                    }

                    if (retry == 0)
                        break@loop

                    decodedBytes!!.rewind()
                    newDecodedBytes!!.put(decodedBytes!!)
                    decodedBytes = newDecodedBytes
                    decodedBytes!!.position(position)
                }

                decodedBytes!!.put(decodedSamples!!, 0, info.size)
                codec.releaseOutputBuffer(outputBufferIndex, false)
            }

            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                || decodedBytes!!.position() / (2 * channels) >= expectedNumSamples
            ) break
        }

        numSamples = decodedBytes!!.position() / (channels * 2) // One sample = 2 bytes.

        this.decodedSamples = decodedBytes!!.run {
            rewind()
            order(ByteOrder.LITTLE_ENDIAN)
            asShortBuffer()
        }

        avgBitrateKbps = (fileSizeBytes * 8 * (sampleRate.toFloat() / numSamples) / 1000).toInt()
        extractor.release()
        codec.stop()
        codec.release()

        // Temporary hack to make it work with the old version.

        numFrames = numSamples / SAMPLES_PER_FRAME

        if (numSamples % SAMPLES_PER_FRAME != 0)
            numFrames++

        frameGains = IntArray(numFrames)
        frameLens = IntArray(numFrames)
        frameOffsets = IntArray(numFrames)

        i = 0
        var q: Int
        var gain: Int
        var value: Int
        val frameLens = (1000 * avgBitrateKbps / 8 *
                (SAMPLES_PER_FRAME.toFloat() / sampleRate)).toInt()

        while (i < numFrames) {
            gain = -1
            q = 0

            while (q < SAMPLES_PER_FRAME) {
                value = 0

                repeat(channels) {
                    if (this.decodedSamples!!.remaining() > 0) {
                        value += abs(this.decodedSamples!!.get().toInt())
                    }
                }

                value /= channels

                if (gain < value)
                    gain = value

                q++
            }

            frameGains!![i] = sqrt(gain.toDouble()).toInt()
            this.frameLens!![i] = frameLens
            frameOffsets!![i] = (i * (1000 * avgBitrateKbps / 8) *
                    (SAMPLES_PER_FRAME.toFloat() / sampleRate)).toInt()

            i++
        }

        this.decodedSamples!!.rewind()
        return Unit
    }

    @SuppressLint("MissingPermission")
    private fun recordAudio() {
        if (progressListener == null)
            return

        inputFile = null
        filetype = "raw"
        fileSizeBytes = 0
        sampleRate = 44100
        channels = 1 // record mono audio.

        val buffer = ShortArray(1024) // buffer contains 1 mono frame of 1024 16 bits samples
        var minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )

        // make sure minBufferSize can contain at least 1 second of audio (16 bits sample).

        if (minBufferSize < sampleRate * 2)
            minBufferSize = sampleRate * 2

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )

        // Allocate memory for 20 seconds first. Reallocate later if more is needed.
        decodedBytes = ByteBuffer.allocate(20 * sampleRate * 2)
        decodedBytes!!.order(ByteOrder.LITTLE_ENDIAN)
        decodedSamples = decodedBytes!!.asShortBuffer()
        audioRecord.startRecording()

        while (true) {
            if (decodedSamples!!.remaining() < 1024) {
                // Try to allocate memory for 10 additional seconds.
                val newCapacity = decodedBytes!!.capacity() + 10 * sampleRate * 2

                val newDecodedBytes: ByteBuffer = try {
                    ByteBuffer.allocate(newCapacity)
                } catch (e: OutOfMemoryError) {
                    e.printStackTrace()
                    break
                }

                val position = decodedSamples!!.position()
                decodedBytes!!.rewind()
                newDecodedBytes.put(decodedBytes!!)

                decodedBytes = newDecodedBytes.apply {
                    order(ByteOrder.LITTLE_ENDIAN)
                    rewind()
                }

                decodedSamples = decodedBytes!!.asShortBuffer()
                decodedSamples!!.position(position)
            }

            audioRecord.read(buffer, 0, buffer.size)
            decodedSamples!!.put(buffer)

            // Let the progress listener know how many seconds have been recorded.
            // The returned value tells us if we should keep recording or stop.

            if (!progressListener!!.reportProgress(
                    (decodedSamples!!.position().toFloat() / sampleRate).toDouble()
                )
            ) break
        }

        audioRecord.stop()
        audioRecord.release()

        numSamples = decodedSamples!!.position()
        decodedSamples!!.rewind()
        decodedBytes!!.rewind()
        avgBitrateKbps = sampleRate * 16 / 1000

        numFrames = numSamples / SAMPLES_PER_FRAME

        if (numSamples % SAMPLES_PER_FRAME != 0)
            numFrames++

        frameGains = IntArray(numFrames)
        frameLens = null
        frameOffsets = null

        var i = 0
        var q: Int
        var gain: Int
        var value: Int

        while (i < numFrames) {
            gain = -1
            q = 0

            while (q < SAMPLES_PER_FRAME) {
                value = when {
                    decodedSamples!!.remaining() > 0 -> abs(
                        decodedSamples!!.get().toInt()
                    )
                    else -> 0
                }

                if (gain < value)
                    gain = value

                q++
            }

            frameGains!![i] =
                sqrt(gain.toDouble()).toInt()

            i++
        }

        decodedSamples!!.rewind()
    }

    @Deprecated("Now only creating .wav files")
    internal fun writeMP4AFile(outputFile: File, startFrame: Int, numFrames: Int) = writeMP4AFile(
        outputFile,
        startFrame.toFloat() * SAMPLES_PER_FRAME / sampleRate,
        (startFrame + numFrames).toFloat() * SAMPLES_PER_FRAME / sampleRate
    )

    @Deprecated("Now only creating .wav files")
    private fun writeMP4AFile(outputFile: File, startTime: Float, endTime: Float) {
        val startOffset = (startTime * sampleRate).toInt() * 2 * channels
        var numSamples = ((endTime - startTime) * sampleRate).toInt()
        val numChannels = if (channels == 1) 2 else channels
        val mimeType = "audio/mp4a-latm"
        val bitrate = 64000 * numChannels
        val codec = MediaCodec.createEncoderByType(mimeType)
        val format = MediaFormat.createAudioFormat(mimeType, sampleRate, numChannels)

        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        // Get an estimation of the encoded data based on the bitrate. Add 10% to it.

        var estimatedEncodedSize = ((endTime - startTime) * (bitrate / 8) * 1.1).toInt()
        var encodedBytes = ByteBuffer.allocate(estimatedEncodedSize)
        val info = MediaCodec.BufferInfo()
        var doneReading = false
        var presentationTime: Long
        val frameSize = 1024 // number of samples per frame per channel for an mp4 (AAC) stream.
        var buffer = ByteArray(frameSize * numChannels * 2) // a sample is coded with a short.

        decodedBytes!!.position(startOffset)
        numSamples += 2 * frameSize // Adding 2 frames, Cf. priming frames for AAC.

        var totNumFrames = 1 + numSamples / frameSize // first AAC frame = 2 bytes
        if (numSamples % frameSize != 0)
            totNumFrames++

        val frameSizes = IntArray(totNumFrames)
        var numOutFrames = 0
        var numFrames = 0
        var numSamplesLeft = numSamples
        var encodedSamplesSize = 0 // size of the output buffer containing the encoded samples.
        var encodedSamples: ByteArray? = null

        while (true) {
            val inputBufferIndex = codec.dequeueInputBuffer(100)

            when {
                !doneReading && inputBufferIndex >= 0 -> {
                    when {
                        numSamplesLeft <= 0 -> {
                            // All samples have been read.
                            codec.queueInputBuffer(
                                inputBufferIndex,
                                0, 0, -1,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            doneReading = true
                        }

                        else -> {
                            codec.getInputBuffer(inputBufferIndex)!!.clear()

                            if (buffer.size > codec.getInputBuffer(inputBufferIndex)!!.remaining())
                                continue

                            // bufferSize is a hack to create a stereo file from a mono stream.
                            val bufferSize = if (channels == 1) buffer.size / 2 else buffer.size

                            when {
                                decodedBytes!!.remaining() < bufferSize -> {
                                    (decodedBytes!!.remaining() until bufferSize).forEach {
                                        buffer[it] = 0
                                    }

                                    decodedBytes!![buffer, 0, decodedBytes!!
                                        .remaining()]
                                }

                                else -> decodedBytes!![buffer, 0, bufferSize]
                            }

                            if (channels == 1) {
                                var i = bufferSize - 1
                                while (i >= 1) {
                                    buffer[2 * i + 1] = buffer[i]
                                    buffer[2 * i] = buffer[i - 1]
                                    buffer[2 * i - 1] = buffer[2 * i + 1]
                                    buffer[2 * i - 2] = buffer[2 * i]
                                    i -= 2
                                }
                            }

                            numSamplesLeft -= frameSize
                            codec.getInputBuffer(inputBufferIndex)!!.put(buffer)
                            presentationTime = (numFrames++ * frameSize * 1e6 / sampleRate).toLong()
                            codec.queueInputBuffer(
                                inputBufferIndex, 0, buffer.size, presentationTime, 0
                            )
                        }
                    }
                }
            }

            // Get the encoded samples from the encoder.
            val outputBufferIndex = codec.dequeueOutputBuffer(info, 100)

            if (outputBufferIndex >= 0 && info.size > 0 && info.presentationTimeUs >= 0) {
                if (numOutFrames < frameSizes.size)
                    frameSizes[numOutFrames++] = info.size

                if (encodedSamplesSize < info.size) {
                    encodedSamplesSize = info.size
                    encodedSamples = ByteArray(encodedSamplesSize)
                }

                codec.getOutputBuffer(outputBufferIndex)?.run {
                    get(encodedSamples!!, 0, info.size)
                    clear()
                }

                codec.releaseOutputBuffer(outputBufferIndex, false)

                if (encodedBytes.remaining() < info.size) {
                    estimatedEncodedSize = (estimatedEncodedSize * 1.2).toInt() // Add 20%.
                    val newEncodedBytes = ByteBuffer.allocate(estimatedEncodedSize)
                    val position = encodedBytes.position()
                    encodedBytes.rewind()
                    newEncodedBytes.put(encodedBytes)
                    encodedBytes = newEncodedBytes
                    encodedBytes.position(position)
                }

                encodedBytes.put(encodedSamples!!, 0, info.size)
            }

            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0)
                break
        }

        val encodedSize = encodedBytes.position()
        encodedBytes.rewind()
        codec.stop()
        codec.release()

        // Write the encoded stream to the file, 4kB at a time.
        buffer = ByteArray(4096)

        try {
            FileOutputStream(outputFile).apply {
                write(
                    MP4Header.getInstanceAsBytes(
                        sampleRate,
                        numChannels,
                        frameSizes,
                        bitrate
                    )
                )
            }.use {
                while (encodedSize - encodedBytes.position() > buffer.size) {
                    encodedBytes[buffer]
                    it.write(buffer)
                }

                val remaining = encodedSize - encodedBytes.position()

                if (remaining > 0) {
                    encodedBytes[buffer, 0, remaining]
                    it.write(buffer, 0, remaining)
                }
            }
        } catch (ignored: IOException) {
        }
    }

    /**
     * Method used to swap the left and right channels (needed for stereo WAV files).
     * @param buffer contains the PCM data: {sample 1 right, sample 1 left, sample 2 right, etc.}
     * The size of a sample is assumed to be 16 bits (for a single channel).
     * When done, [buffer] will contain {sample 1 left, sample 1 right, sample 2 left, etc.}
     */

    private fun swapLeftRightChannels(buffer: ByteArray) {
        val left = ByteArray(2)
        val right = ByteArray(2)

        if (buffer.size % 4 != 0)
            return

        var offset = 0

        while (offset < buffer.size) {
            left[0] = buffer[offset]
            left[1] = buffer[offset + 1]
            right[0] = buffer[offset + 2]
            right[1] = buffer[offset + 3]
            buffer[offset] = right[0]
            buffer[offset + 1] = right[1]
            buffer[offset + 2] = left[0]
            buffer[offset + 3] = left[1]
            offset += 4
        }
    }

    internal fun writeWAVFile(outputFile: File, startFrame: Int, numFrames: Int) = writeWAVFile(
        outputFile,
        startFrame.toFloat() * SAMPLES_PER_FRAME / sampleRate,
        (startFrame + numFrames).toFloat() * SAMPLES_PER_FRAME / sampleRate
    )

    private fun writeWAVFile(outputFile: File, startTime: Float, endTime: Float) {
        val startOffset = (startTime * sampleRate).toInt() * 2 * channels
        val numSamples = ((endTime - startTime) * sampleRate).toInt()

        // Start by writing the RIFF header.
        FileOutputStream(outputFile).use { fileOutputStream ->
            fileOutputStream.write(
                WAVHeader.getInstanceAsBytes(sampleRate, channels, numSamples)
            )

            // Write the samples to the file, 1024 at a time.
            val buffer = ByteArray(1024 * channels * 2) // Each sample is coded with a short.
            decodedBytes!!.position(startOffset)
            var numBytesLeft = numSamples * channels * 2

            while (numBytesLeft >= buffer.size) {
                when {
                    decodedBytes!!.remaining() < buffer.size -> {
                        (decodedBytes!!.remaining() until buffer.size).forEach {
                            buffer[it] = 0
                        }

                        decodedBytes!![buffer, 0, decodedBytes!!.remaining()]
                    }

                    else -> decodedBytes!![buffer]
                }

                if (channels == 2)
                    swapLeftRightChannels(buffer)

                fileOutputStream.write(buffer)
                numBytesLeft -= buffer.size
            }

            if (numBytesLeft > 0) {
                when {
                    decodedBytes!!.remaining() < numBytesLeft -> {
                        (decodedBytes!!.remaining() until numBytesLeft).forEach {
                            buffer[it] = 0
                        }

                        decodedBytes!![buffer, 0, decodedBytes!!.remaining()]
                    }

                    else -> decodedBytes!![buffer, 0, numBytesLeft]
                }

                if (channels == 2)
                    swapLeftRightChannels(buffer)

                fileOutputStream.write(buffer, 0, numBytesLeft)
            }
        }
    }
}