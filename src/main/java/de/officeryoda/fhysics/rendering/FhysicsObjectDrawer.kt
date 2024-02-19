package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2
import javafx.application.Application
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.stage.Stage
import java.awt.Color
import java.awt.Insets
import java.awt.geom.Rectangle2D


class FhysicsObjectDrawer : Application() {

    val fhysics: FhysicsCore = FhysicsCore()

    private lateinit var stage: Stage
    private var zoom: Double = 1.0 // TODO add a minus; leaving it out for now

    val OBJECT_COLOR: Color = Color.BLUE

    fun launch() {
        launch(FhysicsObjectDrawer::class.java)
    }

    override fun start(stage: Stage) {
        println("FhysicsFX")

        this.stage = stage

//        fhysics = FhysicsCore()

        setWindowSize(stage)
        zoom = calculateZoom(stage)

        val root = Group()
        val canvas = Canvas(stage.width, stage.height)
        root.children.add(canvas)

        val scene = Scene(root, stage.width, stage.height)
        stage.title = "Fhysics"
        stage.scene = scene
        stage.isResizable = false

        fhysics.drawer = this
        fhysics.startUpdateLoop()

        canvas.setOnScroll { onMouseWheel(it.deltaY) }
        canvas.setOnMousePressed { onMousePressed(Vector2(it.x, it.y)) }
        canvas.setOnKeyPressed { keyPressed(it.text[0]) }

        stage.show()
    }

    fun drawFrame() {
    }

    private fun setWindowSize(stage: Stage) {
        val insets = Insets(31, 8, 8, 8) // these will be the values
        val border: Rectangle2D = FhysicsCore.BORDER
        val borderWidth: Double = border.width
        val borderHeight: Double = border.height

        val ratio: Double = borderHeight / borderWidth
        val maxWidth = 1440.0
        val maxHeight = 960.0

        var windowWidth: Double = maxWidth
        var windowHeight: Double = (windowWidth * ratio + insets.top)

        if (windowHeight > maxHeight) {
            windowHeight = maxHeight
            windowWidth = ((windowHeight - insets.top) / ratio).toInt().toDouble()
        }

        stage.width = windowWidth
        stage.height = windowHeight
    }

    private fun calculateZoom(stage: Stage): Double {
        val border: Rectangle2D = FhysicsCore.BORDER
        val borderWidth: Double = border.width
        val windowWidth: Double = stage.width - (8 + 8) // -(insets.left[8] + insets.right[8])
        return windowWidth / borderWidth
    }

    private fun onMouseWheel(delta: Double) {
        TODO("Not yet implemented")
//        zoom -= delta * 0.2
//        drawFrame()
    }

    private fun onMousePressed(mousePos: Vector2) {
        TODO("Not yet implemented")
//        val transformedMousePos = mousePos / zoom
//        fhysics.fhysicsObjects.add(Circle(transformedMousePos, 1.0))
//        drawFrame()
    }

    private fun keyPressed(pressedChar: Char) {
        // if pressed char is p toggle isRunning in FhysicsCore
        // if it is Enter or space call the update function
        when (pressedChar) {
            'p' -> fhysics.isRunning = !fhysics.isRunning
            ' ' -> fhysics.update() // space
            '\n' -> fhysics.update() // enter
        }
    }
}

