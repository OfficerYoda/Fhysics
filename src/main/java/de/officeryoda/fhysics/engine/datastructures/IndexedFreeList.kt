package de.officeryoda.fhysics.engine.datastructures

/**
 * An efficient, indexed free list for managing elements with constant time and space complexity.
 * It reuses memory slots, preventing fragmentation and reducing heap allocations.
 *
 * Key Operations:
 * - **insert(element: T)**: Inserts an element and returns its index. Reuses free slots when possible.
 * - **erase(index: Int)**: Removes an element, marking its index as free for reuse.
 * - **clear()**: Clears all elements from the list.
 * - **range()**: Returns the number of elements in the list.
 * - **get(index: Int)**: Accesses an element by index.
 *
 * @param T The type of elements stored in the free list.
 */
class IndexedFreeList<T> : Iterable<T> {
    /**
     * The data structure for a free element in the free list.
     * @param element The element.
     * @param next The index of the next free element.
     */
    private data class FreeElement<T>(
        /**
         * The element.
         * null if the element is free.
         */
        var element: T? = null,
        /**
         * The index of the next free element.
         * -1 if there is no next free element or the element is not free.
         */
        var next: Int = -1
    )

    /**
     * The data of the free list.
     */
    private val data: MutableList<FreeElement<T>> = mutableListOf()

    /**
     * The index of the first free element in the free list.
     */
    private var firstFree: Int = -1

    /**
     * Inserts an element into the free list.
     *
     * @param element The element to insert.
     * @return The index of the inserted element.
     */
    fun add(element: T): Int {
        return if (firstFree != -1) {
            // Reuse a free element
            val index: Int = firstFree
            firstFree = data[firstFree].next
            data[index].element = element
            index
        } else {
            // Append a new element
            val freeElement: FreeElement<T> = FreeElement(element = element)
            data.add(freeElement)
            data.size - 1
        }
    }

    /**
     * Removes an element from the free list.
     *
     * @param n The index of the element to remove.
     */
    fun remove(n: Int) {
        data[n].element = null
        data[n].next = firstFree
        firstFree = n
    }

    /**
     * Clears the free list.
     */
    fun clear() {
        data.clear()
        firstFree = -1
    }

    /**
     * Returns the total capacity of the free list.
     * This includes free and occupied elements.
     */
    fun capacity(): Int {
        return data.size
    }

    /**
     * Returns the number of elements in the free list. This excludes free elements.
     */
    fun size(): Int {
        return data.count { it.element != null }
    }

    /**
     * Returns the element at the specified index.
     *
     * @param n The index of the element to return.
     */
    operator fun get(n: Int): T {
        return data[n].element ?: throw IndexOutOfBoundsException("No element at index $n")
    }

    /**
     * Returns the index of the specified element.
     *
     * @param element The element to search for.
     */
    fun indexOf(element: T): Int {
        return data.indexOfFirst { it.element == element }
    }

    /**
     * Returns an iterator over the elements of this object.
     */
    override fun iterator(): Iterator<T> {
        return data.mapNotNull { it.element }.iterator()
    }
}
