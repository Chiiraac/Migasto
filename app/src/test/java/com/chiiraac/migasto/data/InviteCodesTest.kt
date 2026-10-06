package com.chiiraac.migasto.data

import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteCodesTest {
    @Test
    fun `generated codes are valid and unambiguous`() {
        val random = Random(42)
        repeat(1_000) {
            val code = InviteCodes.generate(random)
            assertTrue(code, InviteCodes.isValid(code))
            assertFalse(code.any { it in "01IOL" })
        }
    }

    @Test
    fun `normalizes user input`() {
        assertEquals("AB2CD3", InviteCodes.normalize(" ab2-cd3 "))
        assertEquals("ABCDEF", InviteCodes.normalize("abcdefgh"))
    }
}
