package org.starloco.locos.anims

import java.util.Collections

class Animation(
    @JvmField val id: Int,
    @JvmField val defaultState: String,
    frames: Map<String, KeyFrame>
) {
    @JvmField
    val frames: Map<String, KeyFrame> = Collections.unmodifiableMap(frames)

    fun getFrame(name: String): KeyFrame? = frames[name]
}
