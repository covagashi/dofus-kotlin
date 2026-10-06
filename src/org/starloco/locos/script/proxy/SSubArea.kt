package org.starloco.locos.script.proxy

import org.classdump.luna.impl.DefaultUserdata
import org.classdump.luna.impl.ImmutableTable
import org.starloco.locos.area.SubArea
import org.starloco.locos.script.types.MetaTables

class SSubArea(userValue: SubArea) : DefaultUserdata<SubArea>(META_TABLE, userValue) {
    private companion object {
        val META_TABLE: ImmutableTable = MetaTables.MetaTable(MetaTables.ReflectIndexTable(SSubArea::class.java))

        @JvmStatic
        private fun id(sa: SubArea): Int = sa.id

        @JvmStatic
        private fun area(sa: SubArea): SArea = sa.area!!.scripted()

        @JvmStatic
        private fun faction(sa: SubArea): Int = sa.alignment

        @JvmStatic
        private fun conquerable(sa: SubArea): Boolean = sa.conquerable
    }
}
