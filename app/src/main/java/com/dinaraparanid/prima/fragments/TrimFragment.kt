package com.dinaraparanid.prima.fragments

import android.app.AlertDialog
import android.content.ContentValues
import android.content.res.Configuration
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.databinding.FragmentTrimBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.dialogs.AfterSaveRingtoneDialog
import com.dinaraparanid.prima.utils.dialogs.FileSaveDialog
import com.dinaraparanid.prima.utils.extensions.correctFileName
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.AsyncContext
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.utils.trimmer.MarkerView
import com.dinaraparanid.prima.utils.trimmer.MarkerView.MarkerListener
import com.dinaraparanid.prima.utils.trimmer.SamplePlayer
import com.dinaraparanid.prima.utils.trimmer.WaveformView.WaveformListener
import com.dinaraparanid.prima.utils.trimmer.soundfile.SoundFile
import com.dinaraparanid.prima.viewmodels.androidx.TrimViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel
import com.kaopiz.kprogresshud.KProgressHUD
import kotlinx.coroutines.*
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * [AbstractFragment] to trim audio. Keeps track of
 * the [com.dinaraparanid.prima.utils.trimmer.WaveformView] display,
 * current horizontal offset, marker handles,
 * start / end text boxes, and handles all of the buttons and controls.
 */

