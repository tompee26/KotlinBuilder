package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.metadata.*
import com.tompee.kotlinbuilder.annotations.Setter
import com.tompee.kotlinbuilder.processor.extensions.metadata
import javax.lang.model.element.VariableElement
import javax.lang.model.util.Types

@OptIn(KotlinPoetMetadataPreview::class)
internal class ParameterInfo(
    private val value: ImmutableKmValueParameter,
    val varElement: VariableElement,
    val spec: PropertySpec,
    private val variableElement: VariableElement,
    private val types: Types
) {

    /**
     * Returns the setter annotation value if it exists
     */
    val setter: Setter? = variableElement.getAnnotation(Setter::class.java)

    /**
     * Returns the kotlin metadata if it exists
     */
    val metadata: ImmutableKmClass?
        get() {
            return try {
                types.asElement(variableElement.asType()).metadata.toImmutableKmClass()
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Parameter name
     */
    val name: String = value.name

    /**
     * True if nullable
     */
    val isNullable = value.type?.isNullable ?: false

    /**
     * True if enum
     */
    val isEnum = metadata?.isEnum ?: false
}