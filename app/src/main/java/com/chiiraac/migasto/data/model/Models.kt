package com.chiiraac.migasto.data.model

import java.time.LocalDate

/** Tipo de movimiento. Las facturas cuentan como gasto en las estadísticas. */
enum class MovementType {
    EXPENSE,
    INCOME,
    BILL,
    TRANSFER;

    val isOutflow: Boolean get() = this == EXPENSE || this == BILL

    companion object {
        fun fromKey(key: String?): MovementType = entries.firstOrNull { it.name == key } ?: EXPENSE
    }
}

/** Cuenta de la que sale (o a la que entra) el dinero. */
enum class PaymentMethod {
    BANK,
    CASH;

    val other: PaymentMethod get() = if (this == BANK) CASH else BANK

    companion object {
        fun fromKey(key: String?): PaymentMethod = entries.firstOrNull { it.name == key } ?: BANK
    }
}

enum class GroupIcon {
    HOME,
    PERSON,
    COUPLE,
    FAMILY,
    FRIENDS,
    TRAVEL,
    WORK,
    SAVINGS;

    companion object {
        fun fromKey(key: String?): GroupIcon = entries.firstOrNull { it.name == key } ?: HOME
    }
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.name == key } ?: SYSTEM
    }
}

data class UserProfile(
    val uid: String,
    val name: String,
    val email: String?,
)

data class Member(
    val uid: String,
    val name: String,
    val email: String?,
)

data class Group(
    val id: String,
    val name: String,
    val icon: GroupIcon,
    val inviteCode: String,
    val ownerId: String,
    val members: List<Member>,
    val createdAt: Long,
) {
    fun memberName(uid: String): String? = members.firstOrNull { it.uid == uid }?.name
}

/**
 * Un movimiento de dinero. Los importes se guardan siempre en céntimos y en positivo;
 * el signo lo determina [type].
 *
 * En un [MovementType.TRANSFER] el dinero sale de [method] y entra en [PaymentMethod.other].
 */
data class Movement(
    val id: String,
    val groupId: String,
    val type: MovementType,
    val method: PaymentMethod,
    val amountCents: Long,
    val description: String,
    val categoryId: String,
    val date: LocalDate,
    val createdAt: Long,
    val createdById: String,
    val createdByName: String,
    val hasPhoto: Boolean,
)

/** Datos editables de un movimiento (alta o edición). */
data class MovementDraft(
    val type: MovementType,
    val method: PaymentMethod,
    val amountCents: Long,
    val description: String,
    val categoryId: String,
    val date: LocalDate,
)

/** Qué hacer con la foto adjunta al guardar un movimiento. */
sealed interface PhotoChange {
    data object Keep : PhotoChange
    data object Remove : PhotoChange
    class Replace(val jpegBytes: ByteArray) : PhotoChange
}
