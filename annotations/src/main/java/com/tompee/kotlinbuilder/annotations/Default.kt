package com.tompee.kotlinbuilder.annotations

/**
 * Allows an optional parameter to explicitly inform that it has an initializer
 * Current support for default is limited as default parameters cannot be reliably parsed during
 * annotation processing. For now, it is imperative that the user should ensure that a default
 * parameter actually has a default value.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Default