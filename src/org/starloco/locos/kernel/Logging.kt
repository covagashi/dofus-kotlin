package org.starloco.locos.kernel

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.ArrayList
import java.util.Calendar
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(Logging::class.java)

object Logging {
    @JvmField
    var USE_LOG = true

    private val logs = ArrayList<Log>()

    @JvmStatic
    fun getInstance(): Logging = this

    fun initialize() {
        if (!File("logs").exists()) File("logs/").mkdir()
    }

    fun stop() {
        logs.stream().filter { log -> log.buffer != null }.forEach { log ->
            try {
                log.buffer.close()
            } catch (e: IOException) {
                logger.error("unexpected error", e)
                }
        }
        this.logs.clear()
    }

    fun write(name: String, arg0: String) {
        if (!USE_LOG) return

        for (log in logs) {
            if (log.name == name) {
                try {
                    log.write(arg0)
                } catch (e: IOException) {
                    logger.error("unexpected error", e)
                }
                return
            }
        }

        val date = (Calendar.getInstance().get(Calendar.YEAR).toString() + "-"
                + Calendar.getInstance().get(Calendar.MONTH) + "-"
                + Calendar.getInstance().get(Calendar.DAY_OF_MONTH))

        try {
            this.logs.add(Log(name, date))
            this.write(name, arg0)
        } catch (e: IOException) {
            logger.error("unexpected error", e)
                }
    }

    class Log(val name: String, date: String) {
        val buffer: BufferedWriter

        init {
            if (!File("Logs/" + this.name).exists())
                File("Logs/" + this.name).mkdir()

            this.buffer = BufferedWriter(
                FileWriter(
                    "Logs/" + this.name + "/" + date + ".log", true
                )
            )
            this.write("Starting logger..")
        }

        @Throws(IOException::class)
        fun write(arg0: String) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val min = Calendar.getInstance().get(Calendar.MINUTE)
            val sec = Calendar.getInstance().get(Calendar.SECOND)

            val date = ("[" + (if (hour < 10) "0" else "") + hour + " : " + (if (min < 10) "0" else "") + min + " : "
                    + (if (sec < 10) "0" else "") + sec + "] : ")

            this.buffer.write(date + arg0)
            this.buffer.newLine()
            this.buffer.flush()
        }
    }
}
