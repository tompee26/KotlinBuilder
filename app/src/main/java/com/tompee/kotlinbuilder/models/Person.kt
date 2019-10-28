package com.tompee.kotlinbuilder.models

import com.tompee.kotlinbuilder.annotations.Builder
import com.tompee.kotlinbuilder.annotations.Setter

@Builder("PersonFactory")
data class Person(

    @Setter("setFullName")
    val name: String,

    val age: Int
)