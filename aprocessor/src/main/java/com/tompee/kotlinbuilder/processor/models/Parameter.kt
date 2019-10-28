package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.kotlinbuilder.annotations.Nullable
import com.tompee.kotlinbuilder.annotations.Optional
import com.tompee.kotlinbuilder.annotations.Provider
import com.tompee.kotlinbuilder.annotations.Setter
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror

/**
 * A class that represents a constructor parameter
 *
 * @param name parameter name
 * @param type type name
 * @param isOptional optional flag
 * @param setter setter name annotation
 */
@KotlinPoetMetadataPreview
internal data class Parameter(
    val name: String,
    val type: TypeName,
    val isOptional: Boolean,
    val setter: Setter?,
    private val generatorDetails: GeneratorDetails?
) {
    companion object {

        /**
         * Parses the list of parameters from a type element
         *
         * @param typeElement builder class type element
         * @param typeSpec builder class type spec
         */
        fun parse(typeElement: TypeElement, typeSpec: TypeSpec): List<Parameter> {
            val javaConstructor =
                typeElement.enclosedElements.firstOrNull { it.kind == ElementKind.CONSTRUCTOR } as? ExecutableElement
                    ?: throw IllegalStateException("No constructor found for ${typeElement.simpleName}")

            /**
             * Now we use the java constructor to check for annotations
             */
            val builders = javaConstructor.parameters.map {
                val isOptional = it.getAnnotation(Optional::class.java) != null
                val details = if (isOptional) {
                    val details = GeneratorDetails(
                        it.getAnnotation(Nullable::class.java),
                        it.getAnnotation(Provider::class.java)
                    )
                    details
                } else null

                Builder(
                    it.simpleName.toString(),
                    isOptional = isOptional,
                    setter = it.getAnnotation(Setter::class.java),
                    details = details
                )
            }

            /**
             * Now we will need the actual kotlin type of the parameter
             */
            typeSpec.propertySpecs.forEach { propertySpec ->
                builders.find { it.name == propertySpec.name }
                    ?.apply { typeName = propertySpec.type }
            }

            /**
             * Perform error checks first
             */
            builders.map { it.build() }
                .forEach {
                    if (it.isOptional) {
                        check(it.withGenerator()) {
                            "Class: ${typeElement.simpleName}, Parameter: ${it.name}." +
                                    " Optional parameter must define at least one method to determine default value"
                        }
                        // TODO: Check nullable if it is a true nullable
                    }
                }
            return builders.map { it.build() }
        }
    }

    /**
     * Saves the generator annotation details of each parameter
     */
    data class GeneratorDetails(
        private val nullable: Nullable?,
        private val provider: Provider?
    ) {
        fun isValid(): Boolean {
            return nullable != null || provider != null
        }

        fun isNullable(): Boolean {
            return nullable != null
        }

        fun withProvider(): Boolean {
            return provider != null
        }

        fun getProvider(): TypeMirror {
            try {
                provider?.provider!!
            } catch (mte: MirroredTypeException) {
                return mte.typeMirror
            }

            throw IllegalStateException("DefaultValueProvider type cannot be determined")
        }
    }

    /**
     * Container for parameter details to defer construction at a later time
     */
    private class Builder(
        var name: String = "",
        var typeName: TypeName? = null,
        var isOptional: Boolean = false,
        var setter: Setter? = null,
        var details: GeneratorDetails? = null
    ) {
        fun build(): Parameter {
            check(name.isNotEmpty()) { "Builder name is empty" }

            return Parameter(
                name = name,
                type = typeName ?: throw IllegalStateException("Type cannot be determined"),
                isOptional = isOptional,
                setter = setter,
                generatorDetails = details
            )
        }
    }

    /**
     * Returns true if generator details contains a single provider
     */
    fun withGenerator(): Boolean {
        return generatorDetails?.isValid() == true
    }

    /**
     * Returns true if this parameter type is nullable
     */
    fun isNullable(): Boolean {
        return generatorDetails?.isNullable() == true
    }

    /**
     * Returns true if this parameter has a default value provider
     */
    fun withProvider(): Boolean {
        return generatorDetails?.withProvider() == true
    }

    /**
     * Returns the provider type. This will throw an exception if not successful
     */
    fun getProvider(): TypeMirror {
        return generatorDetails?.getProvider()!!
    }
}
