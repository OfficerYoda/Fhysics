package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.extensions.contains
import de.officeryoda.fhysics.extensions.intersects
import de.officeryoda.fhysics.objects.FhysicsObject
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer
import java.awt.geom.Rectangle2D
import kotlin.reflect.KFunction2

data class QuadTree(
    private val boundary: Rectangle2D,
    private val capacity: Int,
    private val parent: QuadTree?,
    private val pos: String,
) {
    val objects: MutableList<FhysicsObject> = ArrayList()
    lateinit var topLeft: QuadTree
    lateinit var topRight: QuadTree
    lateinit var botLeft: QuadTree
    lateinit var botRight: QuadTree
    var divided: Boolean = false

    fun insert(obj: FhysicsObject) {
        if (!boundary.intersects(obj)) return

        if (objects.contains(obj)) {
//            println("Object ${obj.id} already in $this")
            return
        }

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
        topLeft.insert(obj)
        topRight.insert(obj)
        botLeft.insert(obj)
        botRight.insert(obj)
    }

    private fun subdivide() {
        val x: Double = boundary.x
        val y: Double = boundary.y
        val hw: Double = boundary.width / 2 // half width
        val hh: Double = boundary.height / 2 // half height

        // top left
        val tl = Rectangle2D.Double(x, y + hh, hw, hh)
        topLeft = QuadTree(tl, capacity, this, "$pos/tl")
        // top right
        val tr = Rectangle2D.Double(x + hw, y + hh, hw, hh)
        topRight = QuadTree(tr, capacity, this, "$pos/tr")
        // bottom left
        val bl = Rectangle2D.Double(x, y, hw, hh)
        botLeft = QuadTree(bl, capacity, this, "$pos/bl")
        // bottom right
        val br = Rectangle2D.Double(x + hw, y, hw, hh)
        botRight = QuadTree(br, capacity, this, "$pos/br")

        objects.forEach { insertInChildren(it) }
        objects.clear()

        divided = true
    }

    fun rebuild() {
        if (divided) {
//            println("Rebuilding ${toString()}")
            topLeft.rebuild()
            topRight.rebuild()
            botLeft.rebuild()
            botRight.rebuild()
            insertRebuildObjects()
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
                    FhysicsObjectDrawer.INSTANCE.addDebugPoint(obj.position)
                }
            }

            // remove what's to remove
            objects.removeAll(toRemove)
        }
    }

    private val rebuildObjects: MutableList<FhysicsObject> = ArrayList()
    private fun addRebuildObject(obj: FhysicsObject) {
        // if the object is still fully in the boundary, or it is the root, add it to the rebuild list
        if (boundary.contains(obj) || parent == null) {
            rebuildObjects.add(obj)
        } else {
            parent.addRebuildObject(obj)
        }
    }

    private fun insertRebuildObjects() {
//        println(rebuildObjects.size)
        // print the size of rebuildObjects if not empty
        for (obj in rebuildObjects) {
            insert(obj)
        }
        rebuildObjects.clear()
    }

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
        objectsInRange.addAll(topLeft.query(range))
        objectsInRange.addAll(topRight.query(range))
        objectsInRange.addAll(botLeft.query(range))
        objectsInRange.addAll(botRight.query(range))

        return objectsInRange
    }

    fun draw(drawRect: KFunction2<Rectangle2D, Int, Unit>) {
        if (!divided) {
            drawRect(boundary, objects.size)
        } else {
            topLeft.draw(drawRect)
            topRight.draw(drawRect)
            botLeft.draw(drawRect)
            botRight.draw(drawRect)
        }
    }

    override fun toString(): String {
//        return if (divided) {
//            "de.officeryoda.fhysics.engine.QuadTree(boundary=$boundary, capacity=$capacity, objects.size=${objects.size}, " +
//                    "divided=true, \n\ttopLeft=$topLeft, \n\ttopRight=$topRight, \n\tbotLeft=$botLeft, \n\tbotRight=$botRight)"
//        } else {
//            "de.officeryoda.fhysics.engine.QuadTree(boundary=$boundary, capacity=$capacity, objects.size=${objects.size}, divided=false)"
//        }
        return "QuadTre(divided=$divided, pos=$pos)"
    }
}
