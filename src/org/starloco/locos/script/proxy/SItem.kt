package org.starloco.locos.script.proxy

import org.classdump.luna.impl.DefaultUserdata
import org.classdump.luna.impl.ImmutableTable
import org.classdump.luna.lib.ArgumentIterator
import org.classdump.luna.Table
import org.starloco.locos.common.SocketManager
import org.starloco.locos.`object`.GameObject
import org.starloco.locos.script.types.MetaTables
import java.util.stream.Collectors

class SItem(userValue: GameObject) : DefaultUserdata<GameObject>(META_TABLE, userValue) {
    private companion object {
        val META_TABLE: ImmutableTable = MetaTables.MetaTable(MetaTables.ReflectIndexTable(SItem::class.java))

        @JvmStatic
        private fun guid(item: GameObject): Int = item.guid

        @JvmStatic
        private fun id(item: GameObject): Int = item.template!!.id

        @JvmStatic
        private fun type(item: GameObject): Int = item.template!!.type

        @JvmStatic
        private fun dateStatTS(item: GameObject, args: ArgumentIterator): Long {
            val statID = args.nextInt()

            var `val` = item.txtStat[statID] ?: return -1
            if (`val`.contains("#")) {
                `val` = `val`.split("#")[3]
            }
            return (`val`).toLong()
        }

        @JvmStatic
        private fun hasTxtStat(item: GameObject, args: ArgumentIterator): Boolean {
            val stat = args.nextInt()
            val `val` = args.nextString().toString()

            val stats = item.txtStat[stat]
            if (stats == null || stats.isEmpty()) {
                return false
            }
            return stats.split(",").contains(`val`)
        }

        @JvmStatic
        private fun consumeTxtStat(item: GameObject, args: ArgumentIterator): Boolean {
            val player = args.nextUserdata("SPlayer", SPlayer::class.java).userValue
            val stat = args.nextInt()
            val `val` = args.nextString().toString()

            val stats = item.txtStat[stat]
            if (stats == null || stats.isEmpty()) return false


            // TODO: change how Text stats are stored. They are a bit hacky currently,
            val newStats = stats.split(",")
                .filter { it != `val` } // Filter out val from stat
                .joinToString(",")

            if (newStats == stats) {
                // New stats are the same as before, we failed to remove the key
                return false
            }
            item.txtStat[stat] = newStats

            SocketManager.GAME_SEND_UPDATE_ITEM(player, item)
            return true
        }
    }
}
