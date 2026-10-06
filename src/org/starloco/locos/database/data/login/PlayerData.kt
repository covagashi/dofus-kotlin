package org.starloco.locos.database.data.login

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.client.Player
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.database.data.game.GuildMemberData
import org.starloco.locos.database.data.game.QuestProgressData
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Config
import org.starloco.locos.kernel.Constant
import org.starloco.locos.kernel.Main
import org.starloco.locos.database.data.login.ObjectData
import org.starloco.locos.database.data.login.MountData
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.Objects

class PlayerData(dataSource: HikariDataSource?) : FunctionDAO<Player>(dataSource, "world_players") {

    override fun loadFully() {
        throw NotImplementedException("cannot load all players at once")
    }

    @Throws(SQLException::class)
    private fun buildFromResultSet(result: ResultSet): Player {
        return Player(result.getInt("id"), result.getString("name"), result.getInt("groupe"), result.getInt("sexe"),
            result.getInt("class"), result.getInt("color1"), result.getInt("color2"), result.getInt("color3"), result.getLong("kamas"),
            result.getInt("spellboost"), result.getInt("capital"), result.getInt("energy"), result.getInt("level"), result.getLong("xp"),
            result.getInt("size"), result.getInt("gfx"), result.getByte("alignement"), result.getInt("account"), this.getStats(result),
            result.getByte("seeFriend"), result.getByte("seeAlign"), result.getByte("seeSeller"), result.getString("canaux"),
            result.getShort("map"), result.getInt("cell"), result.getString("objets"), result.getString("storeObjets"),
            result.getInt("pdvper"), result.getString("spells"), result.getString("savepos"), result.getString("jobs"),
            result.getInt("mountxpgive"), result.getInt("mount"), result.getInt("honor"), result.getInt("deshonor"),
            result.getInt("alvl"), result.getString("zaaps"), result.getByte("title"), result.getInt("wife"),
            result.getString("morphMode"), result.getString("allTitle"), result.getString("emotes"), result.getLong("prison"),
            false, result.getString("parcho"), result.getLong("timeDeblo"), result.getBoolean("noall"),
            result.getString("deadInformation"), result.getByte("deathCount"), result.getLong("totalKills"))
    }

    override fun load(id: Int): Player? {
        val oldPlayer = World.world.getPlayer(id)
        try {
            val player = getData<Player?>("SELECT * FROM " + getTableName() + " WHERE id = '" + id + "' AND server = " + Config.gameServerId + ";") { result ->
                if (!result.next()) null else buildFromResultSet(result)
            }

            if (player == null) return null

            if (oldPlayer != null)
                player.setLastFightForEndFightAction(oldPlayer.lastFight)

            player.VerifAndChangeItemPlace()

            DatabaseManager.get(QuestProgressData::class.java).load(player.id)

            // Find player's guild
            World.world.guilds.values.stream().map { g -> g.getMember(id) }.findFirst().ifPresent { gm -> player.guildMember = gm }

            // Add to world
            World.world.addPlayer(player)

            return player
        } catch (e: SQLException) {
            super.sendError(e)
            Main.stop("unknown")
        }
        return null
    }

    override fun insert(entity: Player): Boolean {
        var statement: PreparedStatement? = null
        var ok = true
        try {
            val connection = this.getConnection()
            val sql = "INSERT INTO " + getTableName() + "(`name`, `sexe`, `class`, `color1`, `color2`, `color3`, `kamas`, `spellboost`, `capital`, `energy`, `level`, `xp`, `size`, `gfx`, `account`, `cell`, `map`, `spells`, `objets`, `storeObjets`, `morphMode`, `server`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'','','0',?)"
            statement = connection?.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
            statement?.setString(1, entity.name)
            statement?.setInt(2, entity.sexe)
            statement?.setInt(3, entity.classe)
            statement?.setInt(4, entity.color1)
            statement?.setInt(5, entity.color2)
            statement?.setInt(6, entity.color3)
            statement?.setLong(7, entity.kamas)
            statement?.setInt(8, entity.get_spellPts())
            statement?.setInt(9, entity.capital)
            statement?.setInt(10, entity.energy)
            statement?.setInt(11, entity.level)
            statement?.setLong(12, entity.exp)
            statement?.setInt(13, entity.size)
            statement?.setInt(14, entity.gfxId)
            statement?.setInt(15, entity.accID)
            statement?.setInt(16, entity.curCell.cellId)
            statement?.setInt(17, entity.curMap.id)
            statement?.setString(18, entity.encodeSpellsToDB())
            statement?.setInt(19, Config.gameServerId)

            val affectedRows = statement?.executeUpdate() ?: 0
            if (affectedRows != 0) {
                statement?.generatedKeys?.use { generatedKeys ->
                    if (generatedKeys.next()) {
                        entity.id = generatedKeys.getInt(1)
                    } else {
                        ok = false
                    }
                }
            } else {
                ok = false
            }
        } catch (e: SQLException) {
            super.sendError(e)
            ok = false
        } finally {
            close(statement)
        }
        return ok
    }

