package de.officeryoda.fhysics.engine.datastructures

/**
 * An efficient, indexed free list for managing elements of type [T] with constant time and space complexity.
 * It reuses memory slots, preventing fragmentation and reducing heap allocations.
 */
class IndexedFreeList<T>() : Iterable<T> {

    constructor(element: T) : this() {
        this.add(element)
    }

    /**
     * The data of the [IndexedFreeList].
     */
    private val data: MutableList<FreeElement<T>> = mutableListOf()

    /**
     * The index of the first free element in the [IndexedFreeList].
     */
    private var firstFree: Int = -1

    /**
     * Adds an [element] to the free list and returns the index of the element.
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

    /**
     * Returns an iterator over the elements of this object.
     */
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
         * null if the element is free.
         */
        var element: T? = null,
        /**
         * The index of the next free element.
         * -1 if there is no next free element or the element is not free.
         */
        var next: Int = -1,
    )

    /**
     * An iterator over the elements of the [IndexedFreeList] with the index of the current element in the [list].
     *
     * The iterator skips free elements.
     *
     * @param T The type of the elements.
     * @property list The [IndexedFreeList] to iterate over.
     */
    class IndexedIterator<T>(private val list: IndexedFreeList<T>) : Iterator<T> {
        private var currentIndex = 0

        override fun hasNext(): Boolean {
            while (currentIndex < list.capacity() && list.data[currentIndex].element == null) {
                currentIndex++
            }
            return currentIndex < list.capacity()
        }

        override fun next(): T {
            if (!hasNext()) throw NoSuchElementException()
            return list.data[currentIndex++].element!!
        }

        /**
         * Returns the index of the current element in the [list].
         */
        fun index(): Int {
            return currentIndex - 1
        }
    }
}
