package org.starloco.locos.entity.monster

import org.starloco.locos.area.map.GameMap
import org.starloco.locos.common.Formulas
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Config
import org.starloco.locos.kernel.Constant
import org.starloco.locos.`object`.GameObject
import org.starloco.locos.util.Pair
import java.util.LinkedList
import java.util.Timer
import java.util.TimerTask
import java.util.stream.Collectors

class MonsterGroup {
    val id: Int
    var cellId: Int
    var orientation: Int = 2
    var alignment: Int
    private var starBonus: Short = 0
    var aggroDistance: Int = 0
        private set
    private var subarea = -1
    private var hasChangedAgro = false
    var isFix = false
    var mobs: MutableMap<Int, MonsterGrade> = HashMap()
    var condition: String = ""
    private var objects: ArrayList<GameObject>? = null

    constructor(id: Int, alignment: Int, possibles: List<MonsterGrade>, map: GameMap, cell: Int, fixSize: Int, extra: MonsterGrade?) {
        this.id = id
        this.alignment = alignment
        var groupSize = if (fixSize > 0 && fixSize < 9) fixSize else Formulas.nextGaussian(1.0, 8.0).toInt()
        var guid = -1
        var haveSameAlign = false

        if (extra != null) {
            groupSize--
            this.mobs[guid] = extra
            guid--
        }

        for (mob in possibles) {
            if (mob.template!!.align == this.alignment) {
                haveSameAlign = true
                break
            }
        }
        if (!haveSameAlign) {
            this.cellId = -1
            return
        }

        for (a in 0 until groupSize) {
            var Mob: MonsterGrade
            do {
                val random = Formulas.getRandomValue(0, possibles.size - 1)
                Mob = possibles[random].getCopy()
            } while (Mob.template!!.align != this.alignment)

            if (Mob.template!!.align != this.alignment)
                this.alignment = Mob.template!!.align
            this.mobs[guid] = Mob
            if (Mob.template!!.aggroDistance > this.aggroDistance)
                this.aggroDistance = Mob.template!!.aggroDistance
            guid--
        }

        this.cellId = if (cell == -1) map.randomFreeCellId else cell
        while (map.containsForbiddenCellSpawn(this.cellId))
            this.cellId = map.randomFreeCellId
        if (this.cellId == 0)
            return
        this.orientation = if (map.id == 11095) 3 else Formulas.getRandomValue(0, 3) * 2 + 1
        this.isFix = false
        this.starBonus = 0
    }

