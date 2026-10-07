package org.starloco.locos.kernel

import java.util.HashMap
import java.util.Map

import org.starloco.locos.area.map.GameCase
import org.starloco.locos.area.map.GameMap
import org.starloco.locos.client.Player
import org.starloco.locos.client.other.Stats
import org.starloco.locos.common.Formulas
import org.starloco.locos.entity.mount.Mount
import org.starloco.locos.fight.spells.Spell.SortStats
import org.starloco.locos.game.world.World
import org.starloco.locos.`object`.ObjectTemplate
import org.starloco.locos.util.RandomStats

object Constant {
    //DEBUG
    const val DEBUG_MAP_LIMIT: Int = 30000
    //Fight
    const val TIME_START_FIGHT: Int = 45000
    @JvmField var TIME_BY_TURN: Int = 30000
    //Phoenix
    const val ALL_PHOENIX: String = "-11-54|2;-12|-41;-17|5;-9|25;-4|36;5|12;12|10;19|-10;13|-14;31|-43;0|-60;-3|-58;18|24;-43|27;-33"

    const val INCARNAM_SUPERAREA: Int = 3
    //ETAT
    const val ETAT_NEUTRE: Int = 0
    const val ETAT_SAOUL: Int = 1
    const val ETAT_CAPT_AME: Int = 2
    const val ETAT_PORTEUR: Int = 3
    const val ETAT_PEUREUX: Int = 4
    const val ETAT_DESORIENTE: Int = 5
    const val ETAT_ENRACINE: Int = 6
    const val ETAT_PESANTEUR: Int = 7
    const val ETAT_PORTE: Int = 8
    const val ETAT_MOTIV_SYLVESTRE: Int = 9
    const val ETAT_APPRIVOISEMENT: Int = 10
    const val ETAT_CHEVAUCHANT: Int = 11


    const val FIGHT_TYPE_CHALLENGE: Int = 0                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //D�fies
    const val FIGHT_TYPE_AGRESSION: Int = 1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //Aggros
    const val FIGHT_TYPE_CONQUETE: Int = 2                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //Conquete
    const val FIGHT_TYPE_DOPEUL: Int = 3                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //Dopeuls de temple
    const val FIGHT_TYPE_PVM: Int = 4                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //PvM
    const val FIGHT_TYPE_PVT: Int = 5
    const val FIGHT_TYPE_ROYAL: Int = 6


    //Percepteur
    const val FIGHT_STATE_INIT: Int = 1
    const val FIGHT_STATE_PLACE: Int = 2
    const val FIGHT_STATE_ACTIVE: Int = 3
    const val FIGHT_STATE_FINISHED: Int = 4


    //Items
    //Positions
    const val ITEM_POS_NO_EQUIPED: Int = -1
    const val ITEM_POS_AMULETTE: Int = 0
    const val ITEM_POS_ARME: Int = 1
    const val ITEM_POS_ANNEAU1: Int = 2
    const val ITEM_POS_CEINTURE: Int = 3
    const val ITEM_POS_ANNEAU2: Int = 4
    const val ITEM_POS_BOTTES: Int = 5
    const val ITEM_POS_COIFFE: Int = 6
    const val ITEM_POS_CAPE: Int = 7
    const val ITEM_POS_FAMILIER: Int = 8
    const val ITEM_POS_DOFUS1: Int = 9
    const val ITEM_POS_DOFUS2: Int = 10
    const val ITEM_POS_DOFUS3: Int = 11
    const val ITEM_POS_DOFUS4: Int = 12
    const val ITEM_POS_DOFUS5: Int = 13
    const val ITEM_POS_DOFUS6: Int = 14
    const val ITEM_POS_BOUCLIER: Int = 15
    const val ITEM_POS_DRAGODINDE: Int = 16
    //Objets dons, mutations, mal�diction, ..
    const val ITEM_POS_MUTATION: Int = 20
    const val ITEM_POS_ROLEPLAY_BUFF: Int = 21
    const val ITEM_POS_PNJ_SUIVEUR: Int = 24
    const val ITEM_POS_BENEDICTION: Int = 23
    const val ITEM_POS_MALEDICTION: Int = 22
    const val ITEM_POS_BONBON: Int = 25
    //Types
    const val ITEM_TYPE_AMULETTE: Int = 1
    const val ITEM_TYPE_ARC: Int = 2
    const val ITEM_TYPE_BAGUETTE: Int = 3
    const val ITEM_TYPE_BATON: Int = 4
    const val ITEM_TYPE_DAGUES: Int = 5
    const val ITEM_TYPE_EPEE: Int = 6
    const val ITEM_TYPE_MARTEAU: Int = 7
    const val ITEM_TYPE_PELLE: Int = 8
    const val ITEM_TYPE_ANNEAU: Int = 9
    const val ITEM_TYPE_CEINTURE: Int = 10
    const val ITEM_TYPE_BOTTES: Int = 11
    const val ITEM_TYPE_POTION: Int = 12
    const val ITEM_TYPE_PARCHO_EXP: Int = 13
    const val ITEM_TYPE_DONS: Int = 14
    const val ITEM_TYPE_RESSOURCE: Int = 15
    const val ITEM_TYPE_COIFFE: Int = 16
    const val ITEM_TYPE_CAPE: Int = 17
    const val ITEM_TYPE_FAMILIER: Int = 18
    const val ITEM_TYPE_HACHE: Int = 19
    const val ITEM_TYPE_OUTIL: Int = 20
    const val ITEM_TYPE_PIOCHE: Int = 21
    const val ITEM_TYPE_FAUX: Int = 22
    const val ITEM_TYPE_DOFUS: Int = 23
    const val ITEM_TYPE_QUETES: Int = 24
    const val ITEM_TYPE_DOCUMENT: Int = 25
    const val ITEM_TYPE_FM_POTION: Int = 26
    const val ITEM_TYPE_TRANSFORM: Int = 27
    const val ITEM_TYPE_BOOST_FOOD: Int = 28
    const val ITEM_TYPE_BENEDICTION: Int = 29
    const val ITEM_TYPE_MALEDICTION: Int = 30
    const val ITEM_TYPE_RP_BUFF: Int = 31
    const val ITEM_TYPE_PERSO_SUIVEUR: Int = 32
    const val ITEM_TYPE_PAIN: Int = 33
    const val ITEM_TYPE_CEREALE: Int = 34
    const val ITEM_TYPE_FLEUR: Int = 35
    const val ITEM_TYPE_PLANTE: Int = 36
    const val ITEM_TYPE_BIERE: Int = 37
    const val ITEM_TYPE_BOIS: Int = 38
    const val ITEM_TYPE_MINERAIS: Int = 39
    const val ITEM_TYPE_ALLIAGE: Int = 40
    const val ITEM_TYPE_POISSON: Int = 41
    const val ITEM_TYPE_BONBON: Int = 42
    const val ITEM_TYPE_POTION_OUBLIE: Int = 43
    const val ITEM_TYPE_POTION_METIER: Int = 44
    const val ITEM_TYPE_POTION_SORT: Int = 45
    const val ITEM_TYPE_FRUIT: Int = 46
    const val ITEM_TYPE_OS: Int = 47
    const val ITEM_TYPE_POUDRE: Int = 48
    const val ITEM_TYPE_COMESTI_POISSON: Int = 49
    const val ITEM_TYPE_PIERRE_PRECIEUSE: Int = 50
    const val ITEM_TYPE_PIERRE_BRUTE: Int = 51
    const val ITEM_TYPE_FARINE: Int = 52
    const val ITEM_TYPE_PLUME: Int = 53
    const val ITEM_TYPE_POIL: Int = 54
    const val ITEM_TYPE_ETOFFE: Int = 55
    const val ITEM_TYPE_CUIR: Int = 56
    const val ITEM_TYPE_LAINE: Int = 57
    const val ITEM_TYPE_GRAINE: Int = 58
    const val ITEM_TYPE_PEAU: Int = 59
    const val ITEM_TYPE_HUILE: Int = 60
    const val ITEM_TYPE_PELUCHE: Int = 61
    const val ITEM_TYPE_POISSON_VIDE: Int = 62
    const val ITEM_TYPE_VIANDE: Int = 63
    const val ITEM_TYPE_VIANDE_CONSERVEE: Int = 64
    const val ITEM_TYPE_QUEUE: Int = 65
    const val ITEM_TYPE_METARIA: Int = 66
    const val ITEM_TYPE_LEGUME: Int = 68
    const val ITEM_TYPE_VIANDE_COMESTIBLE: Int = 69
    const val ITEM_TYPE_TEINTURE: Int = 70
    const val ITEM_TYPE_EQUIP_ALCHIMIE: Int = 71
    const val ITEM_TYPE_OEUF_FAMILIER: Int = 72
    const val ITEM_TYPE_MAITRISE: Int = 73
    const val ITEM_TYPE_FEE_ARTIFICE: Int = 74
    const val ITEM_TYPE_PARCHEMIN_SORT: Int = 75
    const val ITEM_TYPE_PARCHEMIN_CARAC: Int = 76
    const val ITEM_TYPE_CERTIFICAT_CHANIL: Int = 77
    const val ITEM_TYPE_RUNE_FORGEMAGIE: Int = 78
    const val ITEM_TYPE_BOISSON: Int = 79
    const val ITEM_TYPE_OBJET_MISSION: Int = 80
    const val ITEM_TYPE_SAC_DOS: Int = 81
    const val ITEM_TYPE_BOUCLIER: Int = 82
    const val ITEM_TYPE_PIERRE_AME: Int = 83
    const val ITEM_TYPE_CLEFS: Int = 84
    const val ITEM_TYPE_PIERRE_AME_PLEINE: Int = 85
    const val ITEM_TYPE_POPO_OUBLI_PERCEP: Int = 86
    const val ITEM_TYPE_PARCHO_RECHERCHE: Int = 87
    const val ITEM_TYPE_PIERRE_MAGIQUE: Int = 88
    const val ITEM_TYPE_CADEAUX: Int = 89
    const val ITEM_TYPE_FANTOME_FAMILIER: Int = 90
    const val ITEM_TYPE_DRAGODINDE: Int = 91
    const val ITEM_TYPE_BOUFTOU: Int = 92
    const val ITEM_TYPE_OBJET_ELEVAGE: Int = 93
    const val ITEM_TYPE_OBJET_UTILISABLE: Int = 94
    const val ITEM_TYPE_PLANCHE: Int = 95
    const val ITEM_TYPE_ECORCE: Int = 96
    const val ITEM_TYPE_CERTIF_MONTURE: Int = 97
    const val ITEM_TYPE_RACINE: Int = 98
    const val ITEM_TYPE_FILET_CAPTURE: Int = 99
    const val ITEM_TYPE_SAC_RESSOURCE: Int = 100
    const val ITEM_TYPE_ARBALETE: Int = 102
    const val ITEM_TYPE_PATTE: Int = 103
    const val ITEM_TYPE_AILE: Int = 104
    const val ITEM_TYPE_OEUF: Int = 105
    const val ITEM_TYPE_OREILLE: Int = 106
    const val ITEM_TYPE_CARAPACE: Int = 107
    const val ITEM_TYPE_BOURGEON: Int = 108
    const val ITEM_TYPE_OEIL: Int = 109
    const val ITEM_TYPE_GELEE: Int = 110
    const val ITEM_TYPE_COQUILLE: Int = 111
    const val ITEM_TYPE_PRISME: Int = 112
    const val ITEM_TYPE_OBJET_VIVANT: Int = 113
    const val ITEM_TYPE_ARME_MAGIQUE: Int = 114
    const val ITEM_TYPE_FRAGM_AME_SHUSHU: Int = 115
    const val ITEM_TYPE_POTION_FAMILIER: Int = 116
    //Alignement
    const val ALIGNEMENT_NEUTRE: Int = -1
    const val ALIGNEMENT_BONTARIEN: Int = 1
    const val ALIGNEMENT_BRAKMARIEN: Int = 2
    const val ALIGNEMENT_MERCENAIRE: Int = 3
    //Elements
    const val ELEMENT_NULL: Int = -1
    const val ELEMENT_NEUTRE: Int = 0
    const val ELEMENT_TERRE: Int = 1
    const val ELEMENT_EAU: Int = 2
    const val ELEMENT_FEU: Int = 3
    const val ELEMENT_AIR: Int = 4
    //Classes
    const val CLASS_FECA: Int = 1
    const val CLASS_OSAMODAS: Int = 2
    const val CLASS_ENUTROF: Int = 3
    const val CLASS_SRAM: Int = 4
    const val CLASS_XELOR: Int = 5
    const val CLASS_ECAFLIP: Int = 6
    const val CLASS_ENIRIPSA: Int = 7
    const val CLASS_IOP: Int = 8
    const val CLASS_CRA: Int = 9
    const val CLASS_SADIDA: Int = 10
    const val CLASS_SACRIEUR: Int = 11
    const val CLASS_PANDAWA: Int = 12
    //Sexes
    const val SEX_MALE: Int = 0
    const val SEX_FEMALE: Int = 1
    //GamePlay
    const val MAX_EFFECTS_ID: Int = 1500
    //Buff a v�rifier en d�but de tour
    @JvmField val BEGIN_TURN_BUFF: IntArray = intArrayOf(91, 92, 93, 94, 95, 96, 97, 98, 99, 89, 100, 108)
    //Buff des Armes
    @JvmField val ARMES_EFFECT_IDS: IntArray = intArrayOf(91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 108)
    //Buff a ne pas booster en cas de CC
    @JvmField val NO_BOOST_CC_IDS: IntArray = intArrayOf(101)
    //Invocation Statiques
    @JvmField val STATIC_INVOCATIONS: IntArray = intArrayOf(282, 556, 2750, 7000)
    //Buff d�clench� en cas de frappe
    @JvmField val ON_HIT_BUFFS: IntArray = intArrayOf(9, 85, 86, 87, 88, 89, 79, 107, 788, 606, 607, 608, 609, 611)

