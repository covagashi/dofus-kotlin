package org.starloco.locos.database.data.game

import java.util.Arrays

class ExperienceTables(players: LongArray, guilds: LongArray, jobs: LongArray, mounts: LongArray, pvp: LongArray, livitinems: LongArray, tormentators: LongArray, bandits: LongArray) {
    class ExperienceTable(private val maxLevelExps: LongArray) {

        fun minXpAt(lvl: Int): Long {
            return maxLevelExps[lvl - 1]
        }

        fun maxXpAt(lvl: Int): Long {
            if (lvl >= maxLevelExps.size) return maxLevelExps[maxLevelExps.size - 1]
            return maxLevelExps[lvl]
        }

        fun levelForXp(xp: Long): Int {
            var idx = Arrays.binarySearch(maxLevelExps, xp)

            // If you have exactly the max value for the level, you're actually next level
            if (idx >= 0) return idx + 1

            // No exact match, get insertion point
            idx = -idx - 1
            // In this case, the insertion point is the level we want :)
            return idx
        }

        fun maxLevel(): Int {
            return maxLevelExps.size
        }
    }

    @JvmField
    val players: ExperienceTable = ExperienceTable(players)
    @JvmField
    val guilds: ExperienceTable = ExperienceTable(guilds)
    @JvmField
    val jobs: ExperienceTable = ExperienceTable(jobs)
    @JvmField
    val mounts: ExperienceTable = ExperienceTable(mounts)
    @JvmField
    val pvp: ExperienceTable = ExperienceTable(pvp)
    @JvmField
    val livitinems: ExperienceTable = ExperienceTable(livitinems)
    @JvmField
    val tormentators: ExperienceTable = ExperienceTable(tormentators)
    @JvmField
    val bandits: ExperienceTable = ExperienceTable(bandits)
}
