package org.starloco.locos.area.map

import org.starloco.locos.anims.KeyFrame
import org.starloco.locos.area.Area
import org.starloco.locos.area.SubArea
import org.starloco.locos.client.Player
import org.starloco.locos.common.Formulas
import org.starloco.locos.common.PathFinding
import org.starloco.locos.common.SocketManager
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.game.MountParkData
import org.starloco.locos.database.data.login.MountData
import org.starloco.locos.entity.Collector
import org.starloco.locos.entity.Prism
import org.starloco.locos.entity.map.InteractiveObject
import org.starloco.locos.entity.map.MountPark
import org.starloco.locos.entity.monster.MobGroupDef
import org.starloco.locos.entity.monster.Monster
import org.starloco.locos.entity.monster.MonsterGrade
import org.starloco.locos.entity.monster.MonsterGroup
import org.starloco.locos.entity.mount.Mount
import org.starloco.locos.entity.npc.Npc
import org.starloco.locos.entity.npc.NpcMovable
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.game.scheduler.Updatable
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Config
import org.starloco.locos.kernel.Constant
import org.starloco.locos.kernel.Main
import org.starloco.locos.kernel.Logging
import org.starloco.locos.kernel.Reboot
import org.starloco.locos.`object`.GameObject
import org.starloco.locos.script.proxy.SMap
import org.starloco.locos.util.Pair
import org.starloco.locos.util.TimerWaiter
import java.util.ArrayList
import java.util.Collections
import java.util.Objects
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import java.util.stream.Stream
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(GameMap::class.java)

class GameMap(@JvmField val data: ScriptMapData) {

    companion object {
        @JvmField
        val fixMobGroupObjects: MutableMap<String, ArrayList<GameObject>> = HashMap()

        @JvmField
        val updatable: Updatable<ArrayList<RespawnGroup>> = object : Updatable<ArrayList<RespawnGroup>>(30000) {
            private val groups = ArrayList<RespawnGroup>()

            private fun randomizeMobGroup(cellID: Int, data: String): MobGroupDef {
                val grades = data.split(";").map { mob ->
                    val infos = mob.split(",")
                    val idMonster = infos[0].toInt()
                    val min = infos[1].toInt()
                    val max = infos[2].toInt()

                    val mgs = Optional.ofNullable(World.world.getMonstre(idMonster))
                        .map { it.grades }.map { it.values }.orElse(mutableListOf<MonsterGrade>()).stream()
                        .filter { mg -> mg.level >= min && mg.level <= max }
                        .map { it.grade }
                        .collect(Collectors.toList())

                    return@map Pair(idMonster, mgs)
                }

                return MobGroupDef(cellID, grades)
            }

            override fun update() {
                if (groups.isNotEmpty()) {
                    val time = System.currentTimeMillis()
                    val random = Formulas.getRandomValue(120000, 300000)

                    for (respawnGroup in ArrayList(groups)) {
                        if (respawnGroup.cell != -1) {
                            val data = World.world.getGroupFix(respawnGroup.map.data.id, respawnGroup.cell)

                            if (data != null && time - respawnGroup.lastTime > data["timer"]!!.toLong()) {
                                val def = randomizeMobGroup(respawnGroup.cell, data["groupData"]!!)
                                respawnGroup.map.spawnMobGroup(def, true)
                                groups.remove(respawnGroup)
                            }
                        } else if (time - respawnGroup.lastTime > random) {
                            respawnGroup.map.spawnGroup(-1, 1, true, -1)
                            groups.remove(respawnGroup)
                        }
                    }
                }

                if (this.verify()) {
                    if (Config.autoReboot) {
                        if (Reboot.check()) {
                            if ((System.currentTimeMillis() - Config.startTime) > 60000) {
                                for (player in World.world.onlinePlayers) {
                                    if (player.fight != null)
                                        player.fight!!.endFight(0.toByte())
                                    player.send(this.toString())
                                }
                                try {
                                    Thread.sleep(5000)
                                } catch (ignored: Exception) {
                                }
                                Main.stop("Automatic restart")
                                return
                            }
                        }
                    }

                    TimerWaiter.addNext({
                        val mapsAlreadyMoved = ArrayList<GameMap>()
                        for (player in World.world.onlinePlayers) {
                            val map = player.curMap
                            if (map != null && !mapsAlreadyMoved.contains(map)) {
                                map.onMapMonsterDeplacement()
                                mapsAlreadyMoved.add(map)
                            }
                        }

                        for (mount in World.world.mounts.values) {
                            val map = World.world.getMap(mount.mapId)
                            if (map != null && !mapsAlreadyMoved.contains(map) && map.mountPark != null) {
                                map.mountPark!!.startMoveMounts()
                                mapsAlreadyMoved.add(map)
                            }
                        }

                        World.world.collectors.values.forEach { it.moveOnMap() }
                    }, 1, TimeUnit.SECONDS)

                    NpcMovable.moveAll()
                }
            }

            override fun get(): ArrayList<RespawnGroup> {
                return groups
            }
        }

        @JvmStatic
        fun removeMountPark(guildId: Int) {
            try {
                World.world.mountParks.values.stream().filter { park -> park.guild != null }.filter { park -> park.guild!!.id == guildId }.forEach { park ->
                    if (park.getListOfRaising().isNotEmpty()) {
                        for (id in ArrayList(park.getListOfRaising())) {
                            if (World.world.getMountById(id) == null) {
                                park.delRaising(id)
                                continue
                            }
                            World.world.removeMount(id)
                            (DatabaseManager.get(MountData::class.java) as MountData).delete(World.world.getMountById(id)!!)
                        }
                        park.getListOfRaising().clear()
                    }
                    if (park.getEtable().isNotEmpty()) {
                        for (mount in ArrayList(park.getEtable())) {
                            if (mount == null) continue
                            World.world.removeMount(mount.id)
                            (DatabaseManager.get(MountData::class.java) as MountData).delete(mount)
                        }
                        park.getEtable().clear()
                    }

                    park.owner = 0
                    park.guild = null
                    park.price = 3000000
                    (DatabaseManager.get(MountParkData::class.java) as MountParkData).update(park)

                    val map = World.world.getMap(park.map)
                    if (map == null) return@forEach
                    for (p in map.players)
                        SocketManager.GAME_SEND_Rp_PACKET(p, park)
                }
            } catch (e: Exception) {
                logger.error("unexpected error", e)
                }
        }

        @JvmStatic
        fun getObjResist(perso: Player, cellid: Int, itemID: Int): Int {
            val MP = perso.curMap.mountPark
            var packets = ""
            if (MP == null || MP.getObject().size == 0)
                return 0
            for (entry in MP.objDurab.entries) {
                for (entry2 in entry.value.entries) {
                    if (cellid == entry.key)
                        packets += entry.key.toString() + ";" + entry2.value + ";" + entry2.key
                }
            }
            var cell: Int
            var durability: Int
            val durabilityMax: Int
            try {
                val infos = packets.split(";")
                cell = infos[0].toInt()
                if (itemID == 7798 || itemID == 7605 || itemID == 7606 || itemID == 7625 || itemID == 7628 || itemID == 7634) {
                    durability = infos[1].toInt()
                } else {
                    durability = infos[1].toInt() - 1
                }
                durabilityMax = infos[2].toInt()
            } catch (e: Exception) {
                logger.error("unexpected error", e)
                return 0
            }

            if (durability <= 0) {
                //if (MP.delObject(cell)) {
                durability = 0
                val InDurab = HashMap<Int, Int>()
                InDurab[durabilityMax] = durability
                MP.objDurab.put(cell, InDurab)
                SocketManager.SEND_GDO_PUT_OBJECT_MOUNT(perso.curMap, cell.toString()
                        + ";" + itemID + ";1;" + durability + ";" + durabilityMax)
                return 0
                //}
            } else {
                val InDurab = HashMap<Int, Int>()
                InDurab[durabilityMax] = durability
                MP.objDurab.put(cell, InDurab)
                SocketManager.SEND_GDO_PUT_OBJECT_MOUNT(perso.curMap, cell.toString()
                        + ";" + itemID + ";1;" + durability + ";" + durabilityMax)
            }
            return durabilityMax
        }

        @JvmStatic
        fun getObjResist(MP: MountPark?, cellid: Int, itemID: Int): Int {
            var packets = ""
            if (MP == null || MP.getObject().size == 0)
                return 0
            for (entry in MP.objDurab.entries) {
                for (entry2 in entry.value.entries) {
                    if (cellid == entry.key)
                        packets += entry.key.toString() + ";" + entry2.value + ";" + entry2.key
                }
            }
            val infos = packets.split(";")
            val cell = infos[0].toInt()
            var durability: Int
            if (itemID == 7798 || itemID == 7605 || itemID == 7606
                || itemID == 7625 || itemID == 7628 || itemID == 7634) {
                durability = infos[1].toInt()
            } else {
                durability = infos[1].toInt() - 1
            }
            val durabilityMax = infos[2].toInt()

            val map = World.world.getMap(MP.map)
            if (durability <= 0) {
                //if (MP.delObject(cell)) {
                durability = 0
                val InDurab = HashMap<Int, Int>()
                InDurab[durabilityMax] = durability
                MP.objDurab.put(cell, InDurab)
                SocketManager.SEND_GDO_PUT_OBJECT_MOUNT(map, cell.toString()
                        + ";" + itemID + ";1;" + durability + ";" + durabilityMax)
                return 0
                //}
            } else {
                val InDurab = HashMap<Int, Int>()
                InDurab[durabilityMax] = durability
                MP.objDurab.put(cell, InDurab)
                SocketManager.SEND_GDO_PUT_OBJECT_MOUNT(map, (cell.toString() + ";"
                        + itemID + ";1;" + durability + ";" + durabilityMax))
            }
            return durabilityMax
        }
    }

