package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.area.map.GameMap
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.entity.monster.MonsterGroup
import org.starloco.locos.game.world.World
import org.starloco.locos.`object`.GameObject
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.Objects

/**
 * Created by Locos on 15/08/2015.
 */
class HeroicMobsGroupsData(dataSource: HikariDataSource?) : FunctionDAO<Any>(dataSource) {

    override fun loadFully() {
        try {
            getData("SELECT * FROM `heroic_mobs_groups`;") { result ->
                while (result.next()) {
                    val map = World.world.getMap(result.getShort("map").toInt())
                    if (map != null) {
                        val group = MonsterGroup(result.getInt("id"), result.getInt("cell"), result.getString("group"), result.getString("objects"), result.getShort("stars"))
                        map.respawnGroup(group)
                    }
                }
            }
        } catch (e: SQLException) {
            super.sendError(e)
        }
    }

    override fun load(id: Int): Any {
        throw NotImplementedException()
    }

    override fun insert(entity: Any): Boolean {
        throw NotImplementedException()
    }

    override fun delete(entity: Any) {
        throw NotImplementedException()
    }

    override fun update(entity: Any) {
        throw NotImplementedException()
    }

    override fun getReferencedClass(): Class<*> {
        return HeroicMobsGroupsData::class.java
    }

    fun insert(map: Int, group: MonsterGroup) {
        var prepare: PreparedStatement? = null
        try {
            val objects = StringBuilder()
            val groups = StringBuilder()

            group.getObjects()?.stream()?.filter(Objects::nonNull)
                ?.forEach { o -> objects.append(if (objects.toString().isEmpty()) "" else ",").append(o.guid) }
            group.mobs.values.stream().filter(Objects::nonNull)
                .forEach { monster -> groups.append(if (groups.toString().isEmpty()) "" else ";").append(monster.template!!.id).append(",").append(monster.level).append(",").append(monster.level) }

            prepare = getPreparedStatement("INSERT INTO `heroic_mobs_groups` VALUES (?, ?, ?, ?, ?, ?);")
            prepare?.setInt(1, group.id)
            prepare?.setInt(2, map)
            prepare?.setInt(3, group.cellId)
            prepare?.setString(4, groups.toString())
            prepare?.setString(5, objects.toString())
            prepare?.setInt(6, group.getStarBonus())
            execute(prepare)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(prepare)
        }
    }

    fun update(map: Short, group: MonsterGroup) {
        var prepare: PreparedStatement? = null
        try {
            val objects = StringBuilder()
            val groups = StringBuilder()
            group.getObjects()?.stream()?.filter(Objects::nonNull)?.forEach { o -> objects.append(if (objects.toString().isEmpty()) "" else ",").append(o.guid) }
            group.mobs.values.stream().filter(Objects::nonNull).forEach { monster -> groups.append(if (groups.toString().isEmpty()) "" else ";").append(monster.template!!.id).append(",").append(monster.level).append(",").append(monster.level) }

            prepare = getPreparedStatement("UPDATE `heroic_mobs_groups` SET `objects` = ?, `stars` = ? WHERE `id` = ? AND `map` = ? AND `group` = ?;")
            prepare?.setString(1, objects.toString())
            prepare?.setInt(2, group.getStarBonus())
            prepare?.setLong(3, group.id.toLong())
            prepare?.setInt(4, map.toInt())
            prepare?.setString(5, groups.toString())
            execute(prepare)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(prepare)
        }
    }

    fun delete(map: Short, group: MonsterGroup) {
        var prepare: PreparedStatement? = null
        try {
            val groups = StringBuilder()
            group.mobs.values.stream().filter(Objects::nonNull).forEach { monster -> groups.append(if (groups.toString().isEmpty()) "" else ";").append(monster.template!!.id).append(",").append(monster.level).append(",").append(monster.level) }

            prepare = getPreparedStatement("DELETE FROM `heroic_mobs_groups` WHERE `id` = ? AND `map` = ? AND `group` = ?;")
            prepare?.setLong(1, group.id.toLong())
            prepare?.setInt(2, map.toInt())
            prepare?.setString(3, groups.toString())
            execute(prepare)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(prepare)
        }
    }

    fun deleteAll() {
        var prepare: PreparedStatement? = null
        try {
            prepare = getPreparedStatement("DELETE FROM `heroic_mobs_groups`;")
            execute(prepare)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(prepare)
        }
    }

    fun loadFix() {
        try {
            getData("SELECT * FROM `heroic_mobs_groups_fix`;") { result ->
                while (result.next()) {
                    val objects = ArrayList<GameObject>()
                    for (value in result.getString("objects").split(",")) {
                        val obj = World.world.getGameObject((value).toInt())
                        if (obj != null)
                            objects.add(obj)
                    }
                    val map = World.world.getMap(result.getInt("map"))
                    val cell = result.getInt("cell")
                    for (group in map.mobGroups.values) {
                        if (group != null && group.cellId == cell)
                            group.setStarBonus(result.getShort("stars"))
                    }
                    GameMap.fixMobGroupObjects[result.getInt("map").toString() + "," + result.getInt("cell")] = objects
                }
            }
        } catch (e: SQLException) {
            super.sendError(e)
        }
    }

    fun insertFix(map: Int, group: MonsterGroup) {
        var prepare: PreparedStatement? = null
        try {
            val objects = StringBuilder()
            val groups = StringBuilder()
            group.getObjects()?.stream()?.filter { o -> o != null }?.forEach { o -> objects.append(if (objects.toString().isEmpty()) "" else ",").append(o!!.guid) }
            group.mobs.values.stream().filter { monster -> monster != null }.forEach { monster -> groups.append(if (groups.toString().isEmpty()) "" else ";").append(monster.template!!.id).append(",").append(monster.level).append(",").append(monster.level) }

            prepare = getPreparedStatement("INSERT INTO `heroic_mobs_groups_fix` VALUES (?, ?, ?, ?)")
            prepare?.setInt(1, map)
            prepare?.setInt(2, group.cellId)
            prepare?.setString(3, groups.toString())
            prepare?.setString(4, objects.toString())
            prepare?.setInt(5, group.getStarBonus())
            execute(prepare)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(prepare)
        }
    }

    fun updateFix() {
        var prepare: PreparedStatement? = null
        try {
            for (entry in GameMap.fixMobGroupObjects.entries) {
                val split = entry.key.split(",")
                val objects = StringBuilder()
                entry.value.stream().filter { o -> o != null }.forEach { o -> objects.append(if (objects.toString().isEmpty()) "" else ",").append(o!!.guid) }

                prepare = getPreparedStatement("UPDATE `heroic_mobs_groups_fix` SET `objects` = ? WHERE `map` = ? AND `cell` = ? AND `group` = ?;")
                prepare?.setString(1, objects.toString())
                prepare?.setLong(2, (split[0]).toInt().toLong())
                prepare?.setInt(3, (split[1]).toInt())
                prepare?.setString(4, World.world.getGroupFix((split[0]).toInt(), (split[1]).toInt())!!["groupData"])
                execute(prepare)
            }
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(prepare)
        }
    }

    fun deleteAllFix() {
        var prepare: PreparedStatement? = null
        try {
            prepare = getPreparedStatement("DELETE FROM `heroic_mobs_groups_fix`;")
            execute(prepare)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(prepare)
        }
    }
}
