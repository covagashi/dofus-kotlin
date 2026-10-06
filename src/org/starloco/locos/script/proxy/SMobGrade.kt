package org.starloco.locos.script.proxy

import org.classdump.luna.impl.DefaultUserdata
import org.classdump.luna.impl.ImmutableTable
import org.starloco.locos.entity.monster.MonsterGrade
import org.starloco.locos.script.types.MetaTables

class SMobGrade(userValue: MonsterGrade) : DefaultUserdata<MonsterGrade>(META_TABLE, userValue) {
    private companion object {
        val META_TABLE: ImmutableTable = MetaTables.MetaTable(MetaTables.ReflectIndexTable(SMobGrade::class.java))

        @JvmStatic
        private fun id(p: MonsterGrade): Int = p.template!!.id

        @JvmStatic
        private fun grade(p: MonsterGrade): Int = p.grade

        @JvmStatic
        private fun level(p: MonsterGrade): Int = p.level
    }
}