    private val scriptVal: SMap

    @JvmField
    var nextObjectId = -1

    val maxTeam = 0
    var isMute = false
        private set
    val mountPark: MountPark?
    @JvmField
    val cellsData: CellsDataProvider.CellsDataOverride
    val cases: MutableList<GameCase>
    private var _fights: MutableList<Fight>? = ArrayList()

    // Make those private once GameCase is gone
    @JvmField
    val actors = ConcurrentHashMap<Int, MutableSet<Actor>>()
    @JvmField
    val droppedItems = ConcurrentHashMap<Int, GameObject>()

    var interactiveObjects: Map<Int, InteractiveObject>? = null
        private set
    // end: Make those private once GameCase is gone

    val mobGroups = HashMap<Int, MonsterGroup>()
    val fixMobGroups = HashMap<Int, MonsterGroup>()
    val npcs = HashMap<Int, Npc>()
    private val mobExtras = HashMap<Int, Int>()

    private val animationStates = ConcurrentHashMap<Int, String>()

    init {
        Objects.requireNonNull(data)
        this.scriptVal = SMap(this)
        this.cellsData = CellsDataProvider.CellsDataOverride(data.cellsData)

        // Temporary create proxy GameCase to replace old GameCase class access
        this.cases = ArrayList(data.cellCount())
        for (i in 0 until data.cellCount()) {
            this.cases.add(GameCase(this, i))
        }

        this.mountPark = World.world.mountParks[data.id]

        this.data.getNPCs().forEach { (k, v) -> addNpc(k, v.first, v.second) }
        this.data.getStaticGroups().forEach { this.addStaticGroup(it) }


        this.refreshInteractiveObjects()
        this.refreshSpawns()
    }

    fun refreshInteractiveObjects() {
        val objects = HashMap<Int, InteractiveObject>()
        this.data.interactiveObjects().forEach { (key, gfxId) -> objects[key] = InteractiveObject(gfxId) }
        this.interactiveObjects = Collections.unmodifiableMap(objects)
    }

    val forbidden: String
        get() = data.getForbidden()

    fun addMobExtra(id: Int?, chances: Int?) {
        if (id != null && chances != null)
            this.mobExtras[id] = chances
    }

    val mobPossibles: List<MonsterGrade>
        get() = data.mobPossibles

    val id: Int
        get() = data.id

    val date: String
        get() = data.date

    val w: Int
        get() = data.width

    val h: Int
        get() = data.height

    val key: String
        get() = data.key

    val places: List<List<Int>>
        get() = data.places



    fun getCase(id: Int): GameCase? {
        if (id < 0 || id >= this.cases.size) {
            return null
        }
        return this.cases[id]
    }