    override fun delete(entity: Player) {
        var p: PreparedStatement? = null
        try {
            p = getPreparedStatement("DELETE FROM " + getTableName() + " WHERE id = ?")
            p?.setInt(1, entity.id)
            execute(p)

            val dao = DatabaseManager.get(ObjectData::class.java)
            if (entity.getItemsIDSplitByChar(",") != "")
                for (id in entity.getItemsIDSplitByChar(",").split(","))
                    dao.delete(World.world.getGameObject(Integer.parseInt(id))!!)
            if (entity.getStoreItemsIDSplitByChar(",") != "")
                for (id in entity.getStoreItemsIDSplitByChar(",").split(","))
                    dao.delete(World.world.getGameObject(Integer.parseInt(id))!!)
            if (entity.mount != null)
                DatabaseManager.get(MountData::class.java).update(entity.mount!!)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(p)
        }
    }

    override fun update(entity: Player) {
        var p: PreparedStatement? = null
        try {
            p = getPreparedStatement("UPDATE " + getTableName() + " SET `kamas`= ?, `spellboost`= ?, `capital`= ?, `energy`= ?, `level`= ?, `xp`= ?, `size` = ?, `gfx`= ?, `alignement`= ?, `honor`= ?, `deshonor`= ?, `alvl`= ?, `vitalite`= ?, `force`= ?, `sagesse`= ?, `intelligence`= ?, `chance`= ?, `agilite`= ?, `seeFriend`= ?, `seeAlign`= ?, `seeSeller`= ?, `canaux`= ?, `map`= ?, `cell`= ?, `pdvper`= ?, `spells`= ?, `objets`= ?, `storeObjets`= ?, `savepos`= ?, `zaaps`= ?, `jobs`= ?, `mountxpgive`= ?, `mount`= ?, `title`= ?, `wife`= ?, `morphMode`= ?, `allTitle` = ?, `emotes` = ?, `prison` = ?, `parcho` = ?, `timeDeblo` = ?, `noall` = ?, `deadInformation` = ?, `deathCount` = ?, `totalKills` = ? WHERE `id` = ? LIMIT 1")
            p?.setLong(1, entity.kamas)
            p?.setInt(2, entity.get_spellPts())
            p?.setInt(3, entity.capital)
            p?.setInt(4, entity.energy)
            p?.setInt(5, entity.level)
            p?.setLong(6, entity.exp)
            p?.setInt(7, entity.size)
            p?.setInt(8, entity.gfxId)
            p?.setInt(9, entity.alignment)
            p?.setInt(10, entity.honor)
            p?.setInt(11, entity.deshonor)
            p?.setInt(12, entity.aLvl)
            p?.setInt(13, entity.stats.getEffect(Constant.STATS_ADD_VITA))
            p?.setInt(14, entity.stats.getEffect(Constant.STATS_ADD_FORC))
            p?.setInt(15, entity.stats.getEffect(Constant.STATS_ADD_SAGE))
            p?.setInt(16, entity.stats.getEffect(Constant.STATS_ADD_INTE))
            p?.setInt(17, entity.stats.getEffect(Constant.STATS_ADD_CHAN))
            p?.setInt(18, entity.stats.getEffect(Constant.STATS_ADD_AGIL))
            p?.setInt(19, if (entity.showFriendConnection) 1 else 0)
            p?.setInt(20, if (entity.showWings) 1 else 0)
            p?.setInt(21, if (entity.seeSeller) 1 else 0)
            p?.setString(22, entity.canaux)
            if (entity.curMap != null) p?.setInt(23, entity.curMap.id)
            else p?.setInt(23, 7411)
            if (entity.curCell != null) p?.setInt(24, entity.curCell.cellId)
            else p?.setInt(24, 311)
            p?.setInt(25, entity.get_pdvper())
            p?.setString(26, entity.encodeSpellsToDB())
            p?.setString(27, entity.parseObjetsToDB())
            p?.setString(28, entity.parseStoreItemstoBD())
            p?.setString(29, entity.savePos.toString(","))
            p?.setString(30, entity.parseZaaps())
            p?.setString(31, entity.parseJobData())
            p?.setInt(32, entity.mountXpGive)
            p?.setInt(33, if (entity.mount != null) entity.mount!!.id else -1)
            p?.setByte(34, entity.currentTitle)
            p?.setInt(35, entity.wife)
            p?.setString(36, (if (entity.morphMode) 1 else 0).toString() + ";" + entity.morphId)
            p?.setString(37, entity.allTitle)
            p?.setString(38, entity.parseEmoteToDB())
            p?.setLong(39, if (entity.isInEnnemyFaction) entity.enteredOnEnnemyFaction else 0)
            p?.setString(40, entity.parseStatsParcho())
            p?.setLong(41, entity.timeTaverne)
            p?.setBoolean(42, entity.noall)
            p?.setString(43, entity.getDeathInformation())
            p?.setByte(44, entity.deathCount)
            p?.setLong(45, entity.totalKills)
            p?.setInt(46, entity.id)
            execute(p)
            if (entity.guildMember != null)
                DatabaseManager.get(GuildMemberData::class.java).update(entity)
            if (entity.mount != null)
                DatabaseManager.get(MountData::class.java).update(entity.mount!!)
            entity.saveQuestProgress()
        } catch (e: Exception) {
            super.sendError(e)
        } finally {
            close(p)
        }
    }

