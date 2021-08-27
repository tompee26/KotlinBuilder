package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.tompee.kotlinbuilder.annotations.DefaultValueProvider
import com.tompee.kotlinbuilder.annotations.Optional
import com.tompee.kotlinbuilder.processor.KBuilderElement
import com.tompee.kotlinbuilder.processor.extensions.className
import com.tompee.kotlinbuilder.processor.extensions.wrapProof
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror

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
 */
@OptIn(KotlinPoetMetadataPreview::class)
internal data class ProviderParameter(
    override val info: ParameterInfo,
    val typeName: TypeName,
    val isStatic: Boolean = false
) : Parameter() {

    class Builder(
        private val kElement: KBuilderElement,
        private val info: ParameterInfo
    ) {

        class Factory(private val kElement: KBuilderElement) {

            fun create(info: ParameterInfo): Builder = Builder(kElement, info)
        }

        fun build(): Parameter {
            val provider =
                info.varElement.getAnnotation(Optional.ValueProvider::class.java).getProvider()
            val typeSpec =
                (kElement.types.asElement(provider) as TypeElement).toTypeSpec(kElement.classInspector)
            val providerType = typeSpec.getInterfaceType().typeArguments.first()
            if (providerType != info.typeName) {
                throw Throwable("Parameter ${info.name} of type (${info.typeName}) is not the same as the ValueProvider type ($providerType)")
            }
            return ProviderParameter(
                info,
                kElement.types.asElement(provider).className,
                typeSpec.kind == TypeSpec.Kind.OBJECT
            )
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
        return if (isStatic) "val $name = this.$name ?: $typeName.get()".wrapProof()
        else "val $name = this.$name ?: $typeName().get()".wrapProof()
    }
}