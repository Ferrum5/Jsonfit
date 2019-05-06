package com.github.waterpeak.jsonfit


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Get(val path: String)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Post(val path: String)


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Multipart(val path: String, val type: Int = FORM){
    companion object{
        const val MIXED = 1
        const val ALTERNATIVE = 2
        const val DIGEST = 3
        const val PARALLEL = 4
        const val FORM = 0
    }
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Json(val path: String)