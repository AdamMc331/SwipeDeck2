package com.daprlabs.aaron.swipedeck

import android.content.Context
import android.database.DataSetObserver
import android.os.Build
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.FrameLayout
import com.daprlabs.aaron.swipedeck.Utility.KDeck
import com.daprlabs.aaron.swipedeck.Utility.KSwipeCallback

/**
 * SwipeDeck maintains each of the cards.
 */
class KSwipeDeck(context: Context, attributeSet: AttributeSet): CoordinatorLayout(context, attributeSet) {
    private val previewLayoutId: Int
    private val numberOfSimultaneousCards: Int
    private val cardSpacing: Float
    private var hasStableIds = false
    private var leftImageResource = 0
    private var rightImageResource = 0
    private var adapterIndex = 0
    var endOpacity: Float
    var rotationDegrees: Float
    var renderAbove: Boolean
    var swipeEnabled: Boolean

    private var adapter: Adapter? = null
    private var observer: DataSetObserver? = null
    private lateinit var deck: KDeck<KCardContainer>
    private var callback: SwipeDeck.SwipeDeckCallback? = null
    private var buffer: MutableList<KCardContainer> = ArrayList()

    init {
        val a = context.theme.obtainStyledAttributes(attributeSet, R.styleable.SwipeDeck2, 0, 0)

        numberOfSimultaneousCards = a.getInt(R.styleable.SwipeDeck2_max_visible, 3)
        endOpacity = a.getFloat(R.styleable.SwipeDeck2_opacity_end, 0.33F)
        rotationDegrees = a.getFloat(R.styleable.SwipeDeck2_rotation_degrees, 15F)
        cardSpacing = a.getDimension(R.styleable.SwipeDeck2_card_spacing, 15F)
        renderAbove = a.getBoolean(R.styleable.SwipeDeck2_render_above, true)
        swipeEnabled = a.getBoolean(R.styleable.SwipeDeck2_swipe_enabled, true)
        previewLayoutId = a.getResourceId(R.styleable.SwipeDeck2_preview_layout, -1)

        deck = KDeck(object : KDeck.KDeckEventListener {
            override fun itemAddedFront(item: Any?) {
                deck.first?.setSwipeEnabled(true)

                if (deck.size > numberOfSimultaneousCards) {
                    deck.removeLast()
                    adapterIndex--
                }

                renderDeck()
            }

            override fun itemAddedBack(item: Any?) {
                deck.first?.setSwipeEnabled(true)
                renderDeck()
            }

            override fun itemRemovedFront(item: Any?) {
                val container = item as KCardContainer
                buffer.add(container)

                // Enable swipe in the next card container
                if (deck.size > 0) deck.first?.setSwipeEnabled(true)
                container.cleanupAndRemoveView()

                // Pull in the next view (if available)
                addNextView()
                renderDeck()
            }

            override fun itemRemovedBack(item: Any?) {
                val container = item as CardContainer
                container.card.animate().setDuration(100).alpha(0F)
            }
        })

        // Set clipping of view parent to false so cards render outside their view boundary.
        // Don't clip to padding.
        clipToPadding = false
        clipChildren = false
        setWillNotDraw(false)

        // If render above is set, make sure everything in this view renders above other views
        // outside of it.
        if (renderAbove) ViewCompat.setTranslationZ(this, Float.MAX_VALUE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (isInEditMode && previewLayoutId != -1) {
            for (i in (numberOfSimultaneousCards - 1)..0) {
                val view = LayoutInflater.from(context).inflate(previewLayoutId, this, false)
                val params = view.layoutParams as FrameLayout.LayoutParams //TODO: Is this a fair assumption?

                // All cards are placed in absolute coordinates, so disable gravity
                params.gravity = Gravity.NO_GRAVITY

                // We can't use translations here, for some reason it's not rendered properly in preview
                val offset = (i * cardSpacing).toInt()
                params.topMargin = offset
                view.layoutParams = params
                addViewInLayout(view, -1, params, true)
            }

            setZTranslations()
        }
    }

    fun setAdapter(adapter: Adapter) {
        this.adapter?.unregisterDataSetObserver(observer)

        hasStableIds = adapter.hasStableIds()
        this.adapter = adapter

        observer = object : DataSetObserver() {
            override fun onChanged() {
                super.onChanged()

                // Handle data set changes
                // If we need to add any cards at this point, add the cards
                // (The amount of cards in the deck is less than the max number of cards to display)
                if (deck.size < numberOfSimultaneousCards) {
                    for (i in deck.size..numberOfSimultaneousCards) {
                        addNextView()
                    }
                }

                // If the adapter has been emptied, empty the view and reset the index
                if (adapter.count == 0) {
                    deck.clear()
                    adapterIndex = 0
                }
            }

            override fun onInvalidated() {
                // Reset state, remove views, and request layout
                deck.clear()
                removeAllViews()
                requestLayout()
            }
        }

        adapter.registerDataSetObserver(observer)
        requestLayout()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        if (isInEditMode) return

        // If we don't have an adapter, we don't need to do anything
        if (adapter == null || adapter?.count == 0) {
            removeAllViewsInLayout()
            return
        }

        // Pull in views from adapter
        for (i in deck.size..numberOfSimultaneousCards) {
            addNextView()
        }
    }

    private fun addNextView() {
        if (adapterIndex < (adapter?.count ?: 0)) {
            val newBottomChild = adapter?.getView(adapterIndex, null, this)
            newBottomChild?.setLayerType(View.LAYER_TYPE_HARDWARE, null)

            newBottomChild?.alpha = 0F
            newBottomChild?.y = paddingTop.toFloat()

            val viewId = adapter?.getItemId(adapterIndex)

            val card = KCardContainer(newBottomChild, this, KCardContainerCallback(viewId ?: 0))
            card.id = viewId ?: 0
            card.positionWithinAdapter = adapterIndex

            if (leftImageResource != 0) card.setLeftImageResource(leftImageResource)
            if (rightImageResource != 0) card.setRightImageResource(rightImageResource)

            deck.addLast(card)
            adapterIndex++
        }
    }

    fun removeFromBuffer(container: KCardContainer) {
        this.buffer.remove(container)
    }

    private fun setZTranslations() {
        //this is only needed to add shadows to cardviews on > lollipop
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val count = childCount
            for (i in 0..count - 1) {
                getChildAt(i).translationZ = (i * 10).toFloat()
            }
        }
    }

