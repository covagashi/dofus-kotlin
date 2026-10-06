package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.client.Account
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import java.sql.PreparedStatement
import java.sql.SQLException

class BankData(dataSource: HikariDataSource?) : FunctionDAO<Account>(dataSource, "banks") {
    override fun loadFully() {
        throw NotImplementedException()
    }
    override fun load(id: Int): Account {
        var account: Account = World.world.ensureAccountLoaded(id)!!
        try {
        getData("SELECT * FROM " + getTableName() + " WHERE id = '" + id + "';") { result ->
                if (result.next()) {
        account.parseBank(result.getInt("kamas"), result.getString("items"))
                } else {
        account.parseBank(-1, null)
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
        return account
    }
    override fun insert(entity: Account): Boolean {
        var p: PreparedStatement? = null
        var ok: Boolean = true
        try {
        p = getPreparedStatement("INSERT INTO " + getTableName() + "(`id`, `kamas`, `items`) VALUES (?, 0, '')")
        p?.setInt(1, entity.id)
        execute(p)
        } catch (e: SQLException) {
        ok = false
        super.sendError(e)
        } finally {
        close(p)
        }
        return ok
    }
    override fun delete(entity: Account) {
        throw NotImplementedException()
    }
    override fun update(entity: Account) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("UPDATE " + getTableName() + " SET `kamas` = ?, `items` = ? WHERE `id` = ?")
        p?.setLong(1, entity.getBankKamas())
        p?.setString(2, entity.parseBankObjectsToDB())
        p?.setInt(3, entity.id)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    override fun getReferencedClass(): Class<*> {
        return BankData::class.java
    }
}