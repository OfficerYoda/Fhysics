package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.extensions.contains
import de.officeryoda.fhysics.extensions.intersects
import de.officeryoda.fhysics.objects.FhysicsObject
import java.awt.geom.Rectangle2D
import kotlin.reflect.KFunction2

data class QuadTree(
    private val boundary: Rectangle2D,
    private val capacity: Int,
    private val parent: QuadTree?,
) {
    val objects: MutableList<FhysicsObject> = ArrayList()

    var divided: Boolean = false

    var topLeft: QuadTree? = null
    var topRight: QuadTree? = null
    var botLeft: QuadTree? = null
    var botRight: QuadTree? = null

    private val rebuildObjects: MutableList<FhysicsObject> = ArrayList()

    /// =====insertion functions=====

    fun insert(obj: FhysicsObject) {
        if (!boundary.intersects(obj)) return
        if (objects.contains(obj)) return

        if (objects.size < capacity && !divided) {
            objects.add(obj)
            return
        }

        if (!divided) {
            subdivide()
        }

        insertInChildren(obj)
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
        val x: Double = boundary.x
        val y: Double = boundary.y
        val hw: Double = boundary.width / 2 // half width
        val hh: Double = boundary.height / 2 // half height

        // top left
        val tl = Rectangle2D.Double(x, y + hh, hw, hh)
        topLeft = QuadTree(tl, capacity, this)
        // top right
        val tr = Rectangle2D.Double(x + hw, y + hh, hw, hh)
        topRight = QuadTree(tr, capacity, this)
        // bottom left
        val bl = Rectangle2D.Double(x, y, hw, hh)
        botLeft = QuadTree(bl, capacity, this)
        // bottom right
        val br = Rectangle2D.Double(x + hw, y, hw, hh)
        botRight = QuadTree(br, capacity, this)

        objects.forEach { insertInChildren(it) }
        objects.clear()

        divided = true
    }

    /// =====rebuild functions=====

    fun rebuild() {
        if (divided) {
            if (parent == null) {
                rebuildChildrenAsync()
            } else {
                topLeft!!.rebuild()
                topRight!!.rebuild()
                botLeft!!.rebuild()
                botRight!!.rebuild()
            }

            insertRebuildObjects()
            tryCollapse()
        } else {
            // check if the objects are still in the boundary
            val toRemove = ArrayList<FhysicsObject>()
            for (obj in objects) {
                // only keep if fully contained in the boundary
                if (!boundary.contains(obj)) {
                    // if parent is null, the quadTree is the root,
                    // and it should not be removed to not remove it from the whole tree
                    if (parent != null) {
                        parent.addRebuildObject(obj)
                        toRemove.add(obj)
                    }
                }
            }

            // remove what's to remove
            objects.removeAll(toRemove)
        }
    }

    private fun tryCollapse() {
        val objectsInChildren = topLeft!!.count() + topRight!!.count() + botLeft!!.count() + botRight!!.count()
        if (objectsInChildren < capacity && parent != null) {
            divided = false
            // add every child object to the parent
            objects.addAll(topLeft!!.objects)
            objects.addAll(topRight!!.objects)
            objects.addAll(botLeft!!.objects)
            objects.addAll(botRight!!.objects)
        }
    }

    private fun addRebuildObject(obj: FhysicsObject) {
        // if the object is still fully in the boundary, or it is the root, add it to the rebuild list
        if (boundary.contains(obj) || parent == null) {
            rebuildObjects.add(obj)
        } else {
            parent.addRebuildObject(obj)
        }
    }

    private fun insertRebuildObjects() {
        // print the size of rebuildObjects if not empty
        for (obj in rebuildObjects) {
            insert(obj)
        }
        rebuildObjects.clear()
    }

    /// =====update functions=====

    fun updateObjects(updatesIntervalSeconds: Double) {
        if (divided) {
            if (parent == null) {
                updateObjectsAsync(updatesIntervalSeconds)
            } else {
                topLeft!!.updateObjects(updatesIntervalSeconds)
                topRight!!.updateObjects(updatesIntervalSeconds)
                botLeft!!.updateObjects(updatesIntervalSeconds)
                botRight!!.updateObjects(updatesIntervalSeconds)
            }
        } else {
            objects.forEach {
                it.updatePosition(updatesIntervalSeconds)
                // check if node is on the edge of the screen
                if (boundary.x == 0.0 || boundary.y == 0.0 || boundary.x + boundary.width == FhysicsCore.BORDER.width || boundary.y + boundary.height == FhysicsCore.BORDER.height) {
                    FhysicsCore.checkBorderCollision(it)
                }
            }
        }
    }

    /// =====async functions=====

    private fun rebuildChildrenAsync() {
        val tl = Thread { topLeft!!.rebuild() }
        val tr = Thread { topRight!!.rebuild() }
        val bl = Thread { botLeft!!.rebuild() }
        val br = Thread { botRight!!.rebuild() }

        tl.start()
        tr.start()
        bl.start()
        br.start()

        tl.join()
        tr.join()
        bl.join()
        br.join()
    }

    private fun updateObjectsAsync(updatesIntervalSeconds: Double) {
        val tl = Thread { topLeft!!.updateObjects(updatesIntervalSeconds) }
        val tr = Thread { topRight!!.updateObjects(updatesIntervalSeconds) }
        val bl = Thread { botLeft!!.updateObjects(updatesIntervalSeconds) }
        val br = Thread { botRight!!.updateObjects(updatesIntervalSeconds) }

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

    fun query(range: Rectangle2D): MutableList<FhysicsObject> {
        // Prepare an array of results
        val objectsInRange: MutableList<FhysicsObject> = ArrayList()

        // Automatically abort if the range does not intersect this quad
        if (!boundary.intersects(range))
            return objectsInRange // empty list

        // Check objects at this quad level
        for (i in 0 until this.objects.size) {
            if (range.intersects(this.objects[i])) {
                objectsInRange.add(this.objects[i])
            }
        }

        // Terminate here, if there are no children
        if (!divided)
            return objectsInRange

        // Otherwise, add the points from the children
        objectsInRange.addAll(topLeft!!.query(range))
        objectsInRange.addAll(topRight!!.query(range))
        objectsInRange.addAll(botLeft!!.query(range))
        objectsInRange.addAll(botRight!!.query(range))

        return objectsInRange
    }

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
                    "divided=true, \n\ttopLeft=$topLeft, \n\ttopRight=$topRight, \n\tbotLeft=$botLeft, \n\tbotRight=$botRight)"
        } else {
            "de.officeryoda.fhysics.engine.QuadTree(boundary=$boundary, capacity=$capacity, objects.size=${objects.size}, divided=false)"
        }
    }
}
