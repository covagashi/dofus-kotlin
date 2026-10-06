package org.starloco.locos.game.world

import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory
import org.starloco.locos.anims.Animation
import org.starloco.locos.area.Area
import org.starloco.locos.area.SubArea
import org.starloco.locos.area.map.GameMap
import org.starloco.locos.entity.map.InteractiveObjectTemplate
import org.starloco.locos.area.map.ScriptMapData
import org.starloco.locos.client.Account
import org.starloco.locos.client.Player
import org.starloco.locos.client.other.Stats
import org.starloco.locos.common.ConditionParser
import org.starloco.locos.common.Formulas
import org.starloco.locos.common.SocketManager
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.game.*
import org.starloco.locos.database.data.login.*
import org.starloco.locos.entity.Collector
import org.starloco.locos.entity.Prism
import org.starloco.locos.entity.map.House
import org.starloco.locos.entity.map.MountPark
import org.starloco.locos.entity.map.Trunk
import org.starloco.locos.entity.monster.Monster
import org.starloco.locos.entity.mount.Mount
import org.starloco.locos.entity.npc.NpcTemplate
import org.starloco.locos.entity.pet.Pet
import org.starloco.locos.entity.pet.PetEntry
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.spells.Spell
import org.starloco.locos.game.scheduler.Updatable
import org.starloco.locos.guild.Guild
import org.starloco.locos.hdv.BigStore
import org.starloco.locos.hdv.BigStoreListing
import org.starloco.locos.job.Job
import org.starloco.locos.kernel.Constant
import org.starloco.locos.kernel.Main
import org.starloco.locos.`object`.GameObject
import org.starloco.locos.`object`.entity.Fragment
import org.starloco.locos.`object`.ObjectSet
import org.starloco.locos.`object`.ObjectTemplate
import org.starloco.locos.`object`.entity.SoulStone
import org.starloco.locos.script.DataScriptVM
import org.starloco.locos.script.Scripted
import org.starloco.locos.script.proxy.SWorld
import org.starloco.locos.util.TimerWaiter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import kotlin.math.abs
import kotlin.math.floor

class World private constructor() : Scripted<SWorld> {
    private val scriptVal: SWorld = SWorld(this)

    companion object {
        @JvmField
        val world = World()
    }

    @JvmField
    var logger: Logger = LoggerFactory.getLogger(World::class.java) as Logger

    private val _accounts = HashMap<Int, Account>()
    private val _players = HashMap<Int, Player>()
    private val _maps = ConcurrentHashMap<Int, GameMap>()
    private val mapsData = ConcurrentHashMap<Int, ScriptMapData>()
    private val objects = ConcurrentHashMap<Int, GameObject>()

    @Volatile
    var experiences: ExperienceTables? = null

    private val _spells = HashMap<Int, Spell>()
    private val ObjTemplates = HashMap<Int, ObjectTemplate>()
    private val MobTemplates = HashMap<Int, Monster>()
    private val npcsTemplate = HashMap<Int, NpcTemplate>()
    private val Dragodindes = HashMap<Int, Mount>()
    private val _areas = HashMap<Int, Area>()
    private val _subAreas = HashMap<Int, SubArea>()
    private val Jobs = HashMap<Int, Job>()
    private val Crafts = HashMap<Int, ArrayList<Couple<Int, Int>>>()
    private val ItemSets = HashMap<Int, ObjectSet>()
    private val Guildes = HashMap<Int, Guild>()
    private val Hdvs = HashMap<Int, BigStore>()
    private val hdvsItems = HashMap<Int, MutableMap<Int, MutableList<BigStoreListing>>>()
    private val _animations = HashMap<Int, Animation>()
    private val MountPark = HashMap<Int, MountPark>()
    private val Trunks = HashMap<Int, Trunk>()
    private val _collectors = HashMap<Int, Collector>()
    private val Houses = HashMap<Int, House>()
    private val Seller = HashMap<Int, MutableList<Int>>()
    private val Challenges = StringBuilder()
    private val Prismes = HashMap<Int, Prism>()
    private val fullmorphs = HashMap<Int, MutableMap<String, String>>()
    private val Pets = HashMap<Int, Pet>()
    private val PetsEntry = HashMap<Int, PetEntry>()
    private val mobsGroupsFix = HashMap<String, MutableMap<String, String>>()
    private val extraMonstre = HashMap<Int, Map<String, Map<String, Int>>>()
    private val _extraMonstreOnMap = HashMap<Int, GameMap>()
    private val _delayCollectors = HashMap<Int, Long>()
    private val interactiveObjects = HashMap<Int, InteractiveObjectTemplate>()
    private val spriteToObject = HashMap<Int, Int>()

    @JvmField
    val scheduler = ScheduledThreadPoolExecutor(1)

    val delayCollectors: MutableMap<Int, Long>
        get() = _delayCollectors

    private val _houseManager = HouseManager()

    val houseManager: HouseManager
        get() = _houseManager

    private val _cryptManager = org.starloco.locos.common.CryptManager()

    val cryptManager: org.starloco.locos.common.CryptManager
        get() = _cryptManager

    private val _conditionManager = org.starloco.locos.common.ConditionParser()

    val conditionManager: ConditionParser
        get() = _conditionManager

    fun addAccount(account: Account) {
        _accounts[account.id] = account
    }

    fun ensureAccountLoaded(id: Int): Account? {
        var account = _accounts[id]
        if (account == null) {
            logger.info("Loading account #{}", id)
            DatabaseManager.get(AccountData::class.java).load(id)
        }
        return _accounts[id]
    }

    val accounts: Collection<Account>
        get() = _accounts.values

    fun getAccountsByIp(ip: String): List<Account> {
        val accounts = ArrayList<Account>()
        _accounts.values.stream().filter { account -> account != null && account.lastIP != null && account.lastIP.equals(ip, ignoreCase = true) }.forEach { accounts.add(it) }
        return accounts
    }

    fun getAccountByPseudo(pseudo: String): Account? {
        for (account in _accounts.values)
            if (account.pseudo == pseudo)
                return account
        return null
    }

    //region Players data
    val players: Collection<Player>
        get() = _players.values

    fun addPlayer(player: Player) {
        _players[player.id] = player
    }

    fun getPlayerByName(name: String): Player? {
        for (player in _players.values)
            if (player.name.equals(name, ignoreCase = true))
                return player
        return null
    }

    fun getPlayer(id: Int): Player? {
        return _players[id]
    }

    val onlinePlayers: List<Player>
        get() = _players.values.stream().filter { player -> player.isOnline && player.getGameClient() != null }.collect(Collectors.toList())
    //endregion

    //region Maps data
    val maps: Collection<GameMap>
        get() = _maps.values

