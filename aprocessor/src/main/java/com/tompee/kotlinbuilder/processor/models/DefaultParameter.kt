package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.kotlinbuilder.processor.extensions.wrapProof

/**
 * Represents an optional default parameter in the target class constructor.
 */
@KotlinPoetMetadataPreview
internal class DefaultParameter private constructor(override val info: ParameterInfo) :
    Parameter() {

    companion object {

        fun create(info: ParameterInfo): Parameter {
            return DefaultParameter(info)
        }
    }

    /**
     * Builds a constructor parameter spec
     */
    override fun toCtrParamSpec(): ParameterSpec {
        return ParameterSpec.builder(name, info.typeName.copy(true), KModifier.PRIVATE).build()
    }

    /**
     * Builds a constructor parameter spec
     */
    override fun toPropertySpec(): PropertySpec {
        return PropertySpec.builder(name, info.typeName.copy(true))
            .initializer(name)
            .mutable()
            .build()
    }

    /**
     * Builds an invoke method parameter spec
     */
    override fun toInvokeParamSpec(): ParameterSpec {
        throw Throwable("Internal error. This should not be called")
    }

    /**
     * Builds an invoke method initializer statement
     */
    override fun createInitializeStatement(): String {
        return "val $name : ${info.typeName.copy(true)} = null".wrapProof()
    }
}