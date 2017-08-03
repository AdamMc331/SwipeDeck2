package com.daprlabs.aaron.swipedeck

import android.view.View
import com.daprlabs.aaron.swipedeck.Utility.KSwipeCallback
import com.daprlabs.aaron.swipedeck.Utility.KSwipeListener

open class KCardContainer(val card: View?, private val parent: KSwipeDeck, private val callback: KSwipeCallback) {
    var positionWithinViewGroup = -1
    var positionWithinAdapter = -1
    var id = 0L
    private var swipeListener = KSwipeListener(card, callback, parent.paddingLeft.toFloat(), parent.paddingTop.toFloat(), parent)
    private var swipeDuration = KSwipeDeck.ANIMATION_DURATION

    fun cleanupAndRemoveView() {
        // Wait for card to render off screen, and remove
        card?.postDelayed({ deleteViewFromSwipeDeck() }, swipeDuration.toLong())
    }

    private fun deleteViewFromSwipeDeck() {
        parent.removeView(card)
        parent.removeFromBuffer(this)
    }

    fun setSwipeEnabled(enabled: Boolean) {
        val listener = if (enabled && parent.swipeEnabled) swipeListener else null
        card?.setOnTouchListener(listener)
    }

    fun setLeftImageResource(resource: Int) {
        val left = card?.findViewById(resource)
        left?.alpha = 0F
        swipeListener.leftView = left
    }

    fun setRightImageResource(resource: Int) {
        val right = card?.findViewById(resource)
        right?.alpha = 0F
        swipeListener.rightView = right
    }

    fun swipeCardLeft(duration: Long) {
        swipeDuration = duration
        setSwipeEnabled(false)
        swipeListener.swipeCardLeft(duration)
    }

    fun swipeCardRight(duration: Long) {
        swipeDuration = duration
        setSwipeEnabled(false)
        swipeListener.swipeCardRight(duration)
    }
}