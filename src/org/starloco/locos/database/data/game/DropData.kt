package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.entity.monster.Monster
import org.starloco.locos.game.world.World
import java.sql.ResultSet
import java.sql.SQLException
import java.util.ArrayList
import java.util.Objects

class DropData(dataSource: HikariDataSource?) : FunctionDAO<World.Drop>(dataSource, "drops") {
    override fun loadFully() {
        try {
        World.world.monstres.forEach { monster -> monster.drops.clear() }
        getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result.next()) {
        val MT: Monster? = World.world.getMonstre(result.getInt("monsterId"))
        var action: String = result.getString("action")
        var condition: String = ""
        var percents: ArrayList<Double> = getPercents(result)
                    if (action != "-1" && action != "1" && action.contains(":")) {
        condition = action.split(":")[1]
        action = action.split(":")[0]
                    }
                    if (World.world.getObjTemplate(result.getInt("objectId")) != null && MT != null) {
        MT?.addDrop(World.Drop(result.getInt("objectId"), percents, result.getInt("ceil"), (action).toInt(), result.getInt("level"), condition))
                    } else {
                        if (MT == null && result.getInt("monsterId") == 0) {
        var drop: World.Drop = World.Drop(result.getInt("objectId"), percents, result.getInt("ceil"), (action).toInt(), result.getInt("level"), condition)
        World.world.monstres.stream().filter(Objects::nonNull).forEach { monster -> monster.addDrop(drop) }
                        }
                    }
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
    }
    private fun getPercents(result: ResultSet): ArrayList<Double> {
        var percents: ArrayList<Double> = ArrayList()
        percents.add(result.getDouble("percentGrade1"))
        percents.add(result.getDouble("percentGrade2"))
        percents.add(result.getDouble("percentGrade3"))
        percents.add(result.getDouble("percentGrade4"))
        percents.add(result.getDouble("percentGrade5"))
        return percents
    }
    override fun load(id: Int): World.Drop {
        throw NotImplementedException()
    }
    override fun insert(entity: World.Drop): Boolean {
        throw NotImplementedException()
    }
    override fun delete(entity: World.Drop) {
        throw NotImplementedException()
    }
    override fun update(entity: World.Drop) {
        throw NotImplementedException()
    }
    override fun getReferencedClass(): Class<*> {
        return DropData::class.java
    }
}