package org.starloco.locos.client.other

import org.starloco.locos.area.map.GameCase
import org.starloco.locos.client.Player
import org.starloco.locos.common.PathFinding
import org.starloco.locos.common.SocketManager
import java.util.ArrayList

class Party(p1: Player, p2: Player) {

    var chief: Player = p1
    var master: Player? = null
    val players: ArrayList<Player> = ArrayList()
    private val options = ArrayList<MasterOption>()

    init {
        this.players.add(p1)
        this.players.add(p2)
    }

    fun isChief(id: Int): Boolean = this.chief.id == id

    fun addPlayer(player: Player) {
        this.players.add(player)
    }

    fun leave(player: Player) {
        if (!this.players.contains(player)) return

        player.follow = null
        player.follower.clear()
        player.party = null
        this.players.removeIf { player1 -> player1.id == player.id }

        for (member in this.players) {
            if (member.follow === player) member.follow = null
            member.follower.remove(player.id)
        }

        if (this.players.size == 1) {
            this.players[0].party = null
            if (this.players[0].account == null || this.players[0].gameClient == null)
                return
            SocketManager.GAME_SEND_PV_PACKET(this.players!![0].gameClient!!, "")
        } else {
            if (this.isChief(player.id)) {
                this.chief = this.players[0]
                for (member in this.players) {
                    member.send("PL" + this.chief.id)
                }
            }
            SocketManager.GAME_SEND_PM_DEL_PACKET_TO_GROUP(this, player.id)
        }
    }

    fun moveAllPlayersToMaster(cell: GameCase?, tp: Boolean) {
        val master = this.master ?: return
        this.players.stream().filter { isWithTheMaster(it, false, false) }.forEach { it.blockMovement = true }
        this.players.stream().filter { isWithTheMaster(it, false, false) }.forEach { follower ->
            try {
                val newCell = cell ?: master.curCell
                val cells = PathFinding.getShortestPathBetween(master.curMap, follower.curCell.cellId, newCell.cellId, 0)!!

                if (cells.isNotEmpty()) {
                    val lastCell = cells[cells.size - 1]

                    if (tp) {
                        follower.teleport(master.curMap, master.curCell.cellId)
                    } else {
                        val path = PathFinding.getShortestStringPathBetween(master.curMap, follower.curCell.cellId, lastCell.cellId, 0)

                        if (path != null) {
                            follower.curCell.removePlayer(follower)
                            follower.curCell = lastCell
                            follower.curCell.addPlayer(follower)
                            SocketManager.GAME_SEND_GA_PACKET_TO_MAP(follower.curMap, "0", 1, follower.id, path)
                        }
                    }
                }
            } catch (ignored: Exception) {
            }
        }
        this.players.stream().filter { isWithTheMaster(it, false, false) }.forEach { it.blockMovement = false }
    }

    fun isWithTheMaster(follower: Player, inFight: Boolean, changeMap: Boolean): Boolean {
        val master = this.master
        val sameMap = changeMap || (master != null && master.curMap === follower.curMap && master.curMap.id == follower.curMap.id)

        return sameMap && master != null && follower.name != master.name && this.players.contains(follower) && follower.gameClient != null &&
            this.haveSameIp(follower) && (if (inFight) follower.fight === master.fight else follower.fight == null)
    }

    private fun haveSameIp(follower: Player): Boolean {
        val account = follower.account
        val master = this.master
        if (account != null && master != null && master.account != null)
            return account.currentIp == master.account.currentIp
        return false
    }

    fun getOptionByPlayer(player: Player): MasterOption? {
        if (this.players.contains(player)) {
            for (option in options)
                if (option.player.id == player.id)
                    return option
            val option = MasterOption(player)
            options.add(option)
            return option
        }
        return null
    }

    fun getOptions(): ArrayList<MasterOption> = options

    class MasterOption(internal val player: Player) {

        var second: Byte = 1
        private var pass: Byte = 0

        fun togglePass() {
            this.pass = (if (this.pass.toInt() == 0) 1 else 0).toByte()
        }

        fun passAuto(): Boolean = pass.toInt() == 1
    }
}
