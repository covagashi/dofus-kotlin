package org.starloco.locos.job

import org.starloco.locos.common.Formulas

import java.util.ArrayList

object JobConstant {


        //Jobs
    const val JOB_BUCHERON = 2
    const val JOB_F_EPEE = 11
    const val JOB_S_ARC = 13
    const val JOB_F_MARTEAU = 14
    const val JOB_CORDONIER = 15
    const val JOB_BIJOUTIER = 16
    const val JOB_F_DAGUE = 17
    const val JOB_S_BATON = 18
    const val JOB_S_BAGUETTE = 19
    const val JOB_F_PELLE = 20
    const val JOB_MINEUR = 24
    const val JOB_BOULANGER = 25
    const val JOB_ALCHIMISTE = 26
    const val JOB_TAILLEUR = 27
    const val JOB_PAYSAN = 28
    const val JOB_F_HACHES = 31
    const val JOB_PECHEUR = 36
    const val JOB_CHASSEUR = 41
    const val JOB_FM_DAGUE = 43
    const val JOB_FM_EPEE = 44
    const val JOB_FM_MARTEAU = 45
    const val JOB_FM_PELLE = 46
    const val JOB_FM_HACHES = 47
    const val JOB_SM_ARC = 48
    const val JOB_SM_BAGUETTE = 49
    const val JOB_SM_BATON = 50
    const val JOB_BOUCHER = 56
    const val JOB_POISSONNIER = 58
    const val JOB_F_BOUCLIER = 60
    const val JOB_CORDOMAGE = 62
    const val JOB_JOAILLOMAGE = 63
    const val JOB_COSTUMAGE = 64
    const val JOB_BRICOLEUR = 65
        //INTERACTIVE OBJET
    const val IOBJECT_STATE_FULL = 1
    const val IOBJECT_STATE_EMPTYING = 2
    const val IOBJECT_STATE_EMPTY = 3
    const val IOBJECT_STATE_EMPTY2 = 4
    const val IOBJECT_STATE_FULLING = 5

        //Action de M�tier {skillID,objetRecolt�,objSp�cial}
    @JvmField
    val JOB_ACTION: Array<IntArray> = arrayOf(
            
            intArrayOf(101), intArrayOf(6, 303), intArrayOf(39, 473), intArrayOf(40, 476), intArrayOf(10, 460), intArrayOf(141, 2357), intArrayOf(139, 2358), intArrayOf(37, 471), intArrayOf(154, 7013), intArrayOf(33, 461), intArrayOf(41, 474), intArrayOf(34, 449), intArrayOf(174, 7925), intArrayOf(155, 7016), intArrayOf(38, 472), intArrayOf(35, 470), intArrayOf(158, 7014),
            
            intArrayOf(48), intArrayOf(32), intArrayOf(24, 312), intArrayOf(25, 441), intArrayOf(26, 442), intArrayOf(28, 443), intArrayOf(56, 445), intArrayOf(162, 7032), intArrayOf(55, 444), intArrayOf(29, 350), intArrayOf(31, 446), intArrayOf(30, 313), intArrayOf(161, 7033),
            
            intArrayOf(133),
            
            intArrayOf(124, 1782, 1844, 603),  
            intArrayOf(125, 1844, 603, 1847, 1794), 
            intArrayOf(126, 603, 1847, 1794, 1779), 
            intArrayOf(127, 1847, 1794, 1779, 1801), 
            
            intArrayOf(128, 598, 1757, 1750), 
            intArrayOf(129, 1757, 1805, 600), 
            intArrayOf(130, 1805, 1750, 1784, 600), 
            intArrayOf(131, 600, 1805, 602, 1784), 
            
            intArrayOf(136, 2187), intArrayOf(140, 1759), intArrayOf(140, 1799),
            
            intArrayOf(23), intArrayOf(68, 421), intArrayOf(69, 428), intArrayOf(71, 395), intArrayOf(72, 380), intArrayOf(73, 593), intArrayOf(74, 594), intArrayOf(160, 7059),
            
            intArrayOf(122), intArrayOf(47), intArrayOf(45, 289), intArrayOf(53, 400), intArrayOf(57, 533), intArrayOf(46, 401), intArrayOf(50, 423), intArrayOf(52, 532), intArrayOf(159, 7018), intArrayOf(58, 405), intArrayOf(54, 425),
            
            intArrayOf(109), intArrayOf(27),
            
            intArrayOf(135),
            
            intArrayOf(134),
            
            intArrayOf(132),
            
            intArrayOf(64), intArrayOf(123), intArrayOf(63),
            
            intArrayOf(11), intArrayOf(12),
            
            intArrayOf(13), intArrayOf(14),
            
            intArrayOf(145), intArrayOf(20),
            
            intArrayOf(144), intArrayOf(19),
            
            intArrayOf(142), intArrayOf(18),
            
            intArrayOf(146), intArrayOf(21),
            
            intArrayOf(65), intArrayOf(143),
            
            intArrayOf(115),
            
            intArrayOf(1),
            
            intArrayOf(116),
            
            intArrayOf(113),
            
            intArrayOf(117),
            
            intArrayOf(120),
            
            intArrayOf(119),
            
            intArrayOf(118),
            
            intArrayOf(165), intArrayOf(166), intArrayOf(167),
            
            intArrayOf(163), intArrayOf(164),
            
            intArrayOf(169), intArrayOf(168),
            
            intArrayOf(171), intArrayOf(182),
            
            intArrayOf(15), intArrayOf(149),
            
            intArrayOf(17), intArrayOf(147),
            
            intArrayOf(16), intArrayOf(148),
            
            intArrayOf(156),
            
            intArrayOf(151),
            
            intArrayOf(110),
            
            intArrayOf(121),
            
            intArrayOf(22)

    )

