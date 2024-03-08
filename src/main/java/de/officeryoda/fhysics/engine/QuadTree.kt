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

    var divided: Boolean = false
        private set

    var topLeft: QuadTree? = null
        private set
    var topRight: QuadTree? = null
        private set
    var botLeft: QuadTree? = null
        private set
    var botRight: QuadTree? = null
        private set

    private val rebuildObjects: HashSet<FhysicsObject> = HashSet()

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

    fun subdivide() {
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
            if (parent == null) {
                // Root node
                updateObjectsAndRebuildChildrenAsync()
            } else {
                // Subdivide and update children
                topLeft!!.updateObjectsAndRebuild()
                topRight!!.updateObjectsAndRebuild()
                botLeft!!.updateObjectsAndRebuild()
                botRight!!.updateObjectsAndRebuild()
            }

            insertRebuildObjects()
            tryCollapse()
        } else {
            // Check if the node is on the border
            val borderNode: Boolean =
                boundary.x == 0.0 || boundary.y == 0.0 || boundary.x + boundary.width == FhysicsCore.BORDER.width || boundary.y + boundary.height == FhysicsCore.BORDER.height

            // Check if the objects are still fully in the boundary
            val toRemove = ArrayList<FhysicsObject>()

            if (parent == null && objects.size > capacity) {
                // Root node handling, because there is no parent to add the rebuild objects to
                rebuildObjects.addAll(objects)
                toRemove.addAll(objects)
                objects.forEach { updateObject(it, true) }
                objects.clear()
                subdivide()
                insertRebuildObjects()
            } else {
                // Non-root node handling
                for (obj: FhysicsObject in objects) {
                    // Physics update
                    updateObject(obj, borderNode)

                    // Keep only if fully contained in the boundary
                    if (!boundary.contains(obj)) {
                        // Add object to the rebuild list if not fully in the boundary
                        parent?.addRebuildObject(obj)
                        toRemove.add(obj)
                    }
                }
            }

            // Remove objects that need to be removed
            objects.removeAll(toRemove)
        }
    }

    private fun tryCollapse() {
        if (!divided) return

        val objectsInChildren: Int = topLeft!!.count() + topRight!!.count() + botLeft!!.count() + botRight!!.count()
        if (objectsInChildren < capacity) {
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
        for (obj: FhysicsObject in rebuildObjects) {
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

    companion object {
        var capacity: Int = 32
            set(value) {
                // I experienced crashes with values below 3
                field = value.coerceAtLeast(3)
            }
    }
}
