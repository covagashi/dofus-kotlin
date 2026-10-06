package org.starloco.locos.`object`

import org.starloco.locos.client.Player
import org.starloco.locos.client.other.Stats
import org.starloco.locos.common.Formulas
import org.starloco.locos.common.SocketManager
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.login.ObjectData
import org.starloco.locos.entity.mount.Mount
import org.starloco.locos.entity.pet.PetEntry
import org.starloco.locos.fight.spells.SpellEffect
import org.starloco.locos.game.world.World
import org.starloco.locos.job.JobAction
import org.starloco.locos.kernel.Constant
import org.starloco.locos.kernel.Logging
import org.starloco.locos.`object`.entity.Fragment
import org.starloco.locos.script.proxy.SItem
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import kotlin.math.floor
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(GameObject::class.java)

open class GameObject {
    val scriptVal: SItem = SItem(this)

    var template: ObjectTemplate? = null
        get
    var quantity = 1
        set(quantity) {
            var qua = quantity
            if (qua <= 0)
                qua = 0
            else if (qua >= 100000)
                if (Logging.USE_LOG)
                    Logging.getInstance().write("Object", "Faille : Objet guid : " + guid + " a dépassé 100 000 qua (" + qua + ") avec comme template : " + template!!.name + " (" + template!!.id + ")")
            field = qua
        }
    var position = Constant.ITEM_POS_NO_EQUIPED
    var guid = 0
        private set
    var obvijevanPos = 0
    var obvijevanLook = 0
    var puit = 0
    var stats = Stats()
    val spellStats = ArrayList<String>()
    var effects = ArrayList<SpellEffect>()
        private set
    var txtStat: MutableMap<Int, String> = HashMap()
        private set
    var soulStat: MutableMap<Int, Int> = HashMap()
        private set

    constructor(Guid: Int, template: Int, qua: Int, pos: Int, strStats: String, puit: Int) {
        this.guid = Guid
        this.template = World.world.getObjTemplate(template)
        this.quantity = qua
        this.position = pos
        this.puit = puit

        this.parseStringToStats(strStats)
    }

    constructor(Guid: Int) {
        this.guid = Guid
        this.template = World.world.getObjTemplate(8378)
        this.quantity = 1
        this.position = -1
        this.puit = 0
    }

    constructor(Guid: Int, template: Int, qua: Int, pos: Int, stats: Stats, effects: ArrayList<SpellEffect>, _SoulStat: Map<Int, Int>, _txtStats: Map<Int, String>, puit: Int) {
        this.guid = Guid
        this.template = World.world.getObjTemplate(template)
        this.quantity = qua
        this.position = pos
        this.stats = stats
        this.effects = effects
        @Suppress("UNCHECKED_CAST")
        this.soulStat = _SoulStat as MutableMap<Int, Int>
        @Suppress("UNCHECKED_CAST")
        this.txtStat = _txtStats as MutableMap<Int, String>
        this.obvijevanPos = 0
        this.obvijevanLook = 0
        this.puit = puit
    }

    fun getClone(qua: Int, insert: Boolean): GameObject? {
        val maps = HashMap<Int, Int>()
        maps.putAll(this.stats.effects)
        val newStats = Stats(maps)
        val effects = ArrayList<SpellEffect>()
        for (effect in this.effects)
            effects.add(effect.cloneEffect())

        val `object` = GameObject(-1, this.template!!.id, qua, if (insert) Constant.ITEM_POS_NO_EQUIPED else this.position, newStats, effects, this.soulStat, this.txtStat, this.puit)
        if (insert)
            if ((DatabaseManager.get(ObjectData::class.java) as ObjectData).insert(`object`))
                return `object`
        return null
    }

    fun setId(id: Int) {
        this.guid = id
    }