    constructor(id: Int, cellId: Int, groupData: String, objects: String, stars: Short) {
        this.id = id
        this.alignment = Constant.ALIGNEMENT_NEUTRE
        this.cellId = cellId
        this.isFix = false
        this.orientation = Formulas.getRandomValue(0, 3) * 2 + 1
        this.starBonus = stars

        var guid = -1

        for (data in groupData.split(";")) {
            if (data.equals("", ignoreCase = true))
                continue
            val infos = data.split(",")

            try {
                val idMonster = infos[0].toInt()
                val min = infos[1].toInt()
                val max = infos[2].toInt()
                val m = World.world.getMonstre(idMonster)
                val mgs = ArrayList<MonsterGrade>()
                //on ajoute a la liste les grades possibles

                for (MG in m!!.grades.values)
                    if (MG.level >= min && MG.level <= max)
                        mgs.add(MG)
                if (mgs.isEmpty())
                    continue
                if (m!!.align != alignment)
                    alignment = m!!.align
                //On prend un grade au hasard entre 0 et size -1 parmis les mobs possibles
                this.mobs[guid] = mgs[Formulas.getRandomValue(0, mgs.size - 1)]
                if (m!!.aggroDistance > this.aggroDistance)
                    this.aggroDistance = m!!.aggroDistance
                guid--
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (objects.isNotEmpty()) {
            this.objects = ArrayList()
            for (value in objects.split(",")) {
                val gameObject = World.world.getGameObject(value.toInt())
                if (gameObject != null)
                    this.objects!!.add(gameObject)
            }
        }
    }

    constructor(id: Int, map: GameMap?, cellId: Int, groupData: String) {
        this.id = id
        this.alignment = Constant.ALIGNEMENT_NEUTRE
        this.cellId = cellId
        this.isFix = true
        var guid = -1
        var star = false
        for (data in groupData.split(";")) {
            if (data.equals("", ignoreCase = true))
                continue
            val infos = data.split(",")
            try {
                val idMonster = infos[0].toInt()
                val min = infos[1].toInt()
                val max = infos[2].toInt()
                val m = World.world.getMonstre(idMonster)
                val mgs = ArrayList<MonsterGrade>()
                //on ajoute a la liste les grades possibles

                for (MG in m!!.grades.values) {
                    if (MG.baseXp != 0)
                        star = true
                    if (MG.level >= min && MG.level <= max)
                        mgs.add(MG)
                }
                if (mgs.isEmpty())
                    continue
                if (m!!.align != alignment)
                    alignment = m!!.align
                //On prend un grade au hasard entre 0 et size -1 parmis les mobs possibles
                this.mobs[guid] = mgs[Formulas.getRandomValue(0, mgs.size - 1)]
                if (m!!.aggroDistance > this.aggroDistance)
                    this.aggroDistance = m!!.aggroDistance
                guid--
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        this.orientation = if (map != null && map.id == 11095) 3 else Formulas.getRandomValue(0, 3) * 2 + 1
        this.starBonus = (if (star) 0 else -1).toShort()
    }

    constructor(id: Int, map: GameMap?, def: MobGroupDef) {
        this.id = id
        this.alignment = Constant.ALIGNEMENT_NEUTRE
        this.cellId = def.cellId
        if (this.cellId == -1) {
            this.cellId = map!!.randomFreeCellId
        }

        this.isFix = true
        var guid = -1
        var star = false

        // Compute aggro distance, faction, if we should enable XP/Drop bonus
        for (mg in def.randomize()) {
            if (mg!!.baseXp != 0) star = true
            val m = mg.template
            this.mobs[guid] = mg
            if (m.aggroDistance > this.aggroDistance)
                this.aggroDistance = m.aggroDistance
            guid--
        }

        this.orientation = if (map != null && map.id == 11095) 3 else Formulas.getRandomValue(0, 3) * 2 + 1
        this.starBonus = (if (star) 0 else -1).toShort()
    }

    fun setSubArea(sa: Int) {
        this.subarea = sa
    }

    fun changeAgro() {
        if (!hasChangedAgro) {
            if (this.haveMineur()) {
                // 29 : sous-terrain
                // 96 : exploitation minire d'astrub
                // 31 : passage vers brakmar
                if (this.subarea != 29 && this.subarea != 96 && this.subarea != 31) {
                    this.removeAgro(118)
                }
            }
        }
        hasChangedAgro = true
    }

    fun removeAgro(id: Int) {
        this.aggroDistance = 0
        for (e in this.mobs.entries) {
            val mb = e.value
            if (mb.template!!.id != id) {
                if (mb.template!!.aggroDistance > this.aggroDistance) {
                    this.aggroDistance = mb.template!!.aggroDistance
                }
            }
        }
    }

    fun haveMineur(): Boolean {
        for (e in this.mobs.entries) {
            val mb = e.value
            if (mb.template!!.id == 118) {
                return true
            }
        }
        return false
    }

    fun getAlignement(): Int = this.alignment

    fun setIsFix(isFix: Boolean) {
        this.isFix = isFix
    }

    fun addStarBonus() {
        this.starBonus = minOf(150, this.starBonus + 5).toShort()
    }

    fun getStarBonus(): Int {
        return if (this.starBonus.toInt() == -1) 0 else this.starBonus.toInt()
    }

    fun setStarBonus(starBonus: Short) {
        this.starBonus = starBonus
    }

    fun startCondTimer() {
        Timer().schedule(object : TimerTask() {
            override fun run() {
                condition = ""
            }
        }, 60000 * 10L)
    }

    fun getObjects(): ArrayList<GameObject>? {
        if (this.objects == null && Config.modeHeroic)
            this.objects = ArrayList()
        else if (!Config.modeHeroic)
            return ArrayList()
        return objects
    }

    fun encodeGM(): String {
        val packet = StringBuilder()

        if (this.mobs.isNotEmpty()) {
            val ids = StringBuilder()
            val gfx = StringBuilder()
            val levels = StringBuilder()
            val colors = StringBuilder()
            var first = true

            for (monster in this.mobs.values) {
                if (!first) {
                    ids.append(",")
                    gfx.append(",")
                    levels.append(",")
                }
                ids.append(monster.template!!.id)
                gfx.append(monster.template!!.gfxId).append("^").append(monster.getSize())
                levels.append(monster.level)
                colors.append(monster.template!!.colors).append(";0,0,0,0;")
                first = false
            }
            packet.append("+").append(this.cellId).append(";").append(this.orientation).append(";")
            packet.append(getStarBonus()) // bonus en pourcentage (toile/20%) // Actuellement 1%/min
            packet.append(";").append(this.id).append(";").append(ids).append(";-3;").append(gfx).append(";")
                .append(levels).append(";").append(colors)
        }

        return packet.toString()
    }

    companion object {
        @JvmStatic
        fun parseMobGroupLevels(groupData: String): List<Pair<Int, List<Int>>> {
            val out = LinkedList<Pair<Int, List<Int>>>()
            groupData.split(";").forEach { s ->
                val parts = s.split(",")
                val idMonster = parts[0].toInt()
                val min = parts[1].toInt()
                val max = parts[2].toInt()

                try {
                    // Not really random anymore, but that will be replaced by script anyway
                    out.add(
                        Pair(idMonster, World.world.getMonstre(idMonster)!!.grades.values.stream()
                            .filter { it.level >= min && it.level <= max }
                            .map { it.grade }.collect(Collectors.toList()))
                    )
                } catch (e: NullPointerException) {
                    throw NullPointerException(String.format("Monster #%d Grade for levels %d or %d does not exist", idMonster, min, max))
                }
            }
            return out
        }
    }
}
