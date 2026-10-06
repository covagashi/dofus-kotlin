package org.starloco.locos.script.proxy

import org.classdump.luna.impl.DefaultUserdata
import org.classdump.luna.impl.ImmutableTable
import org.starloco.locos.area.Area
import org.starloco.locos.script.types.MetaTables

class SArea(userValue: Area) : DefaultUserdata<Area>(META_TABLE, userValue) {
    private companion object {
        val META_TABLE: ImmutableTable = MetaTables.MetaTable(MetaTables.ReflectIndexTable(SArea::class.java))

        @JvmStatic
        private fun id(a: Area): Int = a.id
    }
}
