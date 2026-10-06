package org.starloco.locos.database.data.login

import java.sql.Statement
import com.zaxxer.hikari.HikariDataSource
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import org.starloco.locos.`object`.GameObject
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

class ObjectData(dataSource: HikariDataSource?) : FunctionDAO<GameObject>(dataSource, "world_objects") {
    override fun loadFully() {
        try {
        getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result != null && result.next()) {
        var id: Int = result.getInt("id")
        var template: Int = result.getInt("template")
        var quantity: Int = result.getInt("quantity")
        var position: Int = result.getInt("position")
        var stats: String = result.getString("stats")
        var puit: Int = result.getInt("puit")
        if (quantity == 0) continue
        var gameObject: GameObject = World.world.newObjet(id, template, quantity, position, stats, puit)!!
                    if (gameObject.template == null)
        this.delete(gameObject)
                    else
        World.world.addGameObject(gameObject)
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
    }
    override fun load(id: Int): GameObject? {
        try {
        return getData<GameObject?>("SELECT * FROM " + getTableName() + " WHERE `id` IN (" + id + ");") { result ->
            if (!result.next()) {
                null
            } else {
                val template: Int = result.getInt("template")
                val quantity: Int = result.getInt("quantity")
                val position: Int = result.getInt("position")
                val stats: String = result.getString("stats")
                val puit: Int = result.getInt("puit")
                if (quantity > 0) {
                    val gameObject: GameObject = World.world.newObjet(result.getInt("id"), template, quantity, position, stats, puit)!!
                    World.world.addGameObject(gameObject)
                    gameObject
                } else null
            }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
        return null
    }
    override fun insert(entity: GameObject): Boolean {
        var statement: PreparedStatement? = null
        var ok: Boolean = true
        try {
        statement = this.getConnection()?.prepareStatement("INSERT INTO " + getTableName() + " (`template`, `quantity`, `position`, `stats`, `puit`) VALUES (?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS)
        statement?.setInt(1, entity.template!!.id)
        statement?.setInt(2, entity.quantity)
        statement?.setInt(3, entity.position)
        statement?.setString(4, entity.parseToSave())
        statement?.setInt(5, entity.puit)
        var affectedRows: Int = statement?.executeUpdate() ?: 0
            if (affectedRows == 0) {
        ok = false
            } else {
        statement?.getGeneratedKeys()?.use { generatedKeys ->
                    if (generatedKeys.next()) {
        entity.setId(generatedKeys.getInt(1))
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
    override fun delete(entity: GameObject) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("DELETE FROM " + getTableName() + " WHERE id = ?;")
        p?.setInt(1, entity.guid)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    override fun update(entity: GameObject) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("UPDATE " + getTableName() + " SET `template` = ?, `quantity` = ?, `position` = ?, `puit` = ?, `stats` = ? WHERE `id` = ?;")
        p?.setInt(1, entity.template!!.id)
        p?.setInt(2, entity.quantity)
        p?.setInt(3, entity.position)
        p?.setInt(4, entity.puit)
        p?.setString(5, entity.parseToSave())
        p?.setInt(6, entity.guid)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    override fun getReferencedClass(): Class<*> {
        return ObjectData::class.java
    }
    fun loads(ids: String) {
        try {
        getData("SELECT * FROM " + getTableName() + " WHERE `id` IN (" + ids + ");") { result ->
                while (result != null && result.next()) {
        var quantity: Int = result.getInt("quantity")
                    if (quantity > 0) {
        var gameObject: GameObject = World.world.newObjet(result.getInt("id"), result.getInt("template"), quantity,
        result.getInt("position"), result.getString("stats"), result.getInt("puit"))!!
        World.world.addGameObject(gameObject)
                    }
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
    }
}