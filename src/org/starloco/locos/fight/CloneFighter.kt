package org.starloco.locos.fight

import org.starloco.locos.client.other.Stats
import org.starloco.locos.entity.mount.Mount
import org.starloco.locos.fight.spells.Spell
import java.util.Optional
import java.util.stream.Stream

class CloneFighter(id: Int, f: Fight, @JvmField val summoner: PlayerFighter) : Fighter(id, f) {

    override fun getType(): Int = 10

    override fun getLvl(): Int = summoner.getLvl()

    override fun baseMaxPdv(): Int = summoner.baseMaxPdv()

    override fun getBaseStats(): Stats = summoner.getBaseStats()

    override fun getDefaultGfx(): Int = summoner.getDefaultGfx()

    override fun spellRankForID(id: Int): Optional<Spell.SortStats> = Optional.empty()

    override fun getPacketsName(): String = summoner.getPacketsName()

    override fun getGMPacketParts(): Stream<String> = summoner.getGMPacketParts()

    override fun getMount(): Optional<Mount> = summoner.getMount()

    override fun getMountColors(): String? = summoner.getMountColors()

    override fun isInvocation(): Boolean = true

    override fun getInvocator(): Fighter = summoner
}
