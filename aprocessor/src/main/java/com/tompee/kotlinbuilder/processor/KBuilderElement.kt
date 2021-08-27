package com.tompee.kotlinbuilder.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import com.tompee.kotlinbuilder.annotations.KBuilder
import com.tompee.kotlinbuilder.processor.extensions.className
import com.tompee.kotlinbuilder.processor.extensions.metadata
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

/**
 * Contains the type element properties
 */
@OptIn(KotlinPoetMetadataPreview::class)
internal class KBuilderElement(
    val typeElement: TypeElement,
    elements: Elements,
    val types: Types
) {
    /**
     * Returns the builder annotation information
     */
    private val builderAnnotation: KBuilder = typeElement.getAnnotation(KBuilder::class.java)

    /**
     * Returns the package name of the type element
     */
    val packageName: String = elements.getPackageOf(typeElement).toString()

    /**
     * Returns the type element's simple name
     */
    val name: String = typeElement.simpleName.toString()

    /**
     * Generates a builder class name
     */
    val builderClassName: ClassName = builderAnnotation.name.let {
        val builderName = if (it.isEmpty()) "${name}Builder" else it
        ClassName(packageName, builderName)
    }

    /**
     * Returns the class inspector
     */
    val classInspector = ElementsClassInspector.create(elements, types)

    /**
     * Returns the kotlin metadata
     */
    val metadata: ImmutableKmClass = typeElement.metadata.toImmutableKmClass()

    /**
     * Returns the [TypeName]
     */
    val className: TypeName = typeElement.className

    /**
     * Returns the [TypeSpec]
     */
    val typeSpec: TypeSpec = typeElement.toTypeSpec(classInspector)

    /**
     * Returns true if this element is declared as private
     */
    val isPrivate: Boolean = typeSpec.modifiers.any { it == KModifier.PRIVATE }

    /**
     * Returns true if this element is declared as internal
     */
    val isInternal: Boolean = typeSpec.modifiers.any { it == KModifier.INTERNAL }
}