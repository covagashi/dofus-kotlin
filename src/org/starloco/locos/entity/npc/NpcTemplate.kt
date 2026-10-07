package org.starloco.locos.entity.npc

import org.classdump.luna.Conversions
import org.classdump.luna.Table
import org.classdump.luna.impl.DefaultTable
import org.starloco.locos.client.Player
import org.starloco.locos.database.data.game.SaleOffer
import org.starloco.locos.database.data.game.SaleOffer.Currency
import org.starloco.locos.game.world.World
import org.starloco.locos.game.world.World.Couple
import org.starloco.locos.`object`.ObjectTemplate
import org.starloco.locos.script.DataScriptVM
import org.starloco.locos.script.ScriptVM
import java.util.Objects
import java.util.stream.Collectors
import org.slf4j.LoggerFactory
import org.starloco.locos.common.splitJ

private val log = LoggerFactory.getLogger(NpcTemplate::class.java)

class NpcTemplate(v: Table) {
    val id: Int
    val gfxId: Int
    val scaleX: Int
    val scaleY: Int
    val sex: Int
    val color1: Int
    val color2: Int
    val color3: Int
    private val accessories: IntArray
    val customArtWork: Int
    // private Quest quest;
    private val scriptVal: Table?
    val flags: Byte

    @JvmField
    val legacy: LegacyData?

    init {
        this.scriptVal = v
        this.legacy = null

        this.id = ScriptVM.rawInt(v, "id")
        this.gfxId = ScriptVM.rawInt(v, "gfxID")
        this.sex = ScriptVM.rawInt(v, "gender")
        this.scaleX = ScriptVM.rawInt(v, "scaleX")
        this.scaleY = ScriptVM.rawInt(v, "scaleY")
        val colors = v.rawget("colors") as Table
        this.color1 = ScriptVM.rawInt(colors, 1)
        this.color2 = ScriptVM.rawInt(colors, 2)
        this.color3 = ScriptVM.rawInt(colors, 3)
        val accessories = v.rawget("accessories") as Table
        this.accessories = ScriptVM.intArrayFromLuaTable(accessories)!!
        this.customArtWork = ScriptVM.rawInt(v, "customArtwork")
        this.flags = ScriptVM.rawInt(v, "flags").toByte()
    }

    fun encodeAccessories(): String {
        return accessories.joinToString(",") { Integer.toHexString(it) }
    }

    fun isBankClerk(): Boolean {
        return when (id) {
            100, 520, 522, 691, 692 -> true
            else -> false
        }
    }

    fun onCreateDialog(player: Player) {
        this.onDialog(player, 0, 0)
    }

    fun onDialog(player: Player, question: Int, response: Int) {
        Objects.requireNonNull(scriptVal)
        DataScriptVM.getInstance()!!.handlers.onDialog(player, this.id, response)
    }

    fun salesList(player: Player): List<SaleOffer> {
        if (scriptVal == null) {
            return legacy!!.sales
        }
        val salesList = ScriptVM.recursiveGet(scriptVal, "salesList") ?: return emptyList()

        val ret = DataScriptVM.getInstance()!!.call(salesList, scriptVal, player.scripted())

        if (ret == null || ret.isEmpty()) return emptyList()
        val offers = ScriptVM.listFromLuaTable<Any>(ret[0] as Table)

        return offers.stream().map { o ->
            val t = o as Table
            val itemID = ScriptVM.rawInt(t, "item")
            val item = World.world.getObjTemplate(itemID)
                ?: throw IllegalArgumentException(("unknown item template #%d").format( itemID))

            val price = ScriptVM.rawOptionalInt(t, "price", item.price)
            var currencyID = ScriptVM.rawOptionalInt(t, "currency", 0)

            // Not specified defaults to kamas
            if (currencyID == 0) currencyID = -1 // currency -1 -> Kamas

            val currency: Currency =
                if (currencyID < 0) Currency.nonItemCurrency(-currencyID) else Currency.itemCurrency(currencyID)

            SaleOffer(item, price.toLong(), currency)
        }.collect(Collectors.toList())
    }

    fun getExtraClip(player: Player): Int {
        if (this.legacy != null) {
            return -1
        }

        val extraClip = ScriptVM.recursiveGet(scriptVal, "extraClip") ?: return -1

        val ret = DataScriptVM.getInstance()!!.call(extraClip, scriptVal, player.scripted())
        if (ret == null || ret.isEmpty() || ret[0] == null) return -1
        if (ret.size > 1) throw RuntimeException(("unexpected count(%d) in extraClip").format( ret.size))

        return Conversions.integerValueOf(ret[0]).toInt()
    }

