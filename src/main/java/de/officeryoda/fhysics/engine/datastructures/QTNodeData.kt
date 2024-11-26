package de.officeryoda.fhysics.engine.datastructures

data class QTNodeData(
    /** Index of the node in the [QuadTree.nodes] list */
    val index: Int,
    /** IntArray representing the node's bounding box: [centerX, centerY, width, height] */
    val cRect: CenterRect,
    /** Depth of the node in the Quadtree */
    val depth: Int,
) {


    /**
     * Constructs a new [QTNodeData] object.
     * @param x The x-coordinate of the node's bottom-left corner
     * @param y The y-coordinate of the node's bottom-left corner
     * @param width The width of the node
     * @param height The height of the node
     * @param index The index of the node in the [QuadTree.nodes] list
     * @param depth The depth of the node in the Quadtree
     */
    constructor(
        x: Int, y: Int,
        width: Int, height: Int,
        index: Int, depth: Int,
    ) : this(
        index,
        CenterRect(x + width / 2, y + height / 2, width, height),
        depth
    )
}