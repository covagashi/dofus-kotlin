package org.starloco.locos.database.data

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Created by Locos on 14/07/2017.
 */
abstract class FunctionDAO<T> : DAO<T> {

    fun interface ResultSetFunction<R> {
        @Throws(SQLException::class)
        fun apply(rs: ResultSet): R
    }

    fun interface ResultSetConsumer {
        @Throws(SQLException::class)
        fun apply(rs: ResultSet)
    }

    private val locker = Any()
    private var tableName: String? = null
    @JvmField
    protected var dataSource: HikariDataSource?
    @JvmField
    protected var logger: Logger = LoggerFactory.getLogger(this.getReferencedClass()) as Logger

    constructor(dataSource: HikariDataSource?) {
        this.dataSource = dataSource
        logger.setLevel(Level.ERROR)
    }

    constructor(dataSource: HikariDataSource?, tableName: String) {
        this.dataSource = dataSource
        this.tableName = tableName
        logger.setLevel(Level.ERROR)
    }

    override fun getTableName(): String = "`$tableName`"

    @Throws(SQLException::class)
    protected open fun getConnection(): Connection? = dataSource?.connection

    protected open fun execute(query: String) {
        synchronized(locker) {
            try {
                getConnection().use { connection ->
                    connection?.createStatement().use { statement ->
                        statement?.execute(query)
                        logger.debug("SQL request executed successfully {}", query)
                    }
                }
            } catch (e: SQLException) {
                logger.error("Can't execute SQL Request :$query", e)
            }
        }
    }

    protected open fun execute(statement: PreparedStatement?) {
        synchronized(locker) {
            try {
                statement?.execute()
                logger.debug("SQL request executed successfully {}", statement.toString())
            } catch (e: SQLException) {
                logger.error("Can't execute SQL Request :" + statement.toString(), e)
            }
        }
    }

    @Throws(SQLException::class)
    protected open fun getPreparedStatement(query: String): PreparedStatement? {
        try {
            return getConnection()?.prepareStatement(query)
        } catch (e: SQLException) {
            logger.error("Can't execute prepared statement on datasource connection", e)
            return null
        }
    }

    @Throws(SQLException::class)
    protected open fun getData(query: String, consumer: ResultSetConsumer) {
        getData<Unit>(query) { rs -> consumer.apply(rs) }
    }

    @Throws(SQLException::class)
    protected open fun <R> getData(query: String, consumer: ResultSetFunction<R>): R? {
        synchronized(locker) {
            dataSource?.connection.use { conn ->
                conn?.createStatement().use { stat ->
                    return if (stat != null) consumer.apply(stat.executeQuery(query)) else null
                }
            }
        }
    }

    protected open fun close(statement: PreparedStatement?) {
        if (statement != null) {
            try {
                if (!statement.isClosed) {
                    statement.clearParameters()
                    statement.close()
                    val connection = statement.connection
                    if (connection != null && !connection.isClosed) {
                        connection.close()
                        logger.trace("{} released", connection)
                    }
                }
            } catch (e: Exception) {
                logger.error("Can't close statement", e)
            }
        }
    }

    protected open fun sendError(e: Exception) {
        logger.error("Error in " + this.getReferencedClass().name + " : " + e.message, e)
    }
}
