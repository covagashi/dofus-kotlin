package org.starloco.locos.util

import java.util.function.Predicate

object Predicates {
    @JvmStatic
    fun <T> not(p: Predicate<T>): Predicate<T> = p.negate()
}
