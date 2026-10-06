package org.starloco.locos.factory

import org.reflections.Reflections
import org.slf4j.LoggerFactory
import org.starloco.locos.annotation.DofusMessage
import org.starloco.locos.api.AbstractDofusMessage

class DofusMessageFactory {

    fun init() {
        val reflections = Reflections("org.starloco.locos.proto")
        val messageClasses = reflections.getSubTypesOf(AbstractDofusMessage::class.java)
        for (messageClass in messageClasses) {
            val dofusMessage = messageClass.getAnnotation(DofusMessage::class.java)
            if (dofusMessage != null) {
                val header = dofusMessage.header
                messages[header] = messageClass
                LoggerFactory.getLogger(DofusMessageFactory::class.java).info("Register message: {} with header: {}", messageClass.name, header)
            }
        }
    }

    companion object {
        private val messages = HashMap<String, Class<out AbstractDofusMessage>>()

        @JvmStatic
        fun getMessage(header: String): AbstractDofusMessage? =
            messages[header]?.let { clazz ->
                try {
                    clazz.getDeclaredConstructor().newInstance()
                } catch (e: ReflectiveOperationException) {
                    throw RuntimeException(e)
                }
            }
    }
}
