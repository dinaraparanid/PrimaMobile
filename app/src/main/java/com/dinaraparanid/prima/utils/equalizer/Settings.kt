package com.dinaraparanid.prima.utils.equalizer

/**
 * Equalizer settings
 */

internal class Settings {
    internal companion object {
        @JvmStatic
        internal var isEqualizerEnabled = true

        @JvmStatic
        internal var isEqualizerReloaded = true

        @JvmStatic
        internal var seekbarpos = IntArray(5)

        @JvmStatic
        internal var presetPos = 0

        @JvmStatic
        internal var reverbPreset: Short = -1

        @JvmStatic
        internal var bassStrength: Short = -1

        @JvmStatic
        internal var equalizerModel: EqualizerModel? = null

        @JvmStatic
        internal var ratio = 1.0

        @JvmStatic
        internal var isEditing = false
    }
}