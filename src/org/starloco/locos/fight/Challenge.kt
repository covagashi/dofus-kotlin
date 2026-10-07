package org.starloco.locos.fight

import org.starloco.locos.client.Player
import org.starloco.locos.common.Formulas
import org.starloco.locos.common.PathFinding
import org.starloco.locos.common.SocketManager
import org.starloco.locos.fight.spells.Spell
import org.starloco.locos.fight.spells.SpellEffect
import org.starloco.locos.game.GameClient
import java.util.ArrayList
import java.util.Collections
import org.slf4j.LoggerFactory
import org.starloco.locos.common.splitJ

private val log = LoggerFactory.getLogger(Challenge::class.java)

class Challenge(private val fight: Fight, private val Type: Int, private val xpWin: Int, private val dropWin: Int) {

    private var Arg = 0
    var challengeAlive = false
        private set
    private var challengeWin = false
    private var looseBy = ""
    private var Args = ""
    private var lastActions = ""
    private var _cible: Fighter? = null
    private val _ordreJeu: MutableList<Fighter> = ArrayList()

    init {
        this.challengeAlive = true
        this._ordreJeu.addAll(fight.orderPlaying!!)
    }

    fun getType(): Int {
        return this.Type
    }

    fun getAlive(): Boolean {
        return challengeAlive
    }

    fun getXp(): Int {
        return xpWin
    }

    fun getDrop(): Int {
        return dropWin
    }

    fun getWin(): Boolean {
        return challengeWin
    }

    fun loose(): Boolean {
        return looseBy.isEmpty()
    }

    fun getPacketEndFight(): String {
        return (if (this.challengeWin) "OK$Type" else "KO$Type")
    }

    private fun challengeWin() {
        challengeWin = true
        challengeAlive = false
        SocketManager.GAME_SEND_CHALLENGE_FIGHT(fight, 1, "OK$Type")
    }

    fun challengeLoose(fighter: Fighter?) {
        var name = ""
        if (fighter != null && fighter.player != null)
            name = fighter.player!!.name
        looseBy = name
        challengeWin = false
        challengeAlive = false
        SocketManager.GAME_SEND_CHALLENGE_FIGHT(fight, 7, "KO$Type")
        SocketManager.GAME_SEND_Im_PACKET_TO_CHALLENGE(fight, 1, "0188;$name")
    }

    fun challengeSpecLoose(player: Player) {
        SocketManager.GAME_SEND_CHALLENGE_PERSO(player, "KO$Type")
        SocketManager.GAME_SEND_Im_PACKET_TO_CHALLENGE_PERSO(player, "0188;$looseBy")
    }

    fun parseToPacket(): String {
        val packet = StringBuilder()
        packet.append(Type).append(";").append(if (_cible != null) "1" else "0").append(";").append(if (_cible != null) _cible!!.id else "").append(";").append(xpWin).append(";0;").append(dropWin).append(";0;")
        if (!challengeAlive) {
            if (challengeWin)
                packet.append("").append(Type)
            else
                packet.append("").append(Type)
        }
        return packet.toString()
    }

    fun showCibleToPerso(p: Player?) {
        if (!challengeAlive || _cible == null || _cible!!.cell == null
                || p == null)
            return
        val Pws = ArrayList<GameClient>()
        Pws.add(p.getGameClient()!!)
        SocketManager.GAME_SEND_FIGHT_SHOW_CASE(Pws, _cible!!.id, _cible!!.cell!!.cellId)
    }

    fun showCibleToFight() {
        if (!challengeAlive || _cible == null || _cible!!.cell == null)
            return
        val Pws = ArrayList<GameClient>()
        for (fighter in fight.getFighters(1)) {
            if (fighter.hasLeft())
                continue
            if (fighter.player == null
                    || !fighter.player!!.isOnline)
                continue
            Pws.add(fighter.player!!.getGameClient()!!)
        }
        SocketManager.GAME_SEND_FIGHT_SHOW_CASE(Pws, _cible!!.id, _cible!!.cell!!.cellId)
    }

