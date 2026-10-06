package org.starloco.locos.job

import org.starloco.locos.client.Player
import org.starloco.locos.common.SocketManager
import org.starloco.locos.game.world.World

class JobStat(val id: Int, val template: Job, lvl: Int, xp: Long) {

    var lvl: Int = lvl
        private set
    var xp: Long = xp
        private set
    private var posActions = ArrayList<JobAction>()
    private var isCheap = false
    private var freeOnFails = false
    private var noRessource = false
    private var curAction: JobAction? = null
    var slotsPublic = 0
    var position = 0

    init {
        this.posActions = JobConstant.getPosActionsToJob(template.id, lvl)
    }

    fun get_lvl(): Int = this.lvl

    fun getJobActionBySkill(skill: Int): JobAction? {
        for (JA in this.posActions)
            if (JA.id == skill)
                return JA
        return null
    }

    fun addXp(P: Player, xp: Long) {
        if (xp < 0) throw IllegalArgumentException("xp must be positive")
        if (this.lvl > 99)
            return
        val exLvl = this.lvl
        this.xp += xp

        val xpTable = World.world.experiences!!.jobs
        while (this.xp >= xpTable.maxXpAt(this.lvl) && this.lvl < xpTable.maxLevel())
            levelUp(P, false)

        if (this.lvl > exLvl && P.isOnline) {
            val list = ArrayList<JobStat>()
            list.add(this)

            SocketManager.GAME_SEND_JS_PACKET(P, list)
            SocketManager.GAME_SEND_JN_PACKET(P, this.template!!.id, this.lvl)
            SocketManager.GAME_SEND_STATS_PACKET(P)
            SocketManager.GAME_SEND_Ow_PACKET(P)
            SocketManager.GAME_SEND_JO_PACKET(P, list)
        }
    }

    fun getXpString(s: String): String {
        val xpTable = World.world.experiences!!.jobs
        return xpTable.minXpAt(this.lvl).toString() + s + this.xp + s + xpTable.maxXpAt(this.lvl)
    }

    fun levelUp(P: Player, send: Boolean) {
        this.lvl++
        this.posActions = JobConstant.getPosActionsToJob(this.template!!.id, this.lvl)

        if (send) {
            //on creer la listes des JobStats a envoyer (Seulement celle ci)
            val list = listOf(this)
            SocketManager.GAME_SEND_JS_PACKET(P, list)
            SocketManager.GAME_SEND_STATS_PACKET(P)
            SocketManager.GAME_SEND_Ow_PACKET(P)
            SocketManager.GAME_SEND_JN_PACKET(P, this.template!!.id, this.lvl)
            SocketManager.GAME_SEND_JO_PACKET(P, list)
        }
    }

    fun parseJS(): String {
        val str = StringBuilder()
        str.append("|").append(this.template!!.id).append(";")
        var first = true
        for (JA in this.posActions) {
            if (!first)
                str.append(",")
            else
                first = false
            str.append(JA.id).append("~").append(JA.min).append("~")
            if (JA.isCraft)
                str.append("0~0~").append(JA.chance)
            else
                str.append(JA.max).append("~0~").append(JA.time)
        }
        return str.toString()
    }

    fun getOptBinValue(): Int {
        var nbr = 0
        nbr += if (this.isCheap) 1 else 0
        nbr += if (this.freeOnFails) 2 else 0
        nbr += if (this.noRessource) 4 else 0
        return nbr
    }

    fun setOptBinValue(bin: Int) {
        this.isCheap = false
        this.freeOnFails = false
        this.noRessource = false
        this.noRessource = bin and 4 == 4
        this.freeOnFails = bin and 2 == 2
        this.isCheap = bin and 1 == 1
    }

    fun isValidMapAction(id: Int): Boolean {
        for (JA in this.posActions)
            if (JA.id == id)
                return true
        return false
    }
}
