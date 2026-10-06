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
import com.chiiraac.migasto.data.repository.Reauth
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

            authA.reauthenticate(Reauth.Password("secreto123")).getOrThrow()
            repoA.purgeUser(emil).getOrThrow()
            authA.deleteAccount().getOrThrow()
            assertEquals(AuthState.SignedOut, authA.authState.first { it == AuthState.SignedOut })

            // El código ya no existe
            val gone = repoB.joinGroup(laura, group.inviteCode).exceptionOrNull() as AppError
            assertEquals(AppError.Reason.INVALID_INVITE_CODE, gone.reason)
        }
    }

    @Test
    fun leavingWithStaleGroupKeepsItForNewMembers() = runBlocking {
        assumeTrue("Emuladores de Firebase no arrancados", isOpen(8080) && isOpen(9099))
        withTimeout(60_000) {
            val suffix = System.currentTimeMillis()
            val appA = app("ana")
            val appB = app("bea")
            val authA = FirebaseAuthRepository(FirebaseAuth.getInstance(appA))
            val authB = FirebaseAuthRepository(FirebaseAuth.getInstance(appB))
            val repoA = FirestoreFinanceRepository(FirebaseFirestore.getInstance(appA))
            val repoB = FirestoreFinanceRepository(FirebaseFirestore.getInstance(appB))
            authA.register("Ana", "ana$suffix@example.com", "secreto123").getOrThrow()
            val ana = authA.signedInUser()
            authB.register("Bea", "bea$suffix@example.com", "secreto456").getOrThrow()
            val bea = authB.signedInUser()

            // Ana crea el grupo y Bea se une, pero Ana aún tiene la copia antigua (solo ella)
            val stale = repoA.createGroup(ana, "Viaje", GroupIcon.HOME).getOrThrow()
            repoB.joinGroup(bea, stale.inviteCode).getOrThrow()
            assertEquals(1, stale.members.size)

            assertTrue(ana.hasPassword)
            assertTrue(!ana.usesGoogle)

            // Ana sale con esa copia: el servidor sabe que queda Bea, así que el grupo no se borra
            repoA.leaveGroup(ana, stale).getOrThrow()
            // Pulsar "Salir" otra vez no da error de permisos
            repoA.leaveGroup(ana, stale).getOrThrow()
            val kept = repoB.observeGroups(bea).first { list -> list.any { it.members.size == 1 } }.single()
            assertEquals(stale.id, kept.id)
            assertEquals(bea.uid, kept.ownerId)
            assertTrue(repoA.flushPendingWrites(5_000))

            repoB.purgeUser(bea).getOrThrow()
            authA.deleteAccount().getOrThrow()
            authB.deleteAccount().getOrThrow()
        }
    }

    @Test
    fun ownerRemovesMembersAndClosesTheGroup() = runBlocking {
        assumeTrue("Emuladores de Firebase no arrancados", isOpen(8080) && isOpen(9099))
        withTimeout(60_000) {
            val suffix = System.currentTimeMillis()
            val users = listOf("olga", "pablo", "quique").map { name ->
                val app = app("owner-$name")
                val auth = FirebaseAuthRepository(FirebaseAuth.getInstance(app))
                auth.register(name, "$name$suffix@example.com", "secreto123").getOrThrow()
                Triple(auth, FirestoreFinanceRepository(FirebaseFirestore.getInstance(app)), auth.signedInUser())
            }
            val (_, repoO, olga) = users[0]
            val (_, repoP, pablo) = users[1]
            val (_, repoQ, quique) = users[2]

            val group = repoO.createGroup(olga, "Piso", GroupIcon.HOME).getOrThrow()
            repoP.joinGroup(pablo, group.inviteCode).getOrThrow()
            val draft = MovementDraft(MovementType.EXPENSE, PaymentMethod.CASH, 1200, "Pan", "groceries", LocalDate.of(2026, 10, 6))
            repoO.saveMovement(olga, group.id, null, draft, PhotoChange.Keep).getOrThrow()
            repoP.saveMovement(pablo, group.id, null, draft, PhotoChange.Replace(byteArrayOf(1, 2, 3))).getOrThrow()
            repoP.saveMovement(pablo, group.id, null, draft.copy(amountCents = 800), PhotoChange.Keep).getOrThrow()
            repoO.observeMovements(group.id).first { it.size == 3 }

            // Solo el creador gestiona el grupo
            val notOwner = repoP.removeMember(pablo, group, olga.uid, deleteMovements = false).exceptionOrNull() as AppError
            assertEquals(AppError.Reason.PERMISSION_DENIED, notOwner.reason)

            // Grupo cerrado: nadie más puede unirse
            repoO.setJoinLocked(group, true).getOrThrow()
            assertTrue(repoO.observeGroups(olga).first { it.single().joinLocked }.single().joinLocked)
            val locked = repoQ.joinGroup(quique, group.inviteCode).exceptionOrNull() as AppError
            assertEquals(AppError.Reason.GROUP_LOCKED, locked.reason)

            // Quitar a Pablo y sus movimientos: deja de ver el grupo y solo queda el de Olga
            repoO.removeMember(olga, group, pablo.uid, deleteMovements = true).getOrThrow()
            assertTrue(repoP.observeGroups(pablo).first { it.isEmpty() }.isEmpty())
            val left = repoO.observeMovements(group.id).first { it.size == 1 }.single()
            assertEquals(olga.uid, left.createdById)

            // Al reabrirlo, se puede volver a entrar
            repoO.setJoinLocked(group, false).getOrThrow()
            repoQ.joinGroup(quique, group.inviteCode).getOrThrow()

            repoQ.purgeUser(quique).getOrThrow()
            repoO.purgeUser(olga).getOrThrow()
            users.forEach { (auth, _, _) -> auth.deleteAccount().getOrThrow() }
        }
    }

    @Test
    fun notificationSettingsAreStoredPerUser() = runBlocking {
        assumeTrue("Emuladores de Firebase no arrancados", isOpen(8080) && isOpen(9099))
        withTimeout(60_000) {
            val suffix = System.currentTimeMillis()
            val app = app("avisos")
            val auth = FirebaseAuthRepository(FirebaseAuth.getInstance(app))
            val repo = FirestoreFinanceRepository(FirebaseFirestore.getInstance(app))
            auth.register("Rosa", "rosa$suffix@example.com", "secreto123").getOrThrow()
            val rosa = auth.signedInUser()

            // Por defecto, ningún grupo silenciado
            assertEquals(emptySet<String>(), repo.observeMutedGroups(rosa).first())
            repo.registerPushToken(rosa, "token-1", "es").getOrThrow()
            repo.setGroupNotifications(rosa, "casa", enabled = false).getOrThrow()
            assertEquals(setOf("casa"), repo.observeMutedGroups(rosa).first { it.isNotEmpty() })
            repo.setGroupNotifications(rosa, "casa", enabled = true).getOrThrow()
            assertEquals(emptySet<String>(), repo.observeMutedGroups(rosa).first { it.isEmpty() })
            repo.unregisterPushToken(rosa, "token-1").getOrThrow()

            // Al borrar la cuenta desaparecen también sus ajustes de avisos
            repo.purgeUser(rosa).getOrThrow()
            auth.deleteAccount().getOrThrow()
        }
    }

    /** El emulador de Auth acepta tokens de Google "falsos" (JSON sin firmar) para probar el flujo. */
    private fun fakeGoogleToken(sub: String, email: String, name: String) =
        """{"sub":"$sub","email":"$email","email_verified":true,"name":"$name"}"""

    @Test
    fun googleAccountFlow() = runBlocking {
        assumeTrue("Emuladores de Firebase no arrancados", isOpen(8080) && isOpen(9099))
        withTimeout(60_000) {
            val suffix = System.currentTimeMillis()
            val appG = app("google")
            val auth = FirebaseAuthRepository(FirebaseAuth.getInstance(appG))
            val repo = FirestoreFinanceRepository(FirebaseFirestore.getInstance(appG))
            val token = fakeGoogleToken("g$suffix", "marta$suffix@gmail.com", "Marta Google")

            // Entrar con Google crea la cuenta con el nombre de Google
            auth.signInWithGoogle(token).getOrThrow()
            val marta = auth.signedInUser()
            assertEquals("Marta Google", marta.name)
            assertEquals("marta$suffix@gmail.com", marta.email)
            assertTrue(marta.usesGoogle)
            assertTrue(!marta.hasPassword)

            // Puede crear un grupo como cualquier otra cuenta
            val group = repo.createGroup(marta, "Piso", GroupIcon.HOME).getOrThrow()
            assertEquals(listOf(group.id), repo.observeGroups(marta).first { it.isNotEmpty() }.map { it.id })

            // Cerrar sesión y volver a entrar con Google recupera la misma cuenta
            auth.signOut()
            auth.authState.first { it == AuthState.SignedOut }
            auth.signInWithGoogle(token).getOrThrow()
            assertEquals(marta.uid, auth.signedInUser().uid)

            // Confirmar la identidad con otra cuenta de Google no vale
            val other = fakeGoogleToken("x$suffix", "otra$suffix@gmail.com", "Otra")
            val mismatch = auth.reauthenticate(Reauth.Google(other)).exceptionOrNull() as AppError
            assertEquals(AppError.Reason.GOOGLE_ACCOUNT_MISMATCH, mismatch.reason)

            // Borrar la cuenta confirmando con Google
            auth.reauthenticate(Reauth.Google(token)).getOrThrow()
            repo.purgeUser(marta).getOrThrow()
            auth.deleteAccount().getOrThrow()
            assertEquals(AuthState.SignedOut, auth.authState.first { it == AuthState.SignedOut })
        }
    }
}
