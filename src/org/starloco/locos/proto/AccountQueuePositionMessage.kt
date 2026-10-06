package org.starloco.locos.proto

import org.starloco.locos.annotation.DofusMessage
import org.starloco.locos.api.AbstractDofusMessage
import org.starloco.locos.kernel.Config

@DofusMessage(header = "Af")
class AccountQueuePositionMessage(
    private var position: Int,
    private var totalAbo: Int,
    private var totalNonAbo: Int,
    private var button: Int
) : AbstractDofusMessage() {

    constructor() : this(0, 0, 0, 0)

    override fun serialize() {
        output.append("Af").append(position).append("|").append(totalAbo).append("|").append(totalNonAbo).append("|").append(button).append(Config.gameServerId)
    }

    override fun deserialize() {
    }

    override fun toString(): String {
        return "AccountQueuePositionMessage{" +
                "position=" + position +
                ", totalAbo=" + totalAbo +
                ", totalNonAbo=" + totalNonAbo +
                ", button=" + button +
                '}'
    }
}
