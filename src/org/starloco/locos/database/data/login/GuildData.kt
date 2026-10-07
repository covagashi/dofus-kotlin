package org.starloco.locos.database.data.login

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import org.starloco.locos.guild.Guild
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement

class GuildData(dataSource: HikariDataSource?) : FunctionDAO<Guild>(dataSource, "world_guilds") {

    override fun loadFully() {
        throw NotImplementedException()
    }

    override fun load(id: Int): Guild? {
        try {
            return getData<Guild?>("SELECT * FROM " + getTableName() + " WHERE `id` = " + id + ";") { result ->
                if (result.next()) {
                    val guild = Guild(result.getInt("id"), result.getString("name") ?: "", result.getString("emblem"), result.getInt("lvl"), result.getLong("xp"), result.getInt("capital"), result.getInt("maxCollectors"), result.getString("spells"), result.getString("stats"), result.getLong("date"))
                    World.world.addGuild(guild)
                    guild
                } else {
                    null
                }
            }
        } catch (e: SQLException) {
            super.sendError(e)
        }
        return null
    }

    override fun insert(entity: Guild): Boolean {
        var statement: PreparedStatement? = null
        var ok = true
        try {
            statement = this.getConnection()?.prepareStatement("INSERT INTO " + getTableName() + " (`name`, `emblem`, `lvl`, `xp`, `capital`, `maxCollectors`, `spells`, `stats`, `date`) VALUES (?,?,1,0,0,0,?,?,?);", Statement.RETURN_GENERATED_KEYS)
            statement?.setString(1, entity.name)
            statement?.setString(2, entity.emblem)
            statement?.setString(3, "462;0|461;0|460;0|459;0|458;0|457;0|456;0|455;0|454;0|453;0|452;0|451;0|")
            statement?.setString(4, "176;100|158;1000|124;100|")
            statement?.setLong(5, entity.date)
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

    override fun delete(entity: Guild) {
        var p: PreparedStatement? = null
        try {
            p = getPreparedStatement("DELETE FROM " + getTableName() + " WHERE `id` = ?;")
            p?.setInt(1, entity.id)
            execute(p)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(p)
        }
    }

    override fun update(entity: Guild) {
        var p: PreparedStatement? = null
        try {
            p = getPreparedStatement("UPDATE " + getTableName() + " SET `lvl` = ?, `xp` = ?, `capital` = ?, `maxCollectors` = ?, `spells` = ?, `stats` = ? WHERE id = ?;")
            p?.setInt(1, entity.lvl)
            p?.setLong(2, entity.xp)
            p?.setInt(3, entity.capital)
            p?.setInt(4, entity.nbCollectors)
            p?.setString(5, entity.compileSpell())
            p?.setString(6, entity.compileStats())
            p?.setInt(7, entity.id)
            execute(p)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(p)
        }
    }

    override fun getReferencedClass(): Class<*> {
        return GuildData::class.java
    }
}
