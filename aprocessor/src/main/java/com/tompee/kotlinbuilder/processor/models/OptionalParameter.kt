package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.kotlinbuilder.annotations.EnumPosition
import com.tompee.kotlinbuilder.annotations.Setter
import com.tompee.kotlinbuilder.processor.extensions.wrapProof
import com.tompee.kotlinbuilder.processor.processor.ProviderMap
import javax.lang.model.element.VariableElement

/**
 * Represents an optional parameter in the target class constructor.
 * An optional parameter is valid if it passes on of the following rules:
 * 1. It is a nullable type
 * 2. It is an enum type
 * 3. It is a value type
 *
 * @property name actual parameter name
 * @property propertySpec property spec
 * @property setter optional setter name annotation
 */
@KotlinPoetMetadataPreview
internal data class OptionalParameter(
    override val name: String,
    override val propertySpec: PropertySpec,
    override val setter: Setter?,
    private val initializer: Initializer
) : Parameter(name, propertySpec, setter) {

    class Initializer(private val initializer: () -> String) {
        fun get() = initializer()
    }

    companion object {

        private val optionalValueTypeMap = mapOf(
            UNIT to Initializer { "Unit" },
            BYTE to Initializer { "0" },
            SHORT to Initializer { "0" },
            INT to Initializer { "0" },
            LONG to Initializer { "0L" },
            FLOAT to Initializer { "0f" },
            DOUBLE to Initializer { "0.0" },
            BOOLEAN to Initializer { "false" },
            STRING to Initializer { "\"\"" },
            LIST to Initializer { "emptyList()" },
            MUTABLE_LIST to Initializer { "mutableListOf()" },
            MAP to Initializer { "emptyMap()" },
            MUTABLE_MAP to Initializer { "mutableMapOf()" },
            ARRAY to Initializer { "emptyArray()" },
            SET to Initializer { "emptySet()" },
            MUTABLE_SET to Initializer { "mutableSetOf()" }
        )

        fun create(
            element: VariableElement,
            providerMap: ProviderMap,
            name: String,
            propertySpec: PropertySpec,
            setter: Setter?
        ): Parameter {

            // Check if nullable
            if (propertySpec.type.isNullable) {
                return NullableParameter(name, propertySpec, setter)
            }

            // Check if enum
            if (EnumParameter.isValidEnum(element)) {
                return EnumParameter(name, propertySpec, setter, EnumPosition.FIRST)
            }

            // Check from value type mapping
            val typeName = propertySpec.type.let {
                if (it is ParameterizedTypeName) it.rawType else it
            }
            val initializer = optionalValueTypeMap[typeName]
            if (initializer != null) {
                return OptionalParameter(name, propertySpec, setter, initializer)
            }

            // Check from provider map
            val providerType = providerMap[propertySpec.type]
                ?: throw Throwable("Default value for parameter $name cannot be inferred")
            return ProviderParameter(name, propertySpec, setter, providerType)
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
        throw Throwable("Internal error. This should not be called")
    }

    /**
     * Builds an invoke method initializer statement
     */
    override fun createInitializeStatement(): String {
        return "val $name : ${propertySpec.type} = ${initializer.get()}".wrapProof()
    }
}