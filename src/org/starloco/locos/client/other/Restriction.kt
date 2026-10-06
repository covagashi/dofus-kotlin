package org.starloco.locos.client.other

import java.util.HashMap

class Restriction {

    //region
    @JvmField
    val aggros = HashMap<String, Long>()
    @JvmField
    var command = true

    companion object {
        private val restrictions = HashMap<Int, Restriction>()

        @JvmStatic
        fun get(id: Int): Restriction {
            if (restrictions[id] != null)
                return restrictions[id]!!
            return Restriction()
        }
    }
    //endregion
}
