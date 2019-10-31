package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.*
import com.tompee.kotlinbuilder.annotations.*
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

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
     * Base parameter builder class
     */
    abstract class Builder(
        var name: String,
        var propertySpec: PropertySpec?,
        var setter: Setter?
    ) {
        abstract fun build(): Parameter
    }

    companion object {

        /**
         * Parses the list of parameters from a type element
         *
         * @param element builder class type element
         * @param typeSpec builder class type spec
         */
        fun parse(element: TypeElement, typeSpec: TypeSpec): List<Parameter> {
            val ctr = element.enclosedElements
                .firstOrNull { it.kind == ElementKind.CONSTRUCTOR } as? ExecutableElement
                ?: throw IllegalStateException("No constructor found for ${element.simpleName}")

            /**
             * Now we use the java constructor to check for annotations
             */
            val builders = ctr.parameters.map {
                if (it.getAnnotation(Optional::class.java) == null) {
                    MandatoryParameter.Builder()
                } else {
                    when {
                        it.getAnnotation(Nullable::class.java) != null -> NullableParameter.Builder()
                        it.getAnnotation(Default::class.java) != null -> DefaultParameter.Builder()
                        it.getAnnotation(ValueProvider::class.java) != null -> ProviderParameter.Builder(
                            it.getAnnotation(ValueProvider::class.java)
                        )
                        else -> throw IllegalStateException("Unsupported optional parameter: ${it.simpleName}")
                    }
                }.apply {
                    name = it.simpleName.toString()
                    setter = it.getAnnotation(Setter::class.java)
                }
            }

            /**
             * Now we will need the actual kotlin type of the parameter
             */
            typeSpec.propertySpecs.forEach { propertySpec ->
                builders.find { it.name == propertySpec.name }
                    ?.apply {
                        this.propertySpec = propertySpec
                    }
            }
            return builders.map { it.build() }
        }
    }

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
     * Generates the builder method
     */
    fun toBuilderFunSpec(className: ClassName): FunSpec {
        val name = setter?.name ?: name
        val providerParamType =
            LambdaTypeName.get(className, returnType = propertySpec.type)
        return FunSpec.builder(name)
            .addParameter(ParameterSpec.builder("provider", providerParamType).build())
            .returns(className)
            .addStatement("${this@Parameter.name} = provider()")
            .addStatement("return this")
            .build()
    }

    /**
     * Checks if this parameter is an optional parameter
     */
    fun isOptional(): Boolean {
        return this !is MandatoryParameter
    }
}
