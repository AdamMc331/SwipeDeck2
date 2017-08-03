package com.daprlabs.aaron.swipedeck.Utility

import android.view.View

interface KSwipeCallback {
    fun cardSwipedLeft(card: View?)
    fun cardSwipedRight(card: View?)
    fun cardOffScreen(card: View?)
    fun cardActionDown()
    fun cardActionUp()

    /**
     * Check whether we are enabled to drag the current view
     */
    fun isDragEnabled(): Boolean
}