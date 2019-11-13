package com.tompee.kotlinbuilder.models

import android.graphics.Bitmap
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
object UriProvider : DefaultValueProvider<Uri> {
    override fun get(): Uri = Uri.EMPTY
}

@Optional.Provides
object BitmapProvider : DefaultValueProvider<Bitmap> {
    override fun get(): Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
}