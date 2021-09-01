package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.kotlinbuilder.processor.extensions.className
import com.tompee.kotlinbuilder.processor.extensions.isObject
import com.tompee.kotlinbuilder.processor.extensions.wrapProof
import com.tompee.kotlinbuilder.processor.processor.ProviderMap

internal typealias Initializer = () -> String

/**
 * Represents an optional parameter in the target class constructor.
 * An optional parameter is valid if it passes on of the following rules:
 * 1. It is a nullable type
 * 2. It is an enum type
 * 3. It is a value type
 */
@KotlinPoetMetadataPreview
internal class OptionalParameter private constructor(
    override val info: ParameterInfo,
    private val initializer: Initializer
) : Parameter() {

    companion object {

        private val optionalValueTypeMap = mapOf(
            UNIT to { "Unit" },
            BYTE to { "0" },
            SHORT to { "0" },
            INT to { "0" },
            LONG to { "0L" },
            FLOAT to { "0f" },
            DOUBLE to { "0.0" },
            BOOLEAN to { "false" },
            STRING to { "\"\"" },
            LIST to { "emptyList()" },
            MUTABLE_LIST to { "mutableListOf()" },
            MAP to { "emptyMap()" },
            MUTABLE_MAP to { "mutableMapOf()" },
            ARRAY to { "emptyArray()" },
            SET to { "emptySet()" },
            MUTABLE_SET to { "mutableSetOf()" }
        )

        fun create(info: ParameterInfo, providerMap: ProviderMap): Parameter {

            // Check from provider map
            val element = providerMap[info.typeName]
            if (element != null) {
                return ProviderParameter.create(info, element.className, element.isObject)
            }

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
            throw Throwable("Default value for parameter ${info.name} cannot be inferred")
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
        return "val $name : ${info.typeName} = ${initializer()}".wrapProof()
    }
}