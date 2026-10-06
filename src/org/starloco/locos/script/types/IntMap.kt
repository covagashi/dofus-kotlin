package org.starloco.locos.script.types

import org.classdump.luna.Table

class IntMap : Table(), MutableMap<String, Int> {

    private val m = HashMap<String, Int>()

    override val size: Int
        get() = m.size

    override fun isEmpty(): Boolean = m.isEmpty()

    override fun containsKey(key: String): Boolean = m.containsKey(key)

    override fun containsValue(value: Int): Boolean = m.containsValue(value)

    override fun get(key: String): Int? = m[key]

    override fun put(key: String, value: Int): Int? = m.put(key, value)

    override fun remove(key: String): Int? = m.remove(key)

    override fun putAll(from: Map<out String, Int>) {
        m.putAll(from)
    }

    override fun clear() {
        m.clear()
    }

    override val keys: MutableSet<String>
        get() = m.keys

    override val values: MutableCollection<Int>
        get() = m.values

    override val entries: MutableSet<MutableMap.MutableEntry<String, Int>>
        get() = m.entries

    override fun rawget(key: Any): Any? = m[key as String]

    override fun rawset(key: Any, value: Any) {
        m[key as String] = value as Int
    }

    override fun initialKey(): Any = throw UnsupportedOperationException()

    override fun successorKeyOf(key: Any): Any = throw UnsupportedOperationException()

    override fun setMode(weakKeys: Boolean, weakValues: Boolean) {
        throw UnsupportedOperationException()
    }
}
