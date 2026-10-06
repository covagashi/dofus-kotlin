package org.starloco.locos.game

import org.apache.mina.core.service.IoHandler
import org.apache.mina.core.session.IdleStatus
import org.apache.mina.core.session.IoSession
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.starloco.locos.api.AbstractDofusMessage
import org.starloco.locos.factory.DofusMessageFactory
import org.starloco.locos.factory.EventDispatcherFactory
import org.starloco.locos.game.filter.PacketFilter
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Config

class GameHandler : IoHandler {

    override fun sessionCreated(arg0: IoSession) {
        if (!filter.authorizes(arg0.remoteAddress.toString().substring(1).split(":")[0])) {
            arg0.close(true)
        } else {
            World.world.logger.info("Session " + arg0.id + " created")
            arg0.setAttribute("client", GameClient(arg0))
        }
    }

    override fun messageReceived(arg0: IoSession, arg1: Any) {
        val client = arg0.getAttribute("client") as GameClient
        var packet = arg1 as String

        if (client != null) {
            if (Config.encryption && !packet.startsWith("AT") && !packet.startsWith("Ak")) {
                packet = World.world.cryptManager.decryptMessage(packet, client.preparedKeys)
                if (packet != null) packet = packet.replace("\n", "")
                else packet = arg1 as String
            }

            val s = packet.split("\n")

            for (p in s) {
                var p = p
                if (p[0] == 'ù') {
                    if (p.split("ù").size < 3) continue
                    p = p.split("ù")[2]
                }
                try {
                    if (p.length > 1) {
                        val abstractDofusMessage = DofusMessageFactory.getMessage(p.substring(0, 2))
                        if (abstractDofusMessage != null) {
                            abstractDofusMessage.input = StringBuilder(p.substring(2))
                            abstractDofusMessage.deserialize()
                            abstractDofusMessage.client = client
                            logger.info(
                                "Receive message: {} with header: {}",
                                abstractDofusMessage.javaClass.name,
                                p.substring(0, 2)
                            )
                            EventDispatcherFactory.dispatch(abstractDofusMessage)
                            return
                        }
                    }
                    client.parsePacket(p)
                } catch (e: Exception) {
                    throw Exception("Cannot process packet: $p", e)
                } finally {
                    if (Config.debug) {
                        World.world.logger.trace((if (client.player == null) "" else client.player.name) + " <-- " + p)
                    }
                }
            }
        }
    }

    override fun sessionClosed(arg0: IoSession) {
        this.kick(arg0)
        World.world.logger.info("Session " + arg0.id + " closed")
    }

    override fun exceptionCaught(arg0: IoSession, arg1: Throwable) {
        if (arg1.message != null && (arg1 is org.apache.mina.filter.codec.RecoverableProtocolDecoderException || arg1.message!!.startsWith("Une connexion ") ||
                    arg1.message!!.startsWith("Connection reset by peer") || arg1.message!!.startsWith("Connection timed out"))
        )
            return
        arg1.printStackTrace()
        if (Config.debug)
            World.world.logger.error("Exception connexion client : ", arg1)
        this.kick(arg0)
    }

    override fun messageSent(arg0: IoSession, arg1: Any) {
        val client = arg0.getAttribute("client") as GameClient

        if (client != null) {
            if (Config.debug) {
                var packet = arg1 as String
                if (Config.encryption && !packet.startsWith("AT") && !packet.startsWith("HG"))
                    packet = World.world.cryptManager.decryptMessage(packet, client.preparedKeys).replace("\n", "")
                if (packet.startsWith("am")) return
                World.world.logger.trace((if (client.player == null) "" else client.player.name) + " --> " + packet)
            }
        }
    }

    override fun inputClosed(ioSession: IoSession) {
        ioSession.close(true)
    }

    override fun sessionIdle(arg0: IoSession, arg1: IdleStatus) {
        World.world.logger.info("Session " + arg0.id + " idle")
    }

    override fun sessionOpened(arg0: IoSession) {
        World.world.logger.info("Session " + arg0.id + " opened")
    }

    fun kick(arg0: IoSession) {
        val client = arg0.getAttribute("client") as GameClient
        if (client != null) {
            client.disconnect()
            client.kick()
            arg0.setAttribute("client", null)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(GameHandler::class.java)
        private val filter = PacketFilter().activeSafeMode()
    }
}
