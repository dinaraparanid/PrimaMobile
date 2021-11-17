package com.dinaraparanid.prima.viewmodels.androidx

import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.utils.trimmer.soundfile.SoundFile
import kotlinx.coroutines.flow.MutableStateFlow

class TrimViewModel : ViewModel() {
    private val soundFileFlow = MutableStateFlow<SoundFile?>(null)
    private val startPosFlow = MutableStateFlow(0)
    private val endPosFlow = MutableStateFlow(0)

    internal var soundFile
        get() = soundFileFlow.value
        set(value) { soundFileFlow.value = value }

    internal var startPos
        get() = startPosFlow.value
        set(value) { startPosFlow.value = value }

    internal var endPos
        get() = endPosFlow.value
        set(value) { endPosFlow.value = value }

    /**
     * Loading params for the [com.dinaraparanid.prima.fragments.TrimFragment]
     * @param soundFile current [SoundFile] or null if it wasn't created
     */

    internal fun load(soundFile: SoundFile?, startPos: Int?, endPos: Int?) {
        soundFileFlow.value = soundFile
        startPosFlow.value = startPos ?: 0
        endPosFlow.value = endPos ?: 0
    }
}