package org.starloco.locos.annotation

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class DofusMessage(val header: String)
