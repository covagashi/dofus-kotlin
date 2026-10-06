package org.starloco.locos.job

import org.starloco.locos.client.Player
import org.starloco.locos.common.Formulas
import org.starloco.locos.common.SocketManager
import org.starloco.locos.fight.spells.SpellEffect
import org.starloco.locos.game.GameClient
import org.starloco.locos.game.world.World
import org.starloco.locos.game.world.World.Couple
import org.starloco.locos.job.maging.Rune
import org.starloco.locos.kernel.Config
import org.starloco.locos.kernel.Constant
import org.starloco.locos.kernel.Logging
import org.starloco.locos.`object`.GameObject
import org.starloco.locos.`object`.ObjectTemplate
import org.starloco.locos.util.RandomStats

import java.util.*
import java.util.Map.Entry
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(JobAction::class.java)

open class JobAction {

    @JvmField var ingredients: MutableMap<Int,Int> = TreeMap()
    @JvmField var lastCraft: MutableMap<Int,Int> = TreeMap()
    @JvmField var player: Player? = null
    @JvmField var data: String = ""
    @JvmField var broke: Boolean = false
    @JvmField var broken: Boolean = false
    @JvmField var isRepeat: Boolean = false
    var id: Int = 0
    var min: Int = 1
    var max: Int = 1
    var isCraft: Boolean = false
    var chance: Int = 100
    var time: Int = 0
    var xpWin: Int = 0
    var SM: JobStat? = null
    var jobCraft: JobCraft? = null
    var oldJobCraft: JobCraft? = null
    private var reConfigingRunes: Int = -1

    constructor(sk: Int, min: Int, max: Int, craft: Boolean, arg: Int, xpWin: Int) {
        this.id = sk
        this.min = min
        this.max = max
        this.isCraft = craft
        this.xpWin = xpWin
        if (craft) this.chance = arg
        else this.time = arg
    }









    fun getJobStat(): JobStat? {
        return this.SM
    }



    fun startCraft(P: Player) {
        this.jobCraft = JobCraft(this, P)
    }

    private fun addCraftObject(player: Player, newObj: GameObject): Int {
        for (entry in  player.items.entries) {
            var obj: GameObject = entry.value
            if (obj.template!!.id == newObj.template!!.id && obj.txtStat == newObj.txtStat
                    && obj.stats!!.isSameStats(newObj.stats!!) && obj.position == Constant.ITEM_POS_NO_EQUIPED) {
                obj.quantity = obj.quantity + newObj.quantity
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(player, obj)
                return obj.guid
            }
        }

        this.player!!.items[newObj.guid] = newObj
        SocketManager.GAME_SEND_OAKO_PACKET(player, newObj)
        World.world.addGameObject(newObj)
        return -1
    }

    fun addIngredient(player: Player, id: Int, quantity: Int) {
        var oldQuantity: Int = this.ingredients[id] ?: 0
        if(quantity < 0) if(- quantity > oldQuantity) return

        this.ingredients.remove(id)
        oldQuantity += quantity

        if (oldQuantity > 0) {
            this.ingredients[id] = oldQuantity
            SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(player, 'O', "+", id.toString() + "|" + oldQuantity)
        } else {
            SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(player, 'O', "-", id.toString() + "")
        }
    }

    fun sizeList(list: Map<Player,ArrayList<Couple<Int,Int>>>): Byte {
        var size: Byte = 0

        for (entry in  list.values) {
            for (couple in  entry) {
                var `object`: GameObject? = World.world.getGameObject(couple.first)
                if (`object` != null) {
                    var objectTemplate: ObjectTemplate? = `object`!!.template
                    if (objectTemplate != null && objectTemplate.id != 7508) size++
                }
            }
        }
        return size
    }

    fun putLastCraftIngredients() {
        if (this.player == null || this.lastCraft == null || !this.ingredients.isEmpty()) return

        this.ingredients.clear()
        this.ingredients.putAll(this.lastCraft)
        for (e in this.ingredients.entries) {
            val o = World.world.getGameObject(e.key)
            if (o != null && o.quantity >= e.value)
                SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(this.player!!, 'O', "+", e.key.toString().toString() + "|" + e.value)
        }
    }

    fun resetCraft() {
        this.ingredients.clear()
        this.lastCraft.clear()
        this.oldJobCraft = null
        this.jobCraft = null
    }

    fun craftPublicMode(crafter: Player, receiver: Player, list: Map<Player,ArrayList<Couple<Int,Int>>>): Boolean {
        if (!this.isCraft) return false

        this.player = crafter
        var SM: JobStat = this.player!!.getMetierBySkill(this.id)!!
        var signed: Boolean = false

        if (this.id == 1 || this.id == 113 || this.id == 115 || this.id == 116 || this.id == 117 || this.id == 118 || this.id == 119 || this.id == 120 || (this.id >= 163 && this.id <= 169)) {
            this.SM = SM
            //craftMaging1(isRepeat, 0);
            return true
        }

        var items: MutableMap<Int,Int> = HashMap()

        for (entry in  list.entries) {
            var player: Player = entry.key

            for (e in  entry.value) {
                if (!player.hasItemGuid(e.first)) {
                    SocketManager.GAME_SEND_Ec_PACKET(player, "EI")
                    SocketManager.GAME_SEND_Ec_PACKET(this.player!!, "EI")
                    return false
                }

                var gameObject: GameObject? = World.world.getGameObject(e.first)
                if (gameObject == null) {
                    SocketManager.GAME_SEND_Ec_PACKET(player, "EI")
                    SocketManager.GAME_SEND_Ec_PACKET(this.player!!, "EI")
                    return false
                }
                if (gameObject!!.quantity < e.second) {
                    SocketManager.GAME_SEND_Ec_PACKET(player, "EI")
                    SocketManager.GAME_SEND_Ec_PACKET(this.player!!, "EI")
                    return false
                }

                var newQua: Int = gameObject!!.quantity - e.second

                if (newQua < 0)
                    return false

                if (newQua == 0) {
                    this.player!!.removeItem(e.first)
                    World.world.removeGameObject(e.first)
                    SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(player, e.first)
                } else {
                    gameObject!!.quantity = newQua
                    SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(player, gameObject)
                }

                if(gameObject!!.template!!.id in items) {
                    var template: Int = gameObject!!.template!!.id
                    var quantity: Int = e.second + items[template]!!
                    items.remove(template)
                    items[template] = quantity
                } else {
                    items[gameObject!!.template!!.id] = e.second
                }
            }
        }

        SocketManager.GAME_SEND_Ow_PACKET(this.player!!)


        //Rune de signature
        if (7508 in items)
            if (SM!!.get_lvl() == 100)
                signed = true

        items.remove(7508)
        var template: Int = World.world.getObjectByIngredientForJob(SM!!.template!!.getListBySkill(this.id), items)

        if (template == -1 || !SM!!.template!!.canCraft(this.id, template)) {
            SocketManager.GAME_SEND_Ec_PACKET(this.player!!, "EI")
            receiver.send("EcEI")
            SocketManager.GAME_SEND_IO_PACKET_TO_MAP(this.player!!.curMap, this.player!!.id, "-")
            items.clear()
            return false
        }

        var success: Boolean = JobConstant.getChanceByNbrCaseByLvl(SM!!.get_lvl(), items.size) >= Formulas.getRandomValue(1, 100)

        if (Logging.USE_LOG)
            Logging.getInstance().write("SecureCraft", this.player!!.name.toString() + " à crafter avec " + (if (success) "SUCCES" else "ECHEC") + " l'item " + template.toString() + " (" + World.world.getObjTemplate(template)!!.name.toString() + ") pour " + receiver.name)
        if (!success) {
            SocketManager.GAME_SEND_Ec_PACKET(this.player!!, "EF")
            SocketManager.GAME_SEND_Ec_PACKET(receiver, "EF")
            SocketManager.GAME_SEND_IO_PACKET_TO_MAP(this.player!!.curMap, this.player!!.id, "-" + template)
            SocketManager.GAME_SEND_Im_PACKET(this.player!!, "0118")
        } else {
            var newObj: GameObject? = World.world.getObjTemplate(template)!!.createNewItem(1, false)
            if (signed) newObj!!.addTxtStat(988, this.player!!.name)
            var guid: Int = this.addCraftObject(receiver, newObj!!)
            if(guid == -1) guid = newObj!!.guid
            var stats: String = newObj!!.encodeStats()

            this.player!!.send("ErKO.toString() + " + guid.toString() + "|1|" + template.toString() + "|" + stats)
            receiver.send("ErKO.toString() + " + guid.toString() + "|1|" + template.toString() + "|" + stats)
            this.player!!.send("EcK;" + template.toString() + ";T" + receiver.name.toString() + ";" + stats)
            receiver.send("EcK;" + template.toString() + ";B" + crafter.name.toString() + ";" + stats)

            SocketManager.GAME_SEND_Ow_PACKET(this.player!!)
            SocketManager.GAME_SEND_IO_PACKET_TO_MAP(this.player!!.curMap, this.player!!.id, "+" + template)
        }

        var winXP: Int = Formulas.calculXpWinCraft(SM!!.get_lvl(), this.ingredients.size) * Config.rateJob
        if (SM!!.template!!.id == 28 && winXP == 1)
            winXP = 10
        if (success) {
            SM!!.addXp(this.player!!, winXP.toLong())
            var SMs: ArrayList<JobStat> = ArrayList()
            SMs.add(SM!!)
            SocketManager.GAME_SEND_JX_PACKET(this.player!!, SMs)
        }

        this.ingredients.clear()
        return success
    }

    fun isMaging(): Boolean {
        return this.id == 1 || this.id == 113 || this.id == 115 || this.id == 116 || this.id == 117
                || this.id == 118 || this.id == 119 || this.id == 120 || (this.id >= 163 && this.id <= 169)
    }

    @Synchronized fun craft(isRepeat: Boolean) {
        if (!this.isCraft) return

        if (this.isMaging()) {
            this.craftMaging1(isRepeat, 1)
            return
        }

        var items: MutableMap<Int,Int> = HashMap()
        //on retire les items mis en ingrdients
        for (e in  this.ingredients.entries) {
            if (!this.player!!.hasItemGuid(e.key)) {
                SocketManager.GAME_SEND_Ec_PACKET(this.player!!, "EI")
                return
            }

            var obj: GameObject? = World.world.getGameObject(e.key)

            if (obj == null) {
                SocketManager.GAME_SEND_Ec_PACKET(this.player!!, "EI")
                return
            }
            if (obj.quantity < e.value) {
                SocketManager.GAME_SEND_Ec_PACKET(this.player!!, "EI")
                return
            }

            var newQua: Int = obj.quantity - e.value
            if (newQua < 0) return

            if (newQua == 0) {
                this.player!!.removeItem(e.key)
                World.world.removeGameObject(e.key)
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this.player!!, e.key)
            } else {
                obj.quantity = newQua
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this.player!!, obj)
            }