    fun addMapData(map: ScriptMapData) {
        mapsData[map.id] = map

        // Update zaap list
        val c = map.zaapCell

        if (c == null || c < 0) {
            Constant.ZAAPS.remove(map.id)
        } else {
            Constant.ZAAPS[map.id] = c
        }

        // Make sure the subArea knows of that map
        Optional.ofNullable(_subAreas[map.subAreaID]).ifPresent { s -> s.addMapID(map.id) }
    }

    fun getMapData(id: Int): Optional<ScriptMapData> {
        return Optional.ofNullable(mapsData[id])
    }

    fun getMap(id: Int): GameMap {
        // Atomically get or load map
        return _maps.computeIfAbsent(id) { mapID ->
            val data = getMapData(mapID)
            if (!data.isPresent) throw IllegalStateException(("no data found for map #%d").format( mapID))
            GameMap(mapsData[mapID]!!)
        }
    }
    //endregion

    //region Objects data
    val gameObjects: CopyOnWriteArrayList<GameObject>
        get() = CopyOnWriteArrayList(objects.values)

    fun addGameObject(gameObject: GameObject?) {
        if (gameObject != null && !this.objects.containsKey(gameObject.guid)) {
            objects[gameObject.guid] = gameObject
        }
    }

    fun getGameObject(id: Int): GameObject? {
        var `object` = objects[id]
        if (`object` == null) {
            `object` = DatabaseManager.get(ObjectData::class.java).load(id)
        }
        return `object`
    }

    fun removeGameObject(id: Int) {
        if (objects.containsKey(id)) {
            (DatabaseManager.get(ObjectData::class.java) as ObjectData).delete(this.getGameObject(id)!!)
            objects.remove(id)
        }
    }
    //endregion

    val spells: Map<Int, Spell>
        get() = _spells

    val objectsTemplates: Map<Int, ObjectTemplate>
        get() = ObjTemplates

    val mounts: Map<Int, Mount>
        get() = Dragodindes

    val areas: Map<Int, Area>
        get() = _areas

    val subAreas: Map<Int, SubArea>
        get() = _subAreas

    val guilds: Map<Int, Guild>
        get() = Guildes

    val mountparks: Map<Int, MountPark>
        get() = MountPark

    val trunks: Map<Int, Trunk>
        get() = Trunks

    val collectors: MutableMap<Int, Collector>
        get() = _collectors

    val houses: Map<Int, House>
        get() = Houses

    val prisms: Map<Int, Prism>
        get() = Prismes

    val extraMonsters: Map<Int, Map<String, Map<String, Int>>>
        get() = extraMonstre

    fun loadScripts() {
        logger.debug("Loading script engine")
        try {
            DataScriptVM.init()
        } catch (e: Exception) {
            logger.error("init NpcScriptVM failed", e)
            throw RuntimeException("init NpcScriptVM failed", e)
        }
    }

    fun createWorld() {
        logger.info("Loading of data..")
        val time = System.currentTimeMillis()

        DatabaseManager.get(ServerData::class.java).loadFully()
        logger.debug("The reset of the logged players were done successfully.")

        DatabaseManager.get(AdData::class.java).loadFully()
        logger.debug("The ads were loaded successfully.")

        DatabaseManager.get(FullMorphData::class.java).loadFully()
        logger.debug("The incarnations were loaded successfully.")

        DatabaseManager.get(ExtraMonsterData::class.java).loadFully()
        logger.debug("The extra-monsters were loaded successfully.")

        DatabaseManager.get(SpellData::class.java).loadFully()
        logger.debug("The spells were loaded successfully.")

        DatabaseManager.get(MonsterData::class.java).loadFully()
        logger.debug("The monsters were loaded successfully.")

        DatabaseManager.get(ObjectTemplateData::class.java).loadFully()
        logger.debug("The template objects were loaded successfully.")

        DatabaseManager.get(PrismData::class.java).loadFully()
        logger.debug("The prisms were loaded successfully.")

        DatabaseManager.get(BaseAreaData::class.java).loadFully()
        logger.debug("The statics areas data were loaded successfully.")
        DatabaseManager.get(AreaData::class.java).loadFully()
        logger.debug("The dynamics areas data were loaded successfully.")

        DatabaseManager.get(BaseSubAreaData::class.java).loadFully()
        logger.debug("The statics sub-areas data were loaded successfully.")
        DatabaseManager.get(SubAreaData::class.java).loadFully()
        logger.debug("The dynamics sub-areas data were loaded successfully.")

        DatabaseManager.get(CraftData::class.java).loadFully()
        logger.debug("The crafts were loaded successfully.")

        DatabaseManager.get(JobData::class.java).loadFully()
        logger.debug("The jobs were loaded successfully.")

        DatabaseManager.get(ObjectSetData::class.java).loadFully()
        logger.debug("The panoplies were loaded successfully.")

//        DatabaseManager.get(ScriptedCellData.class).loadFully();
//        logger.debug("The scripted cells were loaded successfully.");
//
//        DatabaseManager.get(EndFightActionData.class).loadFully();
//        logger.debug("The end fight actions were loaded successfully.");
//
//        DatabaseManager.get(NpcData.class).loadFully();
//        logger.debug("The placement of non-player character were done successfully.");

        DatabaseManager.get(ObjectActionData::class.java).loadFully()
        logger.debug("The action of objects were loaded successfully.")

        DatabaseManager.get(DropData::class.java).loadFully()
        logger.debug("The drops were loaded successfully.")

        logger.debug("The mounts were loaded successfully.")

        DatabaseManager.get(GuildMemberData::class.java).loadFully()
        logger.debug("The guilds and guild members were loaded successfully.")

        DatabaseManager.get(PetData::class.java).loadFully()
        logger.debug("The pets were loaded successfully.")

        DatabaseManager.get(PetTemplateData::class.java).loadFully()
        logger.debug("The templates of pets were loaded successfully.")

        DatabaseManager.get(BaseMountParkData::class.java).loadFully()
        logger.debug("The statics parks of the mounts were loaded successfully.")
        DatabaseManager.get(MountParkData::class.java).loadFully()
        logger.debug("The dynamics parks of the mounts were loaded successfully.")

        DatabaseManager.get(CollectorData::class.java).loadFully()
        logger.debug("The collectors were loaded successfully.")

        DatabaseManager.get(BaseHouseData::class.java).loadFully()
        logger.debug("The statics houses were loaded successfully.")
        DatabaseManager.get(HouseData::class.java).loadFully()
        logger.debug("The dynamics houses were loaded successfully.")

        DatabaseManager.get(BaseTrunkData::class.java).loadFully()
        logger.debug("The statics trunks were loaded successfully.")
        DatabaseManager.get(TrunkData::class.java).loadFully()
        logger.debug("The dynamics trunks were loaded successfully.")

        DatabaseManager.get(ZaapiData::class.java).loadFully()
        logger.debug("The zappys were loaded successfully.")

        DatabaseManager.get(ChallengeData::class.java).loadFully()
        logger.debug("The challenges were loaded successfully.")

        DatabaseManager.get(HdvData::class.java).loadFully()
        logger.debug("The hotels of sales were loaded successfully.")

        DatabaseManager.get(BigStoreListingData::class.java).loadFully()
        logger.debug("The objects of hotels were loaded successfully.")

        DatabaseManager.get(RuneData::class.java).loadFully()
        logger.debug("The runes were loaded successfully.")

//        loadMonsterOnMap();
//        logger.debug("The adding of mobs groups on the maps were done successfully.");

//        DatabaseManager.get(GangsterData.class).loadFully();
//        logger.debug("The adding of gangsters on the maps were done successfully.");

//        logger.debug("Initialization of the dungeons : Dragon Pig.");
//        PigDragon.initialize();
//        logger.debug("Initialization of the dungeons : Labyrinth of the Minotoror.");
//        Minotoror.initialize();

        // Load auction
        DatabaseManager.get(AuctionData::class.java).loadFully()
        logger.debug("Initialization and loading auction : ok.")

        // Script engine
        world.loadScripts()

        world.loadExtraMonster()
        logger.debug("The adding of extra-monsters on the maps were done successfully.")

        (DatabaseManager.get(ServerData::class.java) as ServerData).update(time)
        logger.info("All data was loaded successfully at "
                + SimpleDateFormat("dd/MM/yyyy - HH:mm:ss", Locale.FRANCE).format(Date()) + " in "
                + SimpleDateFormat("mm", Locale.FRANCE).format((System.currentTimeMillis() - time)) + " min "
                + SimpleDateFormat("ss", Locale.FRANCE).format((System.currentTimeMillis() - time)) + " s.")
        logger.setLevel(ch.qos.logback.classic.Level.ALL)
    }

