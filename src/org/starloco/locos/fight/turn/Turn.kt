package org.starloco.locos.fight.turn

import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.kernel.Constant
import org.starloco.locos.util.TimerWaiter
import java.util.concurrent.TimeUnit

class Turn(private val fight: Fight, private val fighter: Fighter) : Runnable {

    private val start: Long
    private var stop = false

    init {
        val duration = Constant.TIME_BY_TURN + 2000
        TimerWaiter.addNext(this, duration.toLong(), TimeUnit.MILLISECONDS)
        this.start = System.currentTimeMillis()
    }

    fun getStartTime(): Long = start

    fun stop() {
        this.stop = true
    }

    override fun run() {
        if (this.stop || this.fighter.isDead) {
            this.stop()
            return
        }

        if (this.fight.orderPlaying == null) {
            this.stop()
            return
        }

        if (this.fight.orderPlaying!![this.fight.curPlayer] == null) {
            this.stop()
            return
        }

        if (this.fight.orderPlaying!![this.fight.curPlayer] !== this.fighter) {
            this.stop()
            return
        }
        this.fight.endTurn(false)
    }
}
