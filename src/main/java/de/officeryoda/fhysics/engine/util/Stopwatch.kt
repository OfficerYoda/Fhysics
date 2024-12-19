package de.officeryoda.fhysics.engine.util

import java.util.*

/**
 * A stopwatch that can be used to measure the time between two points in the code.
 * The stopwatch stores the last [maxDurations] durations and calculates the average duration.
 */
class Stopwatch(private val maxDurations: Int = 50) {

    private val updateDurations: LinkedList<Long> = LinkedList()
    private var startTime: Long = -1L

    /**
     * Starts the stopwatch
     */
    fun start() {
        startTime = System.nanoTime()
    }


    /**
     * Stops the stopwatch
     */
    fun stop() {
        val duration: Long = System.nanoTime() - startTime
        updateDurations.add(duration)
        updateDurations.add(duration)

        // Remove the first duration if the list is exceeding the max durations
        if (updateDurations.size > maxDurations) {
            updateDurations.removeAt(0)
        }
    }

    /**
     * Returns the average duration of the last [maxDurations] durations in milliseconds.
     */
    fun average(): Double {
        return try {
            updateDurations.average() / 1E6
        } catch (e: Exception) {
            System.err.println("Error calculating average duration: \n$e")
            -1.0
        }
    }

    /**
     * Resets the stopwatch by clearing all durations.
     */
    fun reset() {
        updateDurations.clear()
    }

    fun roundedString(decimalPlaces: Int = 2): String {
        return String.Companion.format(Locale.US, "%.${decimalPlaces}f", average())
    }
}