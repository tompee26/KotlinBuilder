package com.tompee.kotlinbuilder.processor.extensions

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import javax.lang.model.element.Element

internal val Element.metadata: Metadata
    get() = getAnnotation(Metadata::class.java) ?: throw IllegalStateException("No metadata found")

@OptIn(KotlinPoetMetadataPreview::class)
internal val Element.className: ClassName
    get() = metadata.toImmutableKmClass().let { ClassInspectorUtil.createClassName(it.name) }