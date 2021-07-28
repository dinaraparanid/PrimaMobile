package com.dinaraparanid.prima.utils.equalizer

import java.io.Serializable

/**
 * Equalizer params
 */

internal class EqualizerModel : Serializable {
    internal var isEqualizerEnabled = true
    internal var seekbarPos = IntArray(5)
    internal var presetPos = 0
    internal var reverbPreset: Short = -1
    internal var bassStrength: Short = -1
}