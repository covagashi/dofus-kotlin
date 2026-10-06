package org.starloco.locos.client.other

import org.starloco.locos.client.Player
import org.starloco.locos.common.PathFinding
import org.starloco.locos.common.SocketManager
import org.starloco.locos.kernel.Constant

class Stalk(var time: Long, var target: Player?) {

    fun onPlayerTryToFight(player: Player, target: Player?): Boolean {
        if (this.target == null || target == null || this.target!!.id != target.id)
            return false
        if (player.fight != null || target.fight != null || target.curMap.id != player.curMap.id || target.dead.toInt() == 1 || player.dead.toInt() == 1)
            return false
        if (!player.canAggro() || !target.canAggro()) {
            player.sendTypeMessage("Stalk", if (player.canAggro()) player.lang.trans("game.battle.mode.game.dubg.battleground.canaggro.true") else player.lang.trans("game.battle.mode.game.dubg.battleground.canaggro.false"))
            return false
        }

        val canDefy = PathFinding.canWalkToThisCell(player.curMap, player.curCell.cellId, target.curCell.cellId, true)

        if (canDefy && player.curCell.cellId != target.curCell.cellId) {
            if (player.gameClient != null)
                player.gameClient!!.clearAllPanels(target)

            player.curMap.newFight(target, player, Constant.FIGHT_TYPE_AGRESSION)
            player.away = false
            target.away = false
            SocketManager.send(player, "ILF0")
            SocketManager.send(target, "ILF0")
            return true
        }
        return false
    }
}
