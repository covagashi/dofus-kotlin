package org.starloco.locos.script

import org.classdump.luna.ByteString
import org.classdump.luna.Table
import org.classdump.luna.exec.CallException
import org.classdump.luna.exec.CallPausedException
import org.classdump.luna.impl.NonsuspendableFunctionException
import org.classdump.luna.lib.AbstractLibFunction
import org.classdump.luna.lib.ArgumentIterator
import org.classdump.luna.load.LoaderException
import org.classdump.luna.runtime.AbstractFunction1
import org.classdump.luna.runtime.ExecutionContext
import org.starloco.locos.anims.Animation
import org.starloco.locos.anims.KeyFrame
import org.starloco.locos.area.map.ScriptMapData
import org.starloco.locos.command.administration.Command
import org.starloco.locos.command.administration.Group
import org.starloco.locos.database.data.game.ExperienceTables
import org.starloco.locos.entity.map.InteractiveObjectTemplate
import org.starloco.locos.entity.npc.NpcTemplate
import org.starloco.locos.game.world.World
import java.io.IOException
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.Collections

class DataScriptVM private constructor() : ScriptVM("Data") {

    @JvmField
    val handlers: EventHandlers = EventHandlers(this)

    @Throws(CallException::class, LoaderException::class, IOException::class, CallPausedException::class, InterruptedException::class)
    override fun loadData() {
        super.loadData()
        this.customizeEnv()
        this.runFile(Paths.get("scripts", "Data.lua"))
    }

    fun safeLoadData() {
        try {
            this.loadData()
        } catch (e: Exception) {
            logger.error("Failed to load static data", e)
        }
    }

    private fun customizeEnv() {
        this.env.rawset("RegisterNPCDef", RegisterNpcTemplate())
        this.env.rawset("RegisterAdminCommand", RegisterAdminCommand())
        this.env.rawset("RegisterAdminGroup", RegisterAdminGroup())
        this.env.rawset("RegisterExpTables", RegisterExpTables())
        this.env.rawset("RegisterMapDef", RegisterMapTemplate())
        this.env.rawset("RegisterAnimation", RegisterAnimation())
        this.env.rawset("RegisterObjectForSprites", RegisterObjectForSprites())
        this.env.rawset("RegisterObjectDef", RegisterObjectDef())
        this.env.rawset("Handlers", handlers)
    }

    class RegisterNpcTemplate : AbstractFunction1<Table>() {
        override fun invoke(context: ExecutionContext, v: Table) {
            World.world.addNpcTemplate(NpcTemplate(v))
            context.returnBuffer.setTo()
        }

        override fun resume(context: ExecutionContext, suspendedState: Any) {
            throw NonsuspendableFunctionException()
        }
    }

    class RegisterMapTemplate : AbstractFunction1<Table>() {
        override fun invoke(context: ExecutionContext, v: Table) {
            try {
                World.world.addMapData(ScriptMapData.build(v))
            } catch (e: Exception) {
                logger.error("Cannot register map #" + rawInt(v, "id"), e)
            }
            context.returnBuffer.setTo()
        }

        override fun resume(context: ExecutionContext, suspendedState: Any) {
            throw NonsuspendableFunctionException()
        }
    }

    class RegisterAdminCommand : AbstractLibFunction() {
        override fun name(): String = "RegisterAdminCommand"

        override fun invoke(context: ExecutionContext, args: ArgumentIterator) {
            val cName = args.nextString().toString()
            val cArgs = args.nextOptionalString(ByteString.of("")).toString()
            val cDesc = args.nextOptionalString(ByteString.of("")).toString()

            Command(cName, cArgs, cDesc)
            context.returnBuffer.setTo()
        }
    }

    class RegisterAdminGroup : AbstractLibFunction() {
        override fun name(): String = "RegisterAdminGroup"

        override fun invoke(context: ExecutionContext, args: ArgumentIterator) {
            val gId = args.nextInt()
            val gName = args.nextString().toString()
            val gIsPlayer = args.nextBoolean()
            val gCommands = args.next()

            if (gCommands is Boolean) {
                Group(gId, gName, gIsPlayer, gCommands, emptyList())
            } else if (gCommands is Table) {
                Group(gId, gName, gIsPlayer, false, listOfString(gCommands)!!)
            } else {
                throw IllegalArgumentException("RegisterAdminGroup with invalid commands param")
            }

            context.returnBuffer.setTo()
        }
    }

    class RegisterExpTables : AbstractLibFunction() {
        override fun name(): String = "RegisterExpTables"

        override fun invoke(context: ExecutionContext, args: ArgumentIterator) {
            val players = longArrayFromLuaTable(args.nextTable())
            val guilds = longArrayFromLuaTable(args.nextTable())
            val jobs = longArrayFromLuaTable(args.nextTable())
            val mounts = longArrayFromLuaTable(args.nextTable())
            val pvp = longArrayFromLuaTable(args.nextTable())
            val livitinems = longArrayFromLuaTable(args.nextTable())
            val tormentators = longArrayFromLuaTable(args.nextTable())
            val bandits = longArrayFromLuaTable(args.nextTable())


            val tables = ExperienceTables(players!!, guilds!!, jobs!!, mounts!!, pvp!!, livitinems!!, tormentators!!, bandits!!)
            World.world.experiences = tables

            context.returnBuffer.setTo()
        }
    }

    class RegisterAnimation : AbstractLibFunction() {
        override fun name(): String = "RegisterAnimation"

        override fun invoke(context: ExecutionContext, args: ArgumentIterator) {
            val spriteID = args.nextInt()
            val defaultFrame = args.nextString().toString()
            val tKeyFrames = args.nextTable()

            val keyFrames = mapFromScript(tKeyFrames,
                { it.toString() },
                { KeyFrame.fromScriptValue(it as Table) }
            )

            if (defaultFrame !in keyFrames) {
                throw IllegalArgumentException("default frame is not a key frame")
            }

            val anim = Animation(spriteID, defaultFrame, keyFrames)
            World.world.addAnimation(anim)
            context.returnBuffer.setTo(anim)
        }
    }

    class RegisterObjectForSprites : AbstractLibFunction() {
        override fun name(): String = "RegisterObjectForSprites"

        override fun invoke(context: ExecutionContext, args: ArgumentIterator) {
            val map = mapFromScript(args.nextTable(),
                { (it as Long).toInt() },
                { (it as Long).toInt() }
            )

            World.world.setObjectForSprites(map)

            context.returnBuffer.setTo()
        }
    }

    class RegisterObjectDef : AbstractLibFunction() {
        override fun name(): String = "RegisterObjectDef"

        override fun invoke(context: ExecutionContext, args: ArgumentIterator) {
            val id = args.nextInt()
            val skills = intsFromLuaTable(args.nextOptionalTable(null))
            val walkable = args.nextOptionalBoolean(false)

            World.world.registerObjectTemplate(InteractiveObjectTemplate(id, skills, walkable))

            context.returnBuffer.setTo()
        }
    }

    companion object {
        private var instance: DataScriptVM? = null

        @JvmStatic
        @Synchronized
        @Throws(LoaderException::class, IOException::class, CallException::class, CallPausedException::class, InterruptedException::class)
        fun init() {
            if (instance != null) return

            val start = Instant.now()
            instance = DataScriptVM()
            instance!!.loadData()
            val loadDuration = Duration.between(start, Instant.now())
            logger.info("Scripts loaded in {} ms", loadDuration.toMillis())
        }

        @JvmStatic
        fun getInstance(): DataScriptVM? = instance
    }
}
