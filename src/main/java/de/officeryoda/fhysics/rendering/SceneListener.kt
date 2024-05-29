package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.QuadTree
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.extensions.contains
import de.officeryoda.fhysics.objects.Circle
import de.officeryoda.fhysics.objects.Rectangle
import de.officeryoda.fhysics.rendering.RenderUtil.drawer
import de.officeryoda.fhysics.rendering.RenderUtil.zoomCenter
import javafx.scene.input.*
import kotlin.math.exp
import kotlin.math.sign

object SceneListener {

    /**
     * The position of the mouse in world space
     */
    val mouseWorldPos: Vector2 = Vector2.ZERO

    /**
     * The position of the mouse when the right mouse button was pressed
     */
    private var rightPressed: Boolean = false

    /**
     * The position of the mouse when the right mouse button was pressed
     */
    private var rightPressedPos: Vector2 = Vector2.ZERO

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
     * Handles mouse pressed events
     *
     * @param e the mouse event
     */
    fun onMousePressed(e: MouseEvent) {
        when (e.button) {
            MouseButton.PRIMARY -> {
                // If the mouse is not hovering over an object remove that object
                if (drawer.hoveredObject != null) {
                    QuadTree.removeQueue.add(drawer.hoveredObject!!)
                } else { // Else spawn a new object
                    // Check if spawn pos is outside the border
                    val transformedMousePos: Vector2 = RenderUtil.screenToWorld(Vector2(e.x.toFloat(), e.y.toFloat()))
                    if (!FhysicsCore.BORDER.contains(transformedMousePos)) return

                    when (UIController.spawnObjectType) {
                        SpawnObjectType.CIRCLE -> spawnCircle(transformedMousePos)
                        SpawnObjectType.RECTANGLE -> spawnRectangle(transformedMousePos)
                        SpawnObjectType.TRIANGLE -> spawnTriangle(transformedMousePos)
                    }
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
     * @param position the world spawn position
     * @return the spawned circle
     */
    private fun spawnCircle(position: Vector2) {
        if (UIController.spawnRadius <= 0.0F) return
        FhysicsCore.spawn(Circle(position, UIController.spawnRadius))
    }

    /**
     * Spawns a rectangle using the mouse position
     *
     * @param position the world spawn position
     * @return the spawned rectangle
     */
    private fun spawnRectangle(position: Vector2) {
        if (UIController.spawnWidth <= 0.0F || UIController.spawnHeight <= 0.0F) return
        FhysicsCore.spawn(Rectangle(position, UIController.spawnWidth, UIController.spawnHeight))
    }

    /**
     * Spawns a triangle using the mouse position
     *
     * @param position the world spawn position
     * @return the spawned triangle
     */
    private fun spawnTriangle(position: Vector2) {
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
        // Don't zoom if moving with the mouse
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
    }

    /**
     * Handles mouse moved events
     *
     * @param e the mouse event
     */
    fun onMouseMoved(e: MouseEvent) {
        // Update the mouse position
        mouseWorldPos.x = RenderUtil.screenToWorldX(e.x.toFloat()).toFloat()
        mouseWorldPos.y = RenderUtil.screenToWorldY(e.y.toFloat()).toFloat()
    }

    /**
     * Handles mouse dragged events
     *
     * @param e the mouse event
     */
    fun onMouseDragged(e: MouseEvent) {
        // to update the mouse position
        onMouseMoved(e)

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
        when (event.code) {
            KeyCode.P -> FhysicsCore.running = !FhysicsCore.running
            KeyCode.SPACE -> FhysicsCore.update()
            KeyCode.ENTER -> FhysicsCore.update()
            KeyCode.Z -> drawer.resetZoom()
            KeyCode.J -> QuadTree.capacity -= 5
            KeyCode.K -> QuadTree.capacity += 5
            KeyCode.G -> MapVisualization(FhysicsCore.qtCapacity)
            KeyCode.Q -> println(QuadTree.root)
            else -> {}
        }
    }
}