    fun parseStringToStats(strStats: String) {
        if (this.template == null) return

        var dj1 = ""
        if (!strStats.equals("")) {
            for (split in strStats.split(",")) {
                try {
                    if (split.equals(""))
                        continue
                    if (split.substring(0, 3).equals("325", ignoreCase = true) && (this.template!!.id == 10207 || this.template!!.id == 10601)) {
                        txtStat[Constant.STATS_DATE] = split.substring(3) + ""
                        continue
                    }
                    if (split.substring(0, 3).equals("3dc", ignoreCase = true)) {// Si c'est une rune de signature cree
                        txtStat[Constant.STATS_SIGNATURE] = split.split("#")[4]
                        continue
                    }
                    if (split.substring(0, 3).equals("3d9", ignoreCase = true)) {// Si c'est une rune de signature modifie
                        txtStat[Constant.STATS_CHANGE_BY] = split.split("#")[4]
                        continue
                    }

                    var stats = split.split("#")
                    val id = (stats[0]).toInt(16)


                    if (id == Constant.STATS_MIMIBIOTE) {
                        val data = split.split("#")
                        txtStat[id] = data[2] + ";" + data[3]
                        continue
                    }
                    if (id in 281..294) {
                        this.spellStats.add(split)
                        continue
                    }
                    if (id == Constant.STATS_PETS_DATE && this.template!!.type == Constant.ITEM_TYPE_CERTIFICAT_CHANIL) {
                        txtStat[id] = split.substring(3)
                        continue
                    }
                    if (id == Constant.STATS_CHANGE_BY || id == Constant.STATS_NAME_TRAQUE || id == Constant.STATS_OWNER_1) {
                        txtStat[id] = stats[4]
                        continue
                    }
                    if (id == Constant.STATS_GRADE_TRAQUE || id == Constant.STATS_ALIGNEMENT_TRAQUE || id == Constant.STATS_NIVEAU_TRAQUE) {
                        txtStat[id] = stats[3]
                        continue
                    }
                    if (id == Constant.STATS_PETS_SOUL) {
                        soulStat[(stats[1]).toInt(16)] = (stats[3]).toInt(16) // put(id_monstre, nombre_tue)
                        continue
                    }
                    if (id == Constant.STATS_NAME_DJ) {
                        dj1 += (if (!dj1.isEmpty()) "," else "") + stats[3]
                        txtStat[Constant.STATS_NAME_DJ] = dj1
                        continue
                    }
                    if (id == 997 || id == 996) {
                        txtStat[id] = stats[4]
                        continue
                    }
                    if (this.template != null && this.template!!.id == 77 && id == Constant.STATS_PETS_DATE) {
                        txtStat[id] = split.substring(3)
                        continue
                    }
                    if (id == Constant.STATS_DATE) {
                        txtStat[id] = stats[3]
                        continue
                    }
                    if (id != Constant.STATS_RESIST && (!stats[3].equals("") && (!stats[3].equals("0") || id == Constant.STATS_PETS_DATE || id == Constant.STATS_PETS_PDV || id == Constant.STATS_PETS_POIDS || id == Constant.STATS_PETS_EPO || id == Constant.STATS_PETS_REPAS))) {//Si le stats n'est pas vide et (n'est pas egale a 0 ou est de type familier)
                        if (!(this.template!!.type == Constant.ITEM_TYPE_CERTIFICAT_CHANIL && id == Constant.STATS_PETS_DATE)) {
                            if (id == Constant.STATS_PETS_DATE && stats[3] == "0") {
                                val date = "328" + Formulas.convertToDate(System.currentTimeMillis())
                                stats = date.split("#")
                            }
                            txtStat[id] = stats[3]
                            continue
                        }//22/11/2017 12:57
                    }
                    if (id == Constant.STATS_RESIST && this.template != null && this.template!!.type == 93) {
                        txtStat[id] = stats[4]
                        continue
                    }
                    if (id == Constant.STATS_RESIST) {
                        txtStat[id] = stats[4]
                        continue
                    }

                    var follow1 = true
                    when (id) {
                        110, 139, 605, 614 -> {
                            val min = stats[1]
                            val max = stats[2]
                            val jet = stats[4]
                            val args = "$min;$max;-1;-1;0;$jet"
                            effects.add(SpellEffect(id, args, 0, -1))
                            follow1 = false
                        }
                    }
                    if (!follow1) {
                        continue
                    }

                    var follow2 = true
                    for (a in Constant.ARMES_EFFECT_IDS) {
                        if (a == id) {
                            effects.add(SpellEffect(id, stats[1] + ";" + stats[2] + ";-1;0;0;" + stats[4], 0, -1))
                            follow2 = false
                        }
                    }
                    if (!follow2)
                        continue//Si c'etait un effet Actif d'arme ou une signature

                    this.stats.addOneStat(id, (stats[1]).toInt(16))
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                }
            }
        }
    }

    fun addTxtStat(i: Int, s: String) {
        txtStat[i] = s
    }

    val traquedName: String?
        get() {
            for ((key, value) in txtStat.entries) {
                if (Integer.toHexString(key).compareTo("3dd") == 0) {
                    return value
                }
            }
            return null
        }



    fun setMountStats(player: Player, mount0: Mount?, castrated: Boolean): Mount {
        var mount = mount0
        if (mount == null)
            mount = Mount(Constant.getMountColorByParchoTemplate(this.template!!.id), player.id, false)
        if (castrated) mount.setCastrated()

        this.clearStats()
        this.stats.addOneStat(995, mount.id)
        this.txtStat[996] = player.name
        this.txtStat[997] = mount.name!!
        return mount
    }

    fun attachToPlayer(player: Player) {
        this.txtStat[Constant.STATS_OWNER_1] = player.name
        SocketManager.GAME_SEND_UPDATE_OBJECT_DISPLAY_PACKET(player, this)
    }

    val isAttach: Boolean
        get() {
            val ok = this.txtStat.containsKey(Constant.STATS_OWNER_1)

            if (ok) {
                val player = World.world.getPlayerByName(this.txtStat[Constant.STATS_OWNER_1]!!)
                player?.send("BN")
            }

            return ok
        }

