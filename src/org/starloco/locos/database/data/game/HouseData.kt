package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.entity.map.House
import org.starloco.locos.client.Player
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import java.sql.PreparedStatement
import java.sql.SQLException

class HouseData(dataSource: HikariDataSource?) : FunctionDAO<House>(dataSource, "houses") {
    override fun loadFully() {
        try {
        getData("SELECT * FROM houses") { result ->
        while (result.next()) {
        var id: Int = result.getInt("id")
        var owner: Int = result.getInt("owner_id")
        var sale: Int = result.getInt("sale")
        var guild: Int = result.getInt("guild_id")
        var access: Int = result.getInt("access")
        var key: String = result.getString("key") ?: ""
        var guildRights: Int = result.getInt("guild_rights")
        var house: House? = World.world.getHouse(id)
                    if (house == null)
        continue
        house.ownerId = owner
        house.sale = sale
        house.guildId = guild
        house.access = access
        house.key = key
        house.setGuildRightsWithParse(guildRights)
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
    }
    override fun load(id: Int): House? {
        throw NotImplementedException()
    }
    override fun insert(entity: House): Boolean {
        throw NotImplementedException()
    }
    override fun delete(entity: House) {
        throw NotImplementedException()
    }
    override fun update(entity: House) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("UPDATE " + getTableName() + " SET `owner_id` = ?,`sale` = ?,`guild_id` = ?,`access` = ?,`key` = ?,`guild_rights` = ? WHERE id = ?")
        p?.setInt(1, entity.ownerId)
        p?.setInt(2, entity.sale)
        p?.setInt(3, entity.guildId)
        p?.setInt(4, entity.access)
        p?.setString(5, entity.key)
        p?.setInt(6, entity.guildRights)
        p?.setInt(7, entity.id)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    override fun getReferencedClass(): Class<*> {
        return HouseData::class.java
    }
    fun update(id: Int, price: Long): Boolean {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("UPDATE " + getTableName() + " SET `sale` = ? WHERE id = ?")
        p?.setLong(1, price)
        p?.setInt(2, id)
        execute(p)
        return true
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
        return false
    }
    fun buy(P: Player, h: House) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("UPDATE " + getTableName() + " SET `sale`='0', `owner_id`=?, `guild_id`='0', `access`='0', `key`='-', `guild_rights`='0' WHERE `id`=?")
        p?.setInt(1, P.accID)
        p?.setInt(2, h.id)
        execute(p)
        h.sale = 0
        h.ownerId = P.accID
        h.guildId = 0
        h.access = 0
        h.key = "-"
        h.guildRights = 0
        DatabaseManager.get(TrunkData::class.java).update(P, h)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    fun sell(h: House, price: Int) {
        h.sale = price
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("UPDATE " + getTableName() + " SET `sale`=? WHERE `id` = ?;")
        p?.setInt(1, price)
        p?.setInt(2, h.id)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    fun updateCode(P: Player, h: House, packet: String) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("UPDATE " + getTableName() + " SET `key`=? WHERE `id`=? AND owner_id = ?;")
        p?.setString(1, packet)
        p?.setInt(2, h.id)
        p?.setInt(3, P.accID)
        execute(p)
        h.key = packet
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    fun updateGuild(h: House, GuildID: Int, GuildRights: Int) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("UPDATE " + getTableName() + " SET `guild_id`=?, `guild_rights`=? WHERE `id` = ?;")
        p?.setInt(1, GuildID)
        p?.setInt(2, GuildRights)
        p?.setInt(3, h.id)
        execute(p)
        h.guildId = GuildID
        h.guildRights = GuildRights
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    fun removeGuild(GuildID: Int) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("UPDATE " + getTableName() + " SET `guild_rights`='0', `guild_id`='0' WHERE `guild_id`=?;")
        p?.setInt(1, GuildID)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
}