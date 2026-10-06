package com.chiiraac.migasto.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chiiraac.migasto.TestData
import com.chiiraac.migasto.data.local.LocalDatabase
import com.chiiraac.migasto.data.local.LocalFinanceRepository
import com.chiiraac.migasto.data.local.LocalPhotoStore
import com.chiiraac.migasto.data.model.GroupIcon
import com.chiiraac.migasto.data.model.MovementDraft
import com.chiiraac.migasto.data.model.MovementType
import com.chiiraac.migasto.data.model.PaymentMethod
import com.chiiraac.migasto.data.model.PhotoChange
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalFinanceRepositoryTest {
    private lateinit var db: LocalDatabase
    private lateinit var photosDir: File
    private lateinit var repository: LocalFinanceRepository
    private val user = TestData.emil

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LocalDatabase::class.java).allowMainThreadQueries().build()
        photosDir = File(context.filesDir, "photos-test").apply { deleteRecursively() }
        repository = LocalFinanceRepository(db, LocalPhotoStore(photosDir))
    }

    @After
    fun tearDown() {
        db.close()
        photosDir.deleteRecursively()
    }

    private fun draft(cents: Long = 1234, description: String = "Compra") = MovementDraft(
        type = MovementType.EXPENSE,
        method = PaymentMethod.CASH,
        amountCents = cents,
        description = description,
        categoryId = "groceries",
        date = LocalDate.of(2026, 10, 6),
    )

    @Test
    fun createGroupAndSaveMovements() = runTest {
        val group = repository.createGroup(user, "  Casa ", GroupIcon.HOME).getOrThrow()
        assertEquals("Casa", group.name)
        assertEquals(listOf(group.id), repository.observeGroups(user).first().map { it.id })

        repository.saveMovement(user, group.id, null, draft(), PhotoChange.Keep).getOrThrow()
        val saved = repository.observeMovements(group.id).first().single()
        assertEquals(1234L, saved.amountCents)
        assertEquals(user.name, saved.createdByName)
        assertFalse(saved.hasPhoto)

        // Edición: conserva autor y fecha de creación
        repository.saveMovement(user.copy(name = "Otro"), group.id, saved.id, draft(999, "Editado"), PhotoChange.Keep)
            .getOrThrow()
        val edited = repository.observeMovements(group.id).first().single()
        assertEquals(999L, edited.amountCents)
        assertEquals("Editado", edited.description)
        assertEquals(saved.createdAt, edited.createdAt)
        assertEquals(user.name, edited.createdByName)
    }

    @Test
    fun photosAreStoredAndRemoved() = runTest {
        val group = repository.createGroup(user, "Casa", GroupIcon.HOME).getOrThrow()
        val bytes = byteArrayOf(1, 2, 3, 4)
        repository.saveMovement(user, group.id, null, draft(), PhotoChange.Replace(bytes)).getOrThrow()
        val movement = repository.observeMovements(group.id).first().single()
        assertTrue(movement.hasPhoto)
        assertArrayEquals(bytes, repository.loadPhoto(movement))

        repository.saveMovement(user, group.id, movement.id, draft(), PhotoChange.Remove).getOrThrow()
        val withoutPhoto = repository.observeMovements(group.id).first().single()
        assertFalse(withoutPhoto.hasPhoto)
        assertNull(repository.loadPhoto(withoutPhoto))
    }

    @Test
    fun leavingGroupDeletesItsMovements() = runTest {
        val group = repository.createGroup(user, "Casa", GroupIcon.HOME).getOrThrow()
        repository.saveMovement(user, group.id, null, draft(), PhotoChange.Replace(byteArrayOf(9))).getOrThrow()
        repository.leaveGroup(user, group).getOrThrow()
        assertTrue(repository.observeGroups(user).first().isEmpty())
        assertTrue(repository.observeMovements(group.id).first().isEmpty())
        assertTrue(photosDir.listFiles().isNullOrEmpty())
    }

    @Test
    fun joiningIsNotAvailableOffline() = runTest {
        val error = repository.joinGroup(user, "ABCDEF").exceptionOrNull()
        assertEquals(AppError.Reason.NOT_AVAILABLE_OFFLINE_MODE, (error as AppError).reason)
    }

    @Test
    fun renamingUpdatesAuthorNames() = runTest {
        val group = repository.createGroup(user, "Casa", GroupIcon.HOME).getOrThrow()
        repository.saveMovement(user, group.id, null, draft(), PhotoChange.Keep).getOrThrow()
        repository.updateMemberProfile(user.copy(name = "Emilio")).getOrThrow()
        assertEquals("Emilio", repository.observeMovements(group.id).first().single().createdByName)
    }
}
