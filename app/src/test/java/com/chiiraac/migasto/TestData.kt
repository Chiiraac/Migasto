package com.chiiraac.migasto

import com.chiiraac.migasto.data.model.Group
import com.chiiraac.migasto.data.model.GroupIcon
import com.chiiraac.migasto.data.model.Member
import com.chiiraac.migasto.data.model.Movement
import com.chiiraac.migasto.data.model.MovementType
import com.chiiraac.migasto.data.model.PaymentMethod
import com.chiiraac.migasto.data.model.UserProfile
import java.time.LocalDate

/** Datos de ejemplo parecidos a los de las capturas originales. */
object TestData {
    val today: LocalDate = LocalDate.of(2026, 10, 6)

    val emil = UserProfile("u-emil", "Emil", "emil@example.com")
    val laura = Member("u-laura", "Laura", "laura@example.com")

    val group = Group(
        id = "g-casa",
        name = "Casa",
        icon = GroupIcon.HOME,
        inviteCode = "F9KB38",
        ownerId = emil.uid,
        members = listOf(Member(emil.uid, emil.name, emil.email), laura),
        createdAt = 0,
    )

    val otherGroup = group.copy(id = "g-viaje", name = "Viaje a Lisboa", icon = GroupIcon.TRAVEL, members = group.members.take(1))

    private var counter = 0

    fun movement(
        description: String,
        cents: Long,
        type: MovementType,
        category: String,
        date: LocalDate,
        method: PaymentMethod = PaymentMethod.BANK,
        author: Member = Member(emil.uid, emil.name, emil.email),
        hasPhoto: Boolean = false,
    ): Movement {
        counter++
        return Movement(
            id = "m$counter",
            groupId = group.id,
            type = type,
            method = method,
            amountCents = cents,
            description = description,
            categoryId = category,
            date = date,
            createdAt = counter.toLong(),
            createdById = author.uid,
            createdByName = author.name,
            hasPhoto = hasPhoto,
        )
    }

    private fun oct(day: Int) = LocalDate.of(2026, 10, day)
    private fun sep(day: Int) = LocalDate.of(2026, 9, day)

    val movements: List<Movement> by lazy {
        listOf(
            movement("Mercadona", 6435, MovementType.EXPENSE, "groceries", oct(5), PaymentMethod.CASH, author = laura, hasPhoto = true),
            movement("Cajero", 6000, MovementType.TRANSFER, "transfer", oct(3)),
            movement("Finiquito Emil", 117800, MovementType.INCOME, "salary", oct(2)),
            movement("Peña Agosto", 4500, MovementType.EXPENSE, "leisure", oct(1)),
            movement("Tarjeta Revolut", 699, MovementType.EXPENSE, "subscriptions", oct(1)),
            movement("Cosas", 398, MovementType.EXPENSE, "other", oct(1)),
            movement("Luz + Agua", 9727, MovementType.BILL, "home", oct(1)),
            movement("Tabaco", 620, MovementType.EXPENSE, "leisure", oct(1), PaymentMethod.CASH),
            movement("Préstamo", 15000, MovementType.EXPENSE, "loans", oct(1)),
            movement("Nómina Laura", 145000, MovementType.INCOME, "salary", oct(1), author = laura),
            movement("Alquiler", 65000, MovementType.BILL, "rent", oct(1), author = laura),
            movement("Gasolina", 5520, MovementType.EXPENSE, "transport", sep(27)),
            movement("Cena cumpleaños", 7340, MovementType.EXPENSE, "restaurants", sep(20), author = laura),
            movement("Supermercado", 11253, MovementType.EXPENSE, "groceries", sep(14)),
            movement("Internet", 3500, MovementType.BILL, "internet", sep(5)),
            movement("Sueldo", 132000, MovementType.INCOME, "salary", sep(1)),
            movement("Alquiler", 65000, MovementType.BILL, "rent", sep(1), author = laura),
        ).sortedWith(compareByDescending<Movement> { it.date }.thenByDescending { it.createdAt })
    }
}
