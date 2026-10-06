package org.starloco.locos.fight

import org.starloco.locos.client.Player
import org.starloco.locos.client.other.Stats
import org.starloco.locos.common.SocketManager
import org.starloco.locos.entity.mount.Mount
import org.starloco.locos.fight.spells.Spell
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant

import java.util.Optional
import java.util.stream.Stream

class PlayerFighter(f: Fight, player: Player) : Fighter(player.id, f) {

    override val player: Player = player


    override fun getPacketsName(): String {
        return player.name
    }

    override fun getType(): Int {
        return 1
    }

    override fun getLvl(): Int {
        return player.level
    }

    override fun baseMaxPdv(): Int {
        return player.maxPdv
    }

    public override fun getBaseStats(): Stats {
        return player.getTotalStats(true)
    }

    override fun getDefaultGfx(): Int {
        return player.gfxId
    }

    fun sendStats() {
        SocketManager.GAME_SEND_STATS_PACKET(this.player)
    }

    override fun initFightBuffs() {
        this.fightBuffs.addAll(this.player.buffs.values)
    }

    override fun send(pck: String) {
        if (!player.isOnline) return
        player.send(pck)
    }

    override fun spellRankForID(id: Int): Optional<Spell.SortStats> {
        return player.getSpells().stream().filter { s -> s.spellID == id }.findFirst()
    }

    override fun criticalStrikeModifier(baseCC: Int, spellID: Int): Int {
        var porcCC = baseCC
        var agi = getTotalStats().getEffect(Constant.STATS_ADD_AGIL)
        if (agi < 0)
            agi = 0
        porcCC -= getTotalStats().getEffect(Constant.STATS_ADD_CC)
        if (spellID in player.objectsClassSpell) {
            val modi = player.getValueOfClassObject(spellID, 287)
            porcCC -= modi
        }
        porcCC = ((porcCC * 2.9901) / Math.log(agi + 12.0)).toInt()
        return Math.max(2, porcCC)
    }

    override fun xpString(separator: String): String {
        val xpTable = World.world.experiences!!.players

        return (xpTable.minXpAt(this.player.level).toString() + separator
                + this.player.exp + separator + xpTable.maxXpAt(this.player.level))
    }

    public override fun getGMPacketParts(): Stream<String> {
        val factionParts = mutableListOf(
            player.alignment.toString(),
            "0",
            if (player.showWings) player.grade.toString() else "0",
            (player.level + player.id).toString() // WTF ?
        )

        if (player.showWings && player.deshonor > 0) {
            factionParts.add((if (player.deshonor > 0) 1 else 0).toString())
        }

        val colors = player.getColors()

        return Stream.of(
            player.classe.toString(),
            player.gfxId.toString() + "^" + player.size,
            player.sexe.toString(),
            player.level.toString(),
            factionParts.joinToString(","),
            if (colors[0] == -1) "-1" else Integer.toHexString(colors[0]),
            if (colors[1] == -1) "-1" else Integer.toHexString(colors[1]),
            if (colors[2] == -1) "-1" else Integer.toHexString(colors[2]),
            player.getGMStuffString(),
            getPdv().toString(),
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

    public override fun getMount(): Optional<Mount> {
        return Optional.ofNullable(player.mount)
    }

    override fun getColors(): IntArray {
        return intArrayOf(player.color1, player.color2, player.color3)
    }

    public override fun getMountColors(): String? {
        return player.encodeColorsForMount()
    }

    override fun aiControlled(): Boolean {
        return false
    }

    override fun canLoot(): Boolean {
        return true
    }

    override fun scripted(): Any {
        return player.scripted()
    }

}
