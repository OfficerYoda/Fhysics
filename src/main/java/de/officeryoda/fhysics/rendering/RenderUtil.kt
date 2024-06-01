package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.Vector2
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Paint
import java.awt.Color

object RenderUtil {

    /**
     * The drawer used to draw the objects
     *
     */
    lateinit var drawer: FhysicsObjectDrawer

    /**
     * The width of the canvas
     */
    private val width: Double
        get() = drawer.width

    /**
     * The height of the canvas
     */
    private val height: Double
        get() = drawer.height

    /**
     * The zoom factor of the drawer
     */
    var zoom: Double
        get() = drawer.zoom
        set(value) {
            drawer.zoom = value
        }

    /**
     * The zoom center of the drawer
     */
    var zoomCenter: Vector2
        get() = drawer.zoomCenter
        set(value) {
            drawer.zoomCenter = value
        }

    /**
     * The target zoom factor of the drawer
     */
    private val gc: GraphicsContext
        get() = drawer.gc

    /**
     * Transforms a position from world coordinates to screen coordinates
     *
     * @param pos the original position
     * @return the transformed position
     */
    fun worldToScreen(pos: Vector2): Vector2 {
        val newX: Float = worldToScreenX(pos.x).toFloat()
        val newY: Float = worldToScreenY(pos.y).toFloat()
        return Vector2(newX, newY)
    }

    /**
     * Transforms an x-coordinate from world coordinates to screen coordinates
     *
     * @param x the original x-coordinate
     * @return the transformed x-coordinate
     */
    fun worldToScreenX(x: Double): Double {
        return x * zoom - zoomCenter.x * zoom + width / 2
    }

    /**
     * Transforms an x-coordinate from world coordinates to screen coordinates
     *
     * @param x the original x-coordinate
     * @return the transformed x-coordinate
     */
    fun worldToScreenX(x: Float): Double {
        return worldToScreenX(x.toDouble())
    }

    /**
     * Transforms a y-coordinate from world coordinates to screen coordinates
     *
     * @param y the original y-coordinate
     * @return the transformed y-coordinate
     */
    fun worldToScreenY(y: Double): Double {
        return height - (y * zoom - zoomCenter.y * zoom + height / 2)
    }

    /**
     * Transforms a y-coordinate from world coordinates to screen coordinates
     *
     * @param y the original y-coordinate
     * @return the transformed y-coordinate
     */
    fun worldToScreenY(y: Float): Double {
        return worldToScreenY(y.toDouble())
    }

    /**
     * Transforms a position from screen coordinates to world coordinates
     *
     * @param pos the original position
     * @return the transformed position
     */
    fun screenToWorld(pos: Vector2): Vector2 {
        val newX: Float = screenToWorldX(pos.x).toFloat()
        val newY: Float = screenToWorldY(pos.y).toFloat()
        return Vector2(newX, newY)
    }

    /**
     * Transforms an x-coordinate from screen coordinates to world coordinates
     *
     * @param x the original x-coordinate
     * @return the transformed x-coordinate
     */
    fun screenToWorldX(x: Double): Double {
        return (x - width / 2 + zoomCenter.x * zoom) / zoom
    }

    /**
     * Transforms an x-coordinate from screen coordinates to world coordinates
     *
     * @param x the original x-coordinate
     * @return the transformed x-coordinate
     */
    fun screenToWorldX(x: Float): Double {
        return screenToWorldX(x.toDouble())
    }

    /**
     * Transforms a y-coordinate from screen coordinates to world coordinates
     *
     * @param y the original y-coordinate
     * @return the transformed y-coordinate
     */
    fun screenToWorldY(y: Double): Double {
        return (height - y - height / 2 + zoomCenter.y * zoom) / zoom
    }

    /**
     * Transforms a y-coordinate from screen coordinates to world coordinates
     *
     * @param y the original y-coordinate
     * @return the transformed y-coordinate
     */
    fun screenToWorldY(y: Float): Double {
        return screenToWorldY(y.toDouble())
    }

    /**
     * Linearly interpolates between two values
     *
     * @param a the first value
     * @param b the second value
     * @param t the interpolation factor
     * @return the interpolated value
     */
    fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }

    fun lerp(a: Double, b: Double, t: Double): Double {
        return a + (b - a) * t
    }

    /**
     * Linearly interpolates between two vectors
     *
     * @param a the first vector
     * @param b the second vector
     * @param t the interpolation factor
     */
    fun lerpV2(a: Vector2, b: Vector2, t: Float): Vector2 {
        val x: Float = lerp(a.x, b.x, t)
        val y: Float = lerp(a.y, b.y, t)
        return Vector2(x, y)
    }


    /**
     * Converts a java.awt.Color to a javafx.scene.paint.Color
     *
     * @param javafxColor the original color
     * @return the converted color
     */
    fun colorToPaint(javafxColor: Color): Paint {
        return javafx.scene.paint.Color(
            javafxColor.red / 255.0,
            javafxColor.green / 255.0,
            javafxColor.blue / 255.0,
            javafxColor.alpha / 255.0
        )
    }

    /**
     * Converts a javafx.scene.paint.Color to a java.awt.Color
     *
     * @param value the original color
     * @return the converted color
     */
    fun paintToColor(value: Paint): Color {
        val color = value as javafx.scene.paint.Color
        return Color(
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt(),
            (color.opacity * 255).toInt()
        )
    }

    /**
     * Sets the fill color of the graphics context
     *
     * @param color the new fill color
     */
    fun setFillColor(color: Color) {
        gc.fill = colorToPaint(color)
    }

    /**
     * Sets the stroke color of the graphics context
     *
     * @param color the new stroke color
     */
    fun setStrokeColor(color: Color) {
        gc.stroke = colorToPaint(color)
    }
}