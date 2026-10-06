package org.starloco.locos.fight.ia.type.boss

import org.starloco.locos.common.SocketManager
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractIA
import org.starloco.locos.fight.ia.util.Function

/**
 * Created by Locos on 04/10/2015.
 */
class IA18(fight: Fight, fighter: Fighter, count: Byte) : AbstractIA(fight, fighter, count) {

    private var pair = false
    private var impair = false
    private var ok = false

    override fun apply() {
        if (!this.stop && this.fighter.canPlay() && this.count > 0) {
            val kimbo = this.findKimbo()
            var time = 0

            if (this.ok && this.fighter.getCurPm(this.fight) > 0) {
                if (Function.getInstance().moveNearIfPossible(this.fight, this.fighter, kimbo!!)) {
                    this.stop = true
                    time = 1500
                }
            } else {
                if (this.pair || this.impair) {
                    if (this.pair) {
                        this.attackGlyph(this.fighter, 1072)
                    } else {
                        this.attackGlyph(this.fighter, 1073)
                    }
                }
            }

            this.addNext({ this.decrementCount() }, time)
        } else {
            this.stop = true
        }
    }

    private fun findKimbo(): Fighter? {
        val id = this.fight.getTeamId(this.fighter.id)
        val fighters = this.fight.getTeam(id).values

        for (fighter in fighters) {
            if (fighter.getMob() != null) {
                if (fighter.getMob()!!.template!!.id == 1045) {
                    if (fighter.haveState(30)) {
                        fighter.setState(30, 0)
                        this.pair = true
                        this.fighter.setState(30, 1)
                    }
                    if (fighter.haveState(29)) {
                        fighter.setState(29, 0)
                        this.impair = true
                        this.fighter.setState(29, 1)
                    }
                    return fighter
                }
            }
        }
        return null
    }

    fun attackGlyph(target: Fighter?, id: Int) {
        if (target == null)
            return
        val spell = Function.getInstance().findSpell(this.fighter, id)
        val attack = fight.tryCastSpell(fighter, spell!!, target.cell!!.cellId)

        if (attack != 0) {
            this.ok = true
            this.fight.glyphs.stream().filter { entry -> entry.cell!!.cellId == this.fighter.cell!!.cellId }.forEach { _ ->
                this.fighter.addBuff(128, 1, 1, true, 1072, "", this.fighter, false, true)
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this.fight, 7, 78, this.fighter.id.toString() + "", this.fighter.id.toString() + "," + "" + "," + 1)
            }
        }
    }
}
