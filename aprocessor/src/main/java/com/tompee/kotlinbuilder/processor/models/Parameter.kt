package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.*
import com.tompee.kotlinbuilder.annotations.Setter
import com.tompee.kotlinbuilder.processor.extensions.wrapProof

/**
 * A class that represents a constructor parameter
 *
 * @property name actual parameter name
 * @property propertySpec property spec
 * @property setter optional setter name annotation
 */
internal abstract class Parameter(
    open val name: String,
    open val propertySpec: PropertySpec,
    open val setter: Setter?
) {

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
        val name = setter?.name ?: name
        val providerParamType =
            LambdaTypeName.get(returnType = propertySpec.type)
        return FunSpec.builder(name)
            .addParameter(ParameterSpec.builder("provider", providerParamType).build())
            .returns(className)
            .addStatement("return apply { ${this@Parameter.name} = provider() }".wrapProof())
            .build()
    }

    /**
     * Checks if this parameter is an optional parameter
     */
    fun isOptional(): Boolean {
        return this !is MandatoryParameter
    }
}
