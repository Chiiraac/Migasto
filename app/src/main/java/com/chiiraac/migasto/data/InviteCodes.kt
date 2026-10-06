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

    /** Normaliza lo que escribe el usuario: mayúsculas y sin espacios ni guiones. */
    fun normalize(input: String): String =
        input.uppercase().filter { it.isLetterOrDigit() }.take(LENGTH)

    fun isValid(code: String): Boolean =
        code.length == LENGTH && code.all { it in ALPHABET }
}
