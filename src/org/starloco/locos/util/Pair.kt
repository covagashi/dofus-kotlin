package org.starloco.locos.util

import java.io.Serializable
import java.util.function.Function

/**
 * Created by Locos on 13/06/2018.
 */
class Pair<K, V>(
    @JvmField val first: K,
    @JvmField val second: V
) : Serializable {

    fun getFirst(): K = first

    fun getSecond(): V = second

    override fun toString(): String = "$first/$second"

    fun toString(sep: String): String = "$first$sep$second"

    override fun hashCode(): Int = first.hashCode() * 13 + (second?.hashCode() ?: 0)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Pair<*, *>) return false
        if (first != other.first) return false
        return second == other.second
    }

    fun <OK, OV> map(km: Function<K, OK>, vm: Function<V, OV>): Pair<OK, OV> =
        Pair(km.apply(first), vm.apply(second))
}
