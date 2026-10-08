package org.starloco.locos.game

import java.net.InetSocketAddress
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.starloco.locos.api.AbstractDofusMessage
import org.starloco.locos.common.CryptManager
import org.starloco.locos.entity.exchange.NpcExchange
import org.starloco.locos.entity.map.*
import org.starloco.locos.game.action.type.BigStoreActionData
import org.starloco.locos.game.action.type.DocumentActionData
import org.starloco.locos.game.action.type.NpcDialogActionData
import org.starloco.locos.game.action.type.ScenarioActionData
import org.starloco.locos.hdv.BigStore
import org.starloco.locos.hdv.BigStoreListingLotSize
import org.starloco.locos.script.DataScriptVM
import org.starloco.locos.util.Pair
import org.apache.mina.core.session.IoSession
import org.starloco.locos.area.Area
import org.starloco.locos.area.map.GameCase
import org.starloco.locos.area.map.GameMap
import org.starloco.locos.auction.AuctionManager
import org.starloco.locos.client.Account
import org.starloco.locos.client.Player
import org.starloco.locos.client.other.Party
import org.starloco.locos.command.CommandAdmin
import org.starloco.locos.command.CommandPlayer
import org.starloco.locos.command.administration.AdminUser
import org.starloco.locos.common.Formulas
import org.starloco.locos.common.PathFinding
import org.starloco.locos.common.SocketManager
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.game.*
import org.starloco.locos.database.data.login.*
import org.starloco.locos.entity.Collector
import org.starloco.locos.entity.Prism
import org.starloco.locos.entity.exchange.CraftSecure
import org.starloco.locos.entity.exchange.Exchange
import org.starloco.locos.entity.exchange.PlayerExchange
import org.starloco.locos.entity.mount.Mount
import org.starloco.locos.entity.npc.Npc
import org.starloco.locos.entity.npc.NpcTemplate
import org.starloco.locos.entity.pet.Pet
import org.starloco.locos.entity.pet.PetEntry
import org.starloco.locos.event.EventManager
import org.starloco.locos.event.type.Event
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.spells.Spell
import org.starloco.locos.game.action.ExchangeAction
import org.starloco.locos.game.action.GameAction
import org.starloco.locos.game.world.World
import org.starloco.locos.game.world.World.Couple
import org.starloco.locos.guild.Guild
import org.starloco.locos.guild.GuildMember
import org.starloco.locos.hdv.BigStoreListing
import org.starloco.locos.job.Job
import org.starloco.locos.job.JobAction
import org.starloco.locos.job.JobConstant
import org.starloco.locos.job.JobStat
import org.starloco.locos.job.maging.BreakingObject
import org.starloco.locos.job.maging.Rune
import org.starloco.locos.kernel.Config
import org.starloco.locos.kernel.Constant
import org.starloco.locos.kernel.Logging
import org.starloco.locos.kernel.Main
import org.starloco.locos.lang.LangEnum
import org.starloco.locos.`object`.GameObject
import org.starloco.locos.`object`.ObjectTemplate
import org.starloco.locos.`object`.entity.Fragment
import org.starloco.locos.`object`.entity.SoulStone
import org.starloco.locos.util.TimerWaiter
import org.starloco.locos.util.generator.NameGenerator
import org.starloco.locos.common.splitJ

private val log = LoggerFactory.getLogger(GameClient::class.java)

open class GameClient {

    @get:JvmName("session")
    lateinit var session: IoSession
    @get:JvmName("account")
    lateinit var account: Account
    @get:JvmName("player")
    lateinit var player: Player
    private var walk: Boolean= false
    private lateinit var adminUser: AdminUser
    private var language: LangEnum = LangEnum.ENGLISH
    private val actions: MutableMap<Int,GameAction> = HashMap()
    @JvmField public var timeLastTradeMsg: Long = 0
    @JvmField public var timeLastRecrutmentMsg: Long = 0
    @JvmField public var timeLastAlignMsg: Long = 0
    @JvmField public var timeLastChatMsg: Long = 0
    @JvmField public var timeLastIncarnamMsg: Long = 0
    @JvmField public var timeLastTaverne: Long = 0
    var lastPacketTime: Long = 0
    @JvmField public var action: Int = 0

    @get:JvmName("preparedKeys")
    lateinit var preparedKeys: String

    constructor(session: IoSession) {
        this.session = session
        this.session.write("HG")
    }
    
    fun getSession(): IoSession {
        return session
    }

    fun getPlayer(): Player {
        return this.player
    }

    fun getAccount(): Account {
        return account
    }

    fun getLanguage(): LangEnum {
        return language
    }

    fun getPreparedKeys(): String{
        return preparedKeys
    }

    fun parsePacket(packet: String)   {
        this.lastPacketTime = System.currentTimeMillis()

        if (packet.length > 3 && packet.substring(0, 4).equals("ping", ignoreCase = true)) {
            this.send("pong")
            return
        }
        if(Logging.USE_LOG) {
            if (this::player.isInitialized) {
                Logging.getInstance().write("RecvPacket", this.player.name + " : " + this.player.getAccount()!!.currentIp + " : " + packet)
            } else {
                var IP: String = (((this.getSession().getRemoteAddress()) as InetSocketAddress)).getAddress().getHostAddress()
                Logging.getInstance().write("RecvPacket", IP + " : " + packet)
            }
        }

        if(Config.modeEvent) {
            var manager: EventManager = EventManager.instance
            if(manager.state == EventManager.State.STARTED) {
                var event: Event? = manager.getCurrentEvent()
                try {
                    if (event != null && event.onReceivePacket(manager, this.player, packet)) {
                        return
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    player.sendMessage(player.getLang().trans("game.gameclient.event.error"))
                }
            }
        }
        if(packet.isEmpty()) return

        when (packet[0]) {
'ù' -> {
}
'A' -> {parseAccountPacket(packet)
                
}
'H' -> {parseAuthPackets(packet)
                
}
'B' -> {parseBasicsPacket(packet)
                
}
'C' -> {parseConquestPacket(packet)
                
}
'c' -> {parseChanelPacket(packet)
                
}
'D' -> {parseDialogPacket(packet)
                
}
'd' -> {parseDocumentPacket(packet)
                
}
'E' -> {parseExchangePacket(packet)
                
}
'e' -> {parseEnvironementPacket(packet)
                
}
'F' -> {parseFrienDDacket(packet)
                
}
'f' -> {parseFightPacket(packet)
                
}
'G' -> {parseGamePacket(packet)
                
}
'g' -> {parseGuildPacket(packet)
                
}
'h' -> {parseHousePacket(packet)
                
}
'i' -> {parseEnemyPacket(packet)
                
}
'J' -> {parseJobOption(packet)
                
}
'K' -> {parseHouseKodePacket(packet)
                
}
'O' -> {parseObjectPacket(packet)
                
}
'P' -> {parseGroupPacket(packet)
                
}
'R' -> {parseMountPacket(packet)
                
}
'Q' -> {parseQuestData(packet)
                
}
'S' -> {parseSpellPacket(packet)
                
}
'T' -> {parseTutorialsPacket(packet)
                
}
'W' -> {parseWaypointPacket(packet)
                
}
else -> {if(this::player.isInitialized)
                    if(this.player.changeName)
                        this.changeName(packet)                
                
}
}
    }

    /**
     * AccountPacket *
     */
    private fun parseAccountPacket(packet: String) {
        when (packet[1]) {
'A' -> {addCharacter(packet)
                
}
'B' -> {boost(packet)
                
}
'D' -> {deleteCharacter(packet)
                
}
'f' -> {getQueuePosition()
                
}
'g' -> {getGifts(packet.substring(2))
                
}
'G' -> {attributeGiftToCharacter(packet.substring(2))
                
}
'i' -> {sendIdentity(packet)
                
}
'k' -> {
}
'L' -> {getCharacters(/*(packet.length == 2)*/)
                
}
'R' -> {hardcodeRevive((packet.substring(2)).toInt())
                
}
'S' -> {setCharacter(packet)
                
}
'T' -> {parseTicket(packet)
                
}
'V' -> {requestRegionalVersion()
                
}
'P' -> {var name: String = NameGenerator.nameGenerator
                        .compose(((
                                Math.random() * 3 +
                                        Formulas.getRandomValue(1, 5)).toInt()))
                SocketManager.send(this, "APK" + name)
                
}
'E' -> {mimibiote(packet)
                
}
}
    }

    private fun switchCharacter() {
        // 1.39.8 Switch character
        var now: Instant = Instant.now()
        var expiry: Instant = now.plusSeconds(30) // TODO Make that a config
        var jws: String = Jwts.builder()
                .setIssuer("StarLocoGameServer")
                .setSubject((account.name).toString())
                .claim("ip", account.currentIp)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(
                    Keys.hmacShaKeyFor(Base64.getDecoder().decode(Config.exchangeKey)),
                    SignatureAlgorithm.HS256
                )
                .compact()

        send("HS"+jws)
    }

    private fun parseAuthPackets(packet: String) {
        when (packet[1]) {
'S' -> {switchCharacter()
                
}
else -> {
}
}
    }

    private fun mimibiote(packet: String) {
        //TODO: Implement
        if (packet[2] == 'i') {
            if (packet[3] == '1') {
                createMimibiote(packet)
            } else if (packet[3] == '0') {
                dissociateMimibiote(packet)
            }
        }
    }

    private fun createMimibiote(packet: String)
    {
        if(this.player.fight != null) return
        val datas: List<String> = packet.split("|")
        if(datas.size < 3) return

        var idItemToKeep: Int
        var idItemToDelete: Int

        try {
            idItemToKeep = (datas[1]).toInt()
            idItemToDelete = (datas[2]).toInt()
        }catch (e: NumberFormatException) {
            return
        }

        val mimibiote: GameObject? = this.player.getItemTemplate(Constant.ID_TEMPLATE_MIMIBIOTE.toInt())
        if(mimibiote == null) return

        val itemToKeep: GameObject? = World.world.getGameObject(idItemToKeep)
        val itemToDelete: GameObject? = World.world.getGameObject(idItemToDelete)

        if(itemToKeep == null || itemToDelete == null) return
        if(!this.player.hasItemGuid(idItemToKeep) || !this.player.hasItemGuid(idItemToDelete)) return
        if(itemToDelete.position != Constant.ITEM_POS_NO_EQUIPED || itemToKeep.position != Constant.ITEM_POS_NO_EQUIPED) return
        if(itemToKeep.isMimibiote() || itemToDelete.isMimibiote()) return
        if(itemToKeep.template!!.level < itemToDelete.template!!.level) return
        if(itemToKeep.template!!.type != itemToDelete.template!!.type) return
        if(!Constant.isTypeForMimibiote(itemToKeep.template!!.type)) return


        // OK
        val guid: String = Integer.toHexString(itemToDelete.guid)
        val id: String = Integer.toHexString(itemToDelete.template!!.id)
        itemToKeep.addTxtStat(Constant.STATS_MIMIBIOTE, guid+";"+id); // setModification est dedans
        this.player.removeItem(idItemToDelete, 1, true, false)
        this.player.removeItem(mimibiote.guid, 1, true, true)
        SocketManager.GAME_SEND_UPDATE_ITEM(player, itemToKeep)
        SocketManager.GAME_SEND_Im_PACKET(this.player, "022;" + 1 + "~" + itemToDelete.template!!.id)
        SocketManager.GAME_SEND_Im_PACKET(this.player, "022;" + 1 + "~" + mimibiote.template!!.id)

    }

    private fun dissociateMimibiote(packet: String)
    {
        if(this.player.fight != null) return
        val datas: List<String> = packet.split("|")
        if(datas.size < 2) return

        var idItem: Int

        try {
            idItem = (datas[1]).toInt()
        }catch (e: NumberFormatException) {
            return
        }

        val item: GameObject? = World.world.getGameObject(idItem)
        if(item == null) return
        if(!this.player.hasItemGuid(idItem)) return
        if(!item.isMimibiote()) return

        val mimibiote: GameObject = World.world.getObjTemplate(Constant.ID_TEMPLATE_MIMIBIOTE.toInt())!!.createNewItem(1, false)!!
        val idApparat: Int = (item.txtStat!![Constant.STATS_MIMIBIOTE]!!.split(";")[0]).toInt(16)
        val apparat: GameObject? = World.world.getGameObject(idApparat)

        if(apparat == null)
        {
            this.player.sendMessage("Merci de contacter un administrateur. L'objet avec comme ID " + idApparat + " a disparu ...")
            return
        }

        if(this.player.addItem(mimibiote, true, false))
            World.world.addGameObject(mimibiote)
        this.player.addItem(apparat, true, false)
        item.txtStat.remove(Constant.STATS_MIMIBIOTE); // setModification est dedans
        SocketManager.GAME_SEND_UPDATE_ITEM(player, item)
        if(item.position != Constant.ITEM_POS_NO_EQUIPED)
            SocketManager.GAME_SEND_ON_EQUIP_ITEM(this.player.curMap, this.player)

        SocketManager.GAME_SEND_Im_PACKET(this.player, "021;" + 1 + "~" + apparat.template!!.id)
        SocketManager.GAME_SEND_Im_PACKET(this.player, "021;" + 1 + "~" + mimibiote.template!!.id)
    }

    private fun addCharacter(packet: String) {
        var infos: List<String> = packet.substring(2).split("|")
        if (DatabaseManager.get(PlayerData::class.java).exist(infos[0])) {
            SocketManager.GAME_SEND_NAME_ALREADY_EXIST(this)
            return
        }
        //Validation du nom du this.playernnage
        var isValid: Boolean = true
        var name: String = infos[0].lowercase()
        //V?rifie d'abord si il contient des termes d?finit
        if (name.length > 20 || name.length < 3 || name.contains("modo")
                || name.contains("admin") || name.contains("putain")
                || name.contains("administrateur") || name.contains("puta")) {
            isValid = false
        }

        //Si le nom passe le test, on v?rifie que les caract?re entr? sont correct.
        if (isValid) {
            var tiretCount: Int = 0
            var exLetterA: Char = ' '
            var exLetterB: Char = ' '
            for (curLetter in  name.toCharArray()) {
                if (!((curLetter >= 'a' && curLetter <= 'z') || curLetter == '-')) {
                    isValid = false
                    break
                }
                if (curLetter == exLetterA && curLetter == exLetterB) {
                    isValid = false
                    break
                }
                if (curLetter >= 'a') {
                    exLetterA = exLetterB
                    exLetterB = curLetter
                }
                if (curLetter == '-') {
                    if (tiretCount >= 6) {
                        isValid = false
                        break
                    } else {
                        tiretCount++
                    }
                }
            }
        }
        //Si le nom est invalide
        if (!isValid) {
            SocketManager.GAME_SEND_NAME_ALREADY_EXIST(this)
            return
        }
        if (this.account.getPlayers().size >= 5) {
            SocketManager.GAME_SEND_CREATE_PERSO_FULL(this)
            return
        }
        if (this.account.createPlayer(infos[0], (infos[2]).toInt(), (infos[1]).toInt(), (infos[3]).toInt(), (infos[4]).toInt(), (infos[5]).toInt())) {
            SocketManager.GAME_SEND_CREATE_OK(this)
            SocketManager.GAME_SEND_PERSO_LIST(this, this.account.getPlayers(), this.account.getSubscribeRemaining())
        } else {
            SocketManager.GAME_SEND_CREATE_FAILED(this)
        }
    }

    private fun boost(packet: String) {
        try {
            if (this.player.morphMode) {
                this.player.sendMessage(this.player.getLang().trans("game.gameclient.boost.incarne"))
                return
            }

            var packetSplit: List<String> = packet.split(Pattern.quote("|"))
            var isMultiple: Boolean = packetSplit.size > 1
            if(isMultiple){
                var quantity: Int = (packetSplit[1]).toInt()
                var stat: Int = (packetSplit[0].substring(2)).toInt()
                this.player.boostStatFixedCount(stat, quantity)
            } else {
                var stat: Int = (packet.substring(2).split("/u000A")[0]).toInt()
                this.player.boostStat(stat, true)
            }
        } catch (e: NumberFormatException) {
            log.error("unexpected error", e)
        }
    }

    private fun deleteCharacter(packet: String) {
        var split: List<String> = packet.substring(2).split("|")
        var GUID: Int = (split[0]).toInt()
        var answer: String =if (split.size > 1) split[1] else ""
        if (this.account.getPlayers().containsKey(GUID) && !this.account.getPlayers()[GUID]!!.isOnline) {
            if (this.account.getPlayers()[GUID]!!.level < 20 || (this.account.getPlayers()[GUID]!!.level >= 20 && answer.equals(this.account.answer.replace(" ", "%20")))) {
                this.account.deletePlayer(GUID)
                SocketManager.GAME_SEND_PERSO_LIST(this, this.account.getPlayers(), this.account.getSubscribeRemaining())
            } else {
                SocketManager.GAME_SEND_DELETE_PERSO_FAILED(this)
            }
        } else {
            SocketManager.GAME_SEND_DELETE_PERSO_FAILED(this)
        }
    }

    private fun getQueuePosition() {
        SocketManager.MULTI_SEND_Af_PACKET(this, 1, 1, 1, 1)
        //SocketManager.MULTI_SEND_Af_PACKET(this, this.queuePlace.getPlace(), QueueThreadPool.executor.getQueue().size, 0, 1);
    }

    private fun getGifts(packet: String) {
        for(lang in  LangEnum.values()) {
            if (lang.flag.equals(packet.lowercase())) {
                this.language = lang
                break
            }
        }
        if(this.language == null) this.language = LangEnum.ENGLISH

        var gifts: String? = DatabaseManager.get(GiftData::class.java).load(this.account.id)!!.second
        if (gifts == null)
            return
        if (!gifts.isEmpty()) {
            var data: String = ""
            var item: Int = -1
            for (gift in  gifts.splitJ(";")) {
                if(gift.isEmpty()) continue
                var id: Int = (gift.split(",")[0]).toInt()
                var qua: Int = (gift.split(",")[1]).toInt()
                var template: ObjectTemplate? = World.world.getObjTemplate(id)
                if(template != null) {
                    if (data.isEmpty()) {
                        data = "1~" + Integer.toString(id, 16) + "~" + Integer.toString(qua, 16) + "~~" + template.strTemplate
                    } else {
                        data += ";1~" + Integer.toString(id, 16) + "~" + Integer.toString(qua, 16) + "~~" + template.strTemplate
                    }
                    if (item == -1) item = id
                } else {
                    log.error("ERROR BOUTIQUE TEMPLATE OBJECT NOT FOUND : " + id)
                }
            }
            SocketManager.GAME_SEND_Ag_PACKET(this, item, data)
        }
    }

    private fun attributeGiftToCharacter(packet: String) {
        var infos: List<String> = packet.split("|")

        var template: Int = (infos[0]).toInt()
        var player: Player? = World.world.getPlayer((infos[1]).toInt()!!)

        if (player == null)
            return

        var gifts: String = DatabaseManager.get(GiftData::class.java).load(this.account.id)!!.getSecond()!!

        if (gifts.isEmpty())
            return

        for (data in  gifts.splitJ(";")) {
            if(data.isEmpty()) continue
            var split: List<String> = data.split(",")
            var id: Int = (split[0]).toInt()

            if (id == template) {
                var qua: Int = (split[1]).toInt()
                var jp: Int = (split[2]).toInt()
                lateinit var obj: GameObject

                var objNeedAttach: List<Int> = listOf(26001, 26002, 26003, 26004, 26005)
                if (qua == 1) {
                    obj = World.world.getObjTemplate(template)!!.createNewItem(qua, (jp == 1))!!
                    if (objNeedAttach.contains(obj.template!!.id))
                        obj.attachToPlayer(player!!)
                    if (player!!.addItem(obj, true, false))
                        World.world.addGameObject(obj)
                    if(obj.template!!.type == Constant.ITEM_TYPE_CERTIF_MONTURE)
                        obj.setMountStats(player!!, null, true).setToMax()
                    var str1: String = id.toString() + "," + qua.toString() + "," + jp.toString()
                    var str2: String = id.toString() + "," + qua.toString() + "," + jp.toString() + ";"
                    var str3: String = ";" + id.toString() + "," + qua.toString() + "," + jp.toString()

                    gifts = gifts.replace(str2, "").replace(str3, "").replace(str1, "")
                } else {
                    obj = World.world.getObjTemplate(template)!!.createNewItem(1, (jp == 1))!!
                    if (objNeedAttach.contains(obj.template!!.id))
                        obj.attachToPlayer(player!!)
                    if (player!!.addItem(obj, true, false))
                        World.world.addGameObject(obj)
                    if(obj.template!!.type == Constant.ITEM_TYPE_CERTIF_MONTURE)
                        obj.setMountStats(player!!, null, true).setToMax()

                    var str1: String = id.toString() + "," + qua.toString() + "," + jp.toString()
                    var str2: String = id.toString() + "," + qua.toString() + "," + jp.toString() + ";"
                    var str3: String = ";" + id.toString() + "," + qua.toString() + "," + jp.toString()
                    var cstr1: String = id.toString() + "," + (qua - 1).toString() + "," + jp.toString()
                    var cstr2: String = id.toString() + "," + (qua - 1).toString() + "," + jp.toString() + ";"
                    var cstr3: String = ";" + id.toString() + "," + (qua - 1).toString() + "," + jp.toString()

                    gifts = gifts.replace(str2, cstr2).replace(str3, cstr3).replace(str1, cstr1)
                }
                DatabaseManager.get(GiftData::class.java).update(Pair(this.account, gifts))
            }
        }

        DatabaseManager.get(PlayerData::class.java).update(player)

        if (gifts.isEmpty())
            player.send("AG")
        else {
            this.getGifts("")
            player.send("AG")
        }
    }

    private fun sendIdentity(packet: String) {}

    private fun getCharacters() {
        this.account.gameClient = this
        for (player in  this.account.getPlayers().values) {
            if (player != null)
                if (player.fight != null && player.fight!!.getFighterByPerso(player) != null) {
                    this.player = player
                    this.player.OnJoinGame()
                    return
                }
        }

        SocketManager.GAME_SEND_PERSO_LIST(this, this.account.getPlayers(), this.account.getSubscribeRemaining())
    }

    private fun hardcodeRevive(id: Int) {
        val player: Player? = this.account.getPlayers()[id]

        this.getSession().write("BN")

        if(player != null) {
            player.revive()
            SocketManager.GAME_SEND_PERSO_LIST(this, this.account.getPlayers(), this.account.getSubscribeRemaining())
        } else {
            this.getSession().write("BN")
        }
    }

    private fun setCharacter(packet: String) {
        var id: Int = (packet.substring(2)).toInt()

        if (this.account.getPlayers()[id] != null) {
            this.player = this.account.getPlayers()[id]!!
            if (this::player.isInitialized) {
                if(this.player.isDead().toInt() == 1 && Config.modeHeroic)
                    this.getSession().write("BN")
                else
                    this.player.OnJoinGame()
                return
            }
        }
        SocketManager.GAME_SEND_PERSO_SELECTION_FAILED(this)
    }

    private fun parseTicket(packet: String) {
        try {
            var id: Int = (packet.substring(2)).toInt()
            val acc: Account? = Config.gameServer!!.getWaitingAccount(id)

            if (acc == null) {
                SocketManager.GAME_SEND_ATTRIBUTE_FAILED(this)
                this.kick()
            } else {
                this.account = acc
                Config.gameServer!!.deleteWaitingAccount(this.account)

                var ip: String = this.session.getRemoteAddress().toString().substring(1).split(":")[0]
                var fight: Fight? = null
                for(p in  this.account.getPlayers().values) {
                    fight = p.fight
                    if (fight != null) break
                }
                if(fight == null && Config.limitByIp != -1 && ArrayList(World.world.onlinePlayers).stream().filter({ p -> p != null &&
                        p.account != null && p.account!!.currentIp.equals(ip, ignoreCase = true) })
                        .count() >= Config.limitByIp) {
                    this.send("M034|" + Config.limitByIp + ";" + ip)
                    this.kick()
                    return
                }

                if(this.account.gameClient != null)
                    this.account.gameClient!!.kick()

                this.account.gameClient = this
                this.account.currentIp = ip
                DatabaseManager.get(AccountData::class.java).setLogged(this.account.id, 1)

                if (Logging.USE_LOG) Logging.getInstance().write("AccountIpConnect", this.account.name + " > " + ip)

                if(Config.encryption){
                    //String key = generateKey();
                    this.getSession().write("ATK18fd8ad4a38cdd0432248a76f8f148ceb")
                    this.preparedKeys = CryptManager.prepareKey("8fd8ad4a38cdd0432248a76f8f148ceb")
                } else {
                    this.getSession().write("ATK0")
                }
            }
        } catch (e: Exception) {
            log.error("unexpected error", e)
            SocketManager.GAME_SEND_ATTRIBUTE_FAILED(this)
            this.kick()
        }
    }

    private fun requestRegionalVersion() {
        SocketManager.GAME_SEND_AV0(this)
    }
    /** Fin Account Packet **/

    /**
     * Basics Packet *
     */
    private fun parseBasicsPacket(packet: String)   {
        when (packet[1]) {
'A' -> {authorisedCommand(packet)
                
}
'D' -> {getDate()
                
}
'M' -> {tchat(packet)
                
}
'W' -> {whoIs(packet)
                
}
'S' -> {this.player.useSmiley(packet.substring(2))
                
}
'Y' -> {chooseState(packet)
                
}
'a' -> {if (packet[2] == 'M')
                    goToMap(packet)
                
}
}
    }

    private fun authorisedCommand(packet: String) {
        if (!::adminUser.isInitialized) this.adminUser = CommandAdmin(this.player)
        if (this.player.getGroup() == null || !this::player.isInitialized) {
            this.getAccount()!!.gameClient!!.kick()
            return
        }

        if (Logging.USE_LOG)
            Logging.getInstance().write("CommandAdmin", this.getAccount()!!.currentIp + " : " + this.getAccount()!!.name + " > " + this.player.name + " > " + packet.substring(2))

        this.adminUser.apply(packet)
    }

    private fun getDate() {
        var calendar: Calendar = GregorianCalendar()
        calendar.setTime(Date())

        this.send("BD" + calendar[Calendar.YEAR] + "|" + calendar[Calendar.MONTH] + "|" + calendar[Calendar.DAY_OF_MONTH])
        this.send("BT" + (calendar.getTime().getTime() + 3600000))
    }

    private fun tchat(packet: String) {
        var packet = packet
        lateinit var msg: String
        var lastMsg: String = ""

        if (this.player.getAccount() != null && this.player.isMuted()) {
            var remaining: Short = (((this.getAccount()!!.getMuteTime() - System.currentTimeMillis()) / 60000).toShort())
            this.player.send("Im117;" + this.getAccount()!!.getMutePseudo() + "~" + remaining)
            return
        }

        if (this.player.hasMap()) {
            if (this.player.curMap.isMute && this.player.getGroup() == null) {
                this.player.sendServerMessage("The map is currently mute.")
                return
            }
        }

        packet = packet.replace("<", "")
        packet = packet.replace(">", "")
        if (packet.length < 6)
            return

        when (packet[2]) {
'¤' -> {
}
'*' -> {if (System.currentTimeMillis() - timeLastChatMsg < 500) {
                    this.send("M10")
                    return
                }
                timeLastChatMsg = System.currentTimeMillis()
                if (!this.player.canaux.contains(packet[2] + ""))
                    return

                msg = packet.split("|", limit = 2)[1]
                if (CommandPlayer.analyse(this.player, msg)) {
                    this.player.send("BN")
                    return
                }
                if (msg.equals(lastMsg)) {
                    SocketManager.GAME_SEND_Im_PACKET(this.player, "184")
                    return
                }
                if (this.player.spec && this.player.fight != null) {
                    var team: Int = this.player.fight!!.getTeamId(this.player.id)
                    if (team == -1)
                        return
                    SocketManager.GAME_SEND_cMK_PACKET_TO_FIGHT(this.player.fight!!, team, "#", this.player.id, this.player.name, msg)
                    return
                }
                if (Logging.USE_LOG)
                    Logging.getInstance().write("DefaultMessage", this.player.name + " > Map " + this.player.curMap.id + " > " + msg)
                if (this.player.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF) != null)
                    if (this.player.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF)!!.template!!.id == 10844)
                        msg = Formulas.translateMsg(msg)
                if (this.player.fight == null) {
                    SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(this.player.curMap, "", this.player.id, this.player.name, msg)
                    // Broken: AuctionManager.getInstance().onPlayerChat(this.player, msg);
                } else
                    SocketManager.GAME_SEND_cMK_PACKET_TO_FIGHT(this.player.fight!!, 7, "", this.player.id, this.player.name, msg)
                
}
'^' -> {msg = packet.split("|", limit = 2)[1]
                var x: Long = System.currentTimeMillis() - timeLastIncarnamMsg
                if (x < 30000) {
                    x = (30000 - x) / 1000;//Chat antiflood
                    SocketManager.GAME_SEND_Im_PACKET(this.player, "0115;" + (Math.ceil(x.toDouble()).toInt() + 1))
                    return
                }
                if (msg.equals(lastMsg)) {
                    SocketManager.GAME_SEND_Im_PACKET(this.player, "184")
                    return
                }

                timeLastIncarnamMsg = System.currentTimeMillis()
                msg = packet.split("|", limit = 2)[1]
                lastMsg = msg
                if (this.player.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF) != null)
                    if (this.player.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF)!!.template!!.id == 10844)
                        msg = Formulas.translateMsg(msg)
                SocketManager.GAME_SEND_cMK_PACKET_INCARNAM_CHAT("^", this.player.id, this.player.name, msg)

                
}
'#' -> {if (!this.player.canaux.contains(packet[2] + ""))
                    return
                if (this.player.fight != null) {
                    msg = packet.split("|", limit = 2)[1]
                    var team: Int = this.player.fight!!.getTeamId(this.player.id)
                    if (team == -1)
                        return
                    if (Logging.USE_LOG)
                        Logging.getInstance().write("TeamMessage", this.player.name + " > " + this.player.fight + " > " + msg)
                    if (this.player.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF) != null)
                        if (this.player.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF)!!.template!!.id == 10844)
                            msg = Formulas.translateMsg(msg)
                    SocketManager.GAME_SEND_cMK_PACKET_TO_FIGHT(this.player.fight!!, team, "#", this.player.id, this.player.name, msg)
                }
                
}
'$' -> {if (!this.player.canaux.contains(packet[2] + ""))
                    return
                if (this.player.party == null)
                    return
                msg = packet.split("|", limit = 2)[1]
                if (Logging.USE_LOG)
                    Logging.getInstance().write("PartyMessage", this.player.name + " > " + this.player.party + " > " + msg)
                if (this.player.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF) != null)
                    if (this.player.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF)!!.template!!.id == 10844)
                        msg = Formulas.translateMsg(msg)
                SocketManager.GAME_SEND_cMK_PACKET_TO_GROUP(this.player.party!!, "$", this.player.id, this.player.name, msg)
                
}
':' -> {if (!this.player.canaux.contains(packet[2] + ""))
                    return
                var l: Long
                if (this.player.isMissingSubscription()) {
                    SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this.player.getGameClient()!!, 'S')
                    return
                }
                if (this.player.cantCanal()) {
                    SocketManager.GAME_SEND_MESSAGE(this.player, "Vous n'avez pas la permission de parler dans ce canal !", "B9121B")
                } else if (this.player.isInPrison()) {
                    SocketManager.GAME_SEND_MESSAGE(this.player, "Vous ?tes en prison, impossible de parler dans ce canal !", "B9121B")
                } else {
                    if (this.player.getGroup() == null) {
                        l = System.currentTimeMillis() - timeLastTradeMsg
if (l < 50000) {
                            l = (50000 - l) / 1000;//On calcul la diff?rence en secondes
                            SocketManager.GAME_SEND_Im_PACKET(this.player, "0115;" + (Math.ceil(l.toDouble()).toInt() + 1))
                            return
                        }
                    }
                    timeLastTradeMsg = System.currentTimeMillis()
                    msg = packet.split("|", limit = 2)[1]
                    if (Logging.USE_LOG)
                        Logging.getInstance().write("TradeMessage", this.player.name + " > " + msg)
                    if (this.player.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF) != null)
                        if (this.player.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF)!!.template!!.id == 10844)
                            msg = Formulas.translateMsg(msg)
                    SocketManager.GAME_SEND_cMK_PACKET_TO_ALL(this.player, ":", this.player.id, this.player.name, msg)
                }
                
}
'@' -> {if (this.player.getGroup() == null)
                    return
                msg = packet.split("|", limit = 2)[1]
                if (Logging.USE_LOG)
                    Logging.getInstance().write("AdminMessage", this.player.name + " > " + msg)
                SocketManager.GAME_SEND_cMK_PACKET_TO_ADMIN("@", this.player.id, this.player.name, msg)
                
}
'?' -> {if (!this.player.canaux.contains(packet[2] + ""))
                    return
                if (this.player.isMissingSubscription()) {
                    SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this.player.getGameClient()!!, 'S')
                    return
                }
                var j: Long
                if (this.player.cantCanal()) {
                    SocketManager.GAME_SEND_MESSAGE(this.player, "Vous n'avez pas la permission de parler dans ce canal !", "B9121B")
                } else if (this.player.isInPrison()) {
                    SocketManager.GAME_SEND_MESSAGE(this.player, "Vous ?tes en prison, impossible de parler dans ce canal !", "B9121B")
                } else {
                    if (this.player.getGroup() == null) {
                        j = System.currentTimeMillis()
                                - timeLastRecrutmentMsg
if (j < 40000) {
                            j = (40000 - j) / 1000;//On calcul la diff?rence en secondes
                            SocketManager.GAME_SEND_Im_PACKET(this.player, "0115;" + (Math.ceil(j.toDouble()).toInt() + 1))
                            return
                        }
                    }
                    timeLastRecrutmentMsg = System.currentTimeMillis()
                    msg = packet.split("|", limit = 2)[1]
                    if (Logging.USE_LOG)
                        Logging.getInstance().write("RecruitmentMessage", this.player.name + " > " + msg)
                    if (this.player.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF) != null)
                        if (this.player.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF)!!.template!!.id == 10844)
                            msg = Formulas.translateMsg(msg)
                    SocketManager.GAME_SEND_cMK_PACKET_TO_ALL(this.player, "?", this.player.id, this.player.name, msg)
                }
                
}
'%' -> {if (!this.player.canaux.contains(packet[2] + ""))
                    return
                if (this.player.getGuild() == null)
                    return
                msg = packet.split("|", limit = 2)[1]
                if (Logging.USE_LOG)
                    Logging.getInstance().write("GuildMessage", this.player.name + " > " + this.player.getGuild()!!.name + " > " + msg)
                if (this.player.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF) != null)
                    if (this.player.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF)!!.template!!.id == 10844)
                        msg = Formulas.translateMsg(msg)
                SocketManager.GAME_SEND_cMK_PACKET_TO_GUILD(this.player.getGuild()!!, "%", this.player.id, this.player.name, msg)
                
}
'!' -> {if (!this.player.canaux.contains(packet[2] + ""))
                    return
                if (this.player.isMissingSubscription()) {
                    SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this.player.getGameClient()!!, 'S')
                    return
                }
                if (this.player.alignment == 0)
                    return
                if (this.player.deshonor >= 1) {
                    SocketManager.GAME_SEND_Im_PACKET(this.player, "183")
                    return
                }
                var k: Long
                k = System.currentTimeMillis() - timeLastAlignMsg
