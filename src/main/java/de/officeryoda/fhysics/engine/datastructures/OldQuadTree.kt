package de.officeryoda.fhysics.engine.datastructures

import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.collision.CollisionSolver
import de.officeryoda.fhysics.engine.math.BoundingBox
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.rendering.DebugDrawer
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer
import de.officeryoda.fhysics.rendering.UIController
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

private data class OldQuadTree(
    private val boundary: BoundingBox,
    private val parent: OldQuadTree?,
) {

    private lateinit var threadPool: ExecutorService

    val objects: MutableList<FhysicsObject> = ArrayList()
    private val isMinWidth: Boolean = boundary.width <= 1 // Minimum width of 1 to prevent infinite division
    private val isRoot: Boolean = parent == null

    private var divided: Boolean = false

    // Child nodes
    private var topLeft: OldQuadTree? = null
    private var topRight: OldQuadTree? = null
    private var botLeft: OldQuadTree? = null
    private var botRight: OldQuadTree? = null

    private val rebuildObjects: HashSet<FhysicsObject> = HashSet()

    init {
        if (isRoot) {
            root = this
            // Only need thread pool in root
            threadPool = Executors.newFixedThreadPool(4)
        }
    }

    fun shutdownThreadPool() {
        if (isRoot) {
            println("Shutting down thread pool")
            threadPool.shutdownNow()
        }
    }

    /// region =====Basic functions=====
    private fun insert(obj: FhysicsObject): Boolean {
        if (!(boundary.overlaps(obj.boundingBox) || isRoot)) return false
        if (objects.contains(obj)) return false

        if (!divided && (objects.size < capacity || isMinWidth)) {
            objects.add(obj)
            return true
        }

        // Check is necessary because the node could be min width
        if (!divided) {
            divide()
        }

        insertInChildren(obj)
        return true
    }

    fun query(pos: Vector2): FhysicsObject? {
        if (!boundary.contains(pos)) return null

        if (divided) {
            return topLeft!!.query(pos) ?: topRight!!.query(pos) ?: botLeft!!.query(pos) ?: botRight!!.query(pos)
        }

        // Check if any object in the node contains the position
        return objects.firstOrNull { it.contains(pos) }
    }

    // Probably should have chosen another name
    private fun insertInChildren(obj: FhysicsObject) {
        // Need to check every Child due to border Objects
        // This has a bug where the object isn't inserted in multiple children if it's on the border
        val successfullyInserted: Boolean =
            topLeft!!.insert(obj)
                    || topRight!!.insert(obj)
                    || botLeft!!.insert(obj)
                    || botRight!!.insert(obj)

        // If the object was not inserted in any child, move it inside the border and try again
        // This should only be called if everything else fails
        if (!successfullyInserted) {
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
        topLeft = OldQuadTree(tl, this)
        // Top right
        val tr = BoundingBox(x + hw, y + hh, hw, hh)
        topRight = OldQuadTree(tr, this)
        // Bottom left
        val bl = BoundingBox(x, y, hw, hh)
        botLeft = OldQuadTree(bl, this)
        // Bottom right
        val br = BoundingBox(x + hw, y, hw, hh)
        botRight = OldQuadTree(br, this)

        for (it: FhysicsObject in objects) {
            insertInChildren(it)
        }
        objects.clear()

        divided = true
    }

    fun insertPendingAdditions() {
        pendingAdditions.forEach { insert(it) }
        pendingAdditions.clear()
    }
    /// endregion

    /// region =====Rebuild and update functions=====
    fun rebuild() {
        // If the capacity was changed, divideNextUpdate will be true and the root should try to divide
        if (divideNextUpdate && isRoot) { //TODO: Only call this on the root
            divideNextUpdate = false
            tryDivide()
        }

        if (divided) {
            // Rebuild the children first
            rebuildChildren()
            // Insert any objects that need to be reinserted
            insertRebuildObjects()
            // Collapse the node if possible
            tryCollapse()
        } else {
            if (isRoot) {
                // Remove objects that are queued for removal
                objects.removeAll(pendingRemovals)
            } else {
                handleNonRootNode()
            }
        }

        // All objects that are queued for removal will be removed at this point
        if (isRoot) {
            pendingRemovals.clear()
        }
    }

    private fun handleNonRootNode() {
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

    private fun rebuildChildren() {
        if (isRoot) {
            // Update root children async
            rebuildChildrenAsync()
        } else {
            topLeft!!.rebuild()
            topRight!!.rebuild()
            botLeft!!.rebuild()
            botRight!!.rebuild()
        }
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

    private fun insertRebuildObjects() {
        for (obj: FhysicsObject in rebuildObjects) {
            insert(obj)
        }
        rebuildObjects.clear()
    }

    private fun tryCollapse() {
        if (!divided) return

        // This doesn't take object on the edges into account, but it should be fine
        val objectsInChildren: Int = topLeft!!.count() + topRight!!.count() + botLeft!!.count() + botRight!!.count()
        if (objectsInChildren <= capacity) {
            divided = false
            // Add every child object to the parent
            // Use a Set to prevent duplicates due to the edges
            val objectsSet: HashSet<FhysicsObject> = HashSet()
            objectsSet.addAll(rebuildObjects)
            objectsSet.addAll(topLeft!!.objects)
            objectsSet.addAll(topRight!!.objects)
            objectsSet.addAll(botLeft!!.objects)
            objectsSet.addAll(botRight!!.objects)

            objects.addAll(objectsSet)
        }
    }

    private fun tryDivide() {
        when {
            divided -> {
                topLeft!!.tryDivide()
                topRight!!.tryDivide()
                botLeft!!.tryDivide()
                botRight!!.tryDivide()
            }

            objects.size > capacity -> {
                divide()
            }
        }
    }

    fun updateObjects() {
        if (divided) {
            topLeft!!.updateObjects()
            topRight!!.updateObjects()
            botLeft!!.updateObjects()
            botRight!!.updateObjects()
        } else {
            objects.forEach { it.update() }
        }
    }
    /// endregion

    /// region =====Collision functions=====
    fun handleCollisions() {
        if (divided) {
            handleCollisionsInChildren()
        } else {
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
                    if (info.hasCollision) {
                        collisions.add(info)
                    }
                }
            }

            // Solve collisions between objects
            collisions.forEach { CollisionSolver.solveCollision(it) }
        }
    }

    private fun handleCollisionsInChildren() {
        if (isRoot) {
            // Update root children async
            handleCollisionsInChildrenAsync()
        } else {
            topLeft!!.handleCollisions()
            topRight!!.handleCollisions()
            botLeft!!.handleCollisions()
            botRight!!.handleCollisions()
        }
    }
    /// endregion

    /// region =====Drawing functions=====
    fun drawObjects(drawer: FhysicsObjectDrawer) {
        when {
            divided -> {
                topLeft?.drawObjects(drawer)
                topRight?.drawObjects(drawer)
                botLeft?.drawObjects(drawer)
                botRight?.drawObjects(drawer)
            }

            UIController.drawBoundingBoxes -> objects.forEach {
                drawer.drawObject(it)
                DebugDrawer.drawBoundingBox(it.boundingBox)
            }

            else -> objects.forEach { drawer.drawObject(it) }
        }
    }

    fun drawNode(drawer: FhysicsObjectDrawer) {
        if (!divided) {
            DebugDrawer.transformAndDrawQuadTreeNode(boundary, objects.size)
        } else {
            topLeft!!.drawNode(drawer)
            topRight!!.drawNode(drawer)
            botLeft!!.drawNode(drawer)
            botRight!!.drawNode(drawer)
        }
    }
    /// endregion

    /// region =====Async functions=====
    private fun rebuildChildrenAsync() {
        val futures: MutableList<Future<*>> = mutableListOf()
        futures.add(threadPool.submit { topLeft!!.rebuild() })
        futures.add(threadPool.submit { topRight!!.rebuild() })
        futures.add(threadPool.submit { botLeft!!.rebuild() })
        futures.add(threadPool.submit { botRight!!.rebuild() })

        // Wait for all tasks to finish
        waitForAllFutures(futures)
    }

    private fun handleCollisionsInChildrenAsync() {
        val futures: MutableList<Future<*>> = mutableListOf()
        futures.add(threadPool.submit { topLeft!!.handleCollisions() })
        futures.add(threadPool.submit { topRight!!.handleCollisions() })
        futures.add(threadPool.submit { botLeft!!.handleCollisions() })
        futures.add(threadPool.submit { botRight!!.handleCollisions() })

        // Wait for all tasks to finish
        waitForAllFutures(futures)
    }

    private fun waitForAllFutures(futures: MutableList<Future<*>>) {
        for (future: Future<*> in futures) {
            future.get()
        }
    }
    /// endregion

    /// region =====Utility functions=====
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
            topLeft!!.count() + topRight!!.count() + botLeft!!.count() + botRight!!.count()
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
        this.count()
        if (divided) {
            topLeft!!.countUnique(objectSet) + topRight!!.countUnique(objectSet) + botLeft!!.countUnique(objectSet) + botRight!!.countUnique(
                objectSet
            )
        } else {
            objectSet.addAll(objects)
        }

        return objectSet.size
    }

    /**
     * Updates the size of the nodes in the QuadTree
     * This function is used to update the size of the nodes after the border size has changed
     * This function should only be called on the root node
     *
     * @param isTop If the node is at the top
     * @param isLeft If the node is at the left
     */
    fun updateNodeSize(isTop: Boolean, isLeft: Boolean) {
        if (isRoot) {
            updateChildNodeSize()
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

        updateChildNodeSize()
    }

    private fun updateChildNodeSize() {
        if (divided) {
            topLeft!!.updateNodeSize(isTop = true, isLeft = true)
            topRight!!.updateNodeSize(isTop = true, isLeft = false)
            botLeft!!.updateNodeSize(isTop = false, isLeft = true)
            botRight!!.updateNodeSize(isTop = false, isLeft = false)
        }
    }

    override fun toString(): String {
        return if (divided) {
            "QuadTree(boundary=$boundary, capacity=$capacity, objects.size=${objects.size}, " +
                    "divided=true, isMinWidth=$isMinWidth, \n\ttopLeft=$topLeft, \n\ttopRight=$topRight, \n\tbotLeft=$botLeft, \n\tbotRight=$botRight)"
        } else {
            "QuadTree(boundary=$boundary, capacity=$capacity, objects.size=${objects.size}, divided=false, isMinWidth=$isMinWidth)"
        }
    }
    /// endregion

    companion object {
        lateinit var root: OldQuadTree
            private set

        var capacity: Int = 32
            set(value) {
                field = value.coerceAtLeast(1)
            }

        // Used to prevent concurrent modification exceptions
        var divideNextUpdate: Boolean = false

        // List of objects to add, used to queue up insertions and prevent concurrent modification
        val pendingAdditions: MutableList<FhysicsObject> = ArrayList()

        // Set of objects to remove, used to mark objects for deletion safely
        val pendingRemovals: MutableSet<FhysicsObject> = HashSet()
    }
}