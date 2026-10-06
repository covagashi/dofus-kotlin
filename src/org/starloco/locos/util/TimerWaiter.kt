package org.starloco.locos.util

import org.starloco.locos.game.world.World
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(TimerWaiter::class.java)

object TimerWaiter {

    private const val numberOfThread = 10
    private val scheduler: ScheduledExecutorService =
        Executors.newScheduledThreadPool(numberOfThread) { r -> Thread(r, "TimerWaiter") }

    @JvmStatic
    fun addNext(run: Runnable, time: Long, unit: TimeUnit): ScheduledFuture<*> =
        scheduler.schedule(catchRunnable(run), time, unit)

    @JvmStatic
    fun addNext(run: Runnable, time: Long): ScheduledFuture<*> =
        addNext(run, time, TimeUnit.MILLISECONDS)

    @JvmStatic
    fun update() {
        //numberOfThread = getNumberOfThread() + 20;
        //scheduler.shutdownNow();
        //scheduler = Executors.newScheduledThreadPool(numberOfThread);
    }

    private fun getNumberOfThread(): Int {
        val fight = getNumberOfFight()
        val player = World.world.onlinePlayers.size
        return (fight + player) / 30
    }

    private fun getNumberOfFight(): Int =
        World.world.maps.sumOf { it.fights.size }

    @JvmStatic
    fun catchRunnable(run: Runnable): Runnable = Runnable {
        try {
            run.run()
        } catch (e: Exception) {
            log.error("unexpected error", e)
                log.error("{}", e.cause?.message)
        }
    }
}
