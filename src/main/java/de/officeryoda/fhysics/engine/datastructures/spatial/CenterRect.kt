package de.officeryoda.fhysics.engine.datastructures.spatial

import de.officeryoda.fhysics.engine.util.ceilToInt
import de.officeryoda.fhysics.engine.util.floorToInt

/**
 * A data class for storing center rectangle data.
 * It stores rectangle data in the following order:
 * 0. centerX (rounded down)
 * 1. centerY (rounded down)
 * 2. width
 * 3. height
 *
 * This class helps clarify the meaning of the values in the array when used in QuadTree
 */
@JvmInline
value class CenterRect(private val data: IntArray) {

    /** The X-coordinate of the center. */
    val centerX: Int get() = data[0]

    /** The Y-coordinate of the center. */
    val centerY: Int get() = data[1]

    /** The width of the rectangle. */
    val width: Int get() = data[2]

    /** The height of the rectangle. */
    val height: Int get() = data[3]

    init {
        require(data.size == 4) { "TransformationData requires exactly 4 int values (centerX, centerY, width, height)." }
    }

    constructor(centerX: Int, centerY: Int, width: Int, height: Int) : this(intArrayOf(centerX, centerY, width, height))

    /**
     * Returns the value represented by the given [index]:
     * 0. centerX (rounded down)
     * 1. centerY (rounded down)
     * 2. width
     * 3. height
     */
    operator fun get(index: Int): Int {
        return data[index]
    }

    override fun toString(): String {
        return "CenterRect(centerX=${data[0]}, centerY=${data[1]}, width=${data[2]}, height=${data[3]})"
    }

    /**
     * Can be used to check if the content of two [CenterRect] objects is equal.
     *
     * Can't override equals() because kotlin won't allow it for inline classes yet.
     */
    fun contentEquals(other: CenterRect): Boolean {
        return data.contentEquals(other.data)
    }

    companion object {
        /**
         * Constructs the smallest possible [CenterRect] that contains the [given][bbox] bounding box.
         */
        fun fromBoundingBox(bbox: BoundingBox): CenterRect {
            // This has problems with negative values, but everything in the border should be positive
            val flooredX: Int = bbox.x.floorToInt()
            val flooredY: Int = bbox.y.floorToInt()
            val width: Int = (bbox.x + bbox.width).ceilToInt() - flooredX
            val height: Int = (bbox.y + bbox.height).ceilToInt() - flooredY
            val x: Int = flooredX + (width / 2)
            val y: Int = flooredY + (height / 2)

            return CenterRect(intArrayOf(x, y, (width * 1.2f).toInt(), (height * 1.2f).toInt()))
        }
    }
}