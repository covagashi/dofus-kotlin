package org.starloco.locos.exchange

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.apache.mina.core.buffer.IoBuffer
import org.apache.mina.core.future.ConnectFuture
import org.apache.mina.core.service.IoConnector
import org.apache.mina.core.session.IoSession
import org.apache.mina.transport.socket.nio.NioSocketConnector
import org.slf4j.LoggerFactory
import org.starloco.locos.kernel.Config
import java.net.InetSocketAddress

class ExchangeClient {

    var ioSession: IoSession? = null
    var connectFuture: ConnectFuture? = null
    private var ioConnector: IoConnector = NioSocketConnector()

    init {
        this.ioConnector.handler = ExchangeHandler()
        Config.exchangeClient = this
        logger.setLevel(Level.INFO)
    }

    fun initialize() {
        try {
            this.connectFuture = this.ioConnector.connect(InetSocketAddress(Config.exchangeIp!!, Config.exchangePort))
        } catch (e: Exception) {
            logger.error("The game server don't found the login server. Exception : " + e.message)
            try {
                Thread.sleep(2000)
            } catch (ignored: Exception) {
            }
            return
        }

        try {
            Thread.sleep(3000)
        } catch (ignored: Exception) {
        }

        if (!ioConnector.isActive) {
            if (!Config.isRunning) return

            logger.error("Try to connect to the login server..")
            restart()
            return
        }
        logger.info("The exchange client was connected on address : " + Config.exchangeIp + ":" + Config.exchangePort)
    }

    fun restart() {
        if (!Config.isRunning) return

        logger.error("The login server was not found..")

        this.stop()
        this.connectFuture = null
        this.ioConnector = NioSocketConnector()
        this.ioConnector.handler = ExchangeHandler()
        this.initialize()
    }

    fun stop() {
        this.ioSession?.close(true)
        this.connectFuture?.cancel()

        this.connectFuture = null
        this.ioConnector.dispose()
        logger.info("The exchange client was stopped.")
    }

    fun send(packet: String) {
        val session = this.ioSession
        if (session != null && !session.isClosing && session.isConnected)
            session.write(StringToIoBuffer(packet))
    }

    companion object {
        @JvmField
        var logger: Logger = LoggerFactory.getLogger(ExchangeClient::class.java) as Logger

        @JvmStatic
        fun StringToIoBuffer(packet: String): IoBuffer {
            val bytes = packet.toByteArray()
            val ioBuffer = IoBuffer.allocate(bytes.size)
            ioBuffer.put(bytes)
            return ioBuffer.flip()
        }
    }
}
