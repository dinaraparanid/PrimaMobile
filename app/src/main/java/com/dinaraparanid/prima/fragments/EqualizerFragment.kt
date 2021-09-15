package com.dinaraparanid.prima.fragments

import android.annotation.SuppressLint
import android.content.Context
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
import com.db.chart.model.LineSet
import com.db.chart.view.AxisController
import com.db.chart.view.ChartView
import com.db.chart.view.LineChartView
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentEqualizerBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.equalizer.EqualizerSettings
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import com.dinaraparanid.prima.viewmodels.mvvm.EqualizerViewModel

/**
 * Equalizer Fragment to modify audio.
 */

internal class EqualizerFragment : AbstractFragment<FragmentEqualizerBinding>() {
    private lateinit var paint: Paint
    private lateinit var chart: LineChartView
    private lateinit var dataset: LineSet
    private lateinit var points: FloatArray
    internal lateinit var context: Context

    override var binding: FragmentEqualizerBinding? = null

    private var seekBarFinal = arrayOfNulls<SeekBar>(5)
    private var numberOfFrequencyBands: Short = 0
    private var y = 0

    internal companion object {
        private const val ARG_AUDIO_SESSION_ID = "audio_session_id"

        @JvmStatic
        internal fun newInstance(mainLabelOldText: String, audioSessionId: Int): EqualizerFragment =
            EqualizerFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_AUDIO_SESSION_ID, audioSessionId)
                    putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)
            ?: resources.getString(R.string.equalizer)
        mainLabelCurText = resources.getString(R.string.equalizer)

        (requireActivity().application as MainApplication).startEqualizer()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val app = requireActivity().application as MainApplication
        val themeColor = Params.instance.primaryColor

        binding = DataBindingUtil
            .inflate<FragmentEqualizerBinding>(
                inflater,
                R.layout.fragment_equalizer,
                container,
                false
            )
            .apply {
                viewModel = EqualizerViewModel(requireActivity())

                equalizerSwitch.trackTintList = ViewSetter.colorStateList
                spinnerDropdownIcon.setOnClickListener { equalizerPresetSpinner.performClick() }

                paint = Paint()
                dataset = LineSet()

                val pit = StorageUtil(requireContext()).loadPitch()

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
                                    val ap = requireActivity().application as MainApplication
                                    val speed = ap.musicPlayer!!.playbackParams.speed
                                    val newPitch = s?.toString()?.toFloatOrNull()
                                        ?: pitchSeekBar!!.progress.toFloat()

                                    try {
                                        val isPlaying = ap.musicPlayer!!.isPlaying

                                        if (isPlaying)
                                            ap.musicPlayer!!.playbackParams = PlaybackParams()
                                                .setSpeed(speed)
                                                .setPitch(newPitch)

                                        if (!isPlaying)
                                            (requireActivity() as MainActivity).reinitializePlayingCoroutine()
                                    } catch (ignored: Exception) {
                                        // old or weak phone
                                    }

                                    pitchSeekBar!!.progress = try {
                                        app.musicPlayer!!.run {
                                            if (isPlaying)
                                                playbackParams = PlaybackParams()
                                                    .setSpeed(app.musicPlayer!!.playbackParams.speed)
                                                    .setPitch(newPitch)
                                        }

                                        when {
                                            newPitch > 1.5F -> 100
                                            else -> (newPitch * 100 - 50).toInt()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            requireContext(),
                                            R.string.not_supported,
                                            Toast.LENGTH_LONG
                                        ).show()

                                        50
                                    }

                                    if (Params.instance.saveEqualizerSettings)
                                        StorageUtil(requireContext()).storePitch(newPitch)
                                }
                            }
                        })
                }

                pitchSeekBar?.run {
                    progress = ((pit - 0.5F) * 100).toInt()
                    var newPitch = 0F

                    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q)
                        setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                            override fun onProgressChanged(
                                seekBar: SeekBar?,
                                progress: Int,
                                fromUser: Boolean
                            ) {
                                val ap = requireActivity().application as MainApplication

                                if (EqualizerSettings.instance.isEqualizerEnabled) {
                                    val speed = ap.musicPlayer!!.playbackParams.speed
                                    newPitch = 0.5F + progress * 0.01F

                                    try {
                                        val isPlaying = ap.musicPlayer!!.isPlaying

                                        ap.musicPlayer!!.playbackParams = PlaybackParams()
                                            .setSpeed(speed)
                                            .setPitch(newPitch)

                                        if (!isPlaying)
                                            (requireActivity() as MainActivity).reinitializePlayingCoroutine()
                                    } catch (ignored: Exception) {
                                        // old or weak phone
                                    }
                                }
                            }

                            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                                try {
                                    app.musicPlayer!!.playbackParams = PlaybackParams()
                                        .setSpeed(app.musicPlayer!!.playbackParams.speed)
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

                                if (Params.instance.saveEqualizerSettings)
                                    StorageUtil(requireContext()).storePitch(newPitch)
                            }
                        })
                }

                val speed = StorageUtil(requireContext()).loadSpeed()

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
                                    val ap = requireActivity().application as MainApplication
                                    val pitch = ap.musicPlayer!!.playbackParams.pitch
                                    val newSpeed = s?.toString()?.toFloatOrNull()
                                        ?: speedSeekBar!!.progress.toFloat()

                                    try {
                                        val isPlaying = ap.musicPlayer!!.isPlaying

                                        if (isPlaying)
                                            ap.musicPlayer!!.playbackParams = PlaybackParams()
                                                .setPitch(pitch)
                                                .setSpeed(newSpeed)

                                        if (!isPlaying)
                                            (requireActivity() as MainActivity).reinitializePlayingCoroutine()
                                    } catch (ignored: Exception) {
                                        // old or weak phone
                                    }

                                    speedSeekBar!!.progress = try {
                                        app.musicPlayer!!.run {
                                            if (isPlaying)
                                                playbackParams = PlaybackParams()
                                                    .setSpeed(newSpeed)
                                                    .setPitch(app.musicPlayer!!.playbackParams.pitch)
                                        }

                                        when {
                                            newSpeed > 1.5F -> {
                                                100
                                            }

                                            else -> (newSpeed * 100 - 50).toInt()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            requireContext(),
                                            R.string.not_supported,
                                            Toast.LENGTH_LONG
                                        ).show()

                                        50
                                    }

                                    if (Params.instance.saveEqualizerSettings)
                                        StorageUtil(requireContext()).storeSpeed(newSpeed)
                                }
                            }
                        })
                }

                speedSeekBar?.run {
                    progress = ((speed - 0.5F) * 100).toInt()
                    var newSpeed = 0F

                    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q)
                        setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                            override fun onProgressChanged(
                                seekBar: SeekBar?,
                                progress: Int,
                                fromUser: Boolean
                            ) {
                                val ap = requireActivity().application as MainApplication

                                if (EqualizerSettings.instance.isEqualizerEnabled) {
                                    val pitch = ap.musicPlayer!!.playbackParams.pitch
                                    newSpeed = 0.5F + progress * 0.01F

                                    try {
                                        val isPlaying = ap.musicPlayer!!.isPlaying

                                        if (isPlaying)
                                            ap.musicPlayer!!.playbackParams = PlaybackParams()
                                                .setPitch(pitch)
                                                .setSpeed(newSpeed)

                                        if (!isPlaying)
                                            (requireActivity() as MainActivity).reinitializePlayingCoroutine()
                                    } catch (ignored: Exception) {
                                        // old or weak phone
                                    }
                                }
                            }

                            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                                try {
                                    app.musicPlayer!!.playbackParams = PlaybackParams()
                                        .setSpeed(newSpeed)
                                        .setPitch(app.musicPlayer!!.playbackParams.pitch)
                                } catch (e: Exception) {
                                    progress = 50

                                    Toast.makeText(
                                        requireContext(),
                                        R.string.not_supported,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                speedStatus!!.setText(newSpeed.toString().take(4))

                                if (Params.instance.saveEqualizerSettings)
                                    StorageUtil(requireContext()).storeSpeed(newSpeed)
                            }
                        })
                }

                controllerBass.run {
                    label = resources.getString(R.string.bass)
                    circlePaint2.color = themeColor
                    linePaint.color = themeColor
                    invalidate()
                    setOnProgressChangedListener {
                        viewModel!!.onControllerBassProgressChanged(it)
                    }
                }

                controller3D.run {
                    label = "3D"
                    circlePaint2.color = themeColor
                    linePaint.color = themeColor
                    invalidate()
                    setOnProgressChangedListener {
                        viewModel!!.onControllerBassProgressChanged(it)
                    }
                }

                when {
                    !EqualizerSettings.instance.isEqualizerReloaded -> {
                        val x = app.bassBoost?.let { it.roundedStrength * 19 / 1000 } ?: 0
                        y = app.presetReverb?.let { it.preset * 19 / 6 } ?: 0
                        controllerBass.progress = if (x == 0) 1 else x
                        controller3D.progress = if (y == 0) 1 else y
                    }

                    else -> {
                        val x = EqualizerSettings.instance.bassStrength * 19 / 1000
                        y = EqualizerSettings.instance.reverbPreset * 19 / 6
                        controllerBass.progress = if (x == 0) 1 else x
                        controller3D.progress = if (y == 0) 1 else y
                    }
                }

                numberOfFrequencyBands = 5
                points = FloatArray(numberOfFrequencyBands.toInt())

                val lowerEqualizerBandLevel = app.equalizer.bandLevelRange[0]
                val upperEqualizerBandLevel = app.equalizer.bandLevelRange[1]

                (0 until numberOfFrequencyBands).forEach {
                    val equalizerBandIndex = it.toShort()

                    val frequencyHeader = TextView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )

                        gravity = Gravity.CENTER_HORIZONTAL
                        text = "${app.equalizer.getCenterFreq(equalizerBandIndex) / 1000} Hz"
                    }

                    val seekBar: SeekBar
                    val textView: TextView

                    when (it) {
                        0 -> {
                            seekBar = seekBar1
                            textView = textView1
                        }

                        1 -> {
                            seekBar = seekBar2
                            textView = textView2
                        }

                        2 -> {
                            seekBar = seekBar3
                            textView = textView3
                        }

                        3 -> {
                            seekBar = seekBar4
                            textView = textView4
                        }

                        else -> {
                            seekBar = seekBar5
                            textView = textView5
                        }
                    }

                    seekBarFinal[it] = seekBar.apply {
                        id = it
                        max = upperEqualizerBandLevel - lowerEqualizerBandLevel
                    }

                    textView.run {
                        text = "${app.equalizer.getCenterFreq(equalizerBandIndex) / 1000} Hz"
                        textAlignment = View.TEXT_ALIGNMENT_CENTER
                    }

                    val seekBarPoses = StorageUtil(requireContext()).loadEqualizerSeekbarsPos()
                        ?: EqualizerSettings.instance.seekbarPos

                    when {
                        EqualizerSettings.instance.isEqualizerReloaded -> {
                            points[it] = (seekBarPoses[it] - lowerEqualizerBandLevel).toFloat()
                            dataset.addPoint(frequencyHeader.text.toString(), points[it])
                            seekBar.progress = seekBarPoses[it] - lowerEqualizerBandLevel
                        }

                        else -> {
                            points[it] =
                                (app.equalizer.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel).toFloat()

                            dataset.addPoint(frequencyHeader.text.toString(), points[it])

                            seekBar.progress =
                                app.equalizer.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel

                            EqualizerSettings.instance.seekbarPos[it] =
                                app.equalizer.getBandLevel(equalizerBandIndex).toInt()

                            EqualizerSettings.instance.isEqualizerReloaded = true
                        }
                    }

                    seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            app.equalizer.setBandLevel(
                                equalizerBandIndex,
                                (progress + lowerEqualizerBandLevel).toShort()
                            )

                            points[seekBar.id] =
                                (app.equalizer.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel).toFloat()

                            EqualizerSettings.instance.seekbarPos[seekBar.id] =
                                progress + lowerEqualizerBandLevel
                            EqualizerSettings.instance.equalizerModel!!.seekbarPos[seekBar.id] =
                                progress + lowerEqualizerBandLevel

                            dataset.updateValues(points)
                            chart.notifyDataUpdate()
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) {
                            equalizerPresetSpinner.setSelection(0)
                            EqualizerSettings.instance.presetPos = 0
                            EqualizerSettings.instance.equalizerModel!!.presetPos = 0
                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar) {
                            if (Params.instance.saveEqualizerSettings)
                                StorageUtil(context)
                                    .storeEqualizerSeekbarsPos(EqualizerSettings.instance.seekbarPos)
                        }
                    })
                }
            }

        binding!!.controller3D.setOnProgressChangedListener {
            EqualizerSettings.instance.reverbPreset = (it * 6 / 19).toShort()
            EqualizerSettings.instance.equalizerModel!!.reverbPreset =
                EqualizerSettings.instance.reverbPreset
            app.presetReverb?.preset = EqualizerSettings.instance.reverbPreset

            if (Params.instance.saveEqualizerSettings)
                StorageUtil(context)
                    .storeReverbPreset(EqualizerSettings.instance.reverbPreset)

            y = it
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

        chart = binding!!.lineChart.apply {
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
                    val app = requireActivity().application as MainApplication

                    if (Params.instance.saveEqualizerSettings)
                        StorageUtil(context).storePresetPos(position)

                    if (position != 0) {
                        app.equalizer.usePreset((position - 1).toShort())
                        EqualizerSettings.instance.presetPos = position

                        val numberOfFreqBands: Short = 5
                        val lowerEqualizerBandLevel = app.equalizer.bandLevelRange[0]

                        (0 until numberOfFreqBands).forEach {
                            seekBarFinal[it]!!.progress =
                                app.equalizer.getBandLevel(it.toShort()) - lowerEqualizerBandLevel

                            points[it] =
                                (app.equalizer.getBandLevel(it.toShort()) - lowerEqualizerBandLevel).toFloat()

                            EqualizerSettings.instance.seekbarPos[it] =
                                app.equalizer.getBandLevel(it.toShort()).toInt()

                            EqualizerSettings.instance.equalizerModel!!.seekbarPos[it] =
                                app.equalizer.getBandLevel(it.toShort()).toInt()
                        }

                        dataset.updateValues(points)
                        chart.notifyDataUpdate()
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
}