    fun fightStart() {//Définit les cibles au début du combat
        if (!challengeAlive)
            return
        when (Type) {
            3, 4, 32, 35 -> {
                if (_cible == null && _ordreJeu.size > 0)//Si aucun cible n'est choise on en choisie une
                {
                    val Choix = ArrayList<Fighter>()
                    Choix.addAll(_ordreJeu)
                    Choix.shuffle()//Mélange l'ArrayList
                    for (f in Choix) {
                        if (f.player != null)
                            continue
                        if (f.mob != null && f.getTeam2() == 2
                                && !f.isDead && !f.isInvocation())
                            _cible = f
                    }
                }
                showCibleToFight()//On le montre a tous les joueurs
            }
            10 -> {
                var levelMin = 2000
                for (fighter in fight.getFighters(2))//La cible sera le niveau le plus faible
                {
                    if (fighter.isInvocation())
                        continue
                    if (fighter.player == null
                            && fighter.mob != null
                            && fighter.getLvl() < levelMin
                            && fighter.getInvocator() == null) {
                        levelMin = fighter.getLvl()
                        _cible = fighter
                    }
                }
                if (_cible != null)
                    showCibleToFight()
            }
            25 -> {
                var levelMax = 0
                for (fighter in fight.getFighters(2)) {
                    if (fighter.isDead || fighter.isInvocation())
                        continue
                    if (fighter.player == null && fighter.mob != null && fighter.getInvocator() == null && fighter.getLvl() > levelMax) {
                        levelMax = fighter.getLvl()
                        this._cible = fighter
                    }
                }
                if (_cible != null)
                    showCibleToFight()
            }
        }
    }

    fun fightEnd() {//Vérifie la validité des challenges en fin de combat (si nécessaire)
        if (!challengeAlive)
            return
        when (Type) {
            44, 46 -> {
                for (fighter in fight.getFighters(1)) {
                    if (!Args.contains(fighter.id.toString()) && !fighter.isInvocation()) {
                        challengeLoose(fighter)
                        return
                    }
                }
            }
        }
        challengeWin()
    }

    fun onFighterDie(fighter: Fighter) {
        if (!challengeAlive)
            return
        when (Type) {
            33, 49 -> {
                if (fighter.player != null)
                    challengeLoose(fight.getFighterByGameOrder())
            }
            44 -> {
                if (fighter.player != null)
                    if (!Args.contains(fighter.id.toString()))
                        challengeLoose(fighter)
            }
        }
    }

    fun onFighterAttacked(caster: Fighter, target: Fighter) {
        if (!challengeAlive)
            return
        when (Type) {
            17 -> {
                if (target.team == 0 && !target.isInvocation()) {
                    if (target.getBuff(9) == null) // Si dérobade
                        challengeLoose(target)
                }
            }
            31 -> {
                if (caster.team == 0 && target.team == 1) {
                    if (Args.isEmpty())
                        Args += "|" + target.id
                    else if (!Args.contains("|" + target.id))
                        challengeLoose(caster)
                }
            }
        }
    }

