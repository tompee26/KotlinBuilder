package com.tompee.kotlinbuilder.annotations

import com.tompee.kotlinbuilder.annotations.types.DefaultValueProvider
import kotlin.reflect.KClass

/**
 * Allows an optional parameter to provide an implementation of a default value provider.
 *
 * @property provider default value provider type
 */
annotation class Provider(val provider: KClass<out DefaultValueProvider<*>>)