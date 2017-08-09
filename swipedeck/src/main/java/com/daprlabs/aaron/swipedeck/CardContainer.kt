package com.daprlabs.aaron.swipedeck

import com.daprlabs.aaron.swipedeck.Utility.SwipeCallback
import com.daprlabs.aaron.swipedeck.Utility.SwipeListener

import android.view.View

class CardContainer(
        val card: View?,
        private val parent: SwipeDeck,
        callback: SwipeCallback) {

    var positionWithinViewGroup = -1
    var positionWithinAdapter = -1
    var id: Long = 0

    private var swipeListener: SwipeListener = SwipeListener(
                card,
                callback,
                parent.paddingLeft,
                parent.paddingTop,
                parent,
                parent.endOpacity,
                parent.rotationDegrees
        )
    private var swipeDuration = SwipeDeck.ANIMATION_DURATION

    fun cleanupAndRemoveView() {
        //wait for card to render off screen, do cleanup and remove from viewgroup
        card?.postDelayed({ deleteViewFromSwipeDeck() }, swipeDuration.toLong())
    }

    private fun deleteViewFromSwipeDeck() {
        parent.removeView(card)
        parent.removeFromBuffer(this)
    }

    fun setSwipeEnabled(enabled: Boolean) {
        //also checks in case user doesn't want to be able to swipe the card freely
        val listener = if (enabled && parent.swipeEnabled) swipeListener else null
        card?.setOnTouchListener(listener)
    }

    fun setLeftImageResource(leftImageResource: Int) {
        val left = card?.findViewById(leftImageResource)
        left?.alpha = 0f
        swipeListener.setLeftView(left)

    }

    fun setRightImageResource(rightImageResource: Int) {
        val right = card?.findViewById(rightImageResource)
        right?.alpha = 0f
        swipeListener.setRightView(right)
    }

    fun swipeCardLeft(duration: Int) {
        // Remember how long card would be animating
        swipeDuration = duration
        // Disable touch events
        setSwipeEnabled(false)
        swipeListener.swipeCardLeft(duration)
    }

    fun swipeCardRight(duration: Int) {
        swipeDuration = duration
        setSwipeEnabled(false)
        swipeListener.swipeCardRight(duration)
    }
}
