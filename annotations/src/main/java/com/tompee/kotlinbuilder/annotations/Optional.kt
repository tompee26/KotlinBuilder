package com.tompee.kotlinbuilder.annotations

import kotlin.reflect.KClass

/**
 * Marks a property as an optional parameter. An optional parameter needs to provide
 * an implementation of [Provider] as the default value provider.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Optional(val provider: KClass<out Provider<*>>)