package org.starloco.locos.fight.traps

import org.starloco.locos.area.map.GameCase
import org.starloco.locos.common.SocketManager
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.spells.Spell
import org.starloco.locos.fight.spells.Spell.SortStats
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant

class Glyph(
    val fight: Fight,
    val caster: Fighter,
    val cell: GameCase,
    val size: Byte,
    val trapSpell: SortStats,
    duration: Byte,
    val spell: Int
) {

    var duration: Byte = duration
    val color: Int = Constant.getGlyphColor(spell)

    fun decrementDuration(): Int {
        //if(this.duration == -1) return -1;
        this.duration--
        return this.duration.toInt()
    }

    fun onTrapped(target: Fighter) {
        if (this.spell == 1072 || this.spell == 1073) {//glyph pair/impair
            if (target.mob != null) {
                if (target.mob!!.template!!.id == 1045) {
                    if (this.spell == 1072) {
                        target.addBuff(217, 400, 2, false, 1077, "", target, false, true)// - 400 air
                        target.addBuff(218, 400, 2, false, 1077, "", target, false, true)// - 400 feu
                        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 1077, caster.id.toString() + "", target.id.toString() + "," + "" + "," + 1)
                        this.fight.getFighters(7).stream().filter { it.player!! != null && it.player!!.isOnline }.forEach {
                            it.player!!.send("GA;217;-100;" + target.id + ",400,1")
                            it.player!!.send("GA;218;-100;" + target.id + ",400,1")
                        }
                    } else {
                        target.addBuff(215, 400, 2, false, 1077, "", target, false, true)// - 400 terre
                        target.addBuff(216, 400, 2, false, 1077, "", target, false, true)// - 400 eau

                        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 1077, caster.id.toString() + "", target.id.toString() + "," + "" + "," + 1)

                        this.fight.getFighters(7).stream().filter { it.player!! != null && it.player!!.isOnline }.forEach {
                            it.player!!.send("GA;216;-100;" + target.id + ",400,1")
                            it.player!!.send("GA;215;-100;" + target.id + ",400,1")
                        }
                    }
                } else {
                    this.fight.onFighterDie(target, target)
                }
            } else {
                fight.onFighterDie(target, target)
            }
        } else {
            val spell = World.world.getSort(this.spell)

            for (integer in spell!!.effectTargets)
                if (integer == 2 && target === this.caster)
                    return

            val str = this.spell.toString() + "," + this.cell.cellId + ", 0, 1, 1," + this.caster.id
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this.fight, 7, 307, target.id.toString() + "", str)
            target.setGlyphed(true)
            this.trapSpell.applySpellEffectToFight(this.fight, this.caster, target.cell!!, false, true)
            this.fight.verifIfTeamAllDead()
            target.setGlyphed(false)
        }
    }

    fun disappear() {
        SocketManager.GAME_SEND_GDZ_PACKET_TO_FIGHT(this.fight, 7, "-", this.cell.cellId, this.size.toInt(), this.color)
        SocketManager.GAME_SEND_GDC_PACKET_TO_FIGHT(this.fight, 7, this.cell.cellId)
    }
}