    const val STATS_MIMIBIOTE: Int = 969 // 3C9
    const val ID_TEMPLATE_MIMIBIOTE: Byte = 4
    //Effects
    const val STATS_ADD_PM2: Int = 78
    const val STATS_REM_PA: Int = 101
    const val STATS_ADD_VIE: Int = 110
    const val STATS_ADD_PA: Int = 111
    const val STATS_MULTIPLY_DOMMAGE: Int = 114
    const val STATS_ADD_CC: Int = 115
    const val STATS_REM_PO: Int = 116
    const val STATS_ADD_PO: Int = 117
    const val STATS_ADD_FORC: Int = 118
    const val STATS_ADD_AGIL: Int = 119
    const val STATS_ADD_PA2: Int = 120
    const val STATS_ADD_DOMA: Int = 121
    const val STATS_ADD_EC: Int = 122
    const val STATS_ADD_CHAN: Int = 123
    const val STATS_ADD_SAGE: Int = 124
    const val STATS_ADD_VITA: Int = 125
    const val STATS_ADD_INTE: Int = 126
    const val STATS_REM_PM: Int = 127
    const val STATS_ADD_PM: Int = 128
    const val STATS_ADD_PERDOM: Int = 138
    const val STATS_ADD_PDOM: Int = 142
    const val STATS_REM_DOMA: Int = 145
    const val STATS_REM_CHAN: Int = 152
    const val STATS_REM_VITA: Int = 153
    const val STATS_REM_AGIL: Int = 154
    const val STATS_REM_INTE: Int = 155
    const val STATS_REM_SAGE: Int = 156
    const val STATS_REM_FORC: Int = 157
    const val STATS_ADD_PODS: Int = 158
    const val STATS_REM_PODS: Int = 159
    const val STATS_ADD_ADODGE: Int = 160
    const val STATS_ADD_MDODGE: Int = 161
    const val STATS_REM_AFLEE: Int = 162
    const val STATS_REM_MFLEE: Int = 163
    const val STATS_ADD_MAITRISE: Int = 165
    const val STATS_REM_PA2: Int = 168
    const val STATS_REM_PM2: Int = 169
    const val STATS_REM_CC: Int = 171
    const val STATS_ADD_INIT: Int = 174
    const val STATS_REM_INIT: Int = 175
    const val STATS_ADD_PROS: Int = 176
    const val STATS_REM_PROS: Int = 177
    const val STATS_ADD_SOIN: Int = 178
    const val STATS_REM_SOIN: Int = 179
    const val STATS_SUMMON_COUNT: Int = 182
    const val STATS_ADD_RES_M: Int = 183
    const val STATS_ADD_RES_P: Int = 184
    const val STATS_REM_PERDOM: Int = 186
    const val STATS_REM_SUM: Int = 187
    const val STATS_ADD_RP_TER: Int = 210
    const val STATS_ADD_RP_EAU: Int = 211
    const val STATS_ADD_RP_AIR: Int = 212
    const val STATS_ADD_RP_FEU: Int = 213
    const val STATS_ADD_RP_NEU: Int = 214
    const val STATS_REM_RP_TER: Int = 215
    const val STATS_REM_RP_EAU: Int = 216
    const val STATS_REM_RP_AIR: Int = 217
    const val STATS_REM_RP_FEU: Int = 218
    const val STATS_REM_RP_NEU: Int = 219
    const val STATS_RETDOM: Int = 220
    const val STATS_ADD_TRAP_DOM: Int = 225
    const val STATS_ADD_TRAP_PERDOM: Int = 226
    const val STATS_ADD_R_FEU: Int = 240
    const val STATS_ADD_R_NEU: Int = 241
    const val STATS_ADD_R_TER: Int = 242
    const val STATS_ADD_R_EAU: Int = 243
    const val STATS_ADD_R_AIR: Int = 244
    const val STATS_REM_R_FEU: Int = 245
    const val STATS_REM_R_NEU: Int = 246
    const val STATS_REM_R_TER: Int = 247
    const val STATS_REM_R_EAU: Int = 248
    const val STATS_REM_R_AIR: Int = 249
    const val STATS_ADD_RP_PVP_TER: Int = 250
    const val STATS_ADD_RP_PVP_EAU: Int = 251
    const val STATS_ADD_RP_PVP_AIR: Int = 252
    const val STATS_ADD_RP_PVP_FEU: Int = 253
    const val STATS_ADD_RP_PVP_NEU: Int = 254
    const val STATS_REM_RP_PVP_TER: Int = 255
    const val STATS_REM_RP_PVP_EAU: Int = 256
    const val STATS_REM_RP_PVP_AIR: Int = 257
    const val STATS_REM_RP_PVP_FEU: Int = 258
    const val STATS_REM_RP_PVP_NEU: Int = 259
    const val STATS_ADD_R_PVP_TER: Int = 260
    const val STATS_ADD_R_PVP_EAU: Int = 261
    const val STATS_ADD_R_PVP_AIR: Int = 262
    const val STATS_ADD_R_PVP_FEU: Int = 263
    const val STATS_ADD_R_PVP_NEU: Int = 264
    //Effets ID & Buffs
    const val EFFECT_PASS_TURN: Int = 140

    const val STATS_FORGET_ONE_LEVEL_SPELL: Int = 616
    //Capture
    const val CAPTURE_MONSTRE: Int = 623
    //Familier
    const val STATS_PETS_PDV: Int = 800
    const val STATS_PETS_POIDS: Int = 806
    const val STATS_PETS_REPAS: Int = 807
    const val STATS_PETS_DATE: Int = 808
    const val STATS_PETS_EPO: Int = 940
    const val STATS_PETS_SOUL: Int = 717
    // Objet d'�levage
    const val STATS_RESIST: Int = 812
    // Other
    const val STATS_TURN: Int = 811
    const val STATS_EXCHANGE_IN: Int = 983
    const val STATS_CHANGE_BY: Int = 985
    const val STATS_BUILD_BY: Int = 988
    const val STATS_NAME_TRAQUE: Int = 989
    const val STATS_GRADE_TRAQUE: Int = 961
    const val STATS_ALIGNEMENT_TRAQUE: Int = 960
    const val STATS_NIVEAU_TRAQUE: Int = 962

    const val STATS_DATE: Int = 805
    const val STATS_NIVEAU: Int = 962
    const val STATS_NAME_DJ: Int = 814
    const val STATS_OWNER_1: Int = 987//#4
    const val STATS_SIGNATURE: Int = 988
    const val ERR_STATS_XP: Int = 1000
    //ZAAPI <alignID,{mapID,mapID,...,mapID}>
    @JvmField var ZAAPI: MutableMap<Int,String> = HashMap<Int,String>()
    //ZAAP <mapID,cellID>
    @JvmField val ZAAPS: MutableMap<Int,Int> = HashMap<Int,Int>()
    //Valeur des droits de guilde
    @JvmField var G_RIGHTS: IntArray = intArrayOf(2, 4, 8, 16, 32, 64, 128, 256, 512, 4096, 8192, 16384)
    @JvmField var G_BOOST: Int = 2;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //G�rer les boost
    @JvmField var G_RIGHT: Int = 4;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //G�rer les droits
    @JvmField var G_INVITE: Int = 8;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //Inviter de nouveaux membres
    @JvmField var G_BAN: Int = 16;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //Bannir
    @JvmField var G_ALLXP: Int = 32;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //G�rer les r�partitions d'xp
    @JvmField var G_HISXP: Int = 256;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //G�rer sa r�partition d'xp
    @JvmField var G_RANK: Int = 64;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //G�rer les rangs
    @JvmField var G_POSPERCO: Int = 128;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //Poser un percepteur
    @JvmField var G_COLLPERCO: Int = 512;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //Collecter les percepteurs
    @JvmField var G_USEENCLOS: Int = 4096;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //Utiliser les enclos
    @JvmField var G_AMENCLOS: Int = 8192;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //Am�nager les enclos
    @JvmField var G_OTHDINDE: Int = 16384;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        //G�rer les montures des autres membres
    //Valeur des droits de maison
    @JvmField var H_GBLASON: Int = 2;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //Afficher blason pour membre de la guilde
    @JvmField var H_OBLASON: Int = 4;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //Afficher blason pour les autres
    @JvmField var H_GNOCODE: Int = 8;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //Entrer sans code pour la guilde
    @JvmField var H_OCANTOPEN: Int = 16;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //Entrer impossible pour les non-guildeux
    @JvmField var C_GNOCODE: Int = 32;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //Coffre sans code pour la guilde
    @JvmField var C_OCANTOPEN: Int = 64;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //Coffre impossible pour les non-guildeux
    @JvmField var H_GREPOS: Int = 256;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //Guilde droit au repos
    @JvmField var H_GTELE: Int = 128;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //Guilde droit a la TP
    // Nom des documents (swfs) : Documents d'avis de recherche
    @JvmField var HUNT_DETAILS_DOC: String = "71_0706251229"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                // PanMap d'explications
    @JvmField var HUNT_FRAKACIA_DOC: String = "63_0706251124"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                // Frakacia Leukocythine
    @JvmField var HUNT_AERMYNE_DOC: String = "100_0706251214"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            // Aermyne 'Braco' Scalptaras
    @JvmField var HUNT_MARZWEL_DOC: String = "96_0706251201"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                // Marzwel le Gobelin
    @JvmField var HUNT_BRUMEN_DOC: String = "68_0706251126"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                // Brumen Tinctorias
    @JvmField var HUNT_MUSHA_DOC: String = "94_0706251138"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                // Musha l'Oni
    @JvmField var HUNT_OGIVOL_DOC: String = "69_0706251058"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                // Ogivol Scarlacin
    @JvmField var HUNT_PADGREF_DOC: String = "61_0802081743"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                // Padgref Demoel
    @JvmField var HUNT_QILBIL_DOC: String = "67_0706251223"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                // Qil Bil
    @JvmField var HUNT_ROK_DOC: String = "93_0706251135"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                // Rok Gnorok
    @JvmField var HUNT_ZATOISHWAN_DOC: String = "98_0706251211"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                // Zatoïshwan
    @JvmField var HUNT_LETHALINE_DOC: String = "65_0706251123"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                // Léthaline Sigisbul
    //public static String HUNT_NERVOES_DOC    = "64_0706251123";  // Nervoes Brakdoun
    @JvmField var HUNT_FOUDUGLEN_DOC: String = "70_0706251122"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                // Fouduglen l'�cureuil

    // {(int)BorneId, (int)CellId, (str)SwfDocName, (int)MobId, (int)ItemFollow, (int)QuestId, (int)reponseID
    @JvmField var HUNTING_QUESTS: Array<Array<String>> = arrayOf(arrayOf("1988", "234", HUNT_DETAILS_DOC, "-1", "-1", "-1", "-1"), arrayOf("1986", "161", HUNT_LETHALINE_DOC, "-1", "-1", "-1", "-1"), arrayOf("1985", "119", HUNT_MARZWEL_DOC, "554", "7353", "117", "2552"), arrayOf("1986", "120", HUNT_PADGREF_DOC, "459", "6870", "29", "2108"), arrayOf("1985", "149", HUNT_FRAKACIA_DOC, "460", "6871", "30", "2109"), arrayOf("1986", "150", HUNT_QILBIL_DOC, "481", "6873", "32", "2111"), arrayOf("1986", "179", HUNT_BRUMEN_DOC, "464", "6874", "33", "2112"), arrayOf("1986", "180", HUNT_OGIVOL_DOC, "462", "6876", "35", "2114"), arrayOf("1985", "269", HUNT_MUSHA_DOC, "552", "7352", "116", "2551"), arrayOf("1986", "270", HUNT_FOUDUGLEN_DOC, "463", "6875", "34", "2113"), arrayOf("1985", "299", HUNT_ROK_DOC, "550", "7351", "115", "2550"), arrayOf("1986", "300", HUNT_AERMYNE_DOC, "446", "7350", "119", "2554"), arrayOf("1985", "329", HUNT_ZATOISHWAN_DOC, "555", "7354", "118", "2553"),)

    @JvmStatic fun getQuestByMobSkin(mobSkin: Int): Int {
        for (v in 0 until HUNTING_QUESTS.size)
            if (World.world.getMonstre(HUNTING_QUESTS[v][3].toInt()) != null
                    && World.world.getMonstre(HUNTING_QUESTS[v][3].toInt())!!.gfxId == mobSkin)
                return HUNTING_QUESTS[v][5].toInt()
        return -1
    }

    @JvmStatic fun getSkinByHuntMob(mobId: Int): Int {
        for (v in 0 until HUNTING_QUESTS.size)
            if (HUNTING_QUESTS[v][3].toInt() == mobId)
                return World.world.getMonstre(mobId)!!.gfxId
        return -1
    }

    @JvmStatic fun getItemByHuntMob(mobId: Int): Int {
        for (v in 0 until HUNTING_QUESTS.size)
            if (HUNTING_QUESTS[v][3].toInt() == mobId)
                return HUNTING_QUESTS[v][4].toInt()
        return -1
    }

    @JvmStatic fun getItemByMobSkin(mobSkin: Int): Int {
        for (v in 0 until HUNTING_QUESTS.size)
            if (World.world.getMonstre(HUNTING_QUESTS[v][3].toInt()) != null
                    && World.world.getMonstre(HUNTING_QUESTS[v][3].toInt())!!.gfxId == mobSkin)
                return HUNTING_QUESTS[v][4].toInt()
        return -1
    }

    @JvmStatic fun getDocNameByBornePos(borneId: Int, cellid: Int): String {
        for (v in 0 until HUNTING_QUESTS.size)
            if (HUNTING_QUESTS[v][0].toInt() == borneId
                    && HUNTING_QUESTS[v][1].toInt() == cellid)
                return HUNTING_QUESTS[v][2]
        return ""
    }

    @JvmStatic fun getClassStatueMap(classID: Int): Short {
        var pos: Short = 10298
        when (classID) {
1 -> {return 7398
}
2 -> {return 7545
}
3 -> {return 7442
}
4 -> {return 7392
}
5 -> {return 7332
}
6 -> {return 7446
}
7 -> {return 7361
}
8 -> {return 7427
}
9 -> {return 7378
}
10 -> {return 7395
}
11 -> {return 7336
}
12 -> {return 8035
}
13 -> {return 7427
        
}
}
        return pos
    }

    @JvmStatic fun getClassStatueCell(classID: Int): Int {
        var pos: Int = 314
        when (classID) {
1 -> {return 299
}
2 -> {return 311
}
3 -> {return 255
}
4 -> {return 282
}
5 -> {return 326
}
6 -> {return 300
}
7 -> {return 207
}
8, 13 -> {return 282
}
9 -> {return 368
}
10 -> {return 370
}
11 -> {return 197
}
12 -> {return 384
        
}
}
        return pos
    }

    @JvmStatic fun getStartMap(classID: Int): Short {
        when (classID) {
Constant.CLASS_FECA -> {return 10300
}
Constant.CLASS_OSAMODAS -> {return 10284
}
Constant.CLASS_ENUTROF -> {return 10299
}
Constant.CLASS_SRAM -> {return 10285
}
Constant.CLASS_XELOR -> {return 10298
}
Constant.CLASS_ECAFLIP -> {return 10276
}
Constant.CLASS_ENIRIPSA -> {return 10283
}
Constant.CLASS_IOP -> {return 10294
}
Constant.CLASS_CRA -> {return 10292
}
Constant.CLASS_SADIDA -> {return 10279
}
Constant.CLASS_SACRIEUR -> {return 10296
}
Constant.CLASS_PANDAWA -> {return 10289
        
}
else -> {return 10300
}
}
    }

    @JvmStatic fun getStartCell(classID: Int): Int {
        var pos: Int = 314
        when (classID) {
Constant.CLASS_FECA -> {pos = 337
                
}
Constant.CLASS_OSAMODAS -> {pos = 386
                
}
Constant.CLASS_ENUTROF -> {pos = 300
                
}
Constant.CLASS_SRAM -> {pos = 263
                
}
Constant.CLASS_XELOR -> {pos = 315
                
}
Constant.CLASS_ECAFLIP -> {pos = 311
                
}
Constant.CLASS_ENIRIPSA -> {pos = 299
                
}
Constant.CLASS_IOP -> {pos = 309
                
}
Constant.CLASS_CRA -> {pos = 299
                
}
Constant.CLASS_SADIDA -> {pos = 284
                
}
Constant.CLASS_SACRIEUR -> {pos = 258
                
}
Constant.CLASS_PANDAWA -> {pos = 250
                
}
}
        return pos
    }

    @JvmStatic fun getStartSortsPlaces(classID: Int): HashMap<Int,Int> {
        var start: HashMap<Int,Int> = HashMap()
        when (classID) {
CLASS_FECA -> {start.put(3, 1);//Attaque Naturelle
                start.put(6, 2);//Armure Terrestre
                start.put(17, 3);//Glyphe Agressif
                
}
CLASS_SRAM -> {start.put(61, 1);//Sournoiserie
                start.put(72, 2);//Invisibilit�
                start.put(65, 3);//Piege sournois
                
}
CLASS_ENIRIPSA -> {start.put(125, 1);//Mot Interdit
                start.put(128, 2);//Mot de Frayeur
                start.put(121, 3);//Mot Curatif
                
}
CLASS_ECAFLIP -> {start.put(102, 1);//Pile ou Face
                start.put(103, 2);//Chance d'ecaflip
                start.put(105, 3);//Bond du felin
                
}
CLASS_CRA -> {start.put(161, 1);//Fleche Magique
                start.put(169, 2);//Fleche de Recul
                start.put(164, 3);//Fleche Empoisonn�e(ex Fleche chercheuse)
                
}
CLASS_IOP -> {start.put(143, 1);//Intimidation
                start.put(141, 2);//Pression
                start.put(142, 3);//Bond
                
}
CLASS_SADIDA -> {start.put(183, 1);//Ronce
                start.put(200, 2);//Poison Paralysant
                start.put(193, 3);//La bloqueuse
                
}
CLASS_OSAMODAS -> {start.put(34, 1);//Invocation de tofu
                start.put(21, 2);//Griffe Spectrale
                start.put(23, 3);//Cri de l'ours
                
}
CLASS_XELOR -> {start.put(82, 1);//Contre
                start.put(81, 2);//Ralentissement
                start.put(83, 3);//Aiguille
                
}
CLASS_PANDAWA -> {start.put(686, 1);//Picole
                start.put(692, 2);//Gueule de bois
                start.put(687, 3);//Poing enflamm�
                
}
CLASS_ENUTROF -> {start.put(51, 1);//Lancer de Piece
                start.put(43, 2);//Lancer de Pelle
                start.put(41, 3);//Sac anim�
                
}
CLASS_SACRIEUR -> {start.put(432, 1);//Pied du Sacrieur
                start.put(431, 2);//Chatiment Os�
                start.put(434, 3);//Attirance
                
}
}
        return start
    }

