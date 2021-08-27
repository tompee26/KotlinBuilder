package com.tompee.kotlinbuilder.processor.extensions

import javax.lang.model.element.Element

internal val Element.metadata: Metadata
    get() = getAnnotation(Metadata::class.java) ?: throw IllegalStateException("No metadata found")