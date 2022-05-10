package com.dinaraparanid.prima.core

import java.io.File
import java.io.FileFilter
import java.io.Serializable

/**
 * Entity for folders that are used in
 * [com.dinaraparanid.prima.fragments.ChooseFolderFragment]
 */

class Folder private constructor(private val file: File) : Serializable {
    internal companion object {
        /**
         * Creates folder from file.
         * @param file from which folder should be created
         * @return [Folder] or null if file is not a directory
         */

        internal fun fromFile(file: File) = file.takeIf(File::isDirectory)?.let(::Folder)
    }

    internal inline val path
        get() = file.path

    internal inline val title
        @JvmName("title")
        get() = file.name

    /**
     * Gets all sub folders of this folders
     * @return list of all sub folders
     */

    internal inline val folders
        get() = file.listFiles(FileFilter(File::isDirectory))?.map(::Folder) ?: listOf()

    /** Compares [Folder] by it's [path] */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return path == (other as Folder).path
    }

    /** Hashes [Folder] by it's [path] */
    override fun hashCode() = path.hashCode()
}