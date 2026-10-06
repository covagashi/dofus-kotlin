package org.starloco.locos.anims

import org.classdump.luna.Table
import org.starloco.locos.script.ScriptVM
import java.security.InvalidParameterException
import java.util.Collections

class KeyFrame private constructor(
    @JvmField val frame: Int,
    private val durationMillis: Int,
    @JvmField val nextFrame: String,
    private val interactive: Boolean,
    overrides: Map<String, Int>
) {
    private val FPS = 60f

    val cellOverrides: Map<String, Int> = Collections.unmodifiableMap(overrides)

    init {
        if (durationMillis > 0 && nextFrame!!.isEmpty()) {
            throw InvalidParameterException("nextFrame is mandatory when duration is set")
        }
    }

    fun hasDuration(): Boolean = durationMillis != 0

    fun durationMillis(): Int = this.durationMillis

    fun isObjectInteractive(): Boolean = this.interactive

    companion object {
        @JvmStatic
        fun fromScriptValue(t: Table): KeyFrame {
            val frame = ScriptVM.rawInt(t, "frame")
            val duration = ScriptVM.rawOptionalInt(t, "duration", 0)
            val nextFrame = ScriptVM.rawOptionalString(t, "next")
            val interactive = ScriptVM.rawOptional(t, "interactive").map { it as Boolean }.orElse(false)

            val cellOverrides = ScriptVM.rawOptional(t, "overrides")
                .map { it as Table }
                .map { tv -> ScriptVM.mapFromScript(tv, { it.toString() }, { (it as Long).toInt() }) }
                .orElse(emptyMap())

            if (frame < 0) throw InvalidParameterException("frame must be positive")
            if (duration < 0) throw InvalidParameterException("duration must be strictly positive")
            if (duration != 0 && nextFrame!!.isEmpty()) throw InvalidParameterException("nextFrame is required when duration >0")

            return KeyFrame(frame, duration, nextFrame!!, interactive, Collections.unmodifiableMap(cellOverrides))
        }
    }
}
