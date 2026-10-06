package org.starloco.locos.`object`

import org.starloco.locos.client.Player
import org.starloco.locos.client.other.Stats
import org.starloco.locos.common.Formulas
import org.starloco.locos.common.SocketManager
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.login.ObjectData
import org.starloco.locos.database.data.login.PetData
import org.starloco.locos.entity.pet.PetEntry
import org.starloco.locos.fight.spells.SpellEffect
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant
import org.starloco.locos.`object`.entity.SoulStone
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap

class ObjectTemplate(
    var id: Int,
    var strTemplate: String,
    var name: String,
    var type: Int,
    var level: Int,
    var pod: Int,
    var price: Int,
    var panoId: Int,
    var conditions: String?,
    armesInfos: String,
    sold: Int,
    avgPrice: Int,
    var points: Int,
    var newPrice: Int
) {

    var pACost: Int = -1
    var pOmin: Int = 1
    var pOmax: Int = 1
    var tauxCC: Int = 100
    var tauxEC: Int = 2
    var bonusCC: Int = 0
    var isTwoHanded: Boolean = false
    var sold: Long = 0
    var avgPrice: Int = 0
    private var _onUseActions: ArrayList<ObjectAction>? = null

    override fun toString(): String {
        return "$id"
    }

    init {
        this.sold = sold.toLong()
        this.avgPrice = avgPrice
        if (!armesInfos.isEmpty()) {
            try {
                val infos = armesInfos.split(";")
                pACost = Integer.parseInt(infos[0])
                pOmin = Integer.parseInt(infos[1])
                pOmax = Integer.parseInt(infos[2])
                tauxCC = Integer.parseInt(infos[3])
                tauxEC = Integer.parseInt(infos[4])
                bonusCC = Integer.parseInt(infos[5])
                isTwoHanded = infos[6] == "1"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setInfos(strTemplate: String, name: String, type: Int, level: Int, pod: Int, price: Int, panoId: Int, conditions: String?, armesInfos: String, sold: Int, avgPrice: Int, points: Int, newPrice: Int) {
        this.strTemplate = strTemplate
        this.name = name
        this.type = type
        this.level = level
        this.pod = pod
        this.price = price
        this.panoId = panoId
        this.conditions = conditions
        this.pACost = -1
        this.pOmin = 1
        this.pOmax = 1
        this.tauxCC = 100
        this.tauxEC = 2
        this.bonusCC = 0
        this.sold = sold.toLong()
        this.avgPrice = avgPrice
        this.points = points
        this.newPrice = newPrice
        try {
            val infos = armesInfos.split(";")
            pACost = Integer.parseInt(infos[0])
            pOmin = Integer.parseInt(infos[1])
            pOmax = Integer.parseInt(infos[2])
            tauxCC = Integer.parseInt(infos[3])
            tauxEC = Integer.parseInt(infos[4])
            bonusCC = Integer.parseInt(infos[5])
            isTwoHanded = infos[6] == "1"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addAction(A: ObjectAction) {
        if (this._onUseActions == null)
            this._onUseActions = ArrayList()
        this._onUseActions!!.add(A)
    }

    fun getOnUseActions(): ArrayList<ObjectAction> {
        return _onUseActions ?: ArrayList()
    }

    fun createNewCertificat(obj: GameObject): GameObject? {
        var item: GameObject? = null
        if (type == Constant.ITEM_TYPE_CERTIFICAT_CHANIL) {
            val myPets = World.world.getPetsEntry(obj.guid)
            val txtStat = HashMap<Int, String>()
            val actualStat = obj.txtStat
            if (actualStat.containsKey(Constant.STATS_PETS_PDV))
                txtStat[Constant.STATS_PETS_PDV] = actualStat[Constant.STATS_PETS_PDV]!!
            if (actualStat.containsKey(Constant.STATS_PETS_DATE))
                txtStat[Constant.STATS_PETS_DATE] = myPets!!.lastEatDate.toString() + ""
            if (actualStat.containsKey(Constant.STATS_PETS_POIDS))
                txtStat[Constant.STATS_PETS_POIDS] = actualStat[Constant.STATS_PETS_POIDS]!!
            if (actualStat.containsKey(Constant.STATS_PETS_EPO))
                txtStat[Constant.STATS_PETS_EPO] = actualStat[Constant.STATS_PETS_EPO]!!
            if (actualStat.containsKey(Constant.STATS_PETS_REPAS))
                txtStat[Constant.STATS_PETS_REPAS] = actualStat[Constant.STATS_PETS_REPAS]!!
            item = GameObject(-1, id, 1, Constant.ITEM_POS_NO_EQUIPED, obj.stats, ArrayList(), HashMap(), txtStat, 0)
            DatabaseManager.get(ObjectData::class.java).insert(item)
            DatabaseManager.get(PetData::class.java).delete(World.world.getPetsEntry(obj.guid)!!)
            World.world.removePetsEntry(obj.guid)
        }
        return item
    }
//TODO: PRendre example ici !!
    fun createNewFamilier(obj: GameObject): GameObject? {
        val stats = HashMap<Int, String>()
        stats.putAll(obj.txtStat)
        val `object` = GameObject(-1, id, 1, Constant.ITEM_POS_NO_EQUIPED, obj.stats, ArrayList(), HashMap(), stats, 0)

        if (DatabaseManager.get(ObjectData::class.java).insert(`object`)) {
            val petEntry = PetEntry(`object`.guid, id, System.currentTimeMillis(), 0, (stats[Constant.STATS_PETS_PDV])!!.toInt(16), (stats[Constant.STATS_PETS_POIDS])!!.toInt(16), !stats.containsKey(Constant.STATS_PETS_EPO))

            if (DatabaseManager.get(PetData::class.java).insert(petEntry)) {
                World.world.addPetsEntry(petEntry)
                return `object`
            }
        }
        return null
    }

    fun createNewBenediction(turn: Int): GameObject? {
        val stats = generateNewStatsFromTemplate(strTemplate, true)
        stats.addOneStat(Constant.STATS_TURN, turn)
        val item = GameObject(-1, id, 1, Constant.ITEM_POS_BENEDICTION, stats, ArrayList(), HashMap(), HashMap(), 0)
        if (DatabaseManager.get(ObjectData::class.java).insert(item))
            return item
        return null
    }

    fun createNewMalediction(): GameObject? {
        val stats = generateNewStatsFromTemplate(strTemplate, true)
        stats.addOneStat(Constant.STATS_TURN, 1)
        val `object` = GameObject(-1, id, 1, Constant.ITEM_POS_MALEDICTION, stats, ArrayList(), HashMap(), HashMap(), 0)
        if (DatabaseManager.get(ObjectData::class.java).insert(`object`))
            return `object`
        return null
    }

    fun createNewRoleplayBuff(): GameObject? {
        val stats = generateNewStatsFromTemplate(strTemplate, true)
        stats.addOneStat(Constant.STATS_TURN, 1)
        val `object` = GameObject(-1, id, 1, Constant.ITEM_POS_ROLEPLAY_BUFF, stats, ArrayList(), HashMap(), HashMap(), 0)
        if (DatabaseManager.get(ObjectData::class.java).insert(`object`))
            return `object`
        return null
    }

    fun createNewCandy(turn: Int): GameObject? {
        val stats = generateNewStatsFromTemplate(strTemplate, true)
        stats.addOneStat(Constant.STATS_TURN, turn)
        val item = GameObject(-1, id, 1, Constant.ITEM_POS_BONBON, stats, ArrayList(), HashMap(), HashMap(), 0)
        if (DatabaseManager.get(ObjectData::class.java).insert(item))
            return item
        return null
    }

    fun createNewFollowPnj(turn: Int): GameObject? {
        val stats = generateNewStatsFromTemplate(strTemplate, true)
        stats.addOneStat(Constant.STATS_TURN, turn)
        stats.addOneStat(148, 0)
        val item = GameObject(id, id, 1, Constant.ITEM_POS_PNJ_SUIVEUR, stats, ArrayList(), HashMap(), HashMap(), 0)
        if (DatabaseManager.get(ObjectData::class.java).insert(item))
            return item
        return null
    }

    fun createNewItem(qua: Int, useMax: Boolean): GameObject? {
        var item: GameObject?
        if (type == Constant.ITEM_TYPE_QUETES && (Constant.isCertificatDopeuls(id) || id == 6653)) {
            val txtStat = HashMap<Int, String>()
            txtStat[Constant.STATS_DATE] = System.currentTimeMillis().toString() + ""
            item = GameObject(-1, id, qua, Constant.ITEM_POS_NO_EQUIPED, Stats(false, null), ArrayList(), HashMap(), txtStat, 0)
        } else if (type == Constant.ITEM_TYPE_FAMILIER) {
            val jet = World.world.getPets(this.id)!!.getJet()
            val stats = if (useMax) generateNewStatsFromTemplate(jet, true) else Stats(false, null)

            item = GameObject(-1, id, 1, Constant.ITEM_POS_NO_EQUIPED, stats, ArrayList(), HashMap(), World.world.getPets(id)!!.generateNewtxtStatsForPets(), 0)
            if (DatabaseManager.get(ObjectData::class.java).insert(item)) {
                val pet = PetEntry(item.guid, id, System.currentTimeMillis(), 0, 10, 0, false)
                World.world.addPetsEntry(pet)
                DatabaseManager.get(PetData::class.java).insert(pet)
                return item
            }
            return null
        } else if (type == Constant.ITEM_TYPE_CERTIF_MONTURE) {
            item = GameObject(-1, id, qua, Constant.ITEM_POS_NO_EQUIPED, generateNewStatsFromTemplate(strTemplate, useMax), getEffectTemplate(strTemplate), HashMap(), HashMap(), 0)
        } else {
            if (type == Constant.ITEM_TYPE_OBJET_ELEVAGE) {
                item = GameObject(-1, id, qua, Constant.ITEM_POS_NO_EQUIPED, Stats(false, null), ArrayList(), HashMap(), getStringResistance(strTemplate), 0)
            } else if (Constant.isIncarnationWeapon(id)) {
                val Stats = HashMap<Int, Int>()
                Stats[Constant.ERR_STATS_XP] = 0
                Stats[Constant.STATS_NIVEAU] = 1
                item = GameObject(-1, id, qua, Constant.ITEM_POS_NO_EQUIPED, generateNewStatsFromTemplate(strTemplate, useMax), getEffectTemplate(strTemplate), Stats, HashMap(), 0)
            } else {
                val Stat = HashMap<Int, String>()
                when (type) {
                    1, 2, 3, 4, 5, 6, 7, 8 -> {
                        if (!(strTemplate == null || strTemplate.equals("", ignoreCase = true) || strTemplate.length <= 1)) {
                            for (stat in this.strTemplate.split(",")) {
                                val stats = stat.split("#")
                                val id = (stats[0]).toInt(16)
                                if (id == Constant.STATS_RESIST) Stat[id] = stats[1]
                            }
                        }
                    }
                }
                item = GameObject(-1, id, qua, Constant.ITEM_POS_NO_EQUIPED, generateNewStatsFromTemplate(strTemplate, useMax), getEffectTemplate(strTemplate), HashMap(), Stat, 0)
                item.spellStats.addAll(this.getSpellStatsTemplate())
            }
        }
        if (DatabaseManager.get(ObjectData::class.java).insert(item))
            return item
        return null
    }

    fun createNewItemWithoutDuplication(objects: Collection<GameObject>, qua: Int, useMax: Boolean): GameObject? {
        var id = -1
        var item: GameObject
        if (type == Constant.ITEM_TYPE_QUETES && (Constant.isCertificatDopeuls(id) || this.id == 6653)) {
            val txtStat = HashMap<Int, String>()
            txtStat[Constant.STATS_DATE] = System.currentTimeMillis().toString() + ""
            item = GameObject(id, this.id, qua, Constant.ITEM_POS_NO_EQUIPED, Stats(false, null), ArrayList(), HashMap(), txtStat, 0)
        } else if (type == Constant.ITEM_TYPE_FAMILIER) {
            item = GameObject(id, this.id, 1, Constant.ITEM_POS_NO_EQUIPED, (if (useMax) generateNewStatsFromTemplate(World.world.getPets(this.id)!!.getJet(), false) else Stats(false, null)), ArrayList(), HashMap(), World.world.getPets(this.id)!!.generateNewtxtStatsForPets(), 0)
            //Ajouter du Pets_data SQL et World
            if (DatabaseManager.get(ObjectData::class.java).insert(item)) {
                val pet = PetEntry(item.guid, this.id, System.currentTimeMillis(), 0, 10, 0, false)
                World.world.addPetsEntry(pet)
                DatabaseManager.get(PetData::class.java).insert(pet)
                return item
            }
            return null
        } else if (type == Constant.ITEM_TYPE_CERTIF_MONTURE) {
            item = GameObject(id, this.id, qua, Constant.ITEM_POS_NO_EQUIPED, generateNewStatsFromTemplate(strTemplate, useMax), getEffectTemplate(strTemplate), HashMap(), HashMap(), 0)
        } else {
            if (type == Constant.ITEM_TYPE_OBJET_ELEVAGE) {
                item = GameObject(id, this.id, qua, Constant.ITEM_POS_NO_EQUIPED, Stats(false, null), ArrayList(), HashMap(), getStringResistance(strTemplate), 0)
            } else if (Constant.isIncarnationWeapon(this.id)) {
                val Stats = HashMap<Int, Int>()
                Stats[Constant.ERR_STATS_XP] = 0
                Stats[Constant.STATS_NIVEAU] = 1
                item = GameObject(id, this.id, qua, Constant.ITEM_POS_NO_EQUIPED, generateNewStatsFromTemplate(strTemplate, useMax), getEffectTemplate(strTemplate), Stats, HashMap(), 0)
            } else {
                val Stat = HashMap<Int, String>()
                when (type) {
                    1, 2, 3, 4, 5, 6, 7, 8 -> {
                        val splitted = strTemplate.split(",")
                        for (s in splitted) {
                            val stats = s.split("#")
                            val statID = (stats[0]).toInt(16)
                            if (statID == Constant.STATS_RESIST) {
                                val ResistanceIni = stats[1]
                                Stat[statID] = ResistanceIni
                            }
                        }
                    }
                }
                item = GameObject(id, this.id, qua, Constant.ITEM_POS_NO_EQUIPED, generateNewStatsFromTemplate(strTemplate, useMax), getEffectTemplate(strTemplate), HashMap(), Stat, 0)
                item.spellStats.addAll(this.getSpellStatsTemplate())
            }
        }

        for (`object` in objects) {
            if (World.world.conditionManager.stackIfSimilar(`object`, item, true)) {
                `object`.quantity = `object`.quantity + item.quantity
                return `object`
            }
        }
        DatabaseManager.get(ObjectData::class.java).insert(item)
        return item
    }

    private fun getStringResistance(statsTemplate: String): Map<Int, String> {
        val Stat = HashMap<Int, String>()
        val splitted = statsTemplate.split(",")

        for (s in splitted) {
            val stats = s.split("#")
            val statID = (stats[0]).toInt(16)
            val ResistanceIni = stats[1]
            Stat[statID] = ResistanceIni
        }
        return Stat
    }

    fun getSpellStatsTemplate(): ArrayList<String> {
        val spellStats = ArrayList<String>()

        if (!this.strTemplate.isEmpty()) {
            for (stats in this.strTemplate.split(",")) {
                val split = stats.split("#")
                val id = (split[0]).toInt(16)

                if (id >= 281 && id <= 294) {
                    spellStats.add(stats)
                }
            }
        }
        return spellStats
    }

    fun generateNewStatsFromTemplate(statsTemplate: String,
                                     useMax: Boolean): Stats {
        val itemStats = Stats(false, null)
        //Si stats Vides
        if (statsTemplate == "")
            return itemStats

        val splitted = statsTemplate.split(",")
        for (s in splitted) {
            val stats = s.split("#")
            val statID = (stats[0]).toInt(16)
            var follow = true


            for (a in Constant.ARMES_EFFECT_IDS)
                if (a == statID)
                    follow = false
            if (!follow)//Si c'était un effet Actif d'arme
                continue
            if (statID >= 281 && statID <= 294)
                continue
            if (statID == Constant.STATS_RESIST)
                continue
            var isStatsInvalid = false
            when (statID) {
                110, 139, 605, 614 -> isStatsInvalid = true
                615 -> itemStats.addOneStat(statID, (stats[3]).toInt(16))
            }
            if (isStatsInvalid)
                continue
            var jet = ""
            var value = 1
            try {
                jet = stats[4]
                value = Formulas.getRandomJet(null, null, jet)
                if (useMax) {
                    try {
                        //on prend le jet max
                        val min = (stats[1]).toInt(16)
                        val max = (stats[2]).toInt(16)
                        value = min
                        if (max != 0)
                            value = max
                    } catch (e: Exception) {
                        e.printStackTrace()
                        value = Formulas.getRandomJet(null, null, jet)
                    }
                }
            } catch (e: Exception) {
               System.err.println("$statsTemplate : $s : ${e.message}")
            }
            itemStats.addOneStat(statID, value)
        }
        return itemStats
    }

    private fun getEffectTemplate(statsTemplate: String): ArrayList<SpellEffect> {
        val Effets = ArrayList<SpellEffect>()
        if (statsTemplate == "")
            return Effets

        val splitted = statsTemplate.split(",")
        for (s in splitted) {
            val stats = s.split("#")
            val statID = (stats[0]).toInt(16)
            for (a in Constant.ARMES_EFFECT_IDS) {
                if (a == statID) {
                    val min = stats[1]
                    val max = stats[2]
                    val jet = stats[4]
                    val args = "$min;$max;-1;-1;0;$jet"
                    Effets.add(SpellEffect(statID, args, 0, -1))
                }
            }
            when (statID) {
                110, 139, 605, 614 -> {
                    val min = stats[1]
                    val max = stats[2]
                    val jet = stats[4]
                    val args = "$min;$max;-1;-1;0;$jet"
                    Effets.add(SpellEffect(statID, args, 0, -1))
                }
            }
        }
        return Effets
    }

    fun applyAction(player: Player, target: Player, objectId: Int, cellId: Short, quantity: Int) {
        for (i in 0 until quantity) {
            if (World.world.getGameObject(objectId) == null) return
            if (World.world.getGameObject(objectId)!!.template!!.type == 85 && World.world.getGameObject(objectId)!!.template!!.id == 7010) {
                if (!SoulStone.isInArenaMap(player.curMap.id))
                    if (!SoulStone.isInArenaMap(player.curMap.id))
                        return

                val soulStone = World.world.getGameObject(objectId) as SoulStone

                player.curMap.spawnNewGroup(true, player.curCell.cellId, soulStone.parseGroupData(), "MiS=" + player.id)
                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + World.world.getGameObject(objectId)!!.template!!.id)
                player.removeItem(objectId, 1, true, true)
            } else {
                for (action in this.getOnUseActions())
                    action.apply(player, target, objectId, cellId.toInt())
            }
        }
    }

    @Synchronized
    fun newSold(amount: Int, price: Int) {
        val oldSold = sold
        sold += amount
        avgPrice = ((avgPrice * oldSold + price) / sold).toInt()
    }

    fun isAnEquipment(ethereal: Boolean, bannedTypes: List<Int>?): Boolean {
        if ((!ethereal && this.strTemplate.contains("32c#")) || this.strTemplate.isEmpty() || (this.conditions != null && this.conditions!!.contains("BI")))
            return false
        if (bannedTypes != null && bannedTypes.contains(this.type))
            return false

        when (this.type) {
            Constant.ITEM_TYPE_AMULETTE, Constant.ITEM_TYPE_ANNEAU, Constant.ITEM_TYPE_BOTTES,
            Constant.ITEM_TYPE_CEINTURE, Constant.ITEM_TYPE_COIFFE, Constant.ITEM_TYPE_CAPE,
            Constant.ITEM_TYPE_OUTIL, Constant.ITEM_TYPE_EPEE, Constant.ITEM_TYPE_SAC_DOS,
            Constant.ITEM_TYPE_ARC, Constant.ITEM_TYPE_PIOCHE, Constant.ITEM_TYPE_HACHE,
            Constant.ITEM_TYPE_MARTEAU, Constant.ITEM_TYPE_BAGUETTE, Constant.ITEM_TYPE_DAGUES,
            Constant.ITEM_TYPE_BATON, Constant.ITEM_TYPE_PELLE, Constant.ITEM_TYPE_FAUX,
            Constant.ITEM_TYPE_FAMILIER, Constant.ITEM_TYPE_BOUCLIER -> {
            }
            Constant.ITEM_TYPE_CERTIF_MONTURE -> {
                if (id == 7806 || id == 7807 || id == 7809 || id == 7864 || id == 7865)
                    return false
            }
            else -> return false
        }
        return !bannedObjects.contains(this.id)
    }

    fun isFilledSoulStone(): Boolean {
        when (type) {
            85, 124, 125 -> return true
            else -> return false
        }
    }

    companion object {
        private val bannedObjects = Arrays.asList(493, 494, 495, 496, 922, 7098, 9972, 454, 577, 596, 492, 491, 493, 494, 495, 496, 498, 499, 500, 577, 579, 911, 922, 1501, 1520, 1539, 1560, 1561, 1562, 1563, 1564, 1565, 2154, 2155, 2156, 2170, 2376, 6663, 6713, 6839, 6840, 7098, 7493, 7495, 7650, 7913, 7920, 8539, 8540, 8854, 9031, 9202, 9396, 9627, 9961, 9544, 9545, 9546, 9547, 9548, 10125, 10126, 10127, 10133)
    }
}
