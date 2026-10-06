package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import org.starloco.locos.quest.QuestProgress
import java.sql.SQLException
import java.util.Objects
import java.util.stream.Collectors

class QuestProgressData(dataSource: HikariDataSource?) : FunctionDAO<QuestProgress>(dataSource, "quest_progress") {

    override fun loadFully() {
        throw NotImplementedException()
    }

    override fun load(accountId: Int): QuestProgress? {
        val account = World.world.getAccount(accountId)
        Objects.requireNonNull(account)
        try {
            getData("SELECT * FROM " + getTableName() + " WHERE `account_id` = " + accountId + ";") { result ->
                while (result.next()) {
                    val pId = result.getInt("player_id")
                    val qId = result.getInt("quest_id")
                    val sId = result.getInt("current_step")
                    val completedObjectives = result.getString("completed_objectives").split("|")
                        .filter { s -> s.isNotEmpty() }.map { it.toInt() }.toMutableSet()
                    val finished = result.getBoolean("finished")
                    val qp = QuestProgress(accountId, pId, qId, sId, completedObjectives, finished)
                    account!!.addQuestProgression(qp)
                }
            }
        } catch (e: SQLException) {
            super.sendError(e)
        }
        return null
    }

    override fun insert(entity: QuestProgress): Boolean {
        replace(entity)
        return true
    }

    override fun delete(entity: QuestProgress) {
        try {
            getConnection().use { c ->
                c?.prepareStatement("DELETE FROM " + getTableName() + " WHERE `account_id` = ? AND `player_id` = ? AND `quest_id` = ?;").use { p ->
                    p?.setInt(1, entity.accountId)
                    p?.setInt(2, entity.playerId)
                    p?.setInt(3, entity.questId)
                    execute(p)
                }
            }
        } catch (e: SQLException) {
            super.sendError(e)
        }
    }

    private fun replace(entity: QuestProgress?) {
        if (entity == null) return
        try {
            getConnection().use { c ->
                c?.prepareStatement("REPLACE INTO " + getTableName() + " (`account_id`, `player_id`, `quest_id`, `current_step`, `completed_objectives`, `finished`) VALUES (?, ?, ?, ?, ?, ?);").use { p ->
                    p?.setInt(1, entity.accountId)
                    p?.setInt(2, entity.playerId)
                    p?.setInt(3, entity.questId)
                    p?.setInt(4, entity.getCurrentStep())
                    p?.setString(5, entity.getCompletedObjectives().stream().map { it.toString() }.collect(Collectors.joining("|")))
                    p?.setBoolean(6, entity.isFinished())
                    execute(p)
                }
            }
        } catch (e: SQLException) {
            super.sendError(e)
        }
    }

    override fun update(entity: QuestProgress) {
        replace(entity)
    }

    override fun getReferencedClass(): Class<*> {
        return QuestProgressData::class.java
    }
}
