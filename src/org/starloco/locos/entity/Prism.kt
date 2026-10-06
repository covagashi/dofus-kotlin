package org.starloco.locos.entity

import org.starloco.locos.area.Area
import org.starloco.locos.area.SubArea
import org.starloco.locos.client.other.Stats
import org.starloco.locos.common.SocketManager
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.PlayerFighter
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant
import org.starloco.locos.util.TimerWaiter

class Prism(val id: Int, alignment: Byte, level: Int, val map: Int, var cell: Int, honor: Int, area: Int) {

    var alignment: Int = alignment.toInt()
    var state: Byte = NEW
    var level: Int = level
    private var name: Int = 0
    @JvmField
    val gfx: Int
    var honor: Int = honor
    var conquestArea: Int = area
    var fight: Fight? = null
    private val stats = HashMap<Int, Int>()

    init {
        if (alignment.toInt() == 1) {
            this.name = 1111
            this.gfx = 8101
        } else {
            this.name = 1112
            this.gfx = 8100
        }
        TimerWaiter.addNext({ this.state = NORMAL }, (60 * 60_000).toLong())
    }

    val grade: Int
        get() {
            if (this.honor >= 17500)
                return 10
            return World.world.experiences!!.pvp.levelForXp(this.honor.toLong())
        }

    fun addHonor(i: Int) {
        this.honor += i
    }

    fun getStats(): Stats {
        return Stats(this.stats)
    }

    fun refreshStats() {
        val feu = 1000 + 500 * this.level
        val intel = 1000 + 500 * this.level
        val agi = 1000 + 500 * this.level
        val sagesse = 1000 + 500 * this.level
        val chance = 1000 + 500 * this.level
        val resistance = 9 * this.level
        this.stats.clear()
        this.stats[Constant.STATS_ADD_FORC] = feu
        this.stats[Constant.STATS_ADD_INTE] = intel
        this.stats[Constant.STATS_ADD_AGIL] = agi
        this.stats[Constant.STATS_ADD_SAGE] = sagesse
        this.stats[Constant.STATS_ADD_CHAN] = chance
        this.stats[Constant.STATS_ADD_RP_NEU] = resistance
        this.stats[Constant.STATS_ADD_RP_FEU] = resistance
        this.stats[Constant.STATS_ADD_RP_EAU] = resistance
        this.stats[Constant.STATS_ADD_RP_AIR] = resistance
        this.stats[Constant.STATS_ADD_RP_TER] = resistance
        this.stats[Constant.STATS_ADD_ADODGE] = resistance
        this.stats[Constant.STATS_ADD_MDODGE] = resistance
        this.stats[Constant.STATS_ADD_PA] = 6
        this.stats[Constant.STATS_ADD_PM] = 0
    }

    val x: Int
        get() = World.world.getMap(this.map).x

    val y: Int
        get() = World.world.getMap(this.map).y

    val subArea: SubArea
        get() = World.world.getMap(this.map).subArea!!

    fun getArea(): Area {
        val map = World.world.getMap(this.map)
        return map.subArea!!.area!!
    }

    fun parseToGM(): String {
        if (this.fight != null)
            return ""
        return "GM|+" + this.cell + ";1;0;" + this.id + ";" + this.name + ";-10;" + this.gfx + "^100;" + this.level + ";" + grade + ";" + this.alignment
    }

    companion object {
        const val NEW: Byte = 1
        const val NORMAL: Byte = 2
        const val FIGHTING: Byte = 3

        @JvmStatic
        fun parseAttack(player: org.starloco.locos.client.Player) {
            for (prism in World.world.AllPrisme()!!)
                if (prism.fight != null && player.alignment == prism.alignment)
                    SocketManager.SEND_Cp_INFO_ATTAQUANT_PRISME(
                        player,
                        attackerOfPrisme(prism.id, prism.map, prism.fight!!.id)
                    )
        }

        @JvmStatic
        fun parseDefense(player: org.starloco.locos.client.Player) {
            for (prism in World.world.AllPrisme()!!)
                if (prism.fight != null && player.alignment == prism.alignment)
                    SocketManager.SEND_CP_INFO_DEFENSEURS_PRISME(
                        player,
                        defenderOfPrisme(prism.id, prism.map, prism.fight!!.id)
                    )
        }

        @JvmStatic
        fun attackerOfPrisme(id: Int, MapId: Int, FightId: Int): String {
            val str = StringBuilder("+")
            str.append(Integer.toString(id, 36))
            val gameMap = World.world.getMap(MapId) ?: return str.toString()

            for (fight in gameMap.fights) {
                if (fight.id == FightId) {
                    for (fighter in fight.getFighters(1)) {
                        if (fighter !is PlayerFighter)
                            continue
                        str.append("|")
                        str.append(Integer.toString(fighter.id, 36)).append(";")
                        str.append(fighter.getPacketsName()).append(";")
                        str.append(fighter.getLvl()).append(";")
                        str.append("0;")
                    }
                }
            }
            return str.toString()
        }

        @JvmStatic
        fun defenderOfPrisme(id: Int, MapId: Int, FightId: Int): String {
            var str = "+"
            var stra = ""
            str += Integer.toString(id, 36)
            val gameMap = World.world.getMap(MapId)
            if (gameMap != null) {
                for (fight in gameMap.fights) {
                    if (fight.id == FightId) {
                        for (fighter in fight.getFighters(2)) {
                            if (fighter !is PlayerFighter)
                                continue
                            str += "|"
                            str += Integer.toString(fighter.id, 36) + ";"
                            str += fighter.getPacketsName() + ";"
                            str += fighter.getDefaultGfx().toString() + ";"
                            str += fighter.getLvl().toString() + ";"
                            str += Integer.toString(fighter.getColors()[0], 36) + ";"
                            str += Integer.toString(fighter.getColors()[1], 36) + ";"
                            str += Integer.toString(fighter.getColors()[2], 36) + ";"
                            if (fight.getFighters(2).size > 7)
                                str += "1;"
                            else
                                str += "0;"
                        }
                        stra = str.substring(1)
                        stra = "-$stra"
                        fight.setDefenders(stra)
                    }
                }
            }
            return str
        }
    }
}
