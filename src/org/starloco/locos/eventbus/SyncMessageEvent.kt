package org.starloco.locos.eventbus

import org.starloco.locos.api.AbstractEventMessageDispatcher

class SyncMessageEvent<T> : AbstractEventMessageDispatcher<T>() {

    override fun publish(parameterObject: T) {
        doPublish(parameterObject)
    }
}
