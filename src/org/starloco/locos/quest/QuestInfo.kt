package org.starloco.locos.quest

import java.util.Collections

class QuestInfo(
    objectives: List<Int>,
    @JvmField val previous: Int?,
    @JvmField val next: Int?,
    @JvmField val question: Int?,
    @JvmField val isAccountBound: Boolean,
    @JvmField val isRepeatable: Boolean
) {
    @JvmField
    val objectives: List<Int> = Collections.unmodifiableList(objectives)
}
