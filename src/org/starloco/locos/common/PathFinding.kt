package org.starloco.locos.common

import org.starloco.locos.area.map.GameCase
import org.starloco.locos.area.map.GameMap
import org.starloco.locos.area.map.OrthogonalProj
import org.starloco.locos.client.Player
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.util.AStarPathFinding
import org.starloco.locos.fight.traps.Glyph
import org.starloco.locos.fight.traps.Trap
import org.starloco.locos.game.GameServer
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(PathFinding::class.java)

class PathFinding {
    companion object {
        @JvmStatic
        fun isValidPath(map: GameMap, cellID: Int,
                        pathRef: AtomicReference<String>, fight: Fight?, perso: Player?,
                        targetCell: Int): Int {
            val nSteps = AtomicReference(0)
            nSteps.set(0)
            synchronized(nSteps) {
                nSteps.set(0)
                var newPos = cellID
                var steps = 0
                val path = pathRef.get()
                var newPath = ""
                var i = 0
                while (i < path.length) {
                    val smallPath = path.substring(i, i + 3)
                    val dir = smallPath[0]
                    val dirCaseID = World.world.cryptManager.cellCode_To_ID(smallPath.substring(1))
                    nSteps.set(0)
                    //Si en combat et Si Pas debut du path, on verifie tacle
                    if (fight != null && i != 0 && getEnemyFighterArround(newPos, map, fight, true) != null) {
                        pathRef.set(newPath)
                        return steps
                    }
                    //Si en combat, et pas au debut du path
                    if (fight != null && i != 0) {
                        for (trap in fight.traps) {
                            if (getDistanceBetween(map, trap.cell.cellId, newPos) <= trap.size) {
                                pathRef.set(newPath)
                                return steps
                            }
                        }
                    }

                    val aPathInfos = ValidSinglePath(nSteps, newPos, smallPath, map, fight, perso, targetCell).split(":")
                    if (aPathInfos[0].equals("stop", ignoreCase = true)) {
                        newPos = aPathInfos[1].toInt()
                        steps += nSteps.get()
                        newPath += dir.toString() + CryptManager.cellID_To_Code(newPos)
                        pathRef.set(newPath)
                        return -steps
                    } else if (aPathInfos[0].equals("ok", ignoreCase = true)) {
                        newPos = dirCaseID
                        steps += nSteps.get()
                    } else if (aPathInfos[0].equals("stoptp", ignoreCase = true)) {
                        newPos = aPathInfos[1].toInt()
                        steps += nSteps.get()
                        newPath += dir.toString() + CryptManager.cellID_To_Code(newPos)
                        pathRef.set(newPath)
                        return -steps - 10000
                    } else {
                        pathRef.set(newPath)
                        return -1000
                    }
                    newPath += dir.toString() + CryptManager.cellID_To_Code(newPos)
                    i += 3
                }
                pathRef.set(newPath)
                return steps
            }
        }

        @JvmStatic
        fun getEnemyFighterArround(cellID: Int, map: GameMap?, fight: Fight, returnNull: Boolean): ArrayList<Fighter>? {
            val dirs = charArrayOf('b', 'd', 'f', 'h')
            val enemy = ArrayList<Fighter>()
            if (map == null) return enemy

            for (dir in dirs) {
                val cell = map.getCase(GetCaseIDFromDirection(cellID, dir, map, false).toShort().toInt())
                if (cell != null) {
                    val f = cell.firstFighter
                    if (f != null) {
                        if (f.fight !== fight || f.isHidden())
                            continue
                        if (f.team != fight.getFighterByGameOrder()!!.team)
                            enemy.add(f)
                    }
                }
            }
            if (returnNull && (enemy.isEmpty() || enemy.size == 4))
                return null

            return enemy
        }

        @JvmStatic
        fun isNextTo(map: GameMap, cell1: Int, cell2: Int): Boolean {
            var result = false
            if (cell1 + 14 == cell2)
                result = true
            else if (cell1 + 15 == cell2)
                result = true
            else
                result = cell1 - 14 == cell2 || cell1 - 15 == cell2
            return result
        }

        @JvmStatic
        fun ValidSinglePath(nSteps: AtomicReference<Int>, CurrentPos: Int, Path: String, map: GameMap,
                            fight: Fight?, perso: Player?, targetCell: Int): String {
            nSteps.set(0)
            val dir = Path[0]
            val dirCaseID = World.world.cryptManager.cellCode_To_ID(Path.substring(1))
            var check = if ("353;339;325;311;297;283;269;255;241;227;213;228;368;354;340;326;312;298;284;270;256;242;243;257;271;285;299;313;327;341;355;369;383".contains(targetCell.toString())) 1 else 0

            if (fight != null && fight.isOccuped(dirCaseID))
                return "no:"

            if (perso != null) {
                if (perso.getCases)
                    if (!perso.thisCases.contains(CurrentPos))
                        perso.thisCases.add(CurrentPos)
            }
            var lastPos = CurrentPos
            var oldPos = CurrentPos

            nSteps.set(1)
            while (nSteps.get() <= 64) {
                if (GetCaseIDFromDirection(lastPos, dir, map, fight != null) == dirCaseID) {
                    if (fight != null && fight.isOccuped(dirCaseID))
                        return "stop:$lastPos"
                    val cell = map.getCase(dirCaseID)
                    if (map.id == 2019) {
                        if (cell!!.cellId == 297 && ((cell.players != null && cell.players.isNotEmpty()) || perso!!.sexe == 0))
                            return "stop:$oldPos"
                        if (cell.cellId == 282 && ((cell.players != null && cell.players.isNotEmpty()) || perso!!.sexe == 1))
                            return "stop:$oldPos"
                    }
                    if (cell!!.isWalkable(true, fight != null, targetCell)) {
                        return "ok:"
                    } else {
                        nSteps.set(nSteps.get() - 1)
                        return "stop:$lastPos"
                    }
                } else {
                    lastPos = GetCaseIDFromDirection(lastPos, dir, map, fight != null)
                }

                if (fight == null) {
                    if (perso!!.curMap.id == 9588) {
                        val cell = "353;339;325;311;297;283;269;255;241;227;213;228;368;354;340;326;312;298;284;270;256;242;243;257;271;285;299;313;327;341;355;369;383"
                        if (cell.contains(lastPos.toString()))
                            check++
                        if (check > 1)
                            return "stoptp:$lastPos"
                    }
                    try {
                        if (perso.getCases)
                            if (!perso.thisCases.contains(lastPos))
                                perso.thisCases.add(lastPos)
                    } catch (e: Exception) {
                        log.error("unexpected error", e)
                }

                    if (lastPos < 0) {
                        nSteps.set(nSteps.get() + 1)
                        continue
                    }
                    val _case = map.getCase(lastPos)
                    if (_case == null) {
                        nSteps.set(nSteps.get() + 1)
                        continue
                    }
                    if (map.id == 2019) {
                        if (_case.cellId == 297 && ((_case.players != null && _case.players.isNotEmpty()) || perso.sexe == 0))
                            return "stop:$oldPos"
                        if (_case.cellId == 282 && ((_case.players != null && _case.players.isNotEmpty()) || perso.sexe == 1))
                            return "stop:$oldPos"
                    }
                    if (map.data.cellHasMoveEndActions(_case.cellId))
                        return "stop:$lastPos"
                    if (map.isAggroByMob(perso, lastPos))
                        return "stop:$lastPos"
                    if (!map.getCase(lastPos)!!.isWalkable(true, false, targetCell))
                        return "stop:$oldPos"
                    oldPos = lastPos
                } else {
                    if (fight.isOccuped(lastPos))
                        return "no:"
                    if (getEnemyFighterArround(lastPos, map, fight, true) != null)//Si ennemie proche
                        return "stop:$lastPos"
                    for (p in fight.traps) {
                        if (getDistanceBetween(map, p.cell.cellId, lastPos) <= p.size) {//on arrete le deplacement sur la 1ere case du piege
                            return "stop:$lastPos"
                        }
                    }
                }
                nSteps.set(nSteps.get() + 1)
            }
            return "no:"
        }

        @JvmStatic
        fun getAllCaseIdAllDirrection(caseId: Int, map: GameMap): ArrayList<Int> {
            val list = ArrayList<Int>()
            val dir = charArrayOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h')
            for (d in dir) {
                val _c = GetCaseIDFromDirection(caseId, d, map, false)
                if (_c > 0)
                    list.add(_c)
            }
            return list
        }

        @JvmStatic
        fun GetCaseIDFromDirection(cellId: Int, dir: Char, map: GameMap?, fight: Boolean): Int {
            if (map == null)
                return -1
            var cell = -1
            when (dir) {
                'a' -> cell = if (fight) -1 else cellId + 1
                'b' -> cell = cellId + map.w
                'c' -> cell = if (fight) -1 else cellId + (map.w * 2 - 1)
                'd' -> cell = cellId + (map.w - 1)
                'e' -> cell = if (fight) -1 else cellId - 1
                'f' -> cell = cellId - map.w
                'g' -> cell = if (fight) -1 else cellId - (map.w * 2 - 1)
                'h' -> cell = cellId - map.w + 1
            }
            if (OrthogonalProj.isEdgeCell(map.data.width, map.data.height, cell))
                return -1
            return cell
        }

        @JvmStatic
        fun getDistanceBetween(map: GameMap?, id1: Int, id2: Int): Int {
            if (id1 == id2)
                return 0
            if (map == null)
                return 0

            val diffX = abs(getCellXCoord(map, id1) - getCellXCoord(map, id2))
            val diffY = abs(getCellYCoord(map, id1) - getCellYCoord(map, id2))
            return diffX + diffY
        }

        @JvmStatic
        fun getEnemyAround(cellId: Int, map: GameMap, fight: Fight): Fighter? {
            val dirs = charArrayOf('b', 'd', 'f', 'h')
            for (dir in dirs) {
                val cell = map.getCase(GetCaseIDFromDirection(cellId, dir, map, false))
                if (cell == null)
                    continue
                val f = cell.firstFighter

                if (f != null)
                    if (f.fight === fight)
                        if (f.team != fight.getFighterByGameOrder()!!.team)
                            return f
            }
            return null
        }

        @JvmStatic
        fun getEnemiesAround(cellId: Int, map: GameMap, fight: Fight): List<Fighter> {
            val array = ArrayList<Fighter>()
            for (dir in charArrayOf('b', 'd', 'f', 'h')) {
                val cell = map.getCase(GetCaseIDFromDirection(cellId, dir, map, false))
                if (cell != null) {
                    val f = cell.firstFighter
                    if (f != null)
                        if (f.fight === fight)
                            if (f.team != fight.getFighterByGameOrder()!!.team)
                                array.add(f)
                }
            }
            return array
        }

        @JvmStatic
        fun newCaseAfterPush(fight: Fight, CCase: GameCase, TCase: GameCase, value: Int): Int {
            var value = value
            val map = fight.map!!
            if (CCase.cellId == TCase.cellId)
                return 0

            var c = getDirBetweenTwoCase(CCase.cellId, TCase.cellId, map, true)
            var id = TCase.cellId

            if (value < 0) {
                c = getOpositeDirection(c)
                value = -value
            }
            var b = false
            for (a in 0 until value) {
                val nextCase = GetCaseIDFromDirection(id, c, map, true)

                if (map.getCase(nextCase) != null && map.getCase(nextCase)!!.isWalkableFight() && map.getCase(nextCase)!!.firstFighter == null)
                    id = nextCase
                else
                    return -(value - a)

                for (trap in fight.traps) {
                    val trapCell = trap.cell
                    val nextCell = map.getCase(nextCase)
                    if (getDistanceBetween(map, trapCell.cellId, nextCell!!.cellId) <= trap.size) {
                        if (!casesAreInSameLine(map, trapCell.cellId, nextCell.cellId, 'z', 15))
                            id = GetCaseIDFromDirection(nextCase, c, map, true)
                        b = true
                    }
                }

                if (b) break
            }

            if (id == TCase.cellId) id = 0
            return id
        }


        @JvmStatic
        fun newCaseAfterPush(fight: Fight, currentCell: GameCase, targetCell: GameCase, value: Int, piege: Boolean): Int {
            var value = value
            val map = fight.map!!

            if (currentCell.cellId == targetCell.cellId)
                return 0
            var dir = getDirBetweenTwoCase(currentCell.cellId, targetCell.cellId, map, true)
            var id = targetCell.cellId
            var nextCase = 0

            if (value < 0) {
                dir = getOpositeDirection(dir)
                value = -value
            }

            var b = false
            for (a in 0 until value) {
                nextCase = GetCaseIDFromDirection(id, dir, map, true)

                if (map.getCase(nextCase) != null && map.getCase(nextCase)!!.isWalkableFight() && map.getCase(nextCase)!!.fighters.isEmpty())
                    id = nextCase
                else
                    return -(value - a)

                for (trap in fight.traps) {
                    val trapCell = trap.cell
                    val nextCell = map.getCase(nextCase)
                    if (getDistanceBetween(map, trapCell.cellId, nextCell!!.cellId) <= trap.size) {
                        if (!casesAreInSameLine(map, trapCell.cellId, nextCell.cellId, 'z', 15))
                            id = GetCaseIDFromDirection(nextCase, dir, map, true)
                        b = true
                        break
                    }
                }

                if (b) break
            }

            if (id == targetCell.cellId)
                return 0
            return id
        }

        @JvmStatic
        fun getDistanceBetweenTwoCase(map: GameMap?, c1: GameCase?, c2: GameCase?): Int {
            var dist = 0
            if (c1 == null || c2 == null) {
                return dist
            }
            if (c1.cellId == c2.cellId)
                return dist
            var id = c1.cellId
            val c = getDirBetweenTwoCase(c1.cellId, c2.cellId, map, true)

            while (map != null && c2 !== map.getCase(id)) {
                id = GetCaseIDFromDirection(id, c, map, true)
                if (map.getCase(id) == null) {
                    return dist
                }
                dist++
            }
            return dist
        }

        @JvmStatic
        fun getOpositeDirection(c: Char): Char {
            return when (c) {
                'a' -> 'e'
                'b' -> 'f'
                'c' -> 'g'
                'd' -> 'h'
                'e' -> 'a'
                'f' -> 'b'
                'g' -> 'c'
                'h' -> 'd'
                else -> 0.toChar()
            }
        }

        @JvmStatic
        fun getCaseBetweenEnemy(cellId: Int, map: GameMap?, fight: Fight): Int {
            if (map == null) return 0
            val dirs = charArrayOf('f', 'd', 'b', 'h')
            for (dir in dirs) {
                val id = GetCaseIDFromDirection(cellId, dir, map, false)
                val cell = map.getCase(id)
                if (cell == null)
                    continue
                val f = cell.firstFighter

                if (f == null && cell.isWalkableFight())
                    return cell.cellId
            }
            return 0
        }

        @JvmStatic
        fun getAvailableCellArround(fight: Fight?, cellId: Int, cellsUnavailable: List<Int>?): Int {
            if (fight == null || fight.map!! == null) return 0
            val dirs = charArrayOf('f', 'd', 'b', 'h')

            for (dir in Formulas.shuffleCharArray(dirs)) {
                val id = GetCaseIDFromDirection(cellId, dir, fight.map!!, false)
                val cell = fight.map!!.getCase(id)

                if (cell != null) {
                    val fighter = cell.firstFighter
                    if (fighter == null && cell.isWalkableFight()) {
                        if (cellsUnavailable != null && cellsUnavailable.contains(cell.cellId))
                            continue
                        return cell.cellId
                    }
                }
            }
            return 0
        }

        @JvmStatic
        fun getNearestligneGA(fight: Fight, startCell: Int,
                              endCell: Int, forbidens0: ArrayList<GameCase>?, distmin: Int): Int {
            val map = fight.map!!
            val glyphs = ArrayList<Glyph>()//Copie du tableau
            glyphs.addAll(fight.glyphs)
            var dist = 1000
            //On prend la cellule autour de la cible, la plus proche
            var cellID = startCell
            val forbidens = forbidens0 ?: ArrayList()
            val dirs = charArrayOf('b', 'd', 'f', 'h')
            for (d in dirs) {
                var c = GetCaseIDFromDirection(startCell, d, map, true)
                if (map.getCase(c) == null)
                    continue
                var dis = getDistanceBetween(map, endCell, c)
                var dis2 = getDistanceBetween(map, startCell, c)
                // Si la distance est strictement inferieur a 1000 et que la case
                // est marchable et que personne ne
                // se trouve dessus et que la case n'est pas interdite
                if (dis < dist && dis2 <= distmin && map.getCase(c)!!.isWalkable(true, true, -1)
                    && map.getCase(c)!!.firstFighter == null
                    && !forbidens.contains(map.getCase(c))) {
                    var ok1 = true
                    for (g in glyphs) {
                        if (getDistanceBetween(map, c, g.cell.cellId) <= g.size && g.spell != 476)
                            ok1 = false
                    }

                    if (!ok1)
                        continue
                    // On cree la distance
                    dist = dis
                    // On modifie la cellule
                    cellID = c
                } else if (dis < dist && map.getCase(c)!!.isWalkable(true, true, -1)
                    && map.getCase(c)!!.firstFighter == null
                    && !forbidens.contains(map.getCase(c))) {
                    var ok1 = true
                    for (g in glyphs) {
                        if (getDistanceBetween(map, c, g.cell.cellId) <= g.size && g.spell != 476)
                            ok1 = false
                    }

                    if (!ok1)
                        continue
                    dist = dis
                    cellID = c
                }
                var ok = false
                while (!ok) {
                    val h = GetCaseIDFromDirection(c, d, map, true)
                    if (map.getCase(h) == null)
                        ok = true
                    dis = getDistanceBetween(map, endCell, c)
                    dis2 = getDistanceBetween(map, startCell, c)
                    if (dis < dist && dis2 <= distmin && map.getCase(c)!!.isWalkable(true, true, -1)
                        && map.getCase(c)!!.firstFighter == null
                        && !forbidens.contains(map.getCase(c))) {
                        var ok1 = true
                        for (g in glyphs) {
                            if (getDistanceBetween(map, c, g.cell.cellId) <= g.size && g.spell != 476)
                                ok1 = false
                        }

                        if (!ok1)
                            continue
                        dist = dis
                        cellID = c
                    } else if (dis < dist && map.getCase(c)!!.isWalkable(true, true, -1)
                        && map.getCase(c)!!.firstFighter == null
                        && !forbidens.contains(map.getCase(c))) {
                        var ok1 = true
                        for (g in glyphs) {
                            if (getDistanceBetween(map, c, g.cell.cellId) <= g.size && g.spell != 476)
                                ok1 = false
                        }

                        if (!ok1)
                            continue
                        dist = dis
                        cellID = c
                    }
                    c = h
                }
            }

            return if (cellID == startCell) -1 else cellID
        }


        @JvmStatic
        fun casesAreInSameLine(map: GameMap, c1: GameCase, c2: GameCase, max: Int): Boolean {
            val dir = getDirBetweenTwoCase(c1.cellId, c2.cellId, map, true)
            if (dir.code != 0) {
                var c = c1
                for (a in 0 until max) {
                    if (GetCaseIDFromDirection(c.cellId, dir, map, true) == c2.cellId)
                        return true
                    if (GetCaseIDFromDirection(c.cellId, dir, map, true) == -1)
                        break
                    if (!c.isWalkableFight() || c.firstFighter != null)
                        break
                    c = map.getCase(GetCaseIDFromDirection(c.cellId, dir, map, true))!!
                }
            }
            return false
        }

        @JvmStatic
        fun casesAreInSameLine(map: GameMap, c1: Int, c2: Int, dir: Char, max: Int): Boolean {
            var c1 = c1
            if (c1 == c2)
                return true

            if (dir != 'z')//Si la direction est definie
            {
                for (a in 0 until max) {
                    if (GetCaseIDFromDirection(c1, dir, map, true) == c2)
                        return true
                    if (GetCaseIDFromDirection(c1, dir, map, true) == -1)
                        break
                    c1 = GetCaseIDFromDirection(c1, dir, map, true)
                }
            } else { //Si on doit chercher dans toutes les directions
                val dirs = charArrayOf('b', 'd', 'f', 'h')
                for (d in dirs) {
                    var c = c1
                    for (a in 0 until max) {
                        if (GetCaseIDFromDirection(c, d, map, true) == c2)
                            return true
                        c = GetCaseIDFromDirection(c, d, map, true)
                    }
                }
            }
            return false
        }

        @JvmStatic
        fun getCiblesByZoneByWeapon(fight: Fight,
                                    type: Int, cell: GameCase, castCellID: Int): ArrayList<Fighter> {
            val cibles = ArrayList<Fighter>()
            val c = getDirBetweenTwoCase(castCellID, cell.cellId, fight.map!!, true)
            if (c.code == 0) {
                //On cible quand meme le fighter sur la case
                if (cell.firstFighter != null)
                    cibles.add(cell.firstFighter!!)
                return cibles
            }

            when (type) {
                //Cases devant celle ou l'on vise
                Constant.ITEM_TYPE_MARTEAU -> {
                    val f = getFighter2CellBefore(castCellID, c, fight.map!!)
                    if (f != null)
                        cibles.add(f)
                    val g = get1StFighterOnCellFromDirection(fight.map!!, castCellID, (c - 1))
                    if (g != null)
                        cibles.add(g)//Ajoute case a gauche
                    val h = get1StFighterOnCellFromDirection(fight.map!!, castCellID, (c + 1))
                    if (h != null)
                        cibles.add(h)//Ajoute case a droite
                    val i = cell.firstFighter
                    if (i != null)
                        cibles.add(i)
                }
                Constant.ITEM_TYPE_BATON -> {
                    val dist = getDistanceBetween(fight.map!!, cell.cellId, castCellID)
                    val newCell = getCaseIDFromDirrection(castCellID, c, fight.map!!)

                    val j = get1StFighterOnCellFromDirection(fight.map!!, if (dist > 1) newCell else castCellID, (c - 1))
                    if (j != null)
                        cibles.add(j)//Ajoute case a gauche
                    val k = get1StFighterOnCellFromDirection(fight.map!!, if (dist > 1) newCell else castCellID, (c + 1))
                    if (k != null)
                        cibles.add(k)//Ajoute case a droite
                    val l = cell.firstFighter
                    if (l != null)
                        cibles.add(l)//Ajoute case cible
                }
                Constant.ITEM_TYPE_PIOCHE, Constant.ITEM_TYPE_EPEE, Constant.ITEM_TYPE_FAUX, Constant.ITEM_TYPE_DAGUES, Constant.ITEM_TYPE_BAGUETTE, Constant.ITEM_TYPE_PELLE, Constant.ITEM_TYPE_ARC, Constant.ITEM_TYPE_HACHE, Constant.ITEM_TYPE_OUTIL -> {
                    val m = cell.firstFighter
                    if (m != null)
                        cibles.add(m)
                }
            }
            return cibles
        }

        private fun get1StFighterOnCellFromDirection(map: GameMap?, id: Int,
                                                     c0: Char): Fighter? {
            var c = c0
            if (c == 'a' - 1)
                c = 'h'
            if (c == 'h' + 1) c = 'a'
            val cell = map?.getCase(GetCaseIDFromDirection(id, c, map, false))
            return cell?.firstFighter
        }

        private fun getFighter2CellBefore(CellID: Int, c: Char, map: GameMap?): Fighter? {
            val new2CellID = GetCaseIDFromDirection(GetCaseIDFromDirection(CellID, c, map, false), c, map, false)
            val cell = map?.getCase(new2CellID)
            return cell?.firstFighter
        }

        @JvmStatic
        fun getDirBetweenTwoCase(cell1ID: Int, cell2ID: Int, map: GameMap?,
                                 Combat: Boolean): Char {
            val dirs = ArrayList<Char>()
            dirs.add('b')
            dirs.add('d')
            dirs.add('f')
            dirs.add('h')
            if (!Combat) {
                dirs.add('a')
                dirs.add('b')
                dirs.add('c')
                dirs.add('d')
            }
            for (c in dirs) {
                var cell = cell1ID
                for (i in 0..64) {
                    if (GetCaseIDFromDirection(cell, c, map, Combat) == cell2ID)
                        return c
                    cell = GetCaseIDFromDirection(cell, c, map, Combat)
                }
            }
            return 0.toChar()
        }

        @JvmStatic
        fun canWalkToThisCell(map: GameMap, cell1: Int, cell2: Int, fight: Boolean): Boolean {
            val path = AStarPathFinding(map, cell1, cell2).getShortestPath()
            if (path == null || path.isEmpty()) return path != null
            val dir = getDirBetweenTwoCase(cell2, path[path.size - 1].cellId, map, true)
            val id = map.getCase(GetCaseIDFromDirection(cell2, dir, map, true))

            return path.contains(id)
        }

        @JvmStatic
        fun getCellListFromAreaString(map: GameMap?, cellID: Int, castCellID: Int, zoneStr: String, PONum: Int, isCC: Boolean): List<GameCase> {
            var cellID = cellID
            if (map == null || map.getCase(cellID) == null)
                return Collections.emptyList()

            val cases = ArrayList<GameCase>()

            cases.add(map.getCase(cellID)!!)

            if (zoneStr.length < PONum + 2)
                return cases

            val size = CryptManager.getIntByHashedValue(zoneStr[PONum + 1])

            when (zoneStr[PONum]) {
                'C' -> {// Cercle
                    for (a in 0 until size) {
                        val dirs = charArrayOf('b', 'd', 'f', 'h')
                        val cases2 = ArrayList(cases)
                        for (aCell in cases2) {
                            for (d in dirs) {
                                val cell = map.getCase(GetCaseIDFromDirection(aCell.cellId, d, map, true))
                                if (cell == null)
                                    continue
                                if (!cases.contains(cell))
                                    cases.add(cell)
                            }
                        }
                    }
                }

                'X' -> {// Croix
                    val dirs = charArrayOf('b', 'd', 'f', 'h')
                    for (d in dirs) {
                        var cID = cellID
                        for (a in 0 until size) {
                            map.getCase(GetCaseIDFromDirection(cID, d, map, true))?.let { cases.add(it) }
                            cID = GetCaseIDFromDirection(cID, d, map, true)
                        }
                    }
                }

                'L' -> {// Ligne
                    val dir = getDirBetweenTwoCase(castCellID, cellID, map, true)
                    for (a in 0 until size) {
                        map.getCase(GetCaseIDFromDirection(cellID, dir, map, true))?.let { cases.add(it) }
                        cellID = GetCaseIDFromDirection(cellID, dir, map, true)
                    }
                }

                'P' -> {}
                'T' -> {}
                'O' -> {}
                'D' -> {}
                'R' -> { // Rectangle
                    if (size == 0) {
                        return cases
                    }

                    val cellX = OrthogonalProj.getOrthX(map.data.width, cellID)
                    val cellY = OrthogonalProj.getOrthY(map.data.width, cellID)

                    val minX = cellX - size
                    val maxX = cellX + size
                    val minY = cellY - size
                    val maxY = cellY + size
                }
                else -> GameServer.a()
            }
            return cases
        }

        @JvmStatic
        fun getCellXCoord(map: GameMap?, cellID: Int): Int {
            if (map == null)
                return 0
            val w = map.w
            return (cellID - (w - 1) * getCellYCoord(map, cellID)) / w
        }

        @JvmStatic
        fun getCellYCoord(map: GameMap, cellID: Int): Int {
            val w = map.w
            val loc5 = cellID / (w * 2 - 1)
            val loc6 = cellID - loc5 * (w * 2 - 1)
            val loc7 = loc6 % w
            return loc5 - loc7
        }

        @JvmStatic
        fun getNearestCellAround(map: GameMap?, startCell: Int, endCell: Int, forbidden0: ArrayList<GameCase>?): Int {
            if (map == null)
                return -1
            val forbidden = forbidden0 ?: ArrayList()
            var dist = 1000
            var cellId = startCell

            for (d in charArrayOf('b', 'd', 'f', 'h')) {
                val newCellId = GetCaseIDFromDirection(startCell, d, map, true)
                val cell = map.getCase(newCellId)

                if (cell != null) {
                    val distance = getDistanceBetween(map, endCell, newCellId)

                    if (distance < dist && cell.isWalkable(true, true, -1) && cell.firstFighter == null
                        && !forbidden.contains(cell) && OrthogonalProj.isEdgeCell(map.data.width, map.data.height, newCellId)) {
                        dist = distance
                        cellId = newCellId
                    }
                }
            }
            return if (cellId == startCell) -1 else cellId
        }

        @JvmStatic
        fun getNearestCellAroundGA(map: GameMap, startCell: Int,
                                   endCell: Int, forbidens0: ArrayList<GameCase>?): Int {
            //On prend la cellule autour de la cible, la plus proche
            var dist = 1000
            var cellID = startCell
            val forbidens = forbidens0 ?: ArrayList()
            val dirs = charArrayOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h')
            for (d in dirs) {
                val c = GetCaseIDFromDirection(startCell, d, map, true)
                val dis = getDistanceBetween(map, endCell, c)
                if (map.getCase(c) == null)
                    continue
                if (dis < dist && map.getCase(c)!!.isWalkableFight()
                    && map.getCase(c)!!.firstFighter == null
                    && !forbidens.contains(map.getCase(c))) {
                    dist = dis
                    cellID = c
                }
            }

            //On renvoie -1 si pas trouve
            return if (cellID == startCell) -1 else cellID
        }

        @JvmStatic
        fun getShortestPathBetween(map: GameMap, start: Int,
                                   dest: Int, distMax: Int): ArrayList<GameCase>? {
            var curPath = ArrayList<GameCase>()
            val curPath2 = ArrayList<GameCase>()
            val closeCells = ArrayList<GameCase>()
            val limit = 1000
            var curCase = map.getCase(start)
            var stepNum = 0
            val stop = false

            while (!stop && stepNum++ <= limit) {
                val nearestCell = getNearestCellAround(map, curCase!!.cellId, dest, closeCells)
                if (nearestCell == -1) {
                    closeCells.add(curCase)
                    if (curPath.isNotEmpty()) {
                        curPath.removeAt(curPath.size - 1)
                        if (curPath.isNotEmpty())
                            curCase = curPath[curPath.size - 1]
                        else
                            curCase = map.getCase(start)
                    } else {
                        curCase = map.getCase(start)
                    }
                } else if (distMax == 0 && nearestCell == dest) {
                    curPath.add(map.getCase(dest)!!)
                    break
                } else if (distMax > getDistanceBetween(map, nearestCell, dest)) {
                    curPath.add(map.getCase(dest)!!)
                    break
                } else { //on continue
                    curCase = map.getCase(nearestCell)
                    closeCells.add(curCase!!)
                    curPath.add(curCase)
                }
            }

            curCase = map.getCase(start)
            closeCells.clear()
            if (curPath.isNotEmpty()) {
                closeCells.add(curPath[0])
            }

            while (!stop && stepNum++ <= limit) {
                val nearestCell = getNearestCellAround(map, curCase!!.cellId, dest, closeCells)
                if (nearestCell == -1) {
                    closeCells.add(curCase)
                    if (curPath2.isNotEmpty()) {
                        curPath2.removeAt(curPath2.size - 1)
                        if (curPath2.isNotEmpty())
                            curCase = curPath2[curPath2.size - 1]
                        else
                            curCase = map.getCase(start)
                    } else { //Si retour a zero
                        curCase = map.getCase(start)
                    }
                } else if (distMax == 0 && nearestCell == dest) {
                    curPath2.add(map.getCase(dest)!!)
                    break
                } else if (distMax > getDistanceBetween(map, nearestCell, dest)) {
                    curPath2.add(map.getCase(dest)!!)
                    break
                } else { //on continue
                    curCase = map.getCase(nearestCell)
                    closeCells.add(curCase!!)
                    curPath2.add(curCase)
                }
            }

            if ((curPath2.size < curPath.size && curPath2.isNotEmpty())
                || curPath.isEmpty())
                curPath = curPath2
            return curPath
        }

        @JvmStatic
        fun getShortestStringPathBetween(map: GameMap, start: Int,
                                         dest: Int, distMax: Int): String? {
            if (start == dest)
                return null
            val path = getShortestPathBetween(map, start, dest, distMax) ?: return null
            var pathstr = ""
            var curCaseID = start
            var curDir = ' '
            for (c in path) {
                val d = getDirBetweenTwoCase(curCaseID, c.cellId, map, true)
                if (d.code == 0)
                    return null
                if (curDir != d) {
                    if (path.indexOf(c) != 0)
                        pathstr = pathstr + CryptManager.cellID_To_Code(curCaseID)
                    pathstr = pathstr + d
                    curDir = d
                }
                curCaseID = c.cellId
            }
            if (curCaseID != start) {
                pathstr = pathstr + CryptManager.cellID_To_Code(curCaseID)
            }
            path.clear()
            if (pathstr.isEmpty())
                return null
            return "a" + CryptManager.cellID_To_Code(start) + pathstr
        }

        @JvmStatic
        fun checkLoS(map: GameMap, cell1: Int, cell2: Int,
                     fighter: Fighter?, isPeur: Boolean): Boolean {
            if (fighter != null && fighter.getPlayer() != null) // on ne neverifie pas (en plus du client) pour les joueurs
                return true
            val cellsToConsider = getLoSBotheringIDCases(map, cell1, cell2, true) ?: return true
            for (cellID in cellsToConsider) {
                if (map.getCase(cellID) != null)
                    if (!map.getCase(cellID)!!.blockLoS()
                        || (!map.getCase(cellID)!!.isWalkableFight() && isPeur)) {
                        return false
                    }
            }
            return true
        }

        private fun getLoSBotheringIDCases(map: GameMap,
                                           cellID1: Int, cellID2: Int, Combat: Boolean): ArrayList<Int>? {
            val toReturn = ArrayList<Int>()
            var consideredCell1 = cellID1
            var consideredCell2 = cellID2
            var dir = 'b'
            var diffX = 0
            var diffY = 0
            var compteur = 0
            val dirs = ArrayList<Char>()
            dirs.add('b')
            dirs.add('d')
            dirs.add('f')
            dirs.add('h')

            while (getDistanceBetween(map, consideredCell1, consideredCell2) > 2
                && compteur < 300) {
                diffX = (getCellXCoord(map, consideredCell1)
                        - getCellXCoord(map, consideredCell2))
                diffY = (getCellYCoord(map, consideredCell1)
                        - getCellYCoord(map, consideredCell2))
                if (abs(diffX) > abs(diffY)) { // si il ya une plus grande difference pour la premiere coordonnee
                    dir = if (diffX > 0)
                        'f'
                    else
                        'b'
                    consideredCell1 = GetCaseIDFromDirection(consideredCell1, dir, map, Combat) // on avance le chemin d'obstacles possibles
                    consideredCell2 = GetCaseIDFromDirection(consideredCell2, getOpositeDirection(dir), map, Combat) // des deux cotes
                    toReturn.add(consideredCell1) // la liste des cases potentiellement obstacles
                    toReturn.add(consideredCell2) // la liste des cases potentiellement obstacles
                } else if (abs(diffX) < abs(diffY)) { // si il y a une plus grand difference pour la seconde
                    dir = if (diffY > 0) // determine dans quel sens
                        'h'
                    else
                        'd'
                    consideredCell1 = GetCaseIDFromDirection(consideredCell1, dir, map, Combat)
                    consideredCell2 = GetCaseIDFromDirection(consideredCell2, getOpositeDirection(dir), map, Combat)
                    toReturn.add(consideredCell1)
                    toReturn.add(consideredCell2)
                } else {
                    if (compteur == 0) // si on est en diagonale parfaite
                        return getLoSBotheringCasesInDiagonal(map, cellID1, cellID2, diffX, diffY)
                    if (dir == 'f' || dir == 'b') // on change la direction dans le cas ou on se retrouve en diagonale
                        dir = if (diffY > 0)
                            'h'
                        else
                            'd'
                    else if (dir == 'h' || dir == 'd')
                        dir = if (diffX > 0)
                            'f'
                        else
                            'b'
                    consideredCell1 = GetCaseIDFromDirection(consideredCell1, dir, map, Combat)
                    consideredCell2 = GetCaseIDFromDirection(consideredCell2, getOpositeDirection(dir), map, Combat)
                    toReturn.add(consideredCell1)
                    toReturn.add(consideredCell2)
                }
                compteur++
            }
            if (getDistanceBetween(map, consideredCell1, consideredCell2) == 2) {
                dir = ' '
                diffX = (getCellXCoord(map, consideredCell1)
                        - getCellXCoord(map, consideredCell2))
                diffY = (getCellYCoord(map, consideredCell1)
                        - getCellYCoord(map, consideredCell2))
                if (diffX == 0)
                    dir = if (diffY > 0)
                        'h'
                    else
                        'd'
                if (diffY == 0)
                    dir = if (diffX > 0)
                        'f'
                    else
                        'b'
                if (dir.code != 0)
                    toReturn.add(GetCaseIDFromDirection(consideredCell1, dir, map, Combat))
            }
            return toReturn
        }

        private fun getLoSBotheringCasesInDiagonal(map: GameMap,
                                                   cellID1: Int, cellID2: Int, diffX: Int, diffY: Int): ArrayList<Int> {
            val toReturn = ArrayList<Int>()
            var dir = 'a'
            if (diffX > 0 && diffY > 0)
                dir = 'g'
            if (diffX > 0 && diffY < 0)
                dir = 'e'
            if (diffX < 0 && diffY > 0)
                dir = 'a'
            if (diffX < 0 && diffY < 0)
                dir = 'c'
            var consideredCell = cellID1
            var compteur = 0
            while (consideredCell != -1 && compteur < 100) {
                consideredCell = GetCaseIDFromDirection(consideredCell, dir, map, false)
                if (consideredCell == cellID2)
                    return toReturn
                toReturn.add(consideredCell)
                compteur++
            }
            return toReturn
        }

        @JvmStatic
        fun getFightersAround(cellID: Int, map: GameMap): ArrayList<Fighter> {
            val dirs = charArrayOf('b', 'd', 'f', 'h')
            val fighters = ArrayList<Fighter>()

            for (dir in dirs) {
                val gameCase = map.getCase(GetCaseIDFromDirection(cellID, dir, map, false))
                if (gameCase == null) continue
                val f = gameCase.firstFighter
                if (f != null)
                    fighters.add(f)
            }
            return fighters
        }

        @JvmStatic
        fun getDirEntreDosCeldas(map: GameMap?, id1: Int, id2: Int): Char {
            if (id1 == id2)
                return '0'
            if (map == null)
                return '0'
            val difX = getCellXCoord(map, id1) - getCellXCoord(map, id2)
            val difY = getCellYCoord(map, id1) - getCellYCoord(map, id2)
            val difXabs = abs(difX)
            val difYabs = abs(difY)
            if (difXabs > difYabs) {
                return if (difX > 0)
                    'f'
                else
                    'b'
            } else {
                return if (difY > 0)
                    'h'
                else
                    'd'
            }
        }

        @JvmStatic
        fun getCellArroundByDir(cellId: Int, dir: Char, map: GameMap?): Int {
            if (map == null)
                return -1

            return when (dir) {
                'b' -> cellId + map.w //En Haut a Droite.
                'd' -> cellId + (map.w - 1) //En Haut a Gauche.
                'f' -> cellId - map.w //En Bas a Gauche.
                'h' -> cellId - map.w + 1 //En Bas a Droite.
                else -> -1
            }
        }

        @JvmStatic
        fun checkIfCanPushEntity(fight: Fight, startCell: Int,
                                 endCell: Int, direction: Char): GameCase? {
            val map = fight.map!!
            val cell = map.getCase(getCellArroundByDir(startCell, direction, map))
            var oldCell = cell
            var actualCell = cell

            while (actualCell!!.cellId != endCell) {
                actualCell = map.getCase(getCellArroundByDir(actualCell.cellId, direction, map))
                if (actualCell!!.fighters.isNotEmpty()
                    || !actualCell.isWalkableFight())
                    return oldCell

                for (trap in fight.traps) {

                    if (getDistanceBetween(fight.map!!, trap.cell.cellId, actualCell.cellId) <= trap.size)
                        return actualCell
                }

                oldCell = actualCell
            }

            return null
        }

        @JvmStatic
        fun haveFighterOnThisCell(cell: Int, fight: Fight, astar: Boolean): Boolean {
            for (f in fight.getFighters(if (astar) 3 else 7)) {
                if (f.cell != null && f.cell!!.cellId == cell && !f.isDead)
                    return true
            }
            return false
        }

        @JvmStatic
        fun getCaseIDFromDirrection(CaseID: Int, Direccion: Char,
                                    map: GameMap): Int {
            return when (Direccion) {// mag.get_w() = te da el ancho del mapa
                'b' -> CaseID + map.w // diagonal derecha abajo
                'd' -> CaseID + (map.w - 1) // diagonal izquierda abajo
                'f' -> CaseID - map.w // diagonal izquierda arriba
                'h' -> CaseID - map.w + 1 // diagonal derecha arriba
                else -> -1
            }
        }

        @JvmStatic
        fun cellArroundCaseIDisOccuped(fight: Fight, cell: Int): Boolean {
            val dirs = charArrayOf('b', 'd', 'f', 'h')
            val cases = ArrayList<Int>()

            for (dir in dirs) {
                val caseID = GetCaseIDFromDirection(cell, dir, fight.map!!, true)
                cases.add(caseID)
            }
            var ha = 0
            for (o in cases.indices) {
                val c = fight.map!!.getCase(cases[o])
                if (c != null && c.firstFighter != null)
                    ha++
            }
            return ha != 4
        }

        @JvmStatic
        fun getCellsByDir(fight: Fight, startCell: Int, dir: Char, limit: Int): List<GameCase> {
            val cells = ArrayList<GameCase>()
            for (i in 0 until limit) {
                val id = GetCaseIDFromDirection(startCell, dir, fight.map!!, true)
                if (!haveFighterOnThisCell(id, fight, false)) {
                    cells.add(fight.map!!.getCase(id)!!)
                }
            }
            return cells
        }
    }
}
