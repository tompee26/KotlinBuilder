package com.tompee.kotlinbuilder.processor.models

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tompee.kotlinbuilder.annotations.Optional
import com.tompee.kotlinbuilder.annotations.SetterName

/**
 * A class that represents a constructor parameter
 *
 * @param name parameter name
 * @param typeName type name
 * @param setter setter name annotation
 * @param optional optional annotation
 */
@KotlinPoetMetadataPreview
internal data class Parameter(
    val name: String,
    var typeName: TypeName? = null,
    var setter: SetterName? = null,
    var optional: Optional? = null
) {
    fun getTypeOrFail(): TypeName {
        return typeName ?: throw IllegalStateException("Type name of $name is not available")
    }

    fun isOptional(): Boolean {
        return optional != null
    }
}
