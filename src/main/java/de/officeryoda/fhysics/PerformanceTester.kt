package de.officeryoda.fhysics

import de.officeryoda.fhysics.PerformanceTestScenario.Companion.idealPhysicsScenarioSetup
import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.FhysicsCore.spawn
import de.officeryoda.fhysics.engine.math.BoundingBox
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.engine.objects.FhysicsObjectFactory.randomCircle
import de.officeryoda.fhysics.engine.objects.FhysicsObjectFactory.randomRectangle
import de.officeryoda.fhysics.rendering.UIController
import kotlin.system.measureTimeMillis

private val scenarioList: List<PerformanceTestScenario> = listOf(
    idealPhysicsScenarioSetup(
        name = "1,000 Circles",
        objectCreation = { List(1_000) { spawn(randomCircle()).first() } },
    ),
    idealPhysicsScenarioSetup(
        name = "10,000 Circles",
        objectCreation = { List(10_000) { spawn(randomCircle()).first() } },
    ),
    idealPhysicsScenarioSetup(
        name = "100,000 Circles",
        objectCreation = { List(100_000) { spawn(randomCircle()).first() } },
        boundary = BoundingBox(0f, 0f, 320f, 320f),
    ),
    idealPhysicsScenarioSetup(
        name = "100 Rectangles",
        objectCreation = { List(100) { spawn(randomRectangle()).first() } },
    ),
    idealPhysicsScenarioSetup(
        name = "1,000 Rectangles",
        objectCreation = { List(1_000) { spawn(randomRectangle()).first() } },
        boundary = BoundingBox(0f, 0f, 320f, 320f),
    ),
//    idealPhysicsScenarioSetup(
//        name = "50 Polygons",
//        objectCreation = { List(50) { FhysicsObjectFactory.randomPolygon() } },
//    ),
//    idealPhysicsScenarioSetup(
//        name = "100 Polygons",
//        objectCreation = { List(100) { FhysicsObjectFactory.randomPolygon() } },
//    ),
)

fun main() {
    val results: List<PerformanceTestResult> = PerformanceTester.testPerformanceAverage(scenarioList)

    println("\n\n============Results============")
    for (result: PerformanceTestResult in results) {
        println("${result.scenario.name}: ${result.time}ms")
    }
}

private object PerformanceTester {

    fun testPerformance(
        scenarioList: List<PerformanceTestScenario>,
        iterations: Int = 1000,
    ): List<PerformanceTestResult> {
        val results: MutableList<PerformanceTestResult> = mutableListOf()

        for ((index: Int, scenario: PerformanceTestScenario) in scenarioList.withIndex()) {
            println("Running scenario ${index + 1}/${scenarioList.size}: ${scenario.name}")

            // Clear the simulation before each scenario
            FhysicsCore.clear()

            // Set up the scenario
            // Set up the object properties
            val objectProperties: PhysicalProperties = scenario.objectProperties
            scenario.objectCreation().forEach {
                it.apply {
                    restitution = objectProperties.restitution
                    frictionStatic = objectProperties.frictionStatic
                    frictionDynamic = objectProperties.frictionDynamic
                }
            }

            // Set up the border properties
            val borderProperties: PhysicalProperties = scenario.borderProperties
            UIController.setBorderProperties(
                restitution = borderProperties.restitution,
                frictionStatic = borderProperties.frictionStatic,
                frictionDynamic = borderProperties.frictionDynamic,
            )

            // Measure the time taken for the specified number of update cycles
            val timeTaken: Long = measureTimeMillis {
                repeat(iterations) {
                    FhysicsCore.update()
                }
            }

            results.add(PerformanceTestResult(scenario, timeTaken))
            println("Finished Scenario ${index + 1}/${scenarioList.size}: ${scenario.name} took $timeTaken ms")
        }

        return results
    }

    fun testPerformanceAverage(
        scenarioList: List<PerformanceTestScenario>,
        iterations: Int = 1000,
        runs: Int = 3,
    ): List<PerformanceTestResult> {
        val allResults: MutableList<PerformanceTestResult> = mutableListOf()

        repeat(runs) {
            println("\n\n============Run ${it + 1}/$runs============")
            allResults.addAll(testPerformance(scenarioList, iterations))
        }

        val averageResults: List<PerformanceTestResult> = allResults.groupBy { it.scenario.name }
            .map { (name: String, results: List<PerformanceTestResult>) ->
                val averageTime: Long = results.map { it.time }.average().toLong()
                PerformanceTestResult(
                    scenario = PerformanceTestScenario(name = name, objectCreation = { emptyList() }),
                    time = averageTime,
                )
            }

        return averageResults
    }
}

private data class PerformanceTestScenario(
    val name: String,
    val objectCreation: () -> List<FhysicsObject>,
    val boundary: BoundingBox = BoundingBox(0f, 0f, 100f, 100f),
    val objectProperties: PhysicalProperties = PhysicalProperties(),
    val borderProperties: PhysicalProperties = PhysicalProperties(),
) {
    companion object {
        fun idealPhysicsScenarioSetup(
            name: String,
            objectCreation: () -> List<FhysicsObject>,
            boundary: BoundingBox = BoundingBox(0f, 0f, 100f, 100f),
        ): PerformanceTestScenario {
            val bouncyProperties: PhysicalProperties =
                PhysicalProperties(restitution = 1f, frictionStatic = 0f, frictionDynamic = 0f)

            return PerformanceTestScenario(
                name = name,
                objectCreation = objectCreation,
                boundary = boundary,
                objectProperties = bouncyProperties,
                borderProperties = bouncyProperties,
            )
        }
    }
}

private data class PerformanceTestResult(
    val scenario: PerformanceTestScenario,
    val time: Long,
)

private data class PhysicalProperties(
    val restitution: Float = 1f,
    val frictionStatic: Float = 0f,
    val frictionDynamic: Float = 0f,
)
