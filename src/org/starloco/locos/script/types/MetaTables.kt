package org.starloco.locos.script.types

import org.classdump.luna.Metatables
import org.classdump.luna.Table
import org.classdump.luna.Userdata
import org.classdump.luna.impl.DefaultUserdata
import org.classdump.luna.impl.ImmutableTable
import org.classdump.luna.lib.AbstractLibFunction
import org.classdump.luna.lib.ArgumentIterator
import org.classdump.luna.runtime.ExecutionContext
import org.starloco.locos.script.ScriptVM
import org.starloco.locos.util.Pair
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.util.function.BiConsumer

object MetaTables {

    private val returnValueConsumers: MutableMap<Class<*>, BiConsumer<ExecutionContext, Any>> = hashMapOf(
        Pair::class.java to BiConsumer { ctx, v ->
            val p = v as Pair<*, *>
            ctx.returnBuffer.setTo(p.first, p.second)
        }
    )

    // Creates index table from a POJO using declared methods
    // Only methods with the following signatures are allowed:
    // static void method(T)
    // static void method(T, ArgumentIterator)
    // static ? method(T)
    // static ? method(T, ArgumentIterator)
    // TODO: support multiple return values
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T, S : Userdata<T>> ReflectIndexTable(c: Class<S>): Table {
        val b = ImmutableTable.Builder()

        val genSuper = c.genericSuperclass as ParameterizedType
        require(genSuper.rawType == DefaultUserdata::class.java) { "ReflectIndexTable param must directly extend DefaultUserdata" }

        val pojoClass = genSuper.actualTypeArguments[0] as Class<T>

        for (m in c.declaredMethods) {
            val params = m.parameters
            if (!Modifier.isStatic(m.modifiers) || params.isEmpty() || params.size > 2) continue

            // Ensure 1st param type is T
            if (params[0].type != pojoClass) continue

            // Ensure 2nd param type is ArgumentIterator if present
            val has2ndParam = params.size == 2
            if (has2ndParam && params[1].type != ArgumentIterator::class.java) continue

            m.isAccessible = true // Make the method accessible even if private
            b.add(m.name, object : AbstractLibFunction() {
                override fun name(): String = m.name

                override fun invoke(context: ExecutionContext, args: ArgumentIterator) {
                    val t = args.nextUserdata(c.typeName, c).userValue

                    try {
                        val ret = if (has2ndParam) m.invoke(null, t, args) else m.invoke(null, t)
                        if (ret == null) {
                            context.returnBuffer.setTo()
                            return
                        }
                        // Allow some types to be sent back to Lua differently
                        val consumer = returnValueConsumers.getOrDefault(ret.javaClass,
                            BiConsumer { ctx, v -> ctx.returnBuffer.setTo(v) })
                        consumer.accept(context, ret)
                    } catch (e: Exception) {
                        throw RuntimeException(e)
                    }
                }
            })

            ScriptVM.logger.debug("Found script method {}:{}", c.simpleName, m.name)
        }

        return b.build()
    }

    @JvmStatic
    fun MetaTable(index: Table): ImmutableTable =
        ImmutableTable.Builder().add(Metatables.MT_INDEX, index).build()
}
