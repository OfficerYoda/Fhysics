package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.QuadTree
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.engine.objects.Rectangle
import de.officeryoda.fhysics.rendering.RenderUtil.drawer
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

object BetterSceneListener {

    /// =====Vanilla event handler fields=====
    /**
     * The minimum distance the mouse has to be moved in screen space to be registered as a drag
     */
    private const val MIN_DRAG_DISTANCE_SQR: Float = 4f // 2px

    /**
     * The position of the mouse in world space
     */
    private val mousePosScreen: Vector2 = Vector2.ZERO
    val mousePosWorld: Vector2 = Vector2.ZERO

    // The state of the mouse buttons
    private var leftPressed: Boolean = false
    private var rightPressed: Boolean = false

    // The position where the left mouse button was pressed (in world space)
    private val leftPressedPosScreen: Vector2 = Vector2.ZERO
    private val leftPressedPosWorld: Vector2 = Vector2.ZERO
    private val rightPressedPosScreen: Vector2 = Vector2.ZERO
    private val rightPressedPosWorld: Vector2 = Vector2.ZERO

    /// =====Custom event handler fields=====
    // The state of the mouse dragging
    private var leftDragging: Boolean = false
    private var rightDragging: Boolean = false

    /**
     * The preview object to spawn
     */
    var spawnPreview: FhysicsObject? = null

    /// region =====Custom event handlers=====
    // TODO: Add parameter: mouse pos, etc.
    private fun onLeftClick(posWorld: Vector2) {
        println("Left click")
        spawnPreview()
    }

    private fun onRightClick(posWorld: Vector2) {
        println("Right click")
    }

    private fun onLeftDrag() {
        leftDragging = true
        println("Left drag")

        // Don't create a drag preview if the spawn object type is not a rectangle
        if (UIController.spawnObjectType != SpawnObjectType.RECTANGLE) return

        // Calculate the corners of the rectangle
        val minX: Float = min(leftPressedPosWorld.x, mousePosWorld.x)
        val maxX: Float = max(leftPressedPosWorld.x, mousePosWorld.x)
        val minY: Float = min(leftPressedPosWorld.y, mousePosWorld.y)
        val maxY: Float = max(leftPressedPosWorld.y, mousePosWorld.y)

        // Create the preview rectangle
        val size = Vector2(maxX - minX, maxY - minY)
        val pos: Vector2 = Vector2(minX, minY) + size / 2f

        val rect = Rectangle(pos, size.x, size.y)
        rect.color = Color(rect.color.red, rect.color.green, rect.color.blue, 128)

        spawnPreview = rect
    }

    private fun onLeftDragRelease() {
        leftDragging = false

        spawnPreview()
        UIController.instance.updateSpawnPreview()
    }

    private fun onRightDrag() {
        rightDragging = true
        println("Right drag")

        // Move the camera by the amount the mouse is away from the position where the right mouse button was pressed
        val deltaMousePos: Vector2 = rightPressedPosWorld - mousePosWorld
        drawer.targetZoomCenter = drawer.targetZoomCenter + deltaMousePos
        drawer.zoomCenter = drawer.targetZoomCenter
    }

    private fun onRightDragRelease() {
        rightDragging = false
    }

    /// endregion

    /**
     * Spawns the preview object at the mouse position
     */
    private fun spawnPreview() {
        // Check if spawn pos is outside the border
        if (!FhysicsCore.BORDER.contains(mousePosWorld)) return

        val validParams: Boolean = when (UIController.spawnObjectType) {
            SpawnObjectType.CIRCLE -> UIController.spawnRadius > 0.0F
            SpawnObjectType.RECTANGLE -> UIController.spawnWidth > 0.0F && UIController.spawnHeight > 0.0F
            else -> false
        }

        if (!validParams) return

        FhysicsCore.spawn(spawnPreview!!.clone())
    }

    /// region =====Vanilla event handlers=====
    fun onMouseWheel(e: ScrollEvent) {
//        TODO("Not yet implemented")
    }

