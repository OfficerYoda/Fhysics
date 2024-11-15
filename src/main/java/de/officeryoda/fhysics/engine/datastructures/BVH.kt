package de.officeryoda.fhysics.engine.datastructures

import de.officeryoda.fhysics.engine.math.BoundingBox
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.rendering.DebugDrawer
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer
import de.officeryoda.fhysics.engine.math.BoundingBox as AABB

/**
 * A bounding volume hierarchy (BVH) is a tree structure on a set of geometric objects.
 */
object BVH {
    private var root: BVHNode? = null

    // Render the BVH for debugging
    fun drawNodes() {
        root?.drawNode()
    }

    fun drawObjects(drawer: FhysicsObjectDrawer) {
        root?.drawObjects(drawer)
    }

    // Insert an object into the BVH
    fun insert(obj: FhysicsObject, aabb: AABB) {
        val newLeaf = BVHNode(aabb, obj = obj)
        root = if (root == null) {
            newLeaf
        } else {
            insertNode(root!!, newLeaf)
        }
    }

    /**
     * Inserts a leaf node into the BVH.
     *
     * @param node The node to insert the leaf into.
     * @param leaf The leaf node to insert.
     * @return The new root node of the BVH.
     */
    private fun insertNode(node: BVHNode, leaf: BVHNode): BVHNode {
        if (node.isLeaf) {
            val mergedAABB: BoundingBox = node.aabb.merge(leaf.aabb)
            return BVHNode(mergedAABB, left = node, right = leaf)
        }
        // If it's an internal node, we decide where to insert based on area increase
        val leftAreaIncrease: Float = areaIncrease(node.left!!.aabb, leaf.aabb)
        val rightAreaIncrease: Float = areaIncrease(node.right!!.aabb, leaf.aabb)

        if (leftAreaIncrease < rightAreaIncrease) {
            node.left = insertNode(node.left!!, leaf)
        } else {
            node.right = insertNode(node.right!!, leaf)
        }

        node.aabb = node.left!!.aabb.merge(node.right!!.aabb)
        return node
    }

    // Compute area increase when adding a bounding box to an existing AABB
    private fun areaIncrease(aabb1: AABB, aabb2: AABB): Float {
        val combined: BoundingBox = aabb1.merge(aabb2)
        return combined.area() - aabb1.area()
    }

    // Update an object in the BVH (reinsert if moved)
    fun update(obj: FhysicsObject, newAABB: AABB) {
        // TODO: Optimize this by reusing the existing leaf node
        remove(obj)
        insert(obj, newAABB)
    }

    // Remove an object from the BVH
    fun remove(obj: Any) {
        root = removeNode(root, obj)
    }

    /**
     * Removes an object from the BVH.
     *
     * @param node The node to remove the object from.
     * @param obj The object to remove.
     * @return The new root node of the BVH.
     */
    private fun removeNode(node: BVHNode?, obj: Any): BVHNode? {
        if (node == null) return null

        if (node.isLeaf) {
            return if (node.obj == obj) null else node
        }

        // Recursive removal from left or right child
        node.left = removeNode(node.left, obj)
        node.right = removeNode(node.right, obj)

        // If a child becomes null, return the other child
        return when {
            node.left == null -> node.right
            node.right == null -> node.left
            else -> {
                node.aabb = node.left!!.aabb.merge(node.right!!.aabb) // TODO check if merging is necessary
                node
            }
        }
    }

    // Query for potential collisions with a given AABB
    fun query(aabb: AABB, result: MutableList<Any>) {
        queryNode(root, aabb, result)
    }

    private fun queryNode(node: BVHNode?, aabb: AABB, result: MutableList<Any>) {
        if (node == null) return

        if (node.aabb.overlaps(aabb)) {
            if (node.isLeaf) {
                result.add(node.obj!!)
            } else {
                queryNode(node.left, aabb, result)
                queryNode(node.right, aabb, result)
            }
        }
    }
}

data class BVHNode(
    var aabb: AABB,
    var left: BVHNode? = null,
    var right: BVHNode? = null,
    var obj: FhysicsObject? = null // Only non-null for leaf nodes
) {
    fun drawNode() {
        DebugDrawer.transformAndDrawBVHNode(aabb)
        left?.drawNode()
        right?.drawNode()
    }

    fun drawObjects(drawer: FhysicsObjectDrawer) {
        if (isLeaf) {
            drawer.drawObject(obj!!)
        } else {
            left?.drawObjects(drawer)
            right?.drawObjects(drawer)
        }
    }

    // Check if this node is a leaf node
    val isLeaf: Boolean
        get() = obj != null
}
