package com.tompee.kotlinbuilder.models

import android.net.Uri
import com.tompee.kotlinbuilder.annotations.DefaultValueProvider
import com.tompee.kotlinbuilder.annotations.KBuilder
import com.tompee.kotlinbuilder.annotations.Optional

@KBuilder
data class Provider(
    @Optional
    val uri: Uri
)

@Optional.Provides
class UriProvider : DefaultValueProvider<Uri> {
    override fun get(): Uri = Uri.EMPTY
}