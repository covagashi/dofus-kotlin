package org.starloco.locos.exchange

import org.apache.mina.core.buffer.IoBuffer
import org.apache.mina.core.service.IoHandlerAdapter
import org.apache.mina.core.session.IoSession
import org.starloco.locos.kernel.Config

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
        cause.printStackTrace()
    }

    companion object {
        @JvmStatic
        fun ioBufferToString(o: Any): String {
            val data = o as IoBuffer
            val buf = ByteArray(data.limit())
            data.get(buf)
            return String(buf)
        }
    }
}
