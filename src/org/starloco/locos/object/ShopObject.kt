package org.starloco.locos.`object`

import java.util.ArrayList

/**
 * Created by Locos on 29/08/2018.
 */
class ShopObject(val id: Short, val template: ObjectTemplate, val isJp: Boolean, val price: Short) {

    companion object {
        @JvmField
        val objects: MutableList<ShopObject> = ArrayList()
    }
}
