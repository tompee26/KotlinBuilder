package com.tompee.kotlinbuilder.annotations.types

/**
 * Default value provider. Non-nullable optional values always require a default value.
 */
interface DefaultValueProvider<T> {

    /**
     * Returns the default value
     */
    fun get(): T
}