package org.starloco.locos.entity.npc

import org.starloco.locos.client.Player
import org.starloco.locos.common.SocketManager
import org.starloco.locos.game.action.ExchangeAction
import org.starloco.locos.game.action.type.NpcDialogActionData
import org.starloco.locos.game.world.World

open class Npc(val id: Int, var cellId: Int, var orientation: Byte, private val templateId: Int) {

    val template: NpcTemplate
        get() = World.world.getNPCTemplate(templateId)!!

    fun onCreateDialog(player: Player) {
        val template = this.template

        val data = NpcDialogActionData(template, -1)
        val action = ExchangeAction(ExchangeAction.TALKING_WITH, data)
        player.exchangeAction = action

        SocketManager.GAME_SEND_DIALOG_CREATE_PACKET(player.gameClient!!, id)
        template.onCreateDialog(player)
    }

    fun encodeGM(alter: Boolean, p: Player): String {
        val template = this.template
        val packet = StringBuilder()
        packet.append(if (alter) "~" else "+")
        packet.append(this.cellId).append(";")
        packet.append(this.orientation).append(";")
        packet.append("0").append(";")
        packet.append(this.id).append(";")
        packet.append(template.id).append(";")
        packet.append("-4").append(";") //type = NPC
        packet.append(template.gfxId).append("^")

        if (template.scaleX == template.scaleY)
            packet.append(template.scaleY).append(";")
        else
            packet.append(template.scaleX).append("x").append(template.scaleY).append(";")

        packet.append(template.sex).append(";")
        packet.append(if (template.color1 != -1) Integer.toHexString(template.color1) else "-1").append(";")
        packet.append(if (template.color2 != -1) Integer.toHexString(template.color2) else "-1").append(";")
        packet.append(if (template.color3 != -1) Integer.toHexString(template.color3) else "-1").append(";")
        packet.append(template.encodeAccessories()).append(";")

        packet.append(template.getExtraClip(p)).append(";")
        packet.append(template.customArtWork)
        return packet.toString()
    }
}
