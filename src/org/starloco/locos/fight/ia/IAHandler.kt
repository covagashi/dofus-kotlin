package org.starloco.locos.fight.ia

import org.starloco.locos.entity.monster.MonsterGrade
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.CloneFighter
import org.starloco.locos.fight.CollectorFighter
import org.starloco.locos.fight.ia.type.Blank
import org.starloco.locos.fight.ia.type.IA1
import org.starloco.locos.fight.ia.type.IA2
import org.starloco.locos.fight.ia.type.IA3
import org.starloco.locos.fight.ia.type.IAPerco
import org.starloco.locos.fight.ia.type.boss.IA10
import org.starloco.locos.fight.ia.type.boss.IA11
import org.starloco.locos.fight.ia.type.boss.IA17
import org.starloco.locos.fight.ia.type.boss.IA18
import org.starloco.locos.fight.ia.type.boss.IA20
import org.starloco.locos.fight.ia.type.boss.IA22
import org.starloco.locos.fight.ia.type.boss.IA23
import org.starloco.locos.fight.ia.type.invocations.Blocker
import org.starloco.locos.fight.ia.type.invocations.Chafer
import org.starloco.locos.fight.ia.type.invocations.Lapino
import org.starloco.locos.fight.ia.type.invocations.Tonneau
import org.starloco.locos.fight.ia.type.invocations.dopeuls.Cra
import org.starloco.locos.fight.ia.type.invocations.dopeuls.Ecaflip
import org.starloco.locos.fight.ia.type.invocations.dopeuls.Eniripsa
import org.starloco.locos.fight.ia.type.invocations.dopeuls.Enutrof
import org.starloco.locos.fight.ia.type.invocations.dopeuls.Feca
import org.starloco.locos.fight.ia.type.invocations.dopeuls.Iop
import org.starloco.locos.fight.ia.type.invocations.dopeuls.Osamodas
import org.starloco.locos.fight.ia.type.invocations.dopeuls.Pandawa
import org.starloco.locos.fight.ia.type.invocations.dopeuls.Sacrieur
import org.starloco.locos.fight.ia.type.invocations.dopeuls.Sadida
import org.starloco.locos.fight.ia.type.invocations.dopeuls.Sram
import org.starloco.locos.fight.ia.type.invocations.dopeuls.Xelor

/**
 * Created by Locos on 18/09/2015.
 */
class IAHandler {

    companion object {
        @JvmStatic
        fun select(fight: Fight, fighter: Fighter) {
            var ia: IA = Blank(fight, fighter)
            val monsterGrade: MonsterGrade? = fighter.mob

            if (monsterGrade == null) {
                if (fighter is CloneFighter)
                    ia = Blocker(fight, fighter, 4)
                if (fighter is CollectorFighter)
                    ia = IAPerco(fight, fighter, 7)
            } else if (monsterGrade.template == null) {
                ia.setStop(true)
                ia.endTurn()
            } else {
                //region select ia
                when (monsterGrade.template!!.ia) {
                    1 -> ia = IA1(fight, fighter, 4) // Random attack friend/enemy
                    2 -> ia = IA2(fight, fighter, 7) // Aggressif
                    3 -> ia = IA3(fight, fighter, 7) // Mi distance

                    10 -> ia = IA10(fight, fighter, 4) // Kralamour géant
                    11 -> ia = IA11(fight, fighter, 2) // Tentacule du Kralamour

                    17 -> ia = IA17(fight, fighter, 6) //IA KIMBO
                    18 -> ia = IA18(fight, fighter, 4) //Disciple
                    20 -> ia = IA20(fight, fighter, 2) //IA Kaskargo

                    22 -> ia = IA22(fight, fighter, 4) //IA Rasboul
                    23 -> ia = IA23(fight, fighter, 3) //IA Rasboul mineur

                    //region Player invocations
                    100 -> ia = Chafer(fight, fighter, 4) // Invocation du Chafer/Chaferfu lancier
                    102 -> ia = Lapino(fight, fighter, 4) // Lapino & Gonflabe & Sac animé
                    107 -> ia = Tonneau(fight, fighter, 4) // Tonneau
                    //endregion

                    //region Dopeuls
                    110 -> ia = Pandawa(fight, fighter, 5) //Pandawa
                    111 -> ia = Feca(fight, fighter, 4) //Feca
                    112 -> ia = Sacrieur(fight, fighter, 4) //Sacrieur
                    113 -> ia = Sadida(fight, fighter, 4) //Sadida
                    114 -> ia = Osamodas(fight, fighter, 4) //Osamodas
                    115 -> ia = Enutrof(fight, fighter, 5) //Enutrof
                    116 -> ia = Sram(fight, fighter, 4) //Sram
                    117 -> ia = Xelor(fight, fighter, 4) //Xélor
                    118 -> ia = Ecaflip(fight, fighter, 4) //Ecaflip
                    119 -> ia = Eniripsa(fight, fighter, 4) //Eniripsa
                    120 -> ia = Iop(fight, fighter, 4) //Iop
                    121 -> ia = Cra(fight, fighter, 4) //Cra
                    else -> ia = Blank(fight, fighter)
                    //endregion
                }
                //endregion
            }

            val finalIA = ia
            ia.addNext({ finalIA.apply() }, 250)
        }
    }
}
