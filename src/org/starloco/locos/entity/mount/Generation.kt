package org.starloco.locos.entity.mount

object Generation {

    @JvmStatic
    fun getPods(generation: Int, level: Int): Int {
        return (100 + 50 * generation - 1) + (5 + 5 * (generation / 2)) * level
    }

    @JvmStatic
    fun getEnergy(generation: Int): Int {
        return (1000 + 100 * generation - 1) + (10 + 5 * (generation / 2))
    }

    @JvmStatic
    fun getMaturity(generation: Int): Int {
        return generation * 1000
    }

    @JvmStatic
    fun getTimeGestation(generation: Int): Short {
        return ((36 + 12 * generation) / 2).toShort()
    }

    @JvmStatic
    fun getLearningRate(generation: Int): Short {
        return when (generation) {
            2, 3, 4 -> 80
            5, 6 -> 60
            7, 8 -> 40
            9, 10 -> 20
            else -> 100
        }
    }
}
