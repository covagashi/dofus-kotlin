package org.starloco.locos.database.data.game

import java.sql.Statement
import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import org.starloco.locos.hdv.BigStore
import org.starloco.locos.hdv.BigStoreListing
import org.starloco.locos.`object`.GameObject
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

class BigStoreListingData(dataSource: HikariDataSource?) : FunctionDAO<BigStoreListing>(dataSource, "hdvs_items") {
    override fun loadFully() {
        try {
        getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result.next()) {
        var bigStore: BigStore? = World.world.getHdv(result.getInt("map"))
                    if (bigStore != null) {
        var gameObject: GameObject? = World.world.getGameObject(result.getInt("itemID"))
                        if (gameObject == null) {
        this.delete(BigStoreListing(result.getInt("id"), 0, 0.toByte(), 0, null))
        continue
                        }
        var entry: BigStoreListing = BigStoreListing(result.getInt("id"), result.getInt("price"), result.getByte("count"), result.getInt("ownerGuid"), gameObject!!)
        bigStore.addEntry(entry)
                    }
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
    }
    override fun load(id: Int): BigStoreListing? {
        throw NotImplementedException()
    }
    override fun insert(entity: BigStoreListing): Boolean {
        var p: PreparedStatement? = null
        var ok: Boolean = true
        try {
        p = this.getConnection()?.prepareStatement("INSERT INTO " + getTableName() + " (`map`,`ownerGuid`,`price`,`count`,`itemID`) VALUES(?,?,?,?,?);", Statement.RETURN_GENERATED_KEYS)
        p?.setInt(1, entity.hdvId)
        p?.setInt(2, entity.owner)
        p?.setInt(3, entity.price)
        p?.setInt(4, entity.lotSize!!.value)
        p?.setInt(5, entity.gameObject!!.guid)
        var affectedRows: Int = p?.executeUpdate() ?: 0
            if (affectedRows == 0) {
        ok = false
            } else {
        p?.getGeneratedKeys()?.use { generatedKeys ->
                    if (generatedKeys.next()) {
        entity.id = generatedKeys.getInt(1)
                    } else {
        ok = false
                    }
                }
            }
        DatabaseManager.get(ObjectTemplateData::class.java).update(entity.gameObject!!.template!!)
        } catch (e: SQLException) {
        ok = false
        super.sendError(e)
        } finally {
        close(p)
        }
        return ok
    }
    override fun delete(entity: BigStoreListing) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("DELETE FROM " + getTableName() + " WHERE `id` = ?;")
        p?.setInt(1, entity.id)
        execute(p)
        var gameObject: GameObject? = entity.gameObject
            if (gameObject != null) {
        DatabaseManager.get(ObjectTemplateData::class.java).update(gameObject!!.template!!)
            }
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    override fun update(entity: BigStoreListing) {
        throw NotImplementedException()
    }
    override fun getReferencedClass(): Class<*> {
        return BigStoreListingData::class.java
    }
}