package com.tompee.kotlinbuilder.processor

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import com.tompee.kotlinbuilder.annotations.KBuilder
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

/**
 * Contains the type element properties
 */
@KotlinPoetMetadataPreview
internal class TypeElementProperties(
    val typeElement: TypeElement,
    val elements: Elements,
    val types: Types
) {
    val classInspector = ElementsClassInspector.create(elements, types)

    /**
     * Returns the [KBuilder] annotation instance tied to the type element
     */
    fun getBuilderAnnotation(): KBuilder {
        return typeElement.getAnnotation(KBuilder::class.java)
    }

    /**
     * Returns the type element simple name
     */
    fun getName(): String {
        return typeElement.simpleName.toString()
    }

    /**
     * Returns the package name of the type element
     */
    fun getPackageName(): String {
        return elements.getPackageOf(typeElement).toString()
    }

    /**
     * Returns the [TypeName]
     */
    fun getTypeName(): TypeName {
        return typeElement.asType().asTypeName()
    }

    /**
     * Returns the [TypeMirror]
     */
    fun getTypeMirror(): TypeMirror {
        return typeElement.asType()
    }

    /**
     * Returns the [TypeSpec]
     */
    fun getTypeSpec(): TypeSpec {
        return typeElement.toTypeSpec(classInspector)
    }

    /**
     * Returns the [ImmutableKmClass]
     */
    fun getKmClass(): ImmutableKmClass {
        return typeElement.toImmutableKmClass()
    }
}