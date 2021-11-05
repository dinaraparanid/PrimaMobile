package com.dinaraparanid.prima.utils.trimmer.soundfile

internal class Atom {
    /** Includes atom header (8 bytes) */
    internal var size: Int
        private set

    private var typeInt: Int

    /** An atom can either contain data or children, but not both. */
    internal var data: ByteArray? = null
        private set

    private var children: Array<Atom?>? = null

    /** if negative, then the atom does not contain version and flags data. */
    private var version: Byte

    private var flags: Int

    /** Create an empty atom of the given type */
    internal constructor(type: String) {
        size = 8
        typeInt = getTypeInt(type)
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
            data != null -> size += data!!.size
            children != null -> children!!.forEach { size += it!!.size }
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

    internal fun setData(data: ByteArray?): Boolean {
        if (children != null || data == null)
            return false

        this.data = data
        setSize()
        return true
    }

    internal fun addChild(child: Atom?): Boolean {
        if (data != null || child == null)
            return false

        var numChildren = 1

        if (children != null)
            numChildren += children!!.size

        val children = arrayOfNulls<Atom>(numChildren)

        if (this.children != null)
            System.arraycopy(
                this.children!!,
                0,
                children,
                0,
                this.children!!.size
            )

        children[numChildren - 1] = child
        this.children = children
        setSize()
        return true
    }

    /**
     * Gets the child atom of the corresponding type.
     * @return the child atom of the corresponding type.
     * Type can contain grand children: e.g. type = "trak.mdia.minf"
     * Return null if the atom does not contain such a child.
     */

    internal fun getChild(type: String): Atom? {
        if (children == null)
            return null

        val types = type.split("\\.".toRegex(), 2).toTypedArray()

        return children!!
            .firstOrNull { it!!.typeStr == types[0] }
            ?.let {
                when (types.size) {
                    1 -> it
                    else -> it.getChild(types[1])
                }
            }
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
                data != null -> System.arraycopy(data!!, 0, atomBytes, offset, data!!.size)

                children != null -> {
                    var childBytes: ByteArray

                    children!!.forEach {
                        childBytes = it!!.bytes
                        System.arraycopy(childBytes, 0, atomBytes, offset, childBytes.size)
                        offset += childBytes.size
                    }
                }
            }

            return atomBytes
        }
}