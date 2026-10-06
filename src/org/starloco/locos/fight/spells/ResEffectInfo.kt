package org.starloco.locos.fight.spells

import org.starloco.locos.kernel.Constant

enum class ResEffectInfo(
    val percentElem: Int,
    val fixedElem: Int,
    val fixed: Int,
    val percentElemPvP: Int,
    val fixedElemPvP: Int
) {

    ELEMENT_NULL(0, 0, 0, 0, 0),
    ELEMENT_NEUTRE(Constant.STATS_ADD_RP_NEU, Constant.STATS_ADD_R_NEU, Constant.STATS_ADD_RES_P, Constant.STATS_ADD_RP_PVP_NEU, Constant.STATS_ADD_R_PVP_NEU),
    ELEMENT_TERRE(Constant.STATS_ADD_RP_TER, Constant.STATS_ADD_R_TER, Constant.STATS_ADD_RES_P, Constant.STATS_ADD_RP_PVP_TER, Constant.STATS_ADD_R_PVP_TER),
    ELEMENT_EAU(Constant.STATS_ADD_RP_EAU, Constant.STATS_ADD_R_EAU, Constant.STATS_ADD_RES_M, Constant.STATS_ADD_RP_PVP_EAU, Constant.STATS_ADD_R_PVP_EAU),
    ELEMENT_FEU(Constant.STATS_ADD_RP_FEU, Constant.STATS_ADD_R_FEU, Constant.STATS_ADD_RES_M, Constant.STATS_ADD_RP_PVP_FEU, Constant.STATS_ADD_R_PVP_FEU),
    ELEMENT_AIR(Constant.STATS_ADD_RP_AIR, Constant.STATS_ADD_R_AIR, Constant.STATS_ADD_RES_M, Constant.STATS_ADD_RP_PVP_AIR, Constant.STATS_ADD_R_PVP_AIR);

    companion object {
        @JvmStatic
        fun forElement(elementID: Int): ResEffectInfo {
            return when (elementID) {
                Constant.ELEMENT_NEUTRE -> ELEMENT_NEUTRE
                Constant.ELEMENT_AIR -> ELEMENT_AIR
                Constant.ELEMENT_EAU -> ELEMENT_EAU
                Constant.ELEMENT_FEU -> ELEMENT_FEU
                Constant.ELEMENT_TERRE -> ELEMENT_TERRE
                else -> ELEMENT_NULL
            }
        }
    }
}
