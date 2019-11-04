package com.tompee.kotlinbuilder.models

import com.tompee.kotlinbuilder.annotations.EnumPosition
import com.tompee.kotlinbuilder.annotations.KBuilder
import com.tompee.kotlinbuilder.annotations.Optional

enum class SampleEnum {
    VALUE_1,
    VALUE_2
}

@KBuilder
data class Enum(

    @Optional.Enumerable
    val start: SampleEnum,

    @Optional.Enumerable(EnumPosition.LAST)
    val end: SampleEnum
)