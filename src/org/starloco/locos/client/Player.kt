package org.starloco.locos.client

import org.starloco.locos.area.Area
import org.starloco.locos.area.SubArea
import org.starloco.locos.area.map.Actor
import org.starloco.locos.area.map.GameCase
import org.starloco.locos.area.map.GameMap
import org.starloco.locos.entity.map.House
import org.starloco.locos.entity.map.MountPark
import org.starloco.locos.entity.map.Trunk
import org.starloco.locos.client.other.Party
import org.starloco.locos.client.other.Stalk
import org.starloco.locos.client.other.Stats
import org.starloco.locos.command.administration.Group
import org.starloco.locos.common.Formulas
import org.starloco.locos.common.CryptManager
import org.starloco.locos.common.SocketManager
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.game.*
import org.starloco.locos.database.data.login.AccountData
import org.starloco.locos.database.data.login.ObjectData
import org.starloco.locos.database.data.login.PlayerData
import org.starloco.locos.dynamic.Start
import org.starloco.locos.entity.Collector
import org.starloco.locos.entity.Prism
import org.starloco.locos.entity.mount.Mount
import org.starloco.locos.entity.pet.Pet
import org.starloco.locos.entity.pet.PetEntry
import org.starloco.locos.event.EventManager
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.spells.Spell
import org.starloco.locos.fight.spells.SpellEffect
import org.starloco.locos.game.GameClient
import org.starloco.locos.game.GameServer
import org.starloco.locos.game.action.ExchangeAction
import org.starloco.locos.game.action.GameAction
import org.starloco.locos.game.action.type.DocumentActionData
import org.starloco.locos.game.action.type.NpcDialogActionData
import org.starloco.locos.game.action.type.ScenarioActionData
import org.starloco.locos.game.world.World
import org.starloco.locos.guild.GuildMember
import org.starloco.locos.job.Job
import org.starloco.locos.job.JobAction
import org.starloco.locos.job.JobConstant
import org.starloco.locos.job.JobStat
import org.starloco.locos.kernel.Config
import org.starloco.locos.kernel.Constant
import org.starloco.locos.kernel.Main
import org.starloco.locos.kernel.Reboot
import org.starloco.locos.lang.LangEnum
import org.starloco.locos.`object`.GameObject
import org.starloco.locos.`object`.ItemHash
import org.starloco.locos.`object`.ObjectSet
import org.starloco.locos.`object`.ObjectTemplate
import org.starloco.locos.other.Action
import org.starloco.locos.guild.Guild
import org.starloco.locos.quest.QuestProgress
import org.starloco.locos.quest.QuestInfo
import org.starloco.locos.script.DataScriptVM
import org.starloco.locos.script.Scripted
import org.starloco.locos.script.proxy.SPlayer
import org.starloco.locos.util.Pair
import org.starloco.locos.util.TimerWaiter
import org.starloco.locos.database.data.game.SaleOffer.Currency

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.Map.Entry
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import java.util.stream.Collectors
import java.util.stream.Stream

import org.starloco.locos.kernel.Constant.INCARNAM_SUPERAREA
import org.slf4j.LoggerFactory
import org.starloco.locos.common.splitJ

private val log = LoggerFactory.getLogger(Player::class.java)

open class Player : Scripted<SPlayer>, Actor {

    lateinit var scriptVal: SPlayer

    var stats: Stats = Stats(true)
        get() = if (useStats) newStatsMorph() else field
    //Job
    //Disponibilit�
    @JvmField public var isAbsent: Boolean= false
    //Suiveur - Suivi
    @JvmField var follower: MutableMap<Int,Player> = HashMap()
    @JvmField var follow: Player? = null
    //Prison Alignement :
    public var isInEnnemyFaction: Boolean = false
    public var enteredOnEnnemyFaction: Long = 0
    public var donjon: Boolean = false
    //Commande h�h�
    @JvmField public var thatMap: Int= -1
    @JvmField public var thatCell: Int= -1
    @JvmField public var walkFast: Boolean= false
    public var getCases: Boolean= false
    var thisCases: ArrayList<Int> = ArrayList()
    @JvmField public var mpToTp: Boolean= false
    public var noall: Boolean= false
    var id: Int = 0
    var name: String = ""
        set(v) {
            field = v
            changeName = false
            DatabaseManager.get(PlayerData::class.java).updateInfos(this)
            if (this.guildMember != null)
                DatabaseManager.get(GuildMemberData::class.java).update(this)
        }
    var sexe: Int = 0
        set(v) {
            field = v
            gfxId = 10 * this.classe + v
        }
    var classe: Int = 0
    var color1: Int = 0
    var color2: Int = 0
    var color3: Int = 0
    var level: Int = 0
    var energy: Int = 0
        set(v) { field = if (v > Player.maxEnergy.toInt()) Player.maxEnergy.toInt() else v }
    var exp: Long = 0
    private var _curPdv: Int = 0
    var curPdv: Int
        get() {
            refreshLife(false)
            return _curPdv
        }
        set(v) { _curPdv = v }
    private var _maxPdv: Int = 0
    var maxPdv: Int
        get() = _maxPdv
        set(v) {
            _maxPdv = v
            SocketManager.GAME_SEND_STATS_PACKET(this)
            if (party != null)
                SocketManager.GAME_SEND_PM_MOD_PACKET_TO_GROUP(party!!, this)
        }
    var statsParcho: Stats = Stats(true)
    var kamas: Long = 0
    var spellPts: Int = 0
    var capital: Int = 0
    var size: Int = 0
    var gfxId: Int = 0
        set(gfxid) {
            if (this.classe * 10 + this.sexe != gfxid) {
                if (this.onMount)
                    this.toogleOnMount()
                this.send(if (gfxid != 8004) "AR3K" else "AR6bK")
            } else {
                this.send("AR6bK")
            }
            field = gfxid
        }
    var orientation: Int= 1
    //PDV
    var accID: Int = 0
    var canAggro: Boolean= true
    //Emote
    var emotes: MutableList<Int> = ArrayList()
    //Variables d'ali
    var alignment: Int= 0
    var deshonor: Int= 0
    var honor: Int= 0
    var showWings: Boolean= false
    var aLvl: Int= 0
    var guildMember: GuildMember? = null
    var showFriendConnection: Boolean = false
    var canaux: String = ""
    var fight: Fight? = null
        set(v) {
            if(this.setSitted(false) || v != null) {
                regenRate = 0
                this.send("ILF0")
            } else if(v == null) {
                regenRate = 1000
                this.send("ILS1000")
            }
            field = v
        }
    var away: Boolean = false
    lateinit var curMap: GameMap // Will become mapInstance GUID
    lateinit var curCell: GameCase
    var ready: Boolean= false
    var isOnline: Boolean= false
    var party: Party? = null
    var duelId: Int= -1
    var buffs: MutableMap<Int,SpellEffect> = HashMap()
    val objects: MutableMap<Int,GameObject> = HashMap()
    lateinit var savePos: Pair<Int,Int>
    var emoteActive: Int= 0
    var savestat: Int = 0
    var curHouse: House? = null
    //Invitation
    var inviting: Int= 0
    var craftingType: ArrayList<Int> = ArrayList()
    var metiers: MutableMap<Int,JobStat> = HashMap()
    //Enclos

    //Monture
    var mount: Mount? = null
    var mountXpGive: Int= 0
    var onMount: Boolean= false
    //Zaap
    val zaaps: ArrayList<Int> = ArrayList()

    //Sort
    lateinit var sorts: MutableMap<Int,Spell.SortStats>
    lateinit var sortsPlaces: MutableMap<Int,Int> // K: SpellID, V: Position
    val itemShortcuts: MutableMap<Int,ItemHash> = HashMap() // K: Position, V: Item Hash

    //Titre
    var currentTitle: Byte= 0
    //Mariage
    private var _wife: Int = 0
    var wife: Int
        get() = _wife
        set(v) {
            _wife = v
            DatabaseManager.get(PlayerData::class.java).update(this)
        }
    var isOK: Int= 0
    //Fantome
    var isGhost: Boolean= false
    var speed: Int= 0
    //Marchand
    var seeSeller: Boolean= false
    var storeItems: MutableMap<Int,Int> = HashMap()                    //<ObjID, Prix>
    //Metier
    var metierPublic: Boolean= false
    var livreArti: Boolean= false

    //Fight end
    var lastFight: Fight? = null
    var endFightAction: Action? = null
    //Item classe
    var objectsClass: ArrayList<Int> = ArrayList()
    var objectsClassSpell: MutableMap<Int,World.Couple<Int,Int>> = HashMap()
    // Taverne
    var timeTaverne: Long= 0
        set(v) {
            field = v
            DatabaseManager.get(PlayerData::class.java).updateTimeTaverne(this)
        }
    //GA
    var gameAction: GameAction? = null
    //Name
    //Fight :
    var spec: Boolean = false
    //Traque
    var traqued: Stalk? = null
    var doAction: Boolean = false
    //FullMorph Stats
    var morphMode: Boolean= false
    var morphId: Int = 0
    var saveSorts: MutableMap<Int,Spell.SortStats> = HashMap()
    var saveSortsPlaces: MutableMap<Int,Int> = HashMap()
    var saveSpellPts: Int = 0
    var pa: Int = 0
    var pm: Int = 0
    var vitalite: Int = 0
    var sagesse: Int = 0
    var terre: Int = 0
    var feu: Int = 0
    var eau: Int = 0
    var air: Int = 0
    var initiative: Int = 0
        get() {
            if (!useStats) {
                var fact: Int = 4
                var maxPdv: Int = this.maxPdv - 55
                var curPdv: Int = _curPdv - 55
                if (this.classe == Constant.CLASS_SACRIEUR)
                    fact = 8
                var coef: Double = maxPdv.toDouble() / fact

                coef += getStuffStats().getEffect(Constant.STATS_ADD_INIT)
                coef += getTotalStats(false).getEffect(Constant.STATS_ADD_AGIL)
                coef += getTotalStats(false).getEffect(Constant.STATS_ADD_CHAN)
                coef += getTotalStats(false).getEffect(Constant.STATS_ADD_INTE)
                coef += getTotalStats(false).getEffect(Constant.STATS_ADD_FORC)

                var init: Int = 1
                if (maxPdv != 0)
                    init = ((coef * ((curPdv.toDouble()) / (maxPdv.toDouble()))).toInt())
                if (init < 0)
                    init = 0
                return init
            } else {
                return field
            }
        }
    var useStats: Boolean= false
    var useCac: Boolean= true
    // Other ?
    var oldMap: Int= 0
    var oldCell: Int= 0
    var allTitle: String = ""
        get() {
            field = DatabaseManager.get(PlayerData::class.java).loadTitles(this.id)
            return field
        }
        set(v) {
            var title = v
            field
            var erreur: Boolean = false
            if (title == "")
                title = "0"
            if (field != null)
                for (i in  field.splitJ(","))
                    if (i == title)
                        erreur = true
            if (field == null && !erreur)
                field = title
            else if (!erreur)
                field += "," + title
            DatabaseManager.get(PlayerData::class.java).updateTitles(this.id, field)
        }
    var isBlocked: Boolean= false
    //Regen hp
    @set:JvmName("setSittedProp") var sitted: Boolean = false
    var regenRate: Int= 2000
    private var regenTime: Long = -1                                                //-1 veut dire que la personne ne c'est jamais connecte
    var isInPrivateArea: Boolean= false
    lateinit var start: Start
    var groupId: Int = 0
    @JvmField var isInvisible: Boolean= false
    var changeName: Boolean = false
        set(v) {
            field = v
            if (v) this.send("AlEr")
        }
    @JvmField public var afterFight: Boolean= false

    lateinit var Savecolors: String
    lateinit var Savestats: String


    constructor(id: Int, name: String, groupe: Int, sexe: Int, classe: Int, color1: Int, color2: Int, color3: Int, kamas: Long, pts: Int, capital: Int, energy: Int, level: Int, exp: Long, size: Int, gfxid: Int, alignement: Byte, account: Int, stats: Map<Int,Int>, seeFriend: Byte, seeAlign: Byte, seeSeller: Byte, canaux: String, map: Short, cell: Int, stuff: String, storeObjets: String, pdvPer: Int, spells: String, savePos: String, jobs: String, mountXp: Int, mount: Int, honor: Int, deshonor: Int, alvl: Int, zaaps: String, title: Byte, wifeGuid: Int, morphMode: String, allTitle: String, emotes: String, prison: Long, isNew: Boolean, parcho: String, timeDeblo: Long, noall: Boolean, deadInformation: String, deathCount: Byte, totalKills: Long) : this(id,
            name,
            groupe,
            sexe,
            classe,
            color1,
            color2,
            color3,
            kamas,
            pts,
            capital,
            energy,
            level,
            exp,
            size,
            gfxid,
            alignement,
            account,
            stats,
            seeFriend,
            seeAlign,
            seeSeller,
            canaux,
            map,
            cell,
            stuff,
            storeObjets,
            pdvPer,
            emptyMap(),
            emptyMap(),
            savePos,
            jobs,
            mountXp,
            mount,
            honor,
            deshonor,
            alvl,
            zaaps,
            title,
            wifeGuid,
            morphMode,
            allTitle,
            emotes,
            prison,
            isNew,
            parcho,
            timeDeblo,
            noall,
            deadInformation,
            deathCount,
            totalKills){parseSpells(spells, false)
    }

    private constructor(id: Int, name: String, groupe: Int, sexe: Int, classe: Int, color1: Int, color2: Int, color3: Int, kamas: Long, pts: Int, capital: Int, energy: Int, level: Int, exp: Long, size: Int, gfxid: Int, alignement: Byte, account: Int, stats: Map<Int,Int>, seeFriend: Byte, seeAlign: Byte, seeSeller: Byte, canaux: String, map: Short, cell: Int, stuff: String, storeObjets: String, pdvPer: Int, spells: Map<Int,Spell.SortStats>, spellPositions: Map<Int,Int>, savePos: String, jobs: String, mountXp: Int, mount: Int, honor: Int, deshonor: Int, alvl: Int, zaaps: String, title: Byte, wifeGuid: Int, morphMode: String, allTitle: String, emotes: String, prison: Long, isNew: Boolean, parcho: String, timeDeblo: Long, noall: Boolean, deadInformation: String, deathCount: Byte, totalKills: Long) {
        this.scriptVal = SPlayer(this)
        var morphMode = morphMode
        this.id = id
        this.noall = noall
        this.name = name
        this.groupId = groupe
        this.sexe = sexe
        this.classe = classe
        this.color1 = color1
        this.color2 = color2
        this.color3 = color3
        this.kamas = kamas
        this.capital = capital
        this.alignment = alignement.toInt()
        this.honor = honor
        this.deshonor = deshonor
        this.aLvl = alvl
        this.energy = energy
        this.level = level
        this.exp = exp
        if (mount != -1)
            this.mount = World.world.getMountById(mount)
        this.size = size
        this.gfxId = gfxid
        this.mountXpGive = mountXp
        this.stats = Stats(stats as MutableMap<Int, Int>, true, this)
        this.accID = account
        this.showFriendConnection = seeFriend.toInt() == 1
        _wife = wifeGuid
        this.metierPublic = false
        this.currentTitle = title
        this.changeName = false
        this.allTitle = allTitle
        this.seeSeller = seeSeller.toInt() == 1
        savestat = 0
        this.canaux = canaux
        this.curMap = World.world.getMap(map.toInt())
        var parts = savePos.split(",")
        this.savePos = Pair((parts[0]).toInt(), (parts[1]).toInt())
        this.regenTime = System.currentTimeMillis()
        this.timeTaverne = timeDeblo
        this.sorts = HashMap(spells)
        this.sortsPlaces = HashMap(spellPositions)
        try {
            var split = deadInformation.split(",")
            this.dead = split[0].toByte()
            this.deadTime = split[1].toLong()
            this.deadType = split[2].toByte()
            this.killByTypeId = split[3].toLong()
            if(split.size >= 5)
                this.deadLevel = split[4].toShort()
        } catch (e: Exception) {
            log.error("unexpected error", e)
        }
        this.totalKills = totalKills
        this.deathCount = deathCount
        try {
            if (!emotes.isEmpty())
                for (i in  emotes.splitJ(";"))
                    this.addStaticEmote((i).toInt())
            if (!morphMode.equals("")) {
                if (morphMode.equals("0"))
                    morphMode = "0;0"
                var i = morphMode.split(";")
                this.morphMode = i[0].equals("1")
                if (!i[1].equals(""))
                    morphId = (i[1]).toInt()
            }
            if (this.morphMode)
                this.saveSpellPts = pts
            else
                this.spellPts = pts
            if (prison != 0L) {
                this.isInEnnemyFaction = true
                this.enteredOnEnnemyFaction = prison
            }
            this.showWings = this.alignment != 0 && seeAlign.toInt() == 1
            if (curMap == null && World.world.getMap(7411) != null) {
                this.curMap = World.world.getMap( 7411)
                this.curCell = curMap.getCase(311)!!
            } else if (curMap == null && World.world.getMap(7411) == null) {
                throw IllegalStateException("Cannot find map 7411")
            } else if (curMap != null) {
                this.curCell = curMap.getCase(cell)!!
                if (curCell == null) {
                    this.curMap = World.world.getMap( 7411)
                    this.curCell = curMap.getCase(311)!!
                }
            }
            if (!zaaps.equals("", ignoreCase = true)) {
                for (str in  zaaps.splitJ(",")) {
                    try {
                        this.zaaps.add((str).toInt())
                    } catch (e: Exception) {
                        log.error("unexpected error", e)
                    }
                }
            }
            if (!isNew && (curMap == null || curCell == null)) {
                throw IllegalStateException("Cannot find map/cell for player")
            }
            this.parseObjects(stuff)
            try {
                if (parcho != null && !parcho.equals("", ignoreCase = true))
                    for (stat in  parcho.splitJ(";"))
                        if (!stat.equals("", ignoreCase = true))
                            this.statsParcho.addOneStat((stat.split(",")[0]).toInt(), (stat.split(",")[1]).toInt())
            } catch (e: Exception) {
                log.error("unexpected error", e)
            }

            if (!storeObjets.equals("")) {
                for (storeObjets in  storeObjets.splitJ("|")) {
                    var infos = storeObjets.split(",")
                    var guid: Int = 0
                    var price: Int = 0
                    try {
                        guid = (infos[0]).toInt()
                        price = (infos[1]).toInt()
                    } catch (e: Exception) {
                        log.error("unexpected error", e)
                        continue
                    }

                    var obj: GameObject? = World.world.getGameObject(guid)
                    if (obj == null)
                        continue

                    storeItems[obj.guid] = price
                }
            }
            _maxPdv = (this.level - 1) * 5 + 55 + getTotalStats(false).getEffect(Constant.STATS_ADD_VITA) + getTotalStats(false).getEffect(Constant.STATS_ADD_VIE)
            if (_curPdv <= 0)
                _curPdv = 1
            if (pdvPer > 100)
                _curPdv = (this.maxPdv * 100 / 100)
            else
                _curPdv = (this.maxPdv * pdvPer / 100)
            if (_curPdv <= 0)
                _curPdv = 1
            //Chargement des m�tiers
            if (!jobs.equals("")) {
                for (aJobData in  jobs.splitJ(";")) {
                    var infos = aJobData.split(",")
                    try {
                        var jobID: Int = (infos[0]).toInt()
                        var xp: Long = infos[1].toLong()
                        var m: Job = World.world.getMetier(jobID)!!
                        var SM: JobStat = metiers[learnJob(m)]!!
                        SM.addXp(this, xp)
                    } catch (e: Exception) {
                        log.error("unexpected error", e)
                    }
                }
            }
            if (this.energy == 0)
                setGhost()
            else if (this.energy == -1)
                setFuneral()
        } catch (e: Exception) {
            log.error("unexpected error", e)
        }
    }

    fun parseObjects(stuff: String) {
        var stuff = stuff
        if (!stuff.equals("")) {
            if (stuff[stuff.length - 1] == '|')
                stuff = stuff.substring(0, stuff.length - 1)
            DatabaseManager.get(ObjectData::class.java).loads(stuff.replace("|", ","))
        }
        for (item in  stuff.splitJ("|")) {
            if (item.equals(""))
                continue
            var infos = item.split(":")

            var guid: Int = 0
            try {
                guid = (infos[0]).toInt()
            } catch (e: Exception) {
                log.error("unexpected error", e)
                continue
            }

            var obj: GameObject? = World.world.getGameObject(guid)
            if (obj != null)
                objects[obj.guid] = obj
        }
    }

    var blockMovement: Boolean
        @JvmName("getBlockMovementProp") get() = this.isBlocked
        @JvmName("setBlockMovementProp") set(v) { this.isBlocked = v }
    var online: Boolean
        @JvmName("getOnlineProp") get() = this.isOnline
        @JvmName("setOnlineProp") set(v) { this.isOnline = v }
    var stalk: Stalk?
        @JvmName("getStalkProp") get() = this.traqued
        @JvmName("setStalkProp") set(v) { this.traqued = v }
    var inHouse: House?
        @JvmName("getInHouseProp") get() = this.curHouse
        @JvmName("setInHouseProp") set(v) { this.curHouse = v }
    val items: MutableMap<Int,GameObject>
        @JvmName("getItemsProp") get() = this.objects
    val account: Account
        @JvmName("getAccountProp") get() = this.getAccount()!!
    val guild: Guild?
        @JvmName("getGuildProp") get() = this.getGuild()
    val lang: LangEnum
        @JvmName("getLangProp") get() = this.getLang()
    val gameClient: GameClient?
        @JvmName("getGameClientProp") get() = this.getGameClient()
    val grade: Int
        @JvmName("getGradeProp") get() = this.getGrade()

    companion object {
        private const val MAX_BASIC_JOBS = 3
        private const val MIN_JOB_LVL_FOR_NEW_JOB = 30
        private const val MIN_JOB_FOR_SPECIALTY = 65
        const val maxEnergy: Short = 10000

    fun create(name: String, sexe: Int, classe: Int, color1: Int, color2: Int, color3: Int, compte: Account): Player? {
        var z: String = ""
        if (Config.allZaap) {
            z = Constant.ZAAPS.keys.stream().map({ it.toString() }).collect(Collectors.joining(","))
        }
        if (classe > 12 || classe < 1)
            return null
        if (sexe < 0 || sexe > 1)
            return null

        var startMapID: Int = if (Config.startMap > 0) Config.startMap.toInt() else Constant.getStartMap(classe).toInt()
        var startCellID: Int = if (Config.startCell > 0) Config.startCell.toInt() else Constant.getStartCell(classe).toInt()

        var player: Player = Player(-1, name, -1, sexe, classe, color1, color2, color3, Config.startKamas.toLong(), ((Config.startLevel - 1)), ((Config.startLevel - 1) * 5), Player.maxEnergy.toInt(), Config.startLevel,
                World.world.experiences!!.players.minXpAt(Config.startLevel), 100, (classe.toString() + "" + sexe).toInt(), (0).toByte(), compte.id, HashMap(), (1).toByte(), (0).toByte(), (0).toByte(), "*#%!pi$:?",
                startMapID.toShort(),
                startCellID,
                "", "", 100, Constant.getStartSorts(classe), Constant.getStartSortsPlaces(classe),
                "%d,%d".format(startMapID, startCellID),
                "", 0, -1, 0, 0, 0, z, (0).toByte(), 0, "0;0", "", (if (Config.allEmotes) "0;1;2;3;4;5;6;7;8;9;10;11;12;13;14;15;16;17;18;19;20;21" else "0"), 0, true, "118,0;119,0;123,0;124,0;125,0;126,0", 0, false, "0,0,0,0", (0).toByte(), 0)

        player.emotes.add(0)
        player.emotes.add(1)
        for (a in 1 .. player.level)
            Constant.onLevelUpSpells(player, a)

        if (!DatabaseManager.get(PlayerData::class.java).insert(player))
            return null

        SocketManager.GAME_SEND_WELCOME(player)
        World.world.sendMessageToAll("client.player.onjoingame.welcome", player.name)

        World.world.addPlayer(player)

        return player
    }


    fun getCompiledEmote(i: List<Int>): String {
        var i2: Int = 0
        for (b in  i) i2 += (2  shl  (b - 2))
        return i2.toString() + "|0"
    }
    }





    fun setColors(i: Int, i1: Int, i2: Int) {
        this.color1 = i
        this.color2 = i1
        this.color3 = i2
        DatabaseManager.get(PlayerData::class.java).updateInfos(this)
    }

    fun isDead(): Byte = dead

    fun getGroup(): Group? {
        return Group.byId(this.groupId)
    }

    fun setGroupe(groupId: Int, reload: Boolean) {
        this.groupId = groupId
        if (reload)
            DatabaseManager.get(PlayerData::class.java).updateGroupe(this)
    }








