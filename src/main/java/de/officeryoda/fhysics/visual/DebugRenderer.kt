package de.officeryoda.fhysics.visual

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Settings
import de.officeryoda.fhysics.engine.datastructures.BoundingBox
import de.officeryoda.fhysics.engine.datastructures.QuadTree
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.visual.RenderUtil.toScreenSpace
import de.officeryoda.fhysics.visual.RenderUtil.toScreenSpaceX
import de.officeryoda.fhysics.visual.RenderUtil.toScreenSpaceY
import de.officeryoda.fhysics.visual.RenderUtil.zoom
import javafx.scene.canvas.GraphicsContext
import javafx.scene.text.Font
import javafx.scene.text.Text
import java.awt.Color
import java.awt.geom.Rectangle2D
import java.util.*
import kotlin.math.PI
import kotlin.math.min

/**
 * Contains functions for rendering debug information.
 */
object DebugRenderer {

    /** The main renderer */
    lateinit var renderer: Renderer

    /** The graphics context of the renderer */
    private val gc: GraphicsContext by lazy { renderer.gc }

    private val debugPoints: MutableList<DebugPoint> = ArrayList()
    private val debugLines: MutableList<DebugLine> = ArrayList()
    private val debugVectors: MutableList<DebugVector> = ArrayList()

    /// region =====Draw functions=====
    fun drawDebug() {
        if (Settings.drawQuadTree) QuadTree.drawNodes(renderer.viewingFrustum)
        drawDebugPoints()
        drawDebugLines()
        drawDebugVectors()
        drawStats()
        drawPaused()
    }

    private fun drawDebugPoints() {
        val pointSize = 6.0

        for (point: DebugPoint in debugPoints.toList()) {
            val pos: Vector2 = point.position.toScreenSpace()
            RenderUtil.setFillColor(point.color)
            gc.fillOval(
                pos.x - pointSize / 2,
                pos.y - pointSize / 2,
                pointSize,
                pointSize
            )

            updateDuration(point, debugPoints)
        }
    }

    private fun drawDebugLines() {
        for (line: DebugLine in debugLines.toList()) {
            RenderUtil.setStrokeColor(line.color)
            strokeLine(line.start, line.end)

            updateDuration(line, debugLines)
        }
    }

    private fun drawDebugVectors() {
        // Draw the vectors as an arrow from the support to the direction
        for (vector: DebugVector in debugVectors.toList()) {
            RenderUtil.setStrokeColor(vector.color)
            drawVector(vector.support, vector.direction)

            updateDuration(vector, debugVectors)
        }
    }

    /**
     * Updates the duration of the given [element] and removes it from the [list] if the duration is 0.
     */
    private fun <T : DebugElement> updateDuration(element: T, list: MutableList<T>) {
        // If the remaining duration is greater than 0, decrease it
        if (element.durationFrames > 0) {
            // Only decrease the duration if the simulation is running
            if (FhysicsCore.running) {
                element.durationFrames--
            }
        } else {
            // If the duration is 0, remove the element from the list
            list.remove(element)
        }
    }

