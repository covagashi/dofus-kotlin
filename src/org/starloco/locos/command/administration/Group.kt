package org.starloco.locos.command.administration

import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedList
import java.util.stream.Collectors

class Group(
    val id: Int,
    val name: String,
    val isPlayer: Boolean,
    private val allCommands: Boolean,
    commands: List<String>
) {

    private val commands: List<String> = ArrayList(commands)

    init {
        groups[id] = this
    }

    fun getCommands(): List<Command> {
        if (allCommands) return LinkedList(Command.commands.values)
        return commands.mapNotNull { Command.commands[it] }
    }

    fun haveCommand(name: String): Boolean {
        if (allCommands) return true
        return commands.contains(name)
    }

    companion object {
        private val groups = HashMap<Int, Group>()

        @JvmStatic
        fun byId(id: Int): Group? = groups[id]

        @JvmStatic
        fun getGroups(): Collection<Group> = groups.values
    }
}
