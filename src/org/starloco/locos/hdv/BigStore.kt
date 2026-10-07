package org.starloco.locos.hdv

import org.starloco.locos.client.Account
import org.starloco.locos.client.Player
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.game.BigStoreListingData
import org.starloco.locos.database.data.login.PlayerData
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant
import org.starloco.locos.`object`.GameObject
import org.starloco.locos.`object`.entity.SoulStone
import org.starloco.locos.util.Pair
import java.text.DecimalFormat
import java.util.Comparator
import java.util.HashMap
import java.util.LinkedList
import java.util.Objects
import java.util.Optional
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream
import org.starloco.locos.common.splitJ

class BigStore(hdvID: Int, taxe: Float, duration: Short, maxItemCompte: Short, lvlMax: Short, strCategory: String) {

    /**
     * TODO:
     *  - If items are sold at a too lower price, the server directly buy it for the milice
     *  - Remove items after X days if not bought
     */

    private fun interface CategoryFilter {
        fun apply(categoryId: Int, listing: BigStoreListing): Boolean
    }

    class CheapestListings(
        @JvmField val lineId: Int,
        @JvmField val itemTemplateId: Int,
        @JvmField val stats: String?,
        @JvmField val minPrices: IntArray
    )

    val hdvId: Int = hdvID
    val taxe: Float = taxe
    val duration: Short = duration
    val maxAccountItem: Short = maxItemCompte
    private val categories: List<Int> = strCategory.splitJ(",").map { it.toInt() }
    val lvlMax: Short = lvlMax
    private val nextLineID = AtomicInteger(1)
    // K: LineID, V: Pair<TemplateID, Stats>
    private val lineIDToItemDesc = HashMap<Int, Pair<Int, String>>()
    private val statHashToLineID = HashMap<Pair<Int, String>, Int>()
    private val listings = LinkedList<BigStoreListing>()
    private val listingsLock = Any()

    val strCategory: String
        get() = categories.joinToString(",")

    private fun getEntriesByTemplate(categoryId: Int, template: Int): Stream<BigStoreListing> {
        val templateMapper = categoryTemplateMapper(categoryId)
        return listings.stream().filter { e -> templateMapper.apply(e).anyMatch { i -> i == template } }
    }

    fun linesForTemplate(category: Int, templateId: Int): List<CheapestListings> {
        synchronized(listingsLock) {
            return getEntriesByTemplate(category, templateId).collect(Collectors.groupingBy { it.lineId }).entries.stream()
                .map { lineEntries ->
                    val desc = lineIDToItemDesc[lineEntries.key]

                    val cheapestByQuantity = lineEntries.value.stream()
                        .collect(Collectors.groupingBy({ it.lotSize }, Collectors.minBy(priceASC)))

                    val cheapest = BigStoreListingLotSize.values()
                        .map { size -> (cheapestByQuantity[size] ?: Optional.empty()).map { it.price }.orElse(0) }
                        .toIntArray()

                    CheapestListings(lineEntries.key, desc!!.first, desc.second, cheapest)
                }.collect(Collectors.toList())
        }
    }

    private fun cheapestListing(category: Int, templateId: Int, ligneId: Int, amount: BigStoreListingLotSize): Optional<BigStoreListing> {
        return getEntriesByTemplate(category, templateId)
            .filter { it.lineId == ligneId }
            .filter { it.lotSize == amount }
            .min(priceASC)
    }

    private fun categoryFilters(category: Int): CategoryFilter {
        when (category) {
            // Normal soul stone: List if it contains at least one monster that is not a boss nor an ArchMonster
            85 -> return CategoryFilter { _, e ->
                SoulStone.safeCast(e.gameObject)
                    .map { it.getMonsterIDs() }.orElse(Stream.empty()).map { World.world.getMonstre(it) }
                    .filter { it != null }
                    .anyMatch { m -> !m!!.isBoss && !m!!.isArchMonster }
            }
            // Dungeon soul stone: List if it contains at least one monster that is a boss
            124 -> return CategoryFilter { _, e ->
                SoulStone.safeCast(e.gameObject)
                    .map { it.getMonsterIDs() }.orElse(Stream.empty()).map { World.world.getMonstre(it) }
                    .filter { it != null }
                    .anyMatch { it!!.isBoss }
            }
            // ArchMonster soul stone: List if it contains at least one monster that is a boss
            125 -> return CategoryFilter { _, e ->
                SoulStone.safeCast(e.gameObject)
                    .map { it.getMonsterIDs() }.orElse(Stream.empty()).map { World.world.getMonstre(it) }
                    .filter { it != null }
                    .anyMatch { it!!.isArchMonster }
            }
        }
        return defaultCategoryFilter
    }

