package com.daprlabs.aaron.swipedeck.Utility

import android.view.View

interface SwipeCallback {
    fun cardSwipedLeft(card: View?)
    fun cardSwipedRight(card: View?)
    fun cardOffScreen(card: View?)
    fun cardActionDown()
    fun cardActionUp()

    /**
     * Check whether we can start dragging current view.
     * @return true if we can start dragging view, false otherwise
     */
    val isDragEnabled: Boolean
}
