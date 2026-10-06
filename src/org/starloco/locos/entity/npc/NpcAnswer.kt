package org.starloco.locos.entity.npc

import org.starloco.locos.client.Player
import org.starloco.locos.other.Action

class NpcAnswer(val id: Int) {

    var actions: ArrayList<Action> = ArrayList()

    fun addAction(action0: Action) {
        val actions = ArrayList<Action>()
        actions.addAll(this.actions)

        for (action1 in actions)
            if (action1.id == action0.id)
                this.actions.remove(action1)

        this.actions.add(action0)
    }

    fun apply(player: Player): Boolean {
        var leave = true
        for (action in this.actions)
            leave = action.apply(player, null, -1, -1, null)
        return leave
    }

    val isAnotherDialog: Boolean
        get() {
            for (action in actions)
                if (action.id == 1) //1 = Discours NPC
                    return true
            return false
        }
}
