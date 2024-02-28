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
        val x: Float = boundary.x.toFloat()
        val y: Float = boundary.y.toFloat()
        val hw: Float = boundary.width.toFloat() / 2 // half width
        val hh: Float = boundary.height.toFloat() / 2 // half height

        // top left
        val tl = Rectangle2D.Float(x, y + hh, hw, hh)
        topLeft = QuadTree(tl, capacity, this)
        // top right
        val tr = Rectangle2D.Float(x + hw, y + hh, hw, hh)
        topRight = QuadTree(tr, capacity, this)
        // bottom left
        val bl = Rectangle2D.Float(x, y, hw, hh)
        botLeft = QuadTree(bl, capacity, this)
        // bottom right
        val br = Rectangle2D.Float(x + hw, y, hw, hh)
        botRight = QuadTree(br, capacity, this)

        objects.forEach { insertInChildren(it) }
        objects.clear()

        divided = true
    }

    /// =====rebuild functions=====

    fun updateObjectsAndRebuild() {
        if (divided) {
            if (parent == null) {
                updateObjectsAndRebuildChildrenAsync()
            } else {
                topLeft!!.updateObjectsAndRebuild()
                topRight!!.updateObjectsAndRebuild()
                botLeft!!.updateObjectsAndRebuild()
                botRight!!.updateObjectsAndRebuild()
            }

            insertRebuildObjects()
            tryCollapse()
        } else {

            // check if the node is on the border
            val borderNode: Boolean =
                boundary.x == 0.0 || boundary.y == 0.0 || boundary.x + boundary.width == FhysicsCore.BORDER.width || boundary.y + boundary.height == FhysicsCore.BORDER.height

            // check if the objects are still fully in the boundary
            val toRemove = ArrayList<FhysicsObject>()
            for (obj in objects) {
                // do the physics update
                updateObject(obj, borderNode)

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
            // only need to execute it async if it is the root
            if (parent == null) {
                synchronized(rebuildObjects) {
                    rebuildObjects.add(obj)
                }
            } else {
                rebuildObjects.add(obj)
            }
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

    private fun updateObject(it: FhysicsObject, checkBorder: Boolean) {
        it.updatePosition()
        // check if node is on the edge of the screen
        if (checkBorder) {
            FhysicsCore.checkBorderCollision(it)
        }
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
                    "divided=true, \n\ttopLeft=$topLeft, \n\ttopRight=$topRight, \n\tbotLeft=$botLeft, \n\tbotRight=$botRight)"
        } else {
            "de.officeryoda.fhysics.engine.QuadTree(boundary=$boundary, capacity=$capacity, objects.size=${objects.size}, divided=false)"
        }
    }
}
