package org.starloco.locos.entity.mount

import org.starloco.locos.area.map.GameMap
import org.starloco.locos.client.Player
import org.starloco.locos.client.other.Stats
import org.starloco.locos.common.Formulas
import org.starloco.locos.common.CryptManager
import org.starloco.locos.common.PathFinding
import org.starloco.locos.common.SocketManager
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.game.ExperienceTables
import org.starloco.locos.database.data.login.MountData
import org.starloco.locos.entity.map.MountPark
import org.starloco.locos.game.scheduler.Updatable
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant
import org.starloco.locos.`object`.GameObject
import org.starloco.locos.util.TimerWaiter

class Mount {

    var id = 0
    var color = 0
    var sex = 0
        private set
    var size = 0
    var name: String? = null
    var level = 0
    var exp: Long = 0
        private set
    var owner = 0
    var mapId = 0
    var cellId = 0
    var orientation = 0
    var fatigue = 0
        private set
    var energy = 0
    var reproduction = 0
        private set
    var amour = 0
        private set
    var endurance = 0
        private set
    var maturity = 0
        private set
    var state = 0
        get() {
            if (field > 10000) field = 10000
            return field
        }
    var savage = 0
        private set
    var ancestors = "?,?,?,?,?,?,?,?,?,?,?,?,?,?"
        private set
    var fecundatedDate: Long = -1
        private set
    var couple = 0
    var stats = Stats()
        private set
    val objects: MutableMap<Int, GameObject> = HashMap()
    val capacitys: MutableList<Int> = ArrayList(2)

    constructor(color: Int, owner: Int, savage: Boolean) {
        this.color = color
        this.sex = Formulas.getRandomValue(0, 1)
        this.level = 1
        this.exp = 0
        this.name = "SansNom"
        this.fatigue = 0
        this.energy = 0
        this.reproduction = if (color == 75 || color == 88) -1 else 0
        this.maturity = 0
        this.state = 0
        this.stats = Constant.getMountStats(this.color, this.level)
        this.ancestors = "?,?,?,?,?,?,?,?,?,?,?,?,?,?"
        this.size = 100
        this.owner = owner
        this.cellId = -1
        this.mapId = -1
        this.orientation = 1
        this.savage = if (savage) 1 else 0

        World.world.addMount(this)
        (DatabaseManager.get(MountData::class.java) as MountData).insert(this)
    }

    constructor(color: Int, mother: Mount, father: Mount) {
        this.color = color
        this.sex = Formulas.getRandomValue(0, 1)
        this.level = 1
        this.exp = 0
        this.name = "SansNom"
        this.fatigue = 0
        this.energy = 0
        this.reproduction = 0
        this.maturity = 0
        this.state = Formulas.getRandomValue(-10000, 10000)
        this.stats = Constant.getMountStats(this.color, this.level)

        val fatherStr = father.ancestors.split(",")
        val motherStr = mother.ancestors.split(",")
        val firstFather = fatherStr[0] + "," + fatherStr[1]
        val firstMother = motherStr[0] + "," + motherStr[1]
        val secondFather = fatherStr[2] + "," + fatherStr[3] + "," + fatherStr[4] + "," + fatherStr[5]
        val secondMother = motherStr[2] + "," + motherStr[3] + "," + motherStr[4] + "," + motherStr[5]

        this.ancestors = "${father.color},${mother.color},$firstFather,$firstMother,$secondFather,$secondMother"

        if (Formulas.getRandomValue(0, 20) == 0)
            this.capacitys.add(Formulas.getRandomValue(1, 8))

        if (father.capacitys.isNotEmpty() || mother.capacitys.isNotEmpty()) {
            if (Formulas.getRandomValue(0, 10) == 0) {
                if (Formulas.getRandomValue(0, 1) == 0) {
                    if (father.capacitys.isNotEmpty())
                        this.capacitys.add(father.capacitys[Formulas.getRandomValue(0, father.capacitys.size - 1)])
                } else {
                    if (mother.capacitys.isNotEmpty())
                        this.capacitys.add(mother.capacitys[Formulas.getRandomValue(0, mother.capacitys.size - 1)])
                }
            }
        }

        this.cellId = -1
        this.mapId = -1
        this.owner = mother.owner
        this.size = 50
        this.orientation = 1
        this.fecundatedDate = -1
        this.couple = -1
        this.savage = 0
        World.world.addMount(this)
        (DatabaseManager.get(MountData::class.java) as MountData).insert(this)
    }

