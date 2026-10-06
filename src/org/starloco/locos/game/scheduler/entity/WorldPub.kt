package org.starloco.locos.game.scheduler.entity

import org.starloco.locos.common.Formulas
import org.starloco.locos.common.SocketManager
import org.starloco.locos.game.scheduler.Updatable
import org.starloco.locos.util.TimerWaiter
import java.util.ArrayList

class WorldPub(wait: Int) : Updatable<Void>(wait) {

    private var last = 0

    override fun update() {
        if (ads.isNotEmpty()) {
            if (this.verify()) {
                var pub: Int
                do {
                    pub = Formulas.getRandomValue(0, ads.size - 1)
                } while (pub == last)

                last = pub
                SocketManager.GAME_SEND_MESSAGE_TO_ALL("(Message Auto) : " + ads[pub], "046380")
                TimerWaiter.update()
            }
        }
    }

    override fun get(): Void? = null

    companion object {
        @JvmField
        val instance = WorldPub(600000)
        @JvmField
        val ads = ArrayList<String>()
    }
}