    fun newFight(init1: Player, init2: Player, type: Int): Fight? {
        if (init1.fight != null || init2.fight != null)
            return null
        var id = 1
        if (this._fights == null)
            this._fights = ArrayList()
        if (this._fights!!.isNotEmpty())
            id = (this._fights!!.toTypedArray()[this._fights!!.size - 1] as Fight).id + 1
        val f = Fight(type, id, this, init1, init2)
        this._fights!!.add(f)

        return f
    }

    fun removeFight(id: Int) {
        if (this._fights != null) {
            val iterator = this.fights.iterator()
            while (iterator.hasNext()) {
                val fight = iterator.next()
                if (fight != null && fight.id == id) {
                    iterator.remove()
                    break
                }
            }

            if (this._fights!!.isEmpty()) this._fights = null
        }
    }

    fun getNbrFight(): Int {
        return if (_fights == null) 0 else this._fights!!.size
    }

    fun getFight(id: Int): Fight? {
        var fight: Fight? = null

        if (this._fights != null)
            this._fights!!.stream().filter { all -> all.id == id }.forEach { selected -> fight = selected }

        return fight
    }

    val fights: MutableList<Fight>
        get() = _fights ?: ArrayList()





    fun removeNpcOrMobGroup(id: Int) {
        this.npcs.remove(id)
        this.mobGroups.remove(id)
    }

    fun addNpc(npcID: Int, cellID: Int, dir: Int): Npc? {
        val template = World.world.getNPCTemplate(npcID) ?: return null
        if (getCase(cellID) == null)
            return null
        val npc: Npc
        if (template.legacy == null || template.legacy.path.isEmpty())
            npc = Npc(this.nextObjectId, cellID, dir.toByte(), npcID)
        else
            npc = NpcMovable(this.nextObjectId, cellID, dir.toByte(), data.id, npcID)


        this.npcs[this.nextObjectId] = npc
        this.nextObjectId--
        return npc
    }



    fun getNpc(id: Int): Npc? {
        return this.npcs[id]
    }

    fun getNpcByTemplateId(id: Int): Npc? {
        for (npc in this.npcs.values)
            if (npc != null && npc.template.id == id)
                return npc
        return null
    }

    fun RemoveNpc(id: Int): Npc? {
        return this.npcs.remove(id)
    }

    fun applyEndFightAction(player: Player) {
        val fight = player.lastFight
        this.data.onFightEnd(fight!!, player, fight.winners, fight.losers)
        player.setLastFightForEndFightAction(null)
    }

    fun applyInitFightAction(fight: Fight) {
        data.onFightInit(fight, fight.team0.values, fight.team1.values)
    }

    fun applyStartFightAction(fight: Fight) {
        data.onFightStart(fight, fight.team0.values, fight.team1.values)
    }

    val x: Int
        get() = data.x

    val y: Int
        get() = data.y

    val subArea: SubArea?
        get() = World.world.getSubArea(data.subAreaID)

    val area: Area?
        get() = Optional.ofNullable(World.world.getSubArea(data.subAreaID)).map { it.area }.orElse(null)



    val maxGroupNumb: Int
        get() = data.mobGroupsMaxCount



    fun containsForbiddenCellSpawn(id: Int): Boolean {
        return this.mountPark != null && this.mountPark!!.getCellAndObject().containsKey(id)
    }

    fun getMapCopy(): GameMap {
        return GameMap(data)
    }

    fun addPlayer(perso: Player) {
        SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(this, perso)
        perso.curCell.addPlayer(perso)
        if (perso.energy > 0) {
            if (perso.energy >= Player.maxEnergy)
                return
            if (Constant.isTaverne(this) && perso.timeTaverne == 0L) {
                perso.timeTaverne = System.currentTimeMillis()
            } else if (perso.timeTaverne != 0L) {
                var gain = ((System.currentTimeMillis() - perso.timeTaverne) / 1000).toInt()
                if (gain >= Player.maxEnergy) gain = Player.maxEnergy - perso.energy
                perso.energy = perso.energy + gain
                SocketManager.GAME_SEND_Im_PACKET(perso, "092;$gain")
                SocketManager.GAME_SEND_STATS_PACKET(perso)
                perso.timeTaverne = 0
            }
        }
    }

    val players: ArrayList<Player>
        get() {
            val player = ArrayList<Player>()
            for (c in cases)
                player.addAll(c.players)
            return player
        }

    fun sendFloorItems(player: Player) {
        val builder = StringBuilder("GDO")
        this.cases.stream().filter { c -> c.getDroppedItem(false) != null }
            .forEach { c -> builder.append("+").append(c.cellId).append(";").append(c.getDroppedItem(false)!!.template!!.id).append(";0|") }
        player.send(builder.toString())
    }

    fun delAllDropItem() {
        for (gameCase in this.cases) {
            SocketManager.GAME_SEND_GDO_PACKET_TO_MAP(this, '-', gameCase.cellId, 0, 0)
            gameCase.clearDroppedItem()
        }
    }

    val storeCount: Int
        get() = if (World.world.getSeller(this.id) == null) 0 else World.world.getSeller(this.id)!!.size

    fun haveMobFix(): Boolean {
        return this.fixMobGroups.isNotEmpty()
    }

    fun isPossibleToPutMonster(): Boolean {
        return this.cases.isNotEmpty() && data.mobGroupsMaxCount > 0 && data.mobPossibles.isNotEmpty()
    }

    fun loadExtraMonsterOnMap(idMob: Int): Boolean {
        if (World.world.getMonstre(idMob) == null)
            return false
        val grade = World.world.getMonstre(idMob)!!.getRandomGrade()
        val cell = this.randomFreeCellId

        val size = Formulas.getRandomValue(data.mobGroupsMinSize, data.mobGroupsMaxSize)
        val group = MonsterGroup(this.nextObjectId, Constant.ALIGNEMENT_NEUTRE, data.mobPossibles, this, cell, size, grade)
        if (group.mobs.isEmpty())
            return false
        this.mobGroups[this.nextObjectId] = group
        this.nextObjectId--
        return true
    }

    fun loadMonsterOnMap() {
        if (data.mobGroupsMaxCount == 0)
            return
        // FIXME MobGroup with faction take slots of neutral ones. See bonta + piwis
        spawnGroup(Constant.ALIGNEMENT_NEUTRE, data.mobGroupsMaxCount, false, -1)//Spawn des groupes d'alignement neutre
        spawnGroup(Constant.ALIGNEMENT_BONTARIEN, 1, false, -1)//Spawn du groupe de gardes bontarien s'il y a
        spawnGroup(Constant.ALIGNEMENT_BRAKMARIEN, 1, false, -1)//Spawn du groupe de gardes brakmarien s'il y a
    }

