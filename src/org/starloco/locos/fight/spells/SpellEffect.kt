package org.starloco.locos.fight.spells

import org.starloco.locos.area.map.GameCase
import org.starloco.locos.client.Player
import org.starloco.locos.common.CryptManager
import org.starloco.locos.common.Formulas
import org.starloco.locos.common.PathFinding
import org.starloco.locos.common.SocketManager
import org.starloco.locos.entity.monster.Monster
import org.starloco.locos.entity.monster.MonsterGrade
import org.starloco.locos.fight.*
import org.starloco.locos.fight.spells.Spell.SortStats
import org.starloco.locos.fight.traps.Glyph
import org.starloco.locos.fight.traps.Trap
import org.starloco.locos.game.GameServer
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant
import org.starloco.locos.util.TimerWaiter

import java.util.*
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(SpellEffect::class.java)

open class SpellEffect : Cloneable {

	var effectID: Int = 0
	@get:JvmName("getTurn")
	var turns: Int= 0
	var jet: String = "0d0+0"
	var chance: Int= 100
	lateinit var args: String
	var value: Int= 0
	var caster: Fighter? = null
	val spell: Int
	var spellLvl: Int= 1
	var debuffable: Boolean= true
var cell: GameCase? = null

	constructor(aID: Int, aArgs: String, aSpell: Int, aSpellLevel: Int) {
		effectID = aID
		args = aArgs
		spell = aSpell
		spellLvl = aSpellLevel
		try {
			value = (args.split(";")[0]).toInt()
			turns = (args.split(";")[3]).toInt()
			chance = (args.split(";")[4]).toInt()
			jet = args.split(";")[5]
		} catch (ignored: Exception) {
		}
	}

	constructor(id: Int, value2: Int, turns2: Int, debuff: Boolean, aCaster: Fighter, args2: String, aspell: Int) {
		effectID = id
		value = value2
		turns = turns2
		debuffable = debuff
		caster = aCaster
		args = args2
		spell = aspell
		try {
			jet = args.split(";")[5]
		} catch (ignored: Exception) {
		}
	}

	fun isPoison(): Boolean {
		return spell == 66 || spell == 164 || spell == 71 || spell == 196 || spell == 219 || spell == 181 || spell == 200
	}

	companion object {
	@JvmStatic fun getTargets(cells: ArrayList<GameCase>): ArrayList<Fighter> {
		var targets: ArrayList<Fighter> = ArrayList()
		for (aCell in  cells) {
			if (aCell == null) continue
			var f: Fighter? = aCell.firstFighter
			if (f == null) continue
			targets.add(f)
		}
		return targets
	}
	}

	fun applyOnHitBuffs(finalDommage: Int, target: Fighter, caster: Fighter, fight: Fight, elementId: Int): Int {
		for (id in  Constant.ON_HIT_BUFFS) {
		var caster = caster
		var finalDommage = finalDommage
			val effects: List<SpellEffect> = target.getBuffsByEffectID(id)

			if (!effects.isEmpty()) {
				when (id){  114 -> {for (effect in  effects) {
							if (effect.spell == 521) {
								finalDommage = finalDommage * 2
							}
						}
						continue
}
107 -> {var totalDamageReturn: Int = 0

						for (effect in  effects) {
							if (this.spell == 66 || spell == 71 || spell == 196 || spell == 181 || spell == 200 || target.id == caster.id)
								break
							if (caster.hasBuff(765) && caster.getBuff(765) != null && !caster.getBuff(765)!!.caster!!.isDead) {
								// Sacrifice
								effect.applyEffect_765B(fight, caster)
								caster = caster.getBuff(765)!!.caster!!
							}

							var args: List<String> = effect.args.split(";")
							var factor: Float = 1 + (target.getTotalStats().getEffect(Constant.STATS_ADD_SAGE) / 100.0f)
							var damageReturn: Int
							try {
								if ((args[1]).toInt() != -1) {
									damageReturn = ((factor * Formulas.getRandomValue((args[0]).toInt(), (args[1]).toInt())).toInt())
								} else {
									damageReturn = ((factor * (args[0]).toInt()).toInt())
								}
							} catch (e: Exception) {
								return finalDommage
							}

							damageReturn = Math.min(damageReturn, finalDommage)
							finalDommage -= damageReturn

							damageReturn = Formulas.applyResistancesOnDamage(elementId, damageReturn, caster, true)
							damageReturn = Math.min(damageReturn, caster.getPdv())
							finalDommage = Math.max(0, finalDommage)
							damageReturn -= Formulas.getArmorResist(caster, -1)

							if (caster.hasBuff(149) && caster.getBuff(149)!!.spell == 197) {
								damageReturn = 0
							}
							if (caster.hasBuff(105)) { // Immunity
								damageReturn = damageReturn - caster.getBuff(105)!!.value
							}
							if (damageReturn < 0) {
								damageReturn = 0
							}
							if (damageReturn > 0) {
								if (damageReturn > caster.getPdv()) damageReturn = caster.getPdv()
								caster.removePdv(caster, damageReturn)
								target.removePdvMax(damageReturn / 10)
								totalDamageReturn += damageReturn
							}
						}

						if (caster.hasBuff(105)) { // Immunity
							SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, (caster.id).toString(), target.id.toString() + "," + caster.getBuff(105)!!.value)
						}
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 107, "-1", target.id.toString() + "," + totalDamageReturn)
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, (caster.id).toString(), caster.id.toString() + ",-" + totalDamageReturn)
						continue
}
else -> {var stat: Int = 0
							var jet: Int = 0
							for (buff in  effects) {
							when (id){  138 -> {if (buff.spell == 1039) {
										var stats: Int = 0
										if (elementId == Constant.ELEMENT_AIR)
											stats = 217
										else if (elementId == Constant.ELEMENT_EAU)
											stats = 216
										else if (elementId == Constant.ELEMENT_FEU)
											stats = 218
										else if (elementId == Constant.ELEMENT_NEUTRE)
											stats = 219
										else if (elementId == Constant.ELEMENT_TERRE)
											stats = 215
										var b: SpellEffect? = target.getBuff(stats)
										if (b != null) {
											var vale: Int= b.value
											var turns: Int = b.turns
											var duration: Int = b.turns
											var args: String = b.args
											for (i in  Constant.getOppositeStats(stats))
												target.addBuff(i, vale, turns, true, buff.spell, args, caster, false, true)
											target.addBuff(stats, vale, duration, true, buff.spell, args, caster, false, true)
										}
									}
									
}
9 -> {if (this.isPoison())
										continue
									if (caster == target || Formulas.getRandomValue(0, 99) + 1 >= buff.value)
										continue

									var distance: Int = PathFinding.getDistanceBetween(fight.map, target.cell!!.getId(), caster.cell!!.getId())
									var nbCell: Int = (buff.args.split(";")[1]).toInt()

									if (distance > 1) {
										continue
									}
									if (nbCell == 0)
										continue
									var exCase: Int = target.cell!!.getId()
									var newCellId: Int = PathFinding.newCaseAfterPush(fight, caster.cell!!, target.cell!!, nbCell, false)

									if (newCellId < 0) { // blocked
										var a: Int = -newCellId
										a = nbCell - a
										newCellId = PathFinding.newCaseAfterPush(fight, caster.cell!!, target.cell!!, a, false)
										if (newCellId == 0) {
											finalDommage = Formulas.getRandomValue(28, 33)
											SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 5, target.id.toString() + "", target.id.toString() + "," + newCellId)
											break
										}
										if (fight.map!!.getCase(newCellId) == null)
											continue
									}
									if (newCellId == 0) continue

									var cacheCell: GameCase? = target.cell
									cacheCell!!.removeFighter(target)

									target.cell = (fight.map!!.getCase(newCellId))
									target.cell!!.addFighter(target)
									SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 5, target.id.toString() + "", target.id.toString() + "," + newCellId)

									var holding: Fighter? = target.getIsHolding()
									if (holding != null) {
										holding.setState(Constant.ETAT_PORTE, 0)
										target.setState(Constant.ETAT_PORTEUR, 0)
										holding.setHoldedBy(null)
										target.setIsHolding(null)
										holding.cell = (cacheCell)
										cacheCell!!.addFighter(holding)
										SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, target.id.toString() + "", target.id.toString() + "," + Constant.ETAT_PORTEUR + ",0")
										SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, holding.id.toString() + "", holding.id.toString() + "," + Constant.ETAT_PORTE + ",0")
										SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 51, target.id.toString() + "", cacheCell!!.getId().toString() + "")
									}

