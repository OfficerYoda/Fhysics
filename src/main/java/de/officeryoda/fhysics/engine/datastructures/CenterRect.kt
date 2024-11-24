package de.officeryoda.fhysics.engine.datastructures

import de.officeryoda.fhysics.engine.math.BoundingBox
import de.officeryoda.fhysics.extensions.ceilToInt
import de.officeryoda.fhysics.extensions.floorToInt

/**
 * A data class for storing center rectangle data.
 * It stores the center rectangle data in the following order:
 * 0. centerX
 * 1. centerY
 * 2. width
 * 3. height
 *
 * This class helps clarify the meaning of the values in the array when used in QuadTree
 */
@JvmInline
value class CenterRect(private val data: IntArray) {

    init {
        require(data.size == 4) { "TransformationData requires exactly 4 int values (centerX, centerY, width, height)." }
    }

    constructor(centerX: Int, centerY: Int, width: Int, height: Int) : this(intArrayOf(centerX, centerY, width, height))

    /**
     * Constructs the smallest possible center rectangle that contains the given bounding box
     */
    constructor(bbox: BoundingBox) : this(
        intArrayOf(
            (bbox.x + bbox.width / 2).floorToInt(),
            (bbox.y + bbox.height / 2).floorToInt(),
            bbox.width.ceilToInt(),
            bbox.height.ceilToInt()
        )
    )

    operator fun get(index: Int): Int {
        return data[index]
    }

    override fun toString(): String {
        return "CenterRect(centerX=${data[0]}, centerY=${data[1]}, width=${data[2]}, height=${data[3]})"
    }
}