    fun mute() {
        this.isMute = !this.isMute
    }

    fun isAggroByMob(player: Player, cell: Int): Boolean {
        if (data.places.size < 2) return false

        if (player.curMap.data.id != data.id || !player.canAggro())
            return false
        for (group in this.mobGroups.values) {
            if (player.alignment == 0 && group.getAlignement() > 0)
                continue
            if (player.alignment == 1 && group.getAlignement() == 1)
                continue
            if (player.alignment == 2 && group.getAlignement() == 2)
                continue

            if (this.subArea != null) {
                group.setSubArea(this.subArea!!.id)
                group.changeAgro()
            }
            if (PathFinding.getDistanceBetween(this, cell, group.cellId) <= group.aggroDistance && group.aggroDistance > 0)//S'il y aggro
                if (World.world.conditionManager.validConditions(player, group.condition))
                    return true
        }
        return false
    }

    fun spawnAfterTimeGroup() {
        updatable.get()!!.add(RespawnGroup(this, -1, System.currentTimeMillis()))
    }

    fun spawnAfterTimeGroupFix(cell: Int) {
        updatable.get()!!.add(RespawnGroup(this, cell, System.currentTimeMillis()))
    }

    fun getInteractiveObject(cellId: Int): InteractiveObject? {
        return this.interactiveObjects?.get(cellId)
    }

    fun getAnimationState(cellId: Int): String? {
        val anim = data.animations[cellId] ?: return null
        return animationStates.getOrDefault(cellId, anim.defaultState)
    }

    @JvmOverloads
    fun setAnimationState(cellId: Int, frameName: String, cb: Runnable? = null) {
        val anim = data.animations[cellId]!!
        val previousStateName = this.animationStates[cellId]

        if (frameName == previousStateName) return

        val previousState = previousStateName?.let { anim.getFrame(it) }

        // Deal with default state
        val frame: KeyFrame
        if (frameName.equals("default", ignoreCase = true) || frameName.equals(anim.defaultState, ignoreCase = true)) {
            this.animationStates.remove(cellId)
            frame = anim.frames[anim.defaultState]!!
        } else {
            this.animationStates[cellId] = frameName
            frame = anim.frames[frameName]!!
        }

        // Remove overrides from previous state
        var overridesChanged = previousStateName
            ?.let { anim.getFrame(it) }
            ?.let { o -> cellsData.removeOverrides(cellId, o.cellOverrides) } ?: false

        // Add overrides for new state
        overridesChanged = overridesChanged or cellsData.applyOverrides(cellId, frame.cellOverrides)

        // ALWAYS send GDC first
        if (overridesChanged) {
            SocketManager.GAME_SEND_GDC_PACKET_TO_MAP(this, cellId, true)
        }

        // Official servers don't send GDF after automated state transition (Opening -> Opened)
        // This prevents them from changing cell states after starting an animation.
        // For now, we just send that extra GDF to give us more possibilities.
        // It's usually invisible to the user.
        // If it becomes an issue, we can update the code to send GDC and GDF together
        SocketManager.GAME_SEND_GDF_PACKET_TO_MAP(this, cellId, frame.frame, frame.isObjectInteractive())


        if (frame.hasDuration()) {
            // Start timer
            World.world.scheduler.schedule({
                this.setAnimationState(cellId, frame.nextFrame, null)
                cb?.run()
            }, frame.durationMillis().toLong(), TimeUnit.MILLISECONDS)
        } else if (cb != null) {
            cb.run()
        }
    }

    fun sendOverrides(player: Player) {
        if (cellsData.isEmpty()) return
        SocketManager.GAME_SEND_GDC_PACKET(player, this, true)
    }

    fun sendAnimStates(player: Player) {
        if (this.animationStates.isEmpty()) return

        SocketManager.GAME_SEND_GDF_PACKET(player,
            this.animationStates.entries!!
                .stream()
                .map { e -> Pair(e.key, data.animations[e.key]!!.frames[e.value]) }
        )
    }

    class RespawnGroup(val map: GameMap, val cell: Int, val lastTime: Long)