    @JvmField
    val JOB_PROTECTORS: Array<IntArray> = arrayOf(intArrayOf(782, 472), intArrayOf(684, 289), intArrayOf(684, 2018), intArrayOf(685, 400), intArrayOf(685, 2032), intArrayOf(686, 533), intArrayOf(686, 1671), intArrayOf(687, 401), intArrayOf(687, 2021), intArrayOf(688, 423), intArrayOf(688, 2026), intArrayOf(689, 532), intArrayOf(689, 2029), intArrayOf(690, 7018), intArrayOf(691, 405), intArrayOf(692, 425), intArrayOf(692, 2035), intArrayOf(693, 312), intArrayOf(694, 441), intArrayOf(695, 442), intArrayOf(696, 443), intArrayOf(697, 445), intArrayOf(698, 444), intArrayOf(699, 7032), intArrayOf(700, 350), intArrayOf(701, 446), intArrayOf(702, 313), intArrayOf(703, 7033), intArrayOf(704, 421), intArrayOf(705, 428), intArrayOf(706, 395), intArrayOf(707, 380), intArrayOf(708, 593), intArrayOf(709, 594), intArrayOf(710, 7059), intArrayOf(711, 303), intArrayOf(712, 473), intArrayOf(713, 476), intArrayOf(714, 460), intArrayOf(715, 2358), intArrayOf(716, 2357), intArrayOf(717, 471), intArrayOf(718, 461), intArrayOf(719, 7013), intArrayOf(720, 7925), intArrayOf(721, 474), intArrayOf(722, 449), intArrayOf(723, 7016), intArrayOf(724, 470), intArrayOf(725, 7014), intArrayOf(726, 1782), intArrayOf(726, 1790), intArrayOf(727, 607), intArrayOf(727, 1844), intArrayOf(727, 1846), intArrayOf(728, 603), intArrayOf(729, 598), intArrayOf(730, 1757), intArrayOf(730, 1759), intArrayOf(731, 1750), intArrayOf(732, 1847), intArrayOf(732, 1749), intArrayOf(733, 1794), intArrayOf(733, 1796), intArrayOf(734, 1805), intArrayOf(734, 1807), intArrayOf(735, 600), intArrayOf(735, 1799), intArrayOf(736, 1779), intArrayOf(736, 1792), intArrayOf(737, 1784), intArrayOf(737, 1788), intArrayOf(738, 1801), intArrayOf(738, 1803), intArrayOf(739, 602), intArrayOf(739, 1853))

    @JvmStatic
    fun getTotalCaseByJobLevel(lvl: Int): Int {
            if (lvl < 10) return 2
            if (lvl == 100) return 9
            return (lvl / 20) + 3
    }

    @JvmStatic
    fun getChanceForMaxCase(lvl: Int): Int {
            if(lvl == 100)
            return 99
            if (lvl < 10)
            return 50
            return 54 + ((lvl / 10) - 1) * 5
    }

    @JvmStatic
    fun isJobAction(a: Int): Boolean {
            for (aJOB_ACTION in JOB_ACTION)
            if (aJOB_ACTION[0] == a)
            return true
            return false
    }

