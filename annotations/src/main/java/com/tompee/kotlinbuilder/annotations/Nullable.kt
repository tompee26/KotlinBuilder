package com.tompee.kotlinbuilder.annotations

/**
 * Allows an optional parameter to explicitly inform that it is a nullable type and therefore
 * set the default value to null
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Nullable