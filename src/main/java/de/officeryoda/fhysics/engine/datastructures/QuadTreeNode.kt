package de.officeryoda.fhysics.engine.datastructures

import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.collision.CollisionSolver
import de.officeryoda.fhysics.engine.datastructures.QuadTree.capacity
import de.officeryoda.fhysics.engine.datastructures.QuadTree.getPendingRemovals
import de.officeryoda.fhysics.engine.math.BoundingBox
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.rendering.DebugDrawer
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer
import de.officeryoda.fhysics.rendering.UIController

data class QuadTreeNode(
    private var boundary: BoundingBox,
    private var parent: QuadTreeNode?,
) {
    // Objects in this node
    val objects: MutableList<FhysicsObject> = ArrayList()

    // Objects that need to be reinserted after the node has been rebuilt
    private val rebuildObjects: HashSet<FhysicsObject> = HashSet()

    // Child nodes
    private val children: Array<QuadTreeNode?> = arrayOfNulls(4)

    private var isMinWidth: Boolean = boundary.width <= 1 // Minimum width of 1 to prevent infinite division
    private var isRoot: Boolean = parent == null
    private var divided: Boolean = false

    private val pendingRemovals: MutableSet<FhysicsObject>
        get() = getPendingRemovals()

    /// region =====Basic functions=====
    fun query(pos: Vector2): FhysicsObject? {
        return when {
            !boundary.contains(pos) -> null
            divided -> children.firstNotNullOfOrNull { it!!.query(pos) }
            else -> objects.firstOrNull { it.contains(pos) }
        }
    }

    fun insert(obj: FhysicsObject): Boolean {
        when {
            // Don't insert if the object is not in the boundary (must insert if it's the root)
            !(boundary.overlaps(obj.boundingBox) || isRoot) -> return false

            // Check if the object is already in the node
            objects.contains(obj) -> return false

            !divided && (objects.size < capacity || isMinWidth) -> {
                objects.add(obj)
                return true
            }

            !divided -> divide()
        }

        insertInChildren(obj)
        return true
    }

    // Probably should've chosen another name
    private fun insertInChildren(obj: FhysicsObject) {
        // Need to check every Child due to border Objects
        children.forEach { it!!.insert(obj) }
    }

    private fun divide() {
        val x: Float = boundary.x
        val y: Float = boundary.y
        val hw: Float = boundary.width / 2 // half width
        val hh: Float = boundary.height / 2 // half height

        // Top left
        val tl = BoundingBox(x, y + hh, hw, hh)
        children[0] = getNewNode(tl)
        // Top right
        val tr = BoundingBox(x + hw, y + hh, hw, hh)
        children[1] = getNewNode(tr)
        // Bottom left
        val bl = BoundingBox(x, y, hw, hh)
        children[2] = getNewNode(bl)
        // Bottom right
        val br = BoundingBox(x + hw, y, hw, hh)
        children[3] = getNewNode(br)

        objects.forEach { insertInChildren(it) }
        objects.clear()

        divided = true
    }

    private fun getNewNode(boundary: BoundingBox): QuadTreeNode {
        val node: QuadTreeNode = QuadTree.getNodeFromPool()
        // Assign new boundary and parent
        node.initialize(boundary, this)
        return node
    }

    /// endregion

    /// region =====Rebuild functions=====
    fun rebuild() {
        if (divided) {
            // Rebuild the children first
            rebuildChildren()
            // Insert any objects that need to be reinserted
            handleRebuildObjects()
            // Collapse the node if possible
            tryCollapse()
        } else {
            if (isRoot) {
                // Remove objects that are queued for removal
                objects.removeAll(pendingRemovals)
            } else {
                removeInvalidObjects()
            }
        }

        // All objects that are queued for removal will be removed at this point
        if (isRoot) {
            pendingRemovals.clear()
        }
    }

    private fun rebuildChildren() {
//        if (isRoot) { // TODO: implement async rebuild
//            // Update root children async
//            rebuildChildrenAsync()
//        } else {
        // Update children synchronously
        children.forEach { it!!.rebuild() }
//        }
    }

    private fun handleRebuildObjects() {
        for (obj: FhysicsObject in rebuildObjects) {
            // If the object is still fully in the boundary, or it is the root, insert it
            if (boundary.contains(obj.boundingBox) || isRoot) {
                insert(obj)
            } else {
                addRebuildObjectToParent(obj)
            }
        }
        rebuildObjects.clear()
    }

    private fun addRebuildObjectToParent(obj: FhysicsObject) {
        if (parent!!.isRoot) {
            // Make sure the object is inside the border
            CollisionSolver.moveInsideBorder(obj)

            // root children are rebuild async
            synchronized(parent!!.rebuildObjects) {
                parent!!.rebuildObjects.add(obj)
            }
        } else {
            parent!!.rebuildObjects.add(obj)
        }
    }

    private fun removeInvalidObjects() {
        val toRemove: MutableList<FhysicsObject> = mutableListOf()

        for (obj: FhysicsObject in objects) {
            if (pendingRemovals.contains(obj)) {
                toRemove.add(obj)
                continue
            }

            // If an object is not within the boundary, add the object to the parent's rebuild list and the removal list
            if (!boundary.contains(obj.boundingBox)) {
                parent!!.addRebuildObject(obj)
                toRemove.add(obj)
            }
        }

        objects.removeAll(toRemove)
    }

    private fun addRebuildObject(obj: FhysicsObject) {
        // If the object is still fully in the boundary, or it is the root, add it to the rebuild list
        if (boundary.contains(obj.boundingBox) || isRoot) {
            // Only need to execute it synchronized if it's adding to the root
            if (isRoot) {
                synchronized(rebuildObjects) {
                    rebuildObjects.add(obj)
                }
            } else {
                rebuildObjects.add(obj)
            }
        } else {
            // If the object is not within the boundary, add the object to the parent's rebuild list
            parent!!.addRebuildObject(obj)
        }
    }

    fun tryDivide() {
        when {
            divided -> children.forEach { it!!.tryDivide() }
            objects.size > capacity -> divide()
        }
    }

    private fun tryCollapse() {
        if (!divided) return

        // This doesn't take object on the edges into account, but it should be fine
        val objectsInChildren: Int = children.sumOf { it!!.count() }
        if (objectsInChildren > capacity) return

        // Collapse the children
        divided = false
        // Add every child object to the parent
        // Use a Set to prevent duplicates due to objects on edges being in multiple children
        val objectsSet: HashSet<FhysicsObject> = HashSet()
        objectsSet.addAll(rebuildObjects)
        children.forEach { objectsSet.addAll(it!!.objects) }

        // Add the children's objects to the parent
        objects.addAll(objectsSet)

        // Add the unused children to the pool
        children.forEach { QuadTree.addNodeToPool(it!!) }
    }
    /// endregion

    /// region =====FhysicsObject related functions=====
    fun updateFhysicsObjects() {
        if (divided) {
            children.forEach { it!!.updateFhysicsObjects() }
        } else {
            objects.forEach { it.update() }
        }
    }

    fun handleCollisions() {
        if (divided) {
            handleCollisionsInChildren()
            return
        }

        // Check for border collisions
        // NOTE: checking for border collisions before object collisions showed better results
        objects.forEach { CollisionSolver.handleBorderCollisions(it) }

        // Find collisions between objects
        val collisions: MutableList<CollisionInfo> = mutableListOf()
        for (i: Int in objects.indices) {
            for (j: Int in i + 1 until objects.size) {
                val objA: FhysicsObject = objects[i]
                val objB: FhysicsObject = objects[j]

                val info: CollisionInfo = objA.testCollision(objB)
                if (info.hasCollision) collisions.add(info)
            }
        }

        // Solve collisions between objects
        collisions.forEach { CollisionSolver.solveCollision(it) }
    }

    private fun handleCollisionsInChildren() {
//        if (isRoot) { // TODO: implement async collision handling
//            // Update root children async
//            handleCollisionsInChildrenAsync()
//        } else {
        children.forEach { it!!.handleCollisions() }
//        }
    }
    /// endregion

    /// region =====Drawing functions=====
    fun drawObjects(drawer: FhysicsObjectDrawer) {
        when {
            divided -> children.forEach { it!!.drawObjects(drawer) }
            UIController.drawBoundingBoxes -> objects.forEach {
                drawer.drawObject(it)
                DebugDrawer.drawBoundingBox(it.boundingBox)
            }

            else -> objects.forEach { drawer.drawObject(it) }
        }
    }

    fun drawNode() {
        if (divided) {
            children.forEach { it!!.drawNode() }
        } else {
            DebugDrawer.transformAndDrawQuadTreeNode(boundary, objects.size)
        }
    }
    /// endregion

    /// region =====Utility functions=====
    // Called when the object is retrieved from the pool
    private fun initialize(boundary: BoundingBox, parent: QuadTreeNode?) {
        this.boundary = boundary
        this.parent = parent
        this.isRoot = parent == null
        this.divided = false
        this.isMinWidth = boundary.width <= 1
    }

    // Called when the node is pooled
    fun clear() {
        objects.clear()
        rebuildObjects.clear()
    }

    /**
     * Counts the objects in this QuadTree node and its children
     *
     * This function will count the same object multiple times if it is in multiple nodes
     * For counting unique objects, use [countUnique]
     *
     * @return The amount of objects in the QuadTree
     */
    private fun count(): Int {
        return if (divided) {
            children.sumOf { it!!.count() }
        } else {
            objects.size
        }
    }

    /**
     * Counts the unique objects in this QuadTree node and its children
     *
     * Unlike the [count] function, this function will not count the same object multiple times
     *
     * @param objectSet The set which is used for counting
     * @return The amount of unique objects in the QuadTree
     */
    fun countUnique(objectSet: MutableSet<FhysicsObject> = HashSet()): Int {
        if (divided) {
            children.forEach { it!!.countUnique(objectSet) }
        } else {
            objectSet.addAll(objects)
        }

        return objectSet.size
    }

    /**
     * Updates the size of the nodes in the QuadTree
     *
     * This function is used to update the size of the nodes after the border size has changed
     * This function should only be called on the root node
     *
     * @param isTop If the node is at the top
     * @param isLeft If the node is at the left
     */
    fun updateNodeSizes(isTop: Boolean, isLeft: Boolean) {
        if (isRoot) {
            // root scales automatically with the border
            updateChildrenNodeSizes()
            return
        }

        val parentBounds: BoundingBox = parent!!.boundary

        // Calculate the new size of the node
        val halfWidth: Float = parentBounds.width / 2f
        val halfHeight: Float = parentBounds.height / 2f

        val xOffset: Float = if (isLeft) 0f else halfWidth
        val yOffset: Float = if (isTop) halfHeight else 0f

        // Update the boundary of the node
        boundary.x = parentBounds.x + xOffset
        boundary.y = parentBounds.y + yOffset
        boundary.width = halfWidth
        boundary.height = halfHeight

        updateChildrenNodeSizes()
    }

    private fun updateChildrenNodeSizes() {
        if (!divided) return

        children[0]!!.updateNodeSizes(isTop = true, isLeft = true) // Top left
        children[1]!!.updateNodeSizes(isTop = true, isLeft = false) // Top right
        children[2]!!.updateNodeSizes(isTop = false, isLeft = true)  // Bottom left
        children[3]!!.updateNodeSizes(isTop = false, isLeft = false) // Bottom right
    }

    override fun toString(): String {
        return if (divided) {
            "QuadTree(boundary=$boundary, capacity=$capacity, objects.size=${objects.size}, " +
                    "divided=true, isMinWidth=$isMinWidth, \n\ttopLeft=${children[0]}, \n\ttopRight=${children[1]}, \n\tbotLeft=${children[2]}, \n\tbotRight=${children[3]})"
        } else {
            "QuadTree(boundary=$boundary, capacity=$capacity, objects.size=${objects.size}, divided=false, isMinWidth=$isMinWidth)"
        }
    }
    /// endregion
}