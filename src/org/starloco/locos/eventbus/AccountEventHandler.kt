package org.starloco.locos.eventbus

import org.starloco.locos.annotation.Handler
import org.starloco.locos.proto.AccountQueuePositionMessage

class AccountEventHandler {

    @Handler
    fun onQueue(message: AccountQueuePositionMessage) {
        message.client!!.send(AccountQueuePositionMessage(1, 1, 1, 1))
    }
}
