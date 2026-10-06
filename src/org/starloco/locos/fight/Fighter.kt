package org.starloco.locos.fight

import org.classdump.luna.impl.ImmutableTable
import org.starloco.locos.area.map.Actor
import org.starloco.locos.area.map.GameCase
import org.starloco.locos.client.Player
import org.starloco.locos.client.other.Stats
import org.starloco.locos.common.Formulas
import org.starloco.locos.common.PathFinding
import org.starloco.locos.common.SocketManager
import org.starloco.locos.entity.Collector
import org.starloco.locos.entity.Prism
import org.starloco.locos.entity.monster.MonsterGrade
import org.starloco.locos.entity.mount.Mount
import org.starloco.locos.fight.spells.LaunchedSpell
import org.starloco.locos.fight.spells.Spell
import org.starloco.locos.fight.spells.SpellEffect
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Config
import org.starloco.locos.kernel.Constant
import org.starloco.locos.script.Scripted
import org.starloco.locos.util.TimerWaiter
import java.util.ArrayList
import java.util.HashMap
import java.util.Objects
import java.util.Optional
import java.util.StringJoiner
import java.util.stream.Collectors
import java.util.stream.Stream
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(Fighter::class.java)

abstract class Fighter protected constructor(val id: Int, val fight: Fight) : Comparable<Fighter>, Scripted<Any>, Actor, Cloneable {

    private var erodedLP = 0

    private var nbrInvoc = 0
    private var trapped = false
    private var glyphed = false
    private var isStatique = false
    private var canPlay = false
    var team = -2
    var cell: GameCase? = null
    private var pdv = 0
    var isDead = false
    private var hasLeft = false
    private var isHolding: Fighter? = null
    private var holdedBy: Fighter? = null
    private var oldCible: Fighter? = null
    private var invocator: Fighter? = null
    var levelUp = false
    private var isDeconnected = false
    var turnRemaining = 0
        private set
    var nbrDisconnection = 0
        private set
    private var isTraqued = false
    private var stats: Stats? = null
    private val state = HashMap<Int, Int>()
    protected val fightBuffs = ArrayList<SpellEffect>()
    val chatiValue: MutableMap<Int, Int> = HashMap()
    private val launchedSpell = ArrayList<LaunchedSpell>()
    private var killedBy: World.Couple<Byte, Long>? = null

    internal open fun init() {
        this.pdv = this.getPdvMax()
    }

    abstract fun getType(): Int

    abstract fun getLvl(): Int

    abstract fun baseMaxPdv(): Int

    protected abstract fun getBaseStats(): Stats?

    abstract fun getDefaultGfx(): Int

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Fighter {
        return super.clone() as Fighter
    }

    protected open fun getMountColors(): String? {
        return null
    }

    fun canPlay(): Boolean {
        return this.canPlay
    }

    fun setCanPlay(canPlay: Boolean) {
        this.canPlay = canPlay
    }

    open fun initFightBuffs() {}

    open fun send(pck: String) {}

    abstract fun spellRankForID(id: Int): Optional<Spell.SortStats>

    open fun xpString(separator: String): String {
        return "0" + separator + "0" + separator + "0"
    }

    abstract fun getPacketsName(): String

    abstract fun getGMPacketParts(): Stream<String>

    open fun getMount(): Optional<Mount> {
        return Optional.empty()
    }

    open fun getColors(): IntArray {
        return intArrayOf(-1, -1, -1)
    }

    fun getTeam2(): Int {
        return this.fight.getTeamId(id)
    }

    fun getOtherTeam(): Int {
        return this.fight.getOtherTeamId(id)
    }

    fun getPdvMax(): Int {
        return this.baseMaxPdv() + getBuffValue(Constant.STATS_ADD_VITA) - erodedLP
    }

    fun removePdvMax(pdv: Int) {
        erodedLP += pdv
        this.pdv = Math.min(getPdvMax(), this.pdv)
    }

    fun getPdv(): Int {
        return (this.pdv + getBuffValue(Constant.STATS_ADD_VITA))
    }