class TrimFragment :
    CallbacksFragment<FragmentTrimBinding, MainActivity>(),
    MarkerListener,
    WaveformListener,
    MainActivityFragment,
    Rising,
    AsyncContext {
    interface Callbacks : CallbacksFragment.Callbacks {
        fun showChooseContactFragment(uri: Uri)
    }

    override var isMainLabelInitialized = false
    override val awaitMainLabelInitLock: Lock = ReentrantLock()
    override val awaitMainLabelInitCondition: Condition = awaitMainLabelInitLock.newCondition()

    override lateinit var mainLabelOldText: String
    override lateinit var mainLabelCurText: String

    override var binding: FragmentTrimBinding? = null

    private lateinit var file: File
    private lateinit var filename: String
    private lateinit var track: AbstractTrack

    private var loadProgressDialog: KProgressHUD? = null
    private var infoContent: String? = null
    private var player: SamplePlayer? = null
    private var loadSoundFileCoroutine: Job? = null
    private var saveSoundFileCoroutine: Job? = null
    private var alertDialog: AlertDialog? = null
    private var handler: Handler? = Handler(Looper.myLooper()!!)

    private var loadingLastUpdateTime = 0L
    private var loadingKeepGoing = false
    private var newFileKind = 0
    private var keyDown = false
    private var caption = ""
    private var width = 0
    private var maxPos = 0
    private var startVisible = false
    private var endVisible = false
    private var lastDisplayedStartPos = -1
    private var lastDisplayedEndPos = -1
    private var offset = 0
    private var offsetGoal = 0
    private var flingVelocity = 0
    private var playStartMilliseconds = 0
    private var playEndMilliseconds = 0
    private var isPlaying = false
    private var touchDragging = false
    private var touchStart = 0F
    private var touchInitialOffset = 0
    private var touchInitialStartPos = 0
    private var touchInitialEndPos = 0
    private var waveformTouchStartMilliseconds = 0L
    private var density = 0F
    private var markerLeftInset = 0
    private var markerRightInset = 0
    private var markerTopOffset = 0
    private var markerBottomOffset = 0

    private val viewModel by lazy {
        ViewModelProvider(this)[TrimViewModel::class.java]
    }

    override val coroutineScope: CoroutineScope
        get() = lifecycleScope

    private val textWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence,
            start: Int, count: Int, after: Int
        ) = Unit

        override fun onTextChanged(
            s: CharSequence,
            start: Int, before: Int, count: Int
        ) = Unit

        override fun afterTextChanged(s: Editable) {
            if (binding!!.startText.hasFocus()) {
                try {
                    viewModel.startPos = binding!!.waveform.secondsToPixels(
                        binding!!.startText.text.toString().toDouble()
                    )
                    updateDisplay()
                } catch (e: NumberFormatException) {
                }
            }

            if (binding!!.endText.hasFocus()) {
                try {
                    viewModel.endPos = binding!!.waveform.secondsToPixels(
                        binding!!.endText.text.toString().toDouble()
                    )
                    updateDisplay()
                } catch (e: NumberFormatException) {
                }
            }
        }
    }

    private var timerRunnable: Runnable? = object : Runnable {
        override fun run() {
            if (viewModel.startPos != lastDisplayedStartPos && binding?.startText?.hasFocus() == false) {
                binding!!.startText.text = formatTime(viewModel.startPos)
                lastDisplayedStartPos = viewModel.startPos
            }

            if (viewModel.endPos != lastDisplayedEndPos && binding?.endText?.hasFocus() == false) {
                binding!!.endText.text = formatTime(viewModel.endPos)
                lastDisplayedEndPos = viewModel.endPos
            }

            handler?.postDelayed(this, 100)
        }
    }

    internal companion object {
        private const val TRACK_KEY = "track"
        private const val SOUND_FILE_KEY = "sound_file"
        private const val START_POS_KEY = "start_pos"
        private const val END_POS_KEY = "end_pos"

        /**
         * Creates new instance of [TrimFragment] with given arguments
         * @param mainLabelOldText mail label text when fragment was created
         * @param mainLabelCurText text to show when fragment is created
         * @param track track to edit
         */

        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            mainLabelCurText: String,
            track: AbstractTrack
        ) = TrimFragment().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
                putSerializable(TRACK_KEY, track)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        track = requireArguments().getSerializable(TRACK_KEY) as AbstractTrack
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY)!!

        setMainLabelInitialized()
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        setMainActivityMainLabel()

        filename = track.path
            .replaceFirst("file://".toRegex(), "")
            .replace("%20".toRegex(), " ")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.load(
            savedInstanceState?.getSerializable(SOUND_FILE_KEY) as? SoundFile?,
            savedInstanceState?.getInt(START_POS_KEY),
            savedInstanceState?.getInt(END_POS_KEY)
        )

        density = Configuration().densityDpi.toFloat()
        markerLeftInset = (46 * density).toInt()
        markerRightInset = (48 * density).toInt()
        markerTopOffset = (10 * density).toInt()
        markerBottomOffset = (10 * density).toInt()

        binding = DataBindingUtil
            .inflate<FragmentTrimBinding>(inflater, R.layout.fragment_trim, container, false)
            .apply {
                viewModel = ViewModel()

                startText.addTextChangedListener(textWatcher)
                endText.addTextChangedListener(textWatcher)

                play.setOnClickListener { onPlay(this@TrimFragment.viewModel.startPos) }

                rew.setOnClickListener {
                    when {
                        isPlaying -> {
                            var newPos = player!!.currentPosition - 5000
                            if (newPos < playStartMilliseconds) newPos = playStartMilliseconds
                            player!!.seekTo(newPos)
                        }

                        else -> {
                            startMarker.requestFocus()
                            markerFocus(startMarker)
                        }
                    }
                }

                ffwd.setOnClickListener {
                    when {
                        isPlaying -> {
                            var newPos = 5000 + player!!.currentPosition
                            if (newPos > playEndMilliseconds) newPos = playEndMilliseconds
                            player!!.seekTo(newPos)
                        }

                        else -> {
                            endMarker.requestFocus()
                            markerFocus(endMarker)
                        }
                    }
                }

                markStart.setOnClickListener {
                    if (isPlaying) {
                        this@TrimFragment.viewModel.startPos =
                            waveform.millisecondsToPixels(player!!.currentPosition)
                        updateDisplay()
                    }
                }

                markEnd.setOnClickListener {
                    if (isPlaying) {
                        this@TrimFragment.viewModel.endPos =
                            waveform.millisecondsToPixels(player!!.currentPosition)
                        updateDisplay()
                        handlePause()
                    }
                }

                play.setImageResource(ViewSetter.getPlayButtonImage(isPlaying))
                waveform.setListener(this@TrimFragment)
                info.text = caption

                if (this@TrimFragment.viewModel.soundFile != null && waveform.hasSoundFile) {
                    waveform.setSoundFile(this@TrimFragment.viewModel.soundFile!!)
                    waveform.recomputeHeights(density)
                    maxPos = waveform.maxPos
                }

                startMarker.setListener(this@TrimFragment)
                startVisible = true

                endMarker.setListener(this@TrimFragment)
                endVisible = true
            }

        updateDisplay()
        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handler!!.postDelayed(timerRunnable!!, 100)

        when (viewModel.soundFile) {
            null -> {
                loadProgressDialog = createAndShowAwaitDialog(requireContext(), false)
                runOnIOThread { loadFromFile() }
            }

            else -> afterOpeningSoundFile(true)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(SOUND_FILE_KEY, viewModel.soundFile)
        outState.putInt(START_POS_KEY, viewModel.startPos)
        outState.putInt(END_POS_KEY, viewModel.endPos)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()

        loadProgressDialog?.dismiss()
        handler = null
        timerRunnable = null
        loadingKeepGoing = false
        loadSoundFileCoroutine = null
        saveSoundFileCoroutine = null
        viewModel.soundFile = null
        System.gc()

        if (alertDialog != null) {
            alertDialog!!.dismiss()
            alertDialog = null
        }

        if (player != null) {
            if (player!!.isPlaying || player!!.isPaused)
                player!!.stop()

            player!!.release()
            player = null
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val saveZoomLevel = binding!!.waveform.zoomLevel
        super.onConfigurationChanged(newConfig)

        runOnUIThread { loadUI() }

        handler!!.postDelayed({
            binding!!.startMarker.requestFocus()
            markerFocus(binding!!.startMarker)
            binding!!.waveform.setZoomLevel(saveZoomLevel)
            binding!!.waveform.recomputeHeights(density)
            updateDisplay()
        }, 500)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_trim, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> onSave()

            R.id.action_reset -> {
                resetPositions()
                offsetGoal = 0
                updateDisplay()
            }
        }

        return false
    }

    override fun waveformDraw() {
        width = binding!!.waveform.measuredWidth
        if (offsetGoal != offset && !keyDown || isPlaying || flingVelocity != 0)
            updateDisplay()
    }

    override fun waveformTouchStart(x: Float) {
        touchDragging = true
        touchStart = x
        touchInitialOffset = offset
        flingVelocity = 0
        waveformTouchStartMilliseconds = currentTime
    }

    override fun waveformTouchMove(x: Float) {
        offset = (touchInitialOffset + (touchStart - x)).toInt().trapped
        updateDisplay()
    }

    override fun waveformTouchEnd() {
        touchDragging = false
        offsetGoal = offset

        val elapsedMs = currentTime - waveformTouchStartMilliseconds
        if (elapsedMs < 300) {
            when {
                isPlaying -> {
                    val seekMs =
                        binding!!.waveform.pixelsToMilliseconds((touchStart + offset).toInt())

                    when (seekMs) {
                        in playStartMilliseconds until playEndMilliseconds ->
                            player!!.seekTo(seekMs)

                        else -> handlePause()
                    }
                }

                else -> onPlay((touchStart + offset).toInt())
            }
        }
    }

    override fun waveformFling(x: Float) {
        touchDragging = false
        offsetGoal = offset
        flingVelocity = (-x).toInt()
        updateDisplay()
    }

    override fun waveformZoomIn() = binding!!.waveform.run {
        zoomIn()
        viewModel.startPos = start
        viewModel.endPos = end
        this@TrimFragment.maxPos = maxPos
        this@TrimFragment.offset = offset
        offsetGoal = offset
        updateDisplay()
    }

    override fun waveformZoomOut() = binding!!.waveform.run {
        zoomOut()
        viewModel.startPos = start
        viewModel.endPos = end
        this@TrimFragment.maxPos = maxPos
        this@TrimFragment.offset = offset
        offsetGoal = offset
        updateDisplay()
    }

    override fun markerDraw() = Unit

    override fun markerTouchStart(marker: MarkerView, pos: Float) {
        touchDragging = true
        touchStart = pos
        touchInitialStartPos = viewModel.startPos
        touchInitialEndPos = viewModel.endPos
    }

    override fun markerTouchMove(marker: MarkerView, pos: Float) {
        val delta = pos - touchStart

        when (marker) {
            binding!!.startMarker -> {
                viewModel.startPos = (touchInitialStartPos + delta).toInt().trapped
                viewModel.endPos = (touchInitialEndPos + delta).toInt().trapped
            }

            else -> {
                viewModel.endPos = (touchInitialEndPos + delta).toInt().trapped
                if (viewModel.endPos < viewModel.startPos) viewModel.endPos = viewModel.startPos
            }
        }

        updateDisplay()
    }

    override fun markerTouchEnd(marker: MarkerView) {
        touchDragging = false

        when (marker) {
            binding!!.startMarker -> setOffsetGoalStart()
            else -> setOffsetGoalEnd()
        }
    }

    override fun markerLeft(marker: MarkerView, velocity: Int) {
        keyDown = true

        if (marker == binding!!.startMarker) {
            val saveStart = viewModel.startPos
            viewModel.startPos = (viewModel.startPos - velocity).trapped
            viewModel.endPos = (viewModel.endPos - (saveStart - viewModel.startPos)).trapped
            setOffsetGoalStart()
        }

        if (marker == binding!!.endMarker) {
            viewModel.endPos = when (viewModel.endPos) {
                viewModel.startPos -> {
                    viewModel.startPos = (viewModel.startPos - velocity).trapped
                    viewModel.startPos
                }

                else -> (viewModel.endPos - velocity).trapped
            }

            setOffsetGoalEnd()
        }

        updateDisplay()
    }

    override fun markerRight(marker: MarkerView, velocity: Int) {
        keyDown = true

        if (marker == binding!!.startMarker) {
            val saveStart = viewModel.startPos
            viewModel.startPos += velocity

            if (viewModel.startPos > maxPos)
                viewModel.startPos = maxPos

            viewModel.endPos += viewModel.startPos - saveStart

            if (viewModel.endPos > maxPos)
                viewModel.endPos = maxPos

            setOffsetGoalStart()
        }

        if (marker == binding!!.endMarker) {
            viewModel.endPos += velocity

            if (viewModel.endPos > maxPos)
                viewModel.endPos = maxPos

            setOffsetGoalEnd()
        }

        updateDisplay()
    }

    override fun markerEnter(marker: MarkerView) = Unit

    override fun markerKeyUp() {
        keyDown = false
        updateDisplay()
    }

    override fun markerFocus(marker: MarkerView) {
        keyDown = false

        when (marker) {
            binding!!.startMarker -> setOffsetGoalStartNoUpdate()
            else -> setOffsetGoalEndNoUpdate()
        }

        // Delay updating the display because if this focus was in
        // response to a touch event, we want to receive the touch
        // event too before updating the display.
        handler!!.postDelayed(this::updateDisplay, 100)
    }

    override fun up() {
        if (!(requireActivity() as MainActivity).isUpped)
            binding!!.trimLayout.layoutParams =
                (binding!!.trimLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = Params.PLAYING_TOOLBAR_HEIGHT
                }
    }

    private fun loadUI() {
        density = resources.displayMetrics.density
        markerLeftInset = (46 * density).toInt()
        markerRightInset = (48 * density).toInt()
        markerTopOffset = (10 * density).toInt()
        markerBottomOffset = (10 * density).toInt()

        binding!!.play.setImageResource(ViewSetter.getPlayButtonImage(isPlaying))
        binding!!.info.text = caption

        if (viewModel.soundFile != null && binding!!.waveform.hasSoundFile) {
            binding!!.waveform.setSoundFile(viewModel.soundFile!!)
            binding!!.waveform.recomputeHeights(density)
            maxPos = binding!!.waveform.maxPos
        }

        startVisible = true
        endVisible = true

        maxPos = 0
        lastDisplayedStartPos = -1
        lastDisplayedEndPos = -1

        updateDisplay()
    }

    private fun loadFromFile() {
        file = File(filename)

        loadingLastUpdateTime = currentTime
        loadingKeepGoing = true

        val listener: SoundFile.ProgressListener = object : SoundFile.ProgressListener {
            override fun reportProgress(fractionComplete: Double): Boolean {
                val now = currentTime

                if (now - loadingLastUpdateTime > 100) {
                    loadingLastUpdateTime = now
                }

                return loadingKeepGoing
            }
        }

        // Load the sound file in a background thread

        loadSoundFileCoroutine = runOnIOThread {
            try {
                SoundFile.create(file.absolutePath, listener)?.let {
                    viewModel.soundFile = it
                } ?: run {
                    launch(Dispatchers.Main) {
                        loadProgressDialog?.dismiss()
                        showFinalAlert(false, resources.getString(R.string.extension_error))
                    }

                    return@runOnIOThread
                }

                player = SamplePlayer(viewModel.soundFile!!)
            } catch (e: Exception) {
                e.printStackTrace()

                infoContent = e.toString()

                launch(Dispatchers.Main) {
                    loadProgressDialog?.dismiss()
                    binding!!.info.text = infoContent!!
                    showFinalAlert(false, resources.getText(R.string.read_error))
                }

                return@runOnIOThread
            }

            if (loadingKeepGoing)
                handler!!.post { afterOpeningSoundFile(false) }
        }
    }

    private fun afterOpeningSoundFile(wasSaved: Boolean) {
        maxPos = binding!!.waveform.run {
            setSoundFile(viewModel.soundFile!!)
            recomputeHeights(density)
            maxPos
        }

        lastDisplayedStartPos = -1
        lastDisplayedEndPos = -1
        touchDragging = false
        offset = 0
        offsetGoal = 0
        flingVelocity = 0

        if (!wasSaved)
            resetPositions()

        if (viewModel.endPos > maxPos)
            viewModel.endPos = maxPos

        val soundFile = viewModel.soundFile!!

        caption = soundFile.filetype + ", " +
                soundFile.sampleRate + " Hz, " +
                soundFile.avgBitrateKbps + " kbps, " +
                formatTime(maxPos) + " " +
                resources.getString(R.string.seconds)

        binding!!.info.text = caption

        runOnUIThread {
            updateDisplay()
            loadProgressDialog?.dismiss()
            binding!!.startMarker.visibility = View.VISIBLE
            binding!!.endMarker.visibility = View.VISIBLE
        }
    }

    @Synchronized
    internal fun updateDisplay() {
        if (isPlaying) {
            val now = player!!.currentPosition
            val frames = binding!!.waveform.millisecondsToPixels(now)

            binding!!.waveform.setPlayback(frames)
            setOffsetGoalNoUpdate(frames - (width shr 1))

            if (now >= playEndMilliseconds)
                handlePause()
        }

        if (!touchDragging) {
            val offsetDelta: Int

            when {
                flingVelocity != 0 -> {
                    offsetDelta = flingVelocity / 30

                    flingVelocity = when {
                        flingVelocity > 80 -> flingVelocity - 80
                        flingVelocity < -80 -> flingVelocity + 80
                        else -> 0
                    }

                    offset += offsetDelta

                    if (offset + (width shr 1) > maxPos) {
                        offset = maxPos - (width shr 1)
                        flingVelocity = 0
                    }

                    if (offset < 0) {
                        offset = 0
                        flingVelocity = 0
                    }

                    offsetGoal = offset
                }

                else -> {
                    val foo = offsetGoal - offset

                    offsetDelta = when {
                        foo > 10 -> foo / 10
                        foo > 0 -> 1
                        foo < -10 -> foo / 10
                        foo < 0 -> -1
                        else -> 0
                    }

                    offset += offsetDelta
                }
            }
        }

        binding!!.waveform.run {
            // СУКА, ЕБАНЫЙ БАГ УКРАЛ 8 ЧАСОВ
            setParameters(viewModel.startPos, viewModel.endPos, this@TrimFragment.offset)
            invalidate()
        }

        var startX = viewModel.startPos - offset - markerLeftInset

        when {
            startX + binding!!.startMarker.width >= 0 -> {
                if (!startVisible)
                    runOnUIThread {
                        startVisible = true
                        binding!!.startMarker.alpha = 1F
                    }
            }

            else -> {
                if (startVisible) {
                    binding!!.startMarker.alpha = 0F
                    startVisible = false
                }
                startX = 0
            }
        }

        var endX = viewModel.endPos - offset - binding!!.endMarker.width + markerRightInset

        when {
            endX + binding!!.endMarker.width >= 0 -> {
                if (!endVisible)
                    runOnUIThread {
                        endVisible = true
                        binding!!.endMarker.alpha = 1F
                    }
            }

            else -> {
                if (endVisible) {
                    binding!!.endMarker.alpha = 0F
                    endVisible = false
                }
                endX = 0
            }
        }

        binding!!.startMarker.layoutParams = RelativeLayout.LayoutParams(
            200,
            200
        ).apply {
            setMargins(
                startX - 100,
                markerTopOffset,
                -binding!!.startMarker.width,
                -binding!!.startMarker.height
            )
        }

        binding!!.endMarker.layoutParams = RelativeLayout.LayoutParams(
            200,
            200
        ).apply {
            setMargins(
                endX + 100,
                binding!!.waveform.measuredHeight - binding!!.endMarker.height - markerBottomOffset,
                -binding!!.startMarker.width,
                -binding!!.startMarker.height
            )
        }
    }

    private fun setPlayButtonImage() {
        binding!!.play.setImageResource(ViewSetter.getPlayButtonImage(isPlaying))
    }

    private fun resetPositions() {
        viewModel.startPos = binding!!.waveform.secondsToPixels(0.0)
        viewModel.endPos = binding!!.waveform.secondsToPixels(15.0)
    }

    private inline val Int.trapped
        get() = when {
            this < 0 -> 0
            this > maxPos -> maxPos
            else -> this
        }

    private fun setOffsetGoal(offset: Int) {
        setOffsetGoalNoUpdate(offset)
        updateDisplay()
    }

    private fun setOffsetGoalNoUpdate(offset: Int) {
        if (touchDragging)
            return

        offsetGoal = offset
        if (offsetGoal + (width shr 1) > maxPos) offsetGoal = maxPos - (width shr 1)
        if (offsetGoal < 0) offsetGoal = 0
    }

    private fun setOffsetGoalStart() = setOffsetGoal(viewModel.startPos - (width shr 1))
    private fun setOffsetGoalStartNoUpdate() = setOffsetGoalNoUpdate(viewModel.startPos - (width shr 1))
    private fun setOffsetGoalEnd() = setOffsetGoal(viewModel.endPos - (width shr 1))
    private fun setOffsetGoalEndNoUpdate() = setOffsetGoalNoUpdate(viewModel.endPos - (width shr 1))

    internal fun formatTime(pixels: Int): String = binding!!.waveform.run {
        when {
            isInitialized -> formatDecimal(pixelsToSeconds(pixels))
            else -> ""
        }
    }

    private fun formatDecimal(x: Double): String {
        var xWhole = x.toInt()
        var xFraction = (100 * (x - xWhole) + 0.5).toInt()

        if (xFraction >= 100) {
            xWhole++
            xFraction -= 100

            if (xFraction < 10)
                xFraction *= 10
        }

        return if (xFraction < 10) "$xWhole.0$xFraction" else "$xWhole.$xFraction"
    }

    @Synchronized
    private fun handlePause() {
        if (player != null && player!!.isPlaying)
            player!!.pause()

        binding!!.waveform.setPlayback(-1)
        isPlaying = false
        setPlayButtonImage()
    }

    @Synchronized
    private fun onPlay(startPosition: Int) {
        if (isPlaying) {
            handlePause()
            return
        }

        if (player != null) {
            try {
                playStartMilliseconds = binding!!.waveform.pixelsToMilliseconds(startPosition)

                playEndMilliseconds = when {
                    startPosition < viewModel.startPos -> binding!!.waveform.pixelsToMilliseconds(viewModel.startPos)
                    startPosition > viewModel.endPos -> binding!!.waveform.pixelsToMilliseconds(maxPos)
                    else -> binding!!.waveform.pixelsToMilliseconds(viewModel.endPos)
                }

                player!!.setOnCompletionListener { handlePause() }

                isPlaying = true
                player!!.seekTo(playStartMilliseconds)
                player!!.start()

                updateDisplay()
                setPlayButtonImage()
            } catch (e: Exception) {
                showFinalAlert(false, R.string.play_error)
            }
        }
    }

    /**
     * Show a "final" alert dialog that will exit the [TrimFragment]
     * after the user clicks on the OK button.  If false
     * is passed, it's assumed to be an error condition, and the
     * dialog is presented as an error, and the stack trace is
     * logged. If true is passed, it's a success message.
     *
     * @param isOk is everything ok
     * @param message message to show (about error or success)
     */

    private fun showFinalAlert(isOk: Boolean, message: CharSequence) {
        val title = resources.getString(
            when {
                isOk -> R.string.success
                else -> R.string.failure
            }
        )

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(
                R.string.ok
            ) { dialog, _ ->
                dialog.dismiss()
                requireActivity().supportFragmentManager.popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Same as [showFinalAlert] but
     * with resource string passed as message
     *
     * @param isOk is everything ok
     * @param messageResourceId message resource to show (about error or success)
     * @see showFinalAlert
     */

    private fun showFinalAlert(isOk: Boolean, messageResourceId: Int) =
        showFinalAlert(isOk, resources.getString(messageResourceId))

    private fun makeAudioFilename(title: CharSequence, extension: String): String {
        val externalRootDir = requireContext()
            .applicationContext
            .getExternalFilesDir(null)!!
            .absolutePath
            .let { if (!it.endsWith("/")) "$it/" else it }
            .split("Android/data/com.dinaraparanid.prima/files/")
            .let { (f, s) -> f + s }

        val subDirectory = "${
            when (newFileKind) {
                FileSaveDialog.FILE_TYPE_MUSIC -> Environment.DIRECTORY_MUSIC
                FileSaveDialog.FILE_TYPE_ALARM -> Environment.DIRECTORY_ALARMS
                FileSaveDialog.FILE_TYPE_NOTIFICATION -> Environment.DIRECTORY_NOTIFICATIONS
                FileSaveDialog.FILE_TYPE_RINGTONE -> Environment.DIRECTORY_RINGTONES
                else -> Environment.DIRECTORY_MUSIC
            }
        }/"

        var parentDir = externalRootDir + subDirectory

        // Create the parent directory
        val parentDirFile = File(parentDir)
        parentDirFile.mkdirs()

        // If we can't write to that special path, try just writing
        // directly to the sdcard
        if (!parentDirFile.isDirectory)
            parentDir = externalRootDir

        // Turn the title into a filename
        val filename = title.filter(Char::isLetterOrDigit)

        // Try to make the filename unique

        val createFile = { i: Int ->
            when (i) {
                0 -> "$parentDir$filename$extension"
                else -> "$parentDir$filename$i$extension"
            }
        }

        return generateSequence(0) { it + 1 }.first {
            val testPath = createFile(it)

            try {
                RandomAccessFile(File(testPath), "r").use { }
                false
            } catch (e: Exception) {
                true
            }
        }.let { createFile(it) }
    }

    internal fun saveAudio(title: CharSequence) {
        val wave = binding!!.waveform
        val startTime = wave.pixelsToSeconds(viewModel.startPos)
        val endTime = wave.pixelsToSeconds(viewModel.endPos)
        val startFrame = wave.secondsToFrames(startTime)
        val endFrame = wave.secondsToFrames(endTime)
        val duration = (endTime - startTime + 0.5).toInt()

        loadProgressDialog = createAndShowAwaitDialog(requireContext(), false)

        saveSoundFileCoroutine = runOnIOThread {
            val outPath = makeAudioFilename(title, ".wav")
            val outFile = File(outPath)

            try {
                // Create the .wav file
                viewModel.soundFile!!.writeWAVFile(outFile, startFrame, endFrame - startFrame)
            } catch (e: Exception) {
                // Creating the .wav file also failed. Stop the progress dialog, show an
                // error message and exit.

                if (outFile.exists())
                    outFile.delete()

                infoContent = e.toString()

                launch(Dispatchers.Main) {
                    loadProgressDialog?.dismiss()

                    showFinalAlert(
                        false, resources.getString(
                            when {
                                e.message != null && e.message == "No space left on device" ->
                                    R.string.no_space_error
                                else -> R.string.write_error
                            }
                        )
                    )
                }
            }

            // Try to load the new file to make sure it worked

            try {
                val listener: SoundFile.ProgressListener = object : SoundFile.ProgressListener {
                    override fun reportProgress(fractionComplete: Double): Boolean {
                        // Do nothing - we're not going to try to
                        // estimate when reloading a saved sound
                        // since it's usually fast, but hard to
                        // estimate anyway.
                        return true // Keep going
                    }
                }

                SoundFile.create(outPath, listener)
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    loadProgressDialog?.dismiss()
                    showFinalAlert(false, resources.getText(R.string.write_error))
                }
            }

            launch(Dispatchers.Main) {
                afterSavingAudio(
                    title,
                    outPath,
                    duration
                )
            }
        }
    }

    private fun afterSavingAudio(
        title: CharSequence,
        outPath: String,
        duration: Int
    ) {
        val outFile = File(outPath)
        val fileSize = outFile.length()

        if (fileSize <= 512) {
            outFile.delete()

            AlertDialog.Builder(requireContext())
                .setTitle(R.string.failure)
                .setMessage(R.string.too_small_error)
                .setPositiveButton(R.string.ok, null)
                .setCancelable(false)
                .show()

            return
        }

        // Create the database record, pointing to the existing file path

        val mimeType = "audio/${
            when {
                outPath.endsWith(".m4a") -> "mp4a-latm"
                outPath.endsWith(".wav") -> "wav"
                else -> "mpeg" // This should never happen
            }
        }"

        // Insert it into the database

        val uri = MediaStore.Audio.Media.getContentUriForPath(outPath)!!
        val newUri = requireActivity().contentResolver.insert(
            uri, ContentValues().apply {
                put(MediaStore.MediaColumns.DATA, outPath)
                put(MediaStore.MediaColumns.TITLE, title.toString())
                put(MediaStore.MediaColumns.SIZE, fileSize)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.Audio.Media.ARTIST, track.artist)
                put(MediaStore.Audio.Media.ALBUM, track.playlist)
                put(MediaStore.Audio.Media.DURATION, duration.toLong() * 1000)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$title.mp3")
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }

                put(
                    MediaStore.Audio.Media.IS_RINGTONE,
                    newFileKind == FileSaveDialog.FILE_TYPE_RINGTONE
                )

                put(
                    MediaStore.Audio.Media.IS_NOTIFICATION,
                    newFileKind == FileSaveDialog.FILE_TYPE_NOTIFICATION
                )

                put(
                    MediaStore.Audio.Media.IS_ALARM,
                    newFileKind == FileSaveDialog.FILE_TYPE_ALARM
                )

                put(
                    MediaStore.Audio.Media.IS_MUSIC,
                    newFileKind == FileSaveDialog.FILE_TYPE_MUSIC
                )
            }
        )!!

        loadProgressDialog?.dismiss()

        // There's nothing more to do with music or an alarm.
        // Show a success message and then quit

        if (newFileKind == FileSaveDialog.FILE_TYPE_MUSIC ||
            newFileKind == FileSaveDialog.FILE_TYPE_ALARM
        ) {
            Toast.makeText(requireContext(), R.string.saved, Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.popBackStack()
            return
        }

        // If it's a notification, give the user the option of making
        // this their default notification. If he says no, we're finished

        if (newFileKind == FileSaveDialog.FILE_TYPE_NOTIFICATION) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.success)
                .setMessage(R.string.set_default_notification)
                .setPositiveButton(R.string.ok) { _, _ ->
                    RingtoneManager.setActualDefaultRingtoneUri(
                        requireContext(),
                        RingtoneManager.TYPE_NOTIFICATION,
                        newUri
                    )

                    requireActivity().supportFragmentManager.popBackStack()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                    requireActivity().supportFragmentManager.popBackStack()
                }
                .setCancelable(false)
                .show()
            return
        }

        // If we get here, that means the type is a ringtone.  There are
        // three choices: make this your default ringtone, assign it to a
        // contact, or do nothing.

        val handler = object : Handler(Looper.myLooper()!!) {
            override fun handleMessage(response: Message) {
                when (response.arg1) {
                    R.id.button_make_default -> {
                        RingtoneManager.setActualDefaultRingtoneUri(
                            requireContext(),
                            RingtoneManager.TYPE_RINGTONE,
                            newUri
                        )

                        Toast.makeText(
                            requireContext(),
                            R.string.default_ringtone_success,
                            Toast.LENGTH_LONG
                        ).show()

                        requireActivity().supportFragmentManager.popBackStack()
                    }

                    R.id.button_choose_contact ->
                        (callbacker as Callbacks).showChooseContactFragment(newUri)

                    else -> requireActivity().supportFragmentManager.popBackStack()
                }
            }
        }

        AfterSaveRingtoneDialog(requireActivity(), Message.obtain(handler)).show()
    }

    private fun onSave() = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
            (requireActivity().application as MainApplication)
                .checkAndRequestManageExternalStoragePermission(this::save)
            Unit
        }

        else -> save()
    }

    private fun save() {
        if (isPlaying)
            handlePause()

        FileSaveDialog(
            requireActivity(),
            track.title,
            Message.obtain(object : Handler(Looper.myLooper()!!) {
                override fun handleMessage(response: Message) {
                    val newTitle = response.obj as CharSequence
                    newFileKind = response.arg1
                    saveAudio(newTitle.correctFileName)
                }
            })
        ).show()
    }

    internal val currentTime: Long
        get() = System.nanoTime() / 1000000
}