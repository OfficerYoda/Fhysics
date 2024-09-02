package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.QuadTree
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.objects.*
import de.officeryoda.fhysics.rendering.RenderUtil.drawer
import de.officeryoda.fhysics.rendering.UIController.Companion.spawnColor
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import java.awt.Color
import kotlin.math.max
import kotlin.math.min
import de.officeryoda.fhysics.rendering.UIController.Companion.spawnObjectType as selectedSpawnObjectType

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

    /// Polygon creation fields
    /**
     * The radius around the first polygon vertex where the polygon closes when clicked inside
     */
    const val POLYGON_CLOSE_RADIUS = 1.0f

    /**
     * The vertices of the polygon being created
     */
    var polyVertices: MutableList<Vector2> = ArrayList()

    /// region =====Custom event handlers=====
    private fun onLeftClick() {
        when (selectedSpawnObjectType) {
            SpawnObjectType.CIRCLE, SpawnObjectType.RECTANGLE -> spawnPreview()
            SpawnObjectType.POLYGON -> handlePolygonCreation()
            else -> {}
        }
    }

    private fun onRightClick() {
        if (selectedSpawnObjectType == SpawnObjectType.POLYGON) {
            polyVertices.clear()
        }
    }

    private fun onLeftDrag() {
        leftDragging = true

        // Don't create a drag preview if the spawn object type is not a rectangle
        if (selectedSpawnObjectType != SpawnObjectType.RECTANGLE) return

        // Calculate the corners of the rectangle
        val minX: Float = min(leftPressedPosWorld.x, mousePosWorld.x)
        val maxX: Float = max(leftPressedPosWorld.x, mousePosWorld.x)
        val minY: Float = min(leftPressedPosWorld.y, mousePosWorld.y)
        val maxY: Float = max(leftPressedPosWorld.y, mousePosWorld.y)

        // Create the preview rectangle
        val size = Vector2(maxX - minX, maxY - minY)
        val pos: Vector2 = Vector2(minX, minY) + size / 2f

        val rect = Rectangle(pos, size.x, size.y)
        val color: Color = if (spawnPreview is Rectangle) spawnPreview!!.color else rect.color
        rect.color = Color(color.red, color.green, color.blue, 128)

        spawnPreview = rect
    }

    private fun onLeftDragRelease() {
        leftDragging = false

        when (selectedSpawnObjectType) {
            SpawnObjectType.CIRCLE, SpawnObjectType.RECTANGLE -> spawnPreview()
            SpawnObjectType.POLYGON -> handlePolygonCreation()
            else -> {}
        }

        updateSpawnPreview()
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

    /// region =====Helper methods=====
    /**
     * Spawns the preview object at the mouse position
     */
    private fun spawnPreview() {
        // Check if spawn pos is outside the border
        if (!FhysicsCore.BORDER.contains(mousePosWorld)) return

        val validParams: Boolean = when (selectedSpawnObjectType) {
            SpawnObjectType.CIRCLE -> UIController.spawnRadius > 0.0F
            SpawnObjectType.RECTANGLE -> UIController.spawnWidth > 0.0F && UIController.spawnHeight > 0.0F
            else -> false
        }

        if (!validParams) return

        FhysicsCore.spawn(spawnPreview!!.apply {
            color = asOpaqueColor(spawnPreview!!.color)
            static = UIController.spawnStatic
        })
        updateSpawnPreview()
    }

    private fun handlePolygonCreation() {
        // Create the polygon if the polygon is complete
        if (polyVertices.size > 2 && PolygonCreator.isPolygonValid(polyVertices)) {
            val startPos: Vector2 = polyVertices.first()
            if (mousePosWorld.sqrDistanceTo(startPos) < POLYGON_CLOSE_RADIUS * POLYGON_CLOSE_RADIUS) {
                createAndSpawnPolygon()
                return
            }
        }

        // Add the vertex to the polygon
        polyVertices.add(mousePosWorld.copy())
    }

    private fun createAndSpawnPolygon() {
        // Create and spawn the polygon
        val polygon: Polygon = PolygonCreator.createPolygon(polyVertices.toTypedArray())
        FhysicsCore.spawn(polygon)

        // Clear the polygon vertices
        polyVertices.clear()
    }

    /**
     * Updates the spawn preview object based on the current spawn object type and the input fields.
     */
    fun updateSpawnPreview() {
        if (UIController.spawnObjectType == SpawnObjectType.NOTHING) {
            spawnPreview = null
            return
        }

        val obj: FhysicsObject = when (UIController.spawnObjectType) {
            SpawnObjectType.CIRCLE -> Circle(mousePosWorld.copy(), UIController.spawnRadius)
            SpawnObjectType.RECTANGLE -> Rectangle(
                mousePosWorld.copy(),
                UIController.spawnWidth,
                UIController.spawnHeight
            )

            SpawnObjectType.POLYGON -> {
                val circle = Circle(mousePosWorld, UIController.spawnRadius)
                circle.color = Color.PINK
                circle
            }

            else -> throw IllegalArgumentException("Invalid spawn object type")
        }

        val color: Color = if (UIController.customColor) spawnColor else obj.color
        obj.color = Color(color.red, color.green, color.blue, 128)
        spawnPreview = obj
    }

    private fun asOpaqueColor(color: Color): Color {
        return Color(color.red, color.green, color.blue)
    }

    /// endregion

    /// region =====Vanilla event handlers=====
    fun onMouseWheel(e: ScrollEvent) {
        // Zoom in or out based on the scroll direction
        val zoomFactor: Float = if (e.deltaY > 0) 1.1f else 1 / 1.1f
        drawer.targetZoom *= zoomFactor

        // Clamp the zoom level
        drawer.targetZoom = drawer.targetZoom.coerceIn(1.0, 200.0)

        // Calculate the difference between the mouse position and the current zoom center
        val deltaMousePos: Vector2 = mousePosWorld - drawer.targetZoomCenter

        // Adjust the target zoom center to zoom towards the mouse position
        drawer.targetZoomCenter = drawer.targetZoomCenter + deltaMousePos * (1 - 1 / zoomFactor)
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
                onLeftClick()
            } else {
                onLeftDragRelease()
            }
        }
        if (rightPressed && !e.isSecondaryButtonDown) {
            // If the mouse wasn't moved too much, it's a click
            if ((mousePosScreen - rightPressedPosScreen).sqrMagnitude() <= MIN_DRAG_DISTANCE_SQR) {
                onRightClick()
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

            KeyCode.Z -> drawer.resetZoom()
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
        if (!(leftDragging && selectedSpawnObjectType == SpawnObjectType.RECTANGLE)) {
            spawnPreview?.position?.set(mousePosWorld)
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