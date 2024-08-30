package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.QuadTree
import de.officeryoda.fhysics.engine.Vector2
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent

object BetterSceneListener {

    /**
     * The minimum distance the mouse has to be moved in screen space to be registered as a drag
     */
    private const val MIN_DRAG_DISTANCE_SQR: Float = 4f // 2px

    /**
     * The position of the mouse in world space
     */
    private val mousePosScreen: Vector2 = Vector2.ZERO
    private val mousePosWorld: Vector2 = Vector2.ZERO

    // The state of the mouse buttons
    private var leftPressed: Boolean = false
    private var rightPressed: Boolean = false

    // The position where the left mouse button was pressed (in world space)
    private val leftPressedPosScreen: Vector2 = Vector2.ZERO
    private val leftPressedPosWorld: Vector2 = Vector2.ZERO
    private val rightPressedPosScreen: Vector2 = Vector2.ZERO
    private val rightPressedPosWorld: Vector2 = Vector2.ZERO

    /// region =====Custom event handlers=====
    // TODO: Add parameter: mouse pos, etc.
    private fun onLeftClick() {
        println("Left click")
    }

    private fun onRightClick() {
        println("Right click")
    }

    private fun onLeftDrag() {
        println("Left drag")
    }

    private fun onRightDrag() {
        println("Right drag")
    }

    /// endregion

    /// region =====Vanilla event handlers=====
    fun onMouseWheel(e: ScrollEvent) {
//        TODO("Not yet implemented")
    }

    fun onMousePressed(e: MouseEvent) {
        // Set the pressed position if the specific button was pressed
        if (!leftPressed && e.isPrimaryButtonDown) {
            leftPressedPosScreen.set(getMouseScreenPos(e))
            leftPressedPosWorld.set(mousePosWorld)
        }
        if (!rightPressed && e.isSecondaryButtonDown) {
            rightPressedPosScreen.set(getMouseScreenPos(e))
            rightPressedPosWorld.set(mousePosWorld)
        }

        updateMouseButtonState(e)
    }

    fun onMouseReleased(e: MouseEvent) {
        if (leftPressed && !e.isPrimaryButtonDown) {
            // If the mouse wasn't moved too much, it's a click
            if ((mousePosScreen - leftPressedPosScreen).sqrMagnitude() <= MIN_DRAG_DISTANCE_SQR) {
                onLeftClick()
            }
        }
        if (rightPressed && !e.isSecondaryButtonDown) {
            // If the mouse wasn't moved too much, it's a click
            if ((mousePosScreen - rightPressedPosScreen).sqrMagnitude() <= MIN_DRAG_DISTANCE_SQR) {
                onRightClick()
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
     * Sets the mouse position in world space
     * @param e the mouse event
     * @return the mouse position in world space
     */
    private fun updateMousePos(e: MouseEvent) {
        mousePosScreen.set(getMouseScreenPos(e))
        mousePosWorld.set(RenderUtil.screenToWorld(mousePosScreen))
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