package de.officeryoda.fhysics.engine.datastructures

import de.officeryoda.fhysics.engine.collision.CollisionSolver
import de.officeryoda.fhysics.engine.datastructures.OldQuadTree.Companion.capacity
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
        if (!(boundary.overlaps(obj.boundingBox) || isRoot)) return false
        if (objects.contains(obj)) return false

        if (!divided && (objects.size < capacity || isMinWidth)) {
            objects.add(obj)
            return true
        }

        // Check is necessary because the node could be min width // TODO: ??
        if (!divided) {
            divide()
        }

        insertInChildren(obj)
        return true
    }

    // Probably should have chosen another name
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
}