package org.starloco.locos.entity.monster

import org.starloco.locos.game.world.World
import org.starloco.locos.game.world.World.Drop

class Monster(
    val id: Int,
    gfxId: Int,
    align: Int,
    colors: String,
    thisGrades: String,
    thisSpells: String,
    thisStats: String,
    thisStatsInfos: String,
    thisPdvs: String,
    thisPoints: String,
    thisInit: String,
    minKamas: Int,
    maxKamas: Int,
    thisXp: String,
    ia: Int,
    capturable: Boolean,
    aggroDistance: Int,
    isBoss: Boolean,
    isArchMonster: Boolean
) {
    var gfxId: Int = gfxId
    var align: Int = align
    var colors: String = colors
    var ia: Int = ia
    var minKamas: Int = minKamas
    var maxKamas: Int = maxKamas
    val grades = HashMap<Int, MonsterGrade>()
    val drops = ArrayList<Drop>()
    var isCapturable: Boolean = capturable
    var aggroDistance: Int = aggroDistance
    var isBoss: Boolean = isBoss
    var isArchMonster: Boolean = isArchMonster

    init {
        parseGrades(thisGrades, thisSpells, thisStats, thisStatsInfos, thisPdvs, thisPoints, thisInit, thisXp)
    }

    fun setInfos(
        gfxId: Int, align: Int, colors: String,
        thisGrades: String, thisSpells: String, thisStats: String,
        thisStatsInfos: String, thisPdvs: String, thisPoints: String,
        thisInit: String, minKamas: Int, maxKamas: Int, thisXp: String, ia: Int,
        capturable: Boolean, aggroDistance: Int, isBoss: Boolean, isArchMonster: Boolean
    ) {
        this.gfxId = gfxId
        this.align = align
        this.colors = colors
        this.minKamas = minKamas
        this.maxKamas = maxKamas
        this.ia = ia
        this.isCapturable = capturable
        this.aggroDistance = aggroDistance
        grades.clear()
        parseGrades(thisGrades, thisSpells, thisStats, thisStatsInfos, thisPdvs, thisPoints, thisInit, thisXp, false)
    }

    private fun parseGrades(
        thisGrades: String, thisSpells: String, thisStats: String,
        thisStatsInfos: String, thisPdvs: String, thisPoints: String,
        thisInit: String, thisXp: String, spellGuard: Boolean = true
    ) {
        var G = 1
        val gradeParts = thisGrades.split("|")
        val statsParts = thisStats.split("|")
        val spellParts = thisSpells.split("|")
        val pdvParts = thisPdvs.split("|")
        val initParts = thisInit.split("|")
        val pointParts = thisPoints.split("|")
        val xpParts = thisXp.split("|")

        for (n in 0 until 12) {
            try {
                //Grades
                val grade = gradeParts[n]
                val infos = grade.split("@")
                val level = infos[0].toInt()
                val resists = infos[1]
                //Stats
                val stats = statsParts[n]
                //Spells
                var spells = ""
                if (!spellGuard || (!thisSpells.equals("||||", ignoreCase = true)
                        && !thisSpells.equals("", ignoreCase = true)
                        && !thisSpells.equals("-1", ignoreCase = true))
                ) {
                    spells = spellParts[n]
                    if (spells == "-1")
                        spells = ""
                }
                //PDVMax//init
                var pdvmax = 1
                var init = 1

                try {
                    if (n < pdvParts.size) pdvmax = pdvParts[n].toInt()
                    if (n < initParts.size) init = initParts[n].toInt()
                } catch (e: Exception) {
                    World.world.logger.error("  > Error : Monster (id:$id, grade: $n : Life or initiative unreadable.", e)
                }
                //PA / PM
                var PA = 3
                var PM = 3
                var xp = 10

                try {
                    val pts = pointParts[n].split(";")
                    try {
                        PA = pts[0].toInt()
                        PM = pts[1].toInt()
                        if (n < xpParts.size) xp = xpParts[n].toInt()
                    } catch (e: Exception) {
                        World.world.logger.error("  > Error : Monster (id:$id, grade: $n : PA, PM or experience unreadable.", e)
                    }
                } catch (e: Exception) {
                    World.world.logger.error("  > Error : Monster (id:$id, grade: $n : column 'points' unreadable.", e)
                }
                grades[G] = MonsterGrade(this, G, level, PA, PM, resists, stats, thisStatsInfos, spells, pdvmax, init, xp)
                G++
            } catch (e: Exception) {
                // ok, pour les dopeuls ...
                //TODO: Enlever toutes les erreurs
            }
        }
    }

    fun addDrop(D: Drop) {
        this.drops.add(D)
    }

    fun getGradeByLevel(lvl: Int): MonsterGrade? {
        for (grade in this.grades.values)
            if (grade != null && grade.level == lvl)
                return grade
        return null
    }

    fun getRandomGrade(): MonsterGrade? {
        val randomGrade = (Math.random() * (6 - 1)).toInt() + 1
        var random = 1

        for (grade in this.grades.values) {
            if (grade != null && random == randomGrade) return grade
            else random++
        }

        return null
    }
}