    fun encodeItem(): String {
        val posi = if (position == Constant.ITEM_POS_NO_EQUIPED) "" else Integer.toHexString(position)
        return (Integer.toHexString(guid) + "~"
                + Integer.toHexString(if (template == null) 39 else template!!.id) + "~"
                + Integer.toHexString(quantity) + "~" + posi + "~"
                + encodeStats() + ";")
    }

    open fun encodeStats(): String {
        if (this.template!!.type == 83) //Si c'est une pierre d'ame vide
            return this.template!!.strTemplate

        val stats = StringBuilder()
        var isFirst = true

        // Panoplie de classe (81 a 92)
        if (this.template!!.panoId in 81..92) {
            for (spell in this.spellStats) {
                if (!isFirst) {
                    stats.append(",")
                }
                stats.append(spell)
                isFirst = false
            }
        }

        for (effect in this.effects) {
            if (!isFirst)
                stats.append(",")
            val split = effect.args.split(";")

            try {
                when (effect.effectID) {
                    614 -> stats.append(Integer.toHexString(effect.effectID)).append("###").append(split[0]).append("#").append(split[5])
                    else -> stats.append(Integer.toHexString(effect.effectID)).append("#").append(split[0]).append("#").append(split[1]).append("#").append(split[1]).append("#").append(split[5])
                }
            } catch (e: Exception) {
                log.error("unexpected error", e)
                continue
            }
            isFirst = false
        }

        for ((key, value) in txtStat.entries) {
            if (!isFirst)
                stats.append(",")
            if (template!!.type == 77 || template!!.type == 90) {
                if (key == Constant.STATS_PETS_PDV)
                    stats.append(Integer.toHexString(key)).append("#").append(value).append("##").append(value)
                if (key == Constant.STATS_PETS_EPO)
                    stats.append(Integer.toHexString(key)).append("#").append(value).append("##").append(value)
                if (key == Constant.STATS_PETS_REPAS)
                    stats.append(Integer.toHexString(key)).append("#").append(value).append("##").append(value)
                if (key == Constant.STATS_PETS_POIDS) {
                    var corpu = 0
                    var corpulence = 0
                    val c = value
                    if (c != null && !c.equals("")) {
                        try {
                            corpulence = c.toInt()
                        } catch (e: Exception) {
                            log.error("unexpected error", e)
                }
                    }
                    if (corpulence > 0 || corpulence < 0)
                        corpu = 7
                    stats.append(Integer.toHexString(key)).append("#").append(Integer.toHexString(corpu)).append("#").append(if (corpulence > 0) corpu else 0).append("#").append(Integer.toHexString(corpu))
                }
                if (key == Constant.STATS_PETS_DATE && template!!.type == 77) {
                    if (value!!.contains("#"))
                        stats.append(Integer.toHexString(key)).append(value)
                    else
                        stats.append(Integer.toHexString(key)).append(Formulas.convertToDate(value.toLong()))
                }
            } else if (key == Constant.STATS_MIMIBIOTE) {
                val data = value!!.split(";")
                stats.append(Integer.toHexString(Constant.STATS_MIMIBIOTE)).append("##").append(data[0]).append("#").append(data[1])
            } else if (key == Constant.STATS_CHANGE_BY || key == Constant.STATS_NAME_TRAQUE || key == Constant.STATS_OWNER_1) {
                stats.append(Integer.toHexString(key)).append("####").append(value)
            } else if (key == Constant.STATS_GRADE_TRAQUE || key == Constant.STATS_ALIGNEMENT_TRAQUE || key == Constant.STATS_NIVEAU_TRAQUE) {
                stats.append(Integer.toHexString(key)).append("###").append(value).append("#")
            } else if (key == Constant.STATS_NAME_DJ) {
                if (value == "0d0+0")
                    continue
                for (i in value!!.split(",")) {
                    stats.append(",").append(Integer.toHexString(key)).append("###").append(i)
                }
                continue
            } else if (key == Constant.STATS_DATE) {
                val item = value
                if (item!!.contains("#")) {
                    val date = item.split("#")[3]
                    if (date != null && !date.equals(""))
                        stats.append(Integer.toHexString(key)).append(Formulas.convertToDate(date.toLong()))
                } else
                    stats.append(Integer.toHexString(key)).append(Formulas.convertToDate(item.toLong()))
            } else if (key == Constant.CAPTURE_MONSTRE) {
                stats.append(Integer.toHexString(key)).append("###").append(value)
            } else if (key == Constant.STATS_PETS_PDV
                || key == Constant.STATS_PETS_POIDS
                || key == Constant.STATS_PETS_DATE
                || key == Constant.STATS_PETS_REPAS
            ) {
                val p = World.world.getPetsEntry(this.guid)
                if (p == null) {
                    if (key == Constant.STATS_PETS_PDV)
                        stats.append(Integer.toHexString(key)).append("#").append("a").append("#0#a")
                    if (key == Constant.STATS_PETS_POIDS)
                        stats.append(Integer.toHexString(key)).append("#").append("##")
                    if (key == Constant.STATS_PETS_DATE)
                        stats.append(Integer.toHexString(key)).append("#").append("##")
                    if (key == Constant.STATS_PETS_REPAS)
                        stats.append(Integer.toHexString(key)).append("#").append("##")
                } else {
                    if (key == Constant.STATS_PETS_PDV)
                        stats.append(Integer.toHexString(key)).append("#").append(Integer.toHexString(p.pdv)).append("##").append(Integer.toHexString(p.pdv))
                    if (key == Constant.STATS_PETS_POIDS)
                        stats.append(Integer.toHexString(key)).append("#").append(p.parseCorpulence()).append("#").append(if (p.corpulence > 0) p.parseCorpulence() else 0).append("#").append(p.parseCorpulence().toString())
                    if (key == Constant.STATS_PETS_DATE)
                        stats.append(Integer.toHexString(key)).append(p.parseLastEatDate())
                    if (key == Constant.STATS_PETS_REPAS)
                        stats.append(Integer.toHexString(key)).append("#").append(value).append("##").append(value)
                    if (p.getIsEupeoh()
                        && key == Constant.STATS_PETS_EPO
                    )
                        stats.append(Integer.toHexString(key)).append("#").append(Integer.toHexString(if (p.getIsEupeoh()) 1 else 0)).append("##").append(Integer.toHexString(if (p.getIsEupeoh()) 1 else 0))
                }
            } else if (key == Constant.STATS_RESIST
                && template!!.type == 93
            ) {
                stats.append(Integer.toHexString(key)).append("#").append(Integer.toHexString(getResistanceMax(template!!.strTemplate))).append("#").append(value).append("#").append(Integer.toHexString(getResistanceMax(template!!.strTemplate)))
            } else if (key == Constant.STATS_RESIST) {
                stats.append(Integer.toHexString(key)).append("#").append(Integer.toHexString(getResistanceMax(template!!.strTemplate))).append("#").append(value).append("#").append(Integer.toHexString(getResistanceMax(template!!.strTemplate)))
            } else {
                stats.append(Integer.toHexString(key)).append("####").append(value)
            }
            isFirst = false
        }

        for ((key, value) in soulStat.entries) {
            if (!isFirst)
                stats.append(",")

            if (this.template!!.type == 18)
                stats.append(Integer.toHexString(Constant.STATS_PETS_SOUL)).append("#").append(Integer.toHexString(key)).append("#").append("0").append("#").append(Integer.toHexString(value))
            if (key == Constant.STATS_NIVEAU)
                stats.append(Integer.toHexString(Constant.STATS_NIVEAU)).append("#").append(Integer.toHexString(key)).append("#").append("0").append("#").append(Integer.toHexString(value))
            isFirst = false
        }

        for ((statID, value) in this.stats.effects.entries) {
            if ((this.template!!.panoId in 81..92)
                || (this.template!!.panoId in 201..212)
            ) {
                val modificable = template!!.strTemplate.split(",")
                val cantMod = modificable.size
                for (j in 0 until cantMod) {
                    val mod = modificable[j].split("#")
                    if ((mod[0]).toInt(16) == statID) {
                        val jet = "0d0+" + (mod[1]).toInt(16)
                        if (!isFirst)
                            stats.append(",")
                        stats.append(mod[0]).append("#").append(if (mod[1] == "0") "" else mod[1]).append("##").append(mod[3]).append("#").append(jet)
                        isFirst = false
                    }
                }
                continue
            }

            if (!isFirst)
                stats.append(",")
            if (statID == 615) {
                stats.append(Integer.toHexString(statID)).append("###").append(Integer.toHexString(value))
            } else if ((statID == 970) || (statID == 971) || (statID == 972)
                || (statID == 973) || (statID == 974)
            ) {
                val jet = value
                if ((statID == 974) || (statID == 972) || (statID == 970))
                    stats.append(Integer.toHexString(statID)).append("#0#0#").append(Integer.toHexString(jet))
                else
                    stats.append(Integer.toHexString(statID)).append("#0#0#").append(jet)
                if (statID == 973)
                    this.obvijevanPos = jet
                if (statID == 972)
                    this.obvijevanLook = jet
            } else if (statID == Constant.STATS_TURN) {
                val jet = "0d0+$value"
                stats.append(Integer.toHexString(statID)).append("#")
                stats.append("0#0#").append(Integer.toHexString(value)).append("#").append(jet)
            } else {
                val jet = "0d0+$value"
                stats.append(Integer.toHexString(statID)).append("#")
                stats.append(Integer.toHexString(value)).append("###").append(jet)
            }
            isFirst = false
        }
        return stats.toString()
    }

