package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.entity.map.House
import org.starloco.locos.entity.map.Trunk
import org.starloco.locos.client.Player
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import java.sql.PreparedStatement
import java.sql.SQLException

class TrunkData(dataSource: HikariDataSource?) : FunctionDAO<Trunk>(dataSource, "coffres") {
    override fun loadFully() {
        try {
        getData("SELECT * FROM " + getTableName()) { result ->
                while (result.next()) {
        var id: Int = result.getInt("id")
        var objects: String = result.getString("object")
        objects = if (objects == null || objects == " ") "" else objects
        var trunk: Trunk? = World.world.getTrunk(id)
                    if (trunk != null) {
        trunk.setObjects(objects)
        trunk.kamas = result.getInt("kamas").toLong()
        trunk.ownerId = result.getInt("owner_id")
        trunk.key = result.getString("key")
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
    override fun insert(trunk: Trunk): Boolean {
        var p: PreparedStatement? = null
        var ok: Boolean = true
        try {
        p = getPreparedStatement("INSERT INTO " + getTableName() + " (`id`, `object`, `kamas`, `key`, `owner_id`) " + "VALUES (?, ?, ?, ?, ?)")
        p?.setInt(1, trunk.id)
        p?.setString(2, "")
        p?.setInt(3, 0)
        p?.setString(4, "-")
        p?.setInt(5, trunk.ownerId)
        execute(p)
        } catch (e: SQLException) {
        ok = false
        super.sendError(e)
        } finally {
        close(p)
        }
        return ok
    }
    override fun delete(entity: Trunk) {
        throw NotImplementedException()
    }
    override fun update(t: Trunk) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("UPDATE " + getTableName() + " SET `kamas`=?, `object`=? WHERE `id`=?")
        p?.setLong(1, t.kamas)
        p?.setString(2, t.parseTrunkObjetsToDB())
        p?.setInt(3, t.id)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    override fun getReferencedClass(): Class<*> {
        return TrunkData::class.java
    }
    fun exist(trunk: Trunk) {
        try {
        getData("SELECT * FROM " + getTableName() + " WHERE `id` = '" + trunk.id + "';") { result ->
                if (!result.next()) {
        this.insert(trunk)
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
    }
    fun update(player: Player, house: House) {
        Trunk.getTrunksByHouse(house)
            .filter { t -> t.ownerId != player.accID }
            .peek { t -> t.ownerId = player.accID }
            .peek { t -> t.key = "-" }
            .forEach { trunk ->
                try {
                    val p = getPreparedStatement("UPDATE " + getTableName() + " SET `owner_id`=?, `key`='-' WHERE `id`=?")
                    p?.setInt(1, player.accID)
                    p?.setInt(2, trunk.id)
                    execute(p)
                    p?.close()
                } catch (e: SQLException) {
                    super.sendError(e)
                }
            }
    }
    fun updateCode(P: Player, t: Trunk, packet: String) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("UPDATE " + getTableName() + " SET `key`=? WHERE `id`=? AND owner_id=?")
        p?.setString(1, packet)
        p?.setInt(2, t.id)
        p?.setInt(3, P.accID)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
}