    fun addExtraMonster(idMob: Int, superArea: String, subArea: String, chances: Int) {
        val map = HashMap<String, Map<String, Int>>()
        val _map = HashMap<String, Int>()
        _map[subArea] = chances
        map[superArea] = _map
        extraMonstre[idMob] = map
    }

    val extraMonsterOnMap: Map<Int, GameMap>
        get() = _extraMonstreOnMap

    fun loadExtraMonster() {
        val mapPossible = ArrayList<GameMap>()
        for (i in extraMonstre.entries) {
            try {
                val map = i.value

                for (areaChances in map.entries) {
                    var chances: Int? = null
                    for (_e in areaChances.value.entries) {
                        val _c = _e.value
                        if (_c != -1)
                            chances = _c
                    }
                    if (areaChances.key != "") {// Si la superArea n'est pas null
                        for (ar in areaChances.key.split(",")) {
                            val Area = _areas[ar.toInt()] ?: continue
                            for (Map in Area.getMaps()) {
                                if (Map == null)
                                    continue
                                if (Map.haveMobFix())
                                    continue
                                if (!Map.isPossibleToPutMonster())
                                    continue

                                if (chances != null)
                                    Map.addMobExtra(i.key, chances)
                                else if (!mapPossible.contains(Map))
                                    mapPossible.add(Map)
                            }
                        }
                    }
                    if (areaChances.value != null) // Si l'area n'est pas null
                    {
                        for (area in areaChances.value.entries) {
                            val areas = area.key
                            for (sub in areas.split(",")) {
                                var subArea: SubArea? = null
                                try {
                                    subArea = _subAreas[sub.toInt()]
                                } catch (e: Exception) {
                                    logger.error("unexpected error", e)
                }
                                if (subArea == null)
                                    continue
                                for (Map in subArea.getMaps()) {
                                    if (Map == null)
                                        continue
                                    if (Map.haveMobFix())
                                        continue
                                    if (!Map.isPossibleToPutMonster())
                                        continue

                                    if (chances != null)
                                        Map.addMobExtra(i.key, chances)
                                    if (!mapPossible.contains(Map))
                                        mapPossible.add(Map)
                                }
                            }
                        }
                    }
                }
                if (mapPossible.size <= 0) {
                    throw Exception(" no maps was found for the extra monster " + i.key + ".")
                } else {
                    var randomMap: GameMap? = null
                    if (mapPossible.size == 1)
                        randomMap = mapPossible[0]
                    else
                        randomMap = mapPossible[Formulas.getRandomValue(0, mapPossible.size - 1)]
                    if (randomMap == null)
                        throw Exception("the random map is null.")
                    if (getMonstre(i.key) == null)
                        throw Exception("the monster template of the extra monster is invalid (id : " + i.key + ").")
                    if (randomMap.loadExtraMonsterOnMap(i.key))
                        _extraMonstreOnMap[i.key] = randomMap
                    else
                        throw Exception("a empty mobs group or invalid monster.")
                }

                mapPossible.clear()
            } catch (e: Exception) {
                logger.error("unexpected error", e)
                mapPossible.clear()
                logger.error("An error occurred when the server try to put extra-monster caused by : " + e.message)
            }
        }
    }

    fun getGroupFix(map: Int, cell: Int): Map<String, String>? {
        return mobsGroupsFix["$map;$cell"]
    }

    fun addGroupFix(str: String, mob: String, Time: Int) {
        mobsGroupsFix[str] = HashMap()
        mobsGroupsFix[str]!!["groupData"] = mob
        mobsGroupsFix[str]!!["timer"] = Time.toString() + ""
    }

    fun loadMonsterOnMap() {
        DatabaseManager.get(HeroicMobsGroupsData::class.java).loadFully()
        (DatabaseManager.get(HeroicMobsGroupsData::class.java) as HeroicMobsGroupsData).loadFix()

        _maps.values.stream().filter { Objects.nonNull(it) }.forEach { map ->
            try {
                map.loadMonsterOnMap()
            } catch (e: Exception) {
                logger.error("An error occurred when the server try to put monster on the map id " + map.id + ".")
            }
        }
    }

    fun getArea(areaID: Int): Area? {
        return _areas[areaID]
    }

    fun getSubArea(areaID: Int): SubArea? {
        return _subAreas[areaID]
    }

    fun addArea(area: Area) {
        _areas[area.id] = area
    }

    fun addSubArea(SA: SubArea) {
        _subAreas[SA.id] = SA
    }

    val sousZoneStateString: String
        get() {
            var str = ""
            var first = false
            for (subarea in _subAreas.values) {
                if (!subarea.conquerable)
                    continue
                if (first)
                    str += "|"
                str += subarea.id.toString() + ";" + subarea.alignment
                first = true
            }
            return str
        }

