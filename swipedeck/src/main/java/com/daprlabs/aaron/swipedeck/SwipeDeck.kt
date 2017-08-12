package com.daprlabs.aaron.swipedeck

import android.content.Context
import android.database.DataSetObserver
import android.os.Build
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.FrameLayout
import com.daprlabs.aaron.swipedeck.utility.Deck
import com.daprlabs.aaron.swipedeck.utility.SwipeCallback
import java.util.*

/**
 * Maintains a deck of cards that can be swiped.
 *
 * @property[previewLayoutId] Preview layout when swipeCard is attached??
 * @property[numberOfSimultaneousCards] The number of cards that will be seen in the stack at a time.
 * @property[cardSpacing] The spacing between each swipeCard.
 * @property[buffer] The cards to be displayed. TODO: Discover how this is different from deck.
 * @property[deck] The cards to be displayed. TODO: Discover how this is different from buffer.
 * @property[mHasStableIds] Whether [adapter] has stable IDs or not.
 * @property[adapter] An adapter for all the cards to display.
 * @property[observer] A watcher that handles changes to the dataset.
 * @property[callback] The callback to use when cards are swiped. TODO: This exists in CardContainer.
 * @property[leftImageResource] The image resource on the left of a swipeCard. TODO: This exists in CardContainer.
 * @property[rightImageResource] The image resource on the right of a swipeCard. TODO: This exists in CardContainer.
 * @property[endOpacity] The opacity of the swipeCard at the end of a swipe.
 * @property[rotationDegrees] The degrees a swipeCard should rotate as its being swiped.
 * @property[renderAbove] Whether the swipe deck should be rendered above everything else.
 * @property[swipeEnabled] Whether or not the cards can be swiped.
 * @property[adapterIndex] The current index of [adapter].
 */
open class SwipeDeck(context: Context, attrs: AttributeSet) : CoordinatorLayout(context, attrs) {
    private val previewLayoutId: Int
    private val numberOfSimultaneousCards: Int
    private val cardSpacing: Float
    private val buffer = ArrayList<SwipeCard>()

    private lateinit var deck: Deck<SwipeCard>
    private var mHasStableIds: Boolean = false
    private var adapter: Adapter? = null
    private var observer: DataSetObserver? = null
    private var callback: SwipeDeckCallback? = null
    private var leftImageResource: Int = 0
    private var rightImageResource: Int = 0

    var endOpacity: Float = 0F
    var rotationDegrees: Float = 0F
    var renderAbove: Boolean = false
    var swipeEnabled: Boolean = false
    var adapterIndex = 0

    init {
        val a = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.SwipeDeck2,
                0, 0)

        numberOfSimultaneousCards = a.getInt(R.styleable.SwipeDeck2_max_visible, 3)
        endOpacity = a.getFloat(R.styleable.SwipeDeck2_opacity_end, 0.33f)
        rotationDegrees = a.getFloat(R.styleable.SwipeDeck2_rotation_degrees, 15f)
        cardSpacing = a.getDimension(R.styleable.SwipeDeck2_card_spacing, 15f)
        renderAbove = a.getBoolean(R.styleable.SwipeDeck2_render_above, true)
        swipeEnabled = a.getBoolean(R.styleable.SwipeDeck2_swipe_enabled, true)
        previewLayoutId = a.getResourceId(R.styleable.SwipeDeck2_preview_layout, -1)

        deck = Deck<SwipeCard>(object : Deck.DeckEventListener {
            override fun itemAddedFront(item: Any) {
                deck.first?.setSwipeEnabled(swipeEnabled)

                // If our deck is too large now, remove the last item.
                if (deck.size() > numberOfSimultaneousCards) {
                    deck.removeLast()
                    adapterIndex--
                }
                renderDeck()
            }

            //TODO: Figure out why this doesn't care about deck size?
            override fun itemAddedBack(item: Any) {
                deck.first?.setSwipeEnabled(swipeEnabled)
                renderDeck()
            }

            override fun itemRemovedFront(item: Any) {
                val container = item as SwipeCard

                buffer.add(container)
                //enable swipe in the next cardContainer
                deck.first?.setSwipeEnabled(swipeEnabled)

                container.cleanupAndRemoveView()
                //pull in the next view (if available)
                addNextView()
                renderDeck()
            }

            override fun itemRemovedBack(item: Any) {
                (item as SwipeCard).card
                        ?.animate()
                        ?.setDuration(100)
                        ?.alpha(0f)
            }
        })

        //set clipping of view parent to false so cards render outside their view boundary
        //make sure not to clip to padding
        clipToPadding = false
        clipChildren = false
        this.setWillNotDraw(false)

