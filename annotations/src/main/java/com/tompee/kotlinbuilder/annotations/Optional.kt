package com.tompee.kotlinbuilder.annotations

/**
 * Marks a property as an optional parameter. Optional parameters are parameters whose values
 * are not required to be set when instantiating the builder. Optional parameters have many types
 * and may require additional annotations to deduce the method of specifying the default value.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Optional