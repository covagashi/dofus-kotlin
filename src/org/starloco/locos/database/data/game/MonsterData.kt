package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.entity.monster.Monster
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Main
import java.sql.SQLException

class MonsterData(dataSource: HikariDataSource?) : FunctionDAO<Monster>(dataSource, "monsters") {
    override fun loadFully() {
        try {
        getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result.next()) {
        var id: Int = result.getInt("id")
        var gfxID: Int = result.getInt("gfxID")
        var align: Int = result.getInt("align")
        var colors: String = result.getString("colors")
        var grades: String = result.getString("grades")
        var spells: String = result.getString("spells")
        var stats: String = result.getString("stats")
        var statsInfos: String = result.getString("statsInfos")
        var pdvs: String = result.getString("pdvs")
        var pts: String = result.getString("points")
        var inits: String = result.getString("inits")
        var mK: Int = result.getInt("minKamas")
        var MK: Int = result.getInt("maxKamas")
        var xp: String = result.getString("exps")
        var IAType: Int = result.getInt("AI_Type")
        var capturable: Boolean = result.getInt("capturable") == 1
        var aggroDistance: Int = result.getInt("aggroDistance")
        var isBoss: Boolean = result.getInt("isBoss") == 1
        var isArchMonster: Boolean = result.getInt("isBoss") == 1
                    if (World.world.getMonstre(id) == null) {
        World.world.addMobTemplate(id, Monster(id, gfxID, align, colors, grades, spells, stats, statsInfos, pdvs, pts, inits, mK, MK, xp, IAType, capturable, aggroDistance, isBoss, isArchMonster))
                    } else {
        World.world.getMonstre(id)!!.setInfos(gfxID, align, colors, grades, spells, stats, statsInfos, pdvs, pts, inits, mK, MK, xp, IAType, capturable, aggroDistance, isBoss, isArchMonster)
                    }
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        Main.stop("Can't load monsters")
        }
    }
    override fun load(id: Int): Monster {
        throw NotImplementedException()
    }
    override fun insert(entity: Monster): Boolean {
        throw NotImplementedException()
    }
    override fun delete(entity: Monster) {
        throw NotImplementedException()
    }
    override fun update(entity: Monster) {
        throw NotImplementedException()
    }
    override fun getReferencedClass(): Class<*> {
        return MonsterData::class.java
    }
}