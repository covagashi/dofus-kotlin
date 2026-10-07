package org.starloco.locos.database.data.login

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.client.Account
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.database.data.game.BankData
import org.starloco.locos.database.data.game.QuestProgressData
import org.starloco.locos.game.world.World
import java.sql.PreparedStatement
import java.sql.SQLException

class AccountData(dataSource: HikariDataSource?) : FunctionDAO<Account>(dataSource, "world_accounts") {

    override fun loadFully() {
        throw NotImplementedException("It's not allowed to load all accounts")
    }

    override fun load(id: Int): Account? {
        try {
            return getData<Account?>("SELECT * FROM " + getTableName() + " WHERE guid = " + id) { result ->
                if (!result.next()) {
                    null
                } else {
                    val acc = Account(result.getInt("guid"), (result.getString("account") ?: "").lowercase(), result.getString("pseudo") ?: "", result.getString("reponse") ?: "", result.getInt("banned") == 1, result.getString("lastIP") ?: "", result.getString("lastConnectionDate") ?: "", result.getString("friends") ?: "", result.getString("enemy") ?: "", result.getInt("points"), result.getLong("subscribe"), result.getLong("muteTime"), result.getString("mutePseudo") ?: "", result.getString("lastVoteIP") ?: "", result.getString("heurevote") ?: "")
                    World.world.addAccount(acc)
                    // Load account specific data
                    DatabaseManager.get(BankData::class.java).load(acc.id)
                    DatabaseManager.get(QuestProgressData::class.java).load(acc.id)
                    // Ensure players are loaded too
                    DatabaseManager.get(PlayerData::class.java).loadByAccountId(acc.id)
                    acc
                }
            }
        } catch (e: Exception) {
            super.sendError(e)
            return null
        }
    }

    override fun insert(entity: Account): Boolean {
        throw NotImplementedException("It's not allowed to insert account directly server-side")
    }

    override fun delete(entity: Account) {
        throw NotImplementedException("It's not allowed to delete account directly server-side")
    }

    //TODO: Account update all data
    override fun update(entity: Account) {
        var statement: PreparedStatement? = null
        try {
            statement = getPreparedStatement("UPDATE " + getTableName() + " SET banned = '"
                + (if (entity.isBanned) 1 else 0) + "', friends = '"
                + entity.parseFriendListToDB() + "', enemy = '"
                + entity.parseEnemyListToDB() + "', muteTime = '"
                + entity.getMuteTime() + "', mutePseudo = '"
                + entity.getMutePseudo() + "' WHERE guid = '" + entity.id
                + "'")
            execute(statement)
        } catch (e: Exception) {
            super.sendError(e)
        } finally {
            close(statement)
        }
    }

    override fun getReferencedClass(): Class<*> {
        return AccountData::class.java
    }

    fun getSubscribe(id: Int): Long {
        try {
            return getData<Long>("SELECT guid, subscribe FROM " + getTableName() + " WHERE guid = " + id) { result ->
                if (result.next()) result.getLong("subscribe") else 0
            } ?: 0
        } catch (e: Exception) {
            super.sendError(e)
        }
        return 0
    }

    fun updateVoteAll() {
        try {
            getData("SELECT guid, heurevote, lastVoteIP FROM " + getTableName() + ";") { result ->
                while (result.next()) {
                    val a = World.world.ensureAccountLoaded(result.getInt("guid"))
                    a?.updateVote(result.getString("heurevote") ?: "", result.getString("lastVoteIP") ?: "")
                }
            }
        } catch (e: SQLException) {
            super.sendError(e)
        }
    }

    fun updateLastConnection(compte: Account) {
        var p: PreparedStatement? = null
        try {
            p = getPreparedStatement("UPDATE " + getTableName() + " SET `lastIP` = ?, `lastConnectionDate` = ? WHERE `guid` = ?")
            p?.setString(1, compte.currentIp)
            p?.setString(2, compte.lastConnectionDate)
            p?.setInt(3, compte.id)
            execute(p)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(p)
        }
    }

    fun setLogged(id: Int, logged: Int) {
        var p: PreparedStatement? = null
        try {
            p = getPreparedStatement("UPDATE " + getTableName() + " SET `logged` = ? WHERE `guid` = ?;")
            if (p != null) {
                p.setInt(1, logged)
                p.setInt(2, id)
                execute(p)
            }
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(p)
        }
    }

    fun updateBannedTime(acc: Account, time: Long) {
        var statement: PreparedStatement? = null
        try {
            statement = getPreparedStatement("UPDATE " + getTableName() + " SET banned = '" + (if (acc.isBanned) 1 else 0) + "', bannedTime = '" + time + "' WHERE guid = '" + acc.id + "'")
            execute(statement)
        } catch (e: Exception) {
            super.sendError(e)
        } finally {
            close(statement)
        }
    }

    fun loadPoints(user: String): Int {
        return this.loadPointsWithoutUsersDb(user)
    }

    fun loadPointsWithoutUsersDb(user: String): Int {
        try {
            return getData<Int>("SELECT * FROM " + getTableName() + " WHERE `account` LIKE '" + user + "'") { result ->
                if (result.next()) result.getInt("points") else 0
            } ?: 0
        } catch (e: SQLException) {
            super.sendError(e)
        }
        return 0
    }

    fun modPoints(id: Int, points: Long): World.Couple<Long, Boolean> {
        val minPts: Long = if (points < 0) -points else 0 // Compute minimum required points

        try {
            getPreparedStatement("UPDATE " + getTableName() + " SET `points` += ? WHERE `guid` = ? AND `points` >= ? RETURNING `points`").use { p ->
                p?.setLong(1, points)
                p?.setLong(2, minPts) // Make sure the user has enough points
                p?.setInt(3, id)
                execute(p)

                p?.resultSet.use { rs ->
                    if (rs == null || !rs.next()) return World.Couple(0L, false)
                    val newVal = rs.getLong(0)
                    return World.Couple(newVal, true)
                }
            }
        } catch (e: SQLException) {
            super.sendError(e)
            return World.Couple(0L, false)
        }
    }
}
