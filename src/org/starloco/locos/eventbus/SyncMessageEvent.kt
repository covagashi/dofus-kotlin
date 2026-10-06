package org.starloco.locos.eventbus

import org.starloco.locos.api.AbstractEventMessageDispatcher

class SyncMessageEvent<T> : AbstractEventMessageDispatcher<T>() {

    override fun publish(message: T) {
        doPublish(message)
    }
}
