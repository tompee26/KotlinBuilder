package com.tompee.kotlinbuilder.models

import com.tompee.kotlinbuilder.annotations.KBuilder
import com.tompee.kotlinbuilder.annotations.Optional

@KBuilder
data class Optional (

    @Optional
    val nullable: String?
)