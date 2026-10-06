package org.starloco.locos.game.scheduler

/**
 * Created by Locos on 24/06/2015.
 */
interface IUpdatable<T> {

    fun update()
    fun get(): T? = null
}
