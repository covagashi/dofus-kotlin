package org.starloco.locos.game.action.type

import org.starloco.locos.client.Player
import org.starloco.locos.script.DataScriptVM

class DocumentActionData(private val id: Int) : ActionDataInterface {

    fun onQuestHRef(player: Player, questID: Int) {
        player.exchangeAction = null
        DataScriptVM.getInstance()!!.handlers.onDocQuestHref(player, id, questID)
    }
}
