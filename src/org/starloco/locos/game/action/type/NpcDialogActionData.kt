package org.starloco.locos.game.action.type

import org.starloco.locos.client.Player
import org.starloco.locos.entity.npc.Npc
import org.starloco.locos.entity.npc.NpcTemplate

class NpcDialogActionData(
    val npcTemplate: NpcTemplate,
    questionId: Int
) : ActionDataInterface {

    var questionId: Int = questionId

    private var answers: List<Int> = ArrayList()

    fun hasAnswer(answer: Int): Boolean = answers.contains(answer)

    fun setAnswers(answers: List<Int>) {
        this.answers = answers
    }

    fun getNpc(player: Player): Npc? =
        player.curMap.getNpcByTemplateId(npcTemplate.id)

    fun isValid(player: Player, npcId: Int): Boolean =
        getNpc(player)?.id == npcId
}
