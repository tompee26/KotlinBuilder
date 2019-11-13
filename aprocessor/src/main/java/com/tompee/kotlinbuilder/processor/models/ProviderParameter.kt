package com.tompee.kotlinbuilder.processor.models

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.tompee.kotlinbuilder.annotations.DefaultValueProvider
import com.tompee.kotlinbuilder.annotations.Optional
import com.tompee.kotlinbuilder.annotations.Setter
import com.tompee.kotlinbuilder.processor.extensions.wrapProof
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types

internal fun Optional.ValueProvider.getProvider(): TypeMirror {
    try {
        this.provider
    } catch (mte: MirroredTypeException) {
        return mte.typeMirror
    }
    throw Throwable("DefaultValueProvider type cannot be determined")
}

internal fun TypeSpec.getInterfaceType(): ParameterizedTypeName {
    return superinterfaces
        .map { it.key }
        .filterIsInstance<ParameterizedTypeName>()
        .find { it.rawType == DefaultValueProvider::class.asClassName() }
        ?: throw Throwable("$name is not a subclass of DefaultValueProvider")
}


/**
 * Represents an optional parameter with default value provider in the target class constructor.
 *
 * @property name actual parameter name
 * @property propertySpec property spec
 * @property setter optional setter name annotation
 * @property typeName provider type name information
 */
@KotlinPoetMetadataPreview
internal data class ProviderParameter(
    override val name: String,
    override val propertySpec: PropertySpec,
    override val setter: Setter?,
    val typeName: TypeName
) : Parameter(name, propertySpec, setter) {

    class Builder @AssistedInject constructor(
        private val classInspector: ClassInspector,
        private val types: Types,
        @Assisted private val element: VariableElement,
        @Assisted private val name: String,
        @Assisted private val propertySpec: PropertySpec,
        @Assisted private val setter: Setter?
    ) {

        @AssistedInject.Factory
        interface Factory {

            fun create(
                element: VariableElement,
                name: String,
                propertySpec: PropertySpec,
                setter: Setter?
            ): Builder
        }

        fun build(): Parameter {
            val provider =
                element.getAnnotation(Optional.ValueProvider::class.java).getProvider()
            val typeSpec = (types.asElement(provider) as TypeElement).toTypeSpec(classInspector)
            val providerType = typeSpec.getInterfaceType().typeArguments.first()
            if (providerType != propertySpec.type) {
                throw Throwable("Parameter $name type (${propertySpec.type}) is not the same as the ValueProvider type ($providerType)")
            }
            return ProviderParameter(name, propertySpec, setter, provider.asTypeName())
        }
    }

    /**
     * Builds a constructor parameter spec
     */
    override fun toCtrParamSpec(): ParameterSpec {
        return ParameterSpec.builder(name, propertySpec.type.copy(true), KModifier.PRIVATE).build()
    }

    /**
     * Builds a constructor parameter spec
     */
    override fun toPropertySpec(): PropertySpec {
        return PropertySpec.builder(name, propertySpec.type.copy(true))
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
        return "val $name : ${propertySpec.type.copy(true)} = null".wrapProof()
    }

    /**
     * Creates a variable that will shadow the global that will contain the non-null initializer
     */
    override fun toBuildInitializer(): String? {
        return "val $name = this.$name ?: $typeName().get()".wrapProof()
    }
}