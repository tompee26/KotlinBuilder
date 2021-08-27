package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.tompee.kotlinbuilder.processor.extensions.wrapProof

/**
 * Represents an optional nullable parameter in the target class constructor.
 */
internal data class NullableParameter(override val info: ParameterInfo) : Parameter() {

    companion object {

        /**
         * Creates a new nullable parameter
         */
        fun create(info: ParameterInfo): NullableParameter {
            if (!info.isNullable) {
                throw Throwable("Parameter ${info.name} is annotated with @Optional.Nullable but its type is not nullable. Actual type is ${info.typeName}")
            }
            return NullableParameter(info)
        }
    }

    /**
     * Builds a constructor parameter spec
     */
    override fun toCtrParamSpec(): ParameterSpec {
        return ParameterSpec.builder(info.name, info.typeName, KModifier.PRIVATE).build()
    }

    /**
     * Builds a constructor parameter spec
     */
    override fun toPropertySpec(): PropertySpec {
        return PropertySpec.builder(info.name, info.typeName)
            .initializer(info.name)
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
        return "val ${info.name} : ${info.typeName} = null".wrapProof()
    }
}