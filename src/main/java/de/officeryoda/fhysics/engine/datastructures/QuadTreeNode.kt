package de.officeryoda.fhysics.engine.datastructures

import de.officeryoda.fhysics.engine.collision.CollisionSolver
import de.officeryoda.fhysics.engine.datastructures.OldQuadTree.Companion.capacity
import de.officeryoda.fhysics.engine.datastructures.OldQuadTree.Companion.pendingRemovals
import de.officeryoda.fhysics.engine.math.BoundingBox
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject

data class QuadTreeNode(
    private var boundary: BoundingBox,
    private var parent: QuadTreeNode?,
) {

    // Objects in this node
    val objects: MutableList<FhysicsObject> = ArrayList()
    private val isMinWidth: Boolean = boundary.width <= 1 // Minimum width of 1 to prevent infinite division
    private val isRoot: Boolean = parent == null
    private var divided: Boolean = true

    // Child nodes
    private val children: Array<QuadTreeNode?> = arrayOfNulls(4)

    private val rebuildObjects: HashSet<FhysicsObject> = HashSet()

    /// region =====Basic functions=====
    fun query(pos: Vector2): FhysicsObject? {
        if (!boundary.contains(pos)) return null

        if (divided) {
            return children.firstNotNullOfOrNull { it?.query(pos) }
        }

        // Check if any object in the node contains the position
        return objects.firstOrNull { it.contains(pos) }
    }

    fun insert(obj: FhysicsObject): Boolean {
        when {
            // Insert if the object is in the boundary or if it's the root node
            !(boundary.overlaps(obj.boundingBox) || isRoot) -> return false

            // Check if the object is already in the node
            objects.contains(obj) -> return false

            !divided && (objects.size < capacity || isMinWidth) -> {
                objects.add(obj)
                return true
            }

            !divided -> {
                divide()
            }
        }

        insertInChildren(obj)
        return true
    }

    // Probably should've chosen another name
    private fun insertInChildren(obj: FhysicsObject) {
        // Need to check every Child due to border Objects
        val successfullyInserted: Boolean = children.any { it?.insert(obj) == true }

        // If the object was not inserted in any child, move it inside the border and try again
        // This should only be called if everything else fails
        if (!successfullyInserted) {
            // TODO: Make a check before inserting if the object is in the root boundary
            System.err.println("Object could not be inserted in any child node")
            CollisionSolver.moveInsideBorder(obj)
            obj.updateBoundingBox() // Update bounding box since it's used to check if the object is in the boundary
            insertInChildren(obj)
        }
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
        // TODO: Check if the node pool is working correctly
        return QuadTree.nodePool.getInstance()?.apply {
            this.boundary = boundary
            this.parent = this@QuadTreeNode
        } ?: QuadTreeNode(boundary, this).apply {
            this.boundary = boundary
            this.parent = this@QuadTreeNode
        }
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
            // root children are rebuild async
            synchronized(parent!!.rebuildObjects) {
                parent!!.rebuildObjects.add(obj)
            }
        } else {
            parent!!.rebuildObjects.add(obj)
        }
    }

    private fun removeInvalidObjects() {
        val toRemove = mutableListOf<FhysicsObject>()

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
        val objectsInChildren: Int = children.sumOf { it!!.objects.size }
        if (objectsInChildren <= capacity) {
            divided = false
            // Add every child object to the parent
            // Use a Set to prevent duplicates due to objects on edges being in multiple children
            val objectsSet: HashSet<FhysicsObject> = HashSet()
            objectsSet.addAll(rebuildObjects)
            children.forEach { objectsSet.addAll(it!!.objects) }

            objects.addAll(objectsSet)
        }
    }
    /// endregion
}