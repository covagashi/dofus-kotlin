package org.starloco.locos.job

import org.starloco.locos.client.Player
import org.starloco.locos.common.SocketManager
import org.starloco.locos.util.TimerWaiter

import java.util.concurrent.TimeUnit

class JobCraft(val jobAction: JobAction, player: Player) {

    private var time = 0
    private var itsOk = true

    init {
        TimerWaiter.addNext({
            if (itsOk) jobAction.craft(false)
        }, CRAFT_TIME.toLong(), TimeUnit.MILLISECONDS)
        TimerWaiter.addNext({
            if (!itsOk) repeat(time, time, player)
        }, CRAFT_TIME.toLong(), TimeUnit.MILLISECONDS)
    }

    fun setAction(time: Int) {
        this.time = time
        this.jobAction.broken = false
        this.itsOk = false
    }

    private fun repeat(time1: Int, time2: Int, player: Player) {
        val j = time1 - time2
        this.jobAction.player = player
        this.jobAction.isRepeat = true

        if (!this.check(player, j, time2) || time2 <= 0) {
            this.end()
        } else {
            TimerWaiter.addNext({ this.repeat(time1, time2 - 1, player) }, CRAFT_TIME.toLong(), TimeUnit.MILLISECONDS)
        }
    }

    private fun check(player: Player, j: Int, time2: Int): Boolean {
        if (this.jobAction.broke || this.jobAction.broken || player.exchangeAction == null || !player.isOnline) {
            if (player.exchangeAction == null)
                this.jobAction.broken = true
            if (player.isOnline)
                SocketManager.GAME_SEND_Ea_PACKET(this.jobAction.player!!, if (this.jobAction.broken) "2" else "4")
            return false
        } else {
            SocketManager.GAME_SEND_EA_PACKET(this.jobAction.player!!, time2.toString())
            this.jobAction.craft(this.jobAction.isRepeat)
            if (!this.jobAction.isMaging())
                this.jobAction.ingredients.clear()
            this.jobAction.ingredients.putAll(this.jobAction.lastCraft)
            return true
        }
    }

    private fun end() {
        SocketManager.GAME_SEND_Ea_PACKET(this.jobAction.player!!, "1")
        this.jobAction.isRepeat = false
        this.jobAction.jobCraft = null

        if (!this.jobAction.isMaging()) {
            this.jobAction.ingredients.clear()
        }
    }

    companion object {
        private const val CRAFT_TIME: Short = 500
    }
}
