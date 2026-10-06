package org.starloco.locos.game.action.type

import org.starloco.locos.client.Player
import org.starloco.locos.game.action.ExchangeAction
import java.util.function.BiConsumer

class ScenarioActionData(
    private val source: ExchangeAction<*>,
    private val onCompleted: BiConsumer<Player, Boolean>
) : ActionDataInterface {

    fun onCompletion(player: Player, succeed: Boolean) {
        player.exchangeAction = source
        onCompleted.accept(player, succeed)
    }
}
