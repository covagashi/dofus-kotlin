package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import java.sql.SQLException

class ExtraMonsterData(dataSource: HikariDataSource?) : FunctionDAO<Any>(dataSource, "extra_monster") {
    override fun loadFully() {
        try {
        getData("SELECT * from extra_monster") { result ->
        while (result.next()) {
        World.world.addExtraMonster(result.getInt("idMob"), result.getString("superArea"), result.getString("subArea"), result.getInt("chances"))
                }
                }
        } catch (e: SQLException) {
        super.sendError(e)
        }
    }
    override fun load(id: Int): Any {
        throw NotImplementedException()
    }
    override fun insert(entity: Any): Boolean {
        throw NotImplementedException()
    }
    override fun delete(entity: Any) {
        throw NotImplementedException()
    }
    override fun update(entity: Any) {
        throw NotImplementedException()
    }
    override fun getReferencedClass(): Class<*> {
        return ExtraMonsterData::class.java
    }
}