    fun getBalanceArea(area: Area, alignement: Int): Double {
        var cant = 0
        for (subarea in _subAreas.values) {
            if (subarea.area === area && subarea.alignment == alignement)
                cant++
        }
        if (cant == 0)
            return 0.0
        return Math.rint((1000 * cant / (area.getSubAreas().size)).toDouble() / 10)
    }

    fun getBalanceWorld(alignement: Int): Double {
        var cant = 0
        for (subarea in _subAreas.values) {
            if (subarea.alignment == alignement)
                cant++
        }
        if (cant == 0)
            return 0.0
        return Math.rint((10 * cant / 4).toDouble() / 10)
    }

    fun getConquestBonus(player: Player?): Double {
        if (player == null) return 1.0
        if (player.alignment == 0) return 1.0
        val factor = 1 + (getBalanceWorld(player.alignment) * Math.rint((player.getGrade() / 2.5) + 1)) / 100
        if (factor < 1) return 1.0
        return factor
    }

    fun registerObjectTemplate(t: InteractiveObjectTemplate) {
        synchronized(this.interactiveObjects) {
            this.interactiveObjects[t.id] = t
        }
    }

    fun setObjectForSprites(map: Map<Int, Int>) {
        synchronized(this.spriteToObject) {
            this.spriteToObject.clear()
            this.spriteToObject.putAll(map)
        }
    }

    fun getObjectIDForSprite(spriteID: Int): Optional<Int> {
        synchronized(this.spriteToObject) {
            return Optional.ofNullable(this.spriteToObject[spriteID])
        }
    }

    fun getObject(objectID: Int): Optional<InteractiveObjectTemplate> {
        synchronized(interactiveObjects) {
            return Optional.ofNullable(interactiveObjects[objectID])
        }
    }

    fun getObjectBySprite(spriteID: Int): Optional<InteractiveObjectTemplate> {
        return getObjectIDForSprite(spriteID).flatMap { this.getObject(it) }
    }

    fun getNPCTemplate(guid: Int): NpcTemplate? {
        return npcsTemplate[guid]
    }

    fun addNpcTemplate(temp: NpcTemplate) {
        if (npcsTemplate.containsKey(temp.id) && temp.legacy == null) {
            Main.logger.warn("Overwriting npc template #{} with script", temp.id)
        }
        npcsTemplate[temp.id] = temp
    }

    fun removePlayer(player: Player) {
        if (player.getGuild() != null) {
            if (player.getGuild()!!.getPlayers().size <= 1) {
                removeGuild(player.getGuild()!!.id)
            } else if (player.guildMember!!.rank == 1) {
                var curMaxRight = 0
                var leader: Player? = null

                for (newLeader in player.getGuild()!!.getPlayers())
                    if (newLeader !== player && newLeader.guildMember!!.rights < curMaxRight)
                        leader = newLeader

                player.getGuild()!!.removeMember(player)
                if (leader != null)
                    leader.guildMember!!.rank = 1
            } else {
                player.getGuild()!!.removeMember(player)
            }
        }
        if (player.wife != 0) {
            val wife = getPlayer(player.wife)

            if (wife != null) {
                wife.wife = 0
            }
        }
        player.remove()
        unloadPerso(player.id)
        _players.remove(player.id)
    }

    fun unloadPerso(perso: Player) {
        unloadPerso(perso.id)//UnLoad du perso+item
        _players.remove(perso.id)
    }

    fun addSort(sort: Spell) {
        _spells[sort.id] = sort
    }

    fun getSort(id: Int): Spell? {
        return _spells[id]
    }

    fun addObjTemplate(obj: ObjectTemplate) {
        ObjTemplates[obj.id] = obj
    }

    fun getObjTemplate(id: Int): ObjectTemplate? {
        return ObjTemplates[id]
    }

    fun getEtherealWeapons(level: Int): ArrayList<ObjectTemplate> {
        val array = ArrayList<ObjectTemplate>()
        val levelMin = (if (level - 5 < 0) 0 else level - 5)
        val levelMax = level + 5
        objectsTemplates.values.stream().filter { objectTemplate -> objectTemplate != null && objectTemplate.strTemplate.contains("32c#")
                && (levelMin < objectTemplate.level && objectTemplate.level < levelMax) && objectTemplate.type != 93 }.forEach { array.add(it) }
        return array
    }

    fun addMobTemplate(id: Int, mob: Monster) {
        MobTemplates[id] = mob
    }

    fun getMonstre(id: Int): Monster? {
        return MobTemplates[id]
    }

    val monstres: Collection<Monster>
        get() = MobTemplates.values

    val statOfAlign: String
        get() {
            var ange = 0
            var demon = 0
            var total = 0
            for (i in _players.values) {
                if (i == null)
                    continue
                if (i.alignment == 1)
                    ange++
                if (i.alignment == 2)
                    demon++
                total++
            }
            ange /= total
            demon /= total
            return if (ange > demon)
                "Les Brâkmarien sont actuellement en minorité, je peux donc te proposer de rejoindre les rangs Brâkmarien ?"
            else if (demon > ange)
                "Les Bontarien sont actuellement en minorité, je peux donc te proposer de rejoindre les rangs Bontarien ?"
            else
                " Aucune milice est actuellement en minorité, je peux donc te proposer de rejoindre aléatoirement une milice ?"
        }

    fun getMountById(id: Int): Mount? {
        var mount = Dragodindes[id]
        if (mount == null) {
            (DatabaseManager.get(MountData::class.java) as MountData).load(id)
            mount = Dragodindes[id]
        }
        return mount
    }

    fun addMount(mount: Mount) {
        Dragodindes[mount.id] = mount
    }

    fun removeMount(id: Int) {
        Dragodindes.remove(id)
    }

    val jobs: Collection<Job>
        get() = Jobs.values

    fun getMetier(id: Int): Job? {
        return Jobs[id]
    }

    fun addJob(metier: Job) {
        Jobs[metier.id] = metier
    }

    fun addCraft(id: Int, m: ArrayList<Couple<Int, Int>>) {
        Crafts[id] = m
    }

    fun getCraft(i: Int): ArrayList<Couple<Int, Int>>? {
        return Crafts[i]
    }

    fun addFullMorph(morphID: Int, name: String, gfxID: Int,
                     spells: String, args: Array<String>?) {
        if (fullmorphs[morphID] != null)
            return

        fullmorphs[morphID] = HashMap()

        fullmorphs[morphID]!!["name"] = name
        fullmorphs[morphID]!!["gfxid"] = gfxID.toString() + ""
        fullmorphs[morphID]!!["spells"] = spells
        if (args != null) {
            fullmorphs[morphID]!!["vie"] = args[0]
            fullmorphs[morphID]!!["pa"] = args[1]
            fullmorphs[morphID]!!["pm"] = args[2]
            fullmorphs[morphID]!!["vitalite"] = args[3]
            fullmorphs[morphID]!!["sagesse"] = args[4]
            fullmorphs[morphID]!!["terre"] = args[5]
            fullmorphs[morphID]!!["feu"] = args[6]
            fullmorphs[morphID]!!["eau"] = args[7]
            fullmorphs[morphID]!!["air"] = args[8]
            fullmorphs[morphID]!!["initiative"] = args[9]
            fullmorphs[morphID]!!["stats"] = args[10]
            fullmorphs[morphID]!!["donjon"] = args[11]
        }
    }