    @JvmStatic fun getStartSorts(classID: Int): HashMap<Int,SortStats> {
        var start: HashMap<Int,SortStats> = HashMap<Int,SortStats>()
        when (classID) {
CLASS_FECA -> {start.put(3, World.world.getSort(3)!!.getStatsByLevel(1)!!);//Attaque Naturelle
                start.put(6, World.world.getSort(6)!!.getStatsByLevel(1)!!);//Armure Terrestre
                start.put(17, World.world.getSort(17)!!.getStatsByLevel(1)!!);//Glyphe Agressif
                
}
CLASS_SRAM -> {start.put(61, World.world.getSort(61)!!.getStatsByLevel(1)!!);//Sournoiserie
                start.put(72, World.world.getSort(72)!!.getStatsByLevel(1)!!);//Invisibilit�
                start.put(65, World.world.getSort(65)!!.getStatsByLevel(1)!!);//Piege sournois
                
}
CLASS_ENIRIPSA -> {start.put(125, World.world.getSort(125)!!.getStatsByLevel(1)!!);//Mot Interdit
                start.put(128, World.world.getSort(128)!!.getStatsByLevel(1)!!);//Mot de Frayeur
                start.put(121, World.world.getSort(121)!!.getStatsByLevel(1)!!);//Mot Curatif
                
}
CLASS_ECAFLIP -> {start.put(102, World.world.getSort(102)!!.getStatsByLevel(1)!!);//Pile ou Face
                start.put(103, World.world.getSort(103)!!.getStatsByLevel(1)!!);//Chance d'ecaflip
                start.put(105, World.world.getSort(105)!!.getStatsByLevel(1)!!);//Bond du felin
                
}
CLASS_CRA -> {start.put(161, World.world.getSort(161)!!.getStatsByLevel(1)!!);//Fleche Magique
                start.put(169, World.world.getSort(169)!!.getStatsByLevel(1)!!);//Fleche de Recul
                start.put(164, World.world.getSort(164)!!.getStatsByLevel(1)!!);//Fleche Empoisonn�e(ex Fleche chercheuse)
                
}
CLASS_IOP -> {start.put(143, World.world.getSort(143)!!.getStatsByLevel(1)!!);//Intimidation
                start.put(141, World.world.getSort(141)!!.getStatsByLevel(1)!!);//Pression
                start.put(142, World.world.getSort(142)!!.getStatsByLevel(1)!!);//Bond
                
}
CLASS_SADIDA -> {start.put(183, World.world.getSort(183)!!.getStatsByLevel(1)!!);//Ronce
                start.put(200, World.world.getSort(200)!!.getStatsByLevel(1)!!);//Poison Paralysant
                start.put(193, World.world.getSort(193)!!.getStatsByLevel(1)!!);//La bloqueuse
                
}
CLASS_OSAMODAS -> {start.put(34, World.world.getSort(34)!!.getStatsByLevel(1)!!);//Invocation de tofu
                start.put(21, World.world.getSort(21)!!.getStatsByLevel(1)!!);//Griffe Spectrale
                start.put(23, World.world.getSort(23)!!.getStatsByLevel(1)!!);//Cri de l'ours
                
}
CLASS_XELOR -> {start.put(82, World.world.getSort(82)!!.getStatsByLevel(1)!!);//Contre
                start.put(81, World.world.getSort(81)!!.getStatsByLevel(1)!!);//Ralentissement
                start.put(83, World.world.getSort(83)!!.getStatsByLevel(1)!!);//Aiguille
                
}
CLASS_PANDAWA -> {start.put(686, World.world.getSort(686)!!.getStatsByLevel(1)!!);//Picole
                start.put(692, World.world.getSort(692)!!.getStatsByLevel(1)!!);//Gueule de bois
                start.put(687, World.world.getSort(687)!!.getStatsByLevel(1)!!);//Poing enflamm�
                
}
CLASS_ENUTROF -> {start.put(51, World.world.getSort(51)!!.getStatsByLevel(1)!!);//Lancer de Piece
                start.put(43, World.world.getSort(43)!!.getStatsByLevel(1)!!);//Lancer de Pelle
                start.put(41, World.world.getSort(41)!!.getStatsByLevel(1)!!);//Sac anim�
                
}
CLASS_SACRIEUR -> {start.put(432, World.world.getSort(432)!!.getStatsByLevel(1)!!);//Pied du Sacrieur
                start.put(431, World.world.getSort(431)!!.getStatsByLevel(1)!!);//Chatiment Forc�
                start.put(434, World.world.getSort(434)!!.getStatsByLevel(1)!!);//Attirance
                
}
}
        return start
    }

    @JvmStatic fun getReqPtsToBoostStatsByClass(classID: Int, statID: Int, v  : Int): Int {
        when (statID) {
11 -> {return 1
}
12 -> {return 3
}
10 -> {when (classID) {
CLASS_SACRIEUR -> {if(v   < 100) return 1
                        if(v   < 200) return 2
                        if(v   < 300) return 3
                        return 4
}
CLASS_FECA -> {if (v   < 50)
                            return 2
                        if (v   < 150)
                            return 3
                        if (v   < 250)
                            return 4
                        return 5
}
CLASS_XELOR -> {if (v   < 50)
                            return 2
                        if (v   < 150)
                            return 3
                        if (v   < 250)
                            return 4
                        return 5
}
CLASS_SRAM -> {if (v   < 100)
                            return 1
                        if (v   < 200)
                            return 2
                        if (v   < 300)
                            return 3
                        if (v   < 400)
                            return 4
                        return 5
}
CLASS_OSAMODAS -> {if (v   < 50)
                            return 2
                        if (v   < 150)
                            return 3
                        if (v   < 250)
                            return 4
                        return 5
}
CLASS_ENIRIPSA -> {if (v   < 50)
                            return 2
                        if (v   < 150)
                            return 3
                        if (v   < 250)
                            return 4
                        return 5
}
CLASS_PANDAWA -> {if (v   < 50)
                            return 1
                        if (v   < 200)
                            return 2
                        return 3
}
CLASS_SADIDA -> {if (v   < 50)
                            return 1
                        if (v   < 250)
                            return 2
                        if (v   < 300)
                            return 3
                        if (v   < 400)
                            return 4
                        return 5
}
CLASS_CRA -> {if (v   < 50)
                            return 1
                        if (v   < 150)
                            return 2
                        if (v   < 250)
                            return 3
                        if (v   < 350)
                            return 4
                        return 5
}
CLASS_ENUTROF -> {if (v   < 50)
                            return 1
                        if (v   < 150)
                            return 2
                        if (v   < 250)
                            return 3
                        if (v   < 350)
                            return 4
                        return 5
}
CLASS_ECAFLIP -> {if (v   < 100)
                            return 1
                        if (v   < 200)
                            return 2
                        if (v   < 300)
                            return 3
                        if (v   < 400)
                            return 4
                        return 5
}
CLASS_IOP -> {if (v   < 100)
                            return 1
                        if (v   < 200)
                            return 2
                        if (v   < 300)
                            return 3
                        if (v   < 400)
                            return 4
                        return 5
                
}
}
                
}
13 -> {when (classID) {
CLASS_FECA -> {if (v   < 20)
                            return 1
                        if (v   < 40)
                            return 2
                        if (v   < 60)
                            return 3
                        if (v   < 80)
                            return 4
                        return 5
}
CLASS_XELOR -> {if (v   < 20)
                            return 1
                        if (v   < 40)
                            return 2
                        if (v   < 60)
                            return 3
                        if (v   < 80)
                            return 4
                        return 5
}
CLASS_SACRIEUR -> {if(v   < 100) return 1
                        if(v   < 200) return 2
                        if(v   < 300) return 3
                        return 4
}
CLASS_SRAM -> {if (v   < 20)
                            return 1
                        if (v   < 40)
                            return 2
                        if (v   < 60)
                            return 3
                        if (v   < 80)
                            return 4
                        return 5
}
CLASS_SADIDA -> {if (v   < 100)
                            return 1
                        if (v   < 200)
                            return 2
                        if (v   < 300)
                            return 3
                        if (v   < 400)
                            return 4
                        return 5
}
CLASS_PANDAWA -> {if (v   < 50)
                            return 1
                        if (v   < 200)
                            return 2
                        return 3
}
CLASS_IOP -> {if (v   < 20)
                            return 1
                        if (v   < 40)
                            return 2
                        if (v   < 60)
                            return 3
                        if (v   < 80)
                            return 4
                        return 5
}
CLASS_ENUTROF -> {if (v   < 100)
                            return 1
                        if (v   < 150)
                            return 2
                        if (v   < 230)
                            return 3
                        if (v   < 330)
                            return 4
                        return 5
}
CLASS_OSAMODAS -> {if (v   < 100)
                            return 1
                        if (v   < 200)
                            return 2
                        if (v   < 300)
                            return 3
                        if (v   < 400)
                            return 4
                        return 5
}
CLASS_ECAFLIP -> {if (v   < 20)
                            return 1
                        if (v   < 40)
                            return 2
                        if (v   < 60)
                            return 3
                        if (v   < 80)
                            return 4
                        return 5
}
CLASS_ENIRIPSA -> {if (v   < 20)
                            return 1
                        if (v   < 40)
                            return 2
                        if (v   < 60)
                            return 3
                        if (v   < 80)
                            return 4
                        return 5
}
CLASS_CRA -> {if (v   < 20)
                            return 1
                        if (v   < 40)
                            return 2
                        if (v   < 60)
                            return 3
                        if (v   < 80)
                            return 4
                        return 5
                
}
}
                
}
14 -> {when (classID) {
CLASS_FECA -> {if (v   < 20)
                            return 1
                        if (v   < 40)
                            return 2
                        if (v   < 60)
                            return 3
                        if (v   < 80)
                            return 4
                        return 5
}
CLASS_XELOR -> {if (v   < 20)
                            return 1
                        if (v   < 40)
                            return 2
                        if (v   < 60)
                            return 3
                        if (v   < 80)
                            return 4
                        return 5
}
CLASS_SACRIEUR -> {if(v   < 100) return 1
                        if(v   < 200) return 2
                        if(v   < 300) return 3
                        return 4
}
CLASS_SRAM -> {if (v   < 100)
                            return 1
                        if (v   < 200)
                            return 2
                        if (v   < 300)
                            return 3
                        if (v   < 400)
                            return 4
                        return 5
}
CLASS_SADIDA -> {if (v   < 20)
                            return 1
                        if (v   < 40)
                            return 2
                        if (v   < 60)
                            return 3
                        if (v   < 80)
                            return 4
                        return 5
}
CLASS_PANDAWA -> {if (v   < 50)
                            return 1
                        if (v   < 200)
                            return 2
                        return 3
}
CLASS_ENIRIPSA -> {if (v   < 20)
                            return 1
                        if (v   < 40)
                            return 2
                        if (v   < 60)
                            return 3
                        if (v   < 80)
                            return 4
                        return 5
}
CLASS_IOP -> {if (v   < 20)
                            return 1
                        if (v   < 40)
                            return 2
                        if (v   < 60)
                            return 3
                        if (v   < 80)
                            return 4
                        return 5
}
CLASS_ENUTROF -> {if (v   < 20)
                            return 1
                        if (v   < 40)
                            return 2
                        if (v   < 60)
                            return 3
                        if (v   < 80)
                            return 4
                        return 5
}
CLASS_ECAFLIP -> {if (v   < 50)
                            return 1
                        if (v   < 100)
                            return 2
                        if (v   < 150)
                            return 3
                        if (v   < 200)
                            return 4
                        return 5
}
CLASS_CRA -> {if (v   < 50)
                            return 1
                        if (v   < 100)
                            return 2
                        if (v   < 150)
                            return 3
                        if (v   < 200)
                            return 4
                        return 5
}
CLASS_OSAMODAS -> {if (v   < 20)
                            return 1
                        if (v   < 40)
                            return 2
                        if (v   < 60)
                            return 3
                        if (v   < 80)
                            return 4
                        return 5
                
}
}
                
}
15 -> {when (classID) {
CLASS_XELOR -> {if (v   < 100)
                            return 1
                        if (v   < 200)
                            return 2
                        if (v   < 300)
                            return 3
                        if (v   < 400)
                            return 4
                        return 5
}
CLASS_FECA -> {if (v   < 100)
                            return 1
                        if (v   < 200)
                            return 2
                        if (v   < 300)
                            return 3
                        if (v   < 400)
                            return 4
                        return 5
}
CLASS_SACRIEUR -> {if(v   < 100) return 1
                        if(v   < 200) return 2
                        if(v   < 300) return 3
                        return 4
}
CLASS_SRAM -> {if (v   < 50)
                            return 2
                        if (v   < 150)
                            return 3
                        if (v   < 250)
                            return 4
                        return 5
}
CLASS_SADIDA -> {if (v   < 100)
                            return 1
                        if (v   < 200)
                            return 2
                        if (v   < 300)
                            return 3
                        if (v   < 400)
                            return 4
                        return 5
}
CLASS_ENUTROF -> {if (v   < 20)
                            return 1
                        if (v   < 60)
                            return 2
                        if (v   < 100)
                            return 3
                        if (v   < 140)
                            return 4
                        return 5
}
CLASS_PANDAWA -> {if (v   < 50)
                            return 1
                        if (v   < 200)
                            return 2
                        return 3
}
CLASS_IOP -> {if (v   < 20)
                            return 1
                        if (v   < 40)
                            return 2
                        if (v   < 60)
                            return 3
                        if (v   < 80)
                            return 4
                        return 5
}
CLASS_ENIRIPSA -> {if (v   < 100)
                            return 1
                        if (v   < 200)
                            return 2
                        if (v   < 300)
                            return 3
                        if (v   < 400)
                            return 4
                        return 5
}
CLASS_CRA -> {if (v   < 50)
                            return 1
                        if (v   < 150)
                            return 2
                        if (v   < 250)
                            return 3
                        if (v   < 350)
                            return 4
                        return 5
}
CLASS_OSAMODAS -> {if (v   < 100)
                            return 1
                        if (v   < 200)
                            return 2
                        if (v   < 300)
                            return 3
                        if (v   < 400)
                            return 4
                        return 5
}
CLASS_ECAFLIP -> {if (v   < 20)
                            return 1
                        if (v   < 40)
                            return 2
                        if (v   < 60)
                            return 3
                        if (v   < 80)
                            return 4
                        return 5
                
}
}
                
}
}
        return 5
    }