    constructor(id: Int, color: Int, sexe: Int, amour: Int, endurance: Int, level: Int, exp: Long, name: String,
                fatigue: Int, energy: Int, reproduction: Int, maturity: Int, state: Int, objects: String,
                ancestors: String, capacitys: String, size: Int, cellId: Int, mapId: Int, owner: Int,
                orientation: Int, fecundatedHour: Long, couple: Int, savage: Int) {
        this.id = id
        this.color = color
        this.sex = sexe
        this.amour = amour
        this.endurance = endurance
        this.level = level
        this.exp = exp
        this.name = name
        this.fatigue = fatigue
        this.energy = energy
        this.reproduction = reproduction
        this.maturity = maturity
        this.state = state
        this.ancestors = ancestors
        this.stats = Constant.getMountStats(this.color, this.level)
        this.size = size
        this.cellId = cellId
        this.mapId = mapId
        this.owner = owner
        this.orientation = orientation
        this.fecundatedDate = fecundatedHour
        this.couple = couple
        this.savage = savage

        for (str in objects.split(";")) {
            if (str.isEmpty()) continue
            try {
                val gameObject = World.world.getGameObject(str.toInt())
                if (gameObject != null)
                    this.objects[gameObject.guid] = gameObject
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        for (str in capacitys.split(",", limit = 2))
            try {
                this.capacitys.add(str.toInt())
            } catch (e: Exception) {
                e.printStackTrace()
            }
    }

    @Synchronized
    fun checkBaby(player: Player, mp: MountPark) {
        if (this.fecundatedDate == -1L) return
        val time = Generation.getTimeGestation(Constant.getGeneration(this.color))
        val actualHours = ((System.currentTimeMillis() - this.fecundatedDate) / 3600000).toInt() + 1

        if (time < actualHours && actualHours < time + 24 * 7) {
            val coupleMount = World.world.getMountById(this.couple)
            val coupleReprod = coupleMount != null && coupleMount.capacitys.contains(3)
            val reproductrice = this.capacitys.contains(3) || coupleReprod
            var offspring = 1 + (if (reproductrice) 1 else 0)
            val value = Formulas.getRandomValue(0, 16)
            val max = 3 + (if (reproductrice) 1 else 0)

            if (value in 5..10)
                offspring = if (reproductrice) 3 else 2
            else if (value < 1)
                offspring = max
            if (this.capacitys.contains(3) && coupleReprod)
                offspring *= 2

            SocketManager.GAME_SEND_Im_PACKET(player, "1111;$offspring")
            val father = World.world.getMountById(this.couple)
            for (i in 0 until offspring) {
                val color = Constant.colorToEtable(player, this, father ?: this)
                val baby = Mount(color, this, father ?: this)
                player.curMap.mountPark!!.getEtable().add(baby)
            }

            this.aumReproduction()
            this.setFecundatedDate(-1)

            if (player.curMap.mountPark!!.hasEtableFull(player.id))
                player.send("Im1105")
            if (father != null && father.savage == 1) {
                (DatabaseManager.get(MountData::class.java) as MountData).delete(father)
                World.world.removeMount(father.id)
            }
            if (this.savage == 1) {
                (DatabaseManager.get(MountData::class.java) as MountData).delete(this)
                World.world.removeMount(this.id)

                mp.delRaising(this.id)
                player.send("Im0112; " + this.name)
            }
        } else if (actualHours >= time + 24 * 7) {
            SocketManager.GAME_SEND_Im_PACKET(player, "1112")
            this.aumReproduction()
            this.resAmor(7500)
            this.resEndurance(7500)
            this.setFecundatedDate(-1)
        }
    }

    //region getter/setter
    private fun setFecundatedDate(fecundatedDate: Int) {
        if (this.reproduction != -1)
            this.fecundatedDate = fecundatedDate.toLong()
    }
    //endregion getter/setter

    fun getStringColor(color: String?): String {
        var secondColor = ""
        if (this.capacitys.contains(9))
            secondColor = ",$color"
        if (this.color == 75)
            secondColor = "," + Constant.getStringColorDragodinde(Formulas.getRandomValue(1, 87))
        return this.color.toString() + secondColor
    }

    fun isMontable(): Int {
        val mountable = if (this.maturity < this.maxMaturity || this.fatigue == 240 || this.savage == 1) 0 else 1
        if (mountable == 1 && this.size == 50)
            this.size = 100
        return mountable
    }

    private fun isFecund(): Int {
        if (this.reproduction != -1 && this.amour >= 7500 && this.endurance >= 7500 && this.maturity == this.maxMaturity && (this.savage == 1 || this.savage == 0 && this.level >= 5))
            return 10
        return 0
    }

    fun setCastrated() {
        this.reproduction = -1
    }

    private fun isCastrated(): Boolean = this.reproduction == -1

    val actualPods: Int
        get() {
            var pods = 0
            for (gameObject in this.objects.values)
                pods += gameObject.template!!.pod * gameObject.quantity
            return pods
        }

    val maxPods: Int
        get() = Generation.getPods(Constant.getGeneration(this.color), this.level)

    fun addXp(amount: Long) {
        this.exp += amount
        val xpTable = World.world.experiences!!.mounts
        while (this.exp >= xpTable.maxXpAt(this.level) && this.level < xpTable.maxLevel()) {
            this.addLvl()
        }
    }

    private fun addLvl() {
        this.level++
        this.stats = Constant.getMountStats(this.color, this.level)
    }

    private fun stateMale() {
        this.state -= 2
        if (this.state < -10000) this.state = -10000
    }

    private fun stateFemale() {
        this.state += 2
        if (this.state < -10000) this.state = -10000
    }

    private fun setMaxEnergy() {
        this.energy = this.maxEnergy
    }

    private val maxEnergy: Int
        get() = Generation.getEnergy(Constant.getGeneration(this.color))

    private fun setMaxMaturity() {
        this.maturity = this.maxMaturity
    }

    private val maxMaturity: Int
        get() = Generation.getMaturity(Constant.getGeneration(this.color))

    private fun aumFatige() {
        this.fatigue += 1
        if (this.fatigue > 240) this.fatigue = 240
    }

    private fun aumEndurance(endurance: Int) {
        this.endurance += ((endurance / 100) * this.bonusFatigue).toInt() /* * Generation.getLearningRate(Constant.getGeneration(this.color))*/
        if (this.capacitys.contains(5)) this.endurance += 1
        if (this.endurance > 10000) this.endurance = 10000
    }

    private fun aumMaturity(Resist: Int) {
        if (this.maturity < this.maxMaturity) {
            this.maturity += ((Resist / 100) * this.bonusFatigue).toInt()
            if (this.capacitys.contains(7))
                this.maturity += Resist / 100
            if (this.size < 100) {
                val map = World.world.getMap(this.mapId)
                if (this.maxMaturity / this.maturity <= 1) {
                    this.size = 100
                    SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(map, this.id)
                    SocketManager.GAME_SEND_GM_MOUNT_TO_MAP(map, this)
                    return
                } else if (this.size < 75 && this.maxMaturity / this.maturity == 2) {
                    this.size = 75
                    SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(map, this.id)
                    SocketManager.GAME_SEND_GM_MOUNT_TO_MAP(map, this)
                    return
                } else if (this.size < 50 && this.maxMaturity / this.maturity == 3) {
                    this.size = 50
                    SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(map, this.id)
                    SocketManager.GAME_SEND_GM_MOUNT_TO_MAP(map, this)
                    return
                }
            }
        }
        if (this.maturity > this.maxMaturity) this.setMaxMaturity()
    }

    private fun aumAmor(amour: Int) {
        this.amour += ((amour / 100) * this.bonusFatigue).toInt()
        if (this.capacitys.contains(6)) this.amour += amour / 500
        if (this.amour > 10000) this.amour = 10000
    }

    private fun aumState(state: Int) {
        this.state += ((state / 100) * this.bonusFatigue).toInt()
        if (this.state > 10000) this.state = 10000
    }

    fun aumEnergy(energy: Int) {
        this.energy += ((energy / 500) * this.bonusFatigue).toInt()
        if (this.capacitys.contains(1)) this.energy += energy / 500
        if (this.energy > this.maxEnergy) this.setMaxEnergy()
    }

    private fun aumReproduction() {
        if (this.reproduction != -1) this.reproduction += 1
    }

    private fun resFatige() {
        this.fatigue -= 20
        if (this.fatigue < 0) this.fatigue = 0
    }

    private fun resAmor(amor: Int) {
        this.amour -= (amor * this.bonusFatigue).toInt()
        if (this.amour < 0) this.amour = 0
    }

    private fun resEndurance(endurance: Int) {
        this.endurance -= (endurance * this.bonusFatigue).toInt()
        if (this.endurance < 0) this.endurance = 0
    }

    private fun resState(state: Int) {
        this.state -= ((state / 100) * this.bonusFatigue).toInt()
        if (this.state < -10000) this.state = -10000
    }

    fun setToMax() {
        this.addXp(World.world.experiences!!.mounts.maxLevel().toLong())
        this.amour = 10000
        this.endurance = 10000
        this.energy = this.maxEnergy
        this.setMaxMaturity()
        (DatabaseManager.get(MountData::class.java) as MountData).update(this)
    }

    private val bonusFatigue: Double
        get() {
            if (this.fatigue in 161..170)
                return 1.15
            if (this.fatigue in 171..180)
                return 1.30
            if (this.fatigue in 181..200)
                return 1.50
            if (this.fatigue in 201..210)
                return 1.80
            if (this.fatigue in 211..220)
                return 2.10
            if (this.fatigue in 221..230)
                return 2.50
            if (this.fatigue in 231..239)
                return 3.00
            return if (fatigue == 240) 0.0 else 1.0
        }

    @Synchronized
    fun moveMounts(player: Player?, cellules: Int, remove: Boolean) {
        var action = 0
        if (player == null)
            return
        if (player.curCell.cellId == this.cellId)
            return
        var path = ""
        val map = player.curMap
        if (map.mountPark == null)
            return
        val MP = map.mountPark
        var dir = PathFinding.getDirEntreDosCeldas(map, this.cellId, player.curCell.cellId)
        if (remove)
            dir = PathFinding.getOpositeDirection(dir)
        var cell = this.cellId
        var cellTest = this.cellId
        val azar = Formulas.getRandomValue(1, 10)
        for (i in 0 until cellules) {
            cellTest = PathFinding.GetCaseIDFromDirection(cellTest, dir, map, false)
            if (map.getCase(cellTest) == null)
                return
            if (MP!!.getCellAndObject().containsKey(cellTest) && (this.fatigue >= 240 || this.isFecund() == 10))
                break
            if (MP.getCellAndObject().containsKey(cellTest)) {
                val item = MP.getCellAndObject()[cellTest]!!
                // liste objet elevage
                if (item == 7755 || item == 7756 || item == 7757 || item == 7758 || item == 7759 || item == 7760 || item == 7761 || item == 7762 || item == 7763 || item == 7764 || item == 7765 || item == 7766 || item == 7767 || item == 7768 || item == 7769 || item == 7770 || item == 7771 || item == 7772 || item == 7773 || item == 7774 || item == 7625 || item == 7626 || item == 7627 || item == 7629) {// Baffeur
                    resState(GameMap.getObjResist(player, cellTest, item))
                    if (this.sex == 0) {
                        this.stateMale()
                    } else {
                        this.stateFemale()
                    }
                    aumFatige()
                } else if (item == 7775 || item == 7776 || item == 7777 || item == 7778 || item == 7779 || item == 7780 || item == 7781 || item == 7782 || item == 7783 || item == 7784 || item == 7785 || item == 7786 || item == 7787 || item == 7788 || item == 7789 || item == 7790 || item == 7791 || item == 7792 || item == 7793 || item == 7794 || item == 7795 || item == 7796 || item == 7797 || item == 7798) {//Foudroyeur
                    if (this.state < 0)
                        this.aumEndurance(GameMap.getObjResist(player, cellTest, item))
                    if (this.sex == 0) {
                        this.stateMale()
                    } else {
                        this.stateFemale()
                    }
                    aumFatige()
                } else if (item == 7606 || item == 7607 || item == 7608 || item == 7609 || item == 7610 || item == 7611 || item == 7612 || item == 7613 || item == 7614 || item == 7615 || item == 7616 || item == 7617 || item == 7618 || item == 7619 || item == 7620 || item == 7621 || item == 7683 || item == 7684 || item == 7685 || item == 7686 || item == 7687 || item == 7688 || item == 7689 || item == 7690) {// Mangeoire
                    resFatige()
                    this.aumEnergy(GameMap.getObjResist(player, cellTest, item))
                    if (this.sex == 0) {
                        this.stateMale()
                    } else {
                        this.stateFemale()
                    }
                } else if (item == 7634 || item == 7635 || item == 7636 || item == 7637 || item == 7691 || item == 7692 || item == 7693 || item == 7694 || item == 7695 || item == 7696 || item == 7697 || item == 7698 || item == 7699 || item == 7700) {// Dragofesse
                    if (this.state > 0)
                        this.aumAmor(GameMap.getObjResist(player, cellTest, item))
                    if (this.sex == 0) {
                        this.stateMale()
                    } else {
                        this.stateFemale()
                    }
                    aumFatige()
                } else if (item == 7628 || item == 7622 || item == 7623 || item == 7624 || item == 7733 || item == 7734 || item == 7735 || item == 7736 || item == 7737 || item == 7738 || item == 7739 || item == 7740 || item == 7741 || item == 7742 || item == 7743 || item == 7744 || item == 7745 || item == 7746) {// Caresseur
                    aumState(GameMap.getObjResist(player, cellTest, item))
                    if (this.sex == 0) {
                        this.stateMale()
                    } else {
                        this.stateFemale()
                    }
                    aumFatige()
                } else if (item == 7590 || item == 7591 || item == 7592 || item == 7593 || item == 7594 || item == 7595 || item == 7596 || item == 7597 || item == 7598 || item == 7599 || item == 7600 || item == 7601 || item == 7602 || item == 7603 || item == 7604 || item == 7605 || item == 7673 || item == 7674 || item == 7675 || item == 7676 || item == 7677 || item == 7678 || item == 7679 || item == 7682) {// Abreuvoir
                    if (this.state <= 2000 && this.state >= -2000)
                        this.aumMaturity(GameMap.getObjResist(player, cellTest, item))
                    if (this.sex == 0) {
                        this.stateMale()
                    } else {
                        this.stateFemale()
                    }
                    aumFatige()
                }
                break
            }
            if (map.getCase(cellTest)!!.isWalkable(false) && MP.door != cellTest && !map.cellSide(cell, cellTest)) {
                cell = cellTest
                path += dir.toString() + CryptManager.cellID_To_Code(cell)
            } else {
                break
            }
        }
        if (cell == this.cellId) {
            this.orientation = CryptManager.getIntByHashedValue(dir)
            SocketManager.GAME_SEND_eD_PACKET_TO_MAP(map, this.id, this.orientation)
            SocketManager.SEND_GDE_FRAME_OBJECT_EXTERNAL(map, "$cellTest;4")
            SocketManager.GAME_SEND_eUK_PACKET_TO_MAP(map, this.id, action)
            return
        }
        if (azar == 5)
            action = 8
        val nb = checkCanKen(MP!!, this, cellTest, action)
        if (nb == 4) action = 4
        SocketManager.GAME_SEND_GA_PACKET_TO_MAP(map, "" + 0, 1, this.id, "a" + CryptManager.cellID_To_Code(this.cellId) + path)
        this.cellId = cell
        this.orientation = CryptManager.getIntByHashedValue(dir)
        val ID = this.id

        val finalCell = cellTest
        val finalAction = action

        TimerWaiter.addNext({
            SocketManager.SEND_GDE_FRAME_OBJECT_EXTERNAL(map, "$finalCell;4")
            if (finalAction != 0)
                SocketManager.GAME_SEND_eUK_PACKET_TO_MAP(map, ID, finalAction)
        }, (if (action == 4) 2500 else 1500).toLong())
    }

    @Synchronized
    fun moveMountsAuto(direction: Char, cellules: Int, remove: Boolean) {
        var action = 0
        var path = ""
        val map = World.world.getMap(this.mapId)
        if (map == null)
            return
        if (map.mountPark == null)
            return
        val MP = map.mountPark
        val dir = direction
        val random = Formulas.getRandomValue(1, 10)
        var cell = this.cellId
        var cellTest = this.cellId
        for (i in 0 until cellules) {
            cellTest = PathFinding.getCellArroundByDir(cellTest, dir, World.world.getMap(this.mapId))
            if (map.getCase(cellTest) == null)
                return
            if (MP!!.getCellAndObject().containsKey(cellTest) && (this.fatigue >= 240 || this.isFecund() == 10))
                break
            if (MP.getCellAndObject().containsKey(cellTest) && this.fatigue < 240) {
                val item = MP.getCellAndObject()[cellTest]!!
                if (item == 7755 || item == 7756 || item == 7757 || item == 7758 || item == 7759 || item == 7760 || item == 7761 || item == 7762 || item == 7763 || item == 7764 || item == 7765 || item == 7766 || item == 7767 || item == 7768 || item == 7769 || item == 7770 || item == 7771 || item == 7772 || item == 7773 || item == 7774 || item == 7625 || item == 7626 || item == 7627 || item == 7629) {
                    resState(GameMap.getObjResist(MP, cellTest, item))
                    if (this.sex == 0) {
                        this.stateMale()
                    } else {
                        this.stateFemale()
                    }
                    aumFatige()
                } else if (item == 7775 || item == 7776 || item == 7777 || item == 7778 || item == 7779 || item == 7780 || item == 7781 || item == 7782 || item == 7783 || item == 7784 || item == 7785 || item == 7786 || item == 7787 || item == 7788 || item == 7789 || item == 7790 || item == 7791 || item == 7792 || item == 7793 || item == 7794 || item == 7795 || item == 7796 || item == 7797 || item == 7798) {// Baffeur
                    if (this.state < 0)
                        this.aumEndurance(GameMap.getObjResist(MP, cellTest, item))
                    if (this.sex == 0) {
                        this.stateMale()
                    } else {
                        this.stateFemale()
                    }
                    aumFatige()
                } else if (item == 7606 || item == 7607 || item == 7608 || item == 7609 || item == 7610 || item == 7611 || item == 7612 || item == 7613 || item == 7614 || item == 7615 || item == 7616 || item == 7617 || item == 7618 || item == 7619 || item == 7620 || item == 7621 || item == 7683 || item == 7684 || item == 7685 || item == 7686 || item == 7687 || item == 7688 || item == 7689 || item == 7690) {// Foudroyeur
                    aumFatige()
                    this.aumEnergy(GameMap.getObjResist(MP, cellTest, item))
                    if (this.sex == 0) {
                        this.stateMale()
                    } else {
                        this.stateFemale()
                    }
                } else if (item == 7634 || item == 7635 || item == 7636 || item == 7637 || item == 7691 || item == 7692 || item == 7693 || item == 7694 || item == 7695 || item == 7696 || item == 7697 || item == 7698 || item == 7699 || item == 7700) {// Mangeoire
                    if (this.state > 0)
                        this.aumAmor(GameMap.getObjResist(MP, cellTest, item))
                    if (this.sex == 0) {
                        this.stateMale()
                    } else {
                        this.stateFemale()
                    }
                    aumFatige()
                } else if (item == 7628 || item == 7622 || item == 7623 || item == 7624 || item == 7733 || item == 7734 || item == 7735 || item == 7736 || item == 7737 || item == 7738 || item == 7739 || item == 7740 || item == 7741 || item == 7742 || item == 7743 || item == 7744 || item == 7745 || item == 7746) {// Dragofesse
                    aumState(GameMap.getObjResist(MP, cellTest, item))
                    if (this.sex == 0) {
                        this.stateMale()
                    } else {
                        this.stateFemale()
                    }
                    aumFatige()
                } else if (item == 7590 || item == 7591 || item == 7592 || item == 7593 || item == 7594 || item == 7595 || item == 7596 || item == 7597 || item == 7598 || item == 7599 || item == 7600 || item == 7601 || item == 7602 || item == 7603 || item == 7604 || item == 7605 || item == 7673 || item == 7674 || item == 7675 || item == 7676 || item == 7677 || item == 7678 || item == 7679 || item == 7682) {// Abreuvoir
                    if (this.state <= 2000 && this.state >= -2000)
                        this.aumMaturity(GameMap.getObjResist(MP, cellTest, item))
                    if (this.sex == 0) {
                        this.stateMale()
                    } else {
                        this.stateFemale()
                    }
                    aumFatige()
                }
                break
            }
            if (map.getCase(cellTest)!!.isWalkable(false) && MP.door != cellTest && !map.cellSide(cell, cellTest)) {
                cell = cellTest
                path += dir.toString() + CryptManager.cellID_To_Code(cell)
            } else {
                break
            }
        }
        if (cell == this.cellId) {
            this.orientation = CryptManager.getIntByHashedValue(dir)
            SocketManager.GAME_SEND_eD_PACKET_TO_MAP(map, this.id, this.orientation)
            SocketManager.SEND_GDE_FRAME_OBJECT_EXTERNAL(map, "$cellTest;4")
            SocketManager.GAME_SEND_eUK_PACKET_TO_MAP(map, this.id, action)
            return
        }
        if (random == 5)
            action = 8
        val id = this.id
        action = checkCanKen(MP!!, this, cellTest, action)
        SocketManager.GAME_SEND_GA_ACTION_TO_MAP(map, "" + 0, 1, this.id.toString() + "", "a" + CryptManager.cellID_To_Code(this.cellId) + path)
        this.cellId = cell
        this.orientation = CryptManager.getIntByHashedValue(dir)

        val finalCell = cellTest
        val finalAction = action

        if (map.players.size > 0) {
            var player: Player? = null
            for (target in map.players)
                player = target
            if (player != null)
                TimerWaiter.addNext({
                    SocketManager.SEND_GDE_FRAME_OBJECT_EXTERNAL(map, "$finalCell;4")
                    if (finalAction != 0) SocketManager.GAME_SEND_eUK_PACKET_TO_MAP(map, id, finalAction)
                }, 2000)
        }
    }

    fun addObject(guid: Int, qua: Int, P: Player) {
        if (qua <= 0)
            return
        val playerObj = World.world.getGameObject(guid) ?: return
        //Si le joueur n'a pas l'item dans son sac ...
        if (P.items[guid] == null) {
            return
        }

        var str = ""

        //Si c'est un item equipe ...
        if (playerObj.position != Constant.ITEM_POS_NO_EQUIPED) return

        var trunkObj = this.getSimilarObject(playerObj)
        val newQua = playerObj.quantity - qua
        if (trunkObj == null) {//S'il n'y pas d'item du meme Template
            //S'il ne reste pas d'item dans le sac
            if (newQua <= 0) {
                //On enleve l'objet du sac du joueur
                P.removeItem(playerObj.guid)
                //On met l'objet du sac dans le coffre, avec la meme quantite
                this.objects[playerObj.guid] = playerObj
                str = "O+" + playerObj.guid + "|" + playerObj.quantity + "|" + playerObj.template!!.id + "|" + playerObj.encodeStats()
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(P, guid)
            } else {//S'il reste des objets au joueur
                //on modifie la quantite d'item du sac
                playerObj.quantity = newQua
                //On ajoute l'objet au coffre et au monde
                trunkObj = playerObj.getClone(qua, true)!!
                World.world.addGameObject(trunkObj)
                this.objects[trunkObj.guid] = trunkObj

                //Envoie des packets
                str = "O+" + trunkObj.guid + "|" + trunkObj.quantity + "|" + trunkObj.template!!.id + "|" + trunkObj.encodeStats()
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(P, playerObj)
            }
        } else { // S'il y avait un item du meme template
            //S'il ne reste pas d'item dans le sac
            if (newQua <= 0) {
                //On enleve l'objet du sac du joueur
                P.removeItem(playerObj.guid)
                //On enleve l'objet du monde
                World.world.removeGameObject(playerObj.guid)
                //On ajoute la quantite a l'objet dans le coffre
                trunkObj.quantity = trunkObj.quantity + playerObj.quantity
                //On envoie l'ajout au coffre de l'objet
                str = "O+" + trunkObj.guid + "|" + trunkObj.quantity + "|" + trunkObj.template!!.id + "|" + trunkObj.encodeStats()
                //On envoie la supression de l'objet du sac au joueur
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(P, guid)
            } else {//S'il restait des objets
                //On modifie la quantite d'item du sac
                playerObj.quantity = newQua
                trunkObj.quantity = trunkObj.quantity + qua
                str = "O+" + trunkObj.guid + "|" + trunkObj.quantity + "|" + trunkObj.template!!.id + "|" + trunkObj.encodeStats()
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(P, playerObj)
            }
        }

        SocketManager.GAME_SEND_Ew_PACKET(P, this.actualPods, this.maxPods)
        SocketManager.GAME_SEND_EL_MOUNT_PACKET(P, this)
    }

    fun removeObject(guid: Int, qua: Int, P: Player) {
        if (qua <= 0)
            return
        val trunkObj = World.world.getGameObject(guid)
        //Si le joueur n'a pas l'item dans son coffre
        if (this.objects[guid] == null) {
            return
        }

        var playerObj = P.getSimilarItem(trunkObj!!)
        var str = ""
        val newQua = trunkObj!!.quantity - qua

        if (playerObj == null) {//Si le joueur n'avait aucun item similaire
            //S'il ne reste rien dans le coffre
            if (newQua <= 0) {
                //On retire l'item du coffre
                this.objects.remove(guid)
                //On l'ajoute au joueur
                P.items[guid] = trunkObj

                //On envoie les packets
                SocketManager.GAME_SEND_OAKO_PACKET(P, trunkObj)
                SocketManager.GAME_SEND_Ew_PACKET(P, this.actualPods, this.maxPods)
                str = "O-$guid"
            } else { //S'il reste des objets dans le coffre
                //On cree une copy de l'item dans le coffre
                playerObj = trunkObj.getClone(qua, true)!!
                //On l'ajoute au monde
                World.world.addGameObject(playerObj)
                //On retire X objet du coffre
                trunkObj.quantity = newQua
                //On l'ajoute au joueur
                P.items[playerObj.guid] = playerObj

                //On envoie les packets
                SocketManager.GAME_SEND_OAKO_PACKET(P, playerObj)
                SocketManager.GAME_SEND_Ew_PACKET(P, this.actualPods, this.maxPods)
                str = "O+" + trunkObj.guid + "|" + trunkObj.quantity + "|" + trunkObj.template!!.id + "|" + trunkObj.encodeStats()
            }
        } else { // Le joueur avait deja un item similaire
            //S'il ne reste rien dans le coffre
            if (newQua <= 0) {
                //On retire l'item du coffre
                this.objects.remove(trunkObj.guid)
                World.world.removeGameObject(trunkObj.guid)
                //On Modifie la quantite de l'item du sac du joueur
                playerObj.quantity = playerObj.quantity + trunkObj.quantity

                //On envoie les packets
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(P, playerObj)
                SocketManager.GAME_SEND_Ew_PACKET(P, this.actualPods, this.maxPods)
                str = "O-$guid"
            } else {//S'il reste des objets dans le coffre
                //On retire X objet du coffre
                trunkObj.quantity = newQua
                //On ajoute X objets au joueurs
                playerObj.quantity = playerObj.quantity + qua

                //On envoie les packets
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(P, playerObj)
                SocketManager.GAME_SEND_Ew_PACKET(P, this.actualPods, this.maxPods)
                str = "O+" + trunkObj.guid + "|" + trunkObj.quantity + "|" + trunkObj.template!!.id + "|" + trunkObj.encodeStats()
            }
        }

        SocketManager.GAME_SEND_EsK_PACKET(P, str)
    }

    private fun getSimilarObject(obj: GameObject): GameObject? {
        for (gameObject in this.objects.values)
            if (gameObject.template!!.type != 85)
                if (gameObject.template!!.id == obj.template!!.id && World.world.conditionManager.stackIfSimilar(gameObject, obj, true) && gameObject.stats.isSameStats(obj.stats))
                    return gameObject
        return null
    }

    private fun convertStatsToString(): String {
        val stats = StringBuilder()
        for ((key, value) in this.stats.effects.entries) {
            if (value <= 0)
                continue
            if (stats.isNotEmpty())
                stats.append(",")
            stats.append(Integer.toHexString(key)).append("#").append(Integer.toHexString(value)).append("#0#0")
        }
        return stats.toString()
    }

    fun parse(): String {
        return this.id.toString() + ":" + this.color + ":" + this.ancestors + ":" + ",," + this.parseCapacitysToString() + ":" + this.name + ":" + this.sex + ":" + this.parseExp() + ":" + this.level + ":" + this.isMontable() + ":" + this.maxPods + ":" + this.savage + ":" + this.endurance + ",10000:" + this.maturity + "," + this.maxMaturity + ":" + this.energy + "," + this.maxEnergy + ":" + this.state + ",-10000,10000:" + this.amour + ",10000:" + (if (this.fecundatedDate == -1L) this.fecundatedDate else (System.currentTimeMillis() - this.fecundatedDate) / 3600000 + 1) + ":" + this.isFecund() + ":" + this.convertStatsToString() + ":" + this.fatigue + ",240:" + this.reproduction + ",20:"
    }

    fun parseToGM(): String {
        val str = StringBuilder()
        str.append("GM|+")
        str.append(this.cellId).append(";")
        str.append(this.orientation).append(";0;").append(this.id).append(";").append(this.name).append(";-9;")
        str.append(if (this.color == 88) 7005 else 7002)
        str.append("^").append(this.size).append(";")
        if (World.world.getPlayer(this.owner) == null)
            str.append("Sans Maitre")
        else
            str.append(World.world.getPlayer(this.owner)!!.name)
        str.append(";").append(this.level).append(";").append(this.color)
        return str.toString()
    }

    fun parseToMountObjects(): String {
        val packet = StringBuilder()
        for (obj in this.objects.values)
            packet.append("O").append(obj.encodeItem()).append(";")
        return packet.toString()
    }

    fun parseObjectsToString(): String {
        var str = ""
        for (gameObject in this.objects.values)
            str += (if (str.isEmpty()) "" else ";") + gameObject.guid
        return str
    }

    fun parseCapacitysToString(): String {
        var str = ""
        for (capacity in this.capacitys)
            str += (if (str.isEmpty()) "" else ",") + capacity
        return if (str.isEmpty()) "0" else str
    }

    private fun parseExp(): String {
        val xpTable = World.world.experiences!!.mounts
        return this.exp.toString() + "," + xpTable.minXpAt(this.level) + "," + xpTable.maxXpAt(this.level)
    }

    companion object {
        @JvmField
        val updatable: Updatable<Void?> = object : Updatable<Void?>(3600000) {
            override fun update() {
                if (this.verify()) {
                    for (mount in World.world.mounts.values) {
                        if (mount.fatigue <= 0) continue
                        mount.fatigue = mount.fatigue - 10
                        if (mount.fatigue < 0) mount.fatigue = 0
                    }
                }
            }

            override fun get(): Void? = null
        }

        private fun checkCanKen(park: MountPark, mount: Mount, cellTest: Int, action: Int): Int {
            if (park.getListOfRaising().size > 1) {
                val map = World.world.getMap(park.map)

                for (arg in park.getListOfRaising()) {
                    val mountArg = World.world.getMountById(arg) ?: continue
                    if (mountArg.sex != mount.sex && mountArg.isFecund() != 0 && mount.isFecund() != 0 && mountArg.cellId == cellTest) {
                        if (mountArg.reproduction < 20 && mount.reproduction < 20 && !mountArg.isCastrated() && !mount.isCastrated()) {
                            if (mountArg.sex == 1) {
                                mountArg.fecundatedDate = System.currentTimeMillis()
                                mountArg.couple = mount.id
                                mountArg.resAmor(7500)
                                mountArg.resEndurance(7500)
                                mount.resAmor(7500)
                                mount.resEndurance(7500)
                                mount.aumReproduction()
                                if (mount.savage == 1) {
                                    park.getListOfRaising().remove(mount.id)
                                    map.send("GM|-" + mount.id)
                                    val player = World.world.getPlayer(mount.owner)
                                    if (player != null && player.isOnline) {
                                        player.send("Im0111;~" + map.x + "," + map.y)
                                        SocketManager.GAME_SEND_Ee_PACKET(player, '-', mount.id.toString())
                                    }
                                    mount.mapId = -1
                                    mount.cellId = -1
                                    mount.owner = -1
                                }
                            } else if (mount.sex == 1) {
                                mount.fecundatedDate = System.currentTimeMillis()
                                mount.couple = mountArg.id
                                mount.resAmor(7500)
                                mount.resEndurance(7500)
                                mountArg.resAmor(7500)
                                mountArg.resEndurance(7500)
                                mountArg.aumReproduction()
                                if (mountArg.savage == 1) {
                                    park.getListOfRaising().remove(mountArg.id)
                                    map.send("GM|-" + mountArg.id)
                                    val player = World.world.getPlayer(mountArg.owner)
                                    if (player != null && player.isOnline) {
                                        player.send("Im0111;~" + map.x + "," + map.y)
                                        SocketManager.GAME_SEND_Ee_PACKET(player, '-', mountArg.id.toString())
                                    }
                                    mountArg.mapId = -1
                                    mountArg.cellId = -1
                                    mountArg.owner = -1
                                }
                            }
                            return 4
                        }
                    }
                }
            }
            return action
        }
    }
}
