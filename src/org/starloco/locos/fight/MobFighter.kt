package org.starloco.locos.fight

import org.starloco.locos.client.other.Stats
import org.starloco.locos.entity.monster.Monster
import org.starloco.locos.entity.monster.MonsterGrade
import org.starloco.locos.fight.spells.Spell
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant

import java.util.Optional
import java.util.stream.Stream

open class MobFighter(id: Int, f: Fight, val mobGrade: MonsterGrade) : Fighter(id, f) {

    override fun getPacketsName(): String {
        return mobGrade.template!!.id.toString()
    }

    override fun getType(): Int {
        return 2
    }

    override fun getLvl(): Int {
        return mobGrade.level
    }

    override fun baseMaxPdv(): Int {
        return getBaseStats().get(Constant.STATS_ADD_VITA)
    }

    open override fun getBaseStats(): Stats {
        return mobGrade.getStats()
    }

    override fun getDefaultGfx(): Int {
        return mobGrade.template!!.gfxId
    }

    override fun spellRankForID(id: Int): Optional<Spell.SortStats> {
        return Optional.ofNullable(mobGrade.spells[id])
    }

    fun getTemplate(): Monster {
        return mobGrade.template
    }

    override fun getGMPacketParts(): Stream<String> {
        return Stream.of(
            "-2",
            mobGrade.template!!.gfxId.toString() + "^" + mobGrade.getSize(),
            mobGrade.grade.toString(),
            mobGrade.template!!.colors.replace(",", ";"),
            "0,0,0,0",
            getPdvMax().toString(),
            mobGrade.pa.toString(),
            mobGrade.pm.toString()
        )
    }

    override fun canLoot(): Boolean {
        return isInvocation() && mobGrade.template!!.id == ENUTROF_CHEST_ID
    }

    override fun minKamasReward(): Int {
        return mobGrade.template!!.minKamas
    }

    override fun maxKamasReward(): Int {
        return mobGrade.template!!.maxKamas
    }

    override fun drops(): Stream<World.Drop> {
        return mobGrade.template!!.drops.stream()
    }

    override fun scripted(): Any {
        return mobGrade.scripted() as Any
    }

    override fun getMob(): MonsterGrade {
        return mobGrade
    }

    companion object {
        const val ENUTROF_CHEST_ID = 285
    }
}