    fun onMousePressed(e: MouseEvent) {
        // Set the pressed position if the specific button was pressed
        if (!leftPressed && e.isPrimaryButtonDown) {
            leftPressedPosScreen.set(getMouseScreenPos(e))
            leftPressedPosWorld.set(mousePosWorld.copy())
        }
        if (!rightPressed && e.isSecondaryButtonDown) {
            rightPressedPosScreen.set(getMouseScreenPos(e))
            rightPressedPosWorld.set(mousePosWorld.copy())
        }

        updateMouseButtonState(e)
    }

    fun onMouseReleased(e: MouseEvent) {
        if (leftPressed && !e.isPrimaryButtonDown) {
            // If the mouse wasn't moved too much, it's a click
            if ((mousePosScreen - leftPressedPosScreen).sqrMagnitude() <= MIN_DRAG_DISTANCE_SQR) {
                onLeftClick(mousePosWorld)
            } else {
                onLeftDragRelease()
            }
        }
        if (rightPressed && !e.isSecondaryButtonDown) {
            // If the mouse wasn't moved too much, it's a click
            if ((mousePosScreen - rightPressedPosScreen).sqrMagnitude() <= MIN_DRAG_DISTANCE_SQR) {
                onRightClick(mousePosWorld)
            } else {
                onRightDragRelease()
            }
        }

        updateMouseButtonState(e)
    }

    fun onMouseMoved(e: MouseEvent) {
        updateMousePos(e)
    }

    fun onMouseDragged(e: MouseEvent) {
        if (leftPressed) {
            // If the mouse was moved enough, it's a drag
            if ((mousePosScreen - leftPressedPosScreen).sqrMagnitude() > MIN_DRAG_DISTANCE_SQR) {
                onLeftDrag()
            }
        }
        if (rightPressed) {
            // If the mouse was moved enough, it's a drag
            if ((mousePosScreen - rightPressedPosScreen).sqrMagnitude() > MIN_DRAG_DISTANCE_SQR) {
                onRightDrag()
            }
        }

        updateMousePos(e)
    }

    /**
     * Handles key pressed events
     *
     * @param event the key event
     */
    fun onKeyPressed(event: KeyEvent) {
        when (event.code) {
            KeyCode.P -> FhysicsCore.running = !FhysicsCore.running
            KeyCode.SPACE -> {
                DebugDrawer.clearDebug()
                FhysicsCore.update()
            }

            KeyCode.ENTER -> {
                DebugDrawer.clearDebug()
                FhysicsCore.update()
            }

            KeyCode.Z -> RenderUtil.drawer.resetZoom()
            KeyCode.J -> QuadTree.capacity -= 5
            KeyCode.K -> QuadTree.capacity += 5
            KeyCode.G -> CapacityDiagram(FhysicsCore.qtCapacity)
            KeyCode.Q -> println(QuadTree.root.objects.forEach { println(it) })
            KeyCode.S -> println(SceneListener.selectedObject)
            else -> {}
        }
    }

    /**
     * Sets the mouse position in world space and updates the spawn preview position
     * @param e the mouse event
     */
    private fun updateMousePos(e: MouseEvent) {
        mousePosScreen.set(getMouseScreenPos(e))
        mousePosWorld.set(RenderUtil.screenToWorld(mousePosScreen))

        // Update the spawn preview position if it's not set due to dragging a rectangle
        if (!leftDragging) {
            spawnPreview?.position!!.set(mousePosWorld)
        }
    }

    /**
     * Updates the state of the mouse buttons
     * @param e the mouse event
     */
    private fun updateMouseButtonState(e: MouseEvent) {
        leftPressed = e.isPrimaryButtonDown
        rightPressed = e.isSecondaryButtonDown
    }

    /**
     * Gets the mouse position in screen space
     * @param e the mouse event
     * @return the mouse position in screen space
     */
    private fun getMouseScreenPos(e: MouseEvent): Vector2 {
        return Vector2(e.x.toFloat(), e.y.toFloat())
    }

    /// endregion
}