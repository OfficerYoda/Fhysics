package de.officeryoda.fhysics.engine.datastructures

import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.collision.CollisionSolver
import de.officeryoda.fhysics.engine.math.BoundingBox
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.extensions.sumOf
import de.officeryoda.fhysics.rendering.DebugDrawer
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer
import kotlin.math.max
import de.officeryoda.fhysics.engine.math.BoundingBox as AABB

/**
 * A bounding volume hierarchy (BVH) is a tree structure on a set of geometric objects.
 */
object BVH {
    private var root: BVHNode? = null

    fun build(objects: List<FhysicsObject>) {
        root = buildRecursive(objects)
    }

    private fun buildRecursive(objects: List<FhysicsObject>): BVHNode {
        if (objects.size == 1) return BVHNode(objects.first())

        // Split objects into two groups using SAH
        val (leftObjects: List<FhysicsObject>, rightObjects: List<FhysicsObject>) = splitObjects(objects)

        // Create parent node and recurse
        val leftNode: BVHNode = buildRecursive(leftObjects)
        val rightNode: BVHNode = buildRecursive(rightObjects)
        val parentVolume: BoundingBox = leftNode.aabb.merge(rightNode.aabb)
        return BVHNode(parentVolume, leftNode, rightNode)
    }

    // Split objects into two groups based on Surface Area Heuristic (SAH)
    private fun splitObjects(objects: List<FhysicsObject>): Pair<List<FhysicsObject>, List<FhysicsObject>> {
        // Sort objects by their x position
        val sortedObjects: List<FhysicsObject> = objects.sortedBy { it.position.x }
        // Calculate the total area of all bounding boxes
        val totalArea: Float = objects.sumOf { it.boundingBox.area() }

        var bestCost: Float = Float.MAX_VALUE
        var bestSplit = 0
        // Iterate through possible split points
        for (i: Int in 1 until objects.size) {
            // Calculate the area of the left and right groups
            val leftArea: Float = sortedObjects.subList(0, i).sumOf { it.boundingBox.area() }
            val rightArea: Float = sortedObjects.subList(i, objects.size).sumOf { it.boundingBox.area() }

            // Calculate the cost of the split
            val leftFraction: Float = leftArea / totalArea
            val rightFraction: Float = rightArea / totalArea
            val cost: Float = leftFraction * leftArea + rightFraction * rightArea

            // Update the best split if the current cost is lower
            if (cost < bestCost) {
                bestCost = cost
                bestSplit = i
            }
        }

        // Return the two groups of objects
        return Pair(sortedObjects.subList(0, bestSplit), sortedObjects.subList(bestSplit, objects.size))
    }

    fun insert(objects: List<FhysicsObject>) {
        if (root == null) {
            // Build the BVH from scratch if it's empty
            build(objects)
        } else {
            // Insert objects into the existing BVH
            for (obj: FhysicsObject in objects) {
                insert(obj)
            }
        }
    }

