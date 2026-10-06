package org.starloco.locos.area.map

object OrthogonalProj {

    @JvmStatic
    fun getOrthX(w: Int, cellId: Int): Int {
        return (cellId + getOrthY(w, cellId) * (w - 1)) / w
    }

    @JvmStatic
    fun getOrthXFromY(w: Int, cellId: Int, y: Int): Int {
        return (cellId + y * (w - 1)) / w
    }

    @JvmStatic
    fun getOrthY(w: Int, cellId: Int): Int {
        val t = (w shl 1) - 1
        val lineNb = cellId / t
        val lineOff = cellId % t % w

        return lineOff - lineNb
    }

    @JvmStatic
    fun getCellId(w: Int, x: Int, y: Int): Short {
        return Math.round(x + y * (w - .5)).toShort()
    }

    @JvmStatic
    fun getCellsDistance(w: Int, c1: Int, c2: Int): Int {
        val y1 = getOrthY(w, c1)
        val y2 = getOrthY(w, c2)

        return (Math.abs(getOrthXFromY(w, c1, y1) - getOrthXFromY(w, c2, y2))
                + Math.abs(y1 - y2))
    }

    @JvmStatic
    fun getOrthCellID(w: Int, x: Int, y: Int): Int {
        val t = (w shl 1) - 1
        val diff = x - y
        val sum = x + y
        return (diff shr 1) * t + (diff % 2 * w) + (sum shr 1)
    }

    // Check is a cellId is on the edge of the grid. Those cells are usually inactive
    @JvmStatic
    fun isEdgeCell(w: Int, h: Int, cellId: Int): Boolean {
        val y = getOrthY(w, cellId)
        val x = getOrthXFromY(w, cellId, y)
        return isEdgeCellOrth(w, h, x, y)
    }

    // Check is a cellId is on the edge of the grid using orthogonal coordinates
    @JvmStatic
    fun isEdgeCellOrth(w: Int, h: Int, x: Int, y: Int): Boolean {
        val diff = x - y
        val sum = x + y

        return (diff == 0 // Top
                || sum == w - 1 shl 1 // Right
                || diff == h - 1 shl 1 // Bottom
                || sum == 0)          // Left
    }
}
