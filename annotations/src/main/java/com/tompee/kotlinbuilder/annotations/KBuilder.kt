package com.tompee.kotlinbuilder.annotations

/**
 * Marks a class for builder generation. A custom builder name can be provided.
 * By default, the class name appended with Builder will be generated
 *
 * @property name optional builder name
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class KBuilder(val name: String = "")