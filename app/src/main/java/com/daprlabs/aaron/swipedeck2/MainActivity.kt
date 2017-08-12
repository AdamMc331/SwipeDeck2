package com.daprlabs.aaron.swipedeck2

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.daprlabs.aaron.swipedeck.SwipeDeck
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val cardStack: SwipeDeck by lazy { findViewById(R.id.swipe_deck) as SwipeDeck }
    private val testData: List<String> = (0..9).map { it.toString() }
    private val adapter: SwipeDeckAdapter by lazy { SwipeDeckAdapter(testData, this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val leftButton = findViewById(R.id.left_button)
        val rightButton = findViewById(R.id.right_button)
        val undoButton = findViewById(R.id.undo_button)
        leftButton.setOnClickListener(this)
        rightButton.setOnClickListener(this)
        undoButton.setOnClickListener(this)

        cardStack.setAdapter(adapter)

        //TODO: Timber
        cardStack.setCallback(object : SwipeDeck.SwipeDeckCallback {
            override fun cardSwipedLeft(itemId: Long) {
                Log.i("MainActivity", "swipeCard was swiped left, position in adapter: " + itemId)
            }

            override fun cardSwipedRight(itemId: Long) {
                Log.i("MainActivity", "swipeCard was swiped right, position in adapter: " + itemId)

            }

            override fun isDragEnabled(itemId: Long): Boolean {
                //TODO:
                return true
            }
        })

        cardStack.leftImageResource = R.id.left_image
        cardStack.rightImageResource = R.id.right_image
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.left_button -> cardStack.swipeTopCardLeft()
            R.id.right_button -> cardStack.swipeTopCardRight()
            R.id.undo_button -> cardStack.unSwipeCard()
        }
    }

    //TODO: Need a ViewHolder.
    inner class SwipeDeckAdapter(private val data: List<String>, private val context: Context) : BaseAdapter() {

        override fun getCount(): Int {
            return data.size
        }

        override fun getItem(position: Int): Any {
            return data[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

            var cardView: View? = convertView

            if (cardView == null) {
                val inflater = layoutInflater
                cardView = inflater.inflate(R.layout.card_layout, parent, false)
            }

            val imageView = cardView?.findViewById(R.id.offer_image) as ImageView
            Picasso.with(context).load(R.drawable.food).fit().centerCrop().into(imageView)

            val textView = cardView.findViewById(R.id.sample_text) as TextView
            val item = getItem(position).toString()
            textView.text = item

            cardView.setOnClickListener { v ->
                Log.i("Layer type: ", Integer.toString(v.layerType))
                Log.i("Hardware Accel type:", Integer.toString(View.LAYER_TYPE_HARDWARE))
            }

            return cardView
        }
    }
}
