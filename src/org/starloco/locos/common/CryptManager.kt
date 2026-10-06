package org.starloco.locos.common

import org.apache.commons.lang.StringEscapeUtils

import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(CryptManager::class.java)

class CryptManager {

    private val HEX_CHARS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

    fun cellCode_To_ID(cellCode: String): Int {
        val char1 = cellCode[0]
        val char2 = cellCode[1]
        var code1 = -1
        var code2 = -1
        var a = 0

        while (a < HASH.size) {
            if (HASH[a] == char1)
                code1 = a shl 6
            if (HASH[a] == char2)
                code2 = a
            if (code1 != -1 && code2 != -1)
                return code1 + code2
            a++
        }
        throw IllegalStateException("invalid cellCode passed to cellCode_To_ID")
    }

    fun prepareMapDataKey(key: String): String {
        val data = StringBuilder()
        val num2 = key.length - 2
        var i = 0

        while (i <= num2) {
            data.append((key.substring(i, i + 2)).toInt(16).toChar())
            i += 2
        }

        return unescape(data.toString())
    }

    fun cryptMessage(message: String, key: String): String {
        var message = message
        val str = StringBuilder()
        message = message.replace("'", "'")
        // Append keyId
        str.append(HEX_CHARS[1])
        // Append checksum
        val checksum = checksum(message)
        str.append(HEX_CHARS[checksum])
        // Prepare key cause it's hexa form
        val c = checksum * 2
        val data = encode(message)
        val keyLength = key.length

        for (i in data.indices)
            str.append(decimalToHexadecimal(data[i].code xor key[(i + c) % keyLength].code))

        return str.toString()
    }

    fun decryptMessage(message: String, key: String): String {
        var message = message
        try {
            val c = (message[1].toString()).toInt(16) * 2
            val str = StringBuilder()
            var j = 0
            val keyLength = key.length

            var i = 2
            while (i < message.length) {
                try {
                    str.append(((message.substring(i, i + 2)).toInt(16) xor key[(j++ + c) % keyLength].code).toChar())
                } catch (ignored: Exception) {
                    log.warn("CryptManager : DecryptMessage : $message (key: $key) : $i to${i + 2}")
                }
                i += 2
            }
            var data = str.toString()
            data = data.replace("%(?![0-9a-fA-F]{2})".toRegex(), "%25")
            data = data.replace("\\+".toRegex(), "%2B")
            return URLDecoder.decode(data, "UTF-8").replace("'", "'")
        } catch (e: Exception) {
            log.error("unexpected error", e)
                return ""
        }
    }

    private fun checksum(data: String): Int {
        var result = 0
        for (c in data.toCharArray())
            result += c.code % 16
        return result % 16
    }

    private fun decimalToHexadecimal(c: Int): String {
        var c = c
        if (c > 255) c = 255
        return HEX_CHARS[c / 16].toString() + "" + HEX_CHARS[c % 16]
    }

    private fun encode(input: String): String {
        val resultStr = StringBuilder()
        for (ch in input.toCharArray()) {
            if (isUnsafe(ch)) {
                resultStr.append('%')
                resultStr.append(toHex(ch.code / 16))
                resultStr.append(toHex(ch.code % 16))
            } else {
                resultStr.append(ch)
            }
        }
        return resultStr.toString()
    }

    private fun toHex(ch: Int): Char {
        return (if (ch < 10) '0' + ch else 'A' + ch - 10)
    }

    private fun isUnsafe(ch: Char): Boolean {
        return ch.code > 255 || "+%".indexOf(ch) >= 0
    }

    companion object {
        @JvmField
        val HASH = charArrayOf(
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
            'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
            'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', '-', '_'
        )

        @JvmStatic
        fun cellID_To_Code(cellID: Int): String {
            return HASH[cellID shr 6].toString() + "" + HASH[cellID and 0x3F]
        }

        @JvmStatic
        fun getIntByHashedValue(c: Char): Int {
            for (a in HASH.indices)
                if (HASH[a] == c)
                    return a
            return -1
        }

        @JvmStatic
        fun getHashedValueByInt(c: Int): Char {
            return HASH[c]
        }

        private fun unescape(data: String): String {
            return StringEscapeUtils.unescapeJava(data)
        }

        @JvmStatic
        fun checksumKey(data: String): String {
            var num = 0
            val num3 = data.length - 1
            var i = 0
            while (i <= num3) {
                num += data.substring(i, i + 1)[0].code % 16
                i++
            }

            val strArray = arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F")
            return strArray[num % 16]
        }

        @JvmStatic
        @Throws(UnsupportedEncodingException::class)
        fun decryptMapData(mapData: String, key: String): String {
            var key = key
            key = prepareKey(key)
            val strsum = checksumKey(key)
            val checksum = (strsum).toInt(16) * 2
            return decypherData(mapData, key, checksum)
        }

        @JvmStatic
        fun decypherData(Data: String, Key: String, Checksum: Int): String {
            val dataToDecrypt = StringBuilder()
            val num4 = Data.length - 2
            var i = 0
            while (i <= num4) {
                val sub = Data.substring(i, i + 2)
                val num = (sub).toInt(16)
                val s = Math.round(((i / 2) + Checksum) % Key.length.toDouble()).toInt()
                val num2 = Key.substring(s, s + 1)[0].code
                dataToDecrypt.append((num.toChar().code xor num2.toChar().code).toChar().toString())
                i += 2
            }
            return unescape(dataToDecrypt.toString())
        }

        @JvmStatic
        fun isMapCiphered(mapData: String): Boolean {
            var nb = 0
            for (a in mapData.toCharArray()) if (Character.isDigit(a)) nb++
            return nb > 1000
        }

        @JvmStatic
        @Throws(UnsupportedEncodingException::class)
        fun prepareKey(key: String): String {
            val sb = StringBuilder()
            var i = 0
            while (i < key.length) {
                sb.append((key.substring(i, i + 2)).toInt(16).toChar())
                i += 2
            }
            return URLDecoder.decode(sb.toString(), "UTF-8")
        }
    }
}
