package com.tompee.kotlinbuilder.processor.processor

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.tompee.kotlinbuilder.processor.models.getInterfaceType
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

@OptIn(KotlinPoetMetadataPreview::class)
internal typealias ProviderMap = Map<TypeName, ProviderProcessor.ProviderInfo>

@KotlinPoetMetadataPreview
internal class ProviderProcessor(private val classInspector: ClassInspector) {

    data class ProviderInfo(
        val element: Element,
        val typeSpec: TypeSpec
    )

    /**
     * Returns the provider mapping. The provider mapping is a pair of the value type and the
     * type of the provider class.
     */
    fun getProviderMap(elements: List<Element>): ProviderMap {
        return elements.filterIsInstance<TypeElement>()
            .map { it to it.toTypeSpec(classInspector) }
            .mapNotNull { pair ->
                val providerType = pair.second.getInterfaceType()
                    .typeArguments.firstOrNull() ?: return@mapNotNull null
                return@mapNotNull providerType to ProviderInfo(pair.first, pair.second)
            }.toMap()
    }
}