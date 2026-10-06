package org.starloco.locos.fight.ia.type.invocations.dopeuls

import org.starloco.locos.common.Formulas
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractEasyIA

/**
 * Created by Locos on 09/04/2018.
 */
class Sram(fight: Fight, fighter: Fighter, count: Byte) : AbstractEasyIA(fight, fighter, count) {

    override fun run() {
        when (this.flag.toInt()) {
            0 -> if (get().tryCastSpell(fight, fighter, fighter, 62) == 0) // Concentration de chakra
                this.time = 1500

            1 -> { // Déplacement
                val target = get().getNearestEnnemy(fight, fighter, true)
                if (get().moveNearIfPossible(fight, fighter, target!!))
                    this.time = 1500
            }
            2 -> { // Fourvoiement
                val cells = get().getCellsAvailableAround(fighter, true, 0.toByte())
                var ok = false
                for (cell in cells) {
                    val first = cell.firstFighter
                    if (first != null && first.team != fighter.team)
                        ok = true
                }
                if (ok) {
                    if (get().tryCastSpell(fight, fighter, fighter, 68) == 0)
                        this.setNextParams(1, 3, 2000)
                } else {
                    if (fighter.getCurPm(fight) == 0) this.time = 500
                    else this.setNextParams(0, 4, 200)
                }
            }
            3 -> { // Piège sournois
                val target = get().getNearestEnnemy(fight, fighter, true)
                val cells = get().getCellsAvailableAround(target!!, false, 1.toByte())
                val cellsCaster = get().getCellsAvailableAround(fighter, true, 0.toByte())
                cells.removeIf { c -> cellsCaster.contains(c) }
                cells.remove(target!!.cell!!)
                if (!cells.isEmpty()) {
                    val cell = cells[Formulas.getRandomValue(0, cells.size - 1)]
                    if (cell != null && fight.tryCastSpell(fighter, get().findSpell(fighter, 65)!!, cell.cellId) == 0)
                        this.setNextParams(2, 2, 2500)
                    else {
                        get().moveFarIfPossible(fight, fighter)
                        time = 2000
                    }
                } else {
                    get().moveFarIfPossible(fight, fighter)
                    time = 2000
                }
            }
        }
    }
}
