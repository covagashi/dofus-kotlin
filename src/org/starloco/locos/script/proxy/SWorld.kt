package org.starloco.locos.script.proxy

import org.classdump.luna.ByteString
import org.classdump.luna.Table
import org.classdump.luna.impl.DefaultTable
import org.classdump.luna.impl.DefaultUserdata
import org.classdump.luna.impl.ImmutableTable
import org.classdump.luna.lib.ArgumentIterator
import org.classdump.luna.runtime.LuaFunction
import org.starloco.locos.area.SubArea
import org.starloco.locos.area.map.GameMap
import org.starloco.locos.client.Account
import org.starloco.locos.client.Player
import org.starloco.locos.game.world.World
import org.starloco.locos.script.DataScriptVM
import org.starloco.locos.script.types.MetaTables
import java.time.ZonedDateTime
import java.util.Optional
import java.util.concurrent.TimeUnit

class SWorld(userValue: World) : DefaultUserdata<World>(META_TABLE, userValue) {

    private companion object {
        val META_TABLE: ImmutableTable = MetaTables.MetaTable(MetaTables.ReflectIndexTable(SWorld::class.java))

        @JvmStatic
        private fun datetime(world: World): Table {
            val zdt = ZonedDateTime.now()
            val out = DefaultTable()

            out.rawset("day", zdt.dayOfMonth)
            out.rawset("month", zdt.monthValue)
            out.rawset("year", zdt.year)
            out.rawset("hour", zdt.hour)
            out.rawset("min", zdt.minute)
            out.rawset("sec", zdt.second)

            return out
        }

        @JvmStatic
        private fun clock(world: World): Long = System.currentTimeMillis()

        @JvmStatic
        private fun account(world: World, args: ArgumentIterator): SAccount? {
            val arg = args.next()

            val a: Account? = when (arg) {
                is Long -> world.getAccount(arg.toInt())
                is ByteString -> world.getAccountByPseudo(arg.toString())
                else -> throw IllegalArgumentException("World:account param must be a number or a string")
            }

            return Optional.ofNullable(a).map { it.scripted() }.orElse(null)
        }

        @JvmStatic
        private fun player(world: World, args: ArgumentIterator): SPlayer? {
            val arg = args.next()

            val p: Player? = when (arg) {
                is Long -> world.getPlayer(arg.toInt())
                is ByteString -> world.getPlayerByName(arg.toString())
                else -> throw IllegalArgumentException("World:player param must be a number or a string")
            }

            return Optional.ofNullable(p).map { it.scripted() }.orElse(null)
        }

        @JvmStatic
        private fun subArea(world: World, args: ArgumentIterator): SSubArea? {
            return Optional.ofNullable(world.getSubArea(args.nextInt())).map { it.scripted() }.orElse(null)
        }

        @JvmStatic
        private fun map(world: World, args: ArgumentIterator): SMap? {
            return Optional.ofNullable(world.getMap(args.nextInt())).map { it.scripted() }.orElse(null)
        }

        @JvmStatic
        private fun delayForMs(world: World, args: ArgumentIterator) {
            val delay = args.nextInteger()
            val fn: LuaFunction<*, *, *, *, *> = args.nextFunction()

            world.scheduler.schedule({
                DataScriptVM.getInstance()!!.call(fn)
            }, delay, TimeUnit.MILLISECONDS)
        }
    }
}