    override fun getReferencedClass(): Class<*> {
        return PlayerData::class.java
    }

    @Throws(SQLException::class)
    private fun getStats(result: ResultSet): HashMap<Int, Int> {
        val stats = HashMap<Int, Int>()
        stats[Constant.STATS_ADD_VITA] = result.getInt("vitalite")
        stats[Constant.STATS_ADD_FORC] = result.getInt("force")
        stats[Constant.STATS_ADD_SAGE] = result.getInt("sagesse")
        stats[Constant.STATS_ADD_INTE] = result.getInt("intelligence")
        stats[Constant.STATS_ADD_CHAN] = result.getInt("chance")
        stats[Constant.STATS_ADD_AGIL] = result.getInt("agilite")
        return stats
    }

    fun loadByAccountId(id: Int) {
        try {
            val account = World.world.ensureAccountLoaded(id)
            if (account != null && account.getPlayers() != null)
                account.getPlayers().values.stream().filter(Objects::nonNull).forEach { p -> World.world.verifyClone(p) }
        } catch (e: Exception) {
            super.sendError(e)
        }

        try {
            getData("SELECT * FROM " + getTableName() + " WHERE account = '" + id + "' AND server = '" + Config.gameServerId + "'") { result ->
                while (result.next()) {
                    val p = World.world.getPlayer(result.getInt("id"))
                    if (p != null) {
                        if (p.fight != null) {
                            continue
                        }
                    }

                    val player = buildFromResultSet(result)

                    if (p != null)
                        player.setLastFightForEndFightAction(p.lastFight)
                    player.VerifAndChangeItemPlace()

                    DatabaseManager.get(QuestProgressData::class.java).load(id)
                    // Find player's guild
                    World.world.guilds.values.stream().map { g -> g.getMember(player.id) }.filter(Objects::nonNull).findFirst().ifPresent { gm -> player.guildMember = gm }

                    World.world.addPlayer(player)
                }
            }
        } catch (e: SQLException) {
            super.sendError(e)
            Main.stop("unknown")
        }
    }

