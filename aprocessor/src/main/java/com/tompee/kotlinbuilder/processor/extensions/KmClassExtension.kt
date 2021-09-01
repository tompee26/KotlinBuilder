package com.tompee.kotlinbuilder.processor.extensions

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.metadata.ImmutableKmType
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.isNullable
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import kotlinx.metadata.KmClassifier

/**
 * Builds a class name from a given KmType
 */
@KotlinPoetMetadataPreview
internal val ImmutableKmType.typeName: TypeName
    get() = (classifier as? KmClassifier.Class)?.name?.let { classifierName ->
        ClassInspectorUtil.createClassName(classifierName).let { className ->
            val args = arguments.map {
                it.type?.classifier to (it.type?.isNullable ?: false)
            }.map { (classifier, isNullable) ->
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

