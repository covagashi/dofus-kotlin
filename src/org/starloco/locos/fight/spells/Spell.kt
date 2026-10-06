package org.starloco.locos.fight.spells

import org.starloco.locos.area.map.GameCase
import org.starloco.locos.common.Formulas
import org.starloco.locos.common.PathFinding
import org.starloco.locos.fight.Challenge
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant
import org.starloco.locos.kernel.Main
import java.util.ArrayList
import java.util.HashMap
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(Spell::class.java)

class Spell(
    val id: Int,
    val name: String,
    private var spriteId: Int,
    private var spriteInfo: String,
    effectTargets: String,
    var type: Int,
    var duration: Short,
    invalidState: String?,
    neededState: String?
) {

    val effectTargets = ArrayList<Int>()
    val effectTargetsCC = ArrayList<Int>()
    val spellsStats: MutableMap<Int, SortStats?> = HashMap()
    private var invalidStates: ArrayList<Byte>? = null
    private var neededStates: ArrayList<Byte>? = null

    init {
        this.parseEffectTargets(effectTargets)
        this.parseStates(invalidState, neededState)
    }

    fun setInfo(spriteId: Int, spriteInfo: String, effectTargets: String, type: Int, duration: Short) {
        this.spriteId = spriteId
        this.spriteInfo = spriteInfo
        this.type = type
        this.duration = duration
        this.parseEffectTargets(effectTargets)
    }

    fun getStatsByLevel(lvl: Int): SortStats? = spellsStats[lvl]

    fun addSpellStats(lvl: Int, stats: SortStats?) {
        if (this.spellsStats[lvl] != null)
            this.spellsStats.remove(lvl)
        this.spellsStats[lvl] = stats
    }

    fun hasInvalidState(fighter: Fighter): Boolean {
        if (this.invalidStates != null) {
            for (state in this.invalidStates!!) {
                if (fighter.haveState(state.toInt()))
                    return true
            }
        }
        return false
    }

    fun hasNeededState(fighter: Fighter): Boolean {
        var ok = true
        if (this.neededStates != null) {
            for (state in this.neededStates!!) {
                if (!fighter.haveState(state.toInt()))
                    ok = false
            }
        }
        return ok
    }

    private fun parseEffectTargets(effectTargets: String) {
        this.effectTargets.clear()
        this.effectTargetsCC.clear()

        if (effectTargets.equals("0", ignoreCase = true)) {
            this.effectTargets.add(0)
            this.effectTargetsCC.add(0)
        } else {
            val nET = effectTargets.split(":")[0]
            var ccET = ""

            if (effectTargets.split(":").size > 1)
                ccET = effectTargets.split(":")[1]
            for (num in nET.split(";")) {
                try {
                    this.effectTargets.add(Integer.parseInt(num))
                } catch (e: Exception) {
                    this.effectTargets.add(0)
                }
            }
            for (num in ccET.split(";")) {
                try {
                    effectTargetsCC.add(Integer.parseInt(num))
                } catch (e: Exception) {
                    effectTargetsCC.add(0)
                }
            }
        }
    }

    private fun parseStates(invalidState: String?, neededState: String?) {
        if (invalidState != null && invalidState.isNotEmpty()) {
            this.invalidStates = ArrayList()
            for (state in invalidState.split(",")) {
                this.invalidStates!!.add(java.lang.Byte.parseByte(state))
            }
        }
        if (neededState != null && neededState.isNotEmpty()) {
            this.neededStates = ArrayList()
            for (state in neededState.split(",")) {
                this.neededStates!!.add(java.lang.Byte.parseByte(state))
            }
        }
    }

    class SortStats(
        val spellID: Int,
        val level: Int,
        val pACost: Int,
        val minPO: Int,
        val maxPO: Int,
        val tauxCC: Int,
        val tauxEC: Int,
        val isLineLaunch: Boolean,
        val hasLDV: Boolean,
        val isEmptyCell: Boolean,
        val isModifPO: Boolean,
        val maxLaunchbyTurn: Int,
        val maxLaunchbyByTarget: Int,
        val coolDown: Int,
        val reqLevel: Int,
        val isEcEndTurn: Boolean,
        effects: String,
        ceffects: String,
        val porteeType: String
    ) {
        //effets, effetsCC, PaCost, PO Min, PO Max, Taux CC, Taux EC, line, LDV, emptyCell, PO Modif, maxByTurn, maxByTarget, Cooldown, type, level, endTurn
        val effects: ArrayList<SpellEffect> = parseEffect(effects)
        val cCeffects: ArrayList<SpellEffect> = parseEffect(ceffects)

        private fun parseEffect(e: String): ArrayList<SpellEffect> {
            val effets = ArrayList<SpellEffect>()
            val splt = e.split("|")
            for (a in splt) {
                try {
                    if (e == "-1")
                        continue
                    val id = Integer.parseInt(a.split(";", limit = 2)[0])
                    val args = a.split(";", limit = 2)[1]
                    effets.add(SpellEffect(id, args, spellID, level))
                } catch (f: Exception) {
                    log.error("unexpected error", f)
                Main.stop("parseEffect spell")
                }
            }
            return effets
        }

        fun getSpell(): Spell? {
            return World.world.getSort(spellID)
        }

        fun getSpriteID(): Int {
            return getSpell()!!.spriteId
        }

        fun getSpriteInfos(): String? {
            return getSpell()!!.spriteInfo
        }

        fun getMaxLaunchByTarget(): Int = maxLaunchbyByTarget

        fun hasLDV(): Boolean = hasLDV

        fun applySpellEffectToFight(fight: Fight, fighter: Fighter, cell: GameCase, cells: ArrayList<GameCase?>, isCC: Boolean) {
            // Seulement appellé par les pieges, or les sorts de piege
            val effects = if (isCC) this.cCeffects else this.effects
            var chance = Formulas.getRandomValue(0, 99)
            var curMin = 0

            for (effect in effects) {
                if (effect.chance != 0 && effect.chance != 100) {// Si pas 100%
                    if (chance <= curMin || chance >= effect.chance + curMin) {
                        curMin += effect.chance
                        continue
                    }
                    curMin += effect.chance
                }

                val targets = getTargets(cells)
                if (fight.type != Constant.FIGHT_TYPE_CHALLENGE && fight.allChallenges.size > 0) {
                    for (c in fight.allChallenges.entries) {
                        if (c.value == null)
                            continue
                        c.value.onFightersAttacked(targets, fighter, effect, this.spellID, true)
                    }
                }
                effect.applyToFight(fight, fighter, cell, targets)
            }
        }

        fun getTargets(cells: List<GameCase?>): ArrayList<Fighter> {
            val targets = ArrayList<Fighter>()
            for (cell in cells) {
                if (cell != null) {
                    val target = cell.firstFighter
                    if (target != null) targets.add(target)
                }
            }
            return targets
        }

        fun applySpellEffectToFight(fight: Fight, perso: Fighter,
                                    cell: GameCase, isCC: Boolean, isTrap: Boolean) {
            val effects = if (isCC) cCeffects else this.effects

            var jetChance = 0
            if (this.getSpell()!!.id == 101) // Si c'est roulette
            {
                jetChance = Formulas.getRandomValue(0, 75)
                if (jetChance % 2 == 0)
                    jetChance++
            } else if (this.getSpell()!!.id == 574) // Si c'est Ouverture hasardeuse fantôme
                jetChance = Formulas.getRandomValue(0, 96)
            else if (this.getSpell()!!.id == 574) // Si c'est Ouverture hasardeuse
                jetChance = Formulas.getRandomValue(0, 95)
            else
                jetChance = Formulas.getRandomValue(0, 99)
            var curMin = 0
            var num = 0
            for (SE in effects) {
                try {
                    if (fight.state >= Constant.FIGHT_STATE_FINISHED)
                        return
                    if (SE.chance != 0 && SE.chance != 100)// Si pas 100%
                    {
                        if (jetChance <= curMin
                                || jetChance >= SE.chance + curMin) {
                            curMin += SE.chance
                            num++
                            continue
                        }
                        curMin += SE.chance
                    }
                    var POnum = num * 2
                    if (isCC) {
                        POnum += this.effects.size * 2// On zaap la partie du String des effets hors CC
                    }
                    val cells = PathFinding.getCellListFromAreaString(fight.map, cell.cellId, perso.cell!!.cellId, porteeType, POnum, isCC)
                    val finalCells = ArrayList<GameCase>()
                    var TE = 0
                    val S = World.world.getSort(spellID)
                    // on prend le targetFlag corespondant au num de l'effet

                    if (S != null && S.effectTargetsCC.size > num && isCC)
                        TE = S.effectTargetsCC[num]
                    else if (S != null && S.effectTargets.size > num && !isCC)
                        TE = S.effectTargets[num]

                    for (C in cells) {
                        if (C == null)
                            continue
                        val F = C.firstFighter ?: continue
                        // Ne touches pas les alliés : 1
                        if (TE and 1 == 1 && F.team == perso.team)
                            continue
                        // Ne touche pas le lanceur : 2
                        if (TE shr 1 and 1 == 1 && F.id == perso.id)
                            continue
                        // Ne touche pas les ennemies : 4
                        if (TE shr 2 and 1 == 1 && F.team != perso.team)
                            continue
                        // Ne touche pas les combatants (seulement invocations) : 8
                        if (TE shr 3 and 1 == 1 && !F.isInvocation())
                            continue
                        // Ne touche pas les invocations : 16
                        if (TE shr 4 and 1 == 1 && F.isInvocation())
                            continue
                        // N'affecte que le lanceur : 32
                        if (TE shr 5 and 1 == 1 && F.id != perso.id)
                            continue
                        // N'affecte que les alliés (pas le lanceur) : 64
                        if (TE shr 6 and 1 == 1 && (F.team != perso.team || F.id == perso.id))
                            continue
                        // N'affecte PERSONNE : 1024
                        if (TE shr 10 and 1 == 1)
                            continue
                        // Si pas encore eu de continue, on ajoute la case, tout le monde : 0
                        finalCells.add(C)
                    }
                    // Si le sort n'affecte que le lanceur et que le lanceur n'est
                    // pas dans la zone

                    if (TE shr 5 and 1 == 1)
                        if (!finalCells.contains(perso.cell))
                            finalCells.add(perso.cell!!)
                    val cibles = SpellEffect.getTargets(finalCells)

                    if (fight.type != Constant.FIGHT_TYPE_CHALLENGE
                            && fight.allChallenges.size > 0) {
                        for (c in fight.allChallenges.entries) {
                            if (c.value == null)
                                continue
                            c.value.onFightersAttacked(cibles, perso, SE, this.spellID, isTrap)
                        }
                    }
                    SE.applyToFight(fight, perso, cell, cibles)
                    num++
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                }
            }
        }
    }
}
