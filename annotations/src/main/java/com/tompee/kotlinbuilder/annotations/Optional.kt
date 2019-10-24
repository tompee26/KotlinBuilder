package com.tompee.kotlinbuilder.annotations

/**
 * Marks a property as an optional parameter. A custom setter name can be provided and is
 * by default, the method name.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Optional(val name: String = "")