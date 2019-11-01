package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import com.tompee.kotlinbuilder.annotations.Optional
import com.tompee.kotlinbuilder.annotations.Setter
import com.tompee.kotlinbuilder.processor.extensions.wrapProof
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types
import kotlin.reflect.KClass

/**
 * Represents an optional parameter with default value provider in the target class constructor.
 *
 * @property name actual parameter name
 * @property propertySpec property spec
 * @property setter optional setter name annotation
 * @property valueProvider valueProvider information
 */
internal data class ProviderParameter(
    override val name: String,
    override val propertySpec: PropertySpec,
    override val setter: Setter?,
    val valueProvider: Optional.ValueProvider
) : Parameter(name, propertySpec, setter) {

    companion object {

        private fun Optional.ValueProvider.getProvider(): TypeMirror {
            try {
                this.provider
            } catch (mte: MirroredTypeException) {
                return mte.typeMirror
            }
            throw IllegalStateException("DefaultValueProvider type cannot be determined")
        }

        private fun KClass<*>.getParameters(): TypeMirror {
            try {
                this.typeParameters
            } catch (mte: MirroredTypeException) {
                return mte.typeMirror
            }
            throw IllegalStateException("DefaultValueProvider type cannot be determined")
        }
    }

    @KotlinPoetMetadataPreview
    class Builder(
        private val valueProvider: Optional.ValueProvider,
        private val types: Types,
        name: String = "",
        propertySpec: PropertySpec? = null,
        setter: Setter? = null
    ) : Parameter.Builder(name, propertySpec, setter) {

        override fun build(): Parameter {
            val provider = valueProvider.getProvider()
            val typeSpec =
                (types.asElement(provider) as TypeElement).toImmutableKmClass().toTypeSpec(null)
            val providerReturnType = typeSpec.funSpecs.find { it.name == "get" }?.returnType
                ?: throw Throwable("Value provider type not found")
            if (providerReturnType != propertySpec?.type) {
                throw Throwable("Parameter $name type is not the same as the ValueProvider type ")
            }
            return ProviderParameter(name, propertySpec!!, setter, valueProvider)
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
        throw IllegalStateException("This should not be called")
    }

    /**
     * Builds an invoke method initializer statement
     */
    override fun createInitializeStatement(): String {
        val typeName = valueProvider.getProvider()
        return "val $name = $typeName().get()".wrapProof()
    }
}