package org.starloco.locos.guild

import org.joda.time.Days
import org.joda.time.LocalDate
import org.starloco.locos.client.Player
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.game.GuildMemberData
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant
import java.util.TreeMap

/**
 * Created by Locos on 31/01/2017
 **/
class GuildMember internal constructor(
    val playerId: Int,
    val guild: Guild,
    rank: Int,
    xpGave: Long,
    xpGive: Byte,
    rights: Int,
    lastCo: String
) {

    val player: Player?
        get() = World.world.getPlayer(playerId)

    val name: String
        get() = player!!.name

    val align: Int
        get() = player!!.alignment

    val gfx: Int
        get() = player!!.gfxId

    val lvl: Int
        get() = player!!.level

    var rank: Int = rank
    var xpGive: Byte = xpGive
    var xpGave: Long = xpGave
        private set
    var rights: Int = rights
        private set
    var lastCo: String = lastCo
    private val haveRights = TreeMap<Int, Boolean>()

    init {
        this.parseIntToRight(this.rights)
    }

    fun getXpGive(): Int = xpGive.toInt()

    fun giveXpToGuild(xp: Long) {
        this.xpGave += xp
        this.guild.addXp(xp)
    }

    fun parseRights(): String = this.rights.toString(36)

    internal val hoursFromLastCo: Int
        get() {
            val split = this.lastCo.split("~")
            val localDate = LocalDate((split[0]).toInt(), (split[1]).toInt(), (split[2]).toInt())
            return Days.daysBetween(localDate, LocalDate()).days * 24
        }

    fun canDo(rightValue: Int): Boolean = this.rights == 1 || haveRights[rightValue]!!

    fun setAllRights(rank: Int, xp: Byte, right: Int, perso: Player) {
        var rank = rank
        var xp = xp
        var right = right
        if (rank == -1) rank = this.rank
        if (xp < 0) xp = this.xpGive
        if (xp > 90) xp = 90
        if (right == -1) right = this.rights

        this.rank = rank
        this.xpGive = xp

        if (right != this.rights && right != 1) //Vérifie si les droits sont pareille ou si des droits de meneur; pour ne pas faire la conversion pour rien
            this.parseIntToRight(right)
        this.rights = right

        (DatabaseManager.get(GuildMemberData::class.java) as GuildMemberData).update(perso)
    }

    private fun initRights() {
        for (right in Constant.G_RIGHTS) {
            this.haveRights[right] = false
        }
    }

    private fun parseIntToRight(total: Int) {
        var total = total
        if (this.haveRights.isEmpty()) {
            this.initRights()
        }
        if (total != 1) {
            if (this.haveRights.isNotEmpty())//Si les droits contiennent quelque chose -> Vidage (Même si le HashMap supprimerais les entrées doublon lors de l'ajout)
                this.haveRights.clear()
            initRights()//Remplissage des droits

            val array = this.haveRights.keys.toTypedArray() //Récupère les clef de map dans un tableau d'Integer

            while (total > 0) {
                var i = this.haveRights.size - 1
                while (i < this.haveRights.size) {
                    if (array[i] <= total) {
                        total = total xor array[i]
                        this.haveRights[array[i]] = true
                        break
                    }
                    i--
                }
            }
        }
    }
}
