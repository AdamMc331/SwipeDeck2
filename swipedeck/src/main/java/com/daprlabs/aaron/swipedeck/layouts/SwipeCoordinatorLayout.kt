package com.daprlabs.aaron.swipedeck.layouts

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.View
import com.daprlabs.aaron.swipedeck.SwipeDeck

/**
 * Base CoordinatorLayout for a swipe deck.
 */
class SwipeCoordinatorLayout(context: Context, attributeSet: AttributeSet? = null): CoordinatorLayout(context, attributeSet) {
    init {
        clipChildren = false
    }

    // This is so that versions of Android pre-lollipop will render
    // the card stack above everything else within the layout.
    override fun onFinishInflate() {
        super.onFinishInflate()

        val children = ArrayList<View>()
        var swipeDeck: View? = null
        (0..childCount - 1)
                .map { getChildAt(it) }
                .forEach { if (it is SwipeDeck) swipeDeck = it else children.add(it) }

        removeAllViews()
        removeAllViewsInLayout()

        children.forEach { addViewInLayout(it, -1, it.layoutParams, true) }

        if (swipeDeck != null) addViewInLayout(swipeDeck, -1, swipeDeck?.layoutParams, true)

        invalidate()
        requestLayout()
    }
}