package com.daprlabs.aaron.swipedeck.Utility

import android.animation.Animator
import android.view.MotionEvent
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.animation.OvershootInterpolator
import com.daprlabs.aaron.swipedeck.KSwipeDeck

class KSwipeListener(
        private val card: View?,
        private val callback: KSwipeCallback,
        private val initialX: Float,
        private val initialY: Float,
        private val parent: KSwipeDeck,
        private val endOpacity: Float = 0.33F,
        private val rotationDegress: Float = 15F) : View.OnTouchListener {

    private var activePointerId = 0
    private var initialXPress = 0F
    private var initialYPress = 0F
    private var deactivated = false
    var rightView: View? = null
    var leftView: View? = null
    private var click = true


    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (deactivated) return false

        when (event?.action?.and(MotionEvent.ACTION_MASK)) {
            MotionEvent.ACTION_DOWN -> {
                click = true

                // Gesture has begun, cancel any current animations
                v?.clearAnimation()
                activePointerId = event.getPointerId(0)

                val x = event.x
                val y = event.y

                if (event.findPointerIndex(activePointerId) == 0) {
                    callback.cardActionDown()
                }

                initialXPress = x
                initialYPress = y
            }
            MotionEvent.ACTION_MOVE -> {
                // Gesture is in progress
                val pointerIndex = event.findPointerIndex(activePointerId)

                if (pointerIndex == 0) {
                    val xMove = event.getX(pointerIndex)
                    val yMove = event.getY(pointerIndex)

                    // Calculate distance moved
                    val dx = xMove - initialXPress
                    val dy = yMove - initialYPress

                    // In this circumstance consider the motion a click
                    click = Math.abs(dx + dy) <= 5

                    // Check whether we are allowed to drag this card
                    // We don't want to do this at the start of the branch, as we need to check
                    // whether we exceeded moving the threshold first.
                    if (!callback.isDragEnabled()) return false

                    // Throw away the move in this case as it seems to be wrong
                    //TODO: Figure out why this is the case
                    if (initialXPress != 0F || initialYPress != 0F) {
                        // Calculate rotation
                        val x = card?.x ?: 0F
                        val y = card?.y ?: 0F

                        val posX = x + dx
                        val posY = y + dy

                        card?.x = posX
                        card?.y = posY

                        // Set rotation
                        val distObjectX = posX - initialX
                        val rotation = rotationDegress * 2F * distObjectX / parent.width
                        card?.rotation = rotation

                        // Set alphas
                        val alpha = (((posX - parent.paddingLeft) / (parent.width * endOpacity)))
                        rightView?.alpha = alpha
                        leftView?.alpha = alpha
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                // Gesture has finished. Check to see if the card has moved beyond the left or right
                // bounds, or reset card position
                checkCardForEvent()

                if (event.findPointerIndex(activePointerId) == 0) {
                    callback.cardActionUp()
                }

                // Check if this is a click event, and then perform a click.
                // This is a workaround, android doesn't play well with multiple listeners.
                if (click) {
                    v?.performClick()
                }
            }
            else -> return false
        }

        return true
    }

    fun swipeCardLeft(duration: Long = KSwipeDeck.ANIMATION_DURATION): ViewPropertyAnimator? {
        return card?.animate()
                ?.setDuration(duration)
                ?.x(-(parent.width).toFloat())
                ?.y(0F)
                ?.rotation(-30F)
    }

    fun swipeCardRight(duration: Long = KSwipeDeck.ANIMATION_DURATION): ViewPropertyAnimator? {
        return card?.animate()
                ?.setDuration(duration)
                ?.x(parent.width * 2F)
                ?.y(0F)
                ?.rotation(30F)
    }

    fun checkCardForEvent() {
        if (cardBeyondLeftBorder()) {
            swipeCardLeft()
                    ?.setListener(object : Animator.AnimatorListener {
                        override fun onAnimationRepeat(animation: Animator?) {
                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                        }

                        override fun onAnimationEnd(animation: Animator?) {
                            callback.cardOffScreen(card)
                        }

                        override fun onAnimationCancel(animation: Animator?) {
                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                        }

                        override fun onAnimationStart(animation: Animator?) {
                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                        }
                    })
            callback.cardSwipedLeft(card)
            this.deactivated = true
        } else if (cardBeyondRightBorder()) {
            swipeCardRight()
                    ?.setListener(object : Animator.AnimatorListener {
                        override fun onAnimationRepeat(animation: Animator?) {
                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                        }

                        override fun onAnimationEnd(animation: Animator?) {
                            callback.cardOffScreen(card)
                        }

                        override fun onAnimationCancel(animation: Animator?) {
                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                        }

                        override fun onAnimationStart(animation: Animator?) {
                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                        }
                    })
            callback.cardSwipedRight(card)
            this.deactivated = true
        } else {
            resetCardPosition()
        }
    }

    /**
     * Determines if the card's center is beyond the left quarter of the screen.
     */
    private fun cardBeyondLeftBorder(): Boolean {
        val x = card?.x ?: 0F
        val width = card?.width ?: 0

        return (x + (width / 2) < (parent.width / 4F))
    }

    /**
     * Determines if the card's center is beyond the right quarter of the screen.
     */
    private fun cardBeyondRightBorder(): Boolean {
        val x = card?.x ?: 0F
        val width = card?.width ?: 0

        return (x + (width / 2)) > ((parent.width / 4F) * 3)
    }

    private fun resetCardPosition(duration: Long = KSwipeDeck.ANIMATION_DURATION): ViewPropertyAnimator? {
        rightView?.alpha = 0F
        leftView?.alpha = 0F

        //TODO: Figure out why I have to set translationX to 0
        return card?.animate()
                ?.setDuration(duration)
                ?.setInterpolator(OvershootInterpolator(1.5F))
                ?.x(initialX)
                ?.y(initialY)
                ?.rotation(0F)
                ?.translationX(0F)
    }
}