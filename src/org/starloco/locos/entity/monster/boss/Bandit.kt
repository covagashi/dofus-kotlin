package org.starloco.locos.entity.monster.boss

import org.starloco.locos.area.map.GameMap
import org.starloco.locos.common.Formulas
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.game.GangsterData
import org.starloco.locos.entity.monster.Monster
import org.starloco.locos.game.world.World
import org.starloco.locos.util.TimerWaiter
import java.util.ArrayList
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(Bandit::class.java)

class Bandit(mobs: String, maps: String, time: Long) {

    val monsters = ArrayList<Monster>()
    val maps = ArrayList<GameMap>()
    var time: Long = 0
    var isPop = false

    init {
        if (!mobs.equals("", ignoreCase = true)) {
            for (mob in mobs.split(",")) {
                var _mob: Int? = null
                try {
                    _mob = Integer.parseInt(mob)
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                }
                if (_mob == null)
                    continue

                val monstre = World.world.getMonstre(_mob) ?: continue
                this.monsters.add(monstre)
            }
        }

        if (!maps.equals("", ignoreCase = true)) {
            for (str in maps.split(",")) {
                try {
                    val id = Integer.parseInt(str)
                    val map = World.world.getMap(id)
                    if (map == null || map.mountPark != null)
                        continue
                    this.maps.add(map)
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                }
            }
        }

        this.time = time
        bandits = this

        run()
    }

    companion object {
        private var bandits: Bandit? = null

        @JvmStatic
        fun getBandits(): Bandit? = bandits

        @JvmStatic
        fun run() {
            val bandit = getBandits() ?: return
            if (bandit.isPop) {
                TimerWaiter.addNext({ run() }, (1000 * 60 * 60).toLong())
            } else {
                val time = bandit.time
                val actuel = System.currentTimeMillis()
                if (time <= 0) {
                    pop(bandit, actuel)
                } else {
                    val random = Formulas.getRandomValue(6, 18)
                    val timeRandom = 1000L * 60 * 60 * random // Temps en MS d'heures entre les repops des bandits
                    if (time + timeRandom <= actuel) {
                        pop(bandit, actuel)
                    } else {
                        TimerWaiter.addNext({ run() }, (1000 * 60 * 60).toLong())
                    }
                }
            }
        }

        private fun pop(bandit: Bandit, actuel: Long) {
            try {
                bandit.time = actuel
                val nbMap = bandit.maps.size
                val random = Formulas.getRandomValue(0, nbMap - 1)
                val map = bandit.maps[random]
                var groupData = StringBuilder()
                for (monstre in bandit.monsters) {
                    val id = monstre.id
                    val lvl = monstre.getRandomGrade()!!.level
                    if (groupData.toString().equals("", ignoreCase = true))
                        groupData = StringBuilder("$id,$lvl,$lvl")
                    else
                        groupData.append(";").append(id).append(",").append(lvl).append(",").append(lvl)
                }
                map.nextObjectId++
                map.spawnNewGroup(false, map.randomFreeCellId, groupData.toString(), "")
                DatabaseManager.get(GangsterData::class.java).update(bandit)
            } catch (e: Exception) {
                log.error("unexpected error", e)
                TimerWaiter.addNext({ pop(bandit, actuel) }, 60000)
            }
        }
    }

    fun parseMobs(): String {
        val str = StringBuilder()
        for (monster in this.monsters) {
            if (str.length > 0) str.append(",")
            str.append(monster.id)
        }
        return str.toString()
    }
}
