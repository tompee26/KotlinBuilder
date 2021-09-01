package com.tompee.kotlinbuilder.processor.parser

import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.kotlinbuilder.annotations.Optional
import com.tompee.kotlinbuilder.processor.extensions.metadata
import com.tompee.kotlinbuilder.processor.extensions.parseAnnotation
import com.tompee.kotlinbuilder.processor.models.*
import com.tompee.kotlinbuilder.processor.processor.ProviderMap
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

@KotlinPoetMetadataPreview
internal class ParameterParser(
    private val providerMap: ProviderMap,
    private val elements: Elements,
    private val types: Types
) {

    fun parse(
        element: TypeElement,
    ): List<Parameter> {
        val parameters = element.metadata.constructors.first()
            .valueParameters
        return parameters.mapIndexed { index, valueParameter ->
            val varElement = element.findVariableElement(index, parameters.size)
            val parameterInfo = ParameterInfo(valueParameter, varElement, types)
            when {
                varElement.parseAnnotation<Optional>() != null -> {
                    OptionalParameter.create(parameterInfo, providerMap)
                }
                varElement.parseAnnotation<Optional.Default>() != null -> {
                    DefaultParameter.create(parameterInfo)
                }
                varElement.parseAnnotation<Optional.Enumerable>() != null -> {
                    EnumParameter.create(parameterInfo)
                }
                varElement.parseAnnotation<Optional.Nullable>() != null -> {
                    NullableParameter.create(parameterInfo)
                }
                varElement.parseAnnotation<Optional.ValueProvider>() != null -> {
                    ProviderParameter.create(parameterInfo, elements, types)
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
}