    private fun renderDeck() {
        // We remove all the views and re-add them so that the z translation is correct
        removeAllViews()

        for (i in (deck.size - 1) downTo 0) {
            val container = deck.get(i)
            val card = container.card
            val params = card?.layoutParams ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            addViewInLayout(card, -1, params, true)
            val itemWidth = width - (paddingLeft + paddingRight)
            val itemHeight = height - (paddingTop + paddingBottom)
            card?.measure(MeasureSpec.EXACTLY.or(itemWidth), MeasureSpec.EXACTLY.or(itemHeight))
        }

        // If there's still a card animating in the buffer, make sure it's re-added after removing
        // all views.
        // Cards in buffer go from older to newer in our deck, newer cards are placed
        // below older cards. We need to start with newer cards, so reverse.
        //TODO: This is the same as the deck above, can be abstracted out.
        for (i in buffer.size - 1 downTo 0) {
            val card = buffer[i].card
            val params = card?.layoutParams ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            addViewInLayout(card, -1, params, true)
            addViewInLayout(card, -1, params, true)
            val itemWidth = width - (paddingLeft + paddingRight)
            val itemHeight = height - (paddingTop + paddingBottom)
            card?.measure(MeasureSpec.EXACTLY.or(itemWidth), MeasureSpec.EXACTLY.or(itemHeight))
        }

        positionCards()
    }

    private fun positionCards() {
        setZTranslations()
        for (i in 0..deck.size - 1) {
            animateCardPosition(deck.get(i).card, deck.get(i).positionWithinViewGroup)
        }
    }

    private fun animateCardPosition(card: View?, position: Int) {
        val offset = (position * cardSpacing)
        card?.animate()
                ?.setDuration(ANIMATION_DURATION)
                ?.y(paddingTop + offset)
                ?.alpha(1.0f)
    }

    companion object {
        val ANIMATION_DURATION = 200L
    }

    private inner class KCardContainerCallback(private val viewId: Long) : KSwipeCallback {


        override fun cardSwipedLeft(card: View?) {
            //TODO: Check if card swiped is equal to top card
            deck.removeFirst()
            callback?.cardSwipedLeft(viewId)
        }

        override fun cardSwipedRight(card: View?) {
            //TODO: Same check as left
            deck.removeFirst()
            callback?.cardSwipedRight(viewId)
        }

        override fun cardOffScreen(card: View?) {
            //TODO: Log
        }

        override fun cardActionDown() {
            //TODO: Log
        }

        override fun cardActionUp() {
            //TODO: Log
        }

        override fun isDragEnabled(): Boolean {
            // Enabled by default, but return callback if it exists
            return callback?.isDragEnabled(viewId) ?: true
        }
    }
}