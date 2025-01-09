package de.officeryoda.fhysics.engine.util

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.FhysicsCore.spawn
import de.officeryoda.fhysics.engine.datastructures.BoundingBox
import de.officeryoda.fhysics.engine.datastructures.QuadTree
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.engine.objects.factories.FhysicsObjectFactory.randomCircle
import de.officeryoda.fhysics.engine.objects.factories.FhysicsObjectFactory.randomPolygon
import de.officeryoda.fhysics.engine.objects.factories.FhysicsObjectFactory.randomRectangle
import de.officeryoda.fhysics.engine.util.PerformanceTestScenario.Companion.idealPhysicsScenarioSetup
import de.officeryoda.fhysics.engine.util.PerformanceTestScenario.Companion.randomPhysicsScenarioSetup
import de.officeryoda.fhysics.visual.UIController
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

/**
 * A list of performance test scenarios for the Fhysics engine.
 */

private val scenarioListShort: List<PerformanceTestScenario> = listOf(
    idealPhysicsScenarioSetup(
        name = "1,000 Circles",
        objectCreation = { List(1_000) { randomCircle() } },
    ),
    idealPhysicsScenarioSetup(
        name = "10,000 Circles",
        objectCreation = { List(10_000) { randomCircle() } },
    ),
    idealPhysicsScenarioSetup(
        name = "100 Rectangles",
        objectCreation = { List(100) { randomRectangle() } },
    ),
    idealPhysicsScenarioSetup(
        name = "50 Polygons",
        objectCreation = { List(50) { randomPolygon() } },
    ),
)

private val scenarioListLong: List<PerformanceTestScenario> = listOf(
    idealPhysicsScenarioSetup(
        name = "1,000 Circles",
        objectCreation = { List(1_000) { randomCircle() } },
    ),
    idealPhysicsScenarioSetup(
        name = "10,000 Circles",
        objectCreation = { List(10_000) { randomCircle() } },
    ),
    idealPhysicsScenarioSetup(
        name = "100,000 Circles",
        objectCreation = { List(100_000) { randomCircle() } },
        boundary = BoundingBox(0f, 0f, 10000f, 10000f),
    ),
    idealPhysicsScenarioSetup(
        name = "50 Polygons",
        objectCreation = { List(50) { randomPolygon() } },
    ),
    idealPhysicsScenarioSetup(
        name = "100 Polygons",
        objectCreation = { List(100) { randomPolygon() } },
    ),
    randomPhysicsScenarioSetup(
        name = "Circle and Rectangle Mix",
        objectCreation = {
            val objects: MutableList<FhysicsObject> = mutableListOf()
            objects.addAll(List(1_000) { randomCircle() })
            objects.addAll(List(100) { randomRectangle() })
            objects
        },
    ),
    randomPhysicsScenarioSetup(
        name = "Circle and Polygon Mix",
        objectCreation = {
            val objects: MutableList<FhysicsObject> = mutableListOf()
            objects.addAll(List(1_000) { randomCircle() })
            objects.addAll(List(50) { randomPolygon() })
            objects
        },
    ),
    randomPhysicsScenarioSetup(
        name = "Circle, Rectangle and Polygon Mix",
        objectCreation = {
            val objects: MutableList<FhysicsObject> = mutableListOf()
            objects.addAll(List(1_000) { randomCircle() })
            objects.addAll(List(100) { randomRectangle() })
            objects.addAll(List(50) { randomPolygon() })
            objects
        },
        boundary = BoundingBox(0f, 0f, 160f, 160f),
    )
)

fun parseArgs(args: Array<String>): Triple<Int, Int, Int> {
    var scenarioList = 0 // 0 = short, 1 = long
    var iterations = 1000
    var runs = 5

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--list", "-l" -> {
                if (i + 1 < args.size) {
                    scenarioList = when {
                        args[i + 1] == "short" -> 0
                        args[i + 1] == "long" -> 1
                        else -> throw IllegalArgumentException("Invalid value for --list")
                    }
                    i++
                } else {
                    throw IllegalArgumentException("Missing value for --list")
                }
            }

            "--iterations", "-i" -> {
                if (i + 1 < args.size) {
                    iterations =
                        args[i + 1].toIntOrNull() ?: throw IllegalArgumentException("Invalid value for --iterations")
                    i++
                } else {
                    throw IllegalArgumentException("Missing value for --iterations")
                }
            }

            "--runs", "-r" -> {
                if (i + 1 < args.size) {
                    runs = args[i + 1].toIntOrNull() ?: throw IllegalArgumentException("Invalid value for --runs")
                    i++
                } else {
                    throw IllegalArgumentException("Missing value for --runs")
                }
            }

            else -> throw IllegalArgumentException("Unknown argument: ${args[i]}")
        }
        i++
    }

    return Triple(scenarioList, runs, iterations)
}