    fun barterOutcome(player: Player, objects: List<Couple<Int, Int>>): Couple<Int, Int>? {
        if (this.legacy != null) {
            val out = this.legacy.checkGetObjects(objects)!!
            if (out.size != 1) throw RuntimeException(("unexpected count(%d) in legacy barterOutcome").format( out.size))
            return out[0]
        }

        val barterOutcome = ScriptVM.recursiveGet(scriptVal, "barterOutcome") ?: return null


        val offer = DefaultTable()
        for (i in objects.indices) {
            val stack = ScriptVM.ItemStack(objects[i])
            offer.rawset(i + 1, stack)
        }

        val ret = DataScriptVM.getInstance()!!.call(barterOutcome, scriptVal, player.scripted(), offer)
        if (ret == null || ret.isEmpty() || ret[0] == null) return null
        if (ret.size > 1) throw RuntimeException(("unexpected count(%d) in barterOutcome").format( ret.size))

        return ScriptVM.ItemStackFromLua(ret[0] as Table)
    }

    class LegacyData(npcID: Int, questions: String, sales: String, exchanges: String, path: String) {
        val path: String = path
        private val initQuestions = HashMap<Int, Int>()
        val sales = ArrayList<SaleOffer>()
        private var exchanges: List<Couple<ArrayList<Couple<Int, Int>>, ArrayList<Couple<Int, Int>>>>? = null

        init {
            if (questions.splitJ("|").size > 1) {
                for (question in questions.splitJ("|")) {
                    try {
                        initQuestions[question.split(",")[0].toInt()] = question.split(",")[1].toInt()
                    } catch (e: Exception) {
                        log.error("unexpected error", e)
                World.world.logger.error("#1# Erreur sur une question id sur le PNJ d'id : $npcID")
                    }
                }
            } else {
                if (questions.equals("", ignoreCase = true)) this.initQuestions[-1] = -1
                else this.initQuestions[-1] = questions.toInt()
            }

            if (sales != "") {
                for (obj in sales.splitJ(",")) {
                    try {
                        val template = World.world.getObjTemplate(obj.toInt())
                        if (template != null)
                            this.sales.add(SaleOffer(template, template.price.toLong()))
                    } catch (e: NumberFormatException) {
                        log.error("unexpected error", e)
                World.world.logger.error("#2# Erreur sur un item en vente sur le PNJ d'id : $npcID")
                    }
                }
            }

            if (exchanges != "") {
                try {
                    val ex = ArrayList<Couple<ArrayList<Couple<Int, Int>>, ArrayList<Couple<Int, Int>>>>()
                    this.exchanges = ex
                    for (data in exchanges.splitJ("~")) {
                        val gives = ArrayList<Couple<Int, Int>>()
                        val gets = ArrayList<Couple<Int, Int>>()

                        var split = data.split("|")
                        val give = split[1]
                        val get = split[0]

                        for (obj in give.splitJ(",")) {
                            split = obj.split(":")
                            gives.add(Couple(split[0].toInt(), split[1].toInt()))
                        }

                        for (obj in get.splitJ(",")) {
                            split = obj.split(":")
                            gets.add(Couple(split[0].toInt(), split[1].toInt()))
                        }
                        ex.add(Couple(gets, gives))
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                World.world.logger.error("#3# Erreur sur l'exchanges sur le PNJ d'id : $npcID")
                }
            }
        }

        fun getInitQuestionId(id: Int): Int {
            if (initQuestions[id] == null) {
                // Just return the 1st one
                for (entry in initQuestions.values) return entry
            }
            return initQuestions[id]!!
        }

        val allItem: List<ObjectTemplate>
            get() = sales.stream().map { it.itemTemplate }.collect(Collectors.toList())

        fun addItemVendor(template: ObjectTemplate): Boolean {
            if (sales.stream().anyMatch { it.itemTemplate === template })
                return false
            sales.add(SaleOffer(template))
            return true
        }

        fun haveItem(id: Int): Boolean {
            return sales.stream().anyMatch { it.itemTemplate.id == id }
        }


        fun checkGetObjects(objects: List<Couple<Int, Int>>): ArrayList<Couple<Int, Int>>? {
            if (this.exchanges == null) return null
            var ok: Boolean
            var multiple = 0
            var newMultiple = 0

            for (entry0 in this.exchanges!!) {
                ok = true
                for (entry1 in entry0.first!!) {
                    var ok1 = false

                    for (entry2 in objects) {
                        if (entry1.first == entry2.first && entry2.second!! % entry1.second!! == 0) {
                            ok1 = true
                            newMultiple = entry2.second!! / entry1.second!!

                            if (multiple == 0 || newMultiple == multiple) {
                                multiple = newMultiple
                            } else {
                                ok1 = false
                            }
                        }
                    }

                    if (!ok1) {
                        ok = false
                        break
                    }
                }

                val fMultiple = multiple

                if (ok && objects.size == entry0.first!!.size) {
                    if (fMultiple != 1) {
                        return entry0.second!!.stream().map { give -> Couple(give.first, give.second!! * fMultiple) }
                            .collect(Collectors.toCollection { ArrayList() })
                    } else {
                        return entry0.second
                    }
                } else {
                    newMultiple = 0
                    multiple = newMultiple
                }
            }
            return null
        }
    }
}
