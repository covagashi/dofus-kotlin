package org.starloco.locos.script

import org.classdump.luna.ByteString
import org.classdump.luna.StateContext
import org.classdump.luna.Table
import org.classdump.luna.Variable
import org.classdump.luna.compiler.CompilerChunkLoader
import org.classdump.luna.env.RuntimeEnvironment
import org.classdump.luna.env.RuntimeEnvironments
import org.classdump.luna.exec.CallException
import org.classdump.luna.exec.CallPausedException
import org.classdump.luna.exec.DirectCallExecutor
import org.classdump.luna.impl.DefaultTable
import org.classdump.luna.impl.ImmutableTable
import org.classdump.luna.impl.StateContexts
import org.classdump.luna.lib.*
import org.classdump.luna.load.ChunkLoader
import org.classdump.luna.load.LoaderException
import org.classdump.luna.runtime.ExecutionContext
import org.classdump.luna.runtime.LuaFunction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.starloco.locos.client.Player
import org.starloco.locos.game.world.World
import org.starloco.locos.game.world.World.Couple
import org.starloco.locos.util.Pair
import java.io.IOException
import java.net.URI
import java.nio.file.*
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedList
import java.util.Optional
import java.util.StringJoiner
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Stream

open class ScriptVM @Throws(CallException::class, LoaderException::class, IOException::class, CallPausedException::class, InterruptedException::class)
protected constructor(name: String) {

    @JvmField
    protected val env: Table
    @JvmField
    protected val state: StateContext
    @JvmField
    protected val loader: ChunkLoader
    @JvmField
    protected val executor: DirectCallExecutor = DirectCallExecutor.newExecutor()

    @JvmField
    protected val _vmLock = Any() // Find a better way if performances become an issue

    init {
        this.state = StateContexts.newDefaultInstance()
        val rtEnv: RuntimeEnvironment = RuntimeEnvironments.system()

        this.loader = CompilerChunkLoader.of(name)
        this.env = state.newTable()
        BasicLib.installInto(state, env, rtEnv, null)
        ModuleLib.installInto(state, env, rtEnv, this.loader, null)
        CoroutineLib.installInto(state, env)
        StringLib.installInto(state, env)
        MathLib.installInto(state, env)
        TableLib.installInto(state, env)

        this.env.rawset("JLogF", LogF())
        this.env.rawset("loadPack", LoadPack())
        this.env.rawset("World", World.world.scripted())
    }

    @Throws(CallException::class, LoaderException::class, IOException::class, CallPausedException::class, InterruptedException::class)
    protected open fun loadData() {
        runFile(Paths.get("scripts", "Common.lua"))
    }

    protected fun runArchive(path: Path) {
        try {
            FileSystems.newFileSystem(URI.create("jar:" + path.normalize().toUri()), emptyMap<String, Any>()).use { fs ->
                fs.rootDirectories
                    .forEach { root ->
                        try {
                            Files.walk(root).use { paths -> runPathStream(paths) }
                        } catch (e: Exception) {
                            logger.error("cannot load directory", e)
                        }
                    }
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun runPathStream(paths: Stream<Path>) {
        paths
            .filter { Files.isRegularFile(it) }
            .filter { it.toString().endsWith(".lua") }
            .forEach { p ->
                try {
                    this.runFile(p)
                } catch (e: Exception) {
                    logger.error("cannot load file: $p", e)
                }
            }
    }

    protected fun runDirectoryOrArchive(path: Path) {
        val archivePath = Paths.get(path.toString() + ".zip")

        if (Files.exists(archivePath)) {
            runArchive(archivePath)
            return
        }

        try {
            Files.walk(path).use { paths -> runPathStream(paths) }
        } catch (e: Exception) {
            logger.error("cannot load directory", e)
        }
    }

    @Throws(IOException::class, LoaderException::class, CallException::class, CallPausedException::class, InterruptedException::class)
    protected fun runFile(path: Path) {
        val bytes = Files.readAllBytes(path)

        val fn: LuaFunction<*, *, *, *, *> = loader.loadTextChunk(Variable(env), path.toString(), String(bytes)) // May need to UTF-8 decode

        this.executor.call(this.state, fn)
    }

    fun call(`val`: Any?, vararg args: Any?): Array<Any> {
        synchronized(_vmLock) {
            try {
                return this.executor.call(this.state, `val`, *args)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

    fun runCustomized(code: String, customizer: Map<String, Any>): Array<Any> {
        synchronized(_vmLock) {
            val original = HashMap<String, Any>()

            customizer.forEach { (name, _) ->
                // Back up existing values
                original[name] = env.rawget(name)

                // Apply overrides
                customizer.forEach { (n, v) -> env.rawset(n, v) }
            }

            try {
                val fn: LuaFunction<*, *, *, *, *> = loader.loadTextChunk(Variable(env), "command", code) // May need to UTF-8 decode
                return this.executor.call(this.state, fn)
            } catch (e: Exception) {
                throw RuntimeException(e)
            } finally {
                // Restore original values
                original.forEach { (n, v) -> env.rawset(n, v) }
            }
        }
    }

    fun runAdminCommand(player: Player, code: String, cbPrint: Consumer<String>): Array<Any> {
        val overrides = HashMap<String, Any>()
        overrides["print"] = printOverwrite(cbPrint)
        overrides["_me"] = player.scripted()

        return runCustomized(code, overrides)
    }

    internal inner class LoadPack : AbstractLibFunction() {
        override fun name(): String = "LoadPack"

        override fun invoke(context: ExecutionContext, args: ArgumentIterator) {
            val path = Paths.get("scripts", args.nextStrictString().toString())

            runDirectoryOrArchive(path)

            context.returnBuffer.setTo(true)
        }
    }

    companion object {
        @JvmField
        val logger: Logger = LoggerFactory.getLogger("Script")

        @JvmStatic
        fun recursiveGet(t: Table?, key: Any): Any? {
            if (t == null) return null

            val v = t.rawget(key)
            if (v != null) return v
            val mtIndex = t.metatable?.rawget("__index")
            if (mtIndex !is Table) return null

            return recursiveGet(mtIndex, key)
        }

        class LogF : AbstractLibFunction() {
            override fun name(): String = "JLogF"

            override fun invoke(context: ExecutionContext, args: ArgumentIterator) {
                val fmt = args.nextString()
                logger.info(fmt.toString(), args.copyRemaining())
            }
        }

        @JvmStatic
        fun rawOptionalInt(v: Table, key: Any, `val`: Int): Int {
            val n = v.rawget(key) ?: return `val`
            return (v.rawget(key) as Long).toInt()
        }

        @JvmStatic
        fun rawOptional(v: Table, key: Any): Optional<Any> = Optional.ofNullable(v.rawget(key))

        @JvmStatic
        fun rawOptionalString(t: Table, key: Any): String? =
            rawOptional(t, key).map { it.toString() }.orElse(null)

        @JvmStatic
        fun rawInt(v: Table, key: Any): Int = (v.rawget(key) as Long).toInt()

        @JvmStatic
        fun rawInteger(v: Table, key: Any): Int? {
            val l = v.rawget(key) as Long? ?: return null
            return l.toInt()
        }

        @JvmStatic
        fun rawInt(v: Table, key: Long): Int = (v.rawget(key) as Long).toInt()

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> listFromLuaTable(t: Table): MutableList<T> {
            val out = ArrayList<T>()

            val len = t.rawlen()
            for (i in 1..len.toInt()) {
                out.add(t.rawget(i) as T)
            }

            return out
        }

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <K, V> toPair(t: Table): Pair<K, V> =
            Pair(t.rawget(1L) as K, t.rawget(2L) as V)

        @JvmStatic
        fun listOfIntPairs(t: Table?): MutableList<Pair<Int, Int>>? {
            if (t == null) return null

            val len = t.rawlen()
            val out = LinkedList<Pair<Int, Int>>()

            for (i in 0 until len) {
                out.add(toPair<Long, Long>(t.rawget(i + 1) as Table).map({ it.toInt() }, { it.toInt() }))
            }

            return out
        }

        @JvmStatic
        fun listOfString(t: Table?): MutableList<String>? {
            if (t == null) return null

            val len = t.rawlen()
            val out = LinkedList<String>()

            for (i in 0 until len) {
                val bs = t.rawget(i + 1) as ByteString
                out.add(bs.toString())
            }

            return out
        }

        @JvmStatic
        fun intsFromLuaTable(t: Table?): MutableList<Int> {
            val out = ArrayList<Int>()
            if (t == null) return out

            val len = t.rawlen()
            for (i in 1..len.toInt()) {
                out.add(rawInt(t, i))
            }

            return out
        }

        @JvmStatic
        fun intArrayFromLuaTable(t: Table?): IntArray? {
            if (t == null) return null

            val len = t.rawlen()
            val out = IntArray(len.toInt())

            for (i in 0 until len.toInt()) {
                out[i] = rawInt(t, i + 1)
            }

            return out
        }

        @JvmStatic
        fun longArrayFromLuaTable(t: Table?): LongArray? {
            if (t == null) return null

            val len = t.rawlen()
            val out = LongArray(len.toInt())

            for (i in 0 until len.toInt()) {
                out[i] = t.rawget(i + 1) as Long
            }

            return out
        }

        @JvmStatic
        fun ItemStack(stack: Couple<Int, Int>): Table =
            ImmutableTable.Builder()
                .add("itemID", stack.first)
                .add("quantity", stack.second)
                .build()

        @JvmStatic
        fun ItemStackFromLua(t: Table): Couple<Int, Int> {
            val id = rawInt(t, "itemID")
            val qua = rawInt(t, "quantity")
            return Couple(id, qua)
        }

        @JvmStatic
        fun <K, V> mapFromScript(t: Table, keyMapper: Function<Any?, K>, valMapper: Function<Any?, V>): Map<K, V> {
            val out = HashMap<K, V>()

            var key = t.initialKey()
            while (key != null) {
                val `val` = t.rawget(key)

                out[keyMapper.apply(key)] = valMapper.apply(`val`)
                key = t.successorKeyOf(key)
            }

            return out
        }

        @JvmStatic
        fun scriptedValsTable(vals: Collection<Scripted<*>>): Table =
            scriptedValsTable(vals.stream())

        @JvmStatic
        fun scriptedValsTable(vals: Stream<out Scripted<*>>): Table {
            val out = DefaultTable()

            var i = 1
            val it = vals.iterator()
            while (it.hasNext()) {
                out.rawset(i, it.next().scripted())
                i++
            }
            return out
        }

        @JvmStatic
        fun <T> listOf(vals: Stream<T>): Table {
            val out = DefaultTable()
            var i = 1
            val it = vals.iterator()
            while (it.hasNext()) {
                out.rawset(i, it.next())
                i++
            }
            return out
        }

        private fun printOverwrite(cbPrint: Consumer<String>): AbstractLibFunction {
            return object : AbstractLibFunction() {
                override fun name(): String = "print"

                override fun invoke(context: ExecutionContext, args: ArgumentIterator) {
                    val sj = StringJoiner(" ")

                    args.forEachRemaining { sj.add(it.toString()) }

                    cbPrint.accept(sj.toString())

                    context.returnBuffer.setTo()
                }
            }
        }
    }
}
