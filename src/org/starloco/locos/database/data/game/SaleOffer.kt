package org.starloco.locos.database.data.game

import org.starloco.locos.game.world.World
import org.starloco.locos.`object`.ObjectTemplate

import java.util.StringJoiner

class SaleOffer(
    @JvmField val itemTemplate: ObjectTemplate,
    @JvmField val unitPrice: Long,
    @JvmField val currency: Currency
) {

    constructor(itemTemplate: ObjectTemplate) : this(itemTemplate, itemTemplate.price.toLong())

    constructor(itemTemplate: ObjectTemplate, unitPrice: Long) : this(itemTemplate, unitPrice, Currency.KAMAS)

    fun encode(): String {
        // [itemID];[stats];[currency];[price];;
        val sj = StringJoiner(";")
        sj.add(itemTemplate.id.toString())
        sj.add(itemTemplate.strTemplate)
        sj.add(if (currency.isItem()) currency.item().id.toString() else "")
        sj.add(java.lang.Long.toString(unitPrice))
        sj.add("") // Unknown
        sj.add("") // Unknown

        return sj.toString()
    }

    class Currency private constructor(private val cur: Any) {
        fun isItem(): Boolean {
            return this.cur is ObjectTemplate
        }

        fun item(): ObjectTemplate {
            if (!isItem()) throw RuntimeException("currency is not an item")
            return cur as ObjectTemplate
        }

        companion object {
            @JvmField
            val KAMAS = Currency(1)
            @JvmField
            val POINTS = Currency(2)

            @JvmStatic
            fun nonItemCurrency(n: Int): Currency {
                return when (n) {
                    1 -> KAMAS
                    2 -> POINTS
                    else -> throw RuntimeException(String.format("unknown non-item currency #%d", n))
                }
            }

            @JvmStatic
            fun itemCurrency(templateID: Int): Currency {
                val t = World.world.getObjTemplate(templateID)
                    ?: throw IllegalArgumentException(String.format("unknown item template #%d", templateID))
                return Currency(t)
            }
        }
    }
}
