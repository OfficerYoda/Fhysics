package de.officeryoda.fhysics.engine.datastructures.spatial

import de.officeryoda.fhysics.engine.util.ceilToInt
import de.officeryoda.fhysics.engine.util.floorToInt

/**
 * A data class for storing the edges of a bounding box.
 */
@JvmInline
value class BoundingBoxEdges(
    /**
     * The edges of the bounding box in the following order:
     * 0. Left edge (X-coordinate)
     * 1. Right edge (X-coordinate)
     * 2. Top edge (Y-coordinate)
     * 3. Bottom edge (Y-coordinate)
     */
    private val edges: IntArray,
) {

    /** The X-coordinate of the left edge. */
    val left: Int get() = edges[0]

    /** The X-coordinate of the right edge. */
    val right: Int get() = edges[1]

    /** The Y-coordinate of the top edge. */
    val top: Int get() = edges[2]

    /** The Y-coordinate of the bottom edge. */
    val bottom: Int get() = edges[3]

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

    companion object {
        /**
         * Creates a [BoundingBoxEdges] object from a [CenterRect] object.
         */
        fun fromCenterRect(cRect: CenterRect): BoundingBoxEdges {
            val halfWidth: Int = cRect.width / 2
            val halfHeight: Int = cRect.height / 2
            return BoundingBoxEdges(
                intArrayOf(
                    cRect.centerX - halfWidth, // Left edge
                    cRect.centerX + cRect.width - halfWidth, // Right edge
                    cRect.centerY + cRect.height - halfHeight, // Top edge
                    cRect.centerY - halfHeight // Bottom edge
                )
            )
        }
    }
}