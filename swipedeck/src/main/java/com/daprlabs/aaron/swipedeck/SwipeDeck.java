package com.daprlabs.aaron.swipedeck;

import com.daprlabs.aaron.swipedeck.Utility.Deck;
import com.daprlabs.aaron.swipedeck.Utility.SwipeCallback;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.FrameLayout;

import java.util.ArrayList;

/**
 * Created by aaron on 21/08/2016.
 */
public class SwipeDeck extends FrameLayout {

    private static final String TAG = "SwipeDeck";
    private final int previewLayoutId;
    private int NUMBER_OF_SIMULTANEOUS_CARDS;
    public float OPACITY_END;
    public float ROTATION_DEGREES;
    private float CARD_SPACING;
    public static int ANIMATION_DURATION = 200;
    public boolean RENDER_ABOVE;
    public boolean SWIPE_ENABLED;
    private boolean mHasStableIds;

    private Adapter mAdapter;
    private DataSetObserver observer;
    private Deck<CardContainer> deck;
    private SwipeDeckCallback callback;
    private ArrayList<CardContainer> buffer = new ArrayList<>();


    private int leftImageResource;
    private int rightImageResource;

    private int adapterIndex = 0;

    public SwipeDeck(final Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.SwipeDeck2,
                0, 0);
        NUMBER_OF_SIMULTANEOUS_CARDS = a.getInt(R.styleable.SwipeDeck2_max_visible, 3);
        OPACITY_END = a.getFloat(R.styleable.SwipeDeck2_opacity_end, 0.33f);
        ROTATION_DEGREES = a.getFloat(R.styleable.SwipeDeck2_rotation_degrees, 15f);
        CARD_SPACING = a.getDimension(R.styleable.SwipeDeck2_card_spacing, 15f);
        RENDER_ABOVE = a.getBoolean(R.styleable.SwipeDeck2_render_above, true);
        SWIPE_ENABLED = a.getBoolean(R.styleable.SwipeDeck2_swipe_enabled, true);
        previewLayoutId = a.getResourceId(R.styleable.SwipeDeck2_preview_layout, -1);

        deck = new Deck<>(new Deck.DeckEventListener() {

            @Override
            public void itemAddedFront(Object item) {
                deck.getFirst().setSwipeEnabled(true);
                if (deck.size() > NUMBER_OF_SIMULTANEOUS_CARDS) {
                    deck.removeLast();
                    adapterIndex--;
                }
                renderDeck();
            }

            @Override
            public void itemAddedBack(Object item) {
                deck.getFirst().setSwipeEnabled(true);
                renderDeck();
            }

            @Override
            public void itemRemovedFront(Object item) {
                CardContainer container = (CardContainer) item;
                buffer.add(container);
                //enable swipe in the next cardContainer
                if (deck.size() > 0) {
                    deck.getFirst().setSwipeEnabled(true);
                }
                container.cleanupAndRemoveView();
                //pull in the next view (if available)
                addNextView();
                renderDeck();
            }

            @Override
            public void itemRemovedBack(Object item) {
                ((CardContainer) item).getCard().animate().setDuration(100).alpha(0);
            }
        });

        //set clipping of view parent to false so cards render outside their view boundary
        //make sure not to clip to padding
        setClipToPadding(false);
        setClipChildren(false);
        this.setWillNotDraw(false);

