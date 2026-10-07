package org.starloco.locos.dynamic

import org.starloco.locos.area.map.GameMap
import org.starloco.locos.common.Formulas
import org.starloco.locos.game.scheduler.Updatable
import org.starloco.locos.game.world.World
import org.starloco.locos.`object`.ObjectTemplate
import org.starloco.locos.util.TimerWaiter
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayList
import java.util.Arrays
import org.slf4j.LoggerFactory
import org.starloco.locos.common.splitJ

private val log = LoggerFactory.getLogger(Tavernier::class.java)

class Tavernier : Updatable<Void?>(5 * 60_000) {

    private val map: GameMap? = World.world.getMap(10354)

    private var drinkAllRound: Byte = 0

    override fun update() {
        if (this.verify()) {
            this.drinkAllRound++
            var count = 0
            for (str in this.parseHtml()) {
                TimerWaiter.addNext({
                    this.talk(str)
                    if (Formulas.getRandomValue(0, 10) == 5)
                        this.map!!.send("cS-3|" + Formulas.getRandomValue(1, 15))
                }, count.toLong())
                count += 7000
            }
            if (this.drinkAllRound.toInt() == 10) {
                TimerWaiter.addNext({
                    this.drinkAllRound()
                    this.drinkAllRound = 0
                }, 2000)
            }
        }
    }

    private fun drinkAllRound() {
        this.map!!.send("cMK|-4|Habitué de la taverne|Tournée générale pour tout le monde, c'est moi qui régale !|")
        val objectTemplate = World.world.getObjTemplate(6857)
        for (p in this.map!!.players) {
            if (p.isOnline) {
                val `object` = objectTemplate!!.createNewItem(1, false)!!
                if (Formulas.getRandomValue(0, 3) == 0) {
                    if (p.addItem(`object`, true, false))
                        World.world.addGameObject(`object`)
                    p.send("Im021;1~" + objectTemplate!!.id)
                } else {
                    p.send("eUK" + p.id + "|18")
                }
            }
        }
    }

    override fun get(): Void? = null

    private fun talk(message: String) {
        this.map!!.send("cMK|-3|Tek Abir|$message|")
    }

    private fun parseHtml(): List<String> {
        val msg = getHTML()
        val temp = ArrayList<String>()
        for (line in msg) {
            var str = line
            str = str.replace("document.write[(][']".toRegex(), "")
            str = str.replace("\\", "")
            str = str.replace("/", "")
            str = str.replace("');", "")
            str = str.replace("<br>", "")
            str = str.replace("<b>", "")
            str = str.replace("<u>", "")
            str = str.replace("<", "")
            str = str.replace(">", "")
            if (!str.matches("(.*)margin(.*)".toRegex()) && !str.matches("(.*)<p>(.*)".toRegex()) && !str.matches("(.*)--(.*)".toRegex()) && str != "p") {
                if (str.length > 300) {
                    temp.addAll(listOf(*str.splitJ(".".toRegex()).toTypedArray()))
                } else temp.add(str)
            }
        }
        return temp
    }

    private fun getHTML(): List<String> {
        val msg = ArrayList<String>()
        try {
            val url = URL("http://185.212.226.3/blagues.php")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connect()
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val rd = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                var line: String?
                while (rd.readLine().also { line = it } != null) {
                    msg.add(line!!)
                }
                rd.close()
                return msg
            }
        } catch (e: IOException) {
            log.error("unexpected error", e)
                }
        return msg
    }

    companion object {
        @JvmStatic
        val instance: Tavernier = Tavernier()
    }
}
