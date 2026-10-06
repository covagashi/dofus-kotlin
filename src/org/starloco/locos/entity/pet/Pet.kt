package org.starloco.locos.entity.pet

import org.starloco.locos.common.Formulas
import org.starloco.locos.kernel.Constant

import java.util.ArrayList
import java.util.HashMap

class Pet(
    val templateId: Int,
    val type: Int,                          //0 ne mange rien, 1 mange des creatures, 2 mange des objets, 3 mange un groupe d'objet.
    val gap: String,                        //En heure 5,72 si type = 2 ou 3
    val statsUp: String,
    val max: Int,
    val gain: Int,
    val deadtemplate: Int,
    val epo: Int,
    private val jet: String
) {

    private val categ = HashMap<Int, ArrayList<Int>>()        // si type 3 StatID|categID#categID;StatID2| ...
    private val template = HashMap<Int, ArrayList<Int>>()     // si type 2 StatID|templateId#templateId#;StatID2| ...
    private val monster = HashMap<Int, ArrayList<MutableMap<Int, Int>>>() // si type 1 StatID|monsterID,qua#monsterID,qua;StatID2|monsterID,qua#monsterID,qua ...

    init {
        decompileStatsUpItem()
    }

    fun getDeadTemplate(): Int = this.deadtemplate

    fun getMonsters(): Map<Int, ArrayList<MutableMap<Int, Int>>> = this.monster

    fun getNumbMonster(StatID: Int, monsterID: Int): Int {
        for (ID in this.monster.entries) {
            if (ID.key == StatID) {
                for (entry in ID.value) {
                    for (monsterEntry in entry.entries) {
                        if (monsterEntry.key == monsterID) {
                            return monsterEntry.value
                        }
                    }
                }
            }
        }
        return 0
    }

    fun decompileStatsUpItem() {
        if (this.type == 3 || this.type == 2) {
            if (this.statsUp.contains(";"))//Plusieurs stats
            {
                for (cut in this.statsUp.split(";"))//On coupe b2|41#49#62 puis 70|63#64
                {
                    val cut2 = cut.split("\\|")
                    val statsID = (cut2[0]).toInt(16)
                    val ar = ArrayList<Int>()

                    for (categ in cut2[1].split("#")) {
                        val categID = categ.toInt()
                        ar.add(categID)
                    }
                    if (this.type == 3)
                        this.categ[statsID] = ar
                    if (this.type == 2)
                        this.template[statsID] = ar
                }

            } else
            //Un seul stats
            {
                val cut2 = this.statsUp.split("\\|") //On coupe b2 puis 41#49#62
                val statsID = (cut2[0]).toInt(16)
                val ar = ArrayList<Int>()
                for (categ in cut2[1].split("#")) {
                    val categID = categ.toInt()
                    ar.add(categID)
                }
                if (this.type == 3)
                    this.categ[statsID] = ar
                if (this.type == 2)
                    this.template[statsID] = ar
            }
        } else if (this.type == 1) //StatID|monsterID,qua#monsterID,qua;StatID2|monsterID,qua#monsterID,qua
        {
            if (this.statsUp.contains(";"))//Plusieurs stats
            {
                for (cut in this.statsUp.split(";"))//On coupe
                {
                    val cut2 = cut.split("\\|")
                    val statsID = (cut2[0]).toInt(16)
                    val ar = ArrayList<MutableMap<Int, Int>>()
                    for (soustotal in cut2[1].split("#")) {
                        var monsterID = 0
                        var qua = 0
                        for (Iqua in soustotal.split(",")) {
                            if (monsterID == 0) {
                                monsterID = Iqua.toInt()
                            } else {
                                qua = Iqua.toInt()
                                val Mqua = HashMap<Int, Int>()
                                Mqua[monsterID] = qua
                                ar.add(Mqua)
                                this.monster[statsID] = ar
                                monsterID = 0
                            }
                        }
                    }
                }
            } else
            //Un seul stats 8a|64,50#65,50#68,50#72,50#96,50#97,40#99,40#179,40#182,10#181,10#180,1
            {
                val cut2 = this.statsUp.split("\\|") //On coupe 8a puis 64,50#65,50#68,50#72,50#96,50#97,40#99,40#179,40#182,10#181,10#180,1
                val statsID = (cut2[0]).toInt(16)
                val ar = ArrayList<MutableMap<Int, Int>>()
                for (categ in cut2[1].split("#")) {
                    var monsterID = 0
                    var qua = 0
                    for (Iqua in categ.split(",")) {
                        if (monsterID == 0) {
                            monsterID = Iqua.toInt()
                        } else {
                            qua = Iqua.toInt()
                            val Mqua = HashMap<Int, Int>()
                            Mqua[monsterID] = qua
                            ar.add(Mqua)
                            this.monster[statsID] = ar
                        }
                    }
                }
            }
        }
    }

    fun canEat(Tid: Int, categID: Int, monsterId: Int): Boolean {
        if (this.type == 1) {
            for (ID in this.monster.entries)
                for (entry in ID.value)
                    for (monsterEntry in entry.entries)
                        if (monsterEntry.key == monsterId)
                            return true
            return false
        } else if (this.type == 2) {
            for (ID in this.template!!.entries)
                if (ID.value.contains(Tid))
                    return true
            return false
        } else if (this.type == 3) {
            for (ID in this.categ.entries)
                if (ID.value.contains(categID))
                    return true
            return false
        } else {
            return false
        }
    }

    fun statsIdByEat(Tid: Int, categID: Int, monsterId: Int): Int {
        if (this.type == 1) {
            for (ID in this.monster.entries)
                for (entry in ID.value)
                    for (monsterEntry in entry.entries)
                        if (monsterEntry.key == monsterId)
                            return ID.key
            return 0
        } else if (this.type == 2) {
            for (ID in this.template!!.entries)
                if (ID.value.contains(Tid))
                    return ID.key
            return 0
        } else if (this.type == 3) {
            for (ID in this.categ.entries)
                if (ID.value.contains(categID))
                    return ID.key
            return 0
        } else {
            return 0
        }
    }

    fun generateNewtxtStatsForPets(): MutableMap<Int, String> {
        val txtStat = HashMap<Int, String>()
        txtStat[Constant.STATS_PETS_PDV] = "a"
        txtStat[Constant.STATS_PETS_DATE] = "0"
        txtStat[Constant.STATS_PETS_POIDS] = "0"
        return txtStat
    }

    fun getJet(): String {
        if (!this.jet.contains("|"))
            return jet
        val split = this.jet.split("\\|")
        return split[Formulas.getRandomValue(1, split.size) - 1]
    }
}
