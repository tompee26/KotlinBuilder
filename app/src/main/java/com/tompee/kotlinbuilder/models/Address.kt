package com.tompee.kotlinbuilder.models

import com.tompee.kotlinbuilder.annotations.*
import com.tompee.kotlinbuilder.annotations.types.DefaultValueProvider

@Builder
internal data class Address(
    @Optional
    @Nullable
    val street: String?,

    @Optional
    @Default
    val town: String = "",

    @Optional
    @Default
    val city: String = "",

    @Optional
    @Default
    val province: String = "",

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