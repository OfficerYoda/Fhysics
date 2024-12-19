package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.math.Vector2
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Paint
import java.awt.Color

object RenderUtil {

    /** The drawer to use for rendering. */
    lateinit var drawer: FhysicsObjectDrawer

    /** The width of the canvas. */
    private val width: Double
        get() = drawer.width

    /** The height of the canvas. */
    private val height: Double
        get() = drawer.height

    /** The zoom factor of the drawer. */
    var zoom: Double
        get() = drawer.zoom
        set(value) {
            drawer.zoom = value
        }

    /** The zoom center of the drawer. */
    var zoomCenter: Vector2
        get() = drawer.zoomCenter
        set(value) {
            drawer.zoomCenter = value
        }

    /** The graphics context of the drawer. */
    private val gc: GraphicsContext
        get() = drawer.gc

    /**
     * Linearly interpolates between [a] and [b] based on [t].
     */
    fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }

    /**
     * Linearly interpolates between [a] and [b] based on [t].
     */
    fun lerp(a: Double, b: Double, t: Double): Double {
        return a + (b - a) * t
    }

    /**
     * Linearly interpolates between [a] and [b] based on [t].
     */
    fun lerpV2(a: Vector2, b: Vector2, t: Float): Vector2 {
        val x: Float = lerp(a.x, b.x, t)
        val y: Float = lerp(a.y, b.y, t)
        return Vector2(x, y)
    }

    /**
     * Converts a [java.awt.Color] to [javafx.scene.paint.Color].
     */
    fun colorToPaint(javaAwtColor: Color): Paint {
        return javafx.scene.paint.Color(
            javaAwtColor.red / 255.0,
            javaAwtColor.green / 255.0,
            javaAwtColor.blue / 255.0,
            javaAwtColor.alpha / 255.0
        )
    }

    /**
     * Converts a [javafx.scene.paint.Color] to a [java.awt.Color].
     */
    fun paintToColor(javafxPaint: Paint): Color {
        val color: javafx.scene.paint.Color = javafxPaint as javafx.scene.paint.Color
        return Color(
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt(),
            (color.opacity * 255).toInt()
        )
    }

    /**
     * Sets the fill [color] of the graphics context.
     */
    fun setFillColor(color: Color) {
        gc.fill = colorToPaint(color)
    }

    /**
     * Sets the stroke [color] of the graphics context.
     */
    fun setStrokeColor(color: Color) {
        gc.stroke = colorToPaint(color)
    }

    /**
     * Darkens a [color] by a certain [percentage].
     */
    fun darkenColor(color: Color, percentage: Float = 0.3f): Color {
        val red: Int = (color.red * (1 - percentage)).toInt().coerceIn(0, 255)
        val green: Int = (color.green * (1 - percentage)).toInt().coerceIn(0, 255)
        val blue: Int = (color.blue * (1 - percentage)).toInt().coerceIn(0, 255)
        return Color(red, green, blue, color.alpha)
    }

    /**
     * Converts a [Vector2] to screen space.
     */
    fun Vector2.toScreenSpace(): Vector2 {
        return Vector2(x.toScreenSpaceX(), y.toScreenSpaceY())
    }

    /**
     * Converts a [Vector2] to world space.
     */
    fun Vector2.toWorldSpace(): Vector2 {
        return Vector2(x.toWorldSpaceX(), y.toWorldSpaceY())
    }

    /**
     * Converts a [Float] x-coordinate to screen space.
     */
    fun Float.toScreenSpaceX(): Float {
        return ((this - zoomCenter.x) * zoom + width / 2).toFloat()
    }

    /**
     * Converts a [Float] y-coordinate to screen space.
     */
    fun Float.toScreenSpaceY(): Float {
        return (height - ((this - zoomCenter.y) * zoom + height / 2)).toFloat()
    }

    /**
     * Converts a [Float] x-coordinate to world space.
     */
    fun Float.toWorldSpaceX(): Float {
        return ((this - width / 2 + zoomCenter.x * zoom) / zoom).toFloat()
    }

    /**
     * Converts a [Float] y-coordinate to world space.
     */
    fun Float.toWorldSpaceY(): Float {
        return ((height - this - height / 2 + zoomCenter.y * zoom) / zoom).toFloat()
    }
}
