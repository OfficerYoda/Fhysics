package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.Border
import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.Vector2Int
import de.officeryoda.fhysics.objects.Box
import de.officeryoda.fhysics.objects.Circle
import de.officeryoda.fhysics.objects.FhysicsObject
import java.awt.*
import java.awt.geom.Rectangle2D
import javax.swing.JFrame
import javax.swing.JPanel


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
        val border: Border = FhysicsCore.BORDER
        val borderWidth: Double = border.rightBorder - border.leftBorder
        val borderHeight: Double = border.topBorder - border.bottomBorder

        val ratio: Double = borderHeight / borderWidth
        val maxWidth = 1440
        val maxHeight = 960

        var windowWidth = maxWidth
        var windowHeight = (windowWidth * ratio + insets.top).toInt()

        if (windowHeight > maxHeight) {
            windowHeight = maxHeight
            windowWidth = ((windowHeight - insets.top) / ratio).toInt()
        }

        setSize(windowWidth, windowHeight)
    }


    private fun calculateZoom(): Double {
        val border: Border = FhysicsCore.BORDER
        val borderWidth: Double = border.rightBorder - border.leftBorder
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

        drawBorder(g)
        drawQuadTree(g)
        drawMSPU(g)
    }

    private fun drawAllObjects(g: Graphics) {
        g.color = objectColor
        for (`object` in fhysics.fhysicsObjects.toList()) {
            drawObject(`object`, g)
        }
    }

    private fun drawObject(obj: FhysicsObject, g: Graphics) {
        g.color = obj.color
        if (obj is Box) {
            drawBox(obj, g)
        } else if (obj is Circle) {
            drawCircle(obj, g)
        }
    }

    private fun drawBox(box: Box, g: Graphics) {
        val pos: Vector2Int = transformPosition(box.position)
        g.fillRect(
            pos.x, pos.y,
            box.width.toInt(), box.height.toInt()
        )
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

    private fun drawQuadTree(g: Graphics) {
        fhysics.quadTree.draw(g, ::drawAndTransformRect)
    }

    private fun drawBorder(g: Graphics) {
        val b: Border = FhysicsCore.BORDER
        val rect = Rectangle2D.Double(
            b.leftBorder,
            b.bottomBorder,
            b.rightBorder,
            b.topBorder
        )

        drawAndTransformRect(g, rect)
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
        val mspu = fhysics.getAverageUpdateTime() // Milliseconds per Update
        val fontSize = height / 30 // Adjust the divisor for the desired scaling

        val font = Font("Spline Sans", Font.PLAIN, fontSize)
        g.font = font
        g.color = Color.WHITE
        g.drawString("MSPU: $mspu", 5, 40)
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
        zoom += dir * -0.2
    }

    fun onMousePressed(mousePos: Vector2) {
        // inverse Transform mousePosition
        mousePos.x -= insets.left
        mousePos.y -= insets.top
        mousePos.y = height - mousePos.y
        mousePos /= zoom

        fhysics.fhysicsObjects.add(Circle(mousePos, 0.2))
    }
}