if (k < 30000) {
                    k = (30000 - k) / 1000;//On calcul la diff?rence en secondes
                    SocketManager.GAME_SEND_Im_PACKET(this.player, "0115;" + (Math.ceil(k.toDouble()).toInt() + 1))
                    return
                }
                timeLastAlignMsg = System.currentTimeMillis()
                msg = packet.split("|", limit = 2)[1]
                if (Logging.USE_LOG)
                    Logging.getInstance().write("AlignMessage", this.player.name + " > " + msg)
                if (this.player.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF) != null)
                    if (this.player.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF)!!.template!!.id == 10844)
                        msg = Formulas.translateMsg(msg)
                SocketManager.GAME_SEND_cMK_PACKET_TO_ALIGN("!", this.player.id, this.player.name, msg, this.player)
                
}
else -> {var nom: String = packet.substring(2).split("|")[0]
                msg = packet.split("|", limit = 2)[1]
                if (!(nom.length <= 1)) {
                    var target: Player? = World.world.getPlayerByName(nom)
                    if (target == null || target.getAccount() == null || target.getGameClient() == null) {
                        SocketManager.GAME_SEND_CHAT_ERROR_PACKET(this, nom)
                        return
                    }
                    if (target.getAccount()!!.isEnemyWith(this.player.getAccount()!!.id) || !target.isDispo(this.player)) {
                        SocketManager.GAME_SEND_Im_PACKET(this.player, "114;" + target.name)
                        return
                    }
                    if (msg.equals(lastMsg)) {
                        SocketManager.GAME_SEND_Im_PACKET(this.player, "184")
                        return
                    }
                    if (this.player.getGroup() == null && target.isInvisible) {
                        SocketManager.GAME_SEND_CHAT_ERROR_PACKET(this, nom)
                        return
                    }
                    if (target.mpToTp) {
                        if (this.player.fight != null)
                            return
                        this.player.thatMap = this.player.curMap.id
                        this.player.thatCell = this.player.curCell.getId()
                        this.player.teleport(target.curMap.id, target.curCell.getId())
                        return
                    }

                    if (Logging.USE_LOG)
                        Logging.getInstance().write("PrivateMessage", this.player.name + " ? " + target.name + " > " + msg)
                    if (this.player.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF) != null)
                        if (this.player.getObjetByPos(Constant.ITEM_POS_ROLEPLAY_BUFF)!!.template!!.id == 10844)
                            msg = Formulas.translateMsg(msg)

                    SocketManager.GAME_SEND_cMK_PACKET(this.player, "T", target.id, target.name, msg)
                    SocketManager.GAME_SEND_cMK_PACKET(target, "F", this.player.id, this.player.name, msg)

                    if(target.getAccount()!!.isMuted())
                        this.send("Im0168;" + target.name + "~" + target.getAccount()!!.getMuteTime())
                }
                
}
}
    }

    private
        fun whoIs(packet: String) {
        var packet = packet
        packet = packet.substring(2)
        var player: Player? = World.world.getPlayerByName(packet)
        if (player == null) {
            if (packet.isEmpty())
                SocketManager.GAME_SEND_BWK(this.player, this.player.getAccount()!!.pseudo + "|1|" + this.player.name + "|" + (if (this.player.curMap.subArea != null) this.player.curMap.subArea!!.area!!.id else "-1"))
            else
                this.player.send("PIEn" + packet)

        } else {
            if (!player.isOnline) {
                this.player.send("PIEn" + player.name)
                return
            }
            if (this.player.getAccount()!!.isFriendWith(player.id))
                SocketManager.GAME_SEND_BWK(this.player, player.getAccount()!!.pseudo + "|1|" + player.name + "|" + (if (player.curMap.subArea != null) player.curMap.subArea!!.area!!.id else "-1"))
            else if (player == this.player)
                SocketManager.GAME_SEND_BWK(this.player, this.player.getAccount()!!.pseudo + "|1|" + this.player.name + "|" + (if (this.player.curMap.subArea != null) this.player.curMap.subArea!!.area!!.id else "-1"))
            else
                SocketManager.GAME_SEND_BWK(this.player, player.getAccount()!!.pseudo + "|1|" + player.name + "|-1")
        }
    }

    private fun chooseState(packet: String) {
        when (packet[2]) {
'A' -> {if (this.player.isAbsent) {
                    SocketManager.GAME_SEND_Im_PACKET(this.player, "038")
                    this.player.isAbsent = false
                } else {
                    SocketManager.GAME_SEND_Im_PACKET(this.player, "037")
                    this.player.isAbsent = true
                }
                
}
'I' -> {if (this.player.isInvisible) {
                    SocketManager.GAME_SEND_Im_PACKET(this.player, "051")
                    this.player.isInvisible = false
                } else {
                    SocketManager.GAME_SEND_Im_PACKET(this.player, "050")
                    this.player.isInvisible = true
                }
                
}
}
    }

    // T?l?portation de MJ
    private fun goToMap(packet: String) {
        if (this.player.getGroup() == null)
            return
        if (this.player.getGroup()!!.isPlayer)
            return

        var datas: String = packet.substring(3)
        if (datas.isEmpty())
            return
        var MapX: Int = (datas.split(",")[0]).toInt()
        var MapY: Int = (datas.split(",")[1]).toInt()
        var sa: Int = Optional.ofNullable(this.player.curMap).map { it.area }.map { it!!.superArea }.orElse(-1)

        var i: List<Int> = World.world.getMapIdByPosInSuperArea(MapX, MapY, sa)

        if (i.isEmpty())
            return

        var map: GameMap = World.world.getMap(i.get(Formulas.getRandomValue(0, i.size - 1)))
        if (map == null)
            return
        var CellId: Int = map.randomFreeCellId
        if (map.getCase(CellId) == null)
            return
        if (this.player.fight != null)
            return

        this.player.teleport(map.id, CellId)
    }

    /** Fin Basics Packet **/

    /**
     * Conquest Packet *
     */
    private fun parseConquestPacket(packet: String) {
        when (packet[1]) {
'b' -> {requestBalance()
                
}
'B' -> {getAlignedBonus()
                
}
'W' -> {worldInfos(packet)
                
}
'I' -> {prismInfos(packet)
                
}
'F' -> {prismFight(packet)
                
}
}
    }

    fun requestBalance() {
        var map: GameMap = this.player.curMap
        if(map != null && map.subArea != null) {
            SocketManager.SEND_Cb_BALANCE_CONQUETE(this.player, World.world.getBalanceWorld(this.player.alignment).toString() + ";" + World.world.getBalanceArea(map.subArea!!.area!!, this.player.alignment))
        }
    }

    fun getAlignedBonus() {
        var porc: Double = World.world.getBalanceWorld(this.player.alignment)
        var porcN: Double = Math.rint((this.player.getGrade() / 2.5) + 1)
        SocketManager.SEND_CB_BONUS_CONQUETE(this.player, porc.toString() + "," + porc.toString() + "," + porc.toString() + ";" + porcN.toString() + "," + porcN.toString() + "," + porcN.toString() + ";" + porc.toString() + "," + porc.toString() + "," + porc)
    }

    private fun worldInfos(packet: String) {
        when (packet[2]) {
'J' -> {SocketManager.SEND_CW_INFO_WORLD_CONQUETE(this.player, World.world.PrismesGeoposition(player, 1))
                SocketManager.SEND_CW_INFO_WORLD_CONQUETE(this.player, World.world.PrismesGeoposition(player, 2))
                
}
'V' -> {SocketManager.SEND_CW_INFO_WORLD_CONQUETE(this.player, World.world.PrismesGeoposition(player, 1))
                SocketManager.SEND_CW_INFO_WORLD_CONQUETE(this.player, World.world.PrismesGeoposition(player, 2))
                
}
}
    }

    private fun prismInfos(packet: String) {
        if (packet[2] == 'J' || packet[2] == 'V') {
            when (packet[2]) {
'J' -> {if(this.player.hasMap() && this.player.curMap.subArea != null) {
                        var prism: Prism? = this.player.curMap.subArea!!.prism
                        if (prism != null) {
                            Prism.parseAttack(this.player)
                            Prism.parseDefense(this.player)
                        }
                        SocketManager.SEND_CIJ_INFO_JOIN_PRISME(this.player, this.player.parsePrism())
                    }
                    
}
}
        }
    }

    private fun prismFight(packet: String) {
        when (packet[2]) {
'J' -> {if (this.player.isInPrison())
                    return

                val prism: Prism? = this.player.curMap.subArea!!.prism

                if (prism == null)
                    return

                var FightID: Int = -1
                var cellID: Int = -1
                var MapID: Int = -1
                try {
                    FightID = prism.fight!!.id
                    MapID = prism.map
                    cellID = prism.cell
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                }

                if (FightID == -1 || MapID == -1 || cellID == -1)
                    return
                if (this.player.fight != null  || prism.alignment != this.player.alignment ||this.player.isDead().toInt() == 1 || World.world.getMap(MapID) == null){
                    SocketManager.GAME_SEND_BN(this.player)
                    return
                }

                val map: Int = MapID
                val cell: Int = cellID
                val fight: Fight? = World.world.getMap(map).getFight(FightID)

                if(fight == null) {
                    SocketManager.GAME_SEND_BN(this.player)
                    return
                }

                if (this.player.curMap.id != MapID) {
                    this.player.curMap = this.player.curMap
                    this.player.curCell = this.player.curCell
                    this.player.teleport(map, cell)
                }

                TimerWaiter.addNext({
                    fight!!.joinPrismFight(this.player, (if (fight.init0.prism != null) fight.init0 else fight.init1).team)
                    World.world.onlinePlayers.stream().filter(Objects::nonNull).filter({ player -> player.alignment == player.alignment }).forEach(Prism::parseDefense)
                }, 2, TimeUnit.SECONDS)
                
}
}
    }

    /** Fin Conquest Packet **/

    /**
     * Chat Packet *
     */
    private fun parseChanelPacket(packet: String) {
        when (packet[1]) {
'C' -> {subscribeChannels(packet)
                
}
}
    }

    private fun subscribeChannels(packet: String) {
        var chan: String = packet[3] + ""
        when (packet[2]) {
'+' -> {this.player.addChanel(chan)
                
}
'-' -> {this.player.removeChanel(chan)
                
}
}
        DatabaseManager.get(PlayerData::class.java).update(this.player)
    }

    /** Fin Chat Packet **/

    /**
     * Dialog Packet *
     */
    private fun parseDialogPacket(packet: String) {
        when (packet[1]) {
'C' -> {npcCreateDialog(packet)
                
}
'R' -> {npcResponse(packet)
                
}
'V' -> {quitDialog()
                
}
}

        val party: Party? = this.player.party

        if(party != null && this.player.fight == null && party.master != null && party.master!!.name.equals(this.player.name)) {
            TimerWaiter.addNext({ party.players.stream().filter({ follower1 -> party.isWithTheMaster(follower1, false, false) })
                    .forEach({ follower -> follower.gameClient!!.parseDialogPacket(packet) }) }, 0, TimeUnit.SECONDS)
        }
    }

    private fun npcCreateDialog(packet: String) {
        var id: Int = (packet.substring(2).split('\n')[0]).toInt()

        if (player.isMissingSubscription() || player.exchangeAction != null) {
            SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(player.getGameClient()!!, 'S')
            return
        }

        var collector: Collector? = World.world.getCollector(id)

        if (collector != null && collector.map == player.curMap.id) {
            SocketManager.GAME_SEND_DIALOG_CREATE_PACKET(this, id)
            send(World.world.getGuild(collector.guildId)!!.encodeTaxCollectorDQ())
            return
        }

        var npc: Npc? = player.curMap.getNpc(id)

        if (npc != null) {
            npc.onCreateDialog(player)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun npcResponse(packet: String) {
        var action: ExchangeAction<*> = this.player.exchangeAction!!

        if (action == null || action.getType() != ExchangeAction.TALKING_WITH)
            return

        var infos: List<String> = packet.substring(2).split("|")

        var data: NpcDialogActionData = ((this.player.exchangeAction as ExchangeAction<NpcDialogActionData>)).getValue()
        var npc: Npc? = data.getNpc(player)

        if (npc != null && infos.size >= 2) {
            var question: Int = (infos[0]).toInt()
            var answer: Int = (infos[1]).toInt()

            if(data.questionId == question && data.hasAnswer(answer)) {
                npc.template.onDialog(this.player, question, answer)
                return
            }
        }

        this.player.exchangeAction = null
        SocketManager.GAME_SEND_END_DIALOG_PACKET(this)
    }

    private fun quitDialog() {
        var action: ExchangeAction<*> = this.player.exchangeAction!!

        if (action == null || action.getType() != ExchangeAction.TALKING_WITH)
            return

        this.walk = false
        this.player.away = false
        this.player.exchangeAction = null
        SocketManager.GAME_SEND_END_DIALOG_PACKET(this)
    }

    /** Fin Dialog Packet **/

    /**
     * Document Packet *
     */
    private fun parseDocumentPacket(packet: String) {
        when (packet[1]) {
'V' -> {if(player.exchangeAction != null
                && player.exchangeAction!!.getType() != ExchangeAction.READING_DOCUMENT) {
                    player.exchangeAction = null
                }

                SocketManager.GAME_SEND_DOCUMENT_CLOSE_PACKET(this)
                
}
}
    }

    /** Fin Document Packet **/

    /**
     * Exchange Packet *
     */
    private @Synchronized fun parseExchangePacket(packet: String) {
        if (this.player.isDead().toInt() == 1)
            return
        when (packet[1]) {
'A' -> {accept()
                
}
'B' -> {buy(packet)
                
}
'H' -> {bigStore(packet)
                
}
'K' -> {ready()
                
}
'L' -> {replayCraft()
                
}
'M' -> {movementItemOrKamas(packet)
                
}
'P' -> {movementItemOrKamasDons(packet.substring(2))
                
}
'q' -> {askOfflineExchange()
                
}
'Q' -> {offlineExchange()
                
}
'r' -> {putInInventory(packet)
                
}
'f' -> {putInMountPark(packet)
                
}
'R' -> {request(packet)
                
}
'S' -> {sell(packet)
                
}
'J' -> {bookOfArtisant(packet)
                
}
'W' -> {setPublicMode(packet)
                
}
'V' -> {leaveExchange(this.player)
                
}
}
    }

    @Suppress("UNCHECKED_CAST")
    private fun accept() {
        var checkExchangeAction: ExchangeAction<*>? = this.player.exchangeAction

        if (Main.tradeAsBlocked ||this.player.isDead().toInt() == 1 || checkExchangeAction == null || !(checkExchangeAction.getValue() is Int) || (checkExchangeAction.getType() != ExchangeAction.TRADING_WITH_PLAYER && checkExchangeAction.getType() != ExchangeAction.CRAFTING_SECURE_WITH))
            return

        var exchangeAction: ExchangeAction<Int> = (this.player.exchangeAction as ExchangeAction<Int>)
        var target: Player? = World.world.getPlayer(exchangeAction.getValue())
        if(target == null) return

        checkExchangeAction = target.exchangeAction

        if (target.isDead().toInt() == 1 || checkExchangeAction == null || !(checkExchangeAction.getValue() is Int) || (checkExchangeAction.getType() != ExchangeAction.TRADING_WITH_PLAYER && checkExchangeAction.getType() != ExchangeAction.CRAFTING_SECURE_WITH))
            return

        var type: Int = this.player.craftingType[0]
        var newExchangeAction: ExchangeAction<*>? = null
        when (type){  1 -> {SocketManager.GAME_SEND_EXCHANGE_CONFIRM_OK(this, 1)
                SocketManager.GAME_SEND_EXCHANGE_CONFIRM_OK(target.getGameClient()!!, 1)
                var exchange: PlayerExchange = PlayerExchange(target, this.player)
                newExchangeAction = ExchangeAction<Any?>(ExchangeAction.TRADING_WITH_PLAYER, exchange)
                this.player.exchangeAction = newExchangeAction
                target.exchangeAction = newExchangeAction
                this.player.craftingType.clear()
                target.craftingType.clear()
                
}
12, 13 -> {var player1: Player = (if (target.craftingType[0] == 12) target else this.player)
                var player2: Player = (if (target.craftingType[0] == 13) target else this.player)

                var craftSecure: CraftSecure = CraftSecure(player1, player2)
                SocketManager.GAME_SEND_ECK_PACKET(this, type, craftSecure.maxCase.toString() + ";" + this.player.craftingType[1])
                SocketManager.GAME_SEND_ECK_PACKET(target.getGameClient()!!, target.craftingType[0], craftSecure.maxCase.toString() + ";" + this.player.craftingType[1])

                newExchangeAction = ExchangeAction(ExchangeAction.CRAFTING_SECURE_WITH, craftSecure)
                this.player.exchangeAction = newExchangeAction
                target.exchangeAction = newExchangeAction
                
}
}
    }

    @Suppress("UNCHECKED_CAST")
    private fun buy(packet: String) {
        var infos: List<String> = packet.substring(2).split("|")

        var checkExchangeAction: ExchangeAction<*>? = this.player.exchangeAction
        if(checkExchangeAction == null || !(checkExchangeAction.getValue() is Int) || (checkExchangeAction.getType() != ExchangeAction.TRADING_WITH_OFFLINE_PLAYER && checkExchangeAction.getType() != ExchangeAction.TRADING_WITH_NPC)) return

        var exchangeAction: ExchangeAction<Int> = (this.player.exchangeAction as ExchangeAction<Int>)

        if (exchangeAction.getType() == ExchangeAction.TRADING_WITH_OFFLINE_PLAYER) {
            var seller: Player? = World.world.getPlayer(exchangeAction.getValue())
            if (seller != null && seller != this.player) {
                var itemID: Int = 0
                var qua: Int = 0
                var price: Int = 0
                try {
                    itemID = (infos[0]).toInt()
                    qua = (infos[1]).toInt()
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    return
                }

                    if (itemID !in seller.storeItems || qua <= 0) {
                        SocketManager.GAME_SEND_BUY_ERROR_PACKET(this)
                        return
                    }
                    price = seller.storeItems[itemID]!! * qua
                    var price2: Int = seller.storeItems[itemID]!!
                    var itemStore: GameObject? = World.world.getGameObject(itemID)
                    if (itemStore == null)
                        return
                    if (price > this.player.kamas)
                        return
                    if (qua <= 0 || qua > 100000)
                        return
                    if (qua > itemStore.quantity)
                        qua = itemStore.quantity
                    if (qua == itemStore.quantity) {
                        seller.storeItems.remove(itemStore.guid)
                        this.player.addItem(itemStore, true, false)
                    } else if (itemStore.quantity > qua) {
                        seller.storeItems.remove(itemStore.guid)
                        itemStore.quantity = itemStore.quantity - qua
                        seller.addStoreItem(itemStore.guid, price2)

                        var clone: GameObject = itemStore.getClone(qua, true)!!
                        if (this.player.addItem(clone, true, false))
                            World.world.addGameObject(clone)
                    } else {
                        SocketManager.GAME_SEND_BUY_ERROR_PACKET(this)
                        return
                    }

                    //remove kamas
                    this.player.addKamas(-price.toLong())
                    //add seller kamas
                    seller.addKamas(price.toLong())
                    DatabaseManager.get(PlayerData::class.java).update(seller)
                    //send packets
                    SocketManager.GAME_SEND_STATS_PACKET(this.player)
                    SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(seller, this.player)
                    SocketManager.GAME_SEND_BUY_OK_PACKET(this)
                    if (seller.storeItems.isEmpty()) {
                        if (World.world.getSeller(seller.curMap.id) != null
                                && World.world.getSeller(seller.curMap.id)!!.contains(seller.id)) {
                            World.world.removeSeller(seller.id, seller.curMap.id)
                            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(seller.curMap, seller.id)
                            leaveExchange(this.player)
                        }
                    }
            }
        } else {

            try {
                var id: Int = (infos[0]).toInt()
                var qua: Int = (infos[1]).toInt()

                if (qua <= 0 || qua > 100000)
                    return

                var npc: Npc? = this.player.curMap.getNpc(exchangeAction.getValue())
                if (npc == null) return
                var npcTemplate: NpcTemplate = npc.template

                var optOffer: Optional<SaleOffer> = npcTemplate.salesList(this.player).stream().filter({ o -> o.itemTemplate.id == id }).findFirst()
                if (!optOffer.isPresent()) {
                    SocketManager.GAME_SEND_BUY_ERROR_PACKET(this)
                    return
                }

                var offer: SaleOffer = optOffer.get()
                if (offer.itemTemplate.type == 18 && qua > 1) {
                    this.player.sendMessage(this.player.getLang().trans("game.gameclient.buy.fami"))
                    return
                }

                var totalPrice: Long = qua * offer.unitPrice


                if(!player.consumeCurrency(offer.currency, totalPrice)) {
                    SocketManager.GAME_SEND_BUY_ERROR_PACKET(this)
                    // FIXME Make a notEnoughCurrency(offer.currency) function to call here
                    return
                }

                player.addItem(offer.itemTemplate, qua,(npcTemplate.flags.toInt() and 0x1) != 0, true)
                SocketManager.GAME_SEND_BUY_OK_PACKET(this)
            } catch (e: Exception) {
                log.error("unexpected error", e)
                SocketManager.GAME_SEND_BUY_ERROR_PACKET(this)
            }
        }
    }

    private fun bigStore(packet: String) {
        if (this.player.fight != null || this.player.away)
            return

        // Check current exchange action
        var exchangeAction: BigStoreActionData = Optional.ofNullable(this.player.exchangeAction).filter({ ea -> ea.getType() == ExchangeAction.AUCTION_HOUSE_BUYING })
                .map { it.getValue() }.map(BigStoreActionData::class.java::cast).orElse(null)
        if(exchangeAction == null) {
            return
        }
        var bigStore: BigStore = World.world.getHdv(exchangeAction.hdvId)!!

        var templateID: Int
        var catContent: List<Int>? = null
        when (packet[2]) {
'B' -> {var info: List<String> = packet.substring(3).split("|")//ligneID|amount|price
                var ligneID: Int = (info[0]).toInt()
                var amount: Int = (info[1]).toInt()
                var price: Int = (info[2]).toInt()

                // Client amount is [1,3], our enum is 0-2
                var lotSize: BigStoreListingLotSize = BigStoreListingLotSize.fromValue(amount-1)!!
                var entry: BigStoreListing = bigStore.buyItem(exchangeAction.categoryId, exchangeAction.templateId, ligneID, lotSize, price, this.player).orElse(null)

                if (entry == null) {
                    SocketManager.GAME_SEND_Im_PACKET(this.player, "172");//Envoie un message d'erreur d'achat
                    return
                }

                var seller: Optional<Account> = Optional.ofNullable(World.world.ensureAccountLoaded(entry.owner))

                var name: String = seller.map { it.name }.orElse("undefined")
                var obj: GameObject = entry.gameObject!!

                try {
                    Logging.getInstance().write("Object", "BuyHdv : " + player.name + " : achat de " + obj.template!!.name + "(" + obj.guid + ") x" + obj.quantity + " venant du compte " + name)
                } catch (ex: Exception) { log.error("unexpected error", ex)
                 }

                seller.map { it.currentPlayer }.ifPresent { p ->
                    SocketManager.GAME_SEND_Im_PACKET(p, "065;" + price + "~" + obj.template!!.id + "~" + obj.template!!.id + "~1")
                    DatabaseManager.get(PlayerData::class.java).update(p)
                }

                var updated: Optional<BigStore.CheapestListings> = bigStore.getCheapestListings(exchangeAction.categoryId, exchangeAction.templateId, ligneID)
                SocketManager.GAME_SEND_EHm_DEL_PACKET(this.player, ligneID)
                // Refresh line if there is still one
                updated.ifPresent({ c -> SocketManager.GAME_SEND_EHm_ADD_PACKET(this.player, c) })
                this.player.refreshStats()
                SocketManager.GAME_SEND_Ow_PACKET(this.player)
                SocketManager.GAME_SEND_Im_PACKET(this.player, "068");//Envoie le message "Lot achet?"

                
}
'l' -> {templateID = (packet.substring(3)).toInt()


                exchangeAction.templateId = templateID
                SocketManager.GAME_SEND_EHl(this.player, bigStore, exchangeAction.categoryId, templateID)

                
}
'P' -> {templateID = (packet.substring(3)).toInt()
                SocketManager.GAME_SEND_EHP_PACKET(this.player, templateID)
                
}
'T' -> {var categ: Int = (packet.substring(3)).toInt()
                catContent = bigStore.getCategoryContent(categ)
                exchangeAction.categoryId = categ
                SocketManager.GAME_SEND_EHL_PACKET(this.player, categ, catContent)
                
}
'S' -> {var infos: List<String> = packet.substring(3).split("|")//type | templateId
                var template: Int = (infos[1]).toInt()
                var category: Int = (infos[0]).toInt()

                catContent = bigStore.getCategoryContent(category)

                if(catContent == null || catContent.isEmpty()) {
                    this.player.send("EHS")
                } else {
                    this.player.send("EHSK")
                    SocketManager.GAME_SEND_EHL_PACKET(this.player, category, catContent)
                    SocketManager.GAME_SEND_EHP_PACKET(this.player, template)
                    SocketManager.GAME_SEND_EHl(this.player, bigStore, category, template)
                }
                
}
}
    }

    private fun ready() {
        if(this.player.exchangeAction == null) return

        var exchangeAction: ExchangeAction<*> = this.player.exchangeAction!!
        var value: Any = exchangeAction.getValue()!!

        if (exchangeAction.getType() == ExchangeAction.CRAFTING && value is JobAction) {
            if (((value as JobAction)).isCraft) {
                ((value as JobAction)).startCraft(this.player)
            }
            return
        }

        if (exchangeAction.getType() == ExchangeAction.TRADING_WITH_NPC_EXCHANGE && value is NpcExchange)
            ((value as NpcExchange)).toogleOK(false)

        if (exchangeAction.getType() == ExchangeAction.TRADING_WITH_NPC_PETS && value is PlayerExchange.NpcExchangePets)
            ((value as PlayerExchange.NpcExchangePets)).toogleOK(false)

        if (exchangeAction.getType() == ExchangeAction.TRADING_WITH_NPC_PETS_RESURRECTION && value is PlayerExchange.NpcRessurectPets)
            ((value as PlayerExchange.NpcRessurectPets)).toogleOK(false)

        if ((exchangeAction.getType() == ExchangeAction.TRADING_WITH_PLAYER || exchangeAction.getType() == ExchangeAction.CRAFTING_SECURE_WITH) && value is Exchange)
            if (((value as Exchange)).toogleOk(this.player.id))
                ((value as Exchange)).apply()

        if (exchangeAction.getType() == ExchangeAction.BREAKING_OBJECTS && value is BreakingObject) {
            if (((value as BreakingObject)).objects.isEmpty())
                return

            var fragment: Fragment = Fragment("")

            for (couple in  ((value as BreakingObject)).objects) {
                var obj: GameObject? = this.player.objects[couple.first]

                if (obj == null || couple.second < 1 || obj.quantity < couple.second) {
                    this.player.send("Ea3")
                    break
                }

                for (k in couple.second downTo 0) {
                    var type: Int = obj.template!!.type
                    if (type > 11 && type < 16 && type > 23 && type != 81 && type != 82)
                        continue
                    for (entry1 in  obj.stats.effects.entries) {
                        var jet: Int = entry1.value
                        for (rune in  Rune.runes) {
                            if (entry1.key == rune.characteristic.toInt()) {
                                if (rune.id.toInt() == 1557 ||rune.id.toInt() == 1558 || rune.id.toInt() == 7438) {
                                    var puissance: Double = 1.5 * (Math.pow(obj.template!!.level.toDouble(), 2.0) / Math.pow(rune.weight.toDouble(), (5.0 / 4.0))) + ((jet - 1) / rune.weight) * (66.66 - 1.5 * (Math.pow(obj.template!!.level.toDouble(), 2.0) / Math.pow(rune.weight.toDouble(), (55.0 / 4.0))))
                                    var chance: Int = Math.ceil(puissance).toInt()

                                    if (chance > 66) chance = 66
                                    else if (chance <= 0) chance = 1
                                    if (Formulas.getRandomValue(1, 100) <= chance)
                                        fragment.addRune(rune.id.toInt())
                                } else {
                                    var vale: Double= rune.bonus.toDouble()
                                    if (rune.id.toInt() == 7451 || rune.id.toInt() == 10662) vale *= 3.0

                                    var tauxGetMin: Double = World.world.getTauxObtentionIntermediaire(vale, true, (vale != 30.0))
                                    var tauxGetMax: Double = (tauxGetMin / (2.0 / 3.0)) / 0.9
                                    var tauxMax: Int = Math.ceil(tauxGetMax).toInt()
                                    var tauxGet: Int = Math.ceil(tauxGetMin).toInt()
                                    var tauxMin: Int = 2 * (tauxMax - tauxGet) - 2

                                    if (rune.id.toInt() == 7433 || rune.id.toInt() == 7434 || rune.id.toInt() == 7435 || rune.id.toInt() == 7441)
                                        tauxMax++
                                    if (jet < tauxMin) continue

                                    var i: Int = jet
                                    while (i > 0) {
                                        var j: Int = 0
                                        if (i > tauxMax) j = tauxMax
                                        else j = i
                                        if (j == tauxMax) fragment.addRune(rune.id.toInt())
                                        else if (Formulas.getRandomValue(1, 100) < (100 * (tauxMax - j) / (tauxMax - tauxMin)))
                                            fragment.addRune(rune.id.toInt())
                                        i -= tauxMax
                                    }
                                }
                            }
                        }
                    }
                }

                if (couple.second == obj.quantity) {
                    this.player.deleteItem(obj.guid)
                    World.world.removeGameObject(obj.guid)
                    SocketManager.SEND_OR_DELETE_ITEM(this, obj.guid)
                } else {
                    obj.quantity = obj.quantity - couple.second
                    SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this.player, obj)
                }
            }

            World.world.addGameObject(fragment)
            this.player.addItem(fragment, true)
            SocketManager.GAME_SEND_Ec_PACKET(this.player, "K;8378")
            SocketManager.GAME_SEND_Ow_PACKET(this.player)
            SocketManager.GAME_SEND_IO_PACKET_TO_MAP(this.player.curMap, this.player.id, "+8378")
            this.player.startActionOnCell(this.player.gameAction!!)
            ((value as BreakingObject)).objects.clear()
        }
    }

    private fun replayCraft() {
        if (this.player.exchangeAction != null && this.player.exchangeAction!!.getType() == ExchangeAction.CRAFTING)
            if (((this.player.exchangeAction!!.getValue() as JobAction)).jobCraft == null)
                ((this.player.exchangeAction!!.getValue() as JobAction)).putLastCraftIngredients()
    }

    private
        @Synchronized fun movementItemOrKamas(packet: String) {
        var packet = packet
        if(this.player.exchangeAction == null) return
        if(packet.contains("NaN")) {
            this.player.sendMessage("Error : StartExchange : (" + this.player.exchangeAction!!.getType() + ") : " + packet + "\n send at administreur.")
            return
        }
        when (this.player.exchangeAction!!.getType()) {
ExchangeAction.TRADING_WITH_ME -> {when (packet[2]) {
'O' -> {if (packet[3] == '+') {
                            var infos: List<String> = packet.substring(4).split("|")
                            try {
                                var guid: Int = (infos[0]).toInt()
                                var qua: Int = (infos[1]).toInt()
                                var price: Int = (infos[2]).toInt()

                                var obj: GameObject? = this.player.objects[guid]
                                if (obj == null)
                                    return
                                if (qua <= 0 || obj.isAttach)
                                    return
                                if (price <= 0)
                                    return

                                if (qua > obj.quantity)
                                    qua = obj.quantity
                                this.player.addInStore(obj.guid, price, qua)
                            } catch (e: NumberFormatException) {
                                World.world.logger.error("Error Echange Store '" + packet + "' => " + e.message)
                                log.error("unexpected error", e)
                                return
                            }
                        } else {
                            var infos: List<String> = packet.substring(4).split("|")
                            try {
                                var guid: Int = (infos[0]).toInt()
                                var qua: Int = (infos[1]).toInt()

                                if (qua <= 0)
                                    return
                                var obj: GameObject? = World.world.getGameObject(guid)
                                if (obj == null)
                                    return
                                if (qua < 0)
                                    return
                                if (qua > obj.quantity)
                                    return
                                if (qua < obj.quantity)
                                    qua = obj.quantity
                                this.player.removeFromStore(obj.guid, qua)
                            } catch (e: NumberFormatException) {
                                World.world.logger.error("Error Echange Store '" + packet + "' => " + e.message)
                                log.error("unexpected error", e)
                                return
                            }
                        }
                        
}
}
                
}
ExchangeAction.TRADING_WITH_COLLECTOR -> {var Collector: Collector? = World.world.getCollector((this.player.exchangeAction!!.getValue() as Int))
                if (Collector == null || Collector.inFight > 0)
                    return
                when (packet[2]) {
'G' -> {if (packet[3] == '-') //On retire
                        {
                            var P_Kamas: Long = -1
                            try {
                                P_Kamas = (packet.substring(4)).toInt().toLong()
                            } catch (e: NumberFormatException) {
                                log.error("unexpected error", e)
                                World.world.logger.error("Error Echange CC '" + packet + "' => " + e.message)
                            }
                            if (P_Kamas < 0)
                                return
                            if (Collector.kamas >= P_Kamas) {//Faille non connu ! :p
                                var P_Retrait: Long = Collector.kamas - P_Kamas
                                Collector.kamas = Collector.kamas - P_Kamas
                                if (P_Retrait < 0) {
                                    P_Retrait = 0
                                    P_Kamas = Collector.kamas
                                }
                                Collector.kamas = P_Retrait
                                this.player.addKamas(P_Kamas)
                                SocketManager.GAME_SEND_STATS_PACKET(this.player)
                                SocketManager.GAME_SEND_EsK_PACKET(this.player, "G" + Collector.kamas)
                            }
                        }
                        
}
'O' -> {if (packet[3] == '-') //On retire
                        {
                            var infos: List<String> = packet.substring(4).split("|")
                            var guid: Int = 0
                            var qua: Int = 0
                            try {
                                guid = (infos[0]).toInt()
                                qua = (infos[1]).toInt()
                            } catch (e: NumberFormatException) {
                                // ok
                                return
                            }

                            if (guid <= 0 || qua <= 0)
                                return

                            var obj: GameObject? = World.world.getGameObject(guid)
                            if (obj == null)
                                return

                            if (Collector.haveObjects(guid)) {
                                Collector.removeFromCollector(this.player, guid, qua)
                            }
                            Collector.addLogObjects(guid, obj)
                        }
                        
}
}
                DatabaseManager.get(GuildData::class.java).update(this.player.getGuild()!!)
                
}
ExchangeAction.BREAKING_OBJECTS -> {val breakingObject: BreakingObject = ((this.player.exchangeAction!!.getValue() as BreakingObject))

                if (packet[2] == 'O') {
                    if (packet[3] == '+') {
                        if (breakingObject.objects.size >= 8)
                            return

                        var infos: List<String> = packet.substring(4).split("|")

                        try {
                            var id: Int = (infos[0]).toInt()
                            var qua: Int = (infos[1]).toInt()

                            if (!this.player.hasItemGuid(id))
                                return

                            var obj: GameObject? = this.player.objects[id]

                            if (obj == null || obj.isAttach)
                                return
                            if (qua < 1)
                                return
                            if (qua > obj.quantity)
                                qua = obj.quantity

                            var type: Int = obj.template!!.type

                            SocketManager.SEND_EMK_MOVE_ITEM(this, 'O', "+", id.toString() + "|" + breakingObject.addObject(id, qua))
                        } catch (e: NumberFormatException) {
                            World.world.logger.error("Error Echange CC '" + packet + "' => " + e.message)
                            log.error("unexpected error", e)
                            return
                        }
                    } else if (packet[3] == '-') {
                        var infos: List<String> = packet.substring(4).split("|")
                        try {
                            var id: Int = (infos[0]).toInt()
                            var qua: Int = (infos[1]).toInt()

                            var obj: GameObject? = World.world.getGameObject(id)

                            if (obj == null)
                                return
                            if (qua < 1)
                                return

                            val quantity: Int = breakingObject.removeObject(id, qua)

                            if (quantity <= 0)
                                SocketManager.SEND_EMK_MOVE_ITEM(this, 'O', "-", id.toString() + "")
                            else
                                SocketManager.SEND_EMK_MOVE_ITEM(this, 'O', "+", id.toString() + "|" + quantity)
                        } catch (e: NumberFormatException) {
                            World.world.logger.error("Error Echange CC '" + packet + "' => " + e.message)
                            log.error("unexpected error", e)
                            return
                        }
                    }
                } else if(packet[2] == 'R') {
                    val count: Int = (packet.substring(3)).toInt()
                    breakingObject.count = count
                    TimerWaiter.addNext({
                        this.recursiveBreakingObject(breakingObject, 0, count)

                    }, 0)
                } else if(packet[2] == 'r') {
                    breakingObject.isStop = true
                }
                
}
ExchangeAction.IN_MOUNT -> {var mount: Mount = this.player.mount!!
                if (mount == null) return
                when (packet[2]) {
'O' -> {var id: Int = 0
                        var cant: Int = 0
                        try {
                            id = (packet.substring(4).split("|")[0]).toInt()
                            cant = (packet.substring(4).split("|")[1]).toInt()
                        } catch (e: Exception) {
                            World.world.logger.error("Error Echange DD '" + packet + "' => " + e.message)
                            log.error("unexpected error", e)
                            return
                        }
                        if (id == 0 || cant <= 0)
                            return
                        if (World.world.getGameObject(id) == null) {
                            SocketManager.GAME_SEND_MESSAGE(this.player, this.player.getLang().trans("game.gameclient.movemenitemorkamas.error1"))
                            return
                        }
                        when (packet[3]) {
'+' -> {mount.addObject(id, cant, this.player)
                                
}
'-' -> {mount.removeObject(id, cant, this.player)
                                
}
',' -> {
}
}
                        
}
}
                
}
ExchangeAction.TRADING_WITH_NPC_EXCHANGE -> {when (packet[2]) {
'O' -> {if (packet[3] == '+') {
                            var infos: List<String> = packet.substring(4).split("|")
                            try {
                                var guid: Int = (infos[0]).toInt()
                                var qua: Int = (infos[1]).toInt()
                                var quaInExch: Int = ((this.player.exchangeAction!!.getValue() as NpcExchange)).getQuaItem(guid, false)

                                if (!this.player.hasItemGuid(guid)) return
                                var obj: GameObject? = this.player.objects[guid]
                                if (obj == null) return

                                if (qua > obj.quantity - quaInExch)
                                    qua = obj.quantity - quaInExch
                                if (qua <= 0)
                                    return
                                if(AuctionManager.getInstance().onPlayerChangeItemInNpcExchange(this.player, obj))
                                    return

                                ((this.player.exchangeAction!!.getValue() as NpcExchange)).addItem(guid, qua)
                            } catch (e: NumberFormatException) {
                                World.world.logger.error("Error Echange NPC '" + packet + "' => " + e.message)
                                log.error("unexpected error", e)
                                return
                            }
                        } else {
                            var infos: List<String> = packet.substring(4).split("|")
                            try {
                                var guid: Int = (infos[0]).toInt()
                                var qua: Int = (infos[1]).toInt()

                                if (qua <= 0)
                                    return
                                if (!this.player.hasItemGuid(guid))
                                    return

                                var obj: GameObject? = World.world.getGameObject(guid)
                                if (obj == null)
                                    return
                                if (qua > ((this.player.exchangeAction!!.getValue() as NpcExchange)).getQuaItem(guid, false))
                                    return

                                ((this.player.exchangeAction!!.getValue() as NpcExchange)).removeItem(guid, qua)
                            } catch (e: NumberFormatException) {
                                World.world.logger.error("Error Echange NPC '" + packet + "' => " + e.message)
                                log.error("unexpected error", e)
                                return
                            }
                        }
                        
}
'G' -> {try {
                            var numb: Long = (packet.substring(3)).toInt().toLong()
                            if (this.player.kamas < numb)
                                numb = this.player.kamas
                            ((this.player.exchangeAction!!.getValue() as NpcExchange)).setKamas(false, numb)
                        } catch (e: NumberFormatException) {
                            World.world.logger.error("Error Echange NPC '" + packet + "' => " + e.message)
                            log.error("unexpected error", e)
                            return
                        }
                        
}
}
                
}
ExchangeAction.TRADING_WITH_NPC_PETS -> {when (packet[2]) {
'O' -> {if (packet[3] == '+') {
                            var infos: List<String> = packet.substring(4).split("|")
                            try {
                                var guid: Int = (infos[0]).toInt()
                                var qua: Int = (infos[1]).toInt()
                                var quaInExch: Int = ((this.player.exchangeAction!!.getValue() as PlayerExchange.NpcExchangePets)).getQuaItem(guid, false)

                                if (!this.player.hasItemGuid(guid))
                                    return
                                var obj: GameObject? = this.player.objects[guid]
                                if (obj == null)
                                    return

                                if (qua > obj.quantity - quaInExch)
                                    qua = obj.quantity - quaInExch

                                if (qua <= 0)
                                    return

                                ((this.player.exchangeAction!!.getValue() as PlayerExchange.NpcExchangePets)).addItem(guid, qua)
                            } catch (e: NumberFormatException) {
                                World.world.logger.error("Error Echange Pets '" + packet + "' => " + e.message)
                                log.error("unexpected error", e)
                                return
                            }
                        } else {
                            var infos: List<String> = packet.substring(4).split("|")
                            try {
                                var guid: Int = (infos[0]).toInt()
                                var qua: Int = (infos[1]).toInt()

                                if (qua <= 0)
                                    return
                                if (!this.player.hasItemGuid(guid))
                                    return

                                var obj: GameObject? = World.world.getGameObject(guid)
                                if (obj == null)
                                    return
                                if (qua > ((this.player.exchangeAction!!.getValue() as PlayerExchange.NpcExchangePets)).getQuaItem(guid, false))
                                    return

                                ((this.player.exchangeAction!!.getValue() as PlayerExchange.NpcExchangePets)).removeItem(guid, qua)
                            } catch (e: NumberFormatException) {
                                World.world.logger.error("Error Echange Pets '" + packet + "' => " + e.message)
                                log.error("unexpected error", e)
                                return
                            }
                        }
                        
}
'G' -> {try {
                            var numb: Long = (packet.substring(3)).toInt().toLong()
                            if (numb < 0)
                                return
                            if (this.player.kamas < numb)
                                numb = this.player.kamas
                            ((this.player.exchangeAction!!.getValue() as PlayerExchange.NpcExchangePets)).setKamas(false, numb)
                        } catch (e: NumberFormatException) {
                            World.world.logger.error("Error Echange Pets '" + packet + "' => " + e.message)
                            log.error("unexpected error", e)
                            return
                        }
                        
}
}
                
}
ExchangeAction.TRADING_WITH_NPC_PETS_RESURRECTION -> {when (packet[2]) {
'O' -> {if (packet[3] == '+') {
                            var infos: List<String> = packet.substring(4).split("|")
                            try {

                                var guid: Int = (infos[0]).toInt()
                                var qua: Int = (infos[1]).toInt()
                                var quaInExch: Int = ((this.player.exchangeAction!!.getValue() as PlayerExchange.NpcRessurectPets)).getQuaItem(guid, false)

                                if (!this.player.hasItemGuid(guid))
                                    return
                                var obj: GameObject? = World.world.getGameObject(guid)
                                if (obj == null)
                                    return

                                if (qua > obj.quantity - quaInExch)
                                    qua = obj.quantity - quaInExch

                                if (qua <= 0)
                                    return

                                ((this.player.exchangeAction!!.getValue() as PlayerExchange.NpcRessurectPets)).addItem(guid, qua)
                            } catch (e: NumberFormatException) {
                                World.world.logger.error("Error Echange RPets '" + packet + "' => " + e.message)
                                log.error("unexpected error", e)
                                return
                            }
                        } else {
                            var infos: List<String> = packet.substring(4).split("|")
                            try {
                                var guid: Int = (infos[0]).toInt()
                                var qua: Int = (infos[1]).toInt()

                                if (qua <= 0)
                                    return
                                if (!this.player.hasItemGuid(guid))
                                    return

                                var obj: GameObject? = World.world.getGameObject(guid)
                                if (obj == null)
                                    return
                                if (qua > ((this.player.exchangeAction!!.getValue() as PlayerExchange.NpcRessurectPets)).getQuaItem(guid, false))
                                    return

                                ((this.player.exchangeAction!!.getValue() as PlayerExchange.NpcRessurectPets)).removeItem(guid, qua)
                            } catch (e: NumberFormatException) {
                                World.world.logger.error("Error Echange RPets '" + packet + "' => " + e.message)
                                log.error("unexpected error", e)
                                return
                            }
                        }
                        
}
'G' -> {try {
                            var numb: Long = (packet.substring(3)).toInt().toLong()
                            if (numb < 0)
                                return
                            if (this.player.kamas < numb)
                                numb = this.player.kamas
                            ((this.player.exchangeAction!!.getValue() as PlayerExchange.NpcRessurectPets)).setKamas(false, numb)
                        } catch (e: NumberFormatException) {
                            log.error("unexpected error", e)
                            return
                        }
                        
}
}
                
}
ExchangeAction.AUCTION_HOUSE_SELLING -> {var exchangeAction: BigStoreActionData = Optional.ofNullable(this.player.exchangeAction).filter({ ea -> ea.getType() == ExchangeAction.AUCTION_HOUSE_SELLING })
                        .map { it.getValue() }.map(BigStoreActionData::class.java::cast).orElse(null)
                if(exchangeAction == null) {
                    return
                }
                var curBigStore: BigStore = World.world.getHdv(exchangeAction.hdvId)!!

                when (packet[3]) {
'-' -> {var count: Int = 0
                        var lineId: Int = 0
                        try {
                            lineId = (packet.substring(4).split("|")[0]).toInt()
                            count = (packet.substring(4).split("|")[1]).toInt()
                        } catch (e: Exception) {
                            World.world.logger.error("Error Echange HDV '" + packet + "' => " + e.message)
                            log.error("unexpected error", e)
                            return
                        }
                        if (count <= 0)
                            return

                        if(!curBigStore.removeListing(this.player.getAccount()!!, lineId)) {
                            return
                        }

                        SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(this, '-', "", lineId.toString() + "")
                        
}
'+' -> {if ((packet.substring(4).split("|")[1]).toInt() > 127) {
                            SocketManager.GAME_SEND_MESSAGE(this.player, this.player.getLang().trans("game.gameclient.movemenitemorkamas.limit"))
                            return
                        }

                        var itmID: Int
                        var price: Int = 0
                        var amount: Byte = 0

                        try {
                            itmID = (packet.substring(4).split("|")[0]).toInt()
                            amount = packet.substring(4).split("|")[1].toByte()
                            price = (packet.substring(4).split("|")[2]).toInt()
                        } catch (e: Exception) {
                            World.world.logger.error("Error Echange HDV '" + packet + "' => " + e.message)
                            // Arrive quand price n'est pas dans le pacquet. C'est que le joueur ne veut pas mettre dans un hdv, mais dans autre chose ... Un paquet qui est MO+itmID|qt?
                            // Peeut-?tre apr?sa voir utilis? le concasseur ...
                            log.error("unexpected error", e)
                            SocketManager.GAME_SEND_MESSAGE(this.player, "Une erreur s'est produite lors de la mise en vente de votre objet. Veuillez vous reconnectez pour corriger l'erreur. Personnage " + this.player.name + " et paquet " + packet + ".")
                            return
                        }

                        if (amount <= 0 || price <= 0)
                            return
                        if (packet.substring(1).split("|")[2] == "0"
                                || packet.substring(2).split("|")[2] == "0"
                                || packet.substring(3).split("|")[2] == "0")
                            return

                        var taxe: Int = ((price * (curBigStore.taxe / 100)).toInt())

                        if (taxe < 0)
                            return

                        if (!this.player.hasItemGuid(itmID))//V?rifie si le this.playernnage a bien l'item sp?cifi? et l'argent pour payer la taxe
                            return
                        if (this.player.getAccount()!!.countHdvEntries(curBigStore.hdvId) >= curBigStore.maxAccountItem) {
                            SocketManager.GAME_SEND_Im_PACKET(this.player, "058")
                            return
                        }
                        if (this.player.kamas < taxe) {
                            SocketManager.GAME_SEND_Im_PACKET(this.player, "176")
                            return
                        }

                        var obj: GameObject? = World.world.getGameObject(itmID)//R?cup?re l'item
                        if (obj == null || obj.isAttach) return

                        this.player.addKamas(taxe.toLong() * -1);//Retire le montant de la taxe au this.playernnage
                        SocketManager.GAME_SEND_STATS_PACKET(this.player);//Met a jour les kamas du client

                        var qua: Int = (if (amount.toInt() == 1) (1).toInt() else (if (amount.toInt() == 2) 10 else 100))

                        if (qua > obj.quantity)//S'il veut mettre plus de cette objet en vente que ce qu'il poss?de
                            return
                        var rAmount: Int = ((Math.pow(10.0, amount.toDouble()) / 10).toInt())
                        var newQua: Int = (obj.quantity - rAmount)

                        if (newQua <= 0)//Si c'est plusieurs objets ensemble enleve seulement la quantit? de mise en vente
                        {
                            this.player.removeItem(itmID);//Enl?ve l'item de l'inventaire du this.playernnage
                            SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this.player, itmID);//Envoie un packet au client pour retirer l'item de son inventaire
                        } else {
                            obj.quantity = obj.quantity - rAmount
                            SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this.player, obj)
                            var newObj: GameObject = obj.getClone(rAmount, true)!!
                            obj = newObj
                        }
                        // Client amount is 1,2,3, we need 0,1,2
                        var toAdd: BigStoreListing = BigStoreListing(price, ((amount-1).toByte()), this.player.getAccount()!!.id, obj)
                        if(!curBigStore.addEntry(toAdd)) {
                            return
                        }
                        SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(this, '+', "", toAdd.parseToEmK()); //Envoie un packet pour ajthiser l'item dans la fenetre de l'HDV du client
                        SocketManager.GAME_SEND_HDVITEM_SELLING(this.player, toAdd.hdvId)
                        DatabaseManager.get(PlayerData::class.java).update(this.player)
                        
}
}
                
}
ExchangeAction.CRAFTING -> {var skillID: Int = (this.player.exchangeAction!!.getValue() as Int)

                when (packet[2]) {
'O' -> {
}
'R' -> {
}
'r' -> {
}
}

//                if (packet.charAt(2) == 'O' && ((JobAction) this.player.getExchangeAction().getValue()).getJobCraft() == null) {
//                    packet = packet.replace("-", ";-").replace("+", ";+").substring(4);
//
//                    for(String part : packet.split(";")) {
//                        try {
//                            char c = part.charAt(0);
//                            String[] infos = part.substring(1).split("|");
//                            int id = (infos[0]).toInt(), quantity = 1;
//                            try {
//                                quantity = (infos[1]).toInt();
//                            } catch (Exception ignored) {}
//
//                            if (quantity <= 0) return;
//                            if (c == '+') {
//                                if (!this.player.hasItemGuid(id))
//                                    return;
//
//                                GameObject obj = this.player.objects[id];
//
//                                if (obj == null || obj.getObvijevanLook() != 0) {
//                                    player.send("BN");
//                                    return;
//                                }
//                                if (obj.getQuantity() < quantity)
//                                    quantity = obj.getQuantity();
//
//                                ((JobAction) this.player.getExchangeAction().getValue()).addIngredient(this.player, id, quantity);
//                            } else if (c == '-') {
//                                ((JobAction) this.player.getExchangeAction().getValue()).addIngredient(this.player, id, -quantity);
//                            }
//                        } catch(Exception e) {
//                            log.error("unexpected error", e)
//                        }
//                    }
//                } else if (packet.charAt(2) == 'R') {
//                    if (((JobAction) this.player.getExchangeAction().getValue()).getJobCraft() == null) {
//                        ((JobAction) this.player.getExchangeAction().getValue()).setJobCraft(((JobAction) this.player.getExchangeAction().getValue()).oldJobCraft);
//                    }
//                    ((JobAction) this.player.getExchangeAction().getValue()).getJobCraft().setAction((packet.substring(3)).toInt());
//                } else if (packet.charAt(2) == 'r') {
//                    if (this.player.getExchangeAction().getValue() != null) {
//                        if (((JobAction) this.player.getExchangeAction().getValue()).getJobCraft() != null) {
//                            ((JobAction) this.player.getExchangeAction().getValue()).broken = true;
//                        }
//                    }
//                }
                
}
ExchangeAction.IN_BANK -> {when (packet[2]) {
'G' -> {if (Main.tradeAsBlocked)
                            return
                        var kamas: Long = 0
                        try {
                            kamas = (packet.substring(3)).toInt().toLong()
                        } catch (e: Exception) {
                            World.world.logger.error("Error Echange Banque '" + packet + "' => " + e.message)
                            log.error("unexpected error", e)
                            return
                        }
                        if (kamas.toInt() == 0)
                            return

                        if (kamas > 0)//Si On ajoute des kamas a la banque
                        {
                            if (this.player.kamas < kamas)
                                kamas = this.player.kamas
                            this.player.setBankKamas(this.player.getBankKamas() + kamas);//On ajthise les kamas a la banque
                            this.player.kamas = this.player.kamas - kamas;//On retire les kamas du this.playernnage
                            SocketManager.GAME_SEND_STATS_PACKET(this.player)
                            SocketManager.GAME_SEND_EsK_PACKET(this.player, "G" + this.player.getBankKamas())
                        } else {
                            kamas = -kamas;//On repasse en positif
                            if (this.player.getBankKamas() < kamas)
                                kamas = this.player.getBankKamas()
                            this.player.setBankKamas(this.player.getBankKamas() - kamas);//On retire les kamas de la banque
                            this.player.kamas = this.player.kamas + kamas;//On ajthise les kamas du this.playernnage
                            SocketManager.GAME_SEND_STATS_PACKET(this.player)
                            SocketManager.GAME_SEND_EsK_PACKET(this.player, "G" + this.player.getBankKamas())
                        }
                        
}
'O' -> {if (Main.tradeAsBlocked)
                            return
                        var guid: Int = 0
                        var qua: Int = 0
                        try {
                            guid = (packet.substring(4).split("|")[0]).toInt()
                            qua = (packet.substring(4).split("|")[1]).toInt()
                        } catch (e: Exception) {
                            World.world.logger.error("Error Echange Banque '" + packet + "' => " + e.message)
                            log.error("unexpected error", e)
                            return
                        }

                        if (guid == 0 || qua <= 0)
                            return

                        when (packet[3]) {
'+' -> {this.player.addInBank(guid, qua, false)
                                
}
'-' -> {var obj: GameObject = World.world.getGameObject(guid)!!
                                if(obj != null) {
                                    if (Constant.STATS_OWNER_1 in obj.txtStat) {
                                        var player: Player? = World.world.getPlayerByName(obj.txtStat[Constant.STATS_OWNER_1] ?: "")
                                        if (player != null) {
                                            if (!player!!.name.equals(this.player.name))
                                                return
                                        }
                                    }
                                    this.player.removeFromBank(guid, qua)
                                }
                                
}
}
                        
}
}
                
}
ExchangeAction.IN_TRUNK -> {if (Main.tradeAsBlocked)
                    return
                var t: Trunk = (this.player.exchangeAction!!.getValue() as Trunk)

                when (packet[2]) {
'G' -> {var kamas: Long = 0
                        try {
                            kamas = (packet.substring(3)).toInt().toLong()
                        } catch (e: Exception) {
                            World.world.logger.error("Error Echange Coffre '" + packet + "' => " + e.message)
                            log.error("unexpected error", e)
                            return
                        }

                        if (kamas.toInt() == 0)
                            return

                        if (kamas > 0)//Si On ajthise des kamas au coffre
                        {
                            if (this.player.kamas < kamas)
                                kamas = this.player.kamas
                            t.kamas = t.kamas + kamas;//On ajthise les kamas au coffre
                            this.player.kamas = this.player.kamas - kamas;//On retire les kamas du this.playernnage
                            SocketManager.GAME_SEND_STATS_PACKET(this.player)
                        } else {
                            kamas = -kamas;//On repasse en positif
                            if (t.kamas < kamas)
                                kamas = t.kamas
                            t.kamas = t.kamas - kamas;//On retire les kamas de la banque
                            this.player.kamas = this.player.kamas + kamas;//On ajthise les kamas du this.playernnage
                            SocketManager.GAME_SEND_STATS_PACKET(this.player)
                        }
                        World.world.onlinePlayers.stream().filter({ player -> player.exchangeAction != null &&
                                player.exchangeAction!!.getType() == ExchangeAction.IN_TRUNK &&
                                ((this.player.exchangeAction!!.getValue() as Trunk)).id == ((player.exchangeAction!!.getValue() as Trunk)).id })
                                .forEach({ P -> SocketManager.GAME_SEND_EsK_PACKET(P, "G" + t.kamas) })
                        DatabaseManager.get(TrunkData::class.java).update(t)
                        
}
'O' -> {var guid: Int = 0
                        var qua: Int = 0
                        try {
                            guid = (packet.substring(4).split("|")[0]).toInt()
                            qua = (packet.substring(4).split("|")[1]).toInt()
                        } catch (e: Exception) {
                            World.world.logger.error("Error Echange Coffre '" + packet + "' => " + e.message)
                            log.error("unexpected error", e)
                            return
                        }

                        if (guid == 0 || qua <= 0)
                            return

                        when (packet[3]) {
'+' -> {t.addInTrunk(guid, qua, this.player)
                                
}
'-' -> {t.removeFromTrunk(guid, qua, this.player)
                                
}
}
                        
}
}
                
}
ExchangeAction.CRAFTING_SECURE_WITH, ExchangeAction.TRADING_WITH_PLAYER -> {when (packet[2]) {
'O' -> {if (packet[3] == '+') {
                            for(arg in  packet.substring(4).split("+")) {
                                var infos: List<String> = arg.split("|")
                                try {
                                    var guid: Int = (infos[0]).toInt()
                                    var qua: Int = (infos[1]).toInt()
                                    var quaInExch: Int = ((this.player.exchangeAction!!.getValue() as PlayerExchange)).getQuaItem(guid, this.player.id)

                                    if (!this.player.hasItemGuid(guid))
                                        return
                                    var obj: GameObject? = this.player.objects[guid]
                                    if (obj == null)
                                        return
                                    if (qua > obj.quantity - quaInExch)
                                        qua = obj.quantity - quaInExch

                                    if (qua <= 0 || obj.isAttach)
                                        return

                                    ((this.player.exchangeAction!!.getValue() as PlayerExchange)).addItem(guid, qua, this.player.id)
                                } catch (e: NumberFormatException) {
                                    this.player.sendMessage("Error : PlayerExchange : " + packet + "\n" + e.message)
                                    log.error("unexpected error", e)
                                    return
                                }
                            }
                        } else {
                            var infos: List<String> = packet.substring(4).split("|")
                            try {
                                var guid: Int = (infos[0]).toInt()
                                var qua: Int = (infos[1]).toInt()

                                if (qua <= 0)
                                    return
                                if (!this.player.hasItemGuid(guid))
                                    return

                                var obj: GameObject? = this.player.objects[guid]
                                if (obj == null)
                                    return
                                if (qua > ((this.player.exchangeAction!!.getValue() as PlayerExchange)).getQuaItem(guid, this.player.id))
                                    return

                                ((this.player.exchangeAction!!.getValue() as PlayerExchange)).removeItem(guid, qua, this.player.id)
                            } catch (e: NumberFormatException) {
                                this.player.sendMessage("Error : PlayerExchange : " + packet + "\n" + e.message)
                                log.error("unexpected error", e)
                                return
                            }
                        }
                        
}
'G' -> {try {
                            if(packet.substring(3).contains("NaN")) return
                            var numb: Long = (packet.substring(3)).toInt().toLong()
                            if (this.player.kamas < numb)
                                numb = this.player.kamas
                            if (numb < 0)
                                return
                            ((this.player.exchangeAction!!.getValue() as PlayerExchange)).setKamas(this.player.id, numb)
                        } catch (e: NumberFormatException) {
                            World.world.logger.error("Error Echange PvP '" + packet + "' => " + e.message)
                            log.error("unexpected error", e)
                            return
                        }
                        
}
}
                
}
}
    }

    private fun recursiveBreakingObject(breakingObject: BreakingObject, i: Int, count: Int) {
        if (breakingObject.isStop || !(i < count)) {
            if (breakingObject.isStop) this.player.send("Ea2")
            else this.player.send("Ea1")
            breakingObject.isStop = false
            return
        }

        TimerWaiter.addNext({
            this.player.send("EA" + (breakingObject.count - i))
            var objects: ArrayList<Couple<Int,Int>> = ArrayList(breakingObject.objects)
            this.ready()
            breakingObject.objects = objects
            this.recursiveBreakingObject(breakingObject, i + 1, count)
        }, 1000, TimeUnit.MILLISECONDS)
    }

    private @Synchronized fun movementItemOrKamasDons(packet: String) {
        if (this.player.exchangeAction != null && this.player.exchangeAction!!.getType() == ExchangeAction.CRAFTING_SECURE_WITH) {
            if (((this.player.exchangeAction!!.getValue() as CraftSecure)).getNeeder() == this.player) {
                var type: Byte = ((packet[0]).toString()).toByte()
                when (packet[1]) {
'O' -> {var split: List<String> = packet.substring(3).split("|")
                        var adding: Boolean = packet[2] == '+'
                        var guid: Int = (split[0]).toInt()
                        var quantity: Int = (split[1]).toInt()

                        ((this.player.exchangeAction!!.getValue() as CraftSecure)).setPayItems(type, adding, guid, quantity)
                        
}
'G' -> {((this.player.exchangeAction!!.getValue() as CraftSecure)).setPayKamas(type, (packet.substring(2)).toInt().toLong())
                        
}
}
            }
        }
    }

    private fun askOfflineExchange() {
        if(EventManager.isInEvent(this.player))
            return
        if (this.player.exchangeAction != null || this.player.fight != null || this.player.away)
            return
        if (this.player.parseStoreItemsList().isEmpty()) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "123")
            return
        }
        if (SoulStone.isInArenaMap(this.player.curMap.id) || this.player.curMap.data.noSellers) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "113")
            return
        }
        if (this.player.curMap.id == 33 || this.player.curMap.id == 38 || this.player.curMap.id == 4601 || this.player.curMap.id == 4259 || this.player.curMap.id == 8036 || this.player.curMap.id == 10301) {
            if (this.player.curMap.storeCount >= 25) {
                SocketManager.GAME_SEND_Im_PACKET(this.player, "125;25")
                return
            }
        } else if (this.player.curMap.storeCount >= 6) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "125;6")
            return
        }
        for (entry in  this.player.storeItems.entries) {
            if (entry.value <= 0) {
                this.kick()
                return
            }
        }


        var taxe: Long = this.player.storeAllBuy().toLong() / 1000.toLong()

        if (taxe < 0) {
            this.kick()
            return
        }

        SocketManager.GAME_SEND_Eq_PACKET(this.player, taxe)
    }

    private fun offlineExchange() {
        if(EventManager.isInEvent(this.player))
            return
        if (SoulStone.isInArenaMap(this.player.curMap.id) || this.player.curMap.data.noSellers) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "113")
            return
        }
        if (this.player.curMap.id == 33 || this.player.curMap.id == 38 || this.player.curMap.id == 4601 || this.player.curMap.id == 4259 || this.player.curMap.id == 8036 || this.player.curMap.id == 10301) {
            if (this.player.curMap.storeCount >= 25) {
                SocketManager.GAME_SEND_Im_PACKET(this.player, "125;25")
                return
            }
        } else if (this.player.curMap.storeCount >= 6) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "125;6")
            return
        }
        var taxe: Long = this.player.storeAllBuy().toLong() / 1000.toLong()
        if (this.player.kamas < taxe) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "176")
            return
        }
        if (taxe < 0) {
            SocketManager.GAME_SEND_MESSAGE(this.player, this.player.getLang().trans("game.gameclient.offlineexchange.negative"))
            return
        }
        var orientation: Int = Formulas.getRandomValue(1, 3)
        this.player.kamas = this.player.kamas - taxe
        this.player.orientation = orientation
        var map: GameMap = this.player.curMap
        this.player.seeSeller = true
        World.world.addSeller(this.player)
        this.kick()
        map.players.stream().filter({ player -> player != null && player.isOnline }).forEach(SocketManager::GAME_SEND_MERCHANT_LIST)
    }

    private @Synchronized fun putInInventory(packet: String) {
        if(this.player.exchangeAction != null && this.player.exchangeAction!!.getType() == ExchangeAction.IN_MOUNTPARK) {
            var id: Int = -1
            var park: MountPark? = this.player.curMap.mountPark

            park = if (park == null) (if (player.curMap.id != 10332) World.world.getMap( 8743) else World.world.getMap( 8848))!!.mountPark!! else park!!
            if(park == null) return

            try { id = (packet.substring(3)).toInt(); } catch (ignored: Exception) {}

            var mount: Mount? = null
            var obj: GameObject? = null
            when (packet[2]) {
'C' -> {if(id == -1 || !this.player.hasItemGuid(id))
                        return
                    if(park.hasEtableFull(this.player.id)) {
                        this.player.send("Im1105")
                        return
                    }

                    obj = World.world.getGameObject(id)!!
                    mount = World.world.getMountById(obj!!.stats.getEffect(995))

                    if(mount == null){
                        /*int color = Constant.getMountColorByParchoTemplate(obj.getTemplate().getId());
						if (color < 1)
							return;
						mount = new Mount(color, this.player.getId(), false);*/
                        return
                    }
                    mount.owner = this.player.id
                    this.player.removeItem(id)
                    World.world.removeGameObject(id)

                    if(!park.getEtable().contains(mount))
                        park.getEtable().add(mount)

                    SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this.player, obj.guid)

                    DatabaseManager.get(MountData::class.java).update(mount)
                    DatabaseManager.get(PlayerData::class.java).update(this.player)
                    SocketManager.GAME_SEND_Ee_PACKET(this.player, if (mount!!.size == 50) '~' else '+', mount!!.parse())
                    
}
'c' -> {mount = World.world.getMountById(id)

                    mount = park.containsMountInList(park.getEtable(), mount)
                    if(mount == null)
                        return

                    park.getEtable().remove(mount)
                    mount.owner = this.player.id

                    obj = Constant.getParchoTemplateByMountColor(mount.color)!!.createNewItem(1, false)
                    obj!!.setMountStats(this.player, mount, false)

                    World.world.addGameObject(obj)
                    this.player.addItem(obj!!, true)

                    SocketManager.GAME_SEND_Ee_PACKET(this.player, '-', mount!!.id.toString())
                    DatabaseManager.get(MountData::class.java).update(mount)
                    DatabaseManager.get(PlayerData::class.java).update(this.player)
                    
}
'g' -> {mount = World.world.getMountById(id)

                    mount = park.containsMountInList(park.getEtable(), mount)
                    if(mount == null) {
                        SocketManager.GAME_SEND_Im_PACKET(this.player, "1104")
                        return
                    }
                    if(this.player.mount != null) {
                        SocketManager.GAME_SEND_BN(this)
                        return
                    }
                    if(mount!!.fecundatedDate != -1L) {
                        SocketManager.GAME_SEND_BN(this)
                        return
                    }

                    mount.owner = this.player.id
                    park.getEtable().remove(mount)
                    this.player.mount = mount

                    SocketManager.GAME_SEND_Re_PACKET(this.player, "+", mount)
                    SocketManager.GAME_SEND_Ee_PACKET(this.player, '-', mount!!.id.toString())
                    SocketManager.GAME_SEND_Rx_PACKET(this.player)
                    DatabaseManager.get(MountData::class.java).update(mount)
                    DatabaseManager.get(PlayerData::class.java).update(this.player)
                    
}
'p' -> {if(this.player.mount != null && this.player.mount!!.id == id) {
                        if(park.hasEtableFull(this.player.id)) {
                            this.player.send("Im1105")
                            return
                        }

                        mount = this.player.mount
                        if(mount!!.objects.size == 0) {
                            if(this.player.onMount)
                                this.player.toogleOnMount()

                            if(!park.getEtable().contains(mount))
                                park.getEtable().add(mount)

                            mount!!.owner = this.player.id
                            this.player.mount = null

                            DatabaseManager.get(MountData::class.java).update(mount)
                            SocketManager.GAME_SEND_Ee_PACKET(this.player, if (mount!!.size == 50) '~' else '+', mount!!.parse())
                            SocketManager.GAME_SEND_Re_PACKET(this.player, "-", null)
                            SocketManager.GAME_SEND_Rx_PACKET(this.player)
                        } else {
                            SocketManager.GAME_SEND_Im_PACKET(this.player, "1106")
                        }
                        DatabaseManager.get(MountData::class.java).update(mount)
                        DatabaseManager.get(PlayerData::class.java).update(this.player)
                    }
                    
}
}
            DatabaseManager.get(BaseMountParkData::class.java).update(park)
        }
    }

    private @Synchronized fun putInMountPark(packet: String) {
        if(this.player.exchangeAction != null && this.player.exchangeAction!!.getType() == ExchangeAction.IN_MOUNTPARK) {
            var id: Int = -1
            var map: GameMap = this.player.curMap
            var park: MountPark? = this.player.curMap.mountPark
            if(park == null) return
            try { id = (packet.substring(3)).toInt()
            } catch (ignored: Exception) {}

            var mount: Mount? = null
            when (packet[2]) {
'g' -> {if(park.hasEtableFull(this.player.id)) {
                        this.player.send("Im1105")
                        return
                    }

                    mount = World.world.getMountById(id)
                    if(!park.getEtable().contains(mount) && park.getListOfRaising().contains(id)) {
                        park.getEtable().add(mount!!)
                        park.delRaising(mount!!.id)

                        mount!!.owner = this.player.id
                        this.player.curMap.mountPark!!.delRaising(id)
                        SocketManager.GAME_SEND_Ef_MOUNT_TO_ETABLE(this.player, '-', mount!!.id.toString() + "")

                        SocketManager.GAME_SEND_Ee_PACKET(this.player, if (mount!!.size == 50) '~' else '+', mount!!.parse())
                        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.player.curMap, id)
                        mount!!.mapId = -1
                        mount!!.cellId = -1

                        DatabaseManager.get(MountData::class.java).update(mount)
                        DatabaseManager.get(PlayerData::class.java).update(this.player)
                    } else return
                    
}
'p' -> {if(this.player.mount != null) {
                        if(this.player.mount!!.objects.size != 0) {
                            SocketManager.GAME_SEND_Im_PACKET(this.player, "1106")
                            return
                        }
                    }

                    if(park.hasEnclosFull(this.player.id)) {
                        this.player.send("Im1107")
                        return
                    }

                    if(this.player.mount != null && this.player.mount!!.id == id) {
                        if (this.player.onMount)
                            this.player.toogleOnMount()
                        if(this.player.onMount)
                            return
                        this.player.mount = null
                    }

                    mount = World.world.getMountById(id)


                    mount = park.containsMountInList(park.getEtable(), mount)
                    if(mount != null) {
                        mount!!.owner = this.player.id
                        mount!!.mapId = park.map
                        mount!!.cellId = park.placeOfSpawn
                        park.getEtable().remove(mount)
                        park.addRaising(id)
                        SocketManager.GAME_SEND_Ef_MOUNT_TO_ETABLE(this.player, '+', mount!!.parse())
                        SocketManager.GAME_SEND_Ee_PACKET(this.player, '-', mount!!.id.toString() + "")
                        SocketManager.GAME_SEND_GM_MOUNT_TO_MAP(map, mount)

                        DatabaseManager.get(MountData::class.java).update(mount)
                        DatabaseManager.get(PlayerData::class.java).update(this.player)
                    } else return
                    
}
}
            DatabaseManager.get(BaseMountParkData::class.java).update(park)
        }
    }

    private fun request(packet: String) {
        if (this.player.exchangeAction != null && this.player.exchangeAction!!.getType() != ExchangeAction.AUCTION_HOUSE_BUYING && this.player.exchangeAction!!.getType() != ExchangeAction.AUCTION_HOUSE_SELLING) {
            SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this, 'O')
            return
        }

        if (packet.substring(2, 4).equals("13") && this.player.exchangeAction == null) { // Craft s?curis? : celui qui n'a pas le job ( this.player ) souhaite invit? player
            try {
                var split: List<String> = packet.split("|")
                var id: Int = (split[1]).toInt()
                var skill: Int = (split[2]).toInt()

                var player: Player? = World.world.getPlayer(id)

                if (player == null) {
                    SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this, 'E')
                    return
                }
                if (player.curMap != this.player.curMap || !player.isOnline) {
                    SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this, 'E')
                    return
                }
                if (player.away || this.player.away) {
                    SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this, 'O')
                    return
                }

                var jobs: List<Job> = player.getJobs()

                if (jobs == null || jobs.isEmpty())
                    return

                var obj: GameObject? = player.getObjetByPos(Constant.ITEM_POS_ARME)

                if (obj == null) {
                    this.player.send("BN")
                    return
                }
                var ok: Boolean = false

                for (job in  jobs) {
                    if (job.getSkills().isEmpty())
                        continue
                    if (!job.isValidTool(obj.template!!.id))
                        continue

                    for (cell in  this.player.curMap.cases) {
//                        if (cell.getObject() != null) {
//                            if (cell.getObject().getTemplate() != null) {
//                                int io = cell.getObject().getTemplate().getId();
//                                ArrayList<Integer> skills = job.getSkills()[io];
//
//                                if (skills != null) {
//                                    for (int arg : skills) {
//                                        if (arg == skill
//                                                && PathFinding.getDistanceBetween(player.getCurMap(), player.getCurCell().getId(), cell.getId()) < 4) {
//                                            ok = true;
//                                            break;
//                                        }
//                                    }
//                                }
//                            }
//                        }
                    }

                    if (ok)
                        break
                }

                if (!ok) {
                    this.player.send("ERET")
                    return
                }

                var exchangeAction: ExchangeAction<Int> = ExchangeAction(ExchangeAction.CRAFTING_SECURE_WITH, id)
                this.player.exchangeAction = exchangeAction
                var exchangeAction1: ExchangeAction<Int> = ExchangeAction(ExchangeAction.CRAFTING_SECURE_WITH, this.player.id)
                player.exchangeAction = exchangeAction1

                this.player.craftingType.add(13)
                player.craftingType.add(12)
                this.player.craftingType.add(skill)
                player.craftingType.add(skill)

                SocketManager.GAME_SEND_EXCHANGE_REQUEST_OK(this, this.player.id, id, 12)
                SocketManager.GAME_SEND_EXCHANGE_REQUEST_OK(player.getGameClient()!!, this.player.id, id, 12)
            } catch (e: NumberFormatException) {
                log.error("unexpected error", e)
            }
            return
        } else if (packet.substring(2, 4).equals("12") && this.player.exchangeAction == null) { // Craft s?curis? : celui qui ? le job ( this.player ) souhaite invit? player
            try {
                var split: List<String> = packet.split("|")
                var id: Int = (split[1]).toInt()
                var skill: Int = (split[2]).toInt()

                var player: Player? = World.world.getPlayer(id)

                if (player == null) {
                    SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this, 'E')
                    return
                }
                if (player.curMap != this.player.curMap || !player.isOnline) {
                    SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this, 'E')
                    return
                }
                if (player.away || this.player.away) {
                    SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this, 'O')
                    return
                }

                var jobs: List<Job> = this.player.getJobs()
                if (jobs == null || jobs.isEmpty()) return

                var obj: GameObject? = this.player.getObjetByPos(Constant.ITEM_POS_ARME)
                if (obj == null) return

                var ok: Boolean = false

                for (job in  jobs) {
                    if (job.getSkills().isEmpty() || !job.isValidTool(obj.template!!.id)) continue
//                    for (GameCase cell : this.player.getCurMap().getCases()) {
//                        if (cell.getObject() != null) {
//                            if (cell.getObject().getTemplate() != null) {
//                                int io = cell.getObject().getTemplate().getId();
//                                ArrayList<Integer> skills = job.getSkills()[io];
//
//                                if (skills != null) {
//                                    for (int arg : skills) {
//                                        if (arg == skill && PathFinding.getDistanceBetween(this.player.getCurMap(), this.player.getCurCell().getId(), cell.getId()) < 4) {
//                                            ok = true;
//                                            break;
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
                    if (ok) break
                }

                if (!ok) {
                    this.player.sendMessage(player.getLang().trans("game.gameclient.atelier.tofar"))
                    return
                }

                var exchangeAction: ExchangeAction<Int> = ExchangeAction(ExchangeAction.CRAFTING_SECURE_WITH, id)
                this.player.exchangeAction = exchangeAction
                exchangeAction = ExchangeAction(ExchangeAction.CRAFTING_SECURE_WITH, this.player.id)
                player.exchangeAction = exchangeAction

                this.player.craftingType.add(12)
                player.craftingType.add(13)
                this.player.craftingType.add(skill)
                player.craftingType.add(skill)

                SocketManager.GAME_SEND_EXCHANGE_REQUEST_OK(this, this.player.id, id, 12)
                SocketManager.GAME_SEND_EXCHANGE_REQUEST_OK(player.getGameClient()!!, this.player.id, id, 13)
            } catch (e: NumberFormatException) {
                log.error("unexpected error", e)
            }
            return
        } else if (packet.substring(2, 4).equals("11")) {//Ouverture HDV achat
            if(this.player.exchangeAction != null) leaveExchange(this.player)
            if (this.player.deshonor >= 5) {
                SocketManager.GAME_SEND_Im_PACKET(this.player, "183")
                return
            }

            var bigStore: BigStore? = World.world.getHdv(this.player.curMap.id)
            if (bigStore != null) {
                var info: String = "1|10|100;" + bigStore.strCategory + ";" + bigStore.parseTaxe() + ";" + bigStore.lvlMax + ";" + bigStore.maxAccountItem + ";-1;" + bigStore.duration
                SocketManager.GAME_SEND_ECK_PACKET(this.player, 11, info)
                var exchangeAction: ExchangeAction<BigStoreActionData> = ExchangeAction(ExchangeAction.AUCTION_HOUSE_BUYING, BigStoreActionData(this.player.curMap.id))
                this.player.exchangeAction = exchangeAction
            }
            return
        } else if (packet.substring(2, 4).equals("15") && this.player.exchangeAction == null) {
            var mount: Mount? = this.player.mount

            if(mount != null) {
                var exchangeAction: ExchangeAction<Int> = ExchangeAction(ExchangeAction.IN_MOUNT, mount.id)
                this.player.exchangeAction = exchangeAction

                SocketManager.GAME_SEND_ECK_PACKET(this, 15, (mount.id).toString())
                SocketManager.GAME_SEND_EL_MOUNT_PACKET(this.player, mount)
                SocketManager.GAME_SEND_Ew_PACKET(this.player, mount.actualPods, mount.maxPods)
            }
            return
        } else if (packet.substring(2, 4).equals("17") && this.player.exchangeAction == null) {//Ressurection famillier
            var id: Int = (packet.substring(5)).toInt()

            if (this.player.curMap.getNpc(id) != null) {
                var ech: PlayerExchange.NpcRessurectPets = PlayerExchange.NpcRessurectPets(this.player, this.player.curMap.getNpc(id)!!.template)
                var exchangeAction: ExchangeAction<PlayerExchange.NpcRessurectPets> = ExchangeAction(ExchangeAction.TRADING_WITH_NPC_PETS_RESURRECTION, ech)
                this.player.exchangeAction = exchangeAction
                SocketManager.GAME_SEND_ECK_PACKET(this.player, 9, (id).toString())
            }
        } else if (packet.substring(2, 4).equals("10")) {//Ouverture HDV vente
            if(this.player.exchangeAction != null) leaveExchange(this.player)
            if (this.player.deshonor >= 5) {
                SocketManager.GAME_SEND_Im_PACKET(this.player, "183")
                return
            }

            var bigStore: BigStore? = World.world.getHdv(this.player.curMap.id)
            if (bigStore != null) {
                var infos: String = "1|10|100;" + bigStore.strCategory + ";" + bigStore.parseTaxe() + ";" + bigStore.lvlMax + ";" + bigStore.maxAccountItem + ";-1;" + bigStore.duration
                SocketManager.GAME_SEND_ECK_PACKET(this.player, 10, infos)
                var data: BigStoreActionData = BigStoreActionData(this.player.curMap.id)
                var exchangeAction: ExchangeAction<BigStoreActionData> = ExchangeAction(ExchangeAction.AUCTION_HOUSE_SELLING, data)
                this.player.exchangeAction = exchangeAction
                SocketManager.GAME_SEND_HDVITEM_SELLING(this.player, data.hdvId)
            }
            return
        } else if(packet.substring(2, 4).equals("18")) {
            var id: Int = (packet.split("|")[1]).toInt()
            if (this.player.curMap.getNpc(id) != null) {
                var ech: NpcExchange = NpcExchange(this.player, this.player.curMap.getNpc(id)!!.template)
                var exchangeAction: ExchangeAction<NpcExchange> = ExchangeAction(ExchangeAction.TRADING_WITH_NPC_EXCHANGE, ech)
                this.player.exchangeAction = exchangeAction
                SocketManager.GAME_SEND_ECK_PACKET(this.player, 2, (id).toString())
            }
            return
        }
        if (this.player.exchangeAction != null) {
            SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this, 'O')
            return
        }
            var id: Int = -1
            var exchangeAction: ExchangeAction<*>? = null
            when (packet[2]) {
'0' -> {id = (packet.substring(4)).toInt()
                var npc: Npc? = this.player.curMap.getNpc(id)

                if (npc != null) {
                    var exchangeAction: ExchangeAction<Int> = ExchangeAction(ExchangeAction.TRADING_WITH_NPC, id)
                    this.player.exchangeAction = exchangeAction

                    SocketManager.GAME_SEND_ECK_PACKET(this, 0, (id).toString())
                    SocketManager.GAME_SEND_ITEM_VENDOR_LIST_PACKET(this, npc.template.salesList(this.player))
                }
                
}
'1' -> {try {
                    id = (packet.substring(4)).toInt()
                    var target: Player? = World.world.getPlayer(id)

                    if (target == null || target.curMap != this.player.curMap || !target.isOnline) {
                        SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this, 'E')
                        return
                    }
                    if (target.away || this.player.away || target.exchangeAction != null || this.player.exchangeAction != null) {
                        SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this, 'O')
                        return
                    }
                    if (target.getGroup() != null && this.player.getGroup() == null) {
                        if (!target.getGroup()!!.isPlayer) {
                            SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this, 'E')
                            return
                        }
                    }
                    var exchangeAction: ExchangeAction<Int> = ExchangeAction(ExchangeAction.TRADING_WITH_PLAYER, id)
                    this.player.exchangeAction = exchangeAction
                    exchangeAction = ExchangeAction(ExchangeAction.TRADING_WITH_PLAYER, this.player.id)
                    target.exchangeAction = exchangeAction

                    this.player.craftingType.add(1)
                    target.craftingType.add(1)

                    SocketManager.GAME_SEND_EXCHANGE_REQUEST_OK(this, this.player.id, id, 1)
                    SocketManager.GAME_SEND_EXCHANGE_REQUEST_OK(target.getGameClient()!!, this.player.id, id, 1)
                } catch (e: NumberFormatException) {
                    log.error("unexpected error", e)
                }
                
}
'2' -> {id = (packet.substring(4)).toInt()
                if (this.player.curMap.getNpc(id) != null) {
                    var ech: NpcExchange = NpcExchange(this.player, this.player.curMap.getNpc(id)!!.template)

                    var exchangeAction: ExchangeAction<NpcExchange> = ExchangeAction(ExchangeAction.TRADING_WITH_NPC_EXCHANGE, ech)
                    this.player.exchangeAction = exchangeAction
                    SocketManager.GAME_SEND_ECK_PACKET(this.player, 2, (id).toString())
                }
                
}
'4' -> {id = (packet.split("|")[1]).toInt()

                var seller: Player? = World.world.getPlayer(id)
                if (seller == null || !seller.seeSeller || seller.curMap != this.player.curMap) return

                var exchangeAction: ExchangeAction<Int> = ExchangeAction(ExchangeAction.TRADING_WITH_OFFLINE_PLAYER, id)
                this.player.exchangeAction = exchangeAction

                SocketManager.GAME_SEND_ECK_PACKET(this.player, 4, (seller.id).toString())
                SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(seller, this.player)
                
}
'6' -> {exchangeAction = ExchangeAction(ExchangeAction.TRADING_WITH_ME, this.player.id)
                this.player.exchangeAction = exchangeAction

                SocketManager.GAME_SEND_ECK_PACKET(this.player, 6, "")
                SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(this.player, this.player)
                
}
'8' -> {var collector: Collector? = World.world.getCollector((packet.substring(4)).toInt())
                if (collector == null || collector.inFight > 0 || collector.exchange || collector.guildId != this.player.getGuild()!!.id || collector.map != this.player.curMap.id)
                    return
                if (!this.player.guildMember!!.canDo(Constant.G_COLLPERCO)) {
                    SocketManager.GAME_SEND_Im_PACKET(this.player, "1101")
                    return
                }
                collector.exchange = true
                exchangeAction = ExchangeAction(ExchangeAction.TRADING_WITH_COLLECTOR, collector.id)
                this.player.exchangeAction = exchangeAction
                this.player.DialogTimer()

                SocketManager.GAME_SEND_ECK_PACKET(this, 8, (collector.id).toString())
                SocketManager.GAME_SEND_ITEM_LIST_PACKET_PERCEPTEUR(this, collector)
                
}
'9' -> {id = (packet.substring(4)).toInt()

                if (this.player.curMap.getNpc(id) != null) {
                    var ech: PlayerExchange.NpcExchangePets = PlayerExchange.NpcExchangePets(this.player, this.player.curMap.getNpc(id)!!.template)
                    this.player.exchangeAction = ExchangeAction(ExchangeAction.TRADING_WITH_NPC_PETS, ech)
                    SocketManager.GAME_SEND_ECK_PACKET(this.player, 9, (id).toString())
                }
                
}
}
    }

    private fun sell(packet: String) {
        try {
            var infos: List<String> = packet.substring(2).split("|")
            var id: Int = (infos[0]).toInt()
            var quantity: Int = (infos[1]).toInt()

            if (!this.player.hasItemGuid(id)) {
                SocketManager.GAME_SEND_SELL_ERROR_PACKET(this)
                return
            }

            this.player.sellItem(id, quantity)
        } catch (e: Exception) {
            log.error("unexpected error", e)
            SocketManager.GAME_SEND_SELL_ERROR_PACKET(this)
        }
    }

    private fun bookOfArtisant(packet: String) {
        when (packet[2]) {
'F' -> {var Metier: Int = (packet.substring(3)).toInt()
                var cant: Int = 0
                for (artissant in  World.world.onlinePlayers) {
                    if (!artissant.metierPublic || artissant.metiers.isEmpty())
                        continue
                    var send: String = ""
                    var id: Int = artissant.id
                    var name: String = artissant.name
                    var color: String = artissant.color1.toString() + "," + artissant.color2.toString() + "," + artissant.color3.toString()
                    var accesoire: String = artissant.getGMStuffString()
                    var sex: Int = artissant.sexe
                    var map: Int = artissant.curMap!!.id
                    var inJob: Int = if ((map == 8731 || map == 8732)) 1 else 0
                    var classe: Int = artissant.classe
                    for (SM in  artissant.metiers.values) {
                        if (SM.template.id != Metier)
                            continue
                        cant++
                        send = "+" + SM.template.id.toString() + ";" + id.toString() + ";" + name + ";" + SM.get_lvl() + ";" + map + ";" + inJob + ";" + classe + ";" + sex + ";" + color + ";" + accesoire + ";" + SM.getOptBinValue() + "," + SM.slotsPublic
                        SocketManager.SEND_EJ_LIVRE(this.player, send)
                    }
                }
                if (cant == 0)
                    SocketManager.GAME_SEND_MESSAGE(this.player, this.player.getLang().trans("game.gameclient.bookofartisant.noartisan"))
                
}
}
    }

    private fun setPublicMode(packet: String) {
        when (packet[2]) {
'+' -> {this.player.metierPublic = true
                var metier: String = ""
                var first: Boolean = false
                for (SM in  this.player.metiers.values) {
                    SocketManager.SEND_Ej_LIVRE(this.player, "+" + SM.template.id)
                    if (first)
                        metier += ";"
                    metier += JobConstant.actionMetier(SM.template.id)
                    first = true
                }
                SocketManager.SEND_EW_METIER_PUBLIC(this.player, "+")
                SocketManager.SEND_EW_METIER_PUBLIC(this.player, "+" + this.player.id.toString() + "|" + metier)
                
}
'-' -> {this.player.metierPublic = false
                for (metiers in  this.player.metiers.values) {
                    SocketManager.SEND_Ej_LIVRE(this.player, "-" + metiers.template.id)
                }
                SocketManager.SEND_EW_METIER_PUBLIC(this.player, "-")
                SocketManager.SEND_EW_METIER_PUBLIC(this.player, "-" + this.player.id)
                
}
}
    }

    companion object {
    @JvmStatic fun leaveExchange(player: Player) {
        var exchangeAction: ExchangeAction<*> = player.exchangeAction!!

        if (exchangeAction == null)
            return

        when (exchangeAction.getType()) {
ExchangeAction.TRADING_WITH_PLAYER -> {if(exchangeAction.getValue() is Int) {
                    var target: Player? = World.world.getPlayer((exchangeAction.getValue() as Int))
                    if(target != null && target.exchangeAction != null && target.exchangeAction!!.getType() == ExchangeAction.TRADING_WITH_PLAYER) {
                        target.send("EV")
                        target.exchangeAction = null
                    }
                } else {
                    ((exchangeAction.getValue() as PlayerExchange)).cancel()
                }
                
}
ExchangeAction.TRADING_WITH_NPC_PETS -> {((exchangeAction.getValue() as PlayerExchange.NpcExchangePets)).cancel()
                
}
ExchangeAction.TRADING_WITH_NPC_EXCHANGE -> {((exchangeAction.getValue() as NpcExchange)).cancel()
                
}
ExchangeAction.CRAFTING_SECURE_WITH -> {if(exchangeAction.getValue() is Int) {
                    var target: Player? = World.world.getPlayer((exchangeAction.getValue() as Int))
                    if(target != null && target.exchangeAction != null && target.exchangeAction!!.getType() == ExchangeAction.CRAFTING_SECURE_WITH) {
                        target.send("EV")
                        target.exchangeAction = null
                    }
                } else {
                    ((exchangeAction.getValue() as CraftSecure)).cancel()
                }
                
}
ExchangeAction.CRAFTING -> {player.send("EV")
                player.doAction = false
                
}
ExchangeAction.BREAKING_OBJECTS -> {((exchangeAction.getValue() as BreakingObject)).isStop = true
                player.send("EV")
                
}
ExchangeAction.TRADING_WITH_NPC -> {player.send("EV")
                if (player.curMap.id == 10129){
                    player.exchangeAction = null
                    try {
                        player.getGameClient()!!.parsePacket("DC-1")
                        return
                    } catch (e: InterruptedException) {
                        log.error("unexpected error", e)
                    }
                }
                
}
ExchangeAction.IN_MOUNT -> {player.send("EV")
                
}
ExchangeAction.IN_MOUNTPARK -> {player.send("EV")
                var objects: ArrayList<GameObject> = ArrayList()
                for(obj in  player.objects.values) {
                    var mount: Mount? = World.world.getMountById(obj.stats.getEffect(995))

                    if(mount == null && obj.template!!.type == Constant.ITEM_TYPE_CERTIF_MONTURE)
                        objects.add(obj)
                }
                for(obj in  objects)
                    player.removeItem(obj.guid, obj.quantity, true, true)
                
}
ExchangeAction.IN_TRUNK -> {((exchangeAction.getValue() as Trunk)).player = null
                player.send("EV")
                
}
ExchangeAction.TRADING_WITH_COLLECTOR -> {var collector: Collector? = World.world.getCollector((exchangeAction.getValue() as Int))
                if (collector == null) return
                for (loc in  World.world.getGuild(collector.guildId)!!.getPlayers()) {
                    if (loc != null && loc.isOnline) {
                        SocketManager.GAME_SEND_gITM_PACKET(loc, org.starloco.locos.entity.Collector.parseToGuild(loc.getGuild()!!.id))
                        var str: String = ""
                        str += "G" + collector.getFullName()
                        str += "|.|" + World.world.getMap(collector.map)!!.x.toString() + "|" + World.world.getMap(collector.map)!!.y.toString() + "|"
                        str += player.name + "|" + (collector.xp)
                        if (!collector.getLogObjects().equals("")) str += collector.getLogObjects()
                        SocketManager.GAME_SEND_gT_PACKET(loc, str)
                    }
                }
                player.guildMember!!.giveXpToGuild(collector.xp)
                player.curMap.RemoveNpc(collector.id)
                SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(player.curMap, collector.id)
                collector.reloadTimer()
                collector.delCollector(collector.id)
                player.send("EV")
                DatabaseManager.get(CollectorData::class.java).delete(collector)
                
}
else -> {player.livreArti = false
                player.send("EV")
                
}
}


        player.exchangeAction = null
        DatabaseManager.get(PlayerData::class.java).update(player)
    }
    }

    /** Fin Exchange Packet **/

    /**
     * Emote Packet *
     */
    private fun parseEnvironementPacket(packet: String) {
        when (packet[1]) {
'D' -> {setDirection(packet)
                
}
'U' -> {useEmote(packet)
                
}
}
    }

    private fun setDirection(packet: String) {
        try {
            if (this.player.fight != null ||this.player.isDead().toInt() == 1)
                return
            var dir: Int = (packet.substring(2)).toInt()
            if (dir > 7 || dir < 0)
                return
            this.player.orientation = dir
            SocketManager.GAME_SEND_eD_PACKET_TO_MAP(this.player.curMap, this.player.id, dir)
        } catch (e: NumberFormatException) {
            log.error("unexpected error", e)
        }
    }

    private fun useEmote(packet: String) {
        var emote: Int = (packet.substring(2)).toInt()
        if (emote == -1)
            return
        if (!this::player.isInitialized)
            return
        if (this.player.fight != null)
            return;//Pas d'?mote en combat
        if (!this.player.emotes.contains(emote))
            return
        if (emote != 1 && emote != 19 && emote != 20 && this.player.sitted)
            this.player.setSitted(false)

        when (emote) {
20, 19, 1 -> {val party: Party? = this.player.party
                if(party != null && this.player.fight == null && party.master != null && this.player.id == party.master!!.id) {
                    TimerWaiter.addNext({ party.players.stream().filter({ follower1 -> party.isWithTheMaster(follower1, false, false) }).forEach { follower ->
                        follower.getGameClient()!!.useEmote("eU1")
                    } }, 0, TimeUnit.MILLISECONDS)
                }
                emote = 20
                this.player.setSitted(!this.player.sitted)
                
}
}

        if (this.player.emoteActive == 1 || this.player.emoteActive == 19 || this.player.emoteActive == 20)
            this.player.emoteActive = 0
        else
            this.player.emoteActive = emote

        var MP: MountPark? = this.player.curMap.mountPark
        SocketManager.GAME_SEND_eUK_PACKET_TO_MAP(this.player.curMap, this.player.id, this.player.emoteActive)
        if((emote == 2 || emote == 4 || emote == 3 || emote == 6 || emote == 8 || emote == 10) && MP != null)
        {
            val mounts: ArrayList<Mount> = ArrayList()
            for(id in  MP.getListOfRaising())  {
                var mount: Mount? = World.world.getMountById(id)
                if(mount != null)
                    if(mount.owner == this.player.id)
                        mounts.add(mount)
            }
            val player: Player = this.player
            if(mounts.isEmpty()) return
            val mount: Mount = mounts.get(Formulas.getRandomValue(0, mounts.size - 1))
            if(mounts.size > 0) {
                var cells: Int = 0
                when (emote) {
2, 4 -> {cells = 1
                        
}
3, 8 -> {cells = Formulas.getRandomValue(2, 3)
                        
}
6, 10 -> {cells = Formulas.getRandomValue(4, 7)
                        
}
}

                mount.moveMounts(player, cells, !(emote == 2 || emote == 3 || emote == 10))
            }
        }
    }

    /** Fin Emote Packet **/

    /**
     * Friend Packet *
     */
    private fun parseFrienDDacket(packet: String) {
        var packet = packet
        when (packet[1]) {
'A' -> {addFriend(packet)
                
}
'D' -> {removeFriend(packet)
                
}
'L' -> {SocketManager.GAME_SEND_FRIENDLIST_PACKET(this.player)
                
}
'O' -> {when (packet[2]) {
'-' -> {this.player.SetSeeFriendOnline(false)
                        SocketManager.GAME_SEND_BN(this.player)
                        
}
'+' -> {this.player.SetSeeFriendOnline(true)
                        SocketManager.GAME_SEND_BN(this.player)
                        
}
}
                
}
'J' -> {joinWife(packet)
                
}
}
    }

    private fun
        addFriend(packet: String) {
        var packet = packet
        if (!this::player.isInitialized)
            return
        var guid: Int = -1
        when (packet[2]) {
'%' -> {packet = packet.substring(3)
                var P: Player? = World.world.getPlayerByName(packet)
                if (P == null || !P.isOnline)//Si P est nul, ou si P est nonNul et P offline
                {
                    SocketManager.GAME_SEND_FA_PACKET(this.player, "Ef")
                    return
                }
                guid = P.accID
                
}
'*' -> {packet = packet.substring(3)
                var C: Account? = World.world.getAccountByPseudo(packet)
                if (C == null || !C.isOnline()) {
                    SocketManager.GAME_SEND_FA_PACKET(this.player, "Ef")
                    return
                }
                guid = C.id
                
}
else -> {packet = packet.substring(2)
                var Pr: Player? = World.world.getPlayerByName(packet)
                if (Pr == null || !Pr.isOnline)//Si P est nul, ou si P est nonNul et P offline
                {
                    SocketManager.GAME_SEND_FA_PACKET(this.player, "Ef")
                    return
                }
                guid = Pr.getAccount()!!.id
                
}
}
        if (guid == -1) {
            SocketManager.GAME_SEND_FA_PACKET(this.player, "Ef")
            return
        }
        account.addFriend(guid)
    }

    private fun removeFriend(packet: String) {
        var packet = packet
        if (!this::player.isInitialized)
            return
        var guid: Int = -1
        when (packet[2]) {
'%' -> {packet = packet.substring(3)
                var P: Player? = World.world.getPlayerByName(packet)
                if (P == null)//Si P est nul, ou si P est nonNul et P offline
                {
                    SocketManager.GAME_SEND_FD_PACKET(this.player, "Ef")
                    return
                }
                guid = P.accID
                
}
'*' -> {packet = packet.substring(3)
                var C: Account? = World.world.getAccountByPseudo(packet)
                if (C == null) {
                    SocketManager.GAME_SEND_FD_PACKET(this.player, "Ef")
                    return
                }
                guid = C.id
                
}
else -> {packet = packet.substring(2)
                var Pr: Player? = World.world.getPlayerByName(packet)
                if (Pr == null || !Pr.isOnline)//Si P est nul, ou si P est nonNul et P offline
                {
                    SocketManager.GAME_SEND_FD_PACKET(this.player, "Ef")
                    return
                }
                guid = Pr.getAccount()!!.id
                
}
}
        if (guid == -1 || !account.isFriendWith(guid)) {
            SocketManager.GAME_SEND_FD_PACKET(this.player, "Ef")
            return
        }
        account.removeFriend(guid)
    }

    private fun joinWife(packet: String) {
        var Wife: Player? = World.world.getPlayer(this.player.wife)
        if (Wife == null)
            return
        if (!Wife.isOnline) {
            if (Wife.sexe == 0)
                SocketManager.GAME_SEND_Im_PACKET(this.player, "140")
            else
                SocketManager.GAME_SEND_Im_PACKET(this.player, "139")

            SocketManager.GAME_SEND_FRIENDLIST_PACKET(this.player)
            return
        }
        when (packet[2]) {
'S' -> {if (Wife.curMap.data.noTp || Wife.curMap.haveMobFix()) {
                    SocketManager.GAME_SEND_MESSAGE(this.player, this.player.getLang().trans("game.gameclient.joinwife.no"))
                    return
                }
                if (this.player.fight != null)
                    return
                else
                    this.player.meetWife(Wife)
                
}
'C' -> {if (packet[3] == '+')//Si lancement de la traque
                {
                    if (this.player.follow != null)
                        this.player.follow!!.follower.remove(this.player.id)
                    SocketManager.GAME_SEND_FLAG_PACKET(this.player, Wife)
                    this.player.follow = Wife
                    Wife.follower[this.player.id] = this.player
                } else
                //On arrete de suivre
                {
                    SocketManager.GAME_SEND_DELETE_FLAG_PACKET(this.player)
                    this.player.follow = null
                    Wife.follower.remove(this.player.id)
                }
                
}
}
    }

    /** Fin Friend Packet **/

    /**
     * Fight Packet *
     */
    private fun parseFightPacket(packet: String) {
        try {
            when (packet[1]) {
'D' -> {var key: Int = -1
                    try {
                        key = (packet.substring(2).replace((0).toChar().toString(), "")).toInt()
                    } catch (e: Exception) {
                        log.error("unexpected error", e)
                    }
                    if (key == -1)
                        return
                    SocketManager.GAME_SEND_FIGHT_DETAILS(this, this.player.curMap.getFight(key)!!)
                    
}
'H' -> {if (this.player.fight == null)
                        return
                    this.player.fight!!.toggleHelp(this.player.id)
                    
}
'L' -> {SocketManager.GAME_SEND_FIGHT_LIST_PACKET(this, this.player.curMap)
                    
}
'N' -> {if (this.player.fight == null)
                        return
                    this.player.fight!!.toggleLockTeam(this.player.id)
                    
}
'P' -> {if (this.player.fight == null || this.player.party == null)
                        return
                    this.player.fight!!.toggleOnlyGroup(this.player.id)
                    
}
'S' -> {if (this.player.fight != null)
                        this.player.fight!!.toggleLockSpec(this.player)
                    
}
}
        } catch (e: Exception) {
            log.error("unexpected error", e)
        }
    }

    /** Fin Fight Packet **/

    /**
     * Game Packet *
     */
    private fun parseGamePacket(packet: String) {
        var c: Char = packet[1]
        when (c) {
'A' -> {if (this::player.isInitialized)
                    parseAction(packet)
                
}
'C' -> {if (this::player.isInitialized)
                    this.player.sendGameCreate()
                
}
'd' -> {showMonsterTarget(packet)
                
}
'f' -> {setFlag(packet)
                
}
'F' -> {if(this.player.energy > 0 &&this.player.isDead().toInt() == 0)
                    return
                this.player.setGhost()
                
}
'I' -> {getExtraInformations()
                
}
'K' -> {actionAck(packet)
                
}
'P' -> {this.player.toggleWings(packet[2])
                
}
'p' -> {setPlayerPosition(packet)
                
}
'Q' -> {leaveFight(packet)
                
}
'R' -> {readyFight(packet)
                
}
't' -> {if (this.player.fight != null)
                    this.player.fight!!.playerPass(this.player)
                
}
else -> {if(c.code == 1030) {
                    getExtraInformations()
                }
                
}
}
    }

    private @Synchronized fun parseAction(packet: String) {
        if (this.player.doAction) {
            SocketManager.GAME_SEND_GA_PACKET(this, "", "0", "", "")
            return
        }
        var actionID: Int
        try {
            actionID = (packet.substring(2, 5)).toInt()
        } catch (e: Exception) {
            log.error("unexpected error", e)
            return
        }
        var nextGameActionID: Int = 0

        if (actions.size > 0) {
            //On prend le plus haut GameActionID + 1
            nextGameActionID = actions.keys.toTypedArray()[actions.size - 1] + 1
        }
        var GA: GameAction = GameAction(nextGameActionID, actionID, packet)

        when (actionID){  1 -> {val party: Party? = this.player.party

                var oldMap: GameMap = this.player.curMap
                var oldCase: GameCase = this.player.curCell
                this.gameParseDeplacementPacket(GA)
                var newMap: GameMap = this.player.curMap
                var newCase: GameCase = this.player.curCell

                if(party != null && this.player.fight == null && party.master != null && party.master!!.name.equals(this.player.name)) {
                    var gm: StringBuilder = StringBuilder("GM")

                    TimerWaiter.addNext({
                        party.players.stream()
                            .filter({ follower1 -> party.isWithTheMaster(follower1, false, oldMap != newMap) })
                            .forEach { follower ->
                                    var client: GameClient? = follower.gameClient
                                    if (client != null && newMap.id == follower.curMap.id) {
                                        follower.curCell.removePlayer(follower)
                                        follower.curCell = newMap.getCase(newCase.getId())!!
                                        follower.curCell.addPlayer(follower)
                                        gm.append("|-").append(follower.id).append("|+").append(follower.parseToGM())
                                    } else if (oldMap.id == follower.curMap.id && oldMap.id != newMap.id && oldCase.getId() == follower.curCell.getId()) {
                                        follower.teleport(newMap.id, newCase.getId())
                                    }
                            }
                        oldMap.send(gm.toString())
                    }, 0, TimeUnit.MILLISECONDS)
                }

                
}
34 -> {var action: ExchangeAction<*> = player.exchangeAction!!
                if(action == null || action.getType() != ExchangeAction.READING_DOCUMENT) {
                    return
                }

                var qID: Int = (packet.substring(5)).toInt()
                var doc: DocumentActionData = (action.getValue() as DocumentActionData)
                doc.onQuestHRef(player, qID)
// fallthrough
gameTryCastSpell(packet)
                
}
300 -> {gameTryCastSpell(packet)
                
}
303 -> {gameTryCac(packet)
                
}
500 -> {gameAction(GA)
                this.player.gameAction = GA
                
}
507 -> {houseAction(packet)
                
}
512 -> {if (this.player.alignment == Constant.ALIGNEMENT_NEUTRE)
                    return
                this.player.openPrismeMenu()
                
}
618 -> {this.player.setisOK((packet.substring(5, 6)).toInt())
                SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(this.player.curMap, "", this.player.id, this.player.name, "game.gameclient.maried.yes")
                var boy: Player = (this.player.curMap.getCase(282)!!.players.toTypedArray()[0] as Player)
                var girl: Player = (this.player.curMap.getCase(297)!!.players.toTypedArray()[0] as Player)

                if (girl.isOK > 0 && boy.isOK > 0)
                    World.world.wedding(girl, boy, 1)
                else
                    World.world.priestRequest(boy, girl, if (this.player == boy) girl else boy)
                
}
619 -> {this.player.setisOK(0)
                SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(this.player.curMap, "", this.player.id, this.player.name, "game.gameclient.maried.no")
                var boy: Player = (this.player.curMap.getCase(282)!!.players.toTypedArray()[0] as Player)
                var girl: Player = (this.player.curMap.getCase(297)!!.players.toTypedArray()[0] as Player)

                World.world.wedding(girl, boy, 0)
                
}
900 -> {if (Main.fightAsBlocked)
                    return
                gameAskDuel(packet)
                
}
901 -> {if (Main.fightAsBlocked)
                    return
                gameAcceptDuel(packet)
                
}
902 -> {if (Main.fightAsBlocked)
                    return
                gameCancelDuel(packet)
                
}
903 -> {gameJoinFight(packet)
                
}
906 -> {if (Main.fightAsBlocked)
                    return
                gameAggro(packet)
                
}
909 -> {if (Main.fightAsBlocked)
                    return
                /*long calcul = System.currentTimeMillis() - Config.startTime;
                if(calcul < 600000) {
                    this.player.sendMessage("Vous devez attendre encore " + ((600000 - calcul) / 60000) + " minute(s).");
                    return;
                }*/
                gameCollector(packet)
                
}
912 -> {if (Main.fightAsBlocked)
                    return
                var calcul: Long = System.currentTimeMillis() - Config.startTime
                if(calcul < 600000) {
                    this.player.sendMessage(player.getLang().trans("game.gameclient.prisme.attack.wait", (600000 - calcul) / 60000))
                    return
                }

                gamePrism(packet)
                
}
}
    }

    private fun gameParseDeplacementPacket(GA: GameAction) {
        var path: String = GA.packet!!.substring(5)

        if (this.player.fight == null) {
            if (this.player.isBlocked) {
                SocketManager.GAME_SEND_GA_PACKET(this, "", "0", "", "")
                removeAction(GA)
                return
            }
            if (this.player.isDead().toInt() == 1) {
                SocketManager.GAME_SEND_BN(this.player)
                removeAction(GA)
                return
            }
            if (this.player.doAction) {
                SocketManager.GAME_SEND_GA_PACKET(this, "", "0", "", "")
                removeAction(GA)
                return
            }
            if (this.player.mount != null && !this.player.isGhost) {
                if (!this.player.morphMode && (this.player.getPodUsed() > this.player.getMaxPod() || this.player.mount!!.actualPods > this.player.mount!!.maxPods)) {
                    SocketManager.GAME_SEND_Im_PACKET(this.player, "112")
                    SocketManager.GAME_SEND_GA_PACKET(this, "", "0", "", "")
                    removeAction(GA)
                    return
                }
            }
            if (this.player.getPodUsed() > this.player.getMaxPod() && !this.player.isGhost
                    && !this.player.morphMode) {
                SocketManager.GAME_SEND_Im_PACKET(this.player, "112")
                SocketManager.GAME_SEND_GA_PACKET(this, "", "0", "", "")
                removeAction(GA)
                return
            }
            //Si d?placement inutile
            var targetCell: GameCase? = this.player.curMap.getCase(World.world.cryptManager.cellCode_To_ID(path.substring(path.length - 2)))

            if (targetCell == null || !targetCell.isWalkable(false)) {
                SocketManager.GAME_SEND_GA_PACKET(this, "", "0", "", "")
                removeAction(GA)
                return
            }
            if(this.player.curMap.id == 6824 && this.player.start != null && targetCell.getId() == 325 && !this.player.start!!.leave) {
                this.player.start!!.leave = true
                SocketManager.GAME_SEND_GA_PACKET(this, "", "0", "", "")
                removeAction(GA)
                return
            }

            var pathRef: AtomicReference<String> = AtomicReference(path)
            var result: Int = PathFinding.isValidPath(this.player.curMap, this.player.curCell.getId(), pathRef, null, this.player, targetCell.getId())

            if (result <= -9999) {
                result += 10000
                GA.tp = true
            }
            if (result == 0) {
                SocketManager.GAME_SEND_GA_PACKET(this, "", "0", "", "")
                removeAction(GA)

                // Maybe the player is right next to an Object and want to use it
                var allowSkill0: Boolean = Optional.ofNullable(this.player.curMap.data.interactiveObjects[targetCell.getId()])
                        .flatMap(World.world::getObjectBySprite)
                        .map({ io -> io.allowSkill(0) })
                        .orElse(false)

                if(allowSkill0) {
                    DataScriptVM.getInstance()!!.handlers.onSkillUse(player, targetCell.getId(), 0)
                }
                return
            }
            if (result != -1000 && result < 0)
                result = -result

            //On prend en compte le nouveau path
            path = pathRef.get()
            //Si le path est invalide
            if (result == -1000)
                path = CryptManager.getHashedValueByInt(this.player.orientation) + CryptManager.cellID_To_Code(this.player.curCell.getId())

            //On sauvegarde le path dans la variable
            GA.args = path
            if (this.player.walkFast) {
                this.player.curCell.removePlayer(this.player)
                SocketManager.GAME_SEND_BN(this)
                //On prend la case ciblee
                var nextCell: GameCase = this.player.curMap.getCase(World.world.cryptManager.cellCode_To_ID(path.substring(path.length - 2)))!!
                targetCell = this.player.curMap.getCase(World.world.cryptManager.cellCode_To_ID(GA.packet!!.substring(GA.packet!!.length - 2)))!!

                //On definie la case et on ajoute le personnage sur la case
                this.player.curCell = nextCell
                this.player.orientation = CryptManager.getIntByHashedValue(path[path.length - 3])
                this.player.curCell.addPlayer(this.player)
                if (!this.player.isGhost)
                    this.player.away = false

                SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.player.curMap, this.player.id)
                SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(this.player.curMap, this.player)
                this.player.curMap.onPlayerArriveOnCell(this.player, this.player.curCell.getId())

                SocketManager.GAME_SEND_GA_PACKET(this, "", "0", "", "")
                this.player.refreshCraftSecure(true)

                removeAction(GA)
                return
            } else {
                SocketManager.GAME_SEND_GA_PACKET_TO_MAP(this.player.curMap, "" + GA.id, 1, this.player.id, "a" + CryptManager.cellID_To_Code(this.player.curCell.getId()) + path)
            }

            this.addAction(GA)
            this.player.setSitted(false)
            this.player.away = true
        } else {
            val fighter: Fighter? = this.player.fight!!.getFighterByPerso(this.player)
            if (fighter != null) {
                GA.args = path
                this.player.fight!!.cast(this.player.fight!!.getFighterByPerso(this.player)!!, {  -> this.player.fight!!.onFighterMovement(fighter!!, GA) }, null)
            }
        }
    }

    private fun gameTryCastSpell(packet: String) {
        try {
            var split: List<String> = packet.split(";")

            if(packet.contains("undefined") || split.size != 2)
                return

            val id: Int = (split[0].substring(5)).toInt()
            val cellId: Int = (split[1]).toInt()
            val fight: Fight? = this.player.fight

            if (fight != null) {
                var SS: Spell.SortStats? = this.player.getSortStatBySortIfHas(id)

                if (SS != null)
                    if(this.player.fight!!.curAction.isEmpty())
                        this.player.fight!!.cast(this.player.fight!!.getFighterByPerso(this.player)!!, {  -> this.player.fight!!.tryCastSpell(this.player.fight!!.getFighterByPerso(this.player)!!, SS, cellId) }, SS)
            }
        } catch (e: NumberFormatException) {
            log.error(packet + "\n" + e)
        }
    }

    private fun gameTryCac(packet: String) {
        try {
            if(packet.contains("undefined")) return
            val cell: Int = (packet.substring(5)).toInt()
            if (this.player.fight != null && this.player.fight!!.curAction.isEmpty())
                this.player.fight!!.cast(this.player.fight!!.getFighterByPerso(this.player)!!, {  -> this.player.fight!!.tryCaC(this.player, cell) }, null)
        } catch (e: Exception) {
            log.error("unexpected error", e)
        }
    }

    private @Synchronized fun gameAction(GA: GameAction) {
        var packet: String = GA.packet!!.substring(5)
        var cellID: Int = -1
        var actionID: Int = -1

        try {
            cellID = (packet.split(";")[0]).toInt()
            actionID = (packet.split(";")[1]).toInt()
        } catch (e: Exception) {
            log.error("unexpected error", e)
        }

        if (walk) {
            actions[-1] = GA
            return
        }

        //Si packet invalide, ou cellule introuvable
        if (cellID == -1 || actionID == -1 || !this::player.isInitialized || !this.player.hasMap() || this.player.curMap.getCase(cellID) == null)
            return

        GA.args = cellID.toString() + ";" + actionID
        this.player.getGameClient()!!.addAction(GA)
        if (this.player.isDead().toInt() == 0)
            this.player.startActionOnCell(GA)
    }

    private fun houseAction(packet: String) {
        var actionID: Int = (packet.substring(5)).toInt()
        var h: House? = this.player.curHouse
        if (h == null)
            return
        when (actionID){  81 -> {h.lock(this.player)
                
}
97 -> {h.buyIt(this.player)
                
}
98, 108 -> {h.sellIt(this.player)
                
}
}
    }

    private fun gameAskDuel(packet: String) {

        try {
            if (this.player.cantDefie())
                return
            var guid: Int = (packet.substring(5)).toInt()
            var target: Player? = World.world.getPlayer(guid)
            if (target == null)
                return
            if(this.player.stalk != null && player.stalk!!.onPlayerTryToFight(player, target))
                return
            if (player.curMap.subArea != null && player.curMap.subArea!!.area!!.superArea == 3) {
                if (((player.alignment != 0 && (!player.curMap.data.noAgro)) || target.deshonor > 0)) {
                    if (!target.isOnline || target.fight != null || target.curMap.id
                            != this.player.curMap.id || target.alignment == this.player.alignment || this.player.
                            curMap.places.size < 2 || !target.canAggro() ||target.isDead().toInt() == 1
                            || this.player.fight != null ||player.isDead().toInt() == 1 || player.isGhost || target.isGhost)
                        return

                    this.clearAllPanels(this.player)
                    this.clearAllPanels(target)
                    SocketManager.GAME_SEND_GA_PACKET_TO_MAP(this.player.curMap, "", 906, this.player.id, target.id.toString() + "")
                    this.player.curMap.newFight(this.player, target, Constant.FIGHT_TYPE_AGRESSION)
                    return
                }
            }
            if (this.player.curMap.places.size < 2) {
                SocketManager.GAME_SEND_DUEL_Y_AWAY(this, this.player.id)
                return
            }
            if (this.player.away || this.player.fight != null ||this.player.isDead().toInt() == 1) {
                SocketManager.GAME_SEND_DUEL_Y_AWAY(this, this.player.id)
                return
            }
            if (this.player.isMissingSubscription()) {
                SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this.player.getGameClient()!!, 'S')
                return
            }
            if (target.isMissingSubscription()) {
                SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this.player.getGameClient()!!, 'S')
                return
            }
            if (target.away || target.fight != null || target.curMap.id != this.player.curMap.id ||target.isDead().toInt() == 1 || target.exchangeAction != null || this.player.exchangeAction != null) {
                SocketManager.GAME_SEND_DUEL_E_AWAY(this, this.player.id)
                return
            }
            this.player.duelId = guid
            this.player.away = true
            World.world.getPlayer(guid)!!.duelId = this.player.id
            World.world.getPlayer(guid)!!.away = true
            SocketManager.GAME_SEND_MAP_NEW_DUEL_TO_MAP(this.player.curMap, this.player.id, guid)
        } catch (e: NumberFormatException) {
            log.error("unexpected error", e)
        }
    }

    private fun gameAcceptDuel(packet: String) {
        if (this.player.cantDefie())
            return
        var guid: Int = -1
        try {
            guid = (packet.substring(5)).toInt()
        } catch (e: NumberFormatException) {
            log.error("unexpected error", e)
            return
        }
        if (this.player.duelId != guid || this.player.duelId == -1 ||this.player.isDead().toInt() == 1)
            return
        var duel: Int = this.player.duelId
        var player: Player = World.world.getPlayer(duel)!!

        SocketManager.GAME_SEND_MAP_START_DUEL_TO_MAP(this.player.curMap, duel, this.player.id)
        this.clearPanelsForPlayer(player)
        var fight: Fight? = this.player.curMap.newFight(World.world.getPlayer(duel)!!, this.player, Constant.FIGHT_TYPE_CHALLENGE)

        this.player.fight = fight
        this.player.away = false
        player.fight = fight
        player.away = false
    }

    private fun gameCancelDuel(packet: String) {
        try {
            if (this.player.duelId == -1)
                return
            SocketManager.GAME_SEND_CANCEL_DUEL_TO_MAP(this.player.curMap, this.player.duelId, this.player.id)
            var player: Player = World.world.getPlayer(this.player.duelId)!!
            player.away = false
            player.duelId = -1
            this.player.away = false
            this.player.duelId = -1
        } catch (e: NumberFormatException) {
            log.error("unexpected error", e)
        }
    }

    private fun gameJoinFight(packet: String) {
        if (this.player.fight != null)
            return
        if (this.player.isDead().toInt() == 1)
            return

        var infos: List<String> = packet.substring(5).split(";")
        if (infos.size == 1) {
            try {
                var F: Fight? = this.player.curMap.getFight((infos[0]).toInt())
                if (F != null)
                    F.joinAsSpectator(this.player)
            } catch (e: Exception) {
                log.error("unexpected error", e)
            }
        } else {
            try {
                var guid: Int = (infos[1]).toInt()
                if (this.player.away) {
                    SocketManager.GAME_SEND_GA903_ERROR_PACKET(this, 'o', guid)
                    return
                }
                var player: Player? = World.world.getPlayer(guid)
                var fight: Fight? = null

                if (player == null) {
                    var prism: Prism? = World.world.getPrisme(guid)
                    if(prism != null)
                        fight = prism.fight
                } else {
                    fight = player.fight
                }
                if (fight == null)
                    return
                if (fight!!.state > 2) {
                    SocketManager.GAME_SEND_Im_PACKET(this.player, "191")
                    return
                }
                if (this.player.isMissingSubscription()) {
                    SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this.player.getGameClient()!!, 'S')
                    return
                }

                this.clearAllPanels(null)
                if(fight!!.prism != null)
                    fight!!.joinPrismFight(this.player, (if (guid in fight.team0) 0 else 1))
                else {
                    fight!!.joinFight(this.player, guid)

                    val party: Party? = if (!this::player.isInitialized) null else this.player.party
                    val f: Fight = fight
                    if(party != null && party.master != null && party.master!!.name.equals(this.player.name)) {
                        party.players.stream().filter({ follower -> party.isWithTheMaster(follower, false, false) }).forEach { follower ->
                            TimerWaiter.addNext({ f.joinFight(follower, this.player.id) }, follower.party!!.getOptionByPlayer(follower)!!.second.toLong(), TimeUnit.SECONDS)
                        }
                    }
                }
            } catch (e: Exception) {
                log.error("unexpected error", e)
            }
        }
    }

    private fun gameAggro(packet: String) {
        try {
            if (!this::player.isInitialized || this.player.fight != null || this.player.isGhost ||this.player.isDead().toInt() == 1 || this.player.cantAgro())
                return
            if (this.player.isMissingSubscription()) {
                SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this.player.getGameClient()!!, 'S')
                return
            }
            var target: Player? = World.world.getPlayer((packet.substring(5)).toInt())

            if (target == null || !target.isOnline || target.fight != null || target.curMap.id
                    != this.player.curMap.id || target.alignment == this.player.alignment || this.player.
                    curMap.places.size < 2 || !target.canAggro() ||target.isDead().toInt() == 1)
                return

            var area: Area? = this.player.curMap.area

            if(area != null && area.id == 42)
                return
            if (target.isMissingSubscription()) {
                SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(target.getGameClient()!!, 'S')
                return
            }
            if (target.alignment == 0) {
                this.player.deshonor = this.player.deshonor + 1
                SocketManager.GAME_SEND_Im_PACKET(this.player, "084;1")
            }

            this.clearAllPanels(target)
            if(!this.player.showWings)
                this.player.toggleWings('+')
            SocketManager.GAME_SEND_GA_PACKET_TO_MAP(this.player.curMap, "", 906, this.player.id, target.id.toString() + "")
            this.player.curMap.newFight(this.player, target, Constant.FIGHT_TYPE_AGRESSION)
        } catch (e: Exception) {
            log.error("unexpected error", e)
        }
    }

    private fun gameCollector(packet: String) {
        try {
            if (!this::player.isInitialized)
                return
            if (this.player.fight != null)
                return
            if (this.player.exchangeAction != null ||this.player.isDead().toInt() == 1 || this.player.away)
                return

            var id: Int = (packet.substring(5)).toInt()
            var target: Collector? = World.world.getCollector(id)

            if (target == null || target.inFight > 0)
                return
            if (this.player.curMap.id != target.map)
                return
            if (target.exchange) {
                SocketManager.GAME_SEND_Im_PACKET(this.player, "1180")
                return
            }

            this.clearAllPanels(null)
            SocketManager.GAME_SEND_GA_PACKET_TO_MAP(this.player.curMap, "", 909, this.player.id, id.toString() + "")
            this.player.curMap.startFightVersusPercepteur(this.player, target)
        } catch (e: Exception) {
            log.error("unexpected error", e)
        }
    }

    private fun gamePrism(packet: String) {
        try {
            if (this.player.isGhost || this.player.fight != null || this.player.exchangeAction != null || this.player.alignment == 0 ||this.player.isDead().toInt() == 1)
                return
            var id: Int = (packet.substring(5)).toInt()
            var prism: Prism = World.world.getPrisme(id)!!
            if (prism.fight != null || prism.map != this.player.curMap.id)
                return
            if(prism.state == Prism.NEW) {
                player.send("Im1154")
            }
            if(!prism.subArea.ownNearestSubArea(player)) {
                player.send("Im1157")
                return
            }
            this.clearAllPanels(null)
            SocketManager.SEND_GA_ACTION_TO_Map(this.player.curMap, "", 909, this.player.id.toString() + "", id.toString() + "")
            this.player.curMap.startFightVersusPrisme(this.player, prism)
        } catch (e: Exception) {
            log.error("unexpected error", e)
        }
    }

    //TODO: Change the duel asking method by ExchangeAction (better way)
    fun clearAllPanels(target: Player?) {
        this.clearPanelsForPlayer(this.player)
        this.clearPanelsForPlayer(target)
    }

    private fun clearPanelsForPlayer(player: Player?) {
        if(player != null) {
            if(player.exchangeAction != null) {
                player.exchangeAction = null
                player.send("EV")
            }
            if(player.duelId != -1) {
                var target: Player? = World.world.getPlayer(player.duelId)
                if(target != null) {
                    target.away = false
                    target.duelId = -1
                    SocketManager.GAME_SEND_CANCEL_DUEL_TO_MAP(target.curMap, target.duelId, target.id)
                }
                player.away = false
                player.duelId = -1
                SocketManager.GAME_SEND_CANCEL_DUEL_TO_MAP(player.curMap, player.duelId, player.id)
            }
        }
    }

    private fun showMonsterTarget(packet: String) {
        var chalID: Int = 0
        chalID = (packet.split("i")[1]).toInt()
        if (chalID != 0 && this.player.fight != null) {
            var fight: Fight? = this.player.fight
            if (chalID in fight!!.allChallenges)
                fight!!.allChallenges[chalID]!!.showCibleToPerso(this.player)
        }
    }

    private fun setFlag(packet: String) {
        if (!this::player.isInitialized)
            return
        if (this.player.fight == null)
            return
        var cellID: Int = -1
        try {
            cellID = (packet.substring(2)).toInt()
        } catch (e: Exception) {
            log.error("unexpected error", e)
        }
        if (cellID == -1)
            return
        this.player.fight!!.showCaseToTeam(this.player.id, cellID)
    }

    private fun getExtraInformations() {
        try {
            if (this::player.isInitialized && this.player.lastFight != null) {
                if (player.applyEndFightAction()) {
                    return
                }
                player.curMap.applyEndFightAction(player)
                player.setLastFightForEndFightAction(null)
            }
            sendExtraInformations()

        } catch (e: Exception) {
            Main.logger.error("getExtraInformations", e)
        }
    }

    private fun sendExtraInformations() {
        try {
            if(!this::player.isInitialized) return
            if (this.player.fight != null && !this.player.fight!!.isFinish()) {
                //Only Collector
                SocketManager.GAME_SEND_MAP_GMS_PACKETS(this.player.fight!!.map!!, this.player)
                SocketManager.GAME_SEND_GDK_PACKET(this)
                if (this.player.fight!!.onPlayerReconnection(this.player))
                    return
            }

            //Objets sur la Map
            SocketManager.GAME_SEND_MAP_GMS_PACKETS(this.player.curMap, this.player)
            SocketManager.GAME_SEND_GDK_PACKET(this)
            SocketManager.GAME_SEND_MAP_NPCS_GMS_PACKETS(player.getGameClient()!!, player.curMap)
            SocketManager.GAME_SEND_MAP_MOBS_GMS_PACKETS(player.getGameClient()!!, player.curMap)
            SocketManager.GAME_SEND_MAP_OBJECTS_GDS_PACKETS(player.getGameClient()!!, player.curMap)

            // Prism
            SocketManager.SEND_GM_PRISME_TO_MAP(this, this.player.curMap)
            // Merchant
            SocketManager.GAME_SEND_MERCHANT_LIST(this.player)
            // Houses
            World.world.houseManager.load(this.player, this.player.curMap.id)
            // Collector
            SocketManager.GAME_SEND_MAP_PERCO_GMS_PACKETS(this, this.player.curMap)
            // Mount park
            SocketManager.GAME_SEND_Rp_PACKET(this.player, this.player.curMap.mountPark)
            // Mount park objects
            SocketManager.GAME_SEND_GDO_OBJECT_TO_MAP(this, this.player.curMap)
            SocketManager.GAME_SEND_GM_MOUNT(this, this.player.curMap, true)

            //Les drapeau de combats
            SocketManager.GAME_SEND_MAP_FIGHT_COUNT(this, this.player.curMap)
            Fight.FightStateAddFlag(this.player.curMap, this.player)

            //items au sol
            this.player.curMap.sendFloorItems(this.player)
            // Cell overrides
            this.player.curMap.sendOverrides(this.player)
            // Anim states
            this.player.curMap.sendAnimStates(this.player)

            AuctionManager.getInstance().onPlayerLoadMap(player)
            World.world.showPrismes(this.player)
            this.player.refreshCraftSecure(false)
            this.player.afterFight = false
        } catch (e: Exception) {
            log.error("unexpected error", e)
        }
    }

    private fun actionAck(packet: String) {
        var id: Int = -1
        var infos: List<String> = packet.substring(3).split("|")
        try {
            id = (infos[0]).toInt()
        } catch (e: Exception) {
            log.error("unexpected error", e)
            return
        }
        if (id == -1)
            return
        var GA: GameAction? = actions[id]

        if (GA == null)
            return
        var isOk: Boolean = packet[2] == 'K'
        when (GA.actionId){  1 -> {if (isOk) {
                    if (this.player.fight == null) {
                        assert(this.player.hasCell())
                        val party: Party? = this.player.party

                        if(party != null && this.player.fight == null && party.master != null && party.master!!.name.equals(this.player.name)) {
                            TimerWaiter.addNext({ party.players.stream()
                                    .filter({ follower1 -> party.isWithTheMaster(follower1, false, false) })
                                    .forEach({ follower -> follower.gameClient!!.actionAck(packet) }) }, 0, TimeUnit.MILLISECONDS)
                        }

                        this.player.curCell.removePlayer(this.player)
                        SocketManager.GAME_SEND_BN(this)
                        var path: String = GA.args!!
                        //On prend la case cibl?e

                        var nextCell: GameCase = this.player.curMap.getCase(World.world.cryptManager.cellCode_To_ID(path.substring(path.length - 2)))!!
                        var targetCellID: Int = World.world.cryptManager.cellCode_To_ID(GA.packet!!.substring(GA.packet!!.length - 2))

                        //On d?finie la case et on ajoute le personnage sur la case
                        this.player.curCell = nextCell
                        this.player.orientation = CryptManager.getIntByHashedValue(path[path.length - 3])
                        this.player.curCell.addPlayer(this.player)
                        if (!this.player.isGhost)
                            this.player.away = false
                        this.player.curMap.onPlayerArriveOnCell(this.player, this.player.curCell.getId())


                        if (GA.tp) {
                            GA.tp = false
                            this.player.teleport(9864, 265)
                            return
                        }

                        // TODO: Maybe check that the player is still where we think he is
                        // Maybe the player is right next to an Object and want to use it
                        var allowSkill0: Boolean = Optional.ofNullable(player.curMap.data.interactiveObjects[targetCellID])
                                .flatMap(World.world::getObjectBySprite)
                                .map({ io -> io.allowSkill(0) })
                                .orElse(false)

                        if(allowSkill0) {
                            DataScriptVM.getInstance()!!.handlers.onSkillUse(player, targetCellID, 0)
                        }
                    } else {
                        this.player.fight!!.onGK(this.player)
                        return
                    }
                } else {
                    //Si le joueur s'arrete sur une case
                    var newCellID: Int = -1
                    try {
                        newCellID = (infos[1]).toInt()
                    } catch (e: Exception) {
                        log.error("unexpected error", e)
                        return
                    }
                    if (newCellID == -1)
                        return

                    var path: String = GA.args!!
                    this.player.curCell.removePlayer(this.player)
                    this.player.curCell = this.player.curMap.getCase(newCellID)!!
                    this.player.orientation = CryptManager.getIntByHashedValue(path[path.length - 3])
                    this.player.curCell.addPlayer(this.player)
                    SocketManager.GAME_SEND_BN(this)
                    if (GA.tp) {
                        GA.tp = false
                        this.player.teleport(9864, 265)
                        return
                    }
                }
                
}
500 -> {this.player.finishActionOnCell(GA)
                this.player.gameAction = null
                
}
}
        removeAction(GA)
    }

    private fun setPlayerPosition(packet: String) {
        if (this.player.fight == null)
            return
        try {
            var cell: Int = (packet.substring(2)).toInt()
            this.player.fight!!.exchangePlace(this.player, cell)
        } catch (e: NumberFormatException) {
            log.error("unexpected error", e)
        }
    }

    private fun leaveFight(packet: String) {
        var id: Int = 0

        if (!packet.substring(2).isEmpty()) {
            try {
                id = (packet.substring(2)).toInt()
            } catch (e: Exception) {
                log.error("unexpected error", e)
            }
        }

        var fight: Fight? = this.player.fight

        if (fight == null || id < 0)
            return

        if (id > 0) {
            val target: Player? = World.world.getPlayer(id)

            if (target == null || target.fight == null)
                return
            if(fight.getTeamId(target.id) != fight.getTeamId(this.player.id))
                return

            if ((fight.hasInit0() && target == fight.init0.player) || (fight.hasInit1() && target == fight.init1.player) || target == this.player)
                return

            fight.leftFight(this.player, target)
        } else {
            fight.leftFight(this.player, null)
        }
    }

    private fun readyFight(packet: String) {
        if (this.player.fight == null)
            return
        if (this.player.fight!!.state != Constant.FIGHT_STATE_PLACE)
            return

        this.player.ready = packet.substring(2).equals("1", ignoreCase = true)
        this.player.fight!!.verifIfAllReady()
        SocketManager.GAME_SEND_FIGHT_PLAYER_READY_TO_FIGHT(this.player.fight!!, 3, this.player.id, packet.substring(2).equals("1", ignoreCase = true))

        val party: Party? = this.player.party

        if(party != null && party.master != null && party.master!!.name.equals(this.player.name)) {
            TimerWaiter.addNext({ party.players.stream()
                    .filter({ follower -> party.isWithTheMaster(follower, true, false) })
                    .forEach({ follower -> follower.gameClient!!.readyFight(packet) }) }, 1, TimeUnit.SECONDS)
        }
    }

    /** Fin Game Packet **/

    /**
     * Guild Packet *
     */
    private fun parseGuildPacket(packet: String) {
        when (packet[1]) {
'B' -> {boostCaracteristique(packet)
                
}
'b' -> {boostSpellGuild(packet)
                
}
'C' -> {createGuild(packet)
                
}
'f' -> {teleportToGuildFarm(packet.substring(2))
                
}
'F' -> {removeTaxCollector(packet.substring(2))
                
}
'h' -> {teleportToGuildHouse(packet.substring(2))
                
}
'H' -> {placeTaxCollector()
                
}
'I' -> {getInfos(packet[2])
                
}
'J' -> {invitationGuild(packet.substring(2))
                
}
'K' -> {banToGuild(packet.substring(2))
                
}
'P' -> {changeMemberProfil(packet.substring(2))
                
}
'T' -> {joinOrLeaveTaxCollector(packet.substring(2))
                
}
'V' -> {leavePanelGuildCreate()
                
}
}
    }

    private fun boostCaracteristique(packet: String) {
        if (this.player.getGuild() == null)
            return
        var G: Guild = this.player.getGuild()!!
        if (!this.player.guildMember!!.canDo(Constant.G_BOOST))
            return
        when (packet[2]) {
'p' -> {if (G.capital < 1)
                    return
                if (G.getStats(176) >= 500)
                    return
                G.capital = G.capital - 1
                G.upgradeStats(176, 1)
                
}
'x' -> {if (G.capital < 1)
                    return
                if (G.getStats(124) >= 400)
                    return
                G.capital = G.capital - 1
                G.upgradeStats(124, 1)
                
}
'o' -> {if (G.capital < 1)
                    return
                if (G.getStats(158) >= 5000)
                    return
                G.capital = G.capital - 1
                G.upgradeStats(158, 20)
                
}
'k' -> {if (G.capital < 10)
                    return
                if (G.nbCollectors >= 50)
                    return
                G.capital = G.capital - 10
                G.nbCollectors = G.nbCollectors + 1
                
}
}
        DatabaseManager.get(GuildData::class.java).update(G)
        SocketManager.GAME_SEND_gIB_PACKET(this.player, this.player.getGuild()!!.parseCollectorToGuild())
    }

    private fun boostSpellGuild(packet: String) {
        if (this.player.getGuild() == null)
            return
        var G2: Guild = this.player.getGuild()!!
        if (!this.player.guildMember!!.canDo(Constant.G_BOOST))
            return
        var spellID: Int = (packet.substring(2)).toInt()
        if (spellID in G2.spells) {
            if (G2.capital < 5)
                return
            G2.capital = G2.capital - 5
            G2.boostSpell(spellID)
            DatabaseManager.get(GuildData::class.java).update(G2)
            SocketManager.GAME_SEND_gIB_PACKET(this.player, this.player.getGuild()!!.parseCollectorToGuild())
        } else {
            GameServer.a()
        }
    }

    private fun createGuild(packet: String) {
        if (!this::player.isInitialized)
            return
        if (this.player.getGuild() != null || this.player.guildMember != null) {
            SocketManager.GAME_SEND_gC_PACKET(this.player, "Ea")
            return
        }
        if (this.player.fight != null || this.player.away)
            return
        try {
            var infos: List<String> = packet.substring(2).split("|")
            //base 10 => 36
            var bgID: String = Integer.toString((infos[0]).toInt(), 36)
            var bgCol: String = Integer.toString((infos[1]).toInt(), 36)
            var embID: String = Integer.toString((infos[2]).toInt(), 36)
            var embCol: String = Integer.toString((infos[3]).toInt(), 36)
            var name: String = infos[4]
            if (World.world.guildNameIsUsed(name)) {
                SocketManager.GAME_SEND_gC_PACKET(this.player, "Ean")
                return
            }

            //Validation du nom de la guilde
            var tempName: String = name.lowercase()
            var isValid: Boolean = true
            //V?rifie d'abord si il contient des termes d?finit
            if (tempName.length > 20 || tempName.contains("mj")
                    || tempName.contains("modo") || tempName.contains("fuck")
                    || tempName.contains("admin")) {
                isValid = false
            }
            //Si le nom passe le test, on v?rifie que les caract?re entr? sont correct.
            if (isValid) {
                var tiretCount: Int = 0
                for (curLetter in  tempName.toCharArray()) {
                    if (!((curLetter >= 'a' && curLetter <= 'z') || curLetter >= 'A' && curLetter <= 'Z' || curLetter == '-' || curLetter == ' ')) {
                        if(curLetter == '\'') continue
                        isValid = false
                        break
                    }
                    if (curLetter == '-') {
                        if (tiretCount >= 3) {
                            isValid = false
                            break
                        } else {
                            tiretCount++
                        }
                    }
                    if (curLetter == ' ') {
                        if (tiretCount >= 3) {
                            isValid = false
                            break
                        } else {
                            tiretCount++
                        }
                    }
                }
            }
            //Si le nom est invalide
            if (!isValid) {
                SocketManager.GAME_SEND_gC_PACKET(this.player, "Ean")
                return
            }
            //FIN de la validation
            var emblem: String = bgID + "," + bgCol + "," + embID + "," + embCol//9,6o5nc,2c,0;
            if (World.world.guildEmblemIsUsed(emblem)) {
                SocketManager.GAME_SEND_gC_PACKET(this.player, "Eae")
                return
            }
            if (this.player.curMap.id == 2196)//Temple de cr?ation de guilde
            {
                if (!this.player.hasItemTemplate(1575, 1, false))//Guildalogemme
                {
                    SocketManager.GAME_SEND_Im_PACKET(this.player, "14")
                    return
                }
                this.player.removeItemByTemplateId(1575, 1, false)
            }
            var G: Guild = Guild(name, emblem)
            var gm: GuildMember = G.addNewMember(this.player)
            gm.setAllRights(1, 0.toByte(), 1, this.player);//1 => Meneur (Tous droits)
            this.player.guildMember = gm;//On ajthise le meneur
            World.world.addGuild(G)
            DatabaseManager.get(GuildMemberData::class.java).update(this.player)
            //Packets
            SocketManager.GAME_SEND_gS_PACKET(this.player, gm)
            SocketManager.GAME_SEND_gC_PACKET(this.player, "K")
            SocketManager.GAME_SEND_gV_PACKET(this.player)
        } catch (e: Exception) {
            log.error("unexpected error", e)
        }
    }

    private fun teleportToGuildFarm(packet: String) {
        if (this.player.getGuild() == null) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "1135")
            return
        }
        if (this.player.fight != null || this.player.away)
            return
        var MapID: Int = (packet).toInt()
        var MP: MountPark = World.world.getMap(MapID).mountPark!!
        if (MP.guild!!.id != this.player.getGuild()!!.id) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "1135")
            return
        }
        var CellID: Int = World.world.getEncloCellIdByMapId(MapID)
        if (this.player.hasItemTemplate(9035, 1, false)) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "022;1~9035")
            this.player.removeItemByTemplateId(9035, 1, false)
            this.player.teleport(MapID, CellID)
        } else {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "1159")
        }
    }

    private fun removeTaxCollector(packet: String) {
        if (this.player.getGuild() == null || this.player.fight != null
                || this.player.away)
            return
        if (!this.player.guildMember!!.canDo(Constant.G_POSPERCO))
            return;//On peut le retirer si on a le droit de le poser
        var idCollector: Int = (packet).toInt()
        var collector: Collector? = World.world.getCollector(idCollector)
        if (collector == null || collector.inFight > 0)
            return
        collector.reloadTimer()
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.player.curMap, idCollector)
        DatabaseManager.get(CollectorData::class.java).delete(collector)
        collector.delCollector(collector.id)
        for (z in  this.player.getGuild()!!.getPlayers()) {
            if (z.isOnline) {
                SocketManager.GAME_SEND_gITM_PACKET(z, org.starloco.locos.entity.Collector.parseToGuild(z.getGuild()!!.id))
                var str: String = ""
                str += "R" + collector.getFullName() + "|"
                str += collector.map.toString() + "|"
                str += World.world.getMap(collector.map)!!.x.toString() + "|" + World.world.getMap(collector.map)!!.y.toString() + "|" + this.player.name
                SocketManager.GAME_SEND_gT_PACKET(z!!, str)
            }
        }
    }

    private fun teleportToGuildHouse(packet: String) {
        if (this.player.getGuild() == null) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "1135")
            return
        }

        if (this.player.fight != null || this.player.away)
            return
        var HouseID: Int = (packet).toInt()
        var h: House? = World.world.houses[HouseID]
        if (h == null)
            return
        if (this.player.getGuild()!!.id != h.guildId) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "1135")
            return
        }
        if (!h.canDo(Constant.H_GTELE)) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "1136")
            return
        }
        if (this.player.hasItemTemplate(8883, 1, false)) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "022;1~8883")
            this.player.removeItemByTemplateId(8883, 1, false)
            this.player.teleport(h.houseMapId, h.houseCellId)
        } else {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "1137")
        }
    }

    private fun placeTaxCollector() {
        val guild: Guild? = this.player.getGuild()
        val map: GameMap = this.player.curMap

        if (guild == null || this.player.fight != null || this.player.away || !this.player.guildMember!!.canDo(Constant.G_POSPERCO) || !guild.haveTenMembers())
            return
        if (this.player.isMissingSubscription()) {
            SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this.player.getGameClient()!!, 'S')
            return
        }

        var price: Short = ((1000 + 10 * guild.lvl).toShort())//Calcul du prix du Collector

        if (this.player.kamas < price) {//Kamas insuffisants
            SocketManager.GAME_SEND_Im_PACKET(this.player, "182")
            return
        }
        if (Collector.getCollectorByGuildId(map.id) > 0) {//La Map poss?de un Collector
            SocketManager.GAME_SEND_Im_PACKET(this.player, "1168;1")
            return
        }
        if (map.places.size < 2 || SoulStone.isInArenaMap(map.id) || map.data.noCollectors) {//La map ne poss?de pas de "places"
            SocketManager.GAME_SEND_Im_PACKET(this.player, "113")
            return
        }

        if (Collector.countCollectorGuild(guild.id) >= guild.nbCollectors)
            return

        if (World.world.delayCollectors[map.id] != null) {
            var time: Long = World.world.delayCollectors[map.id]!!

            if ((System.currentTimeMillis() - time) < (((10L * guild.lvl) * 60) * 1000)) {
                this.player.send("Im1167;" + ((((((10L * guild.lvl) * 60) * 1000) - (System.currentTimeMillis() - time)) / 1000) / 60))
                return
            }
            World.world.delayCollectors.remove(map.id)
        }

        if(map.subArea != null) {
            val quit: ByteArray = byteArrayOf(0)
            World.world.collectors.values.stream().filter({ collector -> collector != null && collector.guildId == guild.id }).forEach { collector ->
                var curMap: GameMap = World.world.getMap(collector.map)!!
                if (curMap.subArea != null && curMap.subArea!!.id == map.subArea!!.id) {
                    this.player.send("Im1168;1")
                    quit[0] = 1
                }
            }
            if(quit[0].toInt() == 1) return
        }

        World.world.delayCollectors[map.id] = System.currentTimeMillis()
        this.player.kamas = this.player.kamas - price

        if (this.player.kamas <= 0)
            this.player.kamas = 0


        var n1: Short = ((Formulas.getRandomValue(1, 129)).toShort())
        var n2: Short = ((Formulas.getRandomValue(1, 227)).toShort())
        var collector: Collector = Collector(-1, map.id, this.player.curCell.getId(), 3.toByte(), guild.id, n1, n2, this.player, System.currentTimeMillis(), "", 0, 0)
        DatabaseManager.get(CollectorData::class.java).insert(collector)
        World.world.addCollector(collector)
        SocketManager.GAME_SEND_ADD_PERCO_TO_MAP(map)
        SocketManager.GAME_SEND_STATS_PACKET(this.player)

        for (player in  guild.getPlayers()) {
            if (player != null && player.isOnline) {
                SocketManager.GAME_SEND_gITM_PACKET(player, org.starloco.locos.entity.Collector.parseToGuild(player.getGuild()!!.id))
                var str: String = ""
                str += "S" + collector.getFullName() + "|"
                str += collector.map.toString() + "|"
                str += World.world.getMap(collector.map)!!.x.toString() + "|" + World.world.getMap(collector.map)!!.y.toString() + "|" + this.player.name
                SocketManager.GAME_SEND_gT_PACKET(this.player, str)
            }
        }
    }

    private fun getInfos(c: Char) {
        when (c) {
'B' -> {SocketManager.GAME_SEND_gIB_PACKET(this.player, this.player.getGuild()!!.parseCollectorToGuild())
                
}
'F' -> {SocketManager.GAME_SEND_gIF_PACKET(this.player, World.world.parseMPtoGuild(this.player.getGuild()!!.id))
                
}
'G' -> {SocketManager.GAME_SEND_gIG_PACKET(this.player, this.player.getGuild()!!)
                
}
'H' -> {SocketManager.GAME_SEND_gIH_PACKET(this.player, World.world.houseManager.parseHouseToGuild(this.player))
                
}
'M' -> {SocketManager.GAME_SEND_gIM_PACKET(this.player, this.player.getGuild()!!, '+')
                
}
'T' -> {SocketManager.GAME_SEND_gITM_PACKET(this.player, Collector.parseToGuild(this.player.getGuild()!!.id))
                Collector.parseAttaque(this.player, this.player.getGuild()!!.id)
                Collector.parseDefense(this.player, this.player.getGuild()!!.id)
                
}
}
    }

    private fun invitationGuild(packet: String) {
        when (packet[0]) {
'R' -> {var P: Player? = World.world.getPlayerByName(packet.substring(1))
                if (P == null || this.player.getGuild() == null) {
                    SocketManager.GAME_SEND_gJ_PACKET(this.player, "Eu")
                    return
                }
                if (!P.isOnline) {
                    SocketManager.GAME_SEND_gJ_PACKET(this.player, "Eu")
                    return
                }
                if (P.away) {
                    SocketManager.GAME_SEND_gJ_PACKET(this.player, "Eo")
                    return
                }
                if (P.getGuild() != null) {
                    SocketManager.GAME_SEND_gJ_PACKET(this.player, "Ea")
                    return
                }
                if (!this.player.guildMember!!.canDo(Constant.G_INVITE)) {
                    SocketManager.GAME_SEND_gJ_PACKET(this.player, "Ed")
                    return
                }
                if (this.player.getGuild()!!.getPlayers().size >= (40 + this.player.getGuild()!!.lvl))//Limite membres max
                {
                    SocketManager.GAME_SEND_Im_PACKET(this.player, "155;" + (40 + this.player.getGuild()!!.lvl))
                    return
                }
                this.player.inviting = P.id
                P.inviting = this.player.id

                SocketManager.GAME_SEND_gJ_PACKET(this.player, "R" + packet.substring(1))
                SocketManager.GAME_SEND_gJ_PACKET(P, "r" + this.player.id.toString() + "|" + this.player.name + "|" + this.player.getGuild()!!.name)
                
}
'E' -> {if (packet.substring(1).equals(this.player.inviting.toString(), ignoreCase = true)) {
                    var p: Player? = World.world.getPlayer(this.player.inviting)
                    if (p == null)
                        return;//Pas cens? arriver
                    SocketManager.GAME_SEND_gJ_PACKET(p!!, "Ec")
                }
                
}
'K' -> {if (packet.substring(1).equals(this.player.inviting.toString(), ignoreCase = true)) {
                    var p: Player? = World.world.getPlayer(this.player.inviting)
                    if (p == null)
                        return;//Pas cens? arriver
                    var G: Guild = p.getGuild()!!
                    var GM: GuildMember = G.addNewMember(this.player)
                    DatabaseManager.get(GuildMemberData::class.java).update(this.player)
                    this.player.guildMember = GM
                    this.player.inviting = -1
                    p.inviting = -1
                    //Packet
                    SocketManager.GAME_SEND_gJ_PACKET(p, "Ka" + this.player.name)
                    SocketManager.GAME_SEND_gS_PACKET(this.player, GM)
                    SocketManager.GAME_SEND_gJ_PACKET(this.player, "Kj")
                }
                
}
}
    }

    private fun banToGuild(name: String) {
        if (this.player.getGuild() == null)
            return
        var P: Player? = World.world.getPlayerByName(name)
        var guid: Int = -1
        var guildId: Int = -1
        lateinit var toRemGuild: Guild
        lateinit var toRemMember: GuildMember
        if (P == null) {
            var infos = intArrayOf()
            guid = infos[0]
            guildId = infos[1]
            if (guildId < 0 || guid < 0)
                return
            toRemGuild = World.world.getGuild(guildId)!!
            toRemMember = toRemGuild.getMember(guid)!!
        } else {
            toRemGuild = P.getGuild()!!
            if (toRemGuild == null)//La guilde du this.playernnage n'est pas charger ?
            {
                toRemGuild = World.world.getGuild(this.player.getGuild()!!.id)!!;//On prend la guilde du this.player qui l'?jecte
            }
            toRemMember = toRemGuild.getMember(P.id)!!
            if (toRemMember == null)
                return;//Si le membre n'est pas dans la guilde.
            if (toRemMember.guild.id != this.player.getGuild()!!.id)
                return;//Si guilde diff?rente
        }
        //si pas la meme guilde
        if (toRemGuild.id != this.player.getGuild()!!.id) {
            SocketManager.GAME_SEND_gK_PACKET(this.player, "Ea")
            return
        }
        //S'il n'a pas le droit de kick, et que ce n'est pas lui m?me la cible
        if (!this.player.guildMember!!.canDo(Constant.G_BAN)
                && this.player.guildMember!!.playerId != toRemMember.playerId) {
            SocketManager.GAME_SEND_gK_PACKET(this.player, "Ed")
            return
        }
        //Si diff?rent : Kick
        if (this.player.guildMember!!.playerId != toRemMember.playerId) {
            if (toRemMember.rank == 1) //S'il veut kicker le meneur
                return

            toRemGuild.removeMember(toRemMember.player!!)
            if (P != null)
                P.guildMember = null
            if (toRemGuild.id == 1)
                toRemMember.player!!.modifAlignement(0)
            SocketManager.GAME_SEND_gK_PACKET(this.player, "K" + this.player.name + "|" + name)
            if (P != null)
                SocketManager.GAME_SEND_gK_PACKET(P, "K" + this.player.name)
        } else
        //si quitter
        {
            var G: Guild = this.player.getGuild()!!
            if (this.player.guildMember!!.rank == 1
                    && G.getPlayers().size > 1) //Si le meneur veut quitter la guilde mais qu'il reste d'autre joueurs
            {
                SocketManager.GAME_SEND_MESSAGE(this.player, this.player.getLang().trans("game.gameclient.bantoguild.rank.mener"))
                return
            }
            G.removeMember(this.player)
            this.player.guildMember = null
            if (G.id == 1)
                this.player.modifAlignement(0)
            //S'il n'y a plus this.playernne
            if (G.getPlayers().isEmpty())
                World.world.removeGuild(G.id)
            SocketManager.GAME_SEND_gK_PACKET(this.player, "K" + name + "|" + name)
        }
    }

    private fun changeMemberProfil(packet: String) {
        if (this.player.getGuild() == null)
            return; //Si le this.playernnage envoyeur n'a m?me pas de guilde

        var infos: List<String> = packet.split("|")

        var guid: Int = (infos[0]).toInt()
        var rank: Int = (infos[1]).toInt()
        var xpGive: Byte = (infos[2]).toByte()
        var right: Int = (infos[3]).toInt()

        var p: Player? = World.world.getPlayer(guid) //Cherche le this.playernnage a qui l'on change les droits dans la m?moire
        lateinit var toChange: GuildMember
        var changer: GuildMember = this.player.guildMember!!

        //R?cup?ration du this.playernnage ? changer, et verification de quelques conditions de base
        if (p == null) //Arrive lorsque le this.playernnage n'est pas charg? dans la m?moire
        {
            var guildId: Int = DatabaseManager.get(GuildMemberData::class.java).isPersoInGuild(guid) //R?cup?re l'id de la guilde du this.playernnage qui n'est pas dans la m?moire

            if (guildId < 0)
                return; //Si le this.playernnage ? qui les droits doivent ?tre modifi? n'existe pas ou n'a pas de guilde

            if (guildId != this.player.getGuild()!!.id) //Si ils ne sont pas dans la m?me guilde
            {
                SocketManager.GAME_SEND_gK_PACKET(this.player, "Ed")
                return
            }
            toChange = World.world.getGuild(guildId)!!.getMember(guid)!!
        } else {
            if (p.getGuild() == null)
                return; //Si la this.playernne ? qui changer les droits n'a pas de guilde
            if (this.player.getGuild()!!.id != p.getGuild()!!.id) //Si ils ne sont pas de la meme guilde
            {
                SocketManager.GAME_SEND_gK_PACKET(this.player, "Ea")
                return
            }

            toChange = p.guildMember!!
        }

        //V?rifie ce que le this.playernnage changeur ? le droit de faire

        if (changer.rank == 1) //Si c'est le meneur
        {
            if (changer.playerId == toChange.playerId) //Si il se modifie lui m?me, reset tthis sauf l'XP
            {
                rank = -1
                right = -1
            } else
            //Si il modifie un autre membre
            {
                if (rank == 1) //Si il met un autre membre "Meneur"
                {
                    changer.setAllRights(2, (-1).toByte(), 29694, this.player); //Met le meneur "Bras droit" avec tthis les droits

                    //D?fini les droits ? mettre au nouveau meneur
                    rank = 1
                    xpGive = -1
                    right = 1
                }
            }
        } else
        //Sinon, c'est un membre normal
        {
            if (toChange.rank == 1) //S'il veut changer le meneur, reset tthis sauf l'XP
            {
                rank = -1
                right = -1
            } else
            //Sinon il veut changer un membre normal
            {
                if (!changer.canDo(Constant.G_RANK) || rank == 1) //S'il ne peut changer les rang ou qu'il veut mettre meneur
                    rank = -1; //"Reset" le rang

                if (!changer.canDo(Constant.G_RIGHT) || right == 1) //S'il ne peut changer les droits ou qu'il veut mettre les droits de meneur
                    right = -1; //"Reset" les droits

                if (!changer.canDo(Constant.G_HISXP)
                        && !changer.canDo(Constant.G_ALLXP)
                        && changer.playerId == toChange.playerId) //S'il ne peut changer l'XP de this.playernne et qu'il est la cible
                    xpGive = -1; //"Reset" l'XP
            }

            if (!changer.canDo(Constant.G_ALLXP) && !changer.equals(toChange)) //S'il n'a pas le droit de changer l'XP des autres et qu'il n'est pas la cible
                xpGive = -1; //"Reset" L'XP
        }
        toChange.setAllRights(rank, xpGive, right, this.player)
        SocketManager.GAME_SEND_gS_PACKET(this.player, this.player.guildMember!!)
        if (p != null && p.id != this.player.id)
            SocketManager.GAME_SEND_gS_PACKET(p, p.guildMember!!)
    }

    private fun joinOrLeaveTaxCollector(packet: String) {
        var id: Int = -1
        var CollectorID: String = Integer.toString((packet.substring(1)).toInt(), 36)
        try {
            id = (CollectorID).toInt()
        } catch (e: Exception) {
            log.error("unexpected error", e)
            return
        }

        var collector: Collector? = World.world.getCollector(id)
        var fail: Boolean =this.player.isDead().toInt() == 1 || collector == null || collector.inFight <= 0

        if(collector != null) {
            when (packet[0]) {
'J' -> {if (player.fight == null && !player.away && !player.isInPrison()) {
                        if (collector.defenseFight.size >= World.world.getMap(collector.map).maxTeam)
                            return;//Plus de place
                        collector.addDefenseFight(player)
                    }
                    
}
'V' -> {collector.delDefenseFight(player)
                    
}
}
        }
        /*if (!fail) {
            SocketManager.GAME_SEND_BN(this.player);
        }*/
        for (z in  World.world.getGuild(collector!!.guildId)!!.getPlayers()) {
            if (z == null)
                continue
            if (z.isOnline) {
                SocketManager.GAME_SEND_gITM_PACKET(z, Collector.parseToGuild(collector!!.guildId))
                Collector.parseAttaque(z, collector!!.guildId)
                Collector.parseDefense(z, collector!!.guildId)
            }
        }
    }

    private fun leavePanelGuildCreate() {
        SocketManager.GAME_SEND_gV_PACKET(this.player)
    }

    /** Fin Guild Packet **/

    /**
     * Housse Packet *
     */
    private fun parseHousePacket(packet: String) {
        var packet: String? = packet
        when (packet!![1]) {
'B' -> {packet = packet!!.substring(2)
                World.world.houseManager.buy(this.player)
                
}
'G' -> {packet = packet!!.substring(2)
                if (packet!!.isEmpty())
                    packet = null
                World.world.houseManager.parseHG(this.player, packet)
                
}
'Q' -> {packet = packet!!.substring(2)
                World.world.houseManager.leave(this.player, packet!!)
                
}
'S' -> {packet = packet!!.substring(2)
                World.world.houseManager.sell(this.player, packet!!)
                
}
'V' -> {World.world.houseManager.closeBuy(this.player)
                
}
}
    }

    /** Fin Housse Packet **/

    /**
     * Enemy Packet *
     */
    private fun parseEnemyPacket(packet: String) {
        when (packet[1]) {
'A' -> {addEnemy(packet)
                
}
'D' -> {removeEnemy(packet)
                
}
'L' -> {SocketManager.GAME_SEND_ENEMY_LIST(this.player)
                
}
}
    }

    private fun addEnemy(packet: String) {
        var packet = packet
        if (!this::player.isInitialized)
            return
        var guid: Int = -1
        when (packet[2]) {
'%' -> {packet = packet.substring(3)
                var P: Player? = World.world.getPlayerByName(packet)
                if (P == null) {
                    SocketManager.GAME_SEND_FD_PACKET(this.player, "Ef")
                    return
                }
                guid = P.accID

                
}
'*' -> {packet = packet.substring(3)
                var C: Account? = World.world.getAccountByPseudo(packet)
                if (C == null) {
                    SocketManager.GAME_SEND_FD_PACKET(this.player, "Ef")
                    return
                }
                guid = C.id
                
}
else -> {packet = packet.substring(2)
                var Pr: Player? = World.world.getPlayerByName(packet)
                if (Pr == null || !Pr.isOnline) {
                    SocketManager.GAME_SEND_FD_PACKET(this.player, "Ef")
                    return
                }
                guid = Pr.getAccount()!!.id
                
}
}
        if (guid == -1) {
            SocketManager.GAME_SEND_FD_PACKET(this.player, "Ef")
            return
        }
        account.addEnemy(packet, guid)
    }

    private fun removeEnemy(packet: String) {
        var packet = packet
        if (!this::player.isInitialized)
            return
        var guid: Int = -1
        when (packet[2]) {
'%' -> {packet = packet.substring(3)
                var P: Player? = World.world.getPlayerByName(packet)
                if (P == null) {
                    SocketManager.GAME_SEND_FD_PACKET(this.player, "Ef")
                    return
                }
                guid = P.accID

                
}
'*' -> {packet = packet.substring(3)
                var C: Account? = World.world.getAccountByPseudo(packet)
                if (C == null) {
                    SocketManager.GAME_SEND_FD_PACKET(this.player, "Ef")
                    return
                }
                guid = C.id
                
}
else -> {packet = packet.substring(2)
                var Pr: Player? = World.world.getPlayerByName(packet)
                if (Pr == null || !Pr.isOnline) {
                    SocketManager.GAME_SEND_FD_PACKET(this.player, "Ef")
                    return
                }
                guid = Pr.getAccount()!!.id
                
}
}
        if (guid == -1 || !account.isEnemyWith(guid)) {
            SocketManager.GAME_SEND_FD_PACKET(this.player, "Ef")
            return
        }
        account.removeEnemy(guid)
    }

    /** Enemy Packet **/

    /**
     * JobOption Packet *
     */
    private fun parseJobOption(packet: String) {
        when (packet[1]) {
'O' -> {var infos: List<String> = packet.substring(2).split("|")
                var pos: Int = (infos[0]).toInt()
                var option: Int = (infos[1]).toInt()
                var slots: Int = (infos[2]).toInt()
                var SM: JobStat? = this.player.metiers[pos]
                if (SM == null)
                    return
                SM.setOptBinValue(option)
                SM.slotsPublic = slots
                SocketManager.GAME_SEND_JO_PACKET(this.player, SM)
                
}
}
    }

    /** Fin JobOption Packet **/

    /**
     * House Code Packet *
     */
    private fun parseHouseKodePacket(packet: String) {
        when (packet[1]) {
'V' -> {World.world.houseManager.closeCode(this.player)
                
}
'K' -> {sendKey(packet)
                
}
}
    }

    private fun sendKey(packet: String) {
        var packet = packet
        when (packet[2]) {
'0' -> {packet = packet.substring(4)
                if (this.player.savestat > 0) {
                    try {
                        var code: Int = 0
                        code = (packet).toInt()
                        if (code < 0)
                            return
                        if (this.player.capital < code)
                            code = this.player.capital
                        this.player.boostStatFixedCount(this.player.savestat, code)
                    } catch (e: Exception) {
                        log.error("unexpected error", e)
                    } finally {
                        this.player.savestat = 0
                        SocketManager.GAME_SEND_KODE(this.player, "V")
                    }
                } else if (this.player.exchangeAction != null && this.player.exchangeAction!!.getType() == ExchangeAction.IN_TRUNK) {
                    Trunk.open(this.player, packet, false)
                } else {
                    if (this.player.curHouse != null) {
                        this.player.curHouse!!.open(this.player, packet, false)
                    }
                }
                
}
'1' -> {if (this.player.exchangeAction != null) {
                    when (this.player.exchangeAction!!.getType()) {
ExchangeAction.LOCK_TRUNK -> {Trunk.lock(this.player, packet.substring(4))
                            
}
ExchangeAction.LOCK_HOUSE -> {World.world.houseManager.lockIt(this.player, packet.substring(4))
                            
}
}
                }
                
}
}
    }

    /** Fin Housse Code Packet **/

    /**
     * Object Packet *
     */
    private fun parseObjectPacket(packet: String) {
        if (this.player.exchangeAction != null && packet[1] != 'M')
            return
        when (packet[1]) {
'd' -> {destroyObject(packet)
                
}
'D' -> {dropObject(packet)
                
}
'M' -> {movementObject(packet)
                
}
'U' -> {useObject(packet)
                
}
'x' -> {dissociateObvi(packet)
                
}
'f' -> {feedObvi(packet)
                
}
's' -> {setSkinObvi(packet)
                
}
'r' -> {setItemShortcut(packet.substring(2))
        
}
}
    }

    private fun destroyObject(packet: String) {

        var infos: List<String> = packet.substring(2).split("|")
        try {
            var guid: Int = (infos[0]).toInt()
            var qua: Int = 1
            try {
                qua = (infos[1]).toInt()
            } catch (ignored: Exception) {}

            var obj: GameObject? = this.player.objects[guid]
            if (obj == null || !this.player.hasItemGuid(guid) || qua <= 0
                    || this.player.fight != null || this.player.away) {
                //SocketManager.GAME_SEND_DELETE_OBJECT_FAILED_PACKET(this);
                return
            }
            if (obj.position != Constant.ITEM_POS_NO_EQUIPED)
                return
            if (qua > obj.quantity)
                qua = obj.quantity
            var newQua: Int = obj.quantity - qua
            if (newQua <= 0) {
                this.player.removeItem(guid)
                World.world.removeGameObject(guid)
                DatabaseManager.get(ObjectData::class.java).delete(obj)
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this.player, guid)
            } else {
                obj.quantity = newQua
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this.player, obj)
            }
            SocketManager.GAME_SEND_STATS_PACKET(this.player)
            SocketManager.GAME_SEND_Ow_PACKET(this.player)
        } catch (e: Exception) {
            log.error("unexpected error", e)
            SocketManager.GAME_SEND_DELETE_OBJECT_FAILED_PACKET(this)
        }
    }

    private fun dropObject(packet: String) {
        var guid: Int = -1
        var qua: Int = -1
        try {
            guid = (packet.substring(2).split("|")[0]).toInt()
            qua = (packet.split("|")[1]).toInt()
        } catch (e: Exception) {
            log.error("unexpected error", e)
        }
        if (guid == -1 || qua <= 0 || !this.player.hasItemGuid(guid)
                || this.player.fight != null || this.player.away)
            return
        var obj: GameObject = this.player.objects[guid]!!

        if(obj.isAttach) return

        var cellPosition: Int = Constant.getNearestCellIdUnused(this.player)

        if (cellPosition < 0) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "1145")
            return
        }
        if (obj.position != Constant.ITEM_POS_NO_EQUIPED) {
            obj.position = Constant.ITEM_POS_NO_EQUIPED
            SocketManager.GAME_SEND_OBJET_MOVE_PACKET(this.player, obj)
            if (obj.position == Constant.ITEM_POS_ARME
                    || obj.position == Constant.ITEM_POS_COIFFE
                    || obj.position == Constant.ITEM_POS_FAMILIER
                    || obj.position == Constant.ITEM_POS_CAPE
                    || obj.position == Constant.ITEM_POS_BOUCLIER
                    || obj.position == Constant.ITEM_POS_NO_EQUIPED)
                SocketManager.GAME_SEND_ON_EQUIP_ITEM(this.player.curMap, this.player)
        }
        if (qua >= obj.quantity) {
            this.player.removeItem(guid)
            this.player.curMap.getCase(cellPosition)!!.tryDropItem(obj)
            obj.position = Constant.ITEM_POS_NO_EQUIPED
            SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this.player, guid)
        } else {
            obj.quantity = obj.quantity - qua
            var obj2: GameObject = obj.getClone(qua, true)!!
            obj2!!.position = Constant.ITEM_POS_NO_EQUIPED
            this.player.curMap.getCase(cellPosition)!!.tryDropItem(obj2)
            SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this.player, obj)
        }
        if (Logging.USE_LOG)
            Logging.getInstance().write("Object", "Dropping : " + this.player.name + " a jet? [" + obj.template!!.id + "@" + obj.guid.toString() + ";" + qua + "]")
        SocketManager.GAME_SEND_Ow_PACKET(this.player)
        SocketManager.GAME_SEND_GDO_PACKET_TO_MAP(this.player.curMap, '+', this.player.curMap.getCase(cellPosition)!!.getId(), obj.template!!.id, 0)
        SocketManager.GAME_SEND_STATS_PACKET(this.player)
    }

    @Synchronized fun movementObject(packet: String) {
        var infos: List<String> = packet.substring(2).split('\n')[0].split("|")
        try {
            var quantity: Int = 1
            var id: Int = (infos[0]).toInt()
            var position: Int = (infos[1]).toInt()
            try {
                quantity = (infos[2]).toInt()
            } catch (ignored: Exception) {}

            var obj: GameObject? = this.player.objects[id]
            if (obj == null || player.exchangeAction != null)
                return
            if (this.player.fight != null)
                if (this.player.fight!!.state > Constant.FIGHT_STATE_ACTIVE)
                    return

            /* Pet subscribe **/
            if (position == Constant.ITEM_POS_FAMILIER && !this.player.isSubscribe()) {
                SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this, 'S')
                return
            }
            /* End pet subscribe **/

            /* Feed mount **/
            if ((position == Constant.ITEM_POS_DRAGODINDE) && (this.player.mount != null)) {
                if (obj.template!!.type == 41) {
                    if (obj.quantity > 0) {
                        if (quantity > obj.quantity)
                            quantity = obj.quantity
                        if (obj.quantity - quantity > 0) {
                            var newQua: Int = obj.quantity - quantity
                            obj.quantity = newQua
                            SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this.player, obj)
                        } else {
                            this.player.deleteItem(id)
                            World.world.removeGameObject(id)
                            SocketManager.SEND_OR_DELETE_ITEM(this, id)
                        }
                    }
                    this.player.mount!!.aumEnergy(5000 * quantity)
                    SocketManager.GAME_SEND_Re_PACKET(this.player, "+", this.player.mount)
                    SocketManager.GAME_SEND_Im_PACKET(this.player, "0105")
                    return
                }
                SocketManager.GAME_SEND_Im_PACKET(this.player, "190")
                return
            }
            /* End feed mount **/

            // Pour equiper un item apres avoir desequiper l item a la meme position
            var equipBack: Boolean = false

            /* Feed pet **/
            if (position == Constant.ITEM_POS_FAMILIER && obj.template!!.type != Constant.ITEM_TYPE_FAMILIER && this.player.getObjetByPos(position) != null) {
                var pets: GameObject = this.player.getObjetByPos(position)!!
                var p: Pet? = World.world.getPets(pets.template!!.id)
                if (p == null)
                    return
                if (p.epo == obj.template!!.id) {
                    var pet: PetEntry? = World.world.getPetsEntry(pets.guid)
                    if (pet != null && p.epo == obj.template!!.id)
                        pet.giveEpo(this.player)
                    return
                }
                if (obj.template!!.id != 2239 && !p.canEat(obj.template!!.id, obj.template!!.type, -1)) {
                    SocketManager.GAME_SEND_Im_PACKET(this.player, "153")
                    return
                }

                var min: Int = 0
                var max: Int = 0
                try {
                    min = (p.gap.split(",")[0]).toInt()
                    max = (p.gap.split(",")[1]).toInt()
                } catch (e: Exception) {
                    // ok
                }

                var MyPets: PetEntry? = World.world.getPetsEntry(pets.guid)
                if (MyPets == null)
                    return
                if (p.type == 2 || p.type == 3
                        || obj.template!!.id == 2239) {
                    if (obj.quantity - 1 > 0) {//Si il en reste
                        obj.quantity = obj.quantity - 1
                        SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this.player, obj)
                    } else {
                        World.world.removeGameObject(obj.guid)
                        this.player.removeItem(obj.guid)
                        SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this.player, obj.guid)
                    }

                    if (obj.template!!.id == 2239)
                        MyPets.restoreLife(this.player)
                    else
                        MyPets.eat(this.player, min, max, p.statsIdByEat(obj.template!!.id, obj.template!!.type, -1), obj)

                    SocketManager.GAME_SEND_UPDATE_OBJECT_DISPLAY_PACKET(this.player, pets)
                    SocketManager.GAME_SEND_Ow_PACKET(this.player)
                    this.player.refreshStats()
                    SocketManager.GAME_SEND_ON_EQUIP_ITEM(this.player.curMap, this.player)
                    SocketManager.GAME_SEND_STATS_PACKET(this.player)
                    if (this.player.party != null)
                        SocketManager.GAME_SEND_PM_MOD_PACKET_TO_GROUP(this.player.party!!, this.player)
                }
                return
            /* End feed pet **/
            } else {
                var template: ObjectTemplate = obj.template!!
                var set: Int = template.panoId

                if (set >= 81 && set <= 92 && position != Constant.ITEM_POS_NO_EQUIPED) {
                    var stats: List<String> = template.strTemplate.split(",")

                    for (stat in  stats) {
                        var split: List<String> = stat.split("#")
                        var effect: Int = (split[0]).toInt(16)
                        var spell: Int = (split[1]).toInt(16)
                        var value: Int = (split[3]).toInt(16)
                        if(effect == 289)
                            value = 1
                        SocketManager.SEND_SB_SPELL_BOOST(this.player, effect.toString() + ";" + spell.toString() + ";" + value)
                        this.player.addObjectClassSpell(spell, effect, value)
                    }
                    this.player.addObjectClass(template.id)
                }
                if (set >= 81 && set <= 92 && position == Constant.ITEM_POS_NO_EQUIPED) {
                    var stats: List<String> = template.strTemplate.split(",")

                    for (stat in  stats) {
                        var split: List<String> = stat.split("#")
                        var effect: Int = (split[0]).toInt(16)
                        var spell: Int = (split[1]).toInt(16)
                        SocketManager.SEND_SB_SPELL_BOOST(this.player, effect.toString() + ";" + spell + ";0")
                        this.player.removeObjectClassSpell((split[1]).toInt(16))
                    }
                    this.player.removeObjectClass(template.id)
                }
                if (!Constant.isValidPlaceForItem(obj.template!!, position) && position != Constant.ITEM_POS_NO_EQUIPED && obj.template!!.type != 113)
                    return


                if (!World.world.conditionManager.validConditions(this.player, obj.template!!.conditions)) {
                    SocketManager.GAME_SEND_Im_PACKET(this.player, "119|44;"+obj.template!!.id); // si le this.player ne v?rifie pas les conditions diverses
                    return
                }
                var shield: GameObject? = null
                var weapon: GameObject? = null
                weapon = this.player.getObjetByPos(Constant.ITEM_POS_ARME)
                shield = this.player.getObjetByPos(Constant.ITEM_POS_BOUCLIER)
                if ((position == Constant.ITEM_POS_BOUCLIER && weapon != null)
                        || (position == Constant.ITEM_POS_ARME && shield != null)) {
                    if (weapon != null) {
                        if (weapon.template!!.isTwoHanded) {
                            this.player.unequipedObjet(weapon!!)
                            SocketManager.GAME_SEND_Im_PACKET(this.player, "119|44"); // si le this.player ne v?rifie pas les conditions diverses
                            return
                        }
                    } else {
                        if (obj.template!!.isTwoHanded) {
                            if(shield != null)
                                this.player.unequipedObjet(shield!!)
                            SocketManager.GAME_SEND_Im_PACKET(this.player, "119|44"); // si le this.player ne v?rifie pas les conditions diverses
                            return
                        }
                    }

                }

                if (obj.template!!.level > this.player.level) {// si le this.player n'a pas le level
                    SocketManager.GAME_SEND_OAEL_PACKET(this)
                    return
                }

                //On ne peut ?quiper 2 items de panoplies identiques, ou 2 Dofus identiques
                if (position != Constant.ITEM_POS_NO_EQUIPED && (obj.template!!.panoId != -1 || obj.template!!.type == Constant.ITEM_TYPE_DOFUS) && this.player.hasEquiped(obj.template!!.id))
                    return

                // FIN DES VERIFS

                var exObj: GameObject? = this.player.getObjetByPos2(position)//Objet a l'ancienne position
                var objGUID: Int = obj.template!!.id
                // CODE OBVI
                if (obj.template!!.type == 113) {
                    if (exObj == null) {// si on place l'obvi sur un emplacement vide
                        SocketManager.send(this.player, "Im1161")
                        return
                    }
                    if (exObj.obvijevanPos != 0) {// si il y a d?j? un obvi
                        SocketManager.GAME_SEND_BN(this.player)
                        return
                    }
                    exObj.obvijevanPos = obj.obvijevanPos; // L'objet qui ?tait en place a maintenant un obvi
                    DatabaseManager.get(ObvijevanData::class.java).insert(Pair(exObj.guid, obj.template!!.id))
                    this.player.removeItem(obj.guid, 1, false, false); // on enl?ve l'existance de l'obvi en lui-m?me
                    SocketManager.send(this.player, "OR" + obj.guid); // on le pr?cise au client.
                    DatabaseManager.get(ObjectData::class.java).delete(obj)

                    exObj.refreshStatsObjet(obj.parseStatsStringSansUserObvi() + ",3ca#" + Integer.toHexString(objGUID) + "#0#0#0d0+" + objGUID)

                    SocketManager.send(this.player, exObj.obvijevanOCO_Packet(position))
                    SocketManager.GAME_SEND_ON_EQUIP_ITEM(this.player.curMap, this.player); // Si l'obvi ?tait cape ou coiffe : packet au client
                    // S'il y avait plusieurs objets
                    if (obj.quantity > 1) {
                        if (quantity > obj.quantity)
                            quantity = obj.quantity

                        if (obj.quantity - quantity > 0)//Si il en reste
                        {
                            var newItemQua: Int = obj.quantity - quantity
                            var newItem: GameObject = obj.getClone(newItemQua, true)!!
                            World.world.addGameObject(newItem)
                            obj.quantity = quantity
                            SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this.player, obj)
                        }
                    } else {
                        World.world.removeGameObject(obj.guid)
                    }
                    DatabaseManager.get(PlayerData::class.java).update(this.player)
                    return; // on s'arr?te l? pour l'obvi
                } // FIN DU CODE OBVI
                if (exObj != null)//S'il y avait d?ja un objet sur cette place on d?s?quipe
                {
                    equipBack = exObj.guid != obj.guid
                    var obj2: GameObject? = null
                    var exObjTpl: ObjectTemplate = exObj.template!!
                    var idSetExObj: Int = exObj.template!!.panoId
                    obj2 = this.player.getSimilarItem(exObj)
                    if (obj2 != null)//On le poss?de deja
                    {
                        obj2!!.quantity = obj2!!.quantity + exObj.quantity
                        SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this.player, obj2)
                        World.world.removeGameObject(exObj.guid)
                        this.player.removeItem(exObj.guid)
                        SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this.player, exObj.guid)
                    } else
                    //On ne le poss?de pas
                    {
                        exObj.position = Constant.ITEM_POS_NO_EQUIPED
                        if ((idSetExObj >= 81 && idSetExObj <= 92)
                                || (idSetExObj >= 201 && idSetExObj <= 212)) {
                            var stats: List<String> = exObjTpl.strTemplate.split(",")
                            for (stat in  stats) {
                                var vale = stat.split("#")
                                var modifi: String = (vale[0]).toInt(16).toString() + ";" + (vale[1]).toInt(16) + ";0"
                                SocketManager.SEND_SB_SPELL_BOOST(this.player, modifi)
                                this.player.removeObjectClassSpell((vale[1]).toInt(16))
                            }
                            this.player.removeObjectClass(exObjTpl.id)
                        }
                        SocketManager.GAME_SEND_OBJET_MOVE_PACKET(this.player, exObj)
                    }
                    if (this.player.getObjetByPos(Constant.ITEM_POS_ARME) == null)
                        SocketManager.GAME_SEND_OT_PACKET(this, -1)

                    //Si objet de panoplie
                    if (exObj.template!!.panoId > 0)
                        SocketManager.GAME_SEND_OS_PACKET(this.player, exObj.template!!.panoId)
                } else {
                    var obj2: GameObject? = null
                    //On a un objet similaire
                    obj2 = this.player.getSimilarItem(obj)
                    if (obj2 != null) {
                        if (quantity > obj.quantity)
                            quantity = obj.quantity

                        obj2!!.quantity = obj2!!.quantity + quantity
                        SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this.player, obj2)

                        if (obj.quantity - quantity > 0)//Si il en reste
                        {
                            obj.quantity = obj.quantity - quantity
                            SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this.player, obj)
                        } else
                        //Sinon on supprime
                        {
                            World.world.removeGameObject(obj.guid)
                            this.player.removeItem(obj.guid)
                            SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this.player, obj.guid)
                        }
                    } else
                    //Pas d'objets similaires
                    {
                        if (obj.position > 16) {
                            var oldPos: Int = obj.position
                            obj.position = position
                            SocketManager.GAME_SEND_OBJET_MOVE_PACKET(this.player, obj)

                            if (obj.quantity > 1) {
                                if (quantity > obj.quantity)
                                    quantity = obj.quantity

                                if (obj.quantity - quantity > 0) {//Si il en reste
                                    var newItem: GameObject = obj.getClone(obj.quantity - quantity, true)!!
                                    newItem.position = oldPos

                                    obj.quantity = quantity
                                    SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this.player, obj)

                                    if (this.player.addItem(newItem, false, false))
                                        World.world.addGameObject(newItem)
                                }
                            }
                        } else {
                            obj.position = position
                            SocketManager.GAME_SEND_OBJET_MOVE_PACKET(this.player, obj)

                            if (obj.quantity > 1) {
                                if (quantity > obj.quantity)
                                    quantity = obj.quantity

                                if (obj.quantity - quantity > 0) {//Si il en reste
                                    var newItemQua: Int = obj.quantity - quantity
                                    var newItem: GameObject = obj.getClone(newItemQua, true)!!
                                    obj.quantity = quantity
                                    SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this.player, obj)
                                }
                            }
                        }
                    }
                }
                if (position == Constant.ITEM_POS_ARME) {
                    when (obj.template!!.id) { 9544 -> {this.player.setFullMorph(1, false, false)
                            
}
9545 -> {this.player.setFullMorph(5, false, false)
                            
}
9546 -> {this.player.setFullMorph(4, false, false)
                            
}
9547 -> {this.player.setFullMorph(3, false, false)
                            
}
9548 -> {this.player.setFullMorph(2, false, false)
                            
}
10125 -> {this.player.setFullMorph(7, false, false)
                            
}
10126 -> {this.player.setFullMorph(6, false, false)
                            
}
10127 -> {this.player.setFullMorph(8, false, false)
                            
}
10133 -> {this.player.setFullMorph(9, false, false)
                            
}
}
                } else {// Tourmenteur ; on d?morphe
                    if (Constant.isIncarnationWeapon(obj.template!!.id))
                        this.player.unsetFullMorph()
                }

                if (obj.template!!.id == 2157) {
                    if (position == Constant.ITEM_POS_COIFFE) {
                        this.player.gfxId = if ((this.player.sexe == 1)) 8009 else 8006
                        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.player.curMap, this.player.id)
                        SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(this.player.curMap, this.player)
                        SocketManager.GAME_SEND_MESSAGE(this.player, this.player.getLang().trans("game.gameclient.movementobject.mercenaire"))
                    } else if (position == Constant.ITEM_POS_NO_EQUIPED) {
                        this.player.gfxId = this.player.classe * 10 + this.player.sexe
                        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.player.curMap, this.player.id)
                        SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(this.player.curMap, this.player)
                        SocketManager.GAME_SEND_MESSAGE(this.player, this.player.getLang().trans("game.gameclient.movementobject.mercenaire.disable"))
                    }
                }
                if (obj.template!!.id != 2157 && this.player.isMorphMercenaire() && position == Constant.ITEM_POS_COIFFE) {
                    this.player.gfxId = this.player.classe * 10 + this.player.sexe
                    SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.player.curMap, this.player.id)
                    SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(this.player.curMap, this.player)
                    SocketManager.GAME_SEND_MESSAGE(this.player, "Vous n'?tes plus mercenaire.")
                }
                // Si équipe un objet de métier, on lui donne le métier sinon on l'enlève
                /*if (position == Constant.ITEM_POS_ARME && this.player.getObjetByPos(Constant.ITEM_POS_ARME) != null) {
                    World.world.getJobs().stream().filter(e -> e.isValidTool(this.player.getObjetByPos(Constant.ITEM_POS_ARME).getTemplate().getId()))
                            .forEach(e -> {
                                this.player.learnJob(e);
                                var SM: JobStat= this.player.getMetierByID(e.getId())
                                if(SM != null) {
                                    SM.addXp(this.player, 581688);
                                    var SMs: ArrayList<JobStat>= ArrayList()
                                    SMs.add(SM);
                                    SocketManager.GAME_SEND_JX_PACKET(this.player, SMs);
                                }
                                SocketManager.GAME_SEND_OT_PACKET(this, e.getId());
                            });
                }
                if(position == -1 && obj.getTemplate().getPACost() > 0) {
                    World.world.getJobs().forEach(e -> {
                        var job: JobStat= this.player.getMetierByID(e.getId())
                        if (job != null) {
                            this.player.unlearnJob(job.getId());
                            this.player.send("JR" + e.getId());
                            SocketManager.GAME_SEND_OT_PACKET(this, -1);
                        }
                    });
                }*/

                this.player.refreshStats()
                SocketManager.GAME_SEND_STATS_PACKET(this.player)

                if (this.player.party != null)
                    SocketManager.GAME_SEND_PM_MOD_PACKET_TO_GROUP(this.player.party!!, this.player)

                if (position == Constant.ITEM_POS_ARME || position == Constant.ITEM_POS_COIFFE || position == Constant.ITEM_POS_FAMILIER || position == Constant.ITEM_POS_CAPE || position == Constant.ITEM_POS_BOUCLIER || position == Constant.ITEM_POS_NO_EQUIPED)
                    SocketManager.GAME_SEND_ON_EQUIP_ITEM(this.player.curMap, this.player)

                //Si familier
                if (position == Constant.ITEM_POS_FAMILIER && this.player.onMount)
                    this.player.toogleOnMount()
                //Verif pour les thisils de m�tier
                if (position == Constant.ITEM_POS_NO_EQUIPED && this.player.getObjetByPos(Constant.ITEM_POS_ARME) == null)
                    SocketManager.GAME_SEND_OT_PACKET(this, -1)
                if (position == Constant.ITEM_POS_ARME && this.player.getObjetByPos(Constant.ITEM_POS_ARME) != null)
                    this.player.metiers.entries.stream().filter({ e -> e.value.template.isValidTool(this.player.getObjetByPos(Constant.ITEM_POS_ARME)!!.template!!.id) }).forEach({ e -> SocketManager.GAME_SEND_OT_PACKET(this, e.value.template.id) })


                //Si objet de panoplie
                if (obj.template!!.panoId > 0)
                    SocketManager.GAME_SEND_OS_PACKET(this.player, obj.template!!.panoId)
                if (this.player.fight != null)
                    SocketManager.GAME_SEND_ON_EQUIP_ITEM_FIGHT(this.player, this.player.fight!!.getFighterByPerso(this.player)!!, this.player.fight!!)
            }

            // Start craft secure show/hide
            if (position == Constant.ITEM_POS_ARME || (position == Constant.ITEM_POS_NO_EQUIPED && obj.template!!.pACost > 0)) {
                this.player.refreshCraftSecure(true)
            }
            // End craft secure show/hide
            if(this.player.fight != null) {
                var target: Fighter? = this.player.fight!!.getFighterByPerso(this.player)
                this.player.fight!!.getFighters(7).stream().filter({ fighter -> fighter != null && fighter.player != null }).forEach({ fighter -> fighter.player!!.send(this.player.curMap.getFighterGMPacket(this.player)) })
                target!!.setPdv(this.player.curPdv)
                SocketManager.GAME_SEND_STATS_PACKET(this.player)
            }

            this.player.verifEquiped()
            DatabaseManager.get(PlayerData::class.java).update(this.player)
            if(equipBack)
                this.movementObject(packet)
        } catch (e: Exception) {
            log.error("unexpected error", e)
            SocketManager.GAME_SEND_DELETE_OBJECT_FAILED_PACKET(this)
        }
    }


    private fun setItemShortcut(packet: String) {
        var packet = packet
        // Official client seems to store Position -> Template+Stats rather than Position->guid
        // Maybe it's time for ItemHash (String) representing a TemplateID+Stats pair as a Comparable
        var action: Char = packet[0]
        packet = packet.substring(1)
        var parts: List<String> = packet.split(";")
        var position: Int = (parts[0]).toInt()
        when (action) {
'A' -> {var itemGUID: Int = (parts[1]).toInt()
                if(player.addItemShortcutSend(position, itemGUID)) return
}
'M' -> {var otherPos: Int = (parts[1]).toInt()
                if(player.moveItemShortcutSend(position, otherPos)) return
}
'R' -> {if(player.removeItemShortcutSend(position)) return
        
}
}
        send("BN"); // Error
        return
    }

    private fun useObject(packet: String) {
        var guid: Int = -1
        var targetGuid: Int = -1
        var cellID: Short = -1
        var quantity: Int = 1
        var target: Player? = null
        try {
            var infos: List<String> = packet.substring(2).split("|")
            guid = (infos[0]).toInt()
            quantity =if (infos.size > 3) (infos[3]).toInt() else 1

            try {
                targetGuid = (infos[1]).toInt()
            } catch (e: Exception) {
                // ok
            }
            try {
                cellID = (infos[2]).toShort()
            } catch (e: Exception) {
                // ok
            }
        } catch (e: Exception) {
            log.error("unexpected error", e)
            return
        }
        //Si le joueur n'a pas l'objet
        if (World.world.getPlayer(targetGuid) != null)
            target = World.world.getPlayer(targetGuid)
        if (!this.player.hasItemGuid(guid) || this.player.away)
            return
        if (target != null && target.away)
            return
        var obj: GameObject? = this.player.objects[guid]
        if (obj == null)
            return
        var T: ObjectTemplate = obj.template!!
        if (T.level > this.player.level || (!obj.template!!.conditions.equals("", ignoreCase = true) && !World.world.conditionManager.validConditions(this.player, obj.template!!.conditions))) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "119|43")
            return
        }
        T.applyAction(this.player, target!!, guid, cellID, quantity)
        if (T.type == Constant.ITEM_TYPE_PAIN || T.type == Constant.ITEM_TYPE_VIANDE_COMESTIBLE) {
            if (target != null)
                SocketManager.GAME_SEND_eUK_PACKET_TO_MAP(target.curMap, target.id, 17)
            else
                SocketManager.GAME_SEND_eUK_PACKET_TO_MAP(this.player.curMap, this.player.id, 17)
        } else if (T.type == Constant.ITEM_TYPE_BIERE) {
            if (target != null)
                SocketManager.GAME_SEND_eUK_PACKET_TO_MAP(target.curMap, target.id, 18)
            else
                SocketManager.GAME_SEND_eUK_PACKET_TO_MAP(this.player.curMap, this.player.id, 18)
        }
    }

    private fun dissociateObvi(packet: String) {
        var guid: Int = -1
        var pos: Int = -1
        try {
            guid = (packet.substring(2).split("|")[0]).toInt()
            pos = (packet.split("|")[1]).toInt()
        } catch (e: Exception) {
            log.error("unexpected error", e)
            return
        }
        if ((guid == -1) || (!this.player.hasItemGuid(guid)))
            return
        var obj: GameObject = this.player.objects[guid]!!
        var idOBVI: Int = DatabaseManager.get(ObvijevanData::class.java).load(obj.guid).getFirst()

        if (idOBVI == -1) {
            DatabaseManager.get(ObvijevanData::class.java).delete(Pair(obj.guid, obj.guid))
            when (obj.template!!.type) { 1 -> {idOBVI = 9255
                    
}
9 -> {idOBVI = 9256
                    
}
16 -> {idOBVI = 9234
                    
}
17 -> {idOBVI = 9233
                    
}
else -> {SocketManager.GAME_SEND_MESSAGE(this.player, "Erreur d'obvijevan numero: 4. Merci de nous le signaler si le probleme est grave.", "000000")
                    return
            
}
}
        }

        var t: ObjectTemplate = World.world.getObjTemplate(idOBVI)!!
        var obV: GameObject = t.createNewItem(1, true)!!
        var obviStats: String = obj.getObvijevanStatsOnly()!!
        if (obviStats.equals("")) {
            SocketManager.GAME_SEND_MESSAGE(this.player, "Erreur d'obvijevan numero: 3. Merci de nous le signaler si le probleme est grave.", "000000")
            return
        }
        obV.clearStats()
        obV.refreshStatsObjet(obviStats)
        if (this.player.addItem(obV, true, false))
            World.world.addGameObject(obV)
        obj.removeAllObvijevanStats()
        SocketManager.send(this.player, obj.obvijevanOCO_Packet(pos))
        SocketManager.GAME_SEND_ON_EQUIP_ITEM(this.player.curMap, this.player)
        DatabaseManager.get(PlayerData::class.java).update(this.player)
    }

    private fun feedObvi(packet: String) {
        var guid: Int = -1
        var pos: Int = -1
        var victime: Int = -1
        try {
            guid = (packet.substring(2).split("|")[0]).toInt()
            pos = (packet.split("|")[1]).toInt()
            victime = (packet.split("|")[2]).toInt()
        } catch (e: Exception) {
            log.error("unexpected error", e)
            return
        }

        if ((guid == -1) || (!this.player.hasItemGuid(guid)))
            return
        var obj: GameObject = this.player.objects[guid]!!
        var objVictime: GameObject = World.world.getGameObject(victime)!!
        obj.obvijevanNourir(objVictime)

        var qua: Int = objVictime.quantity
        if (qua <= 1) {
            this.player.removeItem(objVictime.guid)
            SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this.player, objVictime.guid)
        } else {
            objVictime.quantity = qua - 1
            SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this.player, objVictime)
        }
        SocketManager.send(this.player, obj.obvijevanOCO_Packet(pos))
        DatabaseManager.get(PlayerData::class.java).update(this.player)
    }

    private fun setSkinObvi(packet: String) {
        var guid: Int = -1
        var pos: Int = -1
        var vale: Int= -1
        try {
            guid = (packet.substring(2).split("|")[0]).toInt()
            pos = (packet.split("|")[1]).toInt()
            vale = (packet.split("|")[2]).toInt()
        } catch (e: Exception) {
            log.error("unexpected error", e)
            return
        }
        if ((guid == -1) || (!this.player.hasItemGuid(guid)))
            return
        var obj: GameObject = this.player.objects[guid]!!
        if ((vale >= 21) || (vale <= 0))
            return

        obj.obvijevanChangeStat(972, vale)
        SocketManager.send(this.player, obj.obvijevanOCO_Packet(pos))
        if (pos != -1)
            SocketManager.GAME_SEND_ON_EQUIP_ITEM(this.player.curMap, this.player)
    }

    /** Fin Object Packet **/

    /**
     * Group Packet *
     */
    private fun parseGroupPacket(packet: String) {
        when (packet[1]) {
'A' -> {acceptInvitation()
                
}
'F' -> {followMember(packet)
                
}
'G' -> {followAllMember(packet)
                
}
'I' -> {inviteParty(packet)
                
}
'R' -> {refuseInvitation()
                
}
'V' -> {leaveParty(packet)
                
}
'W' -> {whereIsParty()
                
}
}
    }

    private fun acceptInvitation() {
        if (!this::player.isInitialized || this.player.inviting == 0)
            return

        var target: Player? = World.world.getPlayer(this.player.inviting)

        if (target == null)
            return

        var party: Party? = target.party

        if (party == null) {
            party = Party(target, this.player)
            SocketManager.GAME_SEND_GROUP_CREATE(this, party)
            SocketManager.GAME_SEND_PL_PACKET(this, party)
            SocketManager.GAME_SEND_GROUP_CREATE(target.getGameClient()!!, party)
            SocketManager.GAME_SEND_PL_PACKET(target.getGameClient()!!, party)
            target.party = party
            SocketManager.GAME_SEND_ALL_PM_ADD_PACKET(target.getGameClient()!!, party)
        } else {
            SocketManager.GAME_SEND_GROUP_CREATE(this, party)
            SocketManager.GAME_SEND_PL_PACKET(this, party)
            SocketManager.GAME_SEND_PM_ADD_PACKET_TO_GROUP(party, this.player)
            party.addPlayer(this.player)
        }

        this.player.party = party
        SocketManager.GAME_SEND_ALL_PM_ADD_PACKET(this, party)
        SocketManager.GAME_SEND_PR_PACKET(target)
    }

    private fun followMember(packet: String) {
        var g: Party? = this.player.party
        if (g == null)
            return
        var pGuid: Int = -1
        try {
            pGuid = (packet.substring(3)).toInt()
        } catch (e: NumberFormatException) {
            log.error("unexpected error", e)
            return
        }
        if (pGuid == -1)
            return
        var P: Player? = World.world.getPlayer(pGuid)
        if (P == null || !P.isOnline)
            return
        if (packet[2] == '+')//Suivre
        {
            if (this.player.follow != null)
                this.player.follow!!.follower.remove(this.player.id)
            SocketManager.GAME_SEND_FLAG_PACKET(this.player, P)
            SocketManager.GAME_SEND_PF(this.player, "+" + P.id)
            this.player.follow = P
            P.follower[this.player.id] = this.player
            P.send("Im052;" + this.player.name)
        } else if (packet[2] == '-')//Ne plus suivre
        {
            SocketManager.GAME_SEND_DELETE_FLAG_PACKET(this.player)
            SocketManager.GAME_SEND_PF(this.player, "-")
            this.player.follow = null
            P.follower.remove(this.player.id)
            P.send("Im053;" + this.player.name)
        }
    }

    private fun followAllMember(packet: String) {
        var g2: Party? = this.player.party
        if (g2 == null)
            return
        var pGuid2: Int = -1
        try {
            pGuid2 = (packet.substring(3)).toInt()
        } catch (e: NumberFormatException) {
            log.error("unexpected error", e)
            return
        }

        if (pGuid2 == -1)
            return
        var P2: Player? = World.world.getPlayer(pGuid2)
        if (P2 == null || !P2.isOnline)
            return
        if (packet[2] == '+')//Suivre
        {
            for (T in  g2.players) {
                if (T.id == P2.id)
                    continue
                if (T.follow != null)
                    T.follow!!.follower.remove(this.player.id)
                SocketManager.GAME_SEND_FLAG_PACKET(T, P2)
                SocketManager.GAME_SEND_PF(T, "+" + P2.id)
                T.follow = P2
                P2.follower[T.id] = T
                P2.send("Im0178")
            }
        } else if (packet[2] == '-')//Ne plus suivre
        {
            for (T in  g2.players) {
                if (T.id == P2.id)
                    continue
                SocketManager.GAME_SEND_DELETE_FLAG_PACKET(T)
                SocketManager.GAME_SEND_PF(T, "-")
                T.follow = null
                P2.follower.remove(T.id)
                P2.send("Im053;" + T.name)
            }
        }
    }

    private fun inviteParty(packet: String) {
        if (!this::player.isInitialized)
            return

        var name: String = packet.substring(2)
        var target: Player? = World.world.getPlayerByName(name)

        if (target == null || !target.isOnline) {
            SocketManager.GAME_SEND_GROUP_INVITATION_ERROR(this, "n" + name)
            return
        }
        if (target.party != null) {
            SocketManager.GAME_SEND_GROUP_INVITATION_ERROR(this, "a" + name)
            return
        }
        if (target.getGroup() != null && this.player.getGroup() == null) {
            if (!target.getGroup()!!.isPlayer) {
                SocketManager.GAME_SEND_MESSAGE(this.player, this.player.getLang().trans("game.gameclient.inviteparty.noauthorize"))
                return
            }
        }
        if (this.player.party != null && this.player.party!!.players.size == 8) {
            SocketManager.GAME_SEND_GROUP_INVITATION_ERROR(this, "f")
            return
        }

        target.inviting = this.player.id
        this.player.inviting = target.id
        SocketManager.GAME_SEND_GROUP_INVITATION(this, this.player.name, name)
        SocketManager.GAME_SEND_GROUP_INVITATION(target.getGameClient()!!, this.player.name, name)
    }

    private fun refuseInvitation() {
        if (!this::player.isInitialized || this.player.inviting == 0)
            return

        var player: Player? = World.world.getPlayer(this.player.inviting!!)

        if (player != null) {
            player.inviting = 0
            SocketManager.GAME_SEND_PR_PACKET(player)
        }

        this.player.inviting = 0
    }

    private fun leaveParty(packet: String) {
        var party: Party? = this.player.party

        if (party != null) {
            if (packet.length == 2) { // player leave group
                party.leave(this.player)
                SocketManager.GAME_SEND_PV_PACKET(this, "")
                SocketManager.GAME_SEND_IH_PACKET(this.player, "")
            } else if (party.isChief(this.player.id)) { // kick player from group
                var id: Int
                try {
                    id = (packet.substring(2)).toInt()
                } catch (e: NumberFormatException) {
                    return
                }

                var target: Player = World.world.getPlayer(id)!!
                party.leave(target)
                SocketManager.GAME_SEND_PV_PACKET(target.getGameClient()!!, (this.player.id).toString())
                SocketManager.GAME_SEND_IH_PACKET(target, "")
            }
        }
    }

    private fun whereIsParty() {
        if (!this::player.isInitialized)
            return
        var g: Party? = this.player.party
        if (g == null)
            return
        var str: String = ""
        var isFirst: Boolean = true
        for (GroupP in  this.player.party!!.players) {
            if (!isFirst)
                str += "|"
            str += GroupP.curMap.x.toString() + ";" + GroupP.curMap.y.toString() + ";" + GroupP.curMap.id + ";2;" + GroupP.id.toString() + ";" + GroupP.name
            isFirst = false
        }
        SocketManager.GAME_SEND_IH_PACKET(this.player, str)
    }

    /** Fin Group Packet **/

    /**
     * MountPark Packet *
     */
    private fun parseMountPacket(packet: String) {
        when (packet[1]) {
'b' -> {buyMountPark(packet)
                
}
'd' -> {dataMount(packet)
                
}
'p' -> {dataMount(packet)
                
}
'f' -> {killMount(packet)
                
}
'n' -> {renameMount(packet.substring(2))
                
}
'r' -> {rideMount()
                
}
's' -> {sellMountPark(packet)
                
}
'v' -> {SocketManager.GAME_SEND_R_PACKET(this.player, "v")
                
}
'x' -> {setXpMount(packet)
                
}
'c' -> {castrateMount()
                
}
'o' -> {removeObjectInMountPark(packet)
                
}
}
    }

    private fun buyMountPark(packet: String) {
        SocketManager.GAME_SEND_R_PACKET(this.player, "v");//Fermeture du panneau
        var MP: MountPark = this.player.curMap.mountPark!!
        var Seller: Player = World.world.getPlayer(MP.owner)!!
        if (MP.owner == -1) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "196")
            return
        }
        if (MP.price == 0) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "197")
            return
        }
        if (this.player.getGuild() == null) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "1135")
            return
        }
        if (this.player.guildMember!!.rank != 1) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "198")
            return
        }
        if((System.currentTimeMillis() - this.player.getGuild()!!.date) <= 3600000L /**2419200000L**/) {//FIXME Ankalike: 2 semaines, en commentaire = 2 mois officiel
            SocketManager.GAME_SEND_MESSAGE(this.player, this.player.getLang().trans("game.gameclient.buy.mountpark.wait"))
            //this.player.send("Im1103");
            return
        }
        var enclosMax: Byte = (Math.floor(this.player.getGuild()!!.lvl.toDouble() / 10).toInt().toByte())
        var TotalEncloGuild: Byte = (World.world.totalMPGuild(this.player.getGuild()!!.id).toByte())
        if (TotalEncloGuild >= enclosMax) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "1103")
            return
        }
        if (this.player.kamas < MP.price) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "182")
            return
        }
        var NewKamas: Long = this.player.kamas - MP.price
        this.player.kamas = NewKamas
        if (Seller != null) {
            var NewSellerBankKamas: Long = Seller.getBankKamas() + MP.price
            Seller.setBankKamas(NewSellerBankKamas)
            if (Seller.isOnline) {
                SocketManager.GAME_SEND_MESSAGE(this.player, this.player.getLang().trans("Vous venez de vendre votre enclos ! ", MP.price))
            }
        }
        MP.price = 0;//On vide le prix
        MP.owner = this.player.id
        MP.guild = this.player.getGuild()
        DatabaseManager.get(MountParkData::class.java).update(MP)
        DatabaseManager.get(PlayerData::class.java).update(this.player)
        //On rafraichit l'enclo
        for (z in  this.player.curMap.players) {
            SocketManager.GAME_SEND_Rp_PACKET(z, MP)
        }
    }

    private fun dataMount(packet: String) {
        try {
            var id: Int = (packet.substring(2).split("|")[0]).toInt()

            if (id != 0) {
                var mount: Mount? = World.world.getMountById(id)
                if (mount != null)
                    SocketManager.GAME_SEND_MOUNT_DESCRIPTION_PACKET(this.player, mount)
            }
        } catch (ignored: Exception) {}
    }

    private fun killMount(packet: String) {
        if (this.player.mount!!.objects.size != 0) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "1106")
            return
        }

        if (this.player.mount != null && this.player.onMount)
            this.player.toogleOnMount()
        SocketManager.GAME_SEND_Re_PACKET(this.player, "-", this.player.mount)
        DatabaseManager.get(MountData::class.java).delete(this.player.mount!!)
        World.world.removeMount(this.player.mount!!.id)
        this.player.mount = null
    }

    private fun renameMount(name: String) {
        if (this.player.mount == null)
            return
        this.player.mount!!.name = name
        DatabaseManager.get(MountData::class.java).update(this.player.mount!!)
        SocketManager.GAME_SEND_Rn_PACKET(this.player, name)
    }

    private fun rideMount() {
        if (!this.player.isSubscribe()) {
            SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(this, 'S')
            return
        }

        this.player.toogleOnMount()
    }

    private fun sellMountPark(packet: String) {
        SocketManager.GAME_SEND_R_PACKET(this.player, "v");//Fermeture du panneau
        var price: Int = (packet.substring(2)).toInt()
        var MP1: MountPark = this.player.curMap.mountPark!!
        if (!MP1.getEtable().isEmpty() || !MP1.getListOfRaising().isEmpty()) {
            SocketManager.GAME_SEND_MESSAGE(this.player, this.player.getLang().trans("game.gameclient.sellmountpark.mountparkfull"))
            return
        }
        if (MP1.owner == -1) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "194")
            return
        }
        if (MP1.owner != this.player.id) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "195")
            return
        }
        MP1.price = price
        DatabaseManager.get(MountParkData::class.java).update(MP1)
        DatabaseManager.get(PlayerData::class.java).update(this.player)
        //On rafraichit l'enclo
        for (z in  this.player.curMap.players) {
            SocketManager.GAME_SEND_Rp_PACKET(z, MP1)
        }
    }

    private fun setXpMount(packet: String) {
        try {
            var xp: Int = (packet.substring(2)).toInt()
            if (xp < 0)
                xp = 0
            if (xp > 90)
                xp = 90
            this.player.mountXpGive = xp
            SocketManager.GAME_SEND_Rx_PACKET(this.player)
        } catch (e: Exception) {
            log.error("unexpected error", e)
        }
    }

    private fun castrateMount() {
        if (this.player.mount == null) {
            SocketManager.GAME_SEND_Re_PACKET(this.player, "Er", null)
            return
        }
        this.player.mount!!.setCastrated()
        SocketManager.GAME_SEND_Re_PACKET(this.player, "+", this.player.mount)
    }

    private fun removeObjectInMountPark(packet: String) {
        var cell: Int = (packet.substring(2)).toInt()
        var map: GameMap = this.player.curMap
        if (map.mountPark == null)
            return
        var MP: MountPark = map.mountPark

        if (this.player.getGuild() == null) {
            SocketManager.GAME_SEND_BN(this)
            return
        }
        if (!this.player.guildMember!!.canDo(8192)) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "193")
            return
        }

        var item: Int = MP.getCellAndObject()[cell]!!
        var t: ObjectTemplate = World.world.getObjTemplate(item)!!
        var obj: GameObject = t.createNewItem(1, false)!! // creation de l'item au stats incorrecte!!

        var statNew: Int = 0// on vas chercher la valeur de la resistance de l'item
        for (entry in  MP.objDurab!!.entries) {
            if (entry.key.equals(cell)) {
                for (entry2 in  entry.value.entries)
                    statNew = entry2.value
            }
        }
        obj.txtStat.remove(812); //on retire les stats "32c"
        obj.addTxtStat(812, Integer.toHexString(statNew));// on ajthis les bonnes stats

        if (this.player.addItem(obj, true, false))//Si le joueur n'avait pas d'item similaire
            World.world.addGameObject(obj)
        if (MP.delObject(cell))
            SocketManager.SEND_GDO_PUT_OBJECT_MOUNT(map, cell.toString() + ";0;0"); // on retire l'objet de la map
    }

    /** Fin MountPark Packet **/

    /**
     * Quest Packet *
     */
    private fun parseQuestData(packet: String) {
        when (packet[1]) {
'L' -> {player.send(player.encodeQuestList())
                
}
'S' -> {var id: Int = (packet.substring(2)).toInt()
                player.sendQuestStatus(id)
                
}
}
    }
    /** Fin Quest Packet **/

    /**
     * Spell Packet *
     */
    private fun parseSpellPacket(packet: String) {
        when (packet[1]) {
'B' -> {boostSpell(packet)
                
}
'F' -> {forgetSpell(packet)
                
}
'M' -> {moveSpell(packet)
                
}
'R' -> {removeSpell(packet)
                
}
}
    }

    private fun boostSpell(packet: String) {
        try {
            var id: Int = (packet.substring(2)).toInt()

            if (this.player.boostSpell(id)) {
                SocketManager.GAME_SEND_SPELL_UPGRADE_SUCCESS(this, id, this.player.getSortStatBySortIfHas(id)!!.level)
                SocketManager.GAME_SEND_STATS_PACKET(this.player)
            } else {
                SocketManager.GAME_SEND_SPELL_UPGRADE_FAILED(this)
            }
        } catch (e: NumberFormatException) {
            log.error("unexpected error", e)
            SocketManager.GAME_SEND_SPELL_UPGRADE_FAILED(this)
        }
    }

    private fun forgetSpell(packet: String) {
        if (this.player.exchangeAction == null || this.player.exchangeAction!!.getType() != ExchangeAction.FORGETTING_SPELL)
            return
        var id: Int = (packet.substring(2)).toInt()
        if(id == -1)
            this.player.exchangeAction = null
        if (this.player.forgetSpell(id)) {
            SocketManager.GAME_SEND_SPELL_UPGRADE_SUCCESS(this, id, this.player.getSortStatBySortIfHas(id)!!.level)
            SocketManager.GAME_SEND_STATS_PACKET(this.player)
            this.player.exchangeAction = null
        }
    }

    private fun removeSpell(packet: String) {
        var packet = packet
        packet = packet.substring(2)

        // Can happen when client gets into a weird state
        if(packet.equals("undefined")) return

        var position: Int = (packet).toInt()
        this.player.removeSpellShortcutAtPosition(position)
        this.player.send("SR"+position)
    }

    private fun moveSpell(packet: String) {
        var parts: List<String> = packet.substring(2).split("|")

        var spellID: Int = (parts[0]).toInt()
        var position: Int = -1
        if(parts.size > 1) {
            position = (parts[1]).toInt(); // May return -1
        }

        var spellStats: Spell.SortStats? = this.player.getSortStatBySortIfHas(spellID)
        if (spellStats != null) {
            this.player.setSpellShortcuts(spellID, position)

            // After 1.41, we need to send back the SM packet
            this.player.send("SM"+spellID+"|"+position)
            // Before 1.41, we only send BN
            SocketManager.GAME_SEND_BN(this)
        }
    }

    /** Fin Spell Packet **/

    /**
     * Waypoint Packet *
     */
    private fun parseWaypointPacket(packet: String) {
        when (packet[1]) {
'U' -> {waypointUse(packet)
                
}
'u' -> {zaapiUse(packet)
                
}
'p' -> {prismUse(packet)
                
}
'V' -> {waypointLeave()
                
}
'v' -> {zaapiLeave()
                
}
'w' -> {prismLeave()
                
}
}
    }

    private fun waypointUse(packet: String) {
        try {
            val id: Int = (packet.substring(2)).toInt()
            val party: Party? = this.player.party

            if(party != null && this.player.fight == null && party.master != null && party.master!!.name.equals(this.player.name)) {
                party.players.stream().filter({ follower1 -> party.isWithTheMaster(follower1, false, false) && follower1.exchangeAction == null }).forEach { follower ->
                    follower.exchangeAction = ExchangeAction<Any?>(ExchangeAction.IN_ZAAPING, null)
                    follower.useZaap(id)
                }
            }

            this.player.useZaap(id)
        } catch (e: Exception) {
            log.error("unexpected error", e)
        }
    }

    private fun zaapiUse(packet: String) {
        if (this.player.deshonor >= 2) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "183")
            return
        }
        val party: Party? = this.player.party

        if(party != null && this.player.fight == null && party.master != null && party.master!!.name.equals(this.player.name)) {
            party.players.stream().filter({ follower1 -> party.isWithTheMaster(follower1, false, false) && follower1.exchangeAction == null }).forEach { follower ->
                follower.exchangeAction = ExchangeAction<Any?>(ExchangeAction.IN_ZAPPI, null)
                follower.gameClient!!.zaapiUse(packet)
            }
        }

        this.player.Zaapi_use(packet)
    }

    private fun prismUse(packet: String) {
        if (this.player.deshonor >= 2) {
            SocketManager.GAME_SEND_Im_PACKET(this.player, "183")
            return
        }
        this.player.usePrisme(packet)
    }

    private fun waypointLeave() {
        this.player.stopZaaping()
    }

    private fun zaapiLeave() {
        this.player.Zaapi_close()
    }

    private fun prismLeave() {
        this.player.Prisme_close()
    }

    /** Fin Waypoint Packet **/

    /**
     * Other *
     */
    private fun parseTutorialsPacket(packet: String) {
        if(this.player.exchangeAction == null || this.player.exchangeAction!!.getType() != ExchangeAction.IN_SCENARIO)
            return
        var param: List<String> = packet.split("|")
        var sad: ScenarioActionData = (this.player.exchangeAction!!.getValue() as ScenarioActionData)

        if(packet[1] != 'V') {
            return
        }
        var succeed: Boolean = packet[2] == '1'

        // Move player to expected cell (FIXME can probably be used to teleport)
        this.player.orientation = param[2].toByte().toInt()
        this.player.curCell.removePlayer(this.player)
        var cell: GameCase = this.player.curMap.getCase(param[1].toShort().toInt())!!
        cell.addPlayer(this.player)
        this.player.curCell = cell

        sad.onCompletion(this.player, succeed)
    }

    /**
     * Fin Other *
     */

    fun kick() {
        if(this.session.isConnected())
            this.session.close(true)
    }

    fun disconnect() {
        if (this::account.isInitialized && this::player.isInitialized)
            this.account.disconnect(this.player)
    }

    fun addAction(GA: GameAction) {
        actions[GA.id] = GA
        if (GA.actionId == 1)
            walk = true

        if (Config.debug) {
            World.world.logger.error("Game > Create action id : " + GA.id)
            World.world.logger.error("Game > Packet : " + GA.packet)
        }
    }

    @Synchronized fun removeAction(GA: GameAction) {
        if (GA.actionId == 1)
            walk = false
        if (Config.debug)
            World.world.logger.debug("Game >  Delete action id : " + GA.id)
        actions.remove(GA.id)

        if (actions[-1] != null && GA.actionId == 1)//Si la queue est pas vide
        {
            //et l'actionID remove = Deplacement
            //int cellID = -1;
            var packet: String = actions[-1]!!.packet!!.substring(5)
            var cell: Int = (packet.split(";")[0]).toInt()
            var list: ArrayList<Int>? = null
            try {
                list = PathFinding.getAllCaseIdAllDirrection(cell, this.player.curMap)
                //cellID = Pathfinding.getNearestCellAroundGA(this.player.getCurMap(), cell, this.player.getCurCell().getId(), null);
            } catch (e: Exception) {
                log.error("unexpected error", e)
            }

            //cellID == this.player.getCurCell().getId()
            if ((list != null && list.contains(this.player.curCell.getId())) || distPecheur())// et on verrifie si le joueur = cellI
                this.player.getGameClient()!!.gameAction(actions[-1]!!);// On renvois comme demande
                //Risqu? mais bon pas le choix si on veut pas ?tre emmerder avec les bl?s. Parser le bon type ?
                //this.player.getGameClient().gameAction(actions.getWaitingAccount(-1));// On renvois comme demande
            actions.remove(-1)
        }
    }

    private fun distPecheur(): Boolean {
        try {
            var packet: String = actions[-1]!!.packet!!.substring(5)
            var SM: JobStat? = this.player.getMetierBySkill((packet.split(";")[1]).toInt())
            if (SM == null)
                return false
            if (SM.template == null)
                return false
            if (SM.template.id != 36)
                return false
            var dis: Int = PathFinding.getDistanceBetween(this.player.curMap, (packet.split(";")[0]).toInt(), this.player.curCell.getId())
            var dist: Int = JobConstant.getDistCanne(this.player.getObjetByPos(Constant.ITEM_POS_ARME)!!.template!!.id)
            if (dis <= dist)
                return true
        } catch (e: Exception) {
            log.error("unexpected error", e)
        }
        return false
    }
    
    fun changeName(packet: String) {
        if(!this.player.hasItemTemplate(10860, 1, false)) {
            this.player.send("AlEr")
            this.player.sendMessage(this.player.getLang().trans("game.gameclient.changename.none.potion"))
            return
        }

        val name: String = packet
        var isValid: Boolean = true

        if (name.length > 20 || name.length < 3 || name.contains("modo") || name.contains("admin") || name.contains("putain") || name.contains("administrateur") || name.contains("puta"))
            isValid = false
        if (isValid) {
            var tiretCount: Int = 0
            var exLetterA: Char = ' '
            var exLetterB: Char = ' '
            for (curLetter in  name.toCharArray()) {
                if (!(((curLetter >= 'a' && curLetter <= 'z') || (curLetter >= 'A' && curLetter <= 'Z')) || curLetter == '-')) {
                    isValid = false
                    break
                }
                if (curLetter == exLetterA && curLetter == exLetterB) {
                    isValid = false
                    break
                }
                if (curLetter >= 'a' && curLetter <= 'z') {
                    exLetterA = exLetterB
                    exLetterB = curLetter
                }
                if (curLetter == '-') {
                    if (tiretCount >= 1) {
                        isValid = false
                        break
                    } else {
                        tiretCount++
                    }
                }
            }
        }

        if(DatabaseManager.get(PlayerData::class.java).exist(name) || !isValid) {
            this.player.send("AlEs")
            return
        }

        this.player.name = name
        this.player.send("AlEr")
        this.player.removeItemByTemplateId(10860, 1, false)
        SocketManager.GAME_SEND_ALTER_GM_PACKET(this.player.curMap, this.player)
    }

    fun send(packet0: String) {
        var packet = packet0
        try {
            if (Config.encryption && this::preparedKeys.isInitialized)
                packet = World.world.cryptManager.cryptMessage(packet, this.preparedKeys)
            this.getSession().write(packet)
        } catch (e: Exception) {
            Logging.getInstance().write("Error", "Send fail : " + packet)
            log.error("unexpected error", e)
        }
    }
    
    fun send(abstractDofusMessage: AbstractDofusMessage) {
        if(getSession() != null && !getSession().isClosing() && getSession().isConnected()) {
            abstractDofusMessage.serialize()
            LoggerFactory.getLogger(GameClient::class.java).debug("Send : " + abstractDofusMessage.toString())
            send(abstractDofusMessage.output.toString())
        }
    }
}
