package com.tompee.kotlinbuilder.models

import com.tompee.kotlinbuilder.annotations.KBuilder
import com.tompee.kotlinbuilder.annotations.Setter

@KBuilder("PersonFactory")
data class Person(

    @Setter("setFullName")
    val name: String,

    val age: Int
)