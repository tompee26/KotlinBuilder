package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.tompee.kotlinbuilder.annotations.Provider
import com.tompee.kotlinbuilder.annotations.Setter
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror

/**
 * Represents an optional parameter with default value provider in the target class constructor.
 *
 * @property name actual parameter name
 * @property typeName parameter type name
 * @property isNullable determines if the parameter type is nullable or not
 * @property setter optional setter name annotation
 * @property provider provider information
 */
internal data class ProviderParameter(
    override val name: String,
    override val typeName: TypeName,
    override val isNullable: Boolean,
    override val setter: Setter?,
    val provider: Provider
) : Parameter(name, typeName, isNullable, setter) {

    class Builder(
        private val provider: Provider,
        name: String = "",
        typeName: TypeName? = null,
        isNullable: Boolean = false,
        setter: Setter? = null
    ) : Parameter.Builder(name, typeName, isNullable, setter) {

        override fun build(): Parameter {
            return ProviderParameter(name, typeName!!, isNullable, setter, provider)
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
        val typeName = getProvider()
        return "val $name = $typeName().get()"
    }

    private fun getProvider(): TypeMirror {
        try {
            provider.provider
        } catch (mte: MirroredTypeException) {
            return mte.typeMirror
        }

        throw IllegalStateException("DefaultValueProvider type cannot be determined")
    }
}