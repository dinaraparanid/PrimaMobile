package com.dinaraparanid.prima.utils

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView

internal class TouchListener(
    context: Context?,
    private val clickListener: OnItemClickListener?
) : RecyclerView.OnItemTouchListener {
    internal interface OnItemClickListener {
        fun onClick(view: View?, index: Int)
    }

    var gestureDetector: GestureDetector =
        GestureDetector(context, object : SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                return true
            }
        })

    override fun onInterceptTouchEvent(recyclerView: RecyclerView, e: MotionEvent): Boolean {
        val child = recyclerView.findChildViewUnder(e.x, e.y)

        if (child != null && clickListener != null && gestureDetector.onTouchEvent(e))
            clickListener.onClick(child, recyclerView.getChildLayoutPosition(child))

        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) = Unit
    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit
}