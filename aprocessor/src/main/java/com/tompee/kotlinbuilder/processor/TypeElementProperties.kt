package com.tompee.kotlinbuilder.processor

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import com.tompee.kotlinbuilder.annotations.KBuilder
import com.tompee.kotlinbuilder.processor.extensions.metadata
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

/**
 * Contains the type element properties
 */
@KotlinPoetMetadataPreview
internal class TypeElementProperties(
    val typeElement: TypeElement,
    elements: Elements,
    val types: Types
) {

    /**
     * Returns the class inspector
     */
    val classInspector = ElementsClassInspector.create(elements, types)

    /**
     * Returns the builder annotation information
     */
    val builderAnnotation: KBuilder = typeElement.getAnnotation(KBuilder::class.java)

    /**
     * Returns the type element's simple name
     */
    val name: String = typeElement.simpleName.toString()

    /**
     * Returns the package name of the type element
     */
    val packageName: String = elements.getPackageOf(typeElement).toString()

    /**
     * Returns the [TypeName]
     */
    val className: TypeName = typeElement.metadata.toImmutableKmClass().let {
        ClassInspectorUtil.createClassName(it.name)
    }

    /**
     * Returns the [TypeSpec]
     */
    val typeSpec: TypeSpec = typeElement.toTypeSpec(classInspector)
}