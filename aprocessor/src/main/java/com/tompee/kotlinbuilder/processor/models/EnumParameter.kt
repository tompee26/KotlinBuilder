package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.kotlinbuilder.annotations.EnumPosition
import com.tompee.kotlinbuilder.annotations.Optional
import com.tompee.kotlinbuilder.processor.extensions.wrapProof

/**
 * Represents an optional enum parameter
 */
@KotlinPoetMetadataPreview
internal class EnumParameter private constructor(
    override val info: ParameterInfo,
    private val position: EnumPosition
) : Parameter() {

    companion object {

        /**
         * Creates a new enum parameter
         */
        fun create(info: ParameterInfo): EnumParameter {
            if (!info.isEnum) throw Throwable("Parameter ${info.name} type is not enum")
            val enumerable = info.varElement.getAnnotation(Optional.Enumerable::class.java)
            return EnumParameter(info, enumerable?.position ?: EnumPosition.FIRST)
        }
    }

    /**
     * Builds a constructor parameter spec
     */
    override fun toCtrParamSpec(): ParameterSpec {
        return ParameterSpec.builder(name, info.typeName, KModifier.PRIVATE).build()
    }

    /**
     * Builds a constructor parameter spec
     */
    override fun toPropertySpec(): PropertySpec {
        return PropertySpec.builder(name, info.typeName)
            .initializer(name)
            .mutable()
            .build()
    }

    /**
     * Builds an invoke method parameter spec
     */
    override fun toInvokeParamSpec(): ParameterSpec {
        throw IllegalStateException("Internal error. This should not be called")
    }

    /**
     * Builds an invoke method initializer statement
     */
    override fun createInitializeStatement(): String {
        val initializer =
            if (position == EnumPosition.FIRST) "val $name = ${info.typeName}.values()[0]"
            else "val $name = ${info.typeName}.values()[${info.typeName}.values().count() - 1]"
        return initializer.wrapProof()
    }
}