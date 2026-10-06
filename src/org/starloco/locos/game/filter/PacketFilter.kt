package org.starloco.locos.game.filter

import java.util.HashMap

class PacketFilter {

    private val maxConnections = 16
    private val restrictedTime = 1000
    private val ipInstances = HashMap<String, IpInstance>()
    private var safe = false

    @Synchronized
    private fun safeCheck(ip: String): Boolean = unSafeCheck(ip)

    private fun unSafeCheck(ip: String): Boolean {
        val ipInstance = find(ip)

        if (ipInstance.isBanned) {
            return false
        } else {
            ipInstance.addConnection()

            if (ipInstance.lastConnection + this.restrictedTime >= System.currentTimeMillis()) {
                if (ipInstance.connections < this.maxConnections)
                    return true
                else {
                    ipInstance.ban()
                    return false
                }
            } else {
                ipInstance.updateLastConnection()
                ipInstance.resetConnections()
            }
            return true
        }
    }

    fun authorizes(ip: String): Boolean = if (safe) safeCheck(ip) else unSafeCheck(ip)

    fun activeSafeMode(): PacketFilter {
        this.safe = true
        return this
    }

    private fun find(ip: String): IpInstance {
        val cleanIp = clearIp(ip)
        return ipInstances.getOrPut(cleanIp) { IpInstance() }
    }

    private fun clearIp(ip: String): String =
        if (ip.contains(":")) ip.split(":")[0] else ip
}
