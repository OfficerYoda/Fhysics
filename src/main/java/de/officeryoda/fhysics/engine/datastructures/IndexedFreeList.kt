package de.officeryoda.fhysics.engine.datastructures

/**
 * An efficient, indexed free list for managing elements of type [T] with constant time and space complexity.
 * It reuses memory slot by keeping track of free indices.
 */
class IndexedFreeList<T>() : Iterable<T> {

    /**
     * The data of the [IndexedFreeList].
     */
    private val data: MutableList<FreeElement<T>> = ArrayList()

    /**
     * The index of the first free element in the [IndexedFreeList].
     *
     * -1 if there is no free element.
     */
    private var firstFree: Int = -1

    /**
     * Constructs an [IndexedFreeList] with [firstElement] as the first element.
     */
    constructor(firstElement: T) : this() {
        this.add(firstElement)
    }

    /**
     * Adds an [element] to the free list and returns the index of the element.
     */
    fun add(element: T): Int {
        return if (firstFree != -1) {
            // Reuse a free element
            val index: Int = firstFree
            firstFree = data[firstFree].next
            data[index].element = element
            data[index].next = -1
            index
        } else {
            // Append a new element
            val freeElement: FreeElement<T> = FreeElement(element = element)
            data.add(freeElement)
            data.size - 1
        }
    }

    /**
     * Frees an [index] in the [IndexedFreeList] for reuse.
     */
    fun free(index: Int) {
        data[index].element = null
        data[index].next = firstFree
        firstFree = index
    }

    /**
     * Clears the [IndexedFreeList].
     */
    fun clear() {
        data.clear()
        firstFree = -1
    }

    /**
     * Returns the capacity of the [IndexedFreeList].
     * This includes free elements.
     */
    fun capacity(): Int {
        return data.size
    }

    /**
     * Returns the number of elements in the [IndexedFreeList].
     * This excludes free elements.
     */
    fun usedCount(): Int {
        return data.count { it.element != null }
    }

    /**
     * Returns the element at the given [index].
     */
    operator fun get(index: Int): T {
        return data[index].element ?: throw IndexOutOfBoundsException("No element at index $index")
    }

    /**
     * Returns the index of the given [element].
     */
    fun indexOf(element: T): Int {
        return data.indexOfFirst { it.element == element }
    }

    override fun iterator(): Iterator<T> {
        return data.mapNotNull { it.element }.iterator()
    }

    override fun toString(): String {
        return "IndexedFreeList(firstFree=$firstFree, capacity=${capacity()}, data=$data)"
    }

    /**
     * The data structure for an element in the [IndexedFreeList].
     */
    private data class FreeElement<T>(
        /**
         * The element.
         *
         * null if the element is free.
         */
        var element: T? = null,
        /**
         * The index of the next free element.
         *
         * -1 if there is no next free element or the element is not free.
         */
        var next: Int = -1,
    )
}
