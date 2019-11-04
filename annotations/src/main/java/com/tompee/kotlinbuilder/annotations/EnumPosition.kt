package com.tompee.kotlinbuilder.annotations

/**
 * Represents the position of the default value of an enum
 */
enum class EnumPosition {
    /**
     * First value of the enum. In case of single valued enum, the first is also the last.
     */
    FIRST,
    /**
     * Last value of the enum. In case of single valued enum, the first is also the last.
     */
    LAST
}