    @JvmStatic
    fun getObjectByJobSkill(skID: Int): Int {
            for (aJOB_ACTION in JOB_ACTION) {
            if (aJOB_ACTION[0] == skID) {
            if (aJOB_ACTION.size > 2) {
            return aJOB_ACTION[Formulas.getRandomValue(1, aJOB_ACTION.size - 1)]
            } else if (aJOB_ACTION.size > 1)
            return aJOB_ACTION[1]
    }
    }
            return -1
    }

    @JvmStatic
    fun getChanceByNbrCaseByLvl(lvl: Int, nbr: Int): Int {
            if (nbr <= getTotalCaseByJobLevel(lvl) - 2)
            return 100//99.999... normalement, mais osef
            return getChanceForMaxCase(lvl)
    }

    @JvmStatic
    fun isMageJob(id: Int): Boolean {
            return (id > 42 && id < 51) || (id > 61 && id < 65)
    }

    @JvmStatic
    fun actionMetier(oficio: Int): String {
        when (oficio) {
            62 -> {
            return "163;164"
            }
            63 -> {
            return "169;168"
            }
            64 -> {
            return "165;166;167"
            }
            45 -> {
            return "116"
            }
            46 -> {
            return "117"
            }
            67 -> {
            return "115"
            }
            43 -> {
            return "1"
            }
            44 -> {
            return "113"
            }
            48 -> {
            return "118"
            }
            49 -> {
            return "119"
            }
            50 -> {
            return "120"
            }
        }
            return ""
    }

    @JvmStatic
    fun getProtectorLvl(lvl: Int): Int {
            if (lvl < 40)
            return 10
            if (lvl < 80)
            return 20
            if (lvl < 120)
            return 30
            if (lvl < 160)
            return 40
            if (lvl < 200)
            return 50
            return 50
    }

