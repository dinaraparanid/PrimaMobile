package com.dinaraparanid.prima.trimmer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.dinaraparanid.prima.trimmer.soundfile.SoundFile
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.unwrap
import kotlin.math.abs

/**
 * WaveformView is an Android view that displays a visual representation
 * of an audio waveform.  It retrieves the frame gains from a CheapSoundFile
 * object and recomputes the shape contour at several zoom levels.
 *
 * This class doesn't handle selection or any of the touch interactions
 * directly, so it exposes a listener interface.  The class that embeds
 * this view should add itself as a listener and make the view scroll
 * and respond to other events appropriately.
 *
 * WaveformView doesn't actually handle selection, but it will just display
 * the selected part of the waveform in a different color.
 */

internal class WaveformView(context: Context, attrs: AttributeSet) :
    View(context, attrs) {
    interface WaveformListener {
        fun waveformTouchStart(x: Float)
        fun waveformTouchMove(x: Float)
        fun waveformTouchEnd()
        fun waveformFling(x: Float)
        fun waveformDraw()
        fun waveformZoomIn()
        fun waveformZoomOut()
    }

    private val gridPaint = Paint().apply {
        isAntiAlias = false
        color = Params.instance.primaryColor
    }

    private val selectedLinePaint = Paint().apply {
        isAntiAlias = false
        color = Params.instance.primaryColor
    }

    private val unselectedLinePaint = Paint().apply {
        isAntiAlias = false
        color = Params.instance.fontColor
    }

    private val unselectedBackgroundLinePaint = Paint().apply {
        isAntiAlias = false
        color = Params.instance.secondaryColor
    }

    private val borderLinePaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 1.5F
        pathEffect = DashPathEffect(floatArrayOf(3F, 2F), 0F)
        color = Params.instance.secondaryColor
    }

    private val playbackLinePaint = Paint().apply {
        isAntiAlias = false
        color = Params.instance.secondaryColor
    }

    private val timeCodePaint = Paint().apply {
        textSize = 12F
        isAntiAlias = true
        color = Params.instance.fontColor
        setShadowLayer(2F, 1F, 1F, Params.instance.fontColor)
    }

    private var soundFile: Option<SoundFile> = None
    private var lenByZoomLevel: Option<IntArray> = None
    private var valuesByZoomLevel: Option<Array<Option<DoubleArray>>> = None
    private var heightsAtThisZoomLevel: Option<IntArray> = None

    internal var zoomLevel = 0
        private set

    private var numZoomLevels = 0
    private var sampleRate = 0
    private var samplesPerFrame = 0

    private lateinit var zoomFactorByZoomLevel: DoubleArray

    internal var offset = 0
        private set

    internal var start = 0
        private set

    internal var end = 0
        private set

    internal var isInitialized = false
        private set

    private var playbackPos = -1
    private var density: Float = 1.0F
    private var initialScaleSpan = 0F
    private var listener: Option<WaveformListener> = None

    private val gestureDetector = GestureDetector(
        context,
        object : SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                vx: Float,
                vy: Float
            ): Boolean {
                listener.unwrap().waveformFling(vx)
                return true
            }
        }
    )

    private val scaleGestureDetector = ScaleGestureDetector(
        context,
        object : SimpleOnScaleGestureListener() {
            override fun onScaleBegin(d: ScaleGestureDetector): Boolean {
                initialScaleSpan = abs(d.currentSpanX)
                return true
            }

            override fun onScale(d: ScaleGestureDetector): Boolean {
                val scale = abs(d.currentSpanX)

                if (scale - initialScaleSpan > 40) {
                    listener.unwrap().waveformZoomIn()
                    initialScaleSpan = scale
                }

                if (scale - initialScaleSpan < -40) {
                    listener.unwrap().waveformZoomOut()
                    initialScaleSpan = scale
                }

                return true
            }

            override fun onScaleEnd(d: ScaleGestureDetector) = Unit
        }
    )

    init {
        isFocusable = false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)

        if (gestureDetector.onTouchEvent(event))
            return true

        when (event.action) {
            MotionEvent.ACTION_DOWN -> listener.unwrap().waveformTouchStart(event.x)
            MotionEvent.ACTION_MOVE -> listener.unwrap().waveformTouchMove(event.x)
            MotionEvent.ACTION_UP -> listener.unwrap().waveformTouchEnd()
        }

        return true
    }

    internal val hasSoundFile
        get() = soundFile.isNotEmpty()

    internal fun setSoundFile(soundFile: SoundFile) {
        this.soundFile = Some(soundFile)
        sampleRate = this.soundFile.unwrap().sampleRate
        samplesPerFrame = SoundFile.SAMPLES_PER_FRAME
        computeDoublesForAllZoomLevels()
        heightsAtThisZoomLevel = None
    }

    fun setZoomLevel(zoomLevel: Int) {
        while (zoomLevel > zoomLevel)
            zoomIn()

        while (zoomLevel < zoomLevel)
            zoomOut()
    }

    private inline val canZoomIn
        get() = zoomLevel > 0

    internal fun zoomIn() {
        if (canZoomIn) {
            zoomLevel--
            start *= 2
            end *= 2
            heightsAtThisZoomLevel = None
            offset = ((offset + (measuredWidth / 2)) * 2) - (measuredWidth / 2)
            if (offset < 0) offset = 0
            invalidate()
        }
    }

    private inline val canZoomOut
        get() = zoomLevel < numZoomLevels - 1

    internal fun zoomOut() {
        if (canZoomOut) {
            zoomLevel++
            start /= 2
            end /= 2
            var offsetCenter = (offset + (measuredWidth / 2)) / 2
            offsetCenter /= 2
            offset = offsetCenter - measuredWidth / 2
            if (offset < 0) offset = 0
            heightsAtThisZoomLevel = None
            invalidate()
        }
    }

    internal val maxPos: Int
        get() = lenByZoomLevel.unwrap()[zoomLevel]

    internal fun secondsToFrames(seconds: Double): Int =
        (1.0 * seconds * sampleRate / samplesPerFrame + 0.5).toInt()

    internal fun secondsToPixels(seconds: Double): Int =
        (zoomFactorByZoomLevel[zoomLevel] * seconds * sampleRate / samplesPerFrame + 0.5).toInt()

    internal fun pixelsToSeconds(pixels: Int): Double =
        pixels * samplesPerFrame.toDouble() / (sampleRate * zoomFactorByZoomLevel[zoomLevel])

    internal fun millisecondsToPixels(ms: Int): Int = (ms * 1.0 * sampleRate *
            zoomFactorByZoomLevel[zoomLevel] / (1000.0 * samplesPerFrame) + 0.5).toInt()

    internal fun pixelsToMilliseconds(pixels: Int): Int = (pixels * (1000.0 * samplesPerFrame) /
            (sampleRate * zoomFactorByZoomLevel[zoomLevel]) + 0.5).toInt()

    internal fun setParameters(start: Int, end: Int, offset: Int) {
        this.start = start
        this.end = end
        this.offset = offset
    }

    internal fun setPlayback(pos: Int) {
        playbackPos = pos
    }

    internal fun setListener(listener: WaveformListener) {
        this.listener = Some(listener)
    }

    internal fun recomputeHeights(density: Float) {
        heightsAtThisZoomLevel = None
        this.density = density
        timeCodePaint.textSize = (12 * density).toInt().toFloat()
        invalidate()
    }

    private fun drawWaveformLine(
        canvas: Canvas,
        x: Int, y0: Int, y1: Int,
        paint: Paint
    ) = canvas.drawLine(x.toFloat(), y0.toFloat(), x.toFloat(), y1.toFloat(), paint)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (soundFile.isEmpty()) return
        if (heightsAtThisZoomLevel.isEmpty()) computeIntsForThisZoomLevel()

        // Draw waveform
        val measuredWidth = measuredWidth
        val measuredHeight = measuredHeight
        val start = offset
        var width = heightsAtThisZoomLevel.unwrap().size - start
        val ctr = measuredHeight / 2

        if (width > measuredWidth)
            width = measuredWidth

        // Draw grid
        val onePixelInSecs = pixelsToSeconds(1)
        val onlyEveryFiveSecs = onePixelInSecs > 1.0 / 50.0
        var fractionalSecs = offset * onePixelInSecs
        var integerSecs = fractionalSecs.toInt()

        repeat(width) {
            fractionalSecs += onePixelInSecs
            val integerSecsNew = fractionalSecs.toInt()

            if (integerSecsNew != integerSecs) {
                integerSecs = integerSecsNew
                if (!onlyEveryFiveSecs || integerSecs % 5 == 0) {
                    canvas.drawLine(
                        (it + 1).toFloat(),
                        0F,
                        (it + 1).toFloat(),
                        measuredHeight.toFloat(),
                        gridPaint
                    )
                }
            }
        }

        // Draw waveform

        // TODO: ЗДЕСЬ

        repeat(width) {
            val paint = when {
                it + start >= this.start && it + start < end -> selectedLinePaint

                else -> {
                    drawWaveformLine(
                        canvas, it, 0, measuredHeight,
                        unselectedBackgroundLinePaint
                    )
                    unselectedLinePaint
                }
            }

            drawWaveformLine(
                canvas, it,
                ctr - heightsAtThisZoomLevel.unwrap()[start + it],
                ctr + 1 + heightsAtThisZoomLevel.unwrap()[start + it],
                paint
            )

            if (it + start == playbackPos)
                canvas.drawLine(
                    it.toFloat(),
                    0F,
                    it.toFloat(),
                    measuredHeight.toFloat(),
                    playbackLinePaint
                )
        }

        // If we can see the right edge of the waveform, draw the
        // non-waveform area to the right as unselected

        (width until measuredWidth).forEach {
            drawWaveformLine(
                canvas, it, 0, measuredHeight,
                unselectedBackgroundLinePaint
            )
        }

        // Draw borders

        canvas.drawLine(
            this.start - offset + 0.5F, 30F,
            this.start - offset + 0.5F, measuredHeight.toFloat(),
            borderLinePaint
        )

        canvas.drawLine(
            end - offset + 0.5F, 0F,
            end - offset + 0.5F, (measuredHeight - 30).toFloat(),
            borderLinePaint
        )

        // Draw time code

        var timeCodeIntervalSecs = 1.0

        if (timeCodeIntervalSecs / onePixelInSecs < 50)
            timeCodeIntervalSecs = 5.0

        if (timeCodeIntervalSecs / onePixelInSecs < 50)
            timeCodeIntervalSecs = 15.0

        // Draw grid

        fractionalSecs = offset * onePixelInSecs
        var integerTimeCode = (fractionalSecs / timeCodeIntervalSecs).toInt()

        repeat(width) {
            fractionalSecs += onePixelInSecs
            integerSecs = fractionalSecs.toInt()

            val integerTimeCodeNew = (fractionalSecs / timeCodeIntervalSecs).toInt()

            if (integerTimeCodeNew != integerTimeCode) {
                integerTimeCode = integerTimeCodeNew

                val timeCodeMinutes = "${integerSecs / 60}"
                var timeCodeSeconds = "${integerSecs % 60}"

                if (integerSecs % 60 < 10)
                    timeCodeSeconds = "0$timeCodeSeconds"

                val timeCodeStr = "$timeCodeMinutes:$timeCodeSeconds"
                val offset = (0.5 * timeCodePaint.measureText(timeCodeStr)).toFloat()

                canvas.drawText(
                    timeCodeStr,
                    (it + 1) - offset,
                    (12 * density).toInt().toFloat(),
                    timeCodePaint
                )
            }
        }

        listener.orNull()?.waveformDraw()
    }

    /**
     * Called once when a new sound file is added
     */

    private fun computeDoublesForAllZoomLevels() {
        val numFrames = soundFile.unwrap().numFrames
        val frameGains = soundFile.unwrap().frameGains.unwrap()
        val smoothedGains = DoubleArray(numFrames)

        when {
            numFrames == 1 -> smoothedGains[0] = frameGains[0].toDouble()

            numFrames == 2 -> {
                smoothedGains[0] = frameGains[0].toDouble()
                smoothedGains[1] = frameGains[1].toDouble()
            }

            numFrames > 2 -> {
                smoothedGains[0] = (frameGains[0] / 2.0 + frameGains[1] / 2.0)

                (1 until numFrames - 1).forEach {
                    smoothedGains[it] =
                        (frameGains[it - 1] / 3.0 + frameGains[it] / 3.0 + frameGains[it + 1] / 3.0)
                }

                smoothedGains[numFrames - 1] =
                    (frameGains[numFrames - 2] / 2.0 + frameGains[numFrames - 1] / 2.0)
            }
        }

        // Make sure the range is no more than 0 - 255

        var maxGain = 1.0

        repeat(numFrames) {
            if (smoothedGains[it] > maxGain)
                maxGain = smoothedGains[it]
        }

        val scaleFactor = if (maxGain > 255.0) 255 / maxGain else 1.0

        // Build histogram of 256 bins and figure out the new scaled max

        maxGain = 0.0
        val gainHist = IntArray(256)

        repeat(numFrames) {
            var smoothedGain = (smoothedGains[it] * scaleFactor).toInt()
            if (smoothedGain < 0) smoothedGain = 0
            if (smoothedGain > 255) smoothedGain = 255
            if (smoothedGain > maxGain) maxGain = smoothedGain.toDouble()
            gainHist[smoothedGain]++
        }

        // Re-calibrate the min to be 5%

        var minGain = 0.0
        var sum = 0

        while (minGain < 255 && sum < numFrames / 20) {
            sum += gainHist[minGain.toInt()]
            minGain++
        }

        // Re-calibrate the max to be 99%

        sum = 0

        while (maxGain > 2 && sum < numFrames / 100) {
            sum += gainHist[maxGain.toInt()]
            maxGain--
        }

        // Compute the heights

        val heights = DoubleArray(numFrames)
        val range = maxGain - minGain

        repeat(numFrames) {
            var value = (smoothedGains[it] * scaleFactor - minGain) / range
            if (value < 0.0) value = 0.0
            if (value > 1.0) value = 1.0
            heights[it] = value * value
        }

        numZoomLevels = 5
        lenByZoomLevel = Some(IntArray(5))
        zoomFactorByZoomLevel = DoubleArray(5)
        valuesByZoomLevel = Some(Array(5) { None })

        // Level 0 is doubled, with interpolated values

        lenByZoomLevel.unwrap()[0] = numFrames * 2
        zoomFactorByZoomLevel[0] = 2.0
        valuesByZoomLevel.unwrap()[0] = Some(DoubleArray(lenByZoomLevel.unwrap()[0]))

        if (numFrames > 0) {
            valuesByZoomLevel.unwrap()[0].unwrap()[0] = 0.5 * heights[0]
            valuesByZoomLevel.unwrap()[0].unwrap()[1] = heights[0]
        }

        (1 until numFrames).forEach {
            valuesByZoomLevel.unwrap()[0].unwrap()[it * 2] = 0.5 * (heights[it - 1] + heights[it])
            valuesByZoomLevel.unwrap()[0].unwrap()[(it * 2) + 1] = heights[it]
        }

        // Level 1 is normal

        lenByZoomLevel.unwrap()[1] = numFrames
        valuesByZoomLevel.unwrap()[1] = Some(DoubleArray(lenByZoomLevel.unwrap()[1]))
        zoomFactorByZoomLevel[1] = 1.0

        repeat(lenByZoomLevel.unwrap()[1]) {
            valuesByZoomLevel.unwrap()[1].unwrap()[it] = heights[it]
        }

        // 3 more levels are each halved

        (2..4).forEach { i ->
            lenByZoomLevel.unwrap()[i] = lenByZoomLevel.unwrap()[i - 1] / 2
            valuesByZoomLevel.unwrap()[i] = Some(DoubleArray(lenByZoomLevel.unwrap()[i]))
            zoomFactorByZoomLevel[i] = zoomFactorByZoomLevel[i - 1] / 2.0

            repeat(lenByZoomLevel.unwrap()[i]) {
                valuesByZoomLevel.unwrap()[i].unwrap()[it] =
                    0.5 * (valuesByZoomLevel.unwrap()[i - 1].unwrap()[2 * it] +
                            valuesByZoomLevel.unwrap()[i - 1].unwrap()[2 * it + 1])
            }
        }

        zoomLevel = when {
            numFrames > 5000 -> 3
            numFrames > 1000 -> 2
            numFrames > 300 -> 1
            else -> 0
        }

        this.isInitialized = true
    }

    /**
     * Called the first time we need to draw when the zoom level has changed
     * or the screen is resized
     */

    private fun computeIntsForThisZoomLevel() {
        val halfHeight = measuredHeight / 2 - 1
        heightsAtThisZoomLevel = Some(IntArray(lenByZoomLevel.unwrap()[zoomLevel]))

        repeat(lenByZoomLevel.unwrap()[zoomLevel]) {
            heightsAtThisZoomLevel.unwrap()[it] =
                (valuesByZoomLevel.unwrap()[zoomLevel].unwrap()[it] * halfHeight).toInt()
        }
    }
}