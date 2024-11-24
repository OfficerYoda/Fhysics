package de.officeryoda.fhysics.engine.datastructures

data class QTNodeData(
    /** Index of the node in the [QuadTree.nodes] list */
    val index: Int,
    /** Array representing the node's bounding box: [centerX, centerY, width, height] */
    val crect: IntArray,
    /** Depth of the node in the Quadtree */
    val depth: Int,
) {

    /**
     * Constructs a new [QTNodeData] object.
     * @param x The x-coordinate of the node's bottom-left corner
     * @param y The y-coordinate of the node's bottom-left corner
     * @param hw The half-width of the node
     * @param hh The half-height of the node
     * @param index The index of the node in the [QuadTree.nodes] list
     * @param depth The depth of the node in the Quadtree
     */
    constructor(x: Int, y: Int, hw: Int, hh: Int, index: Int, depth: Int) : this(
        index,
        intArrayOf(x + hw / 2, y + hh / 2, hw, hh),
        depth
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QTNodeData

        if (index != other.index) return false
        if (!crect.contentEquals(other.crect)) return false
        if (depth != other.depth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + crect.contentHashCode()
        result = 31 * result + depth
        return result
    }
}