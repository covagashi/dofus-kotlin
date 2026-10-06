package org.starloco.locos.auction

import org.starloco.locos.client.Player
import org.starloco.locos.`object`.GameObject

/**
 * Created by Locos on 31/01/2018.
 */
class Auction(
    var price: Int,
    val owner: Player?,
    var `object`: GameObject?,
    var retry: Byte
) {

    var customer: Player? = null

    fun incRetry() {
        this.retry++
    }
}
