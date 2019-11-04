package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.sun.tools.javac.code.Type
import com.tompee.kotlinbuilder.annotations.EnumPosition
import com.tompee.kotlinbuilder.annotations.Optional
import com.tompee.kotlinbuilder.annotations.Setter
import com.tompee.kotlinbuilder.processor.extensions.wrapProof
import javax.lang.model.element.VariableElement

/**
 * Represents an optional enum parameter
 *
 * @property name actual parameter name
 * @property propertySpec property spec
 * @property setter optional setter name annotation
 * @property position position of the default value
 */
internal data class EnumParameter(
    override val name: String,
    override val propertySpec: PropertySpec,
    override val setter: Setter?,
    val position: EnumPosition
) : Parameter(name, propertySpec, setter) {

    companion object {

        /**
         * Checks if an element is a subtype of enum
         */
        fun isValidEnum(element: VariableElement): Boolean {
            val elementType = element.asType() as? Type.ClassType
            val superTypeName =
                (elementType?.supertype_field?.asTypeName() as? ParameterizedTypeName)?.rawType
            val enumTypeName = java.lang.Enum::class.java.asTypeName()
            return superTypeName == enumTypeName
        }
    }

    @KotlinPoetMetadataPreview
    class Builder(
        private val element: VariableElement,
        private val enumerable: Optional.Enumerable,
        name: String = "",
        propertySpec: PropertySpec? = null,
        setter: Setter? = null
    ) : Parameter.Builder(name, propertySpec, setter) {

        override fun build(): Parameter {
            if (!isValidEnum(element)) throw Throwable("Parameter $name type is not enum")

            val position = enumerable.position
            return EnumParameter(name, propertySpec!!, setter, position)
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
        val initializer =
            if (position == EnumPosition.FIRST) "val $name = ${propertySpec.type}.values()[0]"
            else "val $name = ${propertySpec.type}.values()[${propertySpec.type}.values().count() - 1]"
        return initializer.wrapProof()
    }
}