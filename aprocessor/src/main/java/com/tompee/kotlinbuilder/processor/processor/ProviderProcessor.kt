package com.tompee.kotlinbuilder.processor.processor

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.tompee.kotlinbuilder.annotations.DefaultValueProvider
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

internal typealias ProviderMap = Map<TypeName, TypeElement>

@KotlinPoetMetadataPreview
internal class ProviderProcessor(elements: Elements, types: Types) {

    companion object {

        fun getProviderType(element: TypeElement, inspector: ClassInspector): TypeName {
            return element.toTypeSpec(inspector).getInterfaceType().typeArguments.first()
        }

        fun TypeSpec.getInterfaceType(): ParameterizedTypeName {
            return superinterfaces
                .map { it.key }
                .filterIsInstance<ParameterizedTypeName>()
                .find { it.rawType == DefaultValueProvider::class.asClassName() }
                ?: throw Throwable("$name is not a subclass of DefaultValueProvider")
        }
    }

    private val inspector = ElementsClassInspector.create(elements, types)

    /**
     * Returns the provider mapping. The provider mapping is a pair of the value type and the
     * type of the provider class.
     */
    fun getProviderMap(elements: List<Element>): ProviderMap {
        return elements.filterIsInstance<TypeElement>()
            .map { element -> getProviderType(element, inspector) to element }
            .toMap()
    }
}