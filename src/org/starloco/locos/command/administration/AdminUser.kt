package org.starloco.locos.command.administration

import org.starloco.locos.client.Account
import org.starloco.locos.client.Player
import org.starloco.locos.common.SocketManager
import org.starloco.locos.game.GameClient
import org.starloco.locos.kernel.Config
import org.starloco.locos.kernel.Main
import javax.swing.Timer
import java.awt.event.ActionListener

abstract class AdminUser(player: Player?) {

    var account: Account? = null
        private set
    var player: Player? = null
        private set
    var client: GameClient? = null
        private set

    protected var isTimerStart = false
    protected var timer: Timer? = null

    init {
        if (player != null) {
            this.account = player.account
            this.player = player
            this.client = player.account.gameClient
        }
    }

    protected open fun createTimer(timer: Int): Timer {
        var time = timer
        val action = ActionListener {
            time -= 1
            if (time == 1)
                SocketManager.GAME_SEND_Im_PACKET_TO_ALL("115;$time minute")
            else
                SocketManager.GAME_SEND_Im_PACKET_TO_ALL("115;$time minutes")
            if (time <= 0) Main.stop("Shutdown by an administrator")
        }
        return Timer(60000, action)
    }

    open fun sendMessage(message: String) {
        this.player!!.send(buildBAT(0, message))
    }

    protected open fun sendErrorMessage(message: String) {
        this.player!!.send(buildBAT(1, message))
    }

    protected open fun sendSuccessMessage(message: String) {
        this.player!!.send(buildBAT(2, message))
    }

    private fun buildBAT(flag: Int, message: String): String {
        return "BAT" +
            flag.toString() +
            (if (Config.isVersionGreaterThan("1.35.0")) "|12||" else "") +
            message
    }

    abstract fun apply(packet: String)
}
