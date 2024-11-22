package de.officeryoda.fhysics.engine.datastructures

import de.officeryoda.fhysics.engine.math.BoundingBox

data class QuadTreeNode(
    var boundingBox: BoundingBox,
) {
    /**
     * Points to the first child node in [QuadTree.nodes] when the node is a branch
     * or the first element in [QuadTree.elements] when the node is a leaf
     *
     * Nodes are stored in blocks of 4 and in the following order: top left, top right, bottom left, bottom right
     * Elements are stored as a linked list
     */
    var firstIdx: Int = -1

    // Stores the number of elements in the leaf or -1 if the node is a branch
    var count: Int = 0

    // Returns if the node is a leaf
    fun isLeaf(): Boolean {
        return count != -1
    }

    override fun toString(): String {
        return "QuadTreeNode(boundary=$boundingBox, firstIdx=$firstIdx, count=$count)"
    }
}