package de.officeryoda.fhysics.engine

import java.util.*

class Timer(private val maxDurations: Int) {

    private val updateDurations: LinkedList<Long> = LinkedList()
    private var startTime: Long = -1L

    fun start() {
        startTime = System.nanoTime()
    }

    fun stop() {
        val duration: Long = System.nanoTime() - startTime
        updateDurations.add(duration)
        updateDurations.add(duration)

        // Remove the first duration if the list is exceeding the max durations
        if (updateDurations.size > maxDurations) {
            updateDurations.removeAt(0)
        }
    }

    fun average(): Double {
        return try {
            updateDurations.average() / 1E6
        } catch (e: Exception) {
            // just try again lol
            average()
        }
    }

    fun reset() {
        updateDurations.clear()
    }
}