    fun getFullMorph(morphID: Int): Map<String, String>? {
        return fullmorphs[morphID]
    }

    fun getObjectByIngredientForJob(list: ArrayList<Int>?,
                                    ingredients: Map<Int, Int>): Int {
        if (list == null)
            return -1
        for (tID in list) {
            val craft = getCraft(tID)
            if (craft == null)
                continue
            if (craft.size != ingredients.size)
                continue
            var ok = true
            for (c in craft) {
                if (!((ingredients[c.first].toString() + " ") == (c.second.toString() + " "))) //si ingredient non présent ou mauvaise quantité
                    ok = false
            }
            if (ok)
                return tID
        }
        return -1
    }


    fun addItemSet(itemSet: ObjectSet) {
        ItemSets[itemSet.id] = itemSet
    }

    fun getItemSet(tID: Int): ObjectSet? {
        return ItemSets[tID]
    }

    val itemSetNumber: Int
        get() = ItemSets.size

    fun getMapByPosInArray(mapX: Int, mapY: Int): ArrayList<GameMap> {
        val i = ArrayList<GameMap>()
        for (map in _maps.values)
            if (map.x == mapX && map.y == mapY)
                i.add(map)
        return i
    }

    fun getMapIdByPosInSuperArea(mapX: Int, mapY: Int, superArea: Int): List<Int> {
        return mapsData.values.stream()
            .filter { Objects.nonNull(it) }
            .filter { map -> map.x == mapX && map.y == mapY }
            .filter { map -> Optional.ofNullable(map.getSubArea()).map { it.area }.map { it?.superArea }.orElse(-1) == superArea }
            .map { md -> md.id }
            .collect(Collectors.toList())
    }

    fun addGuild(g: Guild) {
        Guildes[g.id] = g
    }

    fun guildNameIsUsed(name: String): Boolean {
        for (g in Guildes.values)
            if (g.name.equals(name, ignoreCase = true))
                return true
        return false
    }

    fun guildEmblemIsUsed(emb: String): Boolean {
        for (g in Guildes.values) {
            if (g.emblem == emb)
                return true
        }
        return false
    }

    fun getGuild(i: Int): Guild? {
        var guild = Guildes[i]
        if (guild == null) {
            (DatabaseManager.get(GuildData::class.java) as GuildData).load(i)
            guild = Guildes[i]
        }
        return guild
    }

    fun getGuildByName(name: String): Int {
        for (g in Guildes.values) {
            if (g.name.equals(name, ignoreCase = true))
                return g.id
        }
        return -1
    }

    fun getGuildXpMax(lvl: Int): Long {
        return experiences!!.guilds.maxXpAt(lvl)
    }

    fun getZaapCellIdByMapId(i: Int): Int {
        return Constant.ZAAPS.getOrDefault(i, -1)
    }

    fun getEncloCellIdByMapId(i: Int): Int {
        val map = getMap(i)
        if (map != null && map.mountPark != null && map.mountPark.cell > 0)
            return map.mountPark.cell
        return -1
    }

    fun delDragoByID(getId: Int) {
        Dragodindes.remove(getId)
    }

    fun removeGuild(id: Int) {
        (DatabaseManager.get(GuildMemberData::class.java) as GuildMemberData).deleteAll(id)
        (DatabaseManager.get(GuildData::class.java) as GuildData).delete(this.Guildes[id]!!)
        this.houseManager.removeHouseGuild(id)
        GameMap.removeMountPark(id)
        Collector.removeCollector(id)
        Guildes.remove(id)
    }

    fun unloadPerso(g: Int) {
        val toRem = _players[g]
        if (toRem != null && !toRem.items.isEmpty())
            for (curObj in toRem.items.entries)
                objects.remove(curObj.key)

    }

    fun newObjet(id: Int, template: Int, qua: Int, pos: Int, stats: String, puit: Int): GameObject? {
        if (getObjTemplate(template) == null) {
            return null
        }

        if (template == 8378) {
            return Fragment(id, stats)
        } else if (getObjTemplate(template)!!.isFilledSoulStone()) {
            return SoulStone(id, qua, template, pos, stats)
        } else if (getObjTemplate(template)!!.type == 24 && (Constant.isCertificatDopeuls(getObjTemplate(template)!!.id) || getObjTemplate(template)!!.id == 6653)) {
            try {
                val txtStat = HashMap<Int, String>()
                txtStat[Constant.STATS_DATE] = stats.substring(3) + ""
                return GameObject(id, template, qua, Constant.ITEM_POS_NO_EQUIPED, Stats(false, null), ArrayList(), HashMap(), txtStat, puit)
            } catch (e: Exception) {
                logger.error("unexpected error", e)
                return GameObject(id, template, qua, pos, stats, 0)
            }
        } else {
            return GameObject(id, template, qua, pos, stats, 0)
        }
    }

    val changeHdv: Map<Int, Int>
        get() {
            val changeHdv = HashMap<Int, Int>()
            changeHdv[8753] = 8759 // HDV Annimaux
            changeHdv[4607] = 4271 // HDV Alchimistes
            changeHdv[4622] = 4216 // HDV Bijoutiers
            changeHdv[4627] = 4232 // HDV Bricoleurs
            changeHdv[5112] = 4178 // HDV Bûcherons
            changeHdv[4562] = 4183 // HDV Cordonniers
            changeHdv[8754] = 8760 // HDV Bibliothèque
            changeHdv[5317] = 4098 // HDV Forgerons
            changeHdv[4615] = 4247 // HDV Pêcheurs
            changeHdv[4646] = 4262 // HDV Ressources
            changeHdv[8756] = 8757 // HDV Forgemagie
            changeHdv[4618] = 4174 // HDV Sculpteurs
            changeHdv[4588] = 4172 // HDV Tailleurs
            changeHdv[8482] = 10129 // HDV Âmes
            changeHdv[4595] = 4287 // HDV Bouchers
            changeHdv[4630] = 2221 // HDV Boulangers
            changeHdv[5311] = 4179 // HDV Mineurs
            changeHdv[4629] = 4299 // HDV Paysans
            return changeHdv
        }

    // Utilisé deux fois. Pour tous les modes HDV dans la fonction getHdv ci-dessous et dans le mode Vente de GameClient.java
    fun changeHdv(map0: Int): Int {
        var map = map0
        val changeHdv = changeHdv
        if (changeHdv.containsKey(map)) {
            map = changeHdv[map]!!
        }
        return map
    }

