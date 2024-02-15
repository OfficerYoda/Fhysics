package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.QuadTree
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.Vector2Int
import de.officeryoda.fhysics.objects.Box
import de.officeryoda.fhysics.objects.Circle
import de.officeryoda.fhysics.objects.FhysicsObject
import java.awt.*
import java.awt.geom.Rectangle2D
import java.util.*
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities


class FhysicsObjectDrawer(fhysics: FhysicsCore) : JFrame() {
    private val fhysicsPanel: FhysicsPanel

    init {
        title = "Fhysics"

        setWindowSize()
        isResizable = false
        defaultCloseOperation = EXIT_ON_CLOSE

        val zoom = calculateZoom()
        fhysicsPanel = FhysicsPanel(fhysics, zoom)
        add(fhysicsPanel)

        val mouseListener = MouseListener(fhysicsPanel)
        addMouseWheelListener(mouseListener)
        addMouseListener(mouseListener)

        setLocationRelativeTo(null) // center the frame on the screen
        isVisible = true
    }

    private fun setWindowSize() {
        val insets = Insets(31, 8, 8, 8) // these will be the values
        val border: Rectangle2D = FhysicsCore.BORDER
        val borderWidth: Double = border.width
        val borderHeight: Double = border.height

        val ratio: Double = borderHeight / borderWidth
        val maxWidth = 1440
        val maxHeight = 960

        var windowWidth = maxWidth
        var windowHeight = (windowWidth * ratio + insets.top).toInt()

        if (windowHeight > maxHeight) {
            windowHeight = maxHeight
            windowWidth = ((windowHeight - insets.top) / ratio).toInt()
        }

        setSize(windowWidth, windowHeight - insets.bottom)
    }

    private fun calculateZoom(): Double {
        val border: Rectangle2D = FhysicsCore.BORDER
        val borderWidth: Double = border.width
        val windowWidth: Int = width - (8 + 8) // -(insets.left[8] + insets.right[8])
        return windowWidth / borderWidth
    }

    fun repaintObjects() {
        fhysicsPanel.repaint()
    }
}

internal class FhysicsPanel(private val fhysics: FhysicsCore, zoom: Double) : JPanel() {

    private val objectColor: Color = Color.decode("#2f2f30")

    // getInsets doesn't return the right values
    private val insets: Insets

    private var zoom: Double

    init {
        val backgroundColor: Color = Color.decode("#010409")
        this.zoom = zoom
        this.insets = Insets(31, 8, 8, 8) // that's just how it is
        background = backgroundColor
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        drawAllObjects(g)

//        drawHighlightQuadTree(g)

//        drawBorder(g)
//        drawQuadTree(g)

        drawMSPU(g)
    }

    private var quadTreeHighlightSize = 20
    private fun drawHighlightQuadTree(g: Graphics) {
        val mousePoint: Point = MouseInfo.getPointerInfo().location
        SwingUtilities.convertPointFromScreen(mousePoint, this)

        val mousePos = Vector2(mousePoint.x.toDouble(), height - mousePoint.y.toDouble()) / zoom

        var rectX = mousePos.x - quadTreeHighlightSize / 2
        var rectY = mousePos.y - quadTreeHighlightSize / 2

        var queryRect = Rectangle2D.Double(rectX, rectY, quadTreeHighlightSize.toDouble(), quadTreeHighlightSize.toDouble())

        val currentCenterX = queryRect.x + (queryRect.width / 2)
        val currentCenterY = queryRect.y + (queryRect.height / 2)

        val newWidth = queryRect.width / zoom
        val newHeight = queryRect.height / zoom

        queryRect =
            Rectangle2D.Double(currentCenterX - (newWidth / 2), currentCenterY - (newHeight / 2), newWidth, newHeight)

        QuadTree.count = 0
        g.color = Color.BLUE
        fhysics.quadTree.query(queryRect).filterIsInstance<Circle>().forEach {
            drawCircle(it, g)
        }

        mousePos *= zoom
        mousePos.x -= insets.left
        mousePos.y += insets.top
        mousePos /= zoom

        rectX = (transformX(mousePos.x) - quadTreeHighlightSize / 2 + insets.left).toDouble()
        rectY = (transformY(mousePos.y) - quadTreeHighlightSize / 2 + insets.top).toDouble()

        g.drawRect(rectX.toInt(), rectY.toInt(), quadTreeHighlightSize, quadTreeHighlightSize)
    }