									fight.checkTraps(target)
									if (exCase != newCellId)
										finalDommage = 0
									
}
79 -> {try {
										var infos: List<String> = buff.args.split(";")
										var coefDom: Int = (infos[0]).toInt()
										var coefHeal: Int = (infos[1]).toInt()
										var chance: Int = (infos[2]).toInt()
										var jet: Int = Formulas.getRandomValue(0, 99)

										if (jet < chance)//Soin
										{
											finalDommage = -(finalDommage * coefHeal)
											if (-finalDommage > (target.getPdvMax() - target.getPdv()))
												finalDommage = -(target.getPdvMax() - target.getPdv())
										} else//Dommage
											finalDommage = finalDommage * coefDom
									} catch (e: Exception) {
										log.error("unexpected error", e)
									}
									
}
606 -> {stat = buff.value
									jet = Formulas.getRandomJet(caster, target, buff.jet)
									target.addBuff(stat, jet, -1, false, buff.spell, buff.args, caster, false, true)
									SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, stat, caster.id.toString() + "", target.id.toString() + "," + jet + "," + -1)
									
}
607 -> {stat = buff.value
									jet = Formulas.getRandomJet(caster, target, buff.jet)
									target.addBuff(stat, jet, -1, false, buff.spell, buff.args, caster, false, true)
									SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, stat, caster.id.toString() + "", target.id.toString() + "," + jet + "," + -1)
									
}
608 -> {stat = buff.value
									jet = Formulas.getRandomJet(caster, target, buff.jet)
									target.addBuff(stat, jet, -1, false, buff.spell, buff.args, caster, false, true)
									SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, stat, caster.id.toString() + "", target.id.toString() + "," + jet + "," + -1)
									
}
609 -> {stat = buff.value
									jet = Formulas.getRandomJet(caster, target, buff.jet)
									target.addBuff(stat, jet, -1, false, buff.spell, buff.args, caster, false, true)
									SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, stat, caster.id.toString() + "", target.id.toString() + "," + jet + "," + -1)
									
}
611 -> {stat = buff.value
									jet = Formulas.getRandomJet(caster, target, buff.jet)
									target.addBuff(stat, jet, -1, false, buff.spell, buff.args, caster, false, true)
									SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, stat, caster.id.toString() + "", target.id.toString() + "," + jet + "," + -1)
									
}
788 -> {if (this.spell == 66 || spell == 71 || spell == 196 || spell == 213 || spell == 181 || spell == 200)
										continue
									var factor: Int = if (caster.player == null && caster.getInvocator() == null) 1 else 2
									var gain: Int = finalDommage / factor
									var max: Int = 0
									stat = buff.value

									try {
										max = (buff.args.split(";")[1]).toInt()
									} catch (e: Exception) {
										log.error("unexpected error", e)
										continue
									}

									//on retire au max possible la valeur déjà gagné sur le chati
									var oldValue: Int = (target.chatiValue[stat] ?: 0)
									max -= oldValue
									//Si gain trop grand, on le reduit au max
									if (gain > max) gain = max
									//On met a jour les valeurs des chatis
									if (gain == 0) continue
									var newValue: Int = oldValue + gain

									if (stat == 125) {
										SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, Constant.STATS_ADD_VIE, (caster.id).toString(), target.id.toString() + "," + gain)
										target.setPdv(target.getPdv() + gain)
										if (target.player != null)
											SocketManager.GAME_SEND_STATS_PACKET(target.player!!)
									} else {
										target.addBuff(stat, gain, 5, true, -1, buff.args, caster, false, true)
										SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, stat, caster.id.toString() + "", target.id.toString() + "," + gain + "," + 5)
									}
									target.chatiValue[stat] = newValue
									
}
else -> {
}
}
						}
						
}
}
			}
		}

		return finalDommage
	}

	fun getMaxMinSpell(fighter: Fighter, value: Int): Int {
		var vale: Int= value
		if (fighter.hasBuff(782)) {
			var max: Int = (args.split(";")[1]).toInt()
			if (max == -1)
				max = (args.split(";")[0]).toInt()
			vale = max
		} else if (fighter.hasBuff(781))
			vale = (args.split(";")[0]).toInt()
		return vale
	}

	fun decrementDuration(): Int {
		if(turns > 0) turns -= 1
		return turns
	}

	fun applyBeginingBuff(fight: Fight, fighter: Fighter) {
		var targets: ArrayList<Fighter> = ArrayList()
		targets.add(fighter)
		var turns: Int = this.turns
		this.turns = -1
		this.applyToFight(fight, this.caster!!, targets, false)
		this.turns = turns
	}

	fun applyToFight(fight: Fight, perso: Fighter, Cell: GameCase, targets2: ArrayList<Fighter>) {
		var targets = targets2
		cell = Cell
		targets.reverse()
		applyToFight(fight, perso, targets, false)
	}

	fun applyToFight(fight: Fight, acaster: Fighter, targets2: ArrayList<Fighter>, isCaC: Boolean) {
		var targets = targets2
		try {
			if (turns != -1)//Si ce n'est pas un buff qu'on applique en début de tour
				turns = (args.split(";")[3]).toInt()
		} catch (ignored: NumberFormatException) {}
		caster = acaster
		try {
			jet = args.split(";")[5]
		} catch (ignored: Exception) {}

		if (caster!!.player != null) {
			var perso: Player? = caster!!.player
			if (spell in perso!!.objectsClassSpell) {
				var modi: Int = 0
				if (effectID == 108)
					modi = perso!!.getValueOfClassObject(spell, 284)
				else if (effectID >= 91 && effectID <= 100)
					modi = perso!!.getValueOfClassObject(spell, 283)
				var jeta: String = jet.split("+")[0]
				var bonus: Int = (jet.split("+")[1]).toInt() + modi
				jet = jeta + "+" + bonus
			}
		}

		when (effectID){  4 -> {applyEffect_4(fight)
				
}
5 -> {applyEffect_5(targets, fight)
				
}
6 -> {applyEffect_6(targets, fight)
				
}
8 -> {applyEffect_8(targets, fight)
				
}
9 -> {applyEffect_9(targets)
				
}
50 -> {applyEffect_50(fight)
				
}
51 -> {applyEffect_51(fight)
				
}
77 -> {applyEffect_77(targets, fight)
				
}
78 -> {applyEffect_78(targets)
				
}
79 -> {applyEffect_79(targets)
				
}
81 -> {applyEffect_81(targets, fight)
				
}
82 -> {applyEffect_82(targets, fight)
				
}
84 -> {applyEffect_84(targets, fight)
				
}
85 -> {applyEffect_85(targets, fight)
				
}
86 -> {applyEffect_86(targets, fight)
				
}
87 -> {applyEffect_87(targets, fight)
				
}
88 -> {applyEffect_88(targets, fight)
				
}
89 -> {applyEffect_89(targets, fight)
				
}
90 -> {applyEffect_90(targets, fight)
				
}
91 -> {applyEffect_91(targets, fight, isCaC)
				
}
92 -> {applyEffect_92(targets, fight, isCaC)
				
}
93 -> {applyEffect_93(targets, fight, isCaC)
				
}
94 -> {applyEffect_94(targets, fight, isCaC)
				
}
95 -> {applyEffect_95(targets, fight, isCaC)
				
}
96 -> {applyEffect_96(targets, fight, isCaC)
				
}
97 -> {applyEffect_97(targets, fight, isCaC)
				
}
98 -> {applyEffect_98(targets, fight, isCaC)
				
}
99 -> {applyEffect_99(targets, fight, isCaC)
				
}
100 -> {applyEffect_100(targets, fight, isCaC)
				
}
101 -> {applyEffect_101(targets, fight)
				
}
105 -> {applyEffect_105(targets)
				
}
106 -> {applyEffect_106(targets)
				
}
107 -> {applyEffect_107(targets)
				
}
108 -> {applyEffect_108(targets, fight, isCaC)
				
}
109 -> {applyEffect_109(fight)
				
}
110 -> {applyEffect_110(targets, fight)
				
}
111 -> {applyEffect_111(targets, fight)
				
}
112 -> {applyEffect_112(targets, fight)
				
}
114 -> {applyEffect_114(targets, fight)
				
}
115 -> {applyEffect_115(targets)
				
}
116 -> {applyEffect_116(targets)
				
}
117 -> {applyEffect_117(targets)
				
}
118 -> {applyEffect_118(targets, fight)
				
}
119 -> {applyEffect_119(targets)
				
}
120 -> {applyEffect_120(fight)
				
}
121 -> {applyEffect_121(targets)
				
}
122 -> {applyEffect_122(targets)
				
}
123 -> {applyEffect_123(targets)
				
}
124 -> {applyEffect_124(targets)
				
}
125 -> {applyEffect_125(targets)
				
}
126 -> {applyEffect_126(targets, fight)
				
}
127 -> {applyEffect_127(targets, fight)
				
}
128 -> {applyEffect_128(targets, fight)
				
}
130 -> {applyEffect_130(fight, targets)
				
}
131 -> {applyEffect_131(targets)
				
}
132 -> {applyEffect_132(targets, fight)
				
}
138 -> {applyEffect_138(targets)
				
}
140 -> {applyEffect_140(targets)
				
}
141 -> {applyEffect_141(fight, targets)
				
}
142 -> {applyEffect_142(targets)
				
}
143 -> {applyEffect_143(targets, fight)
				
}
144 -> {applyEffect_144(targets)
// fallthrough
applyEffect_145(targets)
				
}
145 -> {applyEffect_145(targets)

}
149 -> {applyEffect_149(fight, targets)
				
}
150 -> {applyEffect_150(fight, targets)
				
}
152 -> {applyEffect_152(targets)
				
}
153 -> {applyEffect_153(targets)
				
}
154 -> {applyEffect_154(targets)
				
}
155 -> {applyEffect_155(targets)
				
}
156 -> {applyEffect_156(targets)
				
}
157 -> {applyEffect_157(targets)
				
}
160 -> {applyEffect_160(targets)
				
}
161 -> {applyEffect_161(targets)
				
}
162 -> {applyEffect_162(targets)
				
}
163 -> {applyEffect_163(fight, targets)
				
}
164 -> {applyEffect_164(targets)
				
}
165 -> {applyEffect_165()
				
}
168 -> {applyEffect_168(targets, fight)
				
}
169 -> {applyEffect_169(targets, fight)
				
}
171 -> {applyEffect_171(targets)
				
}
176 -> {applyEffect_176(targets)
				
}
177 -> {applyEffect_177(targets)
				
}
178 -> {applyEffect_178(targets)
				
}
179 -> {applyEffect_179(targets)
				
}
180 -> {applyEffect_180(fight)
				
}
181 -> {applyEffect_181(fight)
				
}
182 -> {applyEffect_182(targets)
				
}
183 -> {applyEffect_183(targets)
				
}
184 -> {applyEffect_184(targets)
				
}
185 -> {applyEffect_185(fight)
				
}
186 -> {applyEffect_186(targets)
				
}
202 -> {applyEffect_202(fight, targets)
				
}
210 -> {applyEffect_210(fight, targets)
				
}
211 -> {applyEffect_211(targets)
				
}
212 -> {applyEffect_212(targets)
				
}
213 -> {applyEffect_213(targets)
				
}
214 -> {applyEffect_214(targets)
				
}
215 -> {applyEffect_215(targets)
				
}
216 -> {applyEffect_216(targets)
				
}
217 -> {applyEffect_217(targets)
				
}
218 -> {applyEffect_218(targets)
				
}
219 -> {applyEffect_219(targets)
				
}
220 -> {applyEffect_220(targets)
				
}
265 -> {applyEffect_265(targets)
				
}
266 -> {applyEffect_266(targets)
				
}
267 -> {applyEffect_267(targets)
				
}
268 -> {applyEffect_268(targets)
				
}
269 -> {applyEffect_269(targets)
				
}
270 -> {applyEffect_270(targets)
				
}
271 -> {applyEffect_271(targets)
				
}
293 -> {applyEffect_293()
				
}
320 -> {applyEffect_320(targets)
				
}
400 -> {applyEffect_400(fight)
				
}
401 -> {applyEffect_401(fight)
				
}
402 -> {applyEffect_402(fight)
				
}
666 -> {
}
671 -> {applyEffect_671(targets, fight)
				
}
672 -> {applyEffect_672(targets, fight)
				
}
765 -> {applyEffect_765(targets)
				
}
776 -> {applyEffect_776(targets)
				
}
780 -> {applyEffect_780(fight)
				
}
781 -> {applyEffect_781(targets)
				
}
782 -> {applyEffect_782(targets)
				
}
783 -> {applyEffect_783(fight)
				
}
784 -> {applyEffect_784(fight)
				
}
786 -> {applyEffect_786(targets)
				
}
787 -> {applyEffect_787(targets)
				
}
788 -> {applyEffect_788(targets)
				
}
950 -> {applyEffect_950(fight, targets)
				
}
951 -> {applyEffect_951(fight, targets)
				
}
1000 -> {applyEffect_1000(fight)
				
}
1001 -> {applyEffect_1001(fight)
				
}
1002 -> {applyEffect_1002(fight)
				
}
else -> {GameServer.a()
				
}
}
	}


	private fun applyEffect_4(fight: Fight) {
		if (turns > 1 || caster!!.getHoldedBy() != null || caster!!.getIsHolding() != null)
			return;//Olol bondir 3 tours apres ?

		if (cell!!.isWalkable(true, true, cell!!.getId()) && !fight.isOccuped(cell!!.getId()))//Si la case est prise, on va �viter que les joueurs se montent dessus *-*
		{
			caster!!.cell!!.removeFighter(caster!!)
			caster!!.cell = (cell)
			caster!!.cell!!.addFighter(caster!!)

			var P: ArrayList<Trap> = ArrayList()
			P.addAll(fight.traps)
			for (p in  P) {
				var dist: Int = PathFinding.getDistanceBetween(fight.map, p.cell!!.getId(), caster!!.cell!!.getId())
				//on active le piege
				if (dist <= p.size)
					p.onTrapped(caster!!)
			}
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 4, caster!!.id.toString() + "", caster!!.id.toString() + "," + cell!!.getId())
		}
	}

	private fun applyEffect_5(targets2: ArrayList<Fighter>, fight: Fight) {
	var targets = targets2
		if (targets.size == 1 && spell == 120 || spell == 310)
			if (!targets[0].isDead)
				caster!!.setOldCible(targets[0])

		if (turns <= 0) {
			when (spell) {
73, 418, 151, 165 -> {targets = this.sortTargets(targets, fight)
					
}
}


			for (target in  targets) {
				if (target.haveState(Constant.ETAT_ENRACINE))
					continue

				var cell: GameCase? = this.cell

				if (target.cell!!.getId() == this.cell!!.getId() || spell == 73)
					cell = caster!!.cell

				var newCellId: Int = PathFinding.newCaseAfterPush(fight, cell!!, target.cell!!, value, spell == 1688)

				if (newCellId == 0)
					return
				if (newCellId < 0) { // dérobase = 0 dmg
					var a: Int = -newCellId
					var factor: Int = Formulas.getRandomJet(caster, target, "1d8+0") // 2 à 9
					var level: Double = if (caster!!.isInvocation()) caster!!.getInvocator()!!.getLvl() / 50.00 else caster!!.getLvl() / 50.00

					if (level < 0.1) level = 0.1

					var finalDmg: Int = (((8+ factor * level) * a).toInt())
					if (finalDmg < 1) finalDmg = 1
					if (finalDmg > target.getPdv()) finalDmg = target.getPdv()

					if (target.hasBuff(184)) {
						finalDmg = finalDmg - target.getBuff(184)!!.value;//Réduction physique
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster!!.id.toString() + "", target.id.toString() + "," + target.getBuff(184)!!.value)
					}
					if (target.hasBuff(105) && // poison passe à travers les réductions
							!(this.spell == 66 || spell == 71 ||spell == 196 || spell == 213 || spell == 181 || spell == 200)) {

						finalDmg = finalDmg - target.getBuff(105)!!.value;//Immu
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster!!.id.toString() + "", target.id.toString() + "," + target.getBuff(105)!!.value)
					}
					if (finalDmg > 0) {
						if(finalDmg > 200) finalDmg = Formulas.getRandomValue(189, 211)
						target.removePdv(caster!!, finalDmg)
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + ",-" + finalDmg)
						if (target.getPdv() <= 0) {
							fight.onFighterDie(target, target);//caster avant
							if (target.canPlay() && target.player != null) fight.endTurn(false)
							else if (target.canPlay()) target.setCanPlay(false)
							return
						}
					}
					a = value - a
					// Flèche de dispersion, Harponnage
					var launchCell: GameCase? = if (spell == 2028 || spell == 418 || spell == 987) cell else caster!!.cell
					newCellId = PathFinding.newCaseAfterPush(fight, launchCell!!, target.cell!!, a, spell == 73)

					var dir: Char = PathFinding.getDirBetweenTwoCase(cell!!.getId(), target.cell!!.getId(), fight.map, true)
					var nextCase: GameCase? = fight.map!!.getCase(PathFinding.GetCaseIDFromDirection(target.cell!!.getId(), dir, fight.map, true))

					if (nextCase != null && nextCase.firstFighter != null) {
						var wallTarget: Fighter? = nextCase!!.firstFighter
						finalDmg = finalDmg / 2
						if (finalDmg < 1) finalDmg = 1
						wallTarget!!.removePdv(caster!!, finalDmg)
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", wallTarget!!.id.toString() + ",-" + finalDmg)
						if (wallTarget!!.getPdv() <= 0)
                            fight.onFighterDie(wallTarget, target);//caster avant
					}

					if (newCellId == 0)
						continue
					if (fight.map!!.getCase(newCellId) == null)
						continue
				}

				var cacheCell: GameCase? = target.cell
				cacheCell!!.removeFighter(target)

				target.cell = (fight.map!!.getCase(newCellId))
				target.cell!!.addFighter(target)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 5, target.id.toString() + "", target.id.toString() + "," + newCellId)

				var holding: Fighter? = target.getIsHolding()
				if(holding != null) {
					holding.setState(Constant.ETAT_PORTE, 0)
					target.setState(Constant.ETAT_PORTEUR, 0)
					holding.setHoldedBy(null)
					target.setIsHolding(null)
					holding.cell = (cacheCell)
					cacheCell!!.addFighter(holding)
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, target.id.toString() + "", target.id.toString() + "," + Constant.ETAT_PORTEUR + ",0")
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, holding.id.toString() + "", holding.id.toString() + "," + Constant.ETAT_PORTE + ",0")
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 51, target.id.toString() + "", cacheCell!!.getId().toString() + "")
				}

				TimerWaiter.addNext({  -> fight.checkTraps(target) }, 750, TimeUnit.MILLISECONDS)

				if(fight.type == 7) {
					if(target.mob != null) {
						if(newCellId == 373 || newCellId == 359 || newCellId == 345) {
							fight.verifIfTeamBoufbowl(0)
							return
						}
						if(newCellId == 105 || newCellId == 119 || newCellId == 133) {
							fight.verifIfTeamBoufbowl(1)
							return
						}
					}
				}
			}
		}
	}

	private fun applyEffect_6(targets: ArrayList<Fighter>, fight: Fight) {
		if (turns <= 0) {
			for (target in  targets) {
				if((target.mob != null && 556 == target.mob!!.template!!.id) || target.haveState(Constant.ETAT_ENRACINE))
					continue

				var eCell: GameCase? = cell
				//Si meme case
				if (target.cell!!.getId() == cell!!.getId()) {
					//on prend la cellule caster
					eCell = caster!!.cell
				}
				var newCellID: Int = PathFinding.newCaseAfterPush(fight, eCell!!, target.cell!!, -value)
				if (newCellID == 0)
					continue

				if (newCellID < 0)//S'il a �t� bloqu�
				{
					var a: Int = -(value + newCellID)
					newCellID = PathFinding.newCaseAfterPush(fight, caster!!.cell!!, target.cell!!, a)
					if (newCellID == 0)
						continue
					if (fight.map!!.getCase(newCellID) == null)
						continue
				}


				var cacheCell: GameCase? = target.cell
				cacheCell!!.removeFighter(target)

				target.cell = (fight.map!!.getCase(newCellID))
				target.cell!!.addFighter(target)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 5, caster!!.id.toString() + "", target.id.toString() + "," + newCellID)

				var holding: Fighter? = target.getIsHolding()

				if(holding != null) {
					holding.setState(Constant.ETAT_PORTE, 0)
					target.setState(Constant.ETAT_PORTEUR, 0)
					holding.setHoldedBy(null)
					target.setIsHolding(null)
					holding.cell = (cacheCell)
					cacheCell!!.addFighter(holding)
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, target.id.toString() + "", target.id.toString() + "," + Constant.ETAT_PORTEUR + ",0")
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, holding.id.toString() + "", holding.id.toString() + "," + Constant.ETAT_PORTE + ",0")
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 51, target.id.toString() + "", cacheCell!!.getId().toString() + "")
				}

				TimerWaiter.addNext({  -> fight.checkTraps(target) }, 750, TimeUnit.MILLISECONDS)
			}
		}
	}

	private fun applyEffect_8(targets: ArrayList<Fighter>, fight: Fight) {
		if (targets.isEmpty())
			return
		var target: Fighter = targets[0]
		if (target == null || target.haveState(Constant.ETAT_PORTE) || target.haveState(Constant.ETAT_PORTEUR))
			return

		if(caster!!.getIsHolding() != null || caster!!.getHoldedBy() != null
				|| target.getHoldedBy() != null || target.getIsHolding() != null)
			return
		when (spell){  438 -> {if(target.haveState(Constant.ETAT_ENRACINE)) {
						return
					}
				//si les 2 joueurs ne sont pas dans la meme team, on ignore
				if (target.team != caster!!.team)
					return
				
}
445 -> {if(target.haveState(Constant.ETAT_ENRACINE)) {
					return
				}
				//si les 2 joueurs sont dans la meme team, on ignore
				if (target.team == caster!!.team)
					return
				
}
449 -> {if(target.haveState(Constant.ETAT_ENRACINE))
					return
				
}
else -> {
}
}
		//on enleve les persos des cases
		target.cell!!.removeFighter(target)
		caster!!.cell!!.removeFighter(caster!!)
		//on retient les cases
		var exTarget: GameCase? = target.cell
		var exCaster: GameCase? = caster!!.cell
		//on �change les cases
		target.cell = (exCaster)
		caster!!.cell = (exTarget)
		//on ajoute les fighters aux cases
		target.cell!!.addFighter(target)
		caster!!.cell!!.addFighter(caster!!)
		var P: ArrayList<Trap> = (ArrayList())
		P.addAll(fight.traps)
		for (p in  P) {
			var dist: Int = PathFinding.getDistanceBetween(fight.map, p.cell!!.getId(), target.cell!!.getId())
			var dist2: Int = PathFinding.getDistanceBetween(fight.map, p.cell!!.getId(), caster!!.cell!!.getId())
			//on active le piege
			if (dist <= p.size)
				p.onTrapped(target)
			else if (dist2 <= p.size)
				p.onTrapped(caster!!)
		}
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 4, caster!!.id.toString() + "", target.id.toString() + "," + exCaster!!.getId())
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 4, caster!!.id.toString() + "", caster!!.id.toString() + "," + exTarget!!.getId())
	}

	private fun applyEffect_9(targets: ArrayList<Fighter>) {
		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, false, true)
		}
	}

	private fun applyEffect_50(fight: Fight) {
		//Porter
		var target: Fighter? = cell!!.firstFighter
		if (target == null) return
		if (target.mob != null)
			for (i in  Constant.STATIC_INVOCATIONS)
				if (i == target.mob!!.template!!.id)
					return
		if (target.haveState(6)) return;//Stabilisation

		//on enleve le porté de sa case
		target.cell!!.removeFighter(target)
		//on lui définie sa nouvelle case
		target.cell = (caster!!.cell)

		//on applique les états
		target.setState(Constant.ETAT_PORTE, 1)
		caster!!.setState(Constant.ETAT_PORTEUR, 1)
		//on lie les 2 Fighter
		target.setHoldedBy(caster)
		caster!!.setIsHolding(target)

		//on envoie les packets
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", caster!!.id.toString() + "," + Constant.ETAT_PORTEUR + ",1")
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, target.id.toString() + "", target.id.toString() + "," + Constant.ETAT_PORTE + ",1")
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 50, caster!!.id.toString() + "", "" + target.id)
	}

	private fun applyEffect_51(fight: Fight) {
		//Si case pas libre
		if (!cell!!.isWalkableFight() || !cell!!.fighters.isEmpty()) return
		var target: Fighter? = caster!!.getIsHolding()
		if (target == null) return
		//if(target.isState(6))return;//Stabilisation
		//on ajoute le porté a sa case
		target.cell = (cell)
		target.cell!!.addFighter(target)
		//on enleve les états
		target.setState(Constant.ETAT_PORTE, 0)
		caster!!.setState(Constant.ETAT_PORTEUR, 0)
		//on dé-lie les 2 Fighter
		target.setHoldedBy(null)
		caster!!.setIsHolding(null)

		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", caster!!.id.toString() + "," + Constant.ETAT_PORTEUR + ",0")
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, target.id.toString() + "", target.id.toString() + "," + Constant.ETAT_PORTE + ",0")
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 51, caster!!.id.toString() + "", cell!!.getId().toString() + "")
		fight.checkTraps(target)
	}

	private fun applyEffect_77(targets: ArrayList<Fighter>, fight: Fight) {
		var total: Int = 0

		for (target in  targets) {
			var value: Int = Formulas.getRandomJet(caster, target, jet)
			var pointsLost: Int = Formulas.getPointsLost('a', value, caster!!, target)

			if (pointsLost < value)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 309, caster!!.id.toString() + "", target.id.toString() + "," + (value - pointsLost))
			if (pointsLost < 1)
				continue

			target.addBuff(Constant.STATS_REM_PM, pointsLost, if (turns == 0) 1 else turns, false, spell, args, caster!!, false, true)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, Constant.STATS_REM_PM, caster!!.id.toString() + "", target.id.toString() + ",-" + pointsLost + "," + turns)
			total += pointsLost
		}
		if (total != 0) {
			caster!!.addBuff(Constant.STATS_ADD_PM, total, turns, true, spell, args!!, caster!!, true, true)
			//Gain de PM pendant le tour de jeu
			if (caster!!.canPlay())
				caster!!.setCurPm(fight, total)
		}
	}

	private fun applyEffect_78(targets: ArrayList<Fighter>) { //Bonus PA
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_79(targets: ArrayList<Fighter>) {
		if (turns < 1) return

		for (target in  targets) {
			target.addBuff(effectID, -1, turns, true, spell, args, caster!!, false, true)
		}
	}

	private fun applyEffect_81(targets: ArrayList<Fighter>, fight: Fight) {// healcion
		if (turns <= 0) {
			var jet: List<String> = args.split(";")
			var heal: Int = 0
			if (jet.size < 6) {
				heal = 1
			} else {
				heal = Formulas.getRandomJet(caster, null, jet[5])
			}
			var heal2: Int = heal
			for (cible in  targets) {
				if (cible.isDead)
					continue
				if (spell == 521)// ruse kistoune
					if (cible.getTeam2() != caster!!.getTeam2())
						continue
				heal = getMaxMinSpell(cible, heal)
				var pdvMax: Int = cible.getPdvMax()
				var healFinal: Int = Formulas.calculFinalHealCac(caster!!, heal, false)
				if ((healFinal + cible.getPdv()) > pdvMax)
					healFinal = pdvMax - cible.getPdv()
				if (healFinal < 1)
					healFinal = 0
				cible.removePdv(caster!!, -healFinal)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 108, caster!!.id.toString() + "", cible.id.toString() + "," + healFinal + "," + Constant.getColorByElement(Constant.ELEMENT_FEU))
				heal = heal2
			}
		} else {
			for (target in  targets) {
				if (!target.isDead) {
					target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true)
				}
			}
		}
	}

	private fun applyEffect_82(targets: ArrayList<Fighter>, fight: Fight) {
		if (turns <= 0) {
			for (target in  targets) {
					var target = target
				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null && !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}

				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])
				//si la cible a le buff renvoie de sort et que le sort peut etre renvoyer
				if (target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0 && !this.isPoison()) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.id.toString() + "", target.id.toString() + ",1")
					//le lanceur devient donc la cible
					target = caster!!
				}

				var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, target, Constant.ELEMENT_NEUTRE, dmg, false, false, spell)
				finalDommage = applyOnHitBuffs(finalDommage, target, caster!!, fight, Constant.ELEMENT_NEUTRE);//S'il y a des buffs sp�ciaux
				if (finalDommage > target.getPdv())
					finalDommage = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, finalDommage)
				finalDommage = -(finalDommage)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + finalDommage)
				//Vol de vie
				var heal: Int = ((-finalDommage).toInt()) / 2
				if ((caster!!.getPdv() + heal) > caster!!.getPdvMax())
					heal = caster!!.getPdvMax() - caster!!.getPdv()
				caster!!.removePdv(caster!!, -heal)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 108, target.id.toString() + "", caster!!.id.toString() + "," + heal)

				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, target)
					if (target.canPlay() && target.player != null)
						fight.endTurn(false)
					else if (target.canPlay())
						target.setCanPlay(false)
				}
			}
		} else {
			for (target in  targets) {
				target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true);//on applique un buff
			}
		}
	}

	private fun applyEffect_84(targets: ArrayList<Fighter>, fight: Fight) {
		var total: Int = 0

		for (target in  targets) {
			var value: Int = Formulas.getRandomJet(caster, target, jet)
			var pointsLost: Int = Formulas.getPointsLost('a', value, caster!!, target)

			if (pointsLost < value)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 308, caster!!.id.toString() + "", target.id.toString() + "," + (value - pointsLost))
			if (pointsLost < 1)
				continue

			target.addBuff(Constant.STATS_REM_PA, pointsLost, if (turns == 0) 1 else turns, false, spell, args, caster!!, false, true)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, Constant.STATS_REM_PA, caster!!.id.toString() + "", target.id.toString() + ",-" + pointsLost + "," + turns)
			total += pointsLost
		}

		if (total != 0) {
			caster!!.addBuff(Constant.STATS_ADD_PA, total, 0, true, spell, args!!, caster!!, true, true)

			if (caster!!.canPlay()) {
				caster!!.setCurPa(fight, total)
			}
		}
	}

	private fun applyEffect_85(targets: ArrayList<Fighter>, fight: Fight) {
		if (turns <= 0) {
			for (target in  targets) {
					var target = target

				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}
				//si la cible a le buff renvoie de sort
				if (target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0 && !this.isPoison()) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.id.toString() + "", target.id.toString() + ",1")
					//le lanceur devient donc la cible
					target = caster!!
				}
				var resP: Int = target.getTotalStats().getEffect(Constant.STATS_ADD_RP_EAU)
				var resF: Int = target.getTotalStats().getEffect(Constant.STATS_ADD_R_EAU)
				if (target.player != null)//Si c'est un joueur, on ajoute les resists bouclier
				{
					resP += target.getTotalStats().getEffect(Constant.STATS_ADD_RP_PVP_EAU)
					resF += target.getTotalStats().getEffect(Constant.STATS_ADD_R_PVP_EAU)
				}
				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])//%age de pdv inflig�
				var vale: Int = caster!!.getPdv() / 100 * dmg//Valeur des d�gats
				//retrait de la r�sist fixe
				vale -= resF
				var reduc: Int = ((((vale.toFloat())) / (100 as Float)).toInt()) * resP//Reduc %resis
				vale -= reduc
				if (vale < 0)
					vale = 0

				vale = applyOnHitBuffs(vale, target, caster!!, fight, Constant.ELEMENT_NULL);//S'il y a des buffs sp�ciaux

				if (vale > target.getPdv())
					vale = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, vale)
				this.checkLifeTree(target, vale)
				vale = -(vale)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + vale)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, caster!!)
					if (target.canPlay() && target.player != null)
						fight.endTurn(false)
					else if (target.canPlay())
						target.setCanPlay(false)
				}
			}
		} else {
			for (target in  targets) {
				target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true);//on applique un buff
			}
		}
	}

	private fun applyEffect_86(targets: ArrayList<Fighter>, fight: Fight) {
		if (turns <= 0) {
			for (target in  targets) {
					var target = target

				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}
				//si la cible a le buff renvoie de sort
				if (target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0 && !this.isPoison()) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.id.toString() + "", target.id.toString() + ",1")
					//le lanceur devient donc la cible
					target = caster!!
				}
				var resP: Int = target.getTotalStats().getEffect(Constant.STATS_ADD_RP_TER)
				var resF: Int = target.getTotalStats().getEffect(Constant.STATS_ADD_R_TER)
				if (target.player != null)//Si c'est un joueur, on ajoute les resists bouclier
				{
					resP += target.getTotalStats().getEffect(Constant.STATS_ADD_RP_PVP_TER)
					resF += target.getTotalStats().getEffect(Constant.STATS_ADD_R_PVP_TER)
				}
				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])//%age de pdv inflig�
				var vale: Int = caster!!.getPdv() / 100 * dmg//Valeur des d�gats
				//retrait de la r�sist fixe
				vale -= resF
				var reduc: Int = ((((vale.toFloat())) / (100 as Float)).toInt()) * resP//Reduc %resis
				vale -= reduc
				if (vale < 0)
					vale = 0

				vale = applyOnHitBuffs(vale, target, caster!!, fight, Constant.ELEMENT_NULL);//S'il y a des buffs sp�ciaux

				if (vale > target.getPdv())
					vale = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, vale)
				this.checkLifeTree(target, vale)
				vale = -(vale)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + vale + "," + Constant.ELEMENT_TERRE)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, caster!!)
					if (target.canPlay() && target.player != null)
						fight.endTurn(false)
					else if (target.canPlay())
						target.setCanPlay(false)
				}
			}
		} else {
			for (target in  targets) {
				target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true);//on applique un buff
			}
		}
	}

	private fun applyEffect_87(targets: ArrayList<Fighter>, fight: Fight) {
		if (turns <= 0) {
			for (target in  targets) {
					var target = target

				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}
				//si la cible a le buff renvoie de sort
				if (target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0 && !this.isPoison()) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.id.toString() + "", target.id.toString() + ",1")
					//le lanceur devient donc la cible
					target = caster!!
				}

				var resP: Int = target.getTotalStats().getEffect(Constant.STATS_ADD_RP_AIR)
				var resF: Int = target.getTotalStats().getEffect(Constant.STATS_ADD_R_AIR)
				if (target.player != null)//Si c'est un joueur, on ajoute les resists bouclier
				{
					resP += target.getTotalStats().getEffect(Constant.STATS_ADD_RP_PVP_AIR)
					resF += target.getTotalStats().getEffect(Constant.STATS_ADD_R_PVP_AIR)
				}
				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])//%age de pdv inflig�
				var vale: Int = caster!!.getPdv() / 100 * dmg//Valeur des d�gats
				//retrait de la r�sist fixe
				vale -= resF
				var reduc: Int = ((((vale.toFloat())) / (100 as Float)).toInt()) * resP//Reduc %resis
				vale -= reduc
				if (vale < 0)
					vale = 0

				vale = applyOnHitBuffs(vale, target, caster!!, fight, Constant.ELEMENT_NULL);//S'il y a des buffs sp�ciaux

				if (vale > target.getPdv())
					vale = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, vale)
				this.checkLifeTree(target, vale)
				vale = -(vale)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + vale)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, caster!!)
					if (target.canPlay() && target.player != null)
						fight.endTurn(false)
					else if (target.canPlay())
						target.setCanPlay(false)
				}
			}
		} else {
			for (target in  targets) {
				target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true);//on applique un buff
			}
		}
	}

	private fun applyEffect_88(targets: ArrayList<Fighter>, fight: Fight) {
		if (turns <= 0) {
			for (target in  targets) {
					var target = target
				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}
				//si la cible a le buff renvoie de sort
				if (target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0 && !this.isPoison()) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.id.toString() + "", target.id.toString() + ",1")
					//le lanceur devient donc la cible
					target = caster!!
				}
				var resP: Int = target.getTotalStats().getEffect(Constant.STATS_ADD_RP_FEU)
				var resF: Int = target.getTotalStats().getEffect(Constant.STATS_ADD_R_FEU)
				if (target.player != null)//Si c'est un joueur, on ajoute les resists bouclier
				{
					resP += target.getTotalStats().getEffect(Constant.STATS_ADD_RP_PVP_FEU)
					resF += target.getTotalStats().getEffect(Constant.STATS_ADD_R_PVP_FEU)
				}
				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])//%age de pdv inflig�
				var vale: Int = caster!!.getPdv() / 100 * dmg//Valeur des d�gats
				//retrait de la r�sist fixe
				vale -= resF
				var reduc: Int = ((((vale.toFloat())) / (100 as Float)).toInt()) * resP//Reduc %resis
				vale -= reduc
				if (vale < 0)
					vale = 0

				vale = applyOnHitBuffs(vale, target, caster!!, fight, Constant.ELEMENT_NULL);//S'il y a des buffs sp�ciaux

				if (vale > target.getPdv())
					vale = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, vale)

				this.checkLifeTree(target, vale)
				vale = -(vale)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + vale)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, caster!!)
					if (target.canPlay() && target.player != null)
						fight.endTurn(false)
					else if (target.canPlay())
						target.setCanPlay(false)
				}
			}
		} else {
			for (target in  targets) {
				target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true);//on applique un buff
			}
		}
	}

	private fun applyEffect_89(targets: ArrayList<Fighter>, fight: Fight) {
		if (turns <= 0) {
			for (target in  targets) {
					var target = target
				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}
				//si la cible a le buff renvoie de sort
				if (target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0 && !this.isPoison()) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.id.toString() + "", target.id.toString() + ",1")
					//le lanceur devient donc la cible
					target = caster!!
				}
				var resP: Int = target.getTotalStats().getEffect(Constant.STATS_ADD_RP_NEU)
				var resF: Int = target.getTotalStats().getEffect(Constant.STATS_ADD_R_NEU)
				if (target.player != null)//Si c'est un joueur, on ajoute les resists bouclier
				{
					resP += target.getTotalStats().getEffect(Constant.STATS_ADD_RP_PVP_NEU)
					resF += target.getTotalStats().getEffect(Constant.STATS_ADD_R_PVP_NEU)
				}
				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])//%age de pdv inflig�
				var vale: Int = caster!!.getPdv() / 100 * dmg//Valeur des d�gats
				//retrait de la r�sist fixe
				vale -= resF
				var reduc: Int = ((((vale.toFloat())) / (100 as Float)).toInt()) * resP//Reduc %resis
				vale -= reduc
				var armor: Int = 0
				for (SE in  target.getBuffsByEffectID(105)) {
					var intell: Int = target.getTotalStats().getEffect(Constant.STATS_ADD_INTE)
					var carac: Int = target.getTotalStats().getEffect(Constant.STATS_ADD_FORC)
					var value: Int = SE.value
					var a: Int = value * (100 + intell / 2 + carac / 2) / 100
					armor += a
				}
				if (armor > 0) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster!!.id.toString() + "", target.id.toString() + "," + armor)
					vale = vale - armor
				}
				if (vale < 0) vale = 0
				vale = applyOnHitBuffs(vale, target, caster!!, fight, Constant.ELEMENT_NULL);//S'il y a des buffs sp�ciaux

				if (vale > target.getPdv()) vale = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, vale)

				this.checkLifeTree(target, vale)
				vale = -(vale)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + vale + "," + Constant.ELEMENT_NEUTRE)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, caster!!)
					if (target.canPlay() && target.player != null)
						fight.endTurn(false)
					else if (target.canPlay())
						target.setCanPlay(false)
				}
			}
		} else {
			for (target in  targets) {
				target.addBuff(effectID, value, turns, true, spell, args, caster!!, false, true);//on applique un buff
			}
		}
	}

	private fun applyEffect_90(targets: ArrayList<Fighter>, fight: Fight) {
		if (turns <= 0)//Si Direct
		{
			var pAge: Int = Formulas.getRandomJet(caster, null, args.split(";")[5])
			var vale: Int= pAge * (caster!!.getPdv() / 100)
			//Calcul des Doms recus par le lanceur
			var finalDommage: Int = applyOnHitBuffs(vale, caster!!, caster!!, fight, Constant.ELEMENT_NULL)//S'il y a des buffs sp�ciaux

			if (finalDommage > caster!!.getPdv())
				finalDommage = caster!!.getPdv();//Caster va mourrir
			caster!!.removePdv(caster!!, finalDommage)
			finalDommage = -(finalDommage)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", caster!!.id.toString() + "," + finalDommage + "," + Constant.ELEMENT_NEUTRE)

			//Application du soin
			for (target in  targets) {
				if ((vale + target.getPdv()) > target.getPdvMax())
					vale = target.getPdvMax() - target.getPdv();//Target va mourrir
				target.removePdv(caster!!, -vale)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + ",+" + vale)
			}
			if (caster!!.getPdv() <= 0)
				fight.onFighterDie(caster!!, caster!!)
		} else {
			for (target in  targets) {
				target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true);//on applique un buff
			}
		}
	}

	private fun applyEffect_91(targets: ArrayList<Fighter>, fight: Fight, isCaC: Boolean)//vole eau
	{
		if (isCaC) {
			for (target in  targets) {
					var target = target
				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}

				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])
				var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, target, Constant.ELEMENT_EAU, dmg, false, true, spell)

				finalDommage = applyOnHitBuffs(finalDommage, target, caster!!, fight, Constant.ELEMENT_EAU);//S'il y a des buffs sp�ciaux

				if (finalDommage > target.getPdv())
					finalDommage = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, finalDommage)
				finalDommage = -(finalDommage)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + finalDommage + "," + Constant.getColorByElement(Constant.ELEMENT_EAU))
				var heal: Int = ((-finalDommage).toInt()) / 2
				if ((caster!!.getPdv() + heal) > caster!!.getPdvMax())
					heal = caster!!.getPdvMax() - caster!!.getPdv()
				caster!!.removePdv(caster!!, -heal)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, target.id.toString() + "", caster!!.id.toString() + "," + heal)
				if (target.mob != null)
					checkMonsters(fight, target, 91)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, caster!!)
					if (target.canPlay() && target.player != null)
						fight.endTurn(false)
					else if (target.canPlay())
						target.setCanPlay(false)
				}
			}
		} else if (turns <= 0) {
			if (caster!!.isHidden())
				caster!!.unHide(spell)
			for (target in  targets) {
					var target = target

				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}
				//si la cible a le buff renvoie de sort
				if (target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0 && !this.isPoison()) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.id.toString() + "", target.id.toString() + ",1")
					//le lanceur devient donc la cible
					target = caster!!
				}
				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])
				var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, target, Constant.ELEMENT_EAU, dmg, false, false, spell)

				finalDommage = applyOnHitBuffs(finalDommage, target, caster!!, fight, Constant.ELEMENT_EAU);//S'il y a des buffs sp�ciaux
				if (finalDommage > target.getPdv())
					finalDommage = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, finalDommage)
				finalDommage = -(finalDommage)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + finalDommage + "," + Constant.getColorByElement(Constant.ELEMENT_EAU))
				var heal: Int = ((-finalDommage).toInt()) / 2
				if ((caster!!.getPdv() + heal) > caster!!.getPdvMax())
					heal = caster!!.getPdvMax() - caster!!.getPdv()
				caster!!.removePdv(caster!!, -heal)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, target.id.toString() + "", caster!!.id.toString() + "," + heal)
				if (target.mob != null)
					checkMonsters(fight, target, 91)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, target)
					if (target.canPlay() && target.player != null)
						fight.endTurn(false)
					else if (target.canPlay())
						target.setCanPlay(false)
				}
			}
		} else {
			for (target in  targets) {
				target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true);//on applique un buff
			}
		}
	}

	private fun applyEffect_92(targets: ArrayList<Fighter>, fight: Fight, isCaC: Boolean)//vole terre
	{
		if (caster!!.isHidden())
			caster!!.unHide(spell)
		if (isCaC) {
			for (target in  targets) {
					var target = target
				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}

				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])
				var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, target, Constant.ELEMENT_TERRE, dmg, false, true, spell)

				finalDommage = applyOnHitBuffs(finalDommage, target, caster!!, fight, Constant.ELEMENT_TERRE);//S'il y a des buffs sp�ciaux

				if (finalDommage > target.getPdv())
					finalDommage = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, finalDommage)
				finalDommage = -(finalDommage)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + finalDommage + "," + Constant.ELEMENT_TERRE)
				var heal: Int = ((-finalDommage).toInt()) / 2
				if ((caster!!.getPdv() + heal) > caster!!.getPdvMax())
					heal = caster!!.getPdvMax() - caster!!.getPdv()
				caster!!.removePdv(caster!!, -heal)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, target.id.toString() + "", caster!!.id.toString() + "," + heal)
				if (target.mob != null)
					checkMonsters(fight, target, 92)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, caster!!)
					if (target.canPlay() && target.player != null)
						fight.endTurn(false)
					else if (target.canPlay())
						target.setCanPlay(false)
				}
			}
		} else if (turns <= 0) {
			for (target in  targets) {
					var target = target
				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}
				//si la cible a le buff renvoie de sort
				if (target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0 && !this.isPoison()) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.id.toString() + "", target.id.toString() + ",1")
					//le lanceur devient donc la cible
					target = caster!!
				}
				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])
				var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, target, Constant.ELEMENT_TERRE, dmg, false, false, spell)

				finalDommage = applyOnHitBuffs(finalDommage, target, caster!!, fight, Constant.ELEMENT_TERRE);//S'il y a des buffs sp�ciaux
				if (finalDommage > target.getPdv())
					finalDommage = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, finalDommage)
				finalDommage = -(finalDommage)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + finalDommage + "," + Constant.ELEMENT_TERRE)
				var heal: Int = ((-finalDommage).toInt()) / 2
				if ((caster!!.getPdv() + heal) > caster!!.getPdvMax())
					heal = caster!!.getPdvMax() - caster!!.getPdv()
				caster!!.removePdv(caster!!, -heal)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, target.id.toString() + "", caster!!.id.toString() + "," + heal)
				if (target.mob != null)
					checkMonsters(fight, target, 92)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, target)
					if (target.canPlay() && target.player != null)
						fight.endTurn(false)
					else if (target.canPlay())
						target.setCanPlay(false)
				}
			}
		} else {
			for (target in  targets) {
				target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true);//on applique un buff
			}
		}
	}

	private fun applyEffect_93(targets: ArrayList<Fighter>, fight: Fight, isCaC: Boolean)//vole air
	{
		if (caster!!.isHidden())
			caster!!.unHide(spell)
		if (isCaC) {
			for (target in  targets) {
					var target = target
				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}

				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])
				var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, target, Constant.ELEMENT_AIR, dmg, false, true, spell)

				finalDommage = applyOnHitBuffs(finalDommage, target, caster!!, fight, Constant.ELEMENT_AIR);//S'il y a des buffs sp�ciaux

				if (finalDommage > target.getPdv())
					finalDommage = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, finalDommage)
				finalDommage = -(finalDommage)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + finalDommage + "," + Constant.ELEMENT_AIR)
				var heal: Int = ((-finalDommage).toInt()) / 2
				if ((caster!!.getPdv() + heal) > caster!!.getPdvMax())
					heal = caster!!.getPdvMax() - caster!!.getPdv()
				caster!!.removePdv(caster!!, -heal)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, target.id.toString() + "", caster!!.id.toString() + "," + heal)
				if (target.mob != null)
					checkMonsters(fight, target, 93)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, caster!!)
					if (target.canPlay() && target.player != null)
						fight.endTurn(false)
					else if (target.canPlay())
						target.setCanPlay(false)
				}
			}
		} else if (turns <= 0) {
			for (target in  targets) {
					var target = target
				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}
				//si la cible a le buff renvoie de sort
				if (target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0 && !this.isPoison()) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.id.toString() + "", target.id.toString() + ",1")
					//le lanceur devient donc la cible
					target = caster!!
				}
				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])
				var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, target, Constant.ELEMENT_AIR, dmg, false, false, spell)

				finalDommage = applyOnHitBuffs(finalDommage, target, caster!!, fight, Constant.ELEMENT_AIR);//S'il y a des buffs sp�ciaux
				if (finalDommage > target.getPdv())
					finalDommage = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, finalDommage)
				finalDommage = -(finalDommage)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + finalDommage + "," + Constant.ELEMENT_AIR)

				var heal: Int = ((-finalDommage).toInt()) / 2
				if ((caster!!.getPdv() + heal) > caster!!.getPdvMax())
					heal = caster!!.getPdvMax() - caster!!.getPdv()
				caster!!.removePdv(caster!!, -heal)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, target.id.toString() + "", caster!!.id.toString() + "," + heal)
				if (target.mob != null)
					checkMonsters(fight, target, 93)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, target)
					if (target.canPlay() && target.player != null)
						fight.endTurn(false)
					else if (target.canPlay())
						target.setCanPlay(false)
				}
			}
		} else {
			for (target in  targets) {
				target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true);//on applique un buff
			}
		}
	}

	private fun applyEffect_94(targets: ArrayList<Fighter>, fight: Fight, isCaC: Boolean) {
		if (caster!!.isHidden())
			caster!!.unHide(spell)
		if (isCaC)//CaC feu
		{
			for (target in  targets) {
					var target = target
				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}

				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])
				var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, target, Constant.ELEMENT_FEU, dmg, false, true, spell)

				finalDommage = applyOnHitBuffs(finalDommage, target, caster!!, fight, Constant.ELEMENT_FEU);//S'il y a des buffs sp�ciaux

				if (finalDommage > target.getPdv())
					finalDommage = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, finalDommage)
				finalDommage = -(finalDommage)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + finalDommage + "," + Constant.getColorByElement(Constant.ELEMENT_FEU))
				var heal: Int = ((-finalDommage).toInt()) / 2
				if ((caster!!.getPdv() + heal) > caster!!.getPdvMax())
					heal = caster!!.getPdvMax() - caster!!.getPdv()
				caster!!.removePdv(caster!!, -heal)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, target.id.toString() + "", caster!!.id.toString() + "," + heal)
				if (target.mob != null)
					checkMonsters(fight, target, 94)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, caster!!)
					if (target.canPlay() && target.player != null)
						fight.endTurn(false)
					else if (target.canPlay())
						target.setCanPlay(false)
				}
			}
		} else if (turns <= 0) {
			for (target in  targets) {
					var target = target

				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}
				//si la cible a le buff renvoie de sort
				if (target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0 && !this.isPoison()) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.id.toString() + "", target.id.toString() + ",1")
					//le lanceur devient donc la cible
					target = caster!!
				}
				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])
				var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, target, Constant.ELEMENT_FEU, dmg, false, false, spell)

				finalDommage = applyOnHitBuffs(finalDommage, target, caster!!, fight, Constant.ELEMENT_FEU);//S'il y a des buffs sp�ciaux
				if (finalDommage > target.getPdv())
					finalDommage = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, finalDommage)
				finalDommage = -(finalDommage)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + finalDommage + "," + Constant.getColorByElement(Constant.ELEMENT_FEU))
				var heal: Int = ((-finalDommage).toInt()) / 2
				if ((caster!!.getPdv() + heal) > caster!!.getPdvMax())
					heal = caster!!.getPdvMax() - caster!!.getPdv()
				caster!!.removePdv(caster!!, -heal)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, target.id.toString() + "", caster!!.id.toString() + "," + heal)
				if (target.mob != null)
					checkMonsters(fight, target, 94)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, target)
					if (target.canPlay() && target.player != null)
						fight.endTurn(false)
					else if (target.canPlay())
						target.setCanPlay(false)
				}
			}
		} else {
			for (target in  targets) {
				target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true);//on applique un buff
			}
		}
	}

	private fun applyEffect_95(targets: ArrayList<Fighter>, fight: Fight, isCaC: Boolean) {
		if (caster!!.isHidden())
			caster!!.unHide(spell)
		if (isCaC)//CaC Eau
		{
			for (target in  targets) {
					var target = target
				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}

				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])
				var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, target, Constant.ELEMENT_NEUTRE, dmg, false, true, spell)

				finalDommage = applyOnHitBuffs(finalDommage, target, caster!!, fight, Constant.ELEMENT_NEUTRE);//S'il y a des buffs sp�ciaux

				if (finalDommage > target.getPdv())
					finalDommage = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, finalDommage)

				finalDommage = -(finalDommage)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + finalDommage + "," + Constant.ELEMENT_NEUTRE)
				var heal: Int = ((-finalDommage).toInt()) / 2
				if ((caster!!.getPdv() + heal) > caster!!.getPdvMax())
					heal = caster!!.getPdvMax() - caster!!.getPdv()
				caster!!.removePdv(caster!!, -heal)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, target.id.toString() + "", caster!!.id.toString() + "," + heal)
				if (target.mob != null)
					checkMonsters(fight, target, 95)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, caster!!)
					if (target.canPlay() && target.player != null)
						fight.endTurn(false)
					else if (target.canPlay())
						target.setCanPlay(false)
				}
			}
		} else if (turns <= 0) {
			for (target in  targets) {
					var target = target

				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}
				//si la cible a le buff renvoie de sort
				if (target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0 && !this.isPoison()) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.id.toString() + "", target.id.toString() + ",1")
					//le lanceur devient donc la cible
					target = caster!!
				}
				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])
				var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, target, Constant.ELEMENT_NEUTRE, dmg, false, false, spell)

				finalDommage = applyOnHitBuffs(finalDommage, target, caster!!, fight, Constant.ELEMENT_NEUTRE);//S'il y a des buffs sp�ciaux
				if (finalDommage > target.getPdv())
					finalDommage = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, finalDommage)
				finalDommage = -(finalDommage)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + finalDommage + "," + Constant.ELEMENT_NEUTRE)

				var heal: Int = ((-finalDommage).toInt()) / 2
				if ((caster!!.getPdv() + heal) > caster!!.getPdvMax())
					heal = caster!!.getPdvMax() - caster!!.getPdv()
				caster!!.removePdv(caster!!, -heal)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, target.id.toString() + "", caster!!.id.toString() + "," + heal)
				if (target.mob != null)
					checkMonsters(fight, target, 95)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, target)
					if (target.canPlay() && target.player != null)
						fight.endTurn(false)
					else if (target.canPlay())
						target.setCanPlay(false)
				}
			}
		} else {
			for (target in  targets) {
				target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true);//on applique un buff
			}
		}
	}

	private fun applyEffect_96(targets: ArrayList<Fighter>, fight: Fight, isCaC: Boolean)//dmg eau
	{
		if (isCaC)//CaC Eau
		{
			if (caster!!.isHidden())
				caster!!.unHide(spell)
			for (target in  targets) {
					var target = target
				if (caster!!.mob != null && (caster!!.getTeam2() == target.getTeam2())
						&& !caster!!.isInvocation())
					continue; // Les monstres de s'entretuent pas
				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}

				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])

				//Si le sort est boost� par un buff sp�cifique
				for (SE in  caster!!.getBuffsByEffectID(293)) {
					if (SE.value == spell) {
						var add: Int = -1
						try {
							add = (SE.args.split(";")[2]).toInt()
						} catch (e: Exception) {
							log.error("unexpected error", e)
						}
						if (add <= 0)
							continue
						dmg += add
					}
				}
				var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, target, Constant.ELEMENT_EAU, dmg, false, true, spell)

				finalDommage = applyOnHitBuffs(finalDommage, target, caster!!, fight, Constant.ELEMENT_EAU);//S'il y a des buffs sp�ciaux

				if (finalDommage > target.getPdv())
					finalDommage = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, finalDommage)

				this.checkLifeTree(target, finalDommage)
				finalDommage = -(finalDommage)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + finalDommage + "," + Constant.getColorByElement(Constant.ELEMENT_EAU))
				if (target.mob != null)
					checkMonsters(fight, target, 96)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, caster!!)
					if (target.canPlay() && target.player != null)
						fight.endTurn(false)
					else if (target.canPlay())
						target.setCanPlay(false)
				}
			}
		} else if (turns <= 0) {
			if (caster!!.isHidden())
				caster!!.unHide(spell)
			for (target in  targets) {
					var target = target
				if (spell != 946 && caster!!.mob != null && (caster!!.getTeam2() == target.getTeam2()) && !caster!!.isInvocation())
					continue; // Les monstres de s'entretuent pas

				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}
				//si la cible a le buff renvoie de sort
				if (target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0 && !this.isPoison()) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.id.toString() + "", target.id.toString() + ",1")
					//le lanceur devient donc la cible
					target = caster!!
				}
				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])

				//Si le sort est boost� par un buff sp�cifique
				for (SE in  caster!!.getBuffsByEffectID(293)) {
					if (SE.value == spell) {
						var add: Int = -1
						try {
							add = (SE.args.split(";")[2]).toInt()
						} catch (e: Exception) {
							log.error("unexpected error", e)
						}
						if (add <= 0)
							continue
						dmg += add
					}
				}

				var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, target, Constant.ELEMENT_EAU, dmg, false, false, spell)

				finalDommage = applyOnHitBuffs(finalDommage, target, caster!!, fight, Constant.ELEMENT_EAU);//S'il y a des buffs sp�ciaux

				if (finalDommage > target.getPdv())
					finalDommage = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, finalDommage)

				this.checkLifeTree(target, finalDommage)
				finalDommage = -(finalDommage)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + finalDommage + "," + Constant.getColorByElement(Constant.ELEMENT_EAU))
				if (target.mob != null)
					checkMonsters(fight, target, 96)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, caster!!)
					if(!target.isTrappedOrGlyphed()) {
						if (target.canPlay() && target.player != null)
							fight.endTurn(false)
						else if (target.canPlay())
							target.setCanPlay(false)
					}
				}
			}
		} else {
			for (target in  targets)
				target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true);//on applique un buff
		}
	}

	private fun applyEffect_97(targets: ArrayList<Fighter>, fight: Fight, isCaC: Boolean)//dmg terre
	{
		if (isCaC)//CaC Terre
		{
			if (caster!!.isHidden())
				caster!!.unHide(spell)
			for (target in  targets) {
					var target = target
				if (caster!!.mob != null) {
					if (spell != 946 && caster!!.getTeam2() == target.getTeam2() && !caster!!.isInvocation())
						continue; // Les monstres de s'entretuent pas
				}

				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null && !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}

				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])

				//Si le sort est boost� par un buff sp�cifique
				for (SE in  caster!!.getBuffsByEffectID(293)) {
					if (SE.value == spell) {
						var add: Int = -1
						try {
							add = (SE.args.split(";")[2]).toInt()
						} catch (e: Exception) {
							log.error("unexpected error", e)
						}
						if (add <= 0)
							continue
						dmg += add
					}
				}
				var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, target, Constant.ELEMENT_TERRE, dmg, false, true, spell)

				finalDommage = applyOnHitBuffs(finalDommage, target, caster!!, fight, Constant.ELEMENT_TERRE);//S'il y a des buffs sp�ciaux

				if (finalDommage > target.getPdv())
					finalDommage = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, finalDommage)

				this.checkLifeTree(target, finalDommage)

				finalDommage = -(finalDommage)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + finalDommage + "," + Constant.ELEMENT_TERRE)
				if (target.mob != null)
					checkMonsters(fight, target, 97)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, caster!!)
					if (target.canPlay() && target.player != null)
						fight.endTurn(false)
					else if (target.canPlay())
						target.setCanPlay(false)
				}
			}
		} else if (turns <= 0) {
			if (caster!!.isHidden())
				caster!!.unHide(spell)
			for (target in  targets) {
					var target = target
				if (spell != 946 &&caster!!.mob != null && (caster!!.getTeam2() == target.getTeam2()) && !caster!!.isInvocation())
					continue; // Les monstres de s'entretuent pas

				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}
				//si la cible a le buff renvoie de sort

				if (target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0 && !this.isPoison()) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.id.toString() + "", target.id.toString() + ",1")
					//le lanceur devient donc la cible
					target = caster!!
				}

				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])

				//Si le sort est boost� par un buff sp�cifique
				if (caster!!.hasBuff(293) || caster!!.haveState(300)) {
					if (caster!!.haveState(300))
						caster!!.setState(300, 0)
					for (SE in  caster!!.getBuffsByEffectID(293)) {
						if (SE == null)
							continue
						if (SE.value == spell) {
							var add: Int = -1
							try {
								add = (SE.args.split(";")[2]).toInt()
							} catch (e: Exception) {
								log.error("unexpected error", e)
							}
							if (add <= 0)
								continue
							dmg += add
						}
					}
				}

				var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, target, Constant.ELEMENT_TERRE, dmg, false, false, spell)
				finalDommage = applyOnHitBuffs(finalDommage, target, caster!!, fight, Constant.ELEMENT_TERRE);//S'il y a des buffs sp�ciaux
				if (finalDommage > target.getPdv())
					finalDommage = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, finalDommage)

				this.checkLifeTree(target, finalDommage)
				finalDommage = -(finalDommage)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + finalDommage + "," + Constant.ELEMENT_TERRE)
				if (target.mob != null)
					checkMonsters(fight, target, 97)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, caster!!)
					if (!target.isTrappedOrGlyphed()) {
						if (target.canPlay() && target.player != null)
							fight.endTurn(false)
						else if (target.canPlay())
							target.setCanPlay(false)
					}
				}
			}
		} else {
			if (spell == 470) {
				for (target in  targets) {
					if (target.team == caster!!.team)
						continue
					target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true);//on applique un buff
				}
			}
			for (target in  targets) {
				target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true);//on applique un buff
			}
		}
	}

	private fun applyEffect_98(targets: ArrayList<Fighter>, fight: Fight, isCaC: Boolean)//dmg air
	{
		if (isCaC)//CaC Air
		{
			if (caster!!.isHidden())
				caster!!.unHide(spell)
			for (target in  targets) {
					var target = target
				if (spell != 946 && caster!!.mob != null && (caster!!.getTeam2() == target.getTeam2()) && !caster!!.isInvocation())
					continue; // Les monstres de s'entretuent pas
				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}

				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])

				//Si le sort est boost� par un buff sp�cifique
				for (SE in  caster!!.getBuffsByEffectID(293)) {
					if (SE.value == spell) {
						var add: Int = -1
						try {
							add = (SE.args.split(";")[2]).toInt()
						} catch (e: Exception) {
							log.error("unexpected error", e)
						}
						if (add <= 0)
							continue
						dmg += add
					}
				}

				// applyEffect_142

				var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, target, Constant.ELEMENT_AIR, dmg, false, true, spell)

				finalDommage = applyOnHitBuffs(finalDommage, target, caster!!, fight, Constant.ELEMENT_AIR);//S'il y a des buffs sp�ciaux

				if (finalDommage > target.getPdv())
					finalDommage = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, finalDommage)

				this.checkLifeTree(target, finalDommage)
				finalDommage = -(finalDommage)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + finalDommage + "," + Constant.ELEMENT_AIR)
				if (target.mob != null)
					checkMonsters(fight, target, 98)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, caster!!)
					if (target.canPlay() && target.player != null)
						fight.endTurn(false)
					else if (target.canPlay())
						target.setCanPlay(false)
				}
			}
		} else if (turns <= 0) {
			if (caster!!.isHidden())
				caster!!.unHide(spell)
			for (target in  targets) {
					var target = target
				if (spell != 946 &&caster!!.mob != null && (caster!!.getTeam2() == target.getTeam2()) && !caster!!.isInvocation())
					continue; // Les monstres de s'entretuent pas

				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}
				//si la cible a le buff renvoie de sort
				if (target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0 && !this.isPoison()) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.id.toString() + "", target.id.toString() + ",1")
					//le lanceur devient donc la cible
					target = caster!!
				}
				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])

				//Si le sort est boost� par un buff sp�cifique
				for (SE in  caster!!.getBuffsByEffectID(293)) {
					if (SE.value == spell) {
						var add: Int = -1
						try {
							add = (SE.args.split(";")[2]).toInt()
						} catch (e: Exception) {
							log.error("unexpected error", e)
						}
						if (add <= 0)
							continue
						dmg += add
					}
				}

				var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, target, Constant.ELEMENT_AIR, dmg, false, false, spell)

				finalDommage = applyOnHitBuffs(finalDommage, target, caster!!, fight, Constant.ELEMENT_AIR);//S'il y a des buffs sp�ciaux

				if (finalDommage > target.getPdv())
					finalDommage = target.getPdv();//Target va mourrir

				target.removePdv(caster!!, finalDommage)

				this.checkLifeTree(target, finalDommage)
				finalDommage = -(finalDommage)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + finalDommage + "," + Constant.ELEMENT_AIR)
				if (target.mob != null)
					checkMonsters(fight, target, 98)

				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, caster!!)
					if(!target.isTrappedOrGlyphed()) {
						if (target.canPlay() && target.player != null)
							fight.endTurn(false)
						else if (target.canPlay())
							target.setCanPlay(false)
					}
				}
			}
		} else {
			for (target in  targets) {
				target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true);//on applique un buff
			}
		}
	}

	private fun applyEffect_99(targets: ArrayList<Fighter>, fight: Fight, isCaC: Boolean)//dmg feu
	{
		if (caster!!.isHidden())
			caster!!.unHide(spell)

		if (isCaC)//CaC Feu
		{
			for (target in  targets) {
					var target = target
				if (spell != 946 && caster!!.mob != null && (caster!!.getTeam2() == target.getTeam2()) && !caster!!.isInvocation())
					continue; // Les monstres de s'entretuent pas
				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}

				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])

				//Si le sort est boost� par un buff sp�cifique
				for (SE in  caster!!.getBuffsByEffectID(293)) {
					if (SE.value == spell) {
						var add: Int = -1
						try {
							add = (SE.args.split(";")[2]).toInt()
						} catch (e: Exception) {
							log.error("unexpected error", e)
						}
						if (add <= 0)
							continue
						dmg += add
					}
				}
				var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, target, Constant.ELEMENT_FEU, dmg, false, true, spell)

				finalDommage = applyOnHitBuffs(finalDommage, target, caster!!, fight, Constant.ELEMENT_FEU);//S'il y a des buffs sp�ciaux

				if (finalDommage > target.getPdv())
					finalDommage = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, finalDommage)

				this.checkLifeTree(target, finalDommage)
				finalDommage = -(finalDommage)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + finalDommage + "," + Constant.getColorByElement(Constant.ELEMENT_FEU))
				if (target.mob != null)
					checkMonsters(fight, target, 99)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, caster!!)
					if (target.canPlay() && target.player != null)
						fight.endTurn(false)
					else if (target.canPlay())
						target.setCanPlay(false)
				}
			}
		} else if (turns <= 0) {
			for (target in  targets) {
					var target = target
				if (spell != 946 && caster!!.mob != null && (caster!!.getTeam2() == target.getTeam2()) && !caster!!.isInvocation())
					continue; // Les monstres de s'entretuent pas
				if (spell == 36 && target == caster)//Frappe du Craqueleur ne tape pas l'osa
				{
					continue
				}

				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}
				//si la cible a le buff renvoie de sort
				if (target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0 && !this.isPoison()) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.id.toString() + "", target.id.toString() + ",1")
					//le lanceur devient donc la cible
					target = caster!!
				}
				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])

				//Si le sort est boost� par un buff sp�cifique
				for (SE in  caster!!.getBuffsByEffectID(293)) {
					if (SE.value == spell) {
						var add: Int = -1
						try {
							add = (SE.args.split(";")[2]).toInt()
						} catch (e: Exception) {
							log.error("unexpected error", e)
						}
						if (add <= 0)
							continue
						dmg += add
					}
				}

				var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, target, Constant.ELEMENT_FEU, dmg, false, false, spell)

				finalDommage = applyOnHitBuffs(finalDommage, target, caster!!, fight, Constant.ELEMENT_FEU);//S'il y a des buffs sp�ciaux

				if (finalDommage > target.getPdv())
					finalDommage = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, finalDommage)

				this.checkLifeTree(target, finalDommage)
				finalDommage = -(finalDommage)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + finalDommage + "," + Constant.getColorByElement(Constant.ELEMENT_FEU))
				if (target.mob != null)
					checkMonsters(fight, target, 99)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, caster!!)
					if(!target.isTrappedOrGlyphed()) {
						if (target.canPlay() && target.player != null)
							fight.endTurn(false)
						else if (target.canPlay())
							target.setCanPlay(false)
					}
				}
			}
		} else {
			for (target in  targets) {
				target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true);//on applique un buff
			}
		}
	}

	private fun applyEffect_100(targets: ArrayList<Fighter>, fight: Fight, isCaC: Boolean) {
		if (caster!!.isHidden())
			caster!!.unHide(spell)
		if (fight.type == 7)
			return
		if (isCaC)//CaC Neutre
		{
			for (target in  targets) {
					var target = target
				if (caster!!.mob != null && (caster!!.getTeam2() == target.getTeam2())
						&& !caster!!.isInvocation())
					continue; // Les monstres de s'entretuent pas
				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}

				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])

				//Si le sort est boost� par un buff sp�cifique
				for (SE in  caster!!.getBuffsByEffectID(293)) {
					if (SE.value == spell) {
						var add: Int = -1
						try {
							add = (SE.args.split(";")[2]).toInt()
						} catch (e: Exception) {
							log.error("unexpected error", e)
						}
						if (add <= 0)
							continue
						dmg += add
					}
				}
				var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, target, Constant.ELEMENT_NEUTRE, dmg, false, true, spell)

				finalDommage = applyOnHitBuffs(finalDommage, target, caster!!, fight, Constant.ELEMENT_NEUTRE);//S'il y a des buffs sp�ciaux

				if (finalDommage > target.getPdv())
					finalDommage = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, finalDommage)

				this.checkLifeTree(target, finalDommage)
				finalDommage = -(finalDommage)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + finalDommage + "," + Constant.ELEMENT_NEUTRE)
				if (target.mob != null)
					checkMonsters(fight, target, 100)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, caster!!)
					if (target.canPlay() && target.player != null)
						fight.endTurn(false)
					else if (target.canPlay())
						target.setCanPlay(false)
				}
			}
		} else if (turns <= 0) {
			for (target in  targets) {
					var target = target
				if (spell != 946 && caster!!.mob != null && (caster!!.getTeam2() == target.getTeam2()) && !caster!!.isInvocation())
					continue; // Les monstres de s'entretuent pas

				if (target.hasBuff(765))//sacrifice
				{
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}
				//si la cible a le buff renvoie de sort
				if (target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0 && !this.isPoison()) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.id.toString() + "", target.id.toString() + ",1"); // le lanceur devient donc la cible
					target = caster!!
				}

				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])

				//Si le sort est boost� par un buff sp�cifique
				for (SE in  caster!!.getBuffsByEffectID(293)) {
					if (SE.value == spell) {
						var add: Int = -1
						try {
							add = (SE.args.split(";")[2]).toInt()
						} catch (e: Exception) {
							log.error("unexpected error", e)
						}
						if (add <= 0)
							continue
						dmg += add
					}
				}

				var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, target, Constant.ELEMENT_NEUTRE, dmg, false, false, spell)

				finalDommage = applyOnHitBuffs(finalDommage, target, caster!!, fight, Constant.ELEMENT_NEUTRE);//S'il y a des buffs sp�ciaux

				if (finalDommage > target.getPdv())
					finalDommage = target.getPdv();//Target va mourrir
				target.removePdv(caster!!, finalDommage)
				this.checkLifeTree(target, finalDommage)
				finalDommage = -(finalDommage)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + finalDommage + "," + Constant.ELEMENT_NEUTRE)
				if (target.mob != null)
					checkMonsters(fight, target, 100)
				if (target.getPdv() <= 0) {
					fight.onFighterDie(target, caster!!)
					if(!target.isTrappedOrGlyphed()) {
						if (target.canPlay() && target.player != null)
							fight.endTurn(false)
						else if (target.canPlay())
							target.setCanPlay(false)
					}
				}
			}
		} else {
			for (target in  targets) {
				target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true);//on applique un buff
			}
		}
	}

	private fun applyEffect_101(targets: ArrayList<Fighter>, fight: Fight) {
		for (target in  targets) {
					var target = target
			if (target.hasBuff(788)) {
				var buff: SpellEffect? = target.getBuff(788)
				if (buff != null && buff.value == 101) {
					target.addBuff(111, value, buff.turns, true, buff.spell, args, target, false, true)
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, (target.id).toString(), target.id.toString() + ",+" + value)
				}
			}

			if (target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0 && !this.isPoison()) {
				// The caster become the target
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, (target.id).toString(), target.id.toString() + ",1")
				target = caster!!
			}

			var value: Int = Formulas.getRandomJet(caster, target, jet)
			var pointsLost: Int = Formulas.getPointsLost('a', value, caster!!, target)

			if ((value - pointsLost) > 0) {
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 308, (caster!!.id).toString(), target.id.toString() + "," + (value - pointsLost))
			}
			if (pointsLost > 0) {
				target.addBuff(effectID, pointsLost, turns, false, spell, args, caster!!, false, true)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, (target.id).toString(), target.id.toString() + ",-" + pointsLost)
			}

			if (fight.getFighterByGameOrder() == target) {
				fight.curFighterPa = fight.curFighterPa - pointsLost
			}

			if (target.mob != null) {
				if (turns <= 0) {
					this.checkMonsters(fight, target, effectID)
				} else if (target.mob!!.template!!.id == 1071) {
					// If "Rasboul" monster
					target.addBuff(111, value, turns, true, spell, args, target, false, true)
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 111, (target.id).toString(), target.id.toString() + ",+" + value)
				}
			}
		}
	}

	private fun applyEffect_105(targets: ArrayList<Fighter>) {
		var vale: Int= Formulas.getRandomJet(caster, null, jet)
		if (vale == -1) return
		for (target in  targets) {
			target.addBuff(effectID, vale, turns, true, spell, args, caster!!, false, true)
		}
	}

	private fun applyEffect_106(targets: ArrayList<Fighter>) {
		var vale: Int= -1
		try {
			vale = (args.split(";")[1]).toInt();//Niveau de sort max
		} catch (e: Exception) {
			log.error("unexpected error", e)
		}
		if (vale == -1)
			return

		for (target in  targets) {
			target.addBuff(effectID, vale, turns, true, spell, args, caster!!, false, true)
		}
	}

	private fun applyEffect_107(targets: ArrayList<Fighter>) {
		if (turns >= 1)
			for (target in  targets)
				target.addBuff(effectID, value, turns, true, spell, args, caster!!, false, true);//on applique un buff
	}

	private fun applyEffect_108(targets: ArrayList<Fighter>, fight: Fight, isCaC: Boolean) {
		if (spell == 441) return
		if (isCaC) return
		if (turns <= 0) {
			var jet: List<String> = args.split(";")
			var heal: Int
			if (jet.size < 6) {
				heal = 1
			} else {
				heal = Formulas.getRandomJet(caster, null, jet[5])
			}
			var heal2: Int = heal
			for (cible in  targets) {
				if (cible.isDead)
					continue
				if(cible.hasBuff(87) && cible.getBuff(87)!!.spell == 1009) {
					var effect: SpellEffect = cible.getBuff(87)!!
					var turns: Int = effect.turns
					effect.turns = 0
					effect.applyEffect_87(ArrayList(listOf(cible)), fight)
					effect.turns = turns
					continue
				}
				if (caster!!.hasBuff(178))
					heal += caster!!.getBuffValue(178)
				if (caster!!.hasBuff(179))
					heal = heal - caster!!.getBuffValue(179)
				heal = getMaxMinSpell(cible, heal)
				var pdvMax: Int = cible.getPdvMax()
				var healFinal: Int = Formulas.calculFinalHealCac(caster!!, heal, false)
				if ((healFinal + cible.getPdv()) > pdvMax)
					healFinal = pdvMax - cible.getPdv()
				if (healFinal < 1)
					healFinal = 0
				cible.removePdv(caster!!, -healFinal)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 108, caster!!.id.toString() + "", cible.id.toString() + "," + healFinal)
				heal = heal2
			}
		} else {
			targets.stream().filter({ target -> !target.isDead }).forEach({ target -> target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true) })
		}
	}

	private fun applyEffect_109(fight: Fight)//Dommage pour le lanceur (fixes)
	{
		if (turns <= 0) {
			var dmg: Int = Formulas.getRandomJet(caster, null, args.split(";")[5])
			var finalDommage: Int = Formulas.calculFinalDommage(fight, caster!!, caster!!, Constant.ELEMENT_NULL, dmg, false, false, spell)

			finalDommage = applyOnHitBuffs(finalDommage, caster!!, caster!!, fight, Constant.ELEMENT_NULL);//S'il y a des buffs sp�ciaux
			if (finalDommage > caster!!.getPdv())
				finalDommage = caster!!.getPdv();//Caster va mourrir
			caster!!.removePdv(caster!!, finalDommage)
			finalDommage = -(finalDommage)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", caster!!.id.toString() + "," + finalDommage + "," + Constant.ELEMENT_NEUTRE)

			if (caster!!.getPdv() <= 0) {
				fight.onFighterDie(caster!!, caster!!)

			}
		} else {
			caster!!.addBuff(effectID, 0, turns, true, spell, args!!, caster!!, false, true);//on applique un buff
		}
	}

	private fun applyEffect_110(targets: ArrayList<Fighter>, fight: Fight) {
		var vale: Int= Formulas.getRandomJet(caster, null, jet)
		if (vale == -1) return
		for (target in  targets) {
			target.addBuff(effectID, vale, turns, true, spell, args, caster!!, false, true)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster!!.id.toString() + "", target.id.toString() + "," + vale + "," + turns)
		}
	}

	private fun applyEffect_111(targets: ArrayList<Fighter>, fight: Fight) {
		var vale: Int= Formulas.getRandomJet(caster, null, jet)
		if (vale == -1) return
		var repetibles: Boolean = false
		var lostPA: Int = 0
		for (target in  targets) {
			if (spell == 115) {// odorat
				if (!repetibles) {
					lostPA = Formulas.getRandomJet(caster, target, jet)
					if (lostPA == -1)
						continue
					value = lostPA
				}
				target.addBuff(effectID, value, turns, true, spell, args, caster!!, false, true)
				repetibles = true
				if (target.canPlay() && target == caster)
					target.setCurPa(fight, value)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster!!.id.toString() + "", target.id.toString() + "," + value + "," + turns)
				continue
			}

			if (spell == 521)// ruse kistoune
				if (target.getTeam2() != caster!!.getTeam2())
					continue
			target.addBuff(effectID, vale, turns, true, spell, args, caster!!, false, true)
			//Gain de PA pendant le tour de jeu
			if (target.canPlay() && target == caster)
				target.setCurPa(fight, vale)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster!!.id.toString() + "", target.id.toString() + "," + vale + "," + turns)
		}
	}

	private fun applyEffect_112(targets: ArrayList<Fighter>, fight: Fight) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if(value == -1) return

		if (spell == 1090) {
			caster!!.addBuff(effectID, value, turns, true, spell, args!!, caster!!, false, true)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster!!.id.toString() + "", caster!!.id.toString() + "," + value + "," + turns)
			return
		}

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_114(targets: ArrayList<Fighter>, fight: Fight) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, false, true)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, (caster!!.id).toString(), target.id.toString() + "," + value + "," + turns)
		}
	}

	private fun applyEffect_115(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_116(targets: ArrayList<Fighter>) {//Malus PO
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_117(targets: ArrayList<Fighter>) {//Bonus PO

		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
			//Gain de PO pendant le tour de jeu
			if (target.canPlay() && target == caster) {
				target.getTotalStats().addOneStat(Constant.STATS_ADD_PO, value)
			}
		}
	}

	private fun applyEffect_118(targets: ArrayList<Fighter>, fight: Fight) {//Bonus Force
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		var targets = targets
		if (value == -1) return

		if (spell == 52) {//cupiditer
			targets = fight.getFighters(3)
		}

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_119(targets: ArrayList<Fighter>) {//Bonus Agilit�
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_120(fight: Fight)//Bonus PA
	{
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		caster!!.addBuff(effectID, value, turns, true, spell, args!!, caster!!, true, true)
		caster!!.setCurPa(fight, value)
	}

	private fun applyEffect_121(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_122(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_123(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_124(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_125(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_126(targets: ArrayList<Fighter>, fight: Fight) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		var targets = targets
		if (value == -1) return

		if (spell == 52) { // Cupiditer
			targets = fight.getFighters(3)
		}

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_127(targets: ArrayList<Fighter>, fight: Fight) {
		for (target in  targets) {
			var value: Int = Formulas.getRandomJet(caster, target, jet)
			var pointsLost: Int = Formulas.getPointsLost('m', value, caster!!, target)

			if ((value - pointsLost) > 0) {
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 309, (caster!!.id).toString(), target.id.toString() + "," + (value - pointsLost))
			}

			if (pointsLost > 0) {
				target.addBuff(effectID, pointsLost, turns, false, spell, args, caster!!, false, true)

				if (turns <= 1) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, (target.id).toString(), target.id.toString() + ",-" + pointsLost)
				}
				if (target.mob != null) {
					this.checkMonsters(fight, target, effectID)
				}
			}
		}
	}

	private fun applyEffect_128(targets: ArrayList<Fighter>, fight: Fight) {
		var vale: Int= Formulas.getRandomJet(caster, null, jet)
		if (vale == -1) return
		var repetibles: Boolean = false
		var lostPM: Int = 0
		for (target in  targets) {
			if (spell == 115) {// odorat
				if (!repetibles) {
					lostPM = Formulas.getRandomJet(caster, target, jet)
					if (lostPM == -1)
						continue
					value = lostPM
				}
				target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
				repetibles = true
				if (target.canPlay() && target == caster)
					target.setCurPm(fight, value)
				continue
			} else if (spell == 521)// ruse kistoune
				if (target.getTeam2() != caster!!.getTeam2())
					continue

			target.addBuff(effectID, vale, turns, true, spell, args, caster!!, true, true)
			//Gain de PM pendant le tour de jeu
			if (target.canPlay() && target == caster)
				target.setCurPm(fight, vale)
		}
	}

	private fun applyEffect_130(fight: Fight, targets: ArrayList<Fighter>) {
		if (turns <= 0) {
			for (target in  targets) {
				var kamas: Long = Formulas.getRandomJet(caster, target, args.split(";")[5]).toLong()
				if (caster!!.player == null) break
				if (fight.type != Constant.FIGHT_TYPE_CHALLENGE && target.player != null) {
					target.player!!.addKamas(-kamas)
					if (target.player!!.kamas < 0)
						target.player!!.kamas = 0
				} else {
					kamas = 0
				}
				if (target.mob != null && target.mob!!.template!!.id != 494) {
					caster!!.player!!.addKamas(kamas.toLong())
				} else {
					caster!!.player!!.addKamas(kamas.toLong())
				}

				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 130, caster!!.id.toString() + "", kamas.toString() + "")
			}
		} else {
			for (target in  targets)
				target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true);//on applique un buff
		}
	}

	private fun applyEffect_131(targets: ArrayList<Fighter>) {
		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, false, true)
		}
	}

	private fun applyEffect_132(targets: ArrayList<Fighter>, fight: Fight) {
		for (target in  targets) {
			if (target.isHidden())
				target.unHide(spell)
			target.debuff(this)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 132, (caster!!.id).toString(), (target.id).toString())
		}
	}

	private fun applyEffect_138(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_140(targets: ArrayList<Fighter>) {
		for (target in  targets) {
			target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true)
		}
	}

	private fun applyEffect_141(fight: Fight, targets: ArrayList<Fighter>) {
		for (target in  targets) {
					var target = target
			var monster: Monster? = if (target.mob != null) target.mob!!.template else null
			if(monster != null && monster.id == 1088)
				continue
			if (target.hasBuff(765)) { // Sacrifice
				if (target.getBuff(765) != null && !target.getBuff(765)!!.caster!!.isDead) {
					applyEffect_765B(fight, target)
					target = target.getBuff(765)!!.caster!!
				}
			}

			target.setIsDead(true)
			fight.onFighterDie(target, caster!!)
		}
	}

	private fun applyEffect_142(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		if(this.cell!!.firstFighter == this.caster && !targets.contains(caster)) {
			targets.add(this.caster!!)
		}
		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_143(targets: ArrayList<Fighter>, fight: Fight) {
		if (spell == 470) {
			var jet: List<String> = args.split(";")
			var heal: Int = 0
			if (jet.size < 6) {
				heal = 1
			} else {
				heal = Formulas.getRandomJet(caster, null, jet[5])
			}
			var dmg2: Int = heal
			for (cible in  targets) {
				if (cible.team != caster!!.team)
					continue
				if (cible.isDead)
					continue
				heal = getMaxMinSpell(cible, heal)
				var healFinal: Int = Formulas.calculFinalHealCac(caster!!, heal, false)
				if ((healFinal + cible.getPdv()) > cible.getPdvMax())
					healFinal = cible.getPdvMax() - cible.getPdv()
				if (healFinal < 1)
					healFinal = 0
				cible.removePdv(caster!!, -healFinal)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 108, caster!!.id.toString() + "", cible.id.toString() + "," + healFinal)
				heal = dmg2
			}
			return
		}
		if (turns <= 0) {
			var jet: List<String> = args.split(";")
			var heal: Int = 0
			if (jet.size < 6) {
				heal = 1
			} else {
				heal = Formulas.getRandomJet(caster, null, jet[5])
			}
			var dmg2: Int = heal
			for (cible in  targets) {
				if (cible.isDead)
					continue
				if(cible.hasBuff(87) && cible.getBuff(87)!!.spell == 1009) {
					var effect: SpellEffect = cible.getBuff(87)!!
					var turns: Int = effect.turns
					effect.turns = 0
					effect.applyEffect_87(ArrayList(listOf(cible)), fight)
					effect.turns = turns
					continue
				}
				heal = getMaxMinSpell(cible, heal)
				var healFinal: Int = Formulas.calculFinalHealCac(caster!!, heal, false)
				if (spell == 450) {
					healFinal = heal
				}
				if ((healFinal + cible.getPdv()) > cible.getPdvMax())
					healFinal = cible.getPdvMax() - cible.getPdv()
				if (healFinal < 1)
					healFinal = 0
				cible.removePdv(caster!!, -healFinal)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 108, caster!!.id.toString() + "", cible.id.toString() + "," + healFinal)
				heal = dmg2
			}
		} else {
			for (cible in  targets) {
				if (cible.isDead)
					continue
				cible.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true)
			}
		}
	}

	private fun applyEffect_144(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		val temp: Int = value
		for (target in  targets) {
			value = getMaxMinSpell(target, value)
			target.addBuff(145, value, turns, true, spell, args, caster!!, true, true)
			value = temp
		}
	}

	private fun applyEffect_145(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_149(fight: Fight, targets: ArrayList<Fighter>) {
		var id: Int = -1

		try {
			id = (args.split(";")[2]).toInt()
		} catch (e: Exception) {
			log.error("unexpected error", e)
		}

		for (target in  targets) {
			if (target.isDead)
				continue
			if (spell == 686)
				if (target.player != null
						&& target.player!!.sexe == 1
						|| target.mob != null
						&& target.mob!!.template!!.id == 547)
					id = 8011
			if (id == -1)
				id = target.getDefaultGfx()

			target.addBuff(effectID, id, turns, true, spell, args, caster!!, false, true)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster!!.id.toString() + "", target.id.toString() + "," + target.getDefaultGfx() + "," + id + "," + (if (target.canPlay()) turns + 1 else turns))
		}
	}

	private fun applyEffect_150(fight: Fight, targets: ArrayList<Fighter>) {
		if(caster!!.getIsHolding() != null || caster!!.getHoldedBy() != null)
			return
		if (turns == 0)
			return

		if (spell == 547 || spell == 546 || spell == 548 || spell == 525) {
			caster!!.addBuff(effectID, 0, 3, true, spell, args!!, caster!!, false, true)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster!!.id.toString() + "", caster!!.id.toString() + "," + (3 - 1))
			return
		}

		for (target in  targets) {
			if(target.getIsHolding() != null || target.getHoldedBy() != null || target.isHidden())
				continue
			target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster!!.id.toString() + "", target.id.toString() + "," + (turns - 1))
		}
	}

	private fun applyEffect_152(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1)
			return
		var val2: Int = value
		for (target in  targets) {
			value = getMaxMinSpell(target, value)
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
			value = val2
		}
	}

	private fun applyEffect_153(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		val temp: Int = value
		for (target in  targets) {
			value = getMaxMinSpell(target, value)
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
			value = temp
		}
	}

	private fun applyEffect_154(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		var val2: Int = value
		for (target in  targets) {
			value = getMaxMinSpell(target, value)
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
			value = val2
		}
	}

	private fun applyEffect_155(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_156(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		var temp: Int = value
		for (target in  targets) {
			value = getMaxMinSpell(target, value)
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
			value = temp
		}
	}

	private fun applyEffect_157(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		val temp: Int = value
		for (target in  targets) {
			value = getMaxMinSpell(target, value)
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
			value = temp
		}
	}

	private fun applyEffect_160(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_161(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_162(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_163(fight: Fight, targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		if (targets.isEmpty() && spell == 310 && caster!!.getOldCible() != null) {
			var effect: SpellEffect = caster!!.getOldCible()!!.addBuff(effectID, value, turns, true, spell, args!!, caster!!, false, true)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, (caster!!.getOldCible()!!.id).toString(), caster!!.getOldCible()!!.id.toString() + "," + value + "," + effect.turns)
		}
		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_164(targets: ArrayList<Fighter>) {
		if (value == -1)
			return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, false, true)
		}
	}

	private fun applyEffect_165() {
		var value: Int = -1
		try {
			value = (args.split(";")[1]).toInt()
		} catch (e: Exception) {
			log.error("unexpected error", e)
		}
		if (value == -1)
			return
		caster!!.addBuff(effectID, value, turns, true, spell, args!!, caster!!, false, true)
	}

	private fun applyEffect_168(targets: ArrayList<Fighter>, fight: Fight) {// - PA, no esquivables
		var lostPA: Int
		var repetibles: Boolean = false

		for (target in  targets) {
					var target = target
			if (target.isDead)
				continue
			if (target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0 && !this.isPoison()) {
				// The caster become the target
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.id.toString() + "", target.id.toString() + ",1")
				target = caster!!
			}

			if (spell == 115) {// Odorat
				if (!repetibles) {
					lostPA = Formulas.getRandomJet(caster, target, jet)
					if (lostPA == -1)
						continue
					value = lostPA
				}
				repetibles = true
			}

			target.addBuff(effectID, value, turns, true, spell, args, caster!!, false, true)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, target.id.toString() + "", target.id.toString() + ",-" + value)

			if (fight.getFighterByGameOrder() == target) {
				fight.curFighterPa = fight.curFighterPa - value
			}
			if (target.mob != null) {
				checkMonsters(fight, target, effectID)
			}
		}
	}


	private fun applyEffect_169(targets: ArrayList<Fighter>, fight: Fight) { // - PM, no esquivables
		if (spell == 686 && caster!!.haveState(Constant.ETAT_SAOUL)) // anti bug saoul
			return

		if (targets.isEmpty() && spell == 120 && caster!!.getOldCible() != null) {
			caster!!.getOldCible()!!.addBuff(effectID, value, turns, false, spell, args!!, caster!!, false, true)
			if (turns <= 1) {
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, (caster!!.getOldCible()!!.id).toString(), caster!!.getOldCible()!!.id.toString() + ",-" + value)
			}
		}

		var lostPM: Int
		var repetibles: Boolean = false

		for (target in  targets) {
			if (target.isDead)
				continue
			if (spell == 115) { // Odorat
				if (!repetibles) {
					lostPM = Formulas.getRandomJet(caster, target, jet)
					if (lostPM == -1) continue
					value = lostPM
				}
				repetibles = true
			}

			target.addBuff(effectID, value, turns, true, spell, args, caster!!, false, true)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, (target.id).toString(), target.id.toString() + ",-" + value)

			if (fight.getFighterByGameOrder() == target) {
				fight.curFighterPm = fight.curFighterPm - value
			}
			if (target.mob != null) {
				checkMonsters(fight, target, effectID)
			}
		}
	}

	private fun applyEffect_171(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_176(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, false, true)
		}
	}

	private fun applyEffect_177(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_178(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return
		
		val temp: Int = value
		for (target in  targets) {
			value = getMaxMinSpell(target, value)
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
			value = temp
		}
	}

	private fun applyEffect_179(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		val temp: Int = value
		for (target in  targets) {
			value = getMaxMinSpell(target, value)
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
			value = temp
		}
	}

	private fun applyEffect_180(fight: Fight)// Summon clone
	{
		var cell: Int = this.cell!!.getId()
		if (!this.cell!!.fighters.isEmpty())
			return
		if(!(caster is PlayerFighter))
			return

		var id: Int = fight.getNextLowerFighterGuid()

		var fighter: Fighter = Fighter.NewClone(-id - 10000, fight, (caster as PlayerFighter))
		fighter.fullPdv()
		fighter.team = (caster!!.team)

		fight.map!!.getCase(cell)!!.addFighter(fighter)
		fighter.cell = (fight.map!!.getCase(cell))

		fight.orderPlaying!!.add((fight.orderPlaying!!.indexOf(caster!!) + 1), fighter)
		fight.addFighterInTeam(fighter, caster!!.team)

		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 180, caster!!.id.toString() + "", fighter.getGmPacket('+', true).substring(3))
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 999, caster!!.id.toString() + "", fight.getGTL())

		fight.checkTraps(fighter)
	}

	private fun applyEffect_181(fight: Fight)//invocation
	{
		var cell: Int = this.cell!!.getId()

		if (!this.cell!!.fighters.isEmpty() || caster!!.getNbrInvoc() >= caster!!.getTotalStats()[Constant.STATS_SUMMON_COUNT])
			return

		var id: Int = -1
		var level: Int = -1

		try {
			var mobs: String = args.split(";")[0]
			var levels: String = args.split(";")[1]

			if (mobs.contains(":")) {
				var split: List<String> = mobs.split(":")
				id = (split[Formulas.getRandomValue(0, split.size - 1)]).toInt()
			} else {
				id = (mobs).toInt()
			}

			if (levels.contains(":")) {
				var split: List<String> = levels.split(":")
				level = (split[Formulas.getRandomValue(0, split.size - 1)]).toInt()
			} else {
				level = (levels).toInt()
			}
		} catch (e: Exception) {
			log.error("unexpected error", e)
		}

var MG: MonsterGrade? = null
		var monster: Monster? = World.world.getMonstre(id)
		if(monster == null) return
		try {
			MG = monster.getGradeByLevel(level)
			if(MG == null) {
				MG = monster.getRandomGrade()
				if(MG == null) return
			}
			MG = MG.getCopy()
		} catch (e1: Exception) {
			log.error("unexpected error", e1)
		}

		if (id == -1 || level == -1 || MG == null)
			return

		var F: Fighter = Fighter.NewSummon(fight.getNextLowerFighterGuid(), fight, MG, caster!!)
		F.team = (caster!!.team)
		F.setInvocator(caster)
		fight.map!!.getCase(cell)!!.addFighter(F)
		F.cell = (fight.map!!.getCase(cell))
		fight.orderPlaying!!.add((fight.orderPlaying!!.indexOf(caster!!) + 1), F)
		fight.addFighterInTeam(F, caster!!.team)
		var gm: String = F.getGmPacket('+', true).substring(3)
		var gtl: String = fight.getGTL()

		TimerWaiter.addNext({  -> {
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 181, caster!!.id.toString() + "", gm)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 999, caster!!.id.toString() + "", gtl)
			caster!!.modNbrInvoc(+1)
			fight.checkTraps(F)
		} }, 100, TimeUnit.MILLISECONDS)
	}

	private fun applyEffect_182(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return
		
		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_183(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return
		
		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_184(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return
		
		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_185(fight: Fight) {
		var monster: Int = -1
		var level: Int = -1

		try {
			monster = (args.split(";")[0]).toInt()
			level = (args.split(";")[1]).toInt()
		} catch (e: Exception) {
			log.error("unexpected error", e)
		}

		lateinit var monsterGrade: MonsterGrade

		try {
			monsterGrade = World.world.getMonstre(monster)!!.getGradeByLevel(level!!)!!.getCopy()
		} catch (e: Exception) {
			log.error("unexpected error", e)
			return
		}

		if (monster == -1 || level == -1 || monsterGrade == null)
			return

		var id: Int = fight.getNextLowerFighterGuid()

		var fighter: Fighter = Fighter.NewSummon(id, fight, monsterGrade, caster!!)

		fighter.team = (this.caster!!.team)
		fighter.setInvocator(this.caster)
		fighter.setState(Constant.ETAT_ENRACINE, 9999)

		fight.map!!.getCase(this.cell!!.getId())!!.addFighter(fighter)
		fighter.cell = (fight.map!!.getCase(this.cell!!.getId()))
		fight.addFighterInTeam(fighter, this.caster!!.team)
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 185, this.caster!!.id.toString() + "", fighter.getGmPacket('+', true).substring(3))
	}

	private fun applyEffect_186(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return
		
		for (target in  targets) {
			value = getMaxMinSpell(target, value)
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_202(fight: Fight, targets: ArrayList<Fighter>) {
		if (spell == 113 || spell == 64) {
			for (target in  targets) {
				if (target.isHidden() && target != caster) {
					target.unHide(spell)
				}
			}
	
			for (trap in  fight.traps) {
				trap.setIsUnHide(caster!!)
				trap.appear(caster!!)
			}
		}
	}

	private fun applyEffect_210(fight: Fight, targets: ArrayList<Fighter>) {
		if (spell == 686 && caster!!.haveState(1))//anti bug saoul
		{
			var pa: Int = 1
			if (this.spellLvl == 5)
				pa = 2
			else if (this.spellLvl == 4)
				pa = 3
			else if (this.spellLvl == 3 || this.spellLvl == 2)
				pa = 4
			else if (this.spellLvl == 1)
				pa = 5

			caster!!.addBuff(111, pa, -1, true, spell, args!!, caster!!, false, true)
			//Gain de PA pendant le tour de jeu
			caster!!.setCurPa(fight, pa)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 111, caster!!.id.toString() + "", caster!!.id.toString() + "," + pa + "," + -1)
			return
		}
		
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return
		
		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_211(targets: ArrayList<Fighter>) {
		if (spell == 686 && caster!!.haveState(1))//anti bug saoul
			return
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return
		
		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_212(targets: ArrayList<Fighter>) {
		if (spell == 686 && caster!!.haveState(1))//anti bug saoul
			return
		
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return
		
		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_213(targets: ArrayList<Fighter>) {
		if (spell == 686 && caster!!.haveState(1))//anti bug saoul
			return
		
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return
		
		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_214(targets: ArrayList<Fighter>) {
		if (spell == 686 && caster!!.haveState(1))//anti bug saoul
			return
		
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return
		
		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_215(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return
		
		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_216(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return
		
		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_217(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return
		
		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_218(targets: ArrayList<Fighter>) {
		var vale: Int= Formulas.getRandomJet(caster, null, jet)
		if (vale == -1) return
		
		for (target in  targets) {
			target.addBuff(effectID, vale, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_219(targets: ArrayList<Fighter>) {
		var vale: Int= Formulas.getRandomJet(caster, null, jet)
		if (vale == -1) return

		for (target in  targets) {
			target.addBuff(effectID, vale, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_220(targets: ArrayList<Fighter>) {
		if (turns < 1) return

		for (target in  targets) {
			target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true)
		}
	}

	private fun applyEffect_265(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_266(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		var steal: Int = 0

		for (target in  targets) {
			target.addBuff(Constant.STATS_REM_CHAN, value, turns, true, spell, args, caster!!, true, true)
			steal += value
		}

		if (steal == 0) return
		caster!!.addBuff(Constant.STATS_ADD_CHAN, steal, turns, true, spell, args!!, caster!!, true, true)
	}

	private fun applyEffect_267(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		var steal: Int = 0

		for (target in  targets) {
			target.addBuff(Constant.STATS_REM_VITA, value, turns, true, spell, args, caster!!, true, true)
			steal += value
		}

		if (steal == 0) return
		caster!!.addBuff(Constant.STATS_ADD_VITA, steal, turns, true, spell, args!!, caster!!, true, true)
	}

	private fun applyEffect_268(targets: ArrayList<Fighter>) {
		var vale: Int= Formulas.getRandomJet(caster, null, jet)
		var steal: Int = 0

		for (target in  targets) {
			target.addBuff(Constant.STATS_REM_AGIL, vale, turns, true, spell, args, caster!!, true, true)
			steal += vale
		}

		if (steal == 0) return

		caster!!.addBuff(Constant.STATS_ADD_AGIL, steal, turns, true, spell, args!!, caster!!, true, true)
	}

	private fun applyEffect_269(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		var steal: Int = 0

		for (target in  targets) {
			target.addBuff(Constant.STATS_REM_INTE, value, turns, true, spell, args, caster!!, true, true)
			steal += value
		}

		if (steal == 0) return
		caster!!.addBuff(Constant.STATS_ADD_INTE, steal, turns, true, spell, args!!, caster!!, true, true)
	}

	private fun applyEffect_270(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		var steal: Int = 0

		for (target in  targets) {
			target.addBuff(Constant.STATS_REM_SAGE, value, turns, true, spell, args, caster!!, true, true)
			steal += value
		}

		if (steal == 0) return
		caster!!.addBuff(Constant.STATS_ADD_SAGE, steal, turns, true, spell, args!!, caster!!, true, true)
	}

	private fun applyEffect_271(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		var steal: Int = 0

		for (target in  targets) {
			target.addBuff(Constant.STATS_REM_FORC, value, turns, true, spell, args, caster!!, true, true)
			steal += value
		}

		if (steal == 0) return
		caster!!.addBuff(Constant.STATS_ADD_FORC, steal, turns, true, spell, args!!, caster!!, true, true)
	}

	private fun applyEffect_293() {
		caster!!.addBuff(effectID, value, turns, false, spell, args!!, caster!!, false, true)
		caster!!.setState(300, turns + 1)
	}

	private fun applyEffect_320(targets: ArrayList<Fighter>) {
		var total: Int = 0

		for (target in  targets) {
			target.addBuff(Constant.STATS_REM_PO, value, turns, true, spell, args, caster!!, true, true)
			total += value
		}

		if (total != 0) {
			caster!!.addBuff(Constant.STATS_ADD_PO, total, turns, true, spell, args!!, caster!!, true, true)

			if (caster!!.canPlay()) {
				caster!!.getTotalStats().addOneStat(Constant.STATS_ADD_PO, total)
			}
		}
	}

	private fun applyEffect_400(fight: Fight) {
		if (!cell!!.isWalkableFight() && cell!!.firstFighter != null && !cell!!.firstFighter!!.isHidden())
			return

		for (trap in  fight.traps) {
			if (trap.cell.getId() == cell!!.getId()) {
				return
			}
		}

		var infos: List<String> = args.split(";")
		var spellID: Int = (infos[0]).toInt()
		var level: Int = (infos[1]).toInt()
		var po: String = World.world.getSort(spell)!!.getStatsByLevel(spellLvl)!!.porteeType
		var size: Byte = CryptManager.getIntByHashedValue(po[1]).toByte()
		var TS: SortStats = World.world.getSort(spellID)!!.getStatsByLevel(level)!!
		var g: Trap = Trap(fight, caster!!, cell!!, size, TS, spell)
		fight.traps.add(g)
		var unk: Int = g.color
		var team: Int = caster!!.team + 1

		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, team, 999, (caster!!.id).toString(), "GDZ+" + cell!!.getId().toString() + ";" + size + ";" + unk)
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, team, 999, (caster!!.id).toString(), "GDC" + cell!!.getId().toString() + ";Haaaaaaaaz3005;")
	}

	private fun applyEffect_401(fight: Fight) {
		if (!cell!!.isWalkableFight() && cell!!.firstFighter != null)
			return

		var infos: List<String> = args.split(";")
		var spellID: Int = (infos[0]).toInt()
		var level: Int = (infos[1]).toInt()
		var duration: Byte = (infos[3]).toByte()
		var po: String = World.world.getSort(spell)!!.getStatsByLevel(spellLvl)!!.porteeType
		var size: Byte = CryptManager.getIntByHashedValue(po[1]).toByte()
		var TS: SortStats = World.world.getSort(spellID)!!.getStatsByLevel(level)!!
		var g: Glyph = Glyph(fight, caster!!, cell!!, size, TS, duration, spell)
		fight.glyphs.add(g)
		var unk: Int = g.color

		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 999, (caster!!.id).toString(), "GDZ+" + cell!!.getId().toString() + ";" + size + ";" + unk)
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 999, (caster!!.id).toString(), "GDC" + cell!!.getId().toString() + ";Haaaaaaaaa3005;")
	}

	private fun applyEffect_402(fight: Fight) {
		if (!cell!!.isWalkableFight())
			return

		var infos: List<String> = args.split(";")
		var spellID: Int = (infos[0]).toInt()
		var level: Int = (infos[1]).toInt()
		var duration: Byte = (infos[3]).toByte()
		var po: String = World.world.getSort(spell)!!.getStatsByLevel(spellLvl)!!.porteeType
		var size: Byte = CryptManager.getIntByHashedValue(po[1]).toByte()
		var TS: SortStats = World.world.getSort(spellID)!!.getStatsByLevel(level)!!
		var g: Glyph = Glyph(fight, caster!!, cell!!, size, TS, duration, spell)
		fight.glyphs.add(g)
		var unk: Int = g.color

		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 999, (caster!!.id).toString(), "GDZ+" + cell!!.getId().toString() + ";" + size + ";" + unk)
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 999, (caster!!.id).toString(), "GDC" + cell!!.getId().toString() + ";Haaaaaaaaa3005;")
	}

	private fun applyEffect_671(targets: ArrayList<Fighter>, fight: Fight) {
		if (turns <= 0) {
			for(target in  targets) {
				var target = target
				if (target.hasBuff(765)) {
					if (target.getBuff(765) != null
							&& !target.getBuff(765)!!.caster!!.isDead) {
						applyEffect_765B(fight, target)
						target = target.getBuff(765)!!.caster!!
					}
				}
				if (target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0 && !this.isPoison()) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.id.toString() + "", target.id.toString() + ",1")
					target = caster!!
				}
				var resP: Int = target.getTotalStats().getEffect(Constant.STATS_ADD_RP_NEU)
				var resF: Int = target.getTotalStats().getEffect(Constant.STATS_ADD_R_NEU)

				if (target.player != null) {
					resP += target.getTotalStats().getEffect(Constant.STATS_ADD_RP_PVP_NEU)
					resF += target.getTotalStats().getEffect(Constant.STATS_ADD_R_PVP_NEU)
				}

				var dmg: Int = Formulas.getRandomJet(caster, target, args.split(";")[5])// % de pdv
				dmg = getMaxMinSpell(target, dmg)
				var vale: Int = caster!!.getPdv() / 100 * dmg// Valor de da�os
				vale -= resF
				var reduc: Int = ((((vale.toFloat())) / (100 as Float)).toInt()) * resP// Reduc
				// %resis
				vale -= reduc
				if (vale < 0)
					vale = 0
				vale = applyOnHitBuffs(vale, target, caster!!, fight, Constant.ELEMENT_NULL)
				if (vale > target.getPdv())
					vale = target.getPdv()
				target.removePdv(caster!!, vale)

				this.checkLifeTree(target, vale)
				vale = -(vale)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + vale + "," + Constant.ELEMENT_NEUTRE)
			}
		} else {
			caster!!.addBuff(effectID, 0, turns, true, spell, args!!, caster!!, false, true)
		}
	}

	private fun applyEffect_672(targets: ArrayList<Fighter>, fight: Fight) {
		//Punition
		//Formule de barge ? :/ Clair que ca punie ceux qui veulent l'utiliser x_x
		var vale: Double= ((Formulas.getRandomJet(caster, null, jet).toDouble()) / (100 as Double))
		var pdvMax: Int = caster!!.baseMaxPdv()
		var pVie: Double = (caster!!.getPdv().toDouble()) / (caster!!.getPdvMax().toDouble())
		var rad: Double = (2 as Double) * Math.PI * ((pVie - 0.5).toDouble())
		var cos: Double = Math.cos(rad)
		var taux: Double = (Math.pow((cos + 1).toDouble(), 2.0)) / 4.0
		var dgtMax: Double = vale * pdvMax
		var dgt: Int = ((taux * dgtMax).toInt())

		for (target in  targets) {
					var target = target

			if (target.hasBuff(765))//sacrifice
			{
				if (target.getBuff(765) != null
						&& !target.getBuff(765)!!.caster!!.isDead) {
					applyEffect_765B(fight, target)
					target = target.getBuff(765)!!.caster!!
				}
			}
			//si la cible a le buff renvoie de sort
			if (target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && !this.isPoison()) {
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.id.toString() + "", target.id.toString() + ",1")
				//le lanceur devient donc la cible
				target = caster!!
			}

			var dmg: Int = applyOnHitBuffs(dgt, target, caster!!, fight, Constant.ELEMENT_NEUTRE)//S'il y a des buffs sp�ciaux
			var neutralResistance: Int = target.getTotalStats().getEffect(Constant.STATS_ADD_RP_NEU)
			var rem: Int? = null
			if (neutralResistance > 2) {
				rem = (dmg * neutralResistance) / 100
				dmg = dmg - rem
			}
			if (neutralResistance < -2) {
				rem = ((-dmg) * (-neutralResistance)) / 100
				dmg = dmg + rem
			}
			if (target.hasBuff(105) && !(this.spell == 66 || spell == 71 ||spell == 196 || spell == 213 || spell == 181 || spell == 200)) {
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster!!.id.toString() + "", target.id.toString() + "," + target.getBuff(105)!!.value)
				var value: Int = dmg - target.getBuff(105)!!.value
				dmg = Math.max(value, 0)
			}

			if (dmg > target.getPdv())
				dmg = target.getPdv();//Target va mourrir
			target.removePdv(caster!!, dmg)
			dmg = -(dmg)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster!!.id.toString() + "", target.id.toString() + "," + dmg + "," + Constant.ELEMENT_NEUTRE)

			if (target.getPdv() <= 0) {
				fight.onFighterDie(target, target)
				if (target.canPlay() && target.player != null)
					fight.endTurn(false)
				else if (target.canPlay())
					target.setCanPlay(false)
			}
		}
	}

	private fun applyEffect_765(targets: ArrayList<Fighter>) {
		for (target in  targets) {
			target.addBuff(effectID, 0, turns, true, spell, args, caster!!, false, true)
		}
	}

	private fun applyEffect_765B(fight: Fight, target: Fighter) {
		var sacrified: Fighter = target.getBuff(765)!!.caster!!

		if(sacrified.getIsHolding() != null || sacrified.getHoldedBy() != null
				|| target.getHoldedBy() != null || target.getIsHolding() != null)
			return

		var cell1: GameCase = sacrified.cell!!
		var cell2: GameCase = target.cell!!

		sacrified.cell!!.removeFighter(sacrified)
		target.cell!!.removeFighter(target)
		sacrified.cell = (cell2)
		sacrified.cell!!.addFighter(sacrified)
		target.cell = (cell1)
		target.cell!!.addFighter(target)
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 4, target.id.toString() + "", target.id.toString() + "," + cell1.getId())
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 4, sacrified.id.toString() + "", sacrified.id.toString() + "," + cell2.getId())
	}

	private fun applyEffect_776(targets: ArrayList<Fighter>) {
		var value: Int = Formulas.getRandomJet(caster, null, jet)
		if (value == -1) return

		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, true, true)
		}
	}

	private fun applyEffect_780(fight: Fight) {
		var target: Fighter? = null
		var targetPlayer: Fighter? = null

		for (fighter in  fight.deadList) {
			if (!fighter.hasLeft() && fighter.team == caster!!.team) {
				if(fighter.player != null)
					targetPlayer = fighter
				target = fighter
			}
		}

		if(targetPlayer != null)
			target = targetPlayer
		if (target == null)
			return

		target.setInvocator(caster)
		fight.addFighterInTeam(target, target.team)
		target.setIsDead(false)
		target.getFightBuff().clear()
		if(fight.orderPlaying!!.contains(target)) {
			fight.orderPlaying!!.remove(target)
			fight.orderPlaying!!.add((fight.orderPlaying!!.indexOf(target.getInvocator()) + 1), target)
			fight.curPlayer = fight.orderPlaying!!.indexOf(caster)
		} else if (target.isInvocation())
			fight.orderPlaying!!.add((fight.orderPlaying!!.indexOf(target.getInvocator()) + 1), target)

		target.cell = (cell)
		target.cell!!.addFighter(target)

		target.fullPdv()
		var percent: Int = (100 - value) * target.getPdvMax() / 100
		target.removePdv(caster!!, percent)

		var gm: String = target.getGmPacket('+', true).substring(3)
		var gtl: String = fight.getGTL()
		SocketManager.GAME_SEND_GIC_PACKETS_TO_FIGHT(fight, 7)
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 780, target.id.toString() + "", gm)
		SocketManager.GAME_SEND_GTL_PACKET_TO_FIGHT(fight, 7)
		SocketManager.GAME_SEND_GTM_PACKET_TO_FIGHT(fight, 7)
		//SocketManager.GAME_SEND_FIGHT_PLAYER_JOIN(fight, 7, target);

		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 999, target.id.toString() + "", gtl)
		if (!target.isInvocation())
			SocketManager.GAME_SEND_STATS_PACKET(target.player!!)

		fight.removeDead(target)
	}

	private fun applyEffect_781(targets: ArrayList<Fighter>) {
		for (target in  targets) {
			target.addBuff(effectID, value, turns, debuffable, spell, args, caster!!, false, true)
		}
	}

	private fun applyEffect_782(targets: ArrayList<Fighter>) {
		if(targets.size > 1) {
			targets.remove(this.caster)
		}
		for (target in  targets) {
			target.addBuff(effectID, value, turns, debuffable, spell, args, caster!!, false, true)
		}
	}

	private fun applyEffect_783(fight: Fight) {
		var casterCell: GameCase = caster!!.cell!!
		var dir: Char = PathFinding.getDirBetweenTwoCase(casterCell.getId(), cell!!.getId(), fight.map, true)
		var targetCellId: Int = PathFinding.GetCaseIDFromDirection(casterCell.getId(), dir, fight.map, true)
		var targetCell: GameCase? = fight.map!!.getCase(targetCellId)

		if (targetCell == null || targetCell.fighters.isEmpty())
			return

		var target: Fighter = targetCell.firstFighter!!

		if(target.haveState(Constant.ETAT_ENRACINE))
			return
		if (target.mob != null)
			for (i in  Constant.STATIC_INVOCATIONS)
				if (i == target.mob!!.template!!.id)
					return

		var limit: Int = 0
		while (PathFinding.GetCaseIDFromDirection(targetCellId, dir, fight.map, true) != cell!!.getId()) {
			if (PathFinding.GetCaseIDFromDirection(targetCellId, dir, fight.map, true) == -1)
				return
			targetCellId = PathFinding.GetCaseIDFromDirection(targetCellId, dir, fight.map, true)
			limit += 1
			if (limit > 50)
				return
		}

		var newCell: GameCase? = PathFinding.checkIfCanPushEntity(fight, casterCell.getId(), cell!!.getId(), dir)
		if (newCell != null) cell = newCell

		target.cell!!.removeFighter(target)
		target.cell = (cell)
		target.cell!!.addFighter(target)

		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 5, (caster!!.id).toString(), target.id.toString() + "," + cell!!.getId())
		TimerWaiter.addNext({  -> fight.checkTraps(target) }, 750, TimeUnit.MILLISECONDS)
	}

	private fun applyEffect_784(fight: Fight) {
		var origPos: Map<Int,GameCase> = fight.rholBack // les positions de début de combat

		var list: ArrayList<Fighter> = fight.getFighters(3) // on copie la liste des fighters
		for (i in 1 until list.size) {   // on boucle si tout le monde est à la place
			for (F in  list) {
				if (F == null || F.isDead || F.id !in origPos || F.cell!!.getId() == origPos[F.id]!!.getId()) {
					continue
				}

				if (origPos[F.id]!!.firstFighter == null) {
					F.cell!!.removeFighter(F)
					F.cell = (origPos[F.id])
					F.cell!!.addFighter(F)
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 4, F.id.toString() + "", F.id.toString() + "," + F.cell!!.getId())
				}
			}
		}
	}

	private fun applyEffect_786(targets: ArrayList<Fighter>) {
		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, false, true)
		}
	}

	private fun applyEffect_787(targets: ArrayList<Fighter>) {
		for (target in  targets) {
			target.addBuff(effectID, value, turns, true, spell, args, caster!!, false, true)
		}
	}

	private fun applyEffect_788(targets: ArrayList<Fighter>) {
		for (target in  targets) {
			target.addBuff(effectID, value, turns, false, spell, args, target, false, true)
		}
	}

	private fun applyEffect_950(fight: Fight, targets: ArrayList<Fighter>) {
		var id: Int = -1
		try {
			id = (args.split(";")[2]).toInt()
		} catch (e: Exception) {
			log.error("unexpected error", e)
		}
		if (id == -1)
			return
		if (id == 31 || id == 32 || id == 33 || id == 34) {
			for (mob in  fight.team1.values) {
				mob.setState(id, turns)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", mob.id.toString() + "," + id + ",1")
				mob.addBuff(effectID, value, turns, false, spell, args, mob, false, true)
			}
		}else {
			for (target in  targets) {
				if (spell == 139 && target.team != caster!!.team)//Mot d'altruisme on saute les ennemis ?
					continue
				if (turns <= 0) {
					target.setState(id, turns)
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + id + ",1")
				} else {
					target.setState(id, turns)
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + id + ",1")
					target.addBuff(effectID, value, turns, false, spell, args, target, false, true)
				}
				if (spell == 686) {
					target.unHide(686)
				}
			}
		}
	}

	private fun applyEffect_951(fight: Fight, targets: ArrayList<Fighter>) {
		val id: Int = (args.split(";")[2]).toInt()

		for (target in  targets) {
			if (!target.haveState(id))
				continue

			target.setState(id, 0)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, (caster!!.id).toString(), target.id.toString() + "," + id + ",0")

			TimerWaiter.addNext({  -> {
				for (effect in  target.getBuffsByEffectID(950)) {
					if (id == (args.split(";")[2]).toInt()) {
						target.getFightBuff().remove(effect)
					}
				}
			} }, 1000)
		}
	}

	private fun applyEffect_1000(fight: Fight) {
		var infos: List<String> = args.split(";")
		var spellID: Int = (infos[0]).toInt()
		var level: Int = (infos[1]).toInt()
		var duration: Byte = 1
		var TS: SortStats = World.world.getSort(spellID)!!.getStatsByLevel(level)!!
		var celll: GameCase? = null
		var casenbr: Int = 0
		var quatorze: Boolean = false
		for (entry in  fight.map!!.cases) {
			casenbr = casenbr + 1

			if (casenbr == 14 && quatorze) {
				quatorze = false
				casenbr = 0
			}
			if (casenbr == 15) {
				quatorze = true
				casenbr = 0
			}
			if (quatorze)
				continue
			celll = entry
			if (celll == null)
				continue
			when (celll.getId()) {
28, 57, 86, 115, 144, 173, 202, 231, 260, 289, 318, 347, 376, 405, 434, 463 -> {continue
			
}
}
			if (!celll.isWalkableFight())
				continue
			var g: Glyph = Glyph(fight, caster!!, celll, (0 as Byte), TS, duration, spell)
			fight.glyphs.add(g)

			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 999, caster!!.id.toString() + "", "GDZ+" + celll.getId().toString() + ";" + 0 + ";" + g.color)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 999, caster!!.id.toString() + "", "GDC" + celll.getId().toString() + ";Haaaaaaaaa3005;")
		}
	}

	private fun applyEffect_1001(fight: Fight) {
		var infos: List<String> = args.split(";")
		var spellID: Int = (infos[0]).toInt()
		var level: Int = (infos[1]).toInt()
		var duration: Byte = 1
		var TS: SortStats = World.world.getSort(spellID)!!.getStatsByLevel(level)!!
		var celll: GameCase? = null
		var casenbr: Int = 0
		var quatorze: Boolean = false
		for (entry in  fight.map!!.cases) {
			casenbr = casenbr + 1

			if (casenbr == 14 && quatorze) {
				quatorze = false
				casenbr = 0
			}
			if (casenbr == 15) {
				quatorze = true
				casenbr = 0
			}
			if (!quatorze)
				continue
			celll = entry
			if (celll == null)
				continue
			if (!celll.isWalkableFight())
				continue
			var g: Glyph = Glyph(fight, caster!!, celll, (0 as Byte), TS, duration, spell)
			fight.glyphs.add(g)
			var unk: Int = g.color
			var str: String = "GDZ+" + celll.getId().toString() + ";" + 0 + ";" + unk
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 999, caster!!.id.toString() + "", str)
			str = "GDC" + celll.getId().toString() + ";Haaaaaaaaa3005;"
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 999, caster!!.id.toString() + "", str)
		}
	}

	private fun applyEffect_1002(fight: Fight) {
		var infos: List<String> = args.split(";")
		var spellID: Int = (infos[0]).toInt()
		var level: Int = (infos[1]).toInt()
		var duration: Byte = 100
		var TS: SortStats = World.world.getSort(spellID)!!.getStatsByLevel(level)!!

		if (cell!!.isWalkableFight() && !fight.isOccuped(cell!!.getId())) {
			caster!!.cell!!.removeFighter(caster!!)
			caster!!.cell = (cell)
			caster!!.cell!!.addFighter(caster!!)
			fight.traps.stream().filter({ trap -> PathFinding.getDistanceBetween(fight.map, trap.cell.getId(), caster!!.cell!!.getId()) <= trap.size }).forEach({ trap -> trap.onTrapped(caster!!) })
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 4, caster!!.id.toString() + "", caster!!.id.toString() + "," + cell!!.getId())
		}

		var g: Glyph = Glyph(fight, caster!!, cell!!, (0 as Byte), TS, duration, spell)
		fight.glyphs.add(g)
		var unk: Int = g.color
		var str: String = "GDZ+" + cell!!.getId().toString() + ";" + 0 + ";" + unk
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 999, caster!!.id.toString() + "", str)
		str = "GDC" + cell!!.getId().toString() + ";Haaaaaaaaa3005;"
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 999, caster!!.id.toString() + "", str)
	}

	private fun sortTargets(targets: ArrayList<Fighter>, fight: Fight): ArrayList<Fighter> {
		var array: ArrayList<Fighter> = ArrayList()
		var max: Int = -1
		var distance: Int

		for (f in  targets) {
			distance = PathFinding.getDistanceBetween(fight.map, this.cell!!.getId(), f.cell!!.getId())
			if (distance > max)
				max = distance
		}

		for (i in max downTo 0) {
			var it: MutableIterator<Fighter> = targets.iterator()
			while (it.hasNext()) {
				var f: Fighter = it.next()
				distance = PathFinding.getDistanceBetween(fight.map, this.cell!!.getId(), f.cell!!.getId())
				if (distance == i) {
					array.add(f)
					it.remove()
				}
			}
		}

		return array
	}

	fun checkMonsters(fight: Fight, target: Fighter, effet: Int) {
		when (target.mob!!.template!!.id) {
			232 -> {if (target.hasBuff(112)) {
					target.addBuff(112, 5, -1, true, spell, args, caster!!, false, true)
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 112, caster!!.id.toString() + "", target.id.toString() + "," + 5 + "," + -1)
				}
				
}
233 -> {if (effet == 168 || effet == 101) {
					target.addBuff(128, 1, 2, true, spell, args, target, false, true)
					//Gain de PM pendant le tour de jeu
					target.setCurPm(fight, 1)
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster!!.id.toString() + "", target.id.toString() + "," + 1 + "," + 2)
				}
				if (effet == 169 || effet == 127)//rall pm don pa
				{
					target.addBuff(111, 1, 2, true, spell, args, target, false, true)
					//Gain de PA pendant le tour de jeu
					target.setCurPa(fight, 1)
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster!!.id.toString() + "", target.id.toString() + "," + 1 + "," + 2)
				}
				if (effet == 100 || effet == 97) {
					var healFinal: Int = 200
					if ((healFinal + caster!!.getPdv()) > caster!!.getPdvMax())
						healFinal = caster!!.getPdvMax() - caster!!.getPdv()
					if (healFinal < 1)
						healFinal = 0
					caster!!.removePdv(caster!!, healFinal)
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 108, caster!!.id.toString() + "", target.id.toString() + "," + healFinal)

				}
				
}
1045 -> {if (effet == 99 || effet == 98 || effet == 94 || effet == 93) {
					target.setState(30, 1);//etat pair
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + 30 + ",1")
					if (target.haveState(29)) {
						target.setState(29, 0);//etat 29 impair
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + 29 + ",0")
					}
				} else if (effet == 97 || effet == 96 || effet == 92 || effet == 91) {
					target.setState(29, 1);//etat etat si pair
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + 29 + ",1")
					if (target.haveState(30)) {
						target.setState(30, 0);//etat 29 si pair
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + 30 + ",0")
					}
				}
				
}
423 -> {if (effet == 99 || effet == 94) {
					target.setState(37, 1);//etat tersiaire feu
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + 37 + ",1")
					if (target.haveState(38))//secondaire terre
					{
						target.setState(38, 0)
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + 38 + ",0")
					}
					if (target.haveState(36))//quanternaire eau
					{
						target.setState(36, 0)
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + 36 + ",0")
					}
					if (target.haveState(35))//primaire air
					{
						target.setState(35, 0)
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + 35 + ",0")
					}
				} else if (effet == 98 || effet == 93) {
					target.setState(35, 1);//etat primaire air
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + 35 + ",1")
					if (target.haveState(38))//secondaire terre
					{
						target.setState(38, 0)
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + 38 + ",0")
					}
					if (target.haveState(36))//quanternaire eau
					{
						target.setState(36, 0)
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + 36 + ",0")
					}
					if (target.haveState(37))//tersiaire feu
					{
						target.setState(37, 0)
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + 37 + ",0")
					}
				} else if (effet == 97 || effet == 92) {
					target.setState(38, 1);//etat secondaire terre
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + 38 + ",1")
					if (target.haveState(35))//primaire air
					{
						target.setState(35, 0)
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + 35 + ",0")
					}
					if (target.haveState(36))//quanternaire eau
					{
						target.setState(36, 0)
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + 36 + ",0")
					}
					if (target.haveState(37))//tersiaire feu
					{
						target.setState(37, 0)
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + 37 + ",0")
					}
				} else if (effet == 96 || effet == 91) {
					target.setState(36, 1);//etat quanternaire eau
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + 36 + ",1")
					if (target.haveState(35)) {//primaire air
						target.setState(35, 0)
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + 35 + ",0")
					}
					if (target.haveState(38)) {//secondaire terre
						target.setState(38, 0)
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + 38 + ",0")
					}
					if (target.haveState(37)) {//tersiaire feu
						target.setState(37, 0)
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster!!.id.toString() + "", target.id.toString() + "," + 37 + ",0")
					}
				}

				
}
1071 -> {if (effet == 99 || effet == 94) {
					if (target.hasBuff(214)) {
						target.addBuff(218, 50, 4, false, 1039, "", target, false, true);// - 50 feu
						target.addBuff(210, 50, 4, false, 1039, "", target, false, true);// + 50 terre
						target.addBuff(211, 50, 4, false, 1039, "", target, false, true);// + 50 eau
						target.addBuff(212, 50, 4, false, 1039, "", target, false, true);// + 50 air
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 1039, caster!!.id.toString() + "", target.id.toString() + "," + "" + "," + 1)
					}
				} else if (effet == 98 || effet == 93) {
					if (target.hasBuff(214)) {
						target.addBuff(217, 50, 4, false, 1039, "", target, false, true);// - 50 air
						target.addBuff(210, 50, 4, false, 1039, "", target, false, true);// + 50 terre
						target.addBuff(211, 50, 4, false, 1039, "", target, false, true);// + 50 eau
						target.addBuff(213, 50, 4, false, 1039, "", target, false, true);// + 50 feu
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 1039, caster!!.id.toString() + "", target.id.toString() + "," + "" + "," + 1)
					}
				} else if (effet == 97 || effet == 92) {
					if (target.hasBuff(214)) {
						target.addBuff(215, 50, 4, false, 1039, "", target, false, true);// - 50 terre
						target.addBuff(212, 50, 4, false, 1039, "", target, false, true);// + 50 air
						target.addBuff(211, 50, 4, false, 1039, "", target, false, true);// + 50 eau
						target.addBuff(213, 50, 4, false, 1039, "", target, false, true);// + 50 feu
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 1039, caster!!.id.toString() + "", target.id.toString() + "," + "" + "," + 1)
					}
				} else if (effet == 96 || effet == 91) {
					if (target.hasBuff(214)) {
						target.addBuff(216, 50, 4, false, 1039, "", target, false, true);// - 50 eau
						target.addBuff(212, 50, 4, false, 1039, "", target, false, true);// + 50 air
						target.addBuff(210, 50, 4, false, 1039, "", target, false, true);// + 50 terre
						target.addBuff(213, 50, 4, false, 1039, "", target, false, true);// + 50 feu
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 1039, caster!!.id.toString() + "", target.id.toString() + "," + "" + "," + 1)
					}
				}
				if (effet == 101) {
					target.addBuff(111, value, 1, true, spell, args, target, false, true)
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 111, target.id.toString() + "", target.id.toString() + ",+" + value)
				}
				
}
}
	}
	
	private fun checkLifeTree(target: Fighter, value: Int) {
		if (target.hasBuff(786)) {
		var value = value
			if ((value + caster!!.getPdv()) > caster!!.getPdvMax())
				value = caster!!.getPdvMax() - caster!!.getPdv()
			caster!!.removePdv(caster!!, -value)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(target.fight, 7, 100, target.id.toString() + "", caster!!.id.toString() + ",+" + value)
		}
	}

	fun cloneEffect(): SpellEffect {
		val clone = SpellEffect(effectID, if (this::args.isInitialized) args else "", spell, spellLvl)
		clone.turns = turns
		clone.jet = jet
		clone.chance = chance
		clone.value = value
		clone.caster = caster
		clone.debuffable = debuffable
		clone.cell = cell
		return clone
	}
}