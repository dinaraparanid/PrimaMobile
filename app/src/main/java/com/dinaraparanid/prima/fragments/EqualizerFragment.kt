package com.dinaraparanid.prima.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Paint
import android.media.PlaybackParams
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import com.bumptech.glide.Glide
import com.db.chart.model.LineSet
import com.db.chart.view.AxisController
import com.db.chart.view.ChartView
import com.db.chart.view.LineChartView
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.equalizer.AnalogController
import com.dinaraparanid.prima.utils.equalizer.EqualizerModel
import com.dinaraparanid.prima.utils.equalizer.EqualizerSettings
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment

/**
 * Equalizer Fragment to modify audio.
 */

internal class EqualizerFragment : AbstractFragment() {
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var equalizerSwitch: Switch
    private lateinit var spinnerDropDownIcon: ImageView
    private lateinit var bassController: AnalogController
    private lateinit var reverbController: AnalogController
    private lateinit var paint: Paint
    private lateinit var mainLayout: LinearLayout
    private lateinit var chart: LineChartView
    private lateinit var backBtn: ImageView
    private lateinit var fragTitle: TextView
    private lateinit var linearLayout: LinearLayout
    private lateinit var presetSpinner: Spinner
    private lateinit var dataset: LineSet
    private lateinit var points: FloatArray
    private lateinit var pitchSeekBar: SeekBar
    private lateinit var speedSeekBar: SeekBar
    private lateinit var pitchStatus: TextView
    private lateinit var speedStatus: TextView
    internal lateinit var context: Context

    private var seekBarFinal = arrayOfNulls<SeekBar>(5)
    private var numberOfFrequencyBands: Short = 0
    private var audioSessionId = 0
    private var y = 0

    internal class Builder(private val mainLabelOldText: String) {
        private var id = -1

        internal fun setAudioSessionId(id: Int): Builder {
            this.id = id
            return this
        }

        internal fun build() = newInstance(mainLabelOldText, id)
    }

    private companion object {
        private const val ARG_AUDIO_SESSION_ID = "audio_session_id"

        fun newInstance(mainLabelOldText: String, audioSessionId: Int): EqualizerFragment {
            val args = Bundle()
            args.putInt(ARG_AUDIO_SESSION_ID, audioSessionId)
            args.putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
            val fragment = EqualizerFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val loader = StorageUtil(requireContext())
        (requireActivity().application as MainApplication).musicPlayer!!.playbackParams =
            PlaybackParams().setPitch(loader.loadPitch()).setSpeed(loader.loadSpeed())

        EqualizerSettings.instance.isEqualizerEnabled = true

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
            (resources.configuration.screenLayout.and(Configuration.SCREENLAYOUT_SIZE_MASK) !=
                    Configuration.SCREENLAYOUT_SIZE_LARGE ||
                    resources.configuration.screenLayout.and(Configuration.SCREENLAYOUT_SIZE_MASK) !=
                    Configuration.SCREENLAYOUT_SIZE_XLARGE)
        ) {
            requireActivity().supportFragmentManager.popBackStack()
            return
        }

        EqualizerSettings.instance.isEditing = true
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)
            ?: resources.getString(R.string.equalizer)
        mainLabelCurText = resources.getString(R.string.equalizer)
        audioSessionId = requireArguments().getInt(ARG_AUDIO_SESSION_ID)

        if (EqualizerSettings.instance.equalizerModel == null) {
            EqualizerSettings.instance.equalizerModel = EqualizerModel(context).apply {
                reverbPreset = PresetReverb.PRESET_NONE
                bassStrength = 1000 / 19
            }
        }

        val app = requireActivity().application as MainApplication

        app.equalizer = Equalizer(0, audioSessionId)
        app.bassBoost = BassBoost(0, audioSessionId).apply {
            enabled = EqualizerSettings.instance.isEqualizerEnabled
            properties = BassBoost.Settings(properties.toString()).apply {
                strength = StorageUtil(requireContext()).loadBassStrength()
            }
        }

        app.presetReverb = PresetReverb(0, audioSessionId).apply {
            try {
                preset = StorageUtil(requireContext()).loadReverbPreset()
            } catch (ignored: Exception) {
                // not supported
            }
            enabled = EqualizerSettings.instance.isEqualizerEnabled
        }

        app.equalizer.enabled = EqualizerSettings.instance.isEqualizerEnabled

        val seekBarPoses = StorageUtil(requireContext()).loadEqualizerSeekbarsPos()
            ?: EqualizerSettings.instance.seekbarPos

