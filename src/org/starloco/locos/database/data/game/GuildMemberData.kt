package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.client.Player
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import java.sql.PreparedStatement
import java.sql.SQLException

class GuildMemberData(dataSource: HikariDataSource?) : FunctionDAO<Player>(dataSource, "guild_members") {

    override fun loadFully() {
        try {
            getData("SELECT * FROM guild_members") { result ->
                while (result.next()) {
                    try {
                        val g = World.world.getGuild(result.getInt("guild"))
                        g?.addMember(result.getInt("guid"), result.getInt("rank"), result.getByte("pxp"), result.getLong("xpdone"), result.getInt("rights"), result.getString("lastConnection").replace("-", "~"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: SQLException) {
            super.sendError(e)
        }
    }

    override fun load(id: Int): Player? {
        throw NotImplementedException()
    }

    override fun insert(entity: Player): Boolean {
        throw NotImplementedException()
    }

    override fun delete(entity: Player) {
        var p: PreparedStatement? = null
        try {
            p = getPreparedStatement("DELETE FROM " + getTableName() + " WHERE `guid` = ?")
            p?.setInt(1, entity.id)
            execute(p)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(p)
        }
    }

    override fun update(player: Player) {
        var p: PreparedStatement? = null
        try {
            p = getPreparedStatement("REPLACE INTO " + getTableName() + " VALUES(?,?,?,?,?,?,?,?,?,?,?)")
            val gm = player.guildMember ?: return
            p?.setInt(1, gm.playerId)
            p?.setInt(2, gm.guild.id)
            p?.setString(3, player.name)
            p?.setInt(4, gm.lvl)
            var gfx: Int = gm.gfx
            if (gfx > 121 || gfx < 10)
                gfx = player.classe * 10 + player.sexe
            p?.setInt(5, gfx)
            p?.setInt(6, gm.rank)
            p?.setLong(7, gm.xpGave)
            p?.setInt(8, gm.getXpGive())
            p?.setInt(9, gm.rights)
            p?.setInt(10, gm.align)
            p?.setString(11, gm.lastCo)
            execute(p)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(p)
        }
    }

    override fun getReferencedClass(): Class<*> {
        return GuildMemberData::class.java
    }

    fun deleteAll(id: Int) {
        var p: PreparedStatement? = null
        try {
            p = getPreparedStatement("DELETE FROM " + getTableName() + " WHERE `guild` = ?;")
            p?.setInt(1, id)
            execute(p)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(p)
        }
    }

    fun isPersoInGuild(id: Int): Int {
        try {
            return getData<Int>("SELECT guild FROM " + getTableName() + " WHERE guid = " + id + ";") { result ->
                if (result.first()) result.getInt("guild") else -1
            } ?: -1
        } catch (e: SQLException) {
            super.sendError(e)
        }
        return -1
    }

    fun isPersoInGuild(name: String): IntArray {
        try {
            return getData<IntArray>("SELECT guild,guid FROM " + getTableName() + " WHERE name = '" + name + "';") { result ->
                if (result.first()) intArrayOf(result.getInt("guid"), result.getInt("guild")) else intArrayOf(-1, -1)
            } ?: intArrayOf(-1, -1)
        } catch (e: SQLException) {
            super.sendError(e)
        }
        return intArrayOf(-1, -1)
    }
}