    fun parseStatsStringSansUserObvi(): String {
        if (template!!.type == 83) //Si c'est une pierre d'ame vide
            return template!!.strTemplate

        val stats = StringBuilder()
        var isFirst = true

        if (this is Fragment) {
            for (couple in this.runes) {
                stats.append(if (stats.toString().isEmpty()) couple.first else ";" + couple.first).append(":").append(couple.second)
            }
            return stats.toString()
        }
        for ((key, value) in txtStat.entries) {
            if (!isFirst)
                stats.append(",")
            if (template!!.type == 77) {
                if (key == Constant.STATS_PETS_PDV)
                    stats.append(Integer.toHexString(key)).append("#").append(value).append("#0#").append(value)
                if (key == Constant.STATS_PETS_POIDS)
                    stats.append(Integer.toHexString(key)).append("#").append(value).append("#").append(value).append("#").append(value)
                if (key == Constant.STATS_PETS_DATE) {
                    if (value!!.contains("#"))
                        stats.append(Integer.toHexString(key)).append(value)
                    else
                        stats.append(Integer.toHexString(key)).append(Formulas.convertToDate(value.toLong()))
                }
            } else if (key == Constant.STATS_DATE) {
                if (value!!.contains("#"))
                    stats.append(Integer.toHexString(key)).append(value)
                else
                    stats.append(Integer.toHexString(key)).append("#0#0#").append(value.toLong())
            } else if (key == Constant.STATS_CHANGE_BY || key == Constant.STATS_NAME_TRAQUE || key == Constant.STATS_OWNER_1) {
                stats.append(Integer.toHexString(key)).append("#0#0#0#").append(value)
            } else if (key == Constant.STATS_GRADE_TRAQUE || key == Constant.STATS_ALIGNEMENT_TRAQUE || key == Constant.STATS_NIVEAU_TRAQUE) {
                stats.append(Integer.toHexString(key)).append("#0#0#").append(value).append("#0")
            } else if (key == Constant.STATS_NAME_DJ) {
                for (i in value!!.split(","))
                    stats.append(",").append(Integer.toHexString(key)).append("#0#0#").append(i)
            } else if (key == Constant.CAPTURE_MONSTRE) {
                stats.append(Integer.toHexString(key)).append("#0#0#").append(value)
            } else if (key == Constant.STATS_PETS_PDV
                || key == Constant.STATS_PETS_POIDS
                || key == Constant.STATS_PETS_DATE
            ) {
                val p = World.world.getPetsEntry(this.guid)
                if (p == null) {
                    if (key == Constant.STATS_PETS_PDV)
                        stats.append(Integer.toHexString(key)).append("#").append("a").append("#0#a")
                    if (key == Constant.STATS_PETS_POIDS)
                        stats.append(Integer.toHexString(key)).append("#").append("0").append("#0#0")
                    if (key == Constant.STATS_PETS_DATE)
                        stats.append(Integer.toHexString(key)).append("#").append("0").append("#0#0")
                } else {
                    if (key == Constant.STATS_PETS_PDV)
                        stats.append(Integer.toHexString(key)).append("#").append(Integer.toHexString(p.pdv)).append("#0#").append(Integer.toHexString(p.pdv))
                    if (key == Constant.STATS_PETS_POIDS)
                        stats.append(Integer.toHexString(key)).append("#").append(p.parseCorpulence().toString()).append("#").append(if (p.corpulence > 0) p.parseCorpulence() else 0).append("#").append(p.parseCorpulence().toString())
                    if (key == Constant.STATS_PETS_DATE)
                        stats.append(Integer.toHexString(key)).append(p.parseLastEatDate())
                    if (p.getIsEupeoh()
                        && key == Constant.STATS_PETS_EPO
                    )
                        stats.append(Integer.toHexString(key)).append("#").append(Integer.toHexString(if (p.getIsEupeoh()) 1 else 0)).append("#0#").append(Integer.toHexString(if (p.getIsEupeoh()) 1 else 0))
                }
            } else if (key == Constant.STATS_MIMIBIOTE) {
                val data = value!!.split(";")
                stats.append(Integer.toHexString(Constant.STATS_MIMIBIOTE)).append("#0#").append(data[0]).append("#").append(data[1])
            } else {
                stats.append(Integer.toHexString(key)).append("#0#0#0#").append(value)
            }
            isFirst = false
        }
        // Panoplie de classe (81 a 92)
        if (this.template!!.panoId in 81..92) {
            for (spell in this.spellStats) {
                if (!isFirst) {
                    stats.append(",")
                }
                stats.append(spell)
                isFirst = false
            }
        }
        for (SE in effects) {
            if (!isFirst)
                stats.append(",")

            val infos = SE.args.split(";")
            try {
                stats.append(Integer.toHexString(SE.effectID)).append("#").append(infos[0]).append("#").append(infos[1]).append("#0#").append(infos[5])
            } catch (e: Exception) {
                log.error("unexpected error", e)
                continue
            }
            isFirst = false
        }
        for ((key, value) in soulStat.entries) {
            if (!isFirst)
                stats.append(",")
            stats.append(Integer.toHexString(Constant.STATS_PETS_SOUL)).append("#").append(Integer.toHexString(key)).append("#").append("0").append("#").append(Integer.toHexString(value))
            isFirst = false
        }
        for ((key, value) in this.stats.effects.entries) {
            if (!isFirst)
                stats.append(",")

            if (key == 615) {
                stats.append(Integer.toHexString(key)).append("#0#0#").append(Integer.toHexString(value))
            } else {
                val jet = "0d0+$value"
                stats.append(Integer.toHexString(key)).append("#").append(Integer.toHexString(value))
                stats.append("#0#0#").append(jet)
            }
            isFirst = false
        }
        return stats.toString()
    }

