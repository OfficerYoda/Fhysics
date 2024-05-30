package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.extensions.contains
import de.officeryoda.fhysics.extensions.intersects
import de.officeryoda.fhysics.objects.FhysicsObject
import de.officeryoda.fhysics.rendering.UIController
import java.awt.geom.Rectangle2D
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2

data class QuadTree(
    private val boundary: Rectangle2D,
    private val parent: QuadTree?,
) {

    val objects: MutableList<FhysicsObject> = ArrayList()
    private val isMinWidth: Boolean = boundary.width <= 1 // Minimum width of 1 to prevent infinite division
    private val isRoot: Boolean = parent == null

    var divided: Boolean = false
        private set

    // Child nodes
    var topLeft: QuadTree? = null
        private set
    var topRight: QuadTree? = null
        private set
    var botLeft: QuadTree? = null
        private set
    var botRight: QuadTree? = null
        private set

    private val rebuildObjects: HashSet<FhysicsObject> = HashSet()

    init {
        if (isRoot) {
            root = this
        }
    }

    /// =====Basic functions=====
    private fun insert(obj: FhysicsObject) {
        if (!boundary.intersects(obj)) return
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
        val x: Float = boundary.x.toFloat()
        val y: Float = boundary.y.toFloat()
        val hw: Float = boundary.width.toFloat() / 2 // half width
        val hh: Float = boundary.height.toFloat() / 2 // half height

        // Top left
        val tl = Rectangle2D.Float(x, y + hh, hw, hh)
        topLeft = QuadTree(tl, this)
        // Top right
        val tr = Rectangle2D.Float(x + hw, y + hh, hw, hh)
        topRight = QuadTree(tr, this)
        // Bottom left
        val bl = Rectangle2D.Float(x, y, hw, hh)
        botLeft = QuadTree(bl, this)
        // Bottom right
        val br = Rectangle2D.Float(x + hw, y, hw, hh)
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
            return
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
            if (!boundary.contains(obj)) {
                parent!!.addRebuildObject(obj)
                toRemove.add(obj)
            }
        }

        objects.removeAll(toRemove)
    }

    private fun tryCollapse() {
        if (!divided) return

        // This doesn't take object on the edges into account, but it should be fine
        val objectsInChildren: Int = topLeft!!.count() + topRight!!.count() + botLeft!!.count() + botRight!!.count()
        if (objectsInChildren < capacity) {
            divided = false
            // Add every child object to the parent
            // Set to prevent duplicates due to the edges
            val objectsSet: HashSet<FhysicsObject> = HashSet()
            objectsSet.addAll(topLeft!!.objects)
            objectsSet.addAll(topRight!!.objects)
            objectsSet.addAll(botLeft!!.objects)
            objectsSet.addAll(botRight!!.objects)

            objects.addAll(objectsSet)
        }
    }

    fun tryDivide() {
        if (divided) {
            topLeft!!.tryDivide()
            topRight!!.tryDivide()
            botLeft!!.tryDivide()
            botRight!!.tryDivide()
        } else if (objects.size > capacity) {
            divide()
        }
    }

    private fun addRebuildObject(obj: FhysicsObject) {
        // If the object is still fully in the boundary, or it is the root, add it to the rebuild list
        if (boundary.contains(obj) || isRoot) {
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

        FhysicsCore.checkBorderCollision(it)
    }

    /// =====Drawing functions=====
    fun drawObjects(drawObject: KFunction1<FhysicsObject, Unit>, drawBoundingBox: (FhysicsObject) -> Unit) {
        when {
            divided -> {
                topLeft?.drawObjects(drawObject, drawBoundingBox)
                topRight?.drawObjects(drawObject, drawBoundingBox)
                botLeft?.drawObjects(drawObject, drawBoundingBox)
                botRight?.drawObjects(drawObject, drawBoundingBox)
            }

            UIController.drawBoundingBoxes -> objects.forEach {
                drawObject(it)
                drawBoundingBox(it)
            }

            else -> objects.forEach { drawObject(it) }
        }
    }

    fun drawNode(drawRect: KFunction2<Rectangle2D, Int, Unit>) {
        if (!divided) {
            drawRect(boundary, objects.size)
        } else {
            topLeft!!.drawNode(drawRect)
            topRight!!.drawNode(drawRect)
            botLeft!!.drawNode(drawRect)
            botRight!!.drawNode(drawRect)
        }
    }

    /// =====Async functions=====
    private fun updateObjectsAndRebuildChildrenAsync() {
        val tl = Thread { topLeft!!.updateObjectsAndRebuild() }
        val tr = Thread { topRight!!.updateObjectsAndRebuild() }
        val bl = Thread { botLeft!!.updateObjectsAndRebuild() }
        val br = Thread { botRight!!.updateObjectsAndRebuild() }

        tl.start()
        tr.start()
        bl.start()
        br.start()

        tl.join()
        tr.join()
        bl.join()
        br.join()
    }

    /// =====Utility functions=====
    /**
     * Counts the objects in the QuadTree
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
     * Counts the unique objects in the QuadTree
     *
     * Unlike the [count] function, this function will not count the same object multiple times
     *
     * @param objectSet The set which is used for counting
     * @return The amount of unique objects in the QuadTree
     */
    fun countUnique(objectSet: MutableSet<FhysicsObject> = HashSet()): Int {
        this.count()
        if (divided) {
            topLeft!!.countUnique(objectSet) + topRight!!.countUnique(objectSet) + botLeft!!.countUnique(objectSet) + botRight!!.countUnique(objectSet)
        } else {
            objectSet.addAll(objects)
        }

        return objectSet.size
    }

    override fun toString(): String {
        return if (divided) {
            "QuadTree(boundary=$boundary, capacity=$capacity, objects.size=${objects.size}, " +
                    "divided=true, canDivide=$isMinWidth, \n\ttopLeft=$topLeft, \n\ttopRight=$topRight, \n\tbotLeft=$botLeft, \n\tbotRight=$botRight)"
        } else {
            "QuadTree(boundary=$boundary, capacity=$capacity, objects.size=${objects.size}, divided=false, canDivide=$isMinWidth)"
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