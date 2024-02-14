package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.engine.collisionhandler.CollisionHandler
import de.officeryoda.fhysics.engine.collisionhandler.ElasticCollision
import de.officeryoda.fhysics.objects.Circle
import de.officeryoda.fhysics.objects.FhysicsObject
import de.officeryoda.fhysics.objects.FhysicsObjectFactory
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer
import java.awt.geom.Rectangle2D
import java.util.*

class FhysicsCore {

    private val quadTreeCapacity: Int = 4

    val fhysicsObjects: MutableList<FhysicsObject> = ArrayList()
    var quadTree: QuadTree = QuadTree(BORDER, quadTreeCapacity)

    private val gravity: Vector2 = Vector2(0.0, -9.81)
    private val updatesPerSecond: Int = 240

    private var updateCount = 0
    private val updateTimes = mutableListOf<Long>()

    var drawer: FhysicsObjectDrawer? = null

    init {
        for (i in 1..400) {
            val circle = FhysicsObjectFactory.randomCircle()
            circle.radius *= 4
            fhysicsObjects.add(circle)
        }
//
//        fhysicsObjects.add(Box(Vector2(10.0, 10.0), 10.0, 10.0))
//        fhysicsObjects.add(Box(Vector2(40.0, 30.0), 2.5, 10.0))
//        fhysicsObjects.add(Box(Vector2(20.0, 50.0), 4.0, 12.0))

//        fhysicsObjects.add(Box(Vector2(80.0, 30.0), 10.0, 40.0))
//        val circle = Circle(Vector2(40.0, 50.0), 3.0)
//        circle.velocity += Vector2(30.0, 0.0)
//        fhysicsObjects.add(circle)
    }

    fun startUpdateLoop() {
        val updateIntervalMillis: Int = (1f / updatesPerSecond * 1000).toInt()
        Timer(true).scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                update()
            }
        }, 0, updateIntervalMillis.toLong())
    }

    private fun update() {
        val startTime: Long = System.nanoTime()
        val updatesIntervalSeconds: Double = 1.0 / updatesPerSecond

//        spawnObject()

        fhysicsObjects.forEach {
            it.update(updatesIntervalSeconds, Vector2.ZERO)
//            obj.update(updatesIntervalSeconds, gravity)
            if (it is Circle)
                checkBorderCollision(it)
        }
        buildQuadTree()
        checkObjectCollisionQuadTree(quadTree)
        drawer!!.repaintObjects()

        updateCount++

        addUpdateTime(System.nanoTime() - startTime)
    }

    private fun buildQuadTree() {
        QuadTree.count = 0
        quadTree = QuadTree(BORDER, quadTreeCapacity)
        fhysicsObjects.forEach { quadTree.insert(it) }
    }

    private fun spawnObject() {
        if (updateCount % 5 != 0) return

        val spawnRows = 2
        val radius = 0.2
        val pos = Vector2(2.0, BORDER.height - 2)
        val yOffset = (objectCount % spawnRows) * 2 * radius
        pos.y -= yOffset

        val vel = Vector2(20.0, 0.0)
        fhysicsObjects.add(FhysicsObjectFactory.customCircle(pos, radius, vel))
    }

    private fun checkObjectCollisionQuadTree(quadTree: QuadTree) {
        if (!quadTree.divided) {
            val objects = quadTree.objects
            for (obj1 in objects) {
                for (obj2 in objects) {
                    if (obj1.id == obj2.id) continue
                    obj1.handleCollision(obj2)
                }
            }
        } else {
            checkObjectCollisionQuadTree(quadTree.topLeft)
            checkObjectCollisionQuadTree(quadTree.topRight)
            checkObjectCollisionQuadTree(quadTree.botLeft)
            checkObjectCollisionQuadTree(quadTree.botRight)
        }
    }

    private fun checkBorderCollision(circle: Circle) {
        val pos: Vector2 = circle.position
        val velocity: Vector2 = circle.velocity
        val radius: Double = circle.radius

        // check top/bottom border
        if (pos.y + radius > BORDER.height) {
//            velocity.y = 0.0
            velocity.y = -velocity.y
            pos.y = BORDER.height - radius
        } else if (pos.y - radius < 0) {
//            velocity.y = 0.0
            velocity.y = -velocity.y
            pos.y = radius
        }

        // check left/right border
        if (pos.x - radius < 0) {
//            velocity.x = 0.0
            velocity.x = -velocity.x
            pos.x = radius
        } else if (pos.x + radius > BORDER.width) {
//            velocity.x = 0.0
            velocity.x = -velocity.x
            pos.x = BORDER.width - radius
        }
    }

    private fun addUpdateTime(updateTime: Long) {
        val updatesToAverage = 50

        updateTimes.add(updateTime)

        if (updateTimes.size > updatesToAverage) {
            updateTimes.removeAt(0) // Remove the oldest time if more than specified updates have been recorded
        }
    }

    fun getAverageUpdateTime(): Double {
        return updateTimes
            .toList()
            .filterNotNull() // filterNotNull is not redundant, even if IntelliJ says so
            .map { it / 1E6 } // convert nano to milliseconds
            .average()
    }

    companion object {
        // x and y must be 0.0
        val BORDER: Rectangle2D = Rectangle2D.Double(0.0, 0.0, 100.0, 100.0)

        val COLLISION_HANDLER: CollisionHandler = ElasticCollision

        private var objectCount: Int = 0
        fun nextId(): Int {
            return objectCount++
        }
    }
}