    // Insert an object into the BVH
    private fun insert(obj: FhysicsObject) {
        val newLeaf = BVHNode(obj.boundingBox, obj = obj)
        if (root == null) {
            root = newLeaf
        } else {
            root = insertNode(root!!, newLeaf)
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

    // Remove an object from the BVH
    fun remove(obj: FhysicsObject) {
        root = removeNode(root, obj)
    }

    /**
     * Removes an object from the BVH.
     *
     * @param node The node to remove the object from.
     * @param obj The object to remove.
     * @return The new root node of the BVH.
     */
    private fun removeNode(node: BVHNode?, obj: FhysicsObject): BVHNode? {
        if (node == null) return null

        if (node.isLeaf) {
            // Return no node if the object is the one to remove
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
    fun query(aabb: AABB, result: MutableList<FhysicsObject>) {
        queryNode(root, aabb, result)
    }

    /**
     * Recursively query the BVH for objects that overlap with a given AABB.
     *
     * @param node The node to query.
     * @param aabb The AABB to query with.
     * @param result The list to store the results in.
     */
    private fun queryNode(node: BVHNode?, aabb: AABB, result: MutableList<FhysicsObject>) {
        if (node == null) return
        if (!node.aabb.overlaps(aabb)) return

        if (node.isLeaf) {
            result.add(node.obj!!)
        } else {
            queryNode(node.left, aabb, result)
            queryNode(node.right, aabb, result)
        }
    }

    // Update the BVH for dynamic objects
    fun rebuild() {
        root?.updateBoundingVolume()
    }

    fun clear() {
        root = null
    }

    fun updateFhysicsObjects() {
        root?.updateFhysicsObjects()
    }

    fun handleCollisions() {
        root?.handleBorderCollision()

        // Collect all potential collisions
        val collisions: MutableList<CollisionInfo> = mutableListOf()
        root?.let { traverseAndCollect(it, collisions) }

        // Solve all collected collisions
        collisions.forEach { CollisionSolver.solveCollision(it) }
    }

    private fun traverseAndCollect(node: BVHNode, collisions: MutableList<CollisionInfo>) {
        if (node.isLeaf) return

        val left: BVHNode? = node.left
        val right: BVHNode? = node.right

        // Collect potential collisions between left and right subtrees
        if (!node.isLeaf) {
            collectPotentialCollisions(left!!, right!!, collisions)
        }

        // Recurse into children
        left?.let { traverseAndCollect(it, collisions) }
        right?.let { traverseAndCollect(it, collisions) }
    }

    private fun collectPotentialCollisions(nodeA: BVHNode, nodeB: BVHNode, collisions: MutableList<CollisionInfo>) {
        when {
            // No bounding volume overlap -> no collision
            !nodeA.aabb.overlaps(nodeB.aabb) -> return

            // Both nodes contain objects -> test for collision
            nodeA.isLeaf && nodeB.isLeaf -> {
                val info: CollisionInfo = nodeA.obj!!.testCollision(nodeB.obj!!)
                if (info.hasCollision) {
                    collisions.add(info)
                }
            }

            // nodeA is a tree, and nodeB is a leaf or a tree with smaller area -> recurse into nodeA
            !nodeA.isLeaf && (nodeB.isLeaf || nodeB.aabb.area() < nodeA.aabb.area()) -> {
                collectPotentialCollisions(nodeA.left!!, nodeB, collisions)
                collectPotentialCollisions(nodeA.right!!, nodeB, collisions)
            }

            // nodeB is a tree, and nodeA is a leaf or a tree with smaller area -> recurse into nodeB
            else -> {
                collectPotentialCollisions(nodeA, nodeB.left!!, collisions)
                collectPotentialCollisions(nodeA, nodeB.right!!, collisions)
            }
        }
    }

    // Render the BVH for debugging
    fun drawNodes() {
        root?.drawNodeBoundingVolume()
    }

    fun drawObjects(drawer: FhysicsObjectDrawer) {
        root?.drawObjects(drawer)
    }
}

private data class BVHNode(
    var aabb: AABB,
    var left: BVHNode? = null,
    var right: BVHNode? = null,
    var obj: FhysicsObject? = null // Only non-null for leaf nodes
) {
    constructor(fhysicsObject: FhysicsObject) : this(fhysicsObject.boundingBox, obj = fhysicsObject)

    // Check if this node is a leaf node
    val isLeaf: Boolean
        get() = obj != null

    fun updateBoundingVolume() {
        if (!isLeaf) {
            left?.updateBoundingVolume()
            right?.updateBoundingVolume()
            aabb = left!!.aabb.merge(right!!.aabb)
        }
    }

    fun handleBorderCollision() {
        if (isLeaf) {
            CollisionSolver.handleBorderCollisions(obj!!)
        } else {
            left?.handleBorderCollision()
            right?.handleBorderCollision()
        }
    }

    fun updateFhysicsObjects() {
        if (isLeaf) {
            obj!!.update()
        } else {
            left?.updateFhysicsObjects()
            right?.updateFhysicsObjects()
        }
    }

    // returns the depth of the node in the tree
    fun drawNodeBoundingVolume(): Int {
        val depth: Int = max(left?.drawNodeBoundingVolume() ?: 0, right?.drawNodeBoundingVolume() ?: 0)

        fun increaseAABBSize(aabb: AABB, amount: Float): AABB {
            return AABB(
                aabb.x - amount,
                aabb.y - amount,
                aabb.width + 2 * amount,
                aabb.height + 2 * amount
            )
        }

        DebugDrawer.transformAndDrawBVHNode(increaseAABBSize(aabb, depth * 0.15f))

        return if (isLeaf) 0 else depth + 1
    }

    fun drawObjects(drawer: FhysicsObjectDrawer) {
        if (isLeaf) {
            drawer.drawObject(obj!!)
        } else {
            left?.drawObjects(drawer)
            right?.drawObjects(drawer)
        }
    }
}
