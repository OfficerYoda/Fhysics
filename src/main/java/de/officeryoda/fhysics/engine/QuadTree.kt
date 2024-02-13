package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.extensions.contains
import de.officeryoda.fhysics.objects.FhysicsObject
import java.awt.Graphics
import java.awt.geom.Rectangle2D

data class QuadTree(
    private val boundary: Rectangle2D,
    private val capacity: Int
) {
    private val objects: MutableList<FhysicsObject> = ArrayList()
    private lateinit var topLeft: QuadTree
    private lateinit var topRight: QuadTree
    private lateinit var botLeft: QuadTree
    private lateinit var botRight: QuadTree
    private var divided: Boolean = false

    fun insert(obj: FhysicsObject): Boolean {
        if (!boundary.contains(obj.position)) {
            return false
        }

        if (objects.size < capacity) {
            objects.add(obj)
            return true
        }

        if (!divided) {
            subdivide()
        }

        return insertInChildren(obj)
    }

    // probably should have chosen another name
    private fun insertInChildren(obj: FhysicsObject): Boolean {
        if (topLeft.insert(obj)) return true
        if (topRight.insert(obj)) return true
        if (botLeft.insert(obj)) return true
        if (botRight.insert(obj)) return true

        // point couldn't be added (this should never happen)
        System.err.println("Point couldn't be added to de.officeryoda.fhysics.engine.QuadTree.")
        return false
    }

    private fun subdivide() {
        val x: Double = boundary.x
        val y: Double = boundary.y
        val hw: Double = boundary.width / 2 // half width
        val hh: Double = boundary.height / 2 // half height

        // top left
        val tl = Rectangle2D.Double(x, y + hh, hw, hh)
        topLeft = QuadTree(tl, capacity)
        // top right
        val tr = Rectangle2D.Double(x + hw, y + hh, hw, hh)
        topRight = QuadTree(tr, capacity)
        // bottom left
        val bl = Rectangle2D.Double(x, y, hw, hh)
        botLeft = QuadTree(bl, capacity)
        // bottom right
        val br = Rectangle2D.Double(x + hw, y, hw, hh)
        botRight = QuadTree(br, capacity)

        objects.forEach { insertInChildren(it) }
        objects.clear()

        divided = true
    }

    fun query(range: Rectangle2D): MutableList<FhysicsObject> {
        // Prepare an array of results
        val objectsInRange: MutableList<FhysicsObject> = ArrayList()

        // Automatically abort if the range does not intersect this quad
        if (!boundary.intersects(range))
            return objectsInRange // empty list

        // Check objects at this quad level
        count += objects.size
        for (i in 0 until objects.size) {
            if (range.contains(objects[i].position)) {
                objectsInRange.add(objects[i])
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

    fun draw(g: Graphics, drawRect: (Graphics, Rectangle2D) -> Unit) {
        if (!divided) {
            drawRect(g, boundary)
        } else {
            topLeft.draw(g, drawRect)
            topRight.draw(g, drawRect)
            botLeft.draw(g, drawRect)
            botRight.draw(g, drawRect)
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
        var count = 0
    }
}
