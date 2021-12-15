package com.dinaraparanid.prima.utils

import android.animation.ValueAnimator
import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.graphics.Canvas
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableWrapper
import android.graphics.drawable.VectorDrawable

/**
 * [DrawableWrapper] for menu icon's animation
 */

internal class AnimationDrawableWrapper(resources: Resources, drawable: Drawable) :
    DrawableWrapper(vectorToBitmapDrawableIfNeeded(resources, drawable)) {
    private var rotation = 0f
    private val _bounds = Rect()

    private companion object {
        @JvmStatic
        fun vectorToBitmapDrawableIfNeeded(
            resources: Resources,
            drawable: Drawable
        ): Drawable {
            var drwbl = drawable

            if (drwbl is VectorDrawable) {
                val b = Bitmap.createBitmap(
                    drwbl.intrinsicWidth,
                    drwbl.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )

                val c = Canvas(b)

                drwbl.setBounds(0, 0, c.width, c.height)
                drwbl.draw(c)
                drwbl = BitmapDrawable(resources, b)
            }

            return drwbl
        }
    }

    override fun draw(canvas: Canvas) {
        copyBounds(_bounds)
        canvas.save()
        canvas.rotate(rotation, _bounds.centerX().toFloat(), _bounds.centerY().toFloat())
        super.draw(canvas)
        canvas.restore()
    }

    internal fun setRotation(valueAnimator: ValueAnimator) {
        rotation = (valueAnimator.animatedValue as Int).toFloat() % 360
        invalidateSelf()
    }
}