    @JvmStatic fun onLevelUpSpells(perso: Player, lvl: Int) {
        when (perso.classe) {
CLASS_FECA -> {if (lvl == 3)
                    perso.learnSpell(4, 1, true, false, false);//Renvoie de sort
                if (lvl == 6)
                    perso.learnSpell(2, 1, true, false, false);//Aveuglement
                if (lvl == 9)
                    perso.learnSpell(1, 1, true, false, false);//Armure Incandescente
                if (lvl == 13)
                    perso.learnSpell(9, 1, true, false, false);//Attaque nuageuse
                if (lvl == 17)
                    perso.learnSpell(18, 1, true, false, false);//Armure Aqueuse
                if (lvl == 21)
                    perso.learnSpell(20, 1, true, false, false);//Immunit�
                if (lvl == 26)
                    perso.learnSpell(14, 1, true, false, false);//Armure Venteuse
                if (lvl == 31)
                    perso.learnSpell(19, 1, true, false, false);//Bulle
                if (lvl == 36)
                    perso.learnSpell(5, 1, true, false, false);//Tr�ve
                if (lvl == 42)
                    perso.learnSpell(16, 1, true, false, false);//Science du b�ton
                if (lvl == 48)
                    perso.learnSpell(8, 1, true, false, false);// falseur du b�ton
                if (lvl == 54)
                    perso.learnSpell(12, 1, true, false, false);//glyphe d'Aveuglement
                if (lvl == 60)
                    perso.learnSpell(11, 1, true, false, false);//T�l�portation
                if (lvl == 70)
                    perso.learnSpell(10, 1, true, false, false);//Glyphe Enflamm�
                if (lvl == 80)
                    perso.learnSpell(7, 1, true, false, false);//Bouclier F�ca
                if (lvl == 90)
                    perso.learnSpell(15, 1, true, false, false);//Glyphe d'Immobilisation
                if (lvl == 100)
                    perso.learnSpell(13, 1, true, false, false);//Glyphe de Silence
                if (lvl == 200)
                    perso.learnSpell(1901, 1, true, false, false);//Invocation de Dopeul F�ca
                
}
CLASS_OSAMODAS -> {if (lvl == 3)
                    perso.learnSpell(26, 1, true, false, false);//B�n�diction Animale
                if (lvl == 6)
                    perso.learnSpell(22, 1, true, false, false);//D�placement F�lin
                if (lvl == 9)
                    perso.learnSpell(35, 1, true, false, false);//Invocation de Bouftou
                if (lvl == 13)
                    perso.learnSpell(28, 1, true, false, false);//Crapaud
                if (lvl == 17)
                    perso.learnSpell(37, 1, true, false, false);//Invocation de Prespic
                if (lvl == 21)
                    perso.learnSpell(30, 1, true, false, false);//Fouet
                if (lvl == 26)
                    perso.learnSpell(27, 1, true, false, false);//Piq�re Motivante
                if (lvl == 31)
                    perso.learnSpell(24, 1, true, false, false);//Corbeau
                if (lvl == 36)
                    perso.learnSpell(33, 1, true, false, false);//Griffe Cinglante
                if (lvl == 42)
                    perso.learnSpell(25, 1, true, false, false);//Soin Animal
                if (lvl == 48)
                    perso.learnSpell(38, 1, true, false, false);//Invocation de Sanglier
                if (lvl == 54)
                    perso.learnSpell(36, 1, true, false, false);//Frappe du Craqueleur
                if (lvl == 60)
                    perso.learnSpell(32, 1, true, false, false);//R�sistance Naturelle
                if (lvl == 70)
                    perso.learnSpell(29, 1, true, false, false);//Crocs du Mulou
                if (lvl == 80)
                    perso.learnSpell(39, 1, true, false, false);//Invocation de Bwork Mage
                if (lvl == 90)
                    perso.learnSpell(40, 1, true, false, false);//Invocation de Craqueleur
                if (lvl == 100)
                    perso.learnSpell(31, 1, true, false, false);//Invocation de Dragonnet Rouge
                if (lvl == 200)
                    perso.learnSpell(1902, 1, true, false, false);//Invocation de Dopeul Osamodas
                
}
CLASS_ENUTROF -> {if (lvl == 3)
                    perso.learnSpell(49, 1, true, false, false);//Pelle Fantomatique
                if (lvl == 6)
                    perso.learnSpell(42, 1, true, false, false);//Chance
                if (lvl == 9)
                    perso.learnSpell(47, 1, true, false, false);//Bo�te de Pandore
                if (lvl == 13)
                    perso.learnSpell(48, 1, true, false, false);//Remblai
                if (lvl == 17)
                    perso.learnSpell(45, 1, true, false, false);//Cl� R�ductrice
                if (lvl == 21)
                    perso.learnSpell(53, 1, true, false, false);//Force de l'Age
                if (lvl == 26)
                    perso.learnSpell(46, 1, true, false, false);//D�sinvocation
                if (lvl == 31)
                    perso.learnSpell(52, 1, true, false, false);//Cupidit�
                if (lvl == 36)
                    perso.learnSpell(44, 1, true, false, false);//Roulage de Pelle
                if (lvl == 42)
                    perso.learnSpell(50, 1, true, false, false);//Maladresse
                if (lvl == 48)
                    perso.learnSpell(54, 1, true, false, false);//Maladresse de Masse
                if (lvl == 54)
                    perso.learnSpell(55, 1, true, false, false);//Acc�l�ration
                if (lvl == 60)
                    perso.learnSpell(56, 1, true, false, false);//Pelle du Jugement
                if (lvl == 70)
                    perso.learnSpell(58, 1, true, false, false);//Pelle Massacrante
                if (lvl == 80)
                    perso.learnSpell(59, 1, true, false, false);//Corruption
                if (lvl == 90)
                    perso.learnSpell(57, 1, true, false, false);//Pelle Anim�e
                if (lvl == 100)
                    perso.learnSpell(60, 1, true, false, false);//Coffre Anim�
                if (lvl == 200)
                    perso.learnSpell(1903, 1, true, false, false);//Invocation de Dopeul Enutrof
                
}
CLASS_SRAM -> {if (lvl == 3)
                    perso.learnSpell(66, 1, true, false, false);//Poison insidieux
                if (lvl == 6)
                    perso.learnSpell(68, 1, true, false, false);//Fourvoiement
                if (lvl == 9)
                    perso.learnSpell(63, 1, true, false, false);//Coup Sournois
                if (lvl == 13)
                    perso.learnSpell(74, 1, true, false, false);//Double
                if (lvl == 17)
                    perso.learnSpell(64, 1, true, false, false);//Rep�rage
                if (lvl == 21)
                    perso.learnSpell(79, 1, true, false, false);//Pi�ge de Masse
                if (lvl == 26)
                    perso.learnSpell(78, 1, true, false, false);//Invisibilit� d'Autrui
                if (lvl == 31)
                    perso.learnSpell(71, 1, true, false, false);//Pi�ge Empoisonn�
                if (lvl == 36)
                    perso.learnSpell(62, 1, true, false, false);//Concentration de Chakra
                if (lvl == 42)
                    perso.learnSpell(69, 1, true, false, false);//Pi�ge d'Immobilisation
                if (lvl == 48)
                    perso.learnSpell(77, 1, true, false, false);//Pi�ge de Silence
                if (lvl == 54)
                    perso.learnSpell(73, 1, true, false, false);//Pi�ge r�pulsif
                if (lvl == 60)
                    perso.learnSpell(67, 1, true, false, false);//Peur
                if (lvl == 70)
                    perso.learnSpell(70, 1, true, false, false);//Arnaque
                if (lvl == 80)
                    perso.learnSpell(75, 1, true, false, false);//Pulsion de Chakra
                if (lvl == 90)
                    perso.learnSpell(76, 1, true, false, false);//Attaque Mortelle
                if (lvl == 100)
                    perso.learnSpell(80, 1, true, false, false);//Pi�ge Mortel
                if (lvl == 200)
                    perso.learnSpell(1904, 1, true, false, false);//Invocation de Dopeul Sram
                
}
CLASS_XELOR -> {if (lvl == 3)
                    perso.learnSpell(84, 1, true, false, false);//Gelure
                if (lvl == 6)
                    perso.learnSpell(100, 1, true, false, false);//Sablier de X�lor
                if (lvl == 9)
                    perso.learnSpell(92, 1, true, false, false);//Rayon Obscur
                if (lvl == 13)
                    perso.learnSpell(88, 1, true, false, false);//T�l�portation
                if (lvl == 17)
                    perso.learnSpell(93, 1, true, false, false);//Fl�trissement
                if (lvl == 21)
                    perso.learnSpell(85, 1, true, false, false);//Flou
                if (lvl == 26)
                    perso.learnSpell(96, 1, true, false, false);//Poussi�re Temporelle
                if (lvl == 31)
                    perso.learnSpell(98, 1, true, false, false);//Vol du Temps
                if (lvl == 36)
                    perso.learnSpell(86, 1, true, false, false);//Aiguille Chercheuse
                if (lvl == 42)
                    perso.learnSpell(89, 1, true, false, false);//D�vouement
                if (lvl == 48)
                    perso.learnSpell(90, 1, true, false, false);//Fuite
                if (lvl == 54)
                    perso.learnSpell(87, 1, true, false, false);//D�motivation
                if (lvl == 60)
                    perso.learnSpell(94, 1, true, false, false);//Protection Aveuglante
                if (lvl == 70)
                    perso.learnSpell(99, 1, true, false, false);//Momification
                if (lvl == 80)
                    perso.learnSpell(95, 1, true, false, false);//Horloge
                if (lvl == 90)
                    perso.learnSpell(91, 1, true, false, false);//Frappe de X�lor
                if (lvl == 100)
                    perso.learnSpell(97, 1, true, false, false);//Cadran de X�lor
                if (lvl == 200)
                    perso.learnSpell(1905, 1, true, false, false);//Invocation de Dopeul X�lor
                
}
CLASS_ECAFLIP -> {if (lvl == 3)
                    perso.learnSpell(109, 1, true, false, false);//Bluff
                if (lvl == 6)
                    perso.learnSpell(113, 1, true, false, false);//Perception
                if (lvl == 9)
                    perso.learnSpell(111, 1, true, false, false);//Contrecoup
                if (lvl == 13)
                    perso.learnSpell(104, 1, true, false, false);//Tr�fle
                if (lvl == 17)
                    perso.learnSpell(119, 1, true, false, false);//Tout ou rien
                if (lvl == 21)
                    perso.learnSpell(101, 1, true, false, false);//Roulette
                if (lvl == 26)
                    perso.learnSpell(107, 1, true, false, false);//Topkaj
                if (lvl == 31)
                    perso.learnSpell(116, 1, true, false, false);//Langue R�peuse
                if (lvl == 36)
                    perso.learnSpell(106, 1, true, false, false);//Roue de la Fortune
                if (lvl == 42)
                    perso.learnSpell(117, 1, true, false, false);//Griffe Invocatrice
                if (lvl == 48)
                    perso.learnSpell(108, 1, true, false, false);//Esprit F�lin
                if (lvl == 54)
                    perso.learnSpell(115, 1, true, false, false);//Odorat
                if (lvl == 60)
                    perso.learnSpell(118, 1, true, false, false);//R�flexes
                if (lvl == 70)
                    perso.learnSpell(110, 1, true, false, false);//Griffe Joueuse
                if (lvl == 80)
                    perso.learnSpell(112, 1, true, false, false);//Griffe de Ceangal
                if (lvl == 90)
                    perso.learnSpell(114, 1, true, false, false);//Rekop
                if (lvl == 100)
                    perso.learnSpell(120, 1, true, false, false);//Destin d'Ecaflip
                if (lvl == 200)
                    perso.learnSpell(1906, 1, true, false, false);//Invocation de Dopeul Ecaflip
                
}
CLASS_ENIRIPSA -> {if (lvl == 3)
                    perso.learnSpell(124, 1, true, false, false);//Mot Soignant
                if (lvl == 6)
                    perso.learnSpell(122, 1, true, false, false);//Mot Blessant
                if (lvl == 9)
                    perso.learnSpell(126, 1, true, false, false);//Mot Stimulant
                if (lvl == 13)
                    perso.learnSpell(127, 1, true, false, false);//Mot de Pr�vention
                if (lvl == 17)
                    perso.learnSpell(123, 1, true, false, false);//Mot Drainant
                if (lvl == 21)
                    perso.learnSpell(130, 1, true, false, false);//Mot Revitalisant
                if (lvl == 26)
                    perso.learnSpell(131, 1, true, false, false);//Mot de R�g�n�ration
                if (lvl == 31)
                    perso.learnSpell(132, 1, true, false, false);//Mot d'Epine
                if (lvl == 36)
                    perso.learnSpell(133, 1, true, false, false);//Mot de Jouvence
                if (lvl == 42)
                    perso.learnSpell(134, 1, true, false, false);//Mot Vampirique
                if (lvl == 48)
                    perso.learnSpell(135, 1, true, false, false);//Mot de Sacrifice
                if (lvl == 54)
                    perso.learnSpell(129, 1, true, false, false);//Mot d'Amiti�
                if (lvl == 60)
                    perso.learnSpell(136, 1, true, false, false);//Mot d'Immobilisation
                if (lvl == 70)
                    perso.learnSpell(137, 1, true, false, false);//Mot d'Envol
                if (lvl == 80)
                    perso.learnSpell(138, 1, true, false, false);//Mot de Silence
                if (lvl == 90)
                    perso.learnSpell(139, 1, true, false, false);//Mot d'Altruisme
                if (lvl == 100)
                    perso.learnSpell(140, 1, true, false, false);//Mot de Reconstitution
                if (lvl == 200)
                    perso.learnSpell(1907, 1, true, false, false);//Invocation de Dopeul Eniripsa
                
}
CLASS_IOP -> {if (lvl == 3)
                    perso.learnSpell(144, 1, true, false, false);//Compulsion
                if (lvl == 6)
                    perso.learnSpell(145, 1, true, false, false);//Ep�e Divine
                if (lvl == 9)
                    perso.learnSpell(146, 1, true, false, false);//Ep�e du Destin
                if (lvl == 13)
                    perso.learnSpell(147, 1, true, false, false);//Guide de Bravoure
                if (lvl == 17)
                    perso.learnSpell(148, 1, true, false, false);//Amplification
                if (lvl == 21)
                    perso.learnSpell(154, 1, true, false, false);//Ep�e Destructrice
                if (lvl == 26)
                    perso.learnSpell(150, 1, true, false, false);//Couper
                if (lvl == 31)
                    perso.learnSpell(151, 1, true, false, false);//Souffle
                if (lvl == 36)
                    perso.learnSpell(155, 1, true, false, false);//Vitalit�
                if (lvl == 42)
                    perso.learnSpell(152, 1, true, false, false);//Ep�e du Jugement
                if (lvl == 48)
                    perso.learnSpell(153, 1, true, false, false);//Puissance
                if (lvl == 54)
                    perso.learnSpell(149, 1, true, false, false);//Mutilation
                if (lvl == 60)
                    perso.learnSpell(156, 1, true, false, false);//Temp�te de Puissance
                if (lvl == 70)
                    perso.learnSpell(157, 1, true, false, false);//Ep�e C�leste
                if (lvl == 80)
                    perso.learnSpell(158, 1, true, false, false);//Concentration
                if (lvl == 90)
                    perso.learnSpell(160, 1, true, false, false);//Ep�e de Iop
                if (lvl == 100)
                    perso.learnSpell(159, 1, true, false, false);//Col�re de Iop
                if (lvl == 200)
                    perso.learnSpell(1908, 1, true, false, false);//Invocation de Dopeul Iop
                
}
CLASS_CRA -> {if (lvl == 3)
                    perso.learnSpell(163, 1, true, false, false);//Fl�che Glac�e
                if (lvl == 6)
                    perso.learnSpell(165, 1, true, false, false);//Fl�che enflamm�e
                if (lvl == 9)
                    perso.learnSpell(172, 1, true, false, false);//Tir Eloign�
                if (lvl == 13)
                    perso.learnSpell(167, 1, true, false, false);//Fl�che d'Expiation
                if (lvl == 17)
                    perso.learnSpell(168, 1, true, false, false);//Oeil de Taupe
                if (lvl == 21)
                    perso.learnSpell(162, 1, true, false, false);//Tir Critique
                if (lvl == 26)
                    perso.learnSpell(170, 1, true, false, false);//Fl�che d'Immobilisation
                if (lvl == 31)
                    perso.learnSpell(171, 1, true, false, false);//Fl�che Punitive
                if (lvl == 36)
                    perso.learnSpell(166, 1, true, false, false);//Tir Puissant
                if (lvl == 42)
                    perso.learnSpell(173, 1, true, false, false);//Fl�che Harcelante
                if (lvl == 48)
                    perso.learnSpell(174, 1, true, false, false);//Fl�che Cinglante
                if (lvl == 54)
                    perso.learnSpell(176, 1, true, false, false);//Fl�che Pers�cutrice
                if (lvl == 60)
                    perso.learnSpell(175, 1, true, false, false);//Fl�che Destructrice
                if (lvl == 70)
                    perso.learnSpell(178, 1, true, false, false);//Fl�che Absorbante
                if (lvl == 80)
                    perso.learnSpell(177, 1, true, false, false);//Fl�che Ralentissante
                if (lvl == 90)
                    perso.learnSpell(179, 1, true, false, false);//Fl�che Explosive
                if (lvl == 100)
                    perso.learnSpell(180, 1, true, false, false);//Ma�trise de l'Arc
                if (lvl == 200)
                    perso.learnSpell(1909, 1, true, false, false);//Invocation de Dopeul Cra
                
}
CLASS_SADIDA -> {if (lvl == 3)
                    perso.learnSpell(198, 1, true, false, false);//Sacrifice Poupesque
                if (lvl == 6)
                    perso.learnSpell(195, 1, true, false, false);//Larme
                if (lvl == 9)
                    perso.learnSpell(182, 1, true, false, false);//Invocation de la Folle
                if (lvl == 13)
                    perso.learnSpell(192, 1, true, false, false);//Ronce Apaisante
                if (lvl == 17)
                    perso.learnSpell(197, 1, true, false, false);//Puissance Sylvestre
                if (lvl == 21)
                    perso.learnSpell(189, 1, true, false, false);//Invocation de la Sacrifi�e
                if (lvl == 26)
                    perso.learnSpell(181, 1, true, false, false);//Tremblement
                if (lvl == 31)
                    perso.learnSpell(199, 1, true, false, false);//Connaissance des Poup�es
                if (lvl == 36)
                    perso.learnSpell(191, 1, true, false, false);//Ronce Multiples
                if (lvl == 42)
                    perso.learnSpell(186, 1, true, false, false);//Arbre
                if (lvl == 48)
                    perso.learnSpell(196, 1, true, false, false);//Vent Empoisonn�
                if (lvl == 54)
                    perso.learnSpell(190, 1, true, false, false);//Invocation de la Gonflable
                if (lvl == 60)
                    perso.learnSpell(194, 1, true, false, false);//Ronces Agressives
                if (lvl == 70)
                    perso.learnSpell(185, 1, true, false, false);//Herbe Folle
                if (lvl == 80)
                    perso.learnSpell(184, 1, true, false, false);//Feu de Brousse
                if (lvl == 90)
                    perso.learnSpell(188, 1, true, false, false);//Ronce Insolente
                if (lvl == 100)
                    perso.learnSpell(187, 1, true, false, false);//Invocation de la Surpuissante
                if (lvl == 200)
                    perso.learnSpell(1910, 1, true, false, false);//Invocation de Dopeul Sadida
                
}
CLASS_SACRIEUR -> {if (lvl == 3)
                    perso.learnSpell(444, 1, true, false, false);//D�robade
                if (lvl == 6)
                    perso.learnSpell(449, 1, true, false, false);//D�tour
                if (lvl == 9)
                    perso.learnSpell(436, 1, true, false, false);//Assaut
                if (lvl == 13)
                    perso.learnSpell(437, 1, true, false, false);//Ch�timent Agile
                if (lvl == 17)
                    perso.learnSpell(439, 1, true, false, false);//Dissolution
                if (lvl == 21)
                    perso.learnSpell(433, 1, true, false, false);//Ch�timent Os�
                if (lvl == 26)
                    perso.learnSpell(443, 1, true, false, false);//Ch�timent Spirituel
                if (lvl == 31)
                    perso.learnSpell(440, 1, true, false, false);//Sacrifice
                if (lvl == 36)
                    perso.learnSpell(442, 1, true, false, false);//Absorption
                if (lvl == 42)
                    perso.learnSpell(441, 1, true, false, false);//Ch�timent Vilatesque
                if (lvl == 48)
                    perso.learnSpell(445, 1, true, false, false);//Coop�ration
                if (lvl == 54)
                    perso.learnSpell(438, 1, true, false, false);//Transposition
                if (lvl == 60)
                    perso.learnSpell(446, 1, true, false, false);//Punition
                if (lvl == 70)
                    perso.learnSpell(447, 1, true, false, false);//Furie
                if (lvl == 80)
                    perso.learnSpell(448, 1, true, false, false);//Ep�e Volante
                if (lvl == 90)
                    perso.learnSpell(435, 1, true, false, false);//Tansfert de Vie
                if (lvl == 100)
                    perso.learnSpell(450, 1, true, false, false);//Folie Sanguinaire
                if (lvl == 200)
                    perso.learnSpell(1911, 1, true, false, false);//Invocation de Dopeul Sacrieur
                
}
CLASS_PANDAWA -> {if (lvl == 3)
                    perso.learnSpell(689, 1, true, false, false);//Epouvante
                if (lvl == 6)
                    perso.learnSpell(690, 1, true, false, false);//Souffle Alcoolis�
                if (lvl == 9)
                    perso.learnSpell(691, 1, true, false, false);//Vuln�rabilit� Aqueuse
                if (lvl == 13)
                    perso.learnSpell(688, 1, true, false, false);//Vuln�rabilit� Incandescente
                if (lvl == 17)
                    perso.learnSpell(693, 1, true, false, false);//Karcham
                if (lvl == 21)
                    perso.learnSpell(694, 1, true, false, false);//Vuln�rabilit� Venteuse
                if (lvl == 26)
                    perso.learnSpell(695, 1, true, false, false);//Stabilisation
                if (lvl == 31)
                    perso.learnSpell(696, 1, true, false, false);//Chamrak
                if (lvl == 36)
                    perso.learnSpell(697, 1, true, false, false);//Vuln�rabilit� Terrestre
                if (lvl == 42)
                    perso.learnSpell(698, 1, true, false, false);//Souillure
                if (lvl == 48)
                    perso.learnSpell(699, 1, true, false, false);//Lait de Bambou
                if (lvl == 54)
                    perso.learnSpell(700, 1, true, false, false);//Vague � Lame
                if (lvl == 60)
                    perso.learnSpell(701, 1, true, false, false);//Col�re de Zato�shwan
                if (lvl == 70)
                    perso.learnSpell(702, 1, true, false, false);//Flasque Explosive
                if (lvl == 80)
                    perso.learnSpell(703, 1, true, false, false);//Pandatak
                if (lvl == 90)
                    perso.learnSpell(704, 1, true, false, false);//Pandanlku
                if (lvl == 100)
                    perso.learnSpell(705, 1, true, false, false);//Lien Spiritueux
                if (lvl == 200)
                    perso.learnSpell(1912, 1, true, false, false);//Invocation de Dopeul Pandawa
                
}
}
    }

