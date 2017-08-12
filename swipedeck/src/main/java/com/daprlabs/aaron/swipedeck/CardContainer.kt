package com.daprlabs.aaron.swipedeck

import com.daprlabs.aaron.swipedeck.utility.SwipeCallback
import com.daprlabs.aaron.swipedeck.utility.SwipeListener

import android.view.View

/**
 * Container for an individual card and relevant interfaces.
 *
 * @param[callback] The callback used for swipe actions against this card.
 * @property[card] The card in this container.
 * @property[parent] The [SwipeDeck] that this card sits in.
 * @property[positionWithinViewGroup] This card's position within the entire view group.
 * @property[positionWithinAdapter] This card's position within the adapter.
 * @property[id] The unique identifier for this card.
 * @property[swipeListener] The callback used to handle swipe events on the card.
 * @property[swipeDuration] The duration for any swipe animations.
 */
open class CardContainer(
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

    /**
     * Wait for card to render off screen, do clean up, and remove from the ViewGroup.
     */
    fun cleanupAndRemoveView() {
        card?.postDelayed({ deleteViewFromSwipeDeck() }, swipeDuration.toLong())
    }

    /**
     * Removes a view from the parent.
     */
    private fun deleteViewFromSwipeDeck() {
        parent.removeView(card)
        parent.removeFromBuffer(this)
    }

    /**
     * Sets the swipe ability on the card, but also sets the listener if necessary.
     */
    fun setSwipeEnabled(enabled: Boolean) {
        val listener = if (enabled && parent.swipeEnabled) swipeListener else null
        card?.setOnTouchListener(listener)
    }

    /**
     * Sets the image resource to use on the left side of the card.
     *
     * TODO: Figure this out. Why is resource passed in? Shouldn't we /know/ the left view ID?
     */
    fun setLeftImageResource(leftImageResource: Int) {
        val left = card?.findViewById(leftImageResource)
        left?.alpha = 0f
        swipeListener.setLeftView(left)
    }

    /**
     * Sets the image resource to use on the right side of the card.
     *
     * TODO: Figure this out. Why is resource passed in? Shouldn't we /know/ the right view ID?
     */
    fun setRightImageResource(rightImageResource: Int) {
        val right = card?.findViewById(rightImageResource)
        right?.alpha = 0f
        swipeListener.setRightView(right)
    }

    /**
     * Swipes a card to the left.
     *
     * TODO: Figure out why this is setting swipeDuration??
     */
    fun swipeCardLeft(duration: Int) {
        // Remember how long card would be animating
        swipeDuration = duration
        // Disable touch events
        setSwipeEnabled(false)
        swipeListener.swipeCardLeft(duration)
    }

    /**
     * Swipes a card to the right.
     *
     * TODO: Figure out why this is setting swipeDuration??
     */
    fun swipeCardRight(duration: Int) {
        swipeDuration = duration
        setSwipeEnabled(false)
        swipeListener.swipeCardRight(duration)
    }
}
