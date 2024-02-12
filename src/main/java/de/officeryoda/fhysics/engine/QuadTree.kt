import de.officeryoda.fhysics.extensions.contains
import de.officeryoda.fhysics.objects.FhysicsObject
import java.awt.Rectangle

data class QuadTree(
    private val boundary: Rectangle,
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
        val x = boundary.x
        val y = boundary.y
        val hw = boundary.width / 2 // half width
        val hh = boundary.height / 2 // half height

        // top left
        val tl = Rectangle(x, y + hh, hw, hh)
        topLeft = QuadTree(tl, capacity)
        // top right
        val tr = Rectangle(x + hw, y + hh, hw, hh)
        topRight = QuadTree(tr, capacity)
        // bottom left
        val bl = Rectangle(x, y, hw, hh)
        botLeft = QuadTree(bl, capacity)
        // bottom right
        val br = Rectangle(x + hw, y, hw, hh)
        botRight = QuadTree(br, capacity)

        divided = true
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
