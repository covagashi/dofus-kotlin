package org.starloco.locos.common

import org.starloco.locos.area.map.GameMap
import org.starloco.locos.area.map.OrthogonalProj
import org.starloco.locos.client.Player
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.PlayerFighter
import org.starloco.locos.fight.spells.ResEffectInfo
import org.starloco.locos.fight.spells.SpellEffect
import org.starloco.locos.game.world.World
import org.starloco.locos.game.world.World.Couple
import org.starloco.locos.kernel.Config
import org.starloco.locos.kernel.Constant
import org.starloco.locos.`object`.GameObject
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Random
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(Formulas::class.java)

class Formulas {
    companion object {
        @JvmField
        val random = Random()

        private const val EMPIRICAL_MIN = -4
        private const val EMPIRICAL_MAX = 4

        @JvmStatic
        fun nextGaussian(min: Double, max: Double): Int {
            var next: Double
            do {
                next = random.nextGaussian()
            } while (next < EMPIRICAL_MIN || next > EMPIRICAL_MAX)
            return Math.round((min + ((max + 1) - min) * (next - EMPIRICAL_MIN) / (EMPIRICAL_MAX - EMPIRICAL_MIN)).toFloat())
        }

        @JvmStatic
        fun shuffleCharArray(ar: CharArray): CharArray {
            // If running on Java 6 or older, use `Random()` on RHS here
            val rnd = ThreadLocalRandom.current()
            for (i in ar.size - 1 downTo 1) {
                val index = rnd.nextInt(i + 1)
                // Simple swap
                val a = ar[index]
                ar[index] = ar[i]
                ar[i] = a
            }
            return ar
        }

        @JvmStatic
        fun countCell(i0: Int): Int {
            var i = i0
            if (i > 64)
                i = 64
            return 2 * i * (i + 1)
        }

        /**  Returns a random int between i1 and i2 included  */
        @JvmStatic
        fun getRandomValue(i1: Int, i2: Int): Int {
            if (i2 < i1)
                return 0
            return random.nextInt(i2 - i1 + 1) + i1
        }

        @JvmStatic
        fun getMinJet(jet: String): Int {
            try {
                val des = jet.split("d")[0].toInt()
                val add = jet.split("d")[1].split("+")[1].toInt()
                return des + add
            } catch (e: NumberFormatException) {
                log.error("unexpected error", e)
                return -1
            }
        }

        @JvmStatic
        fun getMaxJet(jet: String): Int {
            var num = 0
            try {
                val des = jet.split("d")[0].toInt()
                val faces = jet.split("d")[1].split("+")[0].toInt()
                val add = jet.split("d")[1].split("+")[1].toInt()
                for (a in 0 until des) {
                    num += faces
                }
                num += add
                return num
            } catch (e: NumberFormatException) {
                log.error("unexpected error", e)
                return -1
            }
        }

        @JvmStatic
        fun getRandomJet(caster: Fighter?, target: Fighter?, jet: String): Int//1d5+6
        {
            if (target != null && target.hasBuff(782)) {
                return getMaxJet(jet)
            }
            if (caster != null && caster.hasBuff(781)) {
                return getMinJet(jet)
            }

            try {
                var num = 0
                val des = jet.split("d")[0].toInt()
                val faces = jet.split("d")[1].split("+")[0].toInt()
                val add = jet.split("d")[1].split("+")[1].toInt()
                if (faces == 0 && add == 0) {
                    num = getRandomValue(0, des)
                } else {
                    for (a in 0 until des) {
                        num += getRandomValue(1, faces)
                    }
                }
                num += add
                return num
            } catch (e: NumberFormatException) {
                log.error("unexpected error", e)
                return -1
            }
        }

        @JvmStatic
        fun getMiddleJet(jet: String): Int//1d5+6
        {
            try {
                var num = 0
                val des = jet.split("d")[0].toInt()
                val faces = jet.split("d")[1].split("+")[0].toInt()
                val add = jet.split("d")[1].split("+")[1].toInt()
                num += ((1 + faces) / 2) * des//on calcule moyenne
                num += add
                return num
            } catch (e: NumberFormatException) {
                log.error("unexpected error", e)
                return 0
            }
        }

        @JvmStatic
        fun getTacleChance(fight: Fighter, fighter: Fighter): Int {
            val agiTacleur = fight.getTotalStats().getEffect(Constant.STATS_ADD_AGIL)
            val agiEnemi = fighter.getTotalStats().getEffect(Constant.STATS_ADD_AGIL)
            var div = agiTacleur + agiEnemi + 50
            if (div == 0)
                div = 1
            val esquive = 300 * (agiTacleur + 25) / div - 100
            return esquive
        }

        @JvmStatic
        fun calculFinalHeal(caster: Player, jet: Int): Int {
            var statC = caster.getTotalStats(false).getEffect(Constant.STATS_ADD_INTE)
            val soins = caster.getTotalStats(false).getEffect(Constant.STATS_ADD_SOIN)
            if (statC < 0)
                statC = 0
            return (jet * (100 + statC) / 100) + soins
        }

        @JvmStatic
        fun calculFinalHealCac(healer: Fighter, rank: Int, isCac: Boolean): Int {
            var intel = healer.getTotalStats().getEffect(126)
            val heals = healer.getTotalStats().getEffect(178)
            if (intel < 0)
                intel = 0
            var adic = 100f
            if (isCac)
                adic = 105f
            return (rank * ((100.00 + intel) / adic) + heals / 2).toInt()
        }

        @JvmStatic
        fun calculXpWinCraft(lvl: Int, numCase: Int): Int {
            if (lvl == 100)
                return 0
            when (numCase) {
                1 -> {
                    if (lvl < 40)
                        return 1
                    return 0
                }
                2 -> {
                    if (lvl < 60)
                        return 10
                    return 0
                }
                3 -> {
                    if (lvl > 9 && lvl < 80)
                        return 25
                    return 0
                }
                4 -> {
                    if (lvl > 19)
                        return 50
                    return 0
                }
                5 -> {
                    if (lvl > 39)
                        return 100
                    return 0
                }
                6 -> {
                    if (lvl > 59)
                        return 250
                    return 0
                }
                7 -> {
                    if (lvl > 79)
                        return 500
                    return 0
                }
                8 -> {
                    if (lvl > 99)
                        return 1000
                    return 0
                }
            }
            return 0
        }

        @JvmStatic
        fun calculXpWinFm(lvl: Int, poid: Int): Int {
            if (lvl <= 1) {
                if (poid <= 10)
                    return 10
                else if (poid <= 50)
                    return 25
                else
                    return 50
            }
            if (lvl <= 25) {
                if (poid <= 10)
                    return 10
                else
                    return 50
            } else if (lvl <= 50) {
                if (poid <= 1)
                    return 10
                if (poid <= 10)
                    return 25
                if (poid <= 50)
                    return 50
                else
                    return 100
            } else if (lvl <= 75) {
                if (poid <= 3)
                    return 25
                if (poid <= 10)
                    return 50
                if (poid <= 50)
                    return 100
                else
                    return 250
            } else if (lvl <= 100) {
                if (poid <= 3)
                    return 50
                if (poid <= 10)
                    return 100
                if (poid <= 50)
                    return 250
                else
                    return 500
            } else if (lvl <= 125) {
                if (poid <= 3)
                    return 100
                if (poid <= 10)
                    return 250
                if (poid <= 50)
                    return 500
                else
                    return 1000
            } else if (lvl <= 150) {
                if (poid <= 10)
                    return 250
                else
                    return 1000
            } else if (lvl <= 175) {
                if (poid <= 1)
                    return 250
                if (poid <= 10)
                    return 500
                else
                    return 1000
            } else {
                if (poid <= 1)
                    return 500
                else
                    return 1000
            }
        }

        @JvmStatic
        fun calculXpLooseCraft(lvl: Int, numCase: Int): Int {
            if (lvl == 100)
                return 0
            when (numCase) {
                1 -> {
                    if (lvl < 40)
                        return 1
                    return 0
                }
                2 -> {
                    if (lvl < 60)
                        return 5
                    return 0
                }
                3 -> {
                    if (lvl > 9 && lvl < 80)
                        return 12
                    return 0
                }
                4 -> {
                    if (lvl > 19)
                        return 25
                    return 0
                }
                5 -> {
                    if (lvl > 39)
                        return 50
                    return 0
                }
                6 -> {
                    if (lvl > 59)
                        return 125
                    return 0
                }
                7 -> {
                    if (lvl > 79)
                        return 250
                    return 0
                }
                8 -> {
                    if (lvl > 99)
                        return 500
                    return 0
                }
            }
            return 0
        }


        @JvmStatic
        fun calculFinalDommage(fight: Fight, caster: Fighter, target: Fighter, statID: Int, jet: Int, isHeal: Boolean, isCaC: Boolean, spellid: Int): Int {
            return Math.max(calculFinalDommagee(fight, caster, target, statID, jet, isHeal, isCaC, spellid), 0)
        }

        @JvmStatic
        fun calculFinalDommagee(fight: Fight, caster: Fighter,
                                target: Fighter, statID: Int, jet: Int, isHeal: Boolean, isCaC: Boolean,
                                spellid: Int): Int {
            var i = 0f//Bonus maitrise
            var j = 100f //Bonus de Classe
            var a = 1f//Calcul
            var num = 0f
            var statC = 0f
            var domC = 0f
            var perdomC = 0f
            var resfT = 0f
            var respT = 0f
            var mulT = 1f
            var multiplier = 0
            if (spellid != 450) { // Folie sanguinaire, on ne prend pas les dommages mais les résistances
                if (!isHeal) {
                    domC = caster.getTotalStats().getEffect(Constant.STATS_ADD_DOMA).toFloat()
                    perdomC = caster.getTotalStats().getEffect(Constant.STATS_ADD_PERDOM).toFloat()
                    multiplier = caster.getTotalStats().getEffect(Constant.STATS_MULTIPLY_DOMMAGE)
                    if (target.isTrapped()) {
                        domC += caster.getTotalStats().getEffect(Constant.STATS_ADD_TRAP_DOM)
                        perdomC += caster.getTotalStats().getEffect(Constant.STATS_ADD_TRAP_PERDOM)
                    }
                    if (caster.hasBuff(114))
                        mulT = caster.getBuffValue(114).toFloat()
                } else {
                    domC = caster.getTotalStats().getEffect(Constant.STATS_ADD_SOIN).toFloat()
                }
            }

            when (statID) {
                Constant.ELEMENT_NULL -> {//Fixe
                    statC = 0f
                    resfT = 0f
                    respT = 0f
                    mulT = 1f
                }
                Constant.ELEMENT_NEUTRE -> {//neutre
                    if (spellid != 450)
                        statC = caster.getTotalStats().getEffect(Constant.STATS_ADD_FORC).toFloat()
                    resfT = target.getTotalStats().getEffect(Constant.STATS_ADD_R_NEU).toFloat()
                    respT = target.getTotalStats().getEffect(Constant.STATS_ADD_RP_NEU).toFloat()
                    if (caster.player != null)//Si c'est un joueur
                    {
                        respT += target.getTotalStats().getEffect(Constant.STATS_ADD_RP_PVP_NEU)
                        resfT += target.getTotalStats().getEffect(Constant.STATS_ADD_R_PVP_NEU)
                    }
                    //on ajoute les dom Physique
                    if (spellid != 450)
                        domC += caster.getTotalStats().getEffect(142)

                    //Ajout de la resist Physique
                    resfT += target.getTotalStats().getEffect(184)
                }
                Constant.ELEMENT_TERRE -> {//force
                    statC = caster.getTotalStats().getEffect(Constant.STATS_ADD_FORC).toFloat()
                    resfT = target.getTotalStats().getEffect(Constant.STATS_ADD_R_TER).toFloat()
                    respT = target.getTotalStats().getEffect(Constant.STATS_ADD_RP_TER).toFloat()
                    if (caster.player != null)//Si c'est un joueur
                    {
                        respT += target.getTotalStats().getEffect(Constant.STATS_ADD_RP_PVP_TER)
                        resfT += target.getTotalStats().getEffect(Constant.STATS_ADD_R_PVP_TER)
                    }
                    //on ajout les dom Physique
                    domC += caster.getTotalStats().getEffect(142)
                    //Ajout de la resist Physique
                    resfT += target.getTotalStats().getEffect(184)
                }
                Constant.ELEMENT_EAU -> {//chance
                    statC = caster.getTotalStats().getEffect(Constant.STATS_ADD_CHAN).toFloat()
                    resfT = target.getTotalStats().getEffect(Constant.STATS_ADD_R_EAU).toFloat()
                    respT = target.getTotalStats().getEffect(Constant.STATS_ADD_RP_EAU).toFloat()
                    if (caster.player != null)//Si c'est un joueur
                    {
                        respT += target.getTotalStats().getEffect(Constant.STATS_ADD_RP_PVP_EAU)
                        resfT += target.getTotalStats().getEffect(Constant.STATS_ADD_R_PVP_EAU)
                    }
                    //Ajout de la resist Magique
                    resfT += target.getTotalStats().getEffect(183)
                }
                Constant.ELEMENT_FEU -> {//intell
                    statC = caster.getTotalStats().getEffect(Constant.STATS_ADD_INTE).toFloat()
                    resfT = target.getTotalStats().getEffect(Constant.STATS_ADD_R_FEU).toFloat()
                    respT = target.getTotalStats().getEffect(Constant.STATS_ADD_RP_FEU).toFloat()
                    if (caster.player != null)//Si c'est un joueur
                    {
                        respT += target.getTotalStats().getEffect(Constant.STATS_ADD_RP_PVP_FEU)
                        resfT += target.getTotalStats().getEffect(Constant.STATS_ADD_R_PVP_FEU)
                    }
                    //Ajout de la resist Magique
                    resfT += target.getTotalStats().getEffect(183)
                }
                Constant.ELEMENT_AIR -> {//agilite
                    statC = caster.getTotalStats().getEffect(Constant.STATS_ADD_AGIL).toFloat()
                    resfT = target.getTotalStats().getEffect(Constant.STATS_ADD_R_AIR).toFloat()
                    respT = target.getTotalStats().getEffect(Constant.STATS_ADD_RP_AIR).toFloat()
                    if (caster.player != null)//Si c'est un joueur
                    {
                        respT += target.getTotalStats().getEffect(Constant.STATS_ADD_RP_PVP_AIR)
                        resfT += target.getTotalStats().getEffect(Constant.STATS_ADD_R_PVP_AIR)
                    }
                    //Ajout de la resist Magique
                    resfT += target.getTotalStats().getEffect(183)
                }
            }
            //On bride la resistance a 50% si c'est un joueur
            if (target is PlayerFighter) {
                respT = Math.min(50f, respT)
            }

            if (statC < 0)
                statC = 0f
            if (caster.player != null && isCaC) {
                val ArmeType = caster.player!!.getObjetByPos(1)!!.template!!.type
                j = Constant.getWeaponBonusByClass(ArmeType, caster.player!!.classe).toFloat()
                if ((caster.getSpellValueBool(392)) && ArmeType == 2)//ARC
                    i = caster.getMaitriseDmg(392).toFloat()
                else if ((caster.getSpellValueBool(390)) && ArmeType == 4)//BATON
                    i = caster.getMaitriseDmg(390).toFloat()
                else if ((caster.getSpellValueBool(391)) && ArmeType == 6)//EPEE
                    i = caster.getMaitriseDmg(391).toFloat()
                else if ((caster.getSpellValueBool(393)) && ArmeType == 7)//MARTEAUX
                    i = caster.getMaitriseDmg(393).toFloat()
                else if ((caster.getSpellValueBool(394)) && ArmeType == 3)//BAGUETTE
                    i = caster.getMaitriseDmg(394).toFloat()
                else if ((caster.getSpellValueBool(395)) && ArmeType == 5)//DAGUES
                    i = caster.getMaitriseDmg(395).toFloat()
                else if ((caster.getSpellValueBool(396)) && ArmeType == 8)//PELLE
                    i = caster.getMaitriseDmg(396).toFloat()
                else if ((caster.getSpellValueBool(397)) && ArmeType == 19)//HACHE
                    i = caster.getMaitriseDmg(397).toFloat()
                a = (((100 + i) / 100) * (j / 100))
            }

            var perdomT = 0
            if (target.hasBuff(Constant.STATS_REM_PERDOM)) {
                perdomT = target.getTotalStats().getEffect(Constant.STATS_REM_PERDOM)
            }

            num = a * mulT * (jet * ((100 + statC + perdomC - perdomT + (multiplier * 100)) / 100)) + domC//dégats bruts
            //Poisons
            if (spellid != -1) {
                when (spellid) {
                    66 -> { // Poison Insidieux
                        statC = caster.getTotalStats().getEffect(Constant.STATS_ADD_AGIL).toFloat()
                        num = (jet * ((100 + statC + perdomC + (multiplier * 100)) / 100)) + domC
                        val reduction = ((num / 100f) * respT).toInt()
                        num -= reduction

                        if (target.hasBuff(184)) {
                            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.id.toString() + "", target.id.toString() + "," + target.getBuff(184)!!.value)
                            val value = num.toInt() - target.getBuff(184)!!.value
                            num = (if (value > 0) value else 0).toFloat()
                        }
                        return num.toInt()
                    }
                    164, //FLèche empoisonnée
                    71, // Piege empoissonee
                    196, // Vent empoisonnee
                    219 -> { // Empoisonement
                        statC = caster.getTotalStats().getEffect(Constant.STATS_ADD_FORC).toFloat()
                        num = (jet * ((100 + statC + perdomC + (multiplier * 100)) / 100)) + domC
                        val reduction = ((num / 100f) * respT).toInt()
                        num -= reduction

                        if (target.hasBuff(184) && spellid != 71) {
                            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.id.toString() + "", target.id.toString() + "," + target.getBuff(184)!!.value)
                            val value = num.toInt() - target.getBuff(184)!!.value
                            num = (if (value > 0) value else 0).toFloat()
                        }
                        return num.toInt()
                    }
                    181, // Tremblement
                    200 -> { // Poison paralysant
                        statC = caster.getTotalStats().getEffect(Constant.STATS_ADD_INTE).toFloat()
                        num = (jet * ((100 + statC + perdomC + (multiplier * 100)) / 100)) + domC
                        val reduction = ((num / 100f) * respT).toInt()
                        num -= reduction

                        if (target.hasBuff(184)) {
                            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.id.toString() + "", target.id.toString() + "," + target.getBuff(184)!!.value)
                            val value = num.toInt() - target.getBuff(184)!!.value
                            num = (if (value > 0) value else 0).toFloat()
                        }
                        return num.toInt()
                    }
                }
            }

            //Renvoie
            if (caster.id != target.id && spellid != -1) {
                var returns = target.getTotalStatsLessBuff()!!.getEffect(Constant.STATS_RETDOM)
                if (returns > 0 && !isHeal) {
                    //returns = calculFinalDommage(fight, target, caster, statID, returns, false, false, -1);
                    if (returns > num) returns = num.toInt()
                    num -= returns

                    SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 107, "-1", target.id.toString() + "," + returns)

                    if (returns > caster.getPdv()) returns = caster.getPdv()
                    if (num < 1) num = 0f

                    if (caster.hasBuff(105)) {
                        returns -= caster.getBuff(105)!!.value//Immu
                        if (returns <= 0)
                            returns = 0
                        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.id.toString() + "", target.id.toString() + "," + caster.getBuff(105)!!.value)
                    }
                    if (returns > 0) {
                        caster.removePdv(caster, returns)
                        if (caster.getPdv() <= returns)
                            fight.onFighterDie(caster, caster)
                    }
                    SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.id.toString() + "", caster.id.toString() + ",-" + returns)
                }
            }

            // Resistance % & armor
            val reduction = ((num / 100f) * respT).toInt()
            val armor = getArmorResist(target, statID)

            if (!isHeal) {
                num -= reduction // Resistance %
                num -= armor // Armor
                num -= resfT // Resistance fix
                if (armor > 0)
                    SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.id.toString() + "", target.id.toString() + "," + armor)
            }

            if (num < 1) num = 0f
            if (target.player != null) // 10% of dommages substract to the max pdv
                target.removePdvMax(floor(num / 10.toDouble()).toInt())

            // The level of the mob help the damage