    fun onFightersAttacked(targets: ArrayList<Fighter>, caster: Fighter,
                           SE: SpellEffect, spell: Int, isTrap: Boolean) {
        val effectID = SE.effectID
        if (!challengeAlive)
            return
        val DamagingEffects = "|82|85|86|87|88|89|91|92|93|94|95|96|97|98|99|100|141|"
        val HealingEffects = "|108|"
        val MPEffects = "|77|127|169|"
        val APEffects = "|84|101|"
        val OPEffects = "|116|320|"
        when (Type) {
            31 -> {
            }
            18 -> {
                if ((caster.team == 0) && !caster.isInvocation() && HealingEffects.contains("|$effectID|"))
                    targets.stream().filter { fighter -> fighter.team == 0 }.forEach { challengeLoose(caster) }
            }
            20 -> {
                if ((caster.team == 0)
                        && DamagingEffects.contains("|$effectID|")
                        && effectID != 141 && !caster.isInvocation()) {
                    when (spell) {
                        126, 149, 106, 111, 108, 435, 135, 123 -> return
                    }
                    if (Arg == 0) {
                        Arg = effectID
                        return
                    }
                    if (Arg != effectID) {
                        val eau = "85 91 96"
                        val terre = "86 92 97"
                        val air = "87 93 98"
                        val feu = "88 94 99"
                        val neutre = "89 95 100"
                        if (eau.contains(Arg.toString())
                                && eau.contains(effectID.toString())) {
                            return
                        } else if (terre.contains(Arg.toString())
                                && terre.contains(effectID.toString())) {
                            return
                        } else if (air.contains(Arg.toString())
                                && air.contains(effectID.toString())) {
                            return
                        } else if (feu.contains(Arg.toString())
                                && feu.contains(effectID.toString())) {
                            return
                        } else if (neutre.contains(Arg.toString())
                                && neutre.contains(effectID.toString())) {
                            return
                        }
                        challengeLoose(caster)
                        return
                    }
                }
            }
            21 -> {
                if ((caster.team == 0) && MPEffects.contains("|$effectID|")) {
                    for (target in targets) {
                        if (target.team == 1) {
                            challengeLoose(caster)
                            break
                        }
                    }
                }
            }
            22 -> {
                if ((caster.team == 0)
                        && APEffects.contains("|$effectID|")) {
                    for (target in targets) {
                        if (target.team == 1) {
                            challengeLoose(caster)
                            break
                        }
                    }
                }
            }
            23 -> {
                if ((caster.team == 0)
                        && OPEffects.contains("|$effectID|")) {
                    for (target in targets) {
                        if (target.team == 1) {
                            challengeLoose(caster)
                            break
                        }
                    }
                }
            }
            32, 34 -> {
                if ((caster.team == 0)
                        && DamagingEffects.contains("|$effectID|")) {
                    for (target in targets) {
                        if (target.team == 1) {
                            if (_cible == null
                                    || _cible!!.id != target.id)
                                challengeLoose(caster)
                        }
                    }
                }
            }
            38 -> {
                if ((caster.team == 0) && DamagingEffects.contains("|$effectID|")) {
                    for (target in targets) {
                        if (target.team == 1) {
                            val id = StringBuilder()
                            id.append(";").append(target.id).append(",")
                            if (!this.Args.contains(id.toString())) {
                                id.append(caster.id)
                                this.Args += id.toString()
                            }
                        }
                    }
                }
            }
            43 -> {
                if ((caster.team == 0) && HealingEffects.contains("|$effectID|") && caster.getInvocator() == null)
                    for (target in targets)
                        if (target.id == caster.id)
                            challengeLoose(caster)
            }
            45 -> {
                if ((caster.team == 0) && DamagingEffects.contains("|$effectID|")) {
                    for (target in targets) {
                        if (target.team == 1) {
                            if (!Args.contains(";" + target.id + ","))
                                Args += ";" + target.id + "," + caster.id + ";"
                            else if (Args.contains(";" + target.id + ",") && !Args.contains(";" + target.id + "," + caster.id + ";"))
                                challengeLoose(target)
                        }
                    }
                }
            }
            46 -> {
                if ((caster.team == 0) && DamagingEffects.contains("|$effectID|")) {
                    for (target in targets) {
                        if (target.team == 1 && !target.isInvocation()) {
                            if (!Args.contains(";" + target.id + ","))
                                Args += ";" + target.id + "," + caster.id + ";"
                            else if (Args.contains(";" + target.id + ",") && !Args.contains(";" + target.id + "," + caster.id + ";"))
                                challengeLoose(target)
                        }
                    }
                }
            }
            47 -> {
                if (DamagingEffects.contains("|$effectID|"))
                    targets.stream().filter { target -> target.team == 0 && target.getPdv() != target.getPdvMax() }
                            .filter { target -> !Args.contains(";" + target.id + ",") }.forEach { target -> Args += ";" + target.id + "," + "3;" }
            }
        }
    }

