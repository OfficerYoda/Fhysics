package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.objects.Circle
import de.officeryoda.fhysics.rendering.RenderUtil.drawer
import de.officeryoda.fhysics.rendering.RenderUtil.zoomCenter
import javafx.scene.input.*
import kotlin.math.exp
import kotlin.math.sign

object SceneListener {

    /**
     * The target zoom factor of the drawer (used for smooth zooming)
     */
    private var targetZoom: Double
        get() = drawer.targetZoom
        set(value) {
            drawer.targetZoom = value
        }

    /**
     * The target zoom center of the drawer (used for smooth zooming)
     */
    private var targetZoomCenter: Vector2
        get() = drawer.targetZoomCenter
        set(value) {
            drawer.targetZoomCenter = value
        }

    /**
     * The right mouse button pressed state
     */
    private var rightPressed: Boolean
        get() = drawer.rightPressed
        set(value) {
            drawer.rightPressed = value
        }

    /**
     * The position where the right mouse button was pressed
     */
    private var rightPressedPos: Vector2
        get() = drawer.rightPressedPos
        set(value) {
            drawer.rightPressedPos = value
        }

    /**
     * Handles mouse pressed events
     *
     * @param e the mouse event
     */
    fun onMousePressed(e: MouseEvent) {
        when (e.button) {
            MouseButton.PRIMARY -> {
                when (UIController.spawnObjectType) {
                    SpawnObjectType.CIRCLE -> spawnCircle(e)
                    SpawnObjectType.RECTANGLE -> spawnRectangle(e)
                    SpawnObjectType.TRIANGLE -> spawnTriangle(e)
                }
            }

            MouseButton.SECONDARY -> {
                rightPressed = true
                rightPressedPos = RenderUtil.screenToWorld(Vector2(e.x.toFloat(), e.y.toFloat()))
            }

            else -> {}
        }
    }

    /**
     * Spawns a circle at the mouse position
     *
     * @param e the mouse event
     * @return the spawned circle
     */
    private fun spawnCircle(e: MouseEvent) {
        if(UIController.spawnRadius <= 0.0F) return
        val transformedMousePos: Vector2 = RenderUtil.screenToWorld(Vector2(e.x.toFloat(), e.y.toFloat()))
        FhysicsCore.spawn(Circle(transformedMousePos, UIController.spawnRadius))
    }

    /**
     * Spawns a rectangle using the mouse position
     *
     * @param e the mouse event
     * @return the spawned rectangle
     */
    private fun spawnRectangle(e: MouseEvent) {
        TODO("Not yet implemented")
    }

    /**
     * Spawns a triangle using the mouse position
     *
     * @param e the mouse event
     * @return the spawned triangle
     */
    private fun spawnTriangle(e: MouseEvent) {
        TODO("Not yet implemented")
    }

    /**
     * Handles mouse released events
     *
     * @param e the mouse event
     */
    fun onMouseReleased(e: MouseEvent) {
        if (e.button == MouseButton.SECONDARY) {
            rightPressed = false
        }
    }

    /**
     * Handles mouse wheel events
     *
     * @param e the scroll event
     */
    fun onMouseWheel(e: ScrollEvent) {
        // don't zoom if moving with the mouse
        if (rightPressed) return

        val zoomBefore: Double = RenderUtil.zoom
        val deltaZoom: Double = exp(targetZoom * 0.035)

        // Record the mouse position before zooming
        var mousePosBeforeZoom = Vector2(e.x.toFloat(), e.y.toFloat())
        mousePosBeforeZoom = RenderUtil.screenToWorld(mousePosBeforeZoom)

        // Adjust the zoom amount
        RenderUtil.zoom += deltaZoom * e.deltaY.sign
        RenderUtil.zoom = RenderUtil.zoom.coerceIn(2.0, 120.0)

        // Record the mouse position after zooming
        var mousePosAfterZoom = Vector2(e.x.toFloat(), e.y.toFloat())
        mousePosAfterZoom = RenderUtil.screenToWorld(mousePosAfterZoom)

        // Calculate the difference in mouse position caused by zooming
        val deltaMousePos: Vector2 = mousePosBeforeZoom - mousePosAfterZoom

        // Update the zoom center to keep the mouse at the same world position
        targetZoomCenter = zoomCenter + deltaMousePos

        // Update the target zoom
        targetZoom = RenderUtil.zoom
        RenderUtil.zoom = zoomBefore
        drawer.lerpCounter = 0
    }

    /**
     * Handles mouse dragged events
     *
     * @param e the mouse event
     */
    fun onMouseDragged(e: MouseEvent) {
        if (rightPressed) {
            val mousePos: Vector2 = RenderUtil.screenToWorld(Vector2(e.x.toFloat(), e.y.toFloat()))
            val deltaMousePos: Vector2 = rightPressedPos - mousePos
            targetZoomCenter = targetZoomCenter + deltaMousePos
            zoomCenter = targetZoomCenter
        }
    }

    /**
     * Handles key pressed events
     *
     * @param event the key event
     */
    fun onKeyPressed(event: KeyEvent) {
        // if pressed char is p toggle isRunning in FhysicsCore
        // if it is Enter or space call the update function
        when (event.code) {
            KeyCode.P -> FhysicsCore.running = !FhysicsCore.running
            KeyCode.SPACE -> FhysicsCore.update()
            KeyCode.ENTER -> FhysicsCore.update()
            KeyCode.Z -> drawer.resetZoom()
            else -> {}
        }
    }
}