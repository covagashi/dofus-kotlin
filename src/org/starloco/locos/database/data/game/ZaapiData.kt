package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.kernel.Constant
import java.sql.SQLException

class ZaapiData(dataSource: HikariDataSource?) : FunctionDAO<Any>(dataSource, "zaapi") {
    override fun loadFully() {
        try {
        getData("SELECT mapid, align FROM " + getTableName() + ";") { result ->
        var angels: StringBuilder = StringBuilder()
        var demons: StringBuilder = StringBuilder()
        var neutral: StringBuilder = StringBuilder()
                while (result.next()) {
                    if (result.getInt("align") == Constant.ALIGNEMENT_BONTARIEN) {
        angels.append(result.getString("mapid"))
        if (!result.isLast()) angels.append(",")
                    } else if (result.getInt("align") == Constant.ALIGNEMENT_BRAKMARIEN) {
        demons.append(result.getString("mapid"))
        if (!result.isLast()) demons.append(",")
                    } else {
        neutral.append(result.getString("mapid"))
        if (!result.isLast()) neutral.append(",")
                    }
                }
        Constant.ZAAPI[Constant.ALIGNEMENT_BONTARIEN] = angels.toString()
        Constant.ZAAPI[Constant.ALIGNEMENT_BRAKMARIEN] = demons.toString()
        Constant.ZAAPI[Constant.ALIGNEMENT_NEUTRE] = neutral.toString()
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
    }
    override fun load(id: Int): Any {
        throw NotImplementedException()
    }
    override fun insert(entity: Any): Boolean {
        throw NotImplementedException()
    }
    override fun delete(entity: Any) {
        throw NotImplementedException()
    }
    override fun update(entity: Any) {
        throw NotImplementedException()
    }
    override fun getReferencedClass(): Class<*> {
        return ZaapiData::class.java
    }
}