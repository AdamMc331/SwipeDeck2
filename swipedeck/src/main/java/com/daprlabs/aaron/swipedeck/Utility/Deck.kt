package com.daprlabs.aaron.swipedeck.Utility

import com.daprlabs.aaron.swipedeck.CardContainer
import java.util.*

/**
 * Represents a collection of cards that the user can swipe away.
 *
 * @param[T] The type of CardContainers to use.
 * @property[listener] The interface used for callbacks when items are added/removed.
 * @property[internal] The LinkedList of type T items.
 */
class Deck<T : CardContainer>(private val listener: DeckEventListener) {

    private val internal = LinkedList<T>()

    /**
     * Retrieves the first item in the deck.
     */
    val first: T?
        get() = internal.firstOrNull()

    /**
     * Retrieves the last item in the deck.
     */
    val last: T?
        get() = internal.lastOrNull()

    /**
     * Retrieves the item at a given position.
     *
     * @param[pos] The position in the deck.
     * @return The item at the given position in the deck.
     */
    operator fun get(pos: Int): T {
        return internal[pos]
    }

    /**
     * @return The size of the deck.
     */
    fun size(): Int {
        return internal.size
    }

    /**
     * Removes all items in the deck.
     */
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

    /**
     * Prepends an item to the beginning of the deck.
     *
     * @param[item] The item to add.
     */
    fun addFirst(item: T) {
        internal.addFirst(item)
        updateItemPositions()
        listener.itemAddedFront(item)
    }

    /**
     * Appends an item to the end of the deck.
     *
     * @param[item] The item to add.
     */
    fun addLast(item: T) {
        internal.addLast(item)
        updateItemPositions()
        listener.itemAddedBack(item)
    }

    /**
     * Removes the first item in the deck.
     *
     * @return The item that was removed.
     */
    fun removeFirst(): T? {
        val toRemove = internal.removeFirst()
        updateItemPositions()
        listener.itemRemovedFront(toRemove)
        return toRemove
    }

    /**
     * Removes the last item in the deck.
     *
     * @return the item that was removed.
     */
    fun removeLast(): T? {
        val toRemove = internal.removeLast()
        updateItemPositions()
        listener.itemRemovedBack(toRemove)
        return toRemove
    }

    /**
     * Callback used as the contents of the deck changed.
     */
    interface DeckEventListener {
        /**
         * Called when an item is added to the front of the deck.
         *
         * @param[item] The item that was added.
         */
        fun itemAddedFront(item: Any)

        /**
         * Called when an item is added to the back of the deck.
         *
         * @param[item] The item that was added.
         */
        fun itemAddedBack(item: Any)

        /**
         * Called when an item is removed from the front of the deck.
         *
         * @param[item] The item that was removed.
         */
        fun itemRemovedFront(item: Any)

        /**
         * Called when an item is removed from the back of the deck.
         *
         * @param[item] The item that was removed.
         */
        fun itemRemovedBack(item: Any)
    }
}
