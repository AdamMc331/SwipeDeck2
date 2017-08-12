package com.daprlabs.aaron.swipedeck

import android.view.View
import com.daprlabs.aaron.swipedeck.utility.SwipeCallback
import com.daprlabs.aaron.swipedeck.utility.SwipeListener

/**
 * Represents an individual swipeCard in the deck.
 */
open class SwipeCard(val card: View?, callback: SwipeCallback, private val parent: SwipeDeck) {
    var leftView: View? = null
        private set
    var rightView: View? = null
        private set

    //TODO: What are these even for?
    var positionWithinViewGroup = -1
    var positionWithinAdapter = -1
    var id: Long = 0

    private var swipeListener = SwipeListener(
            this,
            callback,
            parent.paddingLeft,
            parent.paddingTop,
            parent,
            parent.rotationDegrees,
            parent.endOpacity)

    private var swipeDuration = SwipeDeck.ANIMATION_DURATION

    /**
     * Wait for swipeCard to render off screen, do clean up, and remove from the ViewGroup.
     */
    fun cleanupAndRemoveView() {
        card?.postDelayed({ deleteViewFromSwipeDeck() }, swipeDuration)
    }

    /**
     * Removes a view from the parent.
     * TODO: This shouldn't be here
     */
    private fun deleteViewFromSwipeDeck() {
        parent.removeView(card)
        parent.removeFromBuffer(this)
    }

    /**
     * Sets the swipe ability on the swipeCard, but also sets the listener if necessary.
     */
    fun setSwipeEnabled(enabled: Boolean) {
        val listener = if (enabled && parent.swipeEnabled) swipeListener else null
        card?.setOnTouchListener(listener)
    }

    /**
     * Swipes a swipeCard to the left. Needs to set swipeDuration because it's used in [cleanupAndRemoveView].
     */
    fun swipeCardLeft(duration: Long) {
        // Remember how long swipeCard would be animating
        swipeDuration = duration
        // Disable touch events
        setSwipeEnabled(false)
        swipeListener.swipeCardLeft(duration)
    }

    /**
     * Swipes a swipeCard to the right. Needs to set swipeDuration because it's used in [cleanupAndRemoveView].
     */
    fun swipeCardRight(duration: Long) {
        swipeDuration = duration
        setSwipeEnabled(false)
        swipeListener.swipeCardRight(duration)
    }

    /**
     * Sets the image resource to use on the left side of the swipeCard.
     */
    fun setLeftImageResource(leftImageResource: Int) {
        leftView = card?.findViewById(leftImageResource)
        leftView?.alpha = 0f
    }

    /**
     * Sets the image resource to use on the right side of the swipeCard.
     */
    fun setRightImageResource(rightImageResource: Int) {
        rightView = card?.findViewById(rightImageResource)
        rightView?.alpha = 0f
    }
}