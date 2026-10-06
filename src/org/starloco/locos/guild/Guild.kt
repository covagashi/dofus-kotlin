package org.starloco.locos.guild

import org.starloco.locos.entity.map.House
import org.starloco.locos.client.Player
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.game.GuildMemberData
import org.starloco.locos.database.data.game.HouseData
import org.starloco.locos.database.data.login.GuildData
import org.starloco.locos.entity.Collector
import org.starloco.locos.fight.spells.Spell.SortStats
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant
import java.util.ArrayList
import java.util.HashMap
import java.util.TreeMap

class Guild {

    var id: Int = 0
    var xp: Long = 0
        private set
    val date: Long
    var name: String = ""
    var emblem: String = ""
        private set
    var lvl: Int = 0
        private set
    var capital = 0
    var nbCollectors = 0
    private val members = TreeMap<Int, GuildMember>()
    val spells: MutableMap<Int, SortStats?> = HashMap() // <Id, Level>
    private val stats: MutableMap<Int, Int> = HashMap() // <Effect, Quantity>

    constructor(name: String, emblem: String) {
        this.name = name
        this.emblem = emblem
        this.lvl = 1
        this.xp = 0
        this.date = System.currentTimeMillis()
        this.decompileSpell("462;0|461;0|460;0|459;0|458;0|457;0|456;0|455;0|454;0|453;0|452;0|451;0|")
        this.decompileStats("176;100|158;1000|124;0|")
        (DatabaseManager.get(GuildData::class.java) as GuildData).insert(this)
    }

    constructor(id: Int, name: String, emblem: String, lvl: Int, xp: Long, capital: Int, nbCollectors: Int, sorts: String, stats: String, date: Long) {
        this.id = id
        this.name = name
        this.emblem = emblem
        this.xp = xp
        this.lvl = lvl
        this.capital = capital
        this.nbCollectors = nbCollectors
        this.date = date
        this.decompileSpell(sorts)
        this.decompileStats(stats)
    }

    fun addMember(id: Int, r: Int, pXp: Byte, x: Long, ri: Int, lastCo: String) {
        val guildMember = GuildMember(id, this, r, x, pXp, ri, lastCo)
        this.members[id] = guildMember
        guildMember.player?.guildMember = guildMember
    }

    fun addNewMember(player: Player): GuildMember {
        val guildMember = GuildMember(player.id, this, 0, 0, 0.toByte(), 0, player.account.lastConnectionDate)
        this.members[player.id] = guildMember
        guildMember.player!!.guildMember = guildMember
        return guildMember
    }

    fun getMembers(): Map<Int, GuildMember> = this.members

    fun boostSpell(id: Int) {
        val ss = this.spells[id]
        if (ss != null && ss.level == 5)
            return
        this.spells[id] = if (ss == null) World.world.getSort(id)!!.getStatsByLevel(1) else World.world.getSort(id)!!.getStatsByLevel(ss.level + 1)
    }

    fun unBoostSpell(id: Int) {
        val ss = this.spells[id]
        if (ss != null) {
            this.capital += 5 * ss.level
            this.spells[id] = null
        }
    }

    fun haveTenMembers(): Boolean = this.id == 1 || this.id == 2 || this.members.size >= 10

    fun getPlayers(): List<Player> {
        //return this.members.stream().filter(guildMember -> guildMember.player != null).map(GuildMember::getPlayer).collect(Collectors.toList());
        val a = ArrayList<Player>()
        for (gm in this.members.values)
            gm.player?.let { a.add(it) }
        return a
    }

    fun getMember(id: Int): GuildMember? {
        for (guildMember in this.members.values)
            if (guildMember.playerId == id)
                return guildMember
        return null
    }

    fun removeMember(player: Player) {
        val house = World.world.houseManager.getHouseByPerso(player)
        if (house != null)
            if (World.world.houseManager.houseOnGuild(this.id) > 0)
                (DatabaseManager.get(HouseData::class.java) as HouseData).updateGuild(house, 0, 0)
        this.members.remove(player.id)
        (DatabaseManager.get(GuildMemberData::class.java) as GuildMemberData).delete(player)
    }

    fun addXp(xp: Long) {
        this.xp += xp
        while (this.xp >= World.world.getGuildXpMax(this.lvl) && this.lvl < 200) this.levelUp()
    }

    private fun levelUp() {
        this.lvl++
        this.capital += 5
    }

    private fun decompileSpell(spells: String) {
        for (split in spells.split("|".toRegex()))
            this.spells[(split.split(";")[0]).toInt()] = World.world.getSort((split.split(";")[0]).toInt())!!.getStatsByLevel((split.split(";")[1]).toInt())
    }

    fun compileSpell(): String {
        if (this.spells.isEmpty())
            return ""

        val toReturn = StringBuilder()
        var isFirst = true

        for (curSpell in this.spells.entries) {
            if (!isFirst)
                toReturn.append("|")
            toReturn.append(curSpell.key).append(";").append(if (curSpell.value == null) 0 else curSpell.value!!.level)
            isFirst = false
        }

        return toReturn.toString()
    }

    private fun decompileStats(statsStr: String) {
        for (split in statsStr.split("|".toRegex()))
            this.stats[(split.split(";")[0]).toInt()] = (split.split(";")[1]).toInt()
    }

    fun compileStats(): String {
        if (this.stats.isEmpty())
            return ""

        val toReturn = StringBuilder()
        var isFirst = true

        for (curStats in this.stats.entries) {
            if (!isFirst)
                toReturn.append("|")

            toReturn.append(curStats.key).append(";").append(curStats.value)

            isFirst = false
        }

        return toReturn.toString()
    }

    fun upgradeStats(id: Int, add: Int) {
        this.stats[id] = this.stats[id]!! + add
    }

    fun resetStats(id: Int): Int {
        val quantity = this.stats[id]!!
        this.stats[id] = 0
        return quantity
    }

    fun getStats(id: Int): Int = stats[id]!!

    fun getStats(): Map<Int, Int> = stats

    //region Parse packet
    fun parseCollectorToGuild(): String {
        return nbCollectors.toString() + "|" + Collector.countCollectorGuild(id) + "|" + 100 * lvl + "|" + lvl + "|" + getStats(158) + "|" + getStats(176) + "|" + getStats(124) + "|" + nbCollectors + "|" + capital + "|" + (1000 + (10 * lvl)) + "|" + compileSpell()
    }

    fun encodeTaxCollectorDQ(): String {
        return java.lang.String.join(",", "DQ;$name", getStats(Constant.STATS_ADD_PODS).toString(), getStats(Constant.STATS_ADD_PROS).toString(), getStats(Constant.STATS_ADD_SAGE).toString(), nbCollectors.toString())
    }

    fun parseMembersToGM(): String {
        val str = StringBuilder()
        for (gm in this.members.values) {
            var online = "0"
            if (gm.player != null)
                if (gm.player!!.isOnline)
                    online = "1"
            if (str.isNotEmpty())
                str.append("|")
            str.append(gm.playerId).append(";")
            str.append(gm.name).append(";")
            str.append(gm.lvl).append(";")
            str.append(gm.gfx).append(";")
            str.append(gm.rank).append(";")
            str.append(gm.xpGave).append(";")
            str.append(gm.getXpGive()).append(";")
            str.append(gm.rights).append(";")
            str.append(online).append(";")
            str.append(gm.align).append(";")
            str.append(gm.hoursFromLastCo)
        }
        return str.toString()
    }
    //endregion
}
