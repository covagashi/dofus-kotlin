package org.starloco.locos.database.data.login

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.entity.pet.PetEntry
import org.starloco.locos.game.world.World
import java.sql.PreparedStatement
import java.sql.SQLException

class PetData(dataSource: HikariDataSource?) : FunctionDAO<PetEntry>(dataSource, "world_pets") {
    override fun loadFully() {
        try {
        getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result.next()) {
        World.world.addPetsEntry(PetEntry(result.getInt("id"), result.getInt("template"), result.getLong("lastEatDate"), result.getInt("quantityEat"), result.getInt("pdv"), result.getInt("corpulence"), (result.getInt("isEPO") == 1)))
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
    }
    override fun load(id: Int): PetEntry {
        throw NotImplementedException()
    }
    //TODO: Change the insert to set the id of the entity
    override fun insert(entity: PetEntry): Boolean {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("INSERT INTO " + getTableName() + "(`id`, `template`, `lastEatDate`, `quantityEat`, `pdv`, `corpulence`, `isEPO`) VALUES (?, ?, ?, ?, ?, ?, ?);")
        p?.setInt(1, entity.objectId)
        p?.setInt(2, entity.template)
        p?.setLong(3, entity.lastEatDate)
        p?.setInt(4, 0)
        p?.setInt(5, 10)
        p?.setInt(6, 0)
        p?.setInt(7, 0)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
        return true
    }
    override fun delete(entity: PetEntry) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("DELETE FROM " + getTableName() + " WHERE `id` = ?")
        p?.setInt(1, entity.objectId)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    override fun update(entity: PetEntry) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("UPDATE " + getTableName() + " SET `lastEatDate` = ?, `quantityEat` = ?, `pdv` = ?, `corpulence` = ?, `isEPO` = ? WHERE `id` = ?;")
        p?.setLong(1, entity.lastEatDate)
        p?.setInt(2, entity.quaEat)
        p?.setInt(3, entity.pdv)
        p?.setInt(4, entity.corpulence)
        p?.setInt(5, (if (entity.getIsEupeoh()) 1 else 0))
        p?.setInt(6, entity.objectId)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    override fun getReferencedClass(): Class<*> {
        return PetData::class.java
    }
}