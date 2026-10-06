package org.starloco.locos.game.action

import java.util.HashMap

/**
 * Created by Locos on 25/10/2015.
 */
class ExchangeAction<T>(private var type: Byte, private var value: T) {

    private val context = HashMap<String, Any>()

    fun getType(): Byte = type

    fun getValue(): T = value

    fun putContextValue(v: String, o: Any) {
        context[v] = o
    }

    fun getContextValue(v: String): Any? = context[v]

    companion object {
        const val TALKING_WITH: Byte = 0
        const val TRADING_WITH_ME: Byte = 1
        const val TRADING_WITH_PLAYER: Byte = 2
        const val TRADING_WITH_OFFLINE_PLAYER: Byte = 3
        const val TRADING_WITH_NPC: Byte = 4
        const val TRADING_WITH_NPC_EXCHANGE: Byte = 5
        const val TRADING_WITH_NPC_PETS: Byte = 6
        const val TRADING_WITH_COLLECTOR: Byte = 7
        const val TRADING_WITH_NPC_PETS_RESURRECTION: Byte = 8
        const val CRAFTING: Byte = 9
        const val BREAKING_OBJECTS: Byte = 10
        const val CRAFTING_SECURE_WITH: Byte = 11
        const val AUCTION_HOUSE_BUYING: Byte = 12
        const val AUCTION_HOUSE_SELLING: Byte = 13
        const val IN_MOUNT: Byte = 14
        const val IN_MOUNTPARK: Byte = 15
        const val IN_TRUNK: Byte = 16
        const val IN_BANK: Byte = 17
        const val IN_ZAAPING: Byte = 18
        const val IN_ZAPPI: Byte = 19
        const val IN_PRISM: Byte = 20
        const val IN_SCENARIO: Byte = 21
        const val FORGETTING_SPELL: Byte = 22
        const val USING_OBJECT: Byte = 23
        const val LOCK_TRUNK: Byte = 24
        const val LOCK_HOUSE: Byte = 25
        const val READING_DOCUMENT: Byte = 26
    }
}
