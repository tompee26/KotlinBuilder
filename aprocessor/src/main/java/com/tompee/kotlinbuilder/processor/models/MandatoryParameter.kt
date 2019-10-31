package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.tompee.kotlinbuilder.annotations.Setter

/**
 * Represents a mandatory parameter in the target class constructor.
 *
 * @property name actual parameter name
 * @property propertySpec property spec
 * @property setter optional setter name annotation
 */
internal data class MandatoryParameter(
    override val name: String,
    override val propertySpec: PropertySpec,
    override val setter: Setter?
) : Parameter(name, propertySpec, setter) {

    class Builder(
        name: String = "",
        propertySpec: PropertySpec? = null,
        setter: Setter? = null
    ) : Parameter.Builder(name, propertySpec, setter) {

        override fun build(): Parameter {
            return MandatoryParameter(name, propertySpec!!, setter)
        }
    }

    /**
     * Builds a constructor parameter spec
     */
    override fun toCtrParamSpec(): ParameterSpec {
        return ParameterSpec.builder(name, propertySpec.type, KModifier.PRIVATE).build()
    }

    /**
     * Builds a constructor parameter spec
     */
    override fun toPropertySpec(): PropertySpec {
        return PropertySpec.builder(name, propertySpec.type)
            .initializer(name)
            .mutable()
            .build()
    }

    /**
     * Builds an invoke method parameter spec
     */
    override fun toInvokeParamSpec(): ParameterSpec {
        return ParameterSpec.builder(name, propertySpec.type).build()
    }

    /**
     * Builds an invoke method initializer statement
     */
    override fun createInitializeStatement(): String {
        throw IllegalStateException("This should not be called")
    }
}