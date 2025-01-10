package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.engine.datastructures.QuadTree
import de.officeryoda.fhysics.engine.objects.Circle
import de.officeryoda.fhysics.engine.objects.Polygon
import de.officeryoda.fhysics.engine.objects.Rectangle
import de.officeryoda.fhysics.engine.objects.factories.FhysicsObjectFactory

object SceneManager {

    val scenes: MutableList<Scene> = mutableListOf()

    /** The name of the scene to load */
    private var loadSceneName: String? = null

    init {
        createScenes()
    }

    /**
     * Loads a scene by its name.
     */
    fun loadScene(sceneName: String?) {
        loadSceneName = sceneName?.split("_")?.first()
    }

    /**
     * Loads the scene that is queued to be loaded.
     *
     * This method exists to prevent concurrent modification exceptions.
     */
    fun loadPendingScene() {
        if (loadSceneName == null) return

        scenes.find { it.name == loadSceneName }?.load()
        loadSceneName = ""
    }

    fun clearScene() {
        QuadTree.clearFlag = true
    }

    /**
     * Creates the custom scenes to be loaded through the UI.
     */
    private fun createScenes() {
        scenes.add(
            Scene(
                "1000 Circles (perfect)",
                mapOf(
                    "borderRestitution" to 1f,
                    "borderFrictionStatic" to 0f,
                    "borderFrictionDynamic" to 0f,
                    "showMSPU" to true,
                    "showObjectCount" to true,
                )
            ) {
                repeat(1000) {
                    val circle: Circle = FhysicsObjectFactory.randomCircle().apply {
                        restitution = 1f
                        frictionStatic = 0f
                        frictionDynamic = 0f
                    }
                    FhysicsCore.spawn(circle)
                }
            }
        )

        scenes.add(
            Scene(
                "200 Rectangles (perfect)",
                mapOf(
                    "borderRestitution" to 1f,
                    "borderFrictionStatic" to 0f,
                    "borderFrictionDynamic" to 0f,
                    "showMSPU" to true,
                    "showObjectCount" to true,
                )
            ) {
                repeat(200) {
                    val rectangle: Rectangle = FhysicsObjectFactory.randomRectangle().apply {
                        restitution = 1f
                        frictionStatic = 0f
                        frictionDynamic = 0f
                    }
                    FhysicsCore.spawn(rectangle)
                }
            }
        )

        scenes.add(
            Scene(
                "100 Polygons (perfect)",
                mapOf(
                    "borderRestitution" to 1f,
                    "borderFrictionStatic" to 0f,
                    "borderFrictionDynamic" to 0f,
                    "showMSPU" to true,
                    "showObjectCount" to true,
                )
            ) {
                repeat(100) {
                    val polygon: Polygon = FhysicsObjectFactory.randomPolygon().apply {
                        restitution = 1f
                        frictionStatic = 0f
                        frictionDynamic = 0f
                    }
                    FhysicsCore.spawn(polygon)
                }
            }
        )
    }
}

data class Scene(
    /** The name shown in the UI */
    val name: String,
    /** The settings that differ from the default settings */
    val settings: Map<String, Any>,
    /** The object creation function */
    val objectCreation: () -> Unit,
) {
    fun load() {
        Settings.loadDefault()
        Settings.load(settings)

        objectCreation()
    }
}
