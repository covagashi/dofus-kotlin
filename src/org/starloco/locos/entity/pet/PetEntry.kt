package org.starloco.locos.entity.pet

import org.starloco.locos.client.Player
import org.starloco.locos.common.SocketManager
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.login.ObjectData
import org.starloco.locos.database.data.login.PetData
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant
import org.starloco.locos.`object`.GameObject

import java.text.SimpleDateFormat
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(PetEntry::class.java)

class PetEntry(
    val objectId: Int,
    val template: Int,
    var lastEatDate: Long,
    var quaEat: Int,
    var pdv: Int,
    var corpulence: Int,
    isEPO: Boolean
) {

    private val RATIO_FEED = 1 //3 official

    private var isEupeoh = false

    init {
        getCurrentStatsPoids()
        this.isEupeoh = isEPO
    }

    fun getIsEupeoh(): Boolean = this.isEupeoh

    fun parseLastEatDate(): String {
        var hexDate = "#"
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date = formatter.format(this.lastEatDate)

        val split = date.split(Regex("\\s"))

        val split0 = split[0].split("-")
        hexDate += Integer.toHexString(split0[0].toInt()) + "#"
        val mois = split0[1].toInt() - 1
        val jour = split0[2].toInt()
        hexDate += (Integer.toHexString(
            ((if (mois < 10) "0$mois" else mois).toString() + if (jour < 10) "0$jour" else jour).toInt()
        ) + "#")

        val split1 = split[1].split(":")
        val heure = split1[0] + split1[1]
        hexDate += Integer.toHexString(heure.toInt())

        return hexDate
    }

    fun parseCorpulence(): Int {
        if (corpulence > 0 || corpulence < 0)
            return 7
        return 0
    }

    fun getCurrentStatsPoids(): Int {
        /*
		 * d6,d5,d4,d3,d2 = 4U de poids 8a = 2U de poids 7c = 2U de poids POUR
		 * PETIT WABBIT = 3U de poids b2 = 8U de poids 70 = 8U de poids le reste
		 * a 1U de poids
		 */
        val obj = World.world.getGameObject(this.objectId) ?: return 0
        var cumul = 0
        for (entry in obj.stats.effects.entries) {
            if (entry.key == ("320").toInt(16)) // Vita du familier
            {
            } else if (entry.key == ("326").toInt(16)) // Poids du familier
            {
            } else if (entry.key == ("328").toInt(16)) // Date du familier
            {
            } else if (entry.key == ("8a").toInt(16)) // %dom
                cumul += 2 * entry.value
            else if (entry.key == ("7c").toInt(16)) // sagesse
                cumul += 3 * entry.value
            else if (entry.key == ("d2").toInt(16)
                || entry.key == ("d3").toInt(16)
                || entry.key == ("d4").toInt(16)
                || entry.key == ("d5").toInt(16)
                || entry.key == ("d6").toInt(16)
            ) // %resist
                cumul += 4 * entry.value
            else if (entry.key == ("b2").toInt(16)
                || entry.key == ("70").toInt(16)
            ) // soin et dommages
                cumul += 8 * entry.value
            else
                cumul += entry.value
        }
        this.Poids = cumul
        return this.Poids
    }

    private var Poids = 0

    fun getMaxStat(): Int = World.world.getPets(this.template)!!.max

    fun looseFight(player: Player) {
        val obj = World.world.getGameObject(this.objectId) ?: return
        val pets = World.world.getPets(obj.template!!.id) ?: return

        this.pdv--
        obj.txtStat.remove(Constant.STATS_PETS_PDV)
        obj.txtStat[Constant.STATS_PETS_PDV] = Integer.toHexString(if (this.pdv > 0) this.pdv else 0)

        if (this.pdv <= 0) {
            this.pdv = 0
            obj.txtStat.remove(Constant.STATS_PETS_PDV)
            obj.txtStat[Constant.STATS_PETS_PDV] = Integer.toHexString(0) //Mise a 0 des pdv

            if (pets.getDeadTemplate() == 0 || World.world.getObjTemplate(pets.getDeadTemplate()) == null)// Si Pets DeadTemplate = 0 remove de l'item et pet entry
            {
                World.world.removeGameObject(obj.guid)
                player.removeItem(obj.guid)
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(player, obj.guid)
                if (player.addItem(obj, true, false))//Si le joueur n'avait pas d'item similaire
                    World.world.addGameObject(obj)
            } else {
                obj.template = World.world.getObjTemplate(pets.getDeadTemplate())
                if (obj.position == Constant.ITEM_POS_FAMILIER) {
                    obj.position = Constant.ITEM_POS_NO_EQUIPED
                    SocketManager.GAME_SEND_OBJET_MOVE_PACKET(player, obj)
                }
            }
            SocketManager.GAME_SEND_Im_PACKET(player, "154")
        }
        SocketManager.GAME_SEND_UPDATE_OBJECT_DISPLAY_PACKET(player, obj)
        (DatabaseManager.get(PetData::class.java) as PetData).update(this)
    }

    fun eat(p: Player, min: Int, max: Int, statsID: Int, feed: GameObject) {
        val obj = World.world.getGameObject(this.objectId) ?: return
        val pets = World.world.getPets(obj.template!!.id) ?: return

        if (this.corpulence <= 0)//Si il est maigrichon (X repas rats) on peu le nourrir plusieurs fois
        {
            //Update du petsEntry
            this.lastEatDate = System.currentTimeMillis()
            this.corpulence++
            this.quaEat++
            //Update de l'item
            obj.txtStat.remove(Constant.STATS_PETS_POIDS)
            obj.txtStat[Constant.STATS_PETS_POIDS] = this.corpulence.toString()
            SocketManager.GAME_SEND_Im_PACKET(p, "029")
            if (this.quaEat >= RATIO_FEED) { //3 normalement
                //Update de l'item
                if ((if (this.getIsEupeoh()) pets.max * 1.1 else pets.max.toDouble()) > this.getCurrentStatsPoids())//Si il est sous l'emprise d'EPO on augmente de +10% le jet maximum
                {
                    if (obj.stats.effects.containsKey(statsID)) {
                        var value = (obj.stats.effects[statsID]!!
                                + World.world.getPets(World.world.getGameObject(this.objectId)!!.template!!.id)!!.gain)
                        if (value > this.getMaxStat())
                            value = this.getMaxStat()
                        obj.stats.effects.remove(statsID)
                        obj.stats.addOneStat(statsID, value)
                    } else
                        obj.stats.addOneStat(statsID, pets.gain)
                }
                this.quaEat = 0
            }
        } else if (this.lastEatDate + min * 3600000 > System.currentTimeMillis()
            && this.corpulence >= 0
        )//Si il n'est pas maigrichon, et on le nourri trop rapidement
        {
            //Update du petsEntry
            this.lastEatDate = System.currentTimeMillis()
            this.corpulence++
            //Update de l'item
            obj.txtStat.remove(Constant.STATS_PETS_POIDS)
            obj.txtStat[Constant.STATS_PETS_POIDS] = this.corpulence.toString()
            if (corpulence == 1) {
                this.quaEat++
                SocketManager.GAME_SEND_Im_PACKET(p, "026")
            } else {
                this.pdv--
                obj.txtStat.remove(Constant.STATS_PETS_PDV)
                obj.txtStat[Constant.STATS_PETS_PDV] =
                    Integer.toHexString(if (this.pdv > 0) this.pdv else 0)
                SocketManager.GAME_SEND_Im_PACKET(p, "027")
            }
            if (this.quaEat >= RATIO_FEED) {
                //Update de l'item
                if ((if (this.getIsEupeoh()) pets.max * 1.1 else pets.max.toDouble()) > this.getCurrentStatsPoids())//Si il est sous l'emprise d'EPO on augmente de +10% le jet maximum
                {
                    if (obj.stats.effects.containsKey(statsID)) {
                        var value = (obj.stats.effects[statsID]!!
                                + World.world.getPets(World.world.getGameObject(this.objectId)!!.template!!.id)!!.gain)
                        if (value > this.getMaxStat())
                            value = this.getMaxStat()
                        obj.stats.effects.remove(statsID)
                        obj.stats.addOneStat(statsID, value)
                    } else
                        obj.stats.addOneStat(statsID, pets.gain)
                }
                this.quaEat = 0
            }
        } else if (this.lastEatDate + min * 3600000 < System.currentTimeMillis()
            && this.corpulence >= 0
        )//Si il n'est pas maigrichon, et que le temps minimal est coul
        {
            //Update du petsEntry
            this.lastEatDate = System.currentTimeMillis()

            if (statsID != 0)
                this.quaEat++
            else
                return
            if (this.quaEat >= RATIO_FEED) {
                //Update de l'item
                if ((if (this.getIsEupeoh()) pets.max * 1.1 else pets.max.toDouble()) > this.getCurrentStatsPoids())//Si il est sous l'emprise d'EPO on augmente de +10% le jet maximum
                {
                    if (obj.stats.effects.containsKey(statsID)) {
                        var value = (obj.stats.effects[statsID]!!
                                + World.world.getPets(World.world.getGameObject(this.objectId)!!.template!!.id)!!.gain)
                        if (value > this.getMaxStat())
                            value = this.getMaxStat()
                        obj.stats.effects.remove(statsID)
                        obj.stats.addOneStat(statsID, value)
                    } else
                        obj.stats.addOneStat(statsID, pets.gain)
                }
                this.quaEat = 0
            }
            SocketManager.GAME_SEND_Im_PACKET(p, "032")
        }

        if (this.pdv <= 0) {
            this.pdv = 0
            obj.txtStat.remove(Constant.STATS_PETS_PDV)
            obj.txtStat[Constant.STATS_PETS_PDV] =
                Integer.toHexString(if (this.pdv > 0) this.pdv else 0) //Mise a 0 des pdv
            if (pets.getDeadTemplate() == 0)// Si Pets DeadTemplate = 0 remove de l'item et pet entry
            {
                World.world.removeGameObject(obj.guid)
                p.removeItem(obj.guid)
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(p, obj.guid)
            } else {
                obj.template = World.world.getObjTemplate(pets.getDeadTemplate())

                if (obj.position == Constant.ITEM_POS_FAMILIER) {
                    obj.position = Constant.ITEM_POS_NO_EQUIPED
                    SocketManager.GAME_SEND_OBJET_MOVE_PACKET(p, obj)
                }
            }
            SocketManager.GAME_SEND_Im_PACKET(p, "154")
        }
        if (obj.txtStat.containsKey(Constant.STATS_PETS_REPAS)) {
            obj.txtStat.remove(Constant.STATS_PETS_REPAS)
            obj.txtStat[Constant.STATS_PETS_REPAS] = Integer.toHexString(feed.template!!.id)
        } else {
            obj.txtStat[Constant.STATS_PETS_REPAS] = Integer.toHexString(feed.template!!.id)
        }
        SocketManager.GAME_SEND_UPDATE_OBJECT_DISPLAY_PACKET(p, obj)
        (DatabaseManager.get(ObjectData::class.java) as ObjectData).update(obj)
        (DatabaseManager.get(PetData::class.java) as PetData).update(this)
    }

    fun eatSouls(p: Player, souls: Map<Int, Int>) {
        val obj = World.world.getGameObject(this.objectId) ?: return
        val pet = World.world.getPets(obj.template!!.id)
        if (pet == null || pet.type != 1)
            return
        //Ajout a l'item les SoulStats tus
        try {
            for (entry in souls.entries) {
                val soul = entry.key
                val count = entry.value
                if (pet.canEat(-1, -1, soul)) {
                    val statsID = pet.statsIdByEat(-1, -1, soul)
                    if (statsID == 0)
                        return
                    val soulCount = if (obj.soulStat[soul] != null) obj.soulStat[soul]!! else 0
                    if (soulCount > 0) {
                        obj.soulStat.remove(soul)
                        obj.soulStat[soul] = count + soulCount
                    } else {
                        obj.soulStat[soul] = count
                    }
                }
            }
            //Re-Calcul des points gagnes
            for (ent in pet.getMonsters().entries) {
                for (entry in ent.value) {
                    for (monsterEntry in entry.entries) {
                        if (pet.getNumbMonster(ent.key, monsterEntry.key) != 0) {
                            var pts = 0
                            for (list in obj.soulStat.entries) {
                                var x = pet.getNumbMonster(ent.key, list.key)
                                if (x == 0)
                                    x = 1
                                pts += Math.floor((list.value / x).toDouble()).toInt() * pet.gain
                                //System.out.println(pts);
                            }
                            if (pts > 0) {
                                if (pts > this.getMaxStat())
                                    pts = this.getMaxStat()
                                if (obj.stats.effects.containsKey(ent.key)) {
                                    val nbr = obj.stats.effects[ent.key]!!
                                    if (nbr - pts > 0)
                                        pts += nbr - pts
                                    obj.stats.effects.remove(ent.key)
                                }
                                obj.stats.effects[ent.key] = pts
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.error("unexpected error", e)
                log.error("Error : " + e.message)
        }
        SocketManager.GAME_SEND_UPDATE_OBJECT_DISPLAY_PACKET(p, obj)
        (DatabaseManager.get(ObjectData::class.java) as ObjectData).update(obj)
        (DatabaseManager.get(PetData::class.java) as PetData).update(this)
    }

    fun updatePets(p: Player, max: Int) {
        val obj = World.world.getGameObject(this.objectId) ?: return
        val pets = World.world.getPets(obj.template!!.id) ?: return
        if (this.pdv <= 0 && obj.template!!.id == pets.getDeadTemplate())
            return //Ne le met pas a jour si deja mort

        if (this.lastEatDate + max * 3600000 < System.currentTimeMillis())//Oublier de le nourrir
        {
            //On calcul le nombre de repas oublier arrondi au suprieur :
            val nbrepas = Math.floor(
                ((System.currentTimeMillis() - this.lastEatDate) / (max * 3600000)).toDouble()
            ).toInt()
            //Perte corpulence
            this.corpulence = this.corpulence - nbrepas

            if (nbrepas != 0) {
                obj.txtStat.remove(Constant.STATS_PETS_POIDS)
                obj.txtStat[Constant.STATS_PETS_POIDS] = this.corpulence.toString()
            }
            //Perte pdv
            this.pdv--
            obj.txtStat.remove(Constant.STATS_PETS_PDV)
            obj.txtStat[Constant.STATS_PETS_PDV] = Integer.toHexString(if (this.pdv > 0) this.pdv else 0)
            this.lastEatDate = System.currentTimeMillis()
        } else {
            if (this.pdv > 0)
                SocketManager.GAME_SEND_Im_PACKET(p, "025")
        }

        if (this.pdv <= 0) {
            this.pdv = 0
            obj.txtStat.remove(Constant.STATS_PETS_PDV)
            obj.txtStat[Constant.STATS_PETS_PDV] = Integer.toHexString(if (this.pdv > 0) this.pdv else 0)

            if (pets.getDeadTemplate() == 0)//Si Pets DeadTemplate = 0 remove de l'item et pet entry
            {
                World.world.removeGameObject(obj.guid)
                p.removeItem(obj.guid)
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(p, obj.guid)
            } else {
                obj.template = World.world.getObjTemplate(pets.getDeadTemplate())
                if (obj.position == Constant.ITEM_POS_FAMILIER) {
                    obj.position = Constant.ITEM_POS_NO_EQUIPED
                    SocketManager.GAME_SEND_OBJET_MOVE_PACKET(p, obj)
                }
            }
            SocketManager.GAME_SEND_Im_PACKET(p, "154")
        }
        SocketManager.GAME_SEND_UPDATE_OBJECT_DISPLAY_PACKET(p, obj)
        (DatabaseManager.get(ObjectData::class.java) as ObjectData).update(obj)
        (DatabaseManager.get(PetData::class.java) as PetData).update(this)
    }

    fun resurrection() {
        val obj = World.world.getGameObject(this.objectId) ?: return

        obj.template = World.world.getObjTemplate(this.template)

        this.pdv = 1
        this.corpulence = 0
        this.quaEat = 0
        this.lastEatDate = System.currentTimeMillis()

        obj.txtStat.remove(Constant.STATS_PETS_PDV)
        obj.txtStat[Constant.STATS_PETS_PDV] = Integer.toHexString(this.pdv)
        (DatabaseManager.get(ObjectData::class.java) as ObjectData).update(obj)
        (DatabaseManager.get(PetData::class.java) as PetData).update(this)
    }

    fun restoreLife(p: Player) {
        val obj = World.world.getGameObject(this.objectId) ?: return
        val pets = World.world.getPets(obj.template!!.id) ?: return

        if (this.pdv >= 10) {
            //Il la mange pas de pdv en plus
            SocketManager.GAME_SEND_Im_PACKET(p, "032")
        } else if (this.pdv > 0) {
            this.pdv++

            obj.txtStat.remove(Constant.STATS_PETS_PDV)
            obj.txtStat[Constant.STATS_PETS_PDV] = Integer.toHexString(this.pdv)

            //this.lastEatDate = System.currentTimeMillis();
            SocketManager.GAME_SEND_Im_PACKET(p, "032")
        } else {
            return
        }
        (DatabaseManager.get(ObjectData::class.java) as ObjectData).update(obj)
        (DatabaseManager.get(PetData::class.java) as PetData).update(this)
    }

    fun giveEpo(p: Player) {
        val obj = World.world.getGameObject(this.objectId) ?: return
        val pets = World.world.getPets(obj.template!!.id) ?: return
        if (this.isEupeoh)
            return
        obj.txtStat[Constant.STATS_PETS_EPO] = Integer.toHexString(1)
        SocketManager.GAME_SEND_Im_PACKET(p, "032")
        SocketManager.GAME_SEND_UPDATE_OBJECT_DISPLAY_PACKET(p, obj)
        (DatabaseManager.get(PetData::class.java) as PetData).update(this)
    }
}
