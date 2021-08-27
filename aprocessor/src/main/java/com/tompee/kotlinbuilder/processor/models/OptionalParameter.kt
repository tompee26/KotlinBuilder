package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.kotlinbuilder.processor.extensions.className
import com.tompee.kotlinbuilder.processor.extensions.wrapProof
import com.tompee.kotlinbuilder.processor.processor.ProviderMap

/**
 * Represents an optional parameter in the target class constructor.
 * An optional parameter is valid if it passes on of the following rules:
 * 1. It is a nullable type
 * 2. It is an enum type
 * 3. It is a value type
 */
@KotlinPoetMetadataPreview
internal data class OptionalParameter(
    override val info: ParameterInfo,
    private val initializer: Initializer
) : Parameter() {

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

        fun create(info: ParameterInfo, providerMap: ProviderMap): Parameter {

            // Check if nullable
            if (info.isNullable) {
                return NullableParameter.create(info)
            }

            // Check if enum
            if (info.isEnum) {
                return EnumParameter.create(info)
            }

            // Check from value type mapping
            val typeName = info.typeName.let {
                if (it is ParameterizedTypeName) it.rawType else it
            }
            val initializer = optionalValueTypeMap[typeName]
            if (initializer != null) {
                return OptionalParameter(info, initializer)
            }

            // Check from provider map
            val providerInfo = providerMap[info.typeName]
                ?: throw Throwable("Default value for parameter ${info.name} cannot be inferred")
            return ProviderParameter(
                info,
                providerInfo.element.className,
                providerInfo.typeSpec.kind == TypeSpec.Kind.OBJECT
            )
        }
    }

    /**
     * Builds a constructor parameter spec
     */
    override fun toCtrParamSpec(): ParameterSpec {
        return ParameterSpec.builder(name, info.typeName, KModifier.PRIVATE).build()
    }

    /**
     * Builds a constructor parameter spec
     */
    override fun toPropertySpec(): PropertySpec {
        return PropertySpec.builder(name, info.typeName)
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
        return "val $name : ${info.typeName} = ${initializer.get()}".wrapProof()
    }
}