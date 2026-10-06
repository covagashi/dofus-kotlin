package org.starloco.locos.entity.exchange

import org.starloco.locos.client.Player
import org.starloco.locos.common.Formulas
import org.starloco.locos.common.SocketManager
import org.starloco.locos.game.world.World
import org.starloco.locos.game.world.World.Couple
import org.starloco.locos.job.JobConstant
import org.starloco.locos.job.JobStat
import org.starloco.locos.kernel.Config
import org.starloco.locos.kernel.Constant
import org.starloco.locos.`object`.GameObject

class CraftSecure(player1: Player, player2: Player) : PlayerExchange(player1, player2) {

    private var payKamas: Long = 0
    private var payIfSuccessKamas: Long = 0
    var maxCase = 9
        private set

    private val payItems = ArrayList<Couple<Int, Int>>()
    private val payItemsIfSuccess = ArrayList<Couple<Int, Int>>()

    init {
        val job = this.player1.getMetierBySkill(this.player1.craftingType[1])
        if (job != null && job.template != null) {
            this.maxCase = if (job.template!!.isMaging()) 3 else JobConstant.getTotalCaseByJobLevel(job.get_lvl())
        } else {
            this.maxCase = 0
            this.cancel()
        }
    }

    fun getNeeder(): Player = player2

    @Synchronized
    override fun apply() {
        val jobStat = this.player1.getMetierBySkill(this.player1.craftingType[1]) ?: return

        val jobAction = jobStat.getJobActionBySkill(this.player1.craftingType[1]) ?: return

        val items = HashMap<Player, ArrayList<Couple<Int, Int>>>()
        items[this.player1] = this.items1
        items[this.player2] = this.items2

        val sizeList = jobAction.sizeList(items)

        val success = jobAction.craftPublicMode(this.player1, this.player2, items)

        this.player1.addKamas(payKamas + (if (success) payIfSuccessKamas else 0))
        this.player2.addKamas(-payKamas - (if (success) payIfSuccessKamas else 0))


        if (success) this.giveObjects(this.payItems, this.payItemsIfSuccess)
        else this.giveObjects(this.payItems)

        var winXP = 0
        if (success)
            winXP = Formulas.calculXpWinCraft(jobStat.get_lvl(), sizeList.toInt()) * Config.rateJob
        else if (!jobStat.template!!.isMaging())
            winXP = Formulas.calculXpWinCraft(jobStat.get_lvl(), sizeList.toInt()) * Config.rateJob

        if (winXP > 0) {
            jobStat.addXp(this.player1, winXP.toLong())
            val SMs = ArrayList<JobStat>()
            SMs.add(jobStat)
            SocketManager.GAME_SEND_JX_PACKET(this.player1, SMs)
        }

        SocketManager.GAME_SEND_STATS_PACKET(this.player1)
        SocketManager.GAME_SEND_STATS_PACKET(this.player2)

        this.payIfSuccessKamas = 0
        this.payKamas = 0
        this.payItems.clear()
        this.payItemsIfSuccess.clear()
        this.items1.clear()
        this.items2.clear()
        this.ok1 = false
        this.ok2 = false
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player1.gameClient!!, ok1, this.player1.id)
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player2.gameClient!!, ok1, this.player1.id)
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player1.gameClient!!, ok2, this.player2.id)
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player2.gameClient!!, ok2, this.player2.id)
    }

    private fun giveObjects(vararg arrays: ArrayList<Couple<Int, Int>>) {
        for (array in arrays) {
            for (couple in array) {
                if (couple.second == 0)
                    continue

                val `object` = World.world.getGameObject(couple.first) ?: continue
                if (`object`.position != Constant.ITEM_POS_NO_EQUIPED)
                    continue
                if (!this.player2.hasItemGuid(couple.first)) {
                    couple.second = 0
                    continue
                }

                this.giveObject(couple, `object`)
            }
        }
    }

    @Synchronized
    override fun cancel() {
        this.send("EV")
        this.player1.craftingType.clear()
        this.player2.craftingType.clear()
        this.player1.exchangeAction = null
        this.player2.exchangeAction = null
    }

    fun setPayKamas(type: Byte, kamas: Long) {
        var kamas = kamas
        if (kamas < 0)
            return
        if (this.player2.kamas < kamas)
            kamas = this.player2.kamas

        this.ok1 = false
        this.ok2 = false
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player1.gameClient!!, ok1, this.player1.id)
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player2.gameClient!!, ok1, this.player1.id)
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player1.gameClient!!, ok2, this.player2.id)
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player2.gameClient!!, ok2, this.player2.id)

        when (type.toInt()) {
            1 -> {// Pay
                if (this.payIfSuccessKamas > 0 && kamas + this.payIfSuccessKamas > this.player2.kamas)
                    kamas -= this.payIfSuccessKamas

                this.payKamas = kamas
                this.send("Ep1;G" + this.payKamas)
            }
            2 -> { // PayIfSuccess
                if (this.payKamas > 0 && kamas + this.payKamas > this.player2.kamas)
                    kamas -= this.payKamas

                this.payIfSuccessKamas = kamas
                this.send("Ep2;G" + this.payIfSuccessKamas)
            }
        }
    }

    fun setPayItems(type: Byte, adding: Boolean, guid: Int, quantity: Int) {
        val `object` = World.world.getGameObject(guid) ?: return
        if (`object`.position != Constant.ITEM_POS_NO_EQUIPED || `object`.isAttach)
            return

        this.ok1 = false
        this.ok2 = false
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player1.gameClient!!, ok1, this.player1.id)
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player2.gameClient!!, ok1, this.player1.id)
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player1.gameClient!!, ok2, this.player2.id)
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player2.gameClient!!, ok2, this.player2.id)

        if (adding) {
            this.addItem(`object`, quantity, type)
        } else {
            this.removeItem(`object`, quantity, type)
        }
    }

    private fun addItem(`object`: GameObject, quantity: Int, type: Byte) {
        var quantity = quantity
        if (`object`.quantity < quantity)
            quantity = `object`.quantity

        val items = if (type.toInt() == 1) this.payItems else this.payItemsIfSuccess
        val couple = getCoupleInList(items, `object`.guid)
        val add = "|" + `object`.template!!.id + "|" + `object`.encodeStats()

        if (couple != null) {
            couple.second += quantity
            this.player2.send("Ep" + type + ";O+" + `object`.guid + "|" + couple.second)
            this.player1.send("Ep" + type + ";O+" + `object`.guid + "|" + couple.second + add)
            return
        }

        items.add(Couple(`object`.guid, quantity))
        this.player2.send("Ep" + type + ";O+" + `object`.guid + "|" + quantity)
        this.player1.send("Ep" + type + ";O+" + `object`.guid + "|" + quantity + add)
    }

    private fun removeItem(`object`: GameObject, quantity: Int, type: Byte) {
        val items = if (type.toInt() == 1) this.payItems else this.payItemsIfSuccess
        val couple = getCoupleInList(items, `object`.guid) ?: return
        val newQua = couple.second - quantity

        if (newQua < 1) {
            items.remove(couple)
            this.player1.send("Ep" + type + ";O-" + `object`.guid)
            this.player2.send("Ep" + type + ";O-" + `object`.guid)
        } else {
            couple.second = newQua
            this.player2.send("Ep" + type + ";O+" + `object`.guid + "|" + newQua)
            this.player1.send("Ep" + type + ";O+" + `object`.guid + "|" + newQua + "|" + `object`.template!!.id + "|" + `object`.encodeStats())
        }
    }

    private fun send(packet: String) {
        this.player1.send(packet)
        this.player2.send(packet)
    }
}
