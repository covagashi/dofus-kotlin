package org.starloco.locos.area.map

import org.classdump.luna.Table
import org.classdump.luna.runtime.LuaFunction
import org.starloco.locos.anims.Animation
import org.starloco.locos.client.Player
import org.starloco.locos.entity.monster.MobGroupDef
import org.starloco.locos.entity.monster.MonsterGrade
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.game.world.World
import org.starloco.locos.script.DataScriptVM
import org.starloco.locos.script.ScriptVM
import org.starloco.locos.util.Pair

import java.util.Objects
import java.util.Optional
import java.util.stream.Collectors

class ScriptMapData private constructor(
    private val scriptVal: Table,
    id: Int,
    date: String,
    key: String,
    cellsData: String,
    width: Int,
    height: Int,
    x: Int,
    y: Int,
    subAreaID: Int,
    capabilities: Int,
    mobGroupsMaxCount: Int,
    mobPossibles: List<MonsterGrade>,
    places: List<List<Int>>,
    mobGroupsMinSize: Int,
    mobGroupsMaxSize: Int,
    @JvmField val zaapCell: Int?,
    animations: MutableMap<Int, Animation>
) : MapData(
    id, date, key, cellsData, width, height, x, y, subAreaID,
    capabilities and 0x80 > 0,
    capabilities and 0x100 > 0,
    capabilities and 0x200 > 0,
    capabilities and 0x8 > 0,
    capabilities and 0x1 > 0,
    capabilities and 0x2 > 0,
    capabilities and 0x80 > 0,
    mobGroupsMaxCount, mobGroupsMinSize, mobGroupsMaxSize, mobPossibles, places, animations
) {

    override fun getNPCs(): Map<Int, Pair<Int, Int>> {
        return ScriptVM.mapFromScript(
            ScriptVM.recursiveGet(scriptVal, "npcs") as Table,
            { o -> (o as Long).toInt() },
            { o ->
                val pair = o as Table
                Pair(ScriptVM.rawInt(pair, 1L), ScriptVM.rawInt(pair, 2L))
            }
        )
    }

    override fun getStaticGroups(): List<MobGroupDef> {
        val mapper = MobGroupDef.Mapper.get()
        return ScriptVM.listFromLuaTable<Table>(ScriptVM.recursiveGet(scriptVal, "staticGroups") as Table).stream()
            .map { it as Table }
            .map { mapper.from(it) }
            .collect(Collectors.toList())
    }

    override fun onMoveEnd(p: Player) {
        val tmp = ScriptVM.recursiveGet(scriptVal, "onMovementEnd")
        if (tmp !is Table) return
        val onMovementEndFn = tmp.rawget(p.curCell.cellId)
        if (onMovementEndFn !is LuaFunction<*, *, *, *, *>) return
        DataScriptVM.getInstance()?.call(onMovementEndFn, scriptVal, p.curMap.scripted(), p.scripted())
    }

    override fun cellHasMoveEndActions(cellId: Int): Boolean {
        val tmp = ScriptVM.recursiveGet(scriptVal, "onMovementEnd")
        if (tmp !is Table) return false

        val onMovementEndFn = tmp.rawget(cellId)
        return onMovementEndFn is LuaFunction<*, *, *, *, *>
    }

    private fun onFightFunctionByType(type: Int, name: String): Optional<Any> {
        val tmp = ScriptVM.recursiveGet(scriptVal, name)
        if (tmp !is Table) return Optional.empty()

        val fn = tmp.rawget(type)
        return if (fn is LuaFunction<*, *, *, *, *>) Optional.of(fn) else Optional.empty()
    }

    override fun onFightInit(f: Fight, team0: Collection<Fighter>, team1: Collection<Fighter>) {
        onFightFunctionByType(f.type, "onFightInit").ifPresent { fn ->
            val t0 = ScriptVM.scriptedValsTable(team0)
            val t1 = ScriptVM.scriptedValsTable(team1)

            DataScriptVM.getInstance()?.call(fn, scriptVal, f.mapOld.scripted(), t0, t1)
        }
    }

    override fun onFightStart(f: Fight, team0: Collection<Fighter>, team1: Collection<Fighter>) {
        onFightFunctionByType(f.type, "onFightStart").ifPresent { fn ->
            val t0 = ScriptVM.scriptedValsTable(team0)
            val t1 = ScriptVM.scriptedValsTable(team1)

            DataScriptVM.getInstance()?.call(fn, scriptVal, f.mapOld.scripted(), t0, t1)
        }
    }

    override fun onFightEnd(f: Fight, p: Player, winTeam: List<Fighter>, looseTeam: List<Fighter>) {
        val isWinner = winTeam.stream().filter(Objects::nonNull).anyMatch { fp -> fp.id == p.id }

        val winners = ScriptVM.scriptedValsTable(winTeam)
        val losers = ScriptVM.scriptedValsTable(looseTeam)

        val vm = DataScriptVM.getInstance()
        onFightFunctionByType(f.type, "onFightEnd").ifPresent { fn ->
            vm?.call(fn, p.scripted(), isWinner, winners, losers)
        }
    }

    override fun hasFightEndForType(type: Int): Boolean {
        return onFightFunctionByType(type, "onFightEnd").isPresent
    }

    fun scripted(): Table {
        return scriptVal
    }

    companion object {
        @JvmStatic
        fun build(`val`: Table): ScriptMapData {
            val lMobGrades = `val`.rawget("allowedMobGrades") as Table
            val allowedMonsters = ScriptVM.listOfIntPairs(lMobGrades)!!.stream()
                .map { p ->
                    Optional.ofNullable(World.world.getMonstre(p.first))
                        .map { m -> m.grades[p.second] }
                        .orElse(null)
                }
                .filter(Objects::nonNull).map { it!! }.collect(Collectors.toList())

            val zaapCell = Optional.ofNullable(`val`.rawget("zaapCell")).map { o -> (o as Long).toInt() }.orElse(null)

            val positions = `val`.rawget("positions").toString()

            val places = MapData.decodePositions(positions)

            val animations = ScriptVM.mapFromScript(
                `val`.rawget("animations") as Table,
                { k -> (k as Long).toInt() },
                { Animation::class.java.cast(it) }
            )

            return ScriptMapData(
                `val`,
                ScriptVM.rawInt(`val`, "id"),
                `val`.rawget("date").toString(),
                `val`.rawget("key").toString(),
                `val`.rawget("cellsData").toString(),
                ScriptVM.rawInt(`val`, "width"),
                ScriptVM.rawInt(`val`, "height"),
                ScriptVM.rawInt(`val`, "x"),
                ScriptVM.rawInt(`val`, "y"),
                ScriptVM.rawInt(`val`, "subAreaId"),
                ScriptVM.rawInt(`val`, "capabilities"),
                ScriptVM.rawInt(`val`, "mobGroupsCount"),
                allowedMonsters,
                places,
                ScriptVM.rawInt(`val`, "mobGroupsMinSize"),
                ScriptVM.rawInt(`val`, "mobGroupsMaxSize"),
                zaapCell,
                animations as MutableMap<Int, Animation>
            )
        }
    }
}
