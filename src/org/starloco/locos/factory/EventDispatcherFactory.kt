package org.starloco.locos.factory

import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.reflections.scanners.SubTypesScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import org.slf4j.LoggerFactory
import org.starloco.locos.annotation.Handler
import org.starloco.locos.api.AbstractDofusMessage
import org.starloco.locos.eventbus.SyncMessageEvent
import java.util.stream.Collectors

class EventDispatcherFactory {

    init {
        syncDofusMessageEvent = SyncMessageEvent()
    }

    fun init() {
        val reflections = Reflections(ConfigurationBuilder().forPackage("org.starloco.locos.eventbus").setScanners(SubTypesScanner(false), MethodAnnotationsScanner()))
        val handlerMethods = reflections.getMethodsAnnotatedWith(Handler::class.java)
        val handlerClasses = handlerMethods.stream().map { it.declaringClass }.collect(Collectors.toSet())

        handlerClasses.forEach { handlerClass ->
            val handler = try {
                handlerClass.getDeclaredConstructor().newInstance()
            } catch (e: ReflectiveOperationException) {
                throw RuntimeException(e)
            }
            LoggerFactory.getLogger(EventDispatcherFactory::class.java).debug("Register handler: {}", handler.javaClass)
            syncDofusMessageEvent.subscribe(handler)
        }
    }

    companion object {
        private lateinit var syncDofusMessageEvent: SyncMessageEvent<AbstractDofusMessage>

        @JvmStatic
        fun dispatch(message: AbstractDofusMessage) {
            syncDofusMessageEvent.publish(message)
        }
    }
}
