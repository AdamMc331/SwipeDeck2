package com.daprlabs.aaron.swipedeck.utility

import android.animation.Animator
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.animation.OvershootInterpolator
import com.daprlabs.aaron.swipedeck.SwipeCard
import com.daprlabs.aaron.swipedeck.SwipeDeck

/**
 * Handles swipe actions on a Card.
 */
class SwipeListener(private val swipeCard: SwipeCard?,
                    private var callback: SwipeCallback,
                    private val initialX: Int,
                    private val initialY: Int,
                    private val parent: SwipeDeck,
                    private val rotationDegrees: Float = 15F,
                    private val opacityEnd: Float = 0.33F): View.OnTouchListener {

    private var activePointerId: Int = 0
    private var initialXPress: Float = 0.toFloat()
    private var initialYPress: Float = 0.toFloat()
    private var deactivated: Boolean = false
    private var click: Boolean = false

    private val cardHorizontalCenter: Float
        get() {
            val x = swipeCard?.card?.x ?: 0F
            val width = swipeCard?.card?.width ?: 0

            return (x + width / 2)
        }

    private val leftBorder: Float
        get() = parent.width / 4F

    private val rightBorder: Float
        get() = leftBorder * 3

    private val beyondLeftBorder: Boolean
        get() = cardHorizontalCenter < leftBorder

    private val beyondRightBorder: Boolean
        get() = cardHorizontalCenter > rightBorder

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        // If swipeCard is deactivated, don't handle the event.
        if (deactivated) return false

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                // By default, assume that a down press is a click.
                click = true

                // Gesture has begun, get initial coordinates.
                initialXPress = event.x
                initialYPress = event.y

                // Cancel any current animations, store current pointer.
                v.clearAnimation()
                activePointerId = event.getPointerId(0)

                if (event.findPointerIndex(activePointerId) == 0) {
                    callback.cardActionDown()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // Gesture is in progress, get new pointer. If it's not the first pointer, ignore the motion
                val pointerIndex = event.findPointerIndex(activePointerId)
                if (pointerIndex != 0) return true

                // Calculate distance moved from original coordinates.
                val xMove = event.getX(pointerIndex)
                val yMove = event.getY(pointerIndex)
                val dx = xMove - initialXPress
                val dy = yMove - initialYPress

                // If we've moved more than 5 pixels, this is no longer a click.
                if (Math.abs(dx + dy) > 5) {
                    click = false
                }

                // Check whether we are allowed to drag this swipeCard
                // We don't want to do this at the start of the branch, as we need to check whether we exceeded
                // moving threshold first
                if (!callback.isDragEnabled) return false

                //throw away the move in this case as it seems to be wrong
                //TODO: figure out why this is the case
                if (initialXPress.toInt() == 0 && initialYPress.toInt() == 0) {
                    //makes sure the pointer is valid
                    return true
                }

                // Calculate what our rotation should be based on position
                val x = swipeCard?.card?.x ?: 0F
                val y = swipeCard?.card?.y ?: 0F

                val posX = x + dx
                val posY = y + dy

                swipeCard?.card?.x = posX
                swipeCard?.card?.y = posY

                val distObjectX = posX - initialX
                val rotation = rotationDegrees * 2f * distObjectX / parent.width
                swipeCard?.card?.rotation = rotation

                // Set alpha of left and right image based on how far we've swiped
                val alpha = (posX - parent.paddingLeft) / (parent.width * opacityEnd)
                swipeCard?.rightView?.alpha = alpha
                swipeCard?.leftView?.alpha = -alpha
            }

            MotionEvent.ACTION_UP -> {
                // Gesture is done, check to see if swipeCard is beyond left or right bounds, or reset.
                checkCardForEvent()

                if (event.findPointerIndex(activePointerId) == 0) {
                    callback.cardActionUp()
                }

                // If this is a click, then perform a click.
                if (click) {
                    v.performClick()
                }
            }
            else -> return false
        }

        return true
    }

    /**
     * Checks if we should preform a full left or right swipe.
     */
    fun checkCardForEvent() {

        //TODO: More logging.
        val listener = object : Animator.AnimatorListener {

            override fun onAnimationStart(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {
                callback.cardOffScreen(swipeCard?.card)
            }

            override fun onAnimationCancel(animation: Animator) {
                Log.d("SwipeListener", "Animation Cancelled")
            }

            override fun onAnimationRepeat(animation: Animator) {}
        }

        if (beyondLeftBorder) {
            animateOffScreenLeft(SwipeDeck.ANIMATION_DURATION)?.setListener(listener)
            callback.cardSwipedLeft(swipeCard?.card)
            this.deactivated = true
        } else if (beyondRightBorder) {
            animateOffScreenRight(SwipeDeck.ANIMATION_DURATION)?.setListener(listener)
            callback.cardSwipedRight(swipeCard?.card)
            this.deactivated = true
        } else {
            resetCardPosition()
        }
    }

    /**
     * Animates the swipeCard until it is off the left of the screen.
     *
     * @return An animator that animates the swipeCard off screen.
     */
    private fun animateOffScreenLeft(duration: Long): ViewPropertyAnimator? {
        return animateOffScreen(duration, (-parent.width).toFloat(), 0F, 30F)
    }

    /**
     * Animates the swipeCard until it is off the right of the screen.
     *
     * @return An animator that animates the swipeCard off screen.
     */
    private fun animateOffScreenRight(duration: Long): ViewPropertyAnimator? {
        return animateOffScreen(duration, (parent.width * 2).toFloat(), 0F, 30F)
    }

    /**
     * Animates the swipeCard off screen.
     *
     * @param[duration] The time (in MS) to run the animation.
     * @param[x] The X coordinate to animate the swipeCard to.
     * @param[y] The Y coordinate to animate the swipeCard to.
     * @param[rotation] The rotation that should be applied to the swipeCard.
     *
     * @return An animator that animates the swipeCard off screen.
     */
    private fun animateOffScreen(duration: Long, x: Float, y: Float, rotation: Float): ViewPropertyAnimator? {
        return swipeCard?.card?.animate()
                ?.setDuration(duration)
                ?.x(x)
                ?.y(y)
                ?.rotation(rotation)
    }

    /**
     * Resets the swipeCard to its original position.
     *
     * @return An Animator that animates the swipeCard back to its original spot.
     */
    private fun resetCardPosition(): ViewPropertyAnimator? {
        swipeCard?.rightView?.alpha = 0F
        swipeCard?.leftView?.alpha = 0F

        //TODO: Why does translationX have to be set to 0?
        return swipeCard?.card?.animate()
                ?.setDuration(SwipeDeck.ANIMATION_DURATION)
                ?.setInterpolator(OvershootInterpolator(1.5f))
                ?.x(initialX.toFloat())
                ?.y(initialY.toFloat())
                ?.rotation(0f)
                ?.translationX(0f)
    }

    /**
     * Swipes the card off to the left.
     *
     * @param[duration] The length of time (in MS) the swipe should take.
     */
    fun swipeCardLeft(duration: Long) {
        animateOffScreenLeft(duration)
    }

    /**
     * Swipes the card off to the right.
     *
     * @param[duration] The length of time (in MS) the swipe should take.
     */
    fun swipeCardRight(duration: Long) {
        animateOffScreenRight(duration)
    }

}