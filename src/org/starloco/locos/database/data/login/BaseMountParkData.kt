package org.starloco.locos.database.data.login

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.entity.map.MountPark
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.database.data.game.MountParkData
import org.starloco.locos.game.world.World
import java.sql.PreparedStatement
import java.sql.SQLException

class BaseMountParkData(dataSource: HikariDataSource?) : FunctionDAO<MountPark>(dataSource, "world_base_mountparks") {

    override fun loadFully() {
        try {
            getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result.next()) {
                    val MP = MountPark(result.getShort("mapid").toInt(), result.getInt("cellid"), result.getInt("size"), result.getInt("priceBase"), result.getInt("cellMount"), result.getInt("cellporte"), result.getString("cellEnclos"), result.getInt("sizeObj"))
                    World.world.addMountPark(MP)
                    DatabaseManager.get(MountParkData::class.java).exist(MP)
                }
            }
        } catch (e: SQLException) {
            super.sendError(e)
        }
    }

    override fun load(id: Int): MountPark? {
        try {
            return getData<MountPark?>("SELECT * FROM " + getTableName() + " WHERE `mapid` = " + id + " ;") { result ->
                if (!result.next()) {
                    null
                } else {
                    val MP = MountPark(result.getInt("mapid"), result.getInt("cellid"), result.getInt("size"), result.getInt("priceBase"), result.getInt("cellMount"), result.getInt("cellporte"), result.getString("cellEnclos"), result.getInt("sizeObj"))
                    World.world.addMountPark(MP)
                    DatabaseManager.get(MountParkData::class.java).exist(MP)
                    MP
                }
            }
        } catch (e: SQLException) {
            super.sendError(e)
        }
        return null
    }

    override fun insert(entity: MountPark): Boolean {
        throw NotImplementedException()
    }

    override fun delete(entity: MountPark) {
        throw NotImplementedException()
    }

    override fun update(entity: MountPark) {
        var p: PreparedStatement? = null
        try {
            p = getPreparedStatement("UPDATE " + getTableName() + " SET `cellMount` =?, `cellPorte`=?, `cellEnclos`=? WHERE `mapid`=?")
            p?.setInt(1, entity.getMountcell())
            p?.setInt(2, entity.door)
            p?.setString(3, entity.parseStringCellObject())
            p?.setInt(4, entity.map)
            execute(p)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(p)
        }
    }

    override fun getReferencedClass(): Class<*> {
        return BaseMountParkData::class.java
    }
}