    fun onMobDie(mob: Fighter, killer: Fighter) {
        if (mob.mob == null)
            return
        if (mob.player != null)
            return
        if (mob.team != 1)
            return
        if (mob.isInvocation() && mob.getInvocator()!!.player != null)
            return

        val isKiller = (killer.id != mob.id)

        if (!challengeAlive)
            return

        when (Type) {
            3 -> {
                if (_cible == null)
                    return
                if (mob.isInvocation()) return

                if (mob.getInvocator() != null)
                    if (mob.getInvocator()!!.id == _cible!!.id)
                        return

                if (_cible!!.id != mob.id) {
                    challengeLoose(fight.getFighterByGameOrder())
                } else {
                    challengeWin()
                }
                _cible = null
            }
            19 -> {
                if (killer.team != 0 || killer.isInvocation() || this.fight.getFighterByGameOrder() !== killer)
                    return
                if (!mob.isTrappedOrGlyphed() && mob.team == 1 && !mob.isInvocation()) {
                    challengeLoose(killer)
                }
            }
            4 -> {
                if (_cible == null)
                    return

                if (_cible!!.id == mob.id && !fight.verifIfTeamIsDead()) {
                    challengeLoose(fight.getFighterByGameOrder())
                }
            }
            28 -> {
                if (!mob.isInvocation() && isKiller && killer.player != null)
                    if (killer.player!!.sexe == 0) {
                        challengeLoose(fight.getFighterByGameOrder())
                    }
            }
            29 -> {
                if (!mob.isInvocation() && isKiller && killer.player != null) {
                    if (killer.player!!.sexe == 1) {
                        challengeLoose(fight.getFighterByGameOrder())
                    }
                }
            }
            31 -> {
                if (killer.mob != null || killer === mob || mob.levelUp)
                    return
                if (Args.contains("|" + mob.id))
                    Args = ""
                else
                    challengeLoose(killer)
            }
            32 -> {
                if (_cible!!.id == mob.id)
                    challengeWin()
            }
            34 -> {
                _cible = null
            }
            42 -> {
                if (mob.isInvocation() || killer.isInvocation())
                    return
                Args += (if (Args.isEmpty()) killer.id.toString() else ";" + killer.id)
            }
            44, 46 -> {
                if (!mob.isInvocation() && isKiller)
                    Args += (if (Args.isEmpty()) killer.id.toString() else ";" + killer.id)
            }
            30, 48 -> {
                if (mob.isInvocation())
                    return
                if (mob.id != killer.id) {
                    var lvlMin = 5000
                    for (f in fight.getTeamFighters(1)) {
                        if (f.isInvocation())
                            continue
                        if (f.getLvl() < lvlMin)
                            lvlMin = f.getLvl()
                    }
                    if (killer.getLvl() > lvlMin)
                        challengeLoose(fight.getFighterByGameOrder())
                }
            }
            35 -> {
                if (_cible == null)
                    return
                if (_cible!!.id != mob.id) {
                    if (!mob.isInvocation())
                        challengeLoose(fight.getFighterByGameOrder())
                } else {
                    try {
                        _cible = null
                        val fighters = ArrayList(fight.getFighters(2))
                        val it = fighters.iterator()
                        while (it.hasNext()) {
                            val f = it.next()
                            if (f.isInvocation() || f.isDead
                                    || f.player != null)
                                it.remove()
                        }
                        fighters.sort()
                        for (f in fighters) {
                            if (!f.isInvocation() && !f.isDead
                                    && f.player == null) {
                                _cible = f
                                break
                            }
                        }
                        showCibleToFight()
                    } catch (e: Exception) {
                        log.error("unexpected error", e)
                }
                }
            }
            10 -> {
                if (_cible == null)
                    return
                if (_cible!!.isInvocation() || mob.player != null)
                    return
                if (_cible!!.id != mob.id
                        && _cible!!.getLvl() != mob.getLvl()) {
                    if (mob.getLvl() > _cible!!.getLvl())
                        challengeLoose(fight.getFighterByGameOrder())
                } else {
                    try {
                        var levelMin = 2000
                        for (fighter in fight.getFighters(2)) {
                            if (fighter.isInvocation() || fighter.player != null || fighter.isDead)
                                continue
                            if (fighter.player == null
                                    && fighter.getLvl() < levelMin) {
                                levelMin = fighter.getLvl()
                                _cible = fighter
                            }
                        }
                        if (_cible != null)
                            showCibleToFight()
                    } catch (e: Exception) {
                        log.error("unexpected error", e)
                }
                }
            }
            25 -> {
                if (_cible == null)
                    return
                if (mob.isInvocation() || mob.player != null)
                    return
                if (killer.mob != null && killer !== mob)
                    return

                if (_cible!!.id != mob.id) {
                    if (mob.getLvl() < _cible!!.getLvl())
                        challengeLoose(fight.getFighterByGameOrder())
                } else {
                    var levelMax = 0
                    for (fighter in fight.getFighters(2)) {
                        if (fighter.isInvocation() || fighter.player != null || fighter.isDead)
                            continue
                        if (fighter.getLvl() > levelMax) {
                            levelMax = fighter.getLvl()
                            _cible = fighter
                        }
                    }
                    if (_cible != null)
                        showCibleToFight()
                }
            }
        }
    }

