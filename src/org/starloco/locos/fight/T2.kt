package org.starloco.locos.fight
class TFight {
    var b = false
    fun scheduleTimer(t: Int) {
        org.starloco.locos.util.TimerWaiter.addNext({ -> {
            if (!this.b) {
                this.b = true
            }
        } }, 5000L)
    }
    fun startFight() {
        if (!this.b) this.b = true
    }
}
