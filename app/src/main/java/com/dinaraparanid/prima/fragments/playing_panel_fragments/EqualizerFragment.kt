package com.dinaraparanid.prima.fragments.playing_panel_fragments

import android.annotation.SuppressLint
import android.graphics.Paint
import android.media.PlaybackParams
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import arrow.core.None
import arrow.core.Some
import com.db.chart.model.LineSet
import com.db.chart.view.AxisController
import com.db.chart.view.ChartView
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentEqualizerBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.equalizer.EqualizerSettings
import com.dinaraparanid.prima.utils.extensions.playbackParam
import com.dinaraparanid.prima.utils.polymorphism.AsyncContext
import com.dinaraparanid.prima.utils.polymorphism.fragments.MainActivitySimpleFragment
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import com.dinaraparanid.prima.utils.polymorphism.runOnWorkerThread
import com.dinaraparanid.prima.viewmodels.mvvm.EqualizerViewModel
import kotlinx.coroutines.CoroutineScope
import java.lang.ref.WeakReference

/** Equalizer Fragment to modify audio's params */

internal class EqualizerFragment :
    MainActivitySimpleFragment<FragmentEqualizerBinding>(),
    AsyncContext {
    private lateinit var paint: Paint
    private lateinit var dataset: LineSet
    private lateinit var points: FloatArray

    override var binding: FragmentEqualizerBinding? = null

    private var seekBarFinal = arrayOfNulls<SeekBar>(5)
    private var numberOfFrequencyBands: Short = 0
    private var y = 0

    internal companion object {
        private const val ARG_AUDIO_SESSION_ID = "audio_session_id"

        /**
         * Creates new instance of [EqualizerFragment]
         * @param audioSessionId id of audio session
         */

        @JvmStatic
        internal fun newInstance(audioSessionId: Int) = EqualizerFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_AUDIO_SESSION_ID, audioSessionId)
            }
        }
    }

    override val coroutineScope: CoroutineScope
        get() = lifecycleScope

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelCurText = resources.getString(R.string.equalizer)
        setMainLabelInitialized()
        super.onCreate(savedInstanceState)

        application.run {
            when (val r = tryWithAudioEffectOrStop(this::startEqualizer)) {
                None -> return
                is Some -> Unit
            }

            equalizer.enabled = true
            bassBoost?.enabled = true
            presetReverb?.enabled = true
            EqualizerSettings.instance.isEqualizerEnabled = true
            EqualizerSettings.instance.equalizerModel!!.isEqualizerEnabled = true
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val themeColor = Params.instance.primaryColor

        binding = DataBindingUtil
            .inflate<FragmentEqualizerBinding>(
                inflater,
                R.layout.fragment_equalizer,
                container,
                false
            )
            .apply {
                viewModel = EqualizerViewModel(WeakReference(fragmentActivity))

                equalizerSwitch.trackTintList = ViewSetter.colorStateList
                spinnerDropdownIcon.setOnClickListener { equalizerPresetSpinner.performClick() }

                paint = Paint()
                dataset = LineSet()

                runOnIOThread {
                    val pit = StorageUtil.getInstanceSynchronized().loadPitchAsyncLocking()

                    // Sets pitch status level

                    pitchStatus?.run {
                        setText(pit.toString().take(4))

                        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q)
                            addTextChangedListener(object : TextWatcher {
                                override fun beforeTextChanged(
                                    s: CharSequence?,
                                    start: Int, count: Int, after: Int
                                ) = Unit

                                override fun onTextChanged(
                                    s: CharSequence?,
                                    start: Int, before: Int, count: Int
                                ) = Unit

                                override fun afterTextChanged(s: Editable?) {
                                    if (EqualizerSettings.instance.isEqualizerEnabled) {
                                        val newPitch = s?.toString()?.toFloatOrNull()?.playbackParam
                                            ?: (pitchSeekBar!!.progress.toFloat() + 50) / 100

                                        val isPlaying = application.musicPlayer!!.isPlaying

                                        if (!isPlaying) runOnWorkerThread {
                                            fragmentActivity.reinitializePlayingCoroutine(isLocking = true)
                                        }

                                        if (isPlaying)
                                            application.musicPlayer!!.run {
                                                playbackParams = PlaybackParams()
                                                    .setSpeed(playbackParams.speed)
                                                    .setPitch(newPitch)
                                            }

                                        pitchSeekBar!!.progress = (newPitch * 100 - 50).toInt()

                                        runOnIOThread {
                                            if (Params.getInstanceSynchronized().isSavingEqualizerSettings)
                                                StorageUtil
                                                    .getInstanceSynchronized()
                                                    .storePitchLocking(newPitch)
                                        }
                                    }
                                }
                            })
                    }

                    // Sets UI for the pitch seek bar

                    pitchSeekBar?.run {
                        setBackgroundResource(viewModel!!.trackType)
                        progress = ((pit - 0.5F) * 100).toInt()
                        var newPitch = 0F

                        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q)
                            setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                                override fun onProgressChanged(
                                    seekBar: SeekBar?,
                                    progress: Int,
                                    fromUser: Boolean
                                ) {
                                    if (EqualizerSettings.instance.isEqualizerEnabled) {
                                        val speed = application.musicPlayer!!.playbackParams.speed
                                        newPitch = 0.5F + progress * 0.01F

                                        try {
                                            val isPlaying = application.musicPlayer!!.isPlaying

                                            application.musicPlayer!!.playbackParams =
                                                PlaybackParams()
                                                    .setSpeed(speed)
                                                    .setPitch(newPitch)

                                            if (!isPlaying) runOnWorkerThread {
                                                fragmentActivity.reinitializePlayingCoroutine(isLocking = true)
                                            }
                                        } catch (ignored: Exception) {
                                            // old or weak phone
                                        }
                                    }
                                }

                                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                                    try {
                                        application.musicPlayer!!.playbackParams = PlaybackParams()
                                            .setSpeed(application.musicPlayer!!.playbackParams.speed)
                                            .setPitch(newPitch)
                                    } catch (e: Exception) {
                                        progress = 50

                                        Toast.makeText(
                                            requireContext(),
                                            R.string.not_supported,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                    pitchStatus!!.setText(newPitch.toString().take(4))

                                    runOnIOThread {
                                        if (Params.getInstanceSynchronized().isSavingEqualizerSettings)
                                            StorageUtil.getInstanceSynchronized().storePitchLocking(newPitch)
                                    }
                                }
                            })
                    }
                }

                runOnIOThread {
                    // Sets playback's speed status level

                    val speed = StorageUtil.getInstanceSynchronized().loadSpeedAsyncLocking()

                    speedStatus?.run {
                        setText(speed.toString().take(4))

                        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q)
                            addTextChangedListener(object : TextWatcher {
                                override fun beforeTextChanged(
                                    s: CharSequence?,
                                    start: Int, count: Int, after: Int
                                ) = Unit

                                override fun onTextChanged(
                                    s: CharSequence?,
                                    start: Int, before: Int, count: Int
                                ) = Unit

                                override fun afterTextChanged(s: Editable?) {
                                    if (EqualizerSettings.instance.isEqualizerEnabled) {
                                        val newSpeed = s?.toString()?.toFloatOrNull()?.playbackParam
                                            ?: (speedSeekBar!!.progress.toFloat() + 50) / 100

                                        val isPlaying = application.musicPlayer!!.isPlaying

                                        if (!isPlaying) runOnWorkerThread {
                                            fragmentActivity.reinitializePlayingCoroutine(isLocking = true)
                                        }

                                        if (isPlaying)
                                            application.musicPlayer!!.run {
                                                playbackParams = PlaybackParams()
                                                    .setPitch(playbackParams.pitch)
                                                    .setSpeed(newSpeed)
                                            }

                                        speedSeekBar!!.progress = (newSpeed * 100 - 50).toInt()

                                        runOnIOThread {
                                            if (Params.getInstanceSynchronized().isSavingEqualizerSettings)
                                                StorageUtil.getInstanceSynchronized().storeSpeedLocking(newSpeed)
                                        }
                                    }
                                }
                            })
                    }

                    // Sets UI for the speed seek bar

                    speedSeekBar?.run {
                        setBackgroundResource(viewModel!!.trackType)
                        progress = ((speed - 0.5F) * 100).toInt()
                        var newSpeed = 0F

                        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q)
                            setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                                override fun onProgressChanged(
                                    seekBar: SeekBar?,
                                    progress: Int,
                                    fromUser: Boolean
                                ) {
                                    if (EqualizerSettings.instance.isEqualizerEnabled) {
                                        val pitch = application.musicPlayer!!.playbackParams.pitch
                                        newSpeed = 0.5F + progress * 0.01F

                                        try {
                                            val isPlaying = application.musicPlayer!!.isPlaying

                                            if (isPlaying)
                                                application.musicPlayer!!.playbackParams =
                                                    PlaybackParams()
                                                        .setPitch(pitch)
                                                        .setSpeed(newSpeed)

                                            if (!isPlaying) runOnWorkerThread {
                                                fragmentActivity.reinitializePlayingCoroutine(isLocking = true)
                                            }
                                        } catch (ignored: Exception) {
                                            // old or weak phone
                                        }
                                    }
                                }

                                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                                    try {
                                        application.musicPlayer!!.playbackParams = PlaybackParams()
                                            .setSpeed(newSpeed)
                                            .setPitch(application.musicPlayer!!.playbackParams.pitch)
                                    } catch (e: Exception) {
                                        progress = 50

                                        Toast.makeText(
                                            requireContext(),
                                            R.string.not_supported,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                    speedStatus!!.setText(newSpeed.toString().take(4))

                                    runOnIOThread {
                                        if (Params.getInstanceSynchronized().isSavingEqualizerSettings)
                                            StorageUtil.getInstanceSynchronized().storeSpeedLocking(newSpeed)
                                    }
                                }
                            })
                    }
                }

                // Sets UI for the bass controller

                controllerBass.run {
                    label = resources.getString(R.string.bass)
                    circlePaint2.color = themeColor
                    linePaint.color = themeColor
                    invalidate()
                    setOnProgressChangedListener {
                        viewModel!!.onControllerBassProgressChanged(it)
                    }
                }

                // Sets UI for the reverb controller

                controllerReverb.run {
                    label = resources.getString(R.string.reverb)
                    circlePaint2.color = themeColor
                    linePaint.color = themeColor
                    invalidate()

                    setOnProgressChangedListener {
                        EqualizerSettings.instance.reverbPreset = (it * 6 / 18).toShort()
                        EqualizerSettings.instance.equalizerModel!!.reverbPreset =
                            EqualizerSettings.instance.reverbPreset
                        application.presetReverb?.preset = EqualizerSettings.instance.reverbPreset

                        runOnIOThread {
                            if (Params.getInstanceSynchronized().isSavingEqualizerSettings)
                                StorageUtil.getInstanceSynchronized()
                                    .storeReverbPresetLocking(EqualizerSettings.instance.reverbPreset)
                        }

                        this@EqualizerFragment.y = it
                    }
                }

                when {
                    !EqualizerSettings.instance.isEqualizerReloaded -> {
                        val x = application.bassBoost?.let { it.roundedStrength * 20 / 1000 } ?: 0
                        y = application.presetReverb?.let { it.preset * 19 / 6 } ?: 0
                        controllerBass.progress = if (x == 0) 1 else x
                        controllerReverb.progress = if (y == 0) 1 else y
                    }

                    else -> {
                        val x = EqualizerSettings.instance.bassStrength * 20 / 1000
                        y = EqualizerSettings.instance.reverbPreset * 19 / 6
                        controllerBass.progress = if (x == 0) 1 else x
                        controllerReverb.progress = if (y == 0) 1 else y
                    }
                }

                // Sets UI for equalizer's bands

                numberOfFrequencyBands = 5
                points = FloatArray(numberOfFrequencyBands.toInt())

                val lowerEqualizerBandLevel = when (
                    val r = tryWithAudioEffectOrStop {
                        application.equalizer.bandLevelRange[0]
                    }
                ) {
                    None -> return root
                    is Some -> r.value
                }

                val upperEqualizerBandLevel = when (
                    val r = tryWithAudioEffectOrStop {
                        application.equalizer.bandLevelRange[1]
                    }
                ) {
                    None -> return root
                    is Some -> r.value
                }

                (0 until numberOfFrequencyBands).forEach {
                    val equalizerBandIndex = it.toShort()

                    val frequencyHeader = TextView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )

                        gravity = Gravity.CENTER_HORIZONTAL
                        text = "${application.equalizer.getCenterFreq(equalizerBandIndex) / 1000} Hz"
                    }

                    val seekBar: SeekBar
                    val textView: TextView

                    when (it) {
                        0 -> {
                            seekBar = seekBar1.apply {
                                setBackgroundResource(viewModel!!.trackType)
                            }

                            textView = textView1
                        }

                        1 -> {
                            seekBar = seekBar2.apply {
                                setBackgroundResource(viewModel!!.trackType)
                            }

                            textView = textView2
                        }

                        2 -> {
                            seekBar = seekBar3.apply {
                                setBackgroundResource(viewModel!!.trackType)
                            }

                            textView = textView3
                        }

                        3 -> {
                            seekBar = seekBar4.apply {
                                setBackgroundResource(viewModel!!.trackType)
                            }

                            textView = textView4
                        }

                        else -> {
                            seekBar = seekBar5.apply {
                                setBackgroundResource(viewModel!!.trackType)
                            }

                            textView = textView5
                        }
                    }

                    seekBarFinal[it] = seekBar.apply {
                        id = it
                        max = upperEqualizerBandLevel - lowerEqualizerBandLevel
                    }

                    textView.run {
                        text = "${application.equalizer.getCenterFreq(equalizerBandIndex) / 1000} Hz"
                        textAlignment = View.TEXT_ALIGNMENT_CENTER
                    }

                    runOnIOThread {
                        val seekBarPoses = StorageUtil
                            .getInstanceSynchronized()
                            .loadEqualizerSeekbarsPosLocking()
                            ?: EqualizerSettings.instance.seekbarPos

                        when {
                            EqualizerSettings.instance.isEqualizerReloaded -> {
                                points[it] = (seekBarPoses[it] - lowerEqualizerBandLevel).toFloat()
                                dataset.addPoint(frequencyHeader.text.toString(), points[it])
                                seekBar.progress = seekBarPoses[it] - lowerEqualizerBandLevel
                            }

                            else -> {
                                points[it] = (application.equalizer.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel).toFloat()
                                dataset.addPoint(frequencyHeader.text.toString(), points[it])
                                seekBar.progress = application.equalizer.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel
                                EqualizerSettings.instance.seekbarPos[it] = application.equalizer.getBandLevel(equalizerBandIndex).toInt()
                                EqualizerSettings.instance.isEqualizerReloaded = true
                            }
                        }
                    }

                    seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            tryWithAudioEffectOrStop {
                                application.equalizer.setBandLevel(
                                    equalizerBandIndex,
                                    (progress + lowerEqualizerBandLevel).toShort()
                                )
                            }

                            points[seekBar.id] = when (
                                val r = tryWithAudioEffectOrStop {
                                    (application.equalizer.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel).toFloat()
                                }

                            ) {
                                None -> return
                                is Some -> r.value
                            }

                            EqualizerSettings.instance.seekbarPos[seekBar.id] =
                                progress + lowerEqualizerBandLevel

                            EqualizerSettings.instance.equalizerModel!!.seekbarPos[seekBar.id] =
                                progress + lowerEqualizerBandLevel

                            try {
                                dataset.updateValues(points)
                            } catch (ignored: Exception) {
                            }

                            binding!!.lineChart.notifyDataUpdate()
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) {
                            equalizerPresetSpinner.setSelection(0)
                            EqualizerSettings.instance.presetPos = 0
                            EqualizerSettings.instance.equalizerModel!!.presetPos = 0
                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar) {
                            runOnIOThread {
                                if (Params.getInstanceSynchronized().isSavingEqualizerSettings)
                                    StorageUtil.getInstanceSynchronized()
                                        .storeEqualizerSeekbarsPosLocking(EqualizerSettings.instance.seekbarPos)
                            }
                        }
                    })
                }
            }

        equalizeSound()

        paint.run {
            color = themeColor
            strokeWidth = (1.10 * EqualizerSettings.instance.ratio).toFloat()
        }

        dataset.run {
            color = themeColor
            isSmooth = true
            thickness = 5F
        }

        binding!!.lineChart.apply {
            setXAxis(false)
            setYAxis(false)
            setYLabels(AxisController.LabelPosition.NONE)
            setXLabels(AxisController.LabelPosition.NONE)
            setGrid(ChartView.GridType.NONE, 7, 10, paint)
            setAxisBorderValues(-300, 3300)
            addData(dataset)
            show()
        }

        return binding!!.root
    }

    /** Sets current pattern of equalizer */
    private fun equalizeSound() {
        val equalizerPresetNames = mutableListOf<String>().apply {
            add(resources.getString(R.string.custom))
            addAll(
                listOf(
                    R.string.normal,
                    R.string.classic,
                    R.string.dance,
                    R.string.flat,
                    R.string.folk,
                    R.string.heavy_metal,
                    R.string.hip_hop,
                    R.string.jazz,
                    R.string.pop,
                    R.string.rock
                ).map(resources::getString)
            )
        }

        binding!!.equalizerPresetSpinner.adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            equalizerPresetNames
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        if (EqualizerSettings.instance.isEqualizerReloaded && EqualizerSettings.instance.presetPos != 0)
            binding!!.equalizerPresetSpinner.setSelection(EqualizerSettings.instance.presetPos)

        binding!!.equalizerPresetSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    runOnIOThread {
                        if (Params.getInstanceSynchronized().isSavingEqualizerSettings)
                            StorageUtil.getInstanceSynchronized().storePresetPosLocking(position)
                    }

                    if (position != 0) {
                        application.equalizer.usePreset((position - 1).toShort())
                        EqualizerSettings.instance.presetPos = position

                        val numberOfFreqBands: Short = 5
                        val lowerEqualizerBandLevel = application.equalizer.bandLevelRange[0]

                        (0 until numberOfFreqBands).forEach {
                            seekBarFinal[it]!!.progress = when (
                                val r = tryWithAudioEffectOrStop {
                                    application.equalizer.getBandLevel(it.toShort()) - lowerEqualizerBandLevel
                                }
                            ) {
                                None -> return
                                is Some -> r.value
                            }

                            points[it] = when (
                                val r = tryWithAudioEffectOrStop {
                                    (application.equalizer.getBandLevel(it.toShort()) - lowerEqualizerBandLevel).toFloat()
                                }
                            ) {
                                None -> return
                                is Some -> r.value
                            }

                            EqualizerSettings.instance.seekbarPos[it] = when (
                                val r = tryWithAudioEffectOrStop {
                                    application.equalizer.getBandLevel(it.toShort()).toInt()
                                }
                            ) {
                                None -> return
                                is Some -> r.value
                            }

                            EqualizerSettings.instance.equalizerModel!!.seekbarPos[it] = when (
                                val r = tryWithAudioEffectOrStop {
                                    application.equalizer.getBandLevel(it.toShort()).toInt()
                                }
                            ) {
                                None -> return
                                is Some -> r.value
                            }
                        }

                        dataset.updateValues(points)
                        binding!!.lineChart.notifyDataUpdate()
                    }

                    EqualizerSettings.instance.equalizerModel!!.presetPos = position
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        EqualizerSettings.instance.isEditing = false
    }

    /** Releases all audio effects */
    private fun releaseAudioEffects() = application.run {
        equalizer.release()
        bassBoost?.release()
        presetReverb?.release()
    }

    /** Restarts all audio effects */
    private fun restartAudioEffects() {
        releaseAudioEffects()
        application.startEqualizer()
    }

    /**
     * Tries action. If fails, [restartAudioEffects]
     * and do it again. If it fails again [onNativeError] is called
     */

    private inline fun <T> tryWithAudioEffectOrStop(action: () -> T) = try {
        Some(action())
    } catch (e: RuntimeException) {
        e.printStackTrace()

        try {
            restartAudioEffects()
            Some(action())
        } catch (e: RuntimeException) {
            e.printStackTrace()
            onNativeError()
            None
        }
    }

    /** If native error occurs, shows message and finishes fragment */
    private fun onNativeError() {
        Toast.makeText(requireContext(), R.string.equalizer_native_error, Toast.LENGTH_LONG).show()
        requireActivity().supportFragmentManager.popBackStack()
    }
}