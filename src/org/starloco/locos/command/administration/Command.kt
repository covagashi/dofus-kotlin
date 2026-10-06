package org.starloco.locos.command.administration

import java.util.HashMap

class Command(val name: String, val args: String, val desc: String) {

    init {
        commands[name] = this
    }

    companion object {
        @JvmField
        val commands = HashMap<String, Command>()
    }
}
