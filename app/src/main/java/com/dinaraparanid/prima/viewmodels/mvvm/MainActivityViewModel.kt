package com.dinaraparanid.prima.viewmodels.mvvm

import android.widget.FrameLayout
import com.dinaraparanid.prima.utils.Params

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.MainActivity]
 */

class MainActivityViewModel : ViewModel() {
    internal inline val isBarVisualizerStyle
        @JvmName("isBarVisualizerStyle")
        get() = when (params.visualizerStyle) {
            Params.Companion.VisualizerStyle.BAR -> FrameLayout.VISIBLE
            else -> FrameLayout.GONE
        }

    internal inline val isWaveVisualizerStyle
        @JvmName("isWaveVisualizerStyle")
        get() = when (params.visualizerStyle) {
            Params.Companion.VisualizerStyle.WAVE -> FrameLayout.VISIBLE
            else -> FrameLayout.GONE
        }
}