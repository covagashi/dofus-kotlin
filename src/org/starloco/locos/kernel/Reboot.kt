package org.starloco.locos.kernel

import java.text.SimpleDateFormat
import java.util.Calendar

object Reboot {

    private var remainingHours: Byte = 0
    private var remainingMinutes: Byte = 0

    fun initialize() {
        check()
    }

    @JvmStatic
    fun check(): Boolean {
        val date = Calendar.getInstance().time

        val actualHour = SimpleDateFormat("HH").format(date).toInt()
        val actualMinute = SimpleDateFormat("mm").format(date).toInt()
        val total = actualHour * 60 + actualMinute

        val restant = (24 * 60) - (total - 5 * 60).toDouble()

        val hour = (restant / 60).toInt().toByte()
        val minute = (((restant / 60) - hour) * 60).toInt().toByte()

        Reboot.remainingHours = hour
        Reboot.remainingMinutes = minute
        when (actualHour) {
            0, 1, 2, 3, 4 -> Reboot.remainingHours = (Reboot.remainingHours - 24).toByte()
        }

        return (hour.toInt() == 0 && minute.toInt() == 0) || (actualHour == 4 && actualMinute == 59)
    }

    @JvmStatic
    fun toStr(): String {
        var im = "Im115;"
        if (Reboot.remainingHours.toInt() == 0) {
            im += Reboot.remainingMinutes.toString() + if (Reboot.remainingMinutes.toInt() > 1) " minutes" else " minute"
        } else {
            im += Reboot.remainingHours.toString() + if (Reboot.remainingHours.toInt() > 1) " heures et " else " heure et "
            im += Reboot.remainingMinutes.toString() + if (Reboot.remainingMinutes.toInt() > 1) " minutes" else " minute"
        }
        return im
    }
}
