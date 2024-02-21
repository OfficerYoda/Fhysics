package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.collision.CollisionSolver
import de.officeryoda.fhysics.engine.collision.ElasticCollision
import de.officeryoda.fhysics.objects.Box
import de.officeryoda.fhysics.objects.FhysicsObject
import de.officeryoda.fhysics.objects.FhysicsObjectFactory
import java.awt.geom.Rectangle2D
import java.util.*

object FhysicsCore {

    // x and y must be 0.0
    val BORDER: Rectangle2D = Rectangle2D.Double(0.0, 0.0, 100.0, 100.0)
    private val COLLISION_SOLVER: CollisionSolver = ElasticCollision

    const val QUAD_TREE_CAPACITY: Int = 4
    var quadTree: QuadTree = QuadTree(BORDER, QUAD_TREE_CAPACITY)

    private var objectCount: Int = 0
    val fhysicsObjects: MutableList<FhysicsObject> = ArrayList()
    private val borderBoxes: List<Box>

    private val gravity: Vector2 = Vector2(0.0, 0.0)
//    private val gravity: Vector2 = Vector2(0.0, -9.81)

    private const val UPDATES_PER_SECOND: Int = 200

    private var updateCount = 0
    private val updateTimes = mutableListOf<Long>()

    var isRunning: Boolean = true

    init {
        borderBoxes = createBorderBoxes()

        for (i in 1..3000) {
            val circle = FhysicsObjectFactory.randomCircle()
//            circle.radius *= 2
            fhysicsObjects.add(circle)
        }

//        for (i in 1..14) {
//            val box = FhysicsObjectFactory.randomBox()
//            fhysicsObjects.add(box)
//        }

        startUpdateLoop()
    }

    private fun startUpdateLoop() {
        val updateIntervalMillis: Int = (1f / UPDATES_PER_SECOND * 1000).toInt()
        Timer(true).scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (isRunning) {
                    update()
                }
            }
        }, 0, updateIntervalMillis.toLong())
    }

    fun update() {
        val updatesIntervalSeconds: Double = 1.0 / UPDATES_PER_SECOND

//        spawnObject()

        fhysicsObjects.parallelStream().forEach {
            it.updatePosition(updatesIntervalSeconds, gravity)
            checkBorderCollision(it)
        }

        val startTime: Long = System.nanoTime()
        buildQuadTree()
        addUpdateTime(System.nanoTime() - startTime)

        checkObjectCollisionQuadTree(quadTree)

        updateCount++
    }

    private fun buildQuadTree() {
        quadTree = QuadTree(BORDER, QUAD_TREE_CAPACITY)
        fhysicsObjects.forEach { quadTree.insert(it) }
    }

    private fun spawnObject() {
        for (i in 1..5) {
            fhysicsObjects.add(FhysicsObjectFactory.randomCircle())
        }

//        if (updateCount % 5 != 0) return
//
//        val spawnRows = 2
//        val radius = 0.5
//        val pos = Vector2(2.0, BORDER.height - 2)
//        val yOffset = (objectCount % spawnRows) * 2 * radius
//        pos.y -= yOffset
//
//        val vel = Vector2(20.0, 0.0)
//        fhysicsObjects.add(FhysicsObjectFactory.customCircle(pos, radius, vel))
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

    fun nextId(): Int {
        return objectCount++
    }
}
