package com.tompee.kotlinbuilder.processor.extensions

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.metadata.*
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import javax.lang.model.element.Element
import javax.lang.model.util.Elements

/**
 * A map that caches resolved metadata converted to immutable KM class
 */
@KotlinPoetMetadataPreview
private val metadataPerType = mutableMapOf<Element, ImmutableKmClass>()

/**
 * Returns the Kotlin metadata associated to the element
 */
@KotlinPoetMetadataPreview
internal val Element.metadata: ImmutableKmClass
    get() = metadataPerType.getOrPut(this) {
        getAnnotation(Metadata::class.java).toImmutableKmClass()
    }

/**
 * Returns the element's class name
 */
@KotlinPoetMetadataPreview
internal val Element.className: ClassName
    get() = metadata.let { ClassInspectorUtil.createClassName(it.name) }

/**
 * Parses a given annotation from the element
 */
internal inline fun <reified T : Annotation> Element.parseAnnotation(): T? =
    getAnnotation(T::class.java)

/**
 * Returns the package name where the element belongs to
 */
internal fun Element.getPackageName(elements: Elements): String {
    return elements.getPackageOf(this).toString()
}

/**
 * Checks if this element is defined as internal
 */
@KotlinPoetMetadataPreview
internal val Element.isInternal: Boolean
    get() = try {
        metadata.isInternal
    } catch (e: Exception) {
        false
    }

/**
 * Checks if this element is defined as internal
 */
@KotlinPoetMetadataPreview
internal val Element.isObject: Boolean
    get() = try {
        metadata.isObject
    } catch (e: Exception) {
        false
    }

/**
 * Checks if this element is defined as private
 */
@KotlinPoetMetadataPreview
internal val Element.isPrivate: Boolean
    get() = try {
        metadata.isPrivate
    } catch (e: Exception) {
        false
    }