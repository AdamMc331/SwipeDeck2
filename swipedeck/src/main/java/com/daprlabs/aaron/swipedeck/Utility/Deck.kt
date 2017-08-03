package com.daprlabs.aaron.swipedeck.Utility

import com.daprlabs.aaron.swipedeck.CardContainer
import java.util.*

class Deck<T : CardContainer>(private val listener: DeckEventListener) {

    private val internal = LinkedList<T>()

    val first: T?
        get() = internal.firstOrNull()

    val last: T?
        get() = internal.lastOrNull()

    operator fun get(pos: Int): T {
        return internal[pos]
    }

    fun size(): Int {
        return internal.size
    }

    fun clear() {
        while (size() > 0) {
            removeFirst()
        }
    }

    /**
     * Makes items in the deck aware of their positions in the deck.
     */
    private fun updateItemPositions() {
        for (i in internal.indices) {
            internal[i].positionWithinViewGroup = i
        }
    }

    fun addFirst(t: T) {
        internal.addFirst(t)
        updateItemPositions()
        listener.itemAddedFront(t)
    }

    fun addLast(t: T) {
        internal.addLast(t)
        updateItemPositions()
        listener.itemAddedBack(t)
    }

    fun removeFirst(): T? {
        val toRemove = internal.removeFirst()
        updateItemPositions()
        listener.itemRemovedFront(toRemove)
        return toRemove
    }

    fun removeLast(): T? {
        val toRemove = internal.removeLast()
        updateItemPositions()
        listener.itemRemovedBack(toRemove)
        return toRemove
    }

    interface DeckEventListener {
        fun itemAddedFront(item: Any)
        fun itemAddedBack(item: Any)
        fun itemRemovedFront(item: Any)
        fun itemRemovedBack(item: Any)
    }
}
