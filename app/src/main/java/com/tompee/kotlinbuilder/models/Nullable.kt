package com.tompee.kotlinbuilder.models

import com.tompee.kotlinbuilder.annotations.KBuilder
import com.tompee.kotlinbuilder.annotations.Optional

@KBuilder
data class Nullable(
    @Optional.Nullable
    val explicit: String?

// This will cause a compile-time error
//    ,
//    @Optional.Nullable
//    val invalid: String
)