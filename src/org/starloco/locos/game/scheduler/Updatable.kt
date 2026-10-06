package org.starloco.locos.game.scheduler

/**
 * Created by Locos on 23/06/2015.
 */
abstract class Updatable<T>(wait: Int) : IUpdatable<T> {

    private val wait: Long = wait.toLong()
    @JvmField
    protected var lastTime: Long = System.currentTimeMillis()

    protected open fun verify(): Boolean {
        if (System.currentTimeMillis() - this.lastTime > this.wait) {
            this.lastTime = System.currentTimeMillis()
            return true
        }
        return false
    }
}
