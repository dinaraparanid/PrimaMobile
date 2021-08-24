package com.dinaraparanid.prima.trimmer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import carbon.widget.ImageView
import com.dinaraparanid.prima.utils.extensions.unwrap
import kotlin.math.sqrt

/**
 * Represents a draggable start or end marker.
 *
 * Most events are passed back to the client class using a
 * listener interface.
 *
 * This class directly keeps track of its own velocity, though,
 * accelerating as the user holds down the left or right arrows
 * while this control is focused.
 */

class MarkerView(context: Context, attrs: AttributeSet) :
    ImageView(context, attrs) {
    interface MarkerListener {
        fun markerTouchStart(marker: MarkerView, pos: Float)
        fun markerTouchMove(marker: MarkerView, pos: Float)
        fun markerTouchEnd(marker: MarkerView)
        fun markerFocus(marker: MarkerView)
        fun markerLeft(marker: MarkerView, velocity: Int)
        fun markerRight(marker: MarkerView, velocity: Int)
        fun markerEnter(marker: MarkerView)
        fun markerKeyUp()
        fun markerDraw()
    }

    private var velocity = 0
    private var listener: Option<MarkerListener> = None

    init {
        isFocusable = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                requestFocus()
                // We use raw x because this window itself is going to
                // move, which will screw up the "local" coordinates
                listener.unwrap().markerTouchStart(this, event.rawX)
            }
            MotionEvent.ACTION_MOVE ->
                // We use raw x because this window itself is going to
                // move, which will screw up the "local" coordinates
                listener.unwrap().markerTouchMove(this, event.rawX)
            MotionEvent.ACTION_UP -> listener.unwrap().markerTouchEnd(this)
        }

        return true
    }

    override fun onFocusChanged(
        gainFocus: Boolean, direction: Int,
        previouslyFocusedRect: Rect?
    ) {
        if (gainFocus && listener.isNotEmpty()) listener.unwrap().markerFocus(this)
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        listener.orNull()?.markerDraw()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        velocity++
        val v = sqrt((1 + (velocity shr 1)).toDouble()).toInt()

        if (listener.isNotEmpty()) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    listener.unwrap().markerLeft(this, v)
                    return true
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    listener.unwrap().markerRight(this, v)
                    return true
                }

                KeyEvent.KEYCODE_DPAD_CENTER -> {
                    listener.unwrap().markerEnter(this)
                    return true
                }
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        velocity = 0
        listener.orNull()?.markerKeyUp()
        return super.onKeyDown(keyCode, event)
    }

    fun setListener(listener: MarkerListener) {
        this.listener = Some(listener)
    }
}