import de.officeryoda.fhysics.extensions.contains
import de.officeryoda.fhysics.objects.FhysicsObject
import java.awt.Graphics
import java.awt.Rectangle
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

        // will return immediately when one returns true
        return topLeft.insert(obj) || topRight.insert(obj) || botLeft.insert(obj) || botRight.insert(obj)
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

        divided = true
    }

    fun draw(g: Graphics, drawRect: (Graphics, Rectangle2D) -> Unit) {
        if (!divided) {
            drawRect(g, boundary)
        } else {
            topLeft.draw(g, drawRect)
            topRight.draw(g, drawRect)
            botLeft.draw(g,drawRect)
            botRight.draw(g, drawRect)
        }
    }

    override fun toString(): String {
        return if (divided) {
            "QuadTree(boundary=$boundary, capacity=$capacity, objects=$objects, " +
                    "divided=true, \n\ttopLeft=$topLeft, \n\ttopRight=$topRight, \n\tbotLeft=$botLeft, \n\tbotRight=$botRight)"
        } else {
            "QuadTree(boundary=$boundary, capacity=$capacity, objects=$objects, divided=false)"
        }
    }
}
