package de.officeryoda.fhysics.engine.util

import java.util.*

/**
 * A stopwatch that can be used to measure the time between two points in the code.
 * The stopwatch stores the last [cacheSize] durations and calculates the average duration.
 */
class Stopwatch(private val cacheSize: Int = 32) {

    private val updateDurations: ArrayDeque<Long> = ArrayDeque(cacheSize)
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
        updateDurations.addLast(duration)

        // Remove the first duration if the list is exceeding the max durations
        if (updateDurations.size > cacheSize) {
            updateDurations.removeFirst()
        }
    }

    /**
     * Returns the average duration of the last [cacheSize] durations in milliseconds.
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

    fun toRoundedString(decimalPlaces: Int = 2): String {
        return String.Companion.format(Locale.US, "%.${decimalPlaces}f", average())
    }
}