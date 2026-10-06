package org.starloco.locos.quest

import java.util.HashSet

class QuestProgress {

    @JvmField
    val accountId: Int
    @JvmField
    val playerId: Int
    private var finished: Boolean = false
    private var currentStep: Int
    @JvmField
    val questId: Int
    private val completedObjectives: MutableSet<Int>

    constructor(entityId: Int, playerId: Int, questId: Int, sId: Int) {
        this.accountId = entityId
        this.playerId = playerId
        this.questId = questId
        this.currentStep = sId
        this.completedObjectives = HashSet()
    }

    constructor(accountId: Int, playerId: Int, questId: Int, sId: Int, completedObjectives: MutableSet<Int>, finished: Boolean) {
        this.accountId = accountId
        this.playerId = playerId
        this.questId = questId
        this.currentStep = sId
        this.completedObjectives = completedObjectives
        this.finished = finished
    }

    fun isFinished(): Boolean = finished

    fun getCurrentStep(): Int = currentStep

    fun setCurrentStep(step: Int) {
        this.completedObjectives.clear()
        this.currentStep = step
    }

    fun hasCompletedObjective(objectiveId: Int): Boolean =
        completedObjectives.contains(objectiveId)

    fun completeObjective(objectiveId: Int) {
        completedObjectives.add(objectiveId)
    }

    fun getCompletedObjectives(): Set<Int> = this.completedObjectives

    fun markFinished() {
        this.currentStep = 0
        this.finished = true
    }

    fun getQuestId(): Int = this.questId

    companion object {
        // Used for account-bound quests
        const val NO_PLAYER_ID = -1
    }
}
