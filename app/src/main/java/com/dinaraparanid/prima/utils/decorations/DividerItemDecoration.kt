package com.dinaraparanid.prima.utils.decorations

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

class DividerItemDecoration(context: Context, div: Drawable? = null) : ItemDecoration() {

    private val divider = div ?: kotlin.run {
        val styledAttributes: TypedArray = context.obtainStyledAttributes(
            intArrayOf(android.R.attr.listDivider)
        )

        val divider = styledAttributes.getDrawable(0)!!
        styledAttributes.recycle()
        divider
    }

    constructor(context: Context, resId: Int) : this(
        context,
        ContextCompat.getDrawable(context, resId)
    )

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight
        val childCount = parent.childCount

        (0 until childCount).forEach {
            val child = parent.getChildAt(it)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin
            val bottom = top + divider.intrinsicHeight
            divider.setBounds(left, top, right, bottom)
            divider.draw(c)
        }
    }
}