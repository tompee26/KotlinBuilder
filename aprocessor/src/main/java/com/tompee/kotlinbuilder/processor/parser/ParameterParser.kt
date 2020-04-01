package com.tompee.kotlinbuilder.processor.parser

import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.kotlinbuilder.annotations.Optional
import com.tompee.kotlinbuilder.annotations.Setter
import com.tompee.kotlinbuilder.processor.models.*
import com.tompee.kotlinbuilder.processor.processor.ProviderMap
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

@KotlinPoetMetadataPreview
internal class ParameterParser(private val providerFactory: ProviderParameter.Builder.Factory) {

    fun parse(element: TypeElement, typeSpec: TypeSpec, providerMap: ProviderMap): List<Parameter> {
        val kotlinCtr = typeSpec.primaryConstructor
            ?: throw Throwable("No kotlin primary constructor defined")
        val javaCtr = element.enclosedElements
            .firstOrNull { it.kind == ElementKind.CONSTRUCTOR } as? ExecutableElement
            ?: throw Throwable("No java constructor found.")

        return kotlinCtr.parameters.zip(javaCtr.parameters) { kParam, jParam ->
            val setter = jParam.getAnnotation(Setter::class.java)
            val propertySpec = typeSpec.propertySpecs.find { it.name == kParam.name }!!

            when {
                jParam.getAnnotation(Optional::class.java) != null -> {
                    OptionalParameter.create(
                        element = jParam,
                        providerMap = providerMap,
                        name = kParam.name,
                        propertySpec = propertySpec,
                        setter = setter
                    )
                }
                jParam.getAnnotation(Optional.Default::class.java) != null -> {
                    DefaultParameter.create(
                        name = kParam.name,
                        propertySpec = propertySpec,
                        setter = setter
                    )
                }
                jParam.getAnnotation(Optional.Enumerable::class.java) != null -> {
                    EnumParameter.create(
                        element = jParam,
                        name = kParam.name,
                        propertySpec = propertySpec,
                        setter = setter
                    )
                }
                jParam.getAnnotation(Optional.Nullable::class.java) != null -> {
                    NullableParameter.create(
                        name = kParam.name,
                        propertySpec = propertySpec,
                        setter = setter
                    )
                }
                jParam.getAnnotation(Optional.ValueProvider::class.java) != null -> {
                    providerFactory.create(
                        element = jParam,
                        name = kParam.name,
                        propertySpec = propertySpec,
                        setter = setter
                    ).build()
                }
                else -> MandatoryParameter.create(
                    name = kParam.name,
                    propertySpec = propertySpec,
                    setter = setter
                )
            }
        }
    }
}