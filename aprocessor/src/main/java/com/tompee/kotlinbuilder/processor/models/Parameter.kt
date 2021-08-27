package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.*
import com.tompee.kotlinbuilder.processor.extensions.wrapProof

/**
 * A class that represents a constructor parameter
 */
internal abstract class Parameter {

    /**
     * Parameter info
     */
    abstract val info: ParameterInfo

    /**
     * Parameter name
     */
    val name by lazy { info.name }

    /**
     * Builds a constructor parameter spec
     */
    abstract fun toCtrParamSpec(): ParameterSpec

    /**
     * Builds a constructor parameter spec
     */
    abstract fun toPropertySpec(): PropertySpec

    /**
     * Builds an invoke method parameter spec
     */
    abstract fun toInvokeParamSpec(): ParameterSpec

    /**
     * Builds an invoke method initializer statement
     */
    abstract fun createInitializeStatement(): String

    /**
     * Creates a variable that will shadow the global that will contain the non-null initializer
     */
    open fun toBuildInitializer(): String? = null

    /**
     * Generates the builder method
     */
    fun toBuilderFunSpec(className: ClassName): FunSpec {
        val name = info.setter?.name ?: info.name
        val providerParamType =
            LambdaTypeName.get(returnType = info.spec.type)
        return FunSpec.builder(name)
            .addParameter(ParameterSpec.builder("provider", providerParamType).build())
            .returns(className)
            .addStatement("return apply { ${info.name} = provider() }".wrapProof())
            .build()
    }

    /**
     * Checks if this parameter is an optional parameter
     */
    fun isOptional(): Boolean {
        return this !is MandatoryParameter
    }
}
