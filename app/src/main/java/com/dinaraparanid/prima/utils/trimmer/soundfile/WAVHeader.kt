package com.dinaraparanid.prima.utils.trimmer.soundfile

internal class WAVHeader private constructor(
    /** Sampling frequency in Hz (e.g. 44100) */
    private val sampleRate: Int,

    /** Number of channels */
    private val channels: Int,

    /** Total number of samples per channel */
    private val numSamples: Int
) {
    /** The complete header */
    internal var wavHeader: ByteArray? = null

    /** Number of bytes per sample, all channels included */
    private val numBytesPerSample: Int = 2 * channels

    init {
        setHeader()
    }

    internal companion object {
        @JvmStatic
        internal fun getInstanceAsBytes(sampleRate: Int, numChannels: Int, numSamples: Int) =
            WAVHeader(sampleRate, numChannels, numSamples).wavHeader
    }

    private fun setHeader() {
        val header = ByteArray(46)
        var offset = 0

        // set the RIFF chunk
        System.arraycopy(
            byteArrayOf(
                'R'.code.toByte(),
                'I'.code.toByte(),
                'F'.code.toByte(),
                'F'.code.toByte()
            ),
            0,
            header,
            offset,
            4
        )

        offset += 4

        var size = 36 + numSamples * numBytesPerSample

        header[offset++] = (size and 0xFF).toByte()
        header[offset++] = (size shr 8 and 0xFF).toByte()
        header[offset++] = (size shr 16 and 0xFF).toByte()
        header[offset++] = (size shr 24 and 0xFF).toByte()

        System.arraycopy(
            byteArrayOf('W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte()),
            0,
            header,
            offset,
            4
        )

        offset += 4

        // Set the fmt chunk
        System.arraycopy(
            byteArrayOf('f'.code.toByte(), 'm'.code.toByte(), 't'.code.toByte(), ' '.code.toByte()),
            0,
            header,
            offset,
            4
        )

        offset += 4

        System.arraycopy(byteArrayOf(0x10, 0, 0, 0), 0, header, offset, 4) // chunk size = 16
        offset += 4

        System.arraycopy(byteArrayOf(1, 0), 0, header, offset, 2) // format = 1 for PCM
        offset += 2

        header[offset++] = (channels and 0xFF).toByte()
        header[offset++] = (channels shr 8 and 0xFF).toByte()
        header[offset++] = (sampleRate and 0xFF).toByte()
        header[offset++] = (sampleRate shr 8 and 0xFF).toByte()
        header[offset++] = (sampleRate shr 16 and 0xFF).toByte()
        header[offset++] = (sampleRate shr 24 and 0xFF).toByte()

        val byteRate = sampleRate * numBytesPerSample
        header[offset++] = (byteRate and 0xFF).toByte()
        header[offset++] = (byteRate shr 8 and 0xFF).toByte()
        header[offset++] = (byteRate shr 16 and 0xFF).toByte()
        header[offset++] = (byteRate shr 24 and 0xFF).toByte()
        header[offset++] = (numBytesPerSample and 0xFF).toByte()
        header[offset++] = (numBytesPerSample shr 8 and 0xFF).toByte()

        System.arraycopy(byteArrayOf(0x10, 0), 0, header, offset, 2)
        offset += 2

        // Set the beginning of the data chunk
        System.arraycopy(
            byteArrayOf('d'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte()),
            0,
            header,
            offset,
            4
        )

        offset += 4

        size = numSamples * numBytesPerSample
        header[offset++] = (size and 0xFF).toByte()
        header[offset++] = (size shr 8 and 0xFF).toByte()
        header[offset++] = (size shr 16 and 0xFF).toByte()
        header[offset] = (size shr 24 and 0xFF).toByte()

        wavHeader = header
    }
}