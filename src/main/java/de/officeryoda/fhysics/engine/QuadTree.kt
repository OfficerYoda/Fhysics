package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.extensions.contains
import de.officeryoda.fhysics.extensions.intersects
import de.officeryoda.fhysics.objects.FhysicsObject
import java.awt.geom.Rectangle2D
import kotlin.reflect.KFunction2

data class QuadTree(
    private val boundary: Rectangle2D,
    private val parent: QuadTree?,
) {

    val objects: MutableList<FhysicsObject> = ArrayList()
    private val isMinWidth: Boolean = boundary.width < 1 // minimum width of 1 to prevent infinite subdivision
    private val isRoot: Boolean = parent == null

    var divided: Boolean = false
        private set

    // child nodes
    var topLeft: QuadTree? = null
        private set
    var topRight: QuadTree? = null
        private set
    var botLeft: QuadTree? = null
        private set
    var botRight: QuadTree? = null
        private set

    private val rebuildObjects: HashSet<FhysicsObject> = HashSet()

    /// =====basic functions=====
    fun insert(obj: FhysicsObject) {
        if (!boundary.intersects(obj)) return
        if (objects.contains(obj)) return

        if ((objects.size < capacity && !divided) || (isMinWidth && !divided)) {
            objects.add(obj)
            return
        }

        if (!divided) {
            subdivide()
        }

        insertInChildren(obj)
    }

    fun query(pos: Vector2): FhysicsObject? {
        if(!boundary.contains(pos)) return null

        if (divided) {
            return topLeft!!.query(pos) ?:
                    topRight!!.query(pos) ?:
                    botLeft!!.query(pos) ?:
                    botRight!!.query(pos)
        }

        // check if any object in the node contains the position
        return objects.firstOrNull { it.contains(pos) }
    }

    // probably should have chosen another name
    private fun insertInChildren(obj: FhysicsObject) {
        // need to check every Child due to border Objects
        topLeft!!.insert(obj)
        topRight!!.insert(obj)
        botLeft!!.insert(obj)
        botRight!!.insert(obj)
    }

    private fun subdivide() {
        val x: Float = boundary.x.toFloat()
        val y: Float = boundary.y.toFloat()
        val hw: Float = boundary.width.toFloat() / 2 // half width
        val hh: Float = boundary.height.toFloat() / 2 // half height

        // top left
        val tl = Rectangle2D.Float(x, y + hh, hw, hh)
        topLeft = QuadTree(tl, this)
        // top right
        val tr = Rectangle2D.Float(x + hw, y + hh, hw, hh)
        topRight = QuadTree(tr, this)
        // bottom left
        val bl = Rectangle2D.Float(x, y, hw, hh)
        botLeft = QuadTree(bl, this)
        // bottom right
        val br = Rectangle2D.Float(x + hw, y, hw, hh)
        botRight = QuadTree(br, this)

        objects.forEach { insertInChildren(it) }
        objects.clear()

        divided = true
    }

    /// =====rebuild functions=====
    fun updateObjectsAndRebuild() {
        if (divided) {
            // rebuild the children first
            updateChildren()
            // insert any objects that need to be rebuilt
            insertRebuildObjects()
            // collapse the node if possible
            tryCollapse()
        } else {
            if (isRoot) {
                // just update the objects if it is the root
                objects.forEach { updateObject(it) }
            } else {
                handleNonRootNode()
            }
        }
    }

    private fun updateChildren() {
        if (isRoot) {
            // update root children async
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

        // this doesn't take object on the edges into account, but it should be fine
        val objectsInChildren: Int = topLeft!!.count() + topRight!!.count() + botLeft!!.count() + botRight!!.count()
        if (objectsInChildren < capacity) {
            divided = false
            // add every child object to the parent
            // set to prevent duplicates due to the edges
            val objectsSet: HashSet<FhysicsObject> = HashSet()
            objectsSet.addAll(topLeft!!.objects)
            objectsSet.addAll(topRight!!.objects)
            objectsSet.addAll(botLeft!!.objects)
            objectsSet.addAll(botRight!!.objects)

            objects.addAll(objectsSet)
        }
    }

    private fun addRebuildObject(obj: FhysicsObject) {
        // if the object is still fully in the boundary, or it is the root, add it to the rebuild list
        if (boundary.contains(obj) || isRoot) {
            // only need to execute it async if it is the root
            if (isRoot) {
                synchronized(rebuildObjects) {
                    rebuildObjects.add(obj)
                }
            } else {
                rebuildObjects.add(obj)
            }
        } else {
            // if the object is not within the boundary, add the object to the parent's rebuild list
            parent!!.addRebuildObject(obj)
        }
    }

    private fun insertRebuildObjects() {
        for (obj: FhysicsObject in rebuildObjects) {
            insert(obj)
        }
        rebuildObjects.clear()
    }

    /// =====update functions=====
    private fun updateObject(it: FhysicsObject) {
        it.updatePosition()

        FhysicsCore.checkBorderCollision(it)
    }

    /// =====async functions=====
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

    /// =====utility functions=====
    private fun count(): Int {
        return if (divided) {
            topLeft!!.count() + topRight!!.count() + botLeft!!.count() + botRight!!.count()
        } else {
            objects.size
        }
    }

    fun draw(drawRect: KFunction2<Rectangle2D, Int, Unit>) {
        if (!divided) {
            drawRect(boundary, objects.size)
        } else {
            topLeft!!.draw(drawRect)
            topRight!!.draw(drawRect)
            botLeft!!.draw(drawRect)
            botRight!!.draw(drawRect)
        }
    }

    override fun toString(): String {
        return if (divided) {
            "de.officeryoda.fhysics.engine.QuadTree(boundary=$boundary, capacity=$capacity, objects.size=${objects.size}, " +
                    "divided=true, canDivide=$isMinWidth, \n\ttopLeft=$topLeft, \n\ttopRight=$topRight, \n\tbotLeft=$botLeft, \n\tbotRight=$botRight)"
        } else {
            "de.officeryoda.fhysics.engine.QuadTree(boundary=$boundary, capacity=$capacity, objects.size=${objects.size}, divided=false, canDivide=$isMinWidth)"
        }
    }

    companion object {
        var capacity: Int = 32
            set(value) {
                field = value.coerceAtLeast(1)
            }
    }
}
