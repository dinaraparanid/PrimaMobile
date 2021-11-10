package com.dinaraparanid.prima.core

import java.io.File
import java.io.FileFilter
import java.io.Serializable

/**
 * Entity for folders that are used in [com.dinaraparanid.prima.fragments.ChooseFolderFragment]
 */

class Folder private constructor(private val file: File) : Serializable {
    internal companion object {
        /**
         * Creates folder from file.
         * @param file from which folder should be created
         * @return [Folder] or null if file is not a directory
         */

        internal fun fromFile(file: File): Folder? = file.takeIf(File::isDirectory)?.let(::Folder)
    }

    internal val path
        get() = file.path

    internal inline val title
        @JvmName("title")
        get() = file.name

    internal inline val folders
        get() = file.listFiles(FileFilter(File::isDirectory))?.map(::Folder) ?: listOf()
}