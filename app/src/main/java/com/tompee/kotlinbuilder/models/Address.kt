package com.tompee.kotlinbuilder.models

import com.tompee.kotlinbuilder.annotations.Builder
import com.tompee.kotlinbuilder.annotations.Nullable
import com.tompee.kotlinbuilder.annotations.Optional
import com.tompee.kotlinbuilder.annotations.Provider
import com.tompee.kotlinbuilder.annotations.types.DefaultValueProvider

@Builder
data class Address(
    @Optional
    @Nullable
    val street: String?,

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