    open fun parseToSave(): String = parseStatsStringSansUserObvi()

    fun obvijevanOCO_Packet(pos: Int): String {
        var strPos = pos.toString()
        if (pos == -1)
            strPos = ""
        var upPacket = "OCO"
        upPacket = upPacket + Integer.toHexString(this.guid) + "~"
        upPacket = upPacket + Integer.toHexString(this.template!!.id) + "~"
        upPacket = upPacket + Integer.toHexString(this.quantity) + "~"
        upPacket = upPacket + strPos + "~"
        upPacket = upPacket + encodeStats()
        return upPacket
    }

    fun obvijevanNourir(obj: GameObject?) {
        if (obj == null)
            return
        for ((key, value) in this.stats.effects.entries) {
            if (key != 974) // on ne boost que la stat de l'experience de l'obvi
                continue
            if (value > 500) // si le boost a une valeur superieure a 500 (irrealiste)
                return
            this.stats.effects[key] = value + obj.template!!.level / 3
        }
    }

    fun obvijevanChangeStat(statID: Int, `val`: Int) {
        for ((key, _) in this.stats.effects.entries) {
            if (key != statID)
                continue
            this.stats.effects[key] = `val`
        }
    }

    fun removeAllObvijevanStats() {
        this.obvijevanPos = 0
        this.obvijevanLook = 0
        val statsSansObvi = Stats()
        for ((statID, value) in this.stats.effects.entries) {
            if ((statID == 970) || (statID == 971) || (statID == 972)
                || (statID == 973) || (statID == 974)
            )
                continue
            statsSansObvi.addOneStat(statID, value)
        }
        this.stats = statsSansObvi
    }

