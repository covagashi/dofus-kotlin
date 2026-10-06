package org.starloco.locos.hdv

import org.starloco.locos.game.world.World
import org.starloco.locos.`object`.GameObject

class BigStoreListing(
    var id: Int,
    val price: Int,
    amount: Byte,
    val owner: Int,
    val gameObject: GameObject?
) {

    var hdvId: Int = 0
    var lineId: Int = 0
    val lotSize: BigStoreListingLotSize? = BigStoreListingLotSize.fromValue(amount.toInt())

    constructor(price: Int, amount: Byte, owner: Int, gameObject: GameObject?) :
        this(-1, price, amount, owner, gameObject)

    fun parseToEL(): String {
        // For EL packet, we want to be able to identify each listing, so we return the listing ID
        val toReturn = StringBuilder()
        val duration = World.world.getHdv(hdvId)!!.duration
        toReturn.append(this.id).append(";").append(lotSize!!.amount).append(";").append(this.gameObject!!.template!!.id).append(";").append(this.gameObject!!.encodeStats()).append(";").append(this.price).append(";").append(duration)
        return toReturn.toString()
    }

    fun parseToEmK(): String {
        val toReturn = StringBuilder()
        val duration = World.world.getHdv(hdvId)!!.duration
        toReturn.append(this.gameObject!!.guid).append("|").append(lotSize!!.amount).append("|").append(this.gameObject!!.template!!.id).append("|").append(this.gameObject!!.encodeStats()).append("|").append(this.price).append("|").append(duration)
        return toReturn.toString()
    }
}
