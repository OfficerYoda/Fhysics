package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.objects.Circle
import de.officeryoda.fhysics.objects.FhysicsObjectFactory
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer
import java.util.*
import java.util.function.Consumer

class FhysicsCore {
    val fhysicsObjects: MutableList<Circle> = ArrayList()
    private val gravity = Vector2(0.0, -9.81)
    private val updatesPerSecond = 500

    var drawer: FhysicsObjectDrawer? = null

    init {
        for (i in 0..49) {
            fhysicsObjects.add(FhysicsObjectFactory.randomCircle())
        }
    }

    fun startUpdateLoop() {
        val updateTimer = Timer(true)
        val updateIntervalMillis = (1f / updatesPerSecond * 1000).toInt()
        updateTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                update()
            }
        }, 0, updateIntervalMillis.toLong())
    }

    private fun update() {
        val updatesIntervalSeconds = 1.0 / updatesPerSecond
        fhysicsObjects.forEach(Consumer<Circle> { obj: Circle ->
            obj.applyGravity(updatesIntervalSeconds, Vector2.zero())
//                        obj.applyGravity(updatesIntervalSeconds, gravity)
            checkBorderCollision(obj)
            checkObjectCollision(obj)
        })
        drawer!!.repaintObjects()
    }

    private fun checkObjectCollision(obj1: Circle) {
        fhysicsObjects.stream().forEach(Consumer { obj2: Circle ->
            if (obj1.id == obj2.id) return@Consumer
            CollisionHandler.handleElasticCollision(obj1, obj2)
        })
    }

    private fun checkBorderCollision(circle: Circle) {
        val pos = circle.position
        val velocity = circle.velocity
        val radius = circle.radius

        // check top/bottom border
        if (pos.y + radius > BORDER.topBorder) {
            velocity.y = -velocity.y
//            velocity.set(Vector2.zero())
            pos.y = BORDER.topBorder - radius
//            velocity.x = Math.random() * 400 - 200
        } else if (pos.y - radius < BORDER.bottomBorder) {
            velocity.y = -velocity.y
            pos.y = BORDER.bottomBorder + radius
//                        velocity.x = Math.random() * 400 - 200
        }

        // check left/right border
        if (pos.x - radius < BORDER.leftBorder) {
            velocity.x = -velocity.x
            pos.x = BORDER.leftBorder + radius
        } else if (pos.x + radius > BORDER.rightBorder) {
            velocity.x = -velocity.x
            pos.x = BORDER.rightBorder - radius
        }
    }

    companion object {
        val BORDER: Border = Border(0.0, 600.0, 0.0, 400.0)
        private var objectCount = 0
        fun nextId(): Int {
            return objectCount++
        }
    }
}
