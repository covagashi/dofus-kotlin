package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.entity.map.MountPark
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import java.sql.PreparedStatement
import java.sql.SQLException

class MountParkData(dataSource: HikariDataSource?) : FunctionDAO<MountPark>(dataSource, "mountpark_data") {
    override fun loadFully() {
        try {
        getData("SELECT * from mountpark_data") { result ->
        while (result.next()) {
        var map: Int = result.getInt("mapid")
        var park: MountPark? = World.world.mountParks.get(map)
        if (park == null) continue
        var owner: Int = result.getInt("owner")
        var guild: Int = result.getInt("guild")
        guild = if (World.world.getGuild(guild) != null) guild else -1
        var price: Int = result.getInt("price")
        var data: String = result.getString("data")
        var enclos: String = result.getString("enclos")
        var objetPlacer: String = result.getString("ObjetPlacer")
        var durabilite: String = result.getString("durabilite")
        enclos = if (enclos == " ") "" else enclos
        objetPlacer = if (objetPlacer == " ") "" else objetPlacer
        durabilite = if (durabilite == " ") "" else durabilite
        park.setData(owner, guild, price, data, objetPlacer, durabilite, enclos)
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
    }
    override fun load(id: Int): MountPark? {
        throw NotImplementedException()
    }
    override fun insert(entity: MountPark): Boolean {
        var p: PreparedStatement? = null
        var ok: Boolean = true
        try {
        p = getPreparedStatement("INSERT INTO " + getTableName() + " (`mapid`, `owner`, `guild`, `price`, `data`, `enclos`, `ObjetPlacer`, `durabilite`) VALUES (?, ?, ?, ?, '', '', '', '');")
        p?.setInt(1, entity.map)
        p?.setInt(2, 0)
        p?.setInt(3, -1)
        p?.setInt(4, entity.priceBase)
        execute(p)
        } catch (e: SQLException) {
        ok = false
        super.sendError(e)
        } finally {
        close(p)
        }
        return ok
    }
    override fun delete(entity: MountPark) {
        throw NotImplementedException()
    }
    override fun update(entity: MountPark) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("UPDATE " + getTableName() + " SET  `owner`=?, `guild`=?, `price` =?, `data` =?, `enclos` =?, `ObjetPlacer`=?, `durabilite`=? WHERE `mapid`=?;")
        p?.setInt(1, entity.owner)
        p?.setInt(2, if (entity.guild != null) entity.guild!!.id else -1)
        p?.setInt(3, entity.price)
        p?.setString(4, entity.parseEtableToString())
        p?.setString(5, entity.parseRaisingToString())
        p?.setString(6, entity.getStringObject())
        p?.setString(7, entity.getStringObjDurab())
        p?.setInt(8, entity.map)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    override fun getReferencedClass(): Class<*> {
        return MountParkData::class.java
    }
    fun exist(mountPark: MountPark) {
        try {
        getData("SELECT * FROM " + getTableName() + " WHERE `mapid` = '" + mountPark.map + "';") { result ->
                if (!result.next()) {
        this.insert(mountPark)
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
    }
}