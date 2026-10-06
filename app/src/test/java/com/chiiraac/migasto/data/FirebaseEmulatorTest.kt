package com.chiiraac.migasto.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chiiraac.migasto.data.model.GroupIcon
import com.chiiraac.migasto.data.model.MovementDraft
import com.chiiraac.migasto.data.model.MovementType
import com.chiiraac.migasto.data.model.PaymentMethod
import com.chiiraac.migasto.data.model.PhotoChange
import com.chiiraac.migasto.data.model.UserProfile
import com.chiiraac.migasto.data.remote.FirebaseAuthRepository
import com.chiiraac.migasto.data.remote.FirestoreFinanceRepository
import com.chiiraac.migasto.data.repository.AuthState
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import java.net.InetSocketAddress
import java.net.Socket
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/**
 * Prueba de integración del modo nube contra los emuladores de Firebase.
 * Se omite si no están arrancados. Para ejecutarla:
 *   firebase emulators:exec --project demo-migasto --only auth,firestore \
 *     "./gradlew :app:testDebugUnitTest --tests '*FirebaseEmulatorTest*'"
 */
@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class FirebaseEmulatorTest {

    private fun isOpen(port: Int) = runCatching {
        Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 500) }
    }.isSuccess

    private fun app(name: String): FirebaseApp {
        val context = ApplicationProvider.getApplicationContext<Context>()
        FirebaseApp.getApps(context).firstOrNull { it.name == name }?.let { return it }
        val app = FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("1:123456789000:android:0000000000000000")
                .setProjectId("demo-migasto")
                .build(),
            name,
        )
        FirebaseAuth.getInstance(app).useEmulator("127.0.0.1", 9099)
        FirebaseFirestore.getInstance(app).apply {
            useEmulator("127.0.0.1", 8080)
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
        }
        return app
    }

    private suspend fun FirebaseAuthRepository.signedInUser(): UserProfile =
        (authState.first { it is AuthState.SignedIn } as AuthState.SignedIn).user

    @Test
    fun sharedGroupFlow() = runBlocking {
        assumeTrue("Emuladores de Firebase no arrancados", isOpen(8080) && isOpen(9099))
        withTimeout(60_000) {
            val suffix = System.currentTimeMillis()
            val appA = app("emil")
            val appB = app("laura")
            val authA = FirebaseAuthRepository(FirebaseAuth.getInstance(appA))
            val authB = FirebaseAuthRepository(FirebaseAuth.getInstance(appB))
            val repoA = FirestoreFinanceRepository(FirebaseFirestore.getInstance(appA))
            val repoB = FirestoreFinanceRepository(FirebaseFirestore.getInstance(appB))

            // Registro
            authA.register("Emil", "emil$suffix@example.com", "secreto123").getOrThrow()
            val emil = authA.signedInUser()
            assertEquals("Emil", emil.name)
            authB.register("Laura", "laura$suffix@example.com", "secreto456").getOrThrow()
            val laura = authB.signedInUser()

            // Contraseña incorrecta
            val wrong = FirebaseAuthRepository(FirebaseAuth.getInstance(app("otro")))
            val error = wrong.signIn("emil$suffix@example.com", "mala").exceptionOrNull() as AppError
            assertEquals(AppError.Reason.WRONG_CREDENTIALS, error.reason)

            // Emil crea un grupo y un movimiento con foto
            val group = repoA.createGroup(emil, "Casa", GroupIcon.HOME).getOrThrow()
            assertEquals(listOf(group.id), repoA.observeGroups(emil).first { it.isNotEmpty() }.map { it.id })
            val draft = MovementDraft(
                MovementType.EXPENSE, PaymentMethod.BANK, 4500, "Peña", "leisure", LocalDate.of(2026, 10, 1),
            )
            val photo = byteArrayOf(1, 2, 3, 4, 5)
            repoA.saveMovement(emil, group.id, null, draft, PhotoChange.Replace(photo)).getOrThrow()
            val saved = repoA.observeMovements(group.id).first { it.isNotEmpty() }.single()
            assertTrue(saved.hasPhoto)
            assertEquals("Emil", saved.createdByName)
            assertArrayEquals(photo, repoA.loadPhoto(saved))

            // Laura no puede unirse con un código falso, pero sí con el bueno
            val badJoin = repoB.joinGroup(laura, "ZZZZZZ").exceptionOrNull() as AppError
            assertEquals(AppError.Reason.INVALID_INVITE_CODE, badJoin.reason)
            val joined = repoB.joinGroup(laura, group.inviteCode.lowercase()).getOrThrow()
            assertEquals(setOf("Emil", "Laura"), joined.members.map { it.name }.toSet())

            // Laura ve y edita el movimiento; Emil ve el cambio
            val seenByLaura = repoB.observeMovements(group.id).first { it.isNotEmpty() }.single()
            repoB.saveMovement(laura, group.id, seenByLaura.id, draft.copy(amountCents = 5000), PhotoChange.Keep)
                .getOrThrow()
            val updated = repoA.observeMovements(group.id).first { list -> list.any { it.amountCents == 5000L } }.single()
            assertEquals("Emil", updated.createdByName)
            assertTrue(updated.hasPhoto)

            // Cambio de nombre visible para el grupo
            repoB.updateMemberProfile(laura.copy(name = "Laura G.")).getOrThrow()
            val renamed = repoA.observeGroups(emil).first { groups ->
                groups.single().members.any { it.name == "Laura G." }
            }
            assertEquals(2, renamed.single().members.size)

            // Laura sale; Emil (último miembro) borra su cuenta y el grupo desaparece
            repoB.leaveGroup(laura, renamed.single()).getOrThrow()
            assertTrue(repoB.observeGroups(laura).first { it.isEmpty() }.isEmpty())
            val onlyEmil = repoA.observeGroups(emil).first { it.single().members.size == 1 }.single()
            assertEquals(emil.uid, onlyEmil.ownerId)

            authA.reauthenticate("secreto123").getOrThrow()
            repoA.purgeUser(emil).getOrThrow()
            authA.deleteAccount().getOrThrow()
            assertEquals(AuthState.SignedOut, authA.authState.first { it == AuthState.SignedOut })

            // El código ya no existe
            val gone = repoB.joinGroup(laura, group.inviteCode).exceptionOrNull() as AppError
            assertEquals(AppError.Reason.INVALID_INVITE_CODE, gone.reason)
        }
    }
}
