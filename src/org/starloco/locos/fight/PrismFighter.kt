package org.starloco.locos.fight

import org.starloco.locos.client.other.Stats
import org.starloco.locos.entity.Prism
import org.starloco.locos.fight.spells.Spell
import org.starloco.locos.kernel.Constant

import java.util.Optional
import java.util.stream.Stream

class PrismFighter(id: Int, fight: Fight, prism: Prism) : Fighter(id, fight) {

    override val prism: Prism = prism

    override fun getPacketsName(): String {
        return (if (prism.alignment == 1) 1111 else 1112).toString()
    }

    override fun getGMPacketParts(): Stream<String> {
        return Stream.of(
            "-2",
            prism.gfx.toString() + "^100",
            prism.level.toString(),
            "-1;-1;-1",
            "0,0,0,0",
            this.getPdvMax().toString(),
            getTotalStats().getEffect(Constant.STATS_ADD_PA).toString(),
            getTotalStats().getEffect(Constant.STATS_ADD_PM).toString(),
            getTotalStats().getEffect(Constant.STATS_ADD_RP_NEU).toString(),
            getTotalStats().getEffect(Constant.STATS_ADD_RP_TER).toString(),
            getTotalStats().getEffect(Constant.STATS_ADD_RP_FEU).toString(),
            getTotalStats().getEffect(Constant.STATS_ADD_RP_EAU).toString(),
            getTotalStats().getEffect(Constant.STATS_ADD_RP_AIR).toString(),
            getTotalStats().getEffect(Constant.STATS_ADD_ADODGE).toString(),
            getTotalStats().getEffect(Constant.STATS_ADD_MDODGE).toString()
        )
    }

    override fun getType(): Int {
        return 7
    }

    override fun getLvl(): Int {
        return prism.level
    }

    override fun baseMaxPdv(): Int {
        return prism.level * 10000
    }

    override fun getBaseStats(): Stats {
        return prism.getStats()
    }

    override fun getDefaultGfx(): Int {
        return prism.gfx
    }

    override fun spellRankForID(id: Int): Optional<Spell.SortStats> {
        return Optional.empty()
    }

}
