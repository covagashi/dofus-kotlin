package org.starloco.locos.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CryptManagerTest {

    private val crypt = CryptManager()

    // region hash table

    @Test
    fun `hash table indices match dofus protocol`() {
        assertEquals(0, CryptManager.getIntByHashedValue('a'))
        assertEquals(25, CryptManager.getIntByHashedValue('z'))
        assertEquals(26, CryptManager.getIntByHashedValue('A'))
        assertEquals(51, CryptManager.getIntByHashedValue('Z'))
        assertEquals(52, CryptManager.getIntByHashedValue('0'))
        assertEquals(61, CryptManager.getIntByHashedValue('9'))
        assertEquals(62, CryptManager.getIntByHashedValue('-'))
        assertEquals(63, CryptManager.getIntByHashedValue('_'))
        assertEquals(-1, CryptManager.getIntByHashedValue('!'))
    }

    @Test
    fun `hashed value by int is inverse of hash index`() {
        for (i in 0 until CryptManager.HASH.size) {
            assertEquals(CryptManager.HASH[i], CryptManager.getHashedValueByInt(i))
        }
    }

    // endregion

    // region cell code <-> id

    @Test
    fun `cell id to code produces two hash chars`() {
        assertEquals("aa", CryptManager.cellID_To_Code(0))
        assertEquals("ab", CryptManager.cellID_To_Code(1))
        // 560 = 8 * 64 + 48 -> 'i' + 'W'
        assertEquals("iW", CryptManager.cellID_To_Code(560))
    }

    @Test
    fun `cell code to id is inverse of cell id to code`() {
        for (id in 0..560) {
            assertEquals(id, crypt.cellCode_To_ID(CryptManager.cellID_To_Code(id)))
        }
    }

    @Test
    fun `cell code with invalid char throws`() {
        assertFailsWith<IllegalStateException> {
            crypt.cellCode_To_ID("!!")
        }
    }

    // endregion

    // region message crypt/decrypt

    @Test
    fun `crypt then decrypt roundtrips ascii messages`() {
        val key = "52c34c5b99f44d2a"
        for (msg in listOf("hello", "AK", "x1y2z3", "This is a longer message 123")) {
            val encrypted = crypt.cryptMessage(msg, key)
            assertEquals(msg, crypt.decryptMessage(encrypted, key), "roundtrip failed for '$msg'")
        }
    }

    @Test
    fun `crypt message prefix is deterministic`() {
        val encrypted = crypt.cryptMessage("abc", "0123456789abcdef")
        assertEquals('1', encrypted[0])
        // checksum("abc") = (1+2+3) % 16 = 6
        assertEquals('6', encrypted[1])
    }

    @Test
    fun `checksumKey sums char mod 16`() {
        assertEquals("6", CryptManager.checksumKey("abc"))   // (97+98+99)%16 = 6
        assertEquals("0", CryptManager.checksumKey("\u0000"))
    }

    // endregion

    // region map data

    @Test
    fun `prepareKey decodes hex pairs`() {
        assertEquals("AB", CryptManager.prepareKey("4142"))
    }

    @Test
    fun `decypherData xors hex pairs against key`() {
        // Data "4142" -> 0x41(65) xor 'a'(97) = 32 (' '), 0x42(66) xor 'b'(98) = 32 (' ')
        assertEquals("  ", CryptManager.decypherData("4142", "ab", 0))
    }

    @Test
    fun `isMapCiphered counts digits threshold`() {
        assertFalse(CryptManager.isMapCiphered("abc123"))
        assertTrue(CryptManager.isMapCiphered("1".repeat(1001)))
    }

    // endregion
}