            items[obj.template!!.id] = e.value
        }

        var signed: Boolean = false

        if (7508 in items) {
            signed = true
            items.remove(7508)
        }

        SocketManager.GAME_SEND_Ow_PACKET(this.player!!)

        var isUnjobSkill: Boolean = this.getJobStat() == null

        if (!isUnjobSkill) {
            var SM: JobStat? = this.player!!.getMetierBySkill(this.id)
            var templateId: Int = World.world.getObjectByIngredientForJob(SM!!.template!!.getListBySkill(this.id), items)
            //Recette non existante ou pas adapt au mtier
            if (templateId == -1 || !SM.template!!.canCraft(this.id, templateId)) {
                if (Logging.USE_LOG)
                    Logging.getInstance().write("Craft", this.player!!.name.toString() + " à crafter une recette inconnu : " + templateId.toString() + ")")
                this.player!!.sendMessage("Undefined craft (" + templateId.toString() + "), ingredients : (please contact an admin)")
                for(entry in  this.ingredients.entries)
                    this.player!!.sendMessage(entry.key.toString().toString() + " x" + entry.value)
                SocketManager.GAME_SEND_Ec_PACKET(this.player!!, "EI")
                SocketManager.GAME_SEND_IO_PACKET_TO_MAP(this.player!!.curMap, this.player!!.id, "-")
                this.ingredients.clear()
                return
            }

            var chan: Int = JobConstant.getChanceByNbrCaseByLvl(SM.get_lvl(), this.ingredients.size)
            var success: Boolean = chan >= Formulas.getRandomValue(0, 100)

            if(chan == 99) {
                success = chan * 2 >= Formulas.getRandomValue(0, 200)
            }
            if(SM.get_lvl() == 100)
                success = true

            when (this.id) {
                109 -> success = true
            }

            if (Logging.USE_LOG)
                Logging.getInstance().write("Craft", this.player!!.name.toString() + " à crafter avec " + (if (success) "SUCCES" else "ECHEC") + " l'item " + templateId.toString() + " (" + World.world.getObjTemplate(templateId)!!.name.toString() + ")")
            if (!success) {
                SocketManager.GAME_SEND_Ec_PACKET(this.player!!, "EF")
                SocketManager.GAME_SEND_IO_PACKET_TO_MAP(this.player!!.curMap, this.player!!.id, "-" + templateId)
                SocketManager.GAME_SEND_Im_PACKET(this.player!!, "0118")
            } else {
                var newObj: GameObject? = World.world.getObjTemplate(templateId)!!.createNewItemWithoutDuplication(this.player!!.items.values, 1, false)
                if(newObj != null) {
                    if (this.player!!.items[newObj.guid] == null) {
                        if (this.player!!.addItem(newObj, true, false))
                            World.world.addGameObject(newObj)
                    } else {
                        SocketManager.GAME_SEND_UPDATE_OBJECT_DISPLAY_PACKET(this.player!!, newObj)
                    }
                    SocketManager.GAME_SEND_Ow_PACKET(this.player!!)
                    if (signed) newObj!!.addTxtStat(988, this.player!!.name)
                    SocketManager.GAME_SEND_Em_PACKET(this.player!!, "KO.toString() + " + newObj.guid.toString() + "|1|" + templateId.toString() + "|" + newObj.encodeStats().replace(";", "#"))
                    SocketManager.GAME_SEND_Ec_PACKET(this.player!!, "K;" + templateId)
                    SocketManager.GAME_SEND_IO_PACKET_TO_MAP(this.player!!.curMap, this.player!!.id, "+" + templateId)
                }
            }

            var winXP: Int = 0
            if (success)
                winXP = Formulas.calculXpWinCraft(SM.get_lvl(), this.ingredients.size) * Config.rateJob
            else if (!SM.template!!.isMaging())
                winXP = Formulas.calculXpWinCraft(SM.get_lvl(), this.ingredients.size) * Config.rateJob

            if (winXP > 0) {
                SM.addXp(this.player!!, winXP.toLong())
                var SMs: ArrayList<JobStat> = ArrayList()
                SMs.add(SM!!)
                SocketManager.GAME_SEND_JX_PACKET(this.player!!, SMs)
            }
        } else {
            var templateId: Int = World.world.getObjectByIngredientForJob(World.world.getMetier(this.id)!!.getListBySkill(this.id), items)

            if (templateId == -1 || !World.world.getMetier(this.id)!!.canCraft(this.id, templateId)) {
                SocketManager.GAME_SEND_Ec_PACKET(this.player!!, "EI")
                SocketManager.GAME_SEND_IO_PACKET_TO_MAP(this.player!!.curMap, this.player!!.id, "-")
                this.ingredients.clear()
                return
            }

            var newObj: GameObject? = World.world.getObjTemplate(templateId)!!.createNewItemWithoutDuplication(this.player!!.items.values, 1, false)

            if(newObj != null) {
                if (this.player!!.items[newObj.guid] == null) {
                    if (this.player!!.addItem(newObj, true, false))
                        World.world.addGameObject(newObj)
                } else {
                    SocketManager.GAME_SEND_UPDATE_OBJECT_DISPLAY_PACKET(this.player!!, newObj)
                }

                if (signed) newObj!!.addTxtStat(988, this.player!!.name)

                SocketManager.GAME_SEND_Ow_PACKET(this.player!!)
                SocketManager.GAME_SEND_Em_PACKET(this.player!!, "KO.toString() + " + newObj.guid.toString() + "|1|" + templateId.toString() + "|" + newObj.encodeStats().replace(";", "#"))
                SocketManager.GAME_SEND_Ec_PACKET(this.player!!, "K;" + templateId)
                SocketManager.GAME_SEND_IO_PACKET_TO_MAP(this.player!!.curMap, this.player!!.id, "+" + templateId)
            }
        }
        this.lastCraft.clear()
        this.lastCraft.putAll(this.ingredients)
        this.ingredients.clear()

        if(!isRepeat) {
            this.oldJobCraft = this.jobCraft
            this.jobCraft = null
        }
    }


    private @Synchronized fun craftMaging(isRepeat: Boolean, receiver: Player, items: Map<Player,ArrayList<Couple<Int,Int>>>): Boolean {
        var isSigningRune: Boolean = false
        var objectFm: GameObject? = null
        var signingRune: GameObject? = null
        var runeOrPotion: GameObject? = null
        var lvlElementRune: Int = 0
        var statId: Int = -1
        var lvlQuaStatsRune: Int = 0
        var statsAdd: Int = 0
        var deleteID: Int = -1
        var poid: Int = 0
        var idRune: Int = 0
        var bonusRune: Boolean = false
        var statsObjectFm: String = "-1"

        val secure: Boolean = items != null && receiver != null
        val ingredients: MutableMap<Int,Int> = if (items == null) this.ingredients else HashMap()

        if(items != null) {
            for(entry in  items.entries) {
                for(couple in  entry.value) {
                    ingredients[couple.first] = couple.second
                }
            }
        }

        for (id in  ingredients.keys) {
            var `object`: GameObject? = World.world.getGameObject(id)

            if(`object` == null) {
                if(!this.player!!.hasItemGuid(id) || (secure && !this.player!!.hasItemGuid(id) && !receiver.hasItemGuid(id))) {
                    SocketManager.GAME_SEND_Ec_PACKET(this.player!!, "EI")
                    SocketManager.GAME_SEND_IO_PACKET_TO_MAP(this.player!!.curMap, this.player!!.id, "-")
                    ingredients.clear()
                    return false
                }
            }

            var template: Int = `object`!!.template!!.id
            if (`object`!!.template!!.type == 78)
                idRune = id

            //region gros switch rune
            when (template) {
1333 -> {statId = 99
                    lvlElementRune = `object`!!.template!!.level
                    runeOrPotion = `object`
                    
}
1335 -> {statId = 96
                    lvlElementRune = `object`!!.template!!.level
                    runeOrPotion = `object`
                    
}
1337 -> {statId = 98
                    lvlElementRune = `object`!!.template!!.level
                    runeOrPotion = `object`
                    
}
1338 -> {statId = 97
                    lvlElementRune = `object`!!.template!!.level
                    runeOrPotion = `object`
                    
}
1340 -> {statId = 97
                    lvlElementRune = `object`!!.template!!.level
                    runeOrPotion = `object`
                    
}
1341 -> {statId = 96
                    lvlElementRune = `object`!!.template!!.level
                    runeOrPotion = `object`
                    
}
1342 -> {statId = 98
                    lvlElementRune = `object`!!.template!!.level
                    runeOrPotion = `object`
                    
}
1343 -> {statId = 99
                    lvlElementRune = `object`!!.template!!.level
                    runeOrPotion = `object`
                    
}
1345 -> {statId = 99
                    lvlElementRune = `object`!!.template!!.level
                    runeOrPotion = `object`
                    
}
1346 -> {statId = 96
                    lvlElementRune = `object`!!.template!!.level
                    runeOrPotion = `object`
                    
}
1347 -> {statId = 98
                    lvlElementRune = `object`!!.template!!.level
                    runeOrPotion = `object`
                    
}
1348 -> {statId = 97
                    lvlElementRune = `object`!!.template!!.level
                    runeOrPotion = `object`
                    
}
1519 -> {runeOrPotion = `object`
                    statsObjectFm = "76"
                    statsAdd = 1
                    poid = 1
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
1521 -> {runeOrPotion = `object`
                    statsObjectFm = "7c"
                    statsAdd = 1
                    poid = 6
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
1522 -> {runeOrPotion = `object`
                    statsObjectFm = "7e"
                    statsAdd = 1
                    poid = 1
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
1523 -> {runeOrPotion = `object`
                    statsObjectFm = "7d"
                    statsAdd = 3
                    poid = 1
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
1524 -> {runeOrPotion = `object`
                    statsObjectFm = "77"
                    statsAdd = 1
                    poid = 1
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
1525 -> {runeOrPotion = `object`
                    statsObjectFm = "7b"
                    statsAdd = 1
                    poid = 1
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
1545 -> {runeOrPotion = `object`
                    statsObjectFm = "76"
                    statsAdd = 3
                    poid = 3
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
1546 -> {runeOrPotion = `object`
                    statsObjectFm = "7c"
                    statsAdd = 3
                    poid = 18
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
1547 -> {runeOrPotion = `object`
                    statsObjectFm = "7e"
                    statsAdd = 3
                    poid = 3
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
1548 -> {runeOrPotion = `object`
                    statsObjectFm = "7d"
                    statsAdd = 10
                    poid = 10
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
1549 -> {runeOrPotion = `object`
                    statsObjectFm = "77"
                    statsAdd = 3
                    poid = 3
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
1550 -> {runeOrPotion = `object`
                    statsObjectFm = "7b"
                    statsAdd = 3
                    poid = 10
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
1551 -> {runeOrPotion = `object`
                    statsObjectFm = "76"
                    statsAdd = 10
                    poid = 10
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
1552 -> {runeOrPotion = `object`
                    statsObjectFm = "7c"
                    statsAdd = 10
                    poid = 50
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
1553 -> {runeOrPotion = `object`
                    statsObjectFm = "7e"
                    statsAdd = 10
                    poid = 10
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
1554 -> {runeOrPotion = `object`
                    statsObjectFm = "7d"
                    statsAdd = 30
                    poid = 10
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
1555 -> {runeOrPotion = `object`
                    statsObjectFm = "77"
                    statsAdd = 10
                    poid = 10
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
1556 -> {runeOrPotion = `object`
                    statsObjectFm = "7b"
                    statsAdd = 10
                    poid = 10
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
1557 -> {runeOrPotion = `object`
                    statsObjectFm = "6f"
                    statsAdd = 1
                    poid = 100
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
1558 -> {runeOrPotion = `object`
                    statsObjectFm = "80"
                    statsAdd = 1
                    poid = 90
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7433 -> {runeOrPotion = `object`
                    statsObjectFm = "73"
                    statsAdd = 1
                    poid = 30
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7434 -> {runeOrPotion = `object`
                    statsObjectFm = "b2"
                    statsAdd = 1
                    poid = 20
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7435 -> {runeOrPotion = `object`
                    statsObjectFm = "79"
                    statsAdd = 1
                    poid = 20
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7436 -> {runeOrPotion = `object`
                    statsObjectFm = "8a"
                    statsAdd = 1
                    poid = 2
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7437 -> {runeOrPotion = `object`
                    statsObjectFm = "dc"
                    statsAdd = 1
                    poid = 2
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7438 -> {runeOrPotion = `object`
                    statsObjectFm = "75"
                    statsAdd = 1
                    poid = 50
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7442 -> {runeOrPotion = `object`
                    statsObjectFm = "b6"
                    statsAdd = 1
                    poid = 30
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7443 -> {runeOrPotion = `object`
                    statsObjectFm = "9e"
                    statsAdd = 10
                    poid = 1
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7444 -> {runeOrPotion = `object`
                    statsObjectFm = "9e"
                    statsAdd = 30
                    poid = 1
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7445 -> {runeOrPotion = `object`
                    statsObjectFm = "9e"
                    statsAdd = 100
                    poid = 1
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7446 -> {runeOrPotion = `object`
                    statsObjectFm = "e1"
                    statsAdd = 1
                    poid = 15
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7447 -> {runeOrPotion = `object`
                    statsObjectFm = "e2"
                    statsAdd = 1
                    poid = 2
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7448 -> {runeOrPotion = `object`
                    statsObjectFm = "ae"
                    statsAdd = 10
                    poid = 1
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7449 -> {runeOrPotion = `object`
                    statsObjectFm = "ae"
                    statsAdd = 30
                    poid = 3
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7450 -> {runeOrPotion = `object`
                    statsObjectFm = "ae"
                    statsAdd = 100
                    poid = 10
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7451 -> {runeOrPotion = `object`
                    statsObjectFm = "b0"
                    statsAdd = 1
                    poid = 5
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7452 -> {runeOrPotion = `object`
                    statsObjectFm = "f3"
                    statsAdd = 1
                    poid = 4
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7453 -> {runeOrPotion = `object`
                    statsObjectFm = "f2"
                    statsAdd = 1
                    poid = 4
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7454 -> {runeOrPotion = `object`
                    statsObjectFm = "f1"
                    statsAdd = 1
                    poid = 4
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7455 -> {runeOrPotion = `object`
                    statsObjectFm = "f0"
                    statsAdd = 1
                    poid = 4
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7456 -> {runeOrPotion = `object`
                    statsObjectFm = "f4"
                    statsAdd = 1
                    poid = 4
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7457 -> {runeOrPotion = `object`
                    statsObjectFm = "d5"
                    statsAdd = 1
                    poid = 5
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7458 -> {runeOrPotion = `object`
                    statsObjectFm = "d4"
                    statsAdd = 1
                    poid = 5
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7459 -> {runeOrPotion = `object`
                    statsObjectFm = "d2"
                    statsAdd = 1
                    poid = 5
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7460 -> {runeOrPotion = `object`
                    statsObjectFm = "d6"
                    statsAdd = 1
                    poid = 5
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7560 -> {runeOrPotion = `object`
                    statsObjectFm = "d3"
                    statsAdd = 1
                    poid = 5
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
8379 -> {runeOrPotion = `object`
                    statsObjectFm = "7d"
                    statsAdd = 10
                    poid = 10
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
10662 -> {runeOrPotion = `object`
                    statsObjectFm = "b0"
                    statsAdd = 3
                    poid = 15
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
10613 -> {runeOrPotion = `object`
                    statsObjectFm = "e1"
                    statsAdd = 3
                    poid = 15
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
10615 -> {runeOrPotion = `object`
                    statsObjectFm = "e2"
                    statsAdd = 3
                    poid = 5
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
10616 -> {runeOrPotion = `object`
                    statsObjectFm = "e2"
                    statsAdd = 10
                    poid = 15
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
10618 -> {runeOrPotion = `object`
                    statsObjectFm = "8a"
                    statsAdd = 3
                    poid = 5
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
10619 -> {runeOrPotion = `object`
                    statsObjectFm = "8a"
                    statsAdd = 10
                    poid = 20
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
7508 -> {isSigningRune = true
                    signingRune = `object`
                    
}
11118 -> {bonusRune = true
                    runeOrPotion = `object`
                    statsObjectFm = "76"
                    statsAdd = 15
                    poid = 1
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
11119 -> {bonusRune = true
                    runeOrPotion = `object`
                    statsObjectFm = "7c"
                    statsAdd = 15
                    poid = 1
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
11120 -> {bonusRune = true
                    runeOrPotion = `object`
                    statsObjectFm = "7e"
                    statsAdd = 15
                    poid = 1
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
11121 -> {bonusRune = true
                    runeOrPotion = `object`
                    statsObjectFm = "7d"
                    statsAdd = 45
                    poid = 1
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
11122 -> {bonusRune = true
                    runeOrPotion = `object`
                    statsObjectFm = "77"
                    statsAdd = 15
                    poid = 1
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
11123 -> {bonusRune = true
                    runeOrPotion = `object`
                    statsObjectFm = "7b"
                    statsAdd = 15
                    poid = 1
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
11124 -> {bonusRune = true
                    runeOrPotion = `object`
                    statsObjectFm = "b0"
                    statsAdd = 10
                    poid = 1
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
11125 -> {bonusRune = true
                    runeOrPotion = `object`
                    statsObjectFm = "73"
                    statsAdd = 3
                    poid = 1
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
11126 -> {bonusRune = true
                    runeOrPotion = `object`
                    statsObjectFm = "b2"
                    statsAdd = 5
                    poid = 1
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
11127 -> {bonusRune = true
                    runeOrPotion = `object`
                    statsObjectFm = "70"
                    statsAdd = 5
                    poid = 1
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
11128 -> {bonusRune = true
                    runeOrPotion = `object`
                    statsObjectFm = "8a"
                    statsAdd = 10
                    poid = 1
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
11129 -> {bonusRune = true
                    runeOrPotion = `object`
                    statsObjectFm = "dc"
                    statsAdd = 5
                    poid = 1
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
10057 -> {bonusRune = true
                    runeOrPotion = `object`
                    statsObjectFm = "31b"
                    statsAdd = 1
                    poid = 0
                    lvlQuaStatsRune = `object`!!.template!!.level
                    
}
else -> {var type: Int = `object`!!.template!!.type
                    if ((type >= 1 && type <= 11) || (type >= 16 && type <= 22) || type == 81 || type == 102 || type == 114 || `object`!!.template!!.pACost > 0) {
                        val player: Player = if (this.player!!.hasItemGuid(`object`!!.guid)) this.player!! else receiver
                        objectFm = `object`
                        SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK_FM(player.getGameClient()!!, 'O', "+", objectFm!!.guid.toString() + "|" + 1)
                        deleteID = id
                        var newObj: GameObject? = objectFm!!.getClone(1, true) // Cr�ation d'un clone avec un nouveau identifiant

                        if (objectFm!!.quantity > 1) { // S'il y avait plus d'un objet
                            var newQuant: Int = objectFm!!.quantity - 1 // On supprime celui que l'on a ajout�
                            objectFm!!.quantity = newQuant
                            SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(player, objectFm)
                        } else {
                            World.world.removeGameObject(id)

                            this.player!!.removeItem(id)
                            SocketManager.GAME_SEND_DELETE_STATS_ITEM_FM(player, id)
                        }
                        objectFm = newObj; // Tout neuf avec un nouveau identifiant
                        break
                    }
            
}
}
            //endregion
        }

        //region Calcul formule
        var poid2: Double = getPwrPerEffet(statsObjectFm.toInt(16))
        if (poid2 > 0.0)
            poid = statsAdd * poid2.toInt()

        if (SM == null || objectFm == null || runeOrPotion == null) {
            if (objectFm != null) {
                World.world.addGameObject(objectFm)
                this.player!!.addItem(objectFm, true)
            }

            if(receiver != null)
                SocketManager.GAME_SEND_Ec_PACKET(receiver, "EI")
            SocketManager.GAME_SEND_Ec_PACKET(this.player!!, "EI")
            SocketManager.GAME_SEND_IO_PACKET_TO_MAP(this.player!!.curMap, this.player!!.id, "-")

            ingredients.clear()
            return false
        }
        if (deleteID != -1) {
            this.ingredients.remove(deleteID)
        }

        val template: ObjectTemplate? = objectFm!!.template
        var chances: ArrayList<Int> = ArrayList()

        var chance: Int = 0
        var lvlJob: Int = SM!!.get_lvl()
        var currentWeightTotal: Int = 1
        var pwrPerte: Int = 0
        var objTemplateID: Int = template!!.id
        var statStringObj: String = objectFm!!.encodeStats()

        if (lvlElementRune > 0 && lvlQuaStatsRune == 0) {
            chance = Formulas.calculChanceByElement(lvlJob, template!!.level, lvlElementRune)
            if (chance > 100 - (lvlJob / 20))
                chance = 100 - (lvlJob / 20)
            if (chance < (lvlJob / 20))
                chance = (lvlJob / 20)
            chances.add(0, chance)
            chances.add(1, 0)
            chances.add(2, 100 - chance)
        } else if (lvlQuaStatsRune > 0 && lvlElementRune == 0) {
            var currentWeightStats: Int = 1
            if (!statStringObj.isEmpty()) {
                currentWeightTotal = currentTotalWeigthBase(statStringObj, objectFm); // Poids total de l'objet : PWRg
                currentWeightStats = currentWeithStats(objectFm, statsObjectFm); // Poids � ajouter : PWRcarac
            }

            var currentTotalBase: Int = WeithTotalBase(objTemplateID) // Poids maximum de l'objet : PWRmax
            var currentMinBase: Int = WeithTotalBaseMin(objTemplateID)

            if (currentTotalBase < 0)
                currentTotalBase = 0
            if (currentWeightStats < 0)
                currentWeightStats = 0
            if (currentWeightTotal < 0)
                currentWeightTotal = 0

            var coef: Float = 1f
            var baseStats: Int = viewBaseStatsItem(objectFm, statsObjectFm).toInt().toInt()
            var currentStats: Int = viewActualStatsItem(objectFm, statsObjectFm).toInt()

            if (baseStats == 1 && currentStats == 1 || baseStats == 1 && currentStats == 0) {
                coef = 1.0f
            } else if (baseStats == 2 && currentStats == 2) {
                coef = 0.50f
            } else if (baseStats == 0 && currentStats == 0 || baseStats == 0 && currentStats == 1) {
                coef = coefExo
            }

            var x: Float = 1f
            var canFM: Boolean = true
            var statMax: Int = getStatBaseMaxs(objectFm!!.template!!, statsObjectFm)
            var actualJet: Int = getActualJet(objectFm, statsObjectFm)

            if (actualJet > statMax) {
                x = 0.8F
                var overPerEffect: Int = (getOverPerEffet(statsObjectFm.toInt(16)).toInt())
                //if (statMax == 0)
                if (actualJet >= (statMax + overPerEffect))
                    canFM = false
                if(statsObjectFm.toInt(16) == 111) {
                    if(objectFm!!.isOverFm2(111, 1))
                        if(!canFM)
                            canFM = true
                } else if(statsObjectFm.toInt(16) == 128) {
                    if(objectFm!!.isOverFm2(128, 1))
                        if(!canFM)
                            canFM = true
                }
            }
            if (lvlJob < (Math.floor((template!!.level / 2).toDouble()).toInt()))
                canFM = false; // On rate le FM si le m�tier n'est pas suffidant

            var diff: Int = (Math.abs((currentTotalBase * 1.3f) - currentWeightTotal).toInt())

            if (canFM) {
                chances = Formulas.chanceFM(currentTotalBase, currentMinBase, currentWeightTotal, currentWeightStats, poid, diff, coef, statMax, getStatBaseMins(objectFm!!.template!!, statsObjectFm), currentStats(objectFm, statsObjectFm), x, bonusRune, statsAdd)
            } else {// Si l'objet est au dessus de l'over (impossible statistiquement ... mais evite un gelano 2 PA :p)
                chances.add(0, 0)
                chances.add(1, 0)
            }
        }

        var aleatoryChance: Int = Formulas.getRandomValue(1, 100)
        var SC: Int = chances[0]
        var SN: Int = chances[1]
        var successC: Boolean = (aleatoryChance <= SC)
        var successN: Boolean = (aleatoryChance <= (SC + SN))

        if(objectFm!!.puit >= statsAdd) {
            if(runeOrPotion!!.template!!.id != 1558 && runeOrPotion!!.template!!.id != 1557 && runeOrPotion!!.template!!.id != 7438) {
                if(Formulas.getRandomValue(1, 2) == 1)
                    successC = true
            }
        }

        if(runeOrPotion!!.template!!.id == 1558 || runeOrPotion!!.template!!.id == 1557)
            if(Formulas.getRandomValue(0, 100) == 1)
                successC = true

        if (successC || successN) {
            var winXP: Int = Formulas.calculXpWinFm(objectFm!!.template!!.level, poid) * Config.rateJob
            if (winXP > 0) {
                SM!!.addXp(this.player!!, winXP.toLong())
                var SMs: ArrayList<JobStat> = ArrayList()
                SMs.add(SM!!)
                SocketManager.GAME_SEND_JX_PACKET(this.player!!, SMs)
            }
        }
        //endregion

        //region succès critique
        if (successC) {
            var coef: Int = 0
            pwrPerte = 0

            if (lvlElementRune == 1) coef = 50
            else if (lvlElementRune == 25) coef = 65
            else if (lvlElementRune == 50) coef = 85
            if (isSigningRune)
                objectFm!!.addTxtStat(985, this.player!!.name)

            if (lvlElementRune > 0 && lvlQuaStatsRune == 0) {
                for (effect in  objectFm!!.effects) {
                    if (effect.effectID != 100)
                        continue
                    var infos: Array<String> = effect.args.split(";").toTypedArray()
                    try {
                        var min: Int = infos[0].toInt(16)
                        var max: Int = infos[1].toInt(16)
                        var newMin: Int = (min * coef) / 100
                        var newMax: Int = (max * coef) / 100
                        if (newMin == 0)
                            newMin = 1
                        var newRange: String = "1d" + (newMax - newMin + 1) + "+" + (newMin - 1)
                        var newArgs: String = Integer.toHexString(newMin).toString() + ";" + Integer.toHexString(newMax).toString() + ";-1;-1;0;" + newRange
                        effect.args = newArgs
                        effect.effectID =(statId)
                    } catch (e: Exception) {
                        log.error("unexpected error", e)
                    }
                }
            } else if (lvlQuaStatsRune > 0 && lvlElementRune == 0) {
                var negative: Boolean = false
                var currentStats: Int = viewActualStatsItem(objectFm, statsObjectFm).toInt()

                if (currentStats == 2) {
                    if (statsObjectFm.compareTo("7b") == 0) {
                        statsObjectFm = "98"
                        negative = true
                    }
                    if (statsObjectFm.compareTo("77") == 0) {
                        statsObjectFm = "9a"
                        negative = true
                    }
                    if (statsObjectFm.compareTo("7e") == 0) {
                        statsObjectFm = "9b"
                        negative = true
                    }
                    if (statsObjectFm.compareTo("76") == 0) {
                        statsObjectFm = "9d"
                        negative = true
                    }
                    if (statsObjectFm.compareTo("7c") == 0) {
                        statsObjectFm = "9c"
                        negative = true
                    }
                    if (statsObjectFm.compareTo("7d") == 0) {
                        statsObjectFm = "99"
                        negative = true
                    }
                }

                if (statStringObj.isEmpty()) {
                    var statsStr: String = statsObjectFm.toString() + "#" + Integer.toHexString(statsAdd).toString() + "#0#0#0d0.toString() + " + statsAdd
                    objectFm!!.clearStats()
                    objectFm!!.parseStringToStats(statsStr)
                } else {
                    lateinit var statsStr: String
                    if (currentStats == 1 || currentStats == 2)
                        statsStr = objectFm!!.parseFMStatsString(statsObjectFm, objectFm, statsAdd, negative)
                    else
                        statsStr = objectFm!!.parseFMStatsString(statsObjectFm, objectFm, statsAdd, negative).toString() + "," + statsObjectFm.toString() + "#" + Integer.toHexString(statsAdd).toString() + "#0#0#0d0.toString() + " + statsAdd

                    objectFm!!.clearStats()
                    objectFm!!.parseStringToStats(statsStr)
                }
            }

            var data: String = objectFm!!.guid.toString() + "|1|" + objectFm!!.template!!.id.toString() + "|" + objectFm!!.encodeStats()

            if (!this.isRepeat)
                this.reConfigingRunes = -1
            if (this.reConfigingRunes != 0 || this.broken)
                if(receiver == null)
                    SocketManager.GAME_SEND_EXCHANGE_MOVE_OK_FM(this.player!!, 'O', "+", data)

            this.data = data
            SocketManager.GAME_SEND_IO_PACKET_TO_MAP(this.player!!.curMap, this.player!!.id, "+" + objTemplateID)
            if(!secure) {
                SocketManager.GAME_SEND_Ec_PACKET(this.player!!, "K;" + objTemplateID)
            }
        }
        //endregion
        //region Succès neutre
        else if (successN) {
            pwrPerte = 0
            if (isSigningRune) {
                objectFm!!.addTxtStat(985, this.player!!.name)
            }

            var negative: Boolean = false
            var currentStats: Int = viewActualStatsItem(objectFm, statsObjectFm).toInt()

            if (currentStats == 2) {
                if (statsObjectFm.compareTo("7b") == 0) {
                    statsObjectFm = "98"
                    negative = true
                }
                if (statsObjectFm.compareTo("77") == 0) {
                    statsObjectFm = "9a"
                    negative = true
                }
                if (statsObjectFm.compareTo("7e") == 0) {
                    statsObjectFm = "9b"
                    negative = true
                }
                if (statsObjectFm.compareTo("76") == 0) {
                    statsObjectFm = "9d"
                    negative = true
                }
                if (statsObjectFm.compareTo("7c") == 0) {
                    statsObjectFm = "9c"
                    negative = true
                }
                if (statsObjectFm.compareTo("7d") == 0) {
                    statsObjectFm = "99"
                    negative = true
                }
            }
            if (statStringObj.isEmpty()) {
                var statsStr: String = statsObjectFm.toString() + "#" + Integer.toHexString(statsAdd).toString() + "#0#0#0d0.toString() + " + statsAdd
                objectFm!!.clearStats()
                objectFm!!.parseStringToStats(statsStr)
            } else {
                lateinit var statsStr: String

                if (objectFm!!.puit <= 0) {// EC en premier s'il n'y a pas de puits
                    statsStr = objectFm!!.parseStringStatsEC_FM(objectFm!!, statsAdd.toDouble(), runeOrPotion!!.template!!.id)
                    objectFm!!.clearStats()
                    objectFm!!.parseStringToStats(statsStr)
                    pwrPerte = currentWeightTotal - currentTotalWeigthBase(statsStr, objectFm)
                }
                if (currentStats == 1 || currentStats == 2)
                    statsStr = objectFm!!.parseFMStatsString(statsObjectFm, objectFm, statsAdd, negative)
                else
                    statsStr = objectFm!!.parseFMStatsString(statsObjectFm, objectFm, statsAdd, negative).toString() + "," + statsObjectFm.toString() + "#" + Integer.toHexString(statsAdd).toString() + "#0#0#0d0.toString() + " + statsAdd
                objectFm!!.clearStats()
                objectFm!!.parseStringToStats(statsStr)
            }

            var data: String = objectFm!!.guid.toString() + "|1|" + objectFm!!.template!!.id.toString() + "|" + objectFm!!.encodeStats()
            if (!this.isRepeat)
                this.reConfigingRunes = -1
            if (this.reConfigingRunes != 0 || this.broken)
                if(receiver == null)
                    SocketManager.GAME_SEND_EXCHANGE_MOVE_OK_FM(this.player!!, 'O', "+", data)

            this.data = data
            SocketManager.GAME_SEND_IO_PACKET_TO_MAP(this.player!!.curMap, this.player!!.id, "+" + objTemplateID)

            if (pwrPerte > 0) {
                SocketManager.GAME_SEND_Ec_PACKET(this.player!!, "EF")
                SocketManager.GAME_SEND_Im_PACKET(this.player!!, "0194")
            } else {
                SocketManager.GAME_SEND_Ec_PACKET(this.player!!, "K;" + objTemplateID)
            }
        }
        //endregion
        //region Echec critique
        else {// EC
            pwrPerte = 0

            if (!statStringObj.isEmpty()) {
                var statsStr: String = objectFm!!.parseStringStatsEC_FM(objectFm!!, statsAdd.toDouble(), -1)
                objectFm!!.clearStats()
                objectFm!!.parseStringToStats(statsStr)
                pwrPerte = currentWeightTotal - currentTotalWeigthBase(statsStr, objectFm)
            }

            var data: String = objectFm!!.guid.toString() + "|1|" + objectFm!!.template!!.id.toString() + "|" + objectFm!!.encodeStats()
            if (!this.isRepeat)
                this.reConfigingRunes = -1
            if (this.reConfigingRunes != 0 || this.broken)
                if(receiver == null)
                    SocketManager.GAME_SEND_EXCHANGE_MOVE_OK_FM(this.player!!, 'O', "+", data)

            this.data = data
            SocketManager.GAME_SEND_IO_PACKET_TO_MAP(this.player!!.curMap, this.player!!.id, "-" + objTemplateID)
            SocketManager.GAME_SEND_Ec_PACKET(this.player!!, "EF")

            if (pwrPerte > 0)
                SocketManager.GAME_SEND_Im_PACKET(this.player!!, "0117")
            else
                SocketManager.GAME_SEND_Im_PACKET(this.player!!, "0183")
        }
        //endregion

        objectFm!!.puit = (objectFm!!.puit + pwrPerte) - poid
        var newQuantity: Int = (ingredients[idRune] ?: 1) - 1

        if (objectFm != null) {
            World.world.addGameObject(objectFm)
            if(receiver == null) {
                this.player!!.addItem(objectFm, true)
            } else {
                receiver.addItem(objectFm, true)
            }
        }

        if(receiver == null) {
            this.decrementObjectQuantity(this.player!!, signingRune!!)
            this.decrementObjectQuantity(this.player!!, runeOrPotion)
            this.player!!.send("EmKO-" + objectFm!!.guid.toString() + "|1|")
            this.ingredients.clear()
            this.player!!.send("EMKO.toString() + " + objectFm!!.guid.toString() + "|1")
            this.ingredients[objectFm!!.guid] = 1

            if (newQuantity >= 1) {
                this.player!!.send("EMKO.toString() + " + idRune.toString() + "|" + newQuantity)
                this.ingredients[idRune] = newQuantity
            } else {
                this.player!!.send("EMKO-" + idRune)
            }
        } else {
            if(items != null) {
                for(entry in  items.entries) {
                    val player: Player = entry.key
                    for(couple in  entry.value) {
                        if(signingRune != null && signingRune!!.guid == couple.first)
                            this.decrementObjectQuantity(player, signingRune)
                        if(runeOrPotion!!.guid == couple.first)
                            this.decrementObjectQuantity(player, runeOrPotion)
                        //player.send("EMKO-" + couple.first);

                    }
                }
            }

            var stats: String = objectFm!!.encodeStats()
            this.player!!.send("ErKO.toString() + " + objectFm!!.guid.toString() + "|1|" + template.toString() + "|" + stats)
            receiver.send("ErKO.toString() + " + objectFm!!.guid.toString() + "|1|" + template.toString() + "|" + stats)
            this.player!!.send("EcK;" + template.toString() + ";T" + receiver.name.toString() + ";" + stats)
            receiver.send("EcK;" + template.toString() + ";B" + this.player!!.name.toString() + ";" + stats)

            if(!successC) {
                receiver.send("EcEF")
            }
        }

        this.lastCraft.clear()
        this.lastCraft.putAll(this.ingredients)

        SocketManager.GAME_SEND_Ow_PACKET(this.player!!)
        if (!isRepeat) this.jobCraft = null
        return true
    }

    //region usefull function for fm
    private fun decrementObjectQuantity(player: Player, `object`: GameObject) {
        if (`object` != null) {
            var newQua: Int = `object`!!.quantity - 1
            if (newQua <= 0) {
                this.player!!.removeItem(`object`!!.guid, `object`!!.quantity, true, true)
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(player, `object`!!.guid)
            } else {
                `object`!!.quantity = newQua
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(player, `object`)
            }
        }
    }

    companion object {
        @JvmField var coefExo: Float = 0.25f
    @JvmStatic fun getStatBaseMaxs(objMod: ObjectTemplate, statsModif: String): Int {
        var split: Array<String> = objMod.strTemplate.split(",").toTypedArray()
        for (s in  split) {
            var stats: Array<String> = s.split("#").toTypedArray()
            if (stats[0].lowercase().compareTo(statsModif.lowercase()) > 0) {
            	
            } else if (stats[0].lowercase().compareTo(statsModif.lowercase()) == 0) {
                var max: Int = stats[2].toInt(16)
                if (max == 0)
                    max = stats[1].toInt(16)
                return max
            }
        }
        return 0
    }

    @JvmStatic fun getStatBaseMins(objMod: ObjectTemplate, statsModif: String): Int {
        var split: Array<String> = objMod.strTemplate.split(",").toTypedArray()
        for (s in  split) {
            var stats: Array<String> = s.split("#").toTypedArray()
            if (stats[0].lowercase().compareTo(statsModif.lowercase()) > 0) {
            } else if (stats[0].lowercase().compareTo(statsModif.lowercase()) == 0) {
                return stats[1].toInt(16)
            }
        }
        return 0
    }

    @JvmStatic fun WeithTotalBaseMin(objTemplateID: Int): Int {
        var weight: Int = 0
        var alt: Int = 0
        var statsTemplate: String = ""
        statsTemplate = World.world.getObjTemplate(objTemplateID)!!.strTemplate
        if (statsTemplate == null || statsTemplate.isEmpty())
            return 0
        var split: Array<String> = statsTemplate.split(",").toTypedArray()
        for (s in  split) {
            var stats: Array<String> = s.split("#").toTypedArray()
            var statID: Int = stats[0].toInt(16)
            var sig: Boolean = true
            for (a in  Constant.ARMES_EFFECT_IDS)
                if (a == statID)
                    sig = false
            if (!sig)
                continue
            var jet: String = ""
            var value: Int = 1
            try {
                jet = stats[4]
                value = Formulas.getRandomJet(null, null, jet)
                try {
                    var min: Int = stats[1].toInt(16)
                    value = min
                } catch (e: Exception) {
                    value = Formulas.getRandomJet(null, null, jet)
                    log.error("unexpected error", e)
                }
            } catch (e: Exception) {
                log.error("unexpected error", e)
            }
            var statX: Int = 1
            if (statID == 125 || statID == 158 || statID == 174) {
                statX = 1
            } else if (statID == 118 || statID == 126 || statID == 119
                    || statID == 123) {
                statX = 2
            } else if (statID == 138 || statID == 666 || statID == 226
                    || statID == 220) // de
            // da�os,Trampas %
            {
                statX = 3
            } else if (statID == 124 || statID == 176) {
                statX = 5
            } else if (statID == 240 || statID == 241 || statID == 242
                    || statID == 243 || statID == 244)

            {
                statX = 7
            } else if (statID == 210 || statID == 211 || statID == 212
                    || statID == 213 || statID == 214)

            {
                statX = 8
            } else if (statID == 225 || statID == 121) {
                statX = 15
            } else if (statID == 178 ) {
                statX = 20
            } else if (statID == 115 || statID == 182) {
                statX = 30
            } else if (statID == 117) {
                statX = 50
            } else if (statID == 128) {
                statX = 90
            } else if (statID == 111) {
                statX = 100
            }
            weight = value * statX
            alt += weight
        }
        return alt
    }

    @JvmStatic fun WeithTotalBase(objTemplateID: Int): Int {
        var weight: Int = 0
        var alt: Int = 0
        var statsTemplate: String = ""
        statsTemplate = World.world.getObjTemplate(objTemplateID)!!.strTemplate
        if (statsTemplate == null || statsTemplate.isEmpty())
            return 0
        var split: Array<String> = statsTemplate.split(",").toTypedArray()
        for (s in  split) {
            var stats: Array<String> = s.split("#").toTypedArray()
            var statID: Int = stats[0].toInt(16)
            var sig: Boolean = true
            for (a in  Constant.ARMES_EFFECT_IDS)
                if (a == statID)
                    sig = false
            if (!sig)
                continue
            var jet: String = ""
            var value: Int = 1
            try {
                jet = stats[4]
                value = Formulas.getRandomJet(null, null, jet)
                try {
                    var min: Int = stats[1].toInt(16)
                    var max: Int = stats[2].toInt(16)
                    value = min
                    if (max != 0)
                        value = max
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    value = Formulas.getRandomJet(null, null, jet)
                }
            } catch (e: Exception) {
                log.error("unexpected error", e)
            }
            var statX: Int = 1
            if (statID == 125 || statID == 158 || statID == 174) {
                statX = 1
            } else if (statID == 118 || statID == 126 || statID == 119
                    || statID == 123) {
                statX = 2
            } else if (statID == 138 || statID == 666 || statID == 226
                    || statID == 220) // de
            // da�os,Trampas %
            {
                statX = 3
            } else if (statID == 124 || statID == 176) {
                statX = 5
            } else if (statID == 240 || statID == 241 || statID == 242
                    || statID == 243 || statID == 244)

            {
                statX = 7
            } else if (statID == 210 || statID == 211 || statID == 212
                    || statID == 213 || statID == 214)

            {
                statX = 8
            } else if (statID == 225 || statID == 121) {
                statX = 15
            } else if (statID == 178 ) {
                statX = 20
            } else if (statID == 115 || statID == 182) {
                statX = 30
            } else if (statID == 117) {
                statX = 50
            } else if (statID == 128) {
                statX = 90
            } else if (statID == 111) {
                statX = 100
            }
            weight = value * statX
            alt += weight
        }
        return alt
    }

    @JvmStatic fun currentWeithStats(obj: GameObject, statsModif: String): Int {
        for (entry in  obj.stats.effects.entries) {
            var statID: Int = entry.key
            if (Integer.toHexString(statID).lowercase().compareTo(statsModif.lowercase()) > 0) {
            } else if (Integer.toHexString(statID).lowercase().compareTo(statsModif.lowercase()) == 0) {
                var statX: Int = 1
                var coef: Int = 1
                var BaseStats: Int = viewBaseStatsItem(obj, Integer.toHexString(statID)).toInt()
                if (BaseStats == 2) {
                    coef = 3
                } else if (BaseStats == 0) {
                    coef = 8
                }
                if (statID == 125 || statID == 158 || statID == 174) {
                    statX = 1
                } else if (statID == 118 || statID == 126 || statID == 119
                        || statID == 123)

                {
                    statX = 2
                } else if (statID == 138 || statID == 666 || statID == 226
                        || statID == 220) // da�os,Trampas
                // %
                {
                    statX = 3
                } else if (statID == 124 || statID == 176) {
                    statX = 5
                } else if (statID == 240 || statID == 241 || statID == 242
                        || statID == 243 || statID == 244)

                {
                    statX = 7
                } else if (statID == 210 || statID == 211 || statID == 212
                        || statID == 213 || statID == 214) {
                    statX = 8
                } else if (statID == 225 || statID == 121) {
                    statX = 15
                } else if (statID == 178 ) {
                    statX = 20
                } else if (statID == 115 || statID == 182) {
                    statX = 30
                } else if (statID == 117) {
                    statX = 50
                } else if (statID == 128) {
                    statX = 90
                } else if (statID == 111) {
                    statX = 100
                }
                var Weight: Int = entry.value * statX * coef
                return Weight
            }
        }
        return 0
    }

    @JvmStatic fun currentStats(obj: GameObject, statsModif: String): Int {
        for (entry in  obj.stats.effects.entries) {
            var statID: Int = entry.key
            if (Integer.toHexString(statID).lowercase().compareTo(statsModif.lowercase()) > 0) {
            } else if (Integer.toHexString(statID).lowercase().compareTo(statsModif.lowercase()) == 0) {
                return entry.value
            }
        }
        return 0
    }

    @JvmStatic fun currentTotalWeigthBase(statsModelo: String, obj: GameObject): Int {
        if (statsModelo.equals("", true))
            return 0
        var Weigth: Int = 0
        var Alto: Int = 0
        var split: Array<String> = statsModelo.split(",").toTypedArray()
        for (s in  split) {
            var stats: Array<String> = s.split("#").toTypedArray()
            var statID: Int = stats[0].toInt(16)
            if (statID == 985 || statID == 988)
                continue
            var xy: Boolean = false
            for (a in  Constant.ARMES_EFFECT_IDS)
                if (a == statID)
                    xy = true
            if (xy)
                continue
            lateinit var jet: String
            var qua: Int
            try {
                jet = stats[4]
                qua = Formulas.getRandomJet(null, null, jet)
                try {
                    var min: Int = stats[1].toInt(16)
                    var max: Int = stats[2].toInt(16)
                    qua = min
                    if (max != 0)
                        qua = max
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    qua = Formulas.getRandomJet(null, null, jet)
                }
            } catch (e: Exception) {
                continue
                // Ok :/
            }
            var statX: Int = 1
            var coef: Int = 1
            var statsBase: Int = viewBaseStatsItem(obj, stats[0]).toInt()
            if (statsBase == 2) {
                coef = 3
            } else if (statsBase == 0) {
                coef = 2
            }
            if (statID == 125 || statID == 158 || statID == 174) {
                statX = 1
            } else if (statID == 118 || statID == 126 || statID == 119
                    || statID == 123) {
                statX = 2
            } else if (statID == 138 || statID == 666 || statID == 226
                    || statID == 220) // de
            // da�os,Trampas %
            {
                statX = 3
            } else if (statID == 124 || statID == 176) {
                statX = 5
            } else if (statID == 240 || statID == 241 || statID == 242
                    || statID == 243 || statID == 244) {
                statX = 7
            } else if (statID == 210 || statID == 211 || statID == 212
                    || statID == 213 || statID == 214)

            {
                statX = 8
            } else if (statID == 225|| statID == 121) {
                statX = 15
            } else if (statID == 178 ) {
                statX = 20
            } else if (statID == 115 || statID == 182) {
                statX = 30
            } else if (statID == 117) {
                statX = 50
            } else if (statID == 128) {
                statX = 90
            } else if (statID == 111) {
                statX = 100
            }
            Weigth = qua * statX * coef
            Alto += Weigth
        }
        return Alto
    }

    @JvmStatic fun getBaseMaxJet(templateID: Int, statsModif: String): Int {
        var t: ObjectTemplate? = World.world.getObjTemplate(templateID)
        var splitted: Array<String> = t!!.strTemplate.split(",").toTypedArray()
        for (s in  splitted) {
            var stats: Array<String> = s.split("#").toTypedArray()
            if (stats[0].compareTo(statsModif) > 0)//Effets n'existe pas de base
            {
            } else if (stats[0].compareTo(statsModif) == 0)//L'effet existe bien !
            {
                var max: Int = stats[2].toInt(16)
                if (max == 0)
                    max = stats[1].toInt(16);//Pas de jet maximum on prend le minimum
                return max
            }
        }
        return 0
    }

    @JvmStatic fun getActualJet(obj: GameObject, statsModif: String): Int {
        for (entry in  obj.stats.effects.entries) {
            if (Integer.toHexString(entry.key).compareTo(statsModif) > 0)//Effets inutiles
            {
            } else if (Integer.toHexString(entry.key).compareTo(statsModif) == 0)//L'effet existe bien !
            {
                var JetActual: Int = entry.value
                return JetActual
            }
        }
        return 0
    }

    @JvmStatic fun viewActualStatsItem(obj: GameObject, stats: String): Byte//retourne vrai si le stats est actuellement sur l'item
    {
        if (!obj.encodeStats().isEmpty()) {
            for (entry in  obj.stats.effects.entries) {
                if (Integer.toHexString(entry.key).compareTo(stats) > 0)//Effets inutiles
                {
                    if (Integer.toHexString(entry.key).compareTo("98") == 0
                            && stats.compareTo("7b") == 0) {
                        return 2
                    } else if (Integer.toHexString(entry.key).compareTo("9a") == 0
                            && stats.compareTo("77") == 0) {
                        return 2
                    } else if (Integer.toHexString(entry.key).compareTo("9b") == 0
                            && stats.compareTo("7e") == 0) {
                        return 2
                    } else if (Integer.toHexString(entry.key).compareTo("9d") == 0
                            && stats.compareTo("76") == 0) {
                        return 2
                    } else if (Integer.toHexString(entry.key).compareTo("74") == 0
                            && stats.compareTo("75") == 0) {
                        return 2
                    } else if (Integer.toHexString(entry.key).compareTo("99") == 0
                            && stats.compareTo("7d") == 0) {
                        return 2
                    } else {
                    }
                } else if (Integer.toHexString(entry.key).compareTo(stats) == 0)//L'effet existe bien !
                {
                    return 1
                }
            }
            return 0
        } else {
            return 0
        }
    }

    @JvmStatic fun viewBaseStatsItem(obj: GameObject, ItemStats: String): Byte//retourne vrai si le stats existe de base sur l'item
    {

        var splitted: Array<String> = obj.template!!.strTemplate.split(",").toTypedArray()
        for (s in  splitted) {
            var stats: Array<String> = s.split("#").toTypedArray()
            if (stats[0].compareTo(ItemStats) > 0)//Effets n'existe pas de base
            {
                if (stats[0].compareTo("98") == 0
                        && ItemStats.compareTo("7b") == 0) {
                    return 2
                } else if (stats[0].compareTo("9a") == 0
                        && ItemStats.compareTo("77") == 0) {
                    return 2
                } else if (stats[0].compareTo("9b") == 0
                        && ItemStats.compareTo("7e") == 0) {
                    return 2
                } else if (stats[0].compareTo("9d") == 0
                        && ItemStats.compareTo("76") == 0) {
                    return 2
                } else if (stats[0].compareTo("74") == 0
                        && ItemStats.compareTo("75") == 0) {
                    return 2
                } else if (stats[0].compareTo("99") == 0
                        && ItemStats.compareTo("7d") == 0) {
                    return 2
                } else {
                }
            } else if (stats[0].compareTo(ItemStats) == 0)//L'effet existe bien !
            {
                return 1
            }
        }
        return 0
    }

    @JvmStatic fun getPwrPerEffet(effect: Int): Double {
        var r: Double = 0.0
        when (effect) {
Constant.STATS_ADD_PA -> {r = 100.0
                
}
Constant.STATS_ADD_PM2 -> {r = 90.0
                
}
Constant.STATS_ADD_VIE -> {r = 0.25
                
}
Constant.STATS_MULTIPLY_DOMMAGE -> {r = 100.0
                
}
Constant.STATS_ADD_CC -> {r = 30.0
                
}
Constant.STATS_ADD_PO -> {r = 51.0
                
}
Constant.STATS_ADD_FORC -> {r = 1.0
                
}
Constant.STATS_ADD_AGIL -> {r = 1.0
                
}
Constant.STATS_ADD_PA2 -> {r = 100.0
                
}
Constant.STATS_ADD_DOMA -> {r = 20.0
                
}
Constant.STATS_ADD_EC -> {r = 1.0
                
}
Constant.STATS_ADD_CHAN -> {r = 1.0
                
}
Constant.STATS_ADD_SAGE -> {r = 3.0
                
}
Constant.STATS_ADD_VITA -> {r = 0.25
                
}
Constant.STATS_ADD_INTE -> {r = 1.0
                
}
Constant.STATS_ADD_PM -> {r = 90.0
                
}
Constant.STATS_ADD_PERDOM -> {r = 2.0
                
}
Constant.STATS_ADD_PDOM -> {r = 2.0
                
}
Constant.STATS_ADD_PODS -> {r = 0.25
                
}
Constant.STATS_ADD_ADODGE -> {r = 1.0
                
}
Constant.STATS_ADD_MDODGE -> {r = 1.0
                
}
Constant.STATS_ADD_INIT -> {r = 0.1
                
}
Constant.STATS_ADD_PROS -> {r = 3.0
                
}
Constant.STATS_ADD_SOIN -> {r = 20.0
                
}
Constant.STATS_SUMMON_COUNT -> {r = 30.0
                
}
Constant.STATS_ADD_RP_TER -> {r = 6.0
                
}
Constant.STATS_ADD_RP_EAU -> {r = 6.0
                
}
Constant.STATS_ADD_RP_AIR -> {r = 6.0
                
}
Constant.STATS_ADD_RP_FEU -> {r = 6.0
                
}
Constant.STATS_ADD_RP_NEU -> {r = 6.0
                
}
Constant.STATS_ADD_TRAP_DOM -> {r = 15.0
                
}
Constant.STATS_ADD_TRAP_PERDOM -> {r = 2.0
                
}
Constant.STATS_ADD_R_FEU -> {r = 2.0
                
}
Constant.STATS_ADD_R_NEU -> {r = 2.0
                
}
Constant.STATS_ADD_R_TER -> {r = 2.0
                
}
Constant.STATS_ADD_R_EAU -> {r = 2.0
                
}
Constant.STATS_ADD_R_AIR -> {r = 2.0
                
}
Constant.STATS_ADD_RP_PVP_TER -> {r = 6.0
                
}
Constant.STATS_ADD_RP_PVP_EAU -> {r = 6.0
                
}
Constant.STATS_ADD_RP_PVP_AIR -> {r = 6.0
                
}
Constant.STATS_ADD_RP_PVP_FEU -> {r = 6.0
                
}
Constant.STATS_ADD_RP_PVP_NEU -> {r = 6.0
                
}
Constant.STATS_ADD_R_PVP_TER -> {r = 2.0
                
}
Constant.STATS_ADD_R_PVP_EAU -> {r = 2.0
                
}
Constant.STATS_ADD_R_PVP_AIR -> {r = 2.0
                
}
Constant.STATS_ADD_R_PVP_FEU -> {r = 2.0
                
}
Constant.STATS_ADD_R_PVP_NEU -> {r = 2.0
                
}
}
        return r
    }

    @JvmStatic fun getOverPerEffet(effect: Int): Double {
        var r: Double = 0.0
        when (effect) {
Constant.STATS_ADD_PA -> {r = 1.0
                
}
Constant.STATS_ADD_PM2 -> {r = 0.0
                
}
Constant.STATS_ADD_VIE -> {r = 404.0
                
}
Constant.STATS_MULTIPLY_DOMMAGE -> {r = 0.0
                
}
Constant.STATS_ADD_CC -> {r = 3.0
                
}
Constant.STATS_ADD_PO -> {r = 0.0
                
}
Constant.STATS_ADD_FORC -> {r = 101.0
                
}
Constant.STATS_ADD_AGIL -> {r = 101.0
                
}
Constant.STATS_ADD_PA2 -> {r = 0.0
                
}
Constant.STATS_ADD_DOMA -> {r = 5.0
                
}
Constant.STATS_ADD_EC -> {r = 0.0
                
}
Constant.STATS_ADD_CHAN -> {r = 101.0
                
}
Constant.STATS_ADD_SAGE -> {r = 33.0
                
}
Constant.STATS_ADD_VITA -> {r = 404.0
                
}
Constant.STATS_ADD_INTE -> {r = 101.0
                
}
Constant.STATS_ADD_PM -> {r = 0.0
                
}
Constant.STATS_ADD_PERDOM -> {r = 50.0
                
}
Constant.STATS_ADD_PDOM -> {r = 50.0
                
}
Constant.STATS_ADD_PODS -> {r = 404.0
                
}
Constant.STATS_ADD_ADODGE -> {r = 0.0
                
}
Constant.STATS_ADD_MDODGE -> {r = 0.0
                
}
Constant.STATS_ADD_INIT -> {r = 1010.0
                
}
Constant.STATS_ADD_PROS -> {r = 33.0
                
}
Constant.STATS_ADD_SOIN -> {r = 5.0
                
}
Constant.STATS_SUMMON_COUNT -> {r = 3.0
                
}
Constant.STATS_ADD_RP_TER -> {r = 16.0
                
}
Constant.STATS_ADD_RP_EAU -> {r = 16.0
                
}
Constant.STATS_ADD_RP_AIR -> {r = 16.0
                
}
Constant.STATS_ADD_RP_FEU -> {r = 16.0
                
}
Constant.STATS_ADD_RP_NEU -> {r = 16.0
                
}
Constant.STATS_ADD_TRAP_DOM -> {r = 6.0
                
}
Constant.STATS_ADD_TRAP_PERDOM -> {r = 50.0
                
}
Constant.STATS_ADD_R_FEU -> {r = 50.0
                
}
Constant.STATS_ADD_R_NEU -> {r = 50.0
                
}
Constant.STATS_ADD_R_TER -> {r = 50.0
                
}
Constant.STATS_ADD_R_EAU -> {r = 50.0
                
}
Constant.STATS_ADD_R_AIR -> {r = 50.0
                
}
Constant.STATS_ADD_RP_PVP_TER -> {r = 16.0
                
}
Constant.STATS_ADD_RP_PVP_EAU -> {r = 16.0
                
}
Constant.STATS_ADD_RP_PVP_AIR -> {r = 16.0
                
}
Constant.STATS_ADD_RP_PVP_FEU -> {r = 16.0
                
}
Constant.STATS_ADD_RP_PVP_NEU -> {r = 16.0
                
}
Constant.STATS_ADD_R_PVP_TER -> {r = 50.0
                
}
Constant.STATS_ADD_R_PVP_EAU -> {r = 50.0
                
}
Constant.STATS_ADD_R_PVP_AIR -> {r = 50.0
                
}
Constant.STATS_ADD_R_PVP_FEU -> {r = 50.0
                
}
Constant.STATS_ADD_R_PVP_NEU -> {r = 50.0
                
}
}
        return r
    }
    }

    //endregion
    /* *********************/

    //region Old craft with new formulas
    private @Synchronized fun craftMaging1(isReapeat: Boolean, repeat: Int) {
        var gameObject: GameObject? = null
        var runeObject: GameObject? = null
        var potionObject: GameObject? = null
        var signingObject: GameObject? = null

        //region Vérification de craft
        /* Type : 26 = potion pour les cac
           Type : 78 = rune
           Signature : Type 50 ou Id 7508 */

        for(id in  this.ingredients.keys) {
            var `object`: GameObject? = World.world.getGameObject(id)
            var type: Int = `object`!!.template!!.type

            if(gameObject == null && this.isAvailableObject(this.getJobStat()!!.template!!.id, type)) {
                gameObject = `object`
            } else if(runeObject == null && type == 78)
                runeObject = `object`
            else if(potionObject == null && type == 26)
                potionObject = `object`
            else if(signingObject == null && `object`!!.template!!.id == 7508)
                signingObject = `object`
        }

        if(gameObject == null || (runeObject == null && potionObject == null)) {
            GameClient.leaveExchange(this.player!!)
            return
        }
        if(this.analyzeObject(gameObject)) {
            this.player!!.sendMessage("Impossible d'FM ce type d'objet pour le moment (avec faiblesses)")
            return
        }
        //endregion Vérification de craft

        /* Poids max : 100 si > EC à 100%
           EXO : Si ça dépasse la valeur de la stats originale ou si elle n'existe pas */
        if(runeObject != null) {
            var runeTemplate: Rune? = Rune.getRuneById(runeObject!!.template!!.id) // On trouve le template de la rune qu'on souhaite appliqué à l'item

            if (runeTemplate == null) { // Si elle n'existe pas..
                //Ne devrait pas arriver.
                return
            }

            //region Initialisation des variables principales
            var originalSplitStats: Array<String> = gameObject!!.template!!.strTemplate.split(",").toTypedArray()
            var actualObjectSplitStats: Array<String> = gameObject!!.encodeStats().split(",").toTypedArray() // Liste toutes les stats originale de l'objet

            var concernedOriginalJet: String? = null
            var concernedActualJet: String? = null // Jet originale concerner
            var PWRGmin: Float = 0f
            var PWRGactual: Float = 0f
            var PWRGmax: Float = 0f

            for (jet in  originalSplitStats) { // On fait une iteration de chaque ligne de l'objet originale
                if(jet.isEmpty()) continue
                var id: Int = jet.split("#")[0].toInt(16)

                if (id == runeTemplate.characteristic.toInt()) // Si l'ID de la stats est égale à l'ID de la stats de la rune
                    concernedOriginalJet = jet; // On met la ligne concerner a jour
            }

            var PWRexotique: Int = 0

            for (jet in  actualObjectSplitStats) { // On fait une iteration de chaque ligne de l'objet actuel
                if (jet.isEmpty()) continue
                var id: Int = jet.split("#")[0].toInt(16)

                if (id == Constant.STATS_OWNER_1 || id == Constant.STATS_CHANGE_BY || id == Constant.STATS_BUILD_BY) continue

                var rune: Rune? = Rune.getRuneByCharacteristicAndByWeight(id.toShort())
                if (rune != null) PWRGactual += this.getPWR(rune!!, jet, 1.toByte())

                if (id == runeTemplate.characteristic.toInt()) { // Si l'ID de la stats est égale à l'ID de la stats de la rune
                    concernedActualJet = gameObject!!.stats.getEffect(jet.split("#")[0].toInt(16)).toString(); // On met la ligne concerner a jour
                }

                var exist: Boolean = false
                for(jet2 in  originalSplitStats) {
                    if(jet2.isEmpty()) continue
                    var id2: Int = jet2.split("#")[0].toInt(16)
                    if(id == id2) {
                        exist = true
                        break
                    }
                }

                if(!exist) {
                    PWRexotique = (PWRexotique + this.getPWR(rune!!, jet, 1.toByte())).toInt()
                }
            }

            var actualJet: Short = if (concernedActualJet == null) 0 else concernedActualJet.toShort()
            var minJet: Short = if (concernedOriginalJet == null) 0 else Formulas.getMinJet(concernedOriginalJet.split("#")[4]).toShort()
            var maxJet: Short = if (concernedOriginalJet == null) 1 else Formulas.getMaxJet(concernedOriginalJet.split("#")[4]).toShort()
            //endregion Initialisation des variables principales

            //region Début des calculs des PWR & PWRG
            var PWGRune: Float = runeTemplate.weight
            var PWRRune: Float = PWGRune / runeTemplate.bonus
            var PWRactual: Float = actualJet * PWRRune
            var PWRmax: Float = maxJet * PWRRune

            for (jet in  originalSplitStats) {
                var id: Int = jet.split("#")[0].toInt(16)
                var rune: Rune? = Rune.getRuneByCharacteristicAndByWeight(id.toShort())

                if(rune == null) continue

                PWRGmin += this.getPWR(rune!!, jet, 0.toByte())
                PWRGmax += this.getPWR(rune!!, jet, 2.toByte())
            }
            //endregion Début des calculs des PWR & PWRG

            //region Réussite normal
            var factorJet: Byte = 47
            var factorObject: Byte = 50
            var successLevel: Byte = 5
                      var EtatJet: Float = if ((maxJet - minJet) <= 0)
                    0f else
                    (((actualJet + runeTemplate.bonus) - minJet) * 100f) / (maxJet - minJet)
            if(EtatJet < 60f)
                EtatJet = 60f

            var EtatObjet: Float = if ((PWRGmax - PWRGmin) <= 0)
                    0f else
                    Math.ceil(((PWRGactual - PWRGmin) * 100.0 / (PWRGmax - PWRGmin))).toFloat()
            if(EtatObjet < 15f)
                EtatObjet = 15f

            var successJet: Float = 1f
            var successObject: Float = 0f
            var criticSuccess: Byte = 1
            var neutralSuccess: Byte = 50
            var criticFail: Byte = 1

            if(concernedOriginalJet == null) {
                // CAS EXOTIQUE
                if(PWGRune < 50f) {
                    factorJet = 40
                    factorObject = 54
                    successLevel = 5
                    EtatJet = 100f
                }

                if(PWGRune <= 3f && (actualJet / maxJet) * 100 > 65 && PWRRune == 1f)
                    EtatJet = 150f

                EtatObjet = Math.ceil(((15 + PWRactual + PWGRune * 3).toDouble()).toDouble()).toFloat()

                if (EtatJet >= 80f)
                    successJet = factorJet * EtatJet / 100

                successObject = factorObject * EtatObjet / 100

                criticSuccess = (Math.ceil((100 - (successJet + successObject + successLevel)).toDouble()).toInt().toByte())
                criticSuccess = if (criticSuccess.toInt() < 0) 0 else criticSuccess

                if (criticSuccess.toInt() > 50)
                    neutralSuccess = ((100 - criticSuccess).toByte())
                else if (criticSuccess.toInt() < 25)
                    neutralSuccess = ((50 - (40 - criticSuccess)).toByte())

                criticFail = ((100 - (neutralSuccess + criticSuccess)).toByte())

                if(PWGRune > 50f) {
                    // Pa/Pm/Po
                    criticSuccess = 1
                    neutralSuccess = 0
                    criticFail = 99
                }
                if(PWRexotique >= 101f) {
                    criticSuccess = 0
                    neutralSuccess = 0
                    criticFail = 100
                }
            } else if(PWRactual + PWGRune > PWRmax && PWRactual + PWGRune < 101f) {
                // CAS OVERMAX
                factorJet = 60
                factorObject = 54
                successLevel = 5
                EtatJet = 100f

                if(PWGRune <= 3f && (actualJet / maxJet) * 100 > 65 && PWRRune == 1f)
                    EtatJet = 150f
                if(PWGRune <= 3f && (actualJet / maxJet) * 100 > 80 && PWRRune == 1f)
                    EtatJet = 300f
                if(PWGRune <= 3f && (actualJet / maxJet) * 100 > 85 && PWRRune == 3f)
                    EtatJet = 200f

                if (EtatJet >= 80f)
                    successJet = factorJet * EtatJet / 100

                if (EtatObjet >= 50f)
                    successObject = factorObject * EtatObjet / 100
                else
                    successObject = EtatObjet

                criticSuccess = (Math.ceil((100 - (successJet + successObject + successLevel)).toDouble()).toInt().toByte())
                criticSuccess = if (criticSuccess.toInt() < 0) 0 else criticSuccess

                if (criticSuccess.toInt() > 50)
                    neutralSuccess = ((100 - criticSuccess).toByte())
                else if (criticSuccess.toInt() < 25)
                    neutralSuccess = ((50 - (40 - criticSuccess)).toByte())

                criticFail = ((100 - (neutralSuccess + criticSuccess)).toByte())

                if(criticSuccess.toInt() > 25) {
                    criticSuccess = 25
                    neutralSuccess = 25
                    criticFail = 50
                }
                if(criticSuccess.toInt() <= 1) {
                    criticSuccess = 1
                    neutralSuccess = 22
                    criticFail = 77
                }
            } else {
                // CAS NORMAL
                if(PWGRune <= 3f && (actualJet / maxJet) * 100 > 65 && PWRRune == 1f)
                    EtatJet = 150f
                if(PWGRune <= 3f && (actualJet / maxJet) * 100 > 80 && PWRRune == 1f)
                    EtatJet = 300f
                if(PWGRune <= 3f && (actualJet / maxJet) * 100 > 85 && PWRRune == 3f)
                    EtatJet = 200f


                if (EtatJet >= 52f)
                    successJet = factorJet * EtatJet / 100
                else
                    successJet = EtatJet / 4

                if (EtatObjet >= 50f)
                    successObject = factorObject * EtatObjet / 100
                else
                    successObject = EtatObjet

                criticSuccess = (Math.ceil((100 - (successJet + successObject + successLevel)).toDouble()).toInt().toByte())
                criticSuccess = if (criticSuccess.toInt() < 0) 0 else criticSuccess

                if (criticSuccess.toInt() > 50)
                    neutralSuccess = ((100 - criticSuccess).toByte())
                else if (criticSuccess.toInt() < 25)
                    neutralSuccess = ((50 - (40 - criticSuccess)).toByte())

                criticFail = ((100 - (neutralSuccess + criticSuccess)).toByte())

                if(criticSuccess.toInt() < 15) {
                    criticSuccess = 15
                    neutralSuccess = 50
                    criticFail = 35
                }
            }
            if(PWRactual + PWGRune > PWRmax && PWRactual + PWGRune >= 101f) {
                criticSuccess = 0
                neutralSuccess = 0
                criticFail = 100
            }
            //endregion

            var randomStats: RandomStats<Byte> = RandomStats()
            randomStats.add(criticSuccess.toInt(), 0.toByte())
            randomStats.add(neutralSuccess.toInt(), 1.toByte())
            randomStats.add(criticFail.toInt(), 2.toByte())
            var result: Byte = randomStats.get()

            if(this.player!!.getGroup() != null) {
                this.player!!.sendMessage("PWRGmin à max : " + PWRGmin.toString() + " | " + PWRGmax.toString() + " | " + PWRGactual)
                this.player!!.sendMessage("FO: " + factorObject.toString() + " EB: " + EtatObjet.toString() + " | FJ: " + factorJet.toString() + " | EJ: " + EtatJet)
                this.player!!.sendMessage("SC: " + criticSuccess.toString() + " | SN: " + neutralSuccess.toString() + " | EC: " + criticFail.toString() + " | R: " + result)
            }
            //region success critique
            if (result.toInt() == 0) {
                var newQuantity: Int = this.ingredients[runeObject!!.guid]!! - 1
                this.player!!.removeItemByTemplateId(runeObject!!.template!!.id, 1, false)

                var winXP: Int = Formulas.calculXpWinFm(gameObject!!.template!!.level, (Math.floor(runeTemplate.weight.toDouble()).toInt())) * Config.rateJob
                if (winXP > 0) this.SM!!.addXp(this.player!!, winXP.toLong())
                this.player!!.send("JX|" + this.SM!!.template!!.id.toString() + ";" + this.SM!!.get_lvl().toString() + ";" + this.SM!!.getXpString(";").toString() + ";")

                var newObject: GameObject? = gameObject!!.getClone(1,true)
                this.player!!.removeItem(gameObject!!.guid, 1, true, gameObject!!.quantity == 1)

                if (signingObject != null) {
                    this.player!!.removeItemByTemplateId(signingObject!!.template!!.id, 1, false)
                    if (985 in newObject!!.txtStat)
                        newObject!!.txtStat.remove(985)
                    newObject!!.addTxtStat(985, this.player!!.name)
                }

                newObject!!.stats.addOneStat(runeTemplate.characteristic.toInt(), runeTemplate.bonus.toInt())

                if (this.player!!.addItem(newObject, false, false))
                    World.world.addGameObject(newObject)

                SocketManager.GAME_SEND_Ow_PACKET(this.player!!)

                this.player!!.send("EmKO.toString() + " + newObject!!.guid.toString() + "|1|" + newObject!!.template!!.id.toString() + "|" + newObject!!.encodeStats())
                this.player!!.send("IO" + this.player!!.id.toString() + "|+" + newObject!!.template!!.id); // Icon tête joueur :  +/-
                this.player!!.send("EcK;" + newObject!!.template!!.id);//Vous avez crée...

                this.ingredients.clear()
                this.player!!.send("EMKO.toString() + " + newObject!!.guid.toString() + "|1")
                this.ingredients[newObject!!.guid] = 1

                if (newQuantity >= 1) {
                    this.player!!.send("EMKO.toString() + " + runeObject!!.guid.toString() + "|" + newQuantity)
                    this.ingredients[runeObject!!.guid] = newQuantity
                }

                this.oldJobCraft = this.jobCraft
                if (!isReapeat) this.jobCraft = null
                return
            }
            //endregion

            var puit: Int = gameObject!!.puit
            var PWGLoose: Float = PWGRune
            if(this.player!!.getGroup() != null)
                this.player!!.sendMessage("Puit before : " + puit)

            if (puit > 0)
                puit = Math.round(puit - PWGLoose)
            if (puit < 0) {
                PWGLoose = -puit.toFloat()
                puit = 0
            }
            if (puit > 0)
                puit = Math.round(puit - PWGLoose)

            if(this.player!!.getGroup() != null)
                this.player!!.sendMessage("Puit after : " + puit)
            var cancel: Boolean = false
            //region Succès neutre
            if(result.toInt() == 1) {
                if(actualObjectSplitStats.size == 1 && (actualObjectSplitStats[0].isEmpty() || actualObjectSplitStats[0].split("#")[0].toInt(16) == runeTemplate.characteristic.toInt()))
                    cancel = true

                var winXP: Int = Formulas.calculXpWinFm(gameObject!!.template!!.level, (Math.floor(runeTemplate.weight.toDouble()).toInt())) * Config.rateJob
                if (winXP > 0) this.SM!!.addXp(this.player!!, winXP.toLong())

                if(!cancel) {
                    var blacklist: MutableList<Short> = ArrayList()
                    var stats: List<String> = getStatsToLoose(runeTemplate, actualObjectSplitStats, originalSplitStats, blacklist)
                    var brokeJet: Int

                    while (PWGLoose > 0 && !stats.isEmpty()) {
                        var jet: String = stats[Formulas.random.nextInt(stats.size)]
                        var id: Int = jet.split("#")[0].toInt(16)
                        if (id == Constant.STATS_OWNER_1 || id == Constant.STATS_CHANGE_BY || id == Constant.STATS_BUILD_BY) continue
                        var rune: Rune? = Rune.getRuneByCharacteristicAndByWeight(id.toShort())
                        var PWRJetRune: Float = rune!!.weight * rune!!.bonus
                        var PWRGJet: Float = this.getPWR(rune!!, jet, 1.toByte())

                        if (PWGRune > 50f) {
                            brokeJet = Math.round(10 + ((PWGRune * 20) / PWRJetRune))
                        } else if (PWGRune >= 10f && PWGRune <= 50f) {
                            brokeJet = Math.round(10 + ((PWGRune * 50) / PWRJetRune))
                        } else {
                            brokeJet = Math.round(10 + ((PWGRune * 100) / PWRJetRune))
                        }

                        var random: Byte = (Formulas.getRandomValue(1, 100).toByte())
                        if (random > brokeJet) {
                            blacklist.add(id.toShort())
                        } else {
                            var puitLoose: Int = Formulas.getRandomValue(1, ((Math.ceil((PWGLoose / PWRJetRune).toDouble())).toInt()))
                            var old: Int = gameObject!!.stats[id.toInt()]
                            var value: Int = gameObject!!.stats!!.addOneStat(id.toInt(), -puitLoose)
                            old = old - puitLoose

                            PWGLoose = PWGLoose - (PWRGJet - (rune!!.weight * value))
                            if(old < 0) PWGLoose += -old * rune!!.weight
                        }
                        actualObjectSplitStats = gameObject!!.encodeStats().split(",").toTypedArray()
                        stats = getStatsToLoose(runeTemplate, actualObjectSplitStats, originalSplitStats, blacklist)
                    }

                    if(this.player!!.getGroup() != null)
                        this.player!!.sendMessage("Puit remove PWGLoose : " + PWGLoose)
                    puit = -(Math.ceil((PWGLoose).toDouble()).toInt())
                }
            }
            //endregion

            //region Echec critique
            if(result.toInt() == 2) {
                var stats: List<String> = getStatsToLoose(runeTemplate, actualObjectSplitStats, originalSplitStats, null)

                while(PWGLoose > 0 && !stats.isEmpty()) {
                    var jet: String = stats[Formulas.random.nextInt(stats.size)]
                    var id: Int = jet.split("#")[0].toInt(16)
                    if (id == Constant.STATS_OWNER_1 || id == Constant.STATS_CHANGE_BY || id == Constant.STATS_BUILD_BY) continue
                    var rune: Rune? = Rune.getRuneByCharacteristicAndByWeight(id.toShort())
                    var PWRJetRune: Float = rune!!.weight * rune!!.bonus
                    var PWRGJet: Float = this.getPWR(rune!!, jet, 1.toByte())

                    var puitLoose: Int = Formulas.getRandomValue(1, ((Math.ceil((PWGLoose / PWRJetRune).toDouble())).toInt()))
                    var old: Int = gameObject!!.stats[id.toInt()]
                    var value: Int = gameObject!!.stats!!.addOneStat(id.toInt(), -puitLoose)
                    old = old - puitLoose

                    PWGLoose = PWGLoose - (PWRGJet - (rune!!.weight * value))
                    if(old < 0) PWGLoose += -old * rune!!.weight
                    actualObjectSplitStats = gameObject!!.encodeStats().split(",").toTypedArray()
                    stats = getStatsToLoose(runeTemplate, actualObjectSplitStats, originalSplitStats, null)
                }

                if(this.player!!.getGroup() != null)
                    this.player!!.sendMessage("Puit remove PWGLoose : " + PWGLoose)
                puit = - (Math.ceil((PWGLoose).toDouble()).toInt())
            }
            //endregion

            var newQuantity: Int = this.ingredients[runeObject!!.guid]!! - 1
            this.player!!.removeItemByTemplateId(runeObject!!.template!!.id, 1, false)

            var newObject: GameObject? = gameObject!!.getClone(1, true)

            if(puit < 0) puit = 0
            newObject!!.puit = puit
            if(this.player!!.getGroup() != null)
                this.player!!.sendMessage("Puit finish : " + puit)


            if (signingObject != null)
                this.player!!.removeItemByTemplateId(signingObject!!.template!!.id, 1, false)

            if(result.toInt() == 1) { // succes neutre
                if (signingObject != null) {
                    if (985 in newObject!!.txtStat)
                        newObject!!.txtStat.remove(985)
                    newObject!!.addTxtStat(985, this.player!!.name)
                }

                if(!cancel)
                    newObject!!.stats.addOneStat(runeTemplate.characteristic.toInt(), runeTemplate.bonus.toInt())
                this.player!!.send("Im0194");//La magie n\'a pas parfaitement fonctionné..
            } else {
                this.player!!.send("Im0117");//La magie n'opère pas..
            }

            this.player!!.removeItem(gameObject!!.guid, 1, true, true)
            if(this.player!!.addItem(newObject, false, false))
                World.world.addGameObject(newObject)

            SocketManager.GAME_SEND_Ow_PACKET(this.player!!)

            this.player!!.send("EmKO.toString() + " + newObject!!.guid.toString() + "|1|" + newObject!!.template!!.id.toString() + "|" + newObject!!.encodeStats())

            this.player!!.send("IO" + this.player!!.id.toString() + "|-" + newObject!!.template!!.id); // Icon tête joueur :  +/-

            this.player!!.send("EMKO-" + gameObject!!.guid.toString() + "|1")
            this.ingredients.clear()

            this.player!!.send("EMKO.toString() + " + newObject!!.guid.toString() + "|1")
            this.ingredients[newObject!!.guid] = 1


            if (newQuantity >= 1) {
                this.player!!.send("EMKO.toString() + " + runeObject!!.guid.toString() + "|" + newQuantity)
                this.ingredients[runeObject!!.guid] = newQuantity
            } else {
                this.player!!.send("EMKO-" + runeObject!!.guid)
            }

            this.oldJobCraft = this.jobCraft
            if (!isReapeat) this.jobCraft = null
        } else if(potionObject != null) {

        }
        //endregion
    }

    private fun analyzeObject(gameObject: GameObject): Boolean {
        for(stat in  gameObject!!.stats.effects.entries) {
            when (stat.key) {
Constant.STATS_REM_PA, Constant.STATS_REM_PM, Constant.STATS_REM_AGIL, Constant.STATS_REM_CHAN, Constant.STATS_REM_FORC, Constant.STATS_REM_INTE, Constant.STATS_REM_SAGE, Constant.STATS_REM_VITA, Constant.STATS_REM_PO, Constant.STATS_REM_PA2, Constant.STATS_REM_PROS, Constant.STATS_REM_AFLEE, Constant.STATS_REM_MFLEE, Constant.STATS_REM_DOMA, Constant.STATS_REM_INIT, Constant.STATS_REM_PM2, Constant.STATS_REM_PODS, Constant.STATS_REM_R_AIR, Constant.STATS_REM_R_FEU, Constant.STATS_REM_R_TER, Constant.STATS_REM_R_EAU, Constant.STATS_REM_RP_AIR, Constant.STATS_REM_RP_FEU, Constant.STATS_REM_RP_TER, Constant.STATS_REM_RP_EAU, Constant.STATS_REM_R_NEU, Constant.STATS_REM_RP_NEU, Constant.STATS_REM_RP_PVP_AIR, Constant.STATS_REM_RP_PVP_FEU, Constant.STATS_REM_RP_PVP_TER, Constant.STATS_REM_RP_PVP_EAU, Constant.STATS_REM_RP_PVP_NEU, Constant.STATS_REM_SOIN, Constant.STATS_REM_CC -> {return true
            
}
}
        }
        return false
    }

    private fun getStatsToLoose(fm: Rune, actualStats: Array<String>, originalStats: Array<String>, blacklist: List<Short>?): List<String> {
        var high: MutableList<String> = ArrayList()
        var low: MutableList<String> = ArrayList()
        for(s1 in  actualStats) {
            if(s1.isEmpty())
                continue

            var id1: Short = s1.split("#")[0].toShort(16)
            if (id1 == Constant.STATS_CHANGE_BY.toShort() || id1 == Constant.STATS_BUILD_BY.toShort()) continue
            var r1: Rune? = Rune.getRuneByCharacteristic(id1.toShort())

            if(r1 == null || blacklist != null && (r1.characteristic == fm.characteristic || blacklist.stream().filter({ i -> i == id1.toShort() }).count() == 1L))
                continue
            var exist: Boolean = false
            var overmax: Boolean = false

            for (s2 in  originalStats) {
                if(s2.isEmpty())
                    continue

                var id2: Short = s2.split("#")[0].toShort(16)
                if (id2 == Constant.STATS_CHANGE_BY.toShort() || id2 == Constant.STATS_BUILD_BY.toShort()) continue
                var r2: Rune? = Rune.getRuneByCharacteristic(id1.toShort())

                if(id1 == id2) {
                    exist = true
                    var pwr1: Float = this.getPWR(r1!!, s1, 1.toByte())
                    var pwr2: Float = this.getPWR(r2!!, s2, 2.toByte())
                    if(pwr1 > pwr2)
                        overmax = true
                }
            }

            if(!exist || overmax) high.add(s1)
            else low.add(s1)
        }
        return if (high.size > 0) high else low
    }

    private fun getPWR(rune: Rune?, jet: String, type: Byte): Float {
        var weight: Float = if (rune == null) 1f else Rune.getRuneByCharacteristicAndByWeight(rune!!.characteristic)!!.weight
        log.debug("getPWR = Weight: " + weight.toString() + " | Type: " + type.toString() + " | Jet: " + jet)
        when (type.toInt()) {
0 -> {return weight * Formulas.getMinJet(jet.split("#")[4])
}
1 -> {return weight * jet.split("+").let { it[it.size - 1] }.toShort().toFloat()
}
2 -> {return weight * Formulas.getMaxJet(jet.split("#")[4])
        
}
}
        return 0f
    }

    private fun isAvailableObject(jobId: Int, type: Int): Boolean {
        when (jobId) {
62 -> {return type == 10 || type == 11
}
63 -> {return type == 1 || type == 9
}
64 -> {return type == 16 || type == 17 || type == Constant.ITEM_TYPE_SAC_DOS
}
43 -> {return type == 5
}
44 -> {return type == 6
}
45 -> {return type == 7
}
46 -> {return type == 8
}
47 -> {return type == 19
}
48 -> {return type == 2
}
49 -> {return type == 3
}
50 -> {return type == 4
        
}
}
        return false
    }
    //endregion Old craft with new formulas
}