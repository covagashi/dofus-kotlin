package org.starloco.locos.entity.exchange

import org.starloco.locos.client.Player
import org.starloco.locos.common.SocketManager
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.login.PlayerData
import org.starloco.locos.entity.npc.NpcTemplate
import org.starloco.locos.entity.pet.PetEntry
import org.starloco.locos.game.world.World
import org.starloco.locos.game.world.World.Couple
import org.starloco.locos.kernel.Constant
import org.starloco.locos.kernel.Logging
import org.starloco.locos.`object`.GameObject
import org.starloco.locos.`object`.ObjectTemplate
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(PlayerExchange::class.java)

open class PlayerExchange(player1: Player, player2: Player) : Exchange(player1, player2) {

    private fun isPodsOK(i: Byte): Boolean {
        if (this is CraftSecure)
            return true

        var newpods = 0
        var oldpods = 0
        if (i.toInt() == 1) {
            val podsmax = this.player1.getMaxPod()
            val pods = this.player1.getPodUsed()
            for (couple in items2) {
                if (couple.second == 0)
                    continue
                val obj = World.world.getGameObject(couple.first)
                newpods += obj!!.template!!.pod * couple.second
            }
            if (newpods == 0) {
                return true
            }
            for (couple in items1) {
                if (couple.second == 0)
                    continue
                val obj = World.world.getGameObject(couple.first)
                oldpods += obj!!.template!!.pod * couple.second
            }
            if (newpods + pods - oldpods > podsmax) {
                // Erreur 70
                // 1 + 70 => 170
                SocketManager.GAME_SEND_Im_PACKET(this.player1, "170")
                return false
            }
        } else {
            val podsmax = this.player2.getMaxPod()
            val pods = this.player2.getPodUsed()
            for (couple in items1) {
                if (couple.second == 0)
                    continue
                val obj = World.world.getGameObject(couple.first)
                newpods += obj!!.template!!.pod * couple.second
            }
            if (newpods == 0) {
                return true
            }
            for (couple in items2) {
                if (couple.second == 0)
                    continue
                val obj = World.world.getGameObject(couple.first)
                oldpods += obj!!.template!!.pod * couple.second
            }
            if (newpods + pods - oldpods > podsmax) {
                SocketManager.GAME_SEND_Im_PACKET(this.player2, "170")
                return false
            }
        }
        return true
    }

    @Synchronized
    fun getKamas(guid: Int): Long {
        var i = 0
        if (this.player1.id == guid)
            i = 1
        else if (this.player2.id == guid)
            i = 2

        if (i == 1)
            return kamas1
        else if (i == 2)
            return kamas2
        return 0
    }

    @Synchronized
    override fun toogleOk(id: Int): Boolean {
        val i = (if (this.player1.id == id) 1 else 2).toByte()
        if (this.isPodsOK(i)) {
            if (i.toInt() == 1) {
                ok1 = !ok1
                SocketManager.GAME_SEND_EXCHANGE_OK(this.player1.gameClient!!, ok1, id)
                SocketManager.GAME_SEND_EXCHANGE_OK(this.player2.gameClient!!, ok1, id)
            } else if (i.toInt() == 2) {
                ok2 = !ok2
                SocketManager.GAME_SEND_EXCHANGE_OK(this.player1.gameClient!!, ok2, id)
                SocketManager.GAME_SEND_EXCHANGE_OK(this.player2.gameClient!!, ok2, id)
            }
            return ok1 && ok2
        }
        return false
    }

