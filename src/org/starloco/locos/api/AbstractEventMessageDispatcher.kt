package org.starloco.locos.api

import org.starloco.locos.annotation.Handler
import org.starloco.locos.invoker.EventDispatcherInvoker

abstract class AbstractEventMessageDispatcher<T> {

    protected val invokers: MutableList<EventDispatcherInvoker<T>> = ArrayList()

    abstract fun publish(parameterObject: T)

    open fun doPublish(message: T) {
        for (invoker in invokers) {
            if (invoker.message!!::class.java.isAssignableFrom(message!!::class.java)) {
                try {
                    invoker.methods.invoke(invoker.handler, message)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    open fun subscribe(handler: Any) {
        for (method in handler.javaClass.declaredMethods) {
            if (method.isAnnotationPresent(Handler::class.java)) {
                require(method.parameterCount == 1) { "Handler method must have only one parameter" }
                @Suppress("UNCHECKED_CAST")
                val parameterType = method.parameterTypes[0] as T
                invokers.add(EventDispatcherInvoker(handler, method, parameterType))
            }
        }
    }
}
