package org.starloco.locos.fight.ia.util

import org.starloco.locos.area.map.GameCase

class Node(
    @JvmField var cell: GameCase,
    @JvmField var parent: Node?
) {

    @JvmField
    var countG = 0
    @JvmField
    var countF = 0
    @JvmField
    var heristic = 0
    @JvmField
    var child: Node? = null
}