    fun spawnGroup(align: Int, nbr: Int, log: Boolean, cellID: Int) {
        var cellID = cellID
        if (nbr < 1)
            return
        if (this.mobGroups.size + this.fixMobGroups.size >= data.mobGroupsMaxCount)
            return
        for (a in 1..nbr) {
            // mobExtras
            val mobPoss = ArrayList(data.mobPossibles)
            if (this.mobExtras.isNotEmpty()) {
                for (entry in this.mobExtras.entries) {
                    if (entry.key == 499) // Si c'est un minotoboule de nowel
                        if (!Config.modeChristmas) // Si ce n'est pas nowel
                            continue
                    var random = Formulas.getRandomValue(0, 99)
                    while (entry.value > random) {
                        val mob = World.world.getMonstre(entry.key)
                        if (mob == null)
                            continue
                        val mobG = mob.getRandomGrade()
                        if (mobG == null)
                            continue
                        mobPoss.add(mobG)
                        if (entry.key == 422 || entry.key == 499) // un seul DDV / Minotoboule
                            break
                        random = Formulas.getRandomValue(0, 99)
                    }
                }
            }

            while (this.mobGroups[this.nextObjectId] != null)
                this.nextObjectId--

            val size = Formulas.getRandomValue(data.mobGroupsMinSize, data.mobGroupsMaxSize)
            val group = MonsterGroup(this.nextObjectId, align, mobPoss, this, cellID, size, null)

            if (group.mobs.isEmpty())
                continue
            this.mobGroups[this.nextObjectId] = group
            if (log)
                SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this, group)
            this.nextObjectId--
        }
    }

    fun respawnGroup(group: MonsterGroup) {
        this.mobGroups[group.id] = group
        SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this, group)
    }

    fun spawnGroupWith(m: Monster) {
        while (this.mobGroups[this.nextObjectId] != null)
            this.nextObjectId--
        var _m: MonsterGrade? = null
        while (_m == null)
            _m = m.getRandomGrade()
        var cell = this.randomFreeCellId
        while (this.containsForbiddenCellSpawn(cell))
            cell = this.randomFreeCellId

        val size = Formulas.getRandomValue(data.mobGroupsMinSize, data.mobGroupsMaxSize)
        val group = MonsterGroup(this.nextObjectId, Constant.ALIGNEMENT_NEUTRE, data.mobPossibles, this, cell, size, _m)
        group.isFix = false
        this.mobGroups[this.nextObjectId] = group
        SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this, group)
        this.nextObjectId--
    }

    fun spawnNewGroup(timer: Boolean, cellID: Int, groupData: String, condition: String) {
        var cellID = cellID
        while (this.mobGroups[this.nextObjectId] != null)
            this.nextObjectId--
        while (this.containsForbiddenCellSpawn(cellID))
            cellID = this.randomFreeCellId

        val group = MonsterGroup(this.nextObjectId, this, cellID, groupData)
        if (group.mobs.isEmpty())
            return
        this.mobGroups[this.nextObjectId] = group
        group.condition = condition
        group.isFix = false
        SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this, group)
        this.nextObjectId--
        if (timer)
            group.startCondTimer()
    }

    fun spawnGroupOnCommand(cellID: Int, groupData: String, send: Boolean): MonsterGroup {
        while (this.mobGroups[this.nextObjectId] != null)
            this.nextObjectId--
        val group = MonsterGroup(this.nextObjectId, this, cellID, groupData)
        if (group.mobs.isEmpty())
            return group
        this.mobGroups[this.nextObjectId] = group
        group.isFix = false
        if (send)
            SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this, group)

        this.nextObjectId--
        return group
    }

    fun spawnMobGroup(def: MobGroupDef, send: Boolean): Int {
        while (this.mobGroups[this.nextObjectId] != null)
            this.nextObjectId--
        val group = MonsterGroup(this.nextObjectId, this, def)

        if (group.mobs.isEmpty())
            return 0
        this.mobGroups[this.nextObjectId] = group
        this.nextObjectId--
        this.fixMobGroups[-1000 + this.nextObjectId] = group
        if (send)
            SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this, group)
        return group.id
    }

    fun refreshSpawns() {
        for (id in this.mobGroups.keys) {
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this, id)
        }
        this.mobGroups.clear()
        this.mobGroups.putAll(this.fixMobGroups)
        for (mg in this.fixMobGroups.values)
            SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this, mg)

        spawnGroup(Constant.ALIGNEMENT_NEUTRE, data.mobGroupsMaxCount, true, -1)//Spawn des groupes d'alignement neutre
        spawnGroup(Constant.ALIGNEMENT_BONTARIEN, 1, true, -1)//Spawn du groupe de gardes bontarien s'il y a
        spawnGroup(Constant.ALIGNEMENT_BRAKMARIEN, 1, true, -1)//Spawn du groupe de gardes brakmarien s'il y a
    }

    fun getPlayersGMsPackets(): String {
        return "GM" + actors.entries.stream()
            .filter { e -> Objects.nonNull(e.value) }
            .flatMap { e -> e.value.stream()
                .filter { it is Player }
                .map { it as Player }
                .map { p -> "|+" + p.parseToGM() }
            }
            .collect(Collectors.joining())
    }

    fun getFightersGMsPackets(fight: Fight): String {
        val packet = StringBuilder("GM")
        for (cell in this.cases)
            ArrayList(cell.fighters).stream().filter { fighter -> fighter.fight === fight }
                .forEach { fighter -> packet.append("|").append(fighter.getGmPacket('+', false)) }
        return packet.toString()
    }

    fun getFighterGMPacket(player: Player): String {
        val target = player.fight!!.getFighterByPerso(player)
        for (cell in this.cases)
            for (fighter in cell.fighters)
                if (fighter.fight === player.fight && fighter === target)
                    return "GM|" + fighter.getGmPacket('~', false)
        return ""
    }

    fun getFighterGMPacket(target: Fighter): String {
        for (cell in this.cases)
            for (fighter in cell.fighters)
                if (fighter.fight === target.fight && fighter === target)
                    return "GM|" + fighter.getGmPacket('~', false)
        return ""
    }

    fun getMobGroupGMsPackets(): String {
        if (this.mobGroups.isEmpty())
            return ""

        val packet = StringBuilder()
        packet.append("GM|")
        var isFirst = true
        for (entry in this.mobGroups.values) {
            val GM = entry.encodeGM()
            if (GM == "")
                continue

            if (!isFirst)
                packet.append("|")

            packet.append(GM)
            isFirst = false
        }
        return packet.toString()
    }

    fun getPrismeGMPacket(): String {
        var str = ""
        val prisms = World.world.AllPrisme()
        if (prisms != null) {
            for (prism in prisms) {
                if (prism.map == data.id) {
                    str = prism.parseToGM()
                    break
                }
            }
        }
        return str
    }

    fun getNpcsGMsPackets(p: Player): String {
        if (this.npcs.isEmpty())
            return ""

        val packet = StringBuilder()
        packet.append("GM|")
        var isFirst = true
        for (entry in this.npcs.entries) {
            val GM = entry.value.encodeGM(false, p)
            if (GM == "")
                continue

            if (!isFirst)
                packet.append("|")

            packet.append(GM)
            isFirst = false
        }
        return packet.toString()
    }

    fun getObjectsGDsPackets(): String {
        val packet = StringBuilder("GDF")
        this.animationStates.forEach { (cellId, frameName) ->
            val anim = data.animations[cellId]!!
            val frame = anim.getFrame(frameName)

            packet.append("|")
                .append(cellId).append(";")
                .append(frame!!.frame).append(";")
                .append(if (frame.isObjectInteractive()) "1" else "0")
        }

        return packet.toString()
    }

    fun startFightMonsterVersusMonster(group1: MonsterGroup, group2: MonsterGroup) {
        var id = 1
        if (this._fights == null)
            this._fights = ArrayList()
        if (this._fights!!.isNotEmpty())
            id = (this._fights!!.toTypedArray()[this._fights!!.size - 1] as Fight).id + 1

        this.mobGroups.remove(group1.id)
        this.mobGroups.remove(group2.id)
        val fight = Fight(id, this, group1, group2)
        this._fights!!.add(fight)
        SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this)
    }

    fun startFightVersusMonstres(player: Player, group: MonsterGroup) {
        if (player.fight != null)
            return
        if (player.isMissingSubscription()) {
            SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(player.getGameClient()!!, 'S')
            return
        }
        if (data.places.size < 2) {
            player.sendMessage(player.getLang().trans("area.map.gamemap.place.empty"))
            return
        }
        if (Main.fightAsBlocked)
            return
        if (player.dead.toInt() == 1)
            return
        if (player.alignment == 0 && group.getAlignement() > 0)
            return
        if (player.alignment == 1 && group.getAlignement() == 1)
            return
        if (player.alignment == 2 && group.getAlignement() == 2)
            return
        if (!player.canAggro())
            return
        if (player.afterFight)
            return
        if (group.condition != "")
            if (!World.world.conditionManager.validConditions(player, group.condition)) {
                SocketManager.GAME_SEND_Im_PACKET(player, "119")
                return
            }

        val party = player.party

        if (party != null && party.master != null && party.master!!.name != player.name && party.isWithTheMaster(player, false, false)) return

        var id = 1
        if (this._fights == null)
            this._fights = ArrayList()
        if (this._fights!!.isNotEmpty())
            id = (this._fights!!.toTypedArray()[this._fights!!.size - 1] as Fight).id + 1

        this.mobGroups.remove(group.id)
        val fight = Fight(id, this, player, group)
        this._fights!!.add(fight)
        SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this)

        if (party != null && party.master != null && party.master!!.name == player.name) {
            party.players.stream().filter { follower -> party.isWithTheMaster(follower, false, false) }.forEach { follower ->
                TimerWaiter.addNext({
                    if (fight.prism != null)
                        fight.joinPrismFight(follower, (if (fight.team0.containsKey(player.id)) 0 else 1))
                    else
                        fight.joinFight(follower, player.id)
                }, follower.party!!.getOptionByPlayer(follower)!!.second.toLong(), TimeUnit.SECONDS)
            }
        }
    }

    fun startFightVersusProtectors(player: Player?, group: MonsterGroup) {
        if (Main.fightAsBlocked || player == null || player.fight != null || player.dead.toInt() == 1 || !player.canAggro())
            return
        if (player.isMissingSubscription()) {
            SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(player.getGameClient()!!, 'S')
            return
        }

        var id = 1

        if (data.places.size < 2) {
            player.sendMessage(player.getLang().trans("area.map.gamemap.place.empty"))
            return
        }
        if (this._fights == null)
            this._fights = ArrayList()
        if (this._fights!!.isNotEmpty())
            id = (this._fights!!.toTypedArray()[this._fights!!.size - 1] as Fight).id + 1
        val fight = Fight(id, this, player, group, Constant.FIGHT_TYPE_PVM)
        fight.startFight()
        this._fights!!.add(fight)
        SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this)
    }

    fun startFigthVersusDopeuls(perso: Player, group: MonsterGroup)//RaZoR
    {
        if (perso.fight != null)
            return
        if (perso.isMissingSubscription()) {
            SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(perso.getGameClient()!!, 'S')
            return
        }
        var id = 1
        if (perso.dead.toInt() == 1)
            return
        if (!perso.canAggro())
            return
        if (this._fights == null)
            this._fights = ArrayList()
        if (this._fights!!.isNotEmpty())
            id = (this._fights!!.toTypedArray()[this._fights!!.size - 1] as Fight).id + 1
        this._fights!!.add(Fight(id, this, perso, group, Constant.FIGHT_TYPE_DOPEUL))
        SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this)
    }

    fun startFightVersusPercepteur(perso: Player, perco: Collector) {
        if (perso.fight != null)
            return
        if (perso.isMissingSubscription()) {
            SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(perso.getGameClient()!!, 'S')
            return
        }
        if (Main.fightAsBlocked)
            return
        if (perso.dead.toInt() == 1)
            return
        if (!perso.canAggro())
            return
        var id = 1
        if (this._fights == null)
            this._fights = ArrayList()
        if (this._fights!!.isNotEmpty())
            id = (this._fights!!.toTypedArray()[this._fights!!.size - 1] as Fight).id + 1

        this._fights!!.add(Fight(id, this, perso, perco))
        SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this)
    }

    fun startFightVersusPrisme(perso: Player, Prisme: Prism) {
        if (perso.fight != null)
            return
        if (perso.isMissingSubscription()) {
            SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(perso.getGameClient()!!, 'S')
            return
        }
        if (Main.fightAsBlocked || perso.dead.toInt() == 1 || !perso.canAggro())
            return
        var id = 1
        if (this._fights == null)
            this._fights = ArrayList()
        if (this._fights!!.isNotEmpty())
            id = (this._fights!!.toTypedArray()[this._fights!!.size - 1] as Fight).id + 1
        this._fights!!.add(Fight(id, this, perso, Prisme))
        SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this)
    }

    val randomFreeCellId: Int
        get() {
        val freecell = ArrayList<Int>()

        for (entry in cases) {
            if (!entry.isWalkable(false))
                continue
            if (entry.`object` != null)
                continue
            if (data.id == 8279) {
                when (entry.cellId) {
                    86, 100, 114, 128, 142, 156, 170, 184, 198 -> continue
                }
            }
            if (this.mountPark != null)
                if (this.mountPark!!.cellOfObject.contains(entry.cellId))
                    continue

            var ok = true
            for (mg in this.mobGroups.values)
                if (mg != null)
                    if (mg.cellId == entry.cellId)
                        ok = false
            for (npc in this.npcs.values)
                if (npc != null)
                    if (npc.cellId == entry.cellId)
                        ok = false
            if (!ok || entry.players.isNotEmpty() || data.cellHasMoveEndActions(entry.cellId))
                continue
            freecell.add(entry.cellId)
        }

        if (freecell.isEmpty())
            return -1
        return freecell[Formulas.getRandomValue(0, freecell.size - 1)]
        }

    fun getRandomNearFreeCellId(cellid: Int): Int//obtenir une cell aléatoire et proche
    {
        val freecell = ArrayList<Int>()
        val cases = ArrayList<Int>()

        cases.add(cellid + 1)
        cases.add(cellid - 1)
        cases.add(cellid + 2)
        cases.add(cellid - 2)
        cases.add(cellid + 14)
        cases.add(cellid - 14)
        cases.add(cellid + 15)
        cases.add(cellid - 15)
        cases.add(cellid + 16)
        cases.add(cellid - 16)
        cases.add(cellid + 27)
        cases.add(cellid - 27)
        cases.add(cellid + 28)
        cases.add(cellid - 28)
        cases.add(cellid + 29)
        cases.add(cellid - 29)
        cases.add(cellid + 30)
        cases.add(cellid - 30)
        cases.add(cellid + 31)
        cases.add(cellid - 31)
        cases.add(cellid + 42)
        cases.add(cellid - 42)
        cases.add(cellid + 43)
        cases.add(cellid - 43)
        cases.add(cellid + 44)
        cases.add(cellid - 44)
        cases.add(cellid + 45)
        cases.add(cellid - 45)
        cases.add(cellid + 57)
        cases.add(cellid - 57)
        cases.add(cellid + 58)
        cases.add(cellid - 58)
        cases.add(cellid + 59)
        cases.add(cellid - 59)

        for (entry in cases) {
            val gameCase = this.getCase(entry)
            if (gameCase == null)
                continue
            if (data.cellHasMoveEndActions(gameCase.cellId))
                continue
            //Si la case n'est pas marchable
            if (!gameCase.isWalkable(false))
                continue
            //Si la case est prise par un groupe de monstre
            var ok = true
            for (mgEntry in this.mobGroups.entries)
                if (mgEntry.value.cellId == gameCase.cellId)
                    ok = false
            if (!ok)
                continue
            //Si la case est prise par un npc
            ok = true
            for (npcEntry in this.npcs.entries)
                if (npcEntry.value.cellId == gameCase.cellId)
                    ok = false
            if (!ok)
                continue
            //Si la case est prise par un joueur
            if (gameCase.players.isNotEmpty())
                continue
            //Sinon
            freecell.add(gameCase.cellId)
        }
        if (freecell.isEmpty())
            return -1
        val rand = Formulas.getRandomValue(0, freecell.size - 1)
        return freecell[rand]
    }

    fun onMapMonsterDeplacement() {
        if (mobGroups.size == 0)
            return
        val RandNumb = Formulas.getRandomValue(1, mobGroups.size)
        var i = 0
        for (group in mobGroups.values) {
            if (group.isFix && data.id != 8279)
                continue
            when (data.id) {
                8279 -> {// W:15   H:17
                    val cell1 = group.cellId
                    val cell2 = this.getCase(cell1 - 15)
                    val cell3 = this.getCase(cell1 - 15 + 1)
                    val cell4 = this.getCase(cell1 + 15 - 1)
                    val cell5 = this.getCase(cell1 + 15)
                    val case2 = (cell2 != null && (cell2.isWalkable(false) && (cell2.players.isEmpty())))
                    val case3 = (cell3 != null && (cell3.isWalkable(false) && (cell3.players.isEmpty())))
                    val case4 = (cell4 != null && (cell4.isWalkable(false) && (cell4.players.isEmpty())))
                    val case5 = (cell5 != null && (cell5.isWalkable(false) && (cell5.players.isEmpty())))
                    val array = ArrayList<Boolean>()
                    array.add(case2)
                    array.add(case3)
                    array.add(case4)
                    array.add(case5)

                    var count = 0
                    for (bo in array)
                        if (bo)
                            count++

                    if (count == 0)
                        return
                    if (count == 1) {
                        val newCell = (if (case2) cell2 else (if (case3) cell3 else (if (case4) cell4 else cell5)))
                        var nextCell: GameCase? = null
                        if (newCell == null)
                            return

                        if (newCell == cell2) {
                            if (checkCell(newCell.cellId - 15)) {
                                nextCell = this.getCase(newCell.cellId - 15)
                                if (this.checkCell(nextCell!!.cellId - 15)) {
                                    nextCell = this.getCase(nextCell.cellId - 15)
                                }
                            }
                        } else if (newCell == cell3) {
                            if (this.checkCell(newCell.cellId - 15 + 1)) {
                                nextCell = this.getCase(newCell.cellId - 15 + 1)
                                if (this.getCase(nextCell!!.cellId - 15 + 1) != null) {
                                    nextCell = this.getCase(nextCell.cellId - 15 + 1)
                                }
                            }
                        } else if (newCell == cell4) {
                            if (this.checkCell(newCell.cellId + 15 - 1)) {
                                nextCell = this.getCase(newCell.cellId + 15 - 1)
                                if (this.checkCell(nextCell!!.cellId + 15 - 1)) {
                                    nextCell = this.getCase(nextCell.cellId + 15 - 1)
                                }
                            }
                        } else if (newCell == cell5) {
                            if (this.checkCell(newCell.cellId + 15)) {
                                nextCell = this.getCase(newCell.cellId + 15)
                                if (this.checkCell(nextCell!!.cellId + 15)) {
                                    nextCell = this.getCase(nextCell.cellId + 15)
                                }
                            }
                        }

                        val pathstr: String?
                        try {
                            pathstr = PathFinding.getShortestStringPathBetween(this, group.cellId, nextCell!!.cellId, 0)
                        } catch (e: Exception) {
                            logger.error("unexpected error", e)
                return
                        }
                        if (pathstr == null)
                            return
                        group.cellId = nextCell.cellId
                        for (z in players)
                            SocketManager.GAME_SEND_GA_PACKET(z.getGameClient()!!, "0", "1", group.id.toString() + "", pathstr)
                    } else {
                        if (group.isFix)
                            continue
                        i++
                        if (i != RandNumb)
                            continue

                        var cell = -1
                        while (cell == -1 || cell == 383 || cell == 384
                            || cell == 398 || cell == 369)
                            cell = getRandomNearFreeCellId(group.cellId)
                        val pathstr: String?
                        try {
                            pathstr = PathFinding.getShortestStringPathBetween(this, group.cellId, cell, 0)
                        } catch (e: Exception) {
                            logger.error("unexpected error", e)
                return
                        }
                        if (pathstr == null)
                            return
                        group.cellId = cell
                        for (z in players)
                            SocketManager.GAME_SEND_GA_PACKET(z.getGameClient()!!, "0", "1", group.id.toString() + "", pathstr)
                    }
                }

                else -> {
                    if (group.isFix)
                        continue
                    i++
                    if (i != RandNumb)
                        continue
                    val cell = getRandomNearFreeCellId(group.cellId)
                    val pathstr: String?
                    try {
                        pathstr = PathFinding.getShortestStringPathBetween(this, group.cellId, cell, 0)
                    } catch (e: Exception) {
                        logger.error("unexpected error", e)
                return
                    }
                    if (pathstr == null)
                        return
                    group.cellId = cell
                    for (z in players)
                        if (z != null)
                            SocketManager.GAME_SEND_GA_PACKET(z.getGameClient()!!, "0", "1", group.id.toString() + "", pathstr)
                }
            }

        }
    }

    fun checkCell(id: Int): Boolean {
        return this.getCase(id - 15) != null && this.getCase(id - 15)!!.isWalkable(false)
    }

    fun getObjects(): String {
        if (this.mountPark == null || this.mountPark.getObject().size == 0)
            return ""
        val packets = StringBuilder("GDO+")
        var first = true
        for (entry in this.mountPark.getObject().entries) {
            for (entry2 in entry.value.entries) {
                if (!first) packets.append("|")
                val cellidDurab = entry.key
                packets.append(entry.key).append(";").append(entry2.key).append(";1;").append(getObjDurable(cellidDurab))
                first = false
            }
        }
        return packets.toString()
    }

    fun getObjDurable(CellID: Int): String {
        var packets = ""
        for (entry in this.mountPark!!.objDurab.entries) {
            for (entry2 in entry.value.entries) {
                if (CellID == entry.key)
                    packets += entry2.value.toString() + ";" + entry2.key
            }
        }
        return packets
    }

    fun cellSideLeft(cell: Int): Boolean {
        var ladoIzq = data.width
        for (i in 0 until data.width) {
            if (cell == ladoIzq)
                return true
            ladoIzq = ladoIzq + (data.width * 2) - 1
        }
        return false
    }

    fun cellSideRight(cell: Int): Boolean {
        var ladoDer = 2 * (data.width - 1)
        for (i in 0 until data.width) {
            if (cell == ladoDer)
                return true
            ladoDer = ladoDer + (data.width * 2) - 1
        }
        return false
    }

    fun cellSide(cell1: Int, cell2: Int): Boolean {
        if (cellSideLeft(cell1))
            if (cell2 == cell1 + (data.width - 1) || cell2 == cell1 - data.width)
                return true
        if (cellSideRight(cell1))
            if (cell2 == cell1 + data.width || cell2 == cell1 - (data.width - 1))
                return true
        return false
    }

    fun getGMOfMount(ok: Boolean): String {
        if (this.mountPark == null || this.mountPark.getListOfRaising().size == 0)
            return ""

        val mounts = ArrayList<Mount>()

        for (id in this.mountPark.getListOfRaising()) {
            val mount = World.world.getMountById(id)

            if (mount != null)
                if (this.getPlayer(mount.owner) != null || this.mountPark.guild != null)
                    mounts.add(mount)
        }

        if (ok)
            for (target in this.players)
                SocketManager.GAME_SEND_GM_MOUNT(target.getGameClient()!!, this, false)

        return this.getGMOfMount(mounts)
    }

    fun getGMOfMount(mounts: ArrayList<Mount>): String {
        if (this.mountPark == null || this.mountPark.getListOfRaising().size == 0)
            return ""
        val packets = StringBuilder()
        packets.append("GM|+")
        var first = true
        for (mount in mounts) {
            val GM = mount.parseToGM()
            if (GM == null || GM == "")
                continue
            if (!first)
                packets.append("|+")
            packets.append(GM)
            first = false
        }

        return packets.toString()
    }

    fun getPlayer(id: Int): Player? {
        for (cell in cases)
            for (player in cell.players)
                if (player != null)
                    if (player.id == id)
                        return player
        return null
    }

    fun onPlayerArriveOnCell(player: Player, id: Int) {
        val cell = this.getCase(id) ?: return

        if (cell.getDroppedItem(false) != null) {
            if (!Main.mapAsBlocked) {
                synchronized(cell) {
                    val obj = cell.getDroppedItem(true)

                    if (obj != null) {
                        if (Logging.USE_LOG)
                            Logging.getInstance().write("Object", "GetInOnTheFloor : " + player.name + " a ramassé [" + obj.template!!.id + "@" + obj.guid + ";" + obj.quantity + "]")
                        if (player.addItem(obj, true, false))
                            World.world.addGameObject(obj)
                        SocketManager.GAME_SEND_GDO_PACKET_TO_MAP(this, '-', id, 0, 0)
                        SocketManager.GAME_SEND_Ow_PACKET(player)
                    }
                }
            } else {
                SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("area.map.gamemap.onplayerarriveoncell"))
            }
        }

        this.data.onMoveEnd(player)

        if (data.places.size < 2)
            return
        if (player.curMap.id != data.id || !player.canAggro())
            return

        for (group in this.mobGroups.values) {
            if (PathFinding.getDistanceBetween(this, id, group.cellId) <= group.aggroDistance) {//S'il y aggr
                startFightVersusMonstres(player, group)
                return
            }
        }
    }

    fun send(packet: String) {
        this.players.stream().filter { Objects.nonNull(it) }.forEach { player -> player.send(packet) }
    }

    fun newFightbouf(init1: Player, init2: Player, fightTypeChallenge: Int): Fight? {
        if (init1.fight != null || init2.fight != null)
            return null
        var id = 1
        if (this._fights == null)
            this._fights = ArrayList()
        if (this._fights!!.isNotEmpty())
            id = (this._fights!!.toTypedArray()[this._fights!!.size - 1] as Fight).id + 1
        val f = Fight(id, init1, init2)
        this._fights!!.add(f)
        SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this)
        return f
    }

    private fun addStaticGroup(def: MobGroupDef) {
        while (this.mobGroups[this.nextObjectId] != null)
            this.nextObjectId--
        val group = MonsterGroup(this.nextObjectId, this, def)

        if (group.mobs.isEmpty())
            return
        this.mobGroups[this.nextObjectId] = group
        this.nextObjectId--
        this.fixMobGroups[-1000 + this.nextObjectId] = group
    }

    fun findObjectsPositionsByID(ids: List<Int>): Stream<Int> {
        return data.interactiveObjects().entries.stream()
            .filter { e -> ids.contains(e.value) }
            .map { it.key }
    }

    fun <T : Actor> addActor(cellId: Int, actor: T) {
        // Safety: remove actor from other cells
        this.actors.values.forEach { l -> l.remove(actor) }
        this.actors.computeIfAbsent(cellId) { HashSet() }.add(actor)
    }

    fun scripted(): SMap {
        return scriptVal
    }
}
