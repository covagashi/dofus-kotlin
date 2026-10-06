package org.starloco.locos.script.proxy

import org.classdump.luna.Table
import org.classdump.luna.impl.DefaultTable
import org.classdump.luna.impl.DefaultUserdata
import org.classdump.luna.impl.ImmutableTable
import org.starloco.locos.client.Account
import org.starloco.locos.script.types.MetaTables

class SAccount(userValue: Account) : DefaultUserdata<Account>(META_TABLE, userValue) {
    private companion object {
        val META_TABLE: ImmutableTable = MetaTables.MetaTable(MetaTables.ReflectIndexTable(SAccount::class.java))

        @JvmStatic
        private fun id(a: Account): Int = a.id

        @JvmStatic
        private fun friends(a: Account): Table {
            val table = DefaultTable()
            a.getFriendIds().forEach { fId ->
                table.rawset(table.rawlen() + 1, fId)
            }
            return table
        }
    }
}
