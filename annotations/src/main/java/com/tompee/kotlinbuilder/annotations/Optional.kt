package com.tompee.kotlinbuilder.annotations

import kotlin.reflect.KClass

/**
 * Marks a property as an optional parameter. Optional parameters are parameters whose values
 * are not required to be set when instantiating the builder. Optional parameters have many types
 * and may require additional annotations to deduce the method of specifying the default value.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Optional {

    /**
     * Allows an optional parameter to explicitly inform that it has an initializer
     * Current support for default is limited as default parameters cannot be reliably parsed during
     * annotation processing. For now, it is imperative that the user should ensure that a default
     * parameter actually has a default value.
     */
    @Retention(AnnotationRetention.SOURCE)
    @Target(AnnotationTarget.VALUE_PARAMETER)
    annotation class Default

    /**
     * Allows an optional parameter to explicitly inform that it is a nullable type and therefore
     * set the default value to null
     */
    @Retention(AnnotationRetention.SOURCE)
    @Target(AnnotationTarget.VALUE_PARAMETER)
    annotation class Nullable

    /**
     * Allows an optional parameter to provide an implementation of a default value provider.
     *
     * @property provider default value provider type
     */
    @Retention(AnnotationRetention.SOURCE)
    @Target(AnnotationTarget.VALUE_PARAMETER)
    annotation class ValueProvider(val provider: KClass<out DefaultValueProvider<*>>)

    /**
     * Allows an optional enum parameter to provide the order of the default enum value
     */
    @Retention(AnnotationRetention.SOURCE)
    @Target(AnnotationTarget.VALUE_PARAMETER)
    annotation class Enumerable(val position: EnumPosition = EnumPosition.FIRST)

    /**
     * Annotates implementation classes of [DefaultValueProvider] to create a default value binding.
     *
     * The implementation class must have a no-arg constructor since the builder will instantiate it.
     */
    @Retention(AnnotationRetention.SOURCE)
    @Target(AnnotationTarget.CLASS)
    annotation class Provides
}