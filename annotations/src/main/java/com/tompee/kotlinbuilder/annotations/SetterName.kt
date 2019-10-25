package com.tompee.kotlinbuilder.annotations

/**
 * Customizes the setter method name. If not set, the default parameter name is used
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class SetterName(val name: String = "")