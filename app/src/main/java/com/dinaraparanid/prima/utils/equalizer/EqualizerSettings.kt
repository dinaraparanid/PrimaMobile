package com.dinaraparanid.prima.utils.equalizer

import com.dinaraparanid.prima.utils.StorageUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Equalizer settings */

internal class EqualizerSettings private constructor() : CoroutineScope by MainScope() {
    internal companion object {
        private var INSTANCE: EqualizerSettings? = null
        private val mutex = Mutex()

        /** Initializes settings only once */

        internal fun initialize() {
            if (INSTANCE == null)
                INSTANCE = EqualizerSettings().apply {
                    StorageUtil.instance.run {
                        launch {
                            loadEqualizerSeekbarsPosLocking()?.let { seekbarPos = it }
                            presetPos = loadPresetPosLocking()
                            reverbPreset = loadReverbPresetLocking()
                            bassStrength = loadBassStrengthLocking()
                        }
                    }
                }
        }

        /** Gets instance of [EqualizerSettings] */

        @JvmStatic
        internal val instance
            @JvmName("getInstance")
            get() = INSTANCE
                ?: throw UninitializedPropertyAccessException("EqualizerSettings is not initialized")

        /** Gets instance of [EqualizerSettings] with [Mutex] protection */

        @JvmStatic
        internal suspend fun getInstanceSynchronized(isLocked: Boolean) = when {
            isLocked -> mutex.withLock { instance }
            else -> instance
        }
    }

    @Volatile
    internal var isEqualizerEnabled = false
        @JvmName("isEqualizerEnabled") get

    @Volatile
    internal var isEqualizerReloaded = true

    internal var seekbarPos = IntArray(5)
    internal var presetPos = 0
    internal var reverbPreset: Short = -1
    internal var bassStrength: Short = -1
    internal var equalizerModel: EqualizerModel? = null
    internal var ratio = 1.0
    internal var isEditing = false
}