package org.starloco.locos.script.proxy

import org.classdump.luna.ByteString
import org.classdump.luna.Table
import org.classdump.luna.impl.DefaultUserdata
import org.classdump.luna.impl.ImmutableTable
import org.classdump.luna.lib.ArgumentIterator
import org.classdump.luna.runtime.LuaFunction
import org.starloco.locos.client.Player
import org.starloco.locos.common.SocketManager
import org.starloco.locos.entity.monster.MobGroupDef
import org.starloco.locos.entity.monster.MonsterGroup
import org.starloco.locos.game.action.ExchangeAction
import org.starloco.locos.game.action.type.NpcDialogActionData
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant
import org.starloco.locos.quest.QuestProgress
import org.starloco.locos.script.DataScriptVM
import org.starloco.locos.script.ScriptVM
import org.starloco.locos.script.types.MetaTables
import org.starloco.locos.util.Pair
import java.util.Collections
import java.util.Optional

class SPlayer(userValue: Player) : DefaultUserdata<Player>(META_TABLE, userValue) {

    private companion object {
        val META_TABLE: ImmutableTable = MetaTables.MetaTable(MetaTables.ReflectIndexTable(SPlayer::class.java))

        //region Basic stuff
        @JvmStatic
        private fun id(p: Player): Int = p.id

        @JvmStatic
        private fun name(p: Player): String = p.name

        @JvmStatic
        private fun level(p: Player): Int = p.level

        @JvmStatic
        private fun breed(p: Player): Int = p.classe

        @JvmStatic
        private fun gender(p: Player): Int = p.sexe

        @JvmStatic
        private fun faction(p: Player): Int = p.alignment

        @JvmStatic
        private fun openBank(p: Player) {
            p.openBank()
        }

        @JvmStatic
        private fun account(p: Player): SAccount = p.account.scripted()

        @JvmStatic
        private fun savePosition(p: Player, args: ArgumentIterator) {
            val mapId = args.nextInt()
            val cellId = args.nextInt()
            val send = args.nextOptionalBoolean(true)

            p.setSavePos(mapId, cellId)
            if (send) {
                SocketManager.GAME_SEND_Im_PACKET(p, "06")
            }
        }

        @JvmStatic
        private fun openZaap(p: Player) {
            p.openZaapMenu()
        }

        @JvmStatic
        private fun openTrunk(p: Player, args: ArgumentIterator) {
            val cellID = args.nextInt()
            p.openTrunk(cellID)
        }

        @JvmStatic
        private fun setExchangeAction(p: Player, args: ArgumentIterator): Boolean {
            if (p.exchangeAction != null) return false

            val t = args.nextInt()
            val v = args.nextOptionalAny(null)

            p.exchangeAction = ExchangeAction(t.toByte(), v)
            return true
        }

        @JvmStatic
        private fun clearExchangeAction(p: Player, args: ArgumentIterator): Boolean {
            val t = args.nextInt()

            val a = p.exchangeAction
            if (a!!.getType().toInt() != t) {
                return false
            }

            p.exchangeAction = null
            return true
        }

        @JvmStatic
        private fun useCraftSkill(p: Player, args: ArgumentIterator) {
            val skillId = args.nextInt()
            val ingredientsCount = args.nextInt()

            p.useCraftSkill(skillId, ingredientsCount)
        }

        @JvmStatic
        private fun getCtxVal(p: Player, args: ArgumentIterator): Any? {
            val key = args.nextString().toString()
            return Optional.ofNullable(p.exchangeAction).map { it.getContextValue(key) }.orElse(null)
        }

        @JvmStatic
        private fun setCtxVal(p: Player, args: ArgumentIterator): Boolean {
            val key = args.nextString().toString()
            val `val` = args.next()

            if (p.exchangeAction == null) return false

            p.exchangeAction!!.putContextValue(key, `val`)
            return true
        }

        @JvmStatic
        private fun addXP(p: Player, args: ArgumentIterator): Boolean {
            val xp = args.nextInteger()
            val show = args.nextOptionalBoolean(true)

            if (show) SocketManager.GAME_SEND_Im_PACKET(p, "08;$xp")
            p.addXp(xp)
            return true
        }


        @JvmStatic
        private fun life(p: Player): Int = p.curPdv

        @JvmStatic
        private fun maxLife(p: Player): Int = p.maxPdv

        @JvmStatic
        private fun modLife(p: Player, args: ArgumentIterator) {
            val life = args.nextInt()

            p.setPdv(p.curPdv + life)
        }

        @JvmStatic
        private fun setLifePercent(p: Player, args: ArgumentIterator) {
            val percent = args.nextInt()

            p.setPdv(((p.maxPdv * 100L) / percent).toInt())
        }

        @JvmStatic
        private fun energy(p: Player): Int = p.energy

        @JvmStatic
        private fun modEnergy(p: Player, args: ArgumentIterator) {
            val energy = args.nextInt()

            val newEnergy = (p.energy + energy).coerceIn(0, 10000)
            p.energy = newEnergy

            if (newEnergy == 0) p.setGhost()
        }

        @JvmStatic
        private fun isGhost(p: Player): Boolean = p.isGhost

        @JvmStatic
        private fun resurrect(p: Player): Boolean {
            if (!p.isGhost) return false
            p.setAlive()
            return true
        }

        @JvmStatic
        private fun sendAction(p: Player, args: ArgumentIterator) {
            val actionID = args.nextInt()
            var actionIDStr = ""
            if (actionID != -1) actionIDStr = actionID.toString()

            val actionType = args.nextInt()
            val actionValue = args.nextString().toString()

            SocketManager.GAME_SEND_GA_PACKET(p.gameClient!!, actionIDStr, actionType.toString(), p.id.toString(), actionValue)
        }

        @JvmStatic
        private fun sendInfoMsg(p: Player, args: ArgumentIterator) {
            val type = args.nextInt()
            if (type > 9) throw IllegalArgumentException("SPlayer:sendInfoMsg type param must be < 10")
            val msgId = args.nextInt()

            SocketManager.GAME_SEND_Im_PACKET(p, "$type$msgId")
        }

        @JvmStatic
        private fun startScenario(p: Player, args: ArgumentIterator) {
            val id = args.nextInt()
            val date = args.nextString()
            val onEnd: LuaFunction<*, *, *, *, *> = args.nextFunction()

            p.startScenario(id, date.toString()) { player, succeed -> DataScriptVM.getInstance()!!.call(onEnd, player.scripted(), succeed) }
        }

        @JvmStatic
        private fun openDocument(p: Player, args: ArgumentIterator) {
            val id = args.nextInt()
            val date = args.nextString().toString()

            p.openDocument(id, date)
        }
        //endregion

        //region Dialogs
        @JvmStatic
        private fun ask(p: Player, args: ArgumentIterator) {
            val question = args.nextInt()
            val answersInts = ScriptVM.intsFromLuaTable(args.nextOptionalTable(null))
            val param = args.nextOptionalString(null)

            val data = p.exchangeAction!!.getValue() as NpcDialogActionData
            data.questionId = question
            data.setAnswers(answersInts)

            val paramVal = if (param == null) null else p.getStringVar(param.toString())
            SocketManager.GAME_SEND_QUESTION_PACKET(p.gameClient!!, question, answersInts, paramVal!!)
        }

        @JvmStatic
        private fun endDialog(p: Player) {
            if (p.exchangeAction == null || p.exchangeAction!!.getType() != ExchangeAction.TALKING_WITH) {
                return
            }
            p.away = false
            p.exchangeAction = null
            SocketManager.GAME_SEND_END_DIALOG_PACKET(p.gameClient!!)
        }

        @JvmStatic
        private fun pauseDialog(p: Player) {
            if (p.exchangeAction == null || p.exchangeAction!!.getType() != ExchangeAction.TALKING_WITH) {
                return
            }
            p.away = false
            p.exchangeAction = null
            SocketManager.GAME_SEND_PAUSE_DIALOG_PACKET(p.gameClient!!)
        }
        //endregion

        //region Quests (Private)

        @JvmStatic
        private fun _questAvailable(p: Player, args: ArgumentIterator): Boolean {
            return p.getQuestProgress(args.nextInt()) == null
        }

        @JvmStatic
        private fun _questFinished(p: Player, args: ArgumentIterator): Boolean {
            return Optional.ofNullable(p.getQuestProgress(args.nextInt())).map { it.isFinished() }.orElse(false)
        }

        @JvmStatic
        private fun _questOngoing(p: Player, args: ArgumentIterator): Boolean {
            return Optional.ofNullable(p.getQuestProgress(args.nextInt())).map { !it.isFinished() }.orElse(false)
        }

        @JvmStatic
        private fun _ongoingQuests(p: Player): Table {
            return ScriptVM.listOf(p.getQuestProgressions().map { it.questId })
        }

        @JvmStatic
        private fun _startQuest(p: Player, args: ArgumentIterator): Boolean {
            val id = args.nextInt()
            val sId = args.nextInt()
            val isAccountQuest = args.nextOptionalBoolean(false)

            if (p.getQuestProgress(id) != null) return false

            val progressOwnerId = if (isAccountQuest) QuestProgress.NO_PLAYER_ID else p.id

            p.addQuestProgression(QuestProgress(p.accID, progressOwnerId, id, sId))

            SocketManager.GAME_SEND_Im_PACKET(p, "054;$id")
            p.saveQuestProgress()
            return true
        }

        @JvmStatic
        private fun _currentStep(p: Player, args: ArgumentIterator): Int {
            val qID = args.nextInt()

            val qp = p.getQuestProgress(qID) ?: return 0
            if (qp.isFinished()) return 0

            return qp.getCurrentStep()
        }

        @JvmStatic
        private fun _completedObjectives(p: Player, args: ArgumentIterator): Table? {
            val qID = args.nextInt()

            val qp = p.getQuestProgress(qID)
            if (qp!!.isFinished()) return null

            return ScriptVM.listOf(qp!!.getCompletedObjectives().stream())
        }

        @JvmStatic
        private fun _completeObjective(p: Player, args: ArgumentIterator): Boolean {
            val qID = args.nextInt()
            val oID = args.nextInt()

            val qp = p.getQuestProgress(qID)
            if (qp!!.isFinished()) return false

            if (qp!!.hasCompletedObjective(oID)) return false

            qp!!.completeObjective(oID)
            SocketManager.GAME_SEND_Im_PACKET(p, "055;$qID")

            p.saveQuestProgress()
            return true
        }

        @JvmStatic
        private fun _setCurrentStep(p: Player, args: ArgumentIterator): Boolean {
            val qID = args.nextInt()
            val sID = args.nextInt()

            val qp = p.getQuestProgress(qID)
            if (qp!!.isFinished()) return false
            if (qp!!.getCurrentStep() == sID) return false

            qp!!.setCurrentStep(sID)

            p.saveQuestProgress()
            return true
        }

        @JvmStatic
        private fun _completeQuest(p: Player, args: ArgumentIterator): Boolean {
            val qID = args.nextInt()
            val remove = args.nextOptionalBoolean(false)

            val qp = p.getQuestProgress(qID)
            if (qp!!.isFinished()) return false

            SocketManager.GAME_SEND_Im_PACKET(p, "056;$qID")
            if (remove) {
                p.delQuestProgress(qp!!)
                return true
            }
            qp!!.markFinished()
            p.saveQuestProgress()

            return true
        }

        //endregion

        //region Emotes
        @JvmStatic
        fun hasEmote(p: Player, args: ArgumentIterator): Boolean {
            val emote = args.nextInt()
            return p.emotes.contains(emote)
        }

        @JvmStatic
        fun learnEmote(p: Player, args: ArgumentIterator): Boolean {
            val emote = args.nextInt()
            return p.addStaticEmote(emote)
        }
        //endregion

        //region Geolocation (Maps)
        @JvmStatic
        private fun savedPosition(p: Player): Pair<Int, Int> = p.savePos

        @JvmStatic
        private fun setSavedPosition(p: Player, args: ArgumentIterator) {
            val mapId = args.nextInt()
            val cellId = args.nextInt()
            val sendIm = args.nextOptionalBoolean(true)

            p.setSavePos(mapId, cellId)
            if (sendIm) {
                SocketManager.GAME_SEND_Im_PACKET(p, "06")
            }
        }

        @JvmStatic
        private fun mapID(p: Player): Int = p.curMap.id

        @JvmStatic
        private fun map(p: Player): SMap = p.curMap.scripted()

        @JvmStatic
        private fun cell(p: Player): Int = p.curCell.cellId

        @JvmStatic
        private fun orientation(p: Player): Int = p.orientation

        @JvmStatic
        private fun teleport(p: Player, args: ArgumentIterator) {
            val mapID = args.nextInt()
            val cellID = args.nextInt()
            p.teleport(mapID, cellID)
        }

        @JvmStatic
        private fun compassTo(p: Player, args: ArgumentIterator) {
            val mapId = args.nextInt()
            val map = World.world.getMap(mapId)

            if (p.fight != null) return
            SocketManager.GAME_SEND_FLAG_PACKET(p, map)
        }
        //endregion

        //region Currency
        @JvmStatic
        private fun kamas(p: Player): Long = p.kamas

        @JvmStatic
        private fun modKamas(p: Player, args: ArgumentIterator): Boolean {
            val quantity = args.nextInt()
            return p.modKamasDisplay(quantity.toLong())
        }
        //endregion

        //region Inventory/Gear
        @JvmStatic
        fun gearAt(p: Player, args: ArgumentIterator): SItem? {
            val pos = args.nextInt()
            return p.getEquippedObjects().stream()
                .filter { it.position == pos }
                .findFirst().map { it.scripted() }
                .orElse(null)
        }

        @JvmStatic
        fun pods(p: Player): Pair<Int, Int> = Pair(p.getPodUsed(), p.getMaxPod())

        @JvmStatic
        private fun getItem(p: Player, args: ArgumentIterator): SItem? {
            val itemID = args.nextInt()
            val quantity = args.nextOptionalInt(1)
            val item = p.getItemTemplate(itemID, quantity)
                ?: // No item return null
                return null
            return item.scripted()
        }

        @JvmStatic
        private fun consumeItem(p: Player, args: ArgumentIterator): Boolean {
            val itemID = args.nextInt()
            val quantity = args.nextInt()
            return p.removeItemByTemplateId(itemID, quantity, true)
        }


        @JvmStatic
        private fun addItem(p: Player, args: ArgumentIterator): Boolean {
            val itemID = args.nextInt()
            val quantity = args.nextOptionalInt(1)
            val pos = args.nextOptionalInt(Constant.ITEM_POS_NO_EQUIPED)
            val isPerfect = args.nextOptionalBoolean(false)
            val display = args.nextOptionalBoolean(true)

            val posAlreadyFilled = p.getEquippedObjects().stream()
                .anyMatch { it.position == pos }
            if (posAlreadyFilled) return false

            val tmpl = World.world.getObjTemplate(itemID)
            val item = tmpl!!.createNewItem(quantity, isPerfect)
            item!!.position = pos

            p.addItem(item, pos == Constant.ITEM_POS_NO_EQUIPED, display)
            return true
        }

        @JvmStatic
        private fun tryBuyItem(p: Player, args: ArgumentIterator): Boolean {
            val itemID = args.nextInt()
            val unitPrice = args.nextInt()
            val quantity = args.nextOptionalInt(1)
            val isPerfect = args.nextOptionalBoolean(true)


            val totalPrice = unitPrice * quantity
            if (!p.modKamasDisplay(-totalPrice.toLong())) return false

            p.addItem(itemID, quantity, isPerfect, true)
            return true
        }

        @JvmStatic
        private fun showReceivedItem(p: Player, args: ArgumentIterator) {
            val actorID = args.nextInt()
            val quantity = args.nextInt()

            p.showReceivedItem(actorID, quantity)
        }
        //endregion

        //region Jobs
        @JvmStatic
        private fun jobs(p: Player, args: ArgumentIterator): Table {
            return ScriptVM.listOf(p.metiers.values.stream().map { it.template }.map { it.id })
        }

        @JvmStatic
        private fun tryLearnJob(p: Player, args: ArgumentIterator): Boolean {
            val jobID = args.nextInt()
            return p.tryLearnJob(jobID)
        }

        @JvmStatic
        private fun tryUnlearnJob(p: Player, args: ArgumentIterator): Boolean {
            val jobID = args.nextInt()
            return p.unlearnJob(jobID)
        }

        @JvmStatic
        private fun canLearnJob(p: Player, args: ArgumentIterator): Boolean {
            val jobID = args.nextInt()
            val send = args.nextOptionalBoolean(false)
            return p.canLearnJob(jobID, send)
        }

        @JvmStatic
        private fun jobLevel(p: Player, args: ArgumentIterator): Int {
            val jobID = args.nextInt()

            return Optional.ofNullable(p.getMetierByID(jobID))
                .map { it.get_lvl() }
                .orElse(0)
        }

        @JvmStatic
        private fun addJobXP(p: Player, args: ArgumentIterator): Boolean {
            val jobID = args.nextInt()
            val xpDelta = args.nextInt()
            val send = args.nextOptionalBoolean(true)

            if (xpDelta < 0) return false // Not supported

            val js = p.getMetierByID(jobID) ?: return false
            js.addXp(p, xpDelta.toLong())

            SocketManager.GAME_SEND_JX_PACKET(p, Collections.singletonList(js))
            return true
        }
        //endregion

        //region Spells
        @JvmStatic
        private fun spellLevel(p: Player, args: ArgumentIterator): Int {
            val spellID = args.nextInt()
            return p.getSpells().stream()
                .filter { it.spellID == spellID }.findFirst()
                .map { it.level }.orElse(0)
        }

        @JvmStatic
        private fun setSpellLevel(p: Player, args: ArgumentIterator): Boolean {
            val spellID = args.nextInt()
            val level = args.nextInt()
            val modPoints = args.nextOptionalBoolean(false)

            return p.ensureSpellLevel(spellID, level, modPoints, false)
        }

        @JvmStatic
        private fun spellResetPanel(p: Player) {
            p.spellResetPanel()
        }

        //endregion

        //region Factions
        @JvmStatic
        private fun setFaction(p: Player, args: ArgumentIterator): Boolean {
            val faction = args.nextInt()
            val replace = args.nextOptionalBoolean(false)

            val current = p.alignment
            if (current == faction) return true

            // We're not allowed to replace an existing faction
            if (!replace && current != Constant.ALIGNEMENT_NEUTRE) return false

            p.modifAlignement(faction)
            return true
        }
        //endregion

        //region Stats
        @JvmStatic
        private fun baseStat(p: Player, args: ArgumentIterator): Int {
            val statID = args.nextInt()
            return p.stats.getEffect(statID)
        }

        @JvmStatic
        private fun modScrollStat(p: Player, args: ArgumentIterator): Int {
            val statID = args.nextInt()
            val `val` = args.nextInt()

            return p.statsParcho.addOneStat(statID, `val`)
        }

        @JvmStatic
        private fun resetStats(p: Player, args: ArgumentIterator) {
            val includeScrolls = args.nextOptionalBoolean(false)

            p.resetStats(includeScrolls)
        }
        //endregion

        //region Other
        @JvmStatic
        private fun forceFight(player: Player, args: ArgumentIterator) {
            val def = MobGroupDef.Mapper.get().from(args.nextTable())
            val map = player.curMap
            val group = MonsterGroup(0, map, def)
            map.startFightVersusMonstres(player, group)
        }
        //endregion
    }
}
