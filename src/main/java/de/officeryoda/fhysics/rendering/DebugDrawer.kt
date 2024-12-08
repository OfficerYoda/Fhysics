package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.datastructures.spatial.BoundingBox
import de.officeryoda.fhysics.engine.datastructures.spatial.CenterRect
import de.officeryoda.fhysics.engine.datastructures.spatial.QuadTree
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.rendering.RenderUtil.zoom
import javafx.scene.canvas.GraphicsContext
import javafx.scene.text.Font
import javafx.scene.text.Text
import java.awt.Color
import java.awt.geom.Rectangle2D
import java.util.*
import kotlin.math.PI
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
    private val debugVectors: MutableList<DebugVector> = ArrayList()

    /// region =====Draw functions=====
    fun drawDebug() {
        if (UIController.drawQuadTree) QuadTree.drawNodes()
        drawDebugPoints()
        drawDebugLines()
        drawDebugVectors()
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
                if (FhysicsCore.running) {
                    point.durationFrames--
                }
            } else {
                debugPoints.remove(point)
            }
        }
    }

    private fun drawDebugLines() {
        for (line: DebugLine in debugLines.toList()) {
            RenderUtil.setStrokeColor(line.color)
            strokeLine(line.start, line.end)

            // Only decrease the duration if the simulation is running
            if (line.durationFrames > 0) {
                if (FhysicsCore.running) {
                    line.durationFrames--
                }
            } else {
                debugLines.remove(line)
            }
        }
    }

    private fun drawDebugVectors() {
        // Draw the vectors as an arrow from the support to the direction
        for (vector: DebugVector in debugVectors.toList()) {
            RenderUtil.setStrokeColor(vector.color)
            drawVector(vector.support, vector.direction)

            // Update the duration of the vector
            // If the max duration is reached remove the vector
            if (vector.durationFrames > 0) {
                if (FhysicsCore.running) {
                    vector.durationFrames--
                }
            } else {
                debugVectors.remove(vector)
            }
        }
    }

    private fun strokeLine(start: Vector2, end: Vector2) {
        val screenStart: Vector2 = RenderUtil.worldToScreen(start)
        val screenEnd: Vector2 = RenderUtil.worldToScreen(end)
        gc.strokeLine(
            screenStart.x.toDouble(),
            screenStart.y.toDouble(),
            screenEnd.x.toDouble(),
            screenEnd.y.toDouble()
        )
    }

    private fun drawVector(support: Vector2, direction: Vector2) {
        // Draw the vectors as an arrow from the support in the direction
        val end: Vector2 = support + direction

        // Draw the main line of the vector
        strokeLine(support, end)

        // Length and angle of the arrowhead lines
        val arrowHeadLength: Float = min(10f, direction.magnitude() / 3.6f)
        val arrowAngle = 0.523599f // 30 degrees in radians
        val arrowHead: Vector2 = direction.normalized() * arrowHeadLength

        // Draw the arrowhead lines
        strokeLine(end, arrowHead.rotated((PI - arrowAngle).toFloat()) + end)
        strokeLine(end, arrowHead.rotated((PI + arrowAngle).toFloat()) + end)
    }

    private fun drawStats() {
        val stats: MutableList<String> = mutableListOf()

        if (UIController.drawQTCapacity)
            stats.add("QuadTree Capacity: ${QuadTree.capacity}")

        if (UIController.drawMSPU) {
            stats.add("MSPU: ${FhysicsCore.updateStopwatch.roundedString()}")
        }

        if (UIController.drawUPS) {
            val mspu: Double = FhysicsCore.updateStopwatch.average()
            val ups: Double = min(FhysicsCore.UPDATES_PER_SECOND.toDouble(), 1000.0 / mspu)
            val upsRounded: String = String.format(Locale.US, "%.2f", ups)
            stats.add("UPS: $upsRounded")
        }

        if (UIController.drawObjectCount)
            stats.add("Objects: ${QuadTree.getObjectCount()}")

        if (UIController.drawRenderTime)
            stats.add("Render Time: ${drawer.drawStopwatch.roundedString()}")

        drawStatsList(stats.reversed()) // Reverse to make it same order as in settings
    }

    private fun drawStatsList(stats: List<String>) {
        val height: Double = gc.canvas.height - FhysicsObjectDrawer.TITLE_BAR_HEIGHT
        val fontSize: Double = height / 30.0 // Adjust the divisor for the desired scaling
        val font = Font("Spline Sans", fontSize)
        gc.font = font
        RenderUtil.setFillColor(Color.WHITE)
        RenderUtil.setStrokeColor(Color.BLACK)

        val lineHeight: Double = font.size
        val borderSpacing = 5.0

        for (i: Int in stats.indices) {
            val text: String = stats[i]

            if (UIController.drawQuadTree) {
                // Outline the text for better readability
                gc.strokeText(text, borderSpacing, height - i * lineHeight - borderSpacing)
            }

            gc.fillText(text, borderSpacing, height - i * lineHeight - borderSpacing)
        }
    }

    fun drawBoundingBox(boundingBox: BoundingBox) {
        RenderUtil.setStrokeColor(Color.RED)
//        gc.strokeRect(
//            RenderUtil.worldToScreenX(boundingBox.x),
//            RenderUtil.worldToScreenY(boundingBox.y + boundingBox.height),
//            boundingBox.width * zoom,
//            boundingBox.height * zoom
//        )

        val cRect: CenterRect = CenterRect.fromBoundingBox(boundingBox)
        val x: Double = RenderUtil.worldToScreenX((cRect[0] - cRect[2] / 2).toDouble())
        val y: Double = RenderUtil.worldToScreenY(((cRect[1] + (cRect[3] - cRect[3] / 2)).toDouble()))
        val width: Double = cRect[2] * zoom
        val height: Double = cRect[3] * zoom
        gc.strokeRect(x, y, width, height)
    }

    fun drawQTNode(bbox: BoundingBox, count: Int) {
        val x: Double = bbox.x.toDouble()
        val y: Double = bbox.y.toDouble()
        val width: Double = bbox.width * zoom
        val height: Double = bbox.height * zoom

        // Draw Border
        RenderUtil.setStrokeColor(Color.WHITE)
        gc.strokeRect(x, y, width, height)

        // Only draw the fill if the option is enabled
        if (!UIController.drawQTNodeUtilization) return

        // Draw transparent fill
        val quadTreeCapacity: Int = QuadTree.capacity
        RenderUtil.setFillColor(
            Color(
                66, 164, 245,
                // Less transparent if more objects are in the cell
                (count.toFloat() / quadTreeCapacity * 192).toInt().coerceAtMost(255)
            )
        )
        gc.fillRect(x, y, width, height)
        // Write the amount of objects in the cell
        drawCenteredText(count.toString(), Rectangle2D.Double(x, y, width, height))
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
    /// endregion

    /// region =====Debug add-functions=====
    fun addDebugPoint(position: Vector2, color: Color = Color.RED, durationFrames: Int = 1) {
        debugPoints.add(DebugPoint(position, color, durationFrames))
    }

    fun addDebugLine(start: Vector2, end: Vector2, color: Color = Color.GREEN, durationFrames: Int = 1) {
        debugLines.add(DebugLine(start, end, color, durationFrames))
    }

    fun addDebugVector(support: Vector2, direction: Vector2, color: Color = Color.BLUE, durationFrames: Int = 1) {
        debugVectors.add(DebugVector(support, direction, color, durationFrames))
    }

    fun clearDebug() {
        debugPoints.clear()
        debugLines.clear()
        debugVectors.clear()
    }
    /// endregion
}