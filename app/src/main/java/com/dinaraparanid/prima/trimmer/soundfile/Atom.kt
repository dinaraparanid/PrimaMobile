package com.dinaraparanid.prima.trimmer.soundfile

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.toOption
import com.dinaraparanid.prima.utils.extensions.unwrap

internal class Atom {
    /** Includes atom header (8 bytes) */
    internal var size: Int
        private set

    private var typeInt: Int

    /** An atom can either contain data or children, but not both. */
    internal var data: Option<ByteArray>
        private set

    private var children: Option<Array<Option<Atom>>>

    /** if negative, then the atom does not contain version and flags data. */
    private var version: Byte

    private var flags: Int

    /** Create an empty atom of the given type */
    internal constructor(type: String) {
        size = 8
        typeInt = getTypeInt(type)
        data = None
        children = None
        version = -1
        flags = 0
    }

    /**
     * Create an empty atom of type type,
     * with a given version and flags.
     */

    internal constructor(type: String, version: Byte, flags: Int) {
        size = 12
        typeInt = getTypeInt(type)
        data = None
        children = None
        this.version = version
        this.flags = flags
    }

    /**
     * Set the size field of the atom
     * based on its content.
     */

    private fun setSize() {
        var size = 8 // type + size

        if (version >= 0)
            size += 4 // version + flags

        when {
            data.isNotEmpty() -> size += data.unwrap().size

            children.isNotEmpty() ->
                children.unwrap().forEach {
                    size += it.unwrap().size
                }
        }

        this.size = size
    }

    private fun getTypeInt(typeStr: String): Int {
        var type = 0
        type = type or (typeStr[0].code shl 24)
        type = type or (typeStr[1].code shl 16)
        type = type or (typeStr[2].code shl 8)
        type = type or typeStr[3].code
        return type
    }

    private val typeStr: String
        get() {
            var type = ""
            type += (typeInt shr 24 and 0xFF).toChar()
            type += (typeInt shr 16 and 0xFF).toChar()
            type += (typeInt shr 8 and 0xFF).toChar()
            type += (typeInt and 0xFF).toChar()
            return type
        }

    internal fun setData(data: Option<ByteArray>): Boolean {
        if (children.isNotEmpty() || data.isEmpty())
            return false

        this.data = data
        setSize()
        return true
    }

    internal fun addChild(child: Option<Atom>): Boolean {
        if (data.isNotEmpty() || child.isEmpty())
            return false

        var numChildren = 1

        if (children.isNotEmpty())
            numChildren += children.unwrap().size

        val children = arrayOfNulls<Atom>(numChildren)

        if (this.children.isNotEmpty())
            System.arraycopy(
                this.children,
                0,
                children,
                0,
                this.children.unwrap().size
            )

        children[numChildren - 1] = child.unwrap()
        this.children = Some(children.map(Atom?::toOption).toTypedArray())
        setSize()
        return true
    }

    /**
     * Gets the child atom of the corresponding type.
     * @return the child atom of the corresponding type.
     * Type can contain grand children: e.g. type = "trak.mdia.minf"
     * Return null if the atom does not contain such a child.
     */

    internal fun getChild(type: String): Option<Atom> {
        if (children.isEmpty())
            return None

        val types = type.split("\\.".toRegex(), 2).toTypedArray()

        return children.unwrap()
            .firstOrNull { it.unwrap().typeStr == types[0] }
            ?.let {
                when (types.size) {
                    1 -> it
                    else -> it.unwrap().getChild(types[1])
                }
            } ?: None
    }

    /**
     * @return a byte array containing
     * the full content of the atom (including header)
     */

    internal val bytes: ByteArray
        get() {
            val atomBytes = ByteArray(size)
            var offset = 0

            atomBytes[offset++] = (size shr 24 and 0xFF).toByte()
            atomBytes[offset++] = (size shr 16 and 0xFF).toByte()
            atomBytes[offset++] = (size shr 8 and 0xFF).toByte()
            atomBytes[offset++] = (size and 0xFF).toByte()
            atomBytes[offset++] = (typeInt shr 24 and 0xFF).toByte()
            atomBytes[offset++] = (typeInt shr 16 and 0xFF).toByte()
            atomBytes[offset++] = (typeInt shr 8 and 0xFF).toByte()
            atomBytes[offset++] = (typeInt and 0xFF).toByte()

            if (version >= 0) {
                atomBytes[offset++] = version
                atomBytes[offset++] = (flags shr 16 and 0xFF).toByte()
                atomBytes[offset++] = (flags shr 8 and 0xFF).toByte()
                atomBytes[offset++] = (flags and 0xFF).toByte()
            }

            when {
                data.isNotEmpty() ->
                    System.arraycopy(data, 0, atomBytes, offset, data.unwrap().size)

                children.isNotEmpty() -> {
                    var childBytes: ByteArray

                    children.unwrap().forEach {
                        childBytes = it.unwrap().bytes
                        System.arraycopy(childBytes, 0, atomBytes, offset, childBytes.size)
                        offset += childBytes.size
                    }
                }
            }

            return atomBytes
        }
}