        //if render above is set make sure everything in this view renders above other views
        //outside of it.
        if (renderAbove) {
            ViewCompat.setTranslationZ(this, java.lang.Float.MAX_VALUE)
        }//todo: make an else here possibly
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isInEditMode && previewLayoutId != -1) {
            for (i in numberOfSimultaneousCards - 1 downTo 0) {
                val view = LayoutInflater.from(context).inflate(previewLayoutId, this, false)
                val params = view.layoutParams as FrameLayout.LayoutParams

                val offset = (i * cardSpacing).toInt()
                // All cards are placed in absolute coordinates, so disable gravity if we have any
                params.gravity = Gravity.NO_GRAVITY
                // We can't user translations here, for some reason it's not rendered properly in preview
                params.topMargin = offset
                view.layoutParams = params
                addViewInLayout(view, -1, params, true)
            }
            setZTranslations()
        }
    }

    fun setAdapter(adapter: Adapter) {
        this.adapter?.unregisterDataSetObserver(observer)

        mHasStableIds = adapter.hasStableIds()
        this.adapter = adapter
        observer = object : DataSetObserver() {
            override fun onChanged() {
                super.onChanged()
                //handle data set changes
                //if we need to add any cards at this point (ie. the amount of cards in the deck
                //is less than the max number of cards to display) add the cards.

                val deckSize = deck.size()
                //only perform action if there are less cards on screen than NUMBER_OF_CARDS
                if (deckSize < numberOfSimultaneousCards) {
                    for (i in deckSize..numberOfSimultaneousCards - 1) {
                        addNextView()
                    }
                }
                //if the adapter has been emptied empty the view and reset adapterIndex
                if (adapter.count == 0) {
                    deck.clear()
                    adapterIndex = 0
                }
            }

            override fun onInvalidated() {
                //reset state, remove views and request layout
                //nextAdapterCard = 0;
                deck.clear()
                removeAllViews()
                requestLayout()
            }
        }
        adapter.registerDataSetObserver(observer)
        requestLayout()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (isInEditMode) return

        // if we don't have an adapter, we don't need to do anything
        if (adapter == null || adapter?.count == 0) {
            //nextAdapterCard = 0;
            removeAllViewsInLayout()
            return
        }
        //pull in views from the adapter at the position the top of the deck is set to
        //stop when you get to for cards or the end of the adapter
        val deckSize = deck.size()
        for (i in deckSize..numberOfSimultaneousCards - 1) {
            addNextView()
        }
    }

    private fun addNextView() {
        if (adapterIndex < (adapter?.count ?: 0)) {
            val newBottomChild = adapter?.getView(adapterIndex, null, this)
            newBottomChild?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            //todo: i'm setting the swipeCard to invisible initially and making it visible when i animate
            //later
            newBottomChild?.alpha = 0f
            newBottomChild?.y = paddingTop.toFloat()
            val viewId = adapter?.getItemId(adapterIndex) ?: 0

            val card = SwipeCard(newBottomChild, CardContainerCallback(viewId), this)

            card.positionWithinAdapter = adapterIndex

            if (leftImageResource != 0) {
                card.setLeftImageResource(leftImageResource)
            }
            if (rightImageResource != 0) {
                card.setRightImageResource(rightImageResource)
            }

            card.id = viewId

            deck.addLast(card)
            adapterIndex++
        }
    }


    private fun addLastView() {
        //get the position of the swipeCard prior to the swipeCard atop the deck
        val positionOfLastCard: Int

        //if there's a swipeCard on the deck get the swipeCard before it, otherwise the last swipeCard is one
        //before the adapter index.
        if (deck.size() > 0) {
            positionOfLastCard = deck.first?.positionWithinAdapter?.minus(1) ?: 0
        } else {
            positionOfLastCard = adapterIndex - 1
        }
        if (positionOfLastCard >= 0) {
            val newBottomChild = adapter?.getView(positionOfLastCard, null, this)
            newBottomChild?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            //todo: i'm setting the swipeCard to invisible initially and making it visible when i animate
            //later
            newBottomChild?.alpha = 0f
            newBottomChild?.y = paddingTop.toFloat()

            val viewId = adapter?.getItemId(positionOfLastCard) ?: 0

            val card = SwipeCard(newBottomChild, CardContainerCallback(viewId), this)

            if (leftImageResource != 0) {
                card.setLeftImageResource(leftImageResource)
            }
            if (rightImageResource != 0) {
                card.setRightImageResource(rightImageResource)
            }

            card.id = viewId

            deck.addFirst(card)
            card.positionWithinAdapter = positionOfLastCard
        }
    }

    private fun renderDeck() {
        //we remove all the views and re add them so that the Z translation is correct
        removeAllViews()
        for (i in deck.size() - 1 downTo 0) {
            val container = deck[i]
            val card = container.card
            val params = card?.layoutParams ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)

            addViewInLayout(card, -1, params, true)
            val itemWidth = width - (paddingLeft + paddingRight)
            val itemHeight = height - (paddingTop + paddingBottom)
            card?.measure(View.MeasureSpec.EXACTLY or itemWidth, View.MeasureSpec.EXACTLY or itemHeight)
        }
        //if there's still a swipeCard animating in the buffer, make sure it's re added after removing all views
        // cards in buffer go from older ones to newer
        // in our deck, newer cards are placed below older cards
        // we need to start with new cards, so older cards would be above them
        for (i in buffer.indices.reversed()) {
            val card = buffer[i].card
            val params= card?.layoutParams ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)

            addViewInLayout(card, -1, params, true)
            val itemWidth = width - (paddingLeft + paddingRight)
            val itemHeight = height - (paddingTop + paddingBottom)
            card?.measure(View.MeasureSpec.EXACTLY or itemWidth, View.MeasureSpec.EXACTLY or itemHeight)
        }

        positionCards()
    }

    private fun positionCards() {
        setZTranslations()
        for (i in 0..deck.size() - 1) {
            animateCardPosition(deck[i].card, deck[i].positionWithinViewGroup)
        }
    }

    protected open fun animateCardPosition(card: View?, position: Int) {
        val offset = (position * cardSpacing).toInt().toFloat()
        card?.animate()
                ?.setDuration(ANIMATION_DURATION)
                ?.y(paddingTop + offset)
                ?.alpha(1.0f)
    }

    fun setCallback(callback: SwipeDeckCallback) {
        this.callback = callback
    }

    /**
     * Swipe top swipeCard to the left side.
     *
     * @param duration animation duration in milliseconds
     */
    fun swipeTopCardLeft(duration: Long) {
        if (deck.size() > 0) {
            deck[0].swipeCardLeft(duration)
            callback?.cardSwipedLeft(deck[0].id)
            deck.removeFirst()
        }
    }

    /**
     * Swipe swipeCard to the right side.
     *
     * @param duration animation duration in milliseconds
     */
    fun swipeTopCardRight(duration: Long) {
        if (deck.size() > 0) {
            deck[0].swipeCardRight(duration)
            callback?.cardSwipedRight(deck[0].id)
            deck.removeFirst()
        }
    }

    fun unSwipeCard() {
        addLastView()
    }

    /**
     * Get item id associated with the swipeCard on top of the deck.
     *
     * @return item id of the swipeCard on the top of the stack or -1 if deck is empty
     */
    val topCardItemId: Long?
        get() {
            if (deck.size() > 0) {
                return deck.first?.id
            } else {
                return -1
            }
        }

    fun removeFromBuffer(container: SwipeCard) {
        this.buffer.remove(container)
    }

    fun setLeftImage(imageResource: Int) {
        leftImageResource = imageResource
    }

    fun setRightImage(imageResource: Int) {
        rightImageResource = imageResource
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

    interface SwipeDeckCallback {
        fun cardSwipedLeft(itemId: Long)

        fun cardSwipedRight(itemId: Long)

        /**
         * Check whether we can start dragging view with provided id.
         *
         * @param itemId id of the swipeCard returned by adapter's [Adapter.getItemId]
         * *
         * @return true if we can start dragging view, false otherwise
         */
        fun isDragEnabled(itemId: Long): Boolean
    }

    private inner class CardContainerCallback(private val viewId: Long) : SwipeCallback {

        override fun cardSwipedLeft(card: View?) {
            Log.d(TAG, "swipeCard swiped left")
            if (!(deck.first?.card === card)) {
                Log.e("SWIPE ERROR: ", "swipeCard on top of deck not equal to swipeCard swiped")
            }
            deck.removeFirst()
            callback?.cardSwipedLeft(viewId)
        }

        override fun cardSwipedRight(card: View?) {
            Log.d(TAG, "swipeCard swiped right")
            if (!(deck.first?.card === card)) {
                Log.e("SWIPE ERROR: ", "swipeCard on top of deck not equal to swipeCard swiped")
            }
            deck.removeFirst()
            callback?.cardSwipedRight(viewId)
        }

        override // Enabled by default, drag would depend on swipeEnabled
        val isDragEnabled: Boolean
            get() {
                return callback?.isDragEnabled(viewId) ?: true
            }

        override fun cardOffScreen(card: View?) {

        }

        override fun cardActionDown() {

        }

        override fun cardActionUp() {

        }

    }

    companion object {
        private val TAG = "SwipeDeck"
        var ANIMATION_DURATION = 200L
    }
}

