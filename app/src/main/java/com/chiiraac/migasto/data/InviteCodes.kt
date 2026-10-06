package com.chiiraac.migasto.data

import java.security.SecureRandom
import java.util.Random

/** Códigos de invitación cortos y fáciles de dictar (sin 0/O ni 1/I/L). */
object InviteCodes {
    const val LENGTH = 6
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    private val secureRandom = SecureRandom()

    fun generate(random: Random = secureRandom): String =
        buildString(LENGTH) {
            repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
        }

    private val CODE_IN_TEXT = Regex("(?<![A-Z0-9])[$ALPHABET]{$LENGTH}(?![A-Z0-9])")

    /**
     * Normaliza lo que escribe el usuario: mayúsculas y sin espacios ni guiones. Si pega el
     * mensaje completo de invitación ("…Código de invitación: ABC234"), extrae el código.
     */
    fun normalize(input: String): String {
        val upper = input.uppercase()
        val compact = upper.filter { it.isLetterOrDigit() }
        if (compact.length <= LENGTH) return compact
        return CODE_IN_TEXT.findAll(upper).lastOrNull()?.value ?: compact.take(LENGTH)
    }

    fun isValid(code: String): Boolean =
        code.length == LENGTH && code.all { it in ALPHABET }
}
