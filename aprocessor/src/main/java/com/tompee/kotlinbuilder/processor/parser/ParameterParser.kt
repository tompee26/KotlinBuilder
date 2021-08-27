package com.tompee.kotlinbuilder.processor.parser

import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.kotlinbuilder.annotations.Optional
import com.tompee.kotlinbuilder.processor.KBuilderElement
import com.tompee.kotlinbuilder.processor.models.*
import com.tompee.kotlinbuilder.processor.processor.ProviderMap
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

@OptIn(KotlinPoetMetadataPreview::class)
internal class ParameterParser(private val providerFactory: ProviderParameter.Builder.Factory) {

    fun parse(kElement: KBuilderElement, providerMap: ProviderMap): List<Parameter> {
        val parameters = kElement.metadata.constructors.first()
            .valueParameters
        return parameters.mapIndexed { index, valueParameter ->
            val element = kElement.typeElement.findVariableElement(index, parameters.size)
            val parameterInfo =
                ParameterInfo(valueParameter, element, kElement.types)
            when {
                element.findAnnotation<Optional>() != null -> {
                    OptionalParameter.create(parameterInfo, providerMap)
                }
                element.findAnnotation<Optional.Default>() != null -> {
                    DefaultParameter.create(parameterInfo)
                }
                element.findAnnotation<Optional.Enumerable>() != null -> {
                    EnumParameter.create(parameterInfo)
                }
                element.findAnnotation<Optional.Nullable>() != null -> {
                    NullableParameter.create(parameterInfo)
                }
                element.findAnnotation<Optional.ValueProvider>() != null -> {
                    providerFactory.create(parameterInfo).build()
                }
                else -> {
                    MandatoryParameter.create(parameterInfo)
                }
            }
        }
    }

    private fun TypeElement.findVariableElement(index: Int, paramCount: Int): VariableElement {
        val constructors = enclosedElements
            .filter { it.kind == ElementKind.CONSTRUCTOR }
            .filterIsInstance<ExecutableElement>()
        // For now, we only count the number of arguments
        return constructors.first { it.parameters.size == paramCount }.parameters[index]
    }

    private inline fun <reified T : Annotation> VariableElement.findAnnotation(): T? {
        return getAnnotation(T::class.java)
    }
}