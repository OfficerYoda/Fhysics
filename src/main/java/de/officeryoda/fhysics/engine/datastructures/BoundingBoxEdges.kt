package de.officeryoda.fhysics.engine.datastructures

import de.officeryoda.fhysics.engine.math.BoundingBox
import de.officeryoda.fhysics.extensions.ceilToInt
import de.officeryoda.fhysics.extensions.floorToInt

/**
 * A data class for storing bounding box edge data.
 * It stores the bounding box edge data in the following order:
 * 0. X-coordinate of the left edge
 * 1. X-coordinate of the right edge
 * 2. Y-coordinate of the top edge
 * 3. Y-coordinate of the bottom edge
 *
 * This class helps clarify the meaning of the values in the array when used in QuadTree
 */
@JvmInline
value class BoundingBoxEdges(private val edges: IntArray) {

    init {
        require(edges.size == 4) { "BoundingBoxEdges requires exactly 4 int values (left-X, right-X, top-Y, bottom-Y)." }
    }

    constructor(bbox: BoundingBox) : this(
        intArrayOf(
            bbox.x.floorToInt(), // Left edge
            (bbox.x + bbox.width).ceilToInt(), // Right edge
            (bbox.y + bbox.height).ceilToInt(), // Top edge
            bbox.y.floorToInt() // Bottom edge
        )
    )

    operator fun get(index: Int): Int {
        return edges[index]
    }
}