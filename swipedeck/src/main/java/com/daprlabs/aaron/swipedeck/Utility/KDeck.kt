package com.daprlabs.aaron.swipedeck.Utility

import com.daprlabs.aaron.swipedeck.KCardContainer
import java.util.*

class KDeck<T : KCardContainer>(private val listener: KDeckEventListener) {
    private val internal = LinkedList<T>()

    val size = internal.size
    val first = internal.firstOrNull()
    val last = internal.lastOrNull()

    fun get(index: Int): T {
        return internal[index]
    }

    fun clear() {
        while (size > 0) {
            removeFirst()
        }
    }

    /**
     * Makes items in the deck aware of their positions in the deck.
     */
    private fun updateItemPositions() {
        for (i in 0..internal.size) {
            internal[i].positionWithinViewGroup = i
        }
    }

    fun addFirst(item: T) {
        internal.addFirst(item)
        updateItemPositions()
        listener.itemAddedFront(item)
    }

    fun addLast(item: T) {
        internal.addLast(item)
        updateItemPositions()
        listener.itemAddedBack(item)
    }

    fun removeFirst(): T {
        val removed = internal.removeFirst()
        updateItemPositions()
        listener.itemRemovedFront(removed)
        return removed
    }

    fun removeLast(): T {
        val removed = internal.removeLast()
        updateItemPositions()
        listener.itemRemovedBack(removed)
        return removed
    }

    interface KDeckEventListener {
        fun itemAddedFront(item: Any?)
        fun itemAddedBack(item: Any?)
        fun itemRemovedFront(item: Any?)
        fun itemRemovedBack(item: Any?)
    }
}