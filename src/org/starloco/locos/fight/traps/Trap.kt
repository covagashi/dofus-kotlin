package org.starloco.locos.fight.traps

import org.starloco.locos.area.map.GameCase
import org.starloco.locos.common.PathFinding
import org.starloco.locos.common.SocketManager
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.spells.Spell.SortStats
import org.starloco.locos.kernel.Constant
import java.util.ArrayList
import java.util.stream.Collectors

class Trap(
    val fight: Fight,
    val caster: Fighter,
    val cell: GameCase,
    val size: Byte,
    val trapSpell: SortStats,
    val spell: Int
) {

    val color: Int = Constant.getTrapsColor(spell)
    private var isUnHide = true
    private var teamUnHide = -1

    fun setIsUnHide(f: Fighter) {
        this.isUnHide = true
        this.teamUnHide = f.team
    }

    fun disappear() {
        val str = StringBuilder()
        val str2 = StringBuilder()
        val str3 = StringBuilder()
        val str4 = StringBuilder()

        val team = this.caster.team + 1
        str.append("GDZ-").append(this.cell.cellId).append(";").append(this.size).append(";").append(this.color)
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this.fight, team, 999, this.caster.id.toString() + "", str.toString())
        str2.append("GDC").append(this.cell.cellId)
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this.fight, team, 999, this.caster.id.toString() + "", str2.toString())

        if (this.isUnHide) {
            val team2 = this.teamUnHide + 1
            str3.append("GDZ-").append(this.cell.cellId).append(";").append(this.size).append(";").append(this.color)
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this.fight, team2, 999, this.caster.id.toString() + "", str3.toString())
            str4.append("GDC").append(this.cell.cellId)
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this.fight, team2, 999, this.caster.id.toString() + "", str4.toString())
        }
    }

    fun appear(f: Fighter) {
        val str = StringBuilder()
        val str2 = StringBuilder()

        val team = f.team + 1
        str.append("GDZ+").append(this.cell.cellId).append(";").append(this.size).append(";").append(this.color)
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this.fight, team, 999, this.caster.id.toString() + "", str.toString())
        str2.append("GDC").append(this.cell.cellId).append(";Haaaaaaaaz3005;")
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this.fight, team, 999, this.caster.id.toString() + "", str2.toString())
    }

    fun refresh(f: Fighter) {
        val str2 = StringBuilder()
        SocketManager.GAME_SEND_GA_PACKET(f.player!!, 999, this.caster.id.toString() + "", "GDZ+" + this.cell.cellId + ";" + this.size + ";" + this.color)
        str2.append("GDC").append(this.cell.cellId).append(";Haaaaaaaaz3005;")
        SocketManager.GAME_SEND_GA_PACKET(f.player!!, 999, this.caster.id.toString() + "", str2.toString())
    }

    fun onTrapped(target: Fighter) {
        if (target.isDead)
            return
        this.fight.traps.remove(this)
        disappear()
        val str = this.spell.toString() + "," + this.cell.cellId + ",0,1,1," + this.caster.id
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this.fight, 7, 307, target.id.toString() + "", str)

        val cells = ArrayList<GameCase?>()
        cells.add(this.cell)

        for (a in 0 until this.size) {
            val dirs = charArrayOf('b', 'd', 'f', 'h')

            for (aCell in ArrayList(cells)) {
                if (aCell == null) continue
                for (d in dirs) {
                    val cell = this.fight.map!!.getCase(PathFinding.GetCaseIDFromDirection(aCell.cellId, d, this.fight.map, true))
                    if (cell != null && !cells.contains(cell))
                        cells.add(cell)
                }
            }
        }

        // Hack creating a fake Fighter that cast the trap spell in its center
        val caster: Fighter = try {
            this.caster.clone()
        } catch (e: CloneNotSupportedException) {
            throw RuntimeException(e)
        }

        caster.cell = this.cell

        val targets = cells.stream().flatMap { cell -> cell!!.fighters.stream() }.collect(Collectors.toList())

        targets.forEach { t -> t.setTrapped(true) }
        this.trapSpell.applySpellEffectToFight(this.fight, caster, target.cell!!, cells, false)
        targets.forEach { t -> t.setTrapped(false) }

        this.fight.verifIfTeamAllDead()
    }
}
