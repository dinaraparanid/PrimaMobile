package com.dinaraparanid.prima.fragments.playing_panel_fragments.trimmer

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
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
import com.dinaraparanid.prima.databinding.FragmentTrimBinding
import com.dinaraparanid.prima.services.MediaScannerService
import com.dinaraparanid.prima.utils.*
import com.dinaraparanid.prima.mvvmp.view.dialogs.AfterSaveRingtoneDialog
import com.dinaraparanid.prima.mvvmp.view.dialogs.TrimmedAudioFileSaveDialog
import com.dinaraparanid.prima.dialogs.QuestionDialog
import com.dinaraparanid.prima.dialogs.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.extensions.*
import com.dinaraparanid.prima.utils.extensions.correctFileName
import com.dinaraparanid.prima.utils.extensions.getBetween
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.extensions.toDp
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.fragments.*
import com.dinaraparanid.prima.utils.polymorphism.fragments.MenuProviderFragment
import com.dinaraparanid.prima.utils.trimmer.MarkerView
import com.dinaraparanid.prima.utils.trimmer.MarkerView.MarkerListener
import com.dinaraparanid.prima.utils.trimmer.SamplePlayer
import com.dinaraparanid.prima.utils.trimmer.WaveformView.WaveformListener
import com.dinaraparanid.prima.utils.trimmer.soundfile.SoundFile
import com.dinaraparanid.prima.mvvmp.androidx.TrimViewModel
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import com.kaopiz.kprogresshud.KProgressHUD
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import ws.schild.jave.Encoder
import ws.schild.jave.MultimediaObject
import ws.schild.jave.encode.AudioAttributes
import ws.schild.jave.encode.EncodingAttributes
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile

/**
 * [CallbacksFragment] to trim audio. Keeps track of
 * the [com.dinaraparanid.prima.utils.trimmer.WaveformView] display,
 * current horizontal offset, marker handles,
 * start / end text boxes, and handles all of the buttons and controls.
 */

