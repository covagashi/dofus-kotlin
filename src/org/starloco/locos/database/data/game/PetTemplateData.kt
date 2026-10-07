package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.entity.pet.Pet
import org.starloco.locos.game.world.World
import java.sql.SQLException

class PetTemplateData(dataSource: HikariDataSource?) : FunctionDAO<Pet>(dataSource, "pets") {
    override fun loadFully() {
        try {
        getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result.next()) {
                    World.world.addPets(Pet(result.getInt("TemplateID"), result.getInt("Type"), result.getString("Gap"),
        result.getString("StatsUp") ?: "", result.getInt("Max"), result.getInt("Gain"), result.getInt("DeadTemplate"), result.getInt("Epo"), result.getString("jet")))
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
    }
    override fun load(id: Int): Pet {
        throw NotImplementedException()
    }
    override fun insert(entity: Pet): Boolean {
        throw NotImplementedException()
    }
    override fun delete(entity: Pet) {
        throw NotImplementedException()
    }
    override fun update(entity: Pet) {
        throw NotImplementedException()
    }
    override fun getReferencedClass(): Class<*> {
        return PetTemplateData::class.java
    }
}