    private fun drawAllObjects(g: Graphics) {
        g.color = objectColor
        for (obj in fhysics.fhysicsObjects.toList()) {
            drawObject(obj, g)
        }
    }

    private fun drawObject(obj: FhysicsObject, g: Graphics) {
        g.color = obj.color
        if (obj is Circle) {
            drawCircle(obj, g)
        } else if (obj is Box) {
            drawBox(obj, g)
        }
    }

    private fun drawCircle(circle: Circle, g: Graphics) {
        val pos: Vector2Int = transformPosition(circle.position)
        val radius: Int = (circle.radius * zoom).toInt()
        val diameter: Int = 2 * radius
        g.fillOval(
            pos.x - radius, pos.y - radius,
            diameter, diameter
        )
    }

    private fun drawBox(box: Box, g: Graphics) {
        val pos: Vector2Int = transformPosition(box.position)
        g.fillRect(
            pos.x,
            (pos.y - box.height * zoom).toInt(),
            (box.width * zoom).toInt(),
            (box.height * zoom).toInt()
        )
    }

    private fun drawQuadTree(g: Graphics) {
        fhysics.quadTree.draw(g, ::drawAndTransformRect)
    }

    private fun drawBorder(g: Graphics) {
        drawAndTransformRect(g, FhysicsCore.BORDER)
    }

    private fun drawAndTransformRect(g: Graphics, rect: Rectangle2D) {
        val x: Int = transformX(rect.x)
        val y: Int = transformY((rect.y + rect.height))
        val width: Int = (rect.width * zoom).toInt()
        val height: Int = (rect.height * zoom).toInt()

        g.color = Color.white
        g.drawRect(x, y, width, height)
    }

    private fun drawMSPU(g: Graphics) {
        val mspu: Double = fhysics.getAverageUpdateTime() // Milliseconds per Update
        val mspuRounded: String = String.format(Locale.US, "%.2f", mspu)
        val fps: Double = 1000.0 / mspu
        val fpsRounded: String = String.format(Locale.US, "%.2f", fps)

        val fontSize: Int = height / 30 // Adjust the divisor for the desired scaling

        val font = Font("Spline Sans", Font.PLAIN, fontSize)
        g.font = font
        g.color = Color.WHITE

        val lineHeight: Int = g.fontMetrics.height
        g.drawString("MSPU: $mspuRounded", 5, lineHeight)
        g.drawString("FPS: $fpsRounded", 5, 2 * lineHeight)
    }

    /**
     * Transforms the position to make objects with y-pos 0
     * appear at the bottom of the window instead of at the top.
     * Takes into account the window's height, top insets, left insets, and zoom factor.
     *
     * @param pos the original position
     * @return the transformed position
     */
    private fun transformPosition(pos: Vector2): Vector2Int {
        val newX: Int = transformX(pos.x)
        val newY: Int = transformY(pos.y)
        return Vector2Int(newX, newY)
    }

    private fun transformX(x: Double): Int {
        return (x * zoom).toInt()
    }

    private fun transformY(y: Double): Int {
        return (height - (y * zoom)).toInt()
    }

    fun onMouseWheel(dir: Int) {
//        zoom -= dir * 0.2
        quadTreeHighlightSize -= dir * 3
    }

    fun onMousePressed(mousePos: Vector2) {
        // inverse Transform mousePosition
        mousePos.x -= insets.left
        mousePos.y -= insets.top
        mousePos.y = height - mousePos.y
        mousePos /= zoom

        fhysics.fhysicsObjects.add(Circle(mousePos, 1.0))
    }
}