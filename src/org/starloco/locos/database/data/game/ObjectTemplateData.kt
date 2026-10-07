package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Main
import org.starloco.locos.`object`.ObjectTemplate
import java.sql.PreparedStatement
import java.sql.SQLException

class ObjectTemplateData(dataSource: HikariDataSource?) : FunctionDAO<ObjectTemplate>(dataSource, "item_template") {
    override fun loadFully() {
        try {
        getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result.next()) {
        var template: ObjectTemplate? = World.world.getObjTemplate(result.getInt("id"))
                    if (template != null) {
                        template.setInfos(result.getString("statsTemplate"), result.getString("name") ?: "", result.getInt("type"),
                                result.getInt("level"), result.getInt("pod"), result.getInt("prix"), result.getInt("panoplie"),
                                result.getString("conditions") ?: "", result.getString("armesInfos"), result.getInt("sold"), result.getInt("avgPrice"),
        result.getInt("points"), result.getInt("newPrice"))
                    } else {
                        World.world.addObjTemplate(ObjectTemplate(result.getInt("id"), result.getString("statsTemplate"),
                                result.getString("name") ?: "", result.getInt("type"), result.getInt("level"), result.getInt("pod"),
                                result.getInt("prix"), result.getInt("panoplie"), result.getString("conditions") ?: "", result.getString("armesInfos"),
        result.getInt("sold"), result.getInt("avgPrice"), result.getInt("points"), result.getInt("newPrice")))
                    }
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        Main.stop("Can't load objects templates")
        }
    }
    override fun load(id: Int): ObjectTemplate {
        throw NotImplementedException()
    }
    override fun insert(entity: ObjectTemplate): Boolean {
        throw NotImplementedException()
    }
    override fun delete(entity: ObjectTemplate) {
        throw NotImplementedException()
    }
    override fun update(entity: ObjectTemplate) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("UPDATE " + getTableName() + " SET sold = ?,avgPrice = ? WHERE id = ?")
        p?.setLong(1, entity.sold)
        p?.setInt(2, entity.avgPrice)
        p?.setInt(3, entity.id)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    override fun getReferencedClass(): Class<*> {
        return ObjectTemplateData::class.java
    }
}