fun main(args: Array<String>) {
    val (scenarioListId: Int, runs: Int, iterations: Int) = parseArgs(args)
    val scenarioList: List<PerformanceTestScenario> = if (scenarioListId == 0) scenarioListShort else scenarioListLong

    val timestamp: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    val outputFile = File("output-$timestamp.txt")
    outputFile.appendText("Performance Test Results: $timestamp\n")

    val results: List<PerformanceTestResult> =
        PerformanceTester.testPerformanceAverage(scenarioList, outputFile, runs = runs, iterations = iterations)


    println("============Results============")
    outputFile.appendText("============Results============\n")
    for (result: PerformanceTestResult in results) {
        val resultString = "${result.scenario.name}: ${result.time}ms"
        println(resultString)
        outputFile.appendText("$resultString\n")
    }

    // Write the results to a file
    println("\nResults written to ${outputFile.absolutePath}")

    exitProcess(0)
}

private object PerformanceTester {

    fun testPerformanceAverage(
        scenarioList: List<PerformanceTestScenario>,
        outputFile: File,
        iterations: Int = 1000,
        runs: Int = 5,
    ): List<PerformanceTestResult> {
        val allResults: MutableList<PerformanceTestResult> = mutableListOf()

        // Run the performance test multiple times to get an average
        var startTime: Long
        repeat(runs) {
            startTime = System.currentTimeMillis()
            println("\n============Run ${it + 1}/$runs============")
            allResults.addAll(testPerformance(scenarioList, iterations))
            println("\nRun ${it + 1}/$runs took ${System.currentTimeMillis() - startTime} ms")
        }

        // Calculate the average time taken for each scenario
        val averageResults: List<PerformanceTestResult> =
            allResults
                .groupBy { it.scenario.name }
                .map { (name: String, results: List<PerformanceTestResult>) ->
                    val averageTime: Long = results.map { it.time }.average().toLong()
                    PerformanceTestResult(
                        scenario = PerformanceTestScenario(name = name, objectCreation = { emptyList() }),
                        time = averageTime,
                    )
                }

        println("\n\n============Average Results============")
        println("Average time per run: ${averageResults.sumOf { it.time }}ms")
        println("Average time taken for $iterations iterations over $runs runs:")
        outputFile.appendText("\n============Average Results============\n")
        outputFile.appendText("Average time per run: ${averageResults.sumOf { it.time }}ms\n")
        outputFile.appendText("Average time taken for $iterations iterations over $runs runs:\n")

        return averageResults
    }

    fun testPerformance(
        scenarioList: List<PerformanceTestScenario>,
        iterations: Int = 1000,
    ): List<PerformanceTestResult> {
        val results: MutableList<PerformanceTestResult> = mutableListOf()

        for ((index: Int, scenario: PerformanceTestScenario) in scenarioList.withIndex()) {
            println("Running scenario ${index + 1}/${scenarioList.size}: ${scenario.name}")

            // Clear the simulation before each scenario
            QuadTree.clearFlag = true
            QuadTree.processPendingOperations()

            // Set up the scenario
            // Set up the object properties
            val objectProperties: PhysicalProperties = scenario.objectProperties
            val objects: List<FhysicsObject> = scenario.objectCreation()
            for (it: FhysicsObject in objects) {
                it.apply {
                    restitution = objectProperties.restitution
                    frictionStatic = objectProperties.frictionStatic
                    frictionDynamic = objectProperties.frictionDynamic
                }

                spawn(it)
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
            println("Completed Scenario ${index + 1}/${scenarioList.size}: ${scenario.name} took $timeTaken ms")
        }

        return results
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
        /**
         * Creates a performance test scenario with ideal physics properties.
         * This includes, perfectly elastic collisions and no friction.
         *
         * @param name The name of the scenario.
         * @param objectCreation A function that creates the objects for the scenario.
         * @param boundary The boundary of the scenario.
         * @return A performance test scenario with ideal physics properties.
         */
        fun idealPhysicsScenarioSetup(
            name: String,
            objectCreation: () -> List<FhysicsObject>,
            boundary: BoundingBox = BoundingBox(0f, 0f, 100f, 100f),
        ): PerformanceTestScenario {
            val bouncyProperties =
                PhysicalProperties(restitution = 1f, frictionStatic = 0f, frictionDynamic = 0f)

            return PerformanceTestScenario(
                name = name,
                objectCreation = objectCreation,
                boundary = boundary,
                objectProperties = bouncyProperties,
                borderProperties = bouncyProperties,
            )
        }

        /**
         * Creates a performance test scenario with pseudo random physics properties.
         *
         * @param name The name of the scenario. This is used to seed the random number generator.
         * @param objectCreation A function that creates the objects for the scenario.
         * @param boundary The boundary of the scenario.
         * @return A performance test scenario with pseudo random physics properties.
         */
        fun randomPhysicsScenarioSetup(
            name: String,
            objectCreation: () -> List<FhysicsObject>,
            boundary: BoundingBox = BoundingBox(0f, 0f, 100f, 100f),
        ): PerformanceTestScenario {
            val random = Random(name.hashCode().toLong())

            val randomObjectProperties = PhysicalProperties(
                restitution = random.nextFloat(),
                frictionStatic = random.nextFloat(),
                frictionDynamic = random.nextFloat()
            )

            val randomBorderProperties = PhysicalProperties(
                restitution = random.nextFloat(),
                frictionStatic = random.nextFloat(),
                frictionDynamic = random.nextFloat()
            )

            return PerformanceTestScenario(
                name = name,
                objectCreation = objectCreation,
                boundary = boundary,
                objectProperties = randomObjectProperties,
                borderProperties = randomBorderProperties,
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
