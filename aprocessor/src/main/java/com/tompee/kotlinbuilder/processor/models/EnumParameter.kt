package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.tompee.kotlinbuilder.annotations.EnumPosition
import com.tompee.kotlinbuilder.annotations.Optional
import com.tompee.kotlinbuilder.processor.extensions.wrapProof

/**
 * Represents an optional enum parameter
 */
internal data class EnumParameter(
    override val info: ParameterInfo,
    val position: EnumPosition
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
        return ParameterSpec.builder(name, info.spec.type, KModifier.PRIVATE).build()
    }

    /**
     * Builds a constructor parameter spec
     */
    override fun toPropertySpec(): PropertySpec {
        return PropertySpec.builder(name, info.spec.type)
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
            if (position == EnumPosition.FIRST) "val $name = ${info.spec.type}.values()[0]"
            else "val $name = ${info.spec.type}.values()[${info.spec.type}.values().count() - 1]"
        return initializer.wrapProof()
    }
}