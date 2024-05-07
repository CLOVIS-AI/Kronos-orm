package com.kotlinorm.annotations

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class UpdateTime(val format: String = "yyyy-MM-dd HH:mm:ss", val enable: Boolean = true)