    fun getHdv(map: Int): BigStore? {
        return Hdvs[changeHdv(map)]
    }

    fun addHdvItem(compteID: Int, hdvID: Int, toAdd: BigStoreListing) {
        //Si le compte n'est pas dans la memoire
        hdvsItems.computeIfAbsent(compteID) { HashMap() } //Ajout du compte cle:compteID et un nouveau Map<hdvID,items<>>
        hdvsItems[compteID]!!.computeIfAbsent(hdvID) { ArrayList() }.add(toAdd)
    }

    fun removeHdvItem(compteID: Int, hdvID: Int, toDel: BigStoreListing) {
        hdvsItems[compteID]!![hdvID]!!.remove(toDel)
    }

    fun addHdv(toAdd: BigStore) {
        Hdvs[toAdd.hdvId] = toAdd
    }

    fun getMyItems(compteID: Int): Map<Int, List<BigStoreListing>> {
        //Si le compte n'est pas dans la memoire
        hdvsItems.computeIfAbsent(compteID) { HashMap() }//Ajout du compte clé:compteID et un nouveau Map<hdvID,items
        return hdvsItems[compteID]!!
    }

    val objTemplates: Collection<ObjectTemplate>
        get() = ObjTemplates.values

    fun priestRequest(boy: Player, girl: Player, asked: Player) {
        if (boy.sexe == 0 && girl.sexe == 1) {
            val map = boy.curMap
            if (boy.wife != 0) {// 0 : femme | 1 = homme
                boy.blockMovement = false
                SocketManager.GAME_SEND_MESSAGE_TO_MAP(map, boy.name + " est déjà marier !", "B9121B")
                return
            }
            if (girl.wife != 0) {
                boy.blockMovement = false
                SocketManager.GAME_SEND_MESSAGE_TO_MAP(map, girl.name + " est déjà marier !", "B9121B")
                return
            }
            SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(map, "", -1, "Prêtre", "world.world.maried.pietre.ask.accept")
            SocketManager.GAME_SEND_WEDDING(617, (if (boy === asked) boy.id else girl.id), (if (boy === asked) girl.id else boy.id), -1)
        }
    }


    fun wedding(boy: Player, girl: Player, isOK: Int) {
        if (isOK > 0) {
            SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(boy.curMap, "", -1, "Prêtre", "world.world.maried.pietre.ask.ok")
            boy.wife = girl.id
            girl.wife = boy.id
        } else {
            SocketManager.GAME_SEND_Im_PACKET_TO_MAP(boy.curMap, "048;" + boy.name + "~" + girl.name)
        }
        boy.setisOK(0)
        boy.blockMovement = false
        girl.setisOK(0)
        girl.blockMovement = false
    }

    fun getAnimation(AnimationId: Int): Optional<Animation> {
        return Optional.ofNullable(_animations[AnimationId])
    }

    fun addAnimation(animation: Animation) {
        _animations[animation.id] = animation
    }

    fun addHouse(house: House) {
        Houses[house.id] = house
    }

    fun getHouse(id: Int): House? {
        return Houses[id]
    }

    fun addCollector(Collector: Collector) {
        _collectors[Collector.id] = Collector
    }

    fun getCollector(CollectorID: Int): Collector? {
        return _collectors[CollectorID]
    }

    fun addTrunk(trunk: Trunk) {
        Trunks[trunk.id] = trunk
    }

    fun getTrunk(id: Int): Trunk? {
        return Trunks[id]
    }

    fun addMountPark(mp: MountPark) {
        MountPark[mp.map] = mp
    }

    val mountParks: Map<Int, MountPark>
        get() = MountPark

    fun parseMPtoGuild(GuildID: Int): String {
        val G = getGuild(GuildID)
        val enclosMax = floor(G!!.lvl / 10.0).toInt().toByte()
        val packet = StringBuilder()
        packet.append(enclosMax)

        for (mp in MountPark.entries) {
            if (mp.value.guild != null
                && mp.value.guild!!.id == GuildID) {
                packet.append("|").append(mp.value.map).append(";").append(mp.value.size).append(";").append(mp.value.maxObject)// Nombre d'objets pour le dernier
                if (mp.value.getListOfRaising().size > 0) {
                    packet.append(";")
                    var primero = false
                    for (id in mp.value.getListOfRaising()) {
                        val dd = getMountById(id)
                        if (dd != null) {
                            if (primero)
                                packet.append(",")
                            packet.append(dd.color).append(",").append(dd.name).append(",")
                            if (getPlayer(dd.owner) == null)
                                packet.append("Sans maitre")
                            else
                                packet.append(getPlayer(dd.owner)!!.name)
                            primero = true
                        }
                    }
                }
            }
        }
        return packet.toString()
    }

    fun totalMPGuild(GuildID: Int): Int {
        var i = 0
        for (mp in MountPark.entries)
            if (mp.value.guild != null && mp.value.guild!!.id == GuildID)
                i++
        return i
    }

    fun addChallenge(chal: String) {
        if (Challenges.toString().isNotEmpty())
            Challenges.append(";")
        Challenges.append(chal)
    }

    @Synchronized
    fun addPrisme(Prisme: Prism) {
        Prismes[Prisme.id] = Prisme
    }

    fun getPrisme(id: Int): Prism? {
        return Prismes[id]
    }

    fun removePrisme(id: Int) {
        Prismes.remove(id)
    }

    fun AllPrisme(): Collection<Prism>? {
        if (Prismes.size > 0)
            return Prismes.values
        return null
    }

    fun PrismesGeoposition(player: Player, alignement: Int): String {
        val str = StringBuilder()
        var first = false
        var subareas = 0

        for (subarea in _subAreas.values) {
            if (player.curMap != null && player.curMap.subArea != null)
                if (subarea.area!!.superArea != player.curMap.subArea!!.area!!.superArea)
                    continue
            if (!subarea.conquerable)
                continue
            if (first)
                str.append(";")
            str.append(subarea.id).append(",").append(if (subarea.alignment == 0) -1 else subarea.alignment).append(",0,")
            if (subarea.prism == null)
                str.append(0.toString() + ",1")
            else
                str.append(subarea.prism!!.map).append(",1")
            first = true
            subareas++
        }
        if (alignement == 1) str.append("|").append(Area.bontarians)
        else if (alignement == 2) str.append("|").append(Area.brakmarians)

        str.append("|").append(_areas.size).append("|")
        first = false
        for (area in _areas.values) {
            if (area.alignement == 0)
                continue
            if (first)
                str.append(";")
            str.append(area.id).append(",").append(area.alignement).append(",1,").append(if (area.prismId == 0) 0 else 1)
            first = true
        }

        val factionAreaCounts = subAreaCountByFaction()
        val bontaAndBrakCount = factionAreaCounts.getOrDefault(Constant.ALIGNEMENT_BONTARIEN, 0L) + factionAreaCounts.getOrDefault(Constant.ALIGNEMENT_BRAKMARIEN, 0L)
        if (alignement == 1)
            str.insert(0, Area.bontarians.toString() + "|" + subareas + "|"
                    + (subareas - (bontaAndBrakCount)) + "|")
        else if (alignement == 2)
            str.insert(0, Area.brakmarians.toString() + "|" + subareas + "|"
                    + (subareas - (bontaAndBrakCount)) + "|")
        return str.toString()
    }

