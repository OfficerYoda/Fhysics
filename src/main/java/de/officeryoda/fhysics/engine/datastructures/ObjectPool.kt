package de.officeryoda.fhysics.engine.datastructures

import java.util.*

class ObjectPool<T> {

    private val pool: Stack<T> = Stack()

    @Synchronized
    fun borrowObject(): T? {
        return pool.takeIf { it.isNotEmpty() }?.pop()
    }

    @Synchronized
    fun returnObject(instance: T) {
        pool.push(instance)
    }
}