    @JvmStatic
    fun getPosActionsToJob(tID: Int, lvl: Int): ArrayList<JobAction> {
            val list = ArrayList<JobAction>()
            var timeWin = lvl * 100
            var dropWin = lvl / 5
            var bonus = if (lvl == 100) 5 else 0
            var min = 1 + bonus

        when (tID) {
            JOB_BIJOUTIER -> {
            //Faire Anneau
            list.add(JobAction(11, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            //Faire Amullette
            list.add(JobAction(12, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            }
            JOB_TAILLEUR -> {
            //Faire Sac
            list.add(JobAction(64, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            //Faire Cape
            list.add(JobAction(123, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            //Faire Chapeau
            list.add(JobAction(63, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            }
            JOB_F_BOUCLIER -> {
            //Forger Bouclier
            list.add(JobAction(156, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            }
            JOB_BRICOLEUR -> {
            //Faire clef
            list.add(JobAction(171, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            //Faire objet brico
            list.add(JobAction(182, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            }
            JOB_CORDONIER -> {
            //Faire botte
            list.add(JobAction(13, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            //Faire ceinture
            list.add(JobAction(14, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            }
            JOB_S_ARC -> {
            //Sculter Arc
            list.add(JobAction(15, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            //ReSculter Arc
            list.add(JobAction(149, 3, 0, true, getChanceForMaxCase(lvl), -1))
            }
            JOB_S_BATON -> {
            //Sculter Baton
            list.add(JobAction(17, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            //ReSculter Baton
            list.add(JobAction(147, 3, 0, true, getChanceForMaxCase(lvl), -1))
            }
            JOB_S_BAGUETTE -> {
            //Sculter Baguette
            list.add(JobAction(16, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            //ReSculter Baguette
            list.add(JobAction(148, 3, 0, true, getChanceForMaxCase(lvl), -1))
            }
            JOB_CORDOMAGE -> {
            //FM Bottes
            list.add(JobAction(163, 3, 0, true, lvl, 0))
            //FM Ceinture
            list.add(JobAction(164, 3, 0, true, lvl, 0))
            }
            JOB_JOAILLOMAGE -> {
            //FM Anneau
            list.add(JobAction(169, 3, 0, true, lvl, 0))
            //FM  Amullette
            list.add(JobAction(168, 3, 0, true, lvl, 0))
            }
            JOB_COSTUMAGE -> {
            //FM Chapeau
            list.add(JobAction(165, 3, 0, true, lvl, 0))
            //FM Cape
            list.add(JobAction(167, 3, 0, true, lvl, 0))
            //FM Sac
            list.add(JobAction(166, 3, 0, true, lvl, 0))
            }
            JOB_F_EPEE -> {
            //Forger Ep�e
            list.add(JobAction(20, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            //Reforger Ep�e
            list.add(JobAction(145, 3, 0, true, getChanceForMaxCase(lvl), -1))
            }
            JOB_F_DAGUE -> {
            //Forger Dague
            list.add(JobAction(142, 3, 0, true, getChanceForMaxCase(lvl), -1))
            //Reforger Dague
            list.add(JobAction(18, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            }
            JOB_F_MARTEAU -> {
            //Forger Marteau
            list.add(JobAction(19, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            //Reforger Marteau
            list.add(JobAction(144, 3, 0, true, getChanceForMaxCase(lvl), -1))
            }
            JOB_F_PELLE -> {
            //Forger Pelle
            list.add(JobAction(21, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            //Reforger Pelle
            list.add(JobAction(146, 3, 0, true, getChanceForMaxCase(lvl), -1))
            }
            JOB_F_HACHES -> {
            //Forger Hache
            list.add(JobAction(65, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            //Reforger Hache
            list.add(JobAction(143, 3, 0, true, getChanceForMaxCase(lvl), -1))
            }
            JOB_FM_HACHES -> {
            //Reforger une hache
            list.add(JobAction(115, 3, 0, true, lvl, 0))
            }
            JOB_FM_DAGUE -> {
            //Reforger une dague
            list.add(JobAction(1, 3, 0, true, lvl, 0))
            }
            JOB_FM_EPEE -> {
            //Reforger une �p�e
            list.add(JobAction(113, 3, 0, true, lvl, 0))
            }
            JOB_FM_MARTEAU -> {
            //Reforger une marteau
            list.add(JobAction(116, 3, 0, true, lvl, 0))
            }
            JOB_FM_PELLE -> {
            //Reforger une pelle
            list.add(JobAction(117, 3, 0, true, lvl, 0))
            }
            JOB_SM_ARC -> {
            //Resculpter un arc
            list.add(JobAction(118, 3, 0, true, lvl, 0))
            }
            JOB_SM_BATON -> {
            //Resculpter un baton
            list.add(JobAction(120, 3, 0, true, lvl, 0))
            }
            JOB_SM_BAGUETTE -> {
            //Resculpter une baguette
            list.add(JobAction(119, 3, 0, true, lvl, 0))
            }
            JOB_CHASSEUR -> {
            //Pr�parer
            list.add(JobAction(132, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            }
            JOB_BOUCHER -> {
            //Pr�parer une Viande
            list.add(JobAction(134, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            }
            JOB_POISSONNIER -> {
            //Preparer un Poisson
            list.add(JobAction(135, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            }
            JOB_BOULANGER -> {
            //Cuir le Pain
            list.add(JobAction(27, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            //Faire des Bonbons
            list.add(JobAction(109, 3, 0, true, 100, -1))
            }
            JOB_MINEUR -> {
            if (lvl > 99) {
            //Miner Dolomite
            list.add(JobAction(161, min, -18 + dropWin, false, 12000 - timeWin, 60))
            }
            if (lvl > 79) {
            //Miner Or
            list.add(JobAction(30, min, -14 + dropWin, false, 12000 - timeWin, 55))
            }
            if (lvl > 69) {
            //Miner Bauxite
            list.add(JobAction(31, min, -12 + dropWin, false, 12000 - timeWin, 50))
            }
            if (lvl > 59) {
            //Miner Argent
            list.add(JobAction(29, min, -10 + dropWin, false, 12000 - timeWin, 40))
            }
            if (lvl > 49) {
            //Miner Etain
            list.add(JobAction(55, min, -8 + dropWin, false, 12000 - timeWin, 35))
            //Miner Silicate
            list.add(JobAction(162, min, -8 + dropWin, false, 12000 - timeWin, 35))
            }
            if (lvl > 39) {
            //Miner Mangan�se
            list.add(JobAction(56, min, -6 + dropWin, false, 12000 - timeWin, 30))
            }
            if (lvl > 29) {
            //Miner Kobalte
            list.add(JobAction(28, min, -4 + dropWin, false, 12000 - timeWin, 25))
            }
            if (lvl > 19) {
            //Miner Bronze
            list.add(JobAction(26, min, -2 + dropWin, false, 12000 - timeWin, 20))
            }
            if (lvl > 9) {
            //Miner Cuivre
            list.add(JobAction(25, min, dropWin, false, 12000 - timeWin, 15))
            }
            //Miner Fer
            list.add(JobAction(24, min, 2 + dropWin, false, 12000 - timeWin, 10))
            //Fondre
            list.add(JobAction(32, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            //Polir
            list.add(JobAction(48, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            }
            JOB_PECHEUR -> {
            if (lvl > 74) {
            //P�cher Poissons g�ants de mer
            list.add(JobAction(131, 0, 1, false, 12000 - timeWin, 35))
            }
            if (lvl > 69) {
            //P�cher Poissons g�ants de rivi�re
            list.add(JobAction(127, 0, 1, false, 12000 - timeWin, 35))
            }
            if (lvl > 49) {
            //P�cher Gros poissons de mers
            list.add(JobAction(130, 0, 1, false, 12000 - timeWin, 30))
            }
            if (lvl > 39) {
            //P�cher Gros poissons de rivi�re
            list.add(JobAction(126, 0, 1, false, 12000 - timeWin, 25))
            }
            if (lvl > 19) {
            //P�cher Poissons de mer
            list.add(JobAction(129, 0, 1, false, 12000 - timeWin, 20))
            }
            if (lvl > 9) {
            //P�cher Poissons de rivi�re
            list.add(JobAction(125, 0, 1, false, 12000 - timeWin, 15))
            }
            //P�cher Ombre Etrange
            list.add(JobAction(140, 0, 1, false, 12000 - timeWin, 50))
            //P�cher Pichon
            list.add(JobAction(136, 1, 1, false, 12000 - timeWin, 5))
            //P�cher Petits poissons de rivi�re
            list.add(JobAction(124, 0, 1, false, 12000 - timeWin, 10))
            //P�cher Petits poissons de mer
            list.add(JobAction(128, 0, 1, false, 12000 - timeWin, 10))
            //Vider
            list.add(JobAction(133, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            }
            JOB_ALCHIMISTE -> {
            if (lvl > 49) {
            //Cueillir Graine de Pandouille
            list.add(JobAction(160, min, -8 + dropWin, false, 12000 - timeWin, 35))
            //Cueillir Edelweiss
            list.add(JobAction(74, min, -8 + dropWin, false, 12000 - timeWin, 35))
            }
            if (lvl > 39) {
            //Cueillir Orchid�e
            list.add(JobAction(73, min, -6 + dropWin, false, 12000 - timeWin, 30))
            }
            if (lvl > 29) {
            //Cueillir Menthe
            list.add(JobAction(72, min, -4 + dropWin, false, 12000 - timeWin, 25))
            }
            if (lvl > 19) {
            //Cueillir Tr�fle
            list.add(JobAction(71, min, -2 + dropWin, false, 12000 - timeWin, 20))
            }
            if (lvl > 9) {
            //Cueillir Chanvre
            list.add(JobAction(69, min, dropWin, false, 12000 - timeWin, 15))
            }
            //Cueillir Lin
            list.add(JobAction(68, min, 2 + dropWin, false, 12000 - timeWin, 10))
            //Fabriquer une Potion
            list.add(JobAction(23, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            }
            JOB_BUCHERON -> {
            if (lvl > 99) {
            //Couper Bambou Sacr�
            list.add(JobAction(158, min, -18 + dropWin, false, 12000 - timeWin, 75))
            }
            if (lvl > 89) {
            //Couper Orme
            list.add(JobAction(35, min, -16 + dropWin, false, 12000 - timeWin, 70))
            }
            if (lvl > 79) {
            //Couper Charme
            list.add(JobAction(38, min, -14 + dropWin, false, 12000 - timeWin, 65))
            //Couper Bambou Sombre
            list.add(JobAction(155, min, -14 + dropWin, false, 12000 - timeWin, 65))
            }
            if (lvl > 74) {
            //Couper Kalyptus
            list.add(JobAction(174, min, -13 + dropWin, false, 12000 - timeWin, 55))
            }
            if (lvl > 69) {
            //Couper Eb�ne
            list.add(JobAction(34, min, -12 + dropWin, false, 12000 - timeWin, 50))
            }
            if (lvl > 59) {
            //Couper Merisier
            list.add(JobAction(41, min, -10 + dropWin, false, 12000 - timeWin, 45))
            }
            if (lvl > 49) {
            //Couper If
            list.add(JobAction(33, min, -8 + dropWin, false, 12000 - timeWin, 40))
            //Couper Bambou
            list.add(JobAction(154, min, -8 + dropWin, false, 12000 - timeWin, 40))
            }
            if (lvl > 39) {
            //Couper Erable
            list.add(JobAction(37, min, -6 + dropWin, false, 12000 - timeWin, 35))
            }
            if (lvl > 34) {
            //Couper Bombu
            list.add(JobAction(139, min, -5 + dropWin, false, 12000 - timeWin, 30))
            //Couper Oliviolet
            list.add(JobAction(141, min, -5 + dropWin, false, 12000 - timeWin, 30))
            }
            if (lvl > 29) {
            //Couper Ch�ne
            list.add(JobAction(10, min, -4 + dropWin, false, 12000 - timeWin, 25))
            }
            if (lvl > 19) {
            //Couper Noyer
            list.add(JobAction(40, min, -2 + dropWin, false, 12000 - timeWin, 20))
            }
            if (lvl > 9) {
            //Couper Ch�taignier
            list.add(JobAction(39, min, dropWin, false, 12000 - timeWin, 15))
            }
            //Couper Fr�ne
            list.add(JobAction(6, min, 2 + dropWin, false, 12000 - timeWin, 10))
            //Scie
            list.add(JobAction(101, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            }

            JOB_PAYSAN -> {
            if (lvl > 69) {
            //Faucher Chanvre
            list.add(JobAction(54, min, -12 + dropWin, false, 12000 - timeWin, 45))
            }
            if (lvl > 59) {
            //Faucher Malt
            list.add(JobAction(58, min, -10 + dropWin, false, 12000 - timeWin, 40))
            }
            if (lvl > 49) {
            //Faucher Riz
            list.add(JobAction(159, min, -8 + dropWin, false, 12000 - timeWin, 35))
            //Faucher Seigle
            list.add(JobAction(52, min, -8 + dropWin, false, 12000 - timeWin, 35))
            }
            if (lvl > 39) {
            //Faucher Lin
            list.add(JobAction(50, min, -6 + dropWin, false, 12000 - timeWin, 30))
            }
            if (lvl > 29) {
            //Faucher Houblon
            list.add(JobAction(46, min, -4 + dropWin, false, 12000 - timeWin, 25))
            }
            if (lvl > 19) {
            //Faucher Avoine
            list.add(JobAction(57, min, -2 + dropWin, false, 12000 - timeWin, 20))
            }
            if (lvl > 9) {
            //Faucher Orge
            list.add(JobAction(53, min, dropWin, false, 12000 - timeWin, 15))
            }
            //Faucher bl�
            list.add(JobAction(45, min, 2 + dropWin, false, 12000 - timeWin, 10))
            //Moudre
            list.add(JobAction(47, getTotalCaseByJobLevel(lvl), 0, true, getChanceForMaxCase(lvl), -1))
            //Egrener 100% 1 case tout le temps ?
            list.add(JobAction(122, 1, 0, true, 100, 10))
            }
    }
            return list
    }

    @JvmStatic
    fun getDistCanne(temp: Int): Int {
        when (temp) {
            8541, 6661, 596 -> { //1 to 2
            return 2
            }
            1866 -> { //1 to 3
            return 3
            }
            1865, 1864 -> { //1 to 4
            return 4
            }
            1867, 2188 -> { //1 to 5
            return 5
            }
            1863, 1862 -> { //1 to 6
            return 6
            }
            1868 -> { //1 to 7
            return 7
            }
            1861, 1860 -> { //1 to 8
            return 8
            }
            2366 -> { //1 to 9
            return 9
            }
        }
            return 0
    }

    @JvmStatic
    fun getPoissonRare(tID: Int): Int {
        when (tID) {
            598 -> { // Greu
            return 1786
            }
            600 -> { // Krala
            return 1799
            }
            602 -> { // Requin
            return 1853
            }
            603 -> { // Poisson Chaton
            return 1762
            }
            1750 -> { // Poisson Pan�
            return 1754
            }
            1757 -> { // Crabe Sourimi
            return 1759
            }
            1779 -> { // Bar
            return 1779
            }
            1785 -> { // Goujon
            return 1790
            }
            1784 -> { // Raie
            return 1788
            }
            1794 -> { // Carpe
            return 1796
            }
            1801 -> { // Perche
            return 1803
            }
            1805 -> { // Sardine
            return 1807
            }
            1844 -> { // Truite
            return 1846
            }
            1847 -> { // Brochet
            return 1849
            }
        }
            return -1
    }

}
