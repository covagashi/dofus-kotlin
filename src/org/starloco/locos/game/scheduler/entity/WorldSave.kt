package org.starloco.locos.game.scheduler.entity

import org.starloco.locos.client.Player
import org.starloco.locos.common.SocketManager
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.game.*
import org.starloco.locos.database.data.login.*
import org.starloco.locos.game.GameServer
import org.starloco.locos.game.scheduler.Updatable
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Config
import java.util.ArrayList
import java.util.Objects

class WorldSave private constructor(wait: Int) : Updatable<Void>(wait) {

    override fun update() {
        if (this.verify())
            if (!Config.isSaving) {
                thread = Thread { cast(1) }
                thread!!.name = WorldSave::class.java.name
                thread!!.isDaemon = true
                thread!!.start()
            }
    }

    companion object {
        @JvmField
        val instance = WorldSave(20 * 60 * 1000)
        private var thread: Thread? = null

        @JvmStatic
        fun cast(trys: Int) {
            if (trys != 0)
                GameServer.setState(2)

            try {
                World.world.logger.debug("Starting the save of the world..")
                SocketManager.GAME_SEND_Im_PACKET_TO_ALL("1164;")
                Config.isSaving = true

                /* Save of data */
                World.world.logger.info("-> of accounts.")
                World.world.accounts.stream().filter(Objects::nonNull).forEach { account -> (DatabaseManager.get(AccountData::class.java) as AccountData).update(account) }

                World.world.logger.info("-> of players.")
                World.world.logger.info("-> of members of guilds.")
                World.world.players.stream().filter(Objects::nonNull).filter { it.isOnline }.forEach { player ->
                    (DatabaseManager.get(PlayerData::class.java) as PlayerData).update(player)
                    if (player.guildMember != null)
                        (DatabaseManager.get(GuildMemberData::class.java) as GuildMemberData).update(player)
                }

                World.world.logger.info("-> of prisms.")
                World.world.prisms.values.forEach { prism -> (DatabaseManager.get(PrismData::class.java) as PrismData).update(prism) }

                World.world.logger.info("-> of guilds.")
                World.world.guilds.values.forEach { guild -> (DatabaseManager.get(GuildData::class.java) as GuildData).update(guild) }

                World.world.logger.info("-> of collectors.")
                World.world.collectors.values.stream().filter { it.inFight <= 0 }.forEach { collector -> (DatabaseManager.get(CollectorData::class.java) as CollectorData).update(collector) }

                World.world.logger.info("-> of houses.")
                World.world.houses.values.stream().filter { it.ownerId > 0 }.forEach { house -> (DatabaseManager.get(HouseData::class.java) as HouseData).update(house) }

                World.world.logger.info("-> of trunks.")
                World.world.trunks.values.forEach { trunk -> (DatabaseManager.get(TrunkData::class.java) as TrunkData).update(trunk) }

                World.world.logger.info("-> of parks.")
                World.world.mountparks.values.stream().filter { it.owner > 0 || it.owner == -1 }.forEach { mp -> (DatabaseManager.get(MountParkData::class.java) as MountParkData).update(mp) }

                World.world.logger.info("-> of mounts.")
                World.world.mounts.values.forEach { mount -> (DatabaseManager.get(MountData::class.java) as MountData).update(mount) }

                World.world.logger.info("-> of areas.")
                World.world.areas.values.forEach { area -> (DatabaseManager.get(AreaData::class.java) as AreaData).update(area) }
                World.world.subAreas.values.forEach { subArea -> (DatabaseManager.get(SubAreaData::class.java) as SubAreaData).update(subArea) }

                World.world.logger.info("-> of objects.")
                try {
                    ArrayList(World.world.gameObjects).stream().filter(Objects::nonNull).filter { it.guid > 0 }
                        .forEach { obj ->
                            try {
                                (DatabaseManager.get(ObjectData::class.java) as ObjectData).update(obj)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                /* end save of data */
                World.world.logger.debug("The save has been doing successfully !")
                SocketManager.GAME_SEND_Im_PACKET_TO_ALL("1165;")
            } catch (exception: Exception) {
                exception.printStackTrace()
                World.world.logger.error("Error when trying save of the world : " + exception.message)
                if (trys < 10) {
                    World.world.logger.error("Fail of the save, num of try : " + (trys + 1) + ".")
                    cast(trys + 1)
                    return
                }
                Config.isSaving = false
            } finally {
                Config.isSaving = false
            }

            if (trys != 0) GameServer.setState(1)

            val t = thread
            if (t != null) {
                World.world.maps.stream().filter { map -> map != null && map.mobGroups != null }
                    .forEach { map ->
                        map.mobGroups.values.stream().filter(Objects::nonNull).forEach { it.addStarBonus() }
                        map.fixMobGroups.values.stream().filter(Objects::nonNull).forEach { it.addStarBonus() }
                    }
                thread = null
                t.interrupt()
            }
        }
    }
}
