package org.starloco.locos.fight;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.classdump.luna.Table;
import org.starloco.locos.area.SubArea;
import org.starloco.locos.area.map.GameCase;
import org.starloco.locos.area.map.GameMap;
import org.starloco.locos.client.Player;
import org.starloco.locos.client.other.Party;
import org.starloco.locos.client.other.Stalk;
import org.starloco.locos.common.CryptManager
import org.starloco.locos.common.Formulas;
import org.starloco.locos.common.PathFinding;
import org.starloco.locos.common.SocketManager;
import org.starloco.locos.database.DatabaseManager;
import org.starloco.locos.database.data.game.CollectorData;
import org.starloco.locos.database.data.game.ExperienceTables;
import org.starloco.locos.database.data.game.PrismData;
import org.starloco.locos.database.data.login.PlayerData;
import org.starloco.locos.dynamic.FormuleOfficiel;
import org.starloco.locos.entity.Collector;
import org.starloco.locos.entity.Prism;
import org.starloco.locos.entity.monster.MonsterGrade;
import org.starloco.locos.entity.monster.MonsterGroup;
import org.starloco.locos.entity.monster.Monster;
import org.starloco.locos.entity.monster.boss.Bandit;
import org.starloco.locos.entity.mount.Mount;
import org.starloco.locos.entity.pet.PetEntry;
import org.starloco.locos.fight.ia.IAHandler;
import org.starloco.locos.fight.spells.LaunchedSpell;
import org.starloco.locos.fight.spells.Spell.SortStats;
import org.starloco.locos.fight.spells.SpellEffect;
import org.starloco.locos.fight.traps.Glyph;
import org.starloco.locos.fight.traps.Trap;
import org.starloco.locos.fight.turn.Turn;
import org.starloco.locos.game.GameClient;
import org.starloco.locos.game.action.GameAction;
import org.starloco.locos.game.world.World;
import org.starloco.locos.game.world.World.Couple;
import org.starloco.locos.game.world.World.Drop;
import org.starloco.locos.guild.Guild;
import org.starloco.locos.job.JobConstant;
import org.starloco.locos.kernel.Config;
import org.starloco.locos.kernel.Constant;
import org.starloco.locos.`object`.GameObject;
import org.starloco.locos.`object`.ObjectTemplate;
import org.starloco.locos.`object`.entity.SoulStone;
import org.starloco.locos.other.Action;
import org.starloco.locos.script.DataScriptVM;
import org.starloco.locos.script.ScriptVM;
import org.starloco.locos.util.TimerWaiter;
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(Fight::class.java)



open class Fight {
    var id: Int = 0
    var state: Int = 0
    private var guildId: Int = -1
    var type: Int = -1
    /**
     * type/state -> byte
     **/
    private var st1: Int = 0
    private var st2: Int = 0
    var curPlayer: Int = 0
    var captWinner: Int = -1
    var curFighterPa: Int = 0
    var curFighterPm: Int = 0
    var curFighterUsedPa: Int = 0
    var curFighterUsedPm: Int = 0
    val team0: MutableMap<Int,Fighter> = HashMap()
    val team1: MutableMap<Int,Fighter> = HashMap()
    val deadList: MutableList<Fighter> = LinkedList()
    val viewer: MutableMap<Int,Player> = HashMap()
    lateinit var start0: MutableList<GameCase>
    lateinit var start1: MutableList<GameCase>
    val allChallenges: MutableMap<Int,Challenge> = HashMap()
    val rholBack: MutableMap<Int,GameCase> = HashMap()
    val glyphs: MutableList<Glyph> = ArrayList()
    val traps: MutableList<Trap> = ArrayList()
    var orderPlaying: MutableList<Fighter>? = ArrayList()
    val capturer: ArrayList<Fighter> = ArrayList(8)
    val trainer: ArrayList<Fighter> = ArrayList(8)
    var launchTime: Long = 0
    var startTime: Long = 0
    var locked0: Boolean = false
    var locked1: Boolean = false
    var onlyGroup0: Boolean = false
    var onlyGroup1: Boolean = false
    var help0: Boolean = false
    var help1: Boolean = false
    var viewerOk: Boolean = true
    var haveKnight: Boolean = false
    var isBegin: Boolean = false
    var checkTimer: Boolean = false
    var finish: Boolean = false
    var collectorProtect: Boolean = false
    var curAction: String = ""
    lateinit var monsterGroup: MonsterGroup
    lateinit var collector: Collector
    lateinit var prism: Prism
    var map: GameMap? = null
    lateinit var mapOld: GameMap
    lateinit var init0: Fighter
    lateinit var init1: Fighter
    var turn: Turn? = null

    private var totalTurns: Int= 0
    private var defenders: String = ""
    private var trainerWinner: Int= -1
    private var nextId: Int= -100
    private lateinit var monsterGroup2: MonsterGroup

    val winners: MutableList<Fighter> = ArrayList()
    val losers: MutableList<Fighter> = ArrayList()

