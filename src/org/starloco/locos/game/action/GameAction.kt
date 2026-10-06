package org.starloco.locos.game.action

class GameAction(
    @JvmField var id: Int,
    @JvmField var actionId: Int,
    @JvmField var packet: String?
) {
    @JvmField
    var args: String? = null
    @JvmField
    var tp = false
}