    fun subAreaCountByFaction(): Map<Int, Long> {
        return _subAreas.values.stream().collect(Collectors.groupingBy({ it.alignment }, Collectors.counting()))
    }

    fun showPrismes(perso: Player) {
        for (subarea in _subAreas.values) {
            if (subarea.alignment == 0)
                continue
            SocketManager.GAME_SEND_am_ALIGN_PACKET_TO_SUBAREA(perso, subarea.id.toString()
                    + "|" + subarea.alignment + "|1")
        }
    }

    @Synchronized
    fun getNextIDPrisme(): Int {
        var max = -102
        for (a in Prismes.keys)
            if (a < max)
                max = a
        return max - 3
    }

    fun addPets(pets: Pet) {
        Pets[pets.templateId] = pets
    }

    fun getPets(Tid: Int): Pet? {
        return Pets[Tid]
    }

    val pets: Collection<Pet>
        get() = Pets.values

    fun addPetsEntry(pets: PetEntry) {
        PetsEntry[pets.objectId] = pets
    }

    fun getPetsEntry(guid: Int): PetEntry? {
        return PetsEntry[guid]
    }

    fun removePetsEntry(guid: Int): PetEntry? {
        return PetsEntry.remove(guid)
    }

    fun getChallengeFromConditions(sevEnn: Boolean,
                                   sevAll: Boolean, bothSex: Boolean, EvenEnn: Boolean, MoreEnn: Boolean,
                                   hasCaw: Boolean, hasChaf: Boolean, hasRoul: Boolean, hasArak: Boolean,
                                   isBoss: Int, ecartLvlPlayer: Boolean, hasArround: Boolean,
                                   hasIndirectDamage: Boolean, isSolo: Boolean): String {
        val toReturn = StringBuilder()
        var isFirst = true
        var isGood = true
        var cond: Int

        for (chal in Challenges.toString().split(";")) {
            if (!isFirst && isGood)
                toReturn.append(";")
            isGood = true
            val id = chal.split(",")[0].toInt()
            cond = chal.split(",")[4].toInt()
            //Necessite plusieurs ennemis
            if (((cond and 1) == 1) && !sevEnn)
                isGood = false
            //Necessite plusieurs allies
            if ((((cond shr 1) and 1) == 1) && !sevAll)
                isGood = false
            //Necessite les deux sexes
            if ((((cond shr 2) and 1) == 1) && !bothSex)
                isGood = false
            //Necessite un nombre pair d'ennemis
            if ((((cond shr 3) and 1) == 1) && !EvenEnn)
                isGood = false
            //Necessite plus d'ennemis que d'allies
            if ((((cond shr 4) and 1) == 1) && !MoreEnn)
                isGood = false
            //Jardinier
            if (!hasCaw && (id == 7))
                isGood = false
            //Fossoyeur
            if (!hasChaf && (id == 12))
                isGood = false
            //Casino Royal
            if (!hasRoul && (id == 14))
                isGood = false
            //Araknophile
            if (!hasArak && (id == 15))
                isGood = false
            //Les mules d'abord
            if (!ecartLvlPlayer && (id == 48))
                isGood = false
            //Contre un boss de donjon
            if (isBoss != -1 && id == 5)
                isGood = false
            //Hardi
            if (!hasArround && id == 36)
                isGood = false
            //Mains propre
            if (!hasIndirectDamage && id == 19)
                isGood = false

            when (id) {
                47, 46, 45, 44 -> if (isSolo)
                    isGood = false
            }

            when (isBoss) {
                1045 -> //Kimbo
                    when (id) {
                        37, 8, 1, 2 -> isGood = false
                    }
                1072, //Tynril
                1085, //Tynril
                1086, //Tynril
                1087 -> //Tynril
                    when (id) {
                        36, 20 -> isGood = false
                    }
                1071 -> //Rasboul Majeur
                    when (id) {
                        9, 22, 17, 47 -> isGood = false
                    }
                780 -> //Skeunk
                    when (id) {
                        35, 25, 4, 32, 3, 31, 34 -> isGood = false
                    }
                113 -> //DC
                    when (id) {
                        12, 15, 7, 41 -> isGood = false
                    }
                612 -> //Maitre pandore
                    when (id) {
                        20, 37 -> isGood = false
                    }
                478, //Bworker
                568, //Tanukoui san
                940 -> //Rat blanc
                    when (id) {
                        20 -> isGood = false
                    }
                1188 -> //Blop multi
                    when (id) {
                        20, 46, 44 -> isGood = false
                    }
                865, //Grozila
                866 -> //Grasmera
                    when (id) {
                        31, 32 -> isGood = false
                    }
            }
            if (isGood)
                toReturn.append(chal)
            isFirst = false
        }
        return toReturn.toString()
    }

    fun verifyClone(p: Player) {
        if (p.curCell != null && p.fight == null) {
            if (p.curCell.players.contains(p)) {
                p.curCell.removePlayer(p)
                (DatabaseManager.get(PlayerData::class.java) as PlayerData).update(p)
            }
        }
        if (p.isOnline)
            (DatabaseManager.get(PlayerData::class.java) as PlayerData).update(p)
    }

