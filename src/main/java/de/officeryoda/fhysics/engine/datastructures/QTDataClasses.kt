package de.officeryoda.fhysics.engine.datastructures


data class QTNodeElement(
    /** The index of the object in [QuadTree.objects] */
    val index: Int,
    /** The index of the element in the node or -1 if it's the last element */
    var next: Int,
)

data class QTNodeData(
    /** Index of the node in the [QuadTree.nodes] list. */
    val index: Int,
    /** Array representing the node's bounding box: [centerX, centerY, width, height]. */
    val crect: IntArray,
    /** Depth of the node in the Quadtree. */
    val depth: Int,
) {
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