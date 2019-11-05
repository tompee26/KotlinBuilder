package com.tompee.kotlinbuilder.processor

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.tompee.kotlinbuilder.annotations.DefaultValueProvider
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

@KotlinPoetMetadataPreview
internal class ProviderMapBuilder(
    elements: List<Element>,
    private val env: ProcessingEnvironment
) {

    private val defaultProviderType = DefaultValueProvider::class.asClassName()
    private val typeMap = elements.filterIsInstance(TypeElement::class.java)
        .map { it to it.toTypeSpec(ElementsClassInspector.create(env.elementUtils, env.typeUtils)) }
        .toMap()

    fun getProviderMap(): Map<TypeName, TypeName> {
        return typeMap.mapNotNull { entry ->
            val interfaceType = entry.value.superinterfaces
                .map { it.key }
                .filterIsInstance<ParameterizedTypeName>()
                .find { it.rawType == defaultProviderType }
                ?: throw Throwable("${entry.value.name} is not a subclass of DefaultValueProvider")
            val providerType = interfaceType.typeArguments.firstOrNull()
                ?: throw Throwable("No type arguments in DefaultValueProvider")
            providerType to entry.key.asType().asTypeName()
        }.toMap()
    }
}