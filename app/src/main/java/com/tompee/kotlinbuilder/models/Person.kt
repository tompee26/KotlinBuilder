package com.tompee.kotlinbuilder.models

import com.tompee.kotlinbuilder.annotations.Builder
import com.tompee.kotlinbuilder.annotations.SetterName

@Builder("PersonFactory")
data class Person(

    @SetterName("setFullName")
    val name: String,

    val age: Int
)