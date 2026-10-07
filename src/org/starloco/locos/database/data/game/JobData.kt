package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import org.starloco.locos.job.Job
import java.sql.SQLException

class JobData(dataSource: HikariDataSource?) : FunctionDAO<Job>(dataSource, "jobs_data") {
    override fun loadFully() {
        try {
        getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result.next()) {
        var skills: String = ""
                    if (result.getString("skills") != null)
        skills = result.getString("skills") ?: ""
        World.world.addJob(Job(result.getInt("id"), result.getString("tools"), result.getString("crafts"), skills))
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
    }
    override fun load(id: Int): Job {
        throw NotImplementedException()
    }
    override fun insert(entity: Job): Boolean {
        throw NotImplementedException()
    }
    override fun delete(entity: Job) {
        throw NotImplementedException()
    }
    override fun update(entity: Job) {
        throw NotImplementedException()
    }
    override fun getReferencedClass(): Class<*> {
        return JobData::class.java
    }
}