    fun loadTitles(guid: Int): String {
        try {
            return getData<String>("SELECT * FROM " + getTableName() + " WHERE id = '" + guid + "';") { result ->
                if (!result.next()) "" else result.getString("allTitle")
            } ?: ""
        } catch (e: SQLException) {
            super.sendError(e)
        }
        return ""
    }

    fun updateInfos(perso: Player) {
        var p: PreparedStatement? = null
        try {
            p = getPreparedStatement("UPDATE " + getTableName() + " SET `name` = ?, `sexe`=?, `class`= ?, `color1` = ?, `color2` = ?, `color3` = ? WHERE `id`= ?;")
            p?.setString(1, perso.name)
            p?.setInt(2, perso.sexe)
            p?.setInt(3, perso.classe)
            p?.setInt(4, perso.color1)
            p?.setInt(5, perso.color2)
            p?.setInt(6, perso.color3)
            p?.setInt(7, perso.id)
            execute(p)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(p)
        }
    }

    fun updateGroupe(group: Int, name: String) {
        var p: PreparedStatement? = null
        try {
            p = getPreparedStatement("UPDATE " + getTableName() + " SET `groupe` = ? WHERE `name` = ?;")

            p?.setInt(1, group)
            p?.setString(2, name)
            execute(p)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(p)
        }
    }

    fun updateGroupe(perso: Player) {
        var p: PreparedStatement? = null
        try {
            p = getPreparedStatement("UPDATE " + getTableName() + " SET `groupe` = ? WHERE `id`= ?")
            val id = if (perso.getGroup() != null) perso.getGroup().id else -1
            p?.setInt(1, id)
            p?.setInt(2, perso.id)
            execute(p)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(p)
        }
    }

    fun updateTimeTaverne(player: Player) {
        var p: PreparedStatement? = null
        try {
            p = getPreparedStatement("UPDATE " + getTableName() + " SET `timeDeblo` = ? WHERE `id` = ?")
            p?.setLong(1, player.timeTaverne)
            p?.setInt(2, player.id)
            execute(p)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(p)
        }
    }

    fun updateTitles(guid: Int, title: String) {
        var p: PreparedStatement? = null
        try {
            p = getPreparedStatement("UPDATE " + getTableName() + " SET `allTitle` = ? WHERE `id` = ?")
            p?.setString(1, title)
            p?.setInt(2, guid)
            execute(p)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(p)
        }
    }

    fun updateLogged(guid: Int, logged: Int) {
        var p: PreparedStatement? = null
        try {
            p = getPreparedStatement("UPDATE " + getTableName() + " SET `logged` = ? WHERE `id` = ?")
            p?.setInt(1, logged)
            p?.setInt(2, guid)
            execute(p)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(p)
        }
    }

    fun exist(name: String): Boolean {
        try {
            return getData<Boolean>("SELECT COUNT(*) AS exist FROM " + getTableName() + " WHERE name LIKE '" + name + "';") { result ->
                if (!result.next()) false else result.getInt("exist") > 0
            } ?: false
        } catch (e: SQLException) {
            super.sendError(e)
        }
        return false
    }

    fun reloadGroup(p: Player) {
        try {
            getData("SELECT groupe FROM " + getTableName() + " WHERE id = '" + p.id + "'") { result ->
                if (result.next()) {
                    val group = result.getInt("groupe")
                    p.setGroupe(group, false)
                }
            }
        } catch (e: SQLException) {
            super.sendError(e)
        }
    }

    fun canRevive(player: Player): Int {
        try {
            return getData<Int>("SELECT id, revive FROM " + getTableName() + " WHERE `id` = '" + player.id + "';") { result ->
                if (!result.next()) 0 else result.getInt("revive")
            } ?: 0
        } catch (e: SQLException) {
            super.sendError(e)
        }
        return 0
    }

    fun setRevive(player: Player) {
        try {
            val p = getPreparedStatement("UPDATE " + getTableName() + " SET `revive` = 0 WHERE `id` = '" + player.id + "';")
            execute(p)
            close(p)
        } catch (e: SQLException) {
            super.sendError(e)
        }
    }
}
