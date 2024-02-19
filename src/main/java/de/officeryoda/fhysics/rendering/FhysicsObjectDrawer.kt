package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.Main
import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2
import javafx.application.Application
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.stage.Stage
import java.awt.Insets
import java.awt.geom.Rectangle2D
import javax.swing.JFrame


class FhysicsObjectDrawer : Application() {

//    private val fhysicsPanel: FhysicsPanel

//    init {
//        title = "Fhysics"
//
//        setWindowSize()
//        isResizable = false
//        defaultCloseOperation = EXIT_ON_CLOSE
//
//        val zoom = calculateZoom()
//        fhysicsPanel = FhysicsPanel(fhysics, zoom)
//        add(fhysicsPanel)
//
//        val mouseListener = MouseListener(fhysicsPanel)
//        addMouseWheelListener(mouseListener)
//        addMouseListener(mouseListener)
//        addKeyListener(KeyListener(fhysics))
//
//        setLocationRelativeTo(null) // center the frame on the screen
//        isVisible = true
//    }
//
//    private fun setWindowSize() {
//        val insets = Insets(31, 8, 8, 8) // these will be the values
//        val border: Rectangle2D = FhysicsCore.BORDER
//        val borderWidth: Double = border.width
//        val borderHeight: Double = border.height
//
//        val ratio: Double = borderHeight / borderWidth
//        val maxWidth = 1440.0
//        val maxHeight = 960.0
//
//        var windowWidth = maxWidth
//        var windowHeight = (windowWidth * ratio + insets.top).toInt()
//
//        if (windowHeight > maxHeight) {
//            windowHeight = maxHeight.toInt()
//            windowWidth = ((windowHeight - insets.top) / ratio).toInt().toDouble()
//        }
//
//        setSize(windowWidth, windowHeight - insets.bottom)
//    }
//
//    private fun calculateZoom(): Double {
//        val border: Rectangle2D = FhysicsCore.BORDER
//        val borderWidth: Double = border.width
//        val windowWidth: Double = width.toDouble() - (8 + 8) // -(insets.left[8] + insets.right[8])
//        return windowWidth / borderWidth
//    }

    fun repaintObjects() {
//        fhysicsPanel.repaint()
    }

    fun launch() {
        launch(FhysicsObjectDrawer::class.java)
    }

    override fun start(primaryStage: Stage) {
        println("FhysicsFX")

//        fhysics = FhysicsCore()

        val root = Group()
        val canvas = Canvas(Main.WIDTH, Main.HEIGHT)
        root.children.add(canvas)

        val scene = Scene(root, Main.WIDTH, Main.HEIGHT)
        primaryStage.title = "Fhysics"
        primaryStage.scene = scene
        primaryStage.isResizable = false

//        val drawer = FhysicsObjectDrawer(fhysics, canvas.graphicsContext2D)
//        fhysics.drawer = drawer
//        fhysics.startUpdateLoop()

//        canvas.setOnScroll { event -> drawer.onMouseWheel(event.deltaY) }
//        canvas.setOnMousePressed { event -> drawer.onMousePressed(Vector2(event.x, event.y)) }

        primaryStage.show()
    }
}