    fun onPlayerMove(fighter: Fighter, failed: Boolean) {
        if (!challengeAlive)
            return
        when (Type) {
            1 -> {
                if (failed || this.fight.curFighterUsedPm > 1) // Si l'on a utilisé plus d'un PM
                    challengeLoose(fight.getFighterByGameOrder())
            }
            8 -> {
                if (failed)
                    challengeLoose(fight.getFighterByGameOrder())
            }
        }
    }

    fun onPlayerAction(fighter: Fighter, actionID: Int) {
        if (!challengeAlive || fighter.team == 1)
            return
        val action = StringBuilder()
        action.append(";").append(fighter.id)
        action.append(",").append(actionID).append(";")
        when (Type) {
            6, 5 -> {
                if (lastActions.contains(action.toString()))
                    challengeLoose(fight.getFighterByGameOrder())
                lastActions += action.toString()
            }
            24 -> {
                if (!lastActions.contains(action.toString())
                        && lastActions.contains(";" + fighter.id + ","))
                    challengeLoose(fight.getFighterByGameOrder())
                lastActions += action.toString()
            }
        }
    }

    fun onPlayerCac(fighter: Fighter) {
        if (!challengeAlive)
            return
        when (Type) {
            11 -> {
                challengeLoose(fight.getFighterByGameOrder())
            }
            6, 5 -> {
                val action = StringBuilder()
                action.append(";").append(fighter.id)
                action.append(",").append("cac").append(";")
                if (lastActions.contains(action.toString()))
                    challengeLoose(fight.getFighterByGameOrder())
                lastActions += action.toString()
            }
        }
    }

    fun onPlayerSpell(fighter: Fighter, spellStats: Spell.SortStats) {
        if (!challengeAlive)
            return
        if (fighter.player == null)
            return
        when (Type) {
            9 -> {
                challengeLoose(fight.getFighterByGameOrder())
            }
            14 -> {
                if (fighter.player != null)
                    if (spellStats.spellID == 101)
                        Args = "cast"
            }
        }
    }

