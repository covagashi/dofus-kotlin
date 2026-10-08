package org.starloco.locos.kernel

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.fusesource.jansi.AnsiConsole
import org.slf4j.LoggerFactory
import org.starloco.locos.area.map.GameMap
import org.starloco.locos.auction.AuctionManager
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.game.HeroicMobsGroupsData
import org.starloco.locos.database.data.login.ServerData
import org.starloco.locos.entity.monster.MonsterGroup
import org.starloco.locos.entity.mount.Mount
import org.starloco.locos.event.EventManager
import org.starloco.locos.exchange.ExchangeClient
import org.starloco.locos.game.GameServer
import org.starloco.locos.game.scheduler.Updatable
import org.starloco.locos.game.scheduler.entity.WorldPub
import org.starloco.locos.game.scheduler.entity.WorldSave
import org.starloco.locos.game.world.World
import org.starloco.locos.util.TimerWaiter

import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.util.*

object Main {

    @JvmField
    var logger: Logger = LoggerFactory.getLogger(Main::class.java) as Logger

    @JvmField
    val runnables: MutableList<Runnable> = LinkedList()

    @JvmField
    var angels: Short = 0
    @JvmField
    var demons: Short = 0

    @JvmField
    var mapAsBlocked = false
    @JvmField
    var fightAsBlocked = false
    @JvmField
    var tradeAsBlocked = false

    @JvmStatic
    @Throws(SQLException::class)
    fun main(args: Array<String>) {
        Runtime.getRuntime().addShutdownHook(Thread {
            if (Config.isRunning) {
                GameServer.setState(0)
                Config.isRunning = false
                Main.fightAsBlocked = true
                Main.tradeAsBlocked = true

                if (Config.gameServer != null)
                    Config.gameServer?.kickAll(true)

                Logging.getInstance().stop()
                DatabaseManager.get(ServerData::class.java).loadFully()
            }
            Main.logger.info("The server is now closed.")
        })

        try {
            System.setOut(PrintStream(System.out, true, "IBM850"))
            if (!File("Logs/Error").exists()) File("Logs/Error").mkdirs()
            System.setErr(
                PrintStream(
                    Files.newOutputStream(
                        Paths.get(
                            "Logs/Error/" + SimpleDateFormat("dd-MM-yyyy - HH-mm-ss", Locale.FRANCE).format(Date()) + ".log"
                        )
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("unexpected error", e)
                }

        Main.start()
    }

    @JvmStatic
    fun start() {
        Main.logger.info("You use " + System.getProperty("java.vendor") + " with the version " + System.getProperty("java.version"))
        Main.logger.debug("Starting of the server : " + SimpleDateFormat("dd/MM/yyyy - HH:mm:ss", Locale.FRANCE).format(Date()))

        val configPath = System.getenv().getOrDefault("STARLOCO_CONFIG_PATH", "game.config.properties")
        Config.verify(configPath)
        Logging.getInstance().initialize()

        if (!Config.debug) {
            // Quiet third-party DEBUG noise (SQL statements, packet traces)
            // from the very start; INFO lifecycle lines still print.
            val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
            root.level = Level.INFO
        }

        // Database
        if (DatabaseManager.getInstance().isConnected()) {
            Config.isRunning = true
            World.world.createWorld()

            GameServer().initialize()
            ExchangeClient().initialize()

            Main.logger.info("The server is ready ! Waiting for connection..\n")

            if (!Config.debug) {
                val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
                root.level = Level.ERROR
            }

            val updatables = listOf(
                WorldSave.instance, GameMap.updatable, Mount.updatable,
                WorldPub.instance,
                AuctionManager.getInstance(), /*Tavernier.getInstance(),*/ EventManager.instance
            )

            while (Config.isRunning) {
                try {
                    for (updatable in updatables) {
                        if (!Config.isRunning)
                            break

                        if (updatable != null)
                            try {
                                updatable.update()
                            } catch (e: Exception) {
                                logger.error("unexpected error", e)
                }
                    }

                    if (!Main.runnables.isEmpty()) {
                        val iterator = Main.runnables.iterator()
                        while (iterator.hasNext()) {
                            try {
                                val runnable = iterator.next()
                                runnable?.run()
                            } catch (e: Exception) {
                                logger.error("unexpected error", e)
                } finally {
                                iterator.remove()
                            }
                        }
                    }

                    try {
                        if (Config.isRunning) Thread.sleep(100)
                    } catch (e: Exception) {
                        logger.error("unexpected error", e)
                }
                } catch (e: Exception) {
                    logger.error("unexpected error", e)
                }
            }
        } else {
            Main.logger.error("An error occurred when the server have try a connection on the Mysql server. Please check your identification.")
        }
    }

    @JvmStatic
    fun stop(reason: String) {
        Logging.getInstance().write("Error", reason)

        GameServer.setState(0)
        DatabaseManager.get(HeroicMobsGroupsData::class.java).deleteAll()
        DatabaseManager.get(HeroicMobsGroupsData::class.java).deleteAllFix()

        for (map in World.world.maps) {
            for (group in map.mobGroups.values) {
                if (!group.isFix)
                    DatabaseManager.get(HeroicMobsGroupsData::class.java).insert(map.id, group)
                else
                    DatabaseManager.get(HeroicMobsGroupsData::class.java).insertFix(map.id, group)
            }
        }

        WorldSave.cast(0)
        GameServer.setState(0)
        checkStop()
    }

    private fun checkStop() {
        if (!Config.isSaving)
            System.exit(0)
        else
            TimerWaiter.addNext({ checkStop() }, 5000)
    }

    @JvmStatic
    fun clear() { //~30ms
        AnsiConsole.out.print("[H[2J")
    }
}
