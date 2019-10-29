package com.tompee.kotlinbuilder.models

import com.tompee.kotlinbuilder.annotations.*
import com.tompee.kotlinbuilder.annotations.types.DefaultValueProvider

@Builder
data class Address(
    @Optional
    @Nullable
    val street: String?,

    @Optional
    @Default
    val town: String,

    @Optional
    @Provider(StateDefaultValueProvider::class)
    val state: String
) {

    class StateDefaultValueProvider : DefaultValueProvider<String> {
        override fun get(): String {
            return "NY"
        }
    }
}