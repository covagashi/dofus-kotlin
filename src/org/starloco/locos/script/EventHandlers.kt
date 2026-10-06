package org.starloco.locos.script

import org.classdump.luna.Table
import org.classdump.luna.impl.DefaultTable
import org.classdump.luna.runtime.LuaFunction
import org.starloco.locos.client.Player
import org.starloco.locos.quest.QuestInfo

class EventHandlers(private val vm: DataScriptVM) : DefaultTable() {

    private val players = DefaultTable()

    init {
        this.rawset("players", players)
    }

    private fun getHandler(t: Table, name: String): LuaFunction<*, *, *, *, *> {
        val mbFn = t.rawget(name)
        require(mbFn is LuaFunction<*, *, *, *, *>) { "event handler is not a function" }
        return mbFn
    }

    fun onDialog(player: Player, npcID: Int, answer: Int) {
        vm.call(getHandler(players, "onDialog"), player.scripted(), npcID, answer)
    }

    fun onMapEnter(player: Player) {
        vm.call(getHandler(players, "onMapEnter"), player.scripted())
    }

    fun onSkillUse(player: Player, cellID: Int, skillID: Int) {
        vm.call(getHandler(players, "onSkillUse"), player.scripted(), cellID, skillID)
    }

    fun onFightEnd(player: Player, type: Int, isWinner: Boolean, winners: Table, losers: Table) {
        vm.call(getHandler(players, "onFightEnd"), player.scripted(), type, isWinner, winners, losers)
    }

    fun questInfo(player: Player, id: Int, currentStep: Int): QuestInfo? {
        val ret = vm.call(getHandler(players, "onQuestStatusRequest"), player.scripted(), id, currentStep)
        if (ret == null || ret.isEmpty() || ret[0] !is Table) return null
        val t = ret[0] as Table

        return QuestInfo(
            ScriptVM.intsFromLuaTable(t.rawget("objectives") as Table),
            ScriptVM.rawInteger(t, "previous"),
            ScriptVM.rawInteger(t, "next"),
            ScriptVM.rawInteger(t, "question"),
            t.rawget("isAccount") as Boolean,
            t.rawget("isRepeatable") as Boolean
        )
    }

    fun onDocQuestHref(player: Player, docID: Int, questID: Int) {
        vm.call(getHandler(players, "onDocQuestHref"), player.scripted(), docID, questID)
    }
}
