package com.adyanta.iot.mask.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Mask(val value: Array<String>)