class TrimFragment :
    CallbacksFragment<FragmentTrimBinding, MainActivity>(),
    MarkerListener,
    WaveformListener,
    MainActivityFragment by MainActivityFragmentImpl(),
    MenuProviderFragment,
    Rising,
    AsyncContext,
    StatisticsUpdatable {
    interface Callbacks : CallbacksFragment.Callbacks {
        fun showChooseContactFragment(uri: Uri)
    }

    override var binding: FragmentTrimBinding? = null
    override val updateStyle = Statistics::withIncrementedNumberOfTrimmed
    override val menuProvider = defaultMenuProvider

    override val coroutineScope: CoroutineScope
        get() = lifecycleScope

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
    private val mutex = Mutex()

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

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence,
            start: Int, count: Int, after: Int
        ) = Unit

        override fun onTextChanged(
            s: CharSequence,
            start: Int, before: Int, count: Int
        ) = Unit

        @SuppressLint("SyntheticAccessor")
        override fun afterTextChanged(s: Editable) {
            if (binding!!.startText.hasFocus())
                try {
                    viewModel.startPos = binding!!.waveform.secondsToPixels(
                        binding!!.startText.text.toString().toDouble()
                    )
                    runOnUIThread { updateDisplay(isLocking = true) }
                } catch (ignored: NumberFormatException) {
                }

            if (binding!!.endText.hasFocus())
                try {
                    viewModel.endPos = binding!!.waveform.secondsToPixels(
                        binding!!.endText.text.toString().toDouble()
                    )
                    runOnUIThread { updateDisplay(isLocking = true) }
                } catch (ignored: NumberFormatException) {
                }
        }
    }

    private var timerRunnable: Runnable? = object : Runnable {
        @SuppressLint("SyntheticAccessor")
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
         * @param track track to edit
         */

        @JvmStatic
        internal fun newInstance(track: AbstractTrack) = TrimFragment().apply {
            arguments = Bundle().apply { putSerializable(TRACK_KEY, track) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        track = requireArguments().getSerializable(TRACK_KEY) as AbstractTrack
        mainLabelCurText.set(resources.getString(R.string.trim_audio))

        setMainLabelInitializedSync()
        super.onCreate(savedInstanceState)

        if (!isWriteSettingsPermissionGranted) QuestionDialog(R.string.write_settings_why) {
            startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
        }.show(requireActivity().supportFragmentManager, null)

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
            savedInstanceState?.getParcelable(SOUND_FILE_KEY),
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
                viewModel = BasePresenter()
                startText.addTextChangedListener(textWatcher)
                endText.addTextChangedListener(textWatcher)

                play.setOnClickListener {
                    runOnWorkerThread {
                        onPlay(
                            isLocking = true,
                            startPosition = this@TrimFragment.viewModel.startPos
                        )
                    }
                }

                rew.setOnClickListener {
                    when {
                        isPlaying -> {
                            var newPos = player!!.currentPosition - 5000
                            if (newPos < playStartMilliseconds) newPos = playStartMilliseconds
                            runOnWorkerThread { player!!.seekTo(isLocking = true, ms = newPos) }
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
                            runOnWorkerThread { player!!.seekTo(isLocking = true, ms = newPos) }
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
                        runOnUIThread { updateDisplay(isLocking = true) }
                    }
                }

                markEnd.setOnClickListener {
                    if (isPlaying) {
                        this@TrimFragment.viewModel.endPos =
                            waveform.millisecondsToPixels(player!!.currentPosition)
                        runOnUIThread {
                            updateDisplay(isLocking = true)
                            handlePause(isLocking = true)
                        }
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

        runOnUIThread { updateDisplay(isLocking = true) }
        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        return binding!!.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.fragment_trim, menu)

        fragmentActivity.run {
            runOnUIThread {
                while (!isMainLabelInitialized.get())
                    awaitMainLabelInitCondition.blockAsync()

                launch(Dispatchers.Main) {
                    mainLabelCurText = this@TrimFragment.mainLabelCurText.get()
                }
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_save -> onSave()

            R.id.action_reset -> {
                resetPositions()
                offsetGoal = 0
                runOnUIThread { updateDisplay(isLocking = true) }
            }
        }

        return super.onMenuItemSelected(menuItem)
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

    override fun onResume() {
        super.onResume()
        requireActivity().addMenuProvider(menuProvider)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().removeMenuProvider(menuProvider)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(SOUND_FILE_KEY, viewModel.soundFile)
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

        if (alertDialog != null) {
            alertDialog!!.dismiss()
            alertDialog = null
        }

        if (player != null) runOnWorkerThread {
            if (player!!.isPlaying || player!!.isPaused)
                player!!.stop(isLocking = true)

            player!!.release(isLocking = true)
            player = null
        }

        isMainLabelInitialized.set(false)
        System.gc()
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
            runOnUIThread { updateDisplay(isLocking = true) }
        }, 500)
    }

    /** Constructs waveform from given track */
    override fun waveformDraw() {
        width = binding!!.waveform.measuredWidth
        if (offsetGoal != offset && !keyDown || isPlaying || flingVelocity != 0) runOnUIThread {
            updateDisplay(isLocking = true)
        }
    }

    /** On waveform touch */
    override fun waveformTouchStart(x: Float) {
        touchDragging = true
        touchStart = x
        touchInitialOffset = offset
        flingVelocity = 0
        waveformTouchStartMilliseconds = currentTime
    }

    /** Moves waveform */
    override fun waveformTouchMove(x: Float) {
        offset = (touchInitialOffset + (touchStart - x)).toInt().trapped
        runOnUIThread { updateDisplay(isLocking = true) }
    }

    /** On stop waveform touching */
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
                        in playStartMilliseconds until playEndMilliseconds -> runOnWorkerThread {
                            player!!.seekTo(isLocking = true, ms = seekMs)
                        }

                        else -> runOnWorkerThread { handlePause(isLocking = true) }
                    }
                }

                else -> runOnWorkerThread {
                    onPlay(
                        isLocking = true,
                        startPosition = (touchStart + offset).toInt()
                    )
                }
            }
        }
    }

    /** On waveform flung */
    override fun waveformFling(x: Float) {
        touchDragging = false
        offsetGoal = offset
        flingVelocity = (-x).toInt()
        runOnUIThread { updateDisplay(isLocking = true) }
    }

    /** Zooms waveform in (unused todo)  */
    override fun waveformZoomIn() {
        binding!!.waveform.run {
            zoomIn()
            viewModel.startPos = start
            viewModel.endPos = end
            this@TrimFragment.maxPos = maxPos
            this@TrimFragment.offset = offset
            offsetGoal = offset
            runOnUIThread { updateDisplay(isLocking = true) }
        }
    }

    /** Zooms waveform out (unused todo)  */
    override fun waveformZoomOut() {
        binding!!.waveform.run {
            zoomOut()
            viewModel.startPos = start
            viewModel.endPos = end
            this@TrimFragment.maxPos = maxPos
            this@TrimFragment.offset = offset
            offsetGoal = offset
            runOnUIThread { updateDisplay(isLocking = true) }
        }
    }

    override fun markerDraw() = Unit

    /** On maker touch started */
    override fun markerTouchStart(marker: MarkerView, pos: Float) {
        touchDragging = true
        touchStart = pos
        touchInitialStartPos = viewModel.startPos
        touchInitialEndPos = viewModel.endPos
    }

    /** on start moving marker (changes time and updates UI) */
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

        runOnUIThread { updateDisplay(isLocking = true) }
    }

    /** On stop marker touching */
    override fun markerTouchEnd(marker: MarkerView) {
        touchDragging = false

        when (marker) {
            binding!!.startMarker -> setOffsetGoalStart()
            else -> setOffsetGoalEnd()
        }
    }

    /** Updates UI for start marker */
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

        runOnUIThread { updateDisplay(isLocking = true) }
    }

    /** Updates UI for end marker */
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

        runOnUIThread { updateDisplay(isLocking = true) }
    }

    override fun markerEnter(marker: MarkerView) = Unit

    /** On marker clicked */
    override fun markerKeyUp() {
        keyDown = false
        runOnUIThread { updateDisplay(isLocking = true) }
    }

    /** Sets focus to marker */
    override fun markerFocus(marker: MarkerView) {
        keyDown = false

        when (marker) {
            binding!!.startMarker -> setOffsetGoalStartNoUpdate()
            else -> setOffsetGoalEndNoUpdate()
        }

        // Delay updating the display because if this focus was in
        // response to a touch event, we want to receive the touch
        // event too before updating the display.
        handler!!.postDelayed({ runOnUIThread { updateDisplay(isLocking = true) } }, 100)
    }

    /**
     * Rise fragment if playing bar is active.
     * It handles GUI error when playing bar was hiding some content
     */

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

        runOnUIThread { updateDisplay(isLocking = true) }
    }

    /** Loads data from track's file */
    private fun loadFromFile() {
        file = File(filename)

        loadingLastUpdateTime = currentTime
        loadingKeepGoing = true

        val listener = object : SoundFile.ProgressListener {
            @SuppressLint("SyntheticAccessor")
            override fun reportProgress(fractionComplete: Double): Boolean {
                val now = currentTime

                if (now - loadingLastUpdateTime > 100)
                    loadingLastUpdateTime = now

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

    /** Sets data when file was read and update UI */
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
            updateDisplay(isLocking = true)
            loadProgressDialog?.dismiss()
            binding!!.startMarker.visibility = View.VISIBLE
            binding!!.endMarker.visibility = View.VISIBLE
        }
    }

    /** Updates UI without any synchronization */
    private fun updateDisplayNoLock() {
        if (isPlaying) {
            val now = player!!.currentPosition
            val frames = binding!!.waveform.millisecondsToPixels(now)

            binding!!.waveform.setPlayback(frames)
            setOffsetGoalNoUpdate(frames - (width shr 1))

            if (now >= playEndMilliseconds)
                handlePauseNoLock()
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
            // СУКА, ЕБАНЫЙ БАГ УКРАЛ 8 ЧАСОВ (Пасхалка)
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

        val markerSize =
            when (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
                Configuration.SCREENLAYOUT_SIZE_NORMAL -> 70
                Configuration.SCREENLAYOUT_SIZE_LARGE -> 70
                else -> 100
            }.toDp(requireContext())

        binding!!.startMarker.layoutParams = RelativeLayout.LayoutParams(
            markerSize, markerSize
        ).apply {
            setMargins(
                startX - markerSize / 2,
                markerTopOffset,
                -binding!!.startMarker.width,
                -binding!!.startMarker.height
            )
        }

        binding!!.endMarker.layoutParams = RelativeLayout.LayoutParams(
            markerSize, markerSize
        ).apply {
            setMargins(
                endX + markerSize / 2,
                binding!!.waveform.measuredHeight - binding!!.endMarker.height - markerBottomOffset,
                -binding!!.startMarker.width,
                -binding!!.startMarker.height
            )
        }
    }

    /** Updates UI */
    private suspend fun updateDisplay(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { updateDisplayNoLock() }
        else -> mutex.withLock { updateDisplayNoLock() }
    }

    /** Sets image of play button depending on [isPlaying] status */
    private fun setPlayButtonImage() {
        binding!!.play.setImageResource(ViewSetter.getPlayButtonImage(isPlaying))
    }

    /** Returns everything to the default position */
    private fun resetPositions() {
        viewModel.startPos = binding!!.waveform.secondsToPixels(0.0)
        viewModel.endPos = binding!!.waveform.secondsToPixels(15.0)
    }

    /** Gets position between 0 and [maxPos] */
    private inline val Int.trapped
        get() = getBetween(0, maxPos)

    /** Sets offset goal and updates UI */
    private fun setOffsetGoal(offset: Int) {
        setOffsetGoalNoUpdate(offset)
        runOnUIThread { updateDisplay(isLocking = true) }
    }

    /** Sets offset goal without UI update */
    private fun setOffsetGoalNoUpdate(offset: Int) {
        if (touchDragging)
            return

        offsetGoal = offset
        if (offsetGoal + (width shr 1) > maxPos) offsetGoal = maxPos - (width shr 1)
        if (offsetGoal < 0) offsetGoal = 0
    }

    /** Sets offset goal to start position */
    private fun setOffsetGoalStart() =
        setOffsetGoal(viewModel.startPos - (width shr 1))

    /** Sets offset goal to start position without UI Update */
    private fun setOffsetGoalStartNoUpdate() =
        setOffsetGoalNoUpdate(viewModel.startPos - (width shr 1))

    /** Sets offset goal to end position */
    private fun setOffsetGoalEnd() =
        setOffsetGoal(viewModel.endPos - (width shr 1))

    /** Sets offset goal to end position without UI Update */
    private fun setOffsetGoalEndNoUpdate() =
        setOffsetGoalNoUpdate(viewModel.endPos - (width shr 1))

    /** Gets trimmed track's duration */
    private fun formatTime(pixels: Int) = binding!!.waveform.run {
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

    /** Pauses playback without any synchronization */
    private fun handlePauseNoLock() {
        if (player != null && player!!.isPlaying) runOnWorkerThread {
            player!!.pause(isLocking = true)
        }

        binding!!.waveform.setPlayback(-1)
        isPlaying = false
        runOnUIThread { setPlayButtonImage() }
    }

    /** Pauses playback */
    private suspend fun handlePause(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { handlePauseNoLock() }
        else -> handlePauseNoLock()
    }

    /** Starts playback and updates UI without any synchronization */
    private fun onPlayNoLock(startPosition: Int) {
        if (isPlaying) {
            handlePauseNoLock()
            return
        }

        if (player != null) {
            try {
                playStartMilliseconds = binding!!.waveform.pixelsToMilliseconds(startPosition)

                playEndMilliseconds = when {
                    startPosition < viewModel.startPos ->
                        binding!!.waveform.pixelsToMilliseconds(viewModel.startPos)

                    startPosition > viewModel.endPos ->
                        binding!!.waveform.pixelsToMilliseconds(maxPos)

                    else -> binding!!.waveform.pixelsToMilliseconds(viewModel.endPos)
                }

                runOnUIThread {
                    player!!.setOnCompletionListener {
                        runOnWorkerThread { handlePause(isLocking = true) }
                    }

                    isPlaying = true
                    player!!.seekTo(isLocking = true, ms = playStartMilliseconds)
                    player!!.start(isLocking = true)

                    updateDisplay(isLocking = true)
                    setPlayButtonImage()
                }
            } catch (e: Exception) {
                showFinalAlert(false, R.string.play_error)
            }
        }
    }

    /** Starts playback and updates UI */
    private suspend fun onPlay(isLocking: Boolean, startPosition: Int) = when {
        isLocking -> mutex.withLock { onPlayNoLock(startPosition) }
        else -> onPlayNoLock(startPosition)
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
            .setPositiveButton(R.string.ok) { dialog, _ ->
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

    /**
     * Creates file name depending on
     * status of trimmed track (music, alarm, etc.)
     */

    private fun makeAudioFilename(title: CharSequence, extension: String): String {
        val externalRootDir = requireContext().rootPath

        val subDirectory = "${
            when (newFileKind) {
                TrimmedAudioFileSaveDialog.FILE_TYPE_MUSIC -> Environment.DIRECTORY_MUSIC
                TrimmedAudioFileSaveDialog.FILE_TYPE_ALARM -> Environment.DIRECTORY_ALARMS
                TrimmedAudioFileSaveDialog.FILE_TYPE_NOTIFICATION -> Environment.DIRECTORY_NOTIFICATIONS
                TrimmedAudioFileSaveDialog.FILE_TYPE_RINGTONE -> Environment.DIRECTORY_RINGTONES
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
                RandomAccessFile(File(testPath), "r").close()
                false
            } catch (e: Exception) {
                true
            }
        }.let { createFile(it) }
    }

    /** Saves trimmed track as .aac of .wav */
    internal fun saveAudio(title: CharSequence) {
        val wave = binding!!.waveform
        val startTime = wave.pixelsToSeconds(viewModel.startPos)
        val endTime = wave.pixelsToSeconds(viewModel.endPos)
        val startFrame = wave.secondsToFrames(startTime)
        val endFrame = wave.secondsToFrames(endTime)
        val duration = (endTime - startTime + 0.5).toInt()

        loadProgressDialog = createAndShowAwaitDialog(requireContext(), false)

        saveSoundFileCoroutine = runOnIOThread {
            // Try AAC first

            var outPath = makeAudioFilename(title, ".m4a")
            var outFile = File(outPath)
            var fallbackToWAV = false

            try {
                viewModel.soundFile!!.writeMP4AFile(outFile, startFrame, endFrame - startFrame)
            } catch (e: Exception) {
                e.printStackTrace()

                if (outFile.exists())
                    outFile.delete()

                fallbackToWAV = true // Trying to create .wav file instead
            }

            // Try to create a .wav file if creating a .m4a file failed

            if (fallbackToWAV) {
                outPath = makeAudioFilename(title, ".wav")
                outFile = File(outPath)

                try {
                    viewModel.soundFile!!.writeWAVFile(outFile, startFrame, endFrame - startFrame)
                } catch (e: Exception) {
                    // Creating the .wav file failed.
                    // Stop the progress dialog, show an error message and exit.

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

    /**
     * Sets tags and adds track to [MediaStore].
     * Then converts to .mp3.
     * Then continue job if it's a ringtone or notification
     */

    // TODO: OOM Bug

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

        runOnIOThread { updateStatisticsAsync() }

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
                put(MediaStore.Audio.Media.ALBUM, track.album)
                put(MediaStore.Audio.Media.DURATION, duration.toLong() * 1000)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$title.mp3")
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }

                put(
                    MediaStore.Audio.Media.IS_RINGTONE,
                    newFileKind == TrimmedAudioFileSaveDialog.FILE_TYPE_RINGTONE
                )

                put(
                    MediaStore.Audio.Media.IS_NOTIFICATION,
                    newFileKind == TrimmedAudioFileSaveDialog.FILE_TYPE_NOTIFICATION
                )

                put(
                    MediaStore.Audio.Media.IS_ALARM,
                    newFileKind == TrimmedAudioFileSaveDialog.FILE_TYPE_ALARM
                )

                put(
                    MediaStore.Audio.Media.IS_MUSIC,
                    newFileKind == TrimmedAudioFileSaveDialog.FILE_TYPE_MUSIC
                )
            }
        )!!

        val setTagsAndConvertToMp3Task = runOnIOThread {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) try {
                AudioFileIO.read(outFile).run {
                    System.gc()

                    tagOrCreateAndSetDefault?.let { tag ->
                        tag.setField(FieldKey.TITLE, title.toString())
                        tag.setField(FieldKey.ARTIST, track.artist)
                        tag.setField(FieldKey.ALBUM, track.album)

                        AudioFileIO.read(File(track.path))
                            .tagOrCreateAndSetDefault
                            ?.firstArtwork
                            ?.binaryData
                            ?.toBitmap()
                            ?.let {
                                val stream = ByteArrayOutputStream()
                                it.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                val byteArray = stream.toByteArray()
                                tag.deleteArtworkField()

                                tag.setField(
                                    ArtworkFactory
                                        .createArtworkFromFile(outFile)
                                        .apply { binaryData = byteArray }
                                )
                            }
                    }

                    var tries = 20

                    while (tries > 0)
                        try {
                            commit()
                        } catch (e: OutOfMemoryError) {
                            System.gc()
                            tries--
                        }
                }
            } catch (ignored: Exception) {
                // No audio header
            }

            // Starting conversion to .mp3 file

            try {
                val source = File(outPath)
                val newPath = outPath.replace(source.extension, "mp3")

                Encoder().encode(
                    MultimediaObject(source),
                    File(newPath).apply { createNewFile() },
                    EncodingAttributes().apply {
                        setOutputFormat("mp3")
                        setAudioAttributes(AudioAttributes().apply { setCodec("libmp3lame") })
                    }
                )

                requireActivity().sendBroadcast(
                    Intent(MediaScannerService.Broadcast_SCAN_SINGLE_FILE)
                        .putExtra(MediaScannerService.TRACK_TO_SCAN_ARG, newPath)
                )
            } catch (e: Exception) {
                // File isn't created
            }

            requireActivity().sendBroadcast(
                Intent(MediaScannerService.Broadcast_SCAN_SINGLE_FILE)
                    .putExtra(MediaScannerService.TRACK_TO_SCAN_ARG, outPath)
            )
        }

        loadProgressDialog?.dismiss()

        // There's nothing more to do with music or an alarm.
        // Show a success message and then quit

        runOnUIThread {
            if (newFileKind == TrimmedAudioFileSaveDialog.FILE_TYPE_MUSIC ||
                newFileKind == TrimmedAudioFileSaveDialog.FILE_TYPE_ALARM
            ) {
                setTagsAndConvertToMp3Task.join()
                Toast.makeText(requireContext(), R.string.saved, Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.popBackStack()
                return@runOnUIThread
            }

            // If it's a notification, give the user the option of making
            // this their default notification. If he says no, we're finished

            if (newFileKind == TrimmedAudioFileSaveDialog.FILE_TYPE_NOTIFICATION) {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.success)
                    .setMessage(R.string.set_default_notification)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        if (isWriteSettingsPermissionGranted) {
                            RingtoneManager.setActualDefaultRingtoneUri(
                                requireContext(),
                                RingtoneManager.TYPE_NOTIFICATION,
                                newUri
                            )

                            Toast.makeText(
                                requireContext(),
                                R.string.default_notification_success,
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        runOnUIThread {
                            setTagsAndConvertToMp3Task.join()
                            requireActivity().supportFragmentManager.popBackStack()
                        }
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        runOnUIThread {
                            dialog.dismiss()
                            setTagsAndConvertToMp3Task.join()
                            requireActivity().supportFragmentManager.popBackStack()
                        }
                    }
                    .setCancelable(false)
                    .show()
                return@runOnUIThread
            }

            // If we get here, that means the type is a ringtone.  There are
            // three choices: make this your default ringtone, assign it to a
            // contact, or do nothing.

            val afterSaveRingtoneHandler = object : Handler(Looper.myLooper()!!) {
                @SuppressLint("SyntheticAccessor")
                override fun handleMessage(response: Message) {
                    when (AfterSaveRingtoneTarget.values()[response.arg1]) {
                        AfterSaveRingtoneTarget.MAKE_DEFAULT -> {
                            if (isWriteSettingsPermissionGranted) {
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
                            }

                            runOnUIThread {
                                setTagsAndConvertToMp3Task.join()
                                requireActivity().supportFragmentManager.popBackStack()
                            }
                        }

                        AfterSaveRingtoneTarget.SET_TO_CONTACT ->
                            (callbacker as Callbacks).showChooseContactFragment(newUri)

                        AfterSaveRingtoneTarget.IGNORE ->
                            requireActivity().supportFragmentManager.popBackStack()
                    }
                }
            }

            AfterSaveRingtoneDialog(
                requireContext(),
                Message.obtain(afterSaveRingtoneHandler)
            ).show()
        }
    }

    /**
     * Checks if app is allowed to create files (Android 11+)
     * and saves file or requests permission to do so
     */

    private fun onSave() = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
            (requireActivity().application as MainApplication)
                .checkAndRequestManageExternalStoragePermission(this::save)
            Unit
        }

        else -> save()
    }

    /** Pauses playback and start saving dialog */
    private fun save() {
        if (isPlaying) runOnWorkerThread {
            handlePause(isLocking = true)
        }

        TrimmedAudioFileSaveDialog(
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

    private inline val isWriteSettingsPermissionGranted
        get() = Settings.System.canWrite(requireContext().applicationContext)

    private inline val currentTime
        get() = System.nanoTime() / 1000000
}