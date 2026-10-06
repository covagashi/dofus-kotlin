package org.starloco.locos.dynamic

import org.starloco.locos.entity.Collector
import org.starloco.locos.fight.CollectorFighter
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.MobFighter
import org.starloco.locos.fight.CloneFighter
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Config
import org.starloco.locos.kernel.Constant
import java.util.ArrayList

object FormuleOfficiel {

    @JvmStatic
    fun getXp(`object`: Any?, winners: ArrayList<Fighter>,
              groupXp: Long, nbonus: Byte, star: Int, challenge: Int, lvlMax: Int,
              lvlMin: Int, lvlLoosers: Int, lvlWinners: Int): Long {
        var nbonus = nbonus
        if (lvlMin <= 0 || `object` == null)
            return 0
        if (`object` is Fighter) {
            val fighter = `object`
            val player = fighter.player!!

            if (winners.contains(fighter)) {
                if (lvlWinners <= 0)
                    return 0

                val sagesse = fighter.getLvl() * 0.5 + fighter.player!!.getTotalStats(true)
                    .getEffect(Constant.STATS_ADD_SAGE)
                var nvGrpMonster = lvlMax.toDouble() / lvlMin.toDouble()
                var bonus = 1.0
                var rapport = lvlLoosers.toDouble() / lvlWinners.toDouble()

                if (winners.size == 1)
                    rapport = 0.6
                else if (rapport == 0.0)
                    return 0
                else if (rapport <= 1.1 && rapport >= 0.9)
                    rapport = 1.0
                else {
                    if (rapport > 1)
                        rapport = 1 / rapport
                    if (rapport < 0.01)
                        rapport = 0.01
                }

                var sizeGroupe = 0
                for (f in winners) {
                    if (f.player != null && !f.isInvocation()
                        && f !is MobFighter && f !is CollectorFighter && f !is CloneFighter)
                        sizeGroupe++
                }
                if (sizeGroupe < 1)
                    return 0
                if (sizeGroupe > 8)
                    sizeGroupe = 8

                if (nbonus > 8)
                    nbonus = 8
                when (nbonus.toInt()) {
                    0 -> bonus = 0.5
                    1 -> bonus = 0.5
                    2 -> bonus = 2.1
                    3 -> bonus = 3.2
                    4 -> bonus = 4.3
                    5 -> bonus = 5.4
                    6 -> bonus = 6.5
                    7 -> bonus = 7.8
                    8 -> bonus = 9.0
                }
                if (nvGrpMonster == 0.0)
                    return 0
                else if (nvGrpMonster < 3.0)
                    nvGrpMonster = 1.0
                else
                    nvGrpMonster = 1 / nvGrpMonster

                if (nvGrpMonster < 0)
                    nvGrpMonster = 0.0
                else if (nvGrpMonster > 1)
                    nvGrpMonster = 1.0

                var total = (((1 + (sagesse / 100)) * (1 + (challenge / 100)) * (1 + (star / 100))
                    * (bonus + rapport) * nvGrpMonster * (groupXp / sizeGroupe))
                    * (if (player.level != 199) Config.rateXp else 1) * World.world.getConquestBonus(fighter.player)).toLong()

                if (Config.modeHeroic && player.level < player.deadLevel) {
                    total *= 2
                }
                if (player.level != 199 && player.account.isSubscribeWithoutCondition()) {
                    val newTotal = (total * 1.2).toLong()
                    player.sendMessage(player.lang.trans("dynamic.formuleofficiel.exp", newTotal - total))
                    return newTotal
                }
                return total
            }
        } else if (`object` is Collector) {
            val collector = `object`

            if (World.world.getGuild(collector.guildId) == null)
                return 0

            if (lvlWinners <= 0)
                return 0

            val sagesse = World.world.getGuild(collector.guildId)!!.lvl * 0.5 +
                World.world.getGuild(collector.guildId)!!.getStats(Constant.STATS_ADD_SAGE)
            var nvGrpMonster = lvlMax.toDouble() / lvlMin.toDouble()
            var bonus = 1.0
            var rapport = lvlLoosers.toDouble() / lvlWinners.toDouble()

            if (winners.size == 1)
                rapport = 0.6
            else if (rapport == 0.0)
                return 0
            else if (rapport <= 1.1 && rapport >= 0.9)
                rapport = 1.0
            else {
                if (rapport > 1)
                    rapport = 1 / rapport
                if (rapport < 0.01)
                    rapport = 0.01
            }

            var sizeGroupe = 0
            for (f in winners) {
                if (f.player != null && !f.isInvocation()
                    && f !is MobFighter && f !is CollectorFighter && f !is CloneFighter)
                    sizeGroupe++
            }
            if (sizeGroupe < 1)
                return 0
            if (sizeGroupe > 8)
                sizeGroupe = 8

            if (nbonus > 8)
                nbonus = 8
            when (nbonus.toInt()) {
                0 -> bonus = 0.5
                1 -> bonus = 0.5
                2 -> bonus = 2.1
                3 -> bonus = 3.2
                4 -> bonus = 4.3
                5 -> bonus = 5.4
                6 -> bonus = 6.5
                7 -> bonus = 7.8
                8 -> bonus = 9.0
            }
            if (nvGrpMonster == 0.0)
                return 0
            else if (nvGrpMonster < 3.0)
                nvGrpMonster = 1.0
            else
                nvGrpMonster = 1 / nvGrpMonster

            if (nvGrpMonster < 0)
                nvGrpMonster = 0.0
            else if (nvGrpMonster > 1)
                nvGrpMonster = 1.0

            return (((1 + ((sagesse + star + challenge) / 100))
                * (bonus + rapport) * nvGrpMonster * (groupXp / sizeGroupe)) * Config.rateXp).toLong()
        }
        return 0
    }
}