    fun onPlayerStartTurn(fighter: Fighter) {
        if (!challengeAlive)
            return
        when (Type) {
            2 -> {
                if (fighter.player == null)
                    return
                Arg = fighter.cell!!.cellId
            }
            6 -> {
                lastActions = ""
            }
            14 -> {
                if (fighter.player != null)
                    if (fighter.canLaunchSpell(101))
                        Args = "ok"
                    else Args = "cant"
            }
            34 -> {
                if (fighter.team == 1)
                    return
                try {
                    var noBoucle = 0
                    var GUID = 0
                    _cible = null
                    while (_cible == null) {
                        if (_ordreJeu.size > 0) {
                            GUID = Formulas.getRandomValue(0, _ordreJeu.size - 1)
                            val f = _ordreJeu[GUID]
                            if (f.player == null && !f.isDead)
                                _cible = f
                            noBoucle++
                            if (noBoucle > 150)
                                return
                        }
                    }
                    showCibleToFight()
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                }
            }
            38 -> {
                if (fighter.team == 1 && Args.contains(";" + fighter.id + ",")) {
                    if (fighter.isDead) return

                    var id = 0

                    for (string in this.Args.splitJ(";")) {
                        if (string.contains("" + fighter.id)) {
                            for (test in string.splitJ(","))
                                id = (test).toInt()
                            break
                        }
                    }

                    for (target in this.fight.getFighters(1))
                        if (target.id == id)
                            if (fighter.getPdv() != fighter.getPdvMax())
                                challengeLoose(target)
                }
            }
            47 -> {
                if (fighter.team == 0) {
                    val str = ";" + fighter.id + ","
                    if (Args.contains(str + "1;"))
                        challengeLoose(fighter)
                    else if (Args.contains(str + "2;"))
                        Args += str + "1;"
                    else if (Args.contains(str + "3;"))
                        Args += str + "2;"
                }
            }
        }
    }

    fun onPlayerEndTurn(fighter: Fighter) {
        if (!challengeAlive)
            return

        var hasFailed = false
        val fighters = PathFinding.getFightersAround(fighter.cell!!.cellId, fight.map!!)

        when (Type) {
            1 -> {
                if (this.fight.curFighterUsedPm <= 0) // Si l'on a pas bougé
                    challengeLoose(fighter)
            }
            2 -> {
                if (fighter.player != null)
                    if (fighter.cell!!.cellId != Arg)
                        challengeLoose(fighter)
            }
            7 -> {
                if (fighter.player != null)
                    if (fighter.canLaunchSpell(367))
                        challengeLoose(fighter)
            }
            8 -> {
                if (!fighter.isInvocation() && this.fight.curFighterPm != 0)
                    challengeLoose(fighter)
            }
            12 -> {
                if (fighter.player != null)
                    if (fighter.canLaunchSpell(373))
                        challengeLoose(fighter)
            }
            14 -> {
                if (fighter.player != null)
                    if (Args == "ok")
                        challengeLoose(fighter)
            }
            15 -> {
                if (fighter.player != null)
                    if (fighter.canLaunchSpell(370))
                        challengeLoose(fighter)
            }
            36 -> {
                hasFailed = true
                if (!fighters.isEmpty())
                    for (f in fighters)
                        if (f.team != fighter.team)
                            hasFailed = false
            }
            37 -> {
                hasFailed = true
                if (!fighters.isEmpty())
                    for (f in fighters)
                        if (f.team == fighter.team)
                            hasFailed = false
            }
            39 -> {
                if (!fighters.isEmpty())
                    fighters.stream().filter { f -> f.team == fighter.team }.forEach { challengeLoose(fighter) }
            }
            40 -> {
                if (!fighters.isEmpty())
                    fighters.stream().filter { f -> f.team != fighter.team }.forEach { challengeLoose(fighter) }
            }
            41 -> {
                if (this.fight.curFighterPa != 0 && !fighter.hasBuff(168))
                    challengeLoose(fighter)
            }
            42 -> {
                if (!Args.isEmpty())
                    if (!(Args.splitJ(";").size % 2 == 0))
                        hasFailed = true
                Args = ""
            }
        }
        if (hasFailed) challengeLoose(fighter)
    }
}
