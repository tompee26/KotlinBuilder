package com.tompee.kotlinbuilder.models

import com.tompee.kotlinbuilder.annotations.Builder
import com.tompee.kotlinbuilder.annotations.Optional
import com.tompee.kotlinbuilder.annotations.Provider

class OptionalProvider : Provider<List<Int>> {
    override fun get() = listOf(1)
}

data class Address(
    val street: String,
    val city: String
)

@Builder
class Person(
    val name: String,
    val address: Address,
    @Optional(OptionalProvider::class) val telephone: List<Int>,
    @Optional(OptionalProvider::class) val optional: List<Int>
) {

    val outsideVariable = 12
}