package org.starloco.locos.fight

import org.starloco.locos.client.other.Stats
import org.starloco.locos.entity.Collector
import org.starloco.locos.fight.spells.Spell
import org.starloco.locos.`object`.GameObject

import java.util.Optional
import java.util.stream.Stream

class CollectorFighter(id: Int, f: Fight, collector: Collector) : Fighter(id, f) {

    private val collector: Collector = collector

    override fun getPacketsName(): String {
        return collector.getFullName()
    }

    override fun getType(): Int {
        return 5
    }

    override fun getLvl(): Int {
        return collector.getGuild()!!.lvl
    }

    override fun baseMaxPdv(): Int {
        return collector.getGuild()!!.lvl * 100
    }

    override fun getBaseStats(): Stats? {
        return null
    }

    override fun getDefaultGfx(): Int {
        return 6000
    }

    override fun spellRankForID(id: Int): Optional<Spell.SortStats> {
        return Optional.ofNullable(collector.getGuild()!!.spells[id])
    }

    override fun getGMPacketParts(): Stream<String> {
        val lvl = getLvl()
        val resistance = Math.min(50, Math.floor(lvl.toDouble() / 2).toInt())

        return Stream.of(
            "-6",
            "6000^100",
            lvl.toString(),
            "1",
            "2",
            "4",
            resistance.toString(),
            resistance.toString(),
            resistance.toString(),
            resistance.toString(),
            resistance.toString(),
            resistance.toString(),
            resistance.toString()
        )
    }

    fun collectorDrops(): Collection<GameObject> {
        return collector.getDrops()
    }

    override fun getCollector(): Collector {
        return collector
    }
}