        //if render above is set make sure everything in this view renders above other views
        //outside of it.
        if (RENDER_ABOVE) {
            ViewCompat.setTranslationZ(this, Float.MAX_VALUE);
        }//todo: make an else here possibly
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isInEditMode() && previewLayoutId != -1) {
            for (int i = NUMBER_OF_SIMULTANEOUS_CARDS - 1; i >= 0; i--) {
                View view = LayoutInflater.from(getContext()).inflate(previewLayoutId, this, false);
                FrameLayout.LayoutParams params = (LayoutParams) view.getLayoutParams();

                int offset = (int) (i * CARD_SPACING);
                // All cards are placed in absolute coordinates, so disable gravity if we have any
                params.gravity = Gravity.NO_GRAVITY;
                // We can't user translations here, for some reason it's not rendered properly in preview
                params.topMargin = offset;
                view.setLayoutParams(params);
                addViewInLayout(view, -1, params, true);
            }
            setZTranslations();
        }
    }

    public void setAdapter(final Adapter adapter) {

        if (this.mAdapter != null) {
            this.mAdapter.unregisterDataSetObserver(observer);
        }
        mHasStableIds = adapter.hasStableIds();
        mAdapter = adapter;
        observer = new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                //handle data set changes
                //if we need to add any cards at this point (ie. the amount of cards in the deck
                //is less than the max number of cards to display) add the cards.

                int deckSize = deck.size();
                //only perform action if there are less cards on screen than NUMBER_OF_CARDS
                if (deckSize < NUMBER_OF_SIMULTANEOUS_CARDS) {
                    for (int i = deckSize; i < NUMBER_OF_SIMULTANEOUS_CARDS; ++i) {
                        addNextView();
                    }
                }
                //if the adapter has been emptied empty the view and reset adapterIndex
                if (adapter.getCount() == 0) {
                    deck.clear();
                    adapterIndex = 0;
                }
            }

            @Override
            public void onInvalidated() {
                //reset state, remove views and request layout
                //nextAdapterCard = 0;
                deck.clear();
                removeAllViews();
                requestLayout();
            }
        };
        adapter.registerDataSetObserver(observer);
        requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (isInEditMode()) {
            return;
        }

        // if we don't have an adapter, we don't need to do anything
        if (mAdapter == null || mAdapter.getCount() == 0) {
            //nextAdapterCard = 0;
            removeAllViewsInLayout();
            return;
        }
        //pull in views from the adapter at the position the top of the deck is set to
        //stop when you get to for cards or the end of the adapter
        int deckSize = deck.size();
        for (int i = deckSize; i < NUMBER_OF_SIMULTANEOUS_CARDS; ++i) {
            addNextView();
        }
    }

    private void addNextView() {
        if (adapterIndex < mAdapter.getCount()) {
            View newBottomChild = mAdapter.getView(adapterIndex, null, this);
            newBottomChild.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            //todo: i'm setting the card to invisible initially and making it visible when i animate
            //later
            newBottomChild.setAlpha(0);
            newBottomChild.setY(getPaddingTop());
            final long viewId = mAdapter.getItemId(adapterIndex);

            CardContainer card = new CardContainer(newBottomChild, this, new CardContainerCallback(viewId));

            card.setPositionWithinAdapter(adapterIndex);

            if (leftImageResource != 0) {
                card.setLeftImageResource(leftImageResource);
            }
            if (rightImageResource != 0) {
                card.setRightImageResource(rightImageResource);
            }

            card.setId(viewId);

            deck.addLast(card);
            adapterIndex++;
        }
    }


    private void addLastView() {
        //get the position of the card prior to the card atop the deck
        int positionOfLastCard;

        //if there's a card on the deck get the card before it, otherwise the last card is one
        //before the adapter index.
        if (deck.size() > 0) {
            positionOfLastCard = deck.getFirst().getPositionWithinAdapter() - 1;
        } else {
            positionOfLastCard = adapterIndex - 1;
        }
        if (positionOfLastCard >= 0) {
            View newBottomChild = mAdapter.getView(positionOfLastCard, null, this);
            newBottomChild.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            //todo: i'm setting the card to invisible initially and making it visible when i animate
            //later
            newBottomChild.setAlpha(0);
            newBottomChild.setY(getPaddingTop());

            final long viewId = mAdapter.getItemId(positionOfLastCard);

            CardContainer card = new CardContainer(newBottomChild, this, new CardContainerCallback(viewId));

            if (leftImageResource != 0) {
                card.setLeftImageResource(leftImageResource);
            }
            if (rightImageResource != 0) {
                card.setRightImageResource(rightImageResource);
            }

            card.setId(viewId);

            deck.addFirst(card);
            card.setPositionWithinAdapter(positionOfLastCard);
        }
    }

    private void renderDeck() {
        //we remove all the views and re add them so that the Z translation is correct
        removeAllViews();
        for (int i = deck.size() - 1; i >= 0; --i) {
            CardContainer container = deck.get(i);
            View card = container.getCard();
            ViewGroup.LayoutParams params = card.getLayoutParams();

            if (params == null) {
                params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            }

            addViewInLayout(card, -1, params, true);
            int itemWidth = getWidth() - (getPaddingLeft() + getPaddingRight());
            int itemHeight = getHeight() - (getPaddingTop() + getPaddingBottom());
            card.measure(MeasureSpec.EXACTLY | itemWidth, MeasureSpec.EXACTLY | itemHeight);
        }
        //if there's still a card animating in the buffer, make sure it's re added after removing all views
        if (buffer != null) {
            // cards in buffer go from older ones to newer
            // in our deck, newer cards are placed below older cards
            // we need to start with new cards, so older cards would be above them
            for (int i = buffer.size() - 1; i >= 0; i--) {
                View card = buffer.get(i).getCard();
                ViewGroup.LayoutParams params = card.getLayoutParams();

                if (params == null) {
                    params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                }
                addViewInLayout(card, -1, params, true);
                int itemWidth = getWidth() - (getPaddingLeft() + getPaddingRight());
                int itemHeight = getHeight() - (getPaddingTop() + getPaddingBottom());
                card.measure(MeasureSpec.EXACTLY | itemWidth, MeasureSpec.EXACTLY | itemHeight);
            }
        }
        positionCards();
    }

    private void positionCards() {
        setZTranslations();
        for (int i = 0; i < deck.size(); ++i) {
            animateCardPosition(deck.get(i).getCard(), deck.get(i).getPositionWithinViewGroup());
        }
    }

    protected void animateCardPosition(View card, int position) {
        float offset = (int) (position * CARD_SPACING);
        card.animate()
                .setDuration(ANIMATION_DURATION)
                .y(getPaddingTop() + offset)
                .alpha(1.0f);
    }

    public void setCallback(SwipeDeckCallback callback) {
        this.callback = callback;
    }

    /**
     * Swipe top card to the left side.
     *
     * @param duration animation duration in milliseconds
     */
    public void swipeTopCardLeft(int duration) {
        if (deck.size() > 0) {
            deck.get(0).swipeCardLeft(duration);
            if (callback != null) {
                callback.cardSwipedLeft(deck.get(0).getId());
            }
            deck.removeFirst();
        }
    }

    /**
     * Swipe card to the right side.
     *
     * @param duration animation duration in milliseconds
     */
    public void swipeTopCardRight(int duration) {
        if (deck.size() > 0) {
            deck.get(0).swipeCardRight(duration);
            if (callback != null) {
                callback.cardSwipedRight(deck.get(0).getId());
            }
            deck.removeFirst();
        }
    }

    public void unSwipeCard() {
        addLastView();
    }

    public void setAdapterIndex(int index) {
        this.adapterIndex = index;
    }

    public int getAdapterIndex() {
        return this.adapterIndex;
    }

    /**
     * Get item id associated with the card on top of the deck.
     *
     * @return item id of the card on the top of the stack or -1 if deck is empty
     */
    public long getTopCardItemId() {
        if (deck.size() > 0) {
            return deck.getFirst().getId();
        } else {
            return -1;
        }
    }

    public void removeFromBuffer(CardContainer container) {
        this.buffer.remove(container);
    }

    public void setLeftImage(int imageResource) {
        leftImageResource = imageResource;
    }

    public void setRightImage(int imageResource) {
        rightImageResource = imageResource;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setZTranslations() {
        //this is only needed to add shadows to cardviews on > lollipop
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int count = getChildCount();
            for (int i = 0; i < count; ++i) {
                getChildAt(i).setTranslationZ(i * 10);
            }
        }
    }

    public interface SwipeDeckCallback {
        void cardSwipedLeft(long itemId);

        void cardSwipedRight(long itemId);

        /**
         * Check whether we can start dragging view with provided id.
         *
         * @param itemId id of the card returned by adapter's {@link Adapter#getItemId(int)}
         * @return true if we can start dragging view, false otherwise
         */
        boolean isDragEnabled(long itemId);
    }

    private class CardContainerCallback implements SwipeCallback {

        private final long viewId;

        public CardContainerCallback(long viewId) {
            this.viewId = viewId;
        }

        @Override
        public void cardSwipedLeft(View card) {
            Log.d(TAG, "card swiped left");
            if (!(deck.getFirst().getCard() == card)) {
                Log.e("SWIPE ERROR: ", "card on top of deck not equal to card swiped");
            }
            deck.removeFirst();
            if (callback != null) {
                callback.cardSwipedLeft(viewId);
            }
        }

        @Override
        public void cardSwipedRight(View card) {
            Log.d(TAG, "card swiped right");
            if (!(deck.getFirst().getCard() == card)) {
                Log.e("SWIPE ERROR: ", "card on top of deck not equal to card swiped");
            }
            deck.removeFirst();
            if (callback != null) {
                callback.cardSwipedRight(viewId);
            }
        }

        @Override
        public boolean isDragEnabled() {
            if (callback != null) {
                return callback.isDragEnabled(viewId);
            } else {
                // Enabled by default, drag would depend on SWIPE_ENABLED
                return true;
            }
        }

        @Override
        public void cardOffScreen(View card) {

        }

        @Override
        public void cardActionDown() {

        }

        @Override
        public void cardActionUp() {

        }

    }
}

