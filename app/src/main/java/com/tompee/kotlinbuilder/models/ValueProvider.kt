package com.tompee.kotlinbuilder.models

import com.tompee.kotlinbuilder.annotations.DefaultValueProvider
import com.tompee.kotlinbuilder.annotations.KBuilder
import com.tompee.kotlinbuilder.annotations.Optional

@KBuilder
data class ValueProvider(

    @Optional.ValueProvider(Provider::class)
    val explicit: List<String>

//    @Optional.ValueProvider(InvalidTypeProvider::class)
//    val invalid: List<String>
) {
    class Provider : DefaultValueProvider<List<String>> {
        override fun get(): List<String> {
            return emptyList()
        }
    }

    class InvalidTypeProvider : DefaultValueProvider<Int> {
        override fun get(): Int {
            return 0
        }
    }
}