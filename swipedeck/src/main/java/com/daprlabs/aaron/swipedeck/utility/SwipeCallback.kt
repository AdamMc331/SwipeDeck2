package com.daprlabs.aaron.swipedeck.utility

import android.view.View

/**
 * Callback methods based on the swipe outcome of a card.
 */
interface SwipeCallback {
    /**
     * Called when a card is swiped to the left.
     *
     * @param[card] The view that was swiped left.
     */
    fun cardSwipedLeft(card: View?)

    /**
     * Called when a card is swiped to the right.
     *
     * @param[card] The view that was swiped right.
     */
    fun cardSwipedRight(card: View?)

    /**
     * Called when a card is moved off the screen.
     *
     * @param[card] The card that moved.
     */
    fun cardOffScreen(card: View?)

    /**
     * Called when there is a MotionEvent.ACTION_DOWN.
     */
    fun cardActionDown()

    /**
     * Called when there is a MotionEvent.ACTION_UP.
     */
    fun cardActionUp()

    /**
     * Check whether we can start dragging current view.
     * @return true if we can start dragging view, false otherwise
     */
    val isDragEnabled: Boolean
}
