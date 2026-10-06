package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.starloco.locos.auction.Auction
import org.starloco.locos.auction.AuctionManager
import org.starloco.locos.client.Player
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import org.starloco.locos.`object`.GameObject
import java.sql.PreparedStatement
import java.sql.SQLException

/**
 * Created by Locos on 03/02/2018.
 */
class AuctionData(dataSource: HikariDataSource?) : FunctionDAO<Auction>(dataSource, "auctions") {
    override fun loadFully() {
        try {
        getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result.next()) {
        var player: Player? = World.world.getPlayer(result.getInt("owner"))
        var gameObject: GameObject? = World.world.getGameObject(result.getInt("object"))
                    if (gameObject == null || player == null) {
        var auction: Auction = Auction(result.getInt("price"), player, GameObject(result.getInt("object"), -1, 1, -1, "", 0), result.getByte("retry"))
        this.delete(auction)
                    } else {
        var auction: Auction = Auction(result.getInt("price"), player, gameObject, result.getByte("retry"))
        AuctionManager.getInstance().getAuctions().add(auction)
                    }
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
    }
    override fun load(id: Int): Auction? {
        return null
    }
    override fun insert(entity: Auction): Boolean {
        var p: PreparedStatement? = null
        var ok: Boolean = true
        try {
            p = getPreparedStatement("INSERT INTO " + getTableName() + "(`price`, `owner`, `object`, `retry`) VALUES ('" +
                    entity.price + "', '" +
                    entity.owner!!.id + "', '" +
                    entity.`object`!!.guid + "', '" +
        entity.retry + "');")
        execute(p)
        } catch (e: SQLException) {
        ok = false
        super.sendError(e)
        } finally {
        close(p)
        }
        return ok
    }
    override fun delete(entity: Auction) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("DELETE FROM " + getTableName() + " WHERE `object` = ?;")
        p?.setInt(1, entity.`object`!!.guid)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    override fun update(entity: Auction) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("UPDATE " + getTableName() + " SET `retry` = ? WHERE `owner` = ? AND `object` = ?;")
        p?.setByte(1, entity.retry)
        p?.setInt(2, entity.owner!!.id)
        p?.setInt(3, entity.`object`!!.guid)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    override fun getReferencedClass(): Class<*> {
        return AuctionData::class.java
    }
}