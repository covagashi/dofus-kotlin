package org.starloco.locos.fight.ia

import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.util.TimerWaiter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by Locos on 18/09/2015.
 */
abstract class AbstractIA(
    @JvmField protected var fight: Fight,
    @JvmField protected var fighter: Fighter,
    @JvmField protected var count: Byte
) : IA {

    @JvmField
    protected var stop: Boolean = false

    override fun getFight(): Fight = fight

    override fun getFighter(): Fighter = fighter

    override fun isStop(): Boolean = stop

    override fun setStop(stop: Boolean) {
        this.stop = stop
    }

    override fun endTurn() {
        this.fight.endTurn(false, this.fighter)
    }

    protected fun decrementCount() {
        this.count--
        if (this.stop || this.count.toInt() == 0) {
            this.endTurn()
        } else {
            this.apply()
        }
    }

    override fun addNext(runnable: Runnable, time: Int) {
        executor.schedule(TimerWaiter.catchRunnable(runnable), time.toLong(), TimeUnit.MILLISECONDS)
    }

    companion object {
        private val executor = Executors.newScheduledThreadPool(5) { r -> Thread(r, "AbstractIA") }
    }
}
