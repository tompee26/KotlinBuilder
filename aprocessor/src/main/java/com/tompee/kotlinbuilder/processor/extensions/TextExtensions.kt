package com.tompee.kotlinbuilder.processor.extensions

/**
 * Replaces all whitespace with wrap-proof whitespaces
 */
internal fun String.wrapProof() : String {
    return this.replace(" ", "·")
}