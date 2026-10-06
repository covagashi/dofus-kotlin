package org.starloco.locos.`object`

import org.starloco.locos.client.other.Stats
import org.starloco.locos.util.Pair

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Comparator
import java.util.Objects
import java.util.stream.Collectors

/**
 * ItemHash makes it easier to find similar items(same template, same stats).
 * It can be used as a map key.
 * DO NOT STORE IN DATABASE. We may want to change how we compute the hashes later on.
 * */
class ItemHash(item: GameObject) {

    @JvmField
    val templateId: Int = item.template!!.id

    @JvmField
    val strStats: String = item.encodeStats()

    @JvmField
    val hash: String = hash(this.templateId, item.stats, item.txtStat)

    override fun hashCode(): Int {
        return this.hash.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ItemHash) return false
        return other.hash == this.hash
    }

    companion object {
        private val digest: MessageDigest = try {
            MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }

        private val charset: Charset = StandardCharsets.UTF_8

        private fun hash(templateID: Int, stats: Stats, txtStats: Map<Int, String>): String {
            // Sort stats in effectID order, we need determinism
            val sortedStats = stats.effects.entries.stream()
                .sorted(Comparator.comparingInt { e: Map.Entry<Int, Int> -> e.key })
                .map { e -> Pair(e.key, e.value) }
                .collect(Collectors.toList())

            // Sort txtStats in effectID order, we need determinism
            // TODO: Maybe we need to also sort the txt stats values ?
            val sortedTxtStats = txtStats.entries.stream()
                .sorted(Comparator.comparingInt { e: Map.Entry<Int, String> -> e.key })
                .map { e -> Pair(e.key, e.value) }
                .collect(Collectors.toList())

            val message: ByteArray
            try {
                ByteArrayOutputStream().use { bbos ->
                    DataOutputStream(bbos).use { dos ->
                        dos.writeInt(templateID)

                        for (p in sortedStats) {
                            dos.writeInt(p.first)
                            dos.writeInt(p.second)
                        }

                        for (p in sortedTxtStats) {
                            dos.writeInt(p.first)
                            dos.write(p.second.toByteArray(charset))
                        }
                    }
                    message = bbos.toByteArray()
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }

            // digest is not thread-safe :(
            synchronized(digest) {
                val sb = StringBuilder()
                for (b in digest.digest(message)) {
                    sb.append(("%02X").format( b))
                }
                return sb.toString()
            }
        }
    }
}
