package org.starloco.locos.entity.monster

import org.classdump.luna.Table
import org.starloco.locos.common.Formulas
import org.starloco.locos.game.world.World
import org.starloco.locos.script.ScriptMapper
import org.starloco.locos.script.ScriptVM
import org.starloco.locos.util.Pair
import java.util.stream.Collectors

class MobGroupDef(@JvmField val cellId: Int, @JvmField val gradesForMobs: List<Pair<Int, MutableList<Int>>>) {

    fun randomize(): List<MonsterGrade?> {
        return gradesForMobs.stream().map { p ->
            val grade = p.second[Formulas.getRandomValue(0, p.second.size - 1)]
            World.world.getMonstre(p.first)!!.grades[grade]
        }.collect(Collectors.toList())
    }

    class Mapper : ScriptMapper<MobGroupDef> {
        override fun from(o: Any?): MobGroupDef {
            require(o is Table) { "MobGroupDef must be a Table" }
            val cellId = ScriptVM.rawInt(o, 1L)
            val def = o.rawget(2L) as Table

            return MobGroupDef(
                cellId, ScriptVM.listFromLuaTable<Table>(def)
                    .stream()
                    .map { ScriptVM.toPair<Long, Table>(it) }
                    .map { Pair(it.first.toInt(), ScriptVM.intsFromLuaTable(it.second)) }
                    .collect(Collectors.toList())
            )
        }

        override fun to(v: MobGroupDef): Any {
            throw UnsupportedOperationException("MobGroupDef cannot be converted to script")
        }

        companion object {
            private val INSTANCE = Mapper()

            @JvmStatic
            fun get(): Mapper = INSTANCE
        }
    }
}
