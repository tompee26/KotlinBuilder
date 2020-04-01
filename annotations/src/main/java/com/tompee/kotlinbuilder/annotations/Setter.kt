package com.tompee.kotlinbuilder.annotations

/**
 * Customizes the setter method name
 *
 * @property name setter name
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Setter(val name: String)