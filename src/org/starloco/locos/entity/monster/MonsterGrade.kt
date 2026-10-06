package org.starloco.locos.entity.monster

import org.starloco.locos.client.other.Stats
import org.starloco.locos.common.Formulas
import org.starloco.locos.fight.spells.Spell
import org.starloco.locos.fight.spells.SpellEffect
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant
import org.starloco.locos.script.proxy.SMobGrade

class MonsterGrade private constructor(
    var template: Monster,
    val grade: Int,
    val level: Int,
    var pdv: Int,
    val pdvMax: Int,
    val pa: Int,
    val pm: Int,
    private val stats: MutableMap<Int, Int>,
    private val statsInfos: ArrayList<Int>,
    val spells: MutableMap<Int, Spell.SortStats>,
    val baseXp: Int,
    private var baseInit: Int = 0
) {
    private val scriptVal: SMobGrade = SMobGrade(this)
    private val size = baseSize + grade * sizeBonusPerGrade
    val buffs = ArrayList<SpellEffect>()

    constructor(
        template: Monster, grade: Int, level: Int, pa: Int, pm: Int, resists: String, strStats: String,
        statsInfosStr: String, allSpells: String, pdvMax: Int, aInit: Int, xp: Int
    ) : this(
        template, grade, level, pdvMax, pdvMax, pa, pm, HashMap(), ArrayList(), HashMap(), xp, aInit
    ) {

        val resist = resists.split(";")
        val stat = strStats.split(",")
        val statInfos = statsInfosStr.split(";")

        for (str in statInfos)
            this.statsInfos.add(str.toInt())

        try {
            if (resist.size > 3) {
                stats[Constant.STATS_ADD_RP_NEU] = resist[0].toInt()
                stats[Constant.STATS_ADD_RP_TER] = resist[1].toInt()
                stats[Constant.STATS_ADD_RP_FEU] = resist[2].toInt()
                stats[Constant.STATS_ADD_RP_EAU] = resist[3].toInt()
                stats[Constant.STATS_ADD_RP_AIR] = resist[4].toInt()
                stats[Constant.STATS_ADD_ADODGE] = resist[5].toInt()
                stats[Constant.STATS_ADD_MDODGE] = resist[6].toInt()
            } else {
                val split = resist[0].split(",")
                stats[-1] = split[0].toInt()
                stats[-100] = split[1].toInt()
                stats[Constant.STATS_ADD_ADODGE] = resist[1].toInt()
                stats[Constant.STATS_ADD_MDODGE] = resist[2].toInt()
            }

            stats[Constant.STATS_ADD_PA] = pa
            stats[Constant.STATS_ADD_PM] = pm
            stats[Constant.STATS_ADD_VITA] = pdvMax
            stats[Constant.STATS_ADD_FORC] = stat[0].toInt()
            stats[Constant.STATS_ADD_SAGE] = stat[1].toInt()
            stats[Constant.STATS_ADD_INTE] = stat[2].toInt()
            stats[Constant.STATS_ADD_CHAN] = stat[3].toInt()
            stats[Constant.STATS_ADD_AGIL] = stat[4].toInt()
            stats[Constant.STATS_ADD_DOMA] = statInfos[0].toInt()
            stats[Constant.STATS_ADD_PERDOM] = statInfos[1].toInt()
            stats[Constant.STATS_ADD_SOIN] = statInfos[2].toInt()
            stats[Constant.STATS_SUMMON_COUNT] = statInfos[3].toInt()
            if (resist.size > 5) {
                stats[Constant.STATS_ADD_SAGE] = resist[5].toInt() * 3
            }
        } catch (e: Exception) {
            throw RuntimeException("Monster (id:" + template.id + ", grade: " + grade + ") : reading stats failed.", e)
        }

        if (!allSpells.equals("", ignoreCase = true)) {
            val spellsStr = allSpells.split(";")

            for (str in spellsStr) {
                if (str == "") continue
                val spellInfo = str.split("@")
                var id = -1
                val lvl: Int

                try {
                    id = spellInfo[0].toInt()
                    lvl = spellInfo[1].toInt()
                } catch (e: Exception) {
                    World.world.logger.error(
                        "  > Error : Monster (id:" + template.id + ", grade: " + grade + ", spell: " + id + ") : reading spell id/level failed.",
                        e
                    )
                    continue
                }

                val spell = World.world.getSort(id)
                if (spell != null) {
                    val spellStats = spell.getStatsByLevel(lvl)
                    if (spellStats != null) this.spells[id] = spellStats
                }
            }
        }
    }

    fun getCopy(): MonsterGrade {
        val newStats = HashMap(this.stats)
        return MonsterGrade(this.template, this.grade, this.level, this.pdv, this.pdvMax, this.pa, this.pm, newStats, this.statsInfos, this.spells, this.baseXp)
    }

    fun refresh() {
        if (this.spells.isEmpty())
            return

        val spells = StringBuilder()
        for (entry in this.spells.entries) {
            spells.append(if (spells.length == 0) entry.key.toString() + "," + entry.value.level else ";" + entry.key + "," + entry.value.level)
        }

        this.spells.clear()

        for (split in spells.toString().split(";")) {
            val id = split.split(",")[0].toInt()
            this.spells[id] = World.world.getSort(id)!!.getStatsByLevel(split.split(",")[1].toInt())!!
        }
    }

    fun getSize(): Int = this.size

    fun getInit(): Int {
        val fact = 4
        val maxPdv = pdvMax
        val curPdv = pdv
        var coef = (maxPdv / fact).toDouble()

        coef += getStats().getEffect(Constant.STATS_ADD_INIT)
        coef += getStats().getEffect(Constant.STATS_ADD_AGIL)
        coef += getStats().getEffect(Constant.STATS_ADD_CHAN)
        coef += getStats().getEffect(Constant.STATS_ADD_INTE)
        coef += getStats().getEffect(Constant.STATS_ADD_FORC)

        var init = 1
        if (maxPdv != 0)
            init = (coef * (curPdv.toDouble() / maxPdv.toDouble())).toInt()
        if (init < 0)
            init = 0
        return init + this.baseInit
    }

    fun getStats(): Stats {
        if (this.template!!.id == 42 && !stats.containsKey(Constant.STATS_SUMMON_COUNT))
            stats[Constant.STATS_SUMMON_COUNT] = 5

        if (this.stats[-1] != null) {
            val stats = HashMap(this.stats)
            stats.remove(-1)
            stats.remove(-100)

            val random = Formulas.getRandomValue(210, 214)
            val one = this.stats[-1]!!
            val all = this.stats[-100]!!

            stats[Constant.STATS_ADD_RP_NEU] = if (random == Constant.STATS_ADD_RP_NEU) one else all
            stats[Constant.STATS_ADD_RP_TER] = if (random == Constant.STATS_ADD_RP_TER) one else all
            stats[Constant.STATS_ADD_RP_FEU] = if (random == Constant.STATS_ADD_RP_FEU) one else all
            stats[Constant.STATS_ADD_RP_EAU] = if (random == Constant.STATS_ADD_RP_EAU) one else all
            stats[Constant.STATS_ADD_RP_AIR] = if (random == Constant.STATS_ADD_RP_AIR) one else all
            return Stats(stats)
        }
        return Stats(this.stats)
    }

    fun scripted(): SMobGrade {
        return this.scriptVal
    }

    companion object {
        private const val baseSize = 90
        private const val sizeBonusPerGrade = 5
    }
}