    private fun strokeLine(start: Vector2, end: Vector2) {
        val screenStart: Vector2 = start.toScreenSpace()
        val screenEnd: Vector2 = end.toScreenSpace()
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

    /**
     * Draws the enabled statistics on the screen.
     */
    private fun drawStats() {
        val stats: MutableList<String> = mutableListOf()

        if (Settings.showQTCapacity) {
            stats.add("QuadTree Capacity: ${QuadTree.capacity}")
        }

        if (Settings.showMSPU) {
            stats.add("MSPU: ${FhysicsCore.updateStopwatch.toRoundedString()}")
        }

        if (Settings.showUPS) {
            val mspu: Double = FhysicsCore.updateStopwatch.average()
            val ups: Double = min(Settings.UPDATES_PER_SECOND.toDouble(), 1000.0 / mspu)
            val upsRounded: String = String.format(Locale.US, "%.2f", ups)
            stats.add("UPS: $upsRounded")
        }

        if (Settings.showSubSteps) {
            stats.add("Sub-steps: ${Settings.SUB_STEPS}")
        }

        if (Settings.showObjectCount) {
            stats.add("Objects: ${QuadTree.getObjectCount()}")
        }

        if (Settings.showRenderTime) {
            stats.add("Render Time: ${renderer.drawStopwatch.toRoundedString()}")
        }

        drawStatsList(stats.reversed()) // Reverse to make it same order as in settings
    }

    /**
     * Draws the given [stats] list as text on the screen.
     */
    private fun drawStatsList(stats: List<String>) {
        val height: Double = gc.canvas.height - Renderer.TITLE_BAR_HEIGHT
        val font: Font = setFont(height)

        val lineHeight: Double = font.size
        val borderSpacing = 5.0

        for (i: Int in stats.indices) {
            val text: String = stats[i]

            if (Settings.drawQuadTree) {
                // Outline the text for better readability
                gc.strokeText(text, borderSpacing, height - i * lineHeight - borderSpacing)
            }

            gc.fillText(text, borderSpacing, height - i * lineHeight - borderSpacing)
        }
    }

    /**
     * Draws the paused text in the bottom right corner when the simulation is paused.
     */
    private fun drawPaused() {
        // Draw the paused text if the simulation is paused in the bottom right corner
        if (!FhysicsCore.running) {
            val height: Double = gc.canvas.height - Renderer.TITLE_BAR_HEIGHT
            val width: Double = gc.canvas.width
            setFont(height)

            val borderSpacing = 5.0

            val text = "Paused"
            if (Settings.drawQuadTree) {
                // Outline the text for better readability
                gc.strokeText(text, width - 118 - borderSpacing, height - borderSpacing)
            }

            gc.fillText(text, width - 118 - borderSpacing, height - borderSpacing)
        }
    }

    private fun setFont(height: Double): Font {
        val fontSize: Double = height / 30.0 // Adjust the divisor for the desired scaling
        val font = Font("Spline Sans", fontSize)
        gc.font = font
        RenderUtil.setFillColor(Color.WHITE)
        RenderUtil.setStrokeColor(Color.BLACK)
        return font
    }

    fun drawBoundingBox(boundingBox: BoundingBox) {
        RenderUtil.setStrokeColor(Color.RED)
        gc.strokeRect(
            boundingBox.x.toScreenSpaceX().toDouble(),
            (boundingBox.y + boundingBox.height).toScreenSpaceY().toDouble(),
            boundingBox.width * zoom,
            boundingBox.height * zoom
        )
    }

    fun drawQTNode(bbox: BoundingBox, count: Int) {
        val x: Double = bbox.x.toScreenSpaceX().toDouble()
        val y: Double = (bbox.y + bbox.height).toScreenSpaceY().toDouble()
        val width: Double = bbox.width * zoom
        val height: Double = bbox.height * zoom

        // Draw Border
        RenderUtil.setStrokeColor(Color.WHITE)
        gc.strokeRect(x, y, width, height)

        // Only draw the fill if the option is enabled
        if (!Settings.drawQTNodeUtilization) return

        // Draw transparent fill
        val quadTreeCapacity: Int = QuadTree.capacity
        RenderUtil.setFillColor(
            Color(
                66, 164, 245,
                // Less transparent if more objects are in the node
                (count.toFloat() / quadTreeCapacity * 192).toInt().coerceAtMost(255)
            )
        )
        gc.fillRect(x, y, width, height)

        // Write the amount of objects in the node
        drawCenteredText(count.toString(), Rectangle2D.Double(x, y, width, height))
    }

    private fun drawCenteredText(text: String, rect: Rectangle2D) {
        val fontSize: Double = (rect.height / 2) // Adjust the divisor for the desired scaling
        val font = Font("Spline Sans", fontSize)

        // Set the font and fill color
        RenderUtil.setFillColor(Color(255, 255, 255, 192))
        val textNode = Text(text)
        gc.font = font
        textNode.font = font

        // The width and height of the text will take up
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