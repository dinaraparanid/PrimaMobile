package com.dinaraparanid.prima.utils.trimmer.soundfile

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.toOption
import com.dinaraparanid.prima.utils.extensions.unwrap
import kotlin.experimental.or

internal class MP4Header private constructor(
    sampleRate: Int,
    numChannels: Int,
    frameSize: Option<IntArray>,
    bitrate: Int
) {
    /**
     * Size of each AAC frames, in bytes.
     * First one should be 2
     */

    private var frameSize: Option<IntArray> = None

    /** Size of the biggest frame */
    private var maxFrameSize = 0

    /** Size of the AAC stream */
    private var totSize = 0

    /** Bitrate used to encode the AAC stream */
    private var bitrate = 0

    /** Time used for 'creation time' and 'modification time' fields */
    private lateinit var time: ByteArray

    /** Duration of stream in milliseconds */
    private lateinit var durationMS: ByteArray

    /** Number of samples in the stream */
    private lateinit var numSamples: ByteArray

    /** The complete header */
    var mp4Header: Option<ByteArray> = None
        private set

    /** Sampling frequency in Hz (e.g. 44100) */
    private var sampleRate = 0

    /** Number of channels */
    private var channels: Int = 0

    /**
     * Creates a new MP4Header object that should be used
     * to generate an .m4a file header.
     */

    init {
        if (!(frameSize.isEmpty() || frameSize.unwrap().size < 2 || frameSize.unwrap()[0] != 2)) {
            this.sampleRate = sampleRate
            channels = numChannels
            this.frameSize = frameSize
            this.bitrate = bitrate
            maxFrameSize = this.frameSize.unwrap()[0]
            totSize = this.frameSize.unwrap()[0]

            (1 until this.frameSize.unwrap().size).forEach {
                if (maxFrameSize < this.frameSize.unwrap()[it])
                    maxFrameSize = this.frameSize.unwrap()[it]
                totSize += this.frameSize.unwrap()[it]
            }

            // Number of seconds between 1904 and 1970
            var time = System.currentTimeMillis() / 1000
            time += ((66 * 365 + 16) * 24 * 60 * 60).toLong()

            this.time = ByteArray(4)
            this.time[0] = (time shr 24 and 0xFF).toByte()
            this.time[1] = (time shr 16 and 0xFF).toByte()
            this.time[2] = (time shr 8 and 0xFF).toByte()
            this.time[3] = (time and 0xFF).toByte()

            // 1st frame does not contain samples.
            val numSamples = 1024 * (frameSize.unwrap().size - 1)
            var durationMS = numSamples * 1000 / this.sampleRate

            // Round the duration up.
            if (numSamples * 1000 % this.sampleRate > 0)
                durationMS++

            this.numSamples = byteArrayOf(
                (numSamples shr 26 and 0XFF).toByte(),
                (numSamples shr 16 and 0XFF).toByte(),
                (numSamples shr 8 and 0XFF).toByte(),
                (numSamples and 0XFF).toByte()
            )

            this.durationMS = byteArrayOf(
                (durationMS shr 26 and 0XFF).toByte(),
                (durationMS shr 16 and 0XFF).toByte(),
                (durationMS shr 8 and 0XFF).toByte(),
                (durationMS and 0XFF).toByte()
            )

            setHeader()
        }
    }

    internal companion object {
        @JvmStatic
        internal fun getInstanceAsBytes(
            sampleRate: Int,
            numChannels: Int,
            frameSize: Option<IntArray>,
            bitrate: Int
        ) = MP4Header(sampleRate, numChannels, frameSize, bitrate).mp4Header
    }

    private fun setHeader() {
        // Create the atoms needed to build the header.
        val aFtyp = fTYPAtom
        val aMoov = oovAtom

        // Create an empty atom. The AAC stream data should follow
        // immediately after. The correct size will be set later.
        val aMdat = Atom("mdat")

        // Set the correct chunk offset in the stco atom.
        val aStco = aMoov.getChild("trak.mdia.minf.stbl.stco")

        if (aStco.isEmpty()) {
            mp4Header = None
            return
        }

        val data = aStco.unwrap().data.unwrap()
        val chunkOffset = aFtyp.size + aMoov.size + aMdat.size
        var offset = data.size - 4 // here stco should contain only one chunk offset.

        data[offset++] = (chunkOffset shr 24 and 0xFF).toByte()
        data[offset++] = (chunkOffset shr 16 and 0xFF).toByte()
        data[offset++] = (chunkOffset shr 8 and 0xFF).toByte()
        data[offset++] = (chunkOffset and 0xFF).toByte()

        // Create the header byte array based on the previous atoms.
        // Here chunkOffset is also the size of the header

        val header = ByteArray(chunkOffset)
        offset = 0

        arrayOf(aFtyp, aMoov, aMdat).forEach {
            val atomBytes = it.bytes
            System.arraycopy(atomBytes, 0, header, offset, atomBytes.size)
            offset += atomBytes.size
        }

        // Set the correct size of the mdat atom
        val size = 8 + totSize
        offset -= 8

        header[offset++] = (size shr 24 and 0xFF).toByte()
        header[offset++] = (size shr 16 and 0xFF).toByte()
        header[offset++] = (size shr 8 and 0xFF).toByte()
        header[offset++] = (size and 0xFF).toByte()

        mp4Header = Some(header)
    }

    /**
     * Major brand.
     * Minor version.
     * Compatible brands
     */

    private inline val fTYPAtom: Atom
        get() = Atom("ftyp").apply {
            setData(
                byteArrayOf(
                    'M'.code.toByte(),
                    '4'.code.toByte(),
                    'A'.code.toByte(),
                    ' '.code.toByte(),  // Major brand
                    0, 0, 0, 0,         // Minor version
                    'M'.code.toByte(),
                    '4'.code.toByte(),
                    'A'.code.toByte(),
                    ' '.code.toByte(),  // Compatible brands
                    'm'.code.toByte(),
                    'p'.code.toByte(),
                    '4'.code.toByte(),
                    '2'.code.toByte(),
                    'i'.code.toByte(),
                    's'.code.toByte(),
                    'o'.code.toByte(),
                    'm'.code.toByte()
                ).toOption()
            )
        }

    private inline val oovAtom: Atom
        get() = Atom("moov").apply {
            addChild(vhdAtom.toOption())
            addChild(trakAtom.toOption())
        }

    @Suppress("Reformat")
    private inline val vhdAtom: Atom
        get() = Atom("mvhd", 0.toByte(), 0).apply {
            setData(
                byteArrayOf(
                    time[0], time[1], time[2], time[3],     // creation time.
                    time[0], time[1], time[2], time[3],     // modification time.
                    0, 0, 0x03, 0xE8.toByte(),              // timescale = 1000 => duration expressed in ms.
                    durationMS[0], durationMS[1],
                    durationMS[2], durationMS[3],           // duration in ms.
                    0, 1, 0, 0,                             // rate = 1.0
                    1, 0,                                   // volume = 1.0
                    0, 0,                                   // reserved
                    0, 0, 0, 0,                             // reserved
                    0, 0, 0, 0,                             // reserved
                    0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,     // unity matrix
                    0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0,
                    0, 0x40, 0, 0, 0, 0, 0, 0, 0,           // pre-defined
                    0, 0, 0, 0,                             // pre-defined
                    0, 0, 0, 0,                             // pre-defined
                    0, 0, 0, 0,                             // pre-defined
                    0, 0, 0, 0,                             // pre-defined
                    0, 0, 0, 0,                             // pre-defined
                    0, 0, 0, 2                              // next track ID
                ).toOption()
            )
        }

    private inline val trakAtom
        get() = Atom("trak").apply {
            addChild(tkhdAtom.toOption())
            addChild(mdiaAtom.toOption())
        }

    private inline val tkhdAtom
        get() = Atom("tkhd", 0.toByte(), 0x07).apply {
            setData(
                byteArrayOf(
                    time[0], time[1], time[2], time[3], // creation time.
                    time[0], time[1], time[2], time[3], // modification time.
                    0, 0, 0, 1,                         // track ID
                    0, 0, 0, 0,                         // reserved
                    durationMS[0], durationMS[1],
                    durationMS[2], durationMS[3],       // duration in ms.
                    0, 0, 0, 0,                         // reserved
                    0, 0, 0, 0,                         // reserved
                    0, 0,                               // layer 0,
                    0,                                  // alternate group
                    1, 0,                               // volume = 1.0
                    0, 0,                               // reserved
                    0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // unity matrix
                    0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0x40, 0, 0,
                    0, 0, 0, 0, 0,                      // width 0,
                    0, 0, 0                             // height
                ).toOption()
            )
        }

    private inline val mdiaAtom
        get() = Atom("mdia").apply {
            addChild(mdhdAtom.toOption())
            addChild(hdlrAtom.toOption())
            addChild(minfAtom.toOption())
        }

    private inline val mdhdAtom
        get() = Atom("mdhd", 0.toByte(), 0).apply {
            setData(
                byteArrayOf(
                    time[0], time[1], time[2], time[3],  // creation time.
                    time[0], time[1], time[2], time[3],  // modification time.
                    (sampleRate shr 24).toByte(),
                    (sampleRate shr 16).toByte(),       // timescale = Fs =>
                    (sampleRate shr 8).toByte(),
                    sampleRate.toByte(),                // duration expressed in samples.
                    numSamples[0], numSamples[1],
                    numSamples[2], numSamples[3],       // duration
                    0, 0,                               // languages
                    0, 0                                // pre-defined
                ).toOption()
            )
        }

    private inline val hdlrAtom
        get() = Atom("hdlr", 0.toByte(), 0).apply {
            setData(
                byteArrayOf(
                    0, 0, 0, 0,         // pre-defined
                    's'.code.toByte(),
                    'o'.code.toByte(),
                    'u'.code.toByte(),
                    'n'.code.toByte(),  // handler type
                    0, 0, 0, 0,         // reserved
                    0, 0, 0, 0,         // reserved
                    0, 0, 0, 0,         // reserved
                    'S'.code.toByte(),
                    'o'.code.toByte(),
                    'u'.code.toByte(),
                    'n'.code.toByte(),  // name (used only for debugging and inspection purposes).
                    'd'.code.toByte(),
                    'H'.code.toByte(),
                    'a'.code.toByte(),
                    'n'.code.toByte(),
                    'd'.code.toByte(),
                    'l'.code.toByte(),
                    'e'.code.toByte(),
                    '\u0000'.code.toByte()
                ).toOption()
            )
        }

    private inline val minfAtom
        get() = Atom("minf").apply {
            addChild(smhdAtom.toOption())
            addChild(dinfAtom.toOption())
            addChild(stblAtom.toOption())
        }

    private inline val smhdAtom
        get() = Atom("smhd", 0.toByte(), 0).apply {
            setData(
                byteArrayOf(
                    0, 0,   // balance (center)
                    0, 0    // reserved
                ).toOption()
            )
        }

    private inline val dinfAtom
        get() = Atom("dinf").apply { addChild(drefAtom.toOption()) }

    private inline val drefAtom: Atom
        get() {
            val atom = Atom("dref", 0.toByte(), 0)
            val url = urlAtom.bytes
            val data = ByteArray(4 + url.size)
            data[3] = 0x01 // entry count = 1
            System.arraycopy(url, 0, data, 4, url.size)
            atom.setData(data.toOption())
            return atom
        }

    private inline val urlAtom
        get() = Atom("url ", 0.toByte(), 0x01) // flags = 0x01: data is self contained.

    private inline val stblAtom
        get() = Atom("stbl").apply {
            addChild(stsdAtom.toOption())
            addChild(sttsAtom.toOption())
            addChild(stscAtom.toOption())
            addChild(stszAtom.toOption())
            addChild(stcoAtom.toOption())
        }

    private inline val stsdAtom: Atom
        get() {
            val atom = Atom("stsd", 0.toByte(), 0)
            val mp4a = mp4aAtom.bytes
            val data = ByteArray(4 + mp4a.size)
            data[3] = 0x01 // entry count = 1
            System.arraycopy(mp4a, 0, data, 4, mp4a.size)
            atom.setData(data.toOption())
            return atom
        }

    /** See also Part 14 section 5.6.1 of ISO/IEC 14496 for this atom */
    private inline val mp4aAtom: Atom
        get() {
            val atom = Atom("mp4a")

            // Audio Sample Entry data
            val ase = byteArrayOf(
                0, 0, 0, 0, 0, 0,                               // reserved
                0, 1,                                           // data reference index
                0, 0, 0, 0,                                     // reserved
                0, 0, 0, 0,                                     // reserved
                (channels shr 8).toByte(), channels.toByte(),   // channel count
                0, 0x10,                                        // sample size
                0, 0,                                           // pre-defined
                0, 0,                                           // reserved
                (sampleRate shr 8).toByte(), sampleRate.toByte(), 0, 0
            )

            val esds = esdsAtom.bytes
            val data = ByteArray(ase.size + esds.size)
            System.arraycopy(ase, 0, data, 0, ase.size)
            System.arraycopy(esds, 0, data, ase.size, esds.size)
            atom.setData(data.toOption())
            return atom
        }

    private inline val esdsAtom
        get() = Atom("esds", 0.toByte(), 0).apply { setData(esDescriptor.toOption()) }

    /**
     * @return an ES Descriptor for an ISO/IEC 14496-3 audio stream,
     * AAC LC, 44100Hz, 2 channels, 1024 samples per frame per channel.
     * The decoder buffer size is set so that it can contain at least 2 frames
     */

    private inline val esDescriptor: ByteArray
        get() {
            val samplingFrequencies = intArrayOf(
                96000, 88200, 64000, 48000, 44100, 32000, 24000,
                22050, 16000, 12000, 11025, 8000, 7350
            )

            // First 5 bytes of the ES Descriptor
            val esDescriptorTop: ByteArray = byteArrayOf(0x03, 0x19, 0x00, 0x00, 0x00)

            // First 4 bytes of Decoder Configuration Descriptor. Audio ISO/IEC 14496-3, AudioStream
            val decConfigDescriptorTop = byteArrayOf(0x04, 0x11, 0x40, 0x15)

            // Audio Specific Configuration: AAC LC, 1024 samples/frame/channel
            val audioSpecificConfig = byteArrayOf(0x05, 0x02, 0x10, 0x00)

            // Specific for MP4 file.
            val slConfigDescriptor = byteArrayOf(0x06, 0x01, 0x02)

            var bufferSize = 0x300

            while (bufferSize < 2 * maxFrameSize)
                bufferSize += 0x100

            // create the Decoder Configuration Descriptor
            val decConfigDescriptor = ByteArray(2 + decConfigDescriptorTop[1])

            System.arraycopy(
                decConfigDescriptorTop,
                0,
                decConfigDescriptor,
                0,
                decConfigDescriptorTop.size
            )

            var offset = decConfigDescriptorTop.size
            decConfigDescriptor[offset++] = (bufferSize shr 16 and 0xFF).toByte()
            decConfigDescriptor[offset++] = (bufferSize shr 8 and 0xFF).toByte()
            decConfigDescriptor[offset++] = (bufferSize and 0xFF).toByte()
            decConfigDescriptor[offset++] = (bitrate shr 24 and 0xFF).toByte()
            decConfigDescriptor[offset++] = (bitrate shr 16 and 0xFF).toByte()
            decConfigDescriptor[offset++] = (bitrate shr 8 and 0xFF).toByte()
            decConfigDescriptor[offset++] = (bitrate and 0xFF).toByte()
            decConfigDescriptor[offset++] = (bitrate shr 24 and 0xFF).toByte()
            decConfigDescriptor[offset++] = (bitrate shr 16 and 0xFF).toByte()
            decConfigDescriptor[offset++] = (bitrate shr 8 and 0xFF).toByte()
            decConfigDescriptor[offset++] = (bitrate and 0xFF).toByte()

            var index: Int
            index = 0

            while (index < samplingFrequencies.size) {
                if (samplingFrequencies[index] == sampleRate)
                    break
                index++
            }

            if (index == samplingFrequencies.size)
                index = 4

            audioSpecificConfig[2] = audioSpecificConfig[2] or (index shr 1 and 0x07).toByte()
            audioSpecificConfig[3] = audioSpecificConfig[3] or
                    (index and 1 shl 7 or (channels and 0x0F shl 3)).toByte()

            System.arraycopy(
                audioSpecificConfig,
                0,
                decConfigDescriptor,
                offset,
                audioSpecificConfig.size
            )

            // create the ES Descriptor
            val esDescriptor = ByteArray(2 + esDescriptorTop[1])
            System.arraycopy(esDescriptorTop, 0, esDescriptor, 0, esDescriptorTop.size)
            offset = esDescriptorTop.size
            System.arraycopy(decConfigDescriptor, 0, esDescriptor, offset, decConfigDescriptor.size)
            offset += decConfigDescriptor.size
            System.arraycopy(slConfigDescriptor, 0, esDescriptor, offset, slConfigDescriptor.size)
            return esDescriptor
        }

    private inline val sttsAtom: Atom
        get() {
            val atom = Atom("stts", 0.toByte(), 0)
            val numAudioFrames = frameSize.unwrap().size - 1
            atom.setData(
                byteArrayOf(
                    0, 0, 0, 0x02,  // entry count
                    0, 0, 0, 0x01,  // first frame contains no audio
                    0, 0, 0, 0,
                    (numAudioFrames shr 24 and 0xFF).toByte(),
                    (numAudioFrames shr 16 and 0xFF).toByte(),
                    (numAudioFrames shr 8 and 0xFF).toByte(),
                    (numAudioFrames and 0xFF).toByte(),
                    0, 0, 0x04, 0
                ).toOption()
            )
            return atom
        }

    private inline val stscAtom: Atom
        get() {
            val atom = Atom("stsc", 0.toByte(), 0)
            val numFrames = frameSize.unwrap().size

            atom.setData(
                byteArrayOf(
                    0, 0, 0, 0x01,                          // entry count
                    0, 0, 0, 0x01,                          // first chunk
                    (numFrames shr 24 and 0xFF).toByte(),
                    (numFrames shr 16 and 0xFF).toByte(),   // samples per
                    (numFrames shr 8 and 0xFF).toByte(),
                    (numFrames and 0xFF).toByte(),          // chunk
                    0, 0, 0, 0x01
                ).toOption()
            )
            return atom
        }

    private inline val stszAtom: Atom
        get() {
            val atom = Atom("stsz", 0.toByte(), 0)
            val numFrames = frameSize.unwrap().size
            val data = ByteArray(8 + 4 * numFrames)
            var offset = 0

            // Sample size (=0 => each frame can have a different size)

            data[offset++] = 0
            data[offset++] = 0
            data[offset++] = 0
            data[offset++] = 0

            // Sample count

            data[offset++] = (numFrames shr 24 and 0xFF).toByte()
            data[offset++] = (numFrames shr 16 and 0xFF).toByte()
            data[offset++] = (numFrames shr 8 and 0xFF).toByte()
            data[offset++] = (numFrames and 0xFF).toByte()

            frameSize.unwrap().forEach {
                data[offset++] = (it shr 24 and 0xFF).toByte()
                data[offset++] = (it shr 16 and 0xFF).toByte()
                data[offset++] = (it shr 8 and 0xFF).toByte()
                data[offset++] = (it and 0xFF).toByte()
            }

            atom.setData(data.toOption())
            return atom
        }

    private inline val stcoAtom
        get() = Atom("stco", 0.toByte(), 0).apply {
            setData(
                byteArrayOf(
                    0, 0, 0, 0x01,
                    0, 0, 0, 0
                ).toOption()
            )
        }
}