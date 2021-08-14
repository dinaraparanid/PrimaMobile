package com.dinaraparanid.prima.utils.equalizer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.dinaraparanid.prima.utils.Params
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Circle button to control bass and reverb
 */

internal class AnalogController : View {
    private var midX = 0F
    private var midY = 0F

    private var textPaint: Paint = Paint().apply {
        color = Params.instance.fontColor
        style = Paint.Style.FILL
        textSize = 33F
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private var circlePaint: Paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    internal var circlePaint2: Paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    internal var linePaint: Paint = Paint().apply {
        color = Params.instance.theme.rgb
        strokeWidth = 7F
    }

    private var angle = "0.0"
    private var currDeg = 0F
    private var deg = 3F
    private var downDeg = 0F
    private var listener: OnProgressChangedListener? = null
    internal var label = ""

    internal interface OnProgressChangedListener {
        fun onProgressChanged(progress: Int)
    }

    internal inline fun setOnProgressChangedListener(crossinline action: (Int) -> Unit) {
        listener = object : OnProgressChangedListener {
            override fun onProgressChanged(progress: Int) = action(progress)
        }
    }

    internal constructor(context: Context) : super(context)

    internal constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    internal constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        midX = width / 2F
        midY = height / 2F

        var x: Float
        var y: Float

        val radius = (midX.coerceAtMost(midY) * (14.5F / 16)).toInt()
        val deg2 = 3F.coerceAtLeast(deg)
        val deg3 = deg.coerceAtMost(21F)

        (deg2.toInt()..21).forEach {
            val tmp = it / 24F
            x = midX + (radius * sin(2 * Math.PI * (1.0 - tmp))).toFloat()
            y = midY + (radius * cos(2 * Math.PI * (1.0 - tmp))).toFloat()

            circlePaint.color = Color.BLACK
            canvas.drawCircle(x, y, radius / 15F, circlePaint)
        }

        var i = 3

        while (i <= deg3) {
            val tmp = i / 24F
            x = midX + (radius * sin(2 * Math.PI * (1.0 - tmp))).toFloat()
            y = midY + (radius * cos(2 * Math.PI * (1.0 - tmp))).toFloat()
            canvas.drawCircle(x, y, radius / 15F, circlePaint2)
            i++
        }

        val tmp = deg / 24
        val x1 = midX + (radius * 0.4F * sin(2 * Math.PI * (1F - tmp))).toFloat()
        val y1 = midY + (radius * 0.4F * cos(2 * Math.PI * (1F - tmp))).toFloat()
        val x2 = midX + (radius * 0.6F * sin(2 * Math.PI * (1F - tmp))).toFloat()
        val y2 = midY + (radius * 0.6F * cos(2 * Math.PI * (1F - tmp))).toFloat()

        circlePaint.color = Params.instance.theme.rgb
        canvas.drawCircle(midX, midY, radius * 13F / 15, circlePaint)
        
        circlePaint.color = Color.BLACK
        canvas.drawCircle(midX, midY, radius * 11F / 15, circlePaint)
        canvas.drawText(label, midX, midY + radius * 1.1F, textPaint)
        canvas.drawLine(x1, y1, x2, y2, linePaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        listener!!.onProgressChanged(deg.toInt() - 2)

        if (e.action == MotionEvent.ACTION_DOWN) {
            val dx = e.x - midX
            val dy = e.y - midY

            downDeg = (atan2(dy.toDouble(), dx.toDouble()) * 180 / Math.PI).toFloat()
            downDeg -= 90F

            if (downDeg < 0)
                downDeg += 360F

            downDeg = floor(downDeg / 15)
            return true
        }

        if (e.action == MotionEvent.ACTION_MOVE) {
            val dx = e.x - midX
            val dy = e.y - midY

            currDeg = (atan2(dy.toDouble(), dx.toDouble()) * 180 / Math.PI).toFloat()
            currDeg -= 90F

            if (currDeg < 0)
                currDeg += 360F

            currDeg = floor(currDeg / 15)

            when {
                currDeg == 0F && downDeg == 23F -> {
                    deg++
                    if (deg > 21) deg = 21F
                    downDeg = currDeg
                }

                currDeg == 23F && downDeg == 0F -> {
                    deg--
                    if (deg < 3) deg = 3F
                    downDeg = currDeg
                }

                else -> {
                    deg += currDeg - downDeg

                    deg = when {
                        deg > 21 -> 21F
                        deg < 3 -> 3F
                        else -> deg
                    }

                    downDeg = currDeg
                }
            }

            angle = deg.toString()
            invalidate()
            return true
        }

        return e.action == MotionEvent.ACTION_UP || super.onTouchEvent(e)
    }

    internal var progress: Int
        get() = deg.toInt() - 2
        set(param) {
            deg = param + 2F
        }
}