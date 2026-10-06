package org.starloco.locos.client

import org.starloco.locos.database.data.game.QuestProgressData
import org.starloco.locos.kernel.Config
import org.starloco.locos.kernel.Constant
import org.starloco.locos.quest.QuestProgress
import org.starloco.locos.script.proxy.SAccount
import org.starloco.locos.util.Pair
import org.starloco.locos.common.SocketManager
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.game.BankData
import org.starloco.locos.database.data.game.GiftData
import org.starloco.locos.database.data.login.AccountData
import org.starloco.locos.database.data.login.MountData
import org.starloco.locos.database.data.login.PlayerData
import org.starloco.locos.game.GameClient
import org.starloco.locos.game.world.World
import org.starloco.locos.hdv.BigStoreListing
import org.starloco.locos.kernel.Main
import org.starloco.locos.`object`.GameObject
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.Objects
import java.util.Optional
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream

class Account(guid: Int, val name: String, val pseudo: String,
              val answer: String, banned: Boolean,
              lastIp: String, lastConnectionDate: String, friends: String?,
              enemy: String?, points: Int, subscriber: Long, muteTime: Long, mutePseudo: String,
              lastVoteIP: String?, heureVote: String) {

    val scriptVal: SAccount = SAccount(this)
    val id: Int = guid
    var currentIp = ""
    var lastIP = ""
    var lastConnectionDate = ""
    var points: Long = 0
        get() {
            field = DatabaseManager.get(AccountData::class.java).loadPoints(name).toLong()
            return field
        }
    private var muteTime: Long = 0
    private var mutePseudo = ""
    var isBanned = false
    private var subscriber: Long = 1
    private var bankKamas: Long = 0
    var lastVoteIP: String? = null
    var currentPlayer: Player? = null
    var gameClient: GameClient? = null
    var state: Byte = 0
        private set
    var heureVote: Long = 0
    val bank: MutableList<GameObject> = ArrayList()
    private val friends: MutableList<Int> = ArrayList()
    private val enemys: MutableList<Int> = ArrayList()
    private val hdvsItems: Map<Int, List<BigStoreListing>>
    private val questsProgression: MutableMap<Int, MutableMap<Int, QuestProgress>> = HashMap()

    init {
        this.isBanned = banned
        this.lastIP = lastIp
        this.lastConnectionDate = lastConnectionDate
        this.hdvsItems = World.world.getMyItems(guid)
        this.points = points.toLong()
        this.subscriber = subscriber
        this.muteTime = muteTime
        this.mutePseudo = mutePseudo
        this.lastVoteIP = lastVoteIP

        if (heureVote.equals("", ignoreCase = true)) this.heureVote = 0
        else this.heureVote = java.lang.Long.parseLong(heureVote)

        if (friends != null && !friends.equals("", ignoreCase = true)) {
            for (f in friends.split(";")) {
                try {
                    this.friends.add(Integer.parseInt(f))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        if (enemy != null && !enemy.equals("", ignoreCase = true)) {
            for (e in enemy.split(";")) {
                try {
                    this.enemys.add(Integer.parseInt(e))
                } catch (e1: Exception) {
                    e1.printStackTrace()
                }
            }
        }

        if (DatabaseManager.get(GiftData::class.java).load(guid)?.second == null)
            DatabaseManager.get(GiftData::class.java).insert(Pair(this, ""))
    }

    fun modPoints(d: Long): Boolean {
        val ret = DatabaseManager.get(AccountData::class.java).modPoints(id, d)
        if (!ret.second) return false
        points = ret.first
        return true
    }

    fun mute(minutes: Short, pseudo: String) {
        if (minutes <= 0) return
        muteTime = System.currentTimeMillis() + minutes * 60000
        mutePseudo = pseudo
        DatabaseManager.get(AccountData::class.java).update(this)
        if (this.currentPlayer != null) this.currentPlayer!!.send("Im117;" + pseudo + "~" + minutes)
    }

    fun unMute() {
        if (muteTime == 0L) return
        muteTime = 0
        mutePseudo = ""
        DatabaseManager.get(AccountData::class.java).update(this)
    }

    fun isMuted(): Boolean {
        if (muteTime == 0L)
            return false
        if (muteTime >= System.currentTimeMillis())
            return true
        muteTime = 0
        mutePseudo = ""
        DatabaseManager.get(AccountData::class.java).update(this)
        return false
    }

    fun getMuteTime(): Long {
        if (!isMuted())
            return 0
        return muteTime
    }

    fun getMutePseudo(): String {
        if (!isMuted())
            return ""
        return mutePseudo
    }

    fun parseBankObjectsToDB(): String {
        val str = StringBuilder()
        if (this.bank.isEmpty())
            return ""
        for (gameObject in this.bank)
            str.append(gameObject.guid).append("|")
        return str.toString()
    }

    fun getBankKamas(): Long {
        return this.bankKamas
    }

    fun setBankKamas(i: Long) {
        this.bankKamas = i
        DatabaseManager.get(BankData::class.java).update(this)
    }

    fun getPlayers(): Map<Int, Player> {
        return World.world.players.stream()
            .filter(Objects::nonNull)
            .filter { player -> player.getAccount() != null && player.getAccount().id == this.id }
            .collect(Collectors.toMap(Player::id, Function.identity()))
    }

    fun isOnline(): Boolean {
        return this.gameClient != null
    }

    fun setState(state: Int) {
        this.state = state.toByte()
        DatabaseManager.get(AccountData::class.java).update(this)
    }

    fun setSubscribe() {
        this.subscriber = DatabaseManager.get(AccountData::class.java).getSubscribe(this.id)
    }

    fun getSubscribeRemaining(): Long {
        val remaining = this.subscriber - System.currentTimeMillis()
        if (!Config.subscription)
            return if (remaining > 0L) remaining else 525600L
        return Math.max(remaining, 0L)
    }

    fun isSubscribe(): Boolean {
        if (!Config.subscription)
            return true
        val remaining = this.subscriber - System.currentTimeMillis()
        return remaining > 0L
    }

    fun isSubscribeWithoutCondition(): Boolean {
        val remaining = this.subscriber - System.currentTimeMillis()
        return remaining > 0L
    }

    fun createPlayer(name: String, sexe: Int, classe: Int, color1: Int, color2: Int, color3: Int): Boolean {
        val perso = Player.create(name, sexe, classe, color1, color2, color3, this)
        return perso != null
    }

    fun deletePlayer(guid: Int) {
        if (this.getPlayers().containsKey(guid))
            World.world.removePlayer(this.getPlayers()[guid]!!)
    }

    fun sendOnline() {
        for (id in this.friends) {
            val player = World.world.getPlayer(id)
            if (player != null && player.showFriendConnection && player.isOnline && player.getAccount().isFriendWith(this.id))
                SocketManager.GAME_SEND_FRIEND_ONLINE(this.currentPlayer!!, player)
        }
    }

    fun addFriend(id: Int) {
        if (this.id == id) {
            SocketManager.GAME_SEND_FA_PACKET(this.currentPlayer!!, "Ey")
            return
        }

        val account = World.world.ensureAccountLoaded(id)

        if (account == null) {
            SocketManager.GAME_SEND_MESSAGE(this.currentPlayer!!, this.currentPlayer!!.getLang().trans("client.account.addfriend.notexist.account"))
            return
        }

        val player = account.currentPlayer // Il est arrivé que le personnage soit null alors que ... non !

        if (player == null) {
            SocketManager.GAME_SEND_MESSAGE(this.currentPlayer!!, this.currentPlayer!!.getLang().trans("client.account.addfriend.notexist.player"))
            return
        }

        val group = player.getGroup()

        if (group != null && !group.isPlayer) {
            SocketManager.GAME_SEND_MESSAGE(this.currentPlayer!!, this.currentPlayer!!.getLang().trans("client.account.addfriend.staff"))
            return
        }
        if (!this.friends.contains(id)) {
            this.friends.add(id)
            SocketManager.GAME_SEND_FA_PACKET(this.currentPlayer!!, "K" + account.pseudo + player.parseToFriendList(id))
            DatabaseManager.get(AccountData::class.java).update(this)
        } else {
            SocketManager.GAME_SEND_FA_PACKET(this.currentPlayer!!, "Ea")
        }
    }

    fun removeFriend(id: Int) {
        if (this.friends.contains(id)) {
            val iterator = this.friends.iterator()
            while (iterator.hasNext())
                if (iterator.next() == id)
                    iterator.remove()
            DatabaseManager.get(AccountData::class.java).update(this)
        }
        SocketManager.GAME_SEND_FD_PACKET(this.currentPlayer!!, "K")
    }

    fun isFriendWith(id: Int): Boolean {
        return friends.contains(id)
    }

    fun getFriendIds(): Stream<Int> {
        return friends.stream()
    }

    fun parseFriendListToDB(): String {
        var str = ""
        for (i in this.friends) {
            if (!str.equals("", ignoreCase = true))
                str += ";"
            str += i.toString()
        }
        return str
    }

    fun parseFriendList(): String {
        val str = StringBuilder()
        if (this.friends.isEmpty())
            return ""
        for (i in this.friends) {
            val C = World.world.ensureAccountLoaded(i)
            if (C == null)
                continue
            str.append("|").append(C.pseudo)

            if (!C.isOnline())
                continue
            val P = C.currentPlayer
            if (P == null)
                continue
            str.append(P.parseToFriendList(id))
        }
        return str.toString()
    }

    fun addEnemy(packet: String, guid: Int) {
        if (this.id == guid) {
            SocketManager.GAME_SEND_FA_PACKET(this.currentPlayer!!, "Ey")
            return
        }
        if (!this.enemys.contains(guid)) {
            this.enemys.add(guid)
            val Pr = World.world.getPlayerByName(packet)
            SocketManager.GAME_SEND_ADD_ENEMY(this.currentPlayer!!, Pr!!)
            DatabaseManager.get(AccountData::class.java).update(this)
        } else
            SocketManager.GAME_SEND_iAEA_PACKET(this.currentPlayer!!)
    }

    fun removeEnemy(id: Int) {
        if (this.enemys.contains(id)) {
            val iterator = this.enemys.iterator()
            while (iterator.hasNext())
                if (iterator.next() == id)
                    iterator.remove()
            DatabaseManager.get(AccountData::class.java).update(this)
        }
        SocketManager.GAME_SEND_iD_COMMANDE(this.currentPlayer!!, "K")
    }

    fun isEnemyWith(id: Int): Boolean {
        return enemys.contains(id)
    }

    fun parseEnemyListToDB(): String {
        var str = ""
        for (i in this.enemys) {
            if (!str.equals("", ignoreCase = true))
                str += ";"
            str += i.toString()
        }
        return str
    }

    fun parseEnemyList(): String {
        val str = StringBuilder()
        if (this.enemys.isEmpty())
            return ""
        for (i in this.enemys) {
            val C = World.world.ensureAccountLoaded(i)
            if (C == null)
                continue
            str.append("|").append(C.pseudo)

            if (!C.isOnline())
                continue
            val P = C.currentPlayer
            if (P == null)
                continue
            str.append(P.parseToEnemyList(id))
        }
        return str.toString()
    }

    fun getHdvEntries(id: Int): List<BigStoreListing> {
        return Collections.unmodifiableList(Optional.ofNullable(this.hdvsItems[id]).orElse(Collections.emptyList()))
    }

    fun countHdvEntries(id: Int): Int {
        return Optional.ofNullable(this.hdvsItems[id]).orElse(Collections.emptyList()).size
    }

    fun resetAllChars() {
        for (player in this.getPlayers().values) {
            if (player.fight != null) {
                if (player.party != null)
                    player.party!!.leave(player)
                player.online = true
            }

            if (player.exchangeAction != null)
                GameClient.leaveExchange(player)
            if (player.party != null)
                player.party!!.leave(player)
            if (player.curCell != null)
                player.curCell.removePlayer(player)
            if (player.curMap != null && player.isOnline)
                SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(player.curMap, player.id)

            player.online = false
        }
    }

    fun disconnect(player: Player) {
        DatabaseManager.get(PlayerData::class.java).updateLogged(player.id, 0)
        DatabaseManager.get(AccountData::class.java).setLogged(this.id, 0)

        if (player.exchangeAction != null)
            GameClient.leaveExchange(player)
        if (player.party != null)
            player.party!!.leave(player)
        if (player.mount != null)
            DatabaseManager.get(MountData::class.java).update(player.mount!!)
        if (player.fight != null) {
            if (player.fight!!.onPlayerDisconnection(player, false)) {
                DatabaseManager.get(PlayerData::class.java).update(player)
                return
            }
        }
        if (Config.modeHeroic) {
            if (player.alignment == Constant.ALIGNEMENT_BONTARIEN) Main.angels--
            else if (player.alignment == Constant.ALIGNEMENT_BRAKMARIEN) Main.demons--
            player.alignment = Constant.ALIGNEMENT_NEUTRE
        }
        this.currentPlayer = null
        this.gameClient = null
        this.currentIp = ""
        for (character in this.getPlayers().values)
            DatabaseManager.get(PlayerData::class.java).update(character)

        player.resetVars()
        this.resetAllChars()
        DatabaseManager.get(AccountData::class.java).update(this)
        World.world.logger.info("The player " + player.name + " come to disconnect.")
    }

    fun updateVote(hour: String, ip: String) {
        if (hour.equals("", ignoreCase = true)) this.heureVote = 0
        else this.heureVote = java.lang.Long.parseLong(hour)
        this.lastVoteIP = ip
    }

    fun parseBank(kamas: Int, items: String?) {
        if (kamas == -1 && items == null) {
            DatabaseManager.get(BankData::class.java).insert(this)
        } else {
            this.bankKamas = kamas.toLong()

            if (items != "") {
                for (item in items!!.split("\\|")) {
                    if (item != "") {
                        val obj = World.world.getGameObject(Integer.parseInt(item))
                        if (obj != null)
                            this.bank.add(obj)
                    }
                }
            }
        }
    }

    fun addGift(template: Int, quantity: Short, jp: Byte) {
        val gd = DatabaseManager.get(GiftData::class.java)
        val gift = "$template,$quantity,$jp"
        val gifts = gd.load(this.id)?.second
        if (gifts == null || gifts.isEmpty()) {
            gd.update(Pair(this, gift))
        } else {
            gd.update(Pair(this, "$gifts;$gift"))
        }
    }

    fun addQuestProgression(qProgress: QuestProgress) {
        questsProgression.computeIfAbsent(qProgress.playerId) { HashMap() }[qProgress.questId] = qProgress
    }

    fun delQuestProgress(qProgress: QuestProgress) {
        questsProgression.computeIfAbsent(qProgress.playerId) { HashMap() }.remove(qProgress.questId, qProgress)

        val dao = Objects.requireNonNull(DatabaseManager.get(QuestProgressData::class.java))
        dao.delete(qProgress)
    }

    fun getQuestProgress(playerId: Int, questId: Int): QuestProgress? {
        val qp = questsProgression.computeIfAbsent(playerId) { HashMap() }[questId]
        if (qp != null) return qp
        return questsProgression.computeIfAbsent(QuestProgress.NO_PLAYER_ID) { HashMap() }[questId]
    }

    fun getQuestProgressions(playerId: Int): Stream<QuestProgress> {
        return Stream.of(QuestProgress.NO_PLAYER_ID, playerId)
            .flatMap { id -> questsProgression.computeIfAbsent(id) { HashMap() }.values.stream() }
    }

    fun saveQuestProgress() {
        val dao = Objects.requireNonNull(DatabaseManager.get(QuestProgressData::class.java))
        this.questsProgression.values.stream().flatMap { m -> m.values.stream() }.forEach(dao::update)
    }

    fun scripted(): SAccount {
        return this.scriptVal
    }
}
