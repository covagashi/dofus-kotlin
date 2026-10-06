package org.starloco.locos.exchange

import org.starloco.locos.command.CommandPlayer
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.login.AccountData
import org.starloco.locos.database.data.login.PlayerData
import org.starloco.locos.game.GameServer
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Config
import org.starloco.locos.kernel.Main
import java.text.SimpleDateFormat
import java.util.Date
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(ExchangePacketHandler::class.java)

internal object ExchangePacketHandler {

    fun parser(packet: String) {
        if (packet.isEmpty()) return
        try {
            when (packet[0]) {
                'F' -> //Free places
                    if (packet[1] == '?') { //Required
                        val i = GameServer.MAX_PLAYERS - World.world.onlinePlayers.size
                        Config.exchangeClient?.send("F$i")
                    }

                'S' -> //Server
                    when (packet[1]) {
                        'H' -> //Host
                            if (packet[2] == 'K') { //Ok
                                ExchangeClient.logger.info("The login server has validated the connection.")
                                GameServer.setState(1)
                            }

                        'K' -> //Key
                            when (packet[2]) {
                                '?' -> { //Required
                                    val i = 50000 - Config.gameServer!!.getClients().size
                                    Config.exchangeClient?.send("SK" + Config.gameServerId + ";" + Config.gameServerKey + ";" + i)
                                }

                                'K' -> { //Ok
                                    ExchangeClient.logger.info("The login server has accepted the connection.")
                                    Config.exchangeClient?.send("SH" + Config.gameIp + ";" + Config.gamePort)
                                }

                                'R' -> { //Refused
                                    ExchangeClient.logger.info("The login server has refused the connection.")
                                    Main.stop("Connection refused by the login")
                                }
                            }
                    }

                'W' -> //Waiting
                    when (packet[1]) {
                        'A' -> { //Add
                            val id = (packet.substring(2)).toInt()
                            val account = World.world.ensureAccountLoaded(id)

                            if (account == null) {
                                // Account doesn't exist, TODO: Send error
                                return
                            }

                            if (account.currentPlayer != null)
                                account.gameClient?.kick()
                            account.setSubscribe()
                            Config.gameServer!!.addWaitingAccount(account)
                        }

                        'K' -> { //Kick
                            val id = (packet.substring(2)).toInt()
                            DatabaseManager.get(PlayerData::class.java).updateLogged(id, 0)
                            DatabaseManager.get(AccountData::class.java).setLogged(id, 0)
                            val account = World.world.ensureAccountLoaded(id)

                            if (account != null) {
                                val client = account.gameClient
                                if (client != null) {
                                    client.disconnect()
                                    client.kick()
                                }
                            }
                        }
                    }

                'D' -> // Data
                    if (packet[1] == 'M') { // Message
                        val split = packet.substring(2).split(";")
                        if (split.size > 1) {
                            val prefix = "<font color='#C35617'>[" + SimpleDateFormat("HH:mm").format(Date(System.currentTimeMillis())) + "] (" + CommandPlayer.canal + ") (" + split[1] + ") <b>" + split[0] + "</b>"
                            val message = "Im116;$prefix~" + split[2] + "</font>"

                            World.world.onlinePlayers.stream().filter { p -> p != null && !p.noall }.forEach { p -> p.send(message.replace("%20", " ")) }
                        }
                    }
            }
        } catch (e: Exception) {
            log.error("unexpected error", e)
                }
    }
}
