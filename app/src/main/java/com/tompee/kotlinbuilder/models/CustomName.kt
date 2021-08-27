package com.tompee.kotlinbuilder.models

import com.tompee.kotlinbuilder.annotations.KBuilder
import com.tompee.kotlinbuilder.annotations.Optional
import com.tompee.kotlinbuilder.annotations.Setter

@KBuilder("CustomFactory")
class CustomName(
    @Setter("setName")
    val name : String,

    @Setter("setAge")
    @Optional
    age: Int
)