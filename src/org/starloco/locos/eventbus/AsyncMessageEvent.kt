package org.starloco.locos.eventbus

import org.starloco.locos.api.AbstractEventMessageDispatcher
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class AsyncMessageEvent<T>(private val executor: Executor) : AbstractEventMessageDispatcher<T>() {

    constructor() : this(Executors.newCachedThreadPool())

    override fun publish(parameterObject: T) {
        executor.execute { doPublish(parameterObject) }
    }
}