    fun getRandomChallenge(nombreChal: Int,
                           challenges: String): ArrayList<String> {
        val MovingChals = ";1;2;8;36;37;39;40;"// Challenges de déplacements incompatibles
        var hasMovingChal = false
        val TargetChals = ";3;4;10;25;31;32;34;35;38;42;"// ceux qui ciblent
        var hasTargetChal = false
        val SpellChals = ";5;6;9;11;19;20;24;41;"// ceux qui obligent à caster spécialement
        var hasSpellChal = false
        val KillerChals = ";28;29;30;44;45;46;48;"// ceux qui disent qui doit tuer
        var hasKillerChal = false
        val HealChals = ";18;43;"// ceux qui empêchent de soigner
        var hasHealChal = false

        var compteur = 0
        var i: Int
        val toReturn = ArrayList<String>()
        var chal: String
        while (compteur < 100 && toReturn.size < nombreChal) {
            compteur++
            i = Formulas.getRandomValue(1, challenges.split(";").size)
            chal = challenges.split(";")[i - 1]// challenge au hasard dans la liste

            if (!toReturn.contains(chal))// si le challenge n'y etait pas encore
            {
                if (MovingChals.contains(";" + chal.split(",")[0] + ";"))// s'il appartient a une liste
                    if (!hasMovingChal)// et qu'aucun de la liste n'a ete choisi deja
                    {
                        hasMovingChal = true
                        toReturn.add(chal)
                        continue
                    } else
                        continue
                if (TargetChals.contains(";" + chal.split(",")[0] + ";"))
                    if (!hasTargetChal) {
                        hasTargetChal = true
                        toReturn.add(chal)
                        continue
                    } else
                        continue
                if (SpellChals.contains(";" + chal.split(",")[0] + ";"))
                    if (!hasSpellChal) {
                        hasSpellChal = true
                        toReturn.add(chal)
                        continue
                    } else
                        continue
                if (KillerChals.contains(";" + chal.split(",")[0] + ";"))
                    if (!hasKillerChal) {
                        hasKillerChal = true
                        toReturn.add(chal)
                        continue
                    } else
                        continue
                if (HealChals.contains(";" + chal.split(",")[0] + ";"))
                    if (!hasHealChal) {
                        hasHealChal = true
                        toReturn.add(chal)
                        continue
                    } else
                        continue
                toReturn.add(chal)
            }
            compteur++
        }
        return toReturn
    }

    fun getCollectorByMap(id: Int): Collector? {
        for (Collector in collectors.entries) {
            val map = getMap(Collector.value.map)
            if (map.id == id) {
                return Collector.value
            }
        }
        return null
    }

    fun addSeller(player: Player) {
        if (player.storeItems.isEmpty())
            return

        val map = player.curMap.id

        if (Seller[map] == null) {
            val players = ArrayList<Int>()
            players.add(player.id)
            Seller[map] = players
        } else {
            val players = ArrayList<Int>()
            players.add(player.id)
            players.addAll(Seller[map]!!)
            Seller.remove(map)
            Seller[map] = players
        }
    }

    fun getSeller(map: Int): Collection<Int>? {
        return Seller[map]
    }

    fun removeSeller(player: Int, map: Int) {
        if (getSeller(map) != null)
            Seller[map]!!.remove(player)
    }

    fun getTauxObtentionIntermediaire(bonus: Double, b1: Boolean, b2: Boolean): Double {
        var taux = bonus
        // 100.0 + 2*(30.0 + 2*10.0) => true true
        // 30.0 + 2*(10.0 + 2*3.0) => true false
        // 10.0 + 2*(3.0 + 2*1.0) => true true
        if (b1) {
            if (bonus == 100.0)
                taux += 2.0 * getTauxObtentionIntermediaire(30.0, true, b2)
            if (bonus == 30.0)
                taux += 2.0 * getTauxObtentionIntermediaire(10.0, (!b2), b2) // Si b2 est false alors on calculera 2*3.0 dans 10.0
            if (bonus == 10.0)
                taux += 2.0 * getTauxObtentionIntermediaire(3.0, (b2), b2) // Si b2 est true alors on calculera après
            else if (bonus == 3.0)
                taux += 2.0 * getTauxObtentionIntermediaire(1.0, false, b2)
        }

        return taux
    }

    fun getMetierByMaging(idMaging: Int): Int {
        var mId = -1
        when (idMaging) {
            43 -> mId = 17
            44 -> mId = 11
            45 -> mId = 14
            46 -> mId = 20
            47 -> mId = 31
            48 -> mId = 13
            49 -> mId = 19
            50 -> mId = 18
            62 -> mId = 15
            63 -> mId = 16
            64 -> mId = 27
        }
        return mId
    }

    fun getTempleByClasse(classe: Int): Int {
        var temple = -1
        when (classe) {
            Constant.CLASS_FECA -> temple = 1554
            Constant.CLASS_OSAMODAS -> temple = 1546
            Constant.CLASS_ENUTROF -> temple = 1470
            Constant.CLASS_SRAM -> temple = 6926
            Constant.CLASS_XELOR -> temple = 1469
            Constant.CLASS_ECAFLIP -> temple = 1544
            Constant.CLASS_ENIRIPSA -> temple = 6928
            Constant.CLASS_IOP -> temple = 1549
            Constant.CLASS_CRA -> temple = 1558
            Constant.CLASS_SADIDA -> temple = 1466
            Constant.CLASS_SACRIEUR -> temple = 6949
            Constant.CLASS_PANDAWA -> temple = 8490
        }
        return temple
    }

    fun sendMessageToAll(key: String, vararg str: Any) {
        TimerWaiter.addNext(Runnable {
            world.onlinePlayers.stream()
                .filter { player -> player != null && player.getGameClient() != null && player.isOnline && player.getLang() != null }
                .forEach { player -> player.sendMessage(player.getLang().trans(key, *str)) }
        }, 0, TimeUnit.SECONDS)
    }

    override fun scripted(): SWorld {
        return scriptVal
    }

    fun getAccount(id: Int): Account? {
        return _accounts[id]
    }

    class Drop {
        private val objectId: Int
        private val ceil: Int
        private val action: Int
        private val level: Int
        private val condition: String?
        private var percents: ArrayList<Double>?
        private var localPercent = 0.0

        constructor(objectId: Int, percents: ArrayList<Double>?, ceil: Int, action: Int, level: Int, condition: String) {
            this.objectId = objectId
            this.percents = percents
            this.ceil = ceil
            this.action = action
            this.level = level
            this.condition = condition
        }

        constructor(objectId: Int, percent: Double, ceil: Int) {
            this.objectId = objectId
            this.localPercent = percent
            this.ceil = ceil
            this.action = -1
            this.level = -1
            this.condition = ""
            this.percents = null
        }

        fun getObjectId(): Int {
            return objectId
        }

        fun getCeil(): Int {
            return ceil
        }

        fun getAction(): Int {
            return action
        }

        fun getLevel(): Int {
            return level
        }

        fun getCondition(): String? {
            return condition
        }

        fun getLocalPercent(): Double {
            return localPercent
        }

        fun copy(grade: Int): Drop? {
            val drop = Drop(this.objectId, null, this.ceil, this.action, this.level, this.condition ?: "")
            if (this.percents == null) return null
            if (this.percents!!.isEmpty()) return null
            try {
                drop.localPercent = this.percents!![grade - 1]
            } catch (ignored: IndexOutOfBoundsException) {
                return null
            }
            return drop
        }
    }

    class Couple<L, R>(@JvmField var first: L, @JvmField var second: R) {
        fun getFirst(): L {
            return first
        }

        fun getSecond(): R {
            return second
        }
    }
}
