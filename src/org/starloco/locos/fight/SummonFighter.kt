package org.starloco.locos.fight

import org.starloco.locos.client.other.Stats
import org.starloco.locos.entity.monster.MonsterGrade
import org.starloco.locos.kernel.Constant
import java.util.HashMap
import java.util.stream.Stream

class SummonFighter(id: Int, f: Fight, mobGrade: MonsterGrade, @JvmField var summoner: Fighter) : MobFighter(id, f, mobGrade) {

    override fun getBaseStats(): Stats {
        if (summoner !is PlayerFighter) return super.getBaseStats()

        // Summons from player have a bonus
        val stats = HashMap(super.getBaseStats().effects)

//        if (mobID == 264 && caster.mob != null)
//            pdvMax = 425;
//        if (mobID == 114 && caster.mob != null)
//            pdvMax = 35;
//        if (mobID == 115 && caster.mob != null)
//            pdvMax = 90;
//        if (mobID == 262 && caster.player != null)
//            pdvMax = 225;
//        if (mobID == 246 && caster.player != null)
//            pdvMax = 80;
//        if (mobID == 1108 && caster.player != null)
//            pdvMax = 490;

        // https://www.dofus.com/fr/forum/1003-divers/293131-calculer-vie-invoquations
        val summonerBoost = 1 + summoner.getLvl() / 100.0
        Stream.of(
            Constant.STATS_ADD_SAGE,
            Constant.STATS_ADD_FORC,
            Constant.STATS_ADD_INTE,
            Constant.STATS_ADD_CHAN,
            Constant.STATS_ADD_AGIL,
            Constant.STATS_ADD_VITA
        )
                .forEach { stat -> stats[stat] = Math.floor(stats[stat]!! * summonerBoost).toInt() }

        return Stats(stats)
    }
}
