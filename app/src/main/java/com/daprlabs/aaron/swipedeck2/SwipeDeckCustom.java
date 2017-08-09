package com.daprlabs.aaron.swipedeck2;

import com.daprlabs.aaron.swipedeck.SwipeDeck;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class SwipeDeckCustom extends SwipeDeck {

    public SwipeDeckCustom(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void animateCardPosition(View card, int position) {
        int offset = position * 15;
        float scale = (card.getMeasuredWidth() - offset) / (float) card.getMeasuredWidth();
        card.animate()
                .setDuration(Companion.getANIMATION_DURATION())
                .y(getPaddingTop() + offset)
                .scaleX(scale)
                .scaleY(scale)
                .alpha(1.0f);
    }
}
