package org.starloco.locos.entity.map

import org.starloco.locos.client.Player
import org.starloco.locos.common.SocketManager
import org.starloco.locos.game.action.ExchangeAction
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant
import java.util.TreeMap

class House(
    val id: Int,
    val mapId: Int,
    val cellId: Int,
    val houseMapId: Int,
    val houseCellId: Int
) {
    var ownerId: Int = 0
    var sale: Int = 0
    var guildId: Int = 0
    var guildRights: Int = 0
    var access: Int = 0
    var key: String? = null
    //Droits de chaques maisons
    private val haveRight = TreeMap<Int, Boolean>()

    fun open(P: Player, packet: String, isHome: Boolean)//Ouvrir une maison ;o
    {
        if ((!this.canDo(Constant.H_OCANTOPEN) && packet.compareTo(this.key!!) == 0)
            || isHome
        )//Si c'est chez lui ou que le mot de passe est bon
        {
            P.teleport(this.houseMapId, this.houseCellId)
            World.world.houseManager.closeCode(P)
        } else if (packet.compareTo(this.key!!) != 0
            || this.canDo(Constant.H_OCANTOPEN)
        )//Mauvais code
        {
            SocketManager.GAME_SEND_KODE(P, "KE")
            SocketManager.GAME_SEND_KODE(P, "V")
        }
    }

    fun setGuildRightsWithParse(guildRights: Int) {
        this.guildRights = guildRights
        parseIntToRight(guildRights)
    }

    fun enter(P: Player) {//Entrer dans la maison
        if (P.fight != null || P.exchangeAction != null)
            return
        if (this.ownerId == P.accID || (P.guild != null && P.guild!!.id == this.guildId && canDo(Constant.H_GNOCODE)))//C'est sa maison ou mme guilde + droits entrer sans pass
            open(P, "-", true)
        else if (this.ownerId > 0) //Une personne autre la acheter, il faut le code pour rentrer
            SocketManager.GAME_SEND_KODE(P, "CK0|8")//8tant le nombre de chiffre du code
        else if (this.ownerId == 0)//Maison non acheter, mais achetable, on peut rentrer sans code
            open(P, "-", false)
    }

    fun buyIt(P: Player)//Acheter une maison
    {
        val h = P.inHouse
        val str = "CK" + h!!.id + "|" + h!!.sale //ID + Prix
        SocketManager.GAME_SEND_hOUSE(P, str)
    }

    fun sellIt(P: Player)//Vendre une maison
    {
        val h = P.inHouse
        if (isHouse(P, h!!)) {
            val str = "CK" + h!!.id + "|" + h!!.sale //ID + Prix
            SocketManager.GAME_SEND_hOUSE(P, str)
        }
    }

    fun isHouse(P: Player, h: House): Boolean//Savoir si c'est sa maison
    {
        return h.ownerId == P.accID
    }

    fun lock(player: Player) {
        player.exchangeAction = ExchangeAction(ExchangeAction.LOCK_HOUSE, this)
        SocketManager.GAME_SEND_KODE(player, "CK1|8")
    }

    fun canDo(rightValue: Int): Boolean {
        return haveRight[rightValue] == true
    }

    private fun initRight() {
        haveRight[Constant.H_GBLASON] = false
        haveRight[Constant.H_OBLASON] = false
        haveRight[Constant.H_GNOCODE] = false
        haveRight[Constant.H_OCANTOPEN] = false
        haveRight[Constant.C_GNOCODE] = false
        haveRight[Constant.C_OCANTOPEN] = false
        haveRight[Constant.H_GREPOS] = false
        haveRight[Constant.H_GTELE] = false
    }

    fun parseIntToRight(total: Int) {
        var total = total
        if (haveRight.isEmpty()) {
            initRight()
        }
        if (total == 1)
            return

        if (haveRight.size > 0) //Si les droits contiennent quelque chose -> Vidage (Mme si le HashMap supprimerais les entres doublon lors de l'ajout)
            haveRight.clear()

        initRight() //Remplissage des droits

        val mapKey = haveRight.keys.toTypedArray() //Rcupre les clef de map dans un tableau d'Integer

        while (total > 0) {
            for (i in haveRight.size - 1 downTo 0) {
                val map = mapKey[i]
                if (map <= total) {
                    total = total xor map
                    haveRight[map] = true
                    break
                }
            }
        }
    }
}
