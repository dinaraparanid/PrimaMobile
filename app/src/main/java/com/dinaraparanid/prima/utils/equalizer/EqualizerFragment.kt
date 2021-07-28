package com.dinaraparanid.prima.utils.equalizer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
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
import com.db.chart.model.LineSet
import com.db.chart.view.AxisController
import com.db.chart.view.ChartView
import com.db.chart.view.LineChartView
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.ViewSetter
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
    private lateinit var equalizerBlocker: FrameLayout
    private lateinit var bassBoost: BassBoost
    private lateinit var presetReverb: PresetReverb
    private lateinit var paint: Paint
    private lateinit var mainLayout: LinearLayout
    private lateinit var equalizer: Equalizer
    private lateinit var chart: LineChartView
    private lateinit var backBtn: ImageView
    private lateinit var fragTitle: TextView
    private lateinit var linearLayout: LinearLayout
    private lateinit var presetSpinner: Spinner
    private lateinit var dataset: LineSet
    private lateinit var points: FloatArray
    internal lateinit var context: Context

    private var seekBarFinal = arrayOfNulls<SeekBar>(5)
    private var numberOfFrequencyBands: Short = 0
    private var audioSessionId = 0
    private var y = 0

    internal class Builder {
        private var id = -1

        internal fun setAudioSessionId(id: Int): Builder {
            this.id = id
            return this
        }

        internal fun build() = newInstance(id)
    }

    private companion object {
        private const val ARG_AUDIO_SESSION_ID = "audio_session_id"
        private var showBackButton = true

        fun newInstance(audioSessionId: Int): EqualizerFragment {
            val args = Bundle()
            args.putInt(ARG_AUDIO_SESSION_ID, audioSessionId)
            val fragment = EqualizerFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Settings.isEditing = true
        mainLabelOldText = (requireActivity() as MainActivity).mainLabel.text.toString()
        mainLabelCurText = resources.getString(R.string.equalizer)

        audioSessionId = requireArguments().getInt(ARG_AUDIO_SESSION_ID)

        if (Settings.equalizerModel == null) {
            Settings.equalizerModel = EqualizerModel()
            Settings.equalizerModel!!.reverbPreset = PresetReverb.PRESET_NONE
            Settings.equalizerModel!!.bassStrength = 1000 / 19
        }

        equalizer = Equalizer(0, audioSessionId)
        bassBoost = BassBoost(0, audioSessionId).apply {
            enabled = Settings.isEqualizerEnabled
            properties = BassBoost.Settings(properties.toString()).apply {
                strength = Settings.equalizerModel!!.bassStrength
            }
        }

        presetReverb = PresetReverb(0, audioSessionId).apply {
            preset = Settings.equalizerModel!!.reverbPreset
            enabled = Settings.isEqualizerEnabled
        }

        equalizer.enabled = Settings.isEqualizerEnabled

        when (Settings.presetPos) {
            0 -> (0 until equalizer.numberOfBands).forEach {
                equalizer.setBandLevel(
                    it.toShort(),
                    Settings.seekbarpos[it].toShort()
                )
            }

            else -> equalizer.usePreset(Settings.presetPos.toShort())
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

        backBtn = view.findViewById<ImageView>(R.id.equalizer_back_btn).apply {
            setImageResource(ViewSetter.returnButtonImage)
            visibility = if (showBackButton) View.VISIBLE else View.GONE
            setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }
        }

        fragTitle = view.findViewById<TextView>(R.id.equalizer_fragment_title).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)

            setTextColor(ViewSetter.textColor)
        }


        equalizerSwitch = view.findViewById<Switch>(R.id.equalizer_switch).apply {
            isChecked = Settings.isEqualizerEnabled

            setOnCheckedChangeListener { _, isChecked ->
                equalizer.enabled = isChecked
                bassBoost.enabled = isChecked
                presetReverb.enabled = isChecked
                Settings.isEqualizerEnabled = isChecked
                Settings.equalizerModel!!.isEqualizerEnabled = isChecked
            }
        }

        spinnerDropDownIcon = view.findViewById(R.id.spinner_dropdown_icon)
        spinnerDropDownIcon.setOnClickListener { presetSpinner.performClick() }

        mainLayout = view.findViewById(R.id.equalizer_layout)
        presetSpinner = view.findViewById(R.id.equalizer_preset_spinner)
        equalizerBlocker = view.findViewById(R.id.equalizer_blocker)
        chart = view.findViewById(R.id.line_chart)

        paint = Paint()
        dataset = LineSet()

        val themeColor = Params.instance.theme.rgb

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
            !Settings.isEqualizerReloaded -> {
                val x = bassBoost.roundedStrength * 19 / 1000
                y = presetReverb.preset * 19 / 6

                bassController.progress = if (x == 0) 1 else x
                reverbController.progress = if (y == 0) 1 else y
            }

            else -> {
                val x = Settings.bassStrength * 19 / 1000
                y = Settings.reverbPreset * 19 / 6

                bassController.progress = if (x == 0) 1 else x
                reverbController.progress = if (y == 0) 1 else y
            }
        }

        linearLayout = view.findViewById(R.id.equalizer_container)

        numberOfFrequencyBands = 5
        points = FloatArray(numberOfFrequencyBands.toInt())

        val lowerEqualizerBandLevel = equalizer.bandLevelRange[0]
        val upperEqualizerBandLevel = equalizer.bandLevelRange[1]

        (0 until numberOfFrequencyBands).forEach {
            val equalizerBandIndex = it.toShort()

            val frequencyHeaderTextView = TextView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                gravity = Gravity.CENTER_HORIZONTAL
                setTextColor(ViewSetter.textColor)
                text = "${equalizer.getCenterFreq(equalizerBandIndex) / 1000} Hz"
            }

            var seekBar = SeekBar(context)
            var textView = TextView(context)

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

                4 -> {
                    seekBar = view.findViewById(R.id.seek_bar_5)
                    textView = view.findViewById(R.id.text_view_5)
                }
            }

            seekBarFinal[it] = seekBar.apply {
                id = it
                max = upperEqualizerBandLevel - lowerEqualizerBandLevel
            }

            textView.run {
                setTextColor(ViewSetter.textColor)
                text = "${equalizer.getCenterFreq(equalizerBandIndex) / 1000} Hz"
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }

            when {
                Settings.isEqualizerReloaded -> {
                    points[it] = (Settings.seekbarpos[it] - lowerEqualizerBandLevel).toFloat()
                    dataset.addPoint(frequencyHeaderTextView.text.toString(), points[it])
                    seekBar.progress = Settings.seekbarpos[it] - lowerEqualizerBandLevel
                }

                else -> {
                    points[it] =
                        (equalizer.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel).toFloat()

                    dataset.addPoint(frequencyHeaderTextView.text.toString(), points[it])

                    seekBar.progress =
                        equalizer.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel

                    Settings.seekbarpos[it] =
                        equalizer.getBandLevel(equalizerBandIndex).toInt()

                    Settings.isEqualizerReloaded = true
                }
            }

            seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    equalizer.setBandLevel(
                        equalizerBandIndex,
                        (progress + lowerEqualizerBandLevel).toShort()
                    )

                    points[seekBar.id] =
                        (equalizer.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel).toFloat()

                    Settings.seekbarpos[seekBar.id] = progress + lowerEqualizerBandLevel
                    Settings.equalizerModel!!.seekbarPos[seekBar.id] =
                        progress + lowerEqualizerBandLevel

                    dataset.updateValues(points)
                    chart.notifyDataUpdate()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    presetSpinner.setSelection(0)
                    Settings.presetPos = 0
                    Settings.equalizerModel!!.presetPos = 0
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            })
        }

        bassController.setOnProgressChangedListener {
            Settings.bassStrength = (1000F / 19 * it).toInt().toShort()
            bassBoost.setStrength(Settings.bassStrength)
            Settings.equalizerModel!!.bassStrength = Settings.bassStrength
        }

        reverbController.setOnProgressChangedListener {
            Settings.reverbPreset = (it * 6 / 19).toShort()
            Settings.equalizerModel!!.reverbPreset = Settings.reverbPreset
            presetReverb.preset = Settings.reverbPreset
            y = it
        }

        equalizeSound()

        paint.run {
            color = themeColor
            strokeWidth = (1.10 * Settings.ratio).toFloat()
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

        if (Settings.isEqualizerReloaded && Settings.presetPos != 0)
            presetSpinner.setSelection(Settings.presetPos)

        presetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                if (position != 0) {
                    equalizer.usePreset((position - 1).toShort())
                    Settings.presetPos = position

                    val numberOfFreqBands: Short = 5
                    val lowerEqualizerBandLevel = equalizer.bandLevelRange[0]

                    (0 until numberOfFreqBands).forEach {
                        seekBarFinal[it]!!.progress =
                            equalizer.getBandLevel(it.toShort()) - lowerEqualizerBandLevel

                        points[it] =
                            (equalizer.getBandLevel(it.toShort()) - lowerEqualizerBandLevel).toFloat()

                        Settings.seekbarpos[it] =
                            equalizer.getBandLevel(it.toShort()).toInt()

                        Settings.equalizerModel!!.seekbarPos[it] =
                            equalizer.getBandLevel(it.toShort()).toInt()
                    }

                    dataset.updateValues(points)
                    chart.notifyDataUpdate()
                }

                Settings.equalizerModel!!.presetPos = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        equalizer.release()
        bassBoost.release()
        presetReverb.release()
        Settings.isEditing = false
    }
}