//        if (caster.player == null && !caster.isCollector())
//            return (int) (num * Math.ceil((caster.getLvl() * 0.5) / 100));
            return num.toInt()
        }

        @JvmStatic
        fun calculZaapCost(player: Player, map1: GameMap, map2: GameMap): Int {
            return if (player.getAccount()!!.isSubscribeWithoutCondition()) 10 else 10 * abs((abs(map2.x - map1.x) + abs(map2.y - map1.y) - 1))
        }

        @JvmStatic
        fun applyResistancesOnDamage(elementID: Int, damage0: Int, target: Fighter, addPVPRes: Boolean): Int {
            var damage = damage0
            val resInfo = ResEffectInfo.forElement(elementID)

            var resF = target.getTotalStats().getEffect(resInfo.fixedElem) + target.getTotalStats().getEffect(resInfo.fixed)
            var resP = target.getTotalStats().getEffect(resInfo.percentElem)

            if (addPVPRes) {
                resF += target.getTotalStats()[resInfo.fixedElemPvP]
                resP += target.getTotalStats()[resInfo.percentElemPvP]
            }

            if (target.player != null) {
                // CAP 50% Players
                resP = Math.min(resP, 50)
            }
            // Apply to damage
            damage -= resF
            damage = (damage * (1 - resP / 100.0)).toInt()
            return damage
        }

        @JvmStatic
        fun getArmorResist(target: Fighter, statID: Int): Int {
            var armor = 0
            for (SE in target.getBuffsByEffectID(265)) {
                val fighter: Fighter

                when (SE.spell) {
                    452, 1 -> {//Armure incandescente
                        //Si pas element feu, on ignore l'armure
                        if (statID != Constant.ELEMENT_FEU)
                            continue
                        //Les stats du féca sont prises en compte
                        fighter = SE.caster!!
                    }
                    453, 6 -> {//Armure Terrestre
                        //Si pas element terre/neutre, on ignore l'armure
                        if (statID != Constant.ELEMENT_TERRE
                            && statID != Constant.ELEMENT_NEUTRE
                        )
                            continue
                        //Les stats du féca sont prises en compte
                        fighter = SE.caster!!
                    }
                    454, 14 -> {//Armure Venteuse
                        //Si pas element air, on ignore l'armure
                        if (statID != Constant.ELEMENT_AIR)
                            continue
                        //Les stats du féca sont prises en compte
                        fighter = SE.caster!!
                    }
                    451, 18 -> {//Armure aqueuse
                        //Si pas element eau, on ignore l'armure
                        if (statID != Constant.ELEMENT_EAU)
                            continue
                        //Les stats du féca sont prises en compte
                        fighter = SE.caster!!
                    }
                    else -> //Dans les autres cas on prend les stats de la cible et on ignore l'element de l'attaque
                        fighter = target
                }
                val intell = fighter.getTotalStats().getEffect(Constant.STATS_ADD_INTE)
                var carac = 0
                when (statID) {
                    Constant.ELEMENT_AIR -> carac = fighter.getTotalStats().getEffect(Constant.STATS_ADD_AGIL)
                    Constant.ELEMENT_FEU -> carac = fighter.getTotalStats().getEffect(Constant.STATS_ADD_INTE)
                    Constant.ELEMENT_EAU -> carac = fighter.getTotalStats().getEffect(Constant.STATS_ADD_CHAN)
                    Constant.ELEMENT_NEUTRE, Constant.ELEMENT_TERRE -> carac = fighter.getTotalStats().getEffect(Constant.STATS_ADD_FORC)
                }
                val value = SE.value
                val a = (value * (100 + intell / 2 + carac / 2)
                        / 100)
                armor += a
            }
            for (SE in target.getBuffsByEffectID(105)) {
                val intell = target.getTotalStats().getEffect(Constant.STATS_ADD_INTE)
                var carac = 0
                when (statID) {
                    Constant.ELEMENT_AIR -> carac = target.getTotalStats().getEffect(Constant.STATS_ADD_AGIL)
                    Constant.ELEMENT_FEU -> carac = target.getTotalStats().getEffect(Constant.STATS_ADD_INTE)
                    Constant.ELEMENT_EAU -> carac = target.getTotalStats().getEffect(Constant.STATS_ADD_CHAN)
                    Constant.ELEMENT_NEUTRE, Constant.ELEMENT_TERRE -> carac = target.getTotalStats().getEffect(Constant.STATS_ADD_FORC)
                }
                val value = SE.value
                val a = (value * (100 + intell / 2 + carac / 2)
                        / 100)
                armor += a
            }
            return armor
        }

        @JvmStatic
        fun getGuildXpWin(perso: Fighter, xpWin: AtomicReference<Long>): Long {
            if (perso.player == null)
                return 0
            if (perso.player!!.guildMember == null)
                return 0

            val gm = perso.player!!.guildMember

            val xp = xpWin.get().toDouble()
            val Lvl = perso.getLvl().toDouble()
            val LvlGuild = perso.player!!.getGuild()!!.lvl.toDouble()
            val pXpGive = gm!!.xpGive.toDouble() / 100

            val maxP = xp * pXpGive * 0.10 //Le maximum donné à la guilde est 10% du montant prélevé sur l'xp du combat
            val diff = abs(Lvl - LvlGuild) //Calcul l'écart entre le niveau du personnage et le niveau de la guilde
            var toGuild: Double
            if (diff >= 70) {
                toGuild = maxP * 0.10 //Si l'écart entre les deux level est de 70 ou plus, l'experience donnée a la guilde est de 10% la valeur maximum de don
            } else if (diff >= 31 && diff <= 69) {
                toGuild = maxP - ((maxP * 0.10) * (floor((diff + 30) / 10)))
            } else if (diff >= 10 && diff <= 30) {
                toGuild = maxP - ((maxP * 0.20) * (floor(diff / 10)))
            } else { //Si la différence est [0,9]
                toGuild = maxP
            }
            xpWin.set((xp - xp * pXpGive).toLong())
            if (toGuild > 1_000_000)
                toGuild = 1_486_215.0
            return Math.round(toGuild)
        }

        @JvmStatic
        fun getMountXpWin(perso: Fighter, xpWin: AtomicReference<Long>): Long {
            if (perso.player == null)
                return 0
            if (perso.player!!.mount == null)
                return 0

            val diff = abs(perso.getLvl() - perso.player!!.mount!!.level)

            var coeff = 0.0
            val xp = xpWin.get().toDouble()
            val pToMount = perso.player!!.mountXpGive.toDouble() / 100 + 0.2

            if (diff >= 0 && diff <= 9)
                coeff = 0.1
            else if (diff >= 10 && diff <= 19)
                coeff = 0.08
            else if (diff >= 20 && diff <= 29)
                coeff = 0.06
            else if (diff >= 30 && diff <= 39)
                coeff = 0.04
            else if (diff >= 40 && diff <= 49)
                coeff = 0.03
            else if (diff >= 50 && diff <= 59)
                coeff = 0.02
            else if (diff >= 60 && diff <= 69)
                coeff = 0.015
            else
                coeff = 0.01

            if (pToMount > 0.2)
                xpWin.set((xp - (xp * (pToMount - 0.2))).toLong())

            return Math.round(xp * pToMount * coeff)
        }

        @JvmStatic
        fun getKamasWin(i: Fighter, winners: ArrayList<Fighter>,
                        maxk0: Int, mink: Int): Int {
            val maxk = maxk0 + 1
            val rkamas = (Math.random() * (maxk - mink)).toInt() + mink
            return Math.round(rkamas * Config.rateKamas.toFloat())
        }

        @JvmStatic
        fun getKamasWinPerco(maxk0: Int, mink: Int): Int {
            val maxk = maxk0 + 1
            val rkamas = (Math.random() * (maxk - mink)).toInt() + mink
            return Math.round(rkamas * Config.rateKamas.toFloat())
        }

        @JvmStatic
        fun decompPierreAme(toDecomp: GameObject): Couple<Int, Int> {
            val stats = toDecomp.encodeStats().split("#")
            val lvlMax = stats[3].toInt(16)
            val chance = stats[1].toInt(16)
            return Couple(chance, lvlMax)
        }

        @JvmStatic
        fun totalCaptChance(pierreChance: Int, p: Player): Int {
            var sortChance = 0

            when (p.getSortStatBySortIfHas(413)!!.level) {
                1 -> sortChance = 1
                2 -> sortChance = 3
                3 -> sortChance = 6
                4 -> sortChance = 10
                5 -> sortChance = 15
                6 -> sortChance = 25
            }

            return sortChance + pierreChance
        }

        @JvmStatic
        fun spellCost(nb: Int): Int {
            var total = 0
            for (i in 1 until nb) {
                total += i
            }

            return total
        }

        @JvmStatic
        fun getLoosEnergy(lvl: Int, isAgression: Boolean, isPerco: Boolean): Int {
            // TODO: add grade
            var returned = 10 * lvl
            if (isAgression)
                returned *= (7 / 4)
            if (isPerco)
                returned += 3000
            return returned
        }

        @JvmStatic
        fun totalAppriChance(Amande: Boolean, Rousse: Boolean,
                             Doree: Boolean, p: Player): Int {
            var sortChance = 0
            var ddChance = 0
            when (p.getSortStatBySortIfHas(414)!!.level) {
                1 -> sortChance = 15
                2 -> sortChance = 20
                3 -> sortChance = 25
                4 -> sortChance = 30
                5 -> sortChance = 35
                6 -> sortChance = 45
            }
            if (Amande || Rousse)
                ddChance = 15
            if (Doree)
                ddChance = 5
            return sortChance + ddChance
        }

        @JvmStatic
        fun getCouleur(Amande: Boolean, Rousse: Boolean, Doree: Boolean): Int {
            val Couleur = 0
            if (Amande && !Rousse && !Doree)
                return 20
            if (Rousse && !Amande && !Doree)
                return 10
            if (Doree && !Amande && !Rousse)
                return 18

            if (Amande && Rousse && !Doree) {
                val Chance = getRandomValue(1, 2)
                if (Chance == 1)
                    return 20
                if (Chance == 2)
                    return 10
            }
            if (Amande && !Rousse && Doree) {
                val Chance = getRandomValue(1, 2)
                if (Chance == 1)
                    return 20
                if (Chance == 2)
                    return 18
            }
            if (!Amande && Rousse && Doree) {
                val Chance = getRandomValue(1, 2)
                if (Chance == 1)
                    return 18
                if (Chance == 2)
                    return 10
            }
            if (Amande && Rousse && Doree) {
                val Chance = getRandomValue(1, 3)
                if (Chance == 1)
                    return 20
                if (Chance == 2)
                    return 10
                if (Chance == 3)
                    return 18
            }
            return Couleur
        }

        @JvmStatic
        fun calculEnergieLooseForToogleMount(pts: Int): Int {
            if (pts <= 170)
                return 4
            if (pts >= 171 && pts < 180)
                return 5
            if (pts >= 180 && pts < 200)
                return 6
            if (pts >= 200 && pts < 210)
                return 7
            if (pts >= 210 && pts < 220)
                return 8
            if (pts >= 220 && pts < 230)
                return 10
            if (pts >= 230 && pts <= 240)
                return 12
            return 10
        }

        @JvmStatic
        fun getLvlDopeuls(lvl: Int): Int//Niveau du dopeul à combattre
        {
            if (lvl < 20)
                return 20
            if (lvl > 19 && lvl < 40)
                return 40
            if (lvl > 39 && lvl < 60)
                return 60
            if (lvl > 59 && lvl < 80)
                return 80
            if (lvl > 79 && lvl < 100)
                return 100
            if (lvl > 99 && lvl < 120)
                return 120
            if (lvl > 119 && lvl < 140)
                return 140
            if (lvl > 139 && lvl < 160)
                return 160
            if (lvl > 159 && lvl < 180)
                return 180
            if (lvl > 180)
                return 200
            return 200
        }

        @JvmStatic
        fun calculChanceByElement(lvlJob: Int, lvlObject: Int,
                                  lvlRune: Int): Int {
            var K = 1
            if (lvlRune == 1)
                K = 100
            else if (lvlRune == 25)
                K = 175
            else if (lvlRune == 50)
                K = 350
            return lvlJob * 100 / (K + lvlObject)
        }

        @JvmStatic
        fun chanceFM(WeightTotalBase: Int,
                     WeightTotalBaseMin: Int, currentWeithTotal: Int,
                     currentWeightStats: Int, weight: Int, diff: Int, coef: Float,
                     maxStat: Int, minStat: Int, actualStat: Int, x: Float,
                     bonusRune: Boolean, statsAdd: Int): ArrayList<Int> {
            val chances = ArrayList<Int>()
            var c = 1f
            val m1 = (maxStat - (actualStat + statsAdd)).toFloat()
            val m2 = (maxStat - minStat).toFloat()
            if ((1 - (m1 / m2)) > 1.0)
                c = (1 - ((1 - (m1 / m2)) / 2)) / 2
            else if ((1 - (m1 / m2)) > 0.8)
                c = 1 - ((1 - (m1 / m2)) / 2)
            if (c < 0)
                c = 0f
            // la variable c reste à 1 si le jet ne depasse pas 80% sinon il diminue très fortement. Si le jet dépasse 100% alors il diminue encore plus.

            val moyenne = floor((WeightTotalBase
                    - ((WeightTotalBase - WeightTotalBaseMin) / 2)).toDouble()).toInt()

            var mStat = (moyenne.toFloat() / currentWeithTotal.toFloat()) // Si l'item est un bon jet dans l'ensemble, diminue les chances sinon l'inverse.

            if (mStat > 1.2)
                mStat = 1.2f
            val a = ((((((WeightTotalBase + diff) * coef) * mStat) * c) * x) * Config.rateFm)
            var b = (sqrt((currentWeithTotal + currentWeightStats).toDouble()) + weight).toFloat()
            if (b < 1.0)
                b = 1.0f

            var p1 = floor((a / b).toDouble()).toInt() // Succes critique
            var p2 = 0 // Succes neutre
            var p3 = 0 // Echec critique
            if (bonusRune)
                p1 += 20
            if (p1 < 1) {
                p1 = 1
                p2 = 0
                p3 = 99
            } else if (p1 > 100) {
                p1 = 66
                p2 = 34
            } else if (p1 > 66)
                p1 = 66

            if (p2 == 0 && p3 == 0) {
                p2 = floor(a
                        / (sqrt((currentWeithTotal + currentWeightStats).toDouble()))).toInt()
                if (p2 > (100 - p1))
                    p2 = (100 - p1)
                if (p2 > 50)
                    p2 = 50
            }
            chances.add(0, p1)
            chances.add(1, p2)
            chances.add(2, p3)
            return chances
        }

        @JvmStatic
        fun convertToDate(time: Long): String {
            var hexDate = "#"
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val date = formatter.format(time)

            val split = date.split(Regex("\\s"))

            val split0 = split[0].split("-")
            hexDate += Integer.toHexString(split0[0].toInt()) + "#"
            val mois = split0[1].toInt() - 1
            val jour = split0[2].toInt()
            hexDate += Integer.toHexString(((if (mois < 10) "0$mois" else mois).toString()
                    + "" + (if (jour < 10) "0$jour" else jour)).toInt()) + "#"

            val split1 = split[1].split(":")
            val heure = split1[0] + split1[1]
            hexDate += Integer.toHexString(heure.toInt())
            return hexDate
        }

        @JvmStatic
        fun getXpStalk(lvl: Int): Int {
            when (lvl) {
                in 50..59 -> return 65000
                in 60..69 -> return 90000
                in 70..79 -> return 120000
                in 80..89 -> return 160000
                in 90..99 -> return 210000
                in 100..109 -> return 270000
                in 110..119 -> return 350000
                in 120..129 -> return 440000
                in 130..139 -> return 540000
                in 140..149 -> return 650000
                in 150..154 -> return 760000
                in 155..159 -> return 880000
                in 160..164 -> return 1000000
                in 165..169 -> return 1130000
                in 170..174 -> return 1300000
                in 175..179 -> return 1500000
                in 180..184 -> return 1700000
                in 185..189 -> return 2000000
                in 190..194 -> return 2500000
                in 195..200 -> return 3000000
            }
            return 65000
        }

        @JvmStatic
        fun translateMsg(msg0: String): String {
            var msg = msg0
            var alpha = "a b c d e f g h i j k l n o p q r s t u v w x y z é è à ç & û â ê ô î ä ë ü ï ö"
            for (i in alpha.splitJ(" "))
                msg = msg.replace(i, "m")
            alpha = "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z Ë Ü Ä Ï Ö Â Ê Û Î Ô"
            for (i in alpha.splitJ(" "))
                msg = msg.replace(i, "H")
            return msg
        }

        //region Formulas fight honor
        @JvmStatic
        fun calculHonorWin(winners: ArrayList<Fighter>?, loosers: ArrayList<Fighter>?, current: Fighter, prism: Boolean): Int {
            val player = current.player
            if (player == null || winners == null || loosers == null) return 0
            if (prism && loosers.size == 1 && loosers[0].prism != null) return 0

            val factor = Config.rateHonor * World.world.getConquestBonus(current.player)
            // [0] = winners, [1] loosers;
            val levelsTotalDivide = intArrayOf(countFightersLevel(winners), countFightersLevel(loosers))
            val levelsTotal = intArrayOf(countFightersLevel(winners) * winners.size, countFightersLevel(loosers) * loosers.size)
            val gradesTotalDivide = intArrayOf(countFightersGrade(winners), countFightersGrade(loosers))
            var diffLevels = levelsTotalDivide[0] - levelsTotalDivide[1]
            var diffGrades = gradesTotalDivide[0] - gradesTotalDivide[1]

            // Condition for winners & loosers (the same)
            if (levelsTotalDivide[0] > levelsTotalDivide[1] + 20)
                return 0

            if (winners.contains(current)) { // He win
                // Condition for the winners
                if (levelsTotal[0] - levelsTotal[1] > (20 * winners.size))
                    return 0
                if (levelsTotalDivide[0] < levelsTotalDivide[1] - 20)
                    diffLevels = -20
                return (Math.round(((100 * factor) - diffLevels + (3 * -diffGrades) + 15 * loosers.size)) * (if (player.getAccount()!!.isSubscribeWithoutCondition()) 2 else 1)).toInt()
            } else { // He loose
                // Variables for the loosers
                diffGrades = player.getGrade() - gradesTotalDivide[0]
                var multiplicator = 1.0

                // Condition for the loosers
                if (levelsTotal[0] - levelsTotal[1] > (20 * loosers.size))
                    return 0
                if (diffLevels <= 0) diffLevels = 1
                if (diffGrades <= 0) diffGrades = 1

                // Variable scalable
                when (player.getGrade()) {
                    3 -> multiplicator = 1.5
                    4 -> multiplicator = 2.0
                    5 -> multiplicator = 2.5
                    6 -> multiplicator = 3.0
                    7 -> multiplicator = 4.0
                    8 -> multiplicator = 6.0
                    9 -> multiplicator = 8.0
                    10 -> multiplicator = 12.0
                    /*case 3: multiplicator = 1.1; break;
                    case 4: multiplicator = 1.2; break;
                    case 5: multiplicator = 1.3; break;
                    case 6: multiplicator = 1.5; break;
                    case 7: multiplicator = 2.0; break;
                    case 8: multiplicator = 3.0; break;
                    case 9: multiplicator = 4.0; break;
                    case 10: multiplicator = 5.0; break;*/
                }
                return Math.round(((-100 * factor) - diffLevels - (3 * diffGrades) - 5 * winners.size) * multiplicator).toInt()
            }
        }

        private fun countFightersLevel(fighters: ArrayList<Fighter>): Int {
            var total = 0
            for (fighter in fighters) if (fighter != null && fighter.player != null) total += fighter.getLvl()
            return if (fighters.size == 0) total else total / fighters.size
        }

        private fun countFightersGrade(fighters: ArrayList<Fighter>): Int {
            var total = 0
            for (fighter in fighters) if (fighter != null && fighter.player != null) total += fighter.player!!.getGrade()
            return total / fighters.size
        }
        //endregion

        //region Formule esquive Pa/Pm
        @JvmStatic
        fun getPointsLost(type: Char, value: Int, caster: Fighter, target: Fighter): Int {
            var esquiveC = (if (type == 'a') caster.getTotalStatsLessBuff()!!.getEffect(Constant.STATS_ADD_ADODGE) else caster.getTotalStatsLessBuff()!!.getEffect(Constant.STATS_ADD_MDODGE)).toFloat()
            var esquiveT = (if (type == 'a') target.getTotalStats().getEffect(Constant.STATS_ADD_ADODGE) else target.getTotalStats().getEffect(Constant.STATS_ADD_MDODGE)).toFloat()
            var ptsMax = (if (type == 'a') target.getTotalStatsLessBuff()!!.getEffect(Constant.STATS_ADD_PA) else target.getTotalStatsLessBuff()!!.getEffect(Constant.STATS_ADD_PM)).toFloat()
            if (target.mob != null)
                ptsMax = (if (type == 'a') target.mob!!.pa else target.mob!!.pm).toFloat()
            var loose = 0

            for (i in 0 until value) {
                val pts = (if (type == 'a') target.getPa() else target.getPm()) - loose

                if (esquiveT <= 0) esquiveT = 1f
                if (esquiveC <= 0) esquiveC = 1f

                val result = Math.round((pts / ptsMax * esquiveC / esquiveT * 0.5).toFloat() * 100)

                /*
                pourcentage de chance de retirer un PA =
                PA ou PM restants de la cible / PA ou PM totaux de la cible * Retrait du lanceur / esquive de la cible.  1/2
                 */
                val jet = getRandomValue(0, 100)

                if (result >= jet) loose++
            }
            return loose
        }
        //endregion

        //region Line of sight
        @JvmStatic
        fun checkLos(map: GameMap?, castID: Short, targetID: Short): Boolean {
            if (map == null || castID == targetID)
                return true

            // Only compute the coordinates once
            var curX = OrthogonalProj.getOrthX(map.data.width, castID.toInt()).toFloat()
            var curY = OrthogonalProj.getOrthY(map.data.width, castID.toInt()).toFloat()
            val dstX = OrthogonalProj.getOrthX(map.data.width, targetID.toInt())
            val dstY = OrthogonalProj.getOrthY(map.data.width, targetID.toInt())

            // Get the offset between the cells
            val offX = (dstX - curX.toInt())
            val offY = (dstY - curY.toInt())

            var steps = OrthogonalProj.getCellsDistance(map.data.width, castID.toInt(), targetID.toInt()).toFloat()

            steps = Math.max(1f, steps)
            curX += .5f
            curY += .5f

            // Raycast & losCheck
            var t = 0
            while (t < steps) {
                val xFloored = floor(curX.toDouble()).toInt()
                val yFloored = floor(curY.toDouble()).toInt()
                val cellId = OrthogonalProj.getOrthCellID(map.data.width, xFloored, yFloored).toShort()

                // Ignore blocking cell if we are between two cells
                if (curX == xFloored.toFloat() && curY == yFloored.toFloat()) {
                    t++
                    curX += offX / steps
                    curY += offY / steps
                    continue
                }

                // We know the cell is busy cause the caster is on it
                // We also want to ignore the last cell cause the target blocks it anyway (But we should not reach it here anyway)
                if (cellId == targetID || cellId == castID) {
                    t++
                    curX += offX / steps
                    curY += offY / steps
                    continue
                }

                val cell = map.getCase(cellId.toInt())
                if (cell != null) {
                    val fighter = cell.firstFighter
                    if (!map.data.lineOfSight(cellId.toInt()) || (fighter != null && !fighter.isHidden()))
                        return false
                }
                t++
                curX += offX / steps
                curY += offY / steps
            }

            return true
        }
        //endregion
    }
}