    fun setPdv(pdv: Int) {
        this.pdv = Math.min(this.getPdvMax(), pdv)
    }

    fun removePdv(caster: Fighter, pdv: Int) {
        if (pdv > 0)
            this.fight.allChallenges.values.stream().filter(Objects::nonNull).forEach { challenge -> challenge.onFighterAttacked(caster, this) }
        this.pdv -= pdv
    }

    fun fullPdv() {
        this.pdv = this.getPdvMax()
    }

    fun isFullPdv(): Boolean {
        return this.pdv == this.getPdvMax()
    }

    fun setIsDead(isDead: Boolean) {
        this.isDead = isDead
    }

    fun hasLeft(): Boolean {
        return this.hasLeft
    }

    fun setLeft(hasLeft: Boolean) {
        this.hasLeft = hasLeft
    }

    open fun getIsHolding(): Fighter? {
        return this.isHolding
    }

    open fun setIsHolding(isHolding: Fighter?) {
        this.isHolding = isHolding
    }

    open fun getHoldedBy(): Fighter? {
        return this.holdedBy
    }

    open fun setHoldedBy(holdedBy: Fighter?) {
        this.holdedBy = holdedBy
    }

    open fun getOldCible(): Fighter? {
        return this.oldCible
    }

    open fun setOldCible(cible: Fighter?) {
        this.oldCible = cible
    }

    open fun getInvocator(): Fighter? {
        return this.invocator
    }

    open fun setInvocator(invocator: Fighter?) {
        this.invocator = invocator
    }

    open fun isInvocation(): Boolean {
        return (this.invocator != null)
    }

    fun Disconnect() {
        if (this.isDeconnected)
            return
        this.isDeconnected = true
        this.turnRemaining = 20
        this.nbrDisconnection++
    }

    fun Reconnect() {
        this.isDeconnected = false
        this.turnRemaining = 0
    }

    fun isDeconnected(): Boolean {
        return !this.hasLeft && this.isDeconnected
    }

    fun setTurnRemaining() {
        this.turnRemaining--
    }

    fun getTraqued(): Boolean {
        return this.isTraqued
    }

    fun setTraqued(isTraqued: Boolean) {
        this.isTraqued = isTraqued
    }

    fun setState(id: Int, t: Int) {
        this.state.remove(id)
        if (t != 0) this.state[id] = t
    }

    fun getState(id: Int): Int {
        return if (this.state[id] != null) this.state[id]!! else -1
    }

    fun haveState(id: Int): Boolean {
        val turn = this.state[id]
        return turn != null && turn != 0
    }

    fun sendState(p: Player?) {
        if (p != null && p.getAccount() != null && p.getGameClient() != null)
            for (state in this.state.entries)
                SocketManager.GAME_SEND_GA_PACKET(p.getGameClient()!!, 7.toString() + "", 950.toString() + "", id.toString() + "", "$id,${state.key},1")
    }

    fun nbInvocation(): Int {
        var i = 0
        for (entry in this.fight.getTeam(this.getTeam2()).entries) {
            val f = entry.value
            if (f.isInvocation() && !f.isStatique)
                if (f.invocator === this)
                    i++
        }
        return i
    }

    fun isTrappedOrGlyphed(): Boolean {
        return isTrapped() || isGlyphed()
    }

    fun isTrapped(): Boolean {
        return trapped
    }

    fun setTrapped(trapped: Boolean) {
        this.trapped = trapped
    }

    fun isGlyphed(): Boolean {
        return trapped
    }

    fun setGlyphed(glyphed: Boolean) {
        this.glyphed = glyphed
    }

    fun getFightBuff(): ArrayList<SpellEffect> {
        return this.fightBuffs
    }

    private fun getFightBuffStats(): Stats {
        val stats = Stats()
        for (entry in this.fightBuffs)
            stats.addOneStat(entry.effectID, entry.value)
        return stats
    }

    fun getBuffValue(id: Int): Int {
        var value = 0
        for (entry in this.fightBuffs)
            if (entry.effectID == id)
                value += entry.value
        return value
    }