    constructor(type: Int, id: Int, rpMap: GameMap, perso: Player, init2: Player) {
        this.launchTime = System.currentTimeMillis();
        this.type = type; // 0: D�fie (4: Pvm) 1:PVP (5:Perco)
        this.id = id;
        this.map = rpMap.getMapCopy();
        this.mapOld = rpMap;
        this.init0 = Fighter.NewPlayer(this, perso);
        this.init1 = Fighter.NewPlayer(this, init2);
        this.team0.put(perso.id, init0);
        this.team1.put(init2.id, init1);

        SocketManager.GAME_SEND_GDF_PACKET_TO_FIGHT(perso, this.map!!.cases);
        // on disable le timer de regen cot� client
        var cancelBtn: Int = if (type == Constant.FIGHT_TYPE_CHALLENGE) 1 else 0
        var time: Long = if (type == Constant.FIGHT_TYPE_CHALLENGE) 0 else Constant.TIME_START_FIGHT.toLong()
        SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(this, 7, 2, cancelBtn, 1, 0, time, type);
        if (init2.alignment == 0 && rpMap.subArea != null && rpMap.subArea!!.alignment == 0)
            setHaveKnight();

        var morph: Int = perso.gfxId
        if (morph == 1109 || morph == 1046 || morph == 9001) {
            perso.unsetFullMorph();
            SocketManager.GAME_SEND_ALTER_GM_PACKET(perso.curMap, perso);
        }

        if (init2.stalk == null || init2.stalk!!.target!!.id != perso.id) {
            this.start0 = map!!.places.get(0).stream().map(map!!::getCase).collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
            this.start1 = map!!.places.get(1).stream().map(map!!::getCase).collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));

            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, map!!.places, 0);
            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 2, map!!.places, 1);

            this.init0.cell = getRandomCell(this.start0);
            this.init1.cell = getRandomCell(this.start1);
        } else {
            this.init0.cell = map!!.getCase(perso.curCell.cellId);
            this.init1.cell = map!!.getCase(init2.curCell.cellId);
            this.start0 = Collections.singletonList(perso.curCell);
            this.start1 = Collections.singletonList(init2.curCell);
        }

        st1 = 0;
        st2 = 1;

        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.id.toString() + "", perso.id.toString() + "," + Constant.ETAT_PORTE.toString() + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.id.toString() + "", perso.id.toString() + "," + Constant.ETAT_PORTEUR.toString() + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init2.id.toString() + "", init2.id.toString() + "," + Constant.ETAT_PORTE.toString() + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init2.id.toString() + "", init2.id.toString() + "," + Constant.ETAT_PORTEUR.toString() + ",0");

        if (perso.curCell != null)
            perso.curCell.removePlayer(perso);
        if (init2.curCell != null)
            init2.curCell.removePlayer(init2);

        this.init0.cell!!.addFighter(init0);
        this.init1.cell!!.addFighter(init1);
        perso.fight = this;
        this.init0.team = 0;
        init2.fight = this;
        this.init1.team = 1;
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.curMap, this.init0.id);
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(init2.curMap, this.init1.id);

        if (type == 1) {
            SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(perso.curMap, 0, this.init0.id, this.init1.id, perso.curCell.cellId, "0;" + perso.alignment, init2.curCell.cellId, "0;" + init2.alignment);
        } else {
            SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(perso.curMap, 0, this.init0.id, this.init1.id, perso.curCell.cellId, "0;-1", init2.curCell.cellId, "0;-1");
        }
        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(perso.curMap, this.init0.id, init0);
        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(perso.curMap, this.init1.id, init1);
        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this, 7, map!!);

        mapOld.applyInitFightAction(this);
        state = Constant.FIGHT_STATE_PLACE;
    }

    constructor(id: Int, rpMap: GameMap, group1: MonsterGroup, group2: MonsterGroup) {
        launchTime = System.currentTimeMillis();
        this.checkTimer = true;
        setMobGroup(group1);
        setMobGroup2(group2);
        type = Constant.FIGHT_TYPE_PVM; // (0: D�fie) 4: Pvm (1:PVP) (5:Perco)
        this.id = id;
        this.map = rpMap.getMapCopy();
        this.mapOld = rpMap;

        for (entry in  group1.mobs.entries) {
            var mob: Fighter = Fighter.NewMob(entry.key, this, entry.value)
            this.team0.put(entry.key, mob);
        }

        for (entry in  group2.mobs.entries) {
            var mob: Fighter = Fighter.NewMob(entry.key, this, entry.value)
            this.team1.put(entry.key, mob);
        }

        SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(this, 1, 2, 0, 1, 0, 45000, type);
        // on disable le timer de regen cot� client

        this.start0 = map!!.places.get(0).stream().map(map!!::getCase).collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        this.start1 = map!!.places.get(1).stream().map(map!!::getCase).collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, map!!.places, 0);
        st1 = 0;
        st2 = 1;
        var e: ArrayList<Map.Entry<Int,Fighter>> = ArrayList(this.team1.entries)
        for (entry in  e) {
            var f: Fighter = entry.value
            var cell: GameCase = getRandomCell(start1)!!
            if (cell == null) {
                this.team1.remove(f.id);
                continue;
            }
            if (init1 == null)
                this.init1 = f;
            f.cell = cell;
            f.cell!!.addFighter(f);
            f.team = 1;
            f.fullPdv();
        }
        e.clear();
        e.addAll(this.team0.entries);
        for (entry in  e) {
            var f: Fighter = entry.value
            var cell: GameCase = getRandomCell(start0)!!
            if (cell == null) {
                this.team0.remove(f.id);
                continue;
            }
            if (init0 == null)
                this.init0 = f;
            f.cell = cell;
            f.cell!!.addFighter(f);
            f.team = 0;
            f.fullPdv();
        }

        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(mapOld, group1.id);
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(mapOld, group2.id);

        var c: Int = PathFinding.getNearestCellAround(map, group1.cellId, group2.cellId, ArrayList())
        SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(mapOld, 4, group1.id, group2.id, c, "0;-1", group1.cellId, "1;-1");

        for (f in  this.team0.values)
            SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(mapOld, group1.id, f);
        for (f in  this.team1.values)
            SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(mapOld, group2.id, f);
        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this, 7, map!!);

        mapOld.applyInitFightAction(this);
        state = Constant.FIGHT_STATE_PLACE;
        this.startFight();
    }

    constructor(id: Int, rpMap: GameMap, perso: Player, group: MonsterGroup) {
        this.launchTime = System.currentTimeMillis();
        this.checkTimer = true;
        this.monsterGroup = group;
        demorph(perso);
        this.type = Constant.FIGHT_TYPE_PVM; // (0: D�fie) 4: Pvm (1:PVP) (5:Perco)
        this.id = id;
        this.map = rpMap.getMapCopy();
        this.mapOld = rpMap;
        this.init0 = Fighter.NewPlayer(this, perso);
        this.team0.put(perso.id, this.init0);
        for (entry in  group.mobs.entries) {
            var mob: Fighter = Fighter.NewMob(entry.key, this, entry.value)
            this.team1.put(entry.key, mob);
        }

        SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(this, 1, 2, 0, 1, 0, 45000, type);
        SocketManager.GAME_SEND_GDF_PACKET_TO_FIGHT(perso, this.map!!.cases);
        // on disable le timer de regen cot� client

        scheduleTimer(45);

        this.start0 = map!!.places.get(0).stream().map(map!!::getCase).collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        this.start1 = map!!.places.get(1).stream().map(map!!::getCase).collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, map!!.places, 0);
        st1 = 0;
        st2 = 1;
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.id.toString() + "", perso.id.toString() + "," + Constant.ETAT_PORTE.toString() + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.id.toString() + "", perso.id.toString() + "," + Constant.ETAT_PORTEUR.toString() + ",0");
        var e: ArrayList<Map.Entry<Int,Fighter>> = ArrayList(this.team1.entries)
        for (entry in  e) {
            var f: Fighter = entry.value
            var cell: GameCase = getRandomCell(start1)!!
            if (cell == null) {
                f.setIsDead(true);
                this.team1.remove(f.id);
                continue;
            }
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.id.toString() + "", f.id.toString() + "," + Constant.ETAT_PORTE.toString() + ",0");
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.id.toString() + "", f.id.toString() + "," + Constant.ETAT_PORTEUR.toString() + ",0");
            f.cell = cell;
            f.cell!!.addFighter(f);
            f.team = 1;
            f.fullPdv();
        }
        this.init0.cell = getRandomCell(this.start0);

        if (this.init0.getPlayer()!!.curCell != null)
            this.init0.getPlayer()!!.curCell.removePlayer(this.init0.getPlayer()!!);

        this.init0.cell!!.addFighter(init0);

        this.init0.getPlayer()!!.fight = this;
        this.init0.team = 0;
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.init0.getPlayer()!!.curMap, this.init0.id);
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.init0.getPlayer()!!.curMap, group.id);

        var c: Int = PathFinding.getNearestCellAround(this.init0.getPlayer()!!.curMap, this.init0.getPlayer()!!.curCell.cellId, group.cellId, ArrayList())
        if (c < 0)
            c = this.init0.getPlayer()!!.curCell.cellId;

        SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(this.init0.getPlayer()!!.curMap, 4, this.init0.id, group.id, c, "0;-1", group.cellId, "1;-1");
        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(this.init0.getPlayer()!!.curMap, this.init0.id, this.init0);
        for (f in  this.team1.values)
            SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(this.init0.getPlayer()!!.curMap, group.id, f);
        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this, 7, this.map!!);

        mapOld.applyInitFightAction(this);
        state = Constant.FIGHT_STATE_PLACE;
    }

    constructor(id: Int, rpMap: GameMap, perso: Player, group: MonsterGroup, type: Int) {
        this.launchTime = System.currentTimeMillis();
        this.monsterGroup = group;
        this.type = type; // (0: D�fie) 4: Pvm (1:PVP) (5:Perco)
        this.id = id;
        this.map = rpMap.getMapCopy();
        this.mapOld = rpMap;
        demorph(perso);
        this.init0 = Fighter.NewPlayer(this, perso);
        this.team0.put(perso.id, init0);
        for (entry in  group.mobs.entries) {
            var mob: Fighter = Fighter.NewMob(entry.key, this, entry.value)
            this.team1.put(entry.key, mob);
        }

        if (perso.curPdv >= perso.maxPdv) {
            var pdvMax: Int = perso.maxPdv
            perso.setPdv(pdvMax);
        }

        SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(this, 1, 2, 0, 1, 0, 45000, type);
        SocketManager.GAME_SEND_GDF_PACKET_TO_FIGHT(perso, this.map!!.cases);
        // on disable le timer de regen cot� client

        scheduleTimer(45);

        this.start0 = map!!.places.get(0).stream().map(map!!::getCase).collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        this.start1 = map!!.places.get(1).stream().map(map!!::getCase).collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, map!!.places, 0);
        st1 = 0;
        st2 = 1;
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.id.toString() + "", perso.id.toString() + "," + Constant.ETAT_PORTE.toString() + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.id.toString() + "", perso.id.toString() + "," + Constant.ETAT_PORTEUR.toString() + ",0");

        var e: ArrayList<Map.Entry<Int,Fighter>> = ArrayList(this.team1.entries)
        for (entry in  e) {
            var f: Fighter = entry.value
            var cell: GameCase = getRandomCell(start1)!!
            if (cell == null) {
                this.team1.remove(f.id);
                continue;
            }

            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.id.toString() + "", f.id.toString() + "," + Constant.ETAT_PORTE.toString() + ",0");
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.id.toString() + "", f.id.toString() + "," + Constant.ETAT_PORTEUR.toString() + ",0");
            f.cell = cell;
            f.cell!!.addFighter(f);
            f.team = 1;
            f.fullPdv();
        }
        this.init0.cell = getRandomCell(start0);

        this.init0.getPlayer()!!.curCell.removePlayer(this.init0.getPlayer()!!);

        this.init0.cell!!.addFighter(init0);

        this.init0.getPlayer()!!.fight = this;
        this.init0.team = 0;
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.init0.getPlayer()!!.curMap, this.init0.id);
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.init0.getPlayer()!!.curMap, group.id);
        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(this.init0.getPlayer()!!.curMap, this.init0.id, init0);
        for (f in  this.team1.values)
            SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(this.init0.getPlayer()!!.curMap, group.id, f);
        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this, 7, map!!);

        mapOld.applyInitFightAction(this);
        state = Constant.FIGHT_STATE_PLACE;
    }

    constructor(id: Int, rpMap: GameMap, perso: Player, perco: Collector) {
        if (perso.fight != null)
            throw IllegalStateException(String.format("%s is already in a fight", perso.name()));
        this.launchTime = System.currentTimeMillis();
        this.guildId = perco.guildId;
        perco.inFight = 1.toByte();

        demorph(perso);

        this.type = Constant.FIGHT_TYPE_PVT; // (0: D�fie) (4: Pvm) (1:PVP) 5:Perco
        this.id = id;
        this.map = rpMap.getMapCopy();
        this.mapOld = rpMap;
        this.init0 = Fighter.NewPlayer(this, perso);
        this.collector = perco;
        // on disable le timer de regen cot� client

        this.team0.put(perso.id, init0);

        var percoF: Fighter = Fighter.NewCollector( id, this, perco)
        this.team1.put(-1, percoF);

        SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(this, 1, 2, 0, 1, 0, 45000, type); // timer de combat
        SocketManager.GAME_SEND_GDF_PACKET_TO_FIGHT(perso, this.map!!.cases);
        scheduleTimer(45);

        var s0: List<GameCase> = map!!.places.get(0).stream().map(map!!::getCase).collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList))
        var s1: List<GameCase> = map!!.places.get(1).stream().map(map!!::getCase).collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList))
        if (Formulas.random.nextBoolean()) {
            this.start0 = ArrayList(s0);
            this.start1 = ArrayList(s1);
            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, map!!.places, 0);
            st1 = 0;
            st2 = 1;
        } else {
            this.start0 = ArrayList(s1);
            this.start1 = ArrayList(s0);
            st1 = 1;
            st2 = 0;
            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, map!!.places, 1);
        }
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.id.toString() + "", perso.id.toString() + "," + Constant.ETAT_PORTE.toString() + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.id.toString() + "", perso.id.toString() + "," + Constant.ETAT_PORTEUR.toString() + ",0");

        var e: ArrayList<Map.Entry<Int,Fighter>> = ArrayList(this.team1.entries)
        for (entry in  e) {
            var f: Fighter = entry.value
            var cell: GameCase = getRandomCell(this.start1)!!
            if (cell == null) {
                this.team1.remove(f.id);
                continue;
            }

            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.id.toString() + "", f.id.toString() + "," + Constant.ETAT_PORTE.toString() + ",0");
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.id.toString() + "", f.id.toString() + "," + Constant.ETAT_PORTEUR.toString() + ",0");
            f.cell = cell;
            f.cell!!.addFighter(f);
            f.team = 1;
            f.fullPdv();

        }
        this.init0.cell = getRandomCell(this.start0);

        this.init0.getPlayer()!!.curCell.removePlayer(this.init0.getPlayer()!!);

        this.init0.cell!!.addFighter(init0);

        this.init0.getPlayer()!!.fight = this;
        this.init0.team = 0;

        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.init0.getPlayer()!!.curMap, this.init0.id);
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.init0.getPlayer()!!.curMap, perco.id);

        var c: Int = PathFinding.getNearestCellAround(this.init0.getPlayer()!!.curMap, this.init0.getPlayer()!!.curCell.cellId, perco.cell, ArrayList())
        if (c < 0)
            c = this.init0.getPlayer()!!.curCell.cellId;

        SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(this.init0.getPlayer()!!.curMap, 5, this.init0.id, perco.id, c, "0;-1", perco.cell, "3;-1");
        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(this.init0.getPlayer()!!.curMap, this.init0.id, init0);

        for (f in  this.team1.values)
            SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(this.init0.getPlayer()!!.curMap, perco.id, f);

        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this, 7, map!!);

        mapOld.applyInitFightAction(this);
        state = Constant.FIGHT_STATE_PLACE;

        var str: String = ""
        if (this.collector != null)
            str = "A" + this.collector.getFullName().toString() + "|.|" + World.world.getMap(collector.map).x.toString() + "|" + World.world.getMap(collector.map).y;

        for (z in  World.world.getGuild(guildId)!!.getPlayers()) {
            if (z == null)
                continue;
            if (z.isOnline) {
                SocketManager.GAME_SEND_gITM_PACKET(z, Collector.parseToGuild(z.getGuild()!!.id));
                Collector.parseAttaque(z, guildId);
                Collector.parseDefense(z, guildId);
                SocketManager.SEND_gA_PERCEPTEUR(z, str);
            }
        }
    }

    constructor(id: Int, rpMap: GameMap, player: Player, prism: Prism) {
        this.launchTime = System.currentTimeMillis();
        prism.fight = this;
        demorph(player);
        this.type = (Constant.FIGHT_TYPE_CONQUETE); // (0: Desafio) (4: Pvm) (1:PVP)
        // 5:Perco
        this.id = (id);
        this.map = (rpMap.getMapCopy());
        this.mapOld = rpMap;
        this.init0 = Fighter.NewPlayer(this, player);
        this.prism = (prism);

        this.team0.put(player.id, this.init0);
        var lPrisme: Fighter = Fighter.NewPrism(-1, this, prism)
        this.init1 = lPrisme;
        this.team1.put(-1, lPrisme);
        SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(this, 1, 2, 0, 1, 0, 60000, type);
        SocketManager.GAME_SEND_GDF_PACKET_TO_FIGHT(player, this.map!!.cases);
        scheduleTimer(60);

        var s0: List<GameCase> = map!!.places.get(0).stream().map(map!!::getCase).collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList))
        var s1: List<GameCase> = map!!.places.get(1).stream().map(map!!::getCase).collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList))
        if (Formulas.random.nextBoolean()) {
            this.start0 = ArrayList(s0);
            this.start1 = ArrayList(s1);
            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, map!!.places, 0);
            st1 = 0;
            st2 = 1;
        } else {
            this.start0 = ArrayList(s1);
            this.start1 = ArrayList(s0);
            st1 = 1;
            st2 = 0;
            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, map!!.places, 1);
        }
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, player.id.toString() + "", player.id.toString() + "," + Constant.ETAT_PORTE.toString() + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, player.id.toString() + "", player.id.toString() + "," + Constant.ETAT_PORTEUR.toString() + ",0");

        var e: ArrayList<Map.Entry<Int,Fighter>> = ArrayList(this.team1.entries)
        for (entry in  e) {
            var f: Fighter = entry.value
            var cell: GameCase = getRandomCell(start1)!!
            if (cell == null) {
                this.team1.remove(f.id);
                continue;
            }
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.id.toString() + "", f.id.toString() + "," + Constant.ETAT_PORTE.toString() + ",0");
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.id.toString() + "", f.id.toString() + "," + Constant.ETAT_PORTEUR.toString() + ",0");
            f.cell = cell;
            f.cell!!.addFighter(f);
            f.team = 1;
            f.fullPdv();
        }
        this.init0.cell = getRandomCell(start0);
        this.init0.getPlayer()!!.curCell.removePlayer(this.init0.getPlayer()!!);
        this.init0.cell!!.addFighter(init0);
        this.init0.getPlayer()!!.fight = this;
        this.init0.team = 0;
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.init0.getPlayer()!!.curMap, this.init0.id);
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.init0.getPlayer()!!.curMap, prism.id);

        var c: Int = PathFinding.getNearestCellAround(this.init0.getPlayer()!!.curMap, this.init0.getPlayer()!!.curCell.cellId, prism.cell, ArrayList())
        if (c < 0)
            c = this.init0.getPlayer()!!.curCell.cellId;

        SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(this.init0.getPlayer()!!.curMap, 0, this.init0.id, prism.id, c, "0;" + this.init0.getPlayer()!!.alignment, prism.cell, "0;" + prism.alignment);
        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(this.init0.getPlayer()!!.curMap, this.init0.id, init0);
        for (f in  this.team1.values)
            SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(this.init0.getPlayer()!!.curMap, prism.id, f);
        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this, 7, map!!);

        mapOld.applyInitFightAction(this);
        state = Constant.FIGHT_STATE_PLACE;
        var str: String = ""
        if (prism != null)
            str = prism.map.toString() + "|" + prism.x.toString() + "|" + prism.y;
        for (z in  World.world.players) {
            if (z == null)
                continue;
            if (z.alignment != prism.alignment)
                continue;
            SocketManager.SEND_CA_ATTAQUE_MESSAGE_PRISME(z, str);
        }
    }

    // BOUFBWAL
    constructor(id: Int, perso: Player, init2: Player) {
        if (perso.curMap.id != 9862)
            throw IllegalStateException("wrong map for boufbawl fight");
        perso.setFullMorphbouf(0);
        init2.setFullMorphbouf(1);


        SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(perso.curMap, "", -1, "Boumawa", "fight.fight.boufbwal.start.fight");

        launchTime = System.currentTimeMillis();
        type = 7;
        this.id = id;
        map = perso.curMap.getMapCopy();
        this.mapOld = perso.curMap;
        this.init0 = Fighter.NewPlayer(this, perso);
        this.init1 = Fighter.NewPlayer(this, init2);
        this.team0.put(perso.id, init0);
        this.team1.put(init2.id, init1);


        var mob: Fighter = Fighter.NewMob(215, this, World.world.getMonstre(2931)!!.getGradeByLevel(3)!!)
        mob.setStatic(true);
        this.team1.put(215, mob);

        SocketManager.GAME_SEND_GDF_PACKET_TO_FIGHT(perso, this.map!!.cases);
        //on desactive le timer de regen cot� client
        var cancelBtn: Int = 1
        var time: Long = 60000
        scheduleTimer(60);
        SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(this, 7, 2, cancelBtn, 1, 0, time, 0);


        var morph: Int = perso.gfxId
        if (morph == 1109 || morph == 1046 || morph == 9001) {
            perso.unsetFullMorph();
            SocketManager.GAME_SEND_ALTER_GM_PACKET(perso.curMap, perso);
        }


        this.start0 = map!!.places.get(0).stream().map(map!!::getCase).collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        this.start1 = map!!.places.get(1).stream().map(map!!::getCase).collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, map!!.places, 0);
        SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 2, map!!.places, 1);
        st1 = 0;
        st2 = 1;

        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.id.toString() + "", perso.id.toString() + "," + Constant.ETAT_PORTE.toString() + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.id.toString() + "", perso.id.toString() + "," + Constant.ETAT_PORTEUR.toString() + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init2.id.toString() + "", init2.id.toString() + "," + Constant.ETAT_PORTE.toString() + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init2.id.toString() + "", init2.id.toString() + "," + Constant.ETAT_PORTEUR.toString() + ",0");


        mob.cell = this.map!!.getCase(239);
        start0.remove(this.map!!.getCase(239));
        start1.remove(this.map!!.getCase(239));
        this.init0.cell = getRandomCell(this.start0);
        this.init1.cell = getRandomCell(this.start1);


        this.init0.getPlayer()!!.curCell.removePlayer(this.init0.getPlayer()!!);
        this.init1.getPlayer()!!.curCell.removePlayer(this.init1.getPlayer()!!);

        this.init0.cell!!.addFighter(init0);
        this.init1.cell!!.addFighter(init1);
        mob.cell!!.addFighter(mob);
        this.init0.getPlayer()!!.fight = this;
        this.init0.team = 0;
        this.init1.getPlayer()!!.fight = this;
        this.init1.team = 1;
        mob.team = 1;


        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.init0.getPlayer()!!.curMap, this.init0.id);
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.init1.getPlayer()!!.curMap, this.init1.id);
        if (type == 1) {
            SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(this.init0.getPlayer()!!.curMap, 0, this.init0.id, this.init1.id, this.init0.getPlayer()!!.curCell.cellId, "0;" + this.init0.getPlayer()!!.alignment, this.init1.getPlayer()!!.curCell.cellId, "0;" + this.init1.getPlayer()!!.alignment);
        } else {
            SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(this.init0.getPlayer()!!.curMap, 0, this.init0.id, this.init1.id, this.init0.getPlayer()!!.curCell.cellId, "0;-1", this.init1.getPlayer()!!.curCell.cellId, "0;-1");
        }
        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(this.init0.getPlayer()!!.curMap, this.init0.id, init0);
        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(this.init0.getPlayer()!!.curMap, this.init1.id, init1);

        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this, 7, map!!);

        mapOld.applyInitFightAction(this);
        state = Constant.FIGHT_STATE_PLACE;
    }























    fun resetCurFighterUsedPa() {
        this.curFighterUsedPa = 0;
    }


    fun resetCurFighterUsedPm() {
        this.curFighterUsedPm = 0;
    }

    fun getTeam(team: Int): MutableMap<Int,Fighter> {
        when (team){  1 -> {return team0;
}
2 -> {return team1;
        
}
}
        return team0;
    }




    fun removeDead(target: Fighter): Boolean {
        return deadList.remove(target);
    }








    fun checkTraps(fighter: Fighter) {
        val nbr: ShortArray = shortArrayOf(0)
        var traps: MutableList<Trap> = this.traps.stream().filter({ trap -> trap.spell != 73 }).collect(Collectors.toList())
        traps.addAll(this.traps.stream().filter({ trap -> trap.spell == 73 }).collect(Collectors.toList()));
        this.recursiveCheckTrap(traps, 0, traps.size, fighter, nbr);
    }

    private fun recursiveCheckTrap(traps: List<Trap>, i: Int, size: Int, fighter: Fighter, nbr: ShortArray) {
        if (i < size) {
            val trap: Trap = traps.get(i)
            var time: Int = 0

            if (trap != null && PathFinding.getDistanceBetween(this.map, trap.cell.cellId, fighter.cell!!.cellId) <= trap.size) {
                trap.onTrapped(fighter);
                time = 750 + nbr[0] * 300;
                nbr[0]++;
                if (this.state == Constant.FIGHT_STATE_FINISHED)
                    return;
            }
            TimerWaiter.addNext({ recursiveCheckTrap(traps, i + 1, size, fighter, nbr) }, time.toLong());
        }
    }






















    fun setHaveKnight() {
        this.haveKnight = true;
    }


    fun setBegin() {
        this.isBegin = true;
    }





    fun getMobGroup(): MonsterGroup {
        return monsterGroup;
    }

    fun setMobGroup(monsterGroup: MonsterGroup) {
        this.monsterGroup = monsterGroup;
    }









    fun getDefenders(): String {
        return defenders;
    }

    fun setDefenders(defenders: String) {
        this.defenders = defenders;
    }

    fun getTrainerWinner(): Int {
        return trainerWinner;
    }

    fun setTrainerWinner(trainerWinner: Int) {
        this.trainerWinner = trainerWinner;
    }

    fun isFinish(): Boolean {
        return finish;
    }

    fun getTeamId(guid: Int): Int {
        if (this.team0.containsKey(guid))
            return 1;
        if (this.team1.containsKey(guid))
            return 2;
        if (viewer.containsKey(guid))
            return 4;
        return -1;
    }

    fun getOtherTeamId(guid: Int): Int {
        if (this.team0.containsKey(guid))
            return 2;
        if (this.team1.containsKey(guid))
            return 1;
        return -1;
    }

    fun scheduleTimer(time: Int) {
        TimerWaiter.addNext({ {
            if (this.isBegin) {
                if (this.state != Constant.FIGHT_STATE_ACTIVE)
                    this.startFight();
            }
        } }, time.toLong(), TimeUnit.SECONDS);
    }

    private fun demorph(p: Player) {
        if (p.morphMode && p.isMorph() && p.getGroup() == null && p.morphId != 8006 && p.morphId != 8007 && p.morphId != 8009)
            p.unsetMorph();
    }

    fun startFight() {
        this.launchTime = -1;
        this.startTime = System.currentTimeMillis();
        if (this.collector != null && this.collectorProtect) {
            var protectors: ArrayList<Player> = ArrayList(collector.defenseFight.values)
            for (player in  protectors) {
                if (player.fight == null && player.away) {
                    player.setOldPosition();

                    if (player.curMap.id != this.mapOld.id) {
                        player.teleport(this.mapOld.id, this.collector.cell);
                    }

                    TimerWaiter.addNext({ this.joinCollectorFight(player!!, collector) }, 1000);
                } else {
                    SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("fight.startfight.collector.error"));
                    collector.delDefenseFight(player);
                }
                player.send("gITP-" + collector.id.toString() + "|" + Integer.toString(player.id, 36));
            }

            this.collectorProtect = true;
            this.scheduleTimer(15);

            this.mapOld.applyStartFightAction(this);
            return;
        }

        if (state >= Constant.FIGHT_STATE_ACTIVE)
            return;

        if (this.type == Constant.FIGHT_TYPE_PVM) {
            if (this.getMobGroup().isFix && checkTimer && this.mapOld.id != 6826 && this.mapOld.id != 10332 && this.mapOld.id != 7388)
                this.mapOld.spawnAfterTimeGroupFix(this.getMobGroup().cellId);// Respawn d'un groupe fix
            if (Config.modeHeroic)
                if (this.getMobGroup().isFix && this.checkTimer)
                    this.mapOld.spawnAfterTimeGroup();// Respawn d'un groupe
        }

        if (type == Constant.FIGHT_TYPE_CONQUETE) {
            for (z in  World.world.players) {
                if (z == null)
                    continue;
                if (z.alignment == prism.alignment) {
                    Prism.parseAttack(z);
                    Prism.parseDefense(z);
                }
            }
        }

        if (type == Constant.FIGHT_TYPE_PVT && collector != null)
            collector.inFight = 2.toByte();

        state = Constant.FIGHT_STATE_ACTIVE;
        startTime = System.currentTimeMillis();
        SocketManager.GAME_SEND_GAME_REMFLAG_PACKET_TO_MAP(mapOld, this.init0.id);

        for (fighter in  this.getFighters(3)) {
            var player: Player = fighter.getPlayer()!!

            if (player != null) {
                player.refreshObjectsClass();
            }
        }

        if (haveKnight && type == Constant.FIGHT_TYPE_AGRESSION)
            addChevalier();

        this.checkTimer = false;
        SocketManager.GAME_SEND_GIC_PACKETS_TO_FIGHT(this, 7);
        SocketManager.GAME_SEND_GS_PACKET_TO_FIGHT(this, 7);
        initOrderPlaying();
        curPlayer = -1;
        SocketManager.GAME_SEND_GTL_PACKET_TO_FIGHT(this, 7);
        SocketManager.GAME_SEND_GTM_PACKET_TO_FIGHT(this, 7);

        /** Challenges **/
        if (type == Constant.FIGHT_TYPE_PVM
                || type == Constant.FIGHT_TYPE_DOPEUL) {
            var hasMale: Boolean = false
            var hasFemale: Boolean = false
            var hasIndirectDamage: Boolean = false
            var hasCawotte: Boolean = false
            var hasChafer: Boolean = false
            var hasRoulette: Boolean = false
            var hasArakne: Boolean = false
            var hasArround: Boolean = false
            var severalEnnemies: Boolean = false
            var severalAllies: Boolean = false
            var bothSexes: Boolean = false
            var EvenEnnemies: Boolean = false
            var MoreEnnemies: Boolean = false
            var ecartLvlPlayer: Boolean = false
            var hasBoss: Int = -1

            if (this.team0.size > 1) {
                var lowLvl1: Int = 201
                var lowLvl2: Int = 201

                for (fighter in  this.team0.values)
                    if (fighter.getLvl() < lowLvl1)
                        lowLvl1 = fighter.getLvl();
                for (fighter in  this.team0.values)
                    if (fighter.getLvl() < lowLvl2
                            && fighter.getLvl() > lowLvl1)
                        lowLvl2 = fighter.getLvl();
                if (lowLvl2 - lowLvl1 > 10)
                    ecartLvlPlayer = true;
            }

            for (f in  this.team0.values) {
                var player: Player = f.getPlayer()!!
                if (f.getPlayer() != null) {
                    when (player.classe) {
Constant.CLASS_OSAMODAS, Constant.CLASS_FECA, Constant.CLASS_SADIDA, Constant.CLASS_XELOR, Constant.CLASS_SRAM -> {hasIndirectDamage = true;
                            
}
}

                    if (player.hasSpell(367))
                        hasCawotte = true;
                    if (player.hasSpell(373))
                        hasChafer = true;
                    if (player.hasSpell(101))
                        hasRoulette = true;
                    if (player.hasSpell(370))
                        hasArakne = true;
                    if (player.sexe == 0)
                        hasMale = true;
                    if (player.sexe == 1)
                        hasFemale = true;
                }
            }

            for (fighter in  this.team1.values) {
                if (fighter.getMob() != null) {
                    if (fighter.getMob()!!.template != null) {
                        if (fighter.getMob()!!.template.isBoss)
                            hasBoss = fighter.getMob()!!.template.id;
                        for (fighter2 in  this.team0.values)
                            if (PathFinding.getDistanceBetween(this.map, fighter2.cell!!.cellId, fighter.cell!!.cellId) >= 5)
                                hasArround = true;

                    }
                }
            }

            for (fighter in  this.team1.values) {
                if (fighter.getMob() != null) {
                    if (fighter.getMob()!!.template != null) {
                        when (fighter.getMob()!!.template.id) {
98, 111, 120, 382, 473, 794, 796, 800, 801, 803, 805, 806, 807, 808, 841, 847, 868, 970, 171, 200, 666, 582 -> {hasArround = false;
                                
}
}
                    }
                }
            }

            severalEnnemies = this.team1.size >= 2;
            severalAllies = this.team0.size >= 2;
            bothSexes = !(hasMale || hasFemale);
            EvenEnnemies = this.team1.size % 2 == 0;
            MoreEnnemies = this.team1.size >= this.team0.size;

            var challenges: String = World.world.getChallengeFromConditions(severalEnnemies, severalAllies, bothSexes, EvenEnnemies, MoreEnnemies, hasCawotte, hasChafer, hasRoulette, hasArakne, hasBoss, ecartLvlPlayer, hasArround, hasIndirectDamage, this.team0.size != 1)
            lateinit var chalInfo: Array<String>

            var challengeID: Int = 0
            var challengeXP: Int = 0
            var challengeDP: Int = 0
            var bonusGroupe: Int = 0
            /* FIXME: Check if in dungeon */
            var challengeNumber: Int = if (false || SoulStone.isInArenaMap(this.mapOld.id)) 2 else 1

            for (chalInfos in  World.world.getRandomChallenge(challengeNumber, challenges)) {
                chalInfo = chalInfos.split(",").toTypedArray();
                challengeID = Integer.parseInt(chalInfo[0]);
                challengeXP = Integer.parseInt(chalInfo[1]);
                challengeDP = Integer.parseInt(chalInfo[2]);
                bonusGroupe = Integer.parseInt(chalInfo[3]);
                bonusGroupe *= this.team1.size;
                allChallenges.put(challengeID, Challenge(this, challengeID, challengeXP + bonusGroupe, challengeDP + bonusGroupe));
            }
            for (c in  allChallenges.entries) {
                if (c.value == null)
                    continue;
                c.value.fightStart();
                SocketManager.GAME_SEND_CHALLENGE_FIGHT(this, 1, c.value.parseToPacket());
            }
        }
        /** Challenges **/

        for (F in  getFighters(3)) {
            var player: Player = F.getPlayer()!!
            if (player != null) {
                player.ready = false;
                if (player.onMount)
                    SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, player.id.toString() + "", player.id.toString() + "," + Constant.ETAT_CHEVAUCHANT.toString() + ",1");
            }
        }

        this.mapOld.applyStartFightAction(this);

        this.startTurn();
        this.getFighters(3).stream().filter { it != null }.forEach { F -> rholBack.put(F.id, F.cell!!)  };
        this.setBegin();
    }

    fun leftFight(playerCaster: Player?, playerTarget: Player?) {
        if (playerCaster == null)
            return;

        val caster: Fighter? = this.getFighterByPerso(playerCaster)
        val target: Fighter? = if (playerTarget != null) this.getFighterByPerso(playerTarget) else null

        if (caster != null) {
            if (!(type == 7 && this.state >= Constant.FIGHT_STATE_ACTIVE))
            when (type) {
                7, Constant.FIGHT_TYPE_ROYAL, Constant.FIGHT_TYPE_CHALLENGE, Constant.FIGHT_TYPE_AGRESSION, Constant.FIGHT_TYPE_PVM, Constant.FIGHT_TYPE_PVT, Constant.FIGHT_TYPE_CONQUETE, Constant.FIGHT_TYPE_DOPEUL -> {
                    if (this.state >= Constant.FIGHT_STATE_ACTIVE) {
                        //region Active
                        caster.setLeft(true);
                        this.onFighterDie(caster, caster);

                        if (this.getFighterByGameOrder() != null && this.getFighterByGameOrder()!!.id == caster.id)
                            endTurn(false, caster);

                        val player: Player = caster.getPlayer()!!
                        player.duelId = -1;
                        player.ready = false;
                        player.fight = null;
                        player.away = false;

                        this.verifIfTeamAllDead();


                        if (this.finish) {
                            this.onPlayerLoose(caster);
                            SocketManager.GAME_SEND_GV_PACKET(caster.getPlayer()!!);
                        }
                        //endregion
                    } else if (state == Constant.FIGHT_STATE_PLACE) {
                        //region Place
                        var isValid: Boolean = false
                        if (target != null) {
                            if (init0 != null && this.init0.getPlayer() != null && caster.getPlayer()!!.id == this.init0.getPlayer()!!.id)
                                isValid = true;
                            if (init1 != null && this.init1.getPlayer() != null && caster.getPlayer()!!.id == this.init1.getPlayer()!!.id)
                                isValid = true;
                        }

                        if (isValid) {// Celui qui fait l'action a lancer le combat et leave un autre personnage
                            //region Cas 0
                            if (target!!.team == caster.team && target.id != caster.id) {
                                SocketManager.GAME_SEND_ON_FIGHTER_KICK(this, target.getPlayer()!!.id, getTeamId(target.id));

                                if (type == Constant.FIGHT_TYPE_AGRESSION || type == Constant.FIGHT_TYPE_CHALLENGE || type == Constant.FIGHT_TYPE_PVT || type == Constant.FIGHT_TYPE_CONQUETE || type == Constant.FIGHT_TYPE_DOPEUL)
                                    SocketManager.GAME_SEND_ON_FIGHTER_KICK(this, target.getPlayer()!!.id, getOtherTeamId(target.id));

                                val player2: Player = target.getPlayer()!!
                                player2.duelId = -1;
                                player2.ready = false;
                                player2.fight = null;
                                player2.away = false;

                                if (player2.isOnline)
                                    SocketManager.GAME_SEND_GV_PACKET(player2);

                                if (this.type == 7) {
                                    player2.unsetFullMorphbouf();
                                    player2.fullPDV();
                                    player2.refreshLife(false);
                                }

                                // On le supprime de la team
                                if (this.team0.containsKey(target.id)) {
                                    target.cell!!.removeFighter(target);
                                    this.team0.remove(target.id);
                                    SocketManager.GAME_SEND_REMOVE_IN_TEAM_PACKET_TO_MAP(mapOld, this.init0.id, target!!);
                                } else if (this.team1.containsKey(target.id)) {
                                    target.cell!!.removeFighter(target);
                                    this.team1.remove(target.id);
                                    SocketManager.GAME_SEND_REMOVE_IN_TEAM_PACKET_TO_MAP(mapOld, this.init1.id, target!!);
                                }
                            }
                            //endregion
                        } else if (target == null) {// Il leave de son plein gr\u00e9 donc (target = null)
                            var isValid2: Boolean = false
                            if (this.init0 != null && this.init0.getPlayer() != null && caster.getPlayer()!!.id == this.init0.getPlayer()!!.id)
                                isValid2 = true;
                            if (this.init1 != null && this.init1.getPlayer() != null && caster.getPlayer()!!.id == this.init1.getPlayer()!!.id)
                                isValid2 = true;

                            if (isValid2) {// Soit il a lancer le combat => annulation du combat
                                //region Cas 1
                                for (fighter in this.getFighters(caster.getTeam2())) {
                                    val player: Player = fighter.getPlayer()!!
                                    player.duelId = -1;
                                    player.ready = false;
                                    player.fight = null;
                                    player.away = false;
                                    fighter.setLeft(true);

                                    if (caster.getPlayer()!!.id != fighter.getPlayer()!!.id) {// Celui qui a join le fight revient sur la map
                                        if (player.isOnline)
                                            SocketManager.GAME_SEND_GV_PACKET(player);
                                    } else {// Celui qui a fait le fight meurt + perte honor
                                        if (type == Constant.FIGHT_TYPE_ROYAL || this.type == Constant.FIGHT_TYPE_AGRESSION || type == Constant.FIGHT_TYPE_PVM || type == Constant.FIGHT_TYPE_PVT || type == Constant.FIGHT_TYPE_CONQUETE) {
                                            val looseEnergy: Int = Formulas.getLoosEnergy(player.level, type == 1, type == 5)
                                            var totalEnergy: Int = player.energy - looseEnergy;
                                            if (totalEnergy < 0) totalEnergy = 0;

                                            player.energy = totalEnergy;
                                            player.setMascotte(0);

                                            if (player.isOnline)
                                                SocketManager.GAME_SEND_Im_PACKET(player, "034;" + looseEnergy);

                                            if (caster.getPlayer()!!.getObjetByPos(Constant.ITEM_POS_FAMILIER) != null) {
                                                val obj: GameObject? = caster.getPlayer()!!.getObjetByPos(Constant.ITEM_POS_FAMILIER)
                                                if (obj != null) {
                                                    val pets: PetEntry? = World.world.getPetsEntry(obj.guid)
                                                    if (pets != null)
                                                        pets.looseFight(caster.getPlayer()!!);
                                                }
                                            }

                                            this.onPlayerLooseHonor(player);

                                            val energy: Int = totalEnergy

                                            if (energy == 0) {
                                                if (type == Constant.FIGHT_TYPE_AGRESSION) {
                                                    for (enemy in (if (this.team1.containsValue(caster)) this.team0 else this.team1).values) {
                                                        if (enemy.getPlayer() != null && enemy.getPlayer()!!.stalk != null) {
                                                            if (enemy.getPlayer()!!.stalk!!.target == caster.getPlayer()) {
                                                                player.teleportFaction(enemy.getPlayer()!!.alignment);
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    TimerWaiter.addNext(player::setFuneral, 1, TimeUnit.SECONDS);
                                                } else {
                                                    TimerWaiter.addNext(player::setFuneral, 1, TimeUnit.SECONDS);
                                                }
                                            } else {
                                                if (type == Constant.FIGHT_TYPE_AGRESSION) {
                                                    for (enemy in (if (this.team1.containsValue(caster)) this.team0 else this.team1).values) {
                                                        if (enemy.getPlayer() != null) {
                                                            if (enemy.getPlayer()!!.stalk!!.target == caster.getPlayer()) {
                                                                player.teleportFaction(enemy.getPlayer()!!.alignment);
                                                                break;
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    if (player.isOnline) {
                                                        player.warpToSavePos();
                                                    } else {
                                                        player.setNeededEndFightAction(this, Action(1001, player.savePos.toString(","), ""));
                                                    }
                                                }
                                                player.setPdv(1);
                                            }
                                        }

                                        if (player.isOnline)
                                            SocketManager.GAME_SEND_GV_PACKET(player);
                                    }
                                }

                                if (type == Constant.FIGHT_TYPE_ROYAL || type == Constant.FIGHT_TYPE_AGRESSION || type == Constant.FIGHT_TYPE_CHALLENGE || type == Constant.FIGHT_TYPE_PVT || type == Constant.FIGHT_TYPE_CONQUETE) {
                                    for (f in this.getFighters(caster.getOtherTeam())) {
                                        if (f.getPlayer() == null)
                                            continue;
                                        val player: Player = f.getPlayer()!!

                                        player.duelId = -1;
                                        player.ready = false;
                                        player.fight = null;
                                        player.away = false;

                                        if (player.isOnline)
                                            SocketManager.GAME_SEND_GV_PACKET(player);
                                    }
                                }

                                this.state = 4;// Nous assure de ne pas d\u00e9marrer le combat
                                this.mapOld.removeFight(this.id);
                                SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this.mapOld);
                                SocketManager.GAME_SEND_GAME_REMFLAG_PACKET_TO_MAP(this.mapOld, this.init0.id);

                                if (type == Constant.FIGHT_TYPE_PVT) {
                                    // FIXME
                                    World.world.getGuild(guildId)!!.getPlayers().stream().filter { player -> player != null && player.isOnline }.forEach { player ->
                                        SocketManager.GAME_SEND_gITM_PACKET(player, Collector.parseToGuild(player.guild!!.id));
                                        SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("fight.end.collector"));
                                    }

                                    this.collector.inFight = 0.toByte();
                                    this.collector._inFightID = -1;

                                    World.world.getMap(this.collector.map).players.stream().filter { it != null }
                                            .forEach { player -> SocketManager.GAME_SEND_MAP_PERCO_GMS_PACKETS(player!!.gameClient!!, player.curMap) }
                                }
                                map = null;
                                this.orderPlaying = null;
                                //endregion
                            } else {// Soit il a rejoin le combat => Left de lui seul
                                //region Cas 2
                                SocketManager.GAME_SEND_ON_FIGHTER_KICK(this, caster.getPlayer()!!.id, getTeamId(caster.id));

                                if (type == Constant.FIGHT_TYPE_ROYAL || type == Constant.FIGHT_TYPE_AGRESSION || type == Constant.FIGHT_TYPE_CHALLENGE || type == Constant.FIGHT_TYPE_PVT || type == Constant.FIGHT_TYPE_CONQUETE)
                                    SocketManager.GAME_SEND_ON_FIGHTER_KICK(this, caster.getPlayer()!!.id, getOtherTeamId(caster.id));

                                val player: Player = caster.getPlayer()!!
                                player.duelId = -1;
                                player.ready = false;
                                player.fight = null;
                                player.away = false;
                                caster.setLeft(true);
                                caster.hasLeft();

                                if (type == Constant.FIGHT_TYPE_ROYAL || type == Constant.FIGHT_TYPE_AGRESSION || type == Constant.FIGHT_TYPE_PVM || type == Constant.FIGHT_TYPE_PVT || type == Constant.FIGHT_TYPE_CONQUETE || type == Constant.FIGHT_TYPE_DOPEUL) {
                                    val loosEnergy: Int = Formulas.getLoosEnergy(player.level, type == 1, type == 5)
                                    var totalEnergy: Int = player.energy - loosEnergy;
                                    if (totalEnergy < 0) totalEnergy = 0;

                                    player.energy = totalEnergy;
                                    player.setMascotte(0);

                                    if (player.isOnline)
                                        SocketManager.GAME_SEND_Im_PACKET(player, "034;" + loosEnergy);
                                    if (caster.getPlayer()!!.getObjetByPos(Constant.ITEM_POS_FAMILIER) != null) {
                                        val obj: GameObject? = caster.getPlayer()!!.getObjetByPos(Constant.ITEM_POS_FAMILIER)
                                        if (obj != null) {
                                            val pets: PetEntry? = World.world.getPetsEntry(obj.guid)
                                            if (pets != null)
                                                pets.looseFight(caster.getPlayer()!!);
                                        }
                                    }

                                    this.onPlayerLooseHonor(player);

                                    val energy: Int = totalEnergy

                                    if (energy == 0) {
                                        if (type == Constant.FIGHT_TYPE_AGRESSION) {
                                            for (enemy in (if (this.team1.containsValue(caster)) this.team0 else this.team1).values) {
                                                if (enemy.getPlayer() != null) {
                                                    if (enemy.getPlayer()!!.stalk!!.target == caster.getPlayer()) {
                                                        player.teleportFaction(enemy.getPlayer()!!.alignment);
                                                        break;
                                                    }
                                                }
                                            }
                                            TimerWaiter.addNext(player::setFuneral, 1, TimeUnit.SECONDS);
                                        } else {
                                            TimerWaiter.addNext(player::setFuneral, 1, TimeUnit.SECONDS);
                                        }
                                    } else {
                                        if (type == Constant.FIGHT_TYPE_AGRESSION) {
                                            for (enemy in (if (this.team1.containsValue(caster)) this.team0 else this.team1).values) {
                                                if (enemy.getPlayer() != null) {
                                                    if (enemy.getPlayer()!!.stalk!!.target == caster.getPlayer()) {
                                                        player.teleportFaction(enemy.getPlayer()!!.alignment);
                                                        break;
                                                    }
                                                }
                                            }
                                        } else {
                                            if (type != Constant.FIGHT_TYPE_PVT)
                                                player.setNeededEndFightAction(this, Action(1001, player.savePos.toString(","), ""));
                                        }
                                        player.setPdv(1);
                                    }
                                }

                                if (player.isOnline)
                                    SocketManager.GAME_SEND_GV_PACKET(player);

                                // On le supprime de la team
                                if (this.team0.containsKey(caster.id)) {
                                    caster.cell!!.removeFighter(caster);
                                    this.team0.remove(caster.id);
                                    SocketManager.GAME_SEND_REMOVE_IN_TEAM_PACKET_TO_MAP(mapOld, this.init0.id, target!!);
                                } else if (this.team1.containsKey(caster.id)) {
                                    caster.cell!!.removeFighter(caster);
                                    this.team1.remove(caster.id);
                                    SocketManager.GAME_SEND_REMOVE_IN_TEAM_PACKET_TO_MAP(mapOld, this.init1.id, target!!);
                                }
                                //endregion
                            }
                        }
                        //endregion
                    }
                }
            }
        } else {
            SocketManager.GAME_SEND_GV_PACKET(playerCaster);
            this.viewer.remove(playerCaster.id);
            playerCaster.fight = null;
            playerCaster.away = false;
        }
    }


    private fun onPlayerLooseHonor(player: Player) {
        if (type == Constant.FIGHT_TYPE_AGRESSION || type == Constant.FIGHT_TYPE_CONQUETE) {
            val caster: Fighter = this.getFighterByPerso(player)
            if (caster == null || caster.hasLeft()) return;
            var winners: ArrayList<Fighter> = ArrayList(this.getTeam(if (caster.team == 0) 2 else 1).values)
            var loosers: ArrayList<Fighter> = ArrayList(this.getTeam(caster.team + 1).values)

            var honor: Int = player.honor - Formulas.calculHonorWin(winners, loosers, caster, false)
            if (honor < 0) honor = 0;
            player.honor = honor;
            if (player.isOnline)
                SocketManager.GAME_SEND_Im_PACKET(player, "076;" + honor);
        }
    }

    fun endFight(type: Byte) {
        if (this.launchTime > 1 || this.state == Constant.FIGHT_STATE_INIT)
            return;
        if (this.state == Constant.FIGHT_STATE_PLACE)
            this.startFight();

        var collection: Collection<Fighter>? = if (type == 0.toByte()) this.team0.values else if (type == 1.toByte()) this.team1.values else null
        var fighters: ArrayList<Fighter> = if (collection == null) ArrayList() else ArrayList(collection)
        fighters.stream().filter({ fighter -> fighter != null }).forEach { fighter -> fighter.setIsDead(true)  };

        if (type == 0.toByte()) this.type = -1;
        this.verifIfTeamAllDead();
    }

    fun verifIfTeamBoufbowl(but: Int): Boolean {
        return verifTeamBoufbowl(this, but);
    }

    private fun verifTeamBoufbowl(fight: Fight, but: Int): Boolean {


        if (fight.finish) {
            val fighters: ArrayList<Fighter> = ArrayList()
            fighters.addAll(fight.team0.values);
            fighters.addAll(fight.team1.values);

            fight.finish = true;
            fight.turn!!.stop();
            fight.turn = null;

            try {

                fight.state = Constant.FIGHT_STATE_FINISHED;
                //fight.closeSheduler();

                var winTeam: ArrayList<Fighter> = ArrayList()
                var looseTeam: ArrayList<Fighter> = ArrayList()

                if (but == 1) {
                    looseTeam.addAll(fight.team0.values);
                    winTeam.addAll(fight.team1.values);
                } else {
                    winTeam.addAll(fight.team0.values);
                    looseTeam.addAll(fight.team1.values);
                }

                fight.init0.getPlayer()!!.curMap.removeFight(fight.id);

                var packet: String = getGE(if (but == 1) 2 else 1)

                for (fighter in  fighters) {
                    SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(fight.map!!, fighter.id);
                    if (fighter.hasLeft()) continue;
                    var player: Player = fighter.getPlayer()!!

                    if (player != null && fight.isBegin) {
                        player.fight = null;
                        player.duelId = -1;
                        player.ready = false;

                        player.send(packet);
                    }
                }


                for (player in  fight.viewer.values) {
                    player.refreshMapAfterFight();
                    player.spec = false;
                    SocketManager.send(player, packet);

                    if (player.getAccount().isBanned)
                        player.getGameClient()!!.kick();
                }

                fight.curPlayer = -1;

                for (fighter in  fight.team1.values) {
                    var player: Player = fighter.getPlayer()!!

                    if (player == null)
                        continue;

                    player.duelId = -1;
                    player.ready = false;
                }

                for (fighter in  fight.getFighters(3))
                    fighter.getFightBuff().clear();

                World.world.getMap(fight.map!!.id).removeFight(fight.id);
                SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(World.world.getMap(fight.map!!.id));
                fight.map = null;
                fight.orderPlaying = null;

                try {
                    Thread.sleep(1000);
                } catch (e: InterruptedException) {
                }
            } catch (e: Exception) {
                for (fighter in  fighters) {
                    var player: Player = fighter.getPlayer()!!
                    if (player != null) {
                        player.duelId = -1;
                        player.ready = false;
                        player.fight = null;
                        SocketManager.GAME_SEND_GV_PACKET(player);
                    }
                }
                log.error("Error : (verifIfTeamAllDead) : " + e.message + e.stackTrace[0].getLineNumber());
            }


            for (fighter in  fighters) {
                var player: Player = fighter.getPlayer()!!


                if (player == null)
                    continue;

                if (player.fight != null)
                    player.fight = null;

                player.refreshLife(false);

                if (player.curCell.isWalkableFight())
                    player.teleport(player.curMap, player.curMap.randomFreeCellId);
                if (player.getAccount().isBanned)
                    player.getGameClient()!!.kick();
                if (fighter.isDeconnected())
                    player.getAccount().disconnect(player);
                if (player.morphMode)
                    SocketManager.GAME_SEND_SPELL_LIST(player);
                if (player != null) {
                    try {
                        Thread.sleep(200);
                    } catch (e: InterruptedException) {
                    }
                    player.unsetFullMorphbouf();
                    player.fullPDV();
                    player.refreshLife(false);
                }

            }

            lateinit var name: String
            if (but == 0)
                name = fight.init1.getPlayer()!!.name;
            else
                name = fight.init0.getPlayer()!!.name;
            SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(fight.mapOld, "", -1, "Boumawa", "fight.fight.boufbwal.end.fight");
            var team1: Int = this.getTeamId(this.init1.getPlayer()!!.id)
            var team2: Int = this.getTeamId(this.init0.getPlayer()!!.id)
            SocketManager.GAME_SEND_cMK_PACKET_TO_FIGHT(fight, team1, "", -1, "Boumawa", "Le match est terminé l'équipe gagnante est celle de : " + name.toString() + " .");
            SocketManager.GAME_SEND_cMK_PACKET_TO_FIGHT(fight, team2, "", -1, "Boumawa", "Le match est terminé l'équipe gagnante est celle de : " + name.toString() + " .");
            return true;
        }

        return false;


    }

    fun startTurn() {
        if (verifyStillInFight())
            verifIfTeamAllDead();

        if (state >= Constant.FIGHT_STATE_FINISHED)
            return;

        curPlayer = curPlayer + 1
        curAction = "";

        if (curPlayer >= this.getOrderPlayingSize())
            curPlayer = 0;
        if (curPlayer == 0)
            this.totalTurns++;

        var current: Fighter = this.getFighterByGameOrder()!!

        curFighterPa = current!!.getPa();
        curFighterPm = current!!.getPm();
        curFighterUsedPa = 0;
        curFighterUsedPm = 0;

        if (current!!.hasLeft() || current!!.isDead) {
            this.endTurn(false, current);
            return;
        }

        current!!.applyBeginningTurnBuff(this);

        if (current!!.isDead && current!!.isInvocation()) {
            endTurn(false, this.getFighterByGameOrder()!!);
            return;
        }

        if (state == Constant.FIGHT_STATE_FINISHED)
            return;

        if (current!!.getPdv() <= 0) {
            if (current!!.isTrappedOrGlyphed()) {
                onFighterDie(current, init0);
                endTurn(false, current);
                return;
            }
        }
        // On actualise les sorts launch
        current!!.refreshLaunchedSort();
        // reset des Max des Chatis
        current!!.chatiValue.clear();

        if (current!!.isDead && current!!.isInvocation()) {
            if (current!!.isTrappedOrGlyphed()) {
                endTurn(false, current);
                return;
            }
        }
        if (current!!.getPlayer() != null)
            SocketManager.GAME_SEND_STATS_PACKET(current!!.getPlayer()!!);

        if (current!!.hasBuff(Constant.EFFECT_PASS_TURN) || current!!.getBuff(149) != null && current!!.getBuff(149)!!.spell == 197) {
            endTurn(false, current);
            return;
        }

        var duration: Int = Constant.TIME_BY_TURN

        SocketManager.GAME_SEND_GAMETURNSTART_PACKET_TO_FIGHT(this, 7, current!!.id, duration, this.totalTurns);
        current!!.setCanPlay(true);
        this.turn = Turn(this, current);

        // Gestion des glyphes
        var glyphs: ArrayList<Glyph> = ArrayList(this.glyphs)// Copie du tableau

        for (glyph in  glyphs) {
            if (glyph.caster.id == current!!.id) {
                if (glyph.decrementDuration() == 0) {
                    glyphs.remove(glyph);
                    glyph.disappear();
                    continue;
                }
            }

            if (PathFinding.getDistanceBetween(map, current!!.cell!!.cellId, glyph.cell.cellId) <= glyph.size && glyph.spell != 476)
                glyph.onTrapped(current);
        }


        if (type == Constant.FIGHT_TYPE_PVM && allChallenges.isEmpty() && current!!.isInvocation() && !(current is CloneFighter)
                || type == Constant.FIGHT_TYPE_DOPEUL && allChallenges.isEmpty() && current!!.isInvocation() && !(current is CloneFighter))
            for (challenge in  this.allChallenges.values)
                if (challenge != null)
                    challenge.onPlayerStartTurn(current);

        if (current!!.isDeconnected()) {
            current!!.setTurnRemaining();
            if (current!!.turnRemaining <= 0) {
                if (current!!.getPlayer() != null) {
                    leftFight(current!!.getPlayer(), null);
                    current!!.getPlayer()!!.disconnectInFight();
                } else {
                    onFighterDie(current, current);
                    current!!.setLeft(true);
                }
            } else {
                SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 7, "0162;" + current!!.getPacketsName().toString() + "~" + current!!.turnRemaining);
                //this.endTurn(false, current);
                return;
            }
        }

        if (current!!.hasBuff(140)) {
            this.endTurn(false, current);
            return;
        }

        if (current!!.getPlayer() != null) {
            var party: Party? = current!!.getPlayer()!!.party
            var option: Party.MasterOption? = null
            if (party != null && party.master != null && party.getOptionByPlayer(current!!.getPlayer()!!).also { option = it } != null) {
                if (option!!.passAuto()) {
                    this.endTurn(false, current);
                }
            }
        }

        if(current!!.aiControlled()) {
            IAHandler.select(this, current);
        }
    }

    @Synchronized fun endTurn(onAction: Boolean, f: Fighter) {
        val current: Fighter = this.getFighterByGameOrder()!!
        if (current != null)
            if (f == current)
                this.endTurn(onAction);
    }

    @Synchronized fun endTurn(onAction: Boolean) {
        val current: Fighter = this.getFighterByGameOrder()!!
        if (current == null)
            return;

        try {
            if (this.state >= Constant.FIGHT_STATE_FINISHED)
                return;
            if (this.turn != null)
                this.turn!!.stop();
            if (current.hasLeft() || current.isDead) {
                this.startTurn();
                return;
            }

            if (this.curAction.equals("")) {
                TimerWaiter.addNext({ this.endTurn(onAction, current) }, 100);
                return;
            }

            SocketManager.GAME_SEND_GAMETURNSTOP_PACKET_TO_FIGHT(this, 7, current.id);
            current.setCanPlay(false);
            curAction = "";

            if (onAction) TimerWaiter.addNext({ this.newTurn(current) }, 2100);
            else this.newTurn(current);
        } catch (e: NullPointerException) {
            log.error("unexpected error", e)
            this.endTurn(false);
        }
    }

    private fun newTurn(current: Fighter) {
        // Si empoisonn� (Cr�er une fonction applyEndTurnbuff si
        // d'autres effets existent)
        for (SE in  current.getBuffsByEffectID(131)) {
            var pas: Int = SE.value
            var vale: Int= -1
            try {
                vale = Integer.parseInt(SE.args.split(";")[1]);
            } catch (e: Exception) {
                log.error("unexpected error", e)
            }

            if (vale == -1)
                continue;

            var nbr: Int = (Math.floor((curFighterUsedPa.toDouble())
                    / (pas.toDouble()))).toInt()
            var dgt: Int = vale * nbr
            // Si poison paralysant
            if (SE.spell == 200) {
                var inte: Int = SE.caster!!.getTotalStats().getEffect(Constant.STATS_ADD_INTE)
                if (inte < 0)
                    inte = 0;
                var pdom: Int = SE.caster!!.getTotalStats().getEffect(Constant.STATS_ADD_PERDOM)
                if (pdom < 0)
                    pdom = 0;
                // on applique le boost
                dgt = (100 + inte + pdom) / 100 * dgt;
            }
            if (current.hasBuff(184)) {
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 105, current.id.toString() + "", current.id.toString() + "," + current.getBuff(184)!!.value);
                dgt = dgt - current.getBuff(184)!!.value;// R�duction physique
            }
            if (current.hasBuff(105)) {
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 105, current.id.toString() + "", current.id.toString() + "," + current.getBuff(105)!!.value);
                dgt = dgt - current.getBuff(105)!!.value;// Immu
            }

            if (dgt <= 0)
                continue;
            if (dgt > current.getPdv())
                dgt = current.getPdv();// va mourrir

            current.removePdv(current, dgt);
            dgt = -dgt;
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 100, SE.caster!!.id.toString() + "", current.id.toString() + "," + dgt);
        }

        for (g in  ArrayList(glyphs)) {
            if (state >= Constant.FIGHT_STATE_FINISHED)
                return;
            // Si dans le glyphe
            var dist: Int = PathFinding.getDistanceBetween(map, current.cell!!.cellId, g.cell.cellId)
            if (dist <= g.size && g.spell == 476)// 476 a effet en fin de tour, alors le joueur est dans le glyphe
                g.onTrapped(current);
        }

        if (current.isTrappedOrGlyphed() && current.getPdv() <= 0) {
            onFighterDie(current, init0);
        }

        if (type == Constant.FIGHT_TYPE_PVM && allChallenges.isEmpty() && current.isInvocation()
                && !(current is CloneFighter) && !(current is CollectorFighter) && current.team == 0 || type == Constant.FIGHT_TYPE_DOPEUL
                && allChallenges.isEmpty() && current.isInvocation() && !(current is CloneFighter) && !(current is CollectorFighter) && current.team == 0) {
            for (c in  allChallenges.entries) {
                if (c.value == null)
                    continue;
                c.value.onPlayerEndTurn(current);
            }
        }
        curFighterUsedPa = 0;
        curFighterUsedPm = 0;
        curFighterPa = current.getTotalStats().getEffect(Constant.STATS_ADD_PA);
        curFighterPm = current.getTotalStats().getEffect(Constant.STATS_ADD_PM);
        current.refreshEndTurnBuff();
        if (current.getPlayer() != null)
            if (current.getPlayer()!!.isOnline)
                SocketManager.GAME_SEND_STATS_PACKET(current!!.getPlayer()!!);

        SocketManager.GAME_SEND_GTM_PACKET_TO_FIGHT(this, 7);
        SocketManager.GAME_SEND_GTR_PACKET_TO_FIGHT(this, 7, current.id);
        // Timer d'une seconde a la fin du tour
        this.startTurn();
    }

    fun playerPass(player: Player) {
        val fighter: Fighter = getFighterByPerso(player)
        if (fighter != null)
            if (fighter.canPlay() && this.curAction.isEmpty())
                this.endTurn(false, fighter);
    }

    @Synchronized fun joinFight(perso: Player, guid: Int) {
        var timeRestant: Long = Constant.TIME_START_FIGHT - (System.currentTimeMillis() - launchTime)
        var currentJoin: Fighter? = null

        if (perso.dead == 1.toByte() || isBegin || perso.fight != null)
            return;
        if (type == Constant.FIGHT_TYPE_AGRESSION || type == Constant.FIGHT_TYPE_CONQUETE || this.type == Constant.FIGHT_TYPE_PVT) {
            var multiIp: Boolean = false

            for (f in  this.getFighters(7))
                if (f.getPlayer() != null)
                    if (perso.getAccount().currentIp.compareTo(f.getPlayer()!!.getAccount().currentIp) == 0)
                        multiIp = true;
            if (multiIp) {
                SocketManager.GAME_SEND_MESSAGE(perso, perso.getLang().trans("fight.join.with.sameip"));
                return;
            }
        }

        if (this.team0.containsKey(guid)) {
            var cell: GameCase = getRandomCell(start0)!!
            if (cell == null)
                return;
            if (type == 7) {
                perso.setFullMorphbouf(0);
            }

            if (onlyGroup0) {
                var g: Party = this.init0.getPlayer()!!.party!!
                if (g != null) {
                    if (g.players.contains(perso)) {
                        SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getGameClient()!!, 'f', guid);
                        return;
                    }
                }
            }
            if (type == Constant.FIGHT_TYPE_AGRESSION || type == Constant.FIGHT_TYPE_CONQUETE) {
                var type: Char = if (type == Constant.FIGHT_TYPE_AGRESSION) 'f' else 'a'
                if (perso.alignment == Constant.ALIGNEMENT_NEUTRE) {
                    SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getGameClient()!!, type, guid);
                    return;
                }
                if (this.init0.getPlayer()!!.alignment != perso.alignment) {
                    SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getGameClient()!!, type, guid);
                    return;
                }
                if (perso.showWings)
                    perso.toggleWings('+');
            }
            if (guildId > -1 && perso.getGuild() != null) {
                if (guildId == perso.getGuild()!!.id) {
                    SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getGameClient()!!, 'f', guid);
                    return;
                }
            }
            if (locked0) {
                SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getGameClient()!!, 'f', guid);
                return;
            }
            if (this.team0.size >= 8 || this.start0.size == this.team0.size)
                return;
            if (type == Constant.FIGHT_TYPE_CHALLENGE)
                SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 1, 1, 0, timeRestant, type);
            else
                SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 0, 1, 0, timeRestant, type);

            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(perso.getGameClient()!!, map!!.places, st1);
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.id.toString() + "", perso.id.toString() + "," + Constant.ETAT_PORTE.toString() + ",0");
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.id.toString() + "", perso.id.toString() + "," + Constant.ETAT_PORTEUR.toString() + ",0");
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.curMap, perso.id);
            SocketManager.GAME_SEND_GDF_PACKET_TO_FIGHT(perso, this.map!!.cases);
            var f: Fighter = Fighter.NewPlayer(this, perso)
            currentJoin = f;
            f.team = 0;
            this.team0.put(perso.id, f);
            perso.fight = this;
            f.cell = cell;
            f.cell!!.addFighter(f);
        } else if (this.team1.containsKey(guid)) {
            if (type == 7) {
                perso.setFullMorphbouf(1);
            }
            var cell: GameCase = getRandomCell(start1)!!
            if (cell == null)
                return;
            if (onlyGroup1) {
                var g: Party = this.init1.getPlayer()!!.party!!
                if (g != null) {
                    if (g.players.contains(perso)) {
                        SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getGameClient()!!, 'f', guid);
                        return;
                    }
                }
            }
            if (type == Constant.FIGHT_TYPE_AGRESSION || type == Constant.FIGHT_TYPE_CONQUETE) {
                var type: Char = if (type == Constant.FIGHT_TYPE_AGRESSION) 'f' else 'a'
                if (perso.alignment == Constant.ALIGNEMENT_NEUTRE) {
                    SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getGameClient()!!, type, guid);
                    return;
                }
                if (this.init1.getPlayer()!!.alignment != perso.alignment) {
                    SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getGameClient()!!, type, guid);
                    return;
                }
                if (perso.showWings)
                    perso.toggleWings('+');
            }
            if (guildId > -1 && perso.getGuild() != null) {
                if (guildId == perso.getGuild()!!.id) {
                    SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getGameClient()!!, 'f', guid);
                    return;
                }
            }
            if (locked1) {
                SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getGameClient()!!, 'f', guid);
                return;
            }
            if (this.team1.size >= 8 || this.start1.size == this.team0.size)
                return;
            if (type == Constant.FIGHT_TYPE_CHALLENGE)
                SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 1, 1, 0, 0, type);
            else
                SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 0, 1, 0, 0, type);

            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(perso.getGameClient()!!, map!!.places, st2);
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.id.toString() + "", perso.id.toString() + "," + Constant.ETAT_PORTE.toString() + ",0");
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.id.toString() + "", perso.id.toString() + "," + Constant.ETAT_PORTEUR.toString() + ",0");
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.curMap, perso.id);

            var f: Fighter = Fighter.NewPlayer(this, perso)
            currentJoin = f;
            f.team = 1;
            this.team1.put(perso.id, f);
            perso.fight = this;
            f.cell = cell;
            f.cell!!.addFighter(f);
        }

        demorph(perso);

        if (this.team0.containsKey(guid) && type == 7)
            perso.setFullMorphbouf(0);

        else if (this.team0.containsKey(guid) && type == 7)
            perso.setFullMorphbouf(1);


        if (currentJoin == null) return;
        perso.curCell.removePlayer(perso);

        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(perso.curMap, (if (currentJoin.team == 0) init0 else init1).id, currentJoin);
        SocketManager.GAME_SEND_FIGHT_PLAYER_JOIN(this, 7, currentJoin);
        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(this, map!!, perso);

        if (collector != null) {
            World.world.getGuild(guildId)!!.getPlayers().stream().filter { it.isOnline }.forEach { z -> 
                Collector.parseAttaque(z, guildId);
                Collector.parseDefense(z, guildId);
            }
        }
        if (prism != null)
            World.world.players.stream().filter { it != null }.filter({ z -> z.alignment == prism.alignment }).forEach { Prism.parseAttack(perso) }
    }

    @Synchronized private fun joinCollectorFight(player: Player, collector: Collector) {
        val cell: GameCase = getRandomCell(this.start1)!!

        if (cell == null)
            return;

        SocketManager.GAME_SEND_GJK_PACKET(player, 2, 0, 1, 0, 0, type);
        SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(player.getGameClient()!!, map!!.places, st2);
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, player.id.toString() + "", player.id.toString() + "," + Constant.ETAT_PORTE.toString() + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, player.id.toString() + "", player.id.toString() + "," + Constant.ETAT_PORTEUR.toString() + ",0");
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(player.curMap, player.id);

        var f: Fighter = Fighter.NewPlayer(this, player)
        f.team = 1;
        this.team1.put(player.id, f);
        player.fight = this;
        f.cell = cell;
        f.cell!!.addFighter(f);
        player.curCell.removePlayer(player);

        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(player.curMap!!, collector.id, f!!);
        SocketManager.GAME_SEND_FIGHT_PLAYER_JOIN(this, 7, f);
        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(this, map!!, player);
        SocketManager.GAME_SEND_GDF_PACKET_TO_FIGHT(player, this.map!!.cases);
    }

    fun joinPrismFight(player: Player, team: Int) {
        val cell: GameCase = getRandomCell(if (team == 1) this.start1 else this.start0)!!

        if (cell == null)
            return;

        var prismTeam: Int = if (this.team0.containsKey(this.prism.id)) 0 else 1

        if (prismTeam == team) {
            if (player.alignment != this.prism.alignment)
                return;
        } else {
            if (player.alignment == this.prism.alignment)
                return;
        }

        var time: Long = this.launchTime + 60000 - System.currentTimeMillis()
        SocketManager.GAME_SEND_GJK_PACKET(player, 2, 0, 1, 0, time, type);
        SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(player.getGameClient()!!, map!!.places, st2);
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, player.id.toString() + "", player.id.toString() + "," + Constant.ETAT_PORTE.toString() + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, player.id.toString() + "", player.id.toString() + "," + Constant.ETAT_PORTEUR.toString() + ",0");
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(player.curMap, player.id);

        var f: Fighter = Fighter.NewPlayer(this, player)
        f.team = team;
        this.getTeam(team + 1).put(player.id, f);
        player.fight = this;
        f.cell = cell;
        demorph(player);
        f.cell!!.addFighter(f);
        player.curCell.removePlayer(player);

        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(player.curMap, ((this.getTeam(team + 1).values.toTypedArray()[0] as Fighter)).id, f);
        SocketManager.GAME_SEND_FIGHT_PLAYER_JOIN(this, 7, f);
        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(this, map!!, player);
        SocketManager.GAME_SEND_GDF_PACKET_TO_FIGHT(player, this.map!!.cases);
    }

    fun joinAsSpectator(p: Player) {
        val current: Fighter = this.getFighterByGameOrder()!!
        if (current == null)
            return;

        if (isBegin || p.fight != null) {
            SocketManager.GAME_SEND_Im_PACKET(p, "157");
            return;
        }
        if (p.getGroup() == null) {
            if (viewerOk || state != Constant.FIGHT_STATE_ACTIVE) {
                SocketManager.GAME_SEND_Im_PACKET(p, "157");
                return;
            }
        }
        demorph(p);
        p.curCell.removePlayer(p);
        SocketManager.GAME_SEND_GJK_PACKET(p, state, 0, 0, 1, 0, type);
        SocketManager.GAME_SEND_GS_PACKET(p);
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(p.curMap, p.id);
        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(this, map!!, p);
        SocketManager.GAME_SEND_GAMETURNSTART_PACKET(p, current.id, Constant.TIME_BY_TURN);
        SocketManager.GAME_SEND_GTL_PACKET(p, this);

        viewer.put(p.id, p);
        p.spec = true;
        p.fight = this;

        val all: ArrayList<Fighter> = ArrayList()
        all.addAll(this.team0.values);
        all.addAll(this.team1.values);
        all.stream().filter(Fighter::isHidden).forEach { f -> SocketManager.GAME_SEND_GA_PACKET(p, 150, f.id.toString() + "", f.id.toString() + ",4")  };
        if (p.getGroup() == null)
            SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 7, "036;" + p.name);
        if (type == Constant.FIGHT_TYPE_PVM && allChallenges.isEmpty() || type == Constant.FIGHT_TYPE_DOPEUL && allChallenges.isEmpty()) {
            for (c in allChallenges.entries) {
                if (c.value == null)
                    continue;
                SocketManager.GAME_SEND_CHALLENGE_PERSO(p, c.value.parseToPacket());
                if (c.value.loose())
                    c.value.challengeSpecLoose(p);
            }
        }
        for (glyph in this.glyphs) {
            SocketManager.GAME_SEND_GA_PACKET(p, 999, glyph.caster.id.toString() + "",
                    "GDZ+" + glyph.cell.cellId.toString() + ";" + glyph.size.toString() + ";" + glyph.color);
            SocketManager.GAME_SEND_GA_PACKET(p, 999, glyph.caster.id.toString() + "",
                    "GDC" + glyph.cell.cellId.toString() + ";Haaaaaaaaa3005;");
        }
    }

    fun toggleLockTeam(guid: Int) {
        if (init0 != null && this.init0.id == guid) {
            locked0 = locked0;
            SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(this.init0.getPlayer()!!.curMap, if (locked0) '+' else '-', 'A', guid);
            SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 1, if (locked0) "095" else "096");
        } else if (init1 != null && this.init1.id == guid) {
            locked1 = locked1;
            SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(this.init1.getPlayer()!!.curMap, if (locked1) '+' else '-', 'A', guid);
            SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 2, if (locked1) "095" else "096");
        }
    }

    fun toggleLockSpec(player: Player) {
        if (init0 != null && this.init0.id == player.id || init1 != null && this.init1.id == player.id) {
            this.viewerOk = this.viewerOk;

            if (this.viewerOk) {
                ArrayList(this.viewer.values).stream().filter({ target -> target.getGroup() == null }).forEach { target ->
                    SocketManager.GAME_SEND_GV_PACKET(target);
                    this.viewer.remove(target.id);
                    target.fight = null;
                    target.away = false;
                    target.spec = false;
                }
                SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(player.curMap, '+', 'S', player.id);
                this.getFighters(3).stream().filter({ fighter -> fighter.getPlayer() != null }).forEach { fighter -> fighter.getPlayer()!!.send("Im040;" + player.name)  };
            } else {
                SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(player.curMap, '-', 'S', player.id);
                this.getFighters(3).stream().filter({ fighter -> fighter.getPlayer() != null }).forEach { fighter -> fighter.getPlayer()!!.send("Im039;" + player.name)  };
            }
        } else {
            player.send("BN");
        }
    }

    fun toggleOnlyGroup(guid: Int) {
        if (init0 != null && this.init0.id == guid) {
            onlyGroup0 = onlyGroup0;
            SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(this.init0.getPlayer()!!.curMap, if (onlyGroup0) '+' else '-', 'P', guid);
            SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 1, if (onlyGroup0) "093" else "094");
        } else if (init1 != null && this.init1.id == guid) {
            onlyGroup1 = onlyGroup1;
            SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(this.init1.getPlayer()!!.curMap, if (onlyGroup1) '+' else '-', 'P', guid);
            SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 2, if (onlyGroup1) "095" else "096");
        }
    }

    fun toggleHelp(guid: Int) {
        if (init0 != null && this.init0.id == guid) {
            help0 = help0;
            SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(this.init0.getPlayer()!!.curMap, if (help0) '+' else '-', 'H', guid);
            SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 1, if (help0) "0103" else "0104");
        } else if (init1 != null && this.init1.id == guid) {
            help1 = help1;
            SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(this.init1.getPlayer()!!.curMap, if (help1) '+' else '-', 'H', guid);
            SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 2, if (help1) "0103" else "0104");
        }
    }


    fun showCaseToTeam(guid: Int, cellID: Int) {
        var teams: Int = getTeamId(guid) - 1
        if (teams == 4)// Les spectateurs ne montrent pas.
            return;
        var PWs: ArrayList<GameClient> = ArrayList()
        if (teams == 0) {
            PWs.addAll(this.team0.entries.stream().filter({ e -> e.value.getPlayer() != null
                    && e.value.getPlayer()!!.getGameClient() != null }).map({ e -> e.value.getPlayer()!!.getGameClient()!! }).collect(Collectors.toList()));
        } else if (teams == 1) {
            PWs.addAll(this.team1.entries.stream().filter({ e -> e.value.getPlayer() != null
                    && e.value.getPlayer()!!.getGameClient() != null }).map({ e -> e.value.getPlayer()!!.getGameClient()!! }).collect(Collectors.toList()));
        }
        SocketManager.GAME_SEND_FIGHT_SHOW_CASE(PWs, guid, cellID);
    }

    private fun showCaseToAll(guid: Int, cellID: Int) {
        var PWs: ArrayList<GameClient> = ArrayList()
        for (e in  this.team0.entries) {
            if (e.value.getPlayer() != null && e.value.getPlayer()!!.getGameClient() != null)
                PWs.add(e.value.getPlayer()!!.getGameClient()!!);
        }
        for (e in  this.team1.entries) {
            if (e.value.getPlayer() != null
                    && e.value.getPlayer()!!.getGameClient() != null)
                PWs.add(e.value.getPlayer()!!.getGameClient()!!);
        }
        for (e in  viewer.entries) {
            PWs.add(e.value.getGameClient()!!);
        }
        SocketManager.GAME_SEND_FIGHT_SHOW_CASE(PWs, guid, cellID);
    }

    private fun initOrderPlaying() {
        var j: Int = 0
        var k: Int = 0
        var start0: Int = 0
        var start1: Int = 0
        var curMaxIni0: Int = 0
        var curMaxIni1: Int = 0
var curMax0: Fighter? = null
        var curMax1: Fighter? = null
        var team1_ready: Boolean = false
        var team2_ready: Boolean = false

        do {
            if (team1_ready) {
                team1_ready = true;
                var team: Map<Int,Fighter> = this.team0
                for (entry in  team.entries) {
                    if (this.haveFighterInOrdreJeu(entry.value))
                        continue;
                    team1_ready = false;

                    if (entry.value.initiative >= curMaxIni0) {
                        curMaxIni0 = entry.value.initiative;
                        curMax0 = entry.value;
                    }
                    if (curMaxIni0 > start0)
                        start0 = curMaxIni0;
                }
            }
            if (team2_ready) {
                team2_ready = true;
                for (entry in  this.team1.entries) {
                    if (this.haveFighterInOrdreJeu(entry.value))
                        continue;
                    team2_ready = false;
                    if (entry.value.initiative >= curMaxIni1) {
                        curMaxIni1 = entry.value.initiative;
                        curMax1 = entry.value;
                    }
                    if (curMaxIni1 > start1)
                        start1 = curMaxIni1;
                }
            }
            if (curMax1 == null && curMax0 == null) {
                return;
            }
            if (start0 > start1) {
                if (getFighters(1).size > j) {
                    this.orderPlaying!!.add(curMax0!!);
                    j++;
                }
                if (getFighters(2).size > k) {
                    this.orderPlaying!!.add(curMax1!!);
                    k++;
                }
            } else {
                if (getFighters(2).size > j) {
                    this.orderPlaying!!.add(curMax1!!);
                    j++;
                }
                if (getFighters(1).size > k) {
                    this.orderPlaying!!.add(curMax0!!);
                    k++;
                }
            }

            curMaxIni0 = 0;
            curMaxIni1 = 0;
            curMax0 = null;
            curMax1 = null;
        }
        while (this.getOrderPlayingSize() != getFighters(3).size);
    }

    fun tryCaC(perso: Player, cellID: Int) {
        val current: Fighter = this.getFighterByGameOrder()!!
        if (current == null)
            return;

        var caster: Fighter = getFighterByPerso(perso)

        if (caster == null)
            return;
        if (current.id != caster.id)// Si ce n'est pas a lui de jouer
            return;
        if (perso.canCac()) {
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 305, perso.id.toString() + "", "");// Echec Critique Cac
            endTurn(false, current);
            return;
        }
        if (type == Constant.FIGHT_TYPE_PVM && allChallenges.isEmpty() || type == Constant.FIGHT_TYPE_DOPEUL && allChallenges.isEmpty()) {
            for (c in  allChallenges.entries) {
                if (c.value == null)
                    continue;
                c.value.onPlayerCac(current);
            }
        }
        if (perso.getObjetByPos(Constant.ITEM_POS_ARME) == null) {
            tryCastSpell(caster, World.world.getSort(0)!!.getStatsByLevel(1)!!, cellID);
        } else {
            var arme: GameObject = perso.getObjetByPos(Constant.ITEM_POS_ARME)!!
            // Pierre d'�mes = EC
            if (arme.template!!.type == 83) {
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 305, perso.id.toString() + "", "");// Echec Critique Cac
                this.endTurn(false, current);
                return;
            }

            var PACost: Int = arme.template!!.pACost

            if (curFighterPa < PACost) {
                SocketManager.GAME_SEND_Im_PACKET(perso, "1170;" + curFighterPa.toString() + "~" + PACost);
                return;
            }

            var dist: Int = PathFinding.getDistanceBetween(map, caster.cell!!.cellId, cellID)
            var MaxPO: Int = arme.template!!.pOmax
            var MinPO: Int = arme.template!!.pOmin

            if (dist < MinPO || dist > MaxPO) {
                SocketManager.GAME_SEND_Im_PACKET(perso, "1171;" + MinPO.toString() + "~" + MaxPO.toString() + "~" + dist);
                return;
            }

            var isEc: Boolean = arme.template!!.tauxEC != 0 && Formulas.getRandomValue(1, arme.template!!.tauxEC) == arme.template!!.tauxEC
            this.curFighterUsedPa += PACost;

            if (isEc) {
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 305, perso.id.toString() + "", "");// Echec Critique Cac
                endTurn(false, current);
            } else {
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 303, perso.id.toString() + "", cellID.toString() + "");
                var isCC: Boolean = caster.critStrikeCheck(arme.template!!.tauxCC)
                if (isCC) {
                    SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 301, perso.id.toString() + "", "0");
                }

                // Si le joueur est invisible
                if (caster.isHidden()) caster.unHide(-1);
                var effects: ArrayList<SpellEffect> =if (isCC) arme.getCritEffects() else arme.effects
                var targets: ArrayList<Fighter> = PathFinding.getCiblesByZoneByWeapon(this, arme.template!!.type, map!!.getCase(cellID)!!, caster.cell!!.cellId)

                for (SE in  effects) {
                    try {
                        if (state != Constant.FIGHT_STATE_ACTIVE)
                            break;
                        if (this.type != Constant.FIGHT_TYPE_CHALLENGE && this.allChallenges.isEmpty()) {
                            this.allChallenges.values.stream().filter { it != null }.forEach { challenge -> challenge.onFightersAttacked(targets, caster, SE, -1, false)  };
                        }
                        SE.applyToFight(this, caster, targets, true);
                    } catch (e: Exception) {
                        log.error("unexpected error", e)
                    }
                }
                /*
                 * 7172 Baguette Rhon
                 * 7156 Marteau Ronton
                 * 1355 Arc Hidsad
                 * 7182 Racine H�couanone
                 * 7040 Arc de Kuri
                 * 6539 Pelle Gicque
                 * 6519 Baguette de Kouartz
                 * 8118 Baguette du Scarabosse Dor�
                 */
                var idArme: Int = arme.template!!.id
                var basePdvSoin: Int = 1
                var pdvSoin: Int? = null
                if (idArme == 7172 || idArme == 7156 || idArme == 1355 || idArme == 7182 || idArme == 7040 || idArme == 6539 || idArme == 6519 || idArme == 8118) {
                    pdvSoin = Constant.getArmeSoin(idArme);
                    if (pdvSoin != -1) {
                        if (isCC) {
                            basePdvSoin = basePdvSoin + arme.template!!.bonusCC;
                            pdvSoin = pdvSoin + arme.template!!.bonusCC;
                        }
                        var intel: Int = perso.stats.getEffect(Constant.STATS_ADD_INTE) + perso.getStuffStats().getEffect(Constant.STATS_ADD_INTE) + perso.getDonsStats().getEffect(Constant.STATS_ADD_INTE) + perso.getBuffsStats().getEffect(Constant.STATS_ADD_INTE)
                        var soins: Int = perso.stats.getEffect(Constant.STATS_ADD_SOIN) + perso.getStuffStats().getEffect(Constant.STATS_ADD_SOIN) + perso.getDonsStats().getEffect(Constant.STATS_ADD_SOIN) + perso.getBuffsStats().getEffect(Constant.STATS_ADD_SOIN)
                        var minSoin: Int = basePdvSoin * (100 + intel) / 100 + soins
                        var maxSoin: Int = pdvSoin * (100 + intel) / 100 + soins

                        for (target in  targets) {
                            if (target == null) continue;
                            var finalSoin: Int = Formulas.getRandomValue(minSoin, maxSoin)
                            if (finalSoin + target.getPdv() > target.getPdvMax())
                                finalSoin = target.getPdvMax() - target.getPdv();// Target

                            target.removePdv(target, -finalSoin);
                            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 100, target.id.toString() + "", target.id.toString() + ",+" + finalSoin);
                        }
                    }
                }
                curFighterPa = curFighterPa - PACost;
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 102, perso.id.toString() + "", perso.id.toString() + ",-" + PACost);
            }
        }
    }

    @Synchronized fun tryCastSpell(fighter: Fighter, spell: SortStats, cell: Int): Int {
        val current: Fighter = this.getFighterByGameOrder()!!

        if (current == null || spell == null || this.curAction.isEmpty() || current.id != fighter.id) {
            return 10;
        }

        var player: Player? = fighter.getPlayer()
        var Cell: GameCase = map!!.getCase(cell)!!
        curAction = "casting";

        if (this.canCastSpell1(fighter, spell, Cell, -1)) {
            if (fighter.getPlayer() != null)
                SocketManager.GAME_SEND_STATS_PACKET(fighter.getPlayer()!!); // envoi des stats du lanceur
            if (fighter.getType() == 1 && player!!.objectsClassSpell.containsKey(spell.spellID)) {
                var value: Int = player!!.getValueOfClassObject(spell.spellID, 285)
                this.curFighterPa = curFighterPa - (spell.pACost - value);
                this.curFighterUsedPa += spell.pACost - value;
            } else {
                curFighterPa = curFighterPa - spell.pACost;
                this.curFighterUsedPa += spell.pACost;
            }

            var isEc: Boolean = spell.tauxEC != 0 && Formulas.getRandomValue(1, spell.tauxEC) == spell.tauxEC
            if (fighter.hasBuff(782)) isEc = false;

            if (isEc) {
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 302, fighter.id.toString() + "", spell.spellID.toString() + ""); // envoi de  l'EC
            } else {
                if (this.type != Constant.FIGHT_TYPE_CHALLENGE && this.allChallenges.isEmpty() && current.isInvocation() && !(current is CloneFighter) && !(current is CollectorFighter)) {
                    this.allChallenges.values.stream().filter { it != null }
                        .forEach { challenge -> 
                            challenge.onPlayerAction(current, spell.spellID);
                            if (spell.getSpell()!!.id != 0)
                                challenge.onPlayerSpell(current, spell);
                        }}

                var isCC: Boolean = fighter.critStrikeCheck(spell.tauxCC, spell, fighter)
                if (fighter.hasBuff(781)) isCC = false;


                var sort: String = spell.spellID.toString() + "," + cell.toString() + "," + spell.getSpriteID().toString() + "," + spell.level.toString() + "," + spell.getSpriteInfos();
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 300, fighter.id.toString() + "", sort); // xx lance le sort

                if (isCC)
                    SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 301, fighter.id.toString() + "", sort); // CC !
                if (fighter.isHidden()) // Si le joueur est invi, on montre la case
                {
                    if (spell.spellID == 0)// Si le coup est Coup de Poing alors on refait apparaitre le personnage
                        fighter.unHide(cell);
                    else
                        showCaseToAll(fighter.id, fighter.cell!!.cellId);
                }
                spell.applySpellEffectToFight(this, fighter, Cell, isCC, false); // on applique les effets de l'arme
            }
            // le client ne peut continuer sans l'envoi de ce packet qui annonce le co�t en PA
            if (fighter.getType() == 1 && player!!.objectsClassSpell.containsKey(spell.spellID)) {
                var value: Int = player!!.getValueOfClassObject(spell.spellID, 285)
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 102, fighter.id.toString() + "", fighter.id.toString() + ",-" + (spell.pACost - value));
            } else {
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 102, fighter.id.toString() + "", fighter.id.toString() + ",-" + spell.pACost);
            }
            if (isEc)
                fighter.addLaunchedSort(Cell.firstFighter!!, spell, fighter);

            if (isEc && spell.isEcEndTurn) {
                TimerWaiter.addNext({ this.curAction = "" }, 500);

                if (fighter.getMob() != null || fighter.isInvocation()) {
                    return 5;
                } else {
                    endTurn(false, current);
                    return 5;
                }
            }
        } else if (fighter.getMob() != null || fighter.isInvocation()) {
            TimerWaiter.addNext({ this.curAction = "" }, 600);
            return 10;
        }

        this.verifIfTeamAllDead();

        TimerWaiter.addNext({ {
            this.curAction = "";
            if (fighter.getPlayer() != null) {
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 102, fighter.id.toString() + "", fighter.id.toString() + ",-0");
            }
        } }, 1000);
        return 0;
    }

    fun forceCastSpellMob(fighter: Fighter, spell: SortStats, cell: Int) {
        val current: Fighter = this.getFighterByGameOrder()!!
        val Cell: GameCase = map!!.getCase(cell)!!

        if (this.canCastSpellMob(fighter, spell, Cell, -1))
            return;

        val isEc: Boolean = spell.tauxEC != 0 && Formulas.getRandomValue(1, spell.tauxEC) == spell.tauxEC

        if (isEc) {
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 302, fighter.id.toString() + "", spell.spellID.toString() + ""); // envoi de  l'EC
        } else {
            if (this.type != Constant.FIGHT_TYPE_CHALLENGE && this.allChallenges.isEmpty() && current!!.isInvocation() && current is CloneFighter && current is CollectorFighter) {
                this.allChallenges.values.stream().filter { it != null }
                        .forEach { challenge ->
                            challenge.onPlayerAction(current, spell.spellID);
                            if (spell.getSpell()!!.id != 0)
                                challenge.onPlayerSpell(current, spell);
                        }
            }

            val isCC: Boolean = fighter.critStrikeCheck(spell.tauxCC, spell, fighter)
            val sort: String = spell.spellID.toString() + "," + cell.toString() + "," + spell.getSpriteID().toString() + "," + spell.level.toString() + "," + spell.getSpriteInfos()
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 300, fighter.id.toString() + "", sort); // xx lance le sort

            if (isCC)
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 301, fighter.id.toString() + "", sort); // CC !
            if (fighter.isHidden()) // Si le joueur est invi, on montre la case
            {
                if (spell.spellID == 0)// Si le coup est Coup de Poing alors on refait apparaitre le personnage
                    fighter.unHide(cell);
                else
                    showCaseToAll(fighter.id, fighter.cell!!.cellId);
            }
            spell.applySpellEffectToFight(this, fighter, Cell, isCC, false); // on applique les effets de l'arme
        }

        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 102, fighter.id.toString() + "", fighter.id.toString() + ",-" + spell.pACost);

        if (isEc)
            fighter.addLaunchedSort(Cell.firstFighter!!, spell, fighter);

        if (isEc && spell.isEcEndTurn) {
            TimerWaiter.addNext({ this.curAction = "" }, 500);

            if (fighter.getMob() != null || fighter.isInvocation()) {
                return;
            } else {
                endTurn(false, current);
                return;
            }
        }

        this.verifIfTeamAllDead();

        TimerWaiter.addNext({
            this.curAction = "";
            if (fighter.getPlayer() != null) {
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 102, fighter.id.toString() + "", fighter.id.toString() + ",-0");
            }
        }, 1000);
    }


    fun canCastSpell1(caster: Fighter, spell: SortStats, cell: GameCase, targetCell: Int): Boolean {
        val current: Fighter = this.getFighterByGameOrder()!!

        if (current == null)
            return false;

        var casterCell: Int = if (targetCell <= -1) caster.cell!!.cellId else targetCell
        var player: Player = caster.getPlayer()!!

        if (spell == null) {
            if (player != null) {
                SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(this, 7);
                SocketManager.GAME_SEND_Im_PACKET(player, "1169");
                SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, player.id);
            }
            return false;
        }

        if (current.id != caster.id) {
            if (player != null)
                SocketManager.GAME_SEND_Im_PACKET(player, "1175");
            return false;
        }

        if (cell != null) {
            for (effect in  spell.effects) {
                if (effect.effectID == 400) {
                    for (trap in  this.traps) {
                        if (trap.cell.cellId == cell.cellId) {
                            if (caster.getPlayer() != null)
                                caster.getPlayer()!!.send("Im1229");
                            return false;
                        }
                    }
                }
            }
        }

        var usedPA: Int

        if (spell.getSpell()!!.hasInvalidState(caster) || spell.getSpell()!!.hasNeededState(caster))
            return false;

        if (caster.getType() == 1 && player.objectsClassSpell.containsKey(spell.spellID)) {
            var modi: Int = player.getValueOfClassObject(spell.spellID, 285)
            usedPA = spell.pACost - modi;
        } else {
            usedPA = spell.pACost;
        }

        if (curFighterPa < usedPA) {
            if (player != null)
                SocketManager.GAME_SEND_Im_PACKET(player, "1170;" + curFighterPa.toString() + "~" + spell.pACost);
            return false;
        }

        if (cell == null) {
            if (player != null)
                SocketManager.GAME_SEND_Im_PACKET(player, "1172");
            return false;
        }

        if (caster.getType() == 1 && player.objectsClassSpell.containsKey(spell.spellID)) {
            var modi: Int = player.getValueOfClassObject(spell.spellID, 288)
            var modif: Boolean = modi == 1
            if (spell.isLineLaunch && modif && PathFinding.casesAreInSameLine(map!!, casterCell, cell.cellId, 'z', 70)) {
                SocketManager.GAME_SEND_Im_PACKET(player, "1173");
                return false;
            }
        } else if (spell.isLineLaunch && PathFinding.casesAreInSameLine(map!!, casterCell, cell.cellId, 'z', 70)) {
            if (player != null)
                SocketManager.GAME_SEND_Im_PACKET(player, "1173");
            return false;
        }

        var dir: Char = PathFinding.getDirBetweenTwoCase(casterCell, cell.cellId, map, true)

        if (spell.spellID == 67) {
            if (PathFinding.checkLoS(map!!, PathFinding.GetCaseIDFromDirection(casterCell, dir, map, true), cell.cellId, null, true)) {
                if (player != null)
                    SocketManager.GAME_SEND_Im_PACKET(player, "1174");
                return false;
            }
        }

        var hasModification: Boolean = player != null && player.objectsClassSpell.containsKey(spell.spellID)
        if (caster.getType() == 1 && hasModification) {
            var modi: Int = player.getValueOfClassObject(spell.spellID, 289)
            var modif: Boolean = modi == 1
            if (spell.hasLDV && Formulas.checkLos(this.map, (casterCell.toShort()), (cell.cellId.toShort())) && modif) {
                SocketManager.GAME_SEND_Im_PACKET(player, "1174");
                return false;
            }
        }

        if (hasModification && spell.hasLDV && Formulas.checkLos(this.map, (casterCell.toShort()), (cell.cellId.toShort()))) {
            if (player != null)
                SocketManager.GAME_SEND_Im_PACKET(player, "1174");
            return false;
        }

        var dist: Int = PathFinding.getDistanceBetween(map, casterCell, cell.cellId)
        var maxAlc: Int = spell.maxPO
        var minAlc: Int = spell.minPO
        // + porté
        if (caster.getType() == 1 && player != null && player.objectsClassSpell.containsKey(spell.spellID)) {
            var modi: Int = player.getValueOfClassObject(spell.spellID, 281)
            maxAlc = maxAlc + modi;
        }// porté modifiable

        if (caster.getType() == 1 && player != null && player.objectsClassSpell.containsKey(spell.spellID)) {
            var modi: Int = player.getValueOfClassObject(spell.spellID, 282)
            var modif: Boolean = modi == 1
            if (spell.isModifPO || modif) {
                maxAlc += caster.getTotalStats().getEffect(117);
                if (maxAlc <= minAlc)
                    maxAlc = minAlc + 1;
            }
        } else if (spell.isModifPO) {
            maxAlc += caster.getTotalStats().getEffect(117);
            if (maxAlc < minAlc)
                maxAlc = minAlc + 1;
        }

        if (maxAlc < minAlc)
            maxAlc = minAlc;
        if (dist < minAlc || dist > maxAlc) {
            if (player != null)
                SocketManager.GAME_SEND_Im_PACKET(player, "1171;" + minAlc.toString() + "~" + maxAlc.toString() + "~" + dist);
            return false;
        }

        if (LaunchedSpell.cooldownGood(caster, spell.spellID)) {
            return false;
        }

        var numLunch: Int = spell.maxLaunchbyTurn

        if (caster.getType() == 1 && player != null && player.objectsClassSpell.containsKey(spell.spellID))
            numLunch += player.getValueOfClassObject(spell.spellID, 290);

        if (numLunch - LaunchedSpell.getNbLaunch(caster, spell.spellID) <= 0 && numLunch > 0) {
            return false;
        }

        if (this.checkKrakenState(caster, spell)) {
            return false;
        }

        var t: Fighter = cell.firstFighter!!
        var numLunchT: Int = spell.getMaxLaunchByTarget()

        if (caster.getType() == 1 && player != null && player.objectsClassSpell.containsKey(spell.spellID))
            numLunchT += player.getValueOfClassObject(spell.spellID, 291);

        return !(numLunchT - LaunchedSpell.getNbLaunchTarget(caster, t, spell.spellID) <= 0 && numLunchT > 0);
    }

    fun canLaunchSpell(caster: Fighter, spell: SortStats, cell: GameCase?): Boolean {
        if (spell == null || spell.getSpell()!!.hasInvalidState(caster) || spell.getSpell()!!.hasNeededState(caster))
            return false;
        if (curFighterPa < spell.pACost)
            return false;
        if (LaunchedSpell.cooldownGood(caster, spell.spellID))
            return false;
        if (spell.maxLaunchbyTurn - LaunchedSpell.getNbLaunch(caster, spell.spellID) <= 0 && spell.maxLaunchbyTurn > 0)
            return false;
        if (cell == null) return true;
        return !(spell.getMaxLaunchByTarget() -
                LaunchedSpell.getNbLaunchTarget(caster, cell.firstFighter, spell.spellID) <= 0 && spell.getMaxLaunchByTarget() > 0);
    }

    fun canCastSpellMob(caster: Fighter, spell: SortStats, cell: GameCase, targetCell: Int): Boolean {
        val current: Fighter = this.getFighterByGameOrder()!!

        if (current == null || spell == null || current.id != caster.id)
            return false;
        if (spell.getSpell()!!.hasInvalidState(caster) || curFighterPa < spell.pACost || cell == null)
            return false;

        var casterCell: Int = if (targetCell <= -1) caster.cell!!.cellId else targetCell

        if (spell.isLineLaunch && PathFinding.casesAreInSameLine(map!!, casterCell, cell.cellId, 'z', 70))
            return false;
        if (spell.hasLDV && Formulas.checkLos(this.map, (casterCell.toShort()), (cell.cellId.toShort())))
            return false;

        var dist: Int = PathFinding.getDistanceBetween(map, casterCell, cell.cellId)
        var maxAlc: Int = spell.maxPO
        var minAlc: Int = spell.minPO

        if (spell.isModifPO) {
            maxAlc += caster.getTotalStats().getEffect(117);
            if (maxAlc <= minAlc)
                maxAlc = minAlc + 1;
        }

        if (maxAlc < minAlc)
            maxAlc = minAlc;
        if (dist < minAlc || dist > maxAlc)
            return false;
        if (LaunchedSpell.cooldownGood(caster, spell.spellID))
            return false;

        var numLunch: Int = spell.maxLaunchbyTurn

        if (numLunch - LaunchedSpell.getNbLaunch(caster, spell.spellID) <= 0 && numLunch > 0)
            return false;
        if (this.checkKrakenState(caster, spell))
            return false;

        var t: Fighter = cell.firstFighter!!
        var numLunchT: Int = spell.getMaxLaunchByTarget()

        return !(numLunchT - LaunchedSpell.getNbLaunchTarget(caster, t, spell.spellID) <= 0 && numLunchT > 0);
    }

    private fun checkKrakenState(caster: Fighter, spell: SortStats): Boolean {
        when (spell.spellID){ 1106 -> {return caster.haveState(31) && caster.haveState(32) && caster.haveState(33) && caster.haveState(34);
}
1097 -> {return caster.haveState(31);
}
1098 -> {return caster.haveState(32);
}
1099 -> {return caster.haveState(33);
        
}
}
        return true;
    }

    fun onFighterMovement(fighter: Fighter, GA: GameAction): Boolean {
        val current: Fighter = this.getFighterByGameOrder()!!
        if (current == null)
            return false;

        var path: String = GA.args!!
        if (path.equals(""))
            return false;
        if (this.getOrderPlayingSize() <= curPlayer)
            return false;
        if (current.id != fighter.id || this.state != Constant.FIGHT_STATE_ACTIVE)
            return false;

        var targets: List<Fighter> = PathFinding.getEnemiesAround(fighter.cell!!.cellId, map!!, this)
        this.curAction = "deplace";

        if (fighter.haveState(Constant.ETAT_ENRACINE)) {
            for (target in  targets) {
                if (target != null && target.haveState(Constant.ETAT_ENRACINE) && target.haveState(Constant.ETAT_PORTE)) {
                    var esquive: Int = Formulas.getTacleChance(fighter, target)
                    var rand: Int = Formulas.getRandomValue(0, 99)

                    if (rand > esquive) {
                        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, GA.id, "104", fighter.id.toString() + ";", "");
                        var looseAP: Int = curFighterPa * esquive / 100
                        if (looseAP < 0)
                            looseAP = -looseAP;
                        if (curFighterPm < 0)
                            curFighterPm = 0;
                        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, GA.id, "129", fighter.id.toString() + "", fighter.id.toString() + ",-" + curFighterPm);
                        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, GA.id, "102", fighter.id.toString() + "", fighter.id.toString() + ",-" + looseAP);
                        curFighterPm = 0;
                        curFighterPa = curFighterPa - looseAP;
                        this.curAction = "";
                        if (type == Constant.FIGHT_TYPE_PVM && allChallenges.isEmpty() && current.isInvocation() && !(current is CloneFighter) && !(current is CollectorFighter) || type == Constant.FIGHT_TYPE_DOPEUL && allChallenges.isEmpty() && current.isInvocation() && !(current is CloneFighter) && !(current is CollectorFighter))
                            this.allChallenges.values.stream().filter { it != null }.forEach { c -> c.onPlayerMove(fighter, true)  };
                        return false;
                    }
                }
            }
        }
        var pathRef: AtomicReference<String> = AtomicReference(path)
        var nStep: Int = PathFinding.isValidPath(map!!, fighter.cell!!.cellId, pathRef, this, null, -1)
        var newPath: String = pathRef.get()

        if (nStep > curFighterPm || nStep == -1000) {
            if (fighter.getPlayer() != null)
                SocketManager.GAME_SEND_GA_PACKET(fighter.getPlayer()!!.getGameClient()!!, "", "0", "", "");
            this.curAction = "";
            return false;
        }
        curFighterPm = curFighterPm - nStep;
        this.curFighterUsedPm += nStep;

        var nextCellID: Int = World.world.cryptManager.cellCode_To_ID(newPath.substring(newPath.length - 2))
        // les monstres n'ont pas de GAS//GAF
        if (current.getPlayer() != null)
            SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(this, 7, current.id);

        // Si le joueur n'est pas invisible
        if (current.isHidden()) {
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, GA.id, "1", current.id.toString() + "", "a" + CryptManager.cellID_To_Code(fighter.cell!!.cellId) + newPath);
        } else {
            if (current.getPlayer() != null) {
                // On envoie le path qu'au joueur qui se d�place
                var out: GameClient = current.getPlayer()!!.getGameClient()!!
                SocketManager.GAME_SEND_GA_PACKET(out, GA.id.toString() + "", "1", current.id.toString() + "", "a" + CryptManager.cellID_To_Code(fighter.cell!!.cellId) + newPath);
            }
        }

        // Si sur un panda
        val po: Fighter = current.getHoldedBy()!!

        if (po != null) {
            // si le joueur va bouger
            if ((nextCellID) != po.cell!!.cellId) {
                // on retire les �tats
                po.setState(Constant.ETAT_PORTEUR, 0);
                current.setState(Constant.ETAT_PORTE, 0);

                po.setIsHolding(null);
                current.setHoldedBy(null);
                // La nouvelle case sera d�finie plus tard dans le code. On
                // envoie les packets !
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 950, po.id.toString() + "", po.id.toString() + "," + Constant.ETAT_PORTEUR.toString() + ",0");
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 950, current.id.toString() + "", current.id.toString() + "," + Constant.ETAT_PORTE.toString() + ",0");
            }
        }

        current.cell!!.removeFighter(current);
        current.cell = map!!.getCase(nextCellID);
        current.cell!!.addFighter(current);

        if (po != null)// m�me erreur que tant�t, bug ou plus de fighter sur la
            // case
            po.cell!!.addFighter(po);
        if (nStep < 0) {
            nStep = nStep * -1;
        }

        curAction = "GA;129;" + current.id.toString() + ";" + current.id.toString() + ",-" + nStep;
        // Si porteur
        val po2: Fighter = current.getIsHolding()!!

        if (po2 != null && current.haveState(Constant.ETAT_PORTEUR)
                && po2.haveState(Constant.ETAT_PORTE)) {
            // on d�place le port� sur la case
            po2.cell = current.cell;
        }

        if (fighter.getPlayer() == null) {
            SocketManager.GAME_SEND_GAMEACTION_TO_FIGHT(this, 7, this.curAction);
            this.curAction = "";
            this.checkTraps(current);
            //new ArrayList(this.getTraps()).stream().filter { it != null }.filter(trap -> PathFinding.getDistanceBetween(getMap(), trap.getCell().getId(), current.cell.getId()) <= trap.size).forEach(trap -> trap.onTraped(current));
            return true;
        }

        if (type == Constant.FIGHT_TYPE_PVM && allChallenges.isEmpty() && current.isInvocation() && !(current is CloneFighter) && !(current is CollectorFighter) || type == Constant.FIGHT_TYPE_DOPEUL && allChallenges.isEmpty() && current.isInvocation() && !(current is CloneFighter) && !(current is CollectorFighter))
            this.allChallenges.values.stream().filter { it != null }.forEach { c -> c.onPlayerMove(fighter, false)  };

        fighter.getPlayer()!!.getGameClient()!!.addAction(GA);
        return true;
    }

    fun onFighterDie(target: Fighter, caster: Fighter) {
        var runnable: Runnable = Runnable {
            val current: Fighter = this.getFighterByGameOrder()!!

            if (current == null)
                return@Runnable

            var player: Player? = null
            //region Statistics
            if (Config.modeHeroic) {
                var p: Player = caster.getPlayer()!!
                var deadPlayer: Player? = target.getPlayer()
                if (deadPlayer != null) {
                    var type: Byte = if (caster.getMob() != null) (2).toByte() else if (p == deadPlayer) (-1).toByte() else 1.toByte()
                    var id: Long = (if (type == 1.toByte()) p.id else if (type == 2.toByte()) caster.getMob()!!.template.id else 0).toLong()
                    target.setKilledBy(Couple(type, id));
                }
                if (p != null && target != caster && deadPlayer != null) p.increaseTotalKills();
            }
            if (target.getPlayer().also { player = it } != null) {
                if (caster != target) {
                    var temp: Player? = null
                    var ok: Boolean = false
                    if (caster != null && (this@Fight.type == Constant.FIGHT_TYPE_AGRESSION || this@Fight.type == Constant.FIGHT_TYPE_CONQUETE || this@Fight.type == Constant.FIGHT_TYPE_ROYAL)) {
                        if (caster.getMob() != null && caster.getInvocator() != null && caster.getInvocator()!!.getPlayer() != null) {
                            ok = true;
                            temp = caster.getInvocator()!!.getPlayer();
                        } else if (caster.getPlayer() != null) {
                            temp = caster.getPlayer();
                            if (temp != player) {
                                ok = true;
                            }
                        }
                        if (ok && temp != null) {
                            var obj: GameObject = World.world.getObjTemplate(10275)!!.createNewItem(1, false)!!
                            if (temp.addItem(obj, true, false))
                                World.world.addGameObject(obj);
                            SocketManager.GAME_SEND_Im_PACKET(temp, "021;1~10275");
                        }
                    }
                }
            }
            //endregion

            if (target.hasLeft()) {
                deadList.add(target);
            }
            target.setIsDead(true);
            target.cell!!.removeFighter(target);

            if (target.haveState(Constant.ETAT_PORTEUR)) {
                var f: Fighter = target.getIsHolding()!!
                f.cell = f.cell;
                f.cell!!.addFighter(f);// Le bug venait par manque de ceci, il ni avait plus de firstFighter
                f.setState(Constant.ETAT_PORTE, 0);// J'ajoute ceci quand m�me pour signaler qu'ils ne sont plus en �tat port�/porteur
                target.setState(Constant.ETAT_PORTEUR, 0);
                f.setHoldedBy(null);
                target.setIsHolding(null);
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 950, f.id.toString() + "", f.id.toString() + "," + Constant.ETAT_PORTE.toString() + ",0");
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 950, target.id.toString() + "", target.id.toString() + "," + Constant.ETAT_PORTEUR.toString() + ",0");
            }
            if (this@Fight.type == Constant.FIGHT_TYPE_PVM && this@Fight.allChallenges.isEmpty() || this@Fight.type == Constant.FIGHT_TYPE_DOPEUL && this@Fight.allChallenges.isEmpty())
                this@Fight.allChallenges.values.stream().filter { it != null }.forEach { challenge -> challenge.onFighterDie(target)  };

            if (target.team == 0) {
                var team: HashMap<Int,Fighter> = HashMap(this@Fight.team0)

                for (entry in  team.values) {
                    if (entry.getInvocator() == null)
                        continue;
                    if (entry.getPdv() == 0 || entry.isDead)
                        continue;

                    if (entry.getInvocator()!!.id == target.id) {
                        this@Fight.onFighterDie(entry, caster);

                        try {
                            if (entry.getPlayer() == null) {
                                if (this@Fight.orderPlaying != null) {
                                    var index: Int = this@Fight.orderPlaying!!.indexOf(entry)
                                    if (index != -1)
                                        this@Fight.orderPlaying!!.removeAt(index);
                                }
                                if (this@Fight.team0.containsKey(entry.id))
                                    this@Fight.team0.remove(entry.id);
                                else if (this@Fight.team1.containsKey(entry.id))
                                    this@Fight.team1.remove(entry.id);
                            }

                        } catch (e: Exception) {
                            log.error("unexpected error", e)
                        }
                        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 999, target.id.toString() + "", this@Fight.getGTL());
                    }
                }
            } else if (target.team == 1) {
                var team: HashMap<Int,Fighter> = HashMap(this@Fight.team1)

                for (fighter in  team.values) {
                    if (fighter.getInvocator() == null)
                        continue;
                    if (fighter.getPdv() == 0 || fighter.isDead)
                        continue;
                    if (fighter.getInvocator()!!.id == target.id) {// si il a �t� invoqu� par le joueur mort
                        fighter.levelUp = true;
                        this@Fight.onFighterDie(fighter, caster);

                        if (fighter.getPlayer() == null) {
                            if (this@Fight.orderPlaying != null && this@Fight.orderPlaying!!.isEmpty()) {
                                try {
                                    var index: Int = this@Fight.orderPlaying!!.indexOf(fighter)
                                    if (index != -1)
                                        this@Fight.orderPlaying!!.removeAt(index);
                                } catch (e: Exception) {
                                    log.error("unexpected error", e)
                                }
                            }
                            if (this@Fight.team0.containsKey(fighter.id))
                                this@Fight.team0.remove(fighter.id);
                            else if (this@Fight.team1.containsKey(fighter.id))
                                this@Fight.team1.remove(fighter.id);
                        }
                        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 999, target.id.toString() + "", getGTL());
                    }
                }
            }
            if (target.getMob() != null) {
                try {
                    if (target.isInvocation() && target.isStatic()) {
                        target.getInvocator()!!.modNbrInvoc(-1);
                        // Il ne peut plus jouer, et est mort on revient au joueur
                        // pr�cedent pour que le startTurn passe au suivant

                        if (current.id == target.id) {
                            if (target.canPlay()) {
                                this@Fight.curAction = "";
                                this@Fight.curPlayer = curPlayer - 1;
                                this@Fight.endTurn(false, current);
                            }
                            // Il peut jouer, et est mort alors on passe son tour
                            // pour que l'autre joue, puis on le supprime de l'index
                            // sans probl�mes
                            else if (target.canPlay()) {
                                this@Fight.curAction = "";
                                this@Fight.endTurn(false, current);
                            }
                        }

                        if (this@Fight.orderPlaying != null && this@Fight.orderPlaying!!.isEmpty()) {
                            var index: Int = this@Fight.orderPlaying!!.indexOf(target)
                            // Si le joueur courant a un index plus �lev�, on le
                            // diminue pour �viter le outOfBound
                            if (index != -1) {
                                if (curPlayer > index && curPlayer > 0)
                                    this@Fight.curPlayer = curPlayer - 1;
                                this@Fight.orderPlaying!!.removeAt(index);
                            }

                            if (this@Fight.curPlayer < 0)
                                return@Runnable;
                            if (this@Fight.team0.containsKey(target.id))
                                this@Fight.team0.remove(target.id);
                            else if (this@Fight.team1.containsKey(target.id))
                                this@Fight.team1.remove(target.id);
                            //SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 999, target.getId().toString() + "", this@Fight.getGTL());
                        }
                        this@Fight.curAction = "";
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                }
            }

            val casterFinal: Fighter = caster
            if ((type == Constant.FIGHT_TYPE_PVM || type == Constant.FIGHT_TYPE_DOPEUL) && allChallenges.isEmpty())
                this@Fight.allChallenges.values.stream().filter { it != null }.forEach { challenge -> challenge.onMobDie(target, casterFinal)  };

            ArrayList(this@Fight.glyphs).stream().filter({ glyph -> glyph.caster.id == target.id }).forEach { glyph -> 
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 999, casterFinal.id.toString() + "", "GDZ-" + glyph.cell.cellId.toString() + ";" + glyph.size.toString() + ";" + glyph.color);
                SocketManager.GAME_SEND_GDC_PACKET_TO_FIGHT(this, 7, glyph.cell.cellId);
                this@Fight.glyphs.remove(glyph);
            }

            ArrayList(this@Fight.traps).stream().filter({ trap -> trap.caster.id == target.id }).forEach { trap -> 
                trap.disappear();
                this@Fight.traps.remove(trap);
            }

            if (caster != null && target.id == caster.id) {
                SocketManager.GAME_SEND_FIGHT_PLAYER_DIE_TO_FIGHT(this, 7, target.id);
                SocketManager.GAME_SEND_GTL_PACKET_TO_FIGHT(this, 7);

                if (target.canPlay() && current.id == target.id && current.hasLeft())
                    this@Fight.endTurn(false, current);
            } else {
                SocketManager.GAME_SEND_FIGHT_PLAYER_DIE_TO_FIGHT(this, 7, target.id);
                SocketManager.GAME_SEND_GTL_PACKET_TO_FIGHT(this, 7);

                if (target.canPlay() && current.id == target.id && current.hasLeft())
                    this@Fight.endTurn(false, current);
            }

            if (target is CollectorFighter) {// Le percepteur viens de mourrir on met fin au cbt
                this@Fight.getFighters(target.getTeam2()).stream().filter({ f -> f.isDead }).forEach { f -> 
                    this@Fight.onFighterDie(f, target);
                    this@Fight.verifIfTeamAllDead();
                }}
            if (target is PrismFighter) {
                this@Fight.getFighters(target.getTeam2()).stream().filter({ f -> f.isDead }).forEach { f -> 
                    this@Fight.onFighterDie(f, target);
                    this@Fight.verifIfTeamAllDead();
                }}

            for (fighter in  getFighters(3)) {
                var newBuffs: ArrayList<SpellEffect> = ArrayList()
                for (entry in  fighter.getFightBuff()) {
                    when (entry.spell) {
                        78, 431, 433, 437, 441, 443 -> {
                            newBuffs.add(entry);
                            continue;
                        }
                    }
                    if (entry.caster!!.id != target.id)
                        newBuffs.add(entry);
                }
                fighter.getFightBuff().clear();
                fighter.getFightBuff().addAll(newBuffs);
            }
            SocketManager.GAME_SEND_GTL_PACKET_TO_FIGHT(this, 7);
            this@Fight.verifIfTeamAllDead();
        };

        if (target.id != caster.id || target.hasLeft() && caster.hasLeft()) {
            runnable.run();
        } else {
            TimerWaiter.addNext(runnable, 3000);
        }
    }

    fun getFighters(teams: Int): ArrayList<Fighter> {// Entre 0 et 7, binaire([spec][t2][t1]).
        var teams = teams
        var fighters: ArrayList<Fighter> = ArrayList()

        if (teams - 4 >= 0) {
            this.viewer.values.stream()
                .filter { it != null }
                    .forEach { player -> Fighter.NewPlayer(this, player!!) } // TODO: Viewers shouldn't need to be fighters
            teams -= 4;
        }
        if (teams - 2 >= 0) {
            this.team1.values.stream().filter { it != null }.forEach(fighters::add);
            teams -= 2;
        }
        if (teams - 1 >= 0)
            this.team0.values.stream().filter { it != null }.forEach(fighters::add);
        return fighters;
    }

    fun getTeamFighters(team: Int): ArrayList<Fighter> {
        var fighters: ArrayList<Fighter> = ArrayList()

        if (team == 0)
            viewer.values.stream().map({ player -> Fighter.NewPlayer(this, player) }).forEach(fighters::add);
        if (team == 2)
            this.team1.values.forEach(fighters::add);
        if (team == 1)
            this.team0.values.forEach(fighters::add);
        return fighters;
    }

    fun getFighterByPerso(player: Player): Fighter {
        var fighter: Fighter? = null
        if (this.team0.get(player.id) != null)
            fighter = this.team0.get(player.id);
        if (this.team1.get(player.id) != null)
            fighter = this.team1.get(player.id);
        return fighter!!;
    }

    private fun getRandomCell(cells: List<GameCase>): GameCase? {
        var cell: GameCase? = null
        if (cells.isEmpty()) return null;

        var limit: Int = 0
        do {
            cell = cells.get(Formulas.random.nextInt(cells.size));
            limit++;
        } while ((cell == null || cell.fighters.isEmpty()) && limit < 80);

        if (limit == 80)
            return null;
        return cell;
    }

    @Synchronized fun exchangePlace(player: Player, cell: Int) {
        var fighter: Fighter = getFighterByPerso(player)

        if (fighter == null || collector != null && this.collectorProtect && collector.defenseFight != null && collector.defenseFight.containsValue(player))
            return;

        var team: Int = fighter.team
        var valid1: Boolean = false
        var valid2: Boolean = false

        for (a in 0 until start0.size)
            if (start0 != null && start0.get(a) != null && start0[a].cellId == cell)
                valid1 = true;
        for (a in 0 until start1.size)
            if (start1 != null && start1.get(a) != null && start1[a].cellId == cell)
                valid2 = true;
        if (state != 2 || isOccuped(cell) || player.ready || team == 0 && valid1 || team == 1 && valid2)
            return;
        fighter.cell!!.removeFighter(fighter);
        fighter.cell = map!!.getCase(cell);
        map!!.getCase(cell)!!.addFighter(fighter);
        SocketManager.GAME_SEND_FIGHT_CHANGE_PLACE_PACKET_TO_FIGHT(this, 3, player.id, cell);
    }

    fun isOccuped(cell: Int): Boolean {
        return map!!.getCase(cell) == null || map!!.getCase(cell)!!.fighters.isEmpty();
    }

    fun getNextLowerFighterGuid(): Int {
        return nextId--;
    }

    fun addFighterInTeam(f: Fighter, team: Int) {
        if (team == 0)
            this.team0.put(f.id, f);
        else if (team == 1)
            this.team1.put(f.id, f);
    }

    private fun addChevalier() {
        var groupData: String = ""
        var a: Int = 0
        for (F in  this.team0.values) {
            if (F.getPlayer() == null)
                continue;
            if (this.team1.size > this.team0.size)
                continue;
            groupData = groupData.toString() + "394," + Constant.getLevelForChevalier(F.getPlayer()!!).toString() + "," + Constant.getLevelForChevalier(F.getPlayer()!!);
            if (a < this.team0.size - 1)
                groupData = groupData.toString() + ";";
            a++;
        }
        setMobGroup(MonsterGroup(mapOld.nextObjectId, this.map, this.init0.getPlayer()!!.curCell.cellId, groupData));
        for (entry in  getMobGroup().mobs.entries) {
            this.team1.put(entry.key, Fighter.NewMob(entry.key, this, entry.value));
        }
        var e: ArrayList<Map.Entry<Int,Fighter>> = ArrayList(this.team1.entries)
        for (entry in  e) {
            if (entry.value.getPlayer() != null)
                continue;
            var f: Fighter = entry.value
            var cell: GameCase = getRandomCell(start1)!!
            if (cell == null) {
                this.team1.remove(f.id);
            } else {
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.id.toString() + "", f.id.toString() + "," + Constant.ETAT_PORTE.toString() + ",0");
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.id.toString() + "", f.id.toString() + "," + Constant.ETAT_PORTEUR.toString() + ",0");
                f.cell = cell;
                f.cell!!.addFighter(f);
                f.team = 1;
                SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(this.init0.getPlayer()!!.curMap, getMobGroup().id, entry.value);
            }
        }
        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this, 7, map!!);
    }

    fun onPlayerDisconnection(player: Player, check: Boolean): Boolean {
        if (this.state == Constant.FIGHT_STATE_INIT)
            return true;

        val current: Fighter = this.getFighterByGameOrder()!!
        val target: Fighter = this.getFighterByPerso(player)

        if (target == null)
            return false;
        if (player.start != null) {
            this.endFight((1.toByte()));
            return true;
        }

        if (this.state == Constant.FIGHT_STATE_INIT || this.state == Constant.FIGHT_STATE_FINISHED) {
            if (check)
                this.leftFight(player, null);
            return false;
        }

        if (target.nbrDisconnection >= 5 && Config.modeHeroic) {
            if (check) {
                this.leftFight(player, null);
                for (fighter in  this.getFighters(7)) {
                    if (!(fighter.getPlayer() == null || fighter.getPlayer()!!.isOnline))
                        SocketManager.GAME_SEND_MESSAGE(fighter.getPlayer()!!, target.getPacketsName().toString() + " s'est déconnecté plus de 5 fois dans le même combat, nous avons décidé de lui faire abandonner.", "A00000");
                }
            }
            return false;
        }
        if (check) {
            if (player.fight!!.getFighterByPerso(player).isDeconnected())
                SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 7, "1182;" + target.getPacketsName().toString() + "~20");
            target.Disconnect();
        }
        if (current != null && current.id == target.id)
            this.endTurn(false, current);
        return true;
    }

    fun onPlayerReconnection(player: Player): Boolean {
        val current: Fighter = this.getFighterByGameOrder()!!
        val target: Fighter = getFighterByPerso(player)

        if (target == null || this.state == Constant.FIGHT_STATE_FINISHED)
            return false;

        target.Reconnect();

        SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 7, "1184;" + target.getPacketsName());

        if (state == Constant.FIGHT_STATE_ACTIVE)
            SocketManager.GAME_SEND_GJK_PACKET(player, state, 0, 0, 0, 0, type);// Join Fight => getState(), pas d'anulation...
        else {
            if (type == Constant.FIGHT_TYPE_CHALLENGE)
                SocketManager.GAME_SEND_GJK_PACKET(player, 2, 1, 1, 0, 0, type);
            else
                SocketManager.GAME_SEND_GJK_PACKET(player, 2, 0, 1, 0, 0, type);
        }

        var f: Fighter = if (target.team == 0) init0 else init1
        if (f != null)
            SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(player, f.id, target);// Indication de la team
        SocketManager.GAME_SEND_STATS_PACKET(player);

        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(this, map!!, player);

        if (state == Constant.FIGHT_STATE_PLACE) {
            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(player.getGameClient()!!, map!!.places, st1);
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, player.id.toString() + "", player.id.toString() + "," + Constant.ETAT_PORTE.toString() + ",0");
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, player.id.toString() + "", player.id.toString() + "," + Constant.ETAT_PORTEUR.toString() + ",0");
        } else {

            SocketManager.GAME_SEND_GS_PACKET(player);// D�but du jeu
            SocketManager.GAME_SEND_GTL_PACKET(player, this);// Liste des tours

            if (current != null)
                SocketManager.GAME_SEND_GAMETURNSTART_PACKET(player, current.id, ((System.currentTimeMillis() - launchTime)).toInt());

            if (this.type == Constant.FIGHT_TYPE_PVM || this.type == Constant.FIGHT_TYPE_DOPEUL && this.allChallenges.isEmpty()) {
                this.allChallenges.values.stream().filter({ challenge -> challenge != null && challenge.loose() })
                        .forEach { challenge -> 
                            SocketManager.GAME_SEND_CHALLENGE_PERSO(player, challenge.parseToPacket());
                            if (challenge.loose())
                                challenge.challengeSpecLoose(player);
                        }}
            for (f1 in  getFighters(3)) {
                f1.sendState(player);
            }
            // Si combat en cours on envois des im
            var all: ArrayList<Fighter> = ArrayList()
            all.addAll(this.team0.values);
            all.addAll(this.team1.values);
            all.stream().filter { it != null }.forEach { f1 -> 
                for (effect in  f1.getFightBuff()) {
                    this.sendBuffPacket(f1, effect, Collections.singletonList(target), player, -999);
                }
            }
            for (trap in  this.traps)
                if (trap.caster.team == target.team)
                    trap.refresh(target);
            for (glyph in  this.glyphs) {
                SocketManager.GAME_SEND_GA_PACKET(target.getPlayer()!!, 999, glyph.caster.id.toString() + "",
                        "GDZ+" + glyph.cell.cellId.toString() + ";" + glyph.size.toString() + ";" + glyph.color);
                SocketManager.GAME_SEND_GA_PACKET(target.getPlayer()!!, 999, glyph.caster.id.toString() + "",
                        "GDC" + glyph.cell.cellId.toString() + ";Haaaaaaaaa3005;");
            }
        }
        return true;
    }

    fun sendBuffPacket(target: Fighter, effect: SpellEffect, receivers: List<Fighter>, reconnectedPlayer: Player?, clientDuration: Int) {
        val packet: StringBuilder = StringBuilder()
        packet.append("GIE").append(effect.effectID).append(";").append(target.id).append(";");

        var value: Int = 0
        when (effect.effectID){ 106 -> {packet.append("-1;").append(effect.value).append(";10;;");
                
}
950 -> {var value: Int = Integer.parseInt(effect.args.split(";")[2])
                packet.append(value).append(";;").append(value).append(";;");
                
}
79 -> {value = Integer.parseInt(effect.args.split(";")[0]);
                var valMax: String = effect.args.split(";")[1]
                var chance: String = effect.args.split(";")[2]
                packet.append(value).append(";").append(valMax).append(";").append(chance).append(";;");
                
}
108, 606, 607, 608, 609, 611 -> {var jet: String = effect.args.split(";")[5]
                var min: Int = Formulas.getMinJet(jet)
                var max: Int = Formulas.getMaxJet(jet)
                packet.append(min).append(";").append(max).append(";").append(max).append(";;");
                
}
788 -> {var args: Array<String> = effect.args.split(";").toTypedArray()

                if (args[0].equals("125")) {
                    // Chatiment vitalessque
                    var packetGIE: String = "GIE108;" + target.id.toString() + ";" + args[1] + ";;;;" + (effect.turns - 1) + ";" + effect.spell
                    for (fighter in  receivers) {
                        if (fighter != null && fighter.getPlayer() != null) {
                            fighter.getPlayer()!!.send(packetGIE);
                        }
                    }
                }

                packet.append(";").append(args[1]).append(";").append(args[2]).append(";;");
                
}
91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 107, 114, 165, 781, 782 -> {value = Integer.parseInt(effect.args.split(";")[0]);
                var valMax1: String = effect.args.split(";")[1]
                if (valMax1.compareTo("-1") == 0 || effect.spell == 82 || effect.spell == 94)
                    packet.append(value).append(";;;;");
                else if (valMax1.compareTo("-1") != 0)
                    packet.append(value).append(";").append(valMax1).append(";;;");
                
}
else -> {packet.append(effect.value).append(";;;;");
                
}
}

        var duration: Int = if (effect.turns == -1) 999 else effect.turns
        if (reconnectedPlayer != null) {
            var param: String? = null
            if (effect.effectID == 149) {
                param = target.id.toString() + "," + target.getDefaultGfx().toString() + "," + effect.value.toString() + "," + effect.turns;
            }
            if (effect.effectID == 150) {
                param = target.id.toString() + "," + effect.turns;
            }
            if (param != null) {
                SocketManager.GAME_SEND_GA_PACKET(reconnectedPlayer, effect.effectID, effect.caster!!.id.toString(), param.toString());
            }
        }

        packet.append(if (clientDuration == -999) duration else clientDuration).append(";").append(effect.spell);

        for (fighter in  receivers) {
            if (fighter != null && fighter.getPlayer() != null) {
                fighter.getPlayer()!!.send(packet.toString());
            }
        }
    }

    fun verifIfAllReady() {
        var vale: Boolean= true
        if (type == Constant.FIGHT_TYPE_DOPEUL) {
            for (f in  this.team0.values) {
                if (f == null || f.getPlayer() == null)
                    continue;
                var perso: Player = f.getPlayer()!!
                if (perso.ready)
                    vale = false;
            }
            if (vale)
                startFight();
            return;
        }

        for (a in 0 until this.team0.size)
            if (this.team0.get(this.team0.keys.toTypedArray()[a])!!.getPlayer()!!.ready)
                vale = false;

        if (type != 4 && type != 5 && type != 7
                && type != Constant.FIGHT_TYPE_CONQUETE)
            for (a in 0 until this.team1.size)
                if (this.team1.get(this.team1.keys.toTypedArray()[a])!!.getPlayer()!!.ready)
                    vale = false;

        if (type == 5 || type == 2)
            vale = false;
        if (vale)
            startFight();
    }

    private fun verifyStillInFight(): Boolean// Return true si au moins un joueur est encore dans le combat
    {
        if (this.monsterGroup2 != null) return false;
        for (f in  this.team0.values) {
            if (f is CollectorFighter)
                return false;
            if (f.isInvocation() || f.isDead || f.getPlayer() == null
                    || f.getMob() != null || (f is CloneFighter)
                    || f.hasLeft())
                continue;
            if (f.getPlayer() != null
                    && f.getPlayer()!!.fight != null
                    && f.getPlayer()!!.fight!!.id == this.id) // Si il n'est plus dans ce combat
                return false;
        }
        for (f in  this.team1.values) {
            if (f is CollectorFighter)
                return false;
            if (f.isInvocation() || f.isDead || f.getPlayer() == null
                    || f.getMob() != null || (f is CloneFighter)
                    || f.hasLeft())
                continue;
            if (f.getPlayer() != null
                    && f.getPlayer()!!.fight != null
                    && f.getPlayer()!!.fight!!.id == this.id) // Si il n'est plus dans ce combat
                return false;
        }
        return true;
    }

    fun verifIfTeamIsDead(): Boolean {
        var finish: Boolean = true
        for (entry in  this.team1.entries) {
            if (entry.value.isInvocation())
                continue;
            if (entry.value.isDead) {
                finish = false;
                break;
            }
        }
        return finish;
    }

    fun verifIfTeamAllDead() {
        if (state >= Constant.FIGHT_STATE_FINISHED)
            return;

        var team0: Boolean = true
        var team1: Boolean = true

        for (fighter in  ArrayList(this.team0.values)) {
            if (fighter.isInvocation())
                continue;
            if (fighter.isDead) {
                team0 = false;
                break;
            }
        }

        for (fighter in  ArrayList(this.team1.values)) {
            if (fighter.isInvocation())
                continue;
            if (fighter.isDead) {
                team1 = false;
                break;
            }
        }


        if ((team0 || team1 || verifyStillInFight()) && this.finish) {
            this.finish = true;

            val copyTeam0: MutableMap<Int,Fighter> = HashMap()
            val copyTeam1: MutableMap<Int,Fighter> = HashMap()
            for (entry in  this.team0.entries) {
                if (entry.value.getMob() != null)
                    if (entry.value.getMob()!!.template.id == 375)
                        Bandit.getBandits()!!.isPop = false;
                copyTeam0.put(entry.key, entry.value);
            }

            for (entry in  this.team1.entries) {
                if (entry.value.getMob() != null)
                    if (entry.value.getMob()!!.template.id == 375)
                        Bandit.getBandits()!!.isPop = false;
                copyTeam1.put(entry.key, entry.value);
            }

            val winners: Boolean = team0

            val fighters: ArrayList<Fighter> = ArrayList()
            fighters.addAll(copyTeam0.values);
            fighters.addAll(copyTeam1.values);
            if (this.turn != null) {
                this.turn!!.stop();
                this.turn = null;
            }

            try {
                var challenges: String = ""
                if (this.type == Constant.FIGHT_TYPE_PVM && this.allChallenges.isEmpty()
                        || type == Constant.FIGHT_TYPE_DOPEUL && this.allChallenges.isEmpty()) {
                    for (challenge in  allChallenges.values) {
                        if (challenge != null) {
                            challenge.fightEnd();
                            challenges +=if (challenges.isEmpty()) challenge.getPacketEndFight() else "," + challenge.getPacketEndFight();
                        }
                    }
                }

                this.state = Constant.FIGHT_STATE_FINISHED;


                if (winners) {
                    losers.addAll(copyTeam0.values);
                    this.winners.addAll(copyTeam1.values);
                } else {
                    this.winners.addAll(copyTeam0.values);
                    losers.addAll(copyTeam1.values);
                }


                if (Constant.FIGHT_TYPE_PVM == this.type) {
                    for (fighter in  this.winners) {
                        var player: Player = fighter.getPlayer()!!
                        if (player == null)
                            continue;

                        player.fight = null;

                        if (fighter.isDeconnected()) {
                            player.setLastFightForEndFightAction(this);
                            player.curMap.applyEndFightAction(player);
                            player.setLastFightForEndFightAction(null);
                        } else if (this.mapOld.data.hasFightEndForType(this.type)) {
                            player.setLastFightForEndFightAction(this);
                        }

                        earlyEndfightEvent(player);
                    }
                }


                val packet: String = if (this.type == -1) "GE" else this.getGE(if (winners) 2 else 1)

                for (fighter in  fighters) {
                    var player: Player = fighter.getPlayer()!!
                    if (player != null) {
                        player.fight = null;
                        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.map!!, fighter.id);
                    }
                }

                this.curPlayer = -1;

                when (this.type) {
Constant.FIGHT_TYPE_CHALLENGE, Constant.FIGHT_TYPE_AGRESSION, Constant.FIGHT_TYPE_CONQUETE -> {for (fighter in  copyTeam1.values) {
                            var player: Player = fighter.getPlayer()!!

                            if (player != null) {
                                player.duelId = -1;
                                player.ready = false;
                            }
                        }
                        
}
}

                for (fighter in  this.team0.values) {
                    var player: Player = fighter.getPlayer()!!

                    if (player != null) {
                        player.duelId = -1;
                        player.ready = false;
                    }
                }

                for (fighter in  this.getFighters(3))
                    fighter.getFightBuff().clear();

                this.mapOld.removeFight(this.id);
                SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(World.world.getMap(this.map!!.id));

                val str: String = if (this.prism != null) this.prism.map.toString() + "|" + this.prism.x.toString() + "|" + this.prism.y else ""

                this.map = null;
                this.orderPlaying = null;

                if (this.type != -1) {
                    /** WINNER **/
                    for (fighter in this.winners) {
                        /** Collector **/
                        if (fighter.getCollector() != null) {
                            World.world.getGuild(guildId)!!.getPlayers().stream().filter { it != null }.filter { it.isOnline }.forEach { player ->
                                SocketManager.GAME_SEND_gITM_PACKET(player, Collector.parseToGuild(player.getGuild()!!.id));
                                SocketManager.GAME_SEND_PERCO_INFOS_PACKET(player, fighter.getCollector()!!, "S");
                            }

                            fighter.getCollector()!!.inFight = 0.toByte();
                            fighter.getCollector()!!._inFightID = -1;
                            fighter.getCollector()!!.clearDefenseFight();

                            this.mapOld.players.stream().filter { it != null }
                                    .forEach { player -> SocketManager.GAME_SEND_MAP_PERCO_GMS_PACKETS(player.getGameClient()!!, player.curMap)  };
                        }
                        /** Prism **/
                        if (fighter.getPrism() != null) {
                            World.world.players.stream().filter { it != null }.filter({ player -> player.alignment == prism.alignment })
                                    .forEach { player -> SocketManager.SEND_CS_SURVIVRE_MESSAGE_PRISME(player, str)  };

                            fighter.getPrism()!!.fight = null;

                            this.mapOld.players.stream().filter { it != null }
                                    .forEach { player -> SocketManager.SEND_GM_PRISME_TO_MAP(player.getGameClient()!!, player.curMap)  };
                        }

                        if (fighter.isInvocation())
                            continue;
                        if (fighter.hasLeft())
                            continue;
                        this.onPlayerWin(fighter, losers);
                    }
                    /** END WINNER **/

                    /** LOOSER **/
                    for (fighter in losers) {
                        if (fighter.getCollector() != null) {
                            World.world.getGuild(guildId)!!.getPlayers().stream().filter { it != null }.filter { it.isOnline }.forEach { player ->
                                SocketManager.GAME_SEND_gITM_PACKET(player, Collector.parseToGuild(player.getGuild()!!.id));
                                SocketManager.GAME_SEND_PERCO_INFOS_PACKET(player, fighter.getCollector()!!, "D");
                            }

                            this.mapOld.RemoveNpc(fighter.getCollector()!!.id);
                            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(mapOld, fighter.getCollector()!!.id);
                            fighter.getCollector()!!.reloadTimer();
                            this.collector.delCollector(fighter.getCollector()!!.id);
                            (DatabaseManager.get(CollectorData::class.java) as CollectorData).delete(fighter.getCollector()!!);
                        }

                        if (fighter.getPrism() != null) {
                            val subarea: SubArea = this.mapOld.subArea!!

                            for (player in World.world.players) {
                                if (player == null)
                                    continue;

                                if (player.alignment == 0) {
                                    SocketManager.GAME_SEND_am_ALIGN_PACKET_TO_SUBAREA(player, subarea.id.toString() + "|-1|1");
                                    continue;
                                }

                                if (player.alignment == prism.alignment)
                                    SocketManager.SEND_CD_MORT_MESSAGE_PRISME(player, str);

                                SocketManager.GAME_SEND_am_ALIGN_PACKET_TO_SUBAREA(player, subarea.id.toString() + "|-1|0");

                                if (prism.conquestArea != -1) {
                                    SocketManager.GAME_SEND_aM_ALIGN_PACKET_TO_AREA(player, subarea.area!!.id.toString() + "|-1");
                                    subarea.area!!.prismId =(0);
                                    subarea.area!!.alignement =(0);
                                }
                                SocketManager.GAME_SEND_am_ALIGN_PACKET_TO_SUBAREA(player, subarea.id.toString() + "|0|1");
                            }
                            val id: Int = fighter.getPrism()!!.id
                            fighter.getPrism()!!.fight = null;
                            subarea.prism = null;
                            subarea.alignment = 0;
                            subarea.conquerable = false;
                            TimerWaiter.addNext({ subarea.conquerable = true }, 5 * 60_000);
                            this.mapOld.RemoveNpc(id);
                            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(mapOld, id);
                            World.world.removePrisme(id);
                            (DatabaseManager.get(PrismData::class.java) as PrismData).delete(fighter.getPrism()!!);
                        }

                        if (fighter.getMob() != null)
                            continue;
                        if (fighter.isInvocation())
                            continue;

                        this.onPlayerLoose(fighter);
                    }
                    /** END LOOSER **/
                }


                for (player in  this.viewer.values) {
                    player.refreshMapAfterFight();
                    player.spec = false;
                    player.send(packet);

                    if (player.getAccount().isBanned)
                        player.getGameClient()!!.kick();
                }

                for (fighter in  fighters) {
                    var player: Player = fighter.getPlayer()!!
                    if (player != null) {
                        if (this.isBegin) {
                            if (player.curMap.id == 8357 && player.hasItemTemplate(7373, 1, false) && player.hasItemTemplate(7374, 1, false) && player.hasItemTemplate(7375, 1, false) && player.hasItemTemplate(7376, 1, false) && player.hasItemTemplate(7377, 1, false) && player.hasItemTemplate(7378, 1, false)) {
                                player.removeItemByTemplateId(7373, 1, false);
                                player.removeItemByTemplateId(7374, 1, false);
                                player.removeItemByTemplateId(7375, 1, false);
                                player.removeItemByTemplateId(7376, 1, false);
                                player.removeItemByTemplateId(7377, 1, false);
                                player.removeItemByTemplateId(7378, 1, false);
                            }
                            player.send(packet);
                        }
                    }
                }
            } catch (e: Exception) {
                log.error("unexpected error", e)
                for (fighter in  fighters) {
                    var player: Player = fighter.getPlayer()!!
                    if (player != null) {
                        player.duelId = -1;
                        player.ready = false;
                        player.fight = null;
                        SocketManager.GAME_SEND_GV_PACKET(player);
                    }
                }
            }


            for (fighter in  fighters) {
                var player: Player = fighter.getPlayer()!!

                if (player == null)
                    continue;

                if (player.fight != null)
                    player.fight = null;

                player.refreshLife(false);

                if (player.curCell.isWalkableFight())
                    player.teleport(player.curMap, player.curMap.randomFreeCellId);
                if (player.getAccount().isBanned)
                    player.getGameClient()!!.kick();
                if (fighter.isDeconnected())
                    player.getAccount().disconnect(player);
                if (player.morphMode)
                    SocketManager.GAME_SEND_SPELL_LIST(player);
                if (player.party != null && player.party!!.master != null && player.party!!.master!!.id == player.id) {
                    player.party!!.moveAllPlayersToMaster(player.curCell, true);
                    TimerWaiter.addNext({ {
                        for (target in  player.party!!.players) {
                            if (target.id != player.id && target.curMap.id == player.curMap.id && target.fight == null) {
                                if (player.getAccount() != null && target.getAccount() != null &&
                                        target.getAccount()!!.currentIp.equals(player.getAccount()!!.currentIp, ignoreCase = true))
                                    target.send("GE");
                            }
                        }
                    } }, 1000);
                }
            }
        }

    }

    // This is a temporary hack function to trigger script's end fight event.
    private fun earlyEndfightEvent(p: Player) {
        var isWinner: Boolean = winners.stream().map(Fighter::getPlayer).filter { it != null }.anyMatch({ fp -> fp!!.id == p.id })

        var winners: Table = ScriptVM.scriptedValsTable(this.winners)
        var losers: Table = ScriptVM.scriptedValsTable(this.losers)

        DataScriptVM.getInstance()!!.handlers.onFightEnd(p, type, isWinner, winners, losers);
    }

    fun onPlayerWin(fighter: Fighter, looseTeam: List<Fighter>) {
        var player: Player = fighter.getPlayer()!!

        if (player == null)
            return;

        player.afterFight = true;

        var weapon: GameObject = player.getObjetByPos(Constant.ITEM_POS_ARME)!!
        if (weapon != null) {
            if (weapon.txtStat.containsKey(Constant.STATS_RESIST)) {
                var statNew: Int = Integer.parseInt(weapon.txtStat.get(Constant.STATS_RESIST), 16) - 1
                if (statNew <= 0) {
                    SocketManager.send(player, "Im160");
                    player.removeItem(weapon.guid, 1, true, true);
                } else {
                    weapon.txtStat.remove(Constant.STATS_RESIST); // on retire les stats "32c"
                    weapon.addTxtStat(Constant.STATS_RESIST, Integer.toHexString(statNew));// on ajout les bonnes stats
                    SocketManager.GAME_SEND_UPDATE_OBJECT_DISPLAY_PACKET(player, weapon);
                }
            }
        }

        if (this.type != Constant.FIGHT_TYPE_CHALLENGE) {
            if (fighter.getPdv() <= 0)
                player.setPdv(1);
            else
                player.setPdv(fighter.getPdv());

            if (fighter.levelUp) player.fullPDV();
        }

        if (this.type == 2)
            if (this.prism != null)
                SocketManager.SEND_CP_INFO_DEFENSEURS_PRISME(player, this.getDefenders());

        if (this.type == Constant.FIGHT_TYPE_PVT)
            if (player.guildMember != null)
                if (this.collector.guildId == player.guildMember!!.guild.id)
                    player.teleportOldMap();

        if (this.type == Constant.FIGHT_TYPE_PVM) {
            var obj: GameObject = player.getObjetByPos(Constant.ITEM_POS_FAMILIER)!!
            if (obj != null) {
                var souls: MutableMap<Int,Int> = HashMap()

                for (f in  looseTeam) {
                    if (f.getMob() == null)
                        continue;

                    var id: Int = f.getMob()!!.template.id

                    if (souls.isEmpty() && souls.containsKey(id))
                        souls.put(id, souls.get(id)!! + 1);
                    else
                        souls.put(id, 1);
                }
                if (souls.isEmpty()) {
                    var pet: PetEntry = World.world.getPetsEntry(obj.guid)!!
                    if (pet != null)
                        pet.eatSouls(player, souls);
                }
            }
        }
    }

    fun onPlayerLoose(fighter: Fighter) {
        val player: Player = fighter.getPlayer()!!

        if (player == null)
            return;
        if (player.morphMode && player.donjon)
            player.unsetFullMorph();

        var arme: GameObject = player.getObjetByPos(Constant.ITEM_POS_ARME)!!

        if (arme != null) {
            if (arme.txtStat.containsKey(Constant.STATS_RESIST)) {
                var statNew: Int = Integer.parseInt(arme.txtStat.get(Constant.STATS_RESIST), 16) - 1
                if (statNew <= 0) {
                    SocketManager.send(player, "Im160");
                    player.removeItem(arme.guid, 1, true, true);
                } else {
                    arme.txtStat.remove(Constant.STATS_RESIST); // on retire les stats "32c"
                    arme.addTxtStat(Constant.STATS_RESIST, Integer.toHexString(statNew));// on ajout les bonnes stats
                    SocketManager.GAME_SEND_UPDATE_OBJECT_DISPLAY_PACKET(player, arme);
                }
            }
        }

        if (player.getObjetByPos(Constant.ITEM_POS_FAMILIER) != null && this.type != Constant.FIGHT_TYPE_CHALLENGE) {
            var obj: GameObject = player.getObjetByPos(Constant.ITEM_POS_FAMILIER)!!
            if (obj != null) {
                var pets: PetEntry = World.world.getPetsEntry(obj.guid)!!
                if (pets != null)
                    pets.looseFight(player);
            }
        }

        if (player.getObjetByPos(Constant.ITEM_POS_PNJ_SUIVEUR) != null)
            player.setMascotte(0);

        if (this.type == 2)
            if (this.prism != null)
                SocketManager.SEND_CP_INFO_DEFENSEURS_PRISME(player, this.getDefenders());

        if (this.type != Constant.FIGHT_TYPE_CHALLENGE) {
            var loose: Int = Formulas.getLoosEnergy(player.level, type == 1, type == 5)
            var energy: Int = player.energy - loose

            player.energy = Math.max(energy, 0);

            if (player.isOnline)
                SocketManager.GAME_SEND_Im_PACKET(player, "034;" + loose);
            if (Config.modeHeroic) {
                if (fighter.getKilledBy() != null)
                    player.die(fighter.getKilledBy()!!.first, fighter.getKilledBy()!!.second);
            } else {
                if (energy <= 0) {
                    if (this.type == Constant.FIGHT_TYPE_AGRESSION && fighter.getTraqued()) {
                        if (this.team1.containsValue(fighter))
                            player.teleportFaction(this.getAlignementOfTraquer(this.team0.values, player));
                        else
                            player.teleportFaction(this.getAlignementOfTraquer(this.team1.values, player));
                        player.energy = 1;
                    } else {
                        TimerWaiter.addNext(player::setFuneral, 1, TimeUnit.SECONDS);
                    }
                } else {
                    if (this.type == Constant.FIGHT_TYPE_AGRESSION && fighter.getTraqued()) {
                        if (this.team1.containsValue(fighter))
                            player.teleportFaction(this.getAlignementOfTraquer(this.team0.values, player));
                        else
                            player.teleportFaction(this.getAlignementOfTraquer(this.team1.values, player));
                    } else {
                        player.setNeededEndFightAction(this, Action(1001, player.savePos.toString(","), ""));
                        player.setPdv(1);
                    }
                }
            }
        }
    }

    fun getAlignementOfTraquer(fighters: Collection<Fighter>, player: Player): Int {
        for (fighter in  fighters)
            if (fighter.getPlayer() != null)
                if (fighter.getPlayer()!!.stalk!!.target == player)
                    return fighter.getPlayer()!!.alignment;
        return 0;
    }

    fun onGK(player: Player) {
        val current: Fighter = this.getFighterByGameOrder()!!
        if (current == null)
            return;
        if (curAction.equals("") || current.id != player.id || state != Constant.FIGHT_STATE_ACTIVE)
            return;

        SocketManager.GAME_SEND_GAMEACTION_TO_FIGHT(this, 7, this.curAction);
        SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 2, current.id);

        if (this.curAction.equals("casting")) {
            val fighter: Fighter = getFighterByPerso(player)
            val currentCell: Int = fighter.cell!!.cellId

            this.checkTraps(fighter);
        }

        this.curAction = "";
    }

    private fun getGEBoufbawl(win: Int): String {
        var time: Long = System.currentTimeMillis() - startTime
        var initGUID: Int = this.init0.id
        var Packet: StringBuilder = StringBuilder()
        Packet.append("GE").append(time);
        Packet.append("|").append(initGUID).append("|").append(0).append("|");

        var TEAM1: ArrayList<Fighter> = ArrayList()
        var TEAM2: ArrayList<Fighter> = ArrayList()
        if (win == 1) {
            TEAM1.addAll(this.team0.values);
            TEAM2.addAll(this.team1.values);
        } else {
            TEAM1.addAll(this.team1.values);
            TEAM2.addAll(this.team0.values);
        }

        for (i in  TEAM1)//Les Gagnant
        {
            if (i.isInvocation() && i.getMob() != null)
                continue;//Pas d'invoc dans les gains
            if (i.getMob() != null)
                continue;
            if (i is CloneFighter)
                continue;//Pas de double dans les gains
            Packet.append("0;").append(i.id).append(";").append(i.getPacketsName()).append(";").append(i.getLvl()).append(";");
            if (i.getPlayer() != null) {
                Packet.append(i.getDefaultGfx()).append(";");
            }
            Packet.append(";").append(if (i.getPdv() == 0 || i.hasLeft()) 1 else 0).append(";").append("0;0;0").append(";;;;|");
        }
        // Fin gagnant
        for (i in  TEAM2)//Les perdants
        {
            if (i.isInvocation() && i.getMob() != null)
                continue;//Pas d'invoc dans les gains
            if (i.getMob() != null)
                continue;
            if (i is CloneFighter)
                continue;//Pas de double dans les gains
            Packet.append("2;").append(i.id).append(";").append(i.getPacketsName()).append(";").append(i.getLvl()).append(";");
            if (i.getPlayer() != null) {
                Packet.append(i.getDefaultGfx()).append(";");
            }
            Packet.append(";").append(if (i.getPdv() == 0 || i.hasLeft()) 1 else 0).append(";").append("0;0;0").append(";;;;|");
        }

        return Packet.toString();
    }

    fun getGE(win: Int): String {
        // Unused: long t1 = System.currentTimeMillis();
        val packet: StringBuilder = StringBuilder()
        var type: Int = Constant.FIGHT_TYPE_CHALLENGE

        // Boufbawl
        if (this.type == 7) {
            return getGEBoufbawl(win);
        }

        if (this.type == Constant.FIGHT_TYPE_AGRESSION || type == Constant.FIGHT_TYPE_CONQUETE)
            type = 1;
        if (this.type == Constant.FIGHT_TYPE_PVT)
            type = Constant.FIGHT_TYPE_CHALLENGE;
        if (this.type == 7) type = 0;

        packet.append("GE").append(System.currentTimeMillis() - startTime);
        if (type == Constant.FIGHT_TYPE_PVM && getMobGroup() != null)
            packet.append(';').append(getMobGroup().getStarBonus());
        packet.append("|").append(this.init0.id).append("|").append(type).append("|");

        //region Purge team
        var winners: ArrayList<Fighter> = ArrayList()
        var loosers: ArrayList<Fighter> = ArrayList()

        var iterator: MutableIterator<Map.Entry<Int,Fighter>> = this.team0.entries.iterator()
        while (iterator.hasNext()) {
            var entry: Map.Entry<Int,Fighter> = iterator.next()
            var fighter: Fighter = entry.value

            if (fighter.isInvocation() && fighter.getMob() != null && fighter.getMob()!!.template.id != 285)
                iterator.remove();
            if (fighter is CloneFighter) iterator.remove();
        }

        iterator = this.team1.entries.iterator();
        while (iterator.hasNext()) {
            var entry: Map.Entry<Int,Fighter> = iterator.next()
            var fighter: Fighter = entry.value

            if (fighter.isInvocation() && fighter.getMob() != null && fighter.getMob()!!.template.id != 285)
                iterator.remove();
            if (fighter is CloneFighter) iterator.remove();
        }

        if (win == 1) {
            winners.addAll(this.team0.values);
            loosers.addAll(this.team1.values);
        } else {
            winners.addAll(this.team1.values);
            loosers.addAll(this.team0.values);
        }
        //endregion

        try {
            /* Var heroic mod **/
            var team: Boolean = false

            var totalXP: Long = 0
            for (F in  loosers) {
                if (F.getMob() != null)
                    totalXP += F.getMob()!!.baseXp;
                if (F.getPlayer() != null)
                    team = true;
            }

            //region Capture d'�mes
            var fullSoul: SoulStone? = null
            var mobCapturable: Boolean = true
            for (fighter in  loosers) {
                if (fighter.getMob() == null || fighter.getMob()!!.template == null || fighter.getMob()!!.template.isCapturable) {
                    mobCapturable = false;
                }
                if (fighter.getMob() != null && fighter.getMob()!!.template != null) {
                    for (protector in  JobConstant.JOB_PROTECTORS) {
                        if (protector[0] == fighter.getMob()!!.template.id) {
                            mobCapturable = false;
                        }
                    }
                }
            }

            if (mobCapturable && SoulStone.isInArenaMap(this.mapOld.id)) {
                var isFirst: Boolean = true
                var maxLvl: Int = 0
                var stats: String = ""

                for (fighter in  loosers) {
                    if (fighter.isInvocation() || fighter.getInvocator() != null)
                        continue;
                    stats += (if (isFirst) "" else "|") + fighter.getMob()!!.template.id.toString() + "," + fighter.getLvl();
                    isFirst = false;
                    if (fighter.getLvl() > maxLvl)
                        maxLvl = fighter.getLvl();
                }

                winners.stream().filter({ F -> F.isInvocation() && F.haveState(Constant.ETAT_CAPT_AME) }).forEach { F -> capturer.add(F)  };

                if (this.capturer.isEmpty() && SoulStone.isInArenaMap(this.mapOld.id)) // S'il y a des captureurs
                {
                    // FIXME Change template based on content
                    fullSoul = SoulStone(1, 7010, Constant.ITEM_POS_NO_EQUIPED, stats); // Cr�e la pierre d'�me
                    for (i in 0 until this.capturer.size) {
                        try {
                            var f: Fighter = this.capturer.get(Formulas.getRandomValue(0, this.capturer.size - 1)) // R�cup�re un captureur au hasard dans la liste
                            if (f != null && f.getPlayer() != null) {
                                if (f.getPlayer()!!.getObjetByPos(Constant.ITEM_POS_ARME) == null || !(f.getPlayer()!!.getObjetByPos(Constant.ITEM_POS_ARME)!!.template!!.type == Constant.ITEM_TYPE_PIERRE_AME)) {
                                    this.capturer.remove(f);
                                    continue;
                                }
                                var playerSoulStone: Couple<Int,Int> = Formulas.decompPierreAme(f.getPlayer()!!.getObjetByPos(Constant.ITEM_POS_ARME)!!)// R�cup�re les stats de la pierre �quipp�

                                if (playerSoulStone.second < maxLvl) {// Si la pierre est trop faible
                                    this.capturer.remove(f);
                                    continue;
                                }
                                if (Formulas.getRandomValue(1, 100) <= Formulas.totalCaptChance(playerSoulStone.first, f.getPlayer()!!)) {// Si le joueur obtiens la capture Retire la pierre vide au personnage et lui envoie ce changement
                                    var emptySoulStone: Int = f.getPlayer()!!.getObjetByPos(Constant.ITEM_POS_ARME)!!.guid
                                    f.getPlayer()!!.deleteItem(emptySoulStone);
                                    SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(f.getPlayer()!!, emptySoulStone);
                                    this.captWinner = f.id;
                                    break;
                                }
                            }
                        } catch (e: NullPointerException) {
                            log.error("unexpected error", e)
                        }
                    }
                }
            }
            //endregion

            //region Quest
            if (this.type == Constant.FIGHT_TYPE_PVM || this.type == Constant.FIGHT_TYPE_DOPEUL) {
                for (fighter in  winners) {
                    var player: Player = fighter.getPlayer()!!
                    if (player == null) continue;

                    // TODO: DIABU CALL LUA FOR QUEST UPDATE
                }
            }
            //endregion

            //region Apprivoisement
            var amande: Boolean = false
            var rousse: Boolean = false
            var doree: Boolean = false

            for (fighter in  loosers) {
                try {
                    if (fighter.getMob()!!.template.id == 171)
                        amande = true;
                    if (fighter.getMob()!!.template.id == 200)
                        rousse = true;
                    if (fighter.getMob()!!.template.id == 666)
                        doree = true;
                } catch (e: Exception) {
                    amande = false;
                    rousse = false;
                    doree = false;
                    break;
                }
            }
            if (amande || rousse || doree) {
                winners.stream().filter({ fighter -> fighter.isInvocation() && fighter.haveState(Constant.ETAT_APPRIVOISEMENT) }).forEach { F -> trainer.add(F)  };
                if (trainer.isEmpty()) {
                    for (i in 0 until trainer.size) {
                        try {
                            var f: Fighter = trainer.get(Formulas.getRandomValue(0, trainer.size - 1)) // R�cup�re un captureur au hasard dans la liste
                            var player: Player = f.getPlayer()!!
                            if (player.getObjetByPos(Constant.ITEM_POS_ARME) == null || !(player.getObjetByPos(Constant.ITEM_POS_ARME)!!.template!!.type == Constant.ITEM_TYPE_FILET_CAPTURE)) {
                                trainer.remove(f);
                                continue;
                            }
                            var chance: Int = Formulas.getRandomValue(1, 100)
                            var appriChance: Int = Formulas.totalAppriChance(amande, rousse, doree, player)
                            if (chance <= appriChance) {
                                // Retire le filet au personnage et lui envoie ce changement
                                var filet: Int = player.getObjetByPos(Constant.ITEM_POS_ARME)!!.guid
                                player.deleteItem(filet);
                                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(player, filet);
                                setTrainerWinner(f.id);
                                break;
                            }
                        } catch (e: NullPointerException) {
                            log.error("unexpected error", e)
                        }
                    }
                }
            }
            //endregion

            var memberGuild: Int = 0

            if (this.type == Constant.FIGHT_TYPE_PVT && win == 1)
                for (i in  winners)
                    if (i.getPlayer() != null)
                        if (i.getPlayer()!!.guildMember != null)
                            memberGuild++;

            var lvlLoosers: Int = 0
            var lvlWinners: Int = 0
            var lvlMaxLooser: Int = 0
            var lvlMax: Int? = null
            var lvlMin: Int? = null
            var challXp: Int = 0
            var nbbonus: Byte = 0
            for (c in  allChallenges.values)
                if (c != null && c.getWin())
                    challXp += c.getXp();

            for (entry in  loosers)
                lvlLoosers += entry.getLvl();

            for (entry in  winners) {
                lvlWinners += entry.getLvl();
                if (entry.getLvl() > lvlMaxLooser
                        && entry.getPlayer() != null)
                    lvlMaxLooser = entry.getLvl();
            }
            if (lvlLoosers > lvlWinners) {
                lvlMax = lvlLoosers;
                lvlMin = lvlWinners;
            } else {
                lvlMax = lvlWinners;
                lvlMin = lvlLoosers;
            }
            for (entry in  winners)
                if (entry.getLvl() > lvlMaxLooser / 3
                        && entry.getPlayer() != null)
                    nbbonus = (nbbonus + 1).toByte();

            if (lvlWinners <= 0)
                lvlWinners = 1;

            var mobs: MutableMap<Int,Int> = HashMap()
            loosers.stream().filter({ mob -> mob.getMob() != null }).forEach { mob -> 
                if (mobs.get(mob.getMob()!!.template.id) != null)
                    mobs.put(mob.getMob()!!.template.id, mobs.get(mob.getMob()!!.template.id)!! + 1); // Quantite
                else
                    mobs.put(mob.getMob()!!.template.id, 1);
            }

            Collections.sort(winners);
            var gains: MutableMap<Int,StringBuilder> = HashMap()

            //region Drop
            // Calcul the total prospecting.
            var totalProspecting: Int = 0
            var challengeFactor: Double = 0.0
            var starFactor: Double = if (this.getMobGroup() != null) (((this.getMobGroup().getStarBonus()).toDouble() / 100.0) + 1) else 1.0

            for (fighter in  winners) {
                if(fighter.canLoot()) {
                    continue;
                }
                totalProspecting += fighter.getPros();
            }

            if (starFactor < 1) starFactor = 1.0;
            if (totalProspecting < 0) totalProspecting = 0;
            // Calcul the total challenge percent.
            if (this.type == Constant.FIGHT_TYPE_PVM && this.allChallenges.isEmpty())
                for (challenge in  this.allChallenges.values)
                    if (challenge.getWin()) challengeFactor += challenge.getDrop();
            if (challengeFactor < 1) challengeFactor = 1.0;
            challengeFactor = 1 + challengeFactor / 100;

            totalProspecting = ((totalProspecting * challengeFactor)).toInt();

            var dropsPlayers: ArrayList<Drop> = ArrayList()
            var dropsMeats: ArrayList<Drop> = ArrayList()
            var dropsCollector: MutableCollection<GameObject>? = null
            var kamas: Couple<Int,Int>

            if (this.type == Constant.FIGHT_TYPE_PVT && win == 1) {
                var kamasCollector: Int = (Math.ceil((collector.kamas).toDouble() / (winners.size.toDouble()))).toInt()
                kamas = Couple(kamasCollector, kamasCollector);
                dropsCollector = ArrayList(this.collector.getDrops());
            } else {
                var minKamas: Int = 0
                var maxKamas: Int = 0
                for (fighter in  loosers) {
                    if(fighter.isInvocation()) continue;

                    minKamas += fighter.minKamasReward();
                    maxKamas += fighter.maxKamasReward();

                    val fTotalProspecting: Int = totalProspecting
                    fighter.drops().forEach { drop1 ->
                        when (drop1.getAction()) {
                            1 -> {
                                val drop = drop1.copy(fighter.getMob()!!.grade) ?: return@forEach
                                dropsMeats.add(drop);
                            }
                            else -> {
                                if (drop1.getCeil() * Config.rateProspectThreshold <= fTotalProspecting && fighter.getMob() != null) {
                                    val drop = drop1.copy(fighter.getMob()!!.grade) ?: return@forEach
                                    dropsPlayers.add(drop);
                                }
                            }
                        }
                    }
                }

                kamas = Couple(minKamas, maxKamas);
            }
            // Sort fighter by prospecting.
            var temporary1: ArrayList<Fighter> = ArrayList()
            var higherFighter: Fighter? = null
            while (temporary1.size < winners.size) {
                var currentProspecting: Int = -1
                for (fighter in  winners) {
                    if (fighter.getTotalStats().getEffect(Constant.STATS_ADD_PROS) > currentProspecting && temporary1.contains(fighter)) {
                        higherFighter = fighter;
                        currentProspecting = fighter.getTotalStats().getEffect(Constant.STATS_ADD_PROS);
                    }
                }
                temporary1.add(higherFighter!!);
            }
            winners.clear();
            winners.addAll(temporary1);
            val formatter: NumberFormat = DecimalFormat("#0.000")
            //endregion

            //region Stalk
            var curPlayer: Player? = null
            var stalk: Boolean = false
            var quantity: Int = 2
            if (this.type == Constant.FIGHT_TYPE_AGRESSION) {
                var isAlone: Boolean = true

                for (fighter in  winners)
                    if (fighter.isInvocation())
                        curPlayer = fighter.getPlayer();

                for (fighter in  winners)
                    if (fighter.getPlayer() != curPlayer && fighter.isInvocation())
                        isAlone = false;

                if (isAlone) {
                    for (fighter in  loosers) {
                        if (fighter.isInvocation() && curPlayer != null && curPlayer.stalk != null && curPlayer.stalk!!.target == fighter.getPlayer()) {
                            SocketManager.GAME_SEND_MESSAGE(curPlayer, "Thomas Sacre : Contrat fini, reviens me voir pour récuperer ta récompense.", "000000");
                            curPlayer.stalk!!.time = -2L;
                            stalk = true;
                            fighter.setTraqued(true);

                            var stalkTarget: Stalk = fighter.getPlayer()!!.stalk!!

                            if (stalkTarget != null)
                                if (stalkTarget.target == curPlayer)
                                    quantity = 4;

                            var obj: GameObject = World.world.getObjTemplate(10275)!!.createNewItem(quantity, false)!!
                            if (curPlayer.addItem(obj, true, false))
                                World.world.addGameObject(obj);
                            kamas = Couple(1000 * quantity, 1000 * quantity);
                            curPlayer.addKamas(1000L * quantity);
                        }
                    }
                }

                if (stalk) {
                    var traqued: Player? = null
                    curPlayer = null;

                    for (fighter in  loosers)
                        if (fighter.getPlayer() != null)
                            if (fighter.getPlayer()!!.stalk != null)
                                traqued = fighter.getPlayer()!!.stalk!!.target;

                    if (traqued != null)
                        for (fighter in  winners)
                            if (fighter.getPlayer() == traqued)
                                curPlayer = traqued;

                    if (curPlayer != null) {
                        kamas = Couple(1000 * quantity, 1000 * quantity);
                        curPlayer.addKamas(1000L * quantity);
                        var obj: GameObject = World.world.getObjTemplate(10275)!!.createNewItem(quantity, false)!!
                        if (curPlayer.addItem(obj, true, false))
                            World.world.addGameObject(obj);
                        stalk = true;
                    }
                }
            }
            //endregion Stalk

            //region Heroic
            var list: Map<Player, String>? = null
            var objects: ArrayList<GameObject>? = null

            if (Config.modeHeroic) {
                when (this.type) {
Constant.FIGHT_TYPE_AGRESSION -> {val objects1: ArrayList<GameObject> = ArrayList()
                        var money: Int = 0

                        for (fighter in  loosers) {
                            val player: Player = fighter.getPlayer()!!
                            if (player != null) {
                                objects1.addAll(player.items.values);
                                money = (money + player.kamas).toInt();
                                totalXP += player.exp * 10 / 100;

                                var iterator1: MutableIterator<GameObject> = ArrayList(player.items.values).iterator()
                                while (iterator1.hasNext()) {
                                    var toRemove: GameObject = iterator1.next()
                                    if (toRemove != null) {
                                        player.removeItem(toRemove.guid, toRemove.quantity, true, false);
                                    }
                                    iterator1.remove();
                                }
                                player.kamas = 0;
                            }
                        }

                        kamas = Couple(money, money);
                        list = Fight.give(objects1, winners);

                        
}
Constant.FIGHT_TYPE_PVM -> {try {
                            val group: MonsterGroup = this.getMobGroup()

                            if (team) { // Players have loose the fight, mob win the fight
                                objects = ArrayList();
                                for (fighter in  loosers) {
                                    val player: Player = fighter.getPlayer()!!
                                    if (player != null)
                                        objects.addAll(player.items.values);
                                }

                                if (group.isFix) {
                                    var infos: String = this.mapOld.id.toString() + "," + group.cellId
                                    if (GameMap.fixMobGroupObjects.get(infos) != null) {
                                        objects.addAll(GameMap.fixMobGroupObjects[infos]!!);
                                        GameMap.fixMobGroupObjects.remove(infos);
                                        GameMap.fixMobGroupObjects.put(infos, objects);
                                    } else {
                                        GameMap.fixMobGroupObjects.put(infos, objects);
                                        //((HeroicMobsGroupsData) DatabaseManager.get(HeroicMobsGroupsData::class.java)).insertFix(this.getMapOld().getId(), group, objects);
                                    }
                                } else {
                                    group.getObjects()!!.addAll(objects);
                                    this.mapOld.respawnGroup(group);
                                    //((HeroicMobsGroupsData) DatabaseManager.get(HeroicMobsGroupsData::class.java)).insert(this.getMapOld().getId(), group);
                                }
                            } else { // mob loose..
                                list = Fight.give(if (group.isFix) GameMap.fixMobGroupObjects[this.mapOld.id.toString() + "," + group.cellId]!! else group.getObjects()!!, winners);
                                if (group.isFix) this.mapOld.spawnAfterTimeGroup();
                            }
                        } catch (e: Exception) {
                            log.error("unexpected error", e)
                        }
                        
}
}
            }
            //endregion

            var t: Long = System.currentTimeMillis()
            //region Winners
            for (i in  winners) {
                if (i.isInvocation() && i.getMob() != null && i.getMob()!!.template.id != 285)
                    continue;
                if (i is CloneFighter)
                    continue;

                val player: Player = i.getPlayer()!!

                if (player != null && type != Constant.FIGHT_TYPE_CHALLENGE)
                    player.calculTurnCandy();
                if (type == Constant.FIGHT_TYPE_PVT || type == Constant.FIGHT_TYPE_PVM || type == Constant.FIGHT_TYPE_CHALLENGE || type == Constant.FIGHT_TYPE_DOPEUL) {
                    var drops: StringBuilder = StringBuilder()
                    var xpPlayer: Long = 0
                    var xpGuild: Long = 0
                    var xpMount: Long = 0
                    var winKamas: Int

                    var XP: AtomicReference<Long> = AtomicReference()
                    /** Xp,kamas **/
                    if (player != null) {
                        xpPlayer = FormuleOfficiel.getXp(i, winners, totalXP, nbbonus, if (getMobGroup() != null) getMobGroup().getStarBonus() else 0, challXp, lvlMax, lvlMin, lvlLoosers, lvlWinners);
                        XP.set(xpPlayer);

                        if (this.type == Constant.FIGHT_TYPE_PVT && win == 1) {
                            if (player != null && memberGuild != 0)
                                if (player.guildMember != null)
                                    xpGuild = (Math.floor(this.collector.xp.toDouble() / memberGuild)).toLong();
                        } else {
                            xpGuild = Formulas.getGuildXpWin(i, XP);
                        }

                        if (player.onMount) {
                            xpMount = Formulas.getMountXpWin(i, XP);
                            player.mount!!.addXp(xpMount);
                            SocketManager.GAME_SEND_Re_PACKET(player, "+", player.mount!!);
                        }
                    }


                    winKamas = (if (this.type == Constant.FIGHT_TYPE_PVT && win == 1) Math.floor(kamas.first.toDouble() / winners.size) else Formulas.getKamasWin(i, winners, kamas.first, kamas.second)).toInt();
                    /** Xp,kamas **/
                    /**********************/
                    /**       Drop       **/
                    /**********************/
                    var objectsWon: MutableMap<Int,Int> = HashMap()
                    var itemWon2: MutableMap<Int,Int> = HashMap()
                    if (this.type == Constant.FIGHT_TYPE_PVT && win == 1 && dropsCollector != null) {
                        var objectPerPlayer: Int = (Math.floor((dropsCollector.size.toDouble()) / (winners.size.toDouble()))).toInt()
                        var counter: Int = 0
                        var temporary2: ArrayList<GameObject> = ArrayList(dropsCollector)
                        Collections.shuffle(temporary2);

                        for (obj in  temporary2) {
                            if (counter <= objectPerPlayer) {
                                objectsWon.put(obj.template!!.id, obj.quantity);
                                dropsCollector.remove(obj);
                                World.world.removeGameObject(obj.guid);
                                counter++;
                            }
                        }
                    } else {
                        var temporary3: ArrayList<Drop> = ArrayList(dropsPlayers)
                        if (this.type == Constant.FIGHT_TYPE_PVM && this.monsterGroup != null && this.monsterGroup.mobs.size > 1 && Formulas.getRandomValue(0, 100) >= 98) {
                            var templates: List<ObjectTemplate> = World.world.getEtherealWeapons(if (i.isInvocation()) i.getInvocator()!!.getLvl() else i.getLvl())
                            if (templates.isEmpty()) {
                                var template: ObjectTemplate = templates.get(templates.size - 1)
                                temporary3.add(World.Drop(template.id, 5.0, 0));
                            }
                        }
                        Collections.shuffle(temporary3);

                        for (drop in  temporary3) {
                            var prospecting: Double = i.getPros() / 100.0
                            if (prospecting < 1) prospecting = 1.0;


                            val jet: Double = Math.random() * 100
                            val chance: Double = drop.getLocalPercent() * prospecting * World.world.getConquestBonus(player) * challengeFactor * starFactor * Config.rateDrop
                            var ok: Boolean = false

                            when (drop.getAction()){ 4 -> {if (player != null && World.world.conditionManager.validConditions(player, "QE=" + drop.getCondition()))
                                        ok = true;
                                    
}
}
                            if (jet < chance || ok) {
                                var objectTemplate: ObjectTemplate = World.world.getObjTemplate(drop.getObjectId())!!

                                if (objectTemplate == null)
                                    continue;

                                quantity = 1;
                                var itsOk: Boolean = false
                                var unique: Boolean = false
                                when (drop.getAction()){ -2 -> {unique = true;
                                        itsOk = true;
                                        
}
-1 -> {itsOk = true;
                                        
}
1 -> {
}
2 -> {for (id in  drop.getCondition()!!.split(","))
                                            if (id == map!!.id.toString())
                                                itsOk = true;
                                        
}
3 -> {if (this.mapOld.subArea == null)
                                            break;
                                        when (drop.getCondition()) {
"0" -> {if (this.mapOld.subArea!!.alignment == 2)
                                                    itsOk = true;
                                                
}
"1" -> {if (this.mapOld.subArea!!.alignment == 1)
                                                    itsOk = true;
                                                
}
"2" -> {if (this.mapOld.subArea!!.alignment == 2)
                                                    itsOk = true;
                                                
}
"3" -> {if (this.mapOld.subArea!!.alignment == 3)
                                                    itsOk = true;
                                                
}
else -> {itsOk = true;
                                                
}
}
                                        
}
4 -> {if (World.world.conditionManager.validConditions(player, "QE=" + drop.getCondition()))
                                            itsOk = true;
                                        
}
5 -> {if (player == null) break;
                                        if (player.getNbItemTemplate(objectTemplate.id) > 0) break;
                                        itsOk = true;
                                        
}
6 -> {if (player == null) break;
                                        var item: Int = Integer.parseInt(drop.getCondition())
                                        if (item == 2039) {
                                            if (this.map!!.id.toShort() == (7388.toShort())) {
                                                if (player.hasItemTemplate(item, 1, false))
                                                    itsOk = true;
                                            } else
                                                itsOk = false;
                                        } else if (player.hasItemTemplate(item, 1, false))
                                            itsOk = true;
                                        
}
7 -> {if (player == null) break;
                                        if (player.hasItemTemplate(objectTemplate.id, 1, false))
                                            break;
                                        for (id in  drop.getCondition()!!.split(",")) {
                                            if (id == this.map!!.id.toString()) {
                                                itsOk = true;
                                            }
                                        }
                                        
}
8 -> {var split: Array<String> = drop.getCondition()!!.split(",").toTypedArray()
                                        quantity = Formulas.getRandomValue(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
                                        itsOk = true;
                                        
}
999 -> {itsOk = true;
                                        
}
else -> {itsOk = true;
                                        
}
}
                                if (itsOk) {
                                    objectsWon.put(objectTemplate.id, objectsWon[objectTemplate.id] ?: quantity + quantity);
                                    if (unique) dropsPlayers.remove(drop);
                                }
                            }
                        }
                        /** Drop Chasseur **/
                        if (player != null) {
                            var temporary: ArrayList<Drop> = ArrayList(dropsMeats)
                            Collections.shuffle(temporary);

                            var weapon: GameObject = player.getObjetByPos(Constant.ITEM_POS_ARME)!!
                            var ok: Boolean = weapon != null && weapon.stats.getEffect(795) == 1

                            if (ok) {
                                for (drop in  temporary) {
                                    val jet: Double = formatter.format(Math.random() * 100).replace(',', '.').toDouble()
                                            val chance = formatter.format(drop.getLocalPercent() * (i.getPros() / 100.0)).replace(',', '.').toDouble()

                                    if (jet < chance) {
                                        var objectTemplate: ObjectTemplate = World.world.getObjTemplate(drop.getObjectId())!!

                                        if (drop.getAction() == 1 && objectTemplate != null && player.getMetierByID(41) != null && player.getMetierByID(41)!!.get_lvl() >= drop.getLevel())
                                            itemWon2.put(objectTemplate.id, (itemWon2[objectTemplate.id] ?: 0) + 1);
                                    }
                                }
                            }
                        }
                    }
                    if (player != null || i.getMob() != null && i.getMob()!!.template.id == MobFighter.ENUTROF_CHEST_ID) {
                        if (player != null) {
                            if (this.getTrainerWinner() != -1 && i.id == this.getTrainerWinner() && player.mount == null) {
                                var color: Int = Formulas.getCouleur(amande, rousse, doree)

                                var mount: Mount = Mount(color, i.id, true)
                                player.mount = mount;
                                SocketManager.GAME_SEND_Re_PACKET(player, "+", mount);
                                SocketManager.GAME_SEND_Rx_PACKET(player);
                                SocketManager.GAME_SEND_STATS_PACKET(player);
                                if (drops.length > 0) drops.append(",");
                                when (color){  20 -> {drops.append("7807~1");
                                        
}
10 -> {drops.append("7809~1");
                                        
}
18 -> {drops.append("7864~1");
                                        
}
}
                            }
                            if (i.id == this.captWinner && fullSoul != null) {
                                if (drops.length > 0)
                                    drops.append(",");
                                drops.append(fullSoul.template!!.id).append("~").append(1);
                                if (player.addItem(fullSoul, false, false))
                                    World.world.addGameObject(fullSoul);
                            }
                            if (list != null) {
                                var value: String? = list.get(i.getPlayer())
                                if (value != null && value.isEmpty())
                                    drops.append(if (drops.length == 0) "" else ",").append(value);
                            }
                        }

                        var target: Player = if (player != null) player else i.getInvocator()!!.getPlayer()!!

                        val dropsToAttribute: MutableMap<ObjectTemplate,Int> = HashMap()
                        for (entry in  objectsWon.entries) {
                            var objectTemplate: ObjectTemplate = World.world.getObjTemplate(entry.key)!!

                            if (player == null && i.getInvocator() == null) break;
                            if (objectTemplate == null || i is CloneFighter) continue;
                            if (drops.length > 0) drops.append(",");

                            drops.append(entry.key).append("~").append(entry.value);
                            dropsToAttribute.put(objectTemplate, entry.value);
                        }
                        for (entry in  itemWon2.entries) {
                            var objectTemplate: ObjectTemplate = World.world.getObjTemplate(entry.key)!!

                            if (player == null && i.getInvocator()!!.getPlayer() == null) break;
                            if (objectTemplate == null) continue;
                            if (drops.length > 0) drops.append(",");

                            drops.append(entry.key).append("~").append(entry.value);
                            dropsToAttribute.put(objectTemplate, entry.value);
                        }

                        TimerWaiter.addNext({ {
                            for (entry in  dropsToAttribute.entries) {
                                var template: ObjectTemplate = entry.key
                                if (template.type == 32 && player != null) {
                                    player.setMascotte(template.id);
                                } else if (template.type == Constant.ITEM_TYPE_FAMILIER && Config.maxPets) {
                                    var obj: GameObject = template.createNewItem(1, false)!!
                                    if (target.addItem(obj, true, false))//Si le joueur n'avait pas d'item similaire
                                        World.world.addGameObject(obj);
                                    SocketManager.GAME_SEND_Ow_PACKET(target);
                                } else {
                                    var newObj: GameObject = World.world.getObjTemplate(template.id)!!.createNewItemWithoutDuplication(target.items.values, entry.value, false)!!
                                    if (World.world.getObjTemplate(template.id)!!.type == Constant.ITEM_TYPE_CERTIF_MONTURE) {
                                        //obj.setMountStats(this.getPlayer(), null);
                                        var mount: Mount = Mount(Constant.getMountColorByParchoTemplate(newObj.template!!.id), target.id, false)
                                        newObj.clearStats();
                                        newObj.stats.addOneStat(995, mount.id);
                                        newObj.txtStat.put(996, target.name);
                                        newObj.txtStat.put(997, mount.name!!);
                                        mount.setToMax();
                                    }

                                    if (newObj != null && target.items.get(newObj.guid) == null) {
                                        if (target.addItem(newObj, true, false))
                                            World.world.addGameObject(newObj);
                                    } else {
                                        SocketManager.GAME_SEND_UPDATE_OBJECT_DISPLAY_PACKET(target, newObj);
                                    }
                                }
                            }
                        } }, 1000);
                        if (this.type == Constant.FIGHT_TYPE_DOPEUL) {
                            for (F in  loosers) {
                                var mob: MonsterGrade = F.getMob()!!
                                var m: Monster = mob.template
                                if (m == null)
                                    continue;
                                var IDmob: Int = m.id
                                if (drops.length > 0) drops.append(",");
                                var id: Int = Constant.getCertificatByDopeuls(IDmob)
                                if (id == -1) continue;
                                drops.append(id).append("~1");
                                // Certificat :
                                var OT2: ObjectTemplate = World.world.getObjTemplate(Constant.getCertificatByDopeuls(IDmob))!!
                                if (OT2 != null) {
                                    var obj2: GameObject = OT2.createNewItem(1, false)!!
                                    if (player.addItem(obj2, true, false))// Si le joueur n'avait pas d'item similaire
                                        World.world.addGameObject(obj2);
                                    obj2.refreshStatsObjet("325#0#0#" + System.currentTimeMillis());
                                    ((DatabaseManager.get(PlayerData::class.java) as PlayerData)).update(player);
                                    SocketManager.GAME_SEND_Ow_PACKET(player);
                                }
                            }
                        }
                        if (this.type == Constant.FIGHT_TYPE_PVM && player != null) {
                            var bouftou: Int = 0
                            var tofu: Int = 0

                            for (mob in  getMobGroup().mobs.values) {
                                when (mob.template.id){ 793 -> {bouftou++;
                                        
}
794 -> {tofu++;
                                        
}
}
                            }

                            if (Config.modeHalloween) {
                                if ((bouftou > 0 || tofu > 0) && player.hasEquiped(976)) {
                                    if (bouftou > tofu) {
                                        drops.append(if (drops.length > 0) "," else "").append("8169~1");
                                        player.setMalediction(8169);
                                        player.setFullMorph(Formulas.getRandomValue(16, 20), false, false);
                                    } else if (tofu > bouftou) {
                                        drops.append(if (drops.length > 0) "," else "").append("8170~1");
                                        player.setMalediction(8170);
                                        player.setFullMorph(Formulas.getRandomValue(21, 25), false, false);
                                    } else {
                                        when (Formulas.getRandomValue(1, 2)) {
                                            1 -> {drops.append(if (drops.length > 0) "," else "").append("8169~1");
                                                player.setMalediction(8169);
                                                player.setFullMorph(Formulas.getRandomValue(16, 20), false, false);
                                            }
                                            2 -> {drops.append(if (drops.length > 0) "," else "").append("8170~1");
                                                player.setMalediction(8170);
                                                player.setFullMorph(Formulas.getRandomValue(21, 25), false, false);
                                            }
                                        }
                                    }
                                }
                            }

                            when (player.curMap.id) {
                                8984 -> {var obj: GameObject = World.world.getObjTemplate(8012)!!.createNewItem(1, false)!!
                                    if (player.addItem(obj, true, false))
                                        World.world.addGameObject(obj);
                                    drops.append(if (drops.length > 0) "," else "").append("8012~1");
                                }
                            }
                        }
                        /**********************/
                        /**     Fin Drop     **/
                        /**********************/

                        if (player != null) {
                            xpPlayer = XP.get();
                            if (xpPlayer != 0L) {
                                if (player.morphMode) {
                                    var obj: GameObject = player.getObjetByPos(Constant.ITEM_POS_ARME)!!
                                    if (obj != null)
                                        if (Constant.isIncarnationWeapon(obj.template!!.id))
                                            if (player.addXpIncarnations(xpPlayer))
                                                i.levelUp = true;
                                } else if (player.addXp(xpPlayer))
                                    i.levelUp = true;
                            }

                            if (winKamas != 0)
                                player.addKamas(winKamas.toLong());
                            if (xpGuild > 0 && player.guildMember != null)
                                player.guildMember!!.giveXpToGuild(xpGuild);
                        }
                        if (winKamas != 0 && i.isInvocation() && !(i is CloneFighter) && i.getInvocator()!!.getPlayer() != null)
                            i.getInvocator()!!.getPlayer()!!.addKamas(winKamas.toLong());
                    }

                    var p: StringBuilder = StringBuilder()
                    p.append("2;");
                    p.append(i.id).append(";");
                    p.append(i.getPacketsName()).append(";");
                    p.append(i.getLvl()).append(";");
                    if (i.canLoot()) {
                        p.append(i.getDefaultGfx()).append(";");
                    }
                    p.append(if (i.isDead) "1" else "0").append(";");
                    p.append(i.xpString(";")).append(";");
                    p.append(if (xpPlayer == 0L) "" else xpPlayer).append(";");
                    p.append(if (xpGuild == 0L) "" else xpGuild).append(";");
                    p.append(if (xpMount == 0L) "" else xpMount).append(";");
                    p.append(drops).append(";");// Drop
                    p.append(if (winKamas == 0) "" else winKamas).append("|");
                    gains.put(i.id, p);
                } else {
                    // Si c'est un neutre, on ne gagne pas de points
                    var winH: Int = 0
                    var winD: Int = 0
                    var winKamas: Int = 0
                    var winXp: Long = 0

                    if (this.type == Constant.FIGHT_TYPE_AGRESSION) {
                        if (i.isInvocation() || i.getPrism() != null || i.getMob() != null || (i is CloneFighter))
                            continue;

                        if (this.type == Constant.FIGHT_TYPE_AGRESSION) {
                            if (this.init1.getPlayer()!!.alignment != 0 && this.init0.getPlayer()!!.alignment != 0) {
                                if (this.init1.getPlayer()!!.getAccount().currentIp.compareTo(this.init0.getPlayer()!!.getAccount().currentIp) != 0 || Config.allowMulePvp)
                                    winH = Formulas.calculHonorWin(winners, loosers, i, false);
                                if (player.deshonor > 0)
                                    winD = -1;
                            }
                        } else if (this.type == Constant.FIGHT_TYPE_CONQUETE)
                            winH = Formulas.calculHonorWin(winners, loosers, i, true);

                        if (player.alignment != 0) {
                            if (player.honor + winH < 0)
                                winH = -player.honor;
                            player.addHonor(winH);
                            player.deshonor = player.deshonor + winD;
                        }

                        var xpTable: ExperienceTables.ExperienceTable = World.world.experiences!!.pvp
                        var maxHonor: Long = xpTable.maxXpAt(player.grade)

                        var temporary: StringBuilder = StringBuilder()
                        temporary.append("2;").append(i.id).append(";").append(i.getPacketsName()).append(";").append(i.getLvl()).append(";");
                        if (i.getPlayer() != null) {
                            temporary.append(i.getDefaultGfx()).append(";");
                        }
                        temporary.append(if (i.isDead) "1" else "0").append(";");
                        temporary.append(if (player.alignment != Constant.ALIGNEMENT_NEUTRE) xpTable.minXpAt(player.grade) else 0).append(";");
                        temporary.append(player.honor).append(";");
                        temporary.append(if (player.alignment != Constant.ALIGNEMENT_NEUTRE) maxHonor else 0).append(";");
                        temporary.append(winH).append(";");
                        temporary.append(player.grade).append(";");
                        temporary.append(player.deshonor).append(";");
                        temporary.append(winD);
                        temporary.append(";");
                        temporary.append(if (stalk) "10275~" + quantity else "");
                        if (Config.modeHeroic && list != null) {
                            var value = list.get(player)
                            if (value != null)
                                if (value.isEmpty())
                                    temporary.append(if (stalk) "," else "").append(value);
                            winXp = totalXP / winners.size;
                            winKamas = Formulas.getRandomValue(kamas.first, kamas.second);
                            player.addXp(winXp);
                        }
                        temporary.append(";").append(winKamas).append(";0;0;0;").append(winXp).append("|");
                        gains.put(i.id, temporary);
                    } else if (this.type == Constant.FIGHT_TYPE_CONQUETE) {
                        if (player != null) {
                            winH = ((player.honor * 0.1)).toInt();
                            if (winH == 0)
                                winH = 50;
                            if (winH > 500)
                                winH = 500;
                            if (player.honor + winH < 0)
                                winH = -player.honor;
                            player.addHonor(winH);
                            if (player.deshonor - winD < 0)
                                winD = 0;
                            player.deshonor = player.deshonor - winD;

                            var xpTable: ExperienceTables.ExperienceTable = World.world.experiences!!.pvp
                            var maxHonor: Long = xpTable.maxXpAt(player.grade)

                            var temporary: StringBuilder = StringBuilder()
                            temporary.append("2;").append(i.id).append(";").append(i.getPacketsName()).append(";").append(i.getLvl()).append(";");
                            temporary.append(i.getDefaultGfx()).append(";");
                            temporary.append(if (i.isDead) "1" else "0").append(";");
                            temporary.append(if (player.alignment != Constant.ALIGNEMENT_NEUTRE) xpTable.minXpAt(player.grade) else 0).append(";");
                            temporary.append(player.honor).append(";");
                            temporary.append(if (player.alignment != Constant.ALIGNEMENT_NEUTRE) maxHonor else 0).append(";");
                            temporary.append(winH).append(";");
                            temporary.append(player.grade).append(";");
                            temporary.append(player.deshonor).append(";");
                            temporary.append(winD);
                            temporary.append(";;0;0;0;0;0|");
                            gains.put(i.id, temporary);
                        } else {
                            val prism: Prism = i.getPrism()!!
                            winH = winH * 5;
                            if (prism.honor + winH < 0) winH = -prism.honor;
                            winH *= 3;
                            prism.addHonor(winH);

                            var xpTable: ExperienceTables.ExperienceTable = World.world.experiences!!.pvp
                            var maxHonor: Long = xpTable.maxXpAt(prism.grade)

                            var temporary: StringBuilder = StringBuilder()
                            temporary.append("2;").append(i.id).append(";").append(i.getPacketsName()).append(";").append(i.getLvl()).append(";");
                            // Always false
//                            if(player != null) {
//                                temporary.append(i.getDefaultGfx()).append(";");
//                            }
                            temporary.append(if (i.isDead) "1" else "0").append(";");
                            temporary.append(xpTable.minXpAt(prism.level)).append(";");
                            temporary.append(prism.honor).append(";");
                            temporary.append(maxHonor).append(";");
                            temporary.append(winH).append(";");
                            temporary.append(prism.level).append(";");
                            temporary.append("0;0;;0;0;0;0;0|");

                            gains.put(i.id, temporary);
                        }
                    }
                }
            }

            Collections.shuffle(winners);
            var invoks: MutableMap<Int,Int> = HashMap()

            winners.stream()
                    .filter({ i -> i.isInvocation() && i.getMob() != null })
                    .filter({ i -> i.getMob()!!.template.id == MobFighter.ENUTROF_CHEST_ID })
                    .forEach { i -> invoks.put(i.id, i.getInvocator()!!.id)  };

            if (invoks != null && invoks.isEmpty())
                for (entry in  invoks.entries)
                    winners = this.insertChestsAfterSummoner(winners, entry.value, entry.key);

            winners.stream()
                    .filter(Fighter::canLoot)
                    .forEach { fighter -> packet.append(gains[fighter.id].toString())  };

            //endregion End winner
            t = System.currentTimeMillis();
            //region Looser
            for (i in  loosers) {
                if (i.isInvocation() && i.getMob() != null && i.getMob()!!.template.id != 285)
                    continue;
                if (i is CloneFighter)
                    continue;

                val player: Player = i.getPlayer()!!

                if (player != null && this.type != Constant.FIGHT_TYPE_CHALLENGE)
                    player.calculTurnCandy();
                if (this.type != Constant.FIGHT_TYPE_AGRESSION && this.type != Constant.FIGHT_TYPE_CONQUETE) {
                    var temporary: StringBuilder = StringBuilder()
                    temporary.append("0;").append(i.id).append(";").append(i.getPacketsName()).append(";").append(i.getLvl()).append(";");
                    temporary.append(";").append(if (i.getPdv() == 0 || i.hasLeft() || i.isDead) 1 else 0).append(";");
                    temporary.append(i.xpString(";")).append(";;;;|");
                    packet.append(temporary);
                } else {
                    // Si c'est un neutre, on ne gagne pas de points
                    var winH: Int = 0
                    var winD: Int = 0
                    if (this.type == Constant.FIGHT_TYPE_AGRESSION) {
                        if (this.init1.getPlayer()!!.alignment != 0 && this.init0.getPlayer()!!.alignment != 0)
                            if (this.init1.getPlayer()!!.getAccount().currentIp.compareTo(this.init0.getPlayer()!!.getAccount().currentIp) != 0 || Config.allowMulePvp)
                                winH = Formulas.calculHonorWin(winners, loosers, i, false);

                        if (player == null)
                            continue;
                        if (player.alignment != 0) {
                            player.remHonor(if (player.honor + winH < 0) -player.honor else -winH);
                            if (player.deshonor - winD < 0)
                                winD = 0;
                            player.deshonor = player.deshonor - winD;
                        }

                        var xpTable: ExperienceTables.ExperienceTable = World.world.experiences!!.pvp
                        var maxHonor: Long = xpTable.maxXpAt(player.grade)

                        packet.append("0;").append(i.id).append(";").append(i.getPacketsName()).append(";").append(i.getLvl()).append(";");
                        packet.append(i.getDefaultGfx()).append(";").append(if (i.isDead) "1" else "0").append(";");
                        packet.append(if (player.alignment != Constant.ALIGNEMENT_NEUTRE) xpTable.minXpAt(player.grade) else 0).append(";");
                        packet.append(player.honor).append(";");
                        packet.append(if (player.alignment != Constant.ALIGNEMENT_NEUTRE) maxHonor else 0).append(";");
                        packet.append(winH).append(";");
                        packet.append(player.grade).append(";");
                        packet.append(player.deshonor).append(";");
                        packet.append(winD);
                        packet.append(";;0;0;0;0;0|");
                    } else if (this.type == Constant.FIGHT_TYPE_CONQUETE) {
                        winH = Formulas.calculHonorWin(winners, loosers, i, true);

                        if (player != null) {
                            winH = 0;
                            if (player.deshonor - winD < 0)
                                winD = 0;

                            var xpTable: ExperienceTables.ExperienceTable = World.world.experiences!!.pvp
                            var maxHonor: Long = xpTable.maxXpAt(player.grade)

                            player.deshonor = player.deshonor - winD;
                            packet.append("0;").append(i.id).append(";").append(i.getPacketsName()).append(";").append(i.getLvl()).append(";");
                            packet.append(i.getDefaultGfx()).append(";").append(if (i.isDead) "1" else "0").append(";");
                            packet.append(if (player.alignment != Constant.ALIGNEMENT_NEUTRE) xpTable.minXpAt(player.grade) else 0).append(";");
                            packet.append(player.honor).append(";");
                            packet.append(if (player.alignment != Constant.ALIGNEMENT_NEUTRE) maxHonor else 0).append(";");
                            packet.append(winH).append(";");
                            packet.append(player.grade).append(";");
                            packet.append(player.deshonor).append(";");
                            packet.append(winD);
                            packet.append(";;0;0;0;0;0|");
                        } else {
                            var prism: Prism = i.getPrism()!!

                            var xpTable: ExperienceTables.ExperienceTable = World.world.experiences!!.pvp
                            if (prism.honor + winH < 0)
                                winH = -prism.honor;
                            var maxHonor: Long = xpTable.maxXpAt(prism.level)

                            prism.addHonor(winH);
                            packet.append("0;").append(i.id).append(";").append(i.getPacketsName()).append(";").append(i.getLvl()).append(";");
                            packet.append(i.getDefaultGfx()).append(";").append(if (i.isDead) "1" else "0").append(";");
                            packet.append(xpTable.minXpAt(prism.level)).append(";");
                            packet.append(prism.honor).append(";");
                            packet.append(maxHonor).append(";");
                            packet.append(winH).append(";");
                            packet.append(prism.level).append(";");
                            packet.append("0;0;;0;0;0;0;0|");
                        }
                    }
                }
            }
            /** End Looser **/
            if (Collector.getCollectorByMapId(map!!.id) != null && type == Constant.FIGHT_TYPE_PVM) {
                var collector: Collector = Collector.getCollectorByMapId(map!!.id)!!

                var winxp: Long = FormuleOfficiel.getXp(collector, winners, totalXP, nbbonus, if (getMobGroup() != null) getMobGroup().getStarBonus() else 0, challXp, lvlMax, lvlMin, lvlLoosers, lvlWinners) / 10
                var winkamas: Long = (Math.floor(Formulas.getKamasWinPerco(kamas.first, kamas.second).toDouble())).toLong()

                collector.xp = collector.xp + winxp;
                collector.kamas = collector.kamas + winkamas;
                var guild: Guild = World.world.getGuild(collector.guildId)!!

                packet.append("5;").append(collector.id).append(";").append(collector.getFullName()).append(";").append(World.world.getGuild(collector.guildId)!!.lvl).append(";0;");
                packet.append(guild.lvl).append(";");
                packet.append(guild.xp).append(";");
                packet.append(World.world.getGuildXpMax(guild.lvl)).append(";");
                packet.append(";");// XpGagner
                packet.append(winxp).append(";");// XpGuilde
                packet.append(";");// Monture

                var drops: String = ""
                var temporary: ArrayList<Drop> = ArrayList(dropsPlayers)
                Collections.shuffle(temporary);
                var objectsWon: MutableMap<Int,Int> = HashMap()

                if (collector.getPodsTotal() < collector.getMaxPod()) {
                    for (drop in  temporary) {
                        val jet: Double = formatter.format(Math.random() * 100).replace(',', '.').toDouble()
                        val chance: Double = (drop.getLocalPercent() * (World.world.getGuild(collector.guildId)!!.getStats(Constant.STATS_ADD_PROS) / 100.0)).toInt().toDouble()

                        if (jet < chance) {
                            var objectTemplate: ObjectTemplate = World.world.getObjTemplate(drop.getObjectId())!!

                            if (objectTemplate == null)
                                continue;

                            var itsOk: Boolean = false
                            var unique: Boolean = false
                            when (drop.getAction()) {
                                -2 -> {
                                    unique = true;
                                    itsOk = true;
                                }
                                -1 -> {// All items without condition.
                                    itsOk = true;
                                }
                                1 -> {// Is meat so..
                                }
                                2 -> {// Verification of the condition ( MAP )
                                    for (id in drop.getCondition()!!.split(","))
                                        if (id.equals(map!!.id.toString() + ""))
                                            itsOk = true;
                                }
                                3 -> {// Alignement
                                    if (this.mapOld.subArea != null) {
                                        when (drop.getCondition()) {
                                            "0" -> {if (this.mapOld.subArea!!.alignment == 2)
                                                itsOk = true;
                                            }
                                            "1" -> {if (this.mapOld.subArea!!.alignment == 1)
                                                itsOk = true;
                                            }
                                            "2" -> {if (this.mapOld.subArea!!.alignment == 2)
                                                itsOk = true;
                                            }
                                            "3" -> {if (this.mapOld.subArea!!.alignment == 3)
                                                itsOk = true;
                                            }
                                            else -> {itsOk = true;
                                            }
                                        }
                                    }
                                }
                                4 -> {if (objectTemplate.id == 2553)//Gros boulet
                                    itsOk = true;
                                }
                                5 -> {
                                    itsOk = false;
                                }
                                6, 7 -> { // Les percepteurs ne font pas de qu\u00eates
                                }
                                else -> {
                                    itsOk = true;
                                }
                            }

                            if (itsOk) {
                                objectsWon.put(objectTemplate.id, (objectsWon[objectTemplate.id] ?: 0) + 1);

                                if (unique)
                                    dropsPlayers.remove(drop);
                            }
                        }
                    }

                    for (entry in  objectsWon.entries) {
                        var objectTemplate: ObjectTemplate = World.world.getObjTemplate(entry.key)!!

                        if (objectTemplate == null || collector.getPodsTotal() + objectTemplate.pod * entry.value >= collector.getMaxPod())
                            continue;
                        if (drops.isEmpty()) drops += ",";

                        drops += entry.key.toString() + "~" + entry.value;

                        var newObj: GameObject = World.world.getObjTemplate(objectTemplate.id)!!.createNewItemWithoutDuplication(collector.getOjects().values, entry.value, false)!!

                        if (newObj != null && collector.getOjects().get(newObj.guid) == null) {
                            if (collector.addObjet(newObj))
                                World.world.addGameObject(newObj);
                        }
                    }
                }
                packet.append(drops).append(";");// Drop
                packet.append(winkamas).append("|");

                ((DatabaseManager.get(CollectorData::class.java) as CollectorData)).update(collector);
            }
            //endregion
            return packet.toString();
        } catch (e: Exception) {
            log.error("unexpected error", e)
            log.error("An error occurred when server went to give the 'GE' packet : " + e.message.toString() + " " + e.getLocalizedMessage());
        }
        return "";
    }

    fun insertChestsAfterSummoner(TEAM1: ArrayList<Fighter>, Invocator: Int, Invocation: Int): ArrayList<Fighter> {
        var k: Int = 0
        var p: Int = 0
        var j: Int = 0
        var s: Int = TEAM1.size - 1
        var b: Boolean = true
        var invok: Fighter? = null
        for (i in  TEAM1) {
            if (i.id == Invocation) {
                invok = i;
                b = false;
            }
            if (b && invok != i) {
                TEAM1.set(k - 1, i);
            }
            k++;
        }
        TEAM1.set(s, invok!!);
        k = 0;
        b = true;
        for (i in  TEAM1) {
            if (i.id == Invocator) {
                p = k;
                b = false;
            }
            if (b && i.id != Invocator) {
                j++;
                if (k < s)
                    TEAM1.set(s - j + 1, TEAM1.get(s - j));
            }
            k++;
        }
        TEAM1.set(p + 1, invok!!);
        return TEAM1;
    }

    fun getGTL(): String {
        var packet: String = "GTL"
        if (this.orderPlaying != null)
            for (f in  this.orderPlaying)
                if (f.isDead)
                    packet += "|" + f.id;
        return packet + (0x00.toChar());
    }

    fun parseFightInfos(): String {
        var infos: StringBuilder = StringBuilder()
        infos.append(id).append(";");
        var time: Long = startTime + TimeZone.getDefault().getRawOffset()
        infos.append(if (startTime == 0L) "-1" else time).append(";");
        // Team1
        infos.append("0,");// 0 car toujours joueur :)
        when (type) {
Constant.FIGHT_TYPE_CHALLENGE -> {infos.append("0,");
                infos.append(this.getTeamSizeWithoutInvocation(this.team0.values)).append(";");
                // Team2
                infos.append("0,");
                infos.append("0,");
                infos.append(this.getTeamSizeWithoutInvocation(this.team1.values)).append(";");
                
}
Constant.FIGHT_TYPE_AGRESSION -> {infos.append(this.init0.getPlayer()!!.alignment).append(",");
                infos.append(this.team0.size).append(";");
                // Team2
                infos.append("0,");
                infos.append(this.init1.getPlayer()!!.alignment).append(",");
                infos.append(this.getTeamSizeWithoutInvocation(this.team1.values)).append(";");
                
}
Constant.FIGHT_TYPE_CONQUETE -> {infos.append(this.init0.getPlayer()!!.alignment).append(",");
                infos.append(this.getTeamSizeWithoutInvocation(this.team0.values)).append(";");
                // Team2
                infos.append("0,");
                infos.append(prism.alignment).append(",");
                infos.append(this.getTeamSizeWithoutInvocation(this.team1.values)).append(";");
                
}
Constant.FIGHT_TYPE_PVM, Constant.FIGHT_TYPE_DOPEUL -> {infos.append("0,");
                infos.append(this.getTeamSizeWithoutInvocation(this.team0.values)).append(";");
                // Team2
                infos.append("1,");
                if (this.team0.isEmpty())
                    infos.append("0,");
                else
                    infos.append(this.team1.get(this.team1.keys.toTypedArray()[0])!!.getMob()!!.template.align).append(",");
                infos.append(this.getTeamSizeWithoutInvocation(this.team1.values)).append(";");
                
}
Constant.FIGHT_TYPE_PVT -> {infos.append("0,");
                infos.append(this.getTeamSizeWithoutInvocation(this.team0.values)).append(";");
                // Team2
                infos.append("3,");
                infos.append("0,");
                infos.append(this.getTeamSizeWithoutInvocation(this.team1.values)).append(";");
                
}
}
        return infos.toString();
    }

    fun getTeamSizeWithoutInvocation(fighters: Collection<Fighter>): Int {
        var i: Int = 0
        for (fighter in  fighters) if (fighter.isInvocation()) i++;
        return i;
    }

    fun getFighterByGameOrder(): Fighter? {
        if (this.orderPlaying == null)
            return null;
        if (this.curPlayer >= this.orderPlaying!!.size)
            this.curPlayer = this.orderPlaying!!.size - 1;
        if (this.curPlayer < 0)
            this.curPlayer = 0;
        if (this.orderPlaying!!.size <= 0)
            return null;
        var current: Fighter? = null
        try {
            current = this.orderPlaying!!.get(this.curPlayer);
        } catch (e: Exception) {
            log.error("unexpected error", e)
        }
        return current!!;
    }

    fun getOrderPlayingSize(): Int {
        if (this.orderPlaying == null)
            return 0;
        if (this.orderPlaying!!.size <= 0)
            return 0;
        return this.orderPlaying!!.size;
    }

    fun haveFighterInOrdreJeu(f: Fighter): Boolean {
        return this.orderPlaying != null && f != null && this.orderPlaying!!.contains(f);
    }


    fun cast(fighter: Fighter, runnable: Runnable, SS: SortStats?) {
        var current: Fighter = this.getFighterByGameOrder()!!
        if (current != null && fighter != null && current.id == fighter.id) {
            if (this.turn != null && System.currentTimeMillis() - this.turn!!.getStartTime() >= Constant.TIME_BY_TURN) {
                if (System.currentTimeMillis() - this.turn!!.getStartTime() >= Constant.TIME_BY_TURN) this.endTurn(false);
                return;
            }
            SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(this, 7, fighter.id);
            try {
                runnable.run();
            } catch (e: Exception) {
                log.error("unexpected error", e)
            }
            SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, fighter.id);
        }
    }

    companion object {
    @JvmStatic fun give(objects: ArrayList<GameObject>, winners: ArrayList<Fighter>): Map<Player,String> {
        val list: MutableMap<Player,String> = HashMap()

        if (Config.modeHeroic) {
            val players: ArrayList<Player> = ArrayList()

            ArrayList(winners).stream().filter({ fighter -> fighter != null }).forEach { fighter ->
                val player: Player? = fighter!!.getPlayer()

                if (player != null) {
                    players.add(player);
                    list.put(player, "");
                }
            }

            if (players.isEmpty() && objects != null && objects.isEmpty()) {
                var count: Int = -1
                var obj: GameObject? = null

                var iterator: MutableIterator<GameObject> = objects!!.iterator()
                while (iterator.hasNext()) {
                    obj = objects.iterator().next();

                    if (obj == null) {
                        iterator.remove();
                        continue;
                    }

                    count++;
                    val player: Player? = players.get(count)

                    if (player != null) {
                        obj.position = Constant.ITEM_POS_NO_EQUIPED;
                        player.addItem(obj, true, false);
                        var value: String? = list[player]
                        value = (if (value!!.isEmpty()) "" else ",") + obj.template!!.id.toString() + "~" + obj.quantity;
                        list.remove(player);
                        list.put(player, value);
                        objects.remove(obj);
                    }
                    if (count >= players.size - 1)
                        count = -1;

                }
            }
        }
        return list;
    }
    @JvmStatic fun FightStateAddFlag(map: GameMap, player: Player) {
        map.fights.stream().filter({ fight -> fight.state == Constant.FIGHT_STATE_PLACE }).forEach { fight -> 
            if (fight.type == Constant.FIGHT_TYPE_CHALLENGE) {
                SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(player, 0,
                        fight.init0.id, fight.init1.id, fight.init0.getPlayer()!!.curCell.cellId, "0;-1", fight.init1.getPlayer()!!.curCell.cellId, "0;-1");
            } else if (fight.type == Constant.FIGHT_TYPE_AGRESSION) {
                SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(player, 0,
                        fight.init0.id, fight.init1.id, fight.init0.getPlayer()!!.curCell.cellId, "0;" + fight.init0.getPlayer()!!.alignment, fight.init1.getPlayer()!!.curCell.cellId, "0;" + fight.init1.getPlayer()!!.alignment);
            } else if (fight.type == Constant.FIGHT_TYPE_PVM) {
                SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(player, 4,
                        fight.init0.id, fight.monsterGroup.id, fight.init0.getPlayer()!!.curCell.cellId + 1, "0;-1", fight.monsterGroup.cellId, "1;-1");
            } else if (fight.type == Constant.FIGHT_TYPE_PVT) {
                SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(player, 5,
                        fight.init0.id, fight.collector.id, fight.init0.getPlayer()!!.curCell.cellId + 1, "0;-1", fight.collector.cell, "3;-1");
            } else if (fight.type == Constant.FIGHT_TYPE_CONQUETE) {
                SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(player, 0,
                        fight.init0.id, fight.prism.id, fight.init0.getPlayer()!!.curCell.cellId, "0;" + fight.init0.getPlayer()!!.alignment, fight.prism.cell, "0;" + fight.prism.alignment);
            }
            SocketManager.GAME_SEND_REFRESH_TEAM_PACKET_TO_MAP(map, fight.init0.id, fight.team0.values);
            var id: Int = if (fight.init1 == null) if (fight.collector == null) if (fight.prism == null) -1 else fight.prism.id else fight.collector.id else fight.init1.id
            SocketManager.GAME_SEND_REFRESH_TEAM_PACKET_TO_MAP(map, id, fight.team1.values);
        }}
    }

    fun setMobGroup2(monsterGroup2: MonsterGroup) {
        this.monsterGroup2 = monsterGroup2;
    }


}
