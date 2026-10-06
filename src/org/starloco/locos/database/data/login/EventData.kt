package org.starloco.locos.database.data.login

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.event.type.Event
import org.starloco.locos.event.type.EventFindMe
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Created by Locos on 02/10/2016.
 */
class EventData(dataSource: HikariDataSource?) : FunctionDAO<Event>(dataSource, "world_event_type") {

    fun load(): Array<Event?> {
        val events: Array<Event?> = arrayOfNulls(this.getNumberOfEvent().toInt())
        try {
            getData("SELECT * FROM " + getTableName() + ";") { result ->
                var i = 0
                while (result.next()) {
                    val event = this.getEventById(result.getByte("id"), result)
                    if (event != null) {
                        events[i] = event
                        i++
                    }
                }
            }
        } catch (e: SQLException) {
            super.sendError(e)
        }
        return events
    }

    private fun getNumberOfEvent(): Byte {
        var numbers: Byte = 0
        try {
            numbers = getData<Byte>("SELECT COUNT(id) AS numbers FROM " + getTableName() + ";") { result ->
                result.next()
                result.getByte("numbers")
            } ?: 0.toByte()
        } catch (e: SQLException) {
            super.sendError(e)
        }
        return numbers
    }

    private fun getEventById(id: Byte, result: ResultSet): Event? {
        val maxPlayers = result.getByte("maxPlayers")
        val name = result.getString("name")
        val description = result.getString("description")
        return when (id.toInt()) {
            // 1: Smiley - TODO: Remettre l'event smiley
            // return EventSmiley(id, result.getByte("maxPlayers"), result.getString("name"), result.getString("description"))
            2 -> EventFindMe(id, maxPlayers, name, description) // Trouve-moi
            else -> null
        }
    }

    override fun loadFully() {
        throw NotImplementedException()
    }

    override fun load(id: Int): Event? {
        throw NotImplementedException()
    }

    override fun insert(entity: Event): Boolean {
        throw NotImplementedException()
    }

    override fun delete(entity: Event) {
        throw NotImplementedException()
    }

    override fun update(entity: Event) {
        throw NotImplementedException()
    }

    override fun getReferencedClass(): Class<*> {
        return EventData::class.java
    }
}
