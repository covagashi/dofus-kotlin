package org.starloco.locos.script.proxy

import io.jsonwebtoken.lang.Maps
import org.classdump.luna.Table
import org.classdump.luna.impl.DefaultTable
import org.classdump.luna.impl.DefaultUserdata
import org.classdump.luna.impl.ImmutableTable
import org.classdump.luna.lib.ArgumentIterator
import org.starloco.locos.area.map.GameMap
import org.starloco.locos.common.SocketManager
import org.starloco.locos.entity.monster.MobGroupDef
import org.starloco.locos.script.ScriptVM
import org.starloco.locos.script.types.MetaTables
import java.util.Optional

class SMap(userValue: GameMap) : DefaultUserdata<GameMap>(META_TABLE, userValue) {

    private companion object {
        val META_TABLE: ImmutableTable = MetaTables.MetaTable(MetaTables.ReflectIndexTable(SMap::class.java))

        @JvmStatic
        private fun id(m: GameMap): Int = m.id

        @JvmStatic
        private fun def(m: GameMap): Table = m.data.scripted()

        @JvmStatic
        private fun area(m: GameMap): SArea = m.area!!.scripted()

        @JvmStatic
        private fun subArea(m: GameMap): SSubArea = m.subArea!!.scripted()

        @JvmStatic
        private fun cellPlayers(m: GameMap, args: ArgumentIterator): Table {
            val cellID = args.nextInt()
            if (m.getCase(cellID) == null) return DefaultTable.factory().newTable()
            return ScriptVM.scriptedValsTable(m.getCase(cellID)!!.players.stream())
        }

        @JvmStatic
        private fun mobGroupById(m: GameMap, args: ArgumentIterator): Table? {
            val id = args.nextInt()
            return Optional.ofNullable(m.mobGroups[id]).map { it.mobs }
                .map { it.values }
                .map { it.stream() }
                .map { s -> s.map { it.scripted() } }
                .map { ScriptVM.listOf(it) }
                .orElse(null)
        }

        @JvmStatic
        private fun mobGroups(m: GameMap): Table {
            return ScriptVM.listOf(m.mobGroups.values.stream()
                .map { it.mobs }
                .map { it.values }
                .map { it.stream() }
                .map { s -> s.map { it.scripted() } }
                .map { ScriptVM.listOf(it) }
            )
        }

        @JvmStatic
        private fun spawnGroupDef(m: GameMap, args: ArgumentIterator): Int {
            val def = MobGroupDef.Mapper.get().from(args.nextTable())

            return m.spawnMobGroup(def, true)
        }

        @JvmStatic
        private fun updateNpcExtraForPlayer(m: GameMap, args: ArgumentIterator) {
            val entId = args.nextInt()
            val p = args.nextUserdata("SPlayer", SPlayer::class.java).userValue // Check this
            val npc = m.getNpcByTemplateId(entId) ?: return

            SocketManager.GAME_SEND_GX_PACKET(p, npc)
        }

        @JvmStatic
        private fun getAnimationState(m: GameMap, args: ArgumentIterator): String {
            val cellId = args.nextInt()
            return m.getAnimationState(cellId)!!
        }

        @JvmStatic
        private fun setAnimationState(m: GameMap, args: ArgumentIterator) {
            val cellId = args.nextInt()
            val animName = args.nextString().toString()
//        Runnable r = Optional.ofNullable(args.nextOptionalFunction(null))
//            .map(fn -> (Runnable)(() -> DataScriptVM.getInstance().call(fn)))
//            .orElse(null);

            m.setAnimationState(cellId, animName)
        }

        @JvmStatic
        private fun sendAction(m: GameMap, args: ArgumentIterator) {
            val p = args.nextUserdata("SPlayer", SPlayer::class.java).userValue
            val actionID = args.nextInt()
            val actionType = args.nextInt()
            val actionValue = args.nextString().toString()

            var actionIDStr = ""
            if (actionID != -1) actionIDStr = actionID.toString()

            SocketManager.GAME_SEND_GA_PACKET_TO_MAP(m, actionIDStr, actionType, p.id, actionValue)
        }

        @JvmStatic
        private fun setCellData(m: GameMap, args: ArgumentIterator) {
            val cellID = args.nextInt()
            val field = args.nextString().toString()
            val `val` = args.nextInt()

            if (m.cellsData.applyOverrides(cellID, Maps.of(field, `val`).build())) {
                SocketManager.GAME_SEND_GDC_PACKET_TO_MAP(m, cellID, true)
            }
        }
    }
}