    private fun categoryTemplateMapper(category: Int): Function<BigStoreListing, Stream<Int>> {
        return when (category) {
            85, 124, 125 -> Function { e -> SoulStone.safeCast(e.gameObject).map { it.getMonsterIDs() }.orElse(Stream.empty()) }
            else -> Function { e -> Stream.of(e.gameObject!!.template!!.id) }
        }
    }

    fun addEntry(toAdd: BigStoreListing): Boolean {
        toAdd.hdvId = this.hdvId
        val obj = checkNotNull(toAdd.gameObject)
        synchronized(listingsLock) {
            // If it doesn't have a guid yet, save it and get one
            if (toAdd.id == -1) {
                val saved = DatabaseManager.get(BigStoreListingData::class.java).insert(toAdd)
                if (!saved) {
                    // We failed to save, the listing didn't get a guid.
                    return false
                }
            }

            val key = Pair(obj.template!!.id, obj.encodeStats())
            val lineId = statHashToLineID.computeIfAbsent(key) {
                val id = nextLineID.getAndIncrement()
                lineIDToItemDesc[id] = key
                id
            }
            toAdd.lineId = lineId
            if (lineId == 0) throw IllegalStateException("LineID should not be 0")
            listings.add(toAdd)

        }
        World.world.addHdvItem(toAdd.owner, this.hdvId, toAdd)
        return true
    }

    fun removeListing(account: Account, listingId: Int): Boolean {
        val player = account.currentPlayer
        val accountListings = account.getHdvEntries(this.hdvId)
        if (accountListings.isEmpty()) return false
        if (player == null) return false

        synchronized(listingsLock) {
            val listing = accountListings.stream()
                .filter { it.id == listingId }
                .findFirst()
            if (!listing.isPresent || !this.listings.contains(listing.get())) return false
            if (!deleteListing(listing.get())) return false

            val obj = listing.get().gameObject!!
            if (account.currentPlayer!!.addObjetSimiler(obj, true, -1)) {
                World.world.removeGameObject(obj.guid)
            } else {
                account.currentPlayer!!.addItem(obj, true)
            }

            DatabaseManager.get(PlayerData::class.java).update(player)
            return true
        }
    }

    private fun deleteListing(listing: BigStoreListing): Boolean {
        if (!this.listings.remove(listing)) return false

        World.world.removeHdvItem(listing.owner, listing.hdvId, listing)
        DatabaseManager.get(BigStoreListingData::class.java).delete(listing)
        return true
    }

    fun getCheapestListings(categoryId: Int, templateId: Int, lineId: Int): Optional<CheapestListings> {
        return linesForTemplate(categoryId, templateId).stream().filter { it.lineId == lineId }.findFirst()
    }

    fun buyItem(category: Int, templateId: Int, lineID: Int, amount: BigStoreListingLotSize, price: Int, newOwner: Player): Optional<BigStoreListing> {
        synchronized(listingsLock) {
            val listing = cheapestListing(category, templateId, lineID, amount)
                .filter { it.gameObject != null }
                .filter { it.price == price }
                .orElse(null) ?: return Optional.empty()

            if (!newOwner.modKamasDisplay(-price.toLong())) {
                return Optional.empty()
            }

            val prevOwner = Optional.ofNullable(World.world.ensureAccountLoaded(listing.owner))
            prevOwner.ifPresent { p -> p.setBankKamas(p.getBankKamas() + price) }

            val obj = listing.gameObject!!
            newOwner.addItem(obj, true, false)
            obj.position = Constant.ITEM_POS_NO_EQUIPED
            obj.template!!.newSold(listing.lotSize!!.amount, price)

            deleteListing(listing)
            return Optional.of(listing)
        }
    }

    fun parseTaxe(): String = pattern.format(this.taxe).replace(",", ".")

    fun getCategoryContent(category: Int): List<Int> {
        val filter = categoryFilters(category)
        val templateMapper = categoryTemplateMapper(category)
        synchronized(listingsLock) {
            return listings.stream().filter { filter.apply(category, it) }
                .flatMap(templateMapper)
                .distinct()
                .collect(Collectors.toList())
        }
    }

    companion object {
        private val priceASC: Comparator<BigStoreListing> = Comparator.comparingInt { it.price }
        private val pattern = DecimalFormat("0.0")
        // defaultCategoryFilter returns true if categoryId == template.type
        private val defaultCategoryFilter = CategoryFilter { c, e -> c == e.gameObject!!.template!!.type }
    }
}
