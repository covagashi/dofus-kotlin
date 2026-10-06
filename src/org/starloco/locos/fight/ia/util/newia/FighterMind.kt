package org.starloco.locos.fight.ia.util.newia

import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractEasyIA
import org.starloco.locos.fight.ia.util.newia.action.IAAction
import java.util.LinkedList
import java.util.stream.Collectors

/**
 * Created by Locos on 14/05/2018.
 */
abstract class FighterMind(protected val ia: AbstractEasyIA) {

    protected val fightersCases = LinkedList<FighterCase>()

    @JvmField
    var highPriorityActions = LinkedList<IAAction>()
    @JvmField
    var lowPriorityActions = LinkedList<IAAction>()

    abstract fun init()

    fun executeActions(actions: LinkedList<IAAction>): IAAction? {
        if (!actions.isEmpty()) {
            val action = actions.pollFirst()
            if (!action.execute())
                return null
            return action
        }
        return null
    }

    fun getEnemies(withHide: Boolean): Collection<Fighter> {
        return ia.getFight().getTeam(if (ia.getFighter().team + 1 == 1) 2 else 1).values.stream()
            .filter { f -> !f.isDead && (withHide || !f.isHidden()) }.collect(Collectors.toSet())
    }

    fun getFriends(): Collection<Fighter> {
        return ia.getFight().getTeam(ia.getFighter().team + 1).values.stream()
            .filter { f -> !f.isDead && !f.isHidden() }.collect(Collectors.toSet())
    }

    fun getInvocations(): Collection<Fighter> {
        return getEnemies(false).stream().filter { f -> f.isInvocation() }.collect(Collectors.toSet())
    }
}
