package org.starloco.locos.database

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import org.starloco.locos.database.data.DAO
import org.starloco.locos.database.data.game.*
import org.starloco.locos.database.data.login.*
import org.starloco.locos.kernel.Config
import org.starloco.locos.kernel.Main
import java.sql.SQLException

class DatabaseManager {

    private val logger = LoggerFactory.getLogger(DatabaseManager::class.java) as Logger

    private val login: HikariDataSource?
    private val game: HikariDataSource?

    private val daos = ArrayList<DAO<*>>()

    /**
     * Construct the two connection database
     */
    init {
        (LoggerFactory.getLogger("com.zaxxer.hikari.HikariDataSource") as Logger).setLevel(Level.ERROR)
        (LoggerFactory.getLogger("com.zaxxer.hikari.HikariConfig") as Logger).setLevel(Level.ERROR)
        (LoggerFactory.getLogger("com.zaxxer.hikari.pool.HikariPool") as Logger).setLevel(Level.ERROR)
        (LoggerFactory.getLogger("DEBUG com.zaxxer.hikari.pool.PoolBase") as Logger).setLevel(Level.ERROR)

        logger.trace("Reading database config")
        this.login = this.createHikariDataSource(Config.databaseLoginHost!!, Config.databaseLoginPort.toString(), Config.databaseLoginName!!, Config.databaseLoginUser!!, Config.databaseLoginPass!!)
        logger.debug("Connection to the login database, ok")
        this.game = this.createHikariDataSource(Config.databaseGameHost!!, Config.databaseGamePort.toString(), Config.databaseGameName!!, Config.databaseGameUser!!, Config.databaseGamePass!!)
        logger.debug("Connection to the game database, ok")
        this.initialize()
        logger.debug("All data have been initialized")
        logger.setLevel(Level.ERROR)
    }

    fun isConnected(): Boolean {
        try {
            return login != null && login.connection != null && !login.connection.isClosed &&
                game != null && game.connection != null && !game.connection.isClosed
        } catch (e: SQLException) {
            logger.error("unexpected error", e)
                }
        return false
    }

    /**
     *  Initialiazation of all entities table
     */
    private fun initialize() {

        //region login data
        this.daos.add(AccountData(this.login))
        this.daos.add(EventData(this.login))
        this.daos.add(PlayerData(this.login))
        this.daos.add(AdData(this.login))
        this.daos.add(ServerData(this.login))
        this.daos.add(BanIpData(this.login))
        this.daos.add(BaseAreaData(this.login))
        this.daos.add(BaseSubAreaData(this.login))
        this.daos.add(GuildData(this.login))
        this.daos.add(BaseHouseData(this.login))
        this.daos.add(BaseTrunkData(this.login))
        this.daos.add(MountData(this.login))
        this.daos.add(BaseMountParkData(this.login))
        this.daos.add(ObjectData(this.login))
        this.daos.add(ObvijevanData(this.login))
        this.daos.add(PetData(this.login))
        //endregion

        //region game data
        this.daos.add(AreaData(this.game))
        this.daos.add(AuctionData(this.game))
        this.daos.add(GangsterData(this.game))
        this.daos.add(BankData(this.game))
        this.daos.add(TrunkData(this.game))
        this.daos.add(GuildMemberData(this.game))
        this.daos.add(BigStoreListingData(this.game))
        this.daos.add(HouseData(this.game))
        this.daos.add(MountParkData(this.game))
        this.daos.add(CollectorData(this.game))
        this.daos.add(PrismData(this.game))
        this.daos.add(SubAreaData(this.game))
        this.daos.add(AreaData(this.game))
        this.daos.add(ChallengeData(this.game))
        this.daos.add(TrunkData(this.game))
        this.daos.add(CraftData(this.game))
        this.daos.add(DropData(this.game))
        this.daos.add(ExtraMonsterData(this.game))
        this.daos.add(FullMorphData(this.game))
        this.daos.add(GiftData(this.game))
        this.daos.add(HdvData(this.game))
        this.daos.add(HouseData(this.game))
        this.daos.add(ObjectTemplateData(this.game))
        this.daos.add(ObjectSetData(this.game))
        this.daos.add(JobData(this.game))
        this.daos.add(MonsterData(this.game))
        this.daos.add(MountParkData(this.game))
        this.daos.add(NpcData(this.game))
        this.daos.add(ObjectActionData(this.game))
        this.daos.add(PetTemplateData(this.game))
        this.daos.add(RuneData(this.game))
        this.daos.add(SubAreaData(this.game))
        this.daos.add(SpellData(this.game))
        this.daos.add(ZaapiData(this.game))
        this.daos.add(HeroicMobsGroupsData(this.game))
        this.daos.add(QuestProgressData(this.game))
        //endregion
    }

    /**
     * Create and try to connect to them
     * @param host ip address of database
     * @param port port of database
     * @param database name
     * @param user of the database
     * @param pass of the database
     * @return connection or null
     */
    private fun createHikariDataSource(host: String, port: String, database: String, user: String, pass: String): HikariDataSource? {
        val config = HikariConfig()
        config.setDataSourceClassName("org.mariadb.jdbc.MariaDbDataSource")
        config.addDataSourceProperty("serverName", host)
        config.addDataSourceProperty("port", port)
        config.addDataSourceProperty("databaseName", database)
        config.addDataSourceProperty("user", user)
        config.addDataSourceProperty("password", pass)
        config.setAutoCommit(true) // AutoCommit, c'est cool
        config.setMaximumPoolSize(20)
        config.setMinimumIdle(1)
        val source = HikariDataSource(config)

        if (!this.tryConnection(source)) {
            logger.error("Please check your username and password and database connection")
            Main.stop("statics try connection failed")
            return null
        }

        return source
    }

    /**
     * @param dataSource Hikari config
     * @return true if it's ok, otherwise false
     */
    private fun tryConnection(dataSource: HikariDataSource): Boolean {
        try {
            val connection = dataSource.connection
            connection.close()
            return true
        } catch (e: Exception) {
            logger.error("error when trying to connect to data source", e)
            return false
        }
    }

    companion object {
        private val instance = DatabaseManager()

        /**
         * @param c the entity class of the dao
         * @return dao class of the entity
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T, D : DAO<T>> get(c: Class<D>): D {
            for (dao in instance.daos) {
                if (dao.getReferencedClass() == c)
                    return dao as D
            }
            return null as D
        }

        @JvmStatic
        fun getInstance(): DatabaseManager {
            return instance
        }
    }
}
