package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import java.sql.SQLException

class ChallengeData(dataSource: HikariDataSource?) : FunctionDAO<Any>(dataSource, "challenge") {
    override fun loadFully() {
        try {
        getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result.next()) {
        World.world.addChallenge(result.getInt("id").toString() + "," + result.getInt("gainXP").toString() + "," + result.getInt("gainDrop").toString() + "," + result.getInt("gainParMob").toString() + "," + result.getInt("conditions"))
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
        return ChallengeData::class.java
    }
}