package de.officeryoda.fhysics.engine.datastructures

data class QTNode(
    /**
     * Points to the first child node in [QuadTree.nodes] when the node is a branch
     * or to the first element in [QuadTree.elements] when the node is a leaf
     *
     * Nodes are stored in blocks of 4 in order: top left, top right, bottom left, bottom right
     *
     * Elements are stored as a linked list
     */
    var firstIdx: Int = -1,
) {

    /**
     * The number of elements in the node or -1 if it's a branch
     */
    var count: Int = 0

    /**
     * Whether the node is a leaf or a branch
     */
    val isLeaf: Boolean get() = count != -1

    override fun toString(): String {
        return "QuadTreeNode(firstIdx=$firstIdx, count=$count, isLeaf=$isLeaf)"

    }
}