    @JvmStatic fun getGlyphColor(spell: Int): Int {
        when (spell) {
10, 2033 -> {return 4
}
12, 2034 -> {return 3
}
13, 2035 -> {return 6
}
15, 2036 -> {return 5
}
17, 2037 -> {return 2
}
1072, 1073, 949 -> {return 0
}
476 -> {return 0
}
else -> {return 4
        
}
}
    }

    @JvmStatic fun getTrapsColor(spell: Int): Int {
        when (spell) {
65 -> {return 7
}
69 -> {return 10
}
71, 2068 -> {return 9
}
73 -> {return 12
}
77, 2071 -> {return 11
}
79, 2072 -> {return 8
}
80 -> {return 13
}
else -> {return 7
        
}
}
    }

    @JvmStatic fun getMountStats(color: Int, lvl: Int): Stats {
        var stats: Stats = Stats()
        when (color) {
1 -> {
}
3 -> {stats.addOneStat(STATS_ADD_VITA, lvl / 2)
                stats.addOneStat(STATS_ADD_AGIL, ((lvl / 1.25).toInt()));//100/1.25 = 80
                
}
10 -> {stats.addOneStat(STATS_ADD_VITA, lvl); //100/1 = 100
                
}
20 -> {stats.addOneStat(STATS_ADD_INIT, lvl * 10); // 100*10 = 1000
                stats.addOneStat(STATS_SUMMON_COUNT, lvl / 50); // 100/50 = 2
                
}
18 -> {stats.addOneStat(STATS_ADD_VITA, lvl / 2)
                stats.addOneStat(STATS_ADD_SAGE, ((lvl / 2.50).toInt())); // 100/2.50 = 40
                
}
38 -> {stats.addOneStat(STATS_ADD_INIT, lvl * 5); // 100*5 = 500
                stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_SUMMON_COUNT, lvl / 50); // 100/50 = 2
                
}
46 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_SAGE, lvl / 4); //100/4 = 25
                
}
33 -> {stats.addOneStat(STATS_ADD_INIT, lvl * 5)
                stats.addOneStat(STATS_ADD_SAGE, lvl / 4)
                stats.addOneStat(STATS_ADD_VITA, lvl / 2)
                stats.addOneStat(STATS_SUMMON_COUNT, lvl / 100); // 100/100 = 1
                
}
17 -> {stats.addOneStat(STATS_ADD_CHAN, ((lvl / 1.25).toInt()))
                stats.addOneStat(STATS_ADD_VITA, lvl / 2)
                
}
62 -> {stats.addOneStat(STATS_ADD_VITA, ((lvl * 1.50).toInt())); // 100*1.50 = 150
                stats.addOneStat(STATS_ADD_CHAN, ((lvl / 1.65).toInt()))
                
}
12 -> {stats.addOneStat(STATS_ADD_VITA, ((lvl * 1.50).toInt()))
                stats.addOneStat(STATS_ADD_AGIL, ((lvl / 1.65).toInt()))
                
}
36 -> {stats.addOneStat(STATS_ADD_INIT, lvl * 5)
                stats.addOneStat(STATS_ADD_VITA, lvl / 2)
                stats.addOneStat(STATS_ADD_CHAN, ((lvl / 1.65).toInt()))
                stats.addOneStat(STATS_SUMMON_COUNT, lvl / 100)
                
}
19 -> {stats.addOneStat(STATS_ADD_FORC, ((lvl / 1.25).toInt()))
                stats.addOneStat(STATS_ADD_VITA, lvl / 2)
                
}
22 -> {stats.addOneStat(STATS_ADD_INTE, ((lvl / 1.25).toInt()))
                stats.addOneStat(STATS_ADD_VITA, lvl / 2)
                
}
48 -> {stats.addOneStat(STATS_ADD_VITA, (lvl))
                stats.addOneStat(STATS_ADD_SAGE, lvl / 4)
                stats.addOneStat(STATS_ADD_INTE, ((lvl / 1.65).toInt()))
                
}
65 -> {stats.addOneStat(STATS_ADD_VITA, (lvl))
                stats.addOneStat(STATS_ADD_CHAN, lvl / 2)
                stats.addOneStat(STATS_ADD_FORC, lvl / 2)
                
}
67 -> {stats.addOneStat(STATS_ADD_VITA, (lvl))
                stats.addOneStat(STATS_ADD_PERDOM, lvl / 2)
                stats.addOneStat(STATS_ADD_INTE, lvl / 2)
                
}
54 -> {stats.addOneStat(STATS_ADD_VITA, (lvl))
                stats.addOneStat(STATS_ADD_FORC, lvl / 2)
                stats.addOneStat(STATS_ADD_AGIL, lvl / 2)
                
}
53 -> {stats.addOneStat(STATS_ADD_VITA, (lvl))
                stats.addOneStat(STATS_ADD_AGIL, lvl / 2)
                stats.addOneStat(STATS_ADD_INTE, lvl / 2)
                
}
76 -> {stats.addOneStat(STATS_ADD_VITA, (lvl))
                stats.addOneStat(STATS_ADD_INTE, lvl / 2)
                stats.addOneStat(STATS_ADD_FORC, lvl / 2)
                
}
34 -> {stats.addOneStat(STATS_ADD_INIT, lvl * 5)
                stats.addOneStat(STATS_ADD_VITA, lvl / 2)
                stats.addOneStat(STATS_ADD_AGIL, ((lvl / 1.65).toInt()))
                stats.addOneStat(STATS_SUMMON_COUNT, lvl / 100)
                
}
37 -> {stats.addOneStat(STATS_ADD_INIT, lvl * 5)
                stats.addOneStat(STATS_ADD_VITA, ((lvl * 0.4).toInt()))
                stats.addOneStat(STATS_ADD_PERDOM, ((lvl * 0.4).toInt()))
                stats.addOneStat(STATS_SUMMON_COUNT, lvl / 100)
                
}
44 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_SAGE, lvl / 4)
                stats.addOneStat(STATS_ADD_CHAN, ((lvl / 1.65).toInt()))
                
}
42 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_SAGE, lvl / 4)
                stats.addOneStat(STATS_ADD_AGIL, ((lvl / 1.65).toInt()))
                
}
51 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_CHAN, lvl / 2)
                stats.addOneStat(STATS_ADD_AGIL, lvl / 2)
                
}
71 -> {stats.addOneStat(STATS_ADD_VITA, ((lvl * 1.5).toInt()))
                stats.addOneStat(STATS_ADD_FORC, ((lvl / 1.65).toInt()))
                
}
70 -> {stats.addOneStat(STATS_ADD_VITA, ((lvl * 1.5).toInt()))
                stats.addOneStat(STATS_ADD_INTE, ((lvl / 1.65).toInt()))
                
}
41 -> {stats.addOneStat(STATS_ADD_INIT, lvl * 5)
                stats.addOneStat(STATS_ADD_VITA, lvl / 2)
                stats.addOneStat(STATS_ADD_FORC, ((lvl / 1.65).toInt()))
                stats.addOneStat(STATS_SUMMON_COUNT, lvl / 100)
                
}
40 -> {stats.addOneStat(STATS_ADD_INIT, lvl * 5)
                stats.addOneStat(STATS_ADD_VITA, lvl / 2)
                stats.addOneStat(STATS_ADD_INTE, ((lvl / 1.65).toInt()))
                stats.addOneStat(STATS_SUMMON_COUNT, lvl / 100)
                
}
49 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_SAGE, lvl / 4)
                stats.addOneStat(STATS_ADD_FORC, ((lvl / 1.65).toInt()))
                
}
16 -> {stats.addOneStat(STATS_ADD_VITA, lvl / 2)
                stats.addOneStat(STATS_ADD_PERDOM, lvl / 2)
                
}
15 -> {stats.addOneStat(STATS_ADD_VITA, lvl / 2)
                stats.addOneStat(STATS_ADD_PROS, ((lvl / 1.25).toInt()))
                
}
11 -> {stats.addOneStat(STATS_ADD_VITA, lvl * 2); // 100*2 = 200
                stats.addOneStat(STATS_ADD_PERDOM, ((lvl / 2.5).toInt())); // = 40
                
}
69 -> {stats.addOneStat(STATS_ADD_VITA, lvl * 2)
                stats.addOneStat(STATS_ADD_PROS, ((lvl / 2.50).toInt()))
                
}
39 -> {stats.addOneStat(STATS_ADD_INIT, lvl * 5)
                stats.addOneStat(STATS_ADD_VITA, lvl / 2)
                stats.addOneStat(STATS_ADD_PROS, ((lvl / 2.50).toInt()))
                stats.addOneStat(STATS_SUMMON_COUNT, lvl / 100)
                
}
45 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_PERDOM, ((lvl / 2.5).toInt()))
                stats.addOneStat(STATS_ADD_SAGE, lvl / 4)
                
}
47 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_PROS, ((lvl / 2.50).toInt()))
                stats.addOneStat(STATS_ADD_SAGE, lvl / 4)
                
}
61 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_CHAN, ((lvl / 2.50).toInt()))
                stats.addOneStat(STATS_ADD_PERDOM, ((lvl / 2.5).toInt()))
                
}
63 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_CHAN, ((lvl / 1.65).toInt()))
                stats.addOneStat(STATS_ADD_PROS, ((lvl / 2.5).toInt()))
                
}
9 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_AGIL, ((lvl / 2.50).toInt()))
                stats.addOneStat(STATS_ADD_PERDOM, ((lvl / 2.5).toInt()))
                
}
52 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_AGIL, ((lvl / 1.65).toInt()))
                stats.addOneStat(STATS_ADD_PROS, ((lvl / 2.50).toInt()))
                
}
68 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_FORC, ((lvl / 1.65).toInt()))
                stats.addOneStat(STATS_ADD_PERDOM, ((lvl / 2.5).toInt()))
                
}
73 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_FORC, ((lvl / 1.65).toInt()))
                stats.addOneStat(STATS_ADD_PROS, ((lvl / 2.50).toInt()))
                
}
72 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_INTE, ((lvl / 1.65).toInt()))
                stats.addOneStat(STATS_ADD_PROS, ((lvl / 2.5).toInt()))
                
}
66 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_PERDOM, ((lvl / 2.5).toInt()))
                stats.addOneStat(STATS_ADD_PROS, ((lvl / 2.50).toInt()))
                
}
21 -> {stats.addOneStat(STATS_ADD_VITA, lvl * 2)
                stats.addOneStat(STATS_ADD_PM, lvl / 100)
                
}
23 -> {stats.addOneStat(STATS_ADD_VITA, lvl * 2); // 100*2 = 200
                stats.addOneStat(STATS_ADD_PO, lvl / 50)
                
}
57 -> {stats.addOneStat(STATS_ADD_VITA, lvl * 3); // 100*3 = 300
                stats.addOneStat(STATS_ADD_PM, lvl / 100)
                
}
84 -> {stats.addOneStat(STATS_ADD_VITA, lvl * 3)
                stats.addOneStat(STATS_ADD_PO, lvl / 100)
                
}
35 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_PM, lvl / 100)
                stats.addOneStat(STATS_SUMMON_COUNT, lvl / 100)
                stats.addOneStat(STATS_ADD_INIT, lvl * 5)
                
}
77 -> {stats.addOneStat(STATS_ADD_VITA, lvl * 2)
                stats.addOneStat(STATS_ADD_INIT, lvl * 5)
                stats.addOneStat(STATS_ADD_PO, lvl / 100)
                stats.addOneStat(STATS_SUMMON_COUNT, lvl / 100)
                
}
43 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_SAGE, lvl / 4)
                stats.addOneStat(STATS_ADD_PM, lvl / 100)
                
}
78 -> {stats.addOneStat(STATS_ADD_VITA, lvl * 2)
                stats.addOneStat(STATS_ADD_SAGE, lvl / 4)
                stats.addOneStat(STATS_ADD_PO, lvl / 100)
                
}
55 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_CHAN, ((lvl / 3.33).toInt()))
                stats.addOneStat(STATS_ADD_PM, lvl / 100)
                
}
82 -> {stats.addOneStat(STATS_ADD_VITA, lvl * 2)
                stats.addOneStat(STATS_ADD_CHAN, ((lvl / 1.65).toInt()))
                stats.addOneStat(STATS_ADD_PO, lvl / 100)
                
}
50 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_AGIL, ((lvl / 3.33).toInt()))
                stats.addOneStat(STATS_ADD_PM, lvl / 100)
                
}
79 -> {stats.addOneStat(STATS_ADD_VITA, lvl * 2)
                stats.addOneStat(STATS_ADD_AGIL, ((lvl / 1.65).toInt()))
                stats.addOneStat(STATS_ADD_PO, lvl / 100)
                
}
60 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_FORC, ((lvl / 3.33).toInt()))
                stats.addOneStat(STATS_ADD_PM, lvl / 100)
                
}
87 -> {stats.addOneStat(STATS_ADD_VITA, lvl * 2)
                stats.addOneStat(STATS_ADD_FORC, ((lvl / 1.65).toInt()))
                stats.addOneStat(STATS_ADD_PO, lvl / 100)
                
}
59 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_INTE, ((lvl / 3.33).toInt()))
                stats.addOneStat(STATS_ADD_PM, lvl / 100)
                
}
86 -> {stats.addOneStat(STATS_ADD_VITA, lvl * 2)
                stats.addOneStat(STATS_ADD_INTE, ((lvl / 1.65).toInt()))
                stats.addOneStat(STATS_ADD_PO, lvl / 100)
                
}
56 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_PERDOM, ((lvl / 3.33).toInt()))
                stats.addOneStat(STATS_ADD_PM, lvl / 100)
                
}
83 -> {stats.addOneStat(STATS_ADD_VITA, lvl * 2)
                stats.addOneStat(STATS_ADD_PERDOM, ((lvl / 1.65).toInt()))
                stats.addOneStat(STATS_ADD_PO, lvl / 100)
                
}
58 -> {stats.addOneStat(STATS_ADD_VITA, lvl)
                stats.addOneStat(STATS_ADD_PROS, ((lvl / 3.33).toInt()))
                stats.addOneStat(STATS_ADD_PM, lvl / 100)
                
}
85 -> {stats.addOneStat(STATS_ADD_VITA, lvl * 2)
                stats.addOneStat(STATS_ADD_PROS, ((lvl / 1.65).toInt()))
                stats.addOneStat(STATS_ADD_PO, lvl / 100)
                
}
80 -> {stats.addOneStat(STATS_ADD_VITA, lvl * 2)
                stats.addOneStat(STATS_ADD_PM, lvl / 100)
                stats.addOneStat(STATS_ADD_PO, lvl / 100)
                
}
88 -> {stats.addOneStat(STATS_ADD_PERDOM, lvl / 2)
                stats.addOneStat(STATS_ADD_RP_AIR, lvl / 20)
                stats.addOneStat(STATS_ADD_RP_EAU, lvl / 20)
                stats.addOneStat(STATS_ADD_RP_TER, lvl / 20)
                stats.addOneStat(STATS_ADD_RP_FEU, lvl / 20)
                stats.addOneStat(STATS_ADD_RP_NEU, lvl / 20)
                
}
75 -> {stats.addOneStat(STATS_ADD_PERDOM, lvl / 2)
                stats.addOneStat(STATS_ADD_RP_AIR, lvl / 20)
                stats.addOneStat(STATS_ADD_RP_EAU, lvl / 20)
                stats.addOneStat(STATS_ADD_RP_TER, lvl / 20)
                stats.addOneStat(STATS_ADD_RP_FEU, lvl / 20)
                stats.addOneStat(STATS_ADD_RP_NEU, lvl / 20)
                
}
}
        return stats
    }

    @JvmStatic fun getParchoTemplateByMountColor(color: Int): ObjectTemplate? {
        when (color) {
2 -> {return World.world.getObjTemplate(7807)
}
3 -> {return World.world.getObjTemplate(7808)
}
4 -> {return World.world.getObjTemplate(7809)
}
9 -> {return World.world.getObjTemplate(7810)
}
10 -> {return World.world.getObjTemplate(7811)
}
11 -> {return World.world.getObjTemplate(7812)
}
12 -> {return World.world.getObjTemplate(7813)
}
15 -> {return World.world.getObjTemplate(7814)
}
16 -> {return World.world.getObjTemplate(7815)
}
17 -> {return World.world.getObjTemplate(7816)
}
18 -> {return World.world.getObjTemplate(7817)
}
19 -> {return World.world.getObjTemplate(7818)
}
20 -> {return World.world.getObjTemplate(7819)
}
21 -> {return World.world.getObjTemplate(7820)
}
22 -> {return World.world.getObjTemplate(7821)
}
23 -> {return World.world.getObjTemplate(7822)
}
33 -> {return World.world.getObjTemplate(7823)
}
34 -> {return World.world.getObjTemplate(7824)
}
35 -> {return World.world.getObjTemplate(7825)
}
36 -> {return World.world.getObjTemplate(7826)
}
37 -> {return World.world.getObjTemplate(7827)
}
38 -> {return World.world.getObjTemplate(7828)
}
39 -> {return World.world.getObjTemplate(7829)
}
40 -> {return World.world.getObjTemplate(7830)
}
41 -> {return World.world.getObjTemplate(7831)
}
42 -> {return World.world.getObjTemplate(7832)
}
43 -> {return World.world.getObjTemplate(7833)
}
44 -> {return World.world.getObjTemplate(7834)
}
45 -> {return World.world.getObjTemplate(7835)
}
46 -> {return World.world.getObjTemplate(7836)
}
47 -> {return World.world.getObjTemplate(7837)
}
48 -> {return World.world.getObjTemplate(7838)
}
49 -> {return World.world.getObjTemplate(7839)
}
50 -> {return World.world.getObjTemplate(7840)
}
51 -> {return World.world.getObjTemplate(7841)
}
52 -> {return World.world.getObjTemplate(7842)
}
53 -> {return World.world.getObjTemplate(7843)
}
54 -> {return World.world.getObjTemplate(7844)
}
55 -> {return World.world.getObjTemplate(7845)
}
56 -> {return World.world.getObjTemplate(7846)
}
57 -> {return World.world.getObjTemplate(7847)
}
58 -> {return World.world.getObjTemplate(7848)
}
59 -> {return World.world.getObjTemplate(7849)
}
60 -> {return World.world.getObjTemplate(7850)
}
61 -> {return World.world.getObjTemplate(7851)
}
62 -> {return World.world.getObjTemplate(7852)
}
63 -> {return World.world.getObjTemplate(7853)
}
64 -> {return World.world.getObjTemplate(7854)
}
65 -> {return World.world.getObjTemplate(7855)
}
66 -> {return World.world.getObjTemplate(7856)
}
67 -> {return World.world.getObjTemplate(7857)
}
68 -> {return World.world.getObjTemplate(7858)
}
69 -> {return World.world.getObjTemplate(7859)
}
70 -> {return World.world.getObjTemplate(7860)
}
71 -> {return World.world.getObjTemplate(7861)
}
72 -> {return World.world.getObjTemplate(7862)
}
73 -> {return World.world.getObjTemplate(7863)
}
74 -> {return World.world.getObjTemplate(7864)
}
75 -> {return World.world.getObjTemplate(7865)
}
76 -> {return World.world.getObjTemplate(7866)
}
77 -> {return World.world.getObjTemplate(7867)
}
78 -> {return World.world.getObjTemplate(7868)
}
79 -> {return World.world.getObjTemplate(7869)
}
80 -> {return World.world.getObjTemplate(7870)
}
82 -> {return World.world.getObjTemplate(7871)
}
83 -> {return World.world.getObjTemplate(7872)
}
84 -> {return World.world.getObjTemplate(7873)
}
85 -> {return World.world.getObjTemplate(7874)
}
86 -> {return World.world.getObjTemplate(7875)
}
87 -> {return World.world.getObjTemplate(7876)
}
88 -> {return World.world.getObjTemplate(9582)
}
else -> {return getParchoTemplateByMountColor(Formulas.getRandomValue(2, 88))
}
}
    }

    @JvmStatic fun getMountColorByParchoTemplate(templateId: Int): Int {
        if(templateId == 7806) {
            var color: Int = -1
            var template: ObjectTemplate? = null
            while(template == null) {
                color = Formulas.getRandomValue(2, 88)
                template = getParchoTemplateByMountColor(color)
            }
            return color
        }
        for (a in 1 until 100) {
            var template: ObjectTemplate? = getParchoTemplateByMountColor(a)
            if (template != null) {
                if (template!!.id == templateId) {
                    return a
                }
            }
        }
        return -1
    }

    @JvmStatic fun isValidPlaceForItem(template: ObjectTemplate, place: Int): Boolean {
        if (template.type == 41 && place == ITEM_POS_DRAGODINDE)
            return true

        when (template.type) {
ITEM_TYPE_AMULETTE -> {if (place == ITEM_POS_AMULETTE)
                    return true
                
}
113 -> {if ((template!!.id == 9233) && (place == 7))
                    return true
                if ((template!!.id == 9234) && (place == 6))
                    return true
                if ((template!!.id == 9255) && (place == 0))
                    return true
                if ((template!!.id == 9256)
                        && ((place == 2) || (place == 4)))
                    return true
                
}
114 -> {if (place == 1) // CaC
                    return true
                
}
ITEM_TYPE_ARC, ITEM_TYPE_BAGUETTE, ITEM_TYPE_BATON, ITEM_TYPE_DAGUES, ITEM_TYPE_EPEE, ITEM_TYPE_MARTEAU, ITEM_TYPE_PELLE, ITEM_TYPE_HACHE, ITEM_TYPE_OUTIL, ITEM_TYPE_PIOCHE, ITEM_TYPE_FAUX, ITEM_TYPE_PIERRE_AME, ITEM_TYPE_FILET_CAPTURE -> {if (place == ITEM_POS_ARME)
                    return true
                
}
ITEM_TYPE_ANNEAU -> {if (place == ITEM_POS_ANNEAU1 || place == ITEM_POS_ANNEAU2)
                    return true
                
}
ITEM_TYPE_CEINTURE -> {if (place == ITEM_POS_CEINTURE)
                    return true
                
}
ITEM_TYPE_BOTTES -> {if (place == ITEM_POS_BOTTES)
                    return true
                
}
ITEM_TYPE_COIFFE -> {if (place == ITEM_POS_COIFFE)
                    return true
                
}
ITEM_TYPE_CAPE, ITEM_TYPE_SAC_DOS -> {if (place == ITEM_POS_CAPE)
                    return true
                
}
ITEM_TYPE_FAMILIER -> {if (place == ITEM_POS_FAMILIER)
                    return true
                
}
ITEM_TYPE_DOFUS -> {if (place == ITEM_POS_DOFUS1 || place == ITEM_POS_DOFUS2
                        || place == ITEM_POS_DOFUS3 || place == ITEM_POS_DOFUS4
                        || place == ITEM_POS_DOFUS5 || place == ITEM_POS_DOFUS6)
                    return true
                
}
ITEM_TYPE_BOUCLIER -> {if (place == ITEM_POS_BOUCLIER)
                    return true
                
}
ITEM_TYPE_POTION, ITEM_TYPE_PARCHO_EXP, ITEM_TYPE_BOOST_FOOD, ITEM_TYPE_PAIN, ITEM_TYPE_BIERE, ITEM_TYPE_POISSON, ITEM_TYPE_BONBON, ITEM_TYPE_COMESTI_POISSON, ITEM_TYPE_VIANDE, ITEM_TYPE_VIANDE_CONSERVEE, ITEM_TYPE_VIANDE_COMESTIBLE, ITEM_TYPE_TEINTURE, ITEM_TYPE_MAITRISE, ITEM_TYPE_BOISSON, ITEM_TYPE_PIERRE_AME_PLEINE, ITEM_TYPE_PARCHO_RECHERCHE, ITEM_TYPE_CADEAUX, ITEM_TYPE_OBJET_ELEVAGE, ITEM_TYPE_OBJET_UTILISABLE, ITEM_TYPE_PRISME, ITEM_TYPE_FEE_ARTIFICE, ITEM_TYPE_DONS -> {if (place >= 35 && place <= 48)
                    return true
                
}
}
        return false
    }

	/*
     * public static boolean feedMount(int type) { for (Integer feed :
	 * Main.itemFeedMount) { if (type == feed) return true; } return false; }
	 */

    @JvmStatic fun tpCim(perso: Player) {
        var idSuperArea: Int = perso.curMap.area!!.superArea
        var idArea: Int = perso.curMap.area!!.id

        if(idSuperArea == INCARNAM_SUPERAREA) {
            perso.teleport(10342, 222)
            return
        }

        when (idArea) {
0, 5, 29, 39, 40, 43, 44 -> {perso.teleport(1174, 279)
                
}
3, 4, 6, 18, 25, 27, 41 -> {perso.teleport(8534, 196)
                
}
2 -> {perso.teleport(420, 408)
                
}
1 -> {perso.teleport(844, 370)
                
}
7 -> {perso.teleport(4285, 572)
                
}
8, 14, 15, 16, 32 -> {perso.teleport(4748, 133)
                
}
11, 12, 13, 33 -> {perso.teleport(5719, 196)
                
}
19, 22, 23 -> {perso.teleport(7910, 381)
                
}
20, 21, 24 -> {perso.teleport(8054, 115)
                
}
28, 34, 35, 36 -> {perso.teleport(9231, 257)
                
}
30 -> {perso.teleport(9539, 128)
                
}
31 -> {if (perso.isGhost)
                    perso.teleport(9558, 268)
                else
                    perso.teleport(9558, 224)
                
}
37 -> {perso.teleport(7796, 433)
                
}
42 -> {perso.teleport(8534, 196)
                
}
46 -> {perso.teleport(10422, 327)
                
}
47 -> {perso.teleport(10590, 302)
                
}
26 -> {perso.teleport(9398, 268)
// fallthrough
perso.teleport(8534, 196)
                
}
else -> {perso.teleport(8534, 196)

}
}
    }

    @JvmStatic fun isTaverne(map: GameMap): Boolean {
        when (map.id) {
10354, 7573, 7572, 7574, 465, 463, 6064, 461, 462, 5867, 6197, 6021, 6044, 8196, 6055, 8195, 1905, 1907, 6049 -> {return true
        
}
}
        return false
    }

    @JvmStatic fun getLevelForChevalier(target: Player): Int {
        var lvl: Int = target.level
        if (lvl <= 50)
            return 50
        if ((lvl <= 80) && (lvl > 50))
            return 80
        if ((lvl <= 110) && (lvl > 80))
            return 110
        if ((lvl <= 140) && (lvl > 110))
            return 140
        if ((lvl <= 170) && (lvl > 140))
            return 170
        if ((lvl <= 500) && (lvl > 170))
            return 200
        return 200
    }

    @JvmStatic fun getStatsOfCandy(id: Int, turn: Int): String {
        var a: String = World.world.getObjTemplate(id)!!.strTemplate
        a += ",32b#64#0#" + Integer.toHexString(turn) + "#0d0+1;"
        return a
    }

    @JvmStatic fun getStatsOfMascotte(): String {
        var a: String = Integer.toHexString(148) + "#0#0#0#0d0+1,"
        a += "32b#64#0#" + Integer.toHexString(1) + "#0d0+1;"
        return a
    }


    @JvmStatic fun getStringColorDragodinde(color: Int): String {
        when (color) {
1 -> {return "16772045,-1,16772045"
}
3 -> {return "1245184,393216,1245184"
}
6 -> {return "16747520,-1,16747520"
}
9 -> {return "1182992,16777200,16777200"
}
10 -> {return "16747520,-1,16747520"
}
11 -> {return "16747520,16777200,16777200"
}
12 -> {return "16747520,1703936,1774084"
}
15 -> {return "4251856,-1,4251856"
}
16 -> {return "16777200,16777200,16777200"
}
17 -> {return "4915330,-1,4915330"
}
18 -> {return "16766720,16766720,16766720"
}
19 -> {return "14423100,-1,14423100"
}
20 -> {return "16772045,-1,16772045"
}
21 -> {return "3329330,-1,3329330"
}
22 -> {return "15859954,16777200,15859954"
}
23 -> {return "14524637,-1,14524637"
}
33 -> {return "16772045,16766720,16766720"
}
34 -> {return "16772045,1245184,1245184"
}
35 -> {return "16772045,3329330,3329330"
}
36 -> {return "16772045,4915330,4915330"
}
37 -> {return "16772045,16777200,16777200"
}
38 -> {return "16772045,16747520,16747520"
}
39 -> {return "16772045,4251856,4251856"
}
40 -> {return "16772045,15859954,15859954"
}
41 -> {return "16772045,14423100,14423100"
}
42 -> {return "1245184,16766720,16766720"
}
43 -> {return "16766720,3329330,3329330"
}
44 -> {return "16766720,4915330,4915330"
}
45 -> {return "16766720,16777200,16777200"
}
46 -> {return "16766720,16747520,16747520"
}
47 -> {return "16766720,4251856,4251856"
}
48 -> {return "16766720,15859954,15859954"
}
49 -> {return "16766720,14423100,14423100"
}
50 -> {return "1245184,3329330,3329330"
}
51 -> {return "4915330,4915330,1245184"
}
52 -> {return "1245184,4251856,4251856"
}
53 -> {return "15859954,0,0"
}
54 -> {return "14423100,14423100,1245184"
}
55 -> {return "3329330,4915330,4915330"
}
56 -> {return "3329330,16777200,16777200"
}
57 -> {return "3329330,16747520,16747520"
}
58 -> {return "3329330,4251856,4251856"
}
59 -> {return "3329330,15859954,15859954"
}
60 -> {return "3329330,14423100,14423100"
}
61 -> {return "4915330,16777200,16777200"
}
62 -> {return "4915330,16747520,16747520"
}
63 -> {return "4915330,4251856,4251856"
}
64 -> {return "4915330,15859954,15859954"
}
65 -> {return "14423100,4915330,4915330"
}
66 -> {return "16777200,4251856,4251856"
}
67 -> {return "16777200,16731355,16711910"
}
68 -> {return "14423100,16777200,16777200"
}
69 -> {return "4251856,16747520,16747520"
}
70 -> {return "14315734,16747520,16747520"
}
71 -> {return "14423100,16747520,16747520"
}
72 -> {return "15859954,4251856,4251856"
}
73 -> {return "14423100,4251856,4251856"
}
74 -> {return "16766720,16766720,16766720"
}
76 -> {return "14315734,14423100,14423100"
}
77 -> {return "14524637,16772045,16772045"
}
78 -> {return "14524637,16766720,16766720"
}
79 -> {return "14524637,1245184,1245184"
}
80 -> {return "14524637,3329330,3329330"
}
82 -> {return "14524637,4915330,4915330"
}
83 -> {return "14524637,16777200,16777200"
}
84 -> {return "14524637,16747520,16747520"
}
85 -> {return "14524637,4251856,4251856"
}
86 -> {return "14524637,15859954,15859954"
}
87 -> {return "14524637,14423100,14423100"
}
else -> {return "-1,-1,-1"
        
}
}
    }

    @JvmStatic fun getGeneration(color: Int): Int {
        when (color) {
10, 18, 20 -> {return 1
}
33, 38, 46 -> {return 2
}
3, 17 -> {return 3
}
62, 12, 36, 34, 44, 42, 51 -> {return 4
}
19, 22 -> {return 5
}
71, 70, 41, 40, 49, 48, 65, 64, 54, 53, 76 -> {return 6
}
15, 16 -> {return 7
}
11, 69, 37, 39, 45, 47, 61, 63, 9, 52, 68, 73, 67, 72, 66 -> {return 8
}
21, 23 -> {return 9
}
57, 35, 43, 50, 55, 56, 58, 59, 60, 77, 78, 79, 80, 82, 83, 84, 85, 86 -> {return 10
}
else -> {return 1
        
}
}
    }

    @JvmStatic fun colorToEtable(player: Player, mother: Mount, father: Mount): Int {
        var color1: Int = 0
        var color2: Int = 0
        var A: Int = 0
        var B: Int = 0
        var C: Int = 0
        var splitM: List<String> = mother.ancestors.split(",")
        var splitF: List<String> = father.ancestors.split(",")
        var random: RandomStats<Int> = RandomStats()

        var i: Int = 0
        for (str in  splitM) {
            i++
            if (str.equals("?")) continue

            var pct: Int = 1

            when (i) {
1, 2 -> {pct = 25
                    
}
3, 4, 5, 6 -> {pct = 10
            
}
}

            random.add(pct, str.toInt())
        }

        random.add(if (random.size() == 0) 100 else 33, mother.color)
        color1 = random.get()

        random = RandomStats()
        i = 0
        for (str in  splitF) {
            i++
            if (str.equals("?")) continue

            var pct: Int = 1

            when (i) {
1, 2 -> {pct = 25
                    
}
3, 4, 5, 6 -> {pct = 10
            
}
}

            random.add(pct, str.toInt())
        }

        random.add(if (random.size() == 0) 100 else 33, father.color)
        color2 = random.get()

        if (color1 == 75)
            color1 = 10
        if (color2 == 75)
            color2 = 10

        if (color1 > color2) {
            A = color2;// moins
            B = color1;// supérieur
        } else if (color1 <= color2) {
            A = color1;// moins
            B = color2;// supérieur
        }
        if (A == 10 && B == 18)
            C = 46; // Rousse y Dorée
        else if (A == 10 && B == 20)
            C = 38; // Rousse y Amande
        else if (A == 18 && B == 20)
            C = 33; // Amande y Dorée
        else if (A == 33 && B == 38)
            C = 17; // Indigo
        else if (A == 33 && B == 46)
            C = 3;// Ebène
        else if (A == 10 && B == 17)
            C = 62; // Rousse e Indigo
        else if (A == 10 && B == 3)
            C = 12; // Ebène y Rousse
        else if (A == 17 && B == 20)
            C = 36; // Amande - Indigo
        else if (A == 3 && B == 20)
            C = 34; // Amande - Ebène
        else if (A == 17 && B == 18)
            C = 44; // Dorée - Indigo
        else if (A == 3 && B == 18)
            C = 42; // Dorée - Ebène
        else if (A == 3 && B == 17)
            C = 51; // Ebène - Indigo
        else if (A == 38 && B == 51)
            C = 19; // Purpre
        else if (A == 46 && B == 51)
            C = 22; // Orchidée
        else if (A == 10 && B == 19)
            C = 71; // Purpre - Rousse
        else if (A == 10 && B == 22)
            C = 70; // Orchidée - Rousse
        else if (A == 19 && B == 20)
            C = 41; // Amande - Purpre
        else if (A == 20 && B == 22)
            C = 40; // Amande - Orchidée
        else if (A == 18 && B == 19)
            C = 49; // Dorée - Purpre
        else if (A == 18 && B == 22)
            C = 48; // Dorée - Orchidée
        else if (A == 17 && B == 19)
            C = 65; // Indigo - Purpre
        else if (A == 17 && B == 22)
            C = 64; // Indigo - Orchidée
        else if (A == 3 && B == 19)
            C = 54; // Ebène - Purpre
        else if (A == 3 && B == 22)
            C = 53; // Ebène - Orchidée
        else if (A == 19 && B == 22)
            C = 76; // Orchidée - Purpre
        else if (A == 53 && B == 76)
            C = 15; // Turquoise
        else if (A == 65 && B == 76)
            C = 16; // Ivoire
        else if (A == 10 && B == 16)
            C = 11; // Ivoire - Rousse
        else if (A == 10 && B == 15)
            C = 69; // Turquoise - Rousse
        else if (A == 16 && B == 20)
            C = 37; // Amande - Ivoire
        else if (A == 15 && B == 20)
            C = 39; // Amande - Turquoise
        else if (A == 16 && B == 18)
            C = 45; // Dorée - Ivoire
        else if (A == 15 && B == 18)
            C = 47; // Dorée - Turquoise
        else if (A == 16 && B == 17)
            C = 61; // Indigo - Ivoire
        else if (A == 15 && B == 17)
            C = 63; // Indigo - Turquoise
        else if (A == 3 && B == 16)
            C = 9; // Ebène - Ivoire
        else if (A == 3 && B == 15)
            C = 52; // Ebène - Turquoise
        else if (A == 16 && B == 19)
            C = 68; // Ivoire - Purpre
        else if (A == 15 && B == 19)
            C = 73; // Turquoise - Purpre
        else if (A == 16 && B == 22)
            C = 67; // Ivoire - Orchidée
        else if (A == 15 && B == 22)
            C = 72; // Orchidée - Turquoise
        else if (A == 15 && B == 16)
            C = 66; // Ivoire - Turquoise
        else if (A == 66 && B == 68)
            C = 21; // Emeraude
        else if (A == 66 && B == 72)
            C = 23; // Prune
        else if (A == 10 && B == 21)
            C = 57;// Emeraude - Rousse
        else if (A == 20 && B == 21)
            C = 35; // Amande - Emeraude
        else if (A == 18 && B == 21)
            C = 43; // Dorée - Emeraude
        else if (A == 3 && B == 21)
            C = 50; // Ebène - Emeraude
        else if (A == 17 && B == 21)
            C = 55; // Emeraude - Indigo
        else if (A == 16 && B == 21)
            C = 56; // Emeraude - Ivoire
        else if (A == 15 && B == 21)
            C = 58; // Emeraude - Turquoise
        else if (A == 21 && B == 22)
            C = 59; // Emeraude - Orchidée
        else if (A == 19 && B == 21)
            C = 60; // Emeraude - Purpre
        else if (A == 20 && B == 23)
            C = 77; // Prune - Amande
        else if (A == 18 && B == 23)
            C = 78; // Prune - Dorée
        else if (A == 3 && B == 23)
            C = 79; // Prune - Ebène
        else if (A == 21 && B == 23)
            C = 80; // Prune - Emeraude
        else if (A == 17 && B == 23)
            C = 82; // Prune - Indigo
        else if (A == 16 && B == 23)
            C = 83; // Prune - Ivoire
        else if (A == 10 && B == 23)
            C = 84; // Prune - Rousse
        else if (A == 15 && B == 23)
            C = 85; // Prune - Turquoise
        else if (A == 22 && B == 23)
            C = 86; // Prune - Orchidée
        else if (A == 19 && B == 23)
            C = 87; // Prune - Purpre
        else if (A == B)
            A = B
            C = A
        if (C == 0) {

            random = RandomStats()
            i = 0
            for (str in  splitF) {
                i++
                if (str.equals("?")) continue

                var pct: Int = 1

                when (i) {
1, 2 -> {pct = 25
                        
}
3, 4, 5, 6 -> {pct = 10
                
}
}

                random.add(pct, str.toInt())
            }
            i = 0
            for (str in  splitM) {
                i++
                if (str.equals("?")) continue

                var pct: Int = 1

                when (i) {
1, 2 -> {pct = 25
                        
}
3, 4, 5, 6 -> {pct = 10
                
}
}

                random.add(pct, str.toInt())
            }
            C = random.get()
            //player.sendMessage("Merci de Poster sur le forum afin de débug l'élevage ! C = 0, A = " + A + ", et B = " + B + ". Valeur finale : " + C + ". Message bien évidement sérieux.");

            return C
        }
        random = RandomStats()
        random.add(33, A)
        random.add(33, B)
        random.add(33, C)
        return random.get()
    }

    @JvmStatic fun getParchoByIdPets(id: Int): Int {
        when (id) {
10802 -> {return 10806
}
10107 -> {return 10135
}
10106 -> {return 10134
}
9795 -> {return 9810
}
9624 -> {return 9685
}
9623 -> {return 9684
}
9620 -> {return 9683
}
9619 -> {return 9682
}
9617 -> {return 9675
}
9594 -> {return 9598
}
8693 -> {return 8707
}
8677 -> {return 8684
}
8561 -> {return 8564
}
8211 -> {return 8544
}
8155 -> {return 8179
}
8154 -> {return 8178
}
8153 -> {return 8175
}
8151 -> {return 8176
}
8000 -> {return 8180
}
7911 -> {return 8526
}
7892 -> {return 7896
}
7891 -> {return 7895
}
7714 -> {return 8708
}
7713 -> {return 9681
}
7712 -> {return 9680
}
7711 -> {return 9679
}
7710 -> {return 9678
}
7709 -> {return 9677
}
7708 -> {return 9676
}
7707 -> {return 9674
}
7706 -> {return 8685
}
7705 -> {return 8889
}
7704 -> {return 8888
}
7703 -> {return 8421
}
7524 -> {return 8887
}
7522 -> {return 7535
}
7520 -> {return 7533
}
7519 -> {return 7534
}
7518 -> {return 7532
}
7415 -> {return 7419
}
7414 -> {return 7418
}
6978 -> {return 7417
}
6716 -> {return 7420
}
2077 -> {return 2098
}
2076 -> {return 2101
}
2075 -> {return 2100
}
2074 -> {return 2099
}
1748 -> {return 2102
}
1728 -> {return 1735
        
}
}
        return -1
    }

    @JvmStatic fun getPetsByIdParcho(id: Int): Int {
        when (id) {
10806 -> {return 10802
}
10135 -> {return 10107
}
10134 -> {return 10106
}
9810 -> {return 9795
}
9685 -> {return 9624
}
9684 -> {return 9623
}
9683 -> {return 9620
}
9682 -> {return 9619
}
9675 -> {return 9617
}
9598 -> {return 9594
}
8707 -> {return 8693
}
8684 -> {return 8677
}
8564 -> {return 8561
}
8544 -> {return 8211
}
8179 -> {return 8155
}
8178 -> {return 8154
}
8175 -> {return 8153
}
8176 -> {return 8151
}
8180 -> {return 8000
}
8526 -> {return 7911
}
7896 -> {return 7892
}
7895 -> {return 7891
}
8708 -> {return 7714
}
9681 -> {return 7713
}
9680 -> {return 7712
}
9679 -> {return 7711
}
9678 -> {return 7710
}
9677 -> {return 7709
}
9676 -> {return 7708
}
9674 -> {return 7707
}
8685 -> {return 7706
}
8889 -> {return 7705
}
8888 -> {return 7704
}
8421 -> {return 7703
}
8887 -> {return 7524
}
7535 -> {return 7522
}
7533 -> {return 7520
}
7534 -> {return 7519
}
7532 -> {return 7518
}
7419 -> {return 7415
}
7418 -> {return 7414
}
7417 -> {return 6978
}
7420 -> {return 6716
}
2098 -> {return 2077
}
2101 -> {return 2076
}
2100 -> {return 2075
}
2099 -> {return 2074
}
2102 -> {return 1748
}
1735 -> {return 1728
        
}
}
        return -1
    }

    @JvmStatic fun getDoplonDopeul(IDmob: Int): Int {
        when (IDmob) {
168 -> {return 10302
}
165 -> {return 10303
}
166 -> {return 10304
}
162 -> {return 10305
}
160 -> {return 10306
}
167 -> {return 10307
}
161 -> {return 10308
}
2691 -> {return 10309
}
455 -> {return 10310
}
169 -> {return 10311
}
163 -> {return 10312
}
164 -> {return 10313
        
}
}
        return -1
    }

    @JvmStatic fun getIDdoplonByMapID(IDmap: Int): Int {
        when (IDmap) {
6926 -> {return 10312
}
1470 -> {return 10305
}
1461 -> {return 10303
}
6949 -> {return 10310
}
1556 -> {return 10302
}
1549 -> {return 10307
}
1469 -> {return 10313
}
487 -> {return 10304
}
490 -> {return 10308
}
177 -> {return 10306
}
1466 -> {return 10311
}
8207 -> {return 10309
        
}
}
        return -1
    }

    @JvmStatic fun getArmeSoin(idArme: Int): Int {
        when (idArme) {
7172 -> {return 100
}
7156 -> {return 80
}
1355 -> {return 42
}
7182 -> {return 100
}
7040 -> {return 10
}
6539 -> {return 80
}
6519 -> {return 23
}
8118 -> {return 30
}
else -> {return -1
        
}
}
    }

    @JvmStatic fun getSectionByDopeuls(id: Int): Int {
        when (id) {
160 -> {return 1
}
161 -> {return 2
}
162 -> {return 3
}
163 -> {return 4
}
164 -> {return 5
}
165 -> {return 6
}
166 -> {return 7
}
167 -> {return 8
}
168 -> {return 9
}
169 -> {return 10
}
455 -> {return 11
}
2691 -> {return 12
        
}
}
        return -1
    }

    @JvmStatic fun getCertificatByDopeuls(id: Int): Int {
        when (id) {
160 -> {return 10293
}
161 -> {return 10295
}
162 -> {return 10292
}
163 -> {return 10299
}
164 -> {return 10300
}
165 -> {return 10290
}
166 -> {return 10291
}
167 -> {return 10294
}
168 -> {return 10289
}
169 -> {return 10298
}
455 -> {return 10297
}
2691 -> {return 10296
        
}
}
        return -1
    }

    @JvmStatic fun isCertificatDopeuls(id: Int): Boolean {
        when (id) {
10293, 10295, 10292, 10299, 10300, 10290, 10291, 10294, 10289, 10298, 10297, 10296 -> {return true
        
}
}
        return false
    }

    @JvmStatic fun getItemIdByMascotteId(id: Int): Int {
        when (id) {
10118 -> {return 1498
}
10078 -> {return 70
}
10077 -> {return -1
}
10009 -> {return 90
}
9993 -> {return 71
}
9096 -> {return 30
}
9061 -> {return 40
}
8563 -> {return 1076
}
7425 -> {return 1588
}
7354 -> {return 1264
}
7353 -> {return 1076
}
7352 -> {return 1153
}
7351 -> {return 1248
}
7350 -> {return 1228
}
7062 -> {return 9001
}
6876 -> {return 1245
}
6875 -> {return 1249
}
6874 -> {return 70
}
6873 -> {return 1243
}
6872 -> {return 50
}
6871 -> {return 1247
}
6870 -> {return 1246
}
6869 -> {return 9043
}
6832 -> {return -1
}
6768 -> {return 9001
}
2272 -> {return 1577
}
2169 -> {return 1205
}
2152 -> {return 1001
}
2134 -> {return 1205
}
2132 -> {return 9004
}
2130 -> {return 1001
}
2082 -> {return 1208;//Marcassin
        
}
}
        return -1
    }

    @JvmStatic fun isIncarnationWeapon(id: Int): Boolean {
        when (id) {
9544, 9545, 9546, 9547, 9548, 10133, 10127, 10126, 10125 -> {return true
        
}
}
        return false
    }

    @JvmStatic fun isTourmenteurWeapon(id: Int): Boolean {
        when (id) {
9544, 9545, 9546, 9547, 9548 -> {return true
        
}
}
        return false
    }

    @JvmStatic fun isBanditsWeapon(id: Int): Boolean {
        when (id) {
10133, 10127, 10126, 10125 -> {return true
        
}
}
        return false
    }

    @JvmStatic fun getSpecialSpellByClasse(classe: Int): Int {
        when (classe) {
Constant.CLASS_FECA -> {return 422
}
Constant.CLASS_OSAMODAS -> {return 420
}
Constant.CLASS_ENUTROF -> {return 425
}
Constant.CLASS_SRAM -> {return 416
}
Constant.CLASS_XELOR -> {return 424
}
Constant.CLASS_ECAFLIP -> {return 412
}
Constant.CLASS_ENIRIPSA -> {return 427
}
Constant.CLASS_IOP -> {return 410
}
Constant.CLASS_CRA -> {return 418
}
Constant.CLASS_SADIDA -> {return 426
}
Constant.CLASS_SACRIEUR -> {return 421
}
Constant.CLASS_PANDAWA -> {return 423
        
}
}
        return 0
    }

    @JvmStatic fun isFlacGelee(id: Int): Boolean {
        when (id) {
2430, 2431, 2432, 2433 -> {return true
        
}
}
        return false
    }

    @JvmStatic fun isDoplon(id: Int): Boolean {
        when (id) {
10302, 10303, 10304, 10305, 10306, 10307, 10308, 10309, 10310, 10311, 10312, 10313 -> {return true
        
}
}
        return false
    }

    @JvmStatic fun isInMorphDonjon(id: Int): Boolean {
        when (id) {
8716, 8718, 8719, 9121, 9122, 9123, 8979, 8980, 8981, 8982, 8983, 8984, 9716 -> {return true
        
}
}
        return false
    }

    @JvmStatic fun getOppositeStats(statsId: Int): IntArray {
        if (statsId == 217)
            return intArrayOf(210, 211, 213, 214)
        else if (statsId == 216)
            return intArrayOf(210, 212, 213, 214)
        else if (statsId == 218)
            return intArrayOf(210, 211, 212, 214)
        else if (statsId == 219)
            return intArrayOf(210, 211, 212, 214)
        else if (statsId == 215)
            return intArrayOf(211, 212, 213, 214)
        return intArrayOf()
    }

    @JvmStatic fun getNearestCellIdUnused(player: Player): Int {
        val map: GameMap = player.curMap
        val width: Int = map.w
        val cell: Int = player.curCell.getId()
        val cells: IntArray = intArrayOf(cell - width, cell - width + 1, cell + width - 1, cell + width)
        var cellPosition: Int = -1

        for (available in  cells) {
            var c: GameCase? = map.getCase(available)
            if (c != null && c.getDroppedItem(false) == null && c.players.isEmpty() && c.isWalkable(player.fight!=null) && map.interactiveObjects?.get(available) == null) {
                return available
            }
        }
        return -1
    }

    @JvmStatic fun getWeaponBonusByClass(type: Int, classId: Int): Float {
        when (classId) {
Constant.CLASS_IOP -> {when (type) {
ITEM_TYPE_EPEE -> {return 100f
}
ITEM_TYPE_MARTEAU -> {return 95f
                
}
}
                
}
Constant.CLASS_OSAMODAS -> {when (type) {
ITEM_TYPE_MARTEAU -> {return 100f
}
ITEM_TYPE_BATON -> {return 95f
                
}
}
                
}
Constant.CLASS_XELOR -> {when (type) {
ITEM_TYPE_MARTEAU -> {return 100f
}
ITEM_TYPE_BAGUETTE -> {return 95f
                
}
}
                
}
Constant.CLASS_ENIRIPSA -> {when (type) {
ITEM_TYPE_BAGUETTE -> {return 100f
}
ITEM_TYPE_BATON -> {return 95f
                
}
}
                
}
Constant.CLASS_SRAM -> {when (type) {
ITEM_TYPE_DAGUES -> {return 100f
}
ITEM_TYPE_ARC -> {return 95f
                
}
}
                
}
Constant.CLASS_CRA -> {when (type) {
ITEM_TYPE_ARC -> {return 100f
}
ITEM_TYPE_DAGUES -> {return 95f
                
}
}
                
}
Constant.CLASS_SADIDA -> {when (type) {
ITEM_TYPE_BATON -> {return 100f
}
ITEM_TYPE_BAGUETTE -> {return 95f
                
}
}
                
}
Constant.CLASS_ENUTROF -> {when (type) {
ITEM_TYPE_PELLE -> {return 100f
}
ITEM_TYPE_MARTEAU -> {return 95f
                
}
}
                
}
Constant.CLASS_ECAFLIP -> {when (type) {
ITEM_TYPE_EPEE -> {return 100f
}
ITEM_TYPE_DAGUES -> {return 95f
                
}
}
                
}
Constant.CLASS_FECA -> {when (type) {
ITEM_TYPE_BATON -> {return 100f
}
ITEM_TYPE_BAGUETTE -> {return 95f
                
}
}
                
}
Constant.CLASS_PANDAWA -> {when (type) {
ITEM_TYPE_HACHE -> {return 100f
}
ITEM_TYPE_BATON -> {return 95f
                
}
}
                
}
else -> {return 90f
        
}
}
        return 90f
    }

    @JvmStatic fun getClassNameById(forbiddenClass: Byte): String {
        when (forbiddenClass.toInt()) {
CLASS_ENUTROF -> {return "Enutrof"
}
CLASS_SACRIEUR -> {return "Sacrieur"
}
CLASS_FECA -> {return "Féca"
}
CLASS_SADIDA -> {return "Sadida"
}
CLASS_SRAM -> {return "Sram"
}
CLASS_ENIRIPSA -> {return "Eniripsa"
}
CLASS_XELOR -> {return "Xelor"
}
CLASS_CRA -> {return "Crâ"
}
CLASS_ECAFLIP -> {return "Ecaflip"
}
CLASS_PANDAWA -> {return "Pandawa"
}
CLASS_OSAMODAS -> {return "Osamodas"
}
CLASS_IOP -> {return "Iop"
}
else -> {return "Undefined"
        
}
}
    }

    @JvmStatic fun getPositionByItemType(type: Int): IntArray {
        when (type) {
Constant.ITEM_TYPE_FAMILIER -> {return intArrayOf(Constant.ITEM_POS_FAMILIER)
}
Constant.ITEM_TYPE_COIFFE -> {return intArrayOf(Constant.ITEM_POS_COIFFE)
}
Constant.ITEM_TYPE_CAPE -> {return intArrayOf(Constant.ITEM_POS_CAPE)
}
Constant.ITEM_TYPE_ANNEAU -> {return intArrayOf(Constant.ITEM_POS_ANNEAU1, Constant.ITEM_POS_ANNEAU2)
}
Constant.ITEM_TYPE_CEINTURE -> {return intArrayOf(Constant.ITEM_POS_CEINTURE)
}
Constant.ITEM_TYPE_AMULETTE -> {return intArrayOf(Constant.ITEM_POS_AMULETTE)
}
Constant.ITEM_TYPE_BOTTES -> {return intArrayOf(Constant.ITEM_POS_BOTTES)
}
Constant.ITEM_TYPE_BOUCLIER -> {return intArrayOf(Constant.ITEM_POS_BOUCLIER)
}
Constant.ITEM_TYPE_DOFUS -> {return intArrayOf(Constant.ITEM_POS_DOFUS1, Constant.ITEM_POS_DOFUS2, Constant.ITEM_POS_DOFUS3,
                        Constant.ITEM_POS_DOFUS4, Constant.ITEM_POS_DOFUS5, Constant.ITEM_POS_DOFUS6)
}
Constant.ITEM_TYPE_ARC, Constant.ITEM_TYPE_EPEE, Constant.ITEM_TYPE_DAGUES, Constant.ITEM_TYPE_BATON, Constant.ITEM_TYPE_FAUX, Constant.ITEM_TYPE_PELLE, Constant.ITEM_TYPE_HACHE, Constant.ITEM_TYPE_BAGUETTE -> {return intArrayOf(Constant.ITEM_POS_ARME)
}
else -> {return intArrayOf()
        
}
}
    }

    @JvmStatic fun isTypeWeapon(type: Int): Boolean {
        when (type) {
ITEM_TYPE_ARME_MAGIQUE, ITEM_TYPE_ARBALETE, ITEM_TYPE_ARC, ITEM_TYPE_BAGUETTE, ITEM_TYPE_BATON, ITEM_TYPE_DAGUES, ITEM_TYPE_EPEE, ITEM_TYPE_MARTEAU, ITEM_TYPE_PELLE, ITEM_TYPE_HACHE, ITEM_TYPE_OUTIL, ITEM_TYPE_PIOCHE, ITEM_TYPE_FAUX -> {return true
        
}
}
        return false
    }

    @JvmStatic fun isTypeForMimibiote(type: Int): Boolean {
        return type == Constant.ITEM_TYPE_COIFFE || type == Constant.ITEM_TYPE_CAPE ||
                type == Constant.ITEM_TYPE_BOUCLIER || type == Constant.ITEM_TYPE_SAC_DOS || isTypeWeapon(type)
    }

    @JvmStatic fun getColorByElement(element: Int): Byte {
        when (element) {
ELEMENT_FEU -> {return 2
}
ELEMENT_EAU -> {return 3
}
ELEMENT_NEUTRE, ELEMENT_TERRE, ELEMENT_AIR -> {return element.toByte()
}
else -> {return element.toByte()
}
}
    }
}