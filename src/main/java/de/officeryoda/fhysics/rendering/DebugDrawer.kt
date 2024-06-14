package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.BoundingBox
import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.QuadTree
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.rendering.RenderUtil.zoom
import javafx.scene.canvas.GraphicsContext
import javafx.scene.text.Font
import javafx.scene.text.Text
import java.awt.Color
import java.awt.geom.Rectangle2D
import java.util.*
import kotlin.math.min

object DebugDrawer {

    /**
     * The drawer used to draw the objects
     *
     */
    lateinit var drawer: FhysicsObjectDrawer

    /**
     * The graphics context of the drawer
     */
    private val gc: GraphicsContext
        get() = drawer.gc

    private val debugPoints: MutableList<DebugPoint> = ArrayList()
    private val debugLines: MutableList<DebugLine> = ArrayList()

    /// =====Draw functions=====
    fun drawDebug() {
        if (UIController.drawQuadTree) QuadTree.root.drawNode(drawer)
        drawDebugPoints()
        drawDebugLines()
        drawStats()
    }

    private fun drawDebugPoints() {
        val pointSize = 6.0

        for (point: DebugPoint in debugPoints.toList()) {
            val pos: Vector2 = RenderUtil.worldToScreen(point.position)
            RenderUtil.setFillColor(point.color)
            gc.fillOval(
                pos.x - pointSize / 2,
                pos.y - pointSize / 2,
                pointSize,
                pointSize
            )

            // Update the duration of the point
            // If the max duration is reached remove the point
            if (point.durationFrames > 0) {
                point.durationFrames--
            } else {
                debugPoints.remove(point)
            }
        }
    }

    private fun drawDebugLines() {
        gc.lineWidth = 4.0

        for (line: DebugLine in debugLines.toList()) {
            val start: Vector2 = RenderUtil.worldToScreen(line.start)
            val end: Vector2 = RenderUtil.worldToScreen(line.end)

            RenderUtil.setStrokeColor(line.color)
            gc.strokeLine(start.x.toDouble(), start.y.toDouble(), end.x.toDouble(), end.y.toDouble())

            // Update the duration of the line
            // If the max duration is reached remove the line
            if (line.durationFrames > 0) {
                line.durationFrames--
            } else {
                debugLines.remove(line)
            }
        }

        gc.lineWidth = 1.0
    }

    private fun drawStats() {
        val stats: ArrayList<String> = ArrayList()

        if (UIController.drawMSPU || UIController.drawUPS) {
            if (UIController.drawMSPU) {
                stats.add("MSPU: ${FhysicsCore.updateTimer.roundedString()}")
            }

            if (UIController.drawUPS) {
                val mspu: Double = FhysicsCore.updateTimer.average() // Milliseconds per Update
                val ups: Double = min(FhysicsCore.UPDATES_PER_SECOND.toDouble(), 1000.0 / mspu)
                val upsRounded: String = String.format(Locale.US, "%.2f", ups)
                stats.add("UPS: $upsRounded")
            }
        }

        if (UIController.drawObjectCount)
            stats.add("Objects: ${QuadTree.root.countUnique()}")

        if (UIController.drawQTCapacity)
            stats.add("QuadTree Capacity: ${QuadTree.capacity}")

        drawStatsList(stats)
    }

    private fun drawStatsList(stats: ArrayList<String>) {
        val height: Double = gc.canvas.height - FhysicsObjectDrawer.TITLE_BAR_HEIGHT
        val fontSize: Double = height / 30.0 // Adjust the divisor for the desired scaling
        val font = Font("Spline Sans", fontSize)
        gc.font = font
        RenderUtil.setFillColor(Color.WHITE)
        RenderUtil.setStrokeColor(Color.BLACK)

        val lineHeight: Double = font.size
        val borderSpacing = 5.0

        for (i in 0 until stats.size) {
            val text: String = stats[i]

            if (UIController.drawQuadTree) {
                // Outline the text for better readability
                gc.strokeText(text, borderSpacing, height - i * lineHeight - borderSpacing)
            }

            gc.fillText(text, borderSpacing, height - i * lineHeight - borderSpacing)
        }
    }

    fun drawBoundingBox(obj: FhysicsObject) {
        val boundingBox: BoundingBox = obj.boundingBox

        RenderUtil.setStrokeColor(Color.RED)
        gc.strokeRect(
            RenderUtil.worldToScreenX(boundingBox.x),
            RenderUtil.worldToScreenY(boundingBox.y + boundingBox.height),
            boundingBox.width * zoom,
            boundingBox.height * zoom
        )
    }

    fun transformAndDrawQuadTreeNode(rect: BoundingBox, contentCount: Int) {
        val x: Double = RenderUtil.worldToScreenX(rect.x)
        val y: Double = RenderUtil.worldToScreenY(rect.y + rect.height)
        val width: Double = rect.width * zoom
        val height: Double = rect.height * zoom

        // Draw Border
        RenderUtil.setStrokeColor(Color.WHITE)
        gc.strokeRect(x, y, width, height)

        // Only draw the fill if the option is enabled
        if (!UIController.drawQTNodeUtilization) return

        // Draw transparent fill
        val quadTreeCapacity: Int = QuadTree.capacity
        RenderUtil.setFillColor(
            Color(
                66,
                164,
                245,
                (contentCount.toFloat() / quadTreeCapacity * 192).toInt().coerceAtMost(255)
            )
        )
        gc.fillRect(x, y, width, height)
        // Write the amount of objects in the cell
        drawCenteredText(contentCount.toString(), Rectangle2D.Double(x, y, width, height))
    }

    private fun drawCenteredText(text: String, rect: Rectangle2D) {
        val fontSize: Double = (rect.height / 2) // Adjust the divisor for the desired scaling
        val font = Font("Spline Sans", fontSize)

        gc.font = font
        RenderUtil.setFillColor(Color(255, 255, 255, 192))

        val textNode = Text(text)
        textNode.font = font

        val textWidth: Double = textNode.layoutBounds.width
        val textHeight: Double = textNode.layoutBounds.height

        val centerX: Double = rect.x + (rect.width - textWidth) / 2
        val centerY: Double = rect.y + (rect.height + textHeight / 2) / 2

        gc.fillText(text, centerX, centerY)
    }

    /// =====Debug add functions=====
    fun addDebugPoint(position: Vector2, color: Color = Color.RED, durationFrames: Int = 200) {
        debugPoints.add(DebugPoint(position, color, durationFrames))
    }

    fun addDebugLine(start: Vector2, end: Vector2, color: Color = Color.GREEN, durationFrames: Int = 200) {
        debugLines.add(DebugLine(start, end, color, durationFrames))
    }
}

abstract class DebugElement(val color: Color, var durationFrames: Int = 200)

class DebugPoint(val position: Vector2, color: Color, durationFrames: Int = 200) : DebugElement(color, durationFrames)

class DebugLine(val start: Vector2, val end: Vector2, color: Color, durationFrames: Int = 200) :
    DebugElement(color, durationFrames)