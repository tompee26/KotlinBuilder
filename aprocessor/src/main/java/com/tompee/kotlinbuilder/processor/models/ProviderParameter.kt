package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.tompee.kotlinbuilder.annotations.DefaultValueProvider
import com.tompee.kotlinbuilder.annotations.Optional
import com.tompee.kotlinbuilder.annotations.Setter
import com.tompee.kotlinbuilder.processor.extensions.wrapProof
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror

/**
 * Represents an optional parameter with default value provider in the target class constructor.
 *
 * @property name actual parameter name
 * @property propertySpec property spec
 * @property setter optional setter name annotation
 * @property providerName providerName information
 */
internal data class ProviderParameter(
    override val name: String,
    override val propertySpec: PropertySpec,
    override val setter: Setter?,
    val providerName: Optional.ValueProvider
) : Parameter(name, propertySpec, setter) {

    companion object {

        private fun Optional.ValueProvider.getProvider(): TypeMirror {
            try {
                this.provider
            } catch (mte: MirroredTypeException) {
                return mte.typeMirror
            }
            throw Throwable("DefaultValueProvider type cannot be determined")
        }
    }

    @KotlinPoetMetadataPreview
    class Builder(
        private val providerName: Optional.ValueProvider,
        env: ProcessingEnvironment,
        name: String = "",
        propertySpec: PropertySpec? = null,
        setter: Setter? = null
    ) : Parameter.Builder(name, propertySpec, setter) {

        private val types = env.typeUtils
        private val classInspector = ElementsClassInspector.create(env.elementUtils, env.typeUtils)
        private val providerTypeClassName = DefaultValueProvider::class.asClassName()

        override fun build(): Parameter {
            val provider = providerName.getProvider()
            val typeSpec = (types.asElement(provider) as TypeElement).toTypeSpec(classInspector)
            val providerReturnType =
                typeSpec.superinterfaces.keys.filterIsInstance<ParameterizedTypeName>()
                    .find { it.rawType == providerTypeClassName }?.typeArguments?.first()
                    ?: throw Throwable("$provider is not a subtype of DefaultValueProvider")

            if (providerReturnType != propertySpec?.type) {
                throw Throwable("Parameter $name type (${propertySpec?.type}) is not the same as the ValueProvider type ($providerReturnType)")
            }
            return ProviderParameter(name, propertySpec!!, setter, providerName)
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
        throw IllegalStateException("Internal error. This should not be called")
    }

    /**
     * Builds an invoke method initializer statement
     */
    override fun createInitializeStatement(): String {
        val typeName = providerName.getProvider()
        return "val $name = $typeName().get()".wrapProof()
    }
}