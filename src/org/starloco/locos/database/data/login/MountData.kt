package org.starloco.locos.database.data.login

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.client.Player
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.entity.mount.Mount
import org.starloco.locos.game.world.World
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement

class MountData(dataSource: HikariDataSource?) : FunctionDAO<Mount>(dataSource, "world_mounts") {

    override fun loadFully() {
        throw NotImplementedException()
    }

    override fun load(id: Int): Mount? {
        try {
            return getData<Mount?>("SELECT * FROM " + getTableName() + " WHERE `id` = " + id + ";") { result ->
                if (result.next()) {
                    val mount = Mount(result.getInt("id"), result.getInt("color"), result.getInt("sex"), result.getInt("amour"), result.getInt("endurance"), result.getInt("level"), result.getLong("xp"),
                        result.getString("name"), result.getInt("fatigue"), result.getInt("energy"), result.getInt("reproductions"), result.getInt("maturity"), result.getInt("serenity"), result.getString("objects"),
                        result.getString("ancestors"), result.getString("capacitys"), result.getInt("size"), result.getInt("cell"), result.getShort("map").toInt(), result.getInt("owner"), result.getInt("orientation"),
                        result.getLong("fecundatedDate"), result.getInt("couple"), result.getInt("savage"))
                    World.world.addMount(mount)
                    mount
                } else {
                    null
                }
            }
        } catch (e: SQLException) {
            super.sendError(e)
        }
        return null
    }

    //TODO: Change the insert to set the id of the entity
    override fun insert(entity: Mount): Boolean {
        var statement: PreparedStatement? = null
        var ok = true
        try {
            statement = this.dataSource?.connection?.prepareStatement("INSERT INTO " + getTableName() + "(`color`, `sex`, `name`, `xp`, `level`, `endurance`, `amour`, `maturity`, `serenity`, `reproductions`, `fatigue`, `energy`," +
                "`objects`, `ancestors`, `capacitys`, `size`, `map`, `cell`, `owner`, `orientation`, `fecundatedDate`, `couple`, `savage`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
            statement?.setInt(1, entity.color)
            statement?.setInt(2, entity.sex)
            statement?.setString(3, entity.name)
            statement?.setLong(4, entity.exp)
            statement?.setInt(5, entity.level)
            statement?.setInt(6, entity.endurance)
            statement?.setInt(7, entity.amour)
            statement?.setInt(8, entity.maturity)
            statement?.setInt(9, entity.state)
            statement?.setInt(10, entity.reproduction)
            statement?.setInt(11, entity.fatigue)
            statement?.setInt(12, entity.energy)
            statement?.setString(13, entity.parseObjectsToString())
            statement?.setString(14, entity.ancestors)
            statement?.setString(15, entity.parseCapacitysToString())
            statement?.setInt(16, entity.size)
            statement?.setInt(17, entity.mapId)
            statement?.setInt(18, entity.cellId)
            statement?.setInt(19, entity.owner)
            statement?.setInt(20, entity.orientation)
            statement?.setLong(21, entity.fecundatedDate)
            statement?.setInt(22, entity.couple)
            statement?.setInt(23, entity.savage)
            val affectedRows = statement?.executeUpdate() ?: 0

            if (affectedRows == 0) {
                ok = false
            } else {
                statement?.generatedKeys?.use { generatedKeys ->
                    if (generatedKeys.next()) {
                        entity.id = generatedKeys.getInt(1)
                    } else {
                        ok = false
                    }
                }
            }
        } catch (e: SQLException) {
            super.sendError(e)
            ok = false
        } finally {
            close(statement)
        }
        return ok
    }

    override fun delete(entity: Mount) {
        var p: PreparedStatement? = null
        try {
            p = getPreparedStatement("DELETE FROM " + getTableName() + " WHERE `id` = ?;")
            if (p != null) {
                p.setInt(1, entity.id)
                execute(p)
            }
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(p)
        }
    }

    fun delete(player: Player) {
        this.delete(player.mount!!)
        World.world.delDragoByID(player.mount!!.id)
        player.mountXpGive = 0
        player.mount = null
        DatabaseManager.get(PlayerData::class.java).update(player)
    }

    override fun update(entity: Mount) {
        var p: PreparedStatement? = null
        try {
            p = getPreparedStatement("UPDATE " + getTableName() + " SET `name` = ?, `xp` = ?, `level` = ?, `endurance` = ?, `amour` = ?, `maturity` = ?, `serenity` = ?, `reproductions` = ?," +
                "`fatigue` = ?, `energy` = ?, `ancestors` = ?, `objects` = ?, `owner` = ?, `capacitys` = ?, `size` = ?, `cell` = ?, `map` = ?," +
                " `orientation` = ?, `fecundatedDate` = ?, `couple` = ? WHERE `id` = ?;")
            if (p != null) {
                p.setString(1, entity.name)
                p.setLong(2, entity.exp)
                p.setInt(3, entity.level)
                p.setInt(4, entity.endurance)
                p.setInt(5, entity.amour)
                p.setInt(6, entity.maturity)
                p.setInt(7, entity.state)
                p.setInt(8, entity.reproduction)
                p.setInt(9, entity.fatigue)
                p.setInt(10, entity.energy)
                p.setString(11, entity.ancestors)
                p.setString(12, entity.parseObjectsToString())
                p.setInt(13, entity.owner)
                p.setString(14, entity.parseCapacitysToString())
                p.setInt(15, entity.size)
                p.setInt(16, entity.cellId)
                p.setInt(17, entity.mapId)
                p.setInt(18, entity.orientation)
                p.setLong(19, entity.fecundatedDate)
                p.setInt(20, entity.couple)
                p.setInt(21, entity.id)
                execute(p)
            }
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(p)
        }
    }

    override fun getReferencedClass(): Class<*> {
        return MountData::class.java
    }
}