    fun getColors(): IntArray {
        var color1: Int = this.color1
        var color2: Int = this.color2
        var color3: Int = this.color3
        if (this.getObjetByPos(Constant.ITEM_POS_MALEDICTION) != null) {
            if (this.getObjetByPos(Constant.ITEM_POS_MALEDICTION)!!.template!!.id == 10838) {
                color1 = 16342021
                color2 = 16342021
                color3 = 16342021
            }
        }

        return intArrayOf(
            color1,
            color2,
            color3,
        )
    }
















    fun setPdv(pdv: Int) {
        _curPdv = pdv
        if (_curPdv >= this.maxPdv)
            _curPdv = this.maxPdv
        if (_curPdv < 0)
            _curPdv = 0

        if (party != null)
            SocketManager.GAME_SEND_PM_MOD_PACKET_TO_GROUP(party!!, this)
    }


    fun parseStatsParcho(): String {
        var parcho: String = ""
        for (i in  statsParcho.effects.entries)
            parcho += (if (parcho.isEmpty()) i.key.toString() + "," + i.value else ";" + i.key.toString() + "," + i.value)
        return parcho
    }



    fun setRoleplayBuff(id: Int) {
        var objTemplate: Int = 0
        when (id){  10673 -> {objTemplate = 10844
                
}
10669 -> {objTemplate = 10681
                
}
}
        if (objTemplate == 0)
            return
        if (getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF) != null) {
            var guid: Int = getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF)!!.guid
            SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, guid)
            this.deleteItem(guid)
        }

        var obj: GameObject = World.world.getObjTemplate(objTemplate)!!.createNewRoleplayBuff()!!
        this.addItem(obj, false, false)
        World.world.addGameObject(obj)
        SocketManager.GAME_SEND_ALTER_GM_PACKET(this.curMap, this)
        SocketManager.GAME_SEND_Ow_PACKET(this)
        SocketManager.GAME_SEND_STATS_PACKET(this)
        DatabaseManager.get(PlayerData::class.java).update(this)
    }

    fun setBenediction(id: Int) {
        if (getObjetByPos(Constant.ITEM_POS_BENEDICTION) != null) {
            var guid: Int = getObjetByPos(Constant.ITEM_POS_BENEDICTION)!!.guid
            SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, guid)
            this.deleteItem(guid)
        }
        if (id == 0) {
            SocketManager.GAME_SEND_ALTER_GM_PACKET(this.curMap, this)
            return
        }
        var turn: Int = 0
        when (id){  10682 -> {turn = 20
                
}
else -> {turn = 1
                
}
}

        var obj: GameObject = World.world.getObjTemplate(id)!!.createNewBenediction(turn)!!
        this.addItem(obj, false, false)
        World.world.addGameObject(obj)
        SocketManager.GAME_SEND_ALTER_GM_PACKET(this.curMap, this)
        SocketManager.GAME_SEND_Ow_PACKET(this)
        SocketManager.GAME_SEND_STATS_PACKET(this)
        DatabaseManager.get(PlayerData::class.java).update(this)
    }

    fun setMalediction(id: Int) {
        var objTemplate: Int = 0
        when (id){  10827 -> {objTemplate = 10838
                
}
else -> {objTemplate = id
        
}
}
        if (objTemplate == 0) {
            SocketManager.GAME_SEND_ALTER_GM_PACKET(this.curMap, this)
            return
        }
        if (getObjetByPos(Constant.ITEM_POS_MALEDICTION) != null) {
            var guid: Int = getObjetByPos(Constant.ITEM_POS_MALEDICTION)!!.guid
            SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, guid)
            this.deleteItem(guid)
        }

        var obj: GameObject? = World.world.getObjTemplate(objTemplate)!!.createNewMalediction()
        this.addItem(obj!!, false, false)
        World.world.addGameObject(obj)
        if (this.fight != null) {
            SocketManager.GAME_SEND_ALTER_GM_PACKET(this.curMap, this)
            SocketManager.GAME_SEND_Ow_PACKET(this)
            SocketManager.GAME_SEND_STATS_PACKET(this)
            DatabaseManager.get(PlayerData::class.java).update(this)
        }
    }

    fun setMascotte(id: Int) {
        if (getObjetByPos(Constant.ITEM_POS_PNJ_SUIVEUR) != null) {
            var guid: Int = getObjetByPos(Constant.ITEM_POS_PNJ_SUIVEUR)!!.guid
            SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, guid)
            this.deleteItem(guid)
        }
        if (id == 0) {
            SocketManager.GAME_SEND_ALTER_GM_PACKET(this.curMap, this)
            return
        }

        var obj: GameObject? = World.world.getObjTemplate(id)!!.createNewFollowPnj(1)
        if (obj != null)
            if (this.addItem(obj, false, false))
                World.world.addGameObject(obj)

        SocketManager.GAME_SEND_ALTER_GM_PACKET(this.curMap, this)
        SocketManager.GAME_SEND_Ow_PACKET(this)
        SocketManager.GAME_SEND_STATS_PACKET(this)
        DatabaseManager.get(PlayerData::class.java).update(this)
    }

    fun setCandy(id: Int) {
        if (getObjetByPos(Constant.ITEM_POS_BONBON) != null) {
            var guid: Int = getObjetByPos(Constant.ITEM_POS_BONBON)!!.guid
            SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, guid)
            this.deleteItem(guid)
        }
        var turn: Int = 30
        when (id) {
8948, 8949, 8950, 8951, 8952, 8953, 8954, 8955 -> {turn = 5
                
}
10665 -> {turn = 20
                
}
else -> {turn = 30
                
}
}

        var obj: GameObject? = World.world.getObjTemplate(id)!!.createNewCandy(turn)
        this.addItem(obj!!, false, false)
        World.world.addGameObject(obj)
        SocketManager.GAME_SEND_Ow_PACKET(this)
        SocketManager.GAME_SEND_STATS_PACKET(this)
        DatabaseManager.get(PlayerData::class.java).update(this)
    }

    fun calculTurnCandy() {
        var obj: GameObject? = getObjetByPos(Constant.ITEM_POS_BONBON)
        if (obj != null) {
            obj.stats.addOneStat(Constant.STATS_TURN, -1)
            if (obj.stats!!.getEffect(Constant.STATS_TURN) <= 0) {
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, obj.guid)
                this.deleteItem(obj.guid)
            } else {
                SocketManager.GAME_SEND_UPDATE_ITEM(this, obj)
            }
            DatabaseManager.get(ObjectData::class.java).update(obj)
        }
        obj = getObjetByPos(Constant.ITEM_POS_PNJ_SUIVEUR)
        if (obj != null) {
            obj.stats.addOneStat(Constant.STATS_TURN, -1)
            if (obj.stats.getEffect(Constant.STATS_TURN) <= 0) {
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, obj.guid)
                this.deleteItem(obj.guid)
            } else {
                SocketManager.GAME_SEND_UPDATE_ITEM(this, obj)
            }
            DatabaseManager.get(ObjectData::class.java).update(obj)
        }
        obj = getObjetByPos(Constant.ITEM_POS_BENEDICTION)
        if (obj != null) {
            obj.stats.addOneStat(Constant.STATS_TURN, -1)
            if (obj.stats.getEffect(Constant.STATS_TURN) <= 0) {
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, obj.guid)
                this.deleteItem(obj.guid)
            } else {
                SocketManager.GAME_SEND_UPDATE_ITEM(this, obj)
            }
            DatabaseManager.get(ObjectData::class.java).update(obj)
        }
        obj = getObjetByPos(Constant.ITEM_POS_MALEDICTION)
        if (obj != null) {
            obj.stats.addOneStat(Constant.STATS_TURN, -1)
            if (obj.stats.getEffect(Constant.STATS_TURN) <= 0) {
                gfxId = classe * 10 + sexe
                if (this.fight == null)
                    SocketManager.GAME_SEND_ALTER_GM_PACKET(curMap, this)
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, obj.guid)
                when (obj.template!!.id) {
8169, 8170 -> {unsetFullMorph()
                        
}
}

                this.deleteItem(obj.guid)
            } else {
                SocketManager.GAME_SEND_UPDATE_ITEM(this, obj)
            }
            DatabaseManager.get(ObjectData::class.java).update(obj)
        }
        obj = getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF)
        if (obj != null) {
            obj.stats.addOneStat(Constant.STATS_TURN, -1)
            if (obj.stats.getEffect(Constant.STATS_TURN) <= 0) {
                gfxId = classe * 10 + sexe
                SocketManager.GAME_SEND_ALTER_GM_PACKET(curMap, this)
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, obj.guid)
                this.deleteItem(obj.guid)
            } else {
                SocketManager.GAME_SEND_UPDATE_ITEM(this, obj)
            }
            DatabaseManager.get(ObjectData::class.java).update(obj)
        }
    }

    fun getSpells(): List<Spell.SortStats> {
        return ArrayList(sorts.values)
    }





    fun teleportOldMap() {
        if(oldMap > 0) {
            this.teleport(oldMap, oldCell)
        }
        this.oldMap = -1
        this.oldCell = -1
    }

    fun setCurrentPositionToOldPosition() {
        if(this.oldMap != -1) {
            this.curMap = World.world.getMap(this.oldMap)
            this.curCell = this.curMap.getCase(this.oldCell)!!
        }
    }

    fun setOldPosition() {
        this.oldMap = this.curMap.id
        this.oldCell = this.curCell.getId()
    }





    fun encodeSpellsToDB(): String {
        var packet: StringBuilder = StringBuilder()

        var spells: MutableMap<Int,Spell.SortStats> = this.sorts
        var positions: MutableMap<Int,Int> = sortsPlaces

        if (morphMode) {
            spells = saveSorts
            positions = saveSortsPlaces
        }

        if (spells.isEmpty())
            return ""
        for (key in  spells.keys) {
            var SS: Spell.SortStats? = spells[key]
            if (SS == null)
                continue
            packet.append(SS.spellID).append(";").append(SS.level).append(";")
            var position: Int = positions.getOrDefault(key, 126)
            if (position > 0 && position < 31)
                packet.append(position)
            packet.append(",")
        }
        return packet.substring(0, packet.length - 1)
    }

    fun parseSpells(str: String, send: Boolean) {
        if(str.length == 0) throw IllegalArgumentException("passed empty spell string to parseSpells")
//        if(str.equals("", ignoreCase = true)) {
//            sorts = Constant.getStartSorts(classe);
//            for (int a = 1; a <= this.getLevel(); a++)
//                Constant.onLevelUpSpells(this, a);
//            this.sortsPlaces = Constant.getStartSortsPlaces(this.classe);
//            return;
//        }

        var spells: MutableMap<Int,Spell.SortStats> = sorts
        var spellPositions: MutableMap<Int,Int> = sortsPlaces
        if (morphMode) {
            spells = saveSorts
            spellPositions = saveSortsPlaces
        }
        spells.clear()
        spellPositions.clear()

        var spellParts = str.split(",")
        for (e in  spellParts) {
            try {
                var parts = e.split(";")
                var id: Int = (parts[0]).toInt()
                var lvl: Int = (parts[1]).toInt()

                var ss: Spell.SortStats? = World.world.getSort(id)!!.getStatsByLevel(lvl)
                if(ss == null) throw IllegalStateException(("player has unknown spell: %d/%d").format( id, lvl))
                spells[id] = World.world.getSort(id)!!.getStatsByLevel(lvl)!!

                if(parts.size < 3 || parts[2].equals("", ignoreCase = true)) continue
                var position: Int = CryptManager.getIntByHashedValue(parts[2][0]) // may return -1
                if(position == 63) continue; // It was "_" which means no shortcut
                if(position > 30) {
                    // Too high to be a valid base64 position
                    position =  (parts[2]).toInt()
                }
                spellPositions[id] = position
            } catch (e1: NumberFormatException) {
                Main.logger.error("Cannot load player's spell", e1)
            }
        }
    }

    private fun parseSpellsFullMorph(str: String) {
        var spells = str.split(",")
        sorts.clear()
        sortsPlaces.clear()
        for (e in  spells) {
            try {
                var parts = e.split(";")
                var id: Int = (parts[0]).toInt()
                var lvl: Int = (parts[1]).toInt()
                var position: Int = CryptManager.getIntByHashedValue(parts[2][0]) // May return -1
                if(position == 63) continue; // base64 '_' -> decimal 63: Placeholder for "No shortcut"

                // Are we using the new 1.39 way: (Only decimals)
                if(parts[2].length > 1 || position > 30) {
                    position =  (parts[2]).toInt()
                }

                if (!morphMode)
                    learnSpell(id, lvl, false, false, false)
                else
                    learnSpell(id, lvl, false, true, false)
                sortsPlaces[id] = position
            } catch (e1: NumberFormatException) {
                log.error("unexpected error", e1)
            }
        }
    }


    fun setSavePos(mapID: Int, cellID: Int) {
        savePos = Pair(mapID, cellID)
    }




    fun getAccount(): Account? {
        return World.world.ensureAccountLoaded(accID)
    }

    fun get_spellPts(): Int {
        if (morphMode)
            return saveSpellPts
        else
            return spellPts
    }

    fun setSpellPoints(pts: Int) {
        if (morphMode)
            saveSpellPts = pts
        else
            spellPts = pts
    }

    fun getGuild(): Guild? {
        if (guildMember == null)
            return null
        return guildMember!!.guild
    }























    fun isMorphMercenaire(): Boolean {
        return (this.gfxId == 8009 || this.gfxId == 8006)
    }






    fun setSitted(sitted: Boolean): Boolean {
        if (this.sitted != sitted) {
            this.refreshLife(this.sitted)
            this.sitted = sitted
            this.regenRate = (if (sitted) 1000 else 2000)
            SocketManager.send(this, "ILS" + regenRate)
            return true
        }
        return false
    }


    fun canLearnJob(jobID: Int, sendIm: Boolean): Boolean {
        var job: Job? = World.world.getMetier(jobID)
        if(job == null) return false


        if(getMetierByID(jobID) != null) {
            // Already known
            if(sendIm) {
                SocketManager.GAME_SEND_Im_PACKET(this, "111")
            }
            return false
        }

        if(totalJobBasic()>=MAX_BASIC_JOBS) {
            if(sendIm) {
                SocketManager.GAME_SEND_Im_PACKET(this, "19")
            }
            return false
        }

        // Common Precondition: All current jobs > 30
        var min: Int = metiers.values.stream().mapToInt(JobStat::get_lvl).min().orElse(100)// 100 is the max level, so that's sure to be enough
        if(min < MIN_JOB_LVL_FOR_NEW_JOB) return false

        if(job.isMaging()) {
            // Magus Precondition: Less than 3 magus jobs
            if (totalJobFM() > 2) {
                if(sendIm) {
                    SocketManager.GAME_SEND_Im_PACKET(this, "19")
                }
                return false
            }

            var baseJobStats: JobStat? = metiers[World.world.getMetierByMaging(jobID)]
            if(baseJobStats == null || baseJobStats.get_lvl() < MIN_JOB_FOR_SPECIALTY) {
                if(sendIm) {
                    SocketManager.GAME_SEND_Im_PACKET(this, "111")
                }
                return false
            }
        }

        return true
    }

    fun tryLearnJob(jobID: Int): Boolean {
        var job: Job? = World.world.getMetier(jobID)
        if(job == null) return false

        if(!canLearnJob(jobID, true)) return false
        learnJob(job)
        return true
    }

    fun startScenario(id: Int, date: String, onEnd: BiConsumer<Player,Boolean>) {
        exchangeAction =  ExchangeAction(
                ExchangeAction.IN_SCENARIO,
                ScenarioActionData(exchangeAction!!, onEnd))
        SocketManager.GAME_SEND_TUTORIAL_CREATE(this, id, date)
    }

    override fun Id(): Long {
        return id.toLong()
    }

    override fun name(): String {
        return name
    }

    fun openDocument(id: Int, date: String) {
        exchangeAction =  ExchangeAction(ExchangeAction.READING_DOCUMENT, DocumentActionData(id))
        SocketManager.GAME_SEND_DOCUMENT_CREATE_PACKET(getGameClient()!!, id, date)
    }

    fun showReceivedItem(actorID: Int, quantity: Int) {
        SocketManager.GAME_SEND_IQ_PACKET(this, actorID, quantity)
    }

    fun resetStats(includeScrolls: Boolean) {
        stats.addOneStat(125, -stats.getEffect(125))
        stats.addOneStat(124, -stats.getEffect(124))
        stats.addOneStat(118, -stats.getEffect(118))
        stats.addOneStat(123, -stats.getEffect(123))
        stats.addOneStat(119, -stats.getEffect(119))
        stats.addOneStat(126, -stats.getEffect(126))

        if(includeScrolls) statsParcho.effects.clear()

        this.addCapital((level - 1) * 5 - capital)

        if(isOnline)SocketManager.GAME_SEND_STATS_PACKET(this)
    }

    fun spellResetPanel() {
        // TODO: Check if player is in another ExchangeAction
        exchangeAction = ExchangeAction(ExchangeAction.FORGETTING_SPELL, 0)
        SocketManager.GAME_SEND_FORGETSPELL_INTERFACE('+', this)
    }


    open class EnsureSpellLevelResult {
        val changed: Boolean
        val ptsDelta: Int
        val oldLevel: Int
        val worked: Boolean // can only be false when spell/level doesn't exist, or modPoints is true

        constructor(changed: Boolean, ptsDelta: Int, oldLevel: Int, worked: Boolean) {
            this.changed = changed
            this.ptsDelta = ptsDelta
            this.oldLevel = oldLevel
            this.worked = worked
        }
    }

    // returns Couple<ptsDelta,worked> Worked can only be false when spell/level doesn't exist, or modPoints is true.
    fun ensureSpellLevelSilent(spell: Int, newLevel: Int, modPoints: Boolean): EnsureSpellLevelResult {
        var previousLevel: Int = Optional.ofNullable(sorts[spell]).map(Spell.SortStats::level).orElse(0)

        // Already in the state we want
        if(previousLevel==newLevel) return EnsureSpellLevelResult(false, 0, 0, true)

        var ss: Spell.SortStats? = Optional.ofNullable(World.world.getSort(spell)).map({ s -> s.getStatsByLevel(newLevel) }).orElse(null)
        if(ss==null) return EnsureSpellLevelResult(false, 0, 0, false)

        var ptsDelta: Int = 0
        if(modPoints) {
            // Compute price changing from lvl 1 -> N:  Price(N) = SumInt(N-1) with SumInt(n) = n(n+1)/2
            // modPoints(Old,New) =  Price(Old) - Price(New)
            ptsDelta = (previousLevel*(previousLevel-1) - (newLevel * (newLevel-1)))/2

            if(spellPts < ptsDelta) {
                // Not enough points
                return EnsureSpellLevelResult(false, 0, previousLevel, false)
            }
            spellPts += ptsDelta
        }

        // Set spell
        sorts[spell] = ss
        return EnsureSpellLevelResult(true, ptsDelta, previousLevel, true)
    }

    fun ensureSpellLevel(spell: Int, level: Int, modPoints: Boolean, silent: Boolean): Boolean {
        var result: EnsureSpellLevelResult = ensureSpellLevelSilent(spell, level, modPoints)

        if(!result.worked) return false
        if(silent || !isOnline || !result.changed) return true

        SocketManager.GAME_SEND_SPELL_LIST(this)
        if(result.oldLevel == 0) {
            // Learned // Do we need a different message to show  c
            SocketManager.GAME_SEND_Im_PACKET(this, "03;" + spell)
        } else if (level == 0){
            // Unlearned // Do we need a different message ID for negative deltas ?
            SocketManager.GAME_SEND_Im_PACKET(this, "0154;" + "<b>" + result.oldLevel + "</b>" + "~" + "<b>" + result.ptsDelta + "</b>")
        } else {
            // Change level
            SocketManager.GAME_SEND_SPELL_UPGRADE_SUCCESS(this.getGameClient()!!, id, level)

        }
        if(result.ptsDelta!=0) {
            SocketManager.GAME_SEND_STATS_PACKET(this)
        }
        return true
    }

    fun learnSpell(spell: Int, level: Int, pos: Int) {
        if (World.world.getSort(spell)!!.getStatsByLevel(level) == null) {
            GameServer.a()
            return
        }

        if (spell !in sorts) {
            sorts[spell] = World.world.getSort(spell)!!.getStatsByLevel(level)!!
            removeSpellShortcutAtPosition(pos)
            sortsPlaces.remove(spell)
            sortsPlaces[spell] = pos
            SocketManager.GAME_SEND_SPELL_LIST(this)
            SocketManager.GAME_SEND_Im_PACKET(this, "03;" + spell)
        }
    }

    fun learnSpell(spellID: Int, level: Int, save: Boolean, send: Boolean, learn: Boolean): Boolean {
        if (World.world.getSort(spellID)!!.getStatsByLevel(level) == null) {
            GameServer.a()
            return false
        }

        if (spellID in sorts && learn) {
            SocketManager.GAME_SEND_MESSAGE(this, this.getLang().trans("client.player.learnspell.exist"))
            return false
        } else {
            sorts[spellID] = World.world.getSort(spellID)!!.getStatsByLevel(level)!!
            if (send) {
                SocketManager.GAME_SEND_SPELL_LIST(this)
                SocketManager.GAME_SEND_Im_PACKET(this, "03;" + spellID)
            }
            if (save)
                DatabaseManager.get(PlayerData::class.java).update(this)
            return true
        }
    }

    fun unlearnSpell(spell: Int): Boolean {
        if (World.world.getSort(spell) == null) {
            GameServer.a()
            return false
        }

        sorts.remove(spell)
        this.sortsPlaces.remove(spell)
        SocketManager.GAME_SEND_SPELL_LIST(this)
        SocketManager.GAME_SEND_STATS_PACKET(this)
        DatabaseManager.get(PlayerData::class.java).update(this)
        return true
    }

    fun unlearnSpell(perso: Player, spellID: Int, level: Int, ancLevel: Int, save: Boolean, send: Boolean): Boolean {
        var spellPoint: Int = 1
        if (ancLevel == 2)
            spellPoint = 1
        if (ancLevel == 3)
            spellPoint = 2 + 1
        if (ancLevel == 4)
            spellPoint = 3 + 3
        if (ancLevel == 5)
            spellPoint = 4 + 6
        if (ancLevel == 6)
            spellPoint = 5 + 10

        if (World.world.getSort(spellID)!!.getStatsByLevel(level) == null) {
            GameServer.a()
            return false
        }

        sorts[(spellID).toInt()] = World.world.getSort(spellID)!!.getStatsByLevel(level)!!
        if (send) {
            SocketManager.GAME_SEND_SPELL_LIST(this)
            SocketManager.GAME_SEND_Im_PACKET(this, "0154;" + "<b>" + ancLevel + "</b>" + "~" + "<b>" + spellPoint + "</b>")
            addSpellPoint(spellPoint)
            SocketManager.GAME_SEND_STATS_PACKET(perso)
        }
        if (save)
            DatabaseManager.get(PlayerData::class.java).update(this)
        return true
    }

    fun boostSpell(spellID: Int): Boolean {
        if (getSortStatBySortIfHas(spellID) == null)
            return false
        var AncLevel: Int = getSortStatBySortIfHas(spellID).level
        if (AncLevel == 6)
            return false
        if (spellPts >= AncLevel && World.world.getSort(spellID)!!.getStatsByLevel(AncLevel + 1)!!.reqLevel <= this.level) {
            if (learnSpell(spellID, AncLevel + 1, true, false, false)) {
                spellPts -= AncLevel
                DatabaseManager.get(PlayerData::class.java).update(this)
                return true
            } else {
                return false
            }
        } else
        //Pas le niveau ou pas les Points
        {
            if (spellPts < AncLevel)
                if (World.world.getSort(spellID)!!.getStatsByLevel(AncLevel + 1)!!.reqLevel > this.level)
                    return false
        }
        return away
    }

    fun boostSpellIncarnation() {
        for (i in  sorts.entries) {
            if (getSortStatBySortIfHas(i.value.spellID) == null)
                continue
            if (learnSpell(i.value.spellID, i.value.level + 1, true, false, false))
                DatabaseManager.get(PlayerData::class.java).update(this)
        }
    }

    fun forgetSpell(spellID: Int): Boolean {
        if (getSortStatBySortIfHas(spellID) == null) {
            return false
        }
        var AncLevel: Int = getSortStatBySortIfHas(spellID).level
        if (AncLevel <= 1)
            return false

        if (learnSpell(spellID, 1, true, false, false)) {
            spellPts += Formulas.spellCost(AncLevel)
            DatabaseManager.get(PlayerData::class.java).update(this)
            return true
        } else {
            return false
        }
    }

    fun demorph() {
        if (this.morphMode) {
            var morphID: Int = this.classe * 10 + this.sexe
            this.gfxId = morphID
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.curMap, this.id)
            SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(this.curMap, this)
        }
    }




    fun setFullMorph(morphid: Int, isLoad: Boolean, join: Boolean) {
        if (this.onMount) this.toogleOnMount()
        if (morphMode && !join)
            unsetFullMorph()
        if (this.isGhost) {
            SocketManager.send(this, "Im1185")
            return
        }

        var fullMorph: Map<String,String>? = World.world.getFullMorph(morphid)

        if (fullMorph == null) return

        if (!join) {
            if (!morphMode) {
                saveSpellPts = spellPts
                saveSorts.putAll(sorts)
                saveSortsPlaces.putAll(sortsPlaces)
            }
            if (isLoad) {
                saveSpellPts = spellPts
                saveSorts.putAll(sorts)
                saveSortsPlaces.putAll(sortsPlaces)
            }
        }

        morphMode = true
        sorts.clear()
        sortsPlaces.clear()
        spellPts = 0


        gfxId = (fullMorph["gfxid"]!!).toInt()
        if (this.fight == null) SocketManager.GAME_SEND_ALTER_GM_PACKET(this.curMap, this)
        parseSpellsFullMorph(fullMorph["spells"]!!)
        morphId = morphid

        if (this.getObjetByPos(Constant.ITEM_POS_ARME) != null)
            if (Constant.isIncarnationWeapon(this.getObjetByPos(Constant.ITEM_POS_ARME)!!.template!!.id))
                for (i in 0 .. this.getObjetByPos(Constant.ITEM_POS_ARME)!!.soulStat[Constant.STATS_NIVEAU]!!)
                    if (i == 10 || i == 20 || i == 30 || i == 40 || i == 50)
                        boostSpellIncarnation()
        if (this.fight == null) {
            SocketManager.GAME_SEND_ASK(this.getGameClient()!!, this)
            SocketManager.GAME_SEND_SPELL_LIST(this)
        }


        if (fullMorph["vie"] != null) {
            try {
                _maxPdv = (fullMorph["vie"])!!.toInt()
                this.setPdv(this.maxPdv)
                this.pa = (fullMorph["pa"])!!.toInt()
                this.pm = (fullMorph["pm"])!!.toInt()
                this.vitalite = (fullMorph["vitalite"])!!.toInt()
                this.sagesse = (fullMorph["sagesse"])!!.toInt()
                this.terre = (fullMorph["terre"])!!.toInt()
                this.feu = (fullMorph["feu"])!!.toInt()
                this.eau = (fullMorph["eau"])!!.toInt()
                this.air = (fullMorph["air"])!!.toInt()
                this.initiative = (fullMorph["initiative"])!!.toInt() + this.sagesse + this.terre + this.feu + this.eau + this.air
                this.useStats = fullMorph["stats"].equals("1")
                this.donjon = fullMorph["donjon"].equals("1")
                this.useCac = false
            } catch (e: Exception) {
                log.error("unexpected error", e)
            }
        }

        if (this.fight == null) SocketManager.GAME_SEND_STATS_PACKET(this)
        if (!join)
            DatabaseManager.get(PlayerData::class.java).update(this)
    }

    fun isMorph(): Boolean {
        return (this.gfxId != 8004 && this.gfxId != (this.classe * 10 + this.sexe))
    }

    fun canCac(): Boolean {
        return this.useCac
    }

    fun unsetMorph() {
        this.gfxId = this.classe * 10 + this.sexe
        SocketManager.GAME_SEND_ALTER_GM_PACKET(this.curMap, this)
        DatabaseManager.get(PlayerData::class.java).update(this)
    }

    fun unsetFullMorph() {
        if (!morphMode)
            return

        var morphID: Int = this.classe * 10 + this.sexe
        gfxId = morphID

        useStats = false
        donjon = false
        morphMode = false
        this.useCac = true
        sorts.clear()
        sortsPlaces.clear()
        spellPts = saveSpellPts
        sorts.putAll(saveSorts)
        sortsPlaces.putAll(saveSortsPlaces)
        parseSpells(encodeSpellsToDB(), true)

        morphId = 0
        if (this.fight == null) {
            SocketManager.GAME_SEND_SPELL_LIST(this)
            SocketManager.GAME_SEND_STATS_PACKET(this)
            SocketManager.GAME_SEND_ALTER_GM_PACKET(this.curMap, this)
        }
        DatabaseManager.get(PlayerData::class.java).update(this)
    }

    fun encodeSpellListForSL(): String {
        return ArrayList(sorts.values).stream().map { s ->
            // Official servers send position 126 for spells without shortcuts
            var pos: Int = Optional.ofNullable(sortsPlaces[s.spellID]).orElse(126)
            listOf(
                s.spellID.toString(),
                s.level.toString(),
                Integer.toHexString(pos)
            ).joinToString("~")
        }.collect(Collectors.joining(";"))
    }

    fun setSpellShortcuts(spellId: Int, position: Int) {
        removeSpellShortcutAtPosition(position)
        sortsPlaces.remove(spellId)
        if(position <= 30) sortsPlaces.put(spellId, position)
        DatabaseManager.get(PlayerData::class.java).update(this)
    }

    fun removeSpellShortcutAtPosition(position: Int) {
        sorts.keys.stream()
            .map(sortsPlaces::get)
            .filter(Objects::nonNull)
            .filter({ p -> p == position })
            .forEach(sortsPlaces::remove)
    }

    fun getSortStatBySortIfHas(spellID: Int): Spell.SortStats {
        return sorts[spellID]!!
    }

    fun parseALK(): String {
        var perso: StringBuilder = StringBuilder()
        perso.append("|")
        perso.append(this.id).append(";")
        perso.append(this.name).append(";")
        perso.append(this.level).append(";")
        var gfx: Int = this.gfxId
        if (this.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF) != null)
            if (this.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF)!!.template!!.id == 10681)
                gfx = 8037
        perso.append(gfx).append(";")
        var color1: Int = this.color1
        var color2: Int = this.color2
        var color3: Int = this.color3
        if (this.getObjetByPos(Constant.ITEM_POS_MALEDICTION) != null)
            if (this.getObjetByPos(Constant.ITEM_POS_MALEDICTION)!!.template!!.id == 10838) {
                color1 = 16342021
                color2 = 16342021
                color3 = 16342021
            }
        perso.append((if (color1 != -1) Integer.toHexString(color1) else "-1")).append(";")
        perso.append((if (color2 != -1) Integer.toHexString(color2) else "-1")).append(";")
        perso.append((if (color3 != -1) Integer.toHexString(color3) else "-1")).append(";")
        perso.append(getGMStuffString()).append(";")
        perso.append((if (this.seeSeller) 1 else 0)).append(";")
        perso.append(Config.gameServerId).append(";")

        if (this.dead.toInt() == 1 && Config.modeHeroic) {
            perso.append(this.dead).append(";").append(this.deathCount)
        } else {
            perso.append(0)
        }
        return perso.toString()
    }

    fun remove() {
        DatabaseManager.get(PlayerData::class.java).delete(this)
    }

    fun OnJoinGame() {
        getAccount()!!.currentPlayer = this
        this.online = true

        if (getAccount()!!.gameClient == null)
            return

        var client: GameClient = getAccount()!!.gameClient!!

        if(Config.modeHeroic) {
            this.alignment = 0
            var p: Optional<Player> = ArrayList(World.world.onlinePlayers).stream().filter { p1 -> p1 != null && p1.alignment > 0
                    && p1.getAccount() != null && p1.getAccount()!!.currentIp.equals(getAccount()!!.currentIp, ignoreCase = true) }
                    .findFirst()
            p.ifPresent { player ->
                this.alignment = player.alignment
                if(this.alignment == Constant.ALIGNEMENT_BONTARIEN) Main.angels++
                else if(player.alignment == Constant.ALIGNEMENT_BRAKMARIEN) Main.demons++
            }

            if(this.alignment <= 0) {
                if (Main.angels > Main.demons) {
                    this.alignment = Constant.ALIGNEMENT_BRAKMARIEN
                    Main.demons++
                } else {
                    this.alignment = Constant.ALIGNEMENT_BONTARIEN
                    Main.angels++
                }
            }
            this.showWings = true
            SocketManager.GAME_SEND_ZC_PACKET(this, this.alignment)
        }
        if (this.seeSeller) {
            this.seeSeller = false
            World.world.removeSeller(this.id, this.curMap.id)
            SocketManager.GAME_SEND_ALTER_GM_PACKET(this.curMap, this)
        }

        if (this.mount != null)
            SocketManager.GAME_SEND_Re_PACKET(this, "+", this.mount!!)

        SocketManager.GAME_SEND_Rx_PACKET(this)
        SocketManager.GAME_SEND_ASK(client, this)

        for (a in 1 until World.world.itemSetNumber)
            if (this.getNumbEquipedItemOfPanoplie(a) != 0)
                SocketManager.GAME_SEND_OS_PACKET(this, a)

        if (this.metiers.size > 0) {
            var list: ArrayList<JobStat> = ArrayList()
            list.addAll(this.metiers.values)
            //packet JS
            SocketManager.GAME_SEND_JS_PACKET(this, list)
            //packet JX
            SocketManager.GAME_SEND_JX_PACKET(this, list)
            //Packet JO (Job Option)
            SocketManager.GAME_SEND_JO_PACKET(this, list)
            var obj: GameObject? = getObjetByPos(Constant.ITEM_POS_ARME)
            if (obj != null)
                for (sm in  list)
                    if (sm.template.isValidTool(obj.template!!.id))
                        SocketManager.GAME_SEND_OT_PACKET(getAccount()!!.gameClient!!, sm.template.id)
        }

        SocketManager.GAME_SEND_ALIGNEMENT(client, alignment)
        SocketManager.GAME_SEND_ADD_CANAL(client, canaux + "^" + (if (this.getGroup() != null) "@" else ""))
        if (guildMember != null)
            SocketManager.GAME_SEND_gS_PACKET(this, guildMember!!)
        SocketManager.GAME_SEND_ZONE_ALLIGN_STATUT(client)
        sendItemShortcuts()
        SocketManager.GAME_SEND_EMOTE_LIST(this, getCompiledEmote(this.emotes))
        SocketManager.GAME_SEND_RESTRICTIONS(client)
        SocketManager.GAME_SEND_Ow_PACKET(this)
        SocketManager.GAME_SEND_SEE_FRIEND_CONNEXION(client, showFriendConnection)
        SocketManager.GAME_SEND_SPELL_LIST(this)
        getAccount()!!.sendOnline()

        //Messages de bienvenue
        SocketManager.GAME_SEND_Im_PACKET(this, "189")
        if (getAccount()!!.lastConnectionDate != null && !getAccount()!!.lastConnectionDate.equals("") && !getAccount()!!.lastIP.equals(""))
            SocketManager.GAME_SEND_Im_PACKET(this, "0152;" + getAccount()!!.lastConnectionDate + "~" + getAccount()!!.lastIP)

        SocketManager.GAME_SEND_Im_PACKET(this, "0153;" + getAccount()!!.currentIp)

        getAccount()!!.lastIP = getAccount()!!.currentIp

        //Mise a jour du lastConnectionDate
        var actDate: Date = Date()
        var dateFormat: DateFormat = SimpleDateFormat("dd")
        var jour: String = dateFormat.format(actDate)
        dateFormat = SimpleDateFormat("MM")
        var mois: String = dateFormat.format(actDate)
        dateFormat = SimpleDateFormat("yyyy")
        var annee: String = dateFormat.format(actDate)
        dateFormat = SimpleDateFormat("HH")
        var heure: String = dateFormat.format(actDate)
        dateFormat = SimpleDateFormat("mm")
        var min: String = dateFormat.format(actDate)
        getAccount()!!.lastConnectionDate = annee + "~" + mois + "~" + jour + "~" + heure + "~" + min
        if (guildMember != null)
            guildMember!!.lastCo = annee + "~" + mois + "~" + jour + "~" + heure + "~" + min
        //Affichage des prismes
        World.world.showPrismes(this)
        //Actualisation dans la DB
        DatabaseManager.get(AccountData::class.java).updateLastConnection(getAccount()!!)
        SocketManager.GAME_SEND_MESSAGE(this, if (Config.startMessage.isNullOrEmpty()) this.getLang().trans("client.player.onjoingame.startmessage") else Config.startMessage!!)
        for (`object` in  this.objects.values) {
            if (`object`.template!!.type == Constant.ITEM_TYPE_FAMILIER) {
                var p: PetEntry? = World.world.getPetsEntry(`object`.guid)
                var pets: Pet? = World.world.getPets(`object`.template!!.id)

                if (p == null || pets == null) {
                    if (p != null && p.pdv > 0)
                        SocketManager.GAME_SEND_Im_PACKET(this, "025")
                    continue
                }
                if (pets.type == 0 || pets.type == 1)
                    continue
                p.updatePets(this, (pets.gap.split(",")[1]).toInt())
            }
        }

        if (morphMode)
            setFullMorph(morphId, true, true)

        if (Config.autoReboot)
            this.send(Reboot.toStr())
        if(Main.fightAsBlocked)
            this.sendServerMessage("You can't fight until new order.")
        var manager: EventManager = EventManager.instance
        if(manager.getCurrentEvent() != null && manager.state == EventManager.State.PROCESSED)
            this.sendMessage(this.getLang().trans("client.player.event.start.join", manager.getCurrentEvent()!!.getEventName()))

        World.world.logger.info("The player " + this.name + " come to connect.")

        if(this.morphMode)
            this.send("AR3K")
        if (this.energy == 0)
            this.setGhost()
        if (this.fight != null) SocketManager.send(this, "ILF0")
        else SocketManager.send(this, "ILS2000")
    }

    fun SetSeeFriendOnline(bool: Boolean) {
        showFriendConnection = bool
    }

    fun sendGameCreate() {
        this.online = true
        getAccount()!!.currentPlayer = this

        if (getAccount()!!.gameClient == null)
            return

        var client: GameClient = getAccount()!!.gameClient!!
        SocketManager.GAME_SEND_GAME_CREATE(client, this.name)
        SocketManager.GAME_SEND_STATS_PACKET(this)
        DatabaseManager.get(PlayerData::class.java).updateLogged(this.id, 1)
        this.verifEquiped()

        if (this.lastFight == null) {
            SocketManager.GAME_SEND_MAPDATA(client, this.curMap.id, this.curMap.date, this.curMap.key)
            SocketManager.GAME_SEND_MAP_FIGHT_COUNT(client, this.curMap)
            if (this.fight == null) this.curMap.addPlayer(this)
        } else {
            try {
                client.parsePacket("GI")
            } catch (e: InterruptedException) {
                log.error("unexpected error", e)
            }
        }
    }

    fun parseToOa(): String {
        return "Oa" + this.id + "|" + getGMStuffString()
    }

    fun parseToGM(): String {
        var str: StringBuilder = StringBuilder()
        if (fight == null && curCell != null)// Hors combat
        {
            str.append(curCell.getId()).append(";").append(orientation).append(";")
            str.append("0").append(";");//FIXME:?
            str.append(this.id).append(";").append(this.name).append(";").append(this.classe)
            str.append((if (this.currentTitle > 0) ("," + this.currentTitle + ";") else (";")))
            var gfx: Int = gfxId
            if (this.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF) != null)
                if (this.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF)!!.template!!.id == 10681)
                    gfx = 8037
            str.append(gfx).append("^").append(size);//gfxID^size

            if (this.getObjetByPos(Constant.ITEM_POS_PNJ_SUIVEUR) != null) {
                str.append(",").append(Constant.getItemIdByMascotteId(this.getObjetByPos(Constant.ITEM_POS_PNJ_SUIVEUR)!!.template!!.id)).append("^100")
            }

            str.append(";").append(this.sexe).append(";")
            str.append(alignment).append(",")
            str.append("0").append(",");//FIXME:?
            str.append((if (showWings) getGrade() else "0")).append(",")
            str.append(this.level + this.id)
            if (showWings && deshonor > 0) {
                str.append(",1;")
            } else {
                str.append(";")
            }
            var color1: Int = this.color1
            var color2: Int = this.color2
            var color3: Int = this.color3
            if (this.getObjetByPos(Constant.ITEM_POS_MALEDICTION) != null)
                if (this.getObjetByPos(Constant.ITEM_POS_MALEDICTION)!!.template!!.id == 10838) {
                    color1 = 16342021
                    color2 = 16342021
                    color3 = 16342021
                }

            str.append((if (color1 == -1) "-1" else Integer.toHexString(color1))).append(";")
            str.append((if (color2 == -1) "-1" else Integer.toHexString(color2))).append(";")
            str.append((if (color3 == -1) "-1" else Integer.toHexString(color3))).append(";")
            str.append(getGMStuffString()).append(";")
            if (hasEquiped(10054) || hasEquiped(10055) || hasEquiped(10056)
                    || hasEquiped(10058) || hasEquiped(10061)
                    || hasEquiped(10102)) {
                str.append(3).append(";")
                setCurrentTitle(2)
            } else {
                if (currentTitle.toInt() == 2)
                    setCurrentTitle(0)
                var g: Group? = this.getGroup()
                var level: Int = this.level
                if (g != null)
                    if (!g.isPlayer || this.size <= 0) // Si c'est un groupe non joueur ou que l'on est invisible on cache l'aura
                        level = 1
                str.append((if (level > 99) (if (level > 199) 2 else 1) else (0))).append(";")
            }
            str.append(";");//Emote
            str.append(";");//Emote timer
            if (this.guildMember != null
                    && this.guildMember!!.guild.haveTenMembers())
                str.append(this.guildMember!!.guild.name).append(";").append(this.guildMember!!.guild.emblem).append(";")
            else
                str.append(";;")
            if (this.dead.toInt() == 1 && !this.isGhost)
                str.append("-1")
            str.append(speed).append(";");//Restriction
            str.append((if (onMount && mount != null) mount!!.getStringColor(encodeColorsForMount()) else "")).append(";")
            str.append(this.dead).append(";")
        }
        return str.toString()
    }

    fun parseToMerchant(): String {
        var str: StringBuilder = StringBuilder()
        str.append(curCell.getId()).append(";")
        str.append(orientation).append(";")
        str.append("0").append(";")
        str.append(this.id).append(";")
        str.append(this.name).append(";")
        str.append("-5").append(";");//Merchant identifier
        str.append(gfxId).append("^").append(size).append(";")
        var color1: Int = this.color1
        var color2: Int = this.color2
        var color3: Int = this.color3
        if (this.getObjetByPos(Constant.ITEM_POS_MALEDICTION) != null)
            if (this.getObjetByPos(Constant.ITEM_POS_MALEDICTION)!!.template!!.id == 10838) {
                color1 = 16342021
                color2 = 16342021
                color3 = 16342021
            }
        str.append((if (color1 == -1) "-1" else Integer.toHexString(color1))).append(";")
        str.append((if (color2 == -1) "-1" else Integer.toHexString(color2))).append(";")
        str.append((if (color3 == -1) "-1" else Integer.toHexString(color3))).append(";")
        str.append(getGMStuffString()).append(";");//acessories
        str.append((if (guildMember != null) guildMember!!.guild.name else "")).append(";");//guildName
        str.append((if (guildMember != null) guildMember!!.guild.emblem else "")).append(";");//emblem
        str.append("0;");//offlineType
        return str.toString()
    }

    fun getGMStuffString(): String {
        var str: StringBuilder = StringBuilder()

        var `object`: GameObject? = getObjetByPos(Constant.ITEM_POS_ARME)

        if (`object` != null)
            str.append(Integer.toHexString(`object`.getAppearanceTemplateId()))

        str.append(",")

        `object` = getObjetByPos(Constant.ITEM_POS_COIFFE)

        if (`object` != null) {
            `object`.encodeStats()

            var obvi: Int? = `object`.stats.effects[970]
            if (obvi == null) {
                str.append(Integer.toHexString(`object`.getAppearanceTemplateId()))
            } else {
                str.append(Integer.toHexString(obvi)).append("~16~").append(`object`.obvijevanLook)
            }
        }

        str.append(",")

        `object` = getObjetByPos(Constant.ITEM_POS_CAPE)

        if (`object` != null) {
            `object`.encodeStats()

            var obvi: Int? = `object`.stats.effects[970]
            if (obvi == null) {
                str.append(Integer.toHexString(`object`.getAppearanceTemplateId()))
            } else {
                str.append(Integer.toHexString(obvi)).append("~17~").append(`object`.obvijevanLook)
            }
        }

        str.append(",")

        `object` = getObjetByPos(Constant.ITEM_POS_FAMILIER)

        if (`object` != null)
            str.append(Integer.toHexString(`object`.getAppearanceTemplateId()))

        str.append(",")

        `object` = getObjetByPos(Constant.ITEM_POS_BOUCLIER)

        if (`object` != null)
            str.append(Integer.toHexString(`object`.getAppearanceTemplateId()))

        return str.toString()
    }

    fun getAsPacket(): String {
        refreshStats()
        refreshLife(true)
        var ASData: StringBuilder = StringBuilder()
        ASData.append("As").append(xpString(",")).append("|")
        ASData.append(kamas)
        ASData.append("|").append(capital).append("|").append(spellPts).append("|")
        ASData.append(alignment).append("~").append(alignment).append(",").append(aLvl).append(",").append(getGrade()).append(",").append(honor).append(",").append(deshonor).append(",").append((if (showWings) "1" else "0")).append("|")
        var pdv: Int = _curPdv
        var pdvMax: Int = this.maxPdv
        if (fight != null && !fight!!.isFinish()) {
            var f: Fighter = fight!!.getFighterByPerso(this)
            if (f != null) {
                pdv = f.getPdv()
                pdvMax = f.getPdvMax()
            }
        }
        var stats: Stats = this.stats
        var sutffStats: Stats = this.getStuffStats()
        var donStats: Stats = this.getDonsStats()
        var buffStats: Stats = this.getBuffsStats()
        var totalStats: Stats = this.getTotalStats(false)

        ASData.append(pdv).append(",").append(pdvMax).append("|")
        ASData.append(this.energy).append(","+Player.maxEnergy+"|")
        ASData.append(initiative).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_PROS) + sutffStats.getEffect(Constant.STATS_ADD_PROS) + (Math.ceil(totalStats.getEffect(Constant.STATS_ADD_CHAN).toDouble() / 10).toInt()) + buffStats.getEffect(Constant.STATS_ADD_PROS) + (Math.ceil(buffStats.getEffect(Constant.STATS_ADD_CHAN).toDouble() / 10).toInt())).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_PA)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_PA)).append(",").append(donStats.getEffect(Constant.STATS_ADD_PA)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_PA)).append(",").append(totalStats.getEffect(Constant.STATS_ADD_PA)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_PM)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_PM)).append(",").append(donStats.getEffect(Constant.STATS_ADD_PM)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_PM)).append(",").append(totalStats.getEffect(Constant.STATS_ADD_PM)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_FORC)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_FORC)).append(",").append(donStats.getEffect(Constant.STATS_ADD_FORC)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_FORC)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_VITA)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_VITA)).append(",").append(donStats.getEffect(Constant.STATS_ADD_VITA)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_VITA)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_SAGE)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_SAGE)).append(",").append(donStats.getEffect(Constant.STATS_ADD_SAGE)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_SAGE)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_CHAN)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_CHAN)).append(",").append(donStats.getEffect(Constant.STATS_ADD_CHAN)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_CHAN)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_AGIL)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_AGIL)).append(",").append(donStats.getEffect(Constant.STATS_ADD_AGIL)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_AGIL)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_INTE)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_INTE)).append(",").append(donStats.getEffect(Constant.STATS_ADD_INTE)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_INTE)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_PO)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_PO)).append(",").append(donStats.getEffect(Constant.STATS_ADD_PO)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_PO)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_SUMMON_COUNT)).append(",").append(sutffStats.getEffect(Constant.STATS_SUMMON_COUNT)).append(",").append(donStats.getEffect(Constant.STATS_SUMMON_COUNT)).append(",").append(buffStats.getEffect(Constant.STATS_SUMMON_COUNT)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_DOMA)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_DOMA)).append(",").append(donStats.getEffect(Constant.STATS_ADD_DOMA)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_DOMA)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_PDOM)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_PDOM)).append(",").append(donStats.getEffect(Constant.STATS_ADD_PDOM)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_PDOM)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_MAITRISE)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_MAITRISE)).append(",").append(donStats.getEffect(Constant.STATS_ADD_MAITRISE)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_MAITRISE)).append("|");//ASData.append("0,0,0,0|");//Maitrise ?
        ASData.append(stats.getEffect(Constant.STATS_ADD_PERDOM)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_PERDOM)).append(",").append(donStats.getEffect(Constant.STATS_ADD_PERDOM)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_PERDOM)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_SOIN)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_SOIN)).append(",").append(donStats.getEffect(Constant.STATS_ADD_SOIN)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_SOIN)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_TRAP_DOM)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_TRAP_DOM)).append(",").append(donStats.getEffect(Constant.STATS_ADD_TRAP_DOM)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_TRAP_DOM)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_TRAP_PERDOM)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_TRAP_PERDOM)).append(",").append(donStats.getEffect(Constant.STATS_ADD_TRAP_PERDOM)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_TRAP_PERDOM)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_RETDOM)).append(",").append(sutffStats.getEffect(Constant.STATS_RETDOM)).append(",").append(donStats.getEffect(Constant.STATS_RETDOM)).append(",").append(buffStats.getEffect(Constant.STATS_RETDOM)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_CC)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_CC)).append(",").append(donStats.getEffect(Constant.STATS_ADD_CC)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_CC)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_EC)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_EC)).append(",").append(donStats.getEffect(Constant.STATS_ADD_EC)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_EC)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_ADODGE)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_ADODGE)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_ADODGE)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_ADODGE)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_MDODGE)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_MDODGE)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_MDODGE)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_MDODGE)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_R_NEU)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_R_NEU)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_R_NEU)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_R_NEU)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_RP_NEU)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_RP_NEU)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_RP_NEU)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_RP_NEU)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_R_PVP_NEU)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_R_PVP_NEU)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_R_PVP_NEU)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_R_PVP_NEU)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_RP_PVP_NEU)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_RP_PVP_NEU)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_RP_PVP_NEU)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_RP_PVP_NEU)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_R_TER)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_R_TER)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_R_TER)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_R_TER)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_RP_TER)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_RP_TER)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_RP_TER)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_RP_TER)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_R_PVP_TER)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_R_PVP_TER)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_R_PVP_TER)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_R_PVP_TER)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_RP_PVP_TER)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_RP_PVP_TER)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_RP_PVP_TER)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_RP_PVP_TER)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_R_EAU)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_R_EAU)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_R_EAU)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_R_EAU)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_RP_EAU)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_RP_EAU)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_RP_EAU)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_RP_EAU)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_R_PVP_EAU)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_R_PVP_EAU)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_R_PVP_EAU)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_R_PVP_EAU)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_RP_PVP_EAU)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_RP_PVP_EAU)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_RP_PVP_EAU)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_RP_PVP_EAU)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_R_AIR)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_R_AIR)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_R_AIR)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_R_AIR)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_RP_AIR)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_RP_AIR)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_RP_AIR)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_RP_AIR)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_R_PVP_AIR)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_R_PVP_AIR)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_R_PVP_AIR)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_R_PVP_AIR)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_RP_PVP_AIR)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_RP_PVP_AIR)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_RP_PVP_AIR)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_RP_PVP_AIR)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_R_FEU)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_R_FEU)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_R_FEU)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_R_FEU)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_RP_FEU)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_RP_FEU)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_RP_FEU)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_RP_FEU)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_R_PVP_FEU)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_R_PVP_FEU)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_R_PVP_FEU)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_R_PVP_FEU)).append("|")
        ASData.append(stats.getEffect(Constant.STATS_ADD_RP_PVP_FEU)).append(",").append(sutffStats.getEffect(Constant.STATS_ADD_RP_PVP_FEU)).append(",").append(0).append(",").append(buffStats.getEffect(Constant.STATS_ADD_RP_PVP_FEU)).append(",").append(buffStats.getEffect(Constant.STATS_ADD_RP_PVP_FEU)).append("|")
        return ASData.toString()
    }

    fun getGrade(): Int {
        if (alignment == Constant.ALIGNEMENT_NEUTRE)
            return 0
        if (honor >= 17500)
            return 10
        return World.world.experiences!!.pvp.levelForXp(honor.toLong())
    }

    fun xpString(c: String): String {
        if (!morphMode) {
            var xpTable: ExperienceTables.ExperienceTable = World.world.experiences!!.players
            return this.exp.toString() + c + xpTable.minXpAt(this.level) + c + xpTable.maxXpAt(this.level)
        }
        if(this.getObjetByPos(Constant.ITEM_POS_ARME) == null
                || !Constant.isIncarnationWeapon(this.getObjetByPos(Constant.ITEM_POS_ARME)!!.template!!.id)
                || this.getObjetByPos(Constant.ITEM_POS_ARME)!!.soulStat[Constant.ERR_STATS_XP]!! == null) {
            return "1" + c + "1" + c + "1"
        }

        // What if it's a tormentator ?
        var xpTable: ExperienceTables.ExperienceTable = World.world.experiences!!.bandits
        var level: Int = this.getObjetByPos(Constant.ITEM_POS_ARME)!!.soulStat[Constant.STATS_NIVEAU]!!
        return this.getObjetByPos(Constant.ITEM_POS_ARME)!!.soulStat[Constant.ERR_STATS_XP]!!.toString() + c + xpTable.minXpAt(level) + c + xpTable.maxXpAt(level)
    }

    fun emoteActive(): Int {
        return emoteActive
    }


    fun getStuffStats(): Stats {
        if (this.useStats) return Stats()

        var stats: Stats = Stats(false, null)
        var itemSetApplied: ArrayList<Int> = ArrayList()
        synchronized(objects) {
            for (gameObject in  ArrayList(this.objects.values)) {
                var position: Byte = (gameObject.position.toByte())
                if (position.toInt() != Constant.ITEM_POS_NO_EQUIPED) {
                    if (position >= 35 && position <= 48)
                        continue

                    stats = Stats.cumulStat(stats, gameObject.stats)
                    var id: Int = gameObject.template!!.panoId

                    if (id > 0 && !itemSetApplied.contains(id)) {
                        itemSetApplied.add(id)
                        var objectSet: ObjectSet? = World.world.getItemSet(id)
                        if (objectSet != null)
                            stats = Stats.cumulStat(stats, objectSet.getBonusStatByItemNumb(this.getNumbEquipedItemOfPanoplie(id)))
                    }
                }
            }
        }

        if (this.mount != null && this.onMount)
            stats = Stats.cumulStat(stats, this.mount!!.stats)

        return stats
    }

    fun getBuffsStats(): Stats {
        var stats: Stats = Stats(false, null)
        if (this.fight != null)
            if (this.fight!!.getFighterByPerso(this) != null)
                for (entry in  this.fight!!.getFighterByPerso(this).getFightBuff())
                    stats.addOneStat(entry.effectID, entry.value)

        for (entry in buffs.entries)
            stats.addOneStat(entry.value.effectID, entry.value.value)
        return stats
    }





    fun getTotalStats(lessBuff: Boolean): Stats {
        var total: Stats = Stats(false, null)
        if (!useStats) {
            total = Stats.cumulStat(total, this.stats)
            total = Stats.cumulStat(total, this.getStuffStats())
            total = Stats.cumulStat(total, this.getDonsStats())
            if (fight != null && !lessBuff)
                total = Stats.cumulStat(total, this.getBuffsStats())
        } else {
            return newStatsMorph()
        }
        return total
    }

    fun getDonsStats(): Stats {
        var stats: Stats = Stats(false, null)
        return stats
    }

    fun newStatsMorph(): Stats {
        var stats: Stats = Stats()
        stats.addOneStat(Constant.STATS_ADD_PA, this.pa)
        stats.addOneStat(Constant.STATS_ADD_PM, this.pm)
        stats.addOneStat(Constant.STATS_ADD_VITA, this.vitalite)
        stats.addOneStat(Constant.STATS_ADD_SAGE, this.sagesse)
        stats.addOneStat(Constant.STATS_ADD_FORC, this.terre)
        stats.addOneStat(Constant.STATS_ADD_INTE, this.feu)
        stats.addOneStat(Constant.STATS_ADD_CHAN, this.eau)
        stats.addOneStat(Constant.STATS_ADD_AGIL, this.air)
        stats.addOneStat(Constant.STATS_ADD_INIT, this.initiative)
        stats.addOneStat(Constant.STATS_ADD_PROS, 100)
        stats.addOneStat(Constant.STATS_SUMMON_COUNT, 1)
        this.useCac = false
        return stats
    }

    fun getPodUsed(): Int {
        var pod: Int = 0

        for (entry in objects.entries) {
            if(entry.value != null)
                pod += entry.value.template!!.pod * entry.value.quantity
        }

        pod += parseStoreItemsListPods()
        return pod
    }

    fun getMaxPod(): Int {
        var total: Stats = Stats(false, null)
        total = Stats.cumulStat(total, this.stats)
        total = Stats.cumulStat(total, this.getStuffStats())
        total = Stats.cumulStat(total, this.getDonsStats())
        var pods: Int = total.getEffect(Constant.STATS_ADD_PODS)
        pods += total.getEffect(Constant.STATS_ADD_FORC) * 5
        for (SM in  metiers.values) {
            pods += SM.get_lvl() * 5
            if (SM.get_lvl() == 100)
                pods += 1000
        }
        if (pods < 1000)
            pods = 1000
        return pods + 9000
    }

    fun refreshLife(refresh: Boolean) {
        var time: Long = (System.currentTimeMillis() - regenTime)
        regenTime = System.currentTimeMillis()
        if (fight != null)
            return
        if (regenRate == 0)
            return
        if (_curPdv > this.maxPdv) {
            _curPdv = this.maxPdv - 1
            if (!refresh)
                SocketManager.GAME_SEND_STATS_PACKET(this)
            return
        }

        var diff: Int = (time.toInt()) / regenRate
        if (diff >= 1 && _curPdv < this.maxPdv && refresh) {
            SocketManager.send(this, "ILF" + diff)
            SocketManager.send(this, "ILS" + regenRate)
        }

        setPdv(_curPdv + diff)
    }



    fun get_pdvper(): Int {
        refreshLife(false)
        var pdvper: Int = 100
        pdvper = (100 * _curPdv) / this.maxPdv
        if (pdvper > 100)
            return 100
        return pdvper
    }

    fun useSmiley(str: String) {
        try {
            var id: Int = (str).toInt()
            var map: GameMap = curMap
            if (fight == null)
                SocketManager.GAME_SEND_EMOTICONE_TO_MAP(map, this.id, id)
            else
                SocketManager.GAME_SEND_EMOTICONE_TO_FIGHT(fight!!, 7, this.id, id)
        } catch (e: NumberFormatException) {
            log.error("unexpected error", e)
        }
    }

    fun boostStat(stat: Int, capital: Boolean) {
        var value: Int = 0
        when (stat){  10 -> {value = this.stats.getEffect(Constant.STATS_ADD_FORC)
                
}
13 -> {value = this.stats.getEffect(Constant.STATS_ADD_CHAN)
                
}
14 -> {value = this.stats.getEffect(Constant.STATS_ADD_AGIL)
                
}
15 -> {value = this.stats.getEffect(Constant.STATS_ADD_INTE)
                
}
}
        var cout: Int = Constant.getReqPtsToBoostStatsByClass(this.classe, stat, value)
        if (!capital)
            cout = 0
        if (cout <= this.capital) {
            when (stat){  11 -> {if (this.classe != Constant.CLASS_SACRIEUR)
                        this.stats.addOneStat(Constant.STATS_ADD_VITA, 1)
                    else
                        this.stats.addOneStat(Constant.STATS_ADD_VITA,if (capital) 2 else 1)
                    
}
12 -> {this.stats.addOneStat(Constant.STATS_ADD_SAGE, 1)
                    
}
10 -> {this.stats.addOneStat(Constant.STATS_ADD_FORC, 1)
                    
}
13 -> {this.stats.addOneStat(Constant.STATS_ADD_CHAN, 1)
                    
}
14 -> {this.stats.addOneStat(Constant.STATS_ADD_AGIL, 1)
                    
}
15 -> {this.stats.addOneStat(Constant.STATS_ADD_INTE, 1)
                    
}
else -> {return
            
}
}
            this.capital = this.capital - cout
            SocketManager.GAME_SEND_STATS_PACKET(this)
            DatabaseManager.get(PlayerData::class.java).update(this)
        }
    }

    fun boostStatFixedCount(stat: Int, countVal: Int) {
        for (i in 0 until countVal) {
            var value: Int = 0
            when (stat){  10 -> {value = this.stats.getEffect(Constant.STATS_ADD_FORC)
                    
}
13 -> {value = this.stats.getEffect(Constant.STATS_ADD_CHAN)
                    
}
14 -> {value = this.stats.getEffect(Constant.STATS_ADD_AGIL)
                    
}
15 -> {value = this.stats.getEffect(Constant.STATS_ADD_INTE)
                    
}
}
            var cout: Int = Constant.getReqPtsToBoostStatsByClass(this.classe, stat, value)
            if (cout <= capital) {
                when (stat){  11 -> {if (this.classe != Constant.CLASS_SACRIEUR)
                            this.stats.addOneStat(Constant.STATS_ADD_VITA, 1)
                        else
                            this.stats.addOneStat(Constant.STATS_ADD_VITA, 2)
                        
}
12 -> {this.stats.addOneStat(Constant.STATS_ADD_SAGE, 1)
                        
}
10 -> {this.stats.addOneStat(Constant.STATS_ADD_FORC, 1)
                        
}
13 -> {this.stats.addOneStat(Constant.STATS_ADD_CHAN, 1)
                        
}
14 -> {this.stats.addOneStat(Constant.STATS_ADD_AGIL, 1)
                        
}
15 -> {this.stats.addOneStat(Constant.STATS_ADD_INTE, 1)
                        
}
else -> {return
                
}
}
                this.capital = this.capital - cout
            }
        }
        SocketManager.GAME_SEND_STATS_PACKET(this)
        DatabaseManager.get(PlayerData::class.java).update(this)
    }

    fun isMuted(): Boolean {
        return getAccount()!!.isMuted()
    }

    fun parseObjetsToDB(): String {
        var str: StringBuilder = StringBuilder()
        if (objects.isEmpty())
            return ""
        for (entry in objects.entries) {
            var obj: GameObject = entry.value
            if (obj == null)
                continue
            str.append(obj.guid).append("|")
        }

        return str.toString()
    }

    fun addItem(templateId: Int, quantity: Int, useMax: Boolean, display: Boolean) {
        this.addItem(World.world.getObjTemplate(templateId)!!, quantity, useMax, display)
    }

    fun addItem(template: ObjectTemplate, quantity: Int, useMax: Boolean, display: Boolean) {
        var item: GameObject = template.createNewItem(quantity, useMax)!!
        if (this.addItem(item, true, display)) {
            World.world.addGameObject(item)
        }
    }

    fun addItem(newItem: GameObject, stack: Boolean, display: Boolean): Boolean {
        synchronized (objects) {
            for (item in  objects.values) {
                if (World.world.conditionManager.stackIfSimilar(item, newItem, stack)) {
                    item.quantity = item.quantity + newItem.quantity
                    if (isOnline) {
                        SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, item)
                        SocketManager.GAME_SEND_Ow_PACKET(this)
                        if(display) {
                            SocketManager.GAME_SEND_Im_PACKET(this, "021;" + newItem.quantity + "~" + newItem.template)
                        }
                    }
                    return false
                }
            }
            addItem(newItem, display)
        }
        return true
    }

    fun addItem(item: GameObject, display: Boolean) {
        this.objects[item.guid] = item
        if(isOnline) {
            SocketManager.GAME_SEND_OAKO_PACKET(this, item)
            if(display) {
                SocketManager.GAME_SEND_Im_PACKET(this, "021;" + item.quantity + "~" + item.template)
            }
        }
    }

    fun addObjetSimiler(objet: GameObject, hasSimiler: Boolean, oldID: Int): Boolean {
        var objModelo: ObjectTemplate = objet.template!!
        if (hasSimiler) {
            for (entry in objects.entries) {
                var obj: GameObject = entry.value
                if (obj.position == -1 && obj.guid != oldID
                        && obj.template!!.id == objModelo.id
                        && obj.stats.isSameStats(objet.stats)
                        && World.world.conditionManager.stackIfSimilar(obj, objet, hasSimiler)) {
                    obj.quantity = obj.quantity + objet.quantity
                    SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, obj)
                    return true
                }
            }
        }
        return false
    }


    fun encodeItemASK(): String {
        var str: StringBuilder = StringBuilder()
        if (objects.isEmpty())
            return ""
        for (obj in  objects.values) {
            str.append(obj.encodeItem())
        }
        return str.toString()
    }

    fun getItemsIDSplitByChar(splitter: String): String {
        var str: StringBuilder = StringBuilder()
        if (objects.isEmpty())
            return ""
        for (entry in objects.keys) {
            if (str.length != 0)
                str.append(splitter)
            str.append(entry)
        }

        return str.toString()
    }

    fun getStoreItemsIDSplitByChar(splitter: String): String {
        var str: StringBuilder = StringBuilder()
        if (storeItems.isEmpty())
            return ""
        for (entry in storeItems.keys) {
            if (str.length != 0)
                str.append(splitter)
            str.append(entry)
        }
        return str.toString()
    }

    fun hasItemGuid(guid: Int): Boolean {
        return objects[guid] != null && objects[guid]!!.quantity > 0
    }

    fun sellItem(guid: Int, qua: Int) {
        var qua = qua
        if (qua <= 0)
            return

        var `object`: GameObject = objects[guid]!!
        if (`object`.quantity < qua)//Si il a moins d'item que ce qu'on veut Del
            qua = `object`.quantity

        var price: Int = qua * (`object`.template!!.price / 10)//Calcul du prix de vente (prix d'achat/10)
        var newQua: Int = `object`.quantity - qua

        if (newQua <= 0) {
            DatabaseManager.get(ObjectData::class.java).delete(`object`)
            objects.remove(guid)
            World.world.removeGameObject(guid)
            SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, guid)
        } else {
            objects[guid]!!.quantity = newQua
            SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, objects[guid]!!)
        }

        kamas = kamas + price
        SocketManager.GAME_SEND_STATS_PACKET(this)
        SocketManager.GAME_SEND_Ow_PACKET(this)
        SocketManager.GAME_SEND_ESK_PACKEt(this)
    }

    fun removeItem(guid: Int) {
        synchronized(objects) {
            objects.remove(guid)
        }
    }

    fun removeItem(guid: Int, nombre: Int, send: Boolean, deleteFromWorld: Boolean) {
        var nombre = nombre
        lateinit var obj: GameObject
        synchronized(objects) {
            obj = objects[guid]!!
        }

        if(obj == null) return

        if (nombre > obj.quantity)
            nombre = obj.quantity

        if (obj.quantity >= nombre) {
            var newQua: Int = obj.quantity - nombre
            if (newQua > 0) {
                obj.quantity = newQua
                if (send && isOnline)
                    SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, obj)
            } else {
                //on supprime de l'inventaire et du Monde
                synchronized(objects) {
                    objects.remove(obj.guid)
                }
                if (deleteFromWorld)
                    World.world.removeGameObject(obj.guid)
                //on envoie le packet si connect�
                if (send && isOnline)
                    SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, obj.guid)
            }
        }

        SocketManager.GAME_SEND_Ow_PACKET(this)
    }

    fun deleteItem(guid: Int) {
        synchronized(objects) {
            objects.remove(guid)
        }
        World.world.removeGameObject(guid)
    }

    fun getObjetByPos(pos: Int): GameObject? {
        if (pos == Constant.ITEM_POS_NO_EQUIPED)
            return null
        synchronized(objects) {
            for (gameObject in  this.objects.values) {
                if (gameObject.position == pos && pos == Constant.ITEM_POS_FAMILIER) {
                    if (gameObject.txtStat.isEmpty()) return null
                    else if (World.world.getPetsEntry(gameObject.guid) == null) return null
                }
                if (gameObject.position == pos) return gameObject
            }
        }

        return null
    }

    //TODO: Delete s'te fonction.
    fun getObjetByPos2(pos: Int): GameObject? {
        if (pos == Constant.ITEM_POS_NO_EQUIPED)
            return null

        for (entry in objects.entries) {
            var obj: GameObject = entry.value

            if (obj.position == pos)
                return obj
        }
        return null
    }

    fun refreshStats() {
        var actPdvPer: Double = (100 * (_curPdv.toDouble())) / (this.maxPdv.toDouble())
        if (!useStats)
            _maxPdv = (this.level - 1) * 5 + 50 + getTotalStats(false).getEffect(Constant.STATS_ADD_VITA)
        _curPdv = (Math.round(maxPdv * actPdvPer / 100).toInt())
    }

    fun levelUp(send: Boolean, addXp: Boolean): Boolean {
        var xpTable: ExperienceTables.ExperienceTable = World.world.experiences!!.players
        if (this.level == xpTable.maxLevel())
            return false
        this.level++
        capital += 5
        spellPts++
        _maxPdv += 5
        this.setPdv(this.maxPdv)
        if (this.level == 100)
            this.stats.addOneStat(Constant.STATS_ADD_PA, 1)
        Constant.onLevelUpSpells(this, this.level)
        if (addXp)
            this.exp =  xpTable.minXpAt(this.level)
        if (send && isOnline) {
            SocketManager.GAME_SEND_STATS_PACKET(this)
            SocketManager.GAME_SEND_SPELL_LIST(this)
        }
        return true
    }

    fun addXp(winxp: Long): Boolean {
        var xpTable: ExperienceTables.ExperienceTable = World.world.experiences!!.players

        var up: Boolean = false
        this.exp += winxp
        while (this.exp >= xpTable.maxXpAt(this.level) && this.level < xpTable.maxLevel())
            up = levelUp(true, false)
        if (isOnline) {
            if (up)
                SocketManager.GAME_SEND_NEW_LVL_PACKET(getAccount()!!.gameClient!!, this.level)
            SocketManager.GAME_SEND_STATS_PACKET(this)
        }
        return up
    }

    fun levelUpIncarnations(send: Boolean, addXp: Boolean): Boolean {
        var level: Int = this.getObjetByPos(Constant.ITEM_POS_ARME)!!.soulStat[Constant.STATS_NIVEAU]!!

        if (level == 50)
            return false

        level++
        this.setPdv(this.maxPdv)
        SocketManager.GAME_SEND_STATS_PACKET(this)

        when (level) {
10, 20, 30, 40, 50 -> {boostSpellIncarnation()
                
}
}

        if (send && isOnline) {
            SocketManager.GAME_SEND_STATS_PACKET(this)
            SocketManager.GAME_SEND_SPELL_LIST(this)
        }

        this.getObjetByPos(Constant.ITEM_POS_ARME)!!.soulStat.clear()
        this.getObjetByPos(Constant.ITEM_POS_ARME)!!.soulStat.put(Constant.STATS_NIVEAU, level)
        this.getObjetByPos(Constant.ITEM_POS_ARME)
        SocketManager.GAME_SEND_UPDATE_OBJECT_DISPLAY_PACKET(this, this!!.getObjetByPos(Constant.ITEM_POS_ARME)!!)
        return true
    }

    fun addXpIncarnations(winxp: Long): Boolean {
        var up: Boolean = false
        var level: Int = this.getObjetByPos(Constant.ITEM_POS_ARME)!!.soulStat[Constant.STATS_NIVEAU]!!
        var exp: Long = this.getObjetByPos(Constant.ITEM_POS_ARME)!!.soulStat[Constant.ERR_STATS_XP]!!.toLong()
        exp += winxp

        if (Constant.isBanditsWeapon(this.getObjetByPos(Constant.ITEM_POS_ARME)!!.template!!.id)) {
            var xpTable: ExperienceTables.ExperienceTable = World.world.experiences!!.bandits

            while (exp >= xpTable.maxXpAt(level) && level < xpTable.maxLevel()) {
                up = levelUpIncarnations(true, false)
                level = this.getObjetByPos(Constant.ITEM_POS_ARME)!!.soulStat[Constant.STATS_NIVEAU]!!
            }
        } else if (Constant.isTourmenteurWeapon(this.getObjetByPos(Constant.ITEM_POS_ARME)!!.template!!.id)) {
            var xpTable: ExperienceTables.ExperienceTable = World.world.experiences!!.tormentators

            while (exp >= xpTable.maxXpAt(level) && level < xpTable.maxLevel()) {
                up = levelUpIncarnations(true, false)
                level = this.getObjetByPos(Constant.ITEM_POS_ARME)!!.soulStat[Constant.STATS_NIVEAU]!!
            }
        }
        if (isOnline)
            SocketManager.GAME_SEND_STATS_PACKET(this)
        level = this.getObjetByPos(Constant.ITEM_POS_ARME)!!.soulStat[Constant.STATS_NIVEAU]!!
        this.getObjetByPos(Constant.ITEM_POS_ARME)!!.soulStat.clear()
        this.getObjetByPos(Constant.ITEM_POS_ARME)!!.soulStat.put(Constant.STATS_NIVEAU, level)
        this.getObjetByPos(Constant.ITEM_POS_ARME)!!.soulStat.put(Constant.ERR_STATS_XP, (exp.toInt()))
        return up
    }

    fun addKamas(l: Long): Boolean {
        // Make sure the player has enough
        if(l < 0 && kamas < -l) return false
        kamas += l
        return true
    }

    fun modKamasDisplay(quantity: Long): Boolean {
        if(!addKamas(quantity)) {
            if(isOnline)SocketManager.GAME_SEND_Im_PACKET(this, "182")
            return false
        }
        if(!isOnline)return true
        if(quantity < 0) {
            SocketManager.GAME_SEND_Im_PACKET(this, "046;" + (-quantity))
        } else {
            SocketManager.GAME_SEND_Im_PACKET(this, "045;" + quantity)
        }
        SocketManager.GAME_SEND_STATS_PACKET(this)
        return true
    }

    fun getSimilarItem(exGameObject: GameObject): GameObject? {
        if (exGameObject.template!!.id == 8378)
            return null
        synchronized(objects) {
            for (gameObject in  this.objects.values)
                if (gameObject.template!!.id == exGameObject.template!!.id
                        && World.world.conditionManager.stackIfSimilar(gameObject, exGameObject, true)
                        && gameObject.stats.isSameStats(exGameObject.stats) && gameObject.guid != exGameObject.guid
                        && !Constant.isIncarnationWeapon(exGameObject.template!!.id)
                        && exGameObject.template!!.type != Constant.ITEM_TYPE_CERTIFICAT_CHANIL
                        && exGameObject.template!!.type != Constant.ITEM_TYPE_PIERRE_AME_PLEINE
                        && gameObject.template!!.type != Constant.ITEM_TYPE_OBJET_ELEVAGE
                        && gameObject.template!!.type != Constant.ITEM_TYPE_CERTIF_MONTURE
                        && (exGameObject.template!!.type != Constant.ITEM_TYPE_QUETES || Constant.isFlacGelee(gameObject.template!!.id))
                        && !Constant.isCertificatDopeuls(gameObject.template!!.id) &&
                        gameObject.template!!.type != Constant.ITEM_TYPE_FAMILIER &&
                        gameObject.template!!.type != Constant.ITEM_TYPE_OBJET_VIVANT && gameObject.position == Constant.ITEM_POS_NO_EQUIPED)
                    return gameObject
        }

        return null
    }

    fun learnJob(m: Job): Int {
        for (entry in metiers.entries) {
            if (entry.value.template!!.id == m.id)//Si le joueur a d�j� le m�tier
                return -1
        }
        var Msize: Int = metiers.size
        if (Msize == 6)//Si le joueur a d�j� 6 m�tiers
            return -1
        var pos: Int = 0
        if (JobConstant.isMageJob(m.id)) {
            if (metiers[5] == null)
                pos = 5
            if (metiers[4] == null)
                pos = 4
            if (metiers[3] == null)
                pos = 3
        } else {
            if (metiers[2] == null)
                pos = 2
            if (metiers[1] == null)
                pos = 1
            if (metiers[0] == null)
                pos = 0
        }

        var sm: JobStat = JobStat(pos, m, 1, 0)
        metiers.put(pos, sm);//On apprend le m�tier lvl 1 avec 0 xp
        if (isOnline) {
            //on cr�er la listes des JobStats a envoyer (Seulement celle ci)
            var list: ArrayList<JobStat> = ArrayList()
            list.add(sm)

            SocketManager.GAME_SEND_Im_PACKET(this, "02;" + m.id)
            //packet JS
            SocketManager.GAME_SEND_JS_PACKET(this, list)
            //packet JX
            SocketManager.GAME_SEND_JX_PACKET(this, list)
            //Packet JO (Job Option)
            SocketManager.GAME_SEND_JO_PACKET(this, list)

            var obj: GameObject? = getObjetByPos(Constant.ITEM_POS_ARME)
            if (obj != null)
                if (sm.template.isValidTool(obj.template!!.id))
                    SocketManager.GAME_SEND_OT_PACKET(getAccount()!!.gameClient!!, m.id)
        }
        return pos
    }

    fun unlearnJob(jobID: Int): Boolean {
        var key: Optional<Int> = metiers.entries.stream()
            .filter({ e -> e.value.template!!.id == jobID })
            .findFirst()
            .map({ it.key })

        if(!key.isPresent()) return false
        metiers.remove(key.get())

        DatabaseManager.get(PlayerData::class.java).update(this)
        if(isOnline) {
            SocketManager.GAME_SEND_STATS_PACKET(this)
            send("JR" + jobID)
        }
        return true
    }

    fun unequipedObjet(o: GameObject) {
        o.position = Constant.ITEM_POS_NO_EQUIPED
        var oTpl: ObjectTemplate = o.template!!
        var idSetExObj: Int = oTpl.panoId
        if ((idSetExObj >= 81 && idSetExObj <= 92)
                || (idSetExObj >= 201 && idSetExObj <= 212)) {
            var stats: List<String> = oTpl.strTemplate.split(",")
            for (stat in  stats) {
                var v = stat.split("#")
                var modifi: String = v[0].toInt(16).toString() + ";" + v[1].toInt(16) + ";0"
                SocketManager.SEND_SB_SPELL_BOOST(this, modifi)
                this.removeObjectClassSpell(v[1].toInt(16))
            }
            this.removeObjectClass(oTpl.id)
        }
        SocketManager.GAME_SEND_OBJET_MOVE_PACKET(this, o)
        if (oTpl.panoId > 0)
            SocketManager.GAME_SEND_OS_PACKET(this, oTpl.panoId)
    }

    fun verifEquiped() {
        if (this.morphMode)
            return
        var arme: GameObject? = this.getObjetByPos(Constant.ITEM_POS_ARME)
        var bouclier: GameObject? = this.getObjetByPos(Constant.ITEM_POS_BOUCLIER)
        if (arme != null) {
            if (arme.template!!.isTwoHanded && bouclier != null) {
                this.unequipedObjet(arme)
                SocketManager.GAME_SEND_Im_PACKET(this, "119|44")
            } else if (!arme.template!!.conditions.equals("", ignoreCase = true)
                    && !World.world.conditionManager.validConditions(this, arme.template!!.conditions)) {
                this.unequipedObjet(arme)
                SocketManager.GAME_SEND_Im_PACKET(this, "119|44")
            }
        }
        if (bouclier != null) {
            /*if (!bouclier.template.getConditions().equalsIgnoreCase("")
                    && !World.world.getConditionManager().validConditions(this, bouclier.template.getConditions())) {
                this.unequipedObjet(bouclier);
                SocketManager.GAME_SEND_Im_PACKET(this, "119|44");
            }*/
        }
    }

    fun hasEquiped(id: Int): Boolean {
        for (`object` in  objects.values)
            if (`object`.template != null && `object`.template!!.id == id && `object`.position.toInt() != Constant.ITEM_POS_NO_EQUIPED)
                return true
        return false
    }



    fun parseToPM(): String {
        var str: StringBuilder = StringBuilder()
        str.append(this.id).append(";")
        str.append(this.name).append(";")
        str.append(gfxId).append(";")
        var color1: Int = this.color1
        var color2: Int = this.color2
        var color3: Int = this.color3
        if (this.getObjetByPos(Constant.ITEM_POS_MALEDICTION) != null)
            if (this.getObjetByPos(Constant.ITEM_POS_MALEDICTION)!!.template!!.id == 10838) {
                color1 = 16342021
                color2 = 16342021
                color3 = 16342021
            }
        str.append(color1).append(";")
        str.append(color2).append(";")
        str.append(color3).append(";")
        str.append(getGMStuffString()).append(";")
        str.append(_curPdv).append(",").append(this.maxPdv).append(";")
        str.append(this.level).append(";")
        str.append(initiative).append(";")
        str.append(getTotalStats(false).getEffect(Constant.STATS_ADD_PROS) + ((Math.ceil(getTotalStats(false).getEffect(Constant.STATS_ADD_CHAN) / 10.0) as Int))).append(";")
        str.append("0");//Side = ?
        return str.toString()
    }

    fun getNumbEquipedItemOfPanoplie(panID: Int): Int {
        var nb: Int = 0

        for (i in objects.entries) {
            //On ignore les objets non �quip�s
            if (i.value.position == Constant.ITEM_POS_NO_EQUIPED)
                continue
            //On prend que les items de la pano demand�e, puis on augmente le nombre si besoin
            if (i.value.template!!.panoId == panID)
                nb++
        }
        return nb
    }

    fun startActionOnCell(GA: GameAction) {
        var cellID: Int
        var skillID: Int
        try {
            cellID = (GA.args!!.split(";")[0]).toInt()
            skillID = (GA.args!!.split(";")[1]).toInt()
        } catch (e: Exception) {
            log.error("unexpected error", e)
            return
        }
        if (cellID == -1 || skillID == -1)
            return

        var allowed: Boolean = World.world
            .getObjectBySprite(curMap.cellsData.object2(cellID))
            .map({ o -> o.allowSkill(skillID) })
            .orElse(false)

        if(!allowed) {
            // TODO: Cheat attempt
            return
        }

        DataScriptVM.getInstance()!!.handlers.onSkillUse(this, cellID, skillID)
    }

    fun finishActionOnCell(GA: GameAction) {
        var cellID: Int = -1
        try {
            cellID = (GA.args!!.split(";")[0]).toInt()
        } catch (e: Exception) {
            log.error("unexpected error", e)
        }
        if (cellID == -1 || this.curMap == null)
            return
        var cell: GameCase? = this.curMap.getCase(cellID)
        if(cell == null) return

        // TODO: Call Lua to finish gathering skills

        // curMap.finishAction(cell,)
        // cell.finishAction(this, GA);
    }

    fun teleportD(newMapID: Int, newCellID: Int) {
        if (this.fight != null) return
        this.curMap = World.world.getMap(newMapID)
        this.curCell = World.world.getMap(newMapID).getCase(newCellID)!!
        DatabaseManager.get(PlayerData::class.java).update(this)
    }

    fun teleportLaby(newMapID: Short, newCellID: Int) {
        if (this.fight != null) return
        var client: GameClient? = this.getGameClient()
        if (client == null)
            return

        if (World.world.getMap(newMapID.toInt()) == null)
            return

        if (World.world.getMap(newMapID.toInt())!!.getCase(newCellID) == null)
            return

        SocketManager.GAME_SEND_GA2_PACKET(client, this.id)
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.curMap, this.id)

        if (this.mount != null)
            if (this.mount!!.fatigue >= 220)
                this.mount!!.energy = this.mount!!.energy - 1

        if (this.curCell.players.contains(this))
            this.curCell.removePlayer(this)
        this.curMap = World.world.getMap(newMapID.toInt())!!
        this.curCell = this.curMap.getCase(newCellID)!!

        SocketManager.GAME_SEND_MAPDATA(client, newMapID.toInt(), this.curMap.date, this.curMap.key)
        this.curMap.addPlayer(this)

        if (!this.follower.isEmpty())// On met a jour la Map des personnages qui nous suivent
        {
            for (t in  this.follower.values) {
                if (t.isOnline)
                    SocketManager.GAME_SEND_FLAG_PACKET(t, this)
                else
                    this.follower.remove(t.id)
            }
        }
    }

    fun teleport(posIDs: Pair<Int,Int>) {
        teleport(posIDs.first, posIDs.second)
    }

    fun teleport(newMapID: Int, newCellID: Int) {
        teleport(newMapID, newCellID, false)
    }

    fun teleport(newMapID: Int, newCellID: Int, forceGDM: Boolean) {
        var client: GameClient? = this.getGameClient()
        var map: GameMap = World.world.getMap(newMapID)

        if (map == null || this.fight != null)
            return
        if (map.getCase(newCellID) == null)
            return

        if (!forceGDM && client != null && newMapID == this.curMap.id) {
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.curMap, this.id)
            this.curCell.removePlayer(this)
            this.curCell = curMap.getCase(newCellID)!!
            this.curMap.addPlayer(this)
            SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(this.curMap, this)
            return
        }

        this.away = false
        var fullmorph: Boolean = false
        if (Constant.isInMorphDonjon(this.curMap.id))
            if (!Constant.isInMorphDonjon(newMapID))
                fullmorph = true

        if(client != null)
            SocketManager.GAME_SEND_GA2_PACKET(client, this.id)
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.curMap, this.id)

        if (this.mount != null)
            if (this.mount!!.fatigue >= 220)
                this.mount!!.energy = this.mount!!.energy - 1

        if (this.curCell.players.contains(this))
            this.curCell.removePlayer(this)

        this.curMap = map
        this.curCell = this.curMap.getCase(newCellID)!!
        // Verification de la Map
        // Verifier la validit� du mountpark

        if (this.curMap.mountPark != null
                && this.curMap.mountPark!!.owner > 0
                && this.curMap.mountPark!!.guild!!.id != -1) {
            if (World.world.getGuild(this.curMap.mountPark!!.guild!!.id) == null) {// Ne devrait  pas  arriver
                GameServer.a()
                //FIXME : Map.MountPark.removeMountPark(curMap.getMountPark().getGuild().getId());
            }
        }

        var collector: Collector? = Collector.getCollectorByMapId(this.curMap.id)
        if (collector != null && World.world.getGuild(collector.guildId) == null)
            Collector.removeCollector(collector.guildId)

        if (this.isMissingSubscription()) {
            if (!this.isInPrivateArea)
                SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this.getGameClient()!!, 'S')
            this.isInPrivateArea = true
        } else {
            this.isInPrivateArea = false
        }

        if(client != null) {
            SocketManager.GAME_SEND_MAPDATA(client, newMapID, this.curMap.date, this.curMap.key)
            this.curMap.addPlayer(this)
        }

        if (fullmorph)
            this.unsetFullMorph()

        if (this.follower != null && !this.follower.isEmpty())// On met a jour la Map des personnages qui nous suivent
        {
            for (t in  this.follower.values) {
                if (t.isOnline)
                    SocketManager.GAME_SEND_FLAG_PACKET(t, this)
                else
                    this.follower.remove(t.id)
            }
        }

        if (this.inHouse != null) {
            if (this.inHouse!!.mapId == this.curMap.id) {
                this.inHouse = null
            }
        }

        // We changed map. Call event handler
        DataScriptVM.getInstance()!!.handlers.onMapEnter(this)
    }

    fun teleport(map: GameMap, cell: Int) {
        if (this.fight != null) return
        var PW: GameClient? = null
        if (getAccount()!!.gameClient != null)
            PW = getAccount()!!.gameClient
        if (map == null)
            return
        if (map.getCase(cell) == null)
            return
        if (!cantTP()) {
            if (this.curMap.subArea != null
                    && map.subArea != null) {
                if (this.curMap.subArea!!.id == 165
                        && map.subArea!!.id == 165) {
                    if (this.hasItemTemplate(997, 1, false)) {
                        this.removeItemByTemplateId(997, 1, false)
                    } else {
                        SocketManager.GAME_SEND_Im_PACKET(this, "14")
                        return
                    }
                }
            }
        }

        var fullmorph: Boolean = false
        if (Constant.isInMorphDonjon(curMap.id))
            if (!Constant.isInMorphDonjon(map.id))
                fullmorph = true

        if (map.id == curMap.id) {
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(curMap, this.id)
            curCell.removePlayer(this)
            curCell = curMap.getCase(cell)!!
            curMap.addPlayer(this)
            SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(curMap, this)
            if (fullmorph)
                this.unsetFullMorph()
            return
        }
        if (PW != null) {
            SocketManager.GAME_SEND_GA2_PACKET(PW, this.id)
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(curMap, this.id)
        }
        if (this.mount != null)
            if (this.mount!!.fatigue >= 220)
                this.mount!!.energy = this.mount!!.energy - 1
        curCell.removePlayer(this)
        curMap = map
        curCell = curMap.getCase(cell)!!
        // Verification de la Map
        // Verifier la validit� du Collector
        if (Collector.getCollectorByMapId(curMap.id) != null) {
            if (World.world.getGuild(Collector.getCollectorByMapId(curMap.id)!!.guildId) == null)// Ne devrait pas arriver
            {
                GameServer.a()
                Collector.removeCollector(Collector.getCollectorByMapId(curMap.id)!!.guildId)
            }
        }

        if (PW != null) {
            SocketManager.GAME_SEND_MAPDATA(PW, map.id, curMap.date, curMap.key)
            curMap.addPlayer(this)
            if (fullmorph)
                this.unsetFullMorph()
        }

        if (!follower.isEmpty())// On met a jour la Map des personnages qui nous suivent
        {
            for (t in  follower.values) {
                if (t.isOnline)
                    SocketManager.GAME_SEND_FLAG_PACKET(t, this)
                else
                    follower.remove(t.id)
            }
        }
    }

    fun disconnectInFight() {
        //Si en groupe
        if (party != null)
            party!!.leave(this)
        resetVars()
        DatabaseManager.get(PlayerData::class.java).update(this)
        World.world.unloadPerso(this.id)
    }

    fun getBankCost(): Int {
        return getAccount()!!.bank.size
    }

    fun openBank() {
        if(this.exchangeAction!!.getType() == ExchangeAction.TALKING_WITH) {
            var data: NpcDialogActionData = (this.exchangeAction!!.getValue() as NpcDialogActionData)

            if(!data.npcTemplate.isBankClerk()) {
                // Opening bank while talking to an NPC is not valid, except when the NPc is a bank clerk
                return
            }
            // We were talking to a clerk, close dialog
            this.exchangeAction = null
            SocketManager.GAME_SEND_END_DIALOG_PACKET(this.getGameClient()!!)
        }
        if(this.exchangeAction != null) {
            return
        }
        if (this.deshonor >= 1) {
            SocketManager.GAME_SEND_Im_PACKET(this, "183")
            return
        }

        val cost: Int = this.getBankCost()
        DatabaseManager.get(PlayerData::class.java).update(this)
        if (cost > 0) {

            val kamas: Long = this.kamas
            val remaining: Long = kamas - cost
            val bank: Long = this.getAccount()!!.getBankKamas()
            val total: Long = bank + kamas
            if (remaining < 0) {

                if (bank >= cost) {

                    this.setBankKamas(bank - cost)
                } else if (total >= cost) {

                    this.kamas = 0
                    this.setBankKamas(total - cost)
                    SocketManager.GAME_SEND_STATS_PACKET(this)
                    SocketManager.GAME_SEND_Im_PACKET(this, "020;" + kamas)
                } else {

                    SocketManager.GAME_SEND_MESSAGE_SERVER(this, "10|" + cost)
                    return
                }
            } else {

                this.kamas = remaining
                SocketManager.GAME_SEND_STATS_PACKET(this)
                SocketManager.GAME_SEND_Im_PACKET(this, "020;" + cost)
            }
        }
        SocketManager.GAME_SEND_ECK_PACKET(this.getGameClient()!!, 5, "")
        SocketManager.GAME_SEND_EL_BANK_PACKET(this)
        this.away = true
        this.exchangeAction = ExchangeAction(ExchangeAction.IN_BANK, 0)

    }

    fun getStringVar(str: String): String {
        when (str) {
"[name]" -> {return this.name
}
"[bankCost]" -> {return getBankCost().toString() + ""
}
"[points]" -> {return this.getAccount()!!.points.toString() + ""
}
"[nbrOnline]" -> {return Config.gameServer!!.getClients().size.toString() + ""
}
"[align]" -> {return World.world.statOfAlign
}
else -> {return str
        
}
}
    }

    fun refreshMapAfterFight() {
        SocketManager.send(this, "ILS" + 2000)
        this.regenRate = 2000
        this.curMap.addPlayer(this)
        if (getAccount()!!.gameClient != null)
            SocketManager.GAME_SEND_STATS_PACKET(this)
        this.fight = null
        this.away = false
    }

    fun getBankKamas(): Long {
        return getAccount()!!.getBankKamas()
    }

    fun setBankKamas(i: Long) {
        var account: Account = getAccount()!!
        account.setBankKamas(i)
        DatabaseManager.get(BankData::class.java).update(account)
    }

    fun parseBankPacket(): String {
        var packet: StringBuilder = StringBuilder()
        for (entry in  getAccount()!!.bank)
            packet.append("O").append(entry.encodeItem()).append(";")
        if (getBankKamas() != 0L)
            packet.append("G").append(getBankKamas())
        return packet.toString()
    }

    fun addCapital(pts: Int) {
        capital += pts
    }

    fun addSpellPoint(pts: Int) {
        if (morphMode)
            saveSpellPts += pts
        else
            spellPts += pts
    }

    fun addInBank(guid: Int, qua: Int, outside: Boolean) {
        if (qua <= 0)
            return
        var PersoObj: GameObject? = World.world.getGameObject(guid)

        if (!outside && this.objects == null) return

        if (!outside && objects[guid] == null) // Si le joueur n'a pas l'item dans son sac ...
            return

        if (PersoObj == null || PersoObj.position.toInt() != Constant.ITEM_POS_NO_EQUIPED) // Si c'est un item �quip� ...
            return

        var account: Account = getAccount()!!
        var BankObj: GameObject? = getSimilarBankItem(PersoObj)
        var newQua: Int = PersoObj.quantity - qua
        if (BankObj == null) // Ajout d'un nouvel objet dans la banque
        {
            if (newQua <= 0) // Ajout de toute la quantit� disponible
            {
                removeItem(PersoObj.guid); // On enleve l'objet du sac du joueur
                account.bank.add(PersoObj); // On met l'objet du sac dans la banque, avec la meme quantit�
                var str: String = "O+" + PersoObj.guid + "|" + PersoObj.quantity + "|" + PersoObj.template!!.id + "|" + PersoObj.encodeStats()
                SocketManager.GAME_SEND_EsK_PACKET(this, str)
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, guid)
            } else
            //S'il reste des objets au joueur
            {
                PersoObj.quantity = newQua; //on modifie la quantit� d'item du sac
                BankObj = PersoObj.getClone(qua, true)!!; //On ajoute l'objet a la banque et au monde
                World.world.addGameObject(BankObj)
                account.bank.add(BankObj)

                var str: String = "O+" + BankObj.guid + "|" + BankObj.quantity + "|" + BankObj.template!!.id + "|" + BankObj.encodeStats()
                SocketManager.GAME_SEND_EsK_PACKET(this, str); //Envoie des packets
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, PersoObj)
            }
        } else
        // S'il y avait un item du meme template
        {
            if (newQua <= 0) //S'il ne reste pas d'item dans le sac
            {
                removeItem(PersoObj.guid); //On enleve l'objet du sac du joueur
                World.world.removeGameObject(PersoObj.guid); //On enleve l'objet du monde
                BankObj.quantity = BankObj.quantity + PersoObj.quantity; //On ajoute la quantit� a l'objet en banque
                var str: String = "O+" + BankObj.guid + "|" + BankObj.quantity + "|" + BankObj.template!!.id + "|" + BankObj.encodeStats() //on envoie l'ajout a la banque de l'objet
                SocketManager.GAME_SEND_EsK_PACKET(this, str)
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, guid); //on envoie la supression de l'objet du sac au joueur
            } else
            //S'il restait des objets
            {
                PersoObj.quantity = newQua; //on modifie la quantit� d'item du sac
                BankObj.quantity = BankObj.quantity + qua
                var str: String = "O+" + BankObj.guid + "|" + BankObj.quantity + "|" + BankObj.template!!.id + "|" + BankObj.encodeStats()
                SocketManager.GAME_SEND_EsK_PACKET(this, str)
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, PersoObj)
            }
        }
        SocketManager.GAME_SEND_Ow_PACKET(this)
        DatabaseManager.get(PlayerData::class.java).update(this)
        DatabaseManager.get(BankData::class.java).update(getAccount()!!)
    }

    private fun getSimilarBankItem(exGameObject: GameObject): GameObject? {
        var account: Account = getAccount()!!
        if(account.bank != null)
            for (gameObject in  account.bank)
                if (gameObject != null && World.world.conditionManager.stackIfSimilar(gameObject, exGameObject, true))
                    return gameObject
        return null
    }

    fun removeFromBank(guid: Int, qua: Int) {
        if (qua <= 0)
            return
        var BankObj: GameObject? = World.world.getGameObject(guid)

        //Si le joueur n'a pas l'item dans sa banque ...
        var index: Int = getAccount()!!.bank.indexOf(BankObj)
        if (index == -1)
            return

        var PersoObj: GameObject? = getSimilarItem(BankObj!!)
        var newQua: Int = BankObj!!.quantity - qua

        if (PersoObj == null)//Si le joueur n'avait aucun item similaire
        {
            //S'il ne reste rien en banque
            if (newQua <= 0) {
                //On retire l'item de la banque
                getAccount()!!.bank.removeAt(index)
                //On l'ajoute au joueur

                objects[guid] = BankObj!!


                //On envoie les packets
                SocketManager.GAME_SEND_OAKO_PACKET(this, BankObj!!)
                var str: String = "O-" + guid
                SocketManager.GAME_SEND_EsK_PACKET(this, str)
            } else
            //S'il reste des objets en banque
            {
                //On cr�e une copy de l'item en banque
                PersoObj = BankObj!!.getClone(qua, true)!!
                //On l'ajoute au monde
                World.world.addGameObject(PersoObj)
                //On retire X objet de la banque
                BankObj!!.quantity = newQua
                //On l'ajoute au joueur

                objects[PersoObj.guid] = PersoObj


                //On envoie les packets
                SocketManager.GAME_SEND_OAKO_PACKET(this, PersoObj)
                var str: String = "O+" + BankObj!!.guid + "|" + BankObj!!.quantity + "|" + BankObj!!.template!!.id + "|" + BankObj!!.encodeStats()
                SocketManager.GAME_SEND_EsK_PACKET(this, str)
            }
        } else {
            //S'il ne reste rien en banque
            if (newQua <= 0) {
                //On retire l'item de la banque
                getAccount()!!.bank.removeAt(index)
                World.world.removeGameObject(BankObj!!.guid)
                //On Modifie la quantit� de l'item du sac du joueur
                PersoObj.quantity = PersoObj.quantity + BankObj!!.quantity

                //On envoie les packets
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, PersoObj)
                var str: String = "O-" + guid
                SocketManager.GAME_SEND_EsK_PACKET(this, str)
            } else
            //S'il reste des objets en banque
            {
                //On retire X objet de la banque
                BankObj!!.quantity = newQua
                //On ajoute X objets au joueurs
                PersoObj.quantity = PersoObj.quantity + qua

                //On envoie les packets
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, PersoObj)
                var str: String = "O+" + BankObj!!.guid + "|" + BankObj!!.quantity + "|" + BankObj!!.template!!.id + "|" + BankObj!!.encodeStats()
                SocketManager.GAME_SEND_EsK_PACKET(this, str)
            }
        }

        SocketManager.GAME_SEND_Ow_PACKET(this)

        DatabaseManager.get(PlayerData::class.java).update(this)
        DatabaseManager.get(BankData::class.java).update(getAccount()!!)
    }

    /** * MountPark * * @param target */
    fun openMountPark(target: MountPark) {
        if (this.deshonor >= 5) {
            SocketManager.GAME_SEND_Im_PACKET(this, "183")
            return
        }

        val park: MountPark = if (target == null) this.curMap.mountPark!! else target

        if (this.guildMember != null && park.guild != null) {
            if (park.guild!!.id == this.guildMember!!.guild.id) {
                if (!this.guildMember!!.canDo(Constant.G_USEENCLOS)) {
                    SocketManager.GAME_SEND_Im_PACKET(this, "1101")
                    return
                }
            }
        }

        this.exchangeAction = ExchangeAction(ExchangeAction.IN_MOUNTPARK, park)
        this.away = true

        var packet: StringBuilder = StringBuilder()

        if (park.getEtable().size > 0) {
            for (mount in  park.getEtable()) {
                if (mount == null || mount.size == 50) continue
                if (!packet.toString().isEmpty()) packet.append(";")
                if (mount.owner == this.id) packet.append(mount.parse())
            }
        }

        packet.append("~")

        if (park.getListOfRaising().size > 0) {
            var first1: Boolean = false
            for (id in  park.getListOfRaising()) {
                var mount: Mount? = World.world.getMountById(id)
                if (mount == null) continue

                if (mount.owner == this.id) {
                    if (first1)
                        packet.append(";")
                    packet.append(mount.parse())
                    first1 = true
                    continue
                }
                if (guildMember != null) {
                    if (guildMember!!.canDo(Constant.G_OTHDINDE) && park.owner != -1 && park.guild != null) {
                        if (park.guild!!.id == this.getGuild()!!.id) {
                            if (first1) packet.append(";")
                            packet.append(mount.parse())
                            first1 = true
                        }
                    }
                }
            }
        }

        SocketManager.GAME_SEND_ECK_PACKET(this, 16, packet.toString())
        TimerWaiter.addNext({  -> park.getEtable().stream().filter({ mount -> mount != null && mount.size == 50 && mount.owner == this.id }).forEach({ mount -> SocketManager.GAME_SEND_Ee_PACKET_WAIT(this, '~', mount.parse()) }) }, 500)
    }

    fun fullPDV() {
        this.setPdv(this.maxPdv)
        SocketManager.GAME_SEND_STATS_PACKET(this)
    }

    fun warpToSavePos() {
        try {
            this.teleport(this.savePos.first, this.savePos.second, true)
        } catch (e: Exception) {
            log.error("unexpected error", e)
        }
    }

    fun removeItemByTemplateId(templateId: Int, count: Int, display: Boolean): Boolean {
        // TODO: Rewrite this function to be fail-safe
        // Currently, if we try to remove 10 items but the user only has 9, it removes 9 items then fails.
        var remove: ArrayList<GameObject> = ArrayList()
        var tempCount: Int = count

        //on verifie pour chaque objet
        for (item in  ArrayList(objects.values)) {
            //Si mauvais TemplateID, on passe
            if (item.template!!.id != templateId)
                continue

            if (item.quantity >= count) {
                var newQua: Int = item.quantity - count
                if (newQua > 0) {
                    item.quantity = newQua
                    if (isOnline) SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, item)
                } else {
                    //on supprime de l'inventaire et du Monde
                    objects.remove(item.guid)
                    World.world.removeGameObject(item.guid)
                    //on envoie le packet si connect�
                    if (isOnline) SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, item.guid)
                }
                if (isOnline) {
                    if (display) {
                        SocketManager.GAME_SEND_Im_PACKET(this, "022;" + count + "~" + item.template!!.id)
                    }
                    SocketManager.GAME_SEND_Ow_PACKET(this)
                }
                return true
            } else {
                //Si pas assez d'objet
                if (item.quantity >= tempCount) {
                    var newQua: Int = item.quantity - tempCount
                    if (newQua > 0) {
                        item.quantity = newQua
                        if (isOnline) SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, item)
                    } else {
                        remove.add(item)
                    }

                    for (o in  remove) {
                        //on supprime de l'inventaire et du Monde

                        objects.remove(o.guid)

                        World.world.removeGameObject(o.guid)
                        //on envoie le packet si connect�
                        if (isOnline) SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, o.guid)
                    }
                    if (isOnline) {
                        if(display) {
                            SocketManager.GAME_SEND_Im_PACKET(this, "022;" + count + "~" + item.template!!.id)
                        }
                        SocketManager.GAME_SEND_Ow_PACKET(this)
                    }
                    return true
                } else {
                    // on r�duit le compteur
                    tempCount -= item.quantity
                    remove.add(item)
                }
            }
        }
        // We failed
        return false
    }

    fun getJobs(): List<Job> {
        return Collections.unmodifiableList(metiers.values.map { it.template })
    }


    fun useCraftSkill(skillId: Int, ingredientsCount: Int) {
        away = true
        exchangeAction = ExchangeAction(ExchangeAction.CRAFTING, skillId)

        SocketManager.GAME_SEND_ECK_PACKET(this, 3, ingredientsCount.toString() + ";" + skillId)
    }

    fun parseJobData(): String {
        var str: StringBuilder = StringBuilder()
        if (metiers.isEmpty())
            return ""
        for (SM in  metiers.values) {
            if (SM == null)
                continue
            if (str.length > 0)
                str.append(";")
            str.append(SM.template!!.id).append(",").append(SM.xp)
        }
        return str.toString()
    }

    fun totalJobBasic(): Int {
        var i: Int = 0

        for (SM in  metiers.values) {
            // Si c'est un m�tier 'basic' :
            if (SM.template!!.id == 2 || SM.template!!.id == 11
                    || SM.template!!.id == 13
                    || SM.template!!.id == 14
                    || SM.template!!.id == 15
                    || SM.template!!.id == 16
                    || SM.template!!.id == 17
                    || SM.template!!.id == 18
                    || SM.template!!.id == 19
                    || SM.template!!.id == 20
                    || SM.template!!.id == 24
                    || SM.template!!.id == 25
                    || SM.template!!.id == 26
                    || SM.template!!.id == 27
                    || SM.template!!.id == 28
                    || SM.template!!.id == 31
                    || SM.template!!.id == 36
                    || SM.template!!.id == 41
                    || SM.template!!.id == 56
                    || SM.template!!.id == 58
                    || SM.template!!.id == 60
                    || SM.template!!.id == 65) {
                i++
            }
        }
        return i
    }

    fun totalJobFM(): Int {
        var i: Int = 0

        for (SM in  metiers.values) {
            // Si c'est une sp�cialisation 'FM' :
            if (SM.template!!.id == 43
                    || SM.template!!.id == 44
                    || SM.template!!.id == 45
                    || SM.template!!.id == 46
                    || SM.template!!.id == 47
                    || SM.template!!.id == 48
                    || SM.template!!.id == 49
                    || SM.template!!.id == 50
                    || SM.template!!.id == 62
                    || SM.template!!.id == 63
                    || SM.template!!.id == 64) {
                i++
            }
        }
        return i
    }

    fun canAggro(): Boolean {
        return canAggro
    }


    fun getMetierBySkill(skID: Int): JobStat? {
        for (SM in  metiers.values)
            if (SM.isValidMapAction(skID))
                return SM
        return null
    }

    fun parseToFriendList(guid: Int): String {
        var str: StringBuilder = StringBuilder()
        str.append(";")
        str.append("?;")
        str.append(this.name).append(";")
        if (getAccount()!!.isFriendWith(guid)) {
            str.append(this.level).append(";")
            str.append(alignment).append(";")
        } else {
            str.append("?;")
            str.append("-1;")
        }
        str.append(this.classe).append(";")
        str.append(this.sexe).append(";")
        str.append(gfxId)
        return str.toString()
    }

    fun parseToEnemyList(guid: Int): String {
        var str: StringBuilder = StringBuilder()
        str.append(";")
        str.append("?;")
        str.append(this.name).append(";")
        if (getAccount()!!.isFriendWith(guid)) {
            str.append(this.level).append(";")
            str.append(alignment).append(";")
        } else {
            str.append("?;")
            str.append("-1;")
        }
        str.append(this.classe).append(";")
        str.append(this.sexe).append(";")
        str.append(gfxId)
        return str.toString()
    }

    fun getMetierByID(job: Int): JobStat? {
        for (SM in  metiers.values)
            if (SM.template!!.id == job)
                return SM
        return null
    }


    fun toogleOnMount() {
        if (mount == null || this.morphMode || this.level < 60)
            return
        if (Config.subscription) {
            SocketManager.GAME_SEND_Im_PACKET(this, "1115")
            return
        }
        if (this.classe * 10 + this.sexe != this.gfxId)
            return
        if (this.inHouse != null) {
            SocketManager.GAME_SEND_Im_PACKET(this, "1117")
            return
        }
        if (!onMount && mount!!.isMontable() == 0) {
            SocketManager.GAME_SEND_Re_PACKET(this, "Er", null)
            return
        }

        if (mount!!.energy < Formulas.calculEnergieLooseForToogleMount(mount!!.fatigue)) {
            SocketManager.GAME_SEND_Im_PACKET(this, "1113")
            return
        }

        if (!onMount) {
            var EnergyoLose: Int = mount!!.energy
                    - Formulas.calculEnergieLooseForToogleMount(mount!!.fatigue)
            mount!!.energy = EnergyoLose
        }

        onMount = !onMount
        var obj: GameObject? = getObjetByPos(Constant.ITEM_POS_FAMILIER)

        if (onMount && obj != null) {
            obj.position = Constant.ITEM_POS_NO_EQUIPED
            SocketManager.GAME_SEND_OBJET_MOVE_PACKET(this, obj)
        }

        if (mount!!.energy <= 0) {
            mount!!.energy = 0
            SocketManager.GAME_SEND_Im_PACKET(this, "1114")
            return
        }
        //on envoie les packets
        if (fight != null && fight!!.state == 2) {
            SocketManager.GAME_SEND_ALTER_FIGHTER_MOUNT(fight!!, fight!!.getFighterByPerso(this), id, fight!!.getTeamId(id), fight!!.getOtherTeamId(id))
        } else {
            SocketManager.GAME_SEND_ALTER_GM_PACKET(curMap, this)
        }
        SocketManager.GAME_SEND_Re_PACKET(this, "+", mount!!)
        SocketManager.GAME_SEND_Rr_PACKET(this,if (onMount) "+" else "-")
        SocketManager.GAME_SEND_STATS_PACKET(this)

    }





    fun resetVars() {
        if (this.exchangeAction != null) {
            if (this.exchangeAction!!.getValue() is JobAction && ((this.exchangeAction!!.getValue() as JobAction)).jobCraft != null)
                ((this.exchangeAction!!.getValue() as JobAction)).jobCraft!!.jobAction.broke = true
            this.exchangeAction = null
        }

        doAction = false
        this.gameAction = null

        away = false
        emoteActive = 0
        fight = null
        duelId = 0
        ready = false
        party = null
        inviting = 0
        sitted = false
        onMount = false
        isAbsent = false
        isInvisible = false
        follower.clear()
        follow = null
        curHouse = null
        isGhost = false
        livreArti = false
        spec = false
        afterFight = false
    }

    fun addChanel(chan: String) {
        if (canaux.indexOf(chan) >= 0)
            return
        canaux += chan
        SocketManager.GAME_SEND_cC_PACKET(this, '+', chan)
    }

    fun removeChanel(chan: String) {
        canaux = canaux.replace(chan, "")
        SocketManager.GAME_SEND_cC_PACKET(this, '-', chan)
    }

    fun modifAlignement(i: Int) {
        honor = 0
        deshonor = 0
        alignment = i.toInt()
        aLvl = 1
        SocketManager.GAME_SEND_ZC_PACKET(this, i)
        SocketManager.GAME_SEND_STATS_PACKET(this)
        toggleWings('+')
    }








    fun toggleWings(type: Char) {
        if (this.alignment == Constant.ALIGNEMENT_NEUTRE || Config.modeHeroic) {
            this.send("BN")
            return
        }

        var loose: Int = this.honor * 5 / 100
        when (type) {
'*' -> {SocketManager.GAME_SEND_GIP_PACKET(this, loose)
                return
}
'+' -> {this.showWings = true
                SocketManager.GAME_SEND_ALTER_GM_PACKET(this.curMap, this)
                DatabaseManager.get(PlayerData::class.java).update(this)
                
}
'-' -> {this.showWings = false
                this.honor -= loose
                SocketManager.GAME_SEND_ALTER_GM_PACKET(this.curMap, this)
                DatabaseManager.get(PlayerData::class.java).update(this)
                
}
}
        SocketManager.GAME_SEND_STATS_PACKET(this)
    }

    fun addHonor(winH: Int) {
        if (alignment == 0)
            return
        var curGrade: Int = getGrade()
        honor += winH
        if (honor > 18000) honor = 18000
        SocketManager.GAME_SEND_Im_PACKET(this, "080;" + winH)
        //Changement de grade
        if (getGrade() != curGrade) {
            SocketManager.GAME_SEND_Im_PACKET(this, "082;" + getGrade())
        }
    }

    fun remHonor(losePH: Int) {
        if (alignment == 0)
            return
        var curGrade: Int = getGrade()
        honor -= losePH
        SocketManager.GAME_SEND_Im_PACKET(this, "081;" + losePH)
        //Changement de grade
        if (getGrade() != curGrade) {
            SocketManager.GAME_SEND_Im_PACKET(this, "083;" + getGrade())
        }
    }




    fun parseZaapList(): String//Pour le packet WC
    {
        var map: Int = Optional.ofNullable(savePos).map({ p -> p.first }).orElse(curMap.id)

        var str: StringBuilder = StringBuilder()
        str.append(map)

        if(this.curMap.subArea != null) {
            var superAreaID: Int = curMap.subArea!!.area!!.superArea
            for (i in  zaaps) {
                try {
                    if (World.world.getMap(i) == null)
                        continue
                }catch (e: NullPointerException) {
                    Main.logger.error("Unknown zaap map #{}", i)
                    continue
                }
                if (World.world.getMap(i).subArea!!.area!!.superArea != superAreaID)
                    continue
                var cost: Int = Formulas.calculZaapCost(this, curMap, World.world.getMap(i))
                if (i == curMap.id)
                    cost = 0
                str.append("|").append(i).append(";").append(cost)
            }
        }
        return str.toString()
    }

    fun parsePrismesList(): String {
        var map: String = curMap.id.toString() + ""
        var str: String = map + ""
        for (Prisme in  World.world.AllPrisme()!!) {
            if (Prisme.alignment != alignment)
                continue
            var MapID: Int = Prisme.map
            if (World.world.getMap(MapID) == null)
                continue
            if (Prisme.fight != null) {
                str += "|" + MapID + ";*"
            } else {
                var costo: Int = Formulas.calculZaapCost(this, curMap, World.world.getMap(MapID))
                if (MapID == curMap.id)
                    costo = 0
                str += "|" + MapID + ";" + costo
            }
        }
        return str
    }

    fun openZaapMenu() {
        if (this.fight == null) {
            if (!verifOtomaiZaap())
                return
            if (deshonor >= 3) {
                SocketManager.GAME_SEND_Im_PACKET(this, "183")
                return
            }

            this.exchangeAction = ExchangeAction(ExchangeAction.IN_ZAAPING, 0)
            verifAndAddZaap(curMap.id)
            SocketManager.GAME_SEND_WC_PACKET(this)
        }
    }

    fun openTrunk(cellID: Int) {
        Trunk.getTrunkIdByCoord(curMap.id, cellID).ifPresent { trunk ->
            if (trunk.player != null) {
                this.send("Im120")
                return@ifPresent
            }
            this.exchangeAction = ExchangeAction(ExchangeAction.IN_TRUNK, trunk)
            Trunk.open(this, "-", true)
        }
    }

    fun verifAndAddZaap(mapId: Int) {
        if (!verifOtomaiZaap())
            return
        if (!zaaps.contains(mapId)) {
            zaaps.add(mapId)
            SocketManager.GAME_SEND_Im_PACKET(this, "024")
            DatabaseManager.get(PlayerData::class.java).update(this)
        }
    }

    fun verifOtomaiZaap(): Boolean {
        return Config.allZaap || !(this.curMap.id == 10643 || this.curMap.id == 11210)
                || World.world.conditionManager.validConditions(this, "QT=231") && World.world.conditionManager.validConditions(this, "QT=232")
    }

    fun openPrismeMenu() {
        if (this.fight == null) {
            if (deshonor >= 3) {
                SocketManager.GAME_SEND_Im_PACKET(this, "183")
                return
            }

            this.exchangeAction = ExchangeAction(ExchangeAction.IN_PRISM, 0)
            SocketManager.SEND_Wp_MENU_Prisme(this)
        }
    }

    fun useZaap(id: Int) {
        if (this.exchangeAction == null || this.exchangeAction!!.getType() != ExchangeAction.IN_ZAAPING)
            return
        if (this.fight != null || this.isInPrison()
                || !this.zaaps.contains(id) || !this.zaaps.contains(this.curMap.id)) {
            SocketManager.GAME_SEND_WV_PACKET(this)
            return
        }

        var map: GameMap = World.world.getMap(id)
        var cost: Int = Formulas.calculZaapCost(this, this.curMap, map)

        if (this.kamas < cost) return

        if (map == null) {
            SocketManager.GAME_SEND_WUE_PACKET(this)
            return
        }

        var cell: GameCase? = map.getCase(World.world.getZaapCellIdByMapId(id))
        if (cell == null || !cell.isWalkable(false)) {
            SocketManager.GAME_SEND_WUE_PACKET(this)
            return
        }
        /*if (World.world.getMap(id).getSubArea().getArea().getSuperArea() != this.curMap.getSubArea().getArea().getSuperArea()) {
            SocketManager.GAME_SEND_WUE_PACKET(this);
            return;
        }*/
        if ((id == 4263 && this.alignment == 2) || (id == 5295 && this.alignment == 1))
            return

        this.kamas -= cost
        this.teleport(id, cell.getId())
        SocketManager.GAME_SEND_STATS_PACKET(this);//On envoie la perte de kamas
        SocketManager.GAME_SEND_WV_PACKET(this);//On ferme l'interface Zaap
        this.exchangeAction = null
    }

    fun usePrisme(packet: String) {
        if (this.exchangeAction == null || this.exchangeAction!!.getType() != ExchangeAction.IN_PRISM)
            return
        var celdaID: Int = 340
        var MapID: Int = 7411
        for (Prisme in  World.world.AllPrisme()!!) {
            if (Prisme.map == packet.substring(2).toInt()) {
                celdaID = Prisme.cell
                MapID = Prisme.map
                break
            }
        }
        var costo: Int = Formulas.calculZaapCost(this, curMap, World.world.getMap(MapID))
        if (MapID == curMap.id)
            costo = 0
        if(costo < 1)
            costo = 100
        if (kamas < costo) {
            SocketManager.GAME_SEND_MESSAGE(this, this.getLang().trans("client.player.useprisme.nokamas"))
            return
        }
        kamas -= costo
        SocketManager.GAME_SEND_STATS_PACKET(this)
        this.teleport(packet.substring(2).toInt(), celdaID)
        SocketManager.SEND_Ww_CLOSE_Prisme(this)
        this.exchangeAction = null
    }

    fun parseZaaps(): String {
        var str: StringBuilder = StringBuilder()
        var first: Boolean = true

        if (zaaps.isEmpty())
            return ""
        for (i in  zaaps) {
            if (!first)
                str.append(",")
            first = false
            str.append(i)
        }
        return str.toString()
    }

    fun parsePrism(): String {
        var prism: Prism? = curMap.subArea!!.prism
        if (prism == null) return "-3"
        else if (prism.fight!!.state == Constant.FIGHT_STATE_PLACE)
            return "0;" + (45000 - (System.currentTimeMillis() - prism.fight!!.launchTime)) + ";45000;7"
        else
            return if (prism.fight != null && prism.fight!!.state == Constant.FIGHT_STATE_ACTIVE) "-2" else "-1"; // -1 ou 0 ?
    }

    fun stopZaaping() {
        if (this.exchangeAction == null || this.exchangeAction!!.getType() != ExchangeAction.IN_ZAAPING)
            return

        this.exchangeAction = null
        SocketManager.GAME_SEND_WV_PACKET(this)
    }

    fun Zaapi_close() {
        if (this.exchangeAction == null || this.exchangeAction!!.getType() != ExchangeAction.IN_ZAPPI)
            return
        this.exchangeAction = null
        SocketManager.GAME_SEND_CLOSE_ZAAPI_PACKET(this)
    }

    fun Prisme_close() {
        if (this.exchangeAction == null || this.exchangeAction!!.getType() != ExchangeAction.IN_PRISM)
            return
        this.exchangeAction = null
        SocketManager.SEND_Ww_CLOSE_Prisme(this)
    }

    fun Zaapi_use(packet: String) {
        if (this.exchangeAction == null || this.exchangeAction!!.getType() != ExchangeAction.IN_ZAPPI)
            return
        var map: GameMap = World.world.getMap((packet.substring(2)).toInt())

        if (map != null) {
            var cell: Int = map.findObjectsPositionsByID(listOf(7030, 7031)).findFirst().orElse(0)
            if(cell == 0) throw IllegalStateException(("no zaapi found for map #%d").format( map.id))

            cell += 18; // Get cell below

            if (map.subArea != null && (map.subArea!!.area!!.id == 7 || map.subArea!!.area!!.id == 11)) {
                var price: Int = 20
                if (this.alignment == 1 || this.alignment == 2)
                    price = 10
                kamas -= price
                SocketManager.GAME_SEND_STATS_PACKET(this)
                if ((map.subArea!!.area!!.id == 7 && this.curMap.subArea!!.area!!.id == 7)
                        || (map.subArea!!.area!!.id == 11 && this.curMap.subArea!!.area!!.id == 11)) {
                    this.teleport((packet.substring(2)).toInt(), cell, false)
                }
                SocketManager.GAME_SEND_CLOSE_ZAAPI_PACKET(this)
                this.exchangeAction = null
            }
        }
    }

    fun hasItemTemplate(i: Int, q: Int, equipped: Boolean): Boolean {
        for (obj in  objects.values) {
            if (!equipped && obj.position.toInt() != Constant.ITEM_POS_NO_EQUIPED)
                continue
            if (obj.template!!.id != i)
                continue
            if (obj.quantity >= q)
                return true
        }
        return false
    }

    fun hasItemType(type: Int): Boolean {
        for (obj in  objects.values) {
            if (obj.position.toInt() != Constant.ITEM_POS_NO_EQUIPED)
                continue
            if (obj.template!!.type == type)
                return true
        }

        return false
    }

    fun getItemTemplate(i: Int, q: Int): GameObject? {
        for (obj in  objects.values) {
            if (obj.position.toInt() != Constant.ITEM_POS_NO_EQUIPED)
                continue
            if (obj.template!!.id != i)
                continue
            if (obj.quantity >= q)
                return obj
        }
        return null
    }

    fun getItemTemplate(i: Int): GameObject? {

        for (obj in  objects.values) {
            if (obj.template!!.id != i)
                continue
            return obj
        }

        return null
    }

    fun getNbItemTemplate(i: Int): Int {
        for (obj in  objects.values) {
            if (obj.template!!.id != i)
                continue
            return obj.quantity
        }
        return -1
    }

    fun isDispo(sender: Player): Boolean {
        return !isAbsent && (!isInvisible || getAccount()!!.isFriendWith(sender.getAccount()!!.id))

    }


    fun setCurrentTitle(i: Int) {
        currentTitle = (i.toByte())
    }

    //FIN CLONAGE
    fun VerifAndChangeItemPlace() {
        var isFirstAM: Boolean = true
        var isFirstAN: Boolean = true
        var isFirstANb: Boolean = true
        var isFirstAR: Boolean = true
        var isFirstBO: Boolean = true
        var isFirstBOb: Boolean = true
        var isFirstCA: Boolean = true
        var isFirstCE: Boolean = true
        var isFirstCO: Boolean = true
        var isFirstDa: Boolean = true
        var isFirstDb: Boolean = true
        var isFirstDc: Boolean = true
        var isFirstDd: Boolean = true
        var isFirstDe: Boolean = true
        var isFirstDf: Boolean = true
        var isFirstFA: Boolean = true

        for (obj in  objects.values) {
            if (obj.position == Constant.ITEM_POS_NO_EQUIPED)
                continue
            if (obj.position == Constant.ITEM_POS_AMULETTE) {
                if (isFirstAM) {
                    isFirstAM = false
                } else {
                    obj.position = Constant.ITEM_POS_NO_EQUIPED
                }
            } else if (obj.position == Constant.ITEM_POS_ANNEAU1) {
                if (isFirstAN) {
                    isFirstAN = false
                } else {
                    obj.position = Constant.ITEM_POS_NO_EQUIPED
                }
            } else if (obj.position == Constant.ITEM_POS_ANNEAU2) {
                if (isFirstANb) {
                    isFirstANb = false
                } else {
                    obj.position = Constant.ITEM_POS_NO_EQUIPED
                }
            } else if (obj.position == Constant.ITEM_POS_ARME) {
                if (isFirstAR) {
                    isFirstAR = false
                } else {
                    obj.position = Constant.ITEM_POS_NO_EQUIPED
                }
            } else if (obj.position == Constant.ITEM_POS_BOTTES) {
                if (isFirstBO) {
                    isFirstBO = false
                } else {
                    obj.position = Constant.ITEM_POS_NO_EQUIPED
                }
            } else if (obj.position == Constant.ITEM_POS_BOUCLIER) {
                if (isFirstBOb) {
                    isFirstBOb = false
                } else {
                    obj.position = Constant.ITEM_POS_NO_EQUIPED
                }
            } else if (obj.position == Constant.ITEM_POS_CAPE) {
                if (isFirstCA) {
                    isFirstCA = false
                } else {
                    obj.position = Constant.ITEM_POS_NO_EQUIPED
                }
            } else if (obj.position == Constant.ITEM_POS_CEINTURE) {
                if (isFirstCE) {
                    isFirstCE = false
                } else {
                    obj.position = Constant.ITEM_POS_NO_EQUIPED
                }
            } else if (obj.position == Constant.ITEM_POS_COIFFE) {
                if (isFirstCO) {
                    isFirstCO = false
                } else {
                    obj.position = Constant.ITEM_POS_NO_EQUIPED
                }
            } else if (obj.position == Constant.ITEM_POS_DOFUS1) {
                if (isFirstDa) {
                    isFirstDa = false
                } else {
                    obj.position = Constant.ITEM_POS_NO_EQUIPED
                }
            } else if (obj.position == Constant.ITEM_POS_DOFUS2) {
                if (isFirstDb) {
                    isFirstDb = false
                } else {
                    obj.position = Constant.ITEM_POS_NO_EQUIPED
                }
            } else if (obj.position == Constant.ITEM_POS_DOFUS3) {
                if (isFirstDc) {
                    isFirstDc = false
                } else {
                    obj.position = Constant.ITEM_POS_NO_EQUIPED
                }
            } else if (obj.position == Constant.ITEM_POS_DOFUS4) {
                if (isFirstDd) {
                    isFirstDd = false
                } else {
                    obj.position = Constant.ITEM_POS_NO_EQUIPED
                }
            } else if (obj.position == Constant.ITEM_POS_DOFUS5) {
                if (isFirstDe) {
                    isFirstDe = false
                } else {
                    obj.position = Constant.ITEM_POS_NO_EQUIPED
                }
            } else if (obj.position == Constant.ITEM_POS_DOFUS6) {
                if (isFirstDf) {
                    isFirstDf = false
                } else {
                    obj.position = Constant.ITEM_POS_NO_EQUIPED
                }
            } else if (obj.position == Constant.ITEM_POS_FAMILIER) {
                if (isFirstFA) {
                    isFirstFA = false
                } else {
                    obj.position = Constant.ITEM_POS_NO_EQUIPED
                }
            }
        }
    }

    //Mariage





    fun get_wife_friendlist(): String {
        var wife: Player? = World.world.getPlayer(this.wife)
        var str: StringBuilder = StringBuilder()
        if (wife != null) {
            var color1: Int = wife.color1
            var color2: Int = wife.color2
            var color3: Int = wife.color3
            if (wife.getObjetByPos(Constant.ITEM_POS_MALEDICTION) != null)
                if (wife.getObjetByPos(Constant.ITEM_POS_MALEDICTION)!!.template!!.id == 10838) {
                    color1 = 16342021
                    color2 = 16342021
                    color3 = 16342021
                }
            str.append(wife.name).append("|").append(wife.gfxId).append("|").append(color1).append("|").append(color2).append("|").append(color3).append("|")
            if (!wife.isOnline) {
                str.append("|")
            } else {
                str.append(wife.parse_towife()).append("|")
            }
        } else {
            str.append("|")
        }
        return str.toString()
    }

    fun parse_towife(): String {
        var f: Int = 0
        if (fight != null) {
            f = 1
        }
        return curMap.id.toString() + "|" + this.level + "|" + f
    }

    fun meetWife(p: Player)// Se teleporter selon les sacro-saintes autorisations du mariage.
    {
        if (p == null)
            return; // Ne devrait theoriquement jamais se produire.

        if (this.getPodUsed() >= this.getMaxPod()) // Refuser la t�l�portation si on est full pods.
        {
            SocketManager.GAME_SEND_Im_PACKET(this, "170")
            return
        }

        var dist: Int = (curMap.x - p.curMap.x) * (curMap.x - p.curMap.x) + (curMap.y - p.curMap.y) * (curMap.y - p.curMap.y)
        if (dist > 100 || p.curMap.id == this.curMap.id)// La distance est trop grande...
        {
            if (p.sexe == 0)
                SocketManager.GAME_SEND_Im_PACKET(this, "178")
            else
                SocketManager.GAME_SEND_Im_PACKET(this, "179")
            return
        }

        var cellPositiontoadd: Int = Constant.getNearestCellIdUnused(p)
        if (cellPositiontoadd == -1) {
            if (p.sexe == 0)
                SocketManager.GAME_SEND_Im_PACKET(this, "141")
            else
                SocketManager.GAME_SEND_Im_PACKET(this, "142")
            return
        }

        teleport(p.curMap.id, cellPositiontoadd)
    }

    fun Divorce() {
        if (online)
            SocketManager.GAME_SEND_Im_PACKET(this, "047;" + World.world.getPlayer(wife)!!.name)

        wife = 0
        DatabaseManager.get(PlayerData::class.java).update(this)
    }


    fun setisOK(ok: Int): Int {
        isOK = ok
        return isOK
    }


    fun getEquippedObjects(): List<GameObject> {
        val objects: MutableList<GameObject> = ArrayList()
        synchronized(this.objects) {
            objects.addAll(this.objects.values.filter { `object` -> `object`.position != -1 && `object`.position < 34 })
        }
        return objects
    }

    fun changeOrientation(toOrientation: Int) {
        if (this.orientation == 0 || this.orientation == 2
                || this.orientation == 4 || this.orientation == 6) {
            this.orientation = toOrientation
            SocketManager.GAME_SEND_eD_PACKET_TO_MAP(curMap, this.id, toOrientation)
        }
    }

    /** Heroic **/
    var dead: Byte = 0
    var deathCount: Byte = 0
    var deadType: Byte = 0
    var deadLevel: Short = 0
    var deadTime: Long = 0
    var killByTypeId: Long = 0
    var totalKills: Long = 0





    fun increaseTotalKills() {
        this.totalKills++
    }


    fun getDeathInformation(): String {
        return dead.toString() + "," + deadTime + "," + deadType + "," + killByTypeId + "," + deadLevel
    }

    fun die(type: Byte, id: Long) {
        ArrayList(this.items.values).filter(Objects::nonNull).forEach({ `object` -> this.removeItem(`object`.guid, `object`.quantity, true, false) })
        this.setFuneral()
        this.deathCount++
        this.deadLevel = (this.level.toShort())
        this.deadType = type
        this.killByTypeId = id
    }

    fun revive() {
        var revive: Int = DatabaseManager.get(PlayerData::class.java).canRevive(this)

        if(revive == 1) {
            this.curMap = World.world.getMap( 7411)
            this.curCell = World.world.getMap( 7411).getCase(311)!!
        } else {
            if(this.morphMode) this.unsetFullMorph()
            if (this.party != null) this.party!!.leave(this)
            this.resetVars()
            this.stats.addOneStat(125, -this.stats.getEffect(125))
            this.stats.addOneStat(124, -this.stats.getEffect(124))
            this.stats.addOneStat(118, -this.stats.getEffect(118))
            this.stats.addOneStat(123, -this.stats.getEffect(123))
            this.stats.addOneStat(119, -this.stats.getEffect(119))
            this.stats.addOneStat(126, -this.stats.getEffect(126))
            this.addCapital(-this.capital)
            this.setSpellPoints(0)
            this.statsParcho.effects.clear()
            this.sorts.clear()
            this.sorts.putAll(Constant.getStartSorts(classe))
            this.sortsPlaces.clear()
            this.sortsPlaces.putAll(Constant.getStartSortsPlaces(classe))
            if(this.level >= 100)
                this.stats.addOneStat(Constant.STATS_ADD_PA, -1)
            this.level = 1
            this.exp = 0
            this.curMap = World.world.getMap(Constant.getStartMap(this.classe).toInt())
            this.curCell = this.curMap.getCase(Constant.getStartCell(this.classe))!!
            this.honor = 0
            this.deshonor = 0
            this.alignment = 0
            this.kamas = 0
            //this.metiers.clear();
            if(this.mount != null) {
                for(gameObject in  this.mount!!.objects.values)
                    World.world.removeGameObject(gameObject.guid)
                this.mount!!.objects.clear()

                this.mount = null
                this.mountXpGive = 0
            }
        }

        this.isGhost = false
        this.dead = 0
        this.energy = Player.maxEnergy.toInt()
        this.gfxId = (this.classe.toString() + "" + this.sexe).toInt()
        this.canAggro = true
        this.away = false
        this.speed = 0

        DatabaseManager.get(PlayerData::class.java).setRevive(this)
    }
    /** End heroic **/



    fun setFuneral() {
        this.dead = 1
        this.deadTime = System.currentTimeMillis()
        this.energy = -1
        if (this.onMount)
            this.toogleOnMount()
        if (this.orientation == 2) {
            this.orientation = 1
            SocketManager.GAME_SEND_eD_PACKET_TO_MAP(this.curMap, this.id, 1)
        }
        this.gfxId = (this.classe.toString() + "3").toInt()
        SocketManager.send(this, "AR3K");//Block l'orientation
        SocketManager.send(this, "M112");//T'es mort!! t'es mort!! Mouhhahahahahaaaarg
        SocketManager.GAME_SEND_ALTER_GM_PACKET(curMap, this)
    }

    fun setGhost() {
        if (onMount)
            toogleOnMount()
        if (Config.modeHeroic) {
            this.gfxId = (this.classe.toString() + "" + this.sexe).toInt()
            this.send("GO")
            return
        }

        this.dead = 0
        this.isGhost = true
        this.energy = 0
        gfxId = 8004
        canAggro = false
        away = true
        speed = -40
        this.regenRate = 0
        SocketManager.send(this, "AR6bk")

        var subArea: Optional<SubArea> = Optional.ofNullable(this.curMap).map { it.subArea }
        if (!subArea.isPresent()) {
            return
        }

        // TODO: Refactor that mess
        var phoenixList: String = subArea
            .map({ it.area })
            .map({ it!!.superArea })
            .map({ superArea ->
                if(superArea == INCARNAM_SUPERAREA) {
                    return@map "1;5"
                }
                null
            }).orElse(Constant.ALL_PHOENIX)!!
        SocketManager.send(this, "IH" + phoenixList)

        Constant.tpCim(this)
    }

    fun setAlive() {
        if (!this.isGhost)
            return
        this.isGhost = false
        this.dead = 0
        this.energy = 1000
        this.setPdv(1)
        this.gfxId = (this.classe.toString() + "" + this.sexe).toInt()
        this.canAggro = true
        this.away = false
        this.speed = 0
        SocketManager.GAME_SEND_MESSAGE(this, "Tu as gagné <b>1000</b> points d'énergie.", "009900")
        SocketManager.GAME_SEND_STATS_PACKET(this)
        SocketManager.GAME_SEND_ALTER_GM_PACKET(this.curMap, this)
        SocketManager.send(this, "IH")
        SocketManager.send(this, "AR6bk");//Block l'orientation
    }



    fun setLastFightForEndFightAction(fight: Fight?) {
        this.endFightAction = null
        this.lastFight = fight
    }

    fun setNeededEndFightAction(f: Fight, endFightAction: Action) {
        this.lastFight = f
        this.endFightAction = endFightAction
    }

    fun applyEndFightAction(): Boolean {
        if(this.endFightAction == null) {
            return false
        }
        this.endFightAction!!.apply(this, null, -1, -1, curMap)
        this.endFightAction = null
        return true
    }

    fun parseStoreItemsList(): String {
        var list: StringBuilder = StringBuilder()
        if (storeItems.isEmpty())
            return ""
        for (obj in storeItems.entries) {
            var O: GameObject? = World.world.getGameObject(obj.key)
            if (O == null)
                continue
            //O.getPoidOfBaseItem(O.getPlayerId());
            list.append(O.guid).append(";").append(O.quantity).append(";").append(O.template!!.id).append(";").append(O.encodeStats()).append(";").append(obj.value).append("|")
        }

        return (if (list.length > 0) list.toString().substring(0, list.length - 1) else list.toString())
    }

    fun parseStoreItemsListPods(): Int {
        if (storeItems.isEmpty())
            return 0
        var total: Int = 0
        for (obj in storeItems.entries) {
            var O: GameObject? = World.world.getGameObject(obj.key)
            if (O != null) {
                var qua: Int = O.quantity
                var poidBase1: Int = O.template!!.pod * qua
                total += poidBase1
            }
        }
        return total
    }

    fun parseStoreItemstoBD(): String {
        var str: StringBuilder = StringBuilder()
        for (storeObjets in storeItems.entries) {
            str.append(storeObjets.key).append(",").append(storeObjets.value).append("|")
        }

        return str.toString()
    }

    fun addInStore(ObjID: Int, price: Int, qua: Int) {
        var PersoObj: GameObject? = World.world.getGameObject(ObjID)
        //Si le joueur n'a pas l'item dans son sac ...
        if (storeItems[ObjID] != null) {
                storeItems.remove(ObjID)
                storeItems[ObjID] = price
                SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(this, this)
                return
        }

        if (objects[ObjID] == null) {
            GameServer.a()
            return
        }

        if(PersoObj == null || PersoObj.isAttach) return

        //Si c'est un item �quip� ...
        if (PersoObj.position.toInt() != Constant.ITEM_POS_NO_EQUIPED)
            return

        var SimilarObj: GameObject? = getSimilarStoreItem(PersoObj!!)
        var newQua: Int = PersoObj!!.quantity - qua
        if (SimilarObj == null)//S'il n'y pas d'item du meme Template
        {
            //S'il ne reste pas d'item dans le sac
            if (newQua <= 0) {
                //On enleve l'objet du sac du joueur
                removeItem(PersoObj.guid)
                //On met l'objet du sac dans le store, avec la meme quantit�
                storeItems[PersoObj.guid] = price
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, PersoObj.guid)
                SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(this, this)
            } else
            //S'il reste des objets au joueur
            {
                //on modifie la quantit� d'item du sac
                PersoObj.quantity = newQua
                //On ajoute l'objet a la banque et au monde
                SimilarObj = PersoObj.getClone(qua, true)!!
                World.world.addGameObject(SimilarObj)
                storeItems[SimilarObj.guid] = price

                //Envoie des packets
                SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(this, this)
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, PersoObj)

            }
        } else
        // S'il y avait un item du meme template
        {
            //S'il ne reste pas d'item dans le sac
            if (newQua <= 0) {
                //On enleve l'objet du sac du joueur
                removeItem(PersoObj.guid)
                //On enleve l'objet du monde
                World.world.removeGameObject(PersoObj.guid)
                //On ajoute la quantit� a l'objet en banque
                SimilarObj.quantity = SimilarObj.quantity + PersoObj.quantity

                storeItems.remove(SimilarObj!!.guid)
                storeItems[SimilarObj.guid] = price

                //on envoie l'ajout a la banque de l'objet
                SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(this, this)
                //on envoie la supression de l'objet du sac au joueur
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, PersoObj.guid)
            } else
            //S'il restait des objets
            {
                //on modifie la quantit� d'item du sac
                PersoObj.quantity = newQua
                SimilarObj.quantity = SimilarObj.quantity + qua

                storeItems.remove(SimilarObj!!.guid)
                storeItems[SimilarObj.guid] = price

                SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(this, this)
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, PersoObj)

            }
        }
        SocketManager.GAME_SEND_Ow_PACKET(this)
        DatabaseManager.get(PlayerData::class.java).update(this)
    }

    private fun getSimilarStoreItem(exGameObject: GameObject): GameObject? {
        for (id in storeItems.keys) {
            var gameObject: GameObject? = World.world.getGameObject(id)
            if (World.world.conditionManager.stackIfSimilar(gameObject!!, exGameObject, true))
                return gameObject
        }

        return null
    }

    fun removeFromStore(guid: Int, qua: Int) {
        var SimilarObj: GameObject? = World.world.getGameObject(guid)
        //Si le joueur n'a pas l'item dans son store ...
        if (storeItems[guid] == null) {
            GameServer.a()
            return
        }

        var PersoObj: GameObject? = getSimilarItem(SimilarObj!!)
        var newQua: Int = SimilarObj!!.quantity - qua
        if (PersoObj == null)//Si le joueur n'avait aucun item similaire
        {
            //S'il ne reste rien en store
            if (newQua <= 0) {
                //On retire l'item du store
                storeItems.remove(guid)
                //On l'ajoute au joueur
                objects[guid] = SimilarObj!!

                //On envoie les packets
                SocketManager.GAME_SEND_OAKO_PACKET(this, SimilarObj!!)
                SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(this, this)
            }
        } else {
            //S'il ne reste rien en store
            if (newQua <= 0) {
                //On retire l'item de la banque
                storeItems.remove(SimilarObj!!.guid)
                World.world.removeGameObject(SimilarObj!!.guid)
                //On Modifie la quantit� de l'item du sac du joueur
                PersoObj!!.quantity = PersoObj!!.quantity + SimilarObj!!.quantity
                //On envoie les packets
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, PersoObj)
                SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(this, this)
            }
        }
        SocketManager.GAME_SEND_Ow_PACKET(this)
        DatabaseManager.get(PlayerData::class.java).update(this)
    }

    fun removeStoreItem(guid: Int) {
        storeItems.remove(guid)
    }

    fun addStoreItem(guid: Int, price: Int) {
        storeItems[guid] = price
    }









    fun hasSpell(spellID: Int): Boolean {
        return (getSortStatBySortIfHas(spellID) != null)
    }

    fun leaveEnnemyFaction() {
        if (!isInEnnemyFaction)
            return;//pas en prison on fait pas la commande
        var pGrade: Int = this.getGrade()
        var compar: Long = System.currentTimeMillis()
                - (enteredOnEnnemyFaction + 60000 * pGrade)

        when (pGrade){  1 -> {if (compar >= 0) {
                    leaveFaction()
                    this.sendMessage(this.getLang().trans("client.player.jail.free.oneminute", "1"))
                } else {
                    var restant: Long = -compar
                    if (restant <= 1000)
                        restant = 1000
                    this.sendMessage(this.getLang().trans("client.player.jail.free.wait", restant / 1000))
                }
                
}
2 -> {if (compar >= 0) {
                    leaveFaction()
                    this.sendMessage(this.getLang().trans("client.player.jail.free.after", "2"))
                } else {
                    var restant: Long = -compar
                    if (restant <= 1000)
                        restant = 1000
                    this.sendMessage(this.getLang().trans("client.player.jail.free.wait", restant / 1000))
                }
                
}
3 -> {if (compar >= 0) {
                    leaveFaction()
                    this.sendMessage(this.getLang().trans("client.player.jail.free.after", "3"))
                } else {
                    var restant: Long = -compar
                    if (restant <= 1000)
                        restant = 1000
                    this.sendMessage(this.getLang().trans("client.player.jail.free.wait", restant / 1000))
                }
                
}
4 -> {if (compar >= 0) {
                    leaveFaction()
                    this.sendMessage(this.getLang().trans("client.player.jail.free.after", "4"))
                } else {
                    var restant: Long = -compar
                    if (restant <= 1000)
                        restant = 1000
                    this.sendMessage(this.getLang().trans("client.player.jail.free.wait", restant / 1000))
                }
                
}
5 -> {if (compar >= 0) {
                    leaveFaction()
                    this.sendMessage(this.getLang().trans("client.player.jail.free.after", "5"))
                } else {
                    var restant: Long = -compar
                    if (restant <= 1000)
                        restant = 1000
                    this.sendMessage(this.getLang().trans("client.player.jail.free.wait", restant / 1000))
                }
                
}
6 -> {if (compar >= 0) {
                    leaveFaction()
                    this.sendMessage(this.getLang().trans("client.player.jail.free.after", "6"))
                } else {
                    var restant: Long = -compar
                    if (restant <= 1000)
                        restant = 1000
                    this.sendMessage(this.getLang().trans("client.player.jail.free.wait", restant / 1000))
                }
                
}
7 -> {if (compar >= 0) {
                    leaveFaction()
                    this.sendMessage(this.getLang().trans("client.player.jail.free.after", "7"))
                } else {
                    var restant: Long = -compar
                    if (restant <= 1000)
                        restant = 1000
                    this.sendMessage(this.getLang().trans("client.player.jail.free.wait", restant / 1000))
                }
                
}
8 -> {if (compar >= 0) {
                    leaveFaction()
                    this.sendMessage(this.getLang().trans("client.player.jail.free.after", "8"))
                } else {
                    var restant: Long = -compar
                    if (restant <= 1000)
                        restant = 1000
                    this.sendMessage(this.getLang().trans("client.player.jail.free.wait", restant / 1000))
                }
                
}
9 -> {if (compar >= 0) {
                    leaveFaction()
                    this.sendMessage(this.getLang().trans("client.player.jail.free.after", "9"))
                } else {
                    var restant: Long = -compar
                    if (restant <= 1000)
                        restant = 1000
                    this.sendMessage(this.getLang().trans("client.player.jail.free.wait", restant / 1000))
                }
                
}
10 -> {if (compar >= 0) {
                    leaveFaction()
                    this.sendMessage(this.getLang().trans("client.player.jail.free.after", "10"))
                } else {
                    var restant: Long = -compar
                    if (restant <= 1000)
                        restant = 1000
                    this.sendMessage(this.getLang().trans("client.player.jail.free.wait", restant / 1000))
                }
                
}
}
        DatabaseManager.get(PlayerData::class.java).update(this)
    }

    fun leaveEnnemyFactionAndPay(perso: Player) {
        if (!isInEnnemyFaction)
            return;//pas en prison on fait pas la commande
        var pGrade: Int = perso.getGrade()
        var curKamas: Long = perso.kamas
        when (pGrade){  1 -> {if (curKamas < 1000) {
                    SocketManager.GAME_SEND_MESSAGE(perso, "Tu ne possédes que " + curKamas + "Kamas. Tu n'as pas assez d'argent pour sortir !", "009900")
                } else {
                    var countKamas: Int = 1000
                    var newKamas: Long = curKamas - countKamas
                    if (newKamas < 0)
                        newKamas = 0
                    perso.kamas = newKamas
                    leaveFaction()
                    SocketManager.GAME_SEND_MESSAGE(perso, "Tu viens de payer " + countKamas + "Kamas pour sortir. Il te reste maintenant " + newKamas + "Kamas.", "009900")
                }
                
}
2 -> {if (curKamas < 2000) {
                    SocketManager.GAME_SEND_MESSAGE(perso, "Tu ne possédes que " + curKamas + "Kamas. Tu n'as pas assez d'argent pour sortir !", "009900")
                } else {
                    var countKamas: Int = 2000
                    var newKamas: Long = curKamas - countKamas
                    if (newKamas < 0)
                        newKamas = 0
                    perso.kamas = newKamas
                    leaveFaction()
                    SocketManager.GAME_SEND_MESSAGE(perso, "Tu viens de payer " + countKamas + "Kamas pour sortir. Il te reste maintenant " + newKamas + "Kamas.", "009900")
                }
                
}
3 -> {if (curKamas < 3000) {
                    SocketManager.GAME_SEND_MESSAGE(perso, "Tu ne possédes que " + curKamas + "Kamas. Tu n'as pas assez d'argent pour sortir !", "009900")
                } else {
                    var countKamas: Int = 3000
                    var newKamas: Long = curKamas - countKamas
                    if (newKamas < 0)
                        newKamas = 0
                    perso.kamas = newKamas
                    leaveFaction()
                    SocketManager.GAME_SEND_MESSAGE(perso, "Tu viens de payer " + countKamas + "Kamas pour sortir. Il te reste maintenant " + newKamas + "Kamas.", "009900")
                }
                
}
4 -> {if (curKamas < 4000) {
                    SocketManager.GAME_SEND_MESSAGE(perso, "Tu ne possédes que " + curKamas + "Kamas. Tu n'as pas assez d'argent pour sortir !", "009900")
                } else {
                    var countKamas: Int = 4000
                    var newKamas: Long = curKamas - countKamas
                    if (newKamas < 0)
                        newKamas = 0
                    perso.kamas = newKamas
                    leaveFaction()
                    SocketManager.GAME_SEND_MESSAGE(perso, "Tu viens de payer " + countKamas + "Kamas pour sortir. Il te reste maintenant " + newKamas + "Kamas.", "009900")
                }
                
}
5 -> {if (curKamas < 5000) {
                    SocketManager.GAME_SEND_MESSAGE(perso, "Tu ne possédes que " + curKamas + "Kamas. Tu n'as pas assez d'argent pour sortir !", "009900")
                } else {
                    var countKamas: Int = 5000
                    var newKamas: Long = curKamas - countKamas
                    if (newKamas < 0)
                        newKamas = 0
                    perso.kamas = newKamas
                    leaveFaction()
                    SocketManager.GAME_SEND_MESSAGE(perso, "Tu viens de payer " + countKamas + "Kamas pour sortir. Il te reste maintenant " + newKamas + "Kamas.", "009900")
                }
                
}
6 -> {if (curKamas < 7000) {
                    SocketManager.GAME_SEND_MESSAGE(perso, "Tu ne possédes que " + curKamas + "Kamas. Tu n'as pas assez d'argent pour sortir !", "009900")
                } else {
                    var countKamas: Int = 7000
                    var newKamas: Long = curKamas - countKamas
                    if (newKamas < 0)
                        newKamas = 0
                    perso.kamas = newKamas
                    leaveFaction()
                    SocketManager.GAME_SEND_MESSAGE(perso, "Tu viens de payer " + countKamas + "Kamas pour sortir. Il te reste maintenant " + newKamas + "Kamas.", "009900")
                }
                
}
7 -> {if (curKamas < 9000) {
                    SocketManager.GAME_SEND_MESSAGE(perso, "Tu ne possédes que " + curKamas + "Kamas. Tu n'as pas assez d'argent pour sortir !", "009900")
                } else {
                    var countKamas: Int = 9000
                    var newKamas: Long = curKamas - countKamas
                    if (newKamas < 0)
                        newKamas = 0
                    perso.kamas = newKamas
                    leaveFaction()
                    SocketManager.GAME_SEND_MESSAGE(perso, "Tu viens de payer " + countKamas + "Kamas pour sortir. Il te reste maintenant " + newKamas + "Kamas.", "009900")
                }
                
}
8 -> {if (curKamas < 12000) {
                    SocketManager.GAME_SEND_MESSAGE(perso, "Tu ne possédes que " + curKamas + "Kamas. Tu n'as pas assez d'argent pour sortir !", "009900")
                } else {
                    var countKamas: Int = 12000
                    var newKamas: Long = curKamas - countKamas
                    if (newKamas < 0)
                        newKamas = 0
                    perso.kamas = newKamas
                    leaveFaction()
                    SocketManager.GAME_SEND_MESSAGE(perso, "Tu viens de payer " + countKamas + "Kamas pour sortir. Il te reste maintenant " + newKamas + "Kamas.", "009900")
                }
                
}
9 -> {if (curKamas < 16000) {
                    SocketManager.GAME_SEND_MESSAGE(perso, "Tu ne possédes que " + curKamas + "Kamas. Tu n'as pas assez d'argent pour sortir !", "009900")
                } else {
                    var countKamas: Int = 16000
                    var newKamas: Long = curKamas - countKamas
                    if (newKamas < 0)
                        newKamas = 0
                    perso.kamas = newKamas
                    leaveFaction()
                    SocketManager.GAME_SEND_MESSAGE(perso, "Tu viens de payer " + countKamas + "Kamas pour sortir. Il te reste maintenant " + newKamas + "Kamas.", "009900")
                }
                
}
10 -> {if (curKamas < 25000) {
                    SocketManager.GAME_SEND_MESSAGE(perso, "Tu ne possédes que " + curKamas + "Kamas. Tu n'as pas assez d'argent pour sortir !", "009900")
                } else {
                    var countKamas: Int = 25000
                    var newKamas: Long = curKamas - countKamas
                    if (newKamas < 0)
                        newKamas = 0
                    perso.kamas = newKamas
                    leaveFaction()
                    SocketManager.GAME_SEND_MESSAGE(perso, "Tu viens de payer " + countKamas + "Kamas pour sortir. Il te reste maintenant " + newKamas + "Kamas.", "009900")
                }
                
}
}
        DatabaseManager.get(PlayerData::class.java).update(this)
        SocketManager.GAME_SEND_STATS_PACKET(perso)
    }

    fun leaveFaction() {
        try {
            isInEnnemyFaction = false
            enteredOnEnnemyFaction = 0
            warpToSavePos()
        } catch (e: Exception) {
            log.error("unexpected error", e)
        }
    }

    fun teleportWithoutBlocked(newMapID: Int, newCellID: Int)//Aucune condition genre <<en_prison>> etc
    {
        var PW: GameClient? = null
        if (getAccount()!!.gameClient != null) {
            PW = getAccount()!!.gameClient
        }
        if (World.world.getMap(newMapID) == null) {
            GameServer.a()
            return
        }
        if (World.world.getMap(newMapID).getCase(newCellID) == null) {
            GameServer.a()
            return
        }
        if (PW != null) {
            SocketManager.GAME_SEND_GA2_PACKET(PW, this.id)
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(curMap, this.id)
        }
        curCell.removePlayer(this)
        curMap = World.world.getMap(newMapID)
        curCell = curMap.getCase(newCellID)!!

        //Verification de la Map
        //Verifier la validit� du mountpark
        if (curMap.mountPark != null
                && curMap.mountPark!!.owner > 0
                && curMap.mountPark!!.guild!!.id != -1) {
            if (World.world.getGuild(curMap.mountPark!!.guild!!.id) == null)//Ne devrait pas arriver
            {
                GameServer.a()
                GameMap.removeMountPark(curMap.mountPark!!.guild!!.id)
            }
        }
        //Verifier la validit� du Collector
        if (Collector.getCollectorByMapId(curMap.id) != null) {
            if (World.world.getGuild(Collector.getCollectorByMapId(curMap.id)!!.guildId) == null)//Ne devrait pas arriver
            {
                GameServer.a()
                Collector.removeCollector(Collector.getCollectorByMapId(curMap.id)!!.guildId)
            }
        }

        if (PW != null) {
            SocketManager.GAME_SEND_MAPDATA(PW, newMapID, curMap.date, curMap.key)
            curMap.addPlayer(this)
        }

        if (!follower.isEmpty())//On met a jour la Map des personnages qui nous suivent
        {
            for (t in  follower.values) {
                if (t.isOnline)
                    SocketManager.GAME_SEND_FLAG_PACKET(t, this)
                else
                    follower.remove(t.id)
            }
        }
    }

    fun teleportFaction(factionEnnemy: Int) {
        var mapID: Int = 0
        var cellID: Int = 0
        enteredOnEnnemyFaction = System.currentTimeMillis()
        isInEnnemyFaction = true

        when (factionEnnemy){  1 -> {mapID = 6164
                cellID = 236
                
}
2 -> {mapID = 6171
                cellID = 397
                
}
3 -> {mapID = 1002
                cellID = 326
                
}
else -> {mapID = 8534
                cellID = 297
                
}
}
        this.sendMessage(this.getLang().trans("client.player.jail.ask.waiting"))
        if (this.energy <= 0) {
            if (onMount)
                toogleOnMount()
            this.isGhost = true
            gfxId = 8004
            canAggro = false
            away = true
            speed = -40
        }
        teleportWithoutBlocked(mapID, cellID)
        DatabaseManager.get(PlayerData::class.java).update(this)
    }

    fun encodeColorsForMount(): String {
        return getColors().joinToString(",") { it.toString() }
    }

    //region Objects class

    fun addObjectClassSpell(spell: Int, effect: Int, value: Int) {
        if (spell !in objectsClassSpell) {
            objectsClassSpell[spell] = World.Couple(effect, value)
        }
    }

    fun removeObjectClassSpell(spell: Int) {
        if (spell in objectsClassSpell) {
            objectsClassSpell.remove(spell)
        }
    }

    fun addObjectClass(item: Int) {
        if (!objectsClass.contains(item))
            objectsClass.add(item)
    }

    fun removeObjectClass(item: Int) {
        if (objectsClass.contains(item)) {
            var index: Int = objectsClass.indexOf(item)
            objectsClass.remove(index)
        }
    }

    fun refreshObjectsClass() {
        for (position in 2 until 8) {
            var `object`: GameObject? = getObjetByPos(position)

            if(`object` != null) {
                var template: ObjectTemplate? = `object`.template
                var set: Int = `object`.template!!.panoId

                if (template != null && set >= 81 && set <= 92) {
                    var stats: List<String> = `object`.template!!.strTemplate.split(",")
                    for (stat in  stats) {
                        val split: List<String> = stat.split("#")
                        var effect: Int = split[0].toInt(16)
                        var spell: Int = split[1].toInt(16)
                        var value: Int = split[3].toInt(16)
                        if(effect == 289)
                            value = 1
                        SocketManager.SEND_SB_SPELL_BOOST(this, effect.toString() + ";" + spell + ";" + value)
                        addObjectClassSpell(spell, effect, value)
                    }

                    if (!this.objectsClass.contains(template.id))
                        this.objectsClass.add(template.id)
                }
            }
        }
    }

    fun getValueOfClassObject(spell: Int, effect: Int): Int {
        if (spell in this.objectsClassSpell) {
            if (this.objectsClassSpell[spell]!!.first == effect) {
               return this.objectsClassSpell[spell]!!.second
            }
        }
        return 0
    }
    //endregion

    fun storeAllBuy(): Int {
        var total: Int = 0
        for (value in storeItems.entries) {
            var O: GameObject? = World.world.getGameObject(value.key)
            var multiple: Int = O!!.quantity
            var add: Int = value.value * multiple
            total += add
        }

        return total
    }

    fun DialogTimer() {
        TimerWaiter.addNext({  -> {
            if (this.exchangeAction == null || this.exchangeAction!!.getType() != ExchangeAction.TRADING_WITH_COLLECTOR)
                return@addNext
            if ((this.exchangeAction!!.getValue() as Int) != 0) {
                var collector: Collector? = World.world.getCollector((this.exchangeAction!!.getValue() as Int))
                if (collector == null)
                    return@addNext
                collector.reloadTimer()
                for (z in  World.world.getGuild(collector.guildId)!!.getPlayers()) {
                    if (z == null)
                        continue
                    if (z.isOnline) {
                        SocketManager.GAME_SEND_gITM_PACKET(z, Collector.parseToGuild(z.getGuild()!!.id))
                        var str: String = "G" + collector.getFullName() + "|.|" + World.world.getMap(collector.map).x + "|" + World.world.getMap(collector.map).y + "|" + name + "|" + collector.xp + ";"

                        if (!collector.getLogObjects().equals(""))
                            str += collector.getLogObjects()

                        this.guildMember!!.giveXpToGuild(collector.xp)
                        SocketManager.GAME_SEND_gT_PACKET(z, str)
                    }
                }
                curMap.RemoveNpc(collector.id)
                SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(curMap, collector.id)
                collector.delCollector(collector.id)
                DatabaseManager.get(CollectorData::class.java).delete(collector)
            }
            DatabaseManager.get(PlayerData::class.java).update(getAccount()!!.currentPlayer!!)
            SocketManager.GAME_SEND_EV_PACKET(getGameClient()!!)
            away = false
        } }, 5, TimeUnit.MINUTES)
    }






    fun getAlignMap(): Int {
        if (this.curMap.subArea == null)
            return -1
        if (this.curMap.subArea!!.alignment == 0)
            return 1
        if (this.curMap.subArea!!.alignment == this.alignment)
            return 1
        return -1
    }


    fun addStaticEmote(emote: Int): Boolean {
        if (this.emotes.contains(emote))
            return false
        this.emotes.add(emote)
        if (!online)
            return true
        SocketManager.GAME_SEND_EMOTE_LIST(this, getCompiledEmote(emotes))
        SocketManager.GAME_SEND_STATS_PACKET(this)
        SocketManager.send(this, "eA" + emote)
        return true
    }

    fun parseEmoteToDB(): String {
        var str: StringBuilder = StringBuilder()
        var isFirst: Boolean = true
        for (i in  emotes) {
            if (isFirst)
                str.append(i).append("")
            else
                str.append(";").append(i)
            isFirst = false
        }
        return str.toString()
    }



    fun getGameClient(): GameClient? {
        return if (this.getAccount() != null) this.getAccount()!!.gameClient else null
    }

    fun send(packet: String) {
        SocketManager.send(this, packet)
    }

    fun sendMessage(msg: String) {
        SocketManager.GAME_SEND_MESSAGE(this, msg)
    }

    fun sendTypeMessage(name: String, msg: String) {
        this.send("Im116;<b>" + name + "</b>~" + msg)
    }

    fun sendServerMessage(msg: String) {
        this.sendTypeMessage("Server", msg)
    }

    fun isSubscribe(): Boolean {
        return !Config.subscription || this.getAccount()!!.isSubscribe()
    }

    fun isMissingSubscription(): Boolean {
        var ok: Boolean = Config.subscription

        if (this.curMap == null)
            return false
        when (this.curMap.id) {
6824, 6825, 6826 -> {return false
        
}
}
        if (this.curMap.subArea == null)
            return false
        if (this.curMap.subArea!!.area == null)
            return false
        if (this.curMap.subArea!!.area!!.superArea == 3
                || this.curMap.subArea!!.area!!.superArea == 4
                || this.curMap.subArea!!.area!!.id == 18)
            ok = false

        return ok
    }

    fun cantDefie(): Boolean {
        return curMap.data.noDefy
    }

    fun cantAgro(): Boolean {
        return curMap.data.noAgro
    }

    fun cantCanal(): Boolean {
        return curMap.data.noCanal
    }

    fun cantTP(): Boolean {
        return this.isInPrison() || curMap.data.noTp || EventManager.isInEvent(this)
    }

    fun isInPrison(): Boolean {
        if (this.curMap == null)
            return false

        when (this.curMap.id) {
666, 8726 -> {return true
        
}
}
        return false
    }

    fun addQuestProgression(qProgress: QuestProgress) {
        getAccount()!!.addQuestProgression(qProgress)
    }

    fun delQuestProgress(qProgress: QuestProgress) {
        getAccount()!!.delQuestProgress(qProgress)
    }

    fun getQuestProgress(questId: Int): QuestProgress? {
        return getAccount()!!.getQuestProgress(this.id, questId)
    }

    fun getQuestProgressForCurrentStep(stepId: Int): Optional<QuestProgress> {
        return getAccount()!!.getQuestProgressions(this.id).filter({ qp -> qp.getCurrentStep() == stepId }).findFirst()
    }


    fun getQuestProgressions(): Stream<QuestProgress> {
        return getAccount()!!.getQuestProgressions(this.id)
    }

    fun sendQuestStatus(questId: Int) {
        var qp: QuestProgress? = getAccount()!!.getQuestProgress(this.id, questId)

        if(qp == null) {
            throw NullPointerException("sendQuestStatus called for non current quest")
        }

        // Call lua to get quest info
        var qi: QuestInfo? = DataScriptVM.getInstance()!!.handlers.questInfo(this, questId, qp.getCurrentStep())
        if(qi == null) {
            throw NullPointerException("sendQuestStatus called for unknown quest")
        }

        var sj: StringJoiner = StringJoiner("|")
        sj.add("QS"+listOf(questId.toString(), if (qi!!.isAccountBound) "1" else "0", if (qi.isRepeatable) "1" else "0").joinToString(";"))


        sj.add(qp.getCurrentStep().toString())
        sj.add(qi.objectives.stream().map { oId ->
            var completedStr: String = if (qp.hasCompletedObjective(oId)) "1" else "0"
            oId.toString()+","+completedStr
        }.collect(Collectors.joining(";")))
        sj.add(Objects.toString(qi.previous, ""))
        sj.add(Objects.toString(qi.next, ""))
        if (qi.question != null) {
            sj.add(Objects.toString(qi.question))
        }

        send(sj.toString())
    }

    fun encodeQuestList(): String {
        return "QL+" + getAccount()!!.getQuestProgressions(this.id).
            map { qp ->
                var qi: QuestInfo? = DataScriptVM.getInstance()!!.handlers.questInfo(this, qp.questId, qp.getCurrentStep())

                listOf(qp.questId.toString(), if (qp.isFinished()) "1" else "0",
                    "", // List sort order. AccountBound/Repeatable quests tend to appear last (higher weight)
                    if (qi!!.isAccountBound) "1" else "0", if (qi.isRepeatable) "1" else "0"
                ).joinToString(";")
            }.collect(Collectors.joining("|"))
    }

    fun saveQuestProgress() {
        getAccount()!!.saveQuestProgress()
    }



    var exchangeAction: ExchangeAction<*>? = null
        @Synchronized set(v) {
            if (v == null) this.away = false
            field = v
        }

    fun refreshCraftSecure(unequip: Boolean) {
        // Optimize by directly checking the job for the equipped tool
        for (player in  this.curMap.players) {
            if(player == null) continue

            var `object`: GameObject? = player.getObjetByPos(Constant.ITEM_POS_ARME)
            if (`object` == null) {
                if (unequip) {
                    for(target in  this.curMap.players)
                        target.send("EW+" + player.id + "|")
                }
                continue
            }
            var toolID: Int = `object`.template!!.id

            var availableSkills: MutableList<Int> = ArrayList()
            for (job in  player.getJobs()) {
                if (job.getSkills().isEmpty())
                    continue
                if (!job.isValidTool(toolID))
                    continue

                // Compute list of skills this player can use on this map
                this.curMap.data.interactiveObjects.values
                    .map({ job.getSkills()[it] }) // Get possibles skills on this `object`
                    .filter(Objects::nonNull)   // Make sure we have one
                    .flatMap({ it!! })  // Group all possible skills from all objects in one list
                    .forEach({ availableSkills.add(it) })
            }

            if(availableSkills.isEmpty()) continue

            var packet: String = "EW+" + player.id + "|" + availableSkills.stream().distinct().map({ it.toString() }).collect(Collectors.joining(";"))
            for(target in  this.curMap.players) {
                if (target == null) continue

                target.send(packet)
            }
        }
    }

    fun setFullMorphbouf(team: Int) {

        if (this.onMount) this.toogleOnMount()
        if (morphMode)
            unsetFullMorph()
        if (this.isGhost) {
            SocketManager.send(this, "Im1185")
            return
        }

        saveSpellPts = spellPts
        saveSorts.putAll(sorts)
        saveSortsPlaces.putAll(sortsPlaces)


        morphMode = true
        sorts.clear()
        sortsPlaces.clear()
        spellPts = 0
        this.Savecolors = this.color1.toString() + "," + this.color2 + "," + this.color3


        if(team == 0)//rouge
        {
            this.color1 = 16713479
            this.color2 = 16777215
            this.color3 = 16718620
        }else//bleu
        {
            this.color1 = 360441
            this.color2 = 94461
            this.color3 = 486135
        }
        if (this.fight == null)
            SocketManager.GAME_SEND_ALTER_GM_PACKET(this.curMap, this)
        parseSpellsFullMorph("143;5;b,689;5;c,151;5;d,50;5;e,449;1;f")
        if (this.fight == null) {
            //SocketManager.GAME_SEND_ALTER_GM_PACKET(this.getCurMap(), this);
            //SocketManager.GAME_SEND_ASK(this.getGameClient(), this);
            SocketManager.GAME_SEND_SPELL_LIST(this)
        }

        this.Savestats = this.maxPdv.toString() + "," + this.pa + "," + this.pm + ","  + this.vitalite + "," + this.sagesse + "," + this.terre + "," + this.feu + "," + this.eau + "," + this.air + "," + this.initiative
        _maxPdv = 1000
        this.setPdv(this.maxPdv)
        this.pa = 6
        this.pm = 4
        this.vitalite = 1000
        this.sagesse = 100
        this.terre = 0
        this.feu = 0
        this.eau = 0
        this.air = 0
        this.initiative = Formulas.getRandomValue(1, 100)
        this.useStats = true
        this.donjon = false
        this.useCac = false
        if (this.fight == null)
            SocketManager.GAME_SEND_STATS_PACKET(this)
    }

    fun unsetFullMorphbouf() {
        if (!morphMode)
            return

        var morphID: Int = this.classe * 10 + this.sexe
        gfxId = morphID

        useStats = false
        donjon = false
        morphMode = false
        this.useCac = true
        sorts.clear()
        sortsPlaces.clear()
        spellPts = saveSpellPts
        sorts.putAll(saveSorts)
        sortsPlaces.putAll(saveSortsPlaces)
        var stats: List<String> = this.Savestats.split(",")

        _maxPdv = (stats[0]).toInt()
        this.pa = (stats[1]).toInt()
        this.pm = (stats[2]).toInt()
        this.vitalite = (stats[3]).toInt()
        this.sagesse = (stats[4]).toInt()
        this.terre = (stats[5]).toInt()
        this.feu = (stats[6]).toInt()
        this.eau = (stats[7]).toInt()
        this.air = (stats[8]).toInt()
        this.initiative = (stats[9]).toInt()

        var color: List<String> = this.Savecolors.split(",")

        this.color1 = (color[0]).toInt()
        this.color2 = (color[1]).toInt()
        this.color3 = (color[2]).toInt()

        parseSpells(encodeSpellsToDB(), true)
        SocketManager.GAME_SEND_SPELL_LIST(this)
        SocketManager.GAME_SEND_STATS_PACKET(this)
        SocketManager.GAME_SEND_ALTER_GM_PACKET(this.curMap, this)
    }


    fun getLang(): LangEnum {
        if(this.getGameClient() != null)
            return this.getGameClient()!!.getLanguage()
        return LangEnum.ENGLISH
    }

    override fun scripted(): SPlayer {
        return this.scriptVal
    }

    fun consumeCurrency(cur: Currency, qua: Long): Boolean {
        if(cur == Currency.KAMAS) return modKamasDisplay(-qua)
        if(cur == Currency.POINTS) return getAccount()!!.modPoints(-qua)
        if(cur.isItem()) return removeItemByTemplateId(cur.item().id, (qua.toInt()), false)
        throw RuntimeException("unknown currency type")
    }

    fun moveItemShortcutSend(oldPos: Int, newPos: Int): Boolean {
        var hash: ItemHash? = itemShortcuts.getOrDefault(oldPos, null)
        if(hash == null) return false

        return addItemHashShortcutSend(newPos, hash)
    }

    private fun addItemHashShortcutSend(position: Int, hash: ItemHash): Boolean {
        // Free up previously used slot if needed
        removeItemShortcutByHash(hash).ifPresent { p ->
            send("OrR"+p)
        }

        itemShortcuts[position] = hash
        send("OrA"+ listOf(position.toString(), hash.templateId.toString(), hash.strStats).joinToString(";"))
        return true
    }

    fun addItemShortcutSend(position: Int, itemID: Int): Boolean {
        // Ensure user owns items
        var item: GameObject? = objects[itemID]
        if(item == null) return false

        var hash: ItemHash = ItemHash(item)
        return addItemHashShortcutSend(position, hash)
    }

    fun removeItemShortcutSend(position: Int): Boolean {
        if(itemShortcuts.remove(position) != null) {
            // GOOD
            send("OrR"+position)
            return true
        }
        return false
    }

    fun removeItemShortcutByHash(hash: ItemHash): Optional<Int> {
        var position: Optional<Int> = itemShortcuts.entries.stream().filter({ e -> hash.equals(e.value) })
                            .map { it.key }.findFirst()
        position.ifPresent(itemShortcuts::remove)
        return position
    }

    fun sendItemShortcuts() {
        itemShortcuts.entries.stream()
            .map({ e -> Pair(e.key, e.value) })
            .filter({ e -> e.second != null })
            .map({ e -> "OrA"+ listOf(e.first.toString(), e.second.templateId.toString(), e.second.strStats).joinToString(";")
            }).forEach(this::send)
    }
}
