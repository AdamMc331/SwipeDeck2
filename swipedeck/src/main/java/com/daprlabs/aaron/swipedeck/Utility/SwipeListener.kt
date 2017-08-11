package com.daprlabs.aaron.swipedeck.Utility

import android.animation.Animator
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.animation.OvershootInterpolator
import com.daprlabs.aaron.swipedeck.SwipeDeck


class SwipeListener(
        private val card: View?,
        private var callback: SwipeCallback,
        private val initialX: Int,
        private val initialY: Int,
        private val parent: SwipeDeck,
        private val opacityEnd: Float = 0.33F,
        private val rotationDegrees: Float = 15F) : View.OnTouchListener {

    private var mActivePointerId: Int = 0
    private var initialXPress: Float = 0.toFloat()
    private var initialYPress: Float = 0.toFloat()
    private var deactivated: Boolean = false
    private var rightView: View? = null
    private var leftView: View? = null
    private var click = true

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (deactivated) return false

        when (event.action and MotionEvent.ACTION_MASK) {

            MotionEvent.ACTION_DOWN -> {
                click = true

                //gesture has begun
                val x: Float = event.x
                val y: Float = event.y

                //cancel any current animations
                v.clearAnimation()

                mActivePointerId = event.getPointerId(0)

                if (event.findPointerIndex(mActivePointerId) == 0) {
                    callback.cardActionDown()
                }

                initialXPress = x
                initialYPress = y
            }

            MotionEvent.ACTION_MOVE -> {
                //gesture is in progress
                val pointerIndex = event.findPointerIndex(mActivePointerId)

                if (pointerIndex != 0) return true

                val xMove = event.getX(pointerIndex)
                val yMove = event.getY(pointerIndex)

                //calculate distance moved
                val dx = xMove - initialXPress
                val dy = yMove - initialYPress

                //in this circumstance consider the motion a click
                if (Math.abs(dx + dy) > 5) {
                    click = false
                }

                // Check whether we are allowed to drag this card
                // We don't want to do this at the start of the branch, as we need to check whether we exceeded
                // moving threshold first
                if (!callback.isDragEnabled) return false

                //TODO: Timber
                Log.d("X:", "" + v.x)

                //throw away the move in this case as it seems to be wrong
                //TODO: figure out why this is the case
                if (initialXPress.toInt() == 0 && initialYPress.toInt() == 0) {
                    //makes sure the pointer is valid
                    return true
                }

                //calc rotation here
                val x = card?.x ?: 0F
                val y = card?.y ?: 0F

                val posX = x + dx
                val posY = y + dy

                card?.x = posX
                card?.y = posY

                //card.setRotation
                val distObjectX = posX - initialX
                val rotation = rotationDegrees * 2f * distObjectX / parent.width
                card?.rotation = rotation

                //set alpha of left and right image
                val alpha = (posX - parent.paddingLeft) / (parent.width * opacityEnd)
                //float alpha = (((posX - paddingLeft) / parentWidth) * ALPHA_MAGNITUDE );
                //Log.i("alpha: ", Float.toString(alpha));
                rightView?.alpha = alpha
                leftView?.alpha = -alpha
            }

            MotionEvent.ACTION_UP -> {
                //gesture has finished
                //check to see if card has moved beyond the left or right bounds or reset
                //card position
                checkCardForEvent()

                if (event.findPointerIndex(mActivePointerId) == 0) {
                    callback.cardActionUp()
                }

                //check if this is a click event and then perform a click
                //this is a workaround, android doesn't play well with multiple listeners
                if (click) {
                    v.performClick()
                }
            }
            else -> return false
        }

        return true
    }

    fun checkCardForEvent() {

        val listener = object : Animator.AnimatorListener {

            override fun onAnimationStart(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {
                callback.cardOffScreen(card)
            }

            override fun onAnimationCancel(animation: Animator) {
                Log.d("SwipeListener", "Animation Cancelled")
            }

            override fun onAnimationRepeat(animation: Animator) {}
        }

        //TODO: Single callback
        if (cardBeyondLeftBorder()) {
            animateOffScreenLeft(SwipeDeck.ANIMATION_DURATION)?.setListener(listener)
            callback.cardSwipedLeft(card)
            this.deactivated = true
        } else if (cardBeyondRightBorder()) {
            animateOffScreenRight(SwipeDeck.ANIMATION_DURATION)?.setListener(listener)
            callback.cardSwipedRight(card)
            this.deactivated = true
        } else {
            resetCardPosition()
        }
    }

    private fun cardBeyondLeftBorder(): Boolean {
        //check if cards middle is beyond the left quarter of the screen
        val x = card?.x ?: 0F
        val width = card?.width ?: 0

        return x + width / 2 < parent.width / 4f
    }

    //TODO: Abstract these two methods
    private fun cardBeyondRightBorder(): Boolean {
        //check if card middle is beyond the right quarter of the screen
        val x = card?.x ?: 0F
        val width = card?.width ?: 0

        return x + width / 2 > parent.width / 4f * 3
    }

    private fun resetCardPosition(): ViewPropertyAnimator? {
        rightView?.alpha = 0F
        leftView?.alpha = 0F

        //todo: figure out why i have to set translationX to 0
        return card?.animate()
                ?.setDuration(SwipeDeck.ANIMATION_DURATION.toLong())
                ?.setInterpolator(OvershootInterpolator(1.5f))
                ?.x(initialX.toFloat())
                ?.y(initialY.toFloat())
                ?.rotation(0f)
                ?.translationX(0f)
    }

    private fun animateOffScreenLeft(duration: Int): ViewPropertyAnimator? {
        return animateOffScreen(duration.toLong(), (-parent.width).toFloat(), 0F, 30F)
    }

    private fun animateOffScreenRight(duration: Int): ViewPropertyAnimator? {
        return animateOffScreen(duration.toLong(), (parent.width * 2).toFloat(), 0F, 30F)
    }

    private fun animateOffScreen(duration: Long, x: Float, y: Float, rotation: Float): ViewPropertyAnimator? {
        return card?.animate()
                ?.setDuration(duration)
                ?.x(x)
                ?.y(y)
                ?.rotation(rotation)
    }

    fun swipeCardLeft(duration: Int) {
        animateOffScreenLeft(duration)
    }

    fun swipeCardRight(duration: Int) {
        animateOffScreenRight(duration)
    }

    fun setRightView(image: View?) {
        this.rightView = image
    }

    fun setLeftView(image: View?) {
        this.leftView = image
    }
}
