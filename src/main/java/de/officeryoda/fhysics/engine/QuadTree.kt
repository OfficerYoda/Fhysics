package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.collision.CollisionSolver
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.rendering.DebugDrawer
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer
import de.officeryoda.fhysics.rendering.UIController
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

data class QuadTree(
    private val boundary: BoundingBox,
    private val parent: QuadTree?,
) {

    private lateinit var threadPool: ExecutorService

    val objects: MutableList<FhysicsObject> = ArrayList()
    private val isMinWidth: Boolean = boundary.width <= 1 // Minimum width of 1 to prevent infinite division
    private val isRoot: Boolean = parent == null

    private var divided: Boolean = false

    // Child nodes
    private var topLeft: QuadTree? = null
    private var topRight: QuadTree? = null
    private var botLeft: QuadTree? = null
    private var botRight: QuadTree? = null

    private val rebuildObjects: HashSet<FhysicsObject> = HashSet()

    init {
        if (isRoot) {
            root = this
            // Only need thread pool in root
            threadPool = Executors.newFixedThreadPool(4)
        }
    }

    /// =====Basic functions=====
    private fun insert(obj: FhysicsObject) {
        if (!boundary.overlaps(obj.boundingBox)) return
        if (objects.contains(obj)) return

        if (!divided && (objects.size < capacity || isMinWidth)) {
            objects.add(obj)
            return
        }

        if (!divided) {
            divide()
        }

        insertInChildren(obj)
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
        topLeft!!.insert(obj)
        topRight!!.insert(obj)
        botLeft!!.insert(obj)
        botRight!!.insert(obj)
    }

    private fun divide() {
        val x: Float = boundary.x
        val y: Float = boundary.y
        val hw: Float = boundary.width / 2 // half width
        val hh: Float = boundary.height / 2 // half height

        // Top left
        val tl = BoundingBox(x, y + hh, hw, hh)
        topLeft = QuadTree(tl, this)
        // Top right
        val tr = BoundingBox(x + hw, y + hh, hw, hh)
        topRight = QuadTree(tr, this)
        // Bottom left
        val bl = BoundingBox(x, y, hw, hh)
        botLeft = QuadTree(bl, this)
        // Bottom right
        val br = BoundingBox(x + hw, y, hw, hh)
        botRight = QuadTree(br, this)

        objects.forEach { insertInChildren(it) }
        objects.clear()

        divided = true
    }

    fun insertObjects() {
        toAdd.forEach { insert(it) }
        toAdd.clear()
    }

    /// =====Rebuild and update functions=====
    fun updateObjectsAndRebuild() {
        if (divided) {
            // Rebuild the children first
            updateChildren()
            // Insert any objects that need to be rebuilt
            insertRebuildObjects()
            // Collapse the node if possible
            tryCollapse()
        } else {
            if (isRoot) {
                // Remove objects that are queued for removal
                objects.removeAll(removeQueue)
                // Update the remaining objects
                objects.forEach { updateObject(it) }
            } else {
                handleNonRootNode()
            }
        }

        // All objects that are queued for removal are removed
        if (isRoot) {
            removeQueue.clear()
        }
    }

    private fun updateChildren() {
        if (isRoot) {
            // Update root children async
            updateObjectsAndRebuildChildrenAsync()
        } else {
            topLeft!!.updateObjectsAndRebuild()
            topRight!!.updateObjectsAndRebuild()
            botLeft!!.updateObjectsAndRebuild()
            botRight!!.updateObjectsAndRebuild()
        }
    }

    private fun handleNonRootNode() {
        val toRemove = ArrayList<FhysicsObject>()

        for (obj: FhysicsObject in objects) {
            if (removeQueue.contains(obj)) {
                toRemove.add(obj)
                continue
            }
            // Update each object
            updateObject(obj)
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
            // Only need to execute it async if it is the root
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

    private fun updateObject(it: FhysicsObject) {
        it.updatePosition()

        CollisionSolver.checkBorderCollision(it)
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

    fun tryDivide() {
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

    /// =====Collision functions=====
    fun handleCollisions() {
        if (divided) {
            handleCollisionsInChildren()
        } else {
            val numObjects: Int = objects.size

            for (i: Int in objects.indices) {
                for (j: Int in i + 1 until numObjects) {
                    handleCollision(objects[i], objects[j])
                }
            }
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

    private fun handleCollision(objA: FhysicsObject, objB: FhysicsObject) {
        if (objA.static && objB.static) return

        val info: CollisionInfo = objA.testCollision(objB)

        if (!info.hasCollision) return
        CollisionSolver.separateOverlappingObjects(info) // Separate before finding contact points or contact points might be inside objects
        val contactPoints: Array<Vector2> = objA.findContactPoints(objB, info)
        CollisionSolver.solveCollisionWithRotation(info, contactPoints)
    }

    /// =====Drawing functions=====
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
                DebugDrawer.drawBoundingBox(it)
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

    /// =====Async functions=====
    private fun updateObjectsAndRebuildChildrenAsync() {
        val futures: MutableList<Future<*>> = mutableListOf()
        futures.add(threadPool.submit { topLeft!!.updateObjectsAndRebuild() })
        futures.add(threadPool.submit { topRight!!.updateObjectsAndRebuild() })
        futures.add(threadPool.submit { botLeft!!.updateObjectsAndRebuild() })
        futures.add(threadPool.submit { botRight!!.updateObjectsAndRebuild() })

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

    /// =====Utility functions=====
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

    override fun toString(): String {
        return if (divided) {
            "QuadTree(boundary=$boundary, capacity=$capacity, objects.size=${objects.size}, " +
                    "divided=true, isMinWidth=$isMinWidth, \n\ttopLeft=$topLeft, \n\ttopRight=$topRight, \n\tbotLeft=$botLeft, \n\tbotRight=$botRight)"
        } else {
            "QuadTree(boundary=$boundary, capacity=$capacity, objects.size=${objects.size}, divided=false, isMinWidth=$isMinWidth)"
        }
    }

    companion object {
        lateinit var root: QuadTree
            private set

        var capacity: Int = 32
            set(value) {
                field = value.coerceAtLeast(1)
            }

        // List of objects to add and remove
        // This is used to prevent concurrent modification exceptions
        val toAdd: MutableList<FhysicsObject> = ArrayList()
        val removeQueue: MutableSet<FhysicsObject> = HashSet()
    }
}