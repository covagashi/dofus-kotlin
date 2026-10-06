package org.starloco.locos.database.data.login

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.entity.map.Trunk
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.database.data.game.TrunkData
import org.starloco.locos.game.world.World
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement

class BaseTrunkData(dataSource: HikariDataSource?) : FunctionDAO<Trunk>(dataSource, "world_base_trunks") {

    override fun loadFully() {
        try {
            getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result.next()) {
                    val id = result.getInt("id")
                    if (World.world.getTrunk(id) == null) {
                        val trunk = Trunk(id, result.getInt("id_house"), result.getShort("mapid").toInt(), result.getInt("cellid"))
                        World.world.addTrunk(trunk)
                        DatabaseManager.get(TrunkData::class.java).exist(trunk)
                    }
                }
            }
        } catch (e: SQLException) {
            super.sendError(e)
        }
    }

    override fun load(id: Int): Trunk? {
        throw NotImplementedException()
    }

    //TODO: Change insert to set the id of the entity
    override fun insert(entity: Trunk): Boolean {
        var entity = entity
        var statement: PreparedStatement? = null
        var ok = true
        try {
            for (trunk in World.world.trunks.values) {
                if (trunk.houseId == entity.houseId && trunk.mapId == entity.mapId && trunk.cellId == entity.cellId) {
                    entity = trunk
                    ok = false
                }
            }


            if (ok) {
                val trunk = entity
                var found = false
                try {
                    found = getData<Boolean>("SELECT * FROM " + getTableName() + ";") { result ->
                        while (result.next()) {
                            if (result.getInt("id_house") == trunk.houseId && result.getShort("mapid").toInt() == trunk.mapId && result.getShort("cellid").toInt() == trunk.cellId) {
                                trunk.id = result.getInt("id")
                                DatabaseManager.get(TrunkData::class.java).exist(trunk)
                                return@getData true
                            }
                        }
                        false
                    } ?: false
                } catch (e: SQLException) {
                    super.sendError(e)
                }

                if (!found) {
                    statement = this.getConnection()?.prepareStatement("INSERT INTO " + getTableName() + " (`id_house`, `mapid`, `cellid`) VALUES (?, ?, ?);", Statement.RETURN_GENERATED_KEYS)
                    statement?.setInt(1, entity.houseId)
                    statement?.setInt(2, entity.mapId)
                    statement?.setInt(3, entity.cellId)
                    val affectedRows = statement?.executeUpdate() ?: 0

                    if (affectedRows == 0) {
                        ok = false
                    } else {
                        statement?.generatedKeys?.use { generatedKeys ->
                            if (generatedKeys.next()) {
                                entity.id = generatedKeys.getInt(1)
                                DatabaseManager.get(TrunkData::class.java).insert(entity)
                            } else {
                                ok = false
                            }
                        }
                    }
                }
            } else {
                ok = true
                DatabaseManager.get(TrunkData::class.java).insert(entity)
            }
        } catch (e: SQLException) {
            super.sendError(e)
            ok = false
        } finally {
            close(statement)
        }
        return ok
    }

    override fun delete(entity: Trunk) {
        throw NotImplementedException()
    }

    override fun update(entity: Trunk) {
        throw NotImplementedException()
    }

    override fun getReferencedClass(): Class<*> {
        return BaseTrunkData::class.java
    }
}
