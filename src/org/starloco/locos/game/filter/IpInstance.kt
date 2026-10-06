package org.starloco.locos.game.filter

internal class IpInstance {

    var connections: Int = 0
        private set
    var lastConnection: Long = 0
        private set
    var isBanned: Boolean = false
        private set

    fun addConnection() {
        connections++
    }

    fun resetConnections() {
        connections = 0
    }

    fun updateLastConnection() {
        this.lastConnection = System.currentTimeMillis()
    }

    fun ban() {
        isBanned = true
    }
}
