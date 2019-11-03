package com.tompee.kotlinbuilder.models

import androidx.fragment.app.Fragment
import com.tompee.kotlinbuilder.annotations.KBuilder
import com.tompee.kotlinbuilder.annotations.Optional

@KBuilder
internal data class Default (
    @Optional.Default
    val explicit: Fragment = Fragment()
)