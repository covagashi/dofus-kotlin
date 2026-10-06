package org.starloco.locos.client.other

import org.starloco.locos.client.Player
import org.starloco.locos.guild.Guild
import org.starloco.locos.kernel.Constant
import java.util.HashMap
import java.util.TreeMap

class Stats(val effects: MutableMap<Int, Int>) {

    constructor(addBases: Boolean, player: Player?) : this(HashMap()) {
        if (addBases) {
            this.effects[Constant.STATS_ADD_PA] = if (player!!.level < 100) 6 else 7
            this.effects[Constant.STATS_ADD_PM] = 3
            this.effects[Constant.STATS_ADD_PROS] = if (player.classe == Constant.CLASS_ENUTROF) 120 else 100
            this.effects[Constant.STATS_ADD_PODS] = 1000
            this.effects[Constant.STATS_SUMMON_COUNT] = 1
            this.effects[Constant.STATS_ADD_INIT] = 1
        }
    }

    constructor(stats: MutableMap<Int, Int>, addBases: Boolean, player: Player?) : this(stats) {
        if (addBases) {
            this.effects[Constant.STATS_ADD_PA] = if (player!!.level < 100) 6 else 7
            this.effects[Constant.STATS_ADD_PM] = 3
            this.effects[Constant.STATS_ADD_PROS] = if (player.classe == Constant.CLASS_ENUTROF) 120 else 100
            this.effects[Constant.STATS_ADD_PODS] = 1000
            this.effects[Constant.STATS_SUMMON_COUNT] = 1
            this.effects[Constant.STATS_ADD_INIT] = 1
        }
    }

    constructor(a: Boolean) : this(HashMap()) { // Parchotage
        this.effects[Constant.STATS_ADD_VITA] = 0
        this.effects[Constant.STATS_ADD_SAGE] = 0
        this.effects[Constant.STATS_ADD_INTE] = 0
        this.effects[Constant.STATS_ADD_FORC] = 0
        this.effects[Constant.STATS_ADD_CHAN] = 0
        this.effects[Constant.STATS_ADD_AGIL] = 0
    }

    constructor() : this(TreeMap())

    constructor(guild: Guild?) : this(HashMap()) { // Stats collector in fight
        if (guild != null) {
            this.effects[Constant.STATS_ADD_SAGE] = guild.getStats(Constant.STATS_ADD_SAGE)
            this.effects[Constant.STATS_ADD_FORC] = guild.lvl
            this.effects[Constant.STATS_ADD_INTE] = guild.lvl
            this.effects[Constant.STATS_ADD_CHAN] = guild.lvl
            this.effects[Constant.STATS_ADD_AGIL] = guild.lvl
            val floor = Math.floor((guild.lvl / 2).toDouble()).toInt()
            this.effects[Constant.STATS_ADD_RP_NEU] = floor
            this.effects[Constant.STATS_ADD_RP_FEU] = floor
            this.effects[Constant.STATS_ADD_RP_EAU] = floor
            this.effects[Constant.STATS_ADD_RP_AIR] = floor
            this.effects[Constant.STATS_ADD_RP_TER] = floor
            this.effects[Constant.STATS_ADD_ADODGE] = floor
            this.effects[Constant.STATS_ADD_MDODGE] = floor
        }
    }

    operator fun get(id: Int): Int = this.effects.getOrDefault(id, 0)

    fun addOneStat(id: Int, `val`: Int): Int {
        var id = id
        if (id == 112) id = Constant.STATS_ADD_DOMA
        val current = this.effects[id]
        if (current == null || current == 0) {
            if (`val` <= 0) return 0
            this.effects[id] = `val`
        } else {
            val newVal = current + `val`
            if (newVal <= 0) {
                this.effects.remove(id)
                return 0
            } else
                this.effects[id] = newVal
        }
        return this.effects[id]!!
    }

