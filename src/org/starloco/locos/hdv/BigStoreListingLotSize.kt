package org.starloco.locos.hdv

import kotlin.math.pow

enum class BigStoreListingLotSize(@JvmField val value: Int) {
    SingleItem(0),
    TenItems(1),
    HundredItems(2);

    @JvmField
    val amount: Int = 10.0.pow(value).toInt()

    companion object {
        @JvmStatic
        fun fromValue(b: Int): BigStoreListingLotSize? {
            for (s in entries) {
                if (s.value == b) return s
            }
            return null
        }
    }
}
