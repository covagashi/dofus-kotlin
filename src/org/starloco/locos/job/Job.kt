package org.starloco.locos.job

import java.util.ArrayList
import java.util.HashMap
import org.slf4j.LoggerFactory
import org.starloco.locos.common.splitJ

private val log = LoggerFactory.getLogger(Job::class.java)

class Job(val id: Int, tools: String, crafts: String, skills: String) {

    private val tools = ArrayList<Int>()
    private val crafts = HashMap<Int, ArrayList<Int>>()
    private val skills = HashMap<Int, ArrayList<Int>>()

    init {
        if (tools != "") {
            for (str in tools.splitJ(",")) {
                try {
                    this.tools.add(str.toInt())
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                }
            }
        }
        if (crafts != "") {
            for (str in crafts.splitJ("|")) {
                try {
                    val skID = str.split(";")[0].toInt()
                    val list = ArrayList<Int>()
                    for (str2 in str.splitJ(";")[1].split(","))
                        list.add(str2.toInt())
                    this.crafts[skID] = list
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                }
            }
        }
        if (skills != "") {
            for (arg0 in skills.splitJ("|")) {
                val io = arg0.split(";")[0]
                val skill = arg0.split(";")[1]
                val list = ArrayList<Int>()

                for (arg1 in skill.splitJ(","))
                    list.add(arg1.toInt())

                for (arg1 in io.splitJ(","))
                    this.skills[arg1.toInt()] = list
            }
        }
    }

    fun getSkills(): Map<Int, ArrayList<Int>> = skills

    fun getCrafts(): Map<Int, ArrayList<Int>> = crafts

    fun isValidTool(id1: Int): Boolean {
        for (id in this.tools)
            if (id == id1)
                return true
        return false
    }

    fun getListBySkill(skill: Int): ArrayList<Int>? = this.crafts[skill]

    fun canCraft(skill: Int, template: Int): Boolean {
        val list = this.crafts[skill]
        if (list != null)
            for (id in list)
                if (id == template)
                    return true
        return false
    }

    fun isMaging(): Boolean = (this.id > 42 && this.id < 51) || (this.id > 61 && this.id < 65)
}
