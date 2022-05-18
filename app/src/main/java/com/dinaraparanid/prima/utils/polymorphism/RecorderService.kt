package com.dinaraparanid.prima.utils.polymorphism

import com.dinaraparanid.prima.utils.Params
import java.io.File

/** Ancestor of the recording services */
abstract class RecorderService : AbstractService(), StatisticsUpdatable {
    /**
     * Gets first filename without repeats.
     * For example, if you already have test.mp3, it will return test(1).mp3
     * @param filename file name that will be converted to new file name
     * @return filename that doesn't already exist
     */

    internal fun getNewMP3FileNameAsync(filename: String) = getFromIOThreadAsync {
        val pathToSave = Params.getInstanceSynchronized().pathToSave

        generateSequence(0) { it + 1 }
            .map { if (it == 0) filename else "$filename($it)" }
            .first { !File("$pathToSave/$it.mp3").exists() }
    }
}