package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.tompee.kotlinbuilder.annotations.Setter

/**
 * Represents an optional nullable parameter in the target class constructor.
 *
 * @property name actual parameter name
 * @property typeName parameter type name
 * @property isNullable determines if the parameter type is nullable or not
 * @property setter optional setter name annotation
 */
internal data class NullableParameter(
    override val name: String,
    override val typeName: TypeName,
    override val isNullable: Boolean,
    override val setter: Setter?
) : Parameter(name, typeName, isNullable, setter) {

    class Builder(
        name: String = "",
        typeName: TypeName? = null,
        isNullable: Boolean = false,
        setter: Setter? = null
    ) : Parameter.Builder(name, typeName, isNullable, setter) {

        override fun build(): Parameter {
            return NullableParameter(name, typeName!!, isNullable, setter)
        }
    }

    /**
     * Builds a constructor parameter spec
     */
    override fun toCtrParamSpec(): ParameterSpec {
        return ParameterSpec.builder(name, typeName, KModifier.PRIVATE).build()
    }

    /**
     * Builds a constructor parameter spec
     */
    override fun toPropertySpec(): PropertySpec {
        return PropertySpec.builder(name, typeName)
            .initializer(name)
            .mutable()
            .build()
    }

    /**
     * Builds an invoke method parameter spec
     */
    override fun toInvokeParamSpec(): ParameterSpec {
        throw IllegalStateException("This should not be called")
    }

    /**
     * Builds an invoke method initializer statement
     */
    override fun createInitializeStatement(): String {
        return "val $name : $typeName = null"
    }
}