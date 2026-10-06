package org.starloco.locos.util

import org.starloco.locos.common.Formulas
import java.util.ArrayList

class RandomStats<Stats> {

    private val randoms = ArrayList<Stats>()

    fun add(pct: Int, `object`: Stats) {
        repeat(pct) { randoms.add(`object`) }
    }

    fun size(): Int = randoms.size

    fun get(): Stats = randoms[Formulas.random.nextInt(randoms.size)]
}
