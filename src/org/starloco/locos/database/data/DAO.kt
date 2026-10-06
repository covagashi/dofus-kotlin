package org.starloco.locos.database.data

interface DAO<T> {

    fun loadFully()
    fun load(id: Int): T?

    fun insert(entity: T): Boolean
    fun delete(entity: T)
    fun update(entity: T)

    fun getReferencedClass(): Class<*>
    fun getTableName(): String
}
