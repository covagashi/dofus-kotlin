package org.starloco.locos.api

import org.starloco.locos.game.GameClient

abstract class AbstractDofusMessage {

    var output: StringBuilder = StringBuilder()
    var input: StringBuilder? = null
    var client: GameClient? = null

    abstract fun serialize()

    abstract fun deserialize()
}
