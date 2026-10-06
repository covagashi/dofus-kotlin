package org.starloco.locos.exchange

import org.apache.mina.core.buffer.IoBuffer
import org.apache.mina.core.service.IoHandlerAdapter
import org.apache.mina.core.session.IoSession
import org.starloco.locos.kernel.Config
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(ExchangeHandler::class.java)

class ExchangeHandler : IoHandlerAdapter() {

    override fun sessionCreated(session: IoSession) {
        Config.exchangeClient?.ioSession = session
    }

    override fun messageReceived(session: IoSession, message: Any) {
        val packet = ioBufferToString(message)
        ExchangeClient.logger.debug(packet)
        ExchangePacketHandler.parser(packet)
    }

    override fun messageSent(session: IoSession, message: Any) {
        ExchangeClient.logger.debug(ioBufferToString(message))
    }

    override fun sessionClosed(session: IoSession) {
        Config.exchangeClient?.restart()
    }

    override fun exceptionCaught(session: IoSession, cause: Throwable) {
        log.error("unexpected error", cause)
                }

    companion object {
        @JvmStatic
        fun ioBufferToString(o: Any): String {
            val data = o as IoBuffer
            val buf = ByteArray(data.limit())
            data[buf]
            return String(buf)
        }
    }
}