    fun isSameStats(other: Stats): Boolean {
        for (entry in this.effects.entries) {
            //Si la stat n'existe pas dans l'autre map
            if (other.effects[entry.key] == null)
                return false
            //Si la stat existe mais n'a pas la même valeur
            if (other.effects[entry.key]!!.compareTo(entry.value) != 0)
                return false
        }
        for (entry in other.effects.entries) {
            //Si la stat n'existe pas dans l'autre map
            if (this.effects[entry.key] == null)
                return false
            //Si la stat existe mais n'a pas la même valeur
            if (this.effects[entry.key]!!.compareTo(entry.value) != 0)
                return false
        }
        return true
    }

    fun encodeItemSetStats(): String {
        val str = StringBuilder()
        if (this.effects.isEmpty())
            return ""
        for (entry in this.effects.entries) {
            if (str.isNotEmpty())
                str.append(",")
            str.append(entry.key.toString(16)).append("#").append(entry.value.toString(16)).append("#0#0")
        }
        return str.toString()
    }

    fun getEffect(id: Int): Int {
        var v = this.effects[id] ?: 0

        when (id) {
            Constant.STATS_ADD_ADODGE -> {
                if (this.effects[Constant.STATS_REM_AFLEE] != null)
                    v -= getEffect(Constant.STATS_REM_AFLEE)
                if (this.effects[Constant.STATS_ADD_SAGE] != null)
                    v += getEffect(Constant.STATS_ADD_SAGE) / 4
            }
            Constant.STATS_ADD_MDODGE -> {
                if (this.effects[Constant.STATS_REM_MFLEE] != null)
                    v -= getEffect(Constant.STATS_REM_MFLEE)
                if (this.effects[Constant.STATS_ADD_SAGE] != null)
                    v += getEffect(Constant.STATS_ADD_SAGE) / 4
            }
            Constant.STATS_ADD_INIT -> if (this.effects[Constant.STATS_REM_INIT] != null)
                v -= this.effects[Constant.STATS_REM_INIT]!!
            Constant.STATS_ADD_AGIL -> if (this.effects[Constant.STATS_REM_AGIL] != null)
                v -= this.effects[Constant.STATS_REM_AGIL]!!
            Constant.STATS_ADD_FORC -> if (this.effects[Constant.STATS_REM_FORC] != null)
                v -= this.effects[Constant.STATS_REM_FORC]!!
            Constant.STATS_ADD_CHAN -> if (this.effects[Constant.STATS_REM_CHAN] != null)
                v -= this.effects[Constant.STATS_REM_CHAN]!!
            Constant.STATS_ADD_INTE -> if (this.effects[Constant.STATS_REM_INTE] != null)
                v -= this.effects[Constant.STATS_REM_INTE]!!
            Constant.STATS_ADD_PA -> {
                if (this.effects[Constant.STATS_ADD_PA2] != null)
                    v += this.effects[Constant.STATS_ADD_PA2]!!
                if (this.effects[Constant.STATS_REM_PA] != null)
                    v -= this.effects[Constant.STATS_REM_PA]!!
                if (this.effects[Constant.STATS_REM_PA2] != null)//Non esquivable
                    v -= this.effects[Constant.STATS_REM_PA2]!!
            }
            Constant.STATS_ADD_PM -> {
                if (this.effects[Constant.STATS_ADD_PM2] != null)
                    v += this.effects[Constant.STATS_ADD_PM2]!!
                if (this.effects[Constant.STATS_REM_PM] != null)
                    v -= this.effects[Constant.STATS_REM_PM]!!
                if (this.effects[Constant.STATS_REM_PM2] != null)//Non esquivable
                    v -= this.effects[Constant.STATS_REM_PM2]!!
            }
            Constant.STATS_ADD_PO -> if (this.effects[Constant.STATS_REM_PO] != null)
                v -= this.effects[Constant.STATS_REM_PO]!!
            Constant.STATS_ADD_VITA -> if (this.effects[Constant.STATS_REM_VITA] != null)
                v -= this.effects[Constant.STATS_REM_VITA]!!
            Constant.STATS_ADD_VIE -> v = Constant.STATS_ADD_VIE
            Constant.STATS_ADD_DOMA -> if (this.effects[Constant.STATS_REM_DOMA] != null)
                v -= this.effects[Constant.STATS_REM_DOMA]!!
            Constant.STATS_ADD_PODS -> if (this.effects[Constant.STATS_REM_PODS] != null)
                v -= this.effects[Constant.STATS_REM_PODS]!!
            Constant.STATS_ADD_PROS -> if (this.effects[Constant.STATS_REM_PROS] != null)
                v -= this.effects[Constant.STATS_REM_PROS]!!
            Constant.STATS_ADD_R_TER -> if (this.effects[Constant.STATS_REM_R_TER] != null)
                v -= this.effects[Constant.STATS_REM_R_TER]!!
            Constant.STATS_ADD_R_EAU -> if (this.effects[Constant.STATS_REM_R_EAU] != null)
                v -= this.effects[Constant.STATS_REM_R_EAU]!!
            Constant.STATS_ADD_R_AIR -> if (this.effects[Constant.STATS_REM_R_AIR] != null)
                v -= this.effects[Constant.STATS_REM_R_AIR]!!
            Constant.STATS_ADD_R_FEU -> if (this.effects[Constant.STATS_REM_R_FEU] != null)
                v -= this.effects[Constant.STATS_REM_R_FEU]!!
            Constant.STATS_ADD_R_NEU -> if (this.effects[Constant.STATS_REM_R_NEU] != null)
                v -= this.effects[Constant.STATS_REM_R_NEU]!!
            Constant.STATS_ADD_RP_TER -> if (this.effects[Constant.STATS_REM_RP_TER] != null)
                v -= this.effects[Constant.STATS_REM_RP_TER]!!
            Constant.STATS_ADD_RP_EAU -> if (this.effects[Constant.STATS_REM_RP_EAU] != null)
                v -= this.effects[Constant.STATS_REM_RP_EAU]!!
            Constant.STATS_ADD_RP_AIR -> if (this.effects[Constant.STATS_REM_RP_AIR] != null)
                v -= this.effects[Constant.STATS_REM_RP_AIR]!!
            Constant.STATS_ADD_RP_FEU -> if (this.effects[Constant.STATS_REM_RP_FEU] != null)
                v -= this.effects[Constant.STATS_REM_RP_FEU]!!
            Constant.STATS_ADD_RP_NEU -> if (this.effects[Constant.STATS_REM_RP_NEU] != null)
                v -= this.effects[Constant.STATS_REM_RP_NEU]!!
            Constant.STATS_ADD_MAITRISE -> if (this.effects[Constant.STATS_ADD_MAITRISE] != null)
                v = this.effects[Constant.STATS_ADD_MAITRISE]!!
        }
        return v
    }

    companion object {
        @JvmStatic
        fun cumulStat(s1: Stats, s2: Stats): Stats {
            val effets = HashMap<Int, Int>()
            for (a in 0..Constant.MAX_EFFECTS_ID) {
                if (s1.effects[a] == null && s2.effects[a] == null)
                    continue

                var som = 0
                if (s1.effects[a] != null)
                    som += s1.effects[a]!!
                if (s2.effects[a] != null)
                    som += s2.effects[a]!!

                effets[a] = som
            }
            return Stats(effets, false, null)
        }

        @JvmStatic
        fun cumulStatFight(s1: Stats, s2: Stats): Stats {
            val effets = HashMap<Int, Int>()
            for (a in 0..Constant.MAX_EFFECTS_ID) {
                if ((s1.effects[a] == null || s1.effects[a] == 0)
                    && (s2.effects[a] == null || s2.effects[a] == 0))
                    continue
                var som = 0
                if (s1.effects[a] != null)
                    som += s1.effects[a]!!
                if (s2.effects[a] != null)
                    som += s2.effects[a]!!
                effets[a] = som
            }
            return Stats(effets, false, null)
        }
    }
}
