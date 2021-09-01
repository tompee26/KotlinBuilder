package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.kotlinbuilder.annotations.Optional
import com.tompee.kotlinbuilder.processor.ProcessorException
import com.tompee.kotlinbuilder.processor.extensions.className
import com.tompee.kotlinbuilder.processor.extensions.isObject
import com.tompee.kotlinbuilder.processor.extensions.parseAnnotation
import com.tompee.kotlinbuilder.processor.extensions.wrapProof
import com.tompee.kotlinbuilder.processor.processor.ProviderProcessor
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

/**
 * Represents an optional parameter with default value provider in the target class constructor.
 */
@KotlinPoetMetadataPreview
internal class ProviderParameter private constructor(
    override val info: ParameterInfo,
    private val providerName: TypeName,
    private val isStatic: Boolean = false
) : Parameter() {

    companion object {

        /**
         * Creates a ProviderParameter deriving the value from the provider map
         */
        fun create(
            info: ParameterInfo,
            childTypeName: TypeName,
            isStatic: Boolean
        ): ProviderParameter {
            return ProviderParameter(info, childTypeName, isStatic)
        }

        /**
         * Creates a ProviderParameter deriving the value from the annotation
         */
        fun create(info: ParameterInfo, elements: Elements, types: Types): ProviderParameter {
            val inspector = ElementsClassInspector.create(elements, types)
            val element = info.varElement.parseAnnotation<Optional.ValueProvider>()?.getProvider()
                ?.let { types.asElement(it) as? TypeElement }
                ?: throw ProcessorException(
                    info.varElement,
                    "Missing @Optional.ValueProvider annotation"
                )
            val providerType = ProviderProcessor.getProviderType(element, inspector)
            if (providerType != info.typeName) {
                throw Throwable("Parameter ${info.name} of type (${info.typeName}) is not the same as the ValueProvider type ($providerType)")
            }
            return ProviderParameter(info, element.className, element.isObject)
        }

        private fun Optional.ValueProvider.getProvider(): TypeMirror {
            try {
                this.provider
            } catch (mte: MirroredTypeException) {
                return mte.typeMirror
            }
            throw Throwable("DefaultValueProvider type cannot be determined")
        }
    }

    /**
     * Builds a constructor parameter spec
     */
    override fun toCtrParamSpec(): ParameterSpec {
        return ParameterSpec.builder(name, info.typeName.copy(true), KModifier.PRIVATE).build()
    }

    /**
     * Builds a constructor parameter spec
     */
    override fun toPropertySpec(): PropertySpec {
        return PropertySpec.builder(name, info.typeName.copy(true))
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
        return "val $name : ${info.typeName.copy(true)} = null".wrapProof()
    }

    /**
     * Creates a variable that will shadow the global that will contain the non-null initializer
     */
    override fun toBuildInitializer(): String {
        return if (isStatic) "val $name = this.$name ?: $providerName.get()".wrapProof()
        else "val $name = this.$name ?: $providerName().get()".wrapProof()
    }
}