    fun removeAll_ExepteObvijevanStats() {
        this.obvijevanPos = 0
        val statsSansObvi = Stats()
        for ((statID, value) in this.stats.effects.entries) {
            if ((statID != 971) && (statID != 972) && (statID != 973)
                && (statID != 974)
            )
                continue
            statsSansObvi.addOneStat(statID, value)
        }
        this.stats = statsSansObvi
    }

    fun getObvijevanStatsOnly(): String? {
        val obj = this.getClone(1, true)
        obj!!.removeAll_ExepteObvijevanStats()
        return obj.parseStatsStringSansUserObvi()
    }

    /* *********FM SYSTEM********* */

    fun generateNewStatsFromTemplate(statsTemplate: String?, useMax: Boolean): Stats {
        val itemStats = Stats(false, null)
        //Si stats Vides
        if (statsTemplate == null || statsTemplate == "")
            return itemStats

        val splitted = statsTemplate.split(",")
        for (s in splitted) {
            val stats = s.split("#")
            val statID = (stats[0]).toInt(16)
            var follow = true

            for (a in Constant.ARMES_EFFECT_IDS)
                if (a == statID)
                    follow = false
            if (!follow)
                continue//Si c'etait un effet Actif d'arme

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
                        log.error("unexpected error", e)
                value = Formulas.getRandomJet(null, null, jet)
                    }
                }
            } catch (e: Exception) {
                log.error("unexpected error", e)
                }
            itemStats.addOneStat(statID, value)
        }
        return itemStats
    }

    fun getCritEffects(): ArrayList<SpellEffect> {
        val effets = ArrayList<SpellEffect>()
        for (SE in effects) {
            try {
                var boost = true
                for (i in Constant.NO_BOOST_CC_IDS)
                    if (i == SE.effectID)
                        boost = false
                val infos = SE.args.split(";")
                if (!boost) {
                    effets.add(SE)
                    continue
                }
                val min = ((infos[0]).toInt(16)
                        + (if (boost) template!!.bonusCC else 0))
                val max = ((infos[1]).toInt(16)
                        + (if (boost) template!!.bonusCC else 0))
                val jet = "1d" + (max - min + 1) + "+" + (min - 1)
                //exCode: String newArgs = Integer.toHexString(min)+";"+Integer.toHexString(max)+";-1;-1;0;"+jet;
                //osef du minMax, vu qu'on se sert du jet pour calculer les degats
                val newArgs = "0;0;0;-1;0;$jet"
                effets.add(SpellEffect(SE.effectID, newArgs, 0, -1))
            } catch (e: Exception) {
                log.error("unexpected error", e)
                }
        }
        return effets
    }

    fun clearStats() {
        //On vide l'item de tous ces effets
        stats = Stats()
        effects.clear()
        txtStat.clear()
        spellStats.clear()
        soulStat.clear()
    }

    fun refreshStatsObjet(newsStats: String) {
        parseStringToStats(newsStats)
        (DatabaseManager.get(ObjectData::class.java) as ObjectData).update(this)
    }

    fun getResistance(statsTemplate: String): Int {
        var resistance = 0

        val splitted = statsTemplate.split(",")
        for (s in splitted) {
            val stats = s.split("#")
            if ((stats[0]).toInt(16) == Constant.STATS_RESIST) {
                resistance = (stats[2]).toInt(16)
            }
        }
        return resistance
    }

    fun getResistanceMax(statsTemplate: String): Int {
        var resistanceMax = 0

        val splitted = statsTemplate.split(",")
        for (s in splitted) {
            val stats = s.split("#")
            if ((stats[0]).toInt(16) == Constant.STATS_RESIST) {
                resistanceMax = (stats[1]).toInt(16)
            }
        }
        return resistanceMax
    }

    fun getRandomValue(statsTemplate: String, statsId: Int): Int {
        if (statsTemplate == "")
            return 0

        val splitted = statsTemplate.split(",")
        var value = 0
        for (s in splitted) {
            val stats = s.split("#")
            val statID = (stats[0]).toInt(16)
            if (statID != statsId)
                continue
            var jet: String
            try {
                jet = stats[4]
                value = Formulas.getRandomJet(null, null, jet)
            } catch (e: Exception) {
                log.error("unexpected error", e)
                return 0
            }
        }
        return value
    }

    /** FM TOUT POURRI  */
    fun parseStringStatsEC_FM(obj: GameObject, poid: Double, carac: Int): String {
        var stats = ""
        var first = false
        var perte = 0.0
        for (EH in obj.effects) {
            if (first)
                stats += ","
            val infos = EH.args.split(";")
            try {
                stats += (Integer.toHexString(EH.effectID) + "#" + infos[0]
                        + "#" + infos[1] + "#0#" + infos[5])
            } catch (e: Exception) {
                log.error("unexpected error", e)
                continue
            }
            first = true
        }
        val statsObj = HashMap(obj.stats.effects)
        val keys = ArrayList(obj.stats.effects.keys)
        Collections.shuffle(keys)
        var p = 0
        var key = 0
        if (keys.size > 1) {
            for (i in keys) // On cherche un OverFM
            {
                val value = statsObj[i]!!
                if (this.isOverFm(i, value)) {
                    key = i
                    break
                }
                p++
            }
            if (key > 0) { // On place l'OverFm en tete de liste pour etre nique
                keys.removeAt(p)
                keys.add(p, keys[0])
                keys.removeAt(0)
                keys.add(0, key)
            }
        }
        for (i in keys) {
            var newstats = 0
            val statID = i
            val value = statsObj[i]!!
            if (perte > poid || statID == carac) {
                newstats = value
            } else if ((statID == 152) || (statID == 154) || (statID == 155)
                || (statID == 157) || (statID == 116) || (statID == 153)
            ) {
                var a = (value * poid / 100.0).toFloat()
                if (a < 1.0f)
                    a = 1.0f
                val chute = value + a
                newstats = floor(chute.toDouble()).toInt()
                if (newstats > JobAction.getBaseMaxJet(obj.template!!.id, Integer.toHexString(i)))
                    newstats = JobAction.getBaseMaxJet(obj.template!!.id, Integer.toHexString(i))

            } else {
                if ((statID == 127) || (statID == 101))
                    continue

                var chute: Float
                if (this.isOverFm(statID, value)) // Gros kick dans la gueulle de l'over FM
                    chute = (value - value
                            * (poid - floor(perte).toInt()) * 2 / 100.0).toFloat()
                else
                    chute = (value - value
                            * (poid - floor(perte).toInt()) / 100.0).toFloat()
                if ((chute / value.toFloat()) < 0.75)
                    chute = value.toFloat() * 0.75f // On ne peut pas perdre plus de 25% d'une stat d'un coup

                val chutePwr = ((value - chute)
                        * JobAction.getPwrPerEffet(statID))

                perte += chutePwr

                newstats = floor(chute.toDouble()).toInt()
            }
            if (newstats < 1)
                continue
            val jet = "0d0+$newstats"
            if (first)
                stats += ","
            stats += (Integer.toHexString(statID) + "#"
                    + Integer.toHexString(newstats) + "#0#0#" + jet)
            first = true
        }
        for ((entryKey, entryValue) in obj.txtStat.entries) {
            if (first)
                stats += ","
            stats += (Integer.toHexString(entryKey) + "#0#0#0#"
                    + entryValue)
            first = true
        }
        return stats
    }


    fun parseFMStatsString(statsstr: String, obj: GameObject, add: Int, negatif: Boolean): String {
        var stats = ""
        var isFirst = true
        for (SE in obj.effects) {
            if (!isFirst)
                stats += ","

            val infos = SE.args.split(";")
            try {
                stats += (Integer.toHexString(SE.effectID) + "#" + infos[0]
                        + "#" + infos[1] + "#0#" + infos[5])
            } catch (e: Exception) {
                log.error("unexpected error", e)
                continue
            }
            isFirst = false
        }

        for ((key, value) in obj.stats.effects.entries) {
            if (!isFirst)
                stats += ","
            if (Integer.toHexString(key).compareTo(statsstr) == 0) {
                var newstats = 0
                if (negatif) {
                    newstats = value - add
                    if (newstats < 1)
                        continue
                } else {
                    newstats = value + add
                }
                val jet = "0d0+$newstats"
                stats += (Integer.toHexString(key) + "#"
                        + Integer.toHexString(value + add) + "#0#0#"
                        + jet)
            } else {
                val jet = "0d0+$value"
                stats += (Integer.toHexString(key) + "#"
                        + Integer.toHexString(value) + "#0#0#" + jet)
            }
            isFirst = false
        }

        for ((key, value) in obj.txtStat.entries) {
            if (!isFirst)
                stats += ","
            stats += (Integer.toHexString(key) + "#0#0#0#"
                    + value)
            isFirst = false
        }

        return stats
    }

    fun isOverFm(stat: Int, `val`: Int): Boolean {
        var trouve = false
        val statsTemplate: String? = this.template!!.strTemplate
        if (statsTemplate == null || statsTemplate.isEmpty())
            return false
        val split = statsTemplate.split(",")
        for (s in split) {
            val stats = s.split("#")
            val statID = (stats[0]).toInt(16)
            if (statID != stat)
                continue

            trouve = true
            var sig = true
            for (a in Constant.ARMES_EFFECT_IDS)
                if (a == statID)
                    sig = false
            if (!sig)
                continue
            var jet = ""
            var value = 1
            try {
                jet = stats[4]
                value = Formulas.getRandomJet(null, null, jet)
                try {
                    val min = (stats[1]).toInt(16)
                    val max = (stats[2]).toInt(16)
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
            if (`val` > value)
                return true
        }
        return !trouve
    }

    fun isSameStats(newObj: GameObject): Boolean {
        var effects = true
        var check = false
        for (effect0 in this.effects) {
            for (effect1 in newObj.effects) {
                if (effect0.effectID == effect1.effectID && effect0.jet == effect1.jet) {
                    check = true
                    break
                }
            }
            if (!check) {
                effects = false
                break
            }
        }
        check = false
        for (spellStats0 in this.spellStats) {
            for (spellStats1 in newObj.spellStats) {
                if (spellStats0 == spellStats1) {
                    check = true
                    break
                }
            }
            if (!check) {
                effects = false
                break
            }
        }
        check = false
        for (effect0 in this.txtStat.values) {
            for (effect1 in newObj.txtStat.values) {
                if (effect0 == effect1) {
                    check = true
                    break
                }
            }
            if (!check) {
                effects = false
                break
            }
        }
        check = false
        for ((key0, value0) in this.soulStat.entries) {
            for ((key1, value1) in newObj.soulStat.entries) {
                if (key0 == key1 && value0 == value1) {
                    check = true
                    break
                }
            }
            if (!check) {
                effects = false
                break
            }
        }
        return effects && this.stats.isSameStats(newObj.stats)
    }

    fun isOverFm2(stat: Int, `val`: Int): Boolean {
        var trouve = false
        val statsTemplate: String? = this.template!!.strTemplate
        if (statsTemplate == null || statsTemplate.isEmpty())
            return false
        val split = statsTemplate.split(",")
        for (s in split) {
            val stats = s.split("#")
            val statID = (stats[0]).toInt(16)
            if (statID != stat)
                continue

            trouve = true
            var sig = true
            for (a in Constant.ARMES_EFFECT_IDS)
                if (a == statID)
                    sig = false
            if (!sig)
                continue
            var jet = ""
            var value = 1
            try {
                jet = stats[4]
                value = Formulas.getRandomJet(null, null, jet)
                try {
                    val min = (stats[1]).toInt(16)
                    val max = (stats[2]).toInt(16)
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
            if (`val` == value)
                return true
        }
        if (!trouve)
            return true
        return false
    }

    fun getAppearanceTemplateId(): Int {
        if (this.txtStat[Constant.STATS_MIMIBIOTE] != null) {
            return Integer.parseInt(this.txtStat[Constant.STATS_MIMIBIOTE]!!.split(";")[1], 16)
        }
        return this.template!!.id
    }

    fun isMimibiote(): Boolean = this.txtStat[Constant.STATS_MIMIBIOTE] != null

    fun scripted(): SItem = this.scriptVal
}