    @Synchronized
    fun setKamas(guid: Int, k: Long) {
        ok1 = false
        ok2 = false

        var i = 0
        if (this.player1.id == guid)
            i = 1
        else if (this.player2.id == guid)
            i = 2
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player1.gameClient!!, ok1, this.player1.id)
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player2.gameClient!!, ok1, this.player1.id)
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player1.gameClient!!, ok2, this.player2.id)
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player2.gameClient!!, ok2, this.player2.id)
        if (k < 0)
            return
        if (i == 1) {
            kamas1 = k
            SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(this.player1, 'G', "", "$k")
            SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(this.player2.gameClient!!, 'G', "", "$k")
        } else if (i == 2) {
            kamas2 = k
            SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(this.player1.gameClient!!, 'G', "", "$k")
            SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(this.player2, 'G', "", "$k")
        }
    }

    @Synchronized
    override fun cancel() {
        if (this.player1.account != null)
            if (this.player1.gameClient != null)
                SocketManager.GAME_SEND_EV_PACKET(this.player1.gameClient!!)
        if (this.player2.account != null)
            if (this.player2.gameClient != null)
                SocketManager.GAME_SEND_EV_PACKET(this.player2.gameClient!!)
        this.player1.exchangeAction = null
        this.player2.exchangeAction = null
    }

    @Synchronized
    override fun apply() {
        var str = ""
        try {
            str += this.player1.name + " : "
            for (couple1 in items1) {
                str += (", ["
                        + World.world.getGameObject(couple1.first)!!.template!!.id
                        + "@" + couple1.first + ";" + couple1.second + "]")
            }
            str += " avec " + kamas1 + " K.\n"
        } catch (e: Exception) {
            log.error("unexpected error", e)
                }
        try {
            str += "Avec " + this.player2.name
            for (couple2 in items2) {
                str += (", ["
                        + World.world.getGameObject(couple2.first)!!.template!!.id
                        + "@" + couple2.first + ";" + couple2.second + "]")
            }
            str += " avec " + kamas2 + " K."
            if (Logging.USE_LOG)
                Logging.getInstance().write("Object", "Exchange : $str")
        } catch (e: Exception) {
            log.error("unexpected error", e)
                }

        //Gestion des Kamas
        this.player1.addKamas(-kamas1 + kamas2)
        this.player2.addKamas(-kamas2 + kamas1)
        for (couple in items1) // Les items du player vers le player2
        {
            if (couple.second == 0)
                continue
            if (World.world.getGameObject(couple.first) == null)
                continue
            if (World.world.getGameObject(couple.first)!!.position != Constant.ITEM_POS_NO_EQUIPED)
                continue
            if (!this.player1.hasItemGuid(couple.first))//Si le player n'a pas l'item (Ne devrait pas arriver : wpepro)
            {
                couple.second = 0 //On met la quantit a 0 pour viter les problemes
                continue
            }
            val obj = World.world.getGameObject(couple.first)
            if (obj!!.quantity - couple.second < 1)//S'il ne reste plus d'item apres l'change
            {
                this.player1.removeItem(couple.first)
                couple.second = obj!!.quantity
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this.player1, couple.first)
                if (!this.player2.addItem(obj, true, false))//Si le joueur avait un item similaire
                    World.world.removeGameObject(couple.first) //On supprime l'item inutile
            } else {
                obj!!.quantity = obj!!.quantity - couple.second
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this.player1, obj)
                val newObj = obj!!.getClone(couple.second, true)
                if (this.player2.addItem(newObj!!, true, false))//Si le joueur n'avait pas d'item similaire
                    World.world.addGameObject(newObj) //On ajoute l'item au World
            }
        }
        for (couple in items2) {
            if (couple.second == 0)
                continue
            if (World.world.getGameObject(couple.first) == null)
                continue
            if (World.world.getGameObject(couple.first)!!.position != Constant.ITEM_POS_NO_EQUIPED)
                continue
            if (!this.player2.hasItemGuid(couple.first))//Si le player n'a pas l'item (Ne devrait pas arriver)
            {
                couple.second = 0 //On met la quantit a 0 pour viter les problemes
                continue
            }
            this.giveObject(couple, World.world.getGameObject(couple.first))
        }
        //Fin
        this.player1.exchangeAction = null
        this.player2.exchangeAction = null
        SocketManager.GAME_SEND_Ow_PACKET(this.player1)
        SocketManager.GAME_SEND_Ow_PACKET(this.player2)
        SocketManager.GAME_SEND_STATS_PACKET(this.player1)
        SocketManager.GAME_SEND_STATS_PACKET(this.player2)
        SocketManager.GAME_SEND_EXCHANGE_VALID(this.player1.gameClient!!, 'a')
        SocketManager.GAME_SEND_EXCHANGE_VALID(this.player2.gameClient!!, 'a')
        (DatabaseManager.get(PlayerData::class.java) as PlayerData).update(this.player1)
        (DatabaseManager.get(PlayerData::class.java) as PlayerData).update(this.player2)
    }

    protected fun giveObject(couple: Couple<Int, Int>, `object`: GameObject?) {
        if (`object` == null) return
        if (`object`.quantity - couple.second < 1) {
            this.player2.removeItem(couple.first)
            couple.second = `object`.quantity
            SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this.player2, couple.first)
            if (!this.player1.addItem(`object`, true, false)) World.world.removeGameObject(couple.first)
        } else {
            `object`.quantity = `object`.quantity - couple.second
            SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this.player2, `object`)
            val newObj = `object`.getClone(couple.second, true)
            if (this.player1.addItem(newObj!!, true, false)) World.world.addGameObject(newObj)
        }
    }

    @Synchronized
    fun addItem(guid: Int, qua: Int, pguid: Int) {
        var qua = qua
        ok1 = false
        ok2 = false

        val obj = World.world.getGameObject(guid)
        var i = 0

        if (this.player1.id == pguid)
            i = 1
        if (this.player2.id == pguid)
            i = 2

        if (qua == 1)
            qua = 1
        val str = "$guid|$qua"
        if (obj == null)
            return
        if (obj.position != Constant.ITEM_POS_NO_EQUIPED)
            return

        if (this is CraftSecure) {
            val tmp = ArrayList<ObjectTemplate>()
            for (couple in this.items1) {
                val _tmp = World.world.getGameObject(couple.first) ?: continue
                if (!tmp.contains(_tmp.template))
                    tmp.add(_tmp.template!!)
            }
            for (couple in this.items2) {
                val _tmp = World.world.getGameObject(couple.first) ?: continue
                if (!tmp.contains(_tmp.template))
                    tmp.add(_tmp.template!!)
            }

            if (!tmp.contains(obj.template)) {
                if (tmp.size + 1 > (this as CraftSecure).maxCase) {
                    SocketManager.GAME_SEND_MESSAGE(
                        if (this.player1.id == pguid) this.player1 else this.player2,
                        "Impossible d'ajouter plus d'ingrédients.",
                        "B9121B"
                    )
                    return
                }
            }
        }

        val add = ("|" + obj.template!!.id + "|"
                + obj.encodeStats())
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player1.gameClient!!, ok1, this.player1.id)
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player2.gameClient!!, ok1, this.player1.id)
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player1.gameClient!!, ok2, this.player2.id)
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player2.gameClient!!, ok2, this.player2.id)
        if (i == 1) {
            val couple = getCoupleInList(items1, guid)
            if (couple != null) {
                couple.second += qua
                SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(
                    this.player1, 'O', "+", "" + guid + "|" + couple.second
                )
                SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(
                    this.player2.gameClient!!, 'O', "+", "" + guid + "|" + couple.second + add
                )
                return
            }
            SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(this.player1, 'O', "+", str)
            SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(this.player2.gameClient!!, 'O', "+", str + add)
            items1.add(Couple(guid, qua))
        } else if (i == 2) {
            val couple = getCoupleInList(items2, guid)
            if (couple != null) {
                couple.second += qua
                SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(
                    this.player2, 'O', "+", "" + guid + "|" + couple.second
                )
                SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(
                    this.player1.gameClient!!, 'O', "+", "" + guid + "|" + couple.second + add
                )
                return
            }
            SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(this.player2, 'O', "+", str)
            SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(this.player1.gameClient!!, 'O', "+", str + add)
            items2.add(Couple(guid, qua))
        }
    }

    @Synchronized
    fun removeItem(guid: Int, qua: Int, pguid: Int) {
        var i = 0
        if (this.player1.id == pguid)
            i = 1
        else if (this.player2.id == pguid)
            i = 2
        ok1 = false
        ok2 = false

        SocketManager.GAME_SEND_EXCHANGE_OK(this.player1.gameClient!!, ok1, this.player1.id)
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player2.gameClient!!, ok1, this.player1.id)
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player1.gameClient!!, ok2, this.player2.id)
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player2.gameClient!!, ok2, this.player2.id)

        val `object` = World.world.getGameObject(guid) ?: return
        val add = "|" + `object`.template!!.id + "|" + `object`.encodeStats()

        if (i == 1) {
            val couple = getCoupleInList(items1, guid) ?: return
            val newQua = couple.second - qua

            if (newQua < 1) {
                items1.remove(couple)
                SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(this.player1, 'O', "-", "" + guid)
                SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(this.player2.gameClient!!, 'O', "-", "" + guid)
            } else {
                couple.second = newQua
                SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(this.player1, 'O', "+", "" + guid + "|" + newQua)
                SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(
                    this.player2.gameClient!!,
                    'O',
                    "+",
                    "" + guid + "|" + newQua + add
                )
            }
        } else if (i == 2) {
            val couple = getCoupleInList(items2, guid) ?: return
            val newQua = couple.second - qua

            if (newQua < 1) {
                items2.remove(couple)
                SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(this.player1.gameClient!!, 'O', "-", "" + guid)
                SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(this.player2, 'O', "-", "" + guid)
            } else {
                couple.second = newQua
                SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(
                    this.player1.gameClient!!,
                    'O',
                    "+",
                    "" + guid + "|" + newQua + add
                )
                SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(this.player2, 'O', "+", "" + guid + "|" + newQua)
            }
        }
    }

    @Synchronized
    fun getQuaItem(itemID: Int, playerGuid: Int): Int {
        val items: ArrayList<Couple<Int, Int>> = if (this.player1.id == playerGuid) items1 else items2
        for (curCoupl in items)
            if (curCoupl.first == itemID)
                return curCoupl.second
        return 0
    }

    /**
     * Other Exchange *
     */
    class NpcExchangePets(var player: Player, var npc: NpcTemplate) {
        private var kamas1: Long = 0
        private var kamas2: Long = 0
        private val items1 = ArrayList<Couple<Int, Int>>()
        private val items2 = ArrayList<Couple<Int, Int>>()
        private var ok1 = false
        private var ok2 = false

        @Synchronized
        fun toogleOK(paramBoolean: Boolean) {
            if (paramBoolean) {
                this.ok2 = !this.ok2
                SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok2)
            } else {
                this.ok1 = !this.ok1
                SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok1, this.player.id)
            }
            if (this.ok2 && this.ok1)
                apply()
        }

        @Synchronized
        fun setKamas(paramBoolean: Boolean, paramLong: Long) {
            if (paramLong < 0L)
                return
            this.ok1 = false
            this.ok2 = false
            SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok1, this.player.id)
            SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok2)
            if (paramBoolean) {
                this.kamas2 = paramLong
                SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(this.player.gameClient!!, 'G', "", "$paramLong")
                return
            }
            if (paramLong > this.player.kamas)
                return
            this.kamas1 = paramLong
            SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(this.player, 'G', "", "$paramLong")
        }

        @Synchronized
        fun cancel() {
            if (this.player.account != null && this.player.gameClient != null)
                SocketManager.GAME_SEND_EV_PACKET(this.player.gameClient!!)
            this.player.exchangeAction = null
        }

        @Synchronized
        fun apply() {
            var objetToChange: GameObject? = null
            for (couple in items1) {
                if (couple.second == 0)
                    continue
                if (World.world.getGameObject(couple.first)!!.position != Constant.ITEM_POS_NO_EQUIPED)
                    continue
                if (!player.hasItemGuid(couple.first))//Si le player n'a pas l'item (Ne devrait pas arriver)
                {
                    couple.second = 0 //On met la quantit a 0 pour viter les problemes
                    continue
                }
                val obj = World.world.getGameObject(couple.first)
                objetToChange = obj
                if (obj!!.quantity - couple.second < 1)//S'il ne reste plus d'item apres l'change
                {
                    player.removeItem(couple.first)
                    couple.second = obj!!.quantity
                    SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(player, couple.first)
                } else {
                    obj!!.quantity = obj!!.quantity - couple.second
                    SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(player, obj)
                }
            }

            for (couple1 in items2) {
                if (couple1.second == 0)
                    continue
                if (World.world.getObjTemplate(couple1.first) == null)
                    continue
                if (World.world.getGameObject(objetToChange!!.guid) == null)
                    continue
                var obj1: GameObject? = null
                if (World.world.getObjTemplate(couple1.first)!!.type == 18)
                    obj1 = World.world.getObjTemplate(couple1.first)!!.createNewFamilier(objetToChange)
                if (World.world.getObjTemplate(couple1.first)!!.type == 77)
                    obj1 = World.world.getObjTemplate(couple1.first)!!.createNewCertificat(objetToChange)

                if (obj1 == null)
                    continue
                if (this.player.addItem(obj1, true, false))
                    World.world.addGameObject(obj1)
                SocketManager.GAME_SEND_Im_PACKET(this.player, "021;" + couple1.second + "~" + couple1.first)
            }
            World.world.removeGameObject(objetToChange!!.guid)
            this.player.exchangeAction = null
            SocketManager.GAME_SEND_EXCHANGE_VALID(this.player.gameClient!!, 'a')
            SocketManager.GAME_SEND_Ow_PACKET(this.player)
            (DatabaseManager.get(PlayerData::class.java) as PlayerData).update(this.player)
        }

        @Synchronized
        fun addItem(obj: Int, qua: Int) {
            if (qua <= 0)
                return
            if (World.world.getGameObject(obj) == null)
                return
            this.ok2 = false
            this.ok1 = this.ok2
            SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok1, this.player.id)
            SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok2)
            val str = "$obj|$qua"
            val couple = getCoupleInList(items1, obj)
            if (couple != null) {
                couple.second += qua
                SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(player, 'O', "+", "" + obj + "|" + couple.second)
                return
            }
            SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(player, 'O', "+", str)
            items1.add(Couple(obj, qua))
            if (verifIfAlonePets() || verifIfAloneParcho()) {
                if (items1.size == 1) {
                    var id = -1
                    var objet: GameObject? = null
                    for (i in items1) {
                        if (World.world.getGameObject(i.first) == null)
                            continue
                        objet = World.world.getGameObject(i.first)
                        if (World.world.getGameObject(i.first)!!.template!!.type == 18) {
                            id = Constant.getParchoByIdPets(World.world.getGameObject(i.first)!!.template!!.id)
                        } else if (World.world.getGameObject(i.first)!!.template!!.type == 77) {
                            id = Constant.getPetsByIdParcho(World.world.getGameObject(i.first)!!.template!!.id)
                        }
                    }
                    if (id == -1)
                        return
                    val str1 = (id.toString() + "|" + 1 + "|" + id + "|"
                            + objet!!.encodeStats())
                    this.items2.add(Couple(id, 1))
                    SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(this.player.gameClient!!, 'O', "+", str1)
                    this.ok2 = true
                    SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok2)
                } else {
                    clearNpcItems()
                    this.ok2 = false
                    SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok2)
                }
            } else {
                clearNpcItems()
                this.ok2 = false
                SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok2)
            }
        }

        @Synchronized
        fun removeItem(guid: Int, qua: Int) {
            if (qua < 0)
                return
            this.ok2 = false
            this.ok1 = this.ok2
            SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok1, this.player.id)
            SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok2)
            if (World.world.getGameObject(guid) == null)
                return
            val couple = getCoupleInList(items1, guid)
            val newQua = couple!!.second - qua
            if (newQua < 1)//Si il n'y a pu d'item
            {
                items1.remove(couple)
                SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(this.player, 'O', "-", "" + guid)
            } else {
                couple.second = newQua
                SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(this.player, 'O', "+", "" + guid + "|" + newQua)
            }
            if (verifIfAlonePets()) {
                if (items1.size == 1) {
                    var id = -1
                    var objet: GameObject? = null
                    for (i in items1) {
                        if (World.world.getGameObject(i.first) == null)
                            continue
                        objet = World.world.getGameObject(i.first)
                        if (World.world.getGameObject(i.first)!!.template!!.type == 18) {
                            id = Constant.getParchoByIdPets(World.world.getGameObject(i.first)!!.template!!.id)
                        } else if (World.world.getGameObject(i.first)!!.template!!.type == 77) {
                            id = Constant.getPetsByIdParcho(World.world.getGameObject(i.first)!!.template!!.id)
                        }
                    }
                    if (id == -1)
                        return
                    val str = (id.toString() + "|" + 1 + "|" + id + "|"
                            + objet!!.encodeStats())
                    this.items2.add(Couple(id, 1))
                    SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(this.player.gameClient!!, 'O', "+", str)
                    this.ok2 = true
                    SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok2)
                } else {
                    clearNpcItems()
                    this.ok2 = false
                    SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok2)
                }
            } else {
                clearNpcItems()
                this.ok2 = false
                SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok2)
            }
        }

        fun verifIfAlonePets(): Boolean {
            for (i in items1)
                if (World.world.getGameObject(i.first)!!.template!!.type != 18)
                    return false
            return true
        }

        fun verifIfAloneParcho(): Boolean {
            for (i in items1)
                if (World.world.getGameObject(i.first)!!.template!!.type != 77)
                    return false
            return true
        }

        @Synchronized
        fun clearNpcItems() {
            for (i in items2)
                SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(this.player.gameClient!!, 'O', "-", i.first.toString() + "")
            this.items2.clear()
        }

        @Synchronized
        private fun getCoupleInList(items: ArrayList<Couple<Int, Int>>, guid: Int): Couple<Int, Int>? {
            for (couple in items)
                if (couple.first == guid)
                    return couple
            return null
        }

        @Synchronized
        fun getQuaItem(obj: Int, b: Boolean): Int {
            val list: ArrayList<Couple<Int, Int>> = if (b) this.items2 else this.items1

            for (item in list)
                if (item.first == obj)
                    return item.second
            return 0
        }
    }

    class NpcRessurectPets(var perso: Player, var npc: NpcTemplate) {
        private var kamas1: Long = 0
        private var kamas2: Long = 0
        private val items1 = ArrayList<Couple<Int, Int>>()
        private val items2 = ArrayList<Couple<Int, Int>>()
        private var ok1 = false
        private var ok2 = false

        @Synchronized
        fun getKamas(b: Boolean): Long {
            if (b)
                return this.kamas2
            return this.kamas1
        }

        @Synchronized
        fun toogleOK(paramBoolean: Boolean) {
            if (paramBoolean) {
                this.ok2 = !this.ok2
                SocketManager.GAME_SEND_EXCHANGE_OK(this.perso.gameClient!!, this.ok2)
            } else {
                this.ok1 = !this.ok1
                SocketManager.GAME_SEND_EXCHANGE_OK(this.perso.gameClient!!, this.ok1, this.perso.id)
            }
            if (this.ok2 && this.ok1)
                apply()
        }

        @Synchronized
        fun setKamas(paramBoolean: Boolean, paramLong: Long) {
            if (paramLong < 0L)
                return
            this.ok1 = false
            this.ok2 = false
            SocketManager.GAME_SEND_EXCHANGE_OK(this.perso.gameClient!!, this.ok1, this.perso.id)
            SocketManager.GAME_SEND_EXCHANGE_OK(this.perso.gameClient!!, this.ok2)
            if (paramBoolean) {
                this.kamas2 = paramLong
                SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(this.perso.gameClient!!, 'G', "", "$paramLong")
                return
            }
            if (paramLong > this.perso.kamas)
                return
            this.kamas1 = paramLong
            SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(this.perso, 'G', "", "$paramLong")
        }

        @Synchronized
        fun cancel() {
            if (this.perso.account != null && this.perso.gameClient != null)
                SocketManager.GAME_SEND_EV_PACKET(this.perso.gameClient!!)
            this.perso.exchangeAction = null
        }

        @Synchronized
        fun apply() {
            for (item in items1) {
                val `object` = World.world.getGameObject(item.first)
                if (`object`!!.template!!.id == 8012) {
                    if (`object`!!.quantity - item.second < 1) {
                        perso.removeItem(item.first)
                        item.second = `object`!!.quantity
                        SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(perso, item.first)
                    } else {
                        `object`!!.quantity = `object`!!.quantity - item.second
                        SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(perso, `object`)
                    }
                } else {
                    val pet = World.world.getPetsEntry(item.first)
                    if (pet != null) {
                        pet.resurrection()
                        SocketManager.GAME_SEND_UPDATE_OBJECT_DISPLAY_PACKET(this.perso, `object`)
                    }
                }
            }
            this.perso.exchangeAction = null
            SocketManager.GAME_SEND_EXCHANGE_VALID(this.perso.gameClient!!, 'a')
            SocketManager.GAME_SEND_Ow_PACKET(this.perso)
            (DatabaseManager.get(PlayerData::class.java) as PlayerData).update(this.perso)
        }

        @Synchronized
        fun addItem(obj: Int, qua: Int) {
            if (qua <= 0)
                return
            if (World.world.getGameObject(obj) == null)
                return
            this.ok2 = false
            this.ok1 = this.ok2
            SocketManager.GAME_SEND_EXCHANGE_OK(this.perso.gameClient!!, this.ok1, this.perso.id)
            SocketManager.GAME_SEND_EXCHANGE_OK(this.perso.gameClient!!, this.ok2)
            val str = "$obj|$qua"
            val couple = getCoupleInList(items1, obj)
            if (couple != null) {
                couple.second += qua
                SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso, 'O', "+", "" + obj + "|" + couple.second)
                return
            }
            SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso, 'O', "+", str)
            items1.add(Couple(obj, qua))
            if (verification()) {
                if (items1.size == 2) {
                    var id = -1
                    var objet: GameObject? = null

                    for (i in items1) {
                        objet = World.world.getGameObject(i.first)

                        if (objet == null)
                            continue
                        if (objet.template!!.type == 90) {
                            val pet = World.world.getPetsEntry(i.first)
                            if (pet != null) {
                                id = pet.template
                                break
                            }
                        }
                    }

                    if (id == -1 || objet == null)
                        return
                    val str1 = (id.toString() + "|" + 1 + "|" + id + "|"
                            + objet.encodeStats())
                    this.items2.add(Couple(id, 1))
                    SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(this.perso.gameClient!!, 'O', "+", str1)
                    this.ok2 = true
                    SocketManager.GAME_SEND_EXCHANGE_OK(this.perso.gameClient!!, this.ok2)
                } else {
                    clearNpcItems()
                    this.ok2 = false
                    SocketManager.GAME_SEND_EXCHANGE_OK(this.perso.gameClient!!, this.ok2)
                }
            } else {
                clearNpcItems()
                this.ok2 = false
                SocketManager.GAME_SEND_EXCHANGE_OK(this.perso.gameClient!!, this.ok2)
            }
        }

        @Synchronized
        fun removeItem(guid: Int, qua: Int) {
            if (qua < 0)
                return
            this.ok2 = false
            this.ok1 = this.ok2
            SocketManager.GAME_SEND_EXCHANGE_OK(this.perso.gameClient!!, this.ok1, this.perso.id)
            SocketManager.GAME_SEND_EXCHANGE_OK(this.perso.gameClient!!, this.ok2)
            if (World.world.getGameObject(guid) == null)
                return
            val couple = getCoupleInList(items1, guid)
            val newQua = couple!!.second - qua
            if (newQua < 1)//Si il n'y a pu d'item
            {
                items1.remove(couple)
                SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(this.perso, 'O', "-", "" + guid)
            } else {
                couple.second = newQua
                SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(this.perso, 'O', "+", "" + guid + "|" + newQua)
            }
            if (verification()) {
                if (items1.size == 2) {
                    var id = -1
                    var objet: GameObject? = null

                    for (i in items1) {
                        objet = World.world.getGameObject(i.first)

                        if (objet == null)
                            continue
                        if (objet.template!!.type == 90) {
                            id = World.world.getPetsEntry(i.first)!!.template
                            break
                        }
                    }

                    if (id == -1 || objet == null)
                        return

                    val str = (id.toString() + "|" + 1 + "|" + id + "|"
                            + objet.encodeStats())
                    this.items2.add(Couple(id, 1))
                    SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(this.perso.gameClient!!, 'O', "+", str)
                    this.ok2 = true
                    SocketManager.GAME_SEND_EXCHANGE_OK(this.perso.gameClient!!, this.ok2)
                } else {
                    clearNpcItems()
                    this.ok2 = false
                    SocketManager.GAME_SEND_EXCHANGE_OK(this.perso.gameClient!!, this.ok2)
                }
            } else {
                clearNpcItems()
                this.ok2 = false
                SocketManager.GAME_SEND_EXCHANGE_OK(this.perso.gameClient!!, this.ok2)
            }
        }

        fun verification(): Boolean {
            var verif = true
            for (item in items1) {
                val `object` = World.world.getGameObject(item.first)
                if ((`object`!!.template!!.id != 8012 && `object`!!.template!!.type != 90) || item.second > 1)
                    verif = false
            }
            return verif
        }

        @Synchronized
        fun clearNpcItems() {
            for (i in items2)
                SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(this.perso.gameClient!!, 'O', "-", i.first.toString() + "")
            this.items2.clear()
        }

        @Synchronized
        private fun getCoupleInList(items: ArrayList<Couple<Int, Int>>, guid: Int): Couple<Int, Int>? {
            for (couple in items)
                if (couple.first == guid)
                    return couple
            return null
        }

        @Synchronized
        fun getQuaItem(obj: Int, b: Boolean): Int {
            val list: ArrayList<Couple<Int, Int>> = if (b) this.items2 else this.items1

            for (item in list)
                if (item.first == obj)
                    return item.second
            return 0
        }
    }
    /** Fin Other Exchange **/
}
