package de.officeryoda.fhysics

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.objects.Circle
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer
import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.stage.Stage
import java.awt.Color

//fun main() {
//    println("Fhysics")
//
//    val fhysics: FhysicsCore = FhysicsCore()
//
//    SwingUtilities.invokeLater {
//        val drawer: FhysicsObjectDrawer = FhysicsObjectDrawer(fhysics)
//        fhysics.drawer = drawer
//        fhysics.startUpdateLoop()
//    }
//}

class Main {

    private lateinit var fhysics: FhysicsCore

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            FhysicsObjectDrawer().launch()
        }

        const val WIDTH = 1440.0
        const val HEIGHT = 960.0
        val OBJECT_COLOR: Color = Color.BLUE
    }
//
//    override fun start(primaryStage: Stage) {
//        println("Fhysics")
//
//        fhysics = FhysicsCore()
//
//        val root = Group()
//        val canvas = Canvas(WIDTH, HEIGHT)
//        root.children.add(canvas)
//
//        val scene = Scene(root, WIDTH, HEIGHT)
//        primaryStage.title = "Fhysics"
//        primaryStage.scene = scene
//        primaryStage.isResizable = false
//
//        val drawer = FhysicsObjectDrawer(fhysics, canvas.graphicsContext2D)
//        fhysics.drawer = drawer
//        fhysics.startUpdateLoop()
//
//        canvas.setOnScroll { event -> drawer.onMouseWheel(event.deltaY) }
//        canvas.setOnMousePressed { event -> drawer.onMousePressed(Vector2(event.x, event.y)) }
//
//        primaryStage.show()
//    }
}