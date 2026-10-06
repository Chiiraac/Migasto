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

    @Test
    fun `extracts the code from a pasted invitation message`() {
        val es = "¡Únete a mi grupo «Casa» en MiGasto para llevar las cuentas juntos! Código de invitación: F9KB38"
        val en = "Join my “Home” group on MiGasto to keep track of our expenses together! Invite code: F9KB38"
        assertEquals("F9KB38", InviteCodes.normalize(es))
        assertEquals("F9KB38", InviteCodes.normalize(en))
    }
}
