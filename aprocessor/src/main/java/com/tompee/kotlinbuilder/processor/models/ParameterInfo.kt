package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.metadata.*
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import com.tompee.kotlinbuilder.annotations.Setter
import com.tompee.kotlinbuilder.processor.extensions.metadata
import kotlinx.metadata.KmClassifier
import javax.lang.model.element.VariableElement
import javax.lang.model.util.Types

@OptIn(KotlinPoetMetadataPreview::class)
internal class ParameterInfo(
    value: ImmutableKmValueParameter,
    val varElement: VariableElement,
    private val types: Types
) {

    /**
     * Returns the setter annotation value if it exists
     */
    val setter: Setter? = varElement.getAnnotation(Setter::class.java)

    /**
     * Returns the kotlin metadata if it exists
     */
    val metadata: ImmutableKmClass?
        get() {
            return try {
                types.asElement(varElement.asType()).metadata.toImmutableKmClass()
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

    /**
     * Type name
     */
    val typeName: TypeName =
        (value.type?.classifier as? KmClassifier.Class)?.name?.let { classifierName ->
            ClassInspectorUtil.createClassName(classifierName).let { className ->
                val args = (value.type?.arguments?.map {
                    it.type?.classifier to (it.type?.isNullable ?: false)
                } ?: emptyList())
                    .map { (classifier, isNullable) ->
                        ClassInspectorUtil.createClassName((classifier as KmClassifier.Class).name)
                            .copy(isNullable)
                    }
                if (args.isNotEmpty()) {
                    className.parameterizedBy(args)
                } else {
                    className
                }
            }.copy(isNullable)
        } ?: throw IllegalStateException("Type cannot be inferred")
}