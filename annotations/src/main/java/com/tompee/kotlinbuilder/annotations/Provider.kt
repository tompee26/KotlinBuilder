package com.tompee.kotlinbuilder.annotations

/**
 * Default value provider. Optional values always require a default value.
 */
interface Provider<T> {

    /**
     * Returns the default value
     */
    fun get(): T
}