    fun getBuff(id: Int): SpellEffect? {
        for (entry in this.fightBuffs)
            if (entry.effectID == id && entry.turns > 0)
                return entry
        return null
    }

    fun getBuffsByEffectID(effectId: Int): ArrayList<SpellEffect> {
        return this.fightBuffs.stream().filter { buff -> buff.effectID == effectId }.collect(Collectors.toCollection(::ArrayList))
    }

    fun getTotalStatsLessBuff(): Stats? {
        return getBaseStats()
    }

    fun hasBuff(id: Int): Boolean {
        for (entry in this.fightBuffs)
            if (entry.effectID == id && entry.turns > 0)
                return true
        return false
    }

    fun addBuff(id: Int, value: Int, duration: Int, debuff: Boolean, spellId: Int, args: String, caster: Fighter, sendGA: Boolean, sendGIE: Boolean): SpellEffect {
        var debuff = debuff
        var duration = duration
        val effect = SpellEffect(id, value, duration, debuff, caster, args, spellId)

        val mob = this.`as`(MobFighter::class.java)
        if (mob.isPresent) {
            for (id1 in Constant.STATIC_INVOCATIONS)
                if (id1 != 2750 && id1 == mob.get().mobGrade.template.id)
                    return effect
        }

        when (spellId) {
            1099 -> {
                if (mob.isPresent && mob.get().mobGrade.template.id == 423)
                    return effect
            }
            99, 5, 20, 127, 89, 126, 115, 192, 4, 1, 6,
            14, 18, 7, 284, 197, 704, 168, 45, 159, 171, 167, 511, 513,
            686, 701, // Sort pandawa (etat)
            431, 433, 437, 443, 441 -> // Chatiment
                debuff = true
        }

        if (id == 606 || id == 607 || id == 608 || id == 609 || id == 611 || id == 125 || id == 114) debuff = true
        else if (id == 293) debuff = false

        when (spellId) {
            // Feca spells shields
            1, 4, 5, 6, 7, 14, 18, 20 -> {
                if (this.id != caster.id)
                    duration++
            }
        }
        //Si c'est le jouer actif qui s'autoBuff, on ajoute 1 a la durée
        if (this.id == caster.id && !this.isTrapped() && id != 84 && id != 950 && spellId != 446) {
            duration += 1
            when (spellId) {
                138, 170, 114 -> duration--
            }
        }
        // Cas infini
        if (mob.isPresent && duration == 0)
            duration = -1

        effect.turns = duration
        this.fightBuffs.add(effect)

        if (Config.debug)
            log.debug("- Ajout du Buff " + id + " sur le personnage fighter (" + this.id + ") val : " + value + " duration : " + duration + " debuff : " + debuff + " spellid : " + spellId + " args : " + args + " !")
        when (spellId) {
            // Feca spells shields
            1, 4, 5, 6, 7, 14, 18, 20 -> {
                if (this.id != caster.id)
                    duration--
            }
        }
        if (id != 950 && spellId != 446) {
            when (spellId) {
                170, 114, 101 -> {
                    if (duration != 0)
                        duration--
                }
                else -> {
                    if (this.id == caster.id || this.isTrapped())
                        duration--
                }
            }
        }

        if (sendGA) {
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effect.effectID, caster.id.toString(), this.id.toString() + "," + effect.value + "," + effect.turns)
        }
        if (sendGIE) {
            fight.sendBuffPacket(this, effect, fight.getFighters(7), null, duration)
        }
        return effect
    }

    fun debuff(effect: SpellEffect) {
        val it = this.fightBuffs.iterator()
        while (it.hasNext()) {
            val spellEffect = it.next()

            if (spellEffect.effectID == 293)
                continue
            when (spellEffect.spell) {
                437, 431, 433, 443, 441, 1104, 1105 -> continue
                197, 52, 228 -> {
                    it.remove()
                    continue
                }
            }

            if (spellEffect.debuffable) {
                it.remove()

                if (effect.caster === this) {
                    when (spellEffect.effectID) {
                        Constant.STATS_ADD_PA, Constant.STATS_ADD_PA2 -> {
                            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this.fight, 7, 101, id.toString() + "", "$id,-${spellEffect.value}")
                            this.setCurPa(this.fight, this.getCurPa(fight) - spellEffect.value)
                        }
                        Constant.STATS_ADD_PM, Constant.STATS_ADD_PM2 -> {
                            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this.fight, 7, 127, id.toString() + "", "$id,-${spellEffect.value}")
                            this.setCurPm(this.fight, this.getCurPm(fight) - spellEffect.value)
                        }
                        //case Constant.STATS_REM_PA:
                        Constant.STATS_REM_PA2 -> {
                            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this.fight, 7, 111, id.toString() + "", "$id,${spellEffect.value}")
                            this.setCurPa(this.fight, this.getCurPa(fight) + spellEffect.value)
                        }
                        //case Constant.STATS_REM_PM:
                        Constant.STATS_REM_PM2 -> {
                            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this.fight, 7, 128, id.toString() + "", "$id,${spellEffect.value}")
                            this.setCurPm(this.fight, this.getCurPm(fight) + spellEffect.value)
                        }
                    }
                }
            }
        }
        val array = ArrayList(this.fightBuffs)
        if (!array.isEmpty()) {
            this.fightBuffs.clear()
            TimerWaiter.addNext({
                array.stream().filter(Objects::nonNull).forEach { spellEffect ->
                    this.addBuff(spellEffect.effectID, spellEffect.value, spellEffect.turns, spellEffect.debuffable, spellEffect.spell, spellEffect.args, spellEffect.caster!!, false, false)
                    val duration = if (effect.caster !== this) spellEffect.turns else spellEffect.turns - 1
                    fight.sendBuffPacket(this, spellEffect, fight.getFighters(7), null, duration)
                }
            }, 750)
        }

        if (!this.hasLeft) {
            this.`as`(PlayerFighter::class.java).ifPresent { obj -> obj.sendStats() }
        }
    }

    private fun refreshEndTurnShield() {
        for (fighter in this.fight.getFighters(7)) {
            val iterator = fighter.getFightBuff().iterator()

            while (iterator.hasNext()) {
                val effect = iterator.next()

                if (effect != null) {
                    when (effect.spell) {
                        // Feca spells shields
                        1, 4, 5, 6, 7, 14, 18, 20 -> {
                            if (this.id == effect.caster!!.id) {
                                if (effect.decrementDuration() == 0) {
                                    iterator.remove()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun refreshEndTurnBuff() {
        this.refreshEndTurnShield()
        val iterator = this.fightBuffs.iterator()

        while (iterator.hasNext()) {

            val effect = iterator.next()
            if (effect == null)
                continue
            when (effect.spell) {
                // Feca spells shields
                1, 4, 5, 6, 7, 14, 18, 20 -> continue
            }

            if (effect.decrementDuration() == 0 || effect.caster!!.isDead) {
                iterator.remove()
                when (effect.effectID) {
                    787 -> {
                        val id = (effect.args.split(";")[0]).toInt()
                        val level = (effect.args.split(";")[1]).toInt()
                        val spell = World.world.getSort(id)

                        for (e in spell!!.getStatsByLevel(level)!!.effects) {
                            if (e.effectID == 89)
                                e.applyToFight(fight, this, this.cell!!, PathFinding.getFightersAround(this.cell!!.cellId, this.fight.map!!))
                        }
                        spell!!.getStatsByLevel(level)!!.applySpellEffectToFight(fight, fight.getFighterByGameOrder()!!, cell!!, false, false)
                    }
                    108 -> {
                        if (effect.spell == 441) {
                            //Baisse des pdvs max
                            erodedLP += effect.value
                            //Baisse des pdvs actuel
                            if (this.pdv - effect.value <= 0) {
                                this.fight.onFighterDie(this, this.holdedBy!!)
                                this.fight.verifIfTeamAllDead()
                            } else this.pdv = (this.pdv - effect.value)
                        }
                    }
                    150 -> {
                        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this.fight, 7, 150, effect.caster!!.id.toString() + "", "$id,0")
                        SocketManager.GAME_SEND_GIC_PACKET_TO_FIGHT(this.fight, 7, this)
                    }
                    950 -> {
                        val args = effect.args
                        val id = (args.split(";")[2]).toInt()

                        if (id != -1) {
                            setState(id, 0)
                            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this.fight, 7, 950, effect.caster!!.id.toString() + "", effect.caster!!.id.toString() + "," + id + ",0")
                        }
                    }
                }
            }
        }
    }

    fun applyBeginningTurnBuff(fight: Fight) {
        val effects = ArrayList(this.fightBuffs)
        for (effectID in Constant.BEGIN_TURN_BUFF) {
            effects.stream().filter { entry -> entry.effectID == effectID }
                    .forEach { entry -> entry.applyBeginingBuff(fight, this) }
        }
    }

    fun getLaunchedSorts(): ArrayList<LaunchedSpell> {
        return this.launchedSpell
    }

    fun refreshLaunchedSort() {
        this.launchedSpell.removeIf { launched -> launched.decrementCooldown() <= 0 }
    }

    fun addLaunchedSort(target: Fighter, sort: Spell.SortStats, fighter: Fighter) {
        val launched = LaunchedSpell(target, sort, fighter)
        this.launchedSpell.add(launched)
    }

    fun getTotalStats(): Stats {
        return Stats.cumulStatFight(getBaseStats()!!, getFightBuffStats())
    }

    fun getMaitriseDmg(id: Int): Int {
        var value = 0
        for (entry in this.fightBuffs)
            if (entry.spell == id)
                value += entry.value
        return value
    }

    fun getSpellValueBool(id: Int): Boolean {
        for (entry in this.fightBuffs)
            if (entry.spell == id)
                return true
        return false
    }

    fun critStrikeCheck(tauxCC: Int): Boolean {
        var tauxCC = tauxCC
        if (tauxCC < 2)
            return false
        var agi = getTotalStats().getEffect(Constant.STATS_ADD_AGIL)
        if (agi < 0)
            agi = 0
        tauxCC -= getTotalStats().getEffect(Constant.STATS_ADD_CC)
        tauxCC = ((tauxCC * 2.9901) / Math.log((agi + 12).toDouble())).toInt()//Influence de l'agi
        if (tauxCC < 2)
            tauxCC = 2
        val jet = Formulas.getRandomValue(1, tauxCC)
        return (jet == tauxCC)
    }

    protected open fun criticalStrikeModifier(baseCC: Int, spellID: Int): Int {
        // By default, there is no modifier
        return baseCC
    }

    fun critStrikeCheck(porcCC: Int, sSort: Spell.SortStats, fighter: Fighter): Boolean {
        var porcCC = porcCC
        if (porcCC == 0) return false
        porcCC = criticalStrikeModifier(porcCC, sSort.spellID)
        val jet = Formulas.getRandomValue(1, porcCC)
        return (jet == porcCC)
    }

    val initiative: Int
        get() = getTotalStats().getEffect(Constant.STATS_ADD_INIT)

    fun getPa(): Int {
        return getTotalStats().getEffect(Constant.STATS_ADD_PA)
    }

    fun getPm(): Int {
        return getTotalStats().getEffect(Constant.STATS_ADD_PM)
    }

    fun getPros(): Int {
        return getTotalStats().getEffect(Constant.STATS_ADD_PROS)
    }

    fun getCurPa(fight: Fight): Int {
        return fight.curFighterPa
    }

    fun setCurPa(fight: Fight, pa: Int) {
        fight.curFighterPa = fight.curFighterPa + pa
    }

    fun getCurPm(fight: Fight): Int {
        return fight.curFighterPm
    }

    fun setCurPm(fight: Fight, pm: Int) {
        fight.curFighterPm = fight.curFighterPm + pm
    }

    fun canLaunchSpell(spellID: Int): Boolean {
        return spellRankForID(spellID).isPresent && LaunchedSpell.cooldownGood(this, spellID)
    }

    fun unHide(spellid: Int) {
       //on retire le buff invi
        if (spellid != -1)// -1 : CAC
        {
            when (spellid) {
                66, 71, 181, 196, 200, 219 -> return
            }
        }
        val buffs = ArrayList(getFightBuff())
        for (SE in buffs) {
            if (SE.effectID == 150)
                getFightBuff().remove(SE)
        }
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this.fight, 7, 150, id.toString() + "", "$id,0")
        //On actualise la position
        SocketManager.GAME_SEND_GIC_PACKET_TO_FIGHT(this.fight, 7, this)
    }

    fun isHidden(): Boolean {
        return hasBuff(150)
    }

    fun getGmPacket(c: Char, withGm: Boolean): String {
        val str = StringJoiner(";", (if (withGm) "GM|" else "") + c, "")
        str.add(cell!!.cellId.toString())
        str.add("1")
        str.add("0")
        str.add(id.toString())
        str.add(getPacketsName())

        getGMPacketParts().forEach { str.add(it) }

        str.add(team.toString())

        getMount().ifPresent { m -> str.add(m.getStringColor(getMountColors())) }

        return str.toString()
    }

    fun isStatic(): Boolean {
        return isStatique
    }

    override fun compareTo(other: Fighter): Int {
        if (this.isInvocation()) return 0
        return this.getPros() - other.getPros()
    }

    override fun scripted(): Any {
        return ImmutableTable.Builder()
            .add("id", this.id)
            .add("type", this.getType())
            .add("level", this.getLvl())
            .build()
    }

    override fun Id(): Long {
        return id.toLong()
    }

    override fun name(): String {
        return getPacketsName()
    }

    fun setStatic(b: Boolean) {
        isStatique = b
    }

    fun modNbrInvoc(i: Int) {
        nbrInvoc += i
    }

    fun getNbrInvoc(): Int {
        return nbrInvoc
    }

    open fun getKilledBy(): World.Couple<Byte, Long>? {
        return killedBy
    }

    open fun setKilledBy(killer: World.Couple<Byte, Long>?) {
        killedBy = killer
    }

    private fun <T : Fighter> `as`(c: Class<T>): Optional<T> {
        return Optional.of(this).filter { c.isInstance(it) }.map { c.cast(it) }
    }

    open fun aiControlled(): Boolean {
        return true
    }

    open fun canLoot(): Boolean {
        return false
    }

    open fun minKamasReward(): Int {
        return 0
    }

    open fun maxKamasReward(): Int {
        return 0
    }

    open fun drops(): Stream<World.Drop> {
        return Stream.empty()
    }

    open val player: Player? get() = null
    open val mob: MonsterGrade? get() = null
    open val collector: Collector? get() = null
    open val prism: Prism? get() = null

    companion object {
        @JvmStatic
        fun NewPlayer(f: Fight, player: Player): Fighter {
            val fi = PlayerFighter(f, player)
            fi.init()
            return fi
        }

        @JvmStatic
        fun NewCollector(id: Int, f: Fight, collector: Collector): Fighter {
            val fi = CollectorFighter(id, f, collector)
            fi.init()
            return fi
        }

        @JvmStatic
        fun NewMob(id: Int, f: Fight, mg: MonsterGrade): Fighter {
            val fi = MobFighter(id, f, mg)
            fi.init()
            return fi
        }

        @JvmStatic
        fun NewPrism(id: Int, f: Fight, p: Prism): Fighter {
            val fi = PrismFighter(id, f, p)
            fi.init()
            return fi
        }

        @JvmStatic
        fun NewClone(id: Int, f: Fight, p: PlayerFighter): Fighter {
            val fi = CloneFighter(id, f, p)
            fi.init()
            return fi
        }

        @JvmStatic
        fun NewSummon(id: Int, f: Fight, mg: MonsterGrade, caster: Fighter): Fighter {
            val fi = SummonFighter(id, f, mg, caster)
            fi.init()
            return fi
        }
    }
}
