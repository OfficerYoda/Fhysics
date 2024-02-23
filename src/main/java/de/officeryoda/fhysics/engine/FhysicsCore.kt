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


    /// =====constants=====
    // x and y must be 0.0
    val BORDER: Rectangle2D = Rectangle2D.Double(0.0, 0.0, 250.0, 250.0)
    private val COLLISION_SOLVER: CollisionSolver = ElasticCollision
    const val QUAD_TREE_CAPACITY: Int = 20
    private const val UPDATES_PER_SECOND: Int = 200

    val GRAVITY: Vector2 = Vector2(0.0, 0.0)
//    val GRAVITY: Vector2 = Vector2(0.0, -9.81)

    /// =====variables=====
    var quadTree: QuadTree = QuadTree(BORDER, QUAD_TREE_CAPACITY, null)

    var objectCount: Int = 0
    val fhysicsObjects: MutableList<FhysicsObject> = ArrayList()
    private val borderBoxes: List<Box>

    private var updateCount = 0
    private val updateDurations: MutableList<Long> = mutableListOf()

    var isRunning: Boolean = true

    init {
        borderBoxes = createBorderBoxes()

        for (i in 1..3000) {
            val circle = FhysicsObjectFactory.randomCircle()
//            circle.radius *= 2
            spawn(circle)
        }

//        for (i in 1..14) {
//            val box = FhysicsObjectFactory.randomBox()
//            fhysicsObjects.add(box)
//        }

        // create four spheres in the top left quadrant which move to the right
//        val pos = Vector2(2.0, BORDER.height - 18)
//        val vel = Vector2(20.0, 0.0)
//        val radius = 0.5
//        for (i in 1..10) {
//            val yOffset = (i - 1) * 10 * radius
//            val xOffset = (i - 1) * 10 * radius
//            val circle = FhysicsObjectFactory.customCircle(Vector2(pos.x + xOffset, pos.y - yOffset), radius, vel)
//            spawn(circle)
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
        val startTime: Long = System.nanoTime()
        val updateIntervalSeconds: Double = 1.0 / UPDATES_PER_SECOND

        spawnObject()

        quadTree.updateObjects(updateIntervalSeconds)

        buildQuadTree()

        checkObjectCollision(quadTree)

        updateCount++
        addUpdateDuration(System.nanoTime() - startTime)
    }

    private fun buildQuadTree() {
//        quadTree = QuadTree(BORDER, QUAD_TREE_CAPACITY, null)
//        fhysicsObjects.forEach { quadTree.insert(it) }
        quadTree.rebuild()
    }

    // This method must be called when trying to spawn an object
    fun spawn(obj: FhysicsObject) {
        fhysicsObjects.add(obj)
        quadTree.insert(obj)
    }

    private fun spawnObject() {
        for (i in 1..100) {
            if (objectCount < 60000) {
                spawn(FhysicsObjectFactory.randomCircle())
            }
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

    private fun checkObjectCollision(quadTree: QuadTree) {
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
            checkObjectCollision(quadTree.topLeft!!)
            checkObjectCollision(quadTree.topRight!!)
            checkObjectCollision(quadTree.botLeft!!)
            checkObjectCollision(quadTree.botRight!!)
        }
    }

    private fun handleCollision(objA: FhysicsObject, objB: FhysicsObject) {
        val points: CollisionInfo = objA.testCollision(objB)

        if (points.overlap == -1.0) return

        COLLISION_SOLVER.solveCollision(points)
    }

    fun checkBorderCollision(obj: FhysicsObject) {
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

    private fun addUpdateDuration(updateTime: Long) {
        val updatesToAverage = 100

        updateDurations.add(updateTime)

        if (updateDurations.size > updatesToAverage) {
            updateDurations.removeAt(0) // Remove the oldest time if more than specified updates have been recorded
        }
    }

    fun getAverageUpdateDuration(): Double {
        return updateDurations
            .toList()
            .filterNotNull() // this is not redundant even if IntelliJ says so
            .map { it / 1E6 } // convert nano to milliseconds
            .average()
    }

    fun nextId(): Int {
        return objectCount++
    }
}
