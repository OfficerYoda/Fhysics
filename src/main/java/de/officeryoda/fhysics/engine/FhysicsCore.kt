package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.collision.CollisionSolver
import de.officeryoda.fhysics.engine.collision.ElasticCollision
import de.officeryoda.fhysics.objects.Box
import de.officeryoda.fhysics.objects.FhysicsObject
import de.officeryoda.fhysics.objects.FhysicsObjectFactory
import java.awt.geom.Rectangle2D
import java.util.*

class FhysicsCore {

    private val quadTreeCapacity: Int = 4
    var quadTree: QuadTree = QuadTree(BORDER, quadTreeCapacity)

    val fhysicsObjects: MutableList<FhysicsObject> = ArrayList()
    private val borderBoxes: List<Box>

    private val gravity: Vector2 = Vector2(0.0, 0.0)
//    private val gravity: Vector2 = Vector2(0.0, -9.81)

    private val updatesPerSecond: Int = 200

    private var updateCount = 0
    private val updateTimes = mutableListOf<Long>()

    var isRunning: Boolean = true

    init {
        borderBoxes = createBorderBoxes()

        for (i in 1..500) {
            val circle = FhysicsObjectFactory.randomCircle()
            circle.radius *= 2
            fhysicsObjects.add(circle)
        }

        for (i in 1..14) {
            val box = FhysicsObjectFactory.randomBox()
            fhysicsObjects.add(box)
        }

//        fhysicsObjects.add(FhysicsObjectFactory.customBox(Vector2(0.0, 0.0), 100.0, 10.0, Vector2(0.0, 0.0)))
//
//        for (i in 1 until 200) {
//            val circle = FhysicsObjectFactory.customCircle(
//                Vector2(i * 5.0, 15.0 + i * 4.5 + 1),
//                0.5,
//                Vector2(0.0, 0.0)
//            )
//            fhysicsObjects.add(circle)
//        }

//        fhysicsObjects.add(FhysicsObjectFactory.customCircle(Vector2(10.0, 50.0), 1.0, Vector2(10.0, 0.0)))
//        fhysicsObjects.add(FhysicsObjectFactory.customCircle(Vector2(20.0, 50.0), 1.0, Vector2(-10.0, 0.0)))
//        fhysicsObjects.add(FhysicsObjectFactory.customCircle(Vector2(30.0, 50.0), 1.0, Vector2(10.0, 0.0)))
//        fhysicsObjects.add(FhysicsObjectFactory.customCircle(Vector2(40.0, 50.0), 1.0, Vector2(-10.0, 0.0)))
//        fhysicsObjects.add(FhysicsObjectFactory.customCircle(Vector2(50.0, 50.0), 1.0, Vector2(-10.0, 0.0)))
//        fhysicsObjects.add(FhysicsObjectFactory.customCircle(Vector2(60.0, 50.0), 1.0, Vector2(10.0, 0.0)))
//        fhysicsObjects.add(FhysicsObjectFactory.customCircle(Vector2(70.0, 50.0), 1.0, Vector2(-10.0, 0.0)))
//        fhysicsObjects.add(FhysicsObjectFactory.customCircle(Vector2(80.0, 50.0), 1.0, Vector2(-10.0, 0.0)))
//        fhysicsObjects.add(FhysicsObjectFactory.customCircle(Vector2(90.0, 50.0), 1.0, Vector2(-10.0, 0.0)))

        startUpdateLoop()
    }

    private fun startUpdateLoop() {
        val updateIntervalMillis: Int = (1f / updatesPerSecond * 1000).toInt()
        Timer(true).scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (isRunning) {
                    update()
                }
            }
        }, 0, updateIntervalMillis.toLong())
    }

    fun update() {
        val startTime: Long = System.nanoTime()
        val updatesIntervalSeconds: Double = 1.0 / updatesPerSecond

//        spawnObject()

        fhysicsObjects.forEach {
            it.updatePosition(updatesIntervalSeconds, gravity)
            checkBorderCollision(it)
        }

        buildQuadTree()
        checkObjectCollisionQuadTree(quadTree)

        updateCount++
        addUpdateTime(System.nanoTime() - startTime)
    }

    private fun buildQuadTree() {
        quadTree = QuadTree(BORDER, quadTreeCapacity)
        fhysicsObjects.forEach { quadTree.insert(it) }
    }

    private fun spawnObject() {
        if (updateCount % 5 != 0) return

        val spawnRows = 2
        val radius = 0.5
        val pos = Vector2(2.0, BORDER.height - 2)
        val yOffset = (objectCount % spawnRows) * 2 * radius
        pos.y -= yOffset

        val vel = Vector2(20.0, 0.0)
        fhysicsObjects.add(FhysicsObjectFactory.customCircle(pos, radius, vel))
    }

    private fun checkObjectCollisionQuadTree(quadTree: QuadTree) {
        if (!quadTree.divided) {
            val objects = quadTree.objects
            val numObjects = objects.size

            for (i in 0 until numObjects - 1) {
                val objA = objects[i]

                for (j in i + 1 until numObjects) {
                    val objB = objects[j]
                    handleCollision(objA, objB)
                }
            }
        } else {
            checkObjectCollisionQuadTree(quadTree.topLeft)
            checkObjectCollisionQuadTree(quadTree.topRight)
            checkObjectCollisionQuadTree(quadTree.botLeft)
            checkObjectCollisionQuadTree(quadTree.botRight)
        }
    }

    private fun handleCollision(objA: FhysicsObject, objB: FhysicsObject) {
        val points: CollisionInfo = objA.testCollision(objB)

        if (points.overlap == -1.0) return

        COLLISION_SOLVER.solveCollision(points)
    }

    private fun checkBorderCollision(obj: FhysicsObject) {
        borderBoxes.forEach { handleCollision(obj, it) }
    }

    private fun createBorderBoxes(): List<Box> {
        val width: Double = BORDER.width
        val height: Double = BORDER.height

        val left: Box = FhysicsObjectFactory.customBox(Vector2(-width, 0.0), width, height, Vector2.ZERO)
        val right: Box = FhysicsObjectFactory.customBox(Vector2(width, 0.0), width, height, Vector2.ZERO)
        val top: Box = FhysicsObjectFactory.customBox(Vector2(-width, height), 3 * width, height, Vector2.ZERO)
        val bottom: Box = FhysicsObjectFactory.customBox(Vector2(-width, -height), 3 * width, height, Vector2.ZERO)

        return listOf(left, right, top, bottom)
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
            .map { it / 1E6 } // convert nano to milliseconds
            .average()
    }

    companion object {
        // x and y must be 0.0
        val BORDER: Rectangle2D = Rectangle2D.Double(0.0, 0.0, 100.0, 100.0)

        val COLLISION_SOLVER: CollisionSolver = ElasticCollision

        private var objectCount: Int = 0
        fun nextId(): Int {
            return objectCount++
        }
    }
}