        when (EqualizerSettings.instance.presetPos) {
            0 -> (0 until app.equalizer.numberOfBands).forEach {
                app.equalizer.setBandLevel(
                    it.toShort(),
                    seekBarPoses[it].toShort()
                )
            }

            else -> app.equalizer.usePreset(EqualizerSettings.instance.presetPos.toShort())
        }
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
    ): View? {
        val view = inflater.inflate(R.layout.fragment_equalizer, container, false)
        val app = requireActivity().application as MainApplication

        backBtn = view.findViewById<ImageView>(R.id.equalizer_back_btn).apply {
            Glide.with(this@EqualizerFragment).load(ViewSetter.returnButtonImage).into(this)
            setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }
        }

        fragTitle = view.findViewById<TextView>(R.id.equalizer_fragment_title).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
        }

        equalizerSwitch = view.findViewById<Switch>(R.id.equalizer_switch).apply {
            isChecked = EqualizerSettings.instance.isEqualizerEnabled

            setOnCheckedChangeListener { _, isChecked ->
                app.equalizer.enabled = isChecked
                app.bassBoost.enabled = isChecked
                app.presetReverb.enabled = isChecked
                EqualizerSettings.instance.isEqualizerEnabled = isChecked
                EqualizerSettings.instance.equalizerModel!!.isEqualizerEnabled = isChecked

                val loader = StorageUtil(requireContext())
                app.musicPlayer!!.playbackParams = PlaybackParams()
                    .setPitch(if (isChecked) loader.loadPitch() else 1F)
                    .setSpeed(if (isChecked) loader.loadSpeed() else 1F)
            }
        }

        spinnerDropDownIcon = view.findViewById(R.id.spinner_dropdown_icon)
        spinnerDropDownIcon.setOnClickListener { presetSpinner.performClick() }

        mainLayout = view.findViewById(R.id.equalizer_layout)
        presetSpinner = view.findViewById(R.id.equalizer_preset_spinner)
        chart = view.findViewById(R.id.line_chart)

        paint = Paint()
        dataset = LineSet()

        val themeColor = Params.instance.theme.rgb

        val seeksLayout = view.findViewById<LinearLayout>(R.id.pitch_speed_layout)

        val pit = StorageUtil(requireContext()).loadPitch()
        val pitchLayout = seeksLayout.findViewById<LinearLayout>(R.id.pitch)

        pitchLayout.findViewById<TextView>(R.id.pitch_title).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
        }

        pitchStatus = pitchLayout.findViewById<TextView>(R.id.pitch_status).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
            text = pit.toString().take(4)
        }

        pitchSeekBar = pitchLayout.findViewById<SeekBar>(R.id.pitch_seek_bar).apply {
            progress = ((pit - 0.5F) * 100).toInt()
            var newPitch = 0F

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

                        pitchStatus.text = newPitch.toString().take(4)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) =
                    (requireActivity() as MainActivity).run { if (isPlaying != true) resumePlaying() }

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

                    if (Params.instance.saveEqualizerSettings)
                        StorageUtil(requireContext()).storePitch(newPitch)
                }
            })
        }

        val speed = StorageUtil(requireContext()).loadSpeed()
        val speedLayout = seeksLayout.findViewById<LinearLayout>(R.id.speed)

        speedLayout.findViewById<TextView>(R.id.speed_title).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
        }

        speedStatus = speedLayout.findViewById<TextView>(R.id.speed_status).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
            text = speed.toString().take(4)
        }

        speedSeekBar = speedLayout.findViewById<SeekBar>(R.id.speed_seek_bar).apply {
            progress = ((StorageUtil(requireContext()).loadSpeed() - 0.5F) * 100).toInt()
            var newSpeed = 0F

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

                            ap.musicPlayer!!.playbackParams = PlaybackParams()
                                .setPitch(pitch)
                                .setSpeed(newSpeed)

                            if (!isPlaying)
                                (requireActivity() as MainActivity).reinitializePlayingCoroutine()
                        } catch (ignored: Exception) {
                            // old or weak phone
                        }

                        speedStatus.text = newSpeed.toString().take(4)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) =
                    (requireActivity() as MainActivity).run { if (isPlaying != true) resumePlaying() }

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

                    if (Params.instance.saveEqualizerSettings)
                        StorageUtil(requireContext())
                            .storeSpeed(app.musicPlayer!!.playbackParams.speed)
                }
            })
        }

        bassController = view.findViewById<AnalogController>(R.id.controller_bass).apply {
            label = resources.getString(R.string.bass)
            circlePaint2.color = themeColor
            linePaint.color = themeColor
            invalidate()
        }

        reverbController = view.findViewById<AnalogController>(R.id.controller_3D).apply {
            label = "3D"
            circlePaint2.color = themeColor
            linePaint.color = themeColor
            invalidate()
        }

        when {
            !EqualizerSettings.instance.isEqualizerReloaded -> {
                val x = app.bassBoost.roundedStrength * 19 / 1000
                y = app.presetReverb.preset * 19 / 6

                bassController.progress = if (x == 0) 1 else x
                reverbController.progress = if (y == 0) 1 else y
            }

            else -> {
                val x = EqualizerSettings.instance.bassStrength * 19 / 1000
                y = EqualizerSettings.instance.reverbPreset * 19 / 6

                bassController.progress = if (x == 0) 1 else x
                reverbController.progress = if (y == 0) 1 else y
            }
        }

        linearLayout = view.findViewById(R.id.equalizer_container)

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
                    seekBar = view.findViewById(R.id.seek_bar_1)
                    textView = view.findViewById(R.id.text_view_1)
                }

                1 -> {
                    seekBar = view.findViewById(R.id.seek_bar_2)
                    textView = view.findViewById(R.id.text_view_2)
                }

                2 -> {
                    seekBar = view.findViewById(R.id.seek_bar_3)
                    textView = view.findViewById(R.id.text_view_3)
                }

                3 -> {
                    seekBar = view.findViewById(R.id.seek_bar_4)
                    textView = view.findViewById(R.id.text_view_4)
                }

                else -> {
                    seekBar = view.findViewById(R.id.seek_bar_5)
                    textView = view.findViewById(R.id.text_view_5)
                }
            }

            seekBarFinal[it] = seekBar.apply {
                id = it
                max = upperEqualizerBandLevel - lowerEqualizerBandLevel
            }

            textView.run {
                text = "${app.equalizer.getCenterFreq(equalizerBandIndex) / 1000} Hz"
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
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
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
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
                    presetSpinner.setSelection(0)
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

        bassController.setOnProgressChangedListener {
            EqualizerSettings.instance.bassStrength = (1000F / 19 * it).toInt().toShort()
            app.bassBoost.setStrength(EqualizerSettings.instance.bassStrength)
            EqualizerSettings.instance.equalizerModel!!.bassStrength =
                EqualizerSettings.instance.bassStrength

            if (Params.instance.saveEqualizerSettings)
                StorageUtil(context)
                    .storeBassStrength(EqualizerSettings.instance.bassStrength)
        }

        reverbController.setOnProgressChangedListener {
            EqualizerSettings.instance.reverbPreset = (it * 6 / 19).toShort()
            EqualizerSettings.instance.equalizerModel!!.reverbPreset =
                EqualizerSettings.instance.reverbPreset
            app.presetReverb.preset = EqualizerSettings.instance.reverbPreset

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

        chart.run {
            setXAxis(false)
            setYAxis(false)
            setYLabels(AxisController.LabelPosition.NONE)
            setXLabels(AxisController.LabelPosition.NONE)
            setGrid(ChartView.GridType.NONE, 7, 10, paint)
            setAxisBorderValues(-300, 3300)
            addData(dataset)
            show()
        }

        return view
    }

    override fun onResume() {
        super.onResume()

        val pos = StorageUtil(requireContext()).loadEqualizerSeekbarsPos()
            ?: EqualizerSettings.instance.seekbarPos

        val lowerEqualizerBandLevel = (requireActivity().application as MainApplication)
            .equalizer.bandLevelRange[0]

        seekBarFinal.forEachIndexed { i, sb -> sb?.progress = pos[i] - lowerEqualizerBandLevel }

        val pit = StorageUtil(requireContext()).loadPitch()
        pitchStatus.text = pit.toString().take(4)
        pitchSeekBar.progress = ((pit - 0.5F) * 100).toInt()

        val speed = StorageUtil(requireContext()).loadSpeed()
        speedStatus.text = speed.toString().take(4)
        speedSeekBar.progress = ((speed - 0.5F) * 100).toInt()
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

        presetSpinner.adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            equalizerPresetNames
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        if (EqualizerSettings.instance.isEqualizerReloaded && EqualizerSettings.instance.presetPos != 0)
            presetSpinner.setSelection(EqualizerSettings.instance.presetPos)

        presetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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