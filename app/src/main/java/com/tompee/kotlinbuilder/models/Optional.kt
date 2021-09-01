package com.tompee.kotlinbuilder.models

import android.graphics.Bitmap
import android.view.View
import androidx.fragment.app.Fragment
import com.tompee.kotlinbuilder.annotations.DefaultValueProvider
import com.tompee.kotlinbuilder.annotations.KBuilder
import com.tompee.kotlinbuilder.annotations.Optional
import java.util.*

@KBuilder
data class Optional (

    @Optional
    val nullable: String?,

    @Optional
    val enums : SampleEnum,

    @Optional
    val unit : Unit,

    @Optional
    val byte : Byte,

    @Optional
    val short: Short,

    @Optional
    val int: Int,

    @Optional
    val long : Long,

    @Optional
    val float: Float,

    @Optional
    val double: Double,

    @Optional
    val boolean: Boolean,

    @Optional
    val string : String,

    @Optional
    val list : List<View>,

    @Optional
    val mutableList : MutableList<View?>,

    @Optional
    val map : Map<Fragment, Int>,

    @Optional
    val mutableMap : MutableMap<Fragment, Int>,

    @Optional
    val array : Array<StringBuffer>,

    @Optional
    val set : Set<Locale>,

    @Optional
    val mutableSet: MutableSet<Locale>,

    @Optional
    val listOfBitmap: List<Bitmap>
)

@Optional.Provides
object BitmapListProvider : DefaultValueProvider<List<Bitmap>> {
    override fun get(